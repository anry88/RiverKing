package stub

import kotlinx.serialization.json.*

class GameCycle(private val api: GameApi) {
    enum class Result { CAUGHT, ESCAPED, BUSY, NO_BAIT }

    suspend fun run(): Result = try {
        joinClubIfNeeded()
        claimRewards()
        var profile = api.get("/api/me").jsonObject
        val locations = profile.items("locations")
        val index = locations.indexOfFirst { it.long("id") == profile.long("locationId") }
        val first = locations.first { it.bool("unlocked") }
        val target = locations.getOrNull(index + 1)?.takeIf { index >= 0 && it.bool("unlocked") } ?: first
        val location = try {
            api.post("/api/location/${target.long("id")}")
            target
        } catch (e: ApiError) {
            if (e.code !in listOf("event_inactive", "event_requires_club", "locked")) throw e
            profile = api.get("/api/me").jsonObject
            val fallback = profile.items("locations").first { it.bool("unlocked") && !it.bool("isEvent") }
            api.post("/api/location/${fallback.long("id")}")
            fallback
        }
        if (!prepareBait(profile, location)) Result.NO_BAIT
        else if (api.post("/api/bot/cast").jsonObject.bool("caught")) Result.CAUGHT else Result.ESCAPED
    } catch (e: ApiError) {
        if (e.code == "casting") Result.BUSY else throw e
    }

    private suspend fun joinClubIfNeeded() {
        if (api.get("/api/club") != JsonNull) return
        for (club in api.get("/api/club/search").jsonArray) {
            try {
                api.post("/api/club/${club.jsonObject.long("id")}/join")
                return
            } catch (e: ApiError) {
                if (e.code == "already_in_club") return
                if (e.status !in listOf(404, 409)) throw e
            }
        }
    }

    private suspend fun claimRewards() {
        for (prize in api.get("/api/prizes").jsonArray) api.post("/api/prizes/${prize.jsonObject.long("id")}/claim")
        if (api.get("/api/referrals/rewards").jsonArray.isNotEmpty()) api.post("/api/referrals/rewards/claim")
        if (api.get("/api/me").jsonObject.bool("dailyAvailable")) api.post("/api/daily")
        api.get("/api/quests")
        for (achievement in api.get("/api/achievements").jsonArray.map { it.jsonObject }.filter { it.bool("claimable") }) {
            api.post("/api/achievements/${achievement.string("code")}/claim")
        }
    }

    private suspend fun prepareBait(profile: JsonObject, location: JsonObject): Boolean {
        val allowed = if (location.bool("isEvent")) null else {
            val guide = api.get("/api/guide").jsonObject
            guide.items("fish").filter { fish -> fish["locationIds"]!!.jsonArray.any { it.jsonPrimitive.long == location.long("id") } }
                .flatMap { it["lures"]!!.jsonArray.map { name -> name.jsonPrimitive.content } }.toSet()
        }
        fun compatible(lure: JsonObject) = allowed == null ||
            (lure["displayName"] ?: lure["name"])?.jsonPrimitive?.content in allowed
        var owned = profile.items("lures").firstOrNull { it.long("qty") > 0 && compatible(it) }
        if (owned == null) {
            val pack = api.get("/api/shop").jsonArray.flatMap { it.jsonObject.items("packs") }
                .filter { it["coinPrice"] != null && it["coinPrice"] != JsonNull && it.long("coinPrice") <= profile.long("coins") }
                .filter { it.items("items").any { item -> item.long("qty") > 0 && (allowed == null || item.string("name") in allowed) } }
                .minWithOrNull(compareBy<JsonObject> { it.long("coinPrice") }.thenBy { it.string("id") }) ?: return false
            val purchase = api.post("/api/shop/${pack.string("id")}/coins").jsonObject
            owned = purchase.items("lures").firstOrNull { it.long("qty") > 0 && compatible(it) } ?: return false
        }
        api.post("/api/lure/${owned.long("id")}")
        return true
    }
}
