package stub

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

class BaitSwitchingTest {
    private class WaterApi(var location: Int, var selected: Int,
        var fresh: Int = 3, var salt: Int = 3, val coins: Int = 1000) : GameApi {
        val posts = mutableListOf<String>()
        override suspend fun get(path: String): JsonElement = Json.parseToJsonElement(when (path) {
            "/api/club", "/api/quests" -> "{}"
            "/api/prizes", "/api/referrals/rewards", "/api/achievements" -> "[]"
            "/api/me" -> """{"locationId":$location,"currentLureId":$selected,"coins":$coins,"locations":[{"id":1,"unlocked":true},{"id":2,"unlocked":true},{"id":3,"unlocked":true}],"lures":[{"id":10,"displayName":"Worm","water":"fresh","qty":$fresh},{"id":20,"displayName":"Squid","water":"salt","qty":$salt}]}"""
            "/api/guide" -> """{"fish":[{"locationIds":[1,2],"lures":["Worm"]},{"locationIds":[3],"lures":["Squid"]}]}"""
            "/api/shop" -> """[{"packs":[{"id":"salt","coinPrice":100,"items":[{"name":"Squid","qty":20}]},{"id":"fresh","coinPrice":360,"items":[{"name":"Worm","qty":20}]}]}]"""
            else -> error("Unexpected GET $path")
        })
        override suspend fun post(path: String, body: JsonObject?): JsonElement {
            posts += path
            return when {
                path.startsWith("/api/location/") -> { location = path.substringAfterLast('/').toInt(); JsonNull }
                path.startsWith("/api/lure/") -> { selected = path.substringAfterLast('/').toInt(); JsonNull }
                path == "/api/shop/fresh/coins" -> { fresh += 20; get("/api/me") }
                path == "/api/shop/salt/coins" -> { salt += 20; get("/api/me") }
                path == "/api/bot/cast" -> {
                    assertEquals(if (location == 3) 20 else 10, selected, "Bait must match the new location before casting")
                    Json.parseToJsonElement("""{"caught":true}""")
                }
                else -> error("Unexpected POST $path")
            }
        }
    }

    @Test
    fun switchesFreshToSaltAndSaltToFreshBeforeCasting() = runBlocking {
        for ((api, location, lure) in listOf(Triple(WaterApi(2, 10), 3, 20), Triple(WaterApi(3, 20), 1, 10))) {
            assertEquals(GameCycle.Result.CAUGHT, GameCycle(api).run())
            assertEquals(listOf("/api/location/$location", "/api/lure/$lure", "/api/bot/cast"), api.posts)
        }
    }

    @Test
    fun keepsCurrentBaitWhenTheNewLocationIsCompatible() = runBlocking {
        val api = WaterApi(1, 10)
        assertEquals(GameCycle.Result.CAUGHT, GameCycle(api).run())
        assertEquals(listOf("/api/location/2", "/api/bot/cast"), api.posts)
    }

    @Test
    fun buysOnlyMatchingBaitEvenWhenOtherWaterBaitIsInStockAndCheaper() = runBlocking {
        for ((api, pack, lure) in listOf(Triple(WaterApi(3, 20, fresh = 0), "fresh", 10),
            Triple(WaterApi(2, 10, salt = 0), "salt", 20))) {
            assertEquals(GameCycle.Result.CAUGHT, GameCycle(api).run())
            assertEquals(listOf("/api/location/${api.location}", "/api/shop/$pack/coins", "/api/lure/$lure", "/api/bot/cast"), api.posts)
        }
    }

    @Test
    fun skipsCastWhenOnlyWrongWaterBaitAndInsufficientCoinsRemain() = runBlocking {
        val api = WaterApi(3, 20, fresh = 0, coins = 10)
        assertEquals(GameCycle.Result.NO_BAIT, GameCycle(api).run())
        assertEquals(listOf("/api/location/1"), api.posts)
    }

    @Test
    fun randomlySelectsAmongAllCompatibleInStockLures() = runBlocking {
        class MultiBaitApi : GameApi {
            var selected = 10
            var castLure: Int? = null
            override suspend fun get(path: String): JsonElement = Json.parseToJsonElement(when (path) {
                "/api/club", "/api/quests" -> "{}"
                "/api/prizes", "/api/referrals/rewards", "/api/achievements" -> "[]"
                "/api/me" -> """{"locationId":1,"currentLureId":$selected,"coins":1000,"locations":[{"id":1,"unlocked":true}],"lures":[{"id":10,"displayName":"Worm","water":"fresh","qty":5},{"id":11,"displayName":"Fly","water":"fresh","qty":2},{"id":12,"displayName":"Worm+","water":"fresh","qty":1},{"id":13,"displayName":"Bread","water":"fresh","qty":0},{"id":20,"displayName":"Squid","water":"salt","qty":10}]}"""
                "/api/guide" -> """{"fish":[{"locationIds":[1],"lures":["Worm","Fly","Worm+","Bread"]}]}"""
                else -> error("Unexpected GET $path")
            })
            override suspend fun post(path: String, body: JsonObject?): JsonElement = when {
                path.startsWith("/api/location/") -> JsonNull
                path.startsWith("/api/lure/") -> { selected = path.substringAfterLast('/').toInt(); JsonNull }
                path == "/api/bot/cast" -> { castLure = selected; Json.parseToJsonElement("""{"caught":true}""") }
                else -> error("Unexpected POST $path")
            }
        }
        val api = MultiBaitApi()
        var offeredCandidates: List<Long>? = null
        val chosenLureId = 12L
        val cycle = GameCycle(api, chooseLure = { candidates ->
            offeredCandidates = candidates.map { it.long("id") }
            candidates.first { it.long("id") == chosenLureId }
        })
        assertEquals(GameCycle.Result.CAUGHT, cycle.run())
        assertEquals(listOf(10L, 11L, 12L), offeredCandidates, "Must offer all compatible lures with qty > 0 (excluding out-of-stock and incompatible)")
        assertEquals(12, api.castLure, "Selected lure must be equipped and used for casting")
    }

    @Test
    fun eventLocationOffersAllInStockLuresRandomly() = runBlocking {
        class EventApi : GameApi {
            var selected = 10
            var castLure: Int? = null
            override suspend fun get(path: String): JsonElement = Json.parseToJsonElement(when (path) {
                "/api/club", "/api/quests" -> "{}"
                "/api/prizes", "/api/referrals/rewards", "/api/achievements" -> "[]"
                "/api/me" -> """{"locationId":99,"currentLureId":$selected,"coins":1000,"locations":[{"id":99,"isEvent":true,"unlocked":true}],"lures":[{"id":10,"displayName":"Worm","water":"fresh","qty":5},{"id":20,"displayName":"Squid","water":"salt","qty":3},{"id":21,"displayName":"Squid+","water":"salt","qty":1},{"id":13,"displayName":"Bread","water":"fresh","qty":0}]}"""
                "/api/guide" -> """{"fish":[]}"""
                else -> error("Unexpected GET $path")
            })
            override suspend fun post(path: String, body: JsonObject?): JsonElement = when {
                path.startsWith("/api/location/") -> JsonNull
                path.startsWith("/api/lure/") -> { selected = path.substringAfterLast('/').toInt(); JsonNull }
                path == "/api/bot/cast" -> { castLure = selected; Json.parseToJsonElement("""{"caught":true}""") }
                else -> error("Unexpected POST $path")
            }
        }
        val api = EventApi()
        var offeredCandidates: List<Long>? = null
        val cycle = GameCycle(api, chooseLure = { candidates ->
            offeredCandidates = candidates.map { it.long("id") }
            candidates.first { it.long("id") == 21L }
        })
        assertEquals(GameCycle.Result.CAUGHT, cycle.run())
        assertEquals(listOf(10L, 20L, 21L), offeredCandidates, "Event locations allow fresh, salt, and boosted lures as long as they are in stock")
        assertEquals(21, api.castLure)
    }
}
