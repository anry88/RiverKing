package stub

import kotlinx.serialization.json.*
import java.nio.file.Files
import kotlin.test.*

class BotProfilesTest {
    @Test
    fun alternatesAllTwentyFiveRussianAndEnglishAccountsWithoutChangingIdentitiesOrTokens() {
        val file = Files.createTempFile("bot-profile-order-", ".json")
        try {
            Files.writeString(file, JsonArray((1..50).map { i -> buildJsonObject {
                put("slot", "v1-${i.toString().padStart(2, '0')}"); put("userId", i)
                put("nickname", "old-$i"); put("language", if (i <= 25) "ru" else "en")
                put("accessToken", "access-$i"); put("refreshToken", "refresh-$i")
            } }).toString())
            val store = SessionStore(file)
            assertEquals((1..25).flatMap { listOf(it.toLong(), (it + 25).toLong()) }, store.accounts().map { it.userId })
            assertEquals(List(25) { listOf("ru", "en") }.flatten(), store.accounts().map { it.language })
            store.updateNickname(26, BotNicknames.forSlot("v1-26"))
            val reloaded = SessionStore(file)
            assertEquals("mikey.exe", reloaded.get(26).nickname)
            assertEquals("access-26", reloaded.get(26).accessToken)
            assertEquals("refresh-26", reloaded.get(26).refreshToken)
            assertEquals(store.accounts(), reloaded.accounts())
        } finally { Files.deleteIfExists(file) }
    }
}
