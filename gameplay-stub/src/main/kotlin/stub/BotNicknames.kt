package stub

import kotlinx.serialization.json.*

/** Stable per-slot nicknames; existing accounts are updated through the normal profile API. */
object BotNicknames {
    val russian = listOf(
        "🎣 Лёха", "поплавОК", "Не клюёт 😅", "рыбный_батя", "Санёк.exe",
        "Тихий омут", "Вовчик 🦆", "ещё один заброс", "Жека без паники", "карась_онлайн",
        "Чай остыл ☕", "Костян", "Рыба где?", "шуршу камышом", "Ромыч 🌿",
        "Макс на чиле", "дед_на_связи", "Ловлю вайб", "Денчик 🧢", "Ну давай уже",
        "Стёпа 2.0", "Гоша_афк", "Боря на зорьке", "клёвый тип 🐟", "Василич, приём",
    )
    val english = listOf(
        "mikey.exe", "Reel Bad Luck", "🦆 QuackAttack", "404 Fish Not Found", "sleepyDan",
        "just vibin 💤", "Big Pete", "fish pls", "Lil Kev 🎣", "Captain Maybe",
        "wet_socks", "OneMoreCast™", "snack_break 🍪", "NoBitesClub", "NateWasHere",
        "oops all weeds 🌿", "Rodzilla", "probably a perch", "Old Man Rob", "ChillGill 🐟",
        "BennyAFK", "bobber.exe", "LuckyCap 🧢", "gone fishin'", "NotTodayFish",
    )
    private val all = russian + english

    fun forSlot(slot: String): String = all[slot.removePrefix("v1-").toInt() - 1]

    suspend fun synchronize(server: GameServer, store: SessionStore) {
        for (account in store.accounts()) {
            val desired = forSlot(account.slot)
            if (account.nickname == desired) continue
            val response = server.forAccount(account.userId).post("/api/nickname",
                buildJsonObject { put("nickname", desired) }).jsonObject
            check(response.string("nickname") == desired) { "Server rejected bot nickname for ${account.slot}" }
            store.updateNickname(account.userId, desired)
        }
    }
}
