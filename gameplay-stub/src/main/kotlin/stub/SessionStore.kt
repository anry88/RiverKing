package stub

import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.attribute.PosixFilePermissions

data class AccountSession(val slot: String, val userId: Long, val nickname: String, val language: String,
    val accessToken: String, val refreshToken: String)

class SessionStore(private val file: Path) {
    private val sessions = Json.parseToJsonElement(Files.readString(file).removePrefix("\uFEFF")).jsonArray.map { item ->
        val obj = item.jsonObject
        AccountSession(obj.string("slot"), obj.long("userId"), obj.string("nickname"), obj.string("language"),
            obj.string("accessToken"), obj.string("refreshToken"))
    }.toMutableList()
    init {
        require(sessions.size == 50 && sessions.map { it.slot }.distinct().size == 50 &&
            sessions.map { it.userId }.distinct().size == 50) { "Expected 50 unique account sessions" }
        require(sessions.count { it.language == "ru" } == 25 && sessions.count { it.language == "en" } == 25)
    }
    // Alternate languages so both halves participate from the start of each round.
    fun accounts(): List<AccountSession> = sessions.filter { it.language == "ru" }
        .zip(sessions.filter { it.language == "en" }) { russian, english -> listOf(russian, english) }.flatten()
    fun get(userId: Long): AccountSession = sessions.single { it.userId == userId }
    fun update(userId: Long, tokens: JsonObject) {
        val index = sessions.indexOfFirst { it.userId == userId }
        sessions[index] = sessions[index].copy(accessToken = tokens.string("accessToken"), refreshToken = tokens.string("refreshToken"))
        persist()
    }
    fun updateNickname(userId: Long, nickname: String) {
        val index = sessions.indexOfFirst { it.userId == userId }
        sessions[index] = sessions[index].copy(nickname = nickname)
        persist()
    }
    private fun persist() {
        val json = JsonArray(sessions.map { s -> buildJsonObject {
            put("slot", s.slot); put("userId", s.userId); put("nickname", s.nickname); put("language", s.language)
            put("accessToken", s.accessToken); put("refreshToken", s.refreshToken)
        } })
        val tmp = file.resolveSibling(file.fileName.toString() + ".tmp")
        Files.writeString(tmp, json.toString())
        runCatching { Files.setPosixFilePermissions(tmp, PosixFilePermissions.fromString("rw-------")) }
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content
fun JsonObject.long(key: String): Long = getValue(key).jsonPrimitive.long
fun JsonObject.bool(key: String): Boolean = get(key)?.jsonPrimitive?.booleanOrNull ?: false
fun JsonObject.items(key: String): List<JsonObject> = get(key)?.jsonArray?.map { it.jsonObject } ?: emptyList()
