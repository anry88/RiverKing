package service

import db.Catches
import db.Clubs
import db.ClubChatMessages
import db.ClubMembers
import db.ClubWeeklyContributions
import db.ClubWeeklyRewards
import db.Users
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import util.Metrics
import util.sanitizeName
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

class ClubService {
    data class ClubSummary(
        val id: Long,
        val name: String,
        val memberCount: Int,
        val capacity: Int,
        val info: String,
        val minJoinWeightKg: Double,
        val recruitingOpen: Boolean,
    )

    data class ClubMemberContribution(
        val userId: Long,
        val name: String?,
        val role: String,
        val coins: Int,
    )

    data class ClubWeekView(
        val weekStart: LocalDate,
        val totalCoins: Int,
        val members: List<ClubMemberContribution>,
    )

    data class ClubDetails(
        val id: Long,
        val name: String,
        val role: String,
        val memberCount: Int,
        val capacity: Int,
        val info: String,
        val minJoinWeightKg: Double,
        val recruitingOpen: Boolean,
        val currentWeek: ClubWeekView,
        val previousWeek: ClubWeekView,
    )

    data class ClubChatMessage(
        val id: Long,
        val message: String,
        val createdAt: Instant,
    )

    @Serializable
    private data class ClubChatPayload(
        val key: String,
        val params: Map<String, String> = emptyMap(),
    )

    class ClubException(val code: String) : RuntimeException(code)

    fun clubDetails(userId: Long): ClubDetails? = transaction {
        val membership = ClubMembers.select { ClubMembers.userId eq userId }.singleOrNull() ?: return@transaction null
        val clubId = membership[ClubMembers.clubId].value
        val club = Clubs.select { Clubs.id eq clubId }.singleOrNull() ?: return@transaction null
        val role = membership[ClubMembers.role]
        val members = loadMembers(clubId)
        val currentWeekStart = weekStart(LocalDate.now(CLUB_ZONE))
        val previousWeekStart = currentWeekStart.minusWeeks(1)
        val currentWeek = buildWeekView(clubId, members, currentWeekStart)
        val previousWeek = buildWeekView(clubId, members, previousWeekStart)
        ClubDetails(
            id = clubId,
            name = club[Clubs.name],
            role = role,
            memberCount = members.size,
            capacity = MAX_MEMBERS,
            info = club[Clubs.info],
            minJoinWeightKg = club[Clubs.minJoinWeightKg],
            recruitingOpen = club[Clubs.recruitingOpen],
            currentWeek = currentWeek,
            previousWeek = previousWeek,
        )
    }

    fun searchClubs(userId: Long, query: String? = null, limit: Int = 10): List<ClubSummary> = transaction {
        val userTotalWeight = totalCaughtKg(userId)
        val normalizedQuery = query?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val countExpr = ClubMembers.userId.count()
        val queryRows = (Clubs leftJoin ClubMembers)
            .slice(
                Clubs.id,
                Clubs.name,
                Clubs.info,
                Clubs.minJoinWeightKg,
                Clubs.recruitingOpen,
                countExpr,
            )
            .selectAll()
        val rows = queryRows
            .groupBy(Clubs.id, Clubs.name, Clubs.info, Clubs.minJoinWeightKg, Clubs.recruitingOpen)
            .map { row ->
                ClubSummary(
                    id = row[Clubs.id].value,
                    name = row[Clubs.name],
                    memberCount = row[countExpr].toInt(),
                    capacity = MAX_MEMBERS,
                    info = row[Clubs.info],
                    minJoinWeightKg = row[Clubs.minJoinWeightKg],
                    recruitingOpen = row[Clubs.recruitingOpen],
                )
            }
            .filter { normalizedQuery == null || it.name.lowercase().contains(normalizedQuery) }
            .filter { it.memberCount < MAX_MEMBERS }
            .filter { it.recruitingOpen }
            .filter { userTotalWeight >= it.minJoinWeightKg }
            .shuffled()
        rows.take(limit)
    }

    fun createClub(userId: Long, name: String): ClubDetails {
        val cleanName = validateName(name)
        transaction {
            if (!ClubMembers.select { ClubMembers.userId eq userId }.empty()) {
                throw ClubException("already_in_club")
            }
            val total = totalCaughtKg(userId)
            if (total < MIN_CREATE_WEIGHT_KG) {
                throw ClubException(weightRequiredCode(MIN_CREATE_WEIGHT_KG))
            }
            val userRow = Users.select { Users.id eq userId }.forUpdate().single()
            val balance = userRow[Users.coins]
            if (balance < CREATE_COST_COINS) {
                throw ClubException("not_enough_coins")
            }
            Users.update({ Users.id eq userId }) { it[Users.coins] = balance - CREATE_COST_COINS }
            val clubId = Clubs.insertAndGetId {
                it[Clubs.name] = cleanName
                it[presidentId] = userId
                it[createdAt] = Instant.now()
            }.value
            ClubMembers.insert {
                it[ClubMembers.clubId] = clubId
                it[ClubMembers.userId] = userId
                it[ClubMembers.role] = ROLE_PRESIDENT
                it[joinedAt] = Instant.now()
            }
        }
        Metrics.counter("club_create_total")
        return clubDetails(userId) ?: throw ClubException("create_failed")
    }

    fun joinClub(userId: Long, clubId: Long): ClubDetails {
        transaction {
            if (!ClubMembers.select { ClubMembers.userId eq userId }.empty()) {
                throw ClubException("already_in_club")
            }
            val club = Clubs.select { Clubs.id eq clubId }.singleOrNull() ?: throw ClubException("not_found")
            if (!club[Clubs.recruitingOpen]) {
                throw ClubException("recruitment_closed")
            }
            val minWeightKg = club[Clubs.minJoinWeightKg]
            val total = totalCaughtKg(userId)
            if (total < minWeightKg) {
                throw ClubException(weightRequiredCode(minWeightKg))
            }
            val memberCount = ClubMembers.select { ClubMembers.clubId eq clubId }.count()
            if (memberCount >= MAX_MEMBERS.toLong()) {
                throw ClubException("club_full")
            }
            ClubMembers.insert {
                it[ClubMembers.clubId] = club[Clubs.id].value
                it[ClubMembers.userId] = userId
                it[ClubMembers.role] = ROLE_NOVICE
                it[joinedAt] = Instant.now()
            }
            val memberName = displayNameByUserId(userId)
            addChatMessageTx(
                club[Clubs.id].value,
                chatPayload("clubChatMemberJoined", mapOf("name" to memberName)),
            )
        }
        Metrics.counter("club_join_total")
        return clubDetails(userId) ?: throw ClubException("join_failed")
    }

    fun leaveClub(userId: Long) {
        var deleted = false
        transaction {
            val membership = ClubMembers.select { ClubMembers.userId eq userId }.singleOrNull()
                ?: throw ClubException("not_in_club")
            val clubId = membership[ClubMembers.clubId].value
            val role = membership[ClubMembers.role]
            val memberName = displayNameByUserId(userId)
            ClubMembers.deleteWhere { ClubMembers.userId eq userId }
            if (role == ROLE_PRESIDENT) {
                val remaining = ClubMembers.select { ClubMembers.clubId eq clubId }.toList()
                if (remaining.isEmpty()) {
                    ClubWeeklyContributions.deleteWhere { ClubWeeklyContributions.clubId eq clubId }
                    ClubWeeklyRewards.deleteWhere { ClubWeeklyRewards.clubId eq clubId }
                    ClubChatMessages.deleteWhere { ClubChatMessages.clubId eq clubId }
                    Clubs.deleteWhere { Clubs.id eq clubId }
                    deleted = true
                    return@transaction
                }
                val heirs = remaining.filter { it[ClubMembers.role] == ROLE_HEIR }
                val nextPresident = (heirs.ifEmpty { remaining }).random()
                val newPresidentId = nextPresident[ClubMembers.userId].value
                Clubs.update({ Clubs.id eq clubId }) { it[presidentId] = newPresidentId }
                ClubMembers.update({
                    (ClubMembers.clubId eq clubId) and (ClubMembers.userId eq newPresidentId)
                }) { it[ClubMembers.role] = ROLE_PRESIDENT }
            }
            addChatMessageTx(clubId, chatPayload("clubChatMemberLeft", mapOf("name" to memberName)))
        }
        if (deleted) {
            Metrics.counter("club_delete_total")
        }
    }

    fun promoteMember(actorId: Long, targetId: Long): ClubDetails {
        return updateMemberRole(actorId, targetId, RoleAction.PROMOTE)
    }

    fun demoteMember(actorId: Long, targetId: Long): ClubDetails {
        return updateMemberRole(actorId, targetId, RoleAction.DEMOTE)
    }

    fun kickMember(actorId: Long, targetId: Long): ClubDetails {
        val clubId = transaction {
            val (actor, target) = loadActorAndTarget(actorId, targetId)
            ensureCanManage(actor.role, target.role)
            ClubMembers.deleteWhere {
                (ClubMembers.clubId eq actor.clubId) and (ClubMembers.userId eq target.userId)
            }
            val actorName = displayNameByUserId(actor.userId)
            val targetName = displayNameByUserId(target.userId)
            addChatMessageTx(
                actor.clubId,
                chatPayload(
                    "clubChatMemberKicked",
                    mapOf(
                        "actor" to actorName,
                        "target" to targetName,
                    ),
                ),
            )
            actor.clubId
        }
        Metrics.counter("club_kick_total")
        return clubDetails(actorId) ?: throw ClubException("update_failed")
    }

    fun appointPresident(actorId: Long, targetId: Long): ClubDetails {
        transaction {
            val (actor, target) = loadActorAndTarget(actorId, targetId)
            if (actor.role != ROLE_PRESIDENT) throw ClubException("forbidden")
            if (target.role != ROLE_HEIR) throw ClubException("invalid_role")
            Clubs.update({ Clubs.id eq actor.clubId }) { it[presidentId] = target.userId }
            ClubMembers.update({
                (ClubMembers.clubId eq actor.clubId) and (ClubMembers.userId eq actor.userId)
            }) { it[ClubMembers.role] = ROLE_HEIR }
            ClubMembers.update({
                (ClubMembers.clubId eq actor.clubId) and (ClubMembers.userId eq target.userId)
            }) { it[ClubMembers.role] = ROLE_PRESIDENT }
            val actorName = displayNameByUserId(actor.userId)
            val targetName = displayNameByUserId(target.userId)
            addChatMessageTx(
                actor.clubId,
                chatPayload(
                    "clubChatPresidentAppointed",
                    mapOf(
                        "actor" to actorName,
                        "target" to targetName,
                    ),
                ),
            )
        }
        return clubDetails(actorId) ?: throw ClubException("update_failed")
    }

    fun addContribution(userId: Long, coins: Int, at: Instant = Instant.now()) {
        if (coins <= 0) return
        transaction {
            val membership = ClubMembers.select { ClubMembers.userId eq userId }.singleOrNull() ?: return@transaction
            val clubId = membership[ClubMembers.clubId].value
            val weekStart = weekStart(ZonedDateTime.ofInstant(at, CLUB_ZONE).toLocalDate())
            val existing = ClubWeeklyContributions.select {
                (ClubWeeklyContributions.clubId eq clubId) and
                    (ClubWeeklyContributions.userId eq userId) and
                    (ClubWeeklyContributions.weekStart eq weekStart)
            }.singleOrNull()
            if (existing == null) {
                ClubWeeklyContributions.insert {
                    it[ClubWeeklyContributions.clubId] = clubId
                    it[ClubWeeklyContributions.userId] = userId
                    it[ClubWeeklyContributions.weekStart] = weekStart
                    it[ClubWeeklyContributions.coins] = coins
                }
            } else {
                val current = existing[ClubWeeklyContributions.coins]
                ClubWeeklyContributions.update({
                    (ClubWeeklyContributions.clubId eq clubId) and
                        (ClubWeeklyContributions.userId eq userId) and
                        (ClubWeeklyContributions.weekStart eq weekStart)
                }) { it[ClubWeeklyContributions.coins] = current + coins }
            }
            val memberName = displayNameByUserId(userId)
            addChatMessageTx(
                clubId,
                chatPayload(
                    "clubChatRatingReward",
                    mapOf(
                        "name" to memberName,
                        "coins" to coins.toString(),
                    ),
                ),
                at,
            )
        }
    }

    fun clubChat(userId: Long): List<ClubChatMessage> = transaction {
        val membership = ClubMembers.select { ClubMembers.userId eq userId }.singleOrNull()
            ?: throw ClubException("not_in_club")
        val clubId = membership[ClubMembers.clubId].value
        ClubChatMessages
            .select { ClubChatMessages.clubId eq clubId }
            .orderBy(ClubChatMessages.createdAt, SortOrder.DESC)
            .limit(CHAT_LIMIT)
            .map { row ->
                ClubChatMessage(
                    id = row[ClubChatMessages.id].value,
                    message = row[ClubChatMessages.message],
                    createdAt = row[ClubChatMessages.createdAt],
                )
            }
            .reversed()
    }

    fun updateClubInfo(userId: Long, info: String): ClubDetails {
        val trimmed = info.trim()
        if (trimmed.length > MAX_INFO_LENGTH) throw ClubException("info_too_long")
        transaction {
            val membership = ClubMembers.select { ClubMembers.userId eq userId }.singleOrNull()
                ?: throw ClubException("not_in_club")
            ensureCanEditClub(membership[ClubMembers.role])
            val clubId = membership[ClubMembers.clubId].value
            Clubs.update({ Clubs.id eq clubId }) {
                it[Clubs.info] = trimmed
            }
        }
        return clubDetails(userId) ?: throw ClubException("update_failed")
    }

    fun updateClubSettings(
        userId: Long,
        minJoinWeightKg: Double,
        recruitingOpen: Boolean,
    ): ClubDetails {
        if (minJoinWeightKg < 0.0 || minJoinWeightKg > MAX_JOIN_WEIGHT_KG) {
            throw ClubException("invalid_min_weight")
        }
        transaction {
            val membership = ClubMembers.select { ClubMembers.userId eq userId }.singleOrNull()
                ?: throw ClubException("not_in_club")
            ensureCanEditClub(membership[ClubMembers.role])
            val clubId = membership[ClubMembers.clubId].value
            Clubs.update({ Clubs.id eq clubId }) {
                it[Clubs.minJoinWeightKg] = minJoinWeightKg
                it[Clubs.recruitingOpen] = recruitingOpen
            }
        }
        return clubDetails(userId) ?: throw ClubException("update_failed")
    }

    internal fun logRareCatchTx(
        userId: Long,
        fishName: String,
        locationName: String,
        weightKg: Double,
        rarity: String,
        at: Instant = Instant.now(),
    ) {
        val membership = ClubMembers.select { ClubMembers.userId eq userId }.singleOrNull() ?: return
        val clubId = membership[ClubMembers.clubId].value
        val memberName = displayNameByUserId(userId)
        val weightLabel = String.format(java.util.Locale.US, "%.2f", weightKg)
        addChatMessageTx(
            clubId,
            chatPayload(
                "clubChatRareCatch",
                buildMap {
                    put("name", memberName)
                    put("rarity", rarity)
                    put("fish", fishName)
                    put("weight", weightLabel)
                    if (locationName.isNotBlank()) {
                        put("location", locationName)
                    }
                },
            ),
            at,
        )
    }

    fun distributeWeeklyRewards(now: Instant = Instant.now()) {
        val weekStart = weekStart(ZonedDateTime.ofInstant(now, CLUB_ZONE).toLocalDate()).minusWeeks(1)
        transaction {
            Clubs.selectAll().forEach { clubRow ->
                val clubId = clubRow[Clubs.id].value
                val alreadyAwarded = !ClubWeeklyRewards.select {
                    (ClubWeeklyRewards.clubId eq clubId) and (ClubWeeklyRewards.weekStart eq weekStart)
                }.empty()
                if (alreadyAwarded) return@forEach
                val sumExpr = ClubWeeklyContributions.coins.sum()
                val total = ClubWeeklyContributions
                    .slice(sumExpr)
                    .select {
                        (ClubWeeklyContributions.clubId eq clubId) and
                            (ClubWeeklyContributions.weekStart eq weekStart)
                    }
                    .singleOrNull()
                    ?.get(sumExpr) ?: 0
                if (total <= 0) return@forEach
                val members = ClubMembers.select { ClubMembers.clubId eq clubId }.toList()
                if (members.isEmpty()) return@forEach
                val perMember = total / members.size
                if (perMember <= 0) return@forEach
                members.forEach { member ->
                    ClubWeeklyRewards.insert {
                        it[ClubWeeklyRewards.clubId] = clubId
                        it[ClubWeeklyRewards.userId] = member[ClubMembers.userId].value
                        it[ClubWeeklyRewards.weekStart] = weekStart
                        it[ClubWeeklyRewards.coins] = perMember
                        it[ClubWeeklyRewards.claimed] = false
                        it[ClubWeeklyRewards.createdAt] = now
                    }
                }
            }
        }
    }

    fun pendingRewards(userId: Long): List<UserPrize> = transaction {
        ClubWeeklyRewards
            .select { (ClubWeeklyRewards.userId eq userId) and (ClubWeeklyRewards.claimed eq false) }
            .map { row ->
                val coins = row[ClubWeeklyRewards.coins]
                UserPrize(
                    id = row[ClubWeeklyRewards.id].value,
                    packageId = COIN_PRIZE_ID,
                    qty = coins,
                    rank = 0,
                    coins = coins,
                    source = PrizeSource.CLUB,
                )
            }
    }

    fun hasPendingReward(userId: Long, rewardId: Long): Boolean = transaction {
        !ClubWeeklyRewards
            .select {
                (ClubWeeklyRewards.id eq rewardId) and
                    (ClubWeeklyRewards.userId eq userId) and
                    (ClubWeeklyRewards.claimed eq false)
            }
            .empty()
    }

    fun claimReward(userId: Long, rewardId: Long): Int = transaction {
        val row = ClubWeeklyRewards.select {
            (ClubWeeklyRewards.id eq rewardId) and
                (ClubWeeklyRewards.userId eq userId) and
                (ClubWeeklyRewards.claimed eq false)
        }.singleOrNull() ?: throw ClubException("not_found")
        ClubWeeklyRewards.update({ ClubWeeklyRewards.id eq rewardId }) { it[claimed] = true }
        row[ClubWeeklyRewards.coins]
    }

    private fun buildWeekView(
        clubId: Long,
        members: List<ClubMemberContribution>,
        weekStart: LocalDate,
    ): ClubWeekView {
        val byUser = ClubWeeklyContributions
            .select {
                (ClubWeeklyContributions.clubId eq clubId) and
                    (ClubWeeklyContributions.weekStart eq weekStart)
            }
            .associate { row ->
                row[ClubWeeklyContributions.userId].value to row[ClubWeeklyContributions.coins]
            }
        val list = members.map { member ->
            member.copy(coins = byUser[member.userId] ?: 0)
        }
        val sorted = list.sortedWith(
            compareByDescending<ClubMemberContribution> { it.coins }
                .thenBy { roleRank(it.role) }
                .thenBy { it.name ?: "" }
                .thenBy { it.userId }
        )
        val total = byUser.values.sum()
        return ClubWeekView(
            weekStart = weekStart,
            totalCoins = total,
            members = sorted,
        )
    }

    private fun loadMembers(clubId: Long): List<ClubMemberContribution> {
        val rows = (ClubMembers innerJoin Users)
            .select { ClubMembers.clubId eq clubId }
            .map { row ->
                ClubMemberContribution(
                    userId = row[ClubMembers.userId].value,
                    name = displayName(row),
                    role = row[ClubMembers.role],
                    coins = 0,
                )
            }
        return rows.sortedWith(
            compareBy<ClubMemberContribution> { roleRank(it.role) }
                .thenBy { it.name ?: "" }
                .thenBy { it.userId }
        )
    }

    private fun updateMemberRole(actorId: Long, targetId: Long, action: RoleAction): ClubDetails {
        transaction {
            val (actor, target) = loadActorAndTarget(actorId, targetId)
            ensureCanManage(actor.role, target.role)
            val nextRole = when (action) {
                RoleAction.PROMOTE -> nextRole(target.role)
                RoleAction.DEMOTE -> previousRole(target.role)
            }
            ClubMembers.update({
                (ClubMembers.clubId eq actor.clubId) and (ClubMembers.userId eq target.userId)
            }) { it[role] = nextRole }
            val actorName = displayNameByUserId(actor.userId)
            val targetName = displayNameByUserId(target.userId)
            val key = if (action == RoleAction.PROMOTE) "clubChatRolePromoted" else "clubChatRoleDemoted"
            addChatMessageTx(
                actor.clubId,
                chatPayload(
                    key,
                    mapOf(
                        "actor" to actorName,
                        "target" to targetName,
                    ),
                ),
            )
        }
        when (action) {
            RoleAction.PROMOTE -> Metrics.counter("club_promote_total")
            RoleAction.DEMOTE -> Metrics.counter("club_demote_total")
        }
        return clubDetails(actorId) ?: throw ClubException("update_failed")
    }

    private fun ensureCanManage(actorRole: String, targetRole: String) {
        val allowed = when (actorRole) {
            ROLE_PRESIDENT -> targetRole != ROLE_PRESIDENT
            ROLE_HEIR -> targetRole == ROLE_VETERAN || targetRole == ROLE_NOVICE
            else -> false
        }
        if (!allowed) throw ClubException("forbidden")
    }

    private fun ensureCanEditClub(actorRole: String) {
        val allowed = actorRole == ROLE_PRESIDENT || actorRole == ROLE_HEIR
        if (!allowed) throw ClubException("forbidden")
    }

    private fun nextRole(role: String): String = when (role) {
        ROLE_NOVICE -> ROLE_VETERAN
        ROLE_VETERAN -> ROLE_HEIR
        else -> throw ClubException("invalid_role")
    }

    private fun previousRole(role: String): String = when (role) {
        ROLE_HEIR -> ROLE_VETERAN
        ROLE_VETERAN -> ROLE_NOVICE
        else -> throw ClubException("invalid_role")
    }

    private fun roleRank(role: String): Int = when (role) {
        ROLE_PRESIDENT -> 0
        ROLE_HEIR -> 1
        ROLE_VETERAN -> 2
        ROLE_NOVICE -> 3
        else -> 9
    }

    private data class MemberEntry(
        val clubId: Long,
        val userId: Long,
        val role: String,
    )

    private fun loadActorAndTarget(actorId: Long, targetId: Long): Pair<MemberEntry, MemberEntry> {
        if (actorId == targetId) throw ClubException("invalid_target")
        val actor = ClubMembers.select { ClubMembers.userId eq actorId }.singleOrNull()
            ?: throw ClubException("not_in_club")
        val target = ClubMembers.select { ClubMembers.userId eq targetId }.singleOrNull()
            ?: throw ClubException("member_not_found")
        val actorClubId = actor[ClubMembers.clubId].value
        val targetClubId = target[ClubMembers.clubId].value
        if (actorClubId != targetClubId) throw ClubException("member_not_found")
        return MemberEntry(actorClubId, actorId, actor[ClubMembers.role]) to
            MemberEntry(targetClubId, targetId, target[ClubMembers.role])
    }

    private fun totalCaughtKg(userId: Long): Double {
        val sumExpr = Catches.weight.sum()
        return Catches
            .slice(sumExpr)
            .select { Catches.userId eq userId }
            .singleOrNull()
            ?.get(sumExpr) ?: 0.0
    }

    private fun validateName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isBlank()) throw ClubException("name_empty")
        if (trimmed.length > MAX_NAME_LENGTH) throw ClubException("name_too_long")
        val sanitized = sanitizeName(trimmed, MAX_NAME_LENGTH)
        if (sanitized != trimmed) throw ClubException("name_profanity")
        return trimmed
    }

    private fun displayName(row: ResultRow): String? {
        val fn = row.getOrNull(Users.firstName)
        val ln = row.getOrNull(Users.lastName)
        val un = row.getOrNull(Users.username)
        val nn = row.getOrNull(Users.nickname)
        val raw = when {
            !nn.isNullOrBlank() -> nn
            !fn.isNullOrBlank() || !ln.isNullOrBlank() -> listOfNotNull(fn, ln).joinToString(" ").trim()
            !un.isNullOrBlank() -> un
            else -> null
        }
        return raw?.let { sanitizeName(it) }
    }

    private fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun displayNameByUserId(userId: Long): String {
        val row = Users.select { Users.id eq userId }.singleOrNull() ?: return "Участник"
        return displayName(row) ?: "Участник"
    }

    private fun addChatMessageTx(clubId: Long, message: String, at: Instant = Instant.now()) {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return
        ClubChatMessages.insert {
            it[ClubChatMessages.clubId] = clubId
            it[ClubChatMessages.message] = trimmed.take(CHAT_MESSAGE_MAX_LENGTH)
            it[ClubChatMessages.createdAt] = at
        }
        trimChatTx(clubId)
    }

    private fun chatPayload(key: String, params: Map<String, String> = emptyMap()): String {
        return Json.encodeToString(ClubChatPayload(key, params))
    }

    private fun trimChatTx(clubId: Long) {
        val keepIds = ClubChatMessages
            .slice(ClubChatMessages.id)
            .select { ClubChatMessages.clubId eq clubId }
            .orderBy(ClubChatMessages.createdAt, SortOrder.DESC)
            .limit(CHAT_LIMIT)
            .map { it[ClubChatMessages.id].value }
        if (keepIds.size < CHAT_LIMIT) return
        val keepEntityIds = keepIds.map { EntityID(it, ClubChatMessages) }
        ClubChatMessages.deleteWhere {
            (ClubChatMessages.clubId eq clubId) and (ClubChatMessages.id notInList keepEntityIds)
        }
    }

    companion object {
        const val MAX_MEMBERS = 20
        const val MAX_NAME_LENGTH = 20
        const val MAX_INFO_LENGTH = 500
        const val CREATE_COST_COINS = 1000L
        const val MIN_CREATE_WEIGHT_KG = 1000.0
        const val MAX_JOIN_WEIGHT_KG = 1000000.0
        const val ROLE_PRESIDENT = "president"
        const val ROLE_HEIR = "heir"
        const val ROLE_VETERAN = "veteran"
        const val ROLE_NOVICE = "novice"
        const val CHAT_LIMIT = 100
        const val CHAT_MESSAGE_MAX_LENGTH = 500
        private val CLUB_ZONE: ZoneId = ZoneId.of("Europe/Belgrade")
    }

    private fun weightRequiredCode(minWeightKg: Double): String =
        "weight_required:${"%.0f".format(minWeightKg)}"

    private enum class RoleAction {
        PROMOTE,
        DEMOTE,
    }
}
