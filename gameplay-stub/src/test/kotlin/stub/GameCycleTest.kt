package stub

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

class GameCycleTest {
    private class Api(val event: Boolean = false, val stock: Boolean = true, val balance: Int = 1000) : GameApi {
        val calls = mutableListOf<String>()
        var purchased = false
        val lure get() = """{"id":7,"displayName":"Worm","qty":${if (stock || purchased) 3 else 0}}"""
        override suspend fun get(path: String): JsonElement {
            calls += "GET $path"
            val body = when (path) {
                "/api/club" -> "null"
                "/api/club/search" -> """[{"id":2}]"""
                "/api/prizes" -> """[{"id":10}]"""
                "/api/referrals/rewards" -> "[]"
                "/api/achievements" -> """[{"code":"first","claimable":true}]"""
                "/api/quests" -> "{}"
                "/api/me" -> """{"dailyAvailable":true,"coins":$balance,"locationId":1,"lures":[$lure],"locations":[{"id":9,"isEvent":true,"unlocked":$event},{"id":1,"unlocked":true},{"id":2,"unlocked":false}]}"""
                "/api/guide" -> """{"fish":[{"locationIds":[1],"lures":["Worm"]}]}"""
                "/api/shop" -> """[{"packs":[{"id":"bait","coinPrice":360,"items":[{"name":"Worm","qty":20}]},{"id":"stars-only","coinPrice":null,"items":[{"name":"Worm","qty":50}]}]}]"""
                else -> error("Unexpected GET $path")
            }
            return Json.parseToJsonElement(body)
        }
        override suspend fun post(path: String, body: JsonObject?): JsonElement {
            calls += "POST $path"
            return when (path) {
                "/api/bot/cast" -> Json.parseToJsonElement("""{"caught":true}""")
                "/api/shop/bait/coins" -> { purchased = true; Json.parseToJsonElement("""{"lures":[$lure]}""") }
                else -> JsonNull
            }
        }
    }
    @Test
    fun cycleUsesNormalServerEndpointsAndOneAutomaticCast() = runBlocking {
        val api = Api()
        assertEquals(GameCycle.Result.CAUGHT, GameCycle(api).run())
        assertTrue(api.calls.indexOf("POST /api/club/2/join") < api.calls.indexOf("POST /api/prizes/10/claim"))
        assertTrue(api.calls.contains("POST /api/daily"))
        assertTrue(api.calls.contains("POST /api/achievements/first/claim"))
        assertTrue(api.calls.contains("POST /api/location/1"))
        assertEquals("POST /api/bot/cast", api.calls.last())
        assertFalse(api.calls.any { it.contains("/api/hook") || it == "POST /api/cast" })
    }
    @Test
    fun wrapsToEventAndBuysBaitOnlyWhenAffordable() = runBlocking {
        val affordable = Api(event = true, stock = false)
        assertEquals(GameCycle.Result.CAUGHT, GameCycle(affordable).run())
        assertTrue(affordable.calls.contains("POST /api/location/9"))
        assertTrue(affordable.calls.contains("POST /api/shop/bait/coins"))
        val poor = Api(stock = false, balance = 10)
        assertEquals(GameCycle.Result.NO_BAIT, GameCycle(poor).run())
        assertFalse(poor.calls.any { it.startsWith("POST /api/shop") || it == "POST /api/bot/cast" })
    }
}
