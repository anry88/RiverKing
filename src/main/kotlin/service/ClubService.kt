package service

import db.Catches
import db.Clubs
import db.ClubMembers
import db.ClubWeeklyContributions
import db.ClubWeeklyRewards
import db.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
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
        val currentWeek: ClubWeekView,
        val previousWeek: ClubWeekView,
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
            currentWeek = currentWeek,
            previousWeek = previousWeek,
        )
    }

    fun searchClubs(limit: Int = 10): List<ClubSummary> = transaction {
        val countExpr = ClubMembers.userId.count()
        val rows = (Clubs leftJoin ClubMembers)
            .slice(Clubs.id, Clubs.name, countExpr)
            .selectAll()
            .groupBy(Clubs.id, Clubs.name)
            .map { row ->
                ClubSummary(
                    id = row[Clubs.id].value,
                    name = row[Clubs.name],
                    memberCount = row[countExpr].toInt(),
                    capacity = MAX_MEMBERS,
                )
            }
            .filter { it.memberCount < MAX_MEMBERS }
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
                throw ClubException("weight_required")
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
        return clubDetails(userId) ?: throw ClubException("create_failed")
    }

    fun joinClub(userId: Long, clubId: Long): ClubDetails {
        transaction {
            if (!ClubMembers.select { ClubMembers.userId eq userId }.empty()) {
                throw ClubException("already_in_club")
            }
            val club = Clubs.select { Clubs.id eq clubId }.singleOrNull() ?: throw ClubException("not_found")
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
        }
        return clubDetails(userId) ?: throw ClubException("join_failed")
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
        }
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
                members.forEach { member ->
                    ClubWeeklyRewards.insert {
                        it[ClubWeeklyRewards.clubId] = clubId
                        it[ClubWeeklyRewards.userId] = member[ClubMembers.userId].value
                        it[ClubWeeklyRewards.weekStart] = weekStart
                        it[ClubWeeklyRewards.coins] = total
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
        val total = byUser.values.sum()
        return ClubWeekView(
            weekStart = weekStart,
            totalCoins = total,
            members = list,
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
        val roleOrder = mapOf(
            ROLE_PRESIDENT to 0,
            ROLE_HEIR to 1,
            ROLE_VETERAN to 2,
            ROLE_NOVICE to 3,
        )
        return rows.sortedWith(
            compareBy<ClubMemberContribution> { roleOrder[it.role] ?: 9 }
                .thenBy { it.name ?: "" }
                .thenBy { it.userId }
        )
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

    companion object {
        const val MAX_MEMBERS = 20
        const val MAX_NAME_LENGTH = 20
        const val CREATE_COST_COINS = 1000L
        const val MIN_CREATE_WEIGHT_KG = 1000.0
        const val ROLE_PRESIDENT = "president"
        const val ROLE_HEIR = "heir"
        const val ROLE_VETERAN = "veteran"
        const val ROLE_NOVICE = "novice"
        private val CLUB_ZONE: ZoneId = ZoneId.of("Europe/Belgrade")
    }
}
