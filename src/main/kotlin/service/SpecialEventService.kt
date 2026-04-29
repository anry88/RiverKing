package service

import db.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.transactions.transaction
import util.sanitizeName
import java.time.Instant

const val EVENT_PRIZE_ID_OFFSET: Long = -9_000_000_000_000L

data class SpecialEvent(
    val id: Long,
    val nameRu: String,
    val nameEn: String,
    val startTime: Instant,
    val endTime: Instant,
    val imagePath: String?,
    val castZone: CastZoneDTO?,
    val weightPrizePlaces: Int,
    val countPrizePlaces: Int,
    val fishPrizePlaces: Int,
    val weightPrizesJson: String,
    val countPrizesJson: String,
    val fishPrizesJson: String,
)

data class SpecialEventFishSpec(
    val fishId: Long,
    val weight: Double,
)

data class SpecialEventPrizeConfig(
    val prizePlaces: Int,
    val prizesJson: String,
)

data class SpecialEventClubEntry(
    val clubId: Long,
    val club: String,
    val value: Double,
    val rank: Int,
    val prize: PrizeSpec? = null,
)

data class SpecialEventPersonalEntry(
    val userId: Long,
    val user: String?,
    val value: Double,
    val rank: Int,
    val catchId: Long?,
    val fish: String,
    val fishId: Long,
    val rarity: String,
    val weight: Double,
    val at: Instant,
    val prize: PrizeSpec? = null,
)

data class SpecialEventLeaderboard(
    val event: SpecialEvent,
    val weight: List<SpecialEventClubEntry>,
    val count: List<SpecialEventClubEntry>,
    val fish: List<SpecialEventPersonalEntry>,
    val mineWeight: SpecialEventClubEntry?,
    val mineCount: SpecialEventClubEntry?,
    val mineFish: SpecialEventPersonalEntry?,
)

class SpecialEventService {
    enum class Category(val id: String) {
        TOTAL_WEIGHT("total_weight"),
        TOTAL_COUNT("total_count"),
        PERSONAL_FISH("personal_fish");

        companion object {
            fun fromId(id: String): Category? = entries.firstOrNull { it.id == id }
        }
    }

    class SpecialEventException(val code: String) : RuntimeException(code)

    private fun ResultRow.toSpecialEvent() = SpecialEvent(
        id = this[SpecialEvents.id].value,
        nameRu = this[SpecialEvents.nameRu],
        nameEn = this[SpecialEvents.nameEn],
        startTime = this[SpecialEvents.startTime],
        endTime = this[SpecialEvents.endTime],
        imagePath = this[SpecialEvents.imagePath],
        castZone = eventLocationCastZoneTx(this[SpecialEvents.id].value),
        weightPrizePlaces = this[SpecialEvents.weightPrizePlaces],
        countPrizePlaces = this[SpecialEvents.countPrizePlaces],
        fishPrizePlaces = this[SpecialEvents.fishPrizePlaces],
        weightPrizesJson = this[SpecialEvents.weightPrizesJson],
        countPrizesJson = this[SpecialEvents.countPrizesJson],
        fishPrizesJson = this[SpecialEvents.fishPrizesJson],
    )

    fun createEvent(
        nameRu: String,
        nameEn: String,
        start: Instant,
        end: Instant,
        imagePath: String?,
        castZone: CastZoneDTO?,
        fish: List<SpecialEventFishSpec>,
        weightPrizes: SpecialEventPrizeConfig,
        countPrizes: SpecialEventPrizeConfig,
        fishPrizes: SpecialEventPrizeConfig,
    ): Long = transaction {
        validateEventInput(nameRu, nameEn, start, end, castZone, fish)
        ensureNoOverlap(start, end, excludeId = null)
        val eventId = SpecialEvents.insertAndGetId {
            it[SpecialEvents.nameRu] = nameRu.trim()
            it[SpecialEvents.nameEn] = nameEn.trim()
            it[SpecialEvents.startTime] = start
            it[SpecialEvents.endTime] = end
            it[SpecialEvents.imagePath] = imagePath
            it[weightPrizePlaces] = weightPrizes.prizePlaces
            it[countPrizePlaces] = countPrizes.prizePlaces
            it[fishPrizePlaces] = fishPrizes.prizePlaces
            it[weightPrizesJson] = weightPrizes.prizesJson
            it[countPrizesJson] = countPrizes.prizesJson
            it[fishPrizesJson] = fishPrizes.prizesJson
        }.value
        Locations.insert {
            it[name] = nameRu.trim()
            it[unlockKg] = 0.0
            it[sizeMultiplier] = 1.0
            it[specialEventId] = eventId
            it[castZoneJson] = CastZoneCodec.encode(castZone)
        }
        replaceFishTx(eventId, fish)
        eventId
    }

    fun updateEvent(
        id: Long,
        nameRu: String,
        nameEn: String,
        start: Instant,
        end: Instant,
        imagePath: String?,
        castZone: CastZoneDTO?,
        fish: List<SpecialEventFishSpec>,
        weightPrizes: SpecialEventPrizeConfig,
        countPrizes: SpecialEventPrizeConfig,
        fishPrizes: SpecialEventPrizeConfig,
    ) = transaction {
        validateEventInput(nameRu, nameEn, start, end, castZone, fish)
        ensureNoOverlap(start, end, excludeId = id)
        val updated = SpecialEvents.update({ SpecialEvents.id eq id }) {
            it[SpecialEvents.nameRu] = nameRu.trim()
            it[SpecialEvents.nameEn] = nameEn.trim()
            it[SpecialEvents.startTime] = start
            it[SpecialEvents.endTime] = end
            it[SpecialEvents.imagePath] = imagePath
            it[weightPrizePlaces] = weightPrizes.prizePlaces
            it[countPrizePlaces] = countPrizes.prizePlaces
            it[fishPrizePlaces] = fishPrizes.prizePlaces
            it[weightPrizesJson] = weightPrizes.prizesJson
            it[countPrizesJson] = countPrizes.prizesJson
            it[fishPrizesJson] = fishPrizes.prizesJson
        }
        if (updated == 0) throw SpecialEventException("not_found")
        val locationUpdated = Locations.update({ Locations.specialEventId eq id }) {
            it[name] = nameRu.trim()
            it[castZoneJson] = CastZoneCodec.encode(castZone)
        }
        if (locationUpdated == 0) {
            Locations.insert {
                it[name] = nameRu.trim()
                it[unlockKg] = 0.0
                it[sizeMultiplier] = 1.0
                it[specialEventId] = id
                it[castZoneJson] = CastZoneCodec.encode(castZone)
            }
        }
        replaceFishTx(id, fish)
    }

    fun deleteEvent(id: Long) = transaction {
        val locationIds = Locations
            .slice(Locations.id)
            .select { Locations.specialEventId eq id }
            .map { it[Locations.id].value }
        if (locationIds.isNotEmpty()) {
            Users.update({ Users.currentLocationId inList locationIds }) {
                it[currentLocationId] = null
                it[currentEventId] = null
            }
            Users.update({ Users.currentEventId eq id }) {
                it[currentEventId] = null
            }
        }
        SpecialEventRewardRuns.deleteWhere { SpecialEventRewardRuns.eventId eq id }
        SpecialEventPrizes.deleteWhere { SpecialEventPrizes.eventId eq id }
        SpecialEventCatches.deleteWhere { SpecialEventCatches.eventId eq id }
        SpecialEventClubProgress.deleteWhere { SpecialEventClubProgress.eventId eq id }
        SpecialEventUserProgress.deleteWhere { SpecialEventUserProgress.eventId eq id }
        SpecialEventFish.deleteWhere { SpecialEventFish.eventId eq id }
        if (locationIds.isNotEmpty()) {
            val hasCatches = Catches.select { Catches.locationId inList locationIds }.any()
            if (!hasCatches) {
                Locations.deleteWhere { Locations.specialEventId eq id }
            }
        }
        SpecialEvents.deleteWhere { SpecialEvents.id eq id }
    }

    fun listEvents(limit: Int? = null, offset: Long = 0L): List<SpecialEvent> = transaction {
        val query = SpecialEvents.selectAll()
            .orderBy(SpecialEvents.startTime to SortOrder.DESC, SpecialEvents.id to SortOrder.DESC)
        (if (limit != null) query.limit(limit, offset) else query).map { it.toSpecialEvent() }
    }

    fun getEvent(id: Long): SpecialEvent? = transaction { getEventTx(id) }

    fun currentEvent(now: Instant = Instant.now()): SpecialEvent? = transaction { currentEventTx(now) }

    fun previousEvent(now: Instant = Instant.now()): SpecialEvent? = transaction {
        SpecialEvents.select { SpecialEvents.endTime less now }
            .orderBy(SpecialEvents.endTime to SortOrder.DESC, SpecialEvents.id to SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.toSpecialEvent()
    }

    fun eventLocationId(eventId: Long): Long? = transaction { eventLocationIdTx(eventId) }

    fun activeEventForLocation(locationId: Long, now: Instant = Instant.now()): SpecialEvent? = transaction {
        val eventId = Locations
            .slice(Locations.specialEventId)
            .select { (Locations.id eq locationId) and Locations.specialEventId.isNotNull() }
            .singleOrNull()
            ?.get(Locations.specialEventId)
            ?: return@transaction null
        val event = getEventTx(eventId) ?: return@transaction null
        if (event.startTime <= now && event.endTime >= now) event else null
    }

    fun fishSpecs(eventId: Long): List<SpecialEventFishSpec> = transaction {
        SpecialEventFish.select { SpecialEventFish.eventId eq eventId }
            .map { SpecialEventFishSpec(it[SpecialEventFish.fishId].value, it[SpecialEventFish.weight]) }
    }

    fun userClubIdTx(userId: Long): Long? =
        ClubMembers.select { ClubMembers.userId eq userId }
            .singleOrNull()
            ?.get(ClubMembers.clubId)
            ?.value

    fun isActiveEventLocationTx(locationId: Long, now: Instant = Instant.now()): SpecialEvent? {
        val eventId = Locations
            .slice(Locations.specialEventId)
            .select { (Locations.id eq locationId) and Locations.specialEventId.isNotNull() }
            .singleOrNull()
            ?.get(Locations.specialEventId)
            ?: return null
        val event = getEventTx(eventId) ?: return null
        return if (event.startTime <= now && event.endTime >= now) event else null
    }

    fun validateCanUseEventLocationTx(userId: Long, locationId: Long, now: Instant = Instant.now()): SpecialEvent? {
        val event = isActiveEventLocationTx(locationId, now) ?: return null
        if (userClubIdTx(userId) == null) throw SpecialEventException("event_requires_club")
        return event
    }

    fun currentEventLocationForUserTx(userId: Long, language: String): FishingEventLocation? {
        val event = currentEventTx() ?: return null
        val locationId = eventLocationIdTx(event.id) ?: return null
        val hasClub = userClubIdTx(userId) != null
        val locationName = if (language == "en") event.nameEn else event.nameRu
        return FishingEventLocation(
            locationId = locationId,
            eventId = event.id,
            name = locationName,
            imagePath = event.imagePath,
            castZone = eventLocationCastZoneTx(event.id),
            unlocked = hasClub,
            lockedReason = if (hasClub) null else if (language == "en") {
                "Join a club to take part in the special event."
            } else {
                "Вступи в клуб, чтобы принять участие в специальном событии."
            },
        )
    }

    fun recordCatchTx(
        userId: Long,
        catchId: Long,
        fishId: Long,
        rarity: String,
        weight: Double,
        locationId: Long,
        caughtAt: Instant,
    ) {
        val event = isActiveEventLocationTx(locationId, caughtAt) ?: return
        val clubId = userClubIdTx(userId) ?: return
        val activeProgress = SpecialEventUserProgress
            .select {
                (SpecialEventUserProgress.eventId eq event.id) and
                    (SpecialEventUserProgress.userId eq userId) and
                    (SpecialEventUserProgress.clubId eq clubId) and
                    (SpecialEventUserProgress.active eq true)
            }
            .orderBy(SpecialEventUserProgress.id, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
        val progressId = activeProgress?.get(SpecialEventUserProgress.id)?.value
            ?: SpecialEventUserProgress.insertAndGetId {
                it[eventId] = event.id
                it[SpecialEventUserProgress.userId] = userId
                it[SpecialEventUserProgress.clubId] = clubId
                it[totalWeight] = 0.0
                it[totalCount] = 0
                it[active] = true
                it[startedAt] = caughtAt
                it[updatedAt] = caughtAt
            }.value

        val currentWeight = activeProgress?.get(SpecialEventUserProgress.totalWeight) ?: 0.0
        val currentCount = activeProgress?.get(SpecialEventUserProgress.totalCount) ?: 0
        SpecialEventUserProgress.update({ SpecialEventUserProgress.id eq progressId }) {
            it[totalWeight] = currentWeight + weight
            it[totalCount] = currentCount + 1
            it[updatedAt] = caughtAt
        }

        addClubProgressTx(event.id, clubId, weight, 1, caughtAt)
        SpecialEventCatches.insert {
            it[eventId] = event.id
            it[SpecialEventCatches.progressId] = progressId
            it[SpecialEventCatches.userId] = userId
            it[SpecialEventCatches.clubId] = clubId
            it[SpecialEventCatches.catchId] = catchId
            it[SpecialEventCatches.fishId] = fishId
            it[SpecialEventCatches.weight] = weight
            it[SpecialEventCatches.rarity] = rarity
            it[rarityRank] = rarityRank(rarity)
            it[createdAt] = caughtAt
        }
    }

    fun deactivateActiveProgressForUserTx(userId: Long, now: Instant = Instant.now()) {
        val eventIds = SpecialEvents
            .slice(SpecialEvents.id)
            .select { (SpecialEvents.startTime lessEq now) and (SpecialEvents.endTime greaterEq now) }
            .map { it[SpecialEvents.id].value }
        if (eventIds.isEmpty()) return
        val rows = SpecialEventUserProgress.select {
            (SpecialEventUserProgress.userId eq userId) and
                (SpecialEventUserProgress.eventId inList eventIds) and
                (SpecialEventUserProgress.active eq true)
        }.toList()
        rows.forEach { row ->
            val eventId = row[SpecialEventUserProgress.eventId].value
            val clubId = row[SpecialEventUserProgress.clubId].value
            val weight = row[SpecialEventUserProgress.totalWeight]
            val count = row[SpecialEventUserProgress.totalCount]
            if (weight != 0.0 || count != 0) {
                addClubProgressTx(eventId, clubId, -weight, -count, now)
            }
            SpecialEventUserProgress.update({ SpecialEventUserProgress.id eq row[SpecialEventUserProgress.id].value }) {
                it[active] = false
                it[updatedAt] = now
            }
        }
    }

    fun leaderboard(eventId: Long, userId: Long, limit: Int = 20): SpecialEventLeaderboard? = transaction {
        val event = getEventTx(eventId) ?: return@transaction null
        val clubId = userClubIdTx(userId)
        val weightPrizes = decodePrizes(event.weightPrizesJson)
        val countPrizes = decodePrizes(event.countPrizesJson)
        val fishPrizes = decodePrizes(event.fishPrizesJson)
        val weightRanked = clubLeaderboardTx(
            eventId = event.id,
            category = Category.TOTAL_WEIGHT,
            limit = limit,
            prizes = weightPrizes,
        )
        val countRanked = clubLeaderboardTx(
            eventId = event.id,
            category = Category.TOTAL_COUNT,
            limit = limit,
            prizes = countPrizes,
        )
        val fishRanked = personalLeaderboardTx(
            eventId = event.id,
            limit = limit,
            prizes = fishPrizes,
        )
        SpecialEventLeaderboard(
            event = event,
            weight = weightRanked.top,
            count = countRanked.top,
            fish = fishRanked.top,
            mineWeight = clubId?.let { weightRanked.all.firstOrNull { row -> row.clubId == it } },
            mineCount = clubId?.let { countRanked.all.firstOrNull { row -> row.clubId == it } },
            mineFish = fishRanked.all.firstOrNull { it.userId == userId },
        )
    }

    fun pendingPrizes(userId: Long, language: String): List<UserPrize> = transaction {
        (SpecialEventPrizes innerJoin SpecialEvents)
            .select {
                (SpecialEventPrizes.userId eq userId) and
                    (SpecialEventPrizes.claimed eq false)
            }
            .map { row ->
                val packageId = row[SpecialEventPrizes.packageId]
                val qty = row[SpecialEventPrizes.qty]
                UserPrize(
                    id = encodePrizeId(row[SpecialEventPrizes.id].value),
                    packageId = packageId,
                    qty = qty,
                    rank = row[SpecialEventPrizes.rank],
                    coins = if (packageId == COIN_PRIZE_ID) qty else null,
                    source = PrizeSource.EVENT,
                    sourceLabel = if (language == "en") row[SpecialEvents.nameEn] else row[SpecialEvents.nameRu],
                )
            }
    }

    fun claimPrize(userId: Long, encodedPrizeId: Long, fishing: FishingService): Pair<List<FishingService.LureDTO>, Long?> {
        val prizeId = decodePrizeId(encodedPrizeId)
        val (pack, qty) = transaction {
            val row = SpecialEventPrizes.select {
                (SpecialEventPrizes.id eq prizeId) and
                    (SpecialEventPrizes.userId eq userId) and
                    (SpecialEventPrizes.claimed eq false)
            }.singleOrNull() ?: error("not found")
            SpecialEventPrizes.update({ SpecialEventPrizes.id eq prizeId }) { it[claimed] = true }
            row[SpecialEventPrizes.packageId] to row[SpecialEventPrizes.qty]
        }
        if (pack == COIN_PRIZE_ID) {
            if (qty > 0) fishing.addCoins(userId, qty)
            return Pair(emptyList(), null)
        }
        var res: Pair<List<FishingService.LureDTO>, Long?>? = null
        repeat(qty) { res = fishing.buyPackage(userId, pack) }
        return res ?: Pair(emptyList(), null)
    }

    fun distributePrizes(now: Instant = Instant.now()) {
        val ended = transaction {
            SpecialEvents.select { SpecialEvents.endTime lessEq now }.map { it.toSpecialEvent() }
        }
        ended.forEach { event ->
            distributeCategory(event, Category.TOTAL_WEIGHT)
            distributeCategory(event, Category.TOTAL_COUNT)
            distributeCategory(event, Category.PERSONAL_FISH)
        }
    }

    fun encodePrizeId(id: Long): Long = EVENT_PRIZE_ID_OFFSET - id

    fun isEncodedPrizeId(id: Long): Boolean = id <= EVENT_PRIZE_ID_OFFSET

    private fun decodePrizeId(encoded: Long): Long = EVENT_PRIZE_ID_OFFSET - encoded

    private fun distributeCategory(event: SpecialEvent, category: Category) = transaction {
        val alreadyRun = SpecialEventRewardRuns.select {
            (SpecialEventRewardRuns.eventId eq event.id) and
                (SpecialEventRewardRuns.category eq category.id)
        }.any()
        if (alreadyRun) return@transaction

        val prizes = when (category) {
            Category.TOTAL_WEIGHT -> decodePrizes(event.weightPrizesJson).take(event.weightPrizePlaces)
            Category.TOTAL_COUNT -> decodePrizes(event.countPrizesJson).take(event.countPrizePlaces)
            Category.PERSONAL_FISH -> decodePrizes(event.fishPrizesJson).take(event.fishPrizePlaces)
        }
        if (prizes.isNotEmpty()) {
            when (category) {
                Category.TOTAL_WEIGHT,
                Category.TOTAL_COUNT -> {
                    val ranked = clubLeaderboardTx(event.id, category, prizes.size, prizes).top
                    ranked.forEach { entry ->
                        val prize = prizes.getOrNull(entry.rank - 1) ?: return@forEach
                        val (packageId, qty) = prizePackageAndQty(prize) ?: return@forEach
                        ClubMembers
                            .slice(ClubMembers.userId)
                            .select { ClubMembers.clubId eq entry.clubId }
                            .forEach { member ->
                                insertPrizeTx(
                                    userId = member[ClubMembers.userId].value,
                                    eventId = event.id,
                                    category = category,
                                    rank = entry.rank,
                                    packageId = packageId,
                                    qty = qty,
                                )
                            }
                    }
                }
                Category.PERSONAL_FISH -> {
                    personalLeaderboardTx(event.id, prizes.size, prizes).top.forEach { entry ->
                        val prize = prizes.getOrNull(entry.rank - 1) ?: return@forEach
                        val (packageId, qty) = prizePackageAndQty(prize) ?: return@forEach
                        insertPrizeTx(
                            userId = entry.userId,
                            eventId = event.id,
                            category = category,
                            rank = entry.rank,
                            packageId = packageId,
                            qty = qty,
                        )
                    }
                }
            }
        }
        SpecialEventRewardRuns.insert {
            it[eventId] = event.id
            it[SpecialEventRewardRuns.category] = category.id
            it[createdAt] = Instant.now()
        }
    }

    private fun validateEventInput(
        nameRu: String,
        nameEn: String,
        start: Instant,
        end: Instant,
        castZone: CastZoneDTO?,
        fish: List<SpecialEventFishSpec>,
    ) {
        if (nameRu.isBlank() || nameEn.isBlank()) throw SpecialEventException("name_required")
        if (!end.isAfter(start)) throw SpecialEventException("invalid_dates")
        try {
            CastZoneCodec.validateNullable(castZone)
        } catch (_: IllegalArgumentException) {
            throw SpecialEventException("invalid_cast_area")
        }
        if (fish.isEmpty() || fish.any { it.fishId <= 0L || it.weight <= 0.0 }) {
            throw SpecialEventException("invalid_fish")
        }
    }

    private fun ensureNoOverlap(start: Instant, end: Instant, excludeId: Long?) {
        var condition: Op<Boolean> = (SpecialEvents.startTime less end) and (SpecialEvents.endTime greater start)
        if (excludeId != null) condition = condition and (SpecialEvents.id neq excludeId)
        if (SpecialEvents.select { condition }.any()) throw SpecialEventException("event_overlap")
    }

    private fun replaceFishTx(eventId: Long, fish: List<SpecialEventFishSpec>) {
        SpecialEventFish.deleteWhere { SpecialEventFish.eventId eq eventId }
        fish.forEach { spec ->
            val exists = Fish.select { Fish.id eq spec.fishId }.any()
            if (!exists) throw SpecialEventException("invalid_fish")
            SpecialEventFish.insert {
                it[SpecialEventFish.eventId] = eventId
                it[fishId] = spec.fishId
                it[weight] = spec.weight
            }
        }
    }

    private fun getEventTx(id: Long): SpecialEvent? =
        SpecialEvents.select { SpecialEvents.id eq id }.singleOrNull()?.toSpecialEvent()

    private fun currentEventTx(now: Instant = Instant.now()): SpecialEvent? =
        SpecialEvents.select { (SpecialEvents.startTime lessEq now) and (SpecialEvents.endTime greaterEq now) }
            .orderBy(SpecialEvents.startTime to SortOrder.DESC, SpecialEvents.id to SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.toSpecialEvent()

    private fun eventLocationIdTx(eventId: Long): Long? =
        Locations
            .slice(Locations.id)
            .select { Locations.specialEventId eq eventId }
            .singleOrNull()
            ?.get(Locations.id)
            ?.value

    private fun eventLocationCastZoneTx(eventId: Long): CastZoneDTO? =
        Locations
            .slice(Locations.castZoneJson)
            .select { Locations.specialEventId eq eventId }
            .singleOrNull()
            ?.get(Locations.castZoneJson)
            ?.let(CastZoneCodec::decode)

    private fun addClubProgressTx(eventId: Long, clubId: Long, weightDelta: Double, countDelta: Int, now: Instant) {
        val existing = SpecialEventClubProgress.select {
            (SpecialEventClubProgress.eventId eq eventId) and
                (SpecialEventClubProgress.clubId eq clubId)
        }.singleOrNull()
        if (existing == null) {
            SpecialEventClubProgress.insert {
                it[SpecialEventClubProgress.eventId] = eventId
                it[SpecialEventClubProgress.clubId] = clubId
                it[totalWeight] = weightDelta.coerceAtLeast(0.0)
                it[totalCount] = countDelta.coerceAtLeast(0)
                it[updatedAt] = now
            }
        } else {
            val nextWeight = (existing[SpecialEventClubProgress.totalWeight] + weightDelta).coerceAtLeast(0.0)
            val nextCount = (existing[SpecialEventClubProgress.totalCount] + countDelta).coerceAtLeast(0)
            SpecialEventClubProgress.update({
                (SpecialEventClubProgress.eventId eq eventId) and
                    (SpecialEventClubProgress.clubId eq clubId)
            }) {
                it[totalWeight] = nextWeight
                it[totalCount] = nextCount
                it[updatedAt] = now
            }
        }
    }

    private data class ClubLeaderboardResult(
        val top: List<SpecialEventClubEntry>,
        val all: List<SpecialEventClubEntry>,
    )

    private fun clubLeaderboardTx(
        eventId: Long,
        category: Category,
        limit: Int,
        prizes: List<PrizeSpec>,
    ): ClubLeaderboardResult {
        val rows = (SpecialEventClubProgress innerJoin Clubs)
            .select { SpecialEventClubProgress.eventId eq eventId }
            .mapNotNull { row ->
                val value = when (category) {
                    Category.TOTAL_WEIGHT -> row[SpecialEventClubProgress.totalWeight]
                    Category.TOTAL_COUNT -> row[SpecialEventClubProgress.totalCount].toDouble()
                    Category.PERSONAL_FISH -> return@mapNotNull null
                }
                if (value <= 0.0) return@mapNotNull null
                row[SpecialEventClubProgress.clubId].value to (row[Clubs.name] to value)
            }
            .sortedWith(compareByDescending<Pair<Long, Pair<String, Double>>> { it.second.second }.thenBy { it.first })
            .mapIndexed { index, (clubId, data) ->
                SpecialEventClubEntry(
                    clubId = clubId,
                    club = data.first,
                    value = data.second,
                    rank = index + 1,
                    prize = prizes.getOrNull(index),
                )
            }
        return ClubLeaderboardResult(rows.take(limit), rows)
    }

    private data class PersonalLeaderboardResult(
        val top: List<SpecialEventPersonalEntry>,
        val all: List<SpecialEventPersonalEntry>,
    )

    private fun personalLeaderboardTx(
        eventId: Long,
        limit: Int,
        prizes: List<PrizeSpec>,
    ): PersonalLeaderboardResult {
        data class Row(
            val userId: Long,
            val user: String?,
            val catchId: Long?,
            val fishId: Long,
            val fish: String,
            val rarity: String,
            val rarityRank: Int,
            val weight: Double,
            val at: Instant,
        )
        val bestRows = ((SpecialEventCatches leftJoin Users) innerJoin Fish)
            .select { SpecialEventCatches.eventId eq eventId }
            .map { row ->
                Row(
                    userId = row[SpecialEventCatches.userId].value,
                    user = rowUser(row),
                    catchId = row[SpecialEventCatches.catchId]?.value,
                    fishId = row[SpecialEventCatches.fishId].value,
                    fish = row[Fish.name],
                    rarity = row[SpecialEventCatches.rarity],
                    rarityRank = row[SpecialEventCatches.rarityRank],
                    weight = row[SpecialEventCatches.weight],
                    at = row[SpecialEventCatches.createdAt],
                )
            }
            .groupBy { it.userId }
            .values
            .mapNotNull { catches ->
                catches.maxWithOrNull(
                    compareBy<Row> { it.rarityRank }
                        .thenBy { it.weight }
                        .thenBy { it.at }
                )
            }
            .sortedWith(
                compareByDescending<Row> { it.rarityRank }
                    .thenByDescending { it.weight }
                    .thenBy { it.at }
                    .thenBy { it.userId }
            )
            .mapIndexed { index, row ->
                SpecialEventPersonalEntry(
                    userId = row.userId,
                    user = row.user,
                    value = row.weight,
                    rank = index + 1,
                    catchId = row.catchId,
                    fish = row.fish,
                    fishId = row.fishId,
                    rarity = row.rarity,
                    weight = row.weight,
                    at = row.at,
                    prize = prizes.getOrNull(index),
                )
            }
        return PersonalLeaderboardResult(bestRows.take(limit), bestRows)
    }

    private fun rowUser(row: ResultRow): String? {
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

    private fun decodePrizes(raw: String): List<PrizeSpec> =
        try { Json.decodeFromString(raw) } catch (_: Exception) { emptyList() }

    private fun prizePackageAndQty(prize: PrizeSpec): Pair<String, Int>? {
        val packageId = when {
            prize.pack.isNotBlank() -> prize.pack
            prize.coins != null && prize.coins > 0 -> COIN_PRIZE_ID
            else -> null
        } ?: return null
        val qty = if (packageId == COIN_PRIZE_ID) (prize.coins ?: 0) else prize.qty
        return if (qty > 0) packageId to qty else null
    }

    private fun insertPrizeTx(
        userId: Long,
        eventId: Long,
        category: Category,
        rank: Int,
        packageId: String,
        qty: Int,
    ) {
        SpecialEventPrizes.insert {
            it[SpecialEventPrizes.userId] = userId
            it[SpecialEventPrizes.eventId] = eventId
            it[SpecialEventPrizes.category] = category.id
            it[SpecialEventPrizes.rank] = rank
            it[SpecialEventPrizes.packageId] = packageId
            it[SpecialEventPrizes.qty] = qty
            it[claimed] = false
        }
    }

    private fun rarityRank(rarity: String): Int = when (rarity) {
        "common" -> 1
        "uncommon" -> 2
        "rare" -> 3
        "epic" -> 4
        "mythic" -> 5
        "legendary" -> 6
        else -> 0
    }
}

data class FishingEventLocation(
    val locationId: Long,
    val eventId: Long,
    val name: String,
    val imagePath: String?,
    val castZone: CastZoneDTO?,
    val unlocked: Boolean,
    val lockedReason: String?,
)
