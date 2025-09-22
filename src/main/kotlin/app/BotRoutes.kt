package app

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import service.FishingService
import service.PayService
import service.StarsPaymentService
import service.ReferralService
import service.TournamentService
import service.UserPrize
import service.PrizeSpec
import service.I18n
import util.Metrics
import util.sanitizeName
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random
import org.slf4j.LoggerFactory
import db.Users
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.slice
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

internal fun parseInvoicePayload(payload: String, userId: Long): String? {
    return if ('=' in payload) {
        val parts = payload.split(';').mapNotNull {
            val p = it.split('=', limit = 2)
            if (p.size == 2) p[0] to p[1] else null
        }.toMap()
        val packId = parts["pack"]
        val payloadUser = parts["user"]?.toLongOrNull()
        if (packId != null && payloadUser == userId) packId else null
    } else if (payload.startsWith("pack_")) {
        payload.removePrefix("pack_")
    } else {
        null
    }
}

private data class AdminDraft(
    var id: Long? = null,
    var step: AdminStep = AdminStep.NAME_RU,
    var nameRu: String = "",
    var nameEn: String = "",
    var start: Instant? = null,
    var end: Instant? = null,
    var fish: String? = null,
    var location: String? = null,
    var metric: String = "",
    var prizePlaces: Int = 0,
    var prizes: MutableList<PrizeSpec> = mutableListOf(),
    var currentPrize: Int = 1,
)

private enum class AdminStep { NAME_RU, NAME_EN, START, END, FISH, LOCATION, METRIC, PRIZE_PLACES, PRIZE }

private data class DiscountDraft(
    var step: DiscountStep = DiscountStep.PACK,
    var packageId: String = "",
    var packageName: String = "",
    var basePrice: Int = 0,
    var price: Int? = null,
    var start: LocalDate? = null,
    var end: LocalDate? = null,
)

private enum class DiscountStep { PACK, PRICE, START, END }

private data class BroadcastDraft(
    var step: BroadcastStep = BroadcastStep.TEXT_RU,
    var textRu: String = "",
    var textEn: String = "",
)

private enum class BroadcastStep { TEXT_RU, TEXT_EN }

private val METRIC_OPTIONS = listOf("largest", "smallest", "count")
private const val METRIC_KEYBOARD = """{"keyboard":[["largest","smallest"],["count"]],"one_time_keyboard":true,"resize_keyboard":true}"""
private const val REMOVE_KEYBOARD = """{"remove_keyboard":true}"""
private const val TELEGRAM_MESSAGE_LENGTH_LIMIT = 4000

private val broadcastScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
private val castScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

private fun parsePrizes(str: String): MutableList<PrizeSpec> {
    return try {
        Json.parseToJsonElement(str).jsonArray.map { el ->
            val obj = el.jsonObject
            PrizeSpec(
                obj["pack"]?.jsonPrimitive?.content ?: "",
                obj["qty"]?.jsonPrimitive?.int ?: 1
            )
        }.toMutableList()
    } catch (_: Exception) {
        str.split(Regex("""[,\n]+"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { s ->
                val parts = s.split(Regex("""[:\s]+""")).filter { it.isNotBlank() }
                val pack = parts.getOrNull(0) ?: s
                val qty = parts.getOrNull(1)?.toIntOrNull() ?: 1
                PrizeSpec(pack, qty)
            }
            .toMutableList()
    }
}

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun Application.botRoutes(env: Env) {
    val bot = TelegramBot(env.botToken)
    val fishing = FishingService()
    val stars = StarsPaymentService(env, fishing)
    val tournaments = TournamentService()
    val adminStates = mutableMapOf<Long, AdminDraft>()
    val discountStates = mutableMapOf<Long, DiscountDraft>()
    val broadcastStates = mutableMapOf<Long, BroadcastDraft>()
    val log = LoggerFactory.getLogger("Bot")
    routing {
        post("/bot") {
            val secret = call.request.headers["X-Telegram-Bot-Api-Secret-Token"]
            if (secret == null || secret != env.telegramWebhookSecret) {
                return@post call.respond(HttpStatusCode.Forbidden)
            }
            val update = try { call.receive<TgUpdate>() } catch (_: Exception) {
                return@post call.respond(HttpStatusCode.OK)
            }

            update.inlineQuery?.let { iq ->
                val from = iq.from
                val uid = fishing.ensureUserByTgId(
                    tgId = from.id,
                    firstName = from.first_name,
                    lastName = from.last_name,
                    username = from.username,
                    language = from.language_code
                )
                val lang = fishing.userLanguage(uid)
                data class InlineCommandInfo(
                    val name: String,
                    val ruDescription: String,
                    val enDescription: String,
                    val assetName: String,
                    val messageText: (lang: String, uid: Long) -> String
                )

                val inlineCommands = listOf(
                    InlineCommandInfo(
                        name = "start",
                        ruDescription = "Стартовать бота и получить список команд",
                        enDescription = "Start the bot and get the command list",
                        assetName = "start.png"
                    ) { _, _ -> "/start" },
                    InlineCommandInfo(
                        name = "startapp",
                        ruDescription = "Открыть игру",
                        enDescription = "Open the game",
                        assetName = "startapp.png"
                    ) { _, _ -> "/startapp" },
                    InlineCommandInfo(
                        name = "cast",
                        ruDescription = "Забросить снасть",
                        enDescription = "Cast your line",
                        assetName = "cast.png"
                    ) { _, _ -> "/cast" },
                    InlineCommandInfo(
                        name = "bait",
                        ruDescription = "Сменить приманку",
                        enDescription = "Change your bait",
                        assetName = "bait.png"
                    ) { _, _ -> "/bait" },
                    InlineCommandInfo(
                        name = "rod",
                        ruDescription = "Сменить удочку",
                        enDescription = "Change your rod",
                        assetName = "rod.png"
                    ) { _, _ -> "/rod" },
                    InlineCommandInfo(
                        name = "location",
                        ruDescription = "Сменить локацию",
                        enDescription = "Change your location",
                        assetName = "location.png"
                    ) { _, _ -> "/location" },
                    InlineCommandInfo(
                        name = "daily",
                        ruDescription = "Получить ежедневную награду",
                        enDescription = "Claim your daily reward",
                        assetName = "daily.png"
                    ) { _, _ -> "/daily" },
                    InlineCommandInfo(
                        name = "prizes",
                        ruDescription = "Забрать призы турнира",
                        enDescription = "Claim tournament prizes",
                        assetName = "prizes.png"
                    ) { _, _ -> "/prizes" },
                    InlineCommandInfo(
                        name = "shop",
                        ruDescription = "Купить приманки за звёзды",
                        enDescription = "Buy baits with Stars",
                        assetName = "shop.png"
                    ) { _, _ -> "/shop" },
                    InlineCommandInfo(
                        name = "tournament",
                        ruDescription = "Таблица текущего турнира и твоя позиция",
                        enDescription = "View the current tournament leaderboard and your rank",
                        assetName = "tournament.png"
                    ) { _, _ -> "/tournament" },
                    InlineCommandInfo(
                        name = "stats",
                        ruDescription = "Статистика по пойманной рыбе",
                        enDescription = "Your fishing stats",
                        assetName = "stats.png"
                    ) { _, _ -> "/stats" },
                    InlineCommandInfo(
                        name = "language",
                        ruDescription = "Выбрать язык",
                        enDescription = "Choose your language",
                        assetName = "language.png"
                    ) { currentLang, currentUid ->
                        val nickname = fishing.displayName(currentUid)
                        val fallback = if (currentLang == "ru") "не задан" else "not set"
                        val label = if (currentLang == "ru") {
                            "Текущий ник: ${nickname ?: fallback}"
                        } else {
                            "Current nickname: ${nickname ?: fallback}"
                        }
                        "/language\n$label"
                    },
                    InlineCommandInfo(
                        name = "nickname",
                        ruDescription = "Сменить ник",
                        enDescription = "Change your nickname",
                        assetName = "nickname.png"
                    ) { _, _ -> "/nickname" }
                )

                val query = iq.query.trim()
                val normalized = query.lowercase().removePrefix("/")
                val assetsBaseUrl = env.publicBaseUrl.trimEnd('/') + "/app/assets/inline_commands"
                val matched = inlineCommands.filter { normalized.isEmpty() || it.name.startsWith(normalized) }
                    .ifEmpty { inlineCommands }
                val results = matched.map { info ->
                    val messageText = info.messageText(lang, uid)
                    InlineQueryResultArticle(
                        id = info.name,
                        title = info.name,
                        description = if (lang == "ru") info.ruDescription else info.enDescription,
                        inputMessageContent = InputTextMessageContent(messageText),
                        thumbUrl = "$assetsBaseUrl/${info.assetName}"
                    )
                }

                try {
                    bot.answerInlineQuery(iq.id, results)
                } catch (e: Exception) {
                    log.error("answerInlineQuery failed id={}", iq.id, e)
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            update.preCheckoutQuery?.let { q ->
                try {
                    bot.answerPreCheckoutQuery(q.id)
                } catch (e: Exception) {
                    log.error("answerPreCheckoutQuery failed id={}", q.id, e)
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            fun logCommandMetric(name: String, params: Map<String, String> = emptyMap(), source: String) {
                Metrics.counter("bot_command_total", mapOf("command" to name, "source" to source) + params)
            }

            fun ensureUserId(user: TgUser?): Long? {
                return user?.let {
                    fishing.ensureUserByTgId(
                        tgId = it.id,
                        firstName = it.first_name,
                        lastName = it.last_name,
                        username = it.username,
                        language = it.language_code
                    )
                }
            }

            fun splitMessage(text: String, limit: Int = TELEGRAM_MESSAGE_LENGTH_LIMIT): List<String> {
                if (text.length <= limit) return listOf(text)
                val chunks = mutableListOf<String>()
                var index = 0
                while (index < text.length) {
                    var end = (index + limit).coerceAtMost(text.length)
                    if (end < text.length) {
                        val newlineIndex = text.lastIndexOf('\n', end - 1)
                        if (newlineIndex >= index) {
                            end = newlineIndex + 1
                        }
                    }
                    chunks += text.substring(index, end)
                    index = end
                }
                return chunks
            }

            fun strikethrough(text: String): String {
                if (text.isBlank()) return text
                val combining = '\u0336'
                val builder = StringBuilder(text.length * 2)
                text.forEach { ch ->
                    builder.append(ch).append(combining)
                }
                return builder.toString()
            }

            fun trySend(
                chatId: Long,
                text: String,
                replyMarkup: String? = null,
                replyToMessageId: Long? = null,
            ) {
                val parts = splitMessage(text)
                val lastIndex = parts.lastIndex
                parts.forEachIndexed { idx, part ->
                    val markup = if (idx == lastIndex) replyMarkup else null
                    val replyTo = if (idx == 0) replyToMessageId else null
                    try {
                        bot.sendMessage(chatId, part, markup, replyTo)
                    } catch (e: Exception) {
                        log.error("sendMessage failed chatId={} part={}/{}", chatId, idx + 1, parts.size, e)
                    }
                }
            }

            fun ownedData(ownerUid: Long, payload: Any): String = "$ownerUid:${payload.toString()}"

            fun sendLanguageMenu(
                uid: Long,
                chatId: Long,
                lang: String,
                prefix: String? = null,
                replyToMessageId: Long? = null,
            ) {
                val ruLabel = if (lang == "ru") "Русский ✅" else "Русский"
                val enLabel = if (lang == "en") "English ✅" else "English"
                val body = if (lang == "ru") {
                    "Текущий язык: Русский\nВыберите язык:"
                } else {
                    "Current language: English\nChoose a language:"
                }
                val text = buildString {
                    if (!prefix.isNullOrBlank()) {
                        append(prefix)
                        append("\n\n")
                    }
                    append(body)
                }
                val markup = Json.encodeToString(
                    InlineKeyboardMarkup(
                        listOf(
                            listOf(InlineKeyboardButton(ruLabel, "/language ${ownedData(uid, "ru")}")),
                            listOf(InlineKeyboardButton(enLabel, "/language ${ownedData(uid, "en")}")),
                        )
                    )
                )
                trySend(chatId, text, markup, replyToMessageId)
            }

            fun rodBonusLabel(water: String?, predator: Boolean?, lang: String): String {
                return when {
                    water == null -> if (lang == "ru") "Бонусов нет." else "No bonus."
                    water == "fresh" && predator == true -> if (lang == "ru") {
                        "−50% шанс побега пресноводных хищных рыб."
                    } else {
                        "50% less escape chance for freshwater predator fish."
                    }
                    water == "fresh" && (predator == false) -> if (lang == "ru") {
                        "−50% шанс побега пресноводных мирных рыб."
                    } else {
                        "50% less escape chance for freshwater peaceful fish."
                    }
                    water == "salt" && predator == true -> if (lang == "ru") {
                        "−50% шанс побега морских хищных рыб."
                    } else {
                        "50% less escape chance for saltwater predator fish."
                    }
                    water == "salt" && (predator == false) -> if (lang == "ru") {
                        "−50% шанс побега морских мирных рыб."
                    } else {
                        "50% less escape chance for saltwater peaceful fish."
                    }
                    else -> if (lang == "ru") "Бонусов нет." else "No bonus."
                }
            }

            fun sendBaitMenu(
                uid: Long,
                chatId: Long,
                lang: String,
                prefix: String? = null,
                replyToMessageId: Long? = null,
            ) {
                val lures = fishing.listLures(uid).filter { it.qty > 0 }
                val currentId = transaction {
                    Users.select { Users.id eq uid }.single()[Users.currentLureId]?.value
                }
                val sorted = lures.sortedBy { I18n.lure(it.name, lang) }
                val currentName = sorted.find { it.id == currentId }?.let { I18n.lure(it.name, lang) }
                val header = if (lang == "ru") {
                    "Текущая приманка: ${currentName ?: "не выбрана"}"
                } else {
                    "Current bait: ${currentName ?: "not selected"}"
                }
                val prompt = if (sorted.isEmpty()) {
                    if (lang == "ru") "У вас нет приманок" else "You don't have any baits"
                } else {
                    if (lang == "ru") "Выберите приманку:" else "Choose a bait:"
                }
                val details = sorted.joinToString("\n") { lure ->
                    val name = I18n.lure(lure.name, lang)
                    val desc = I18n.lureDescription(lure.name, lang)
                    val qtyLabel = if (lang == "ru") "${lure.qty} шт." else "${lure.qty} pcs."
                    if (desc.isBlank()) {
                        "• $name — $qtyLabel"
                    } else {
                        "• $name — $desc ($qtyLabel)"
                    }
                }
                val text = buildString {
                    if (!prefix.isNullOrBlank()) {
                        append(prefix)
                        append("\n\n")
                    }
                    append(header)
                    append("\n")
                    append(prompt)
                    if (details.isNotBlank()) {
                        append("\n")
                        append(details)
                    }
                }
                if (sorted.isEmpty()) {
                    trySend(chatId, text, replyToMessageId = replyToMessageId)
                    return
                }
                val buttons = sorted.map { lure ->
                    val title = buildString {
                        append(I18n.lure(lure.name, lang))
                        append(" (")
                        append(lure.qty)
                        append(")")
                        if (lure.id == currentId) append(" ✅")
                    }
                        InlineKeyboardButton(title, "/bait ${ownedData(uid, lure.id)}")
                }.chunked(2)
                val markup = Json.encodeToString(InlineKeyboardMarkup(buttons))
                trySend(chatId, text, markup, replyToMessageId)
            }

            fun sendDiscountMenu(chatId: Long) {
                val discounts = fishing.listDiscounts().sortedBy { it.startDate }
                val body = if (discounts.isEmpty()) {
                    "Скидок сейчас нет."
                } else {
                    val lines = discounts.joinToString("\n") { d ->
                        val pack = fishing.findPack(d.packageId)
                        val name = pack?.let { I18n.text(it.name, "ru") } ?: d.packageId
                        val basePrice = pack?.price
                        val priceText = basePrice?.let { strikethrough("$it⭐") + " ${d.price}⭐" } ?: "${d.price}⭐"
                        val start = d.startDate.format(DATE_FMT)
                        val end = d.endDate.format(DATE_FMT)
                        "• $name — $priceText ($start — $end)"
                    }
                    "Текущие скидки:\n$lines"
                }
                val buttons = mutableListOf<List<InlineKeyboardButton>>()
                buttons += listOf(InlineKeyboardButton("Добавить скидку", "create_discount"))
                discounts.forEach { d ->
                    val pack = fishing.findPack(d.packageId)
                    val name = pack?.let { I18n.text(it.name, "ru") } ?: d.packageId
                    val short = if (name.length > 32) name.take(29) + "…" else name
                    buttons += listOf(InlineKeyboardButton("Убрать: $short", "remove_discount_${d.packageId}"))
                }
                buttons += listOf(InlineKeyboardButton("Закрыть", "discount_cancel"))
                val markup = Json.encodeToString(InlineKeyboardMarkup(buttons))
                try {
                    bot.sendMessage(chatId, body, markup)
                } catch (e: Exception) {
                    log.error("sendMessage failed chatId={}", chatId, e)
                }
            }

            fun sendDiscountPackPicker(chatId: Long) {
                val packs = fishing.listShop("ru").flatMap { it.packs }.sortedBy { it.name }
                if (packs.isEmpty()) {
                    try {
                        bot.sendMessage(chatId, "Магазин пуст, скидку добавить нельзя")
                    } catch (e: Exception) {
                        log.error("sendMessage failed chatId={}", chatId, e)
                    }
                    return
                }
                val buttons = packs.map { pack ->
                    InlineKeyboardButton(pack.name, "discount_pack_${pack.id}")
                }.chunked(2).toMutableList()
                buttons += listOf(InlineKeyboardButton("Отмена", "discount_cancel"))
                val markup = Json.encodeToString(InlineKeyboardMarkup(buttons))
                try {
                    bot.sendMessage(chatId, "Выберите товар для скидки", markup)
                } catch (e: Exception) {
                    log.error("sendMessage failed chatId={}", chatId, e)
                }
            }

            fun sendRodMenu(
                uid: Long,
                chatId: Long,
                lang: String,
                prefix: String? = null,
                replyToMessageId: Long? = null,
            ) {
                val rods = fishing.listRods(uid)
                val currentId = transaction {
                    Users.select { Users.id eq uid }.single()[Users.currentRodId]?.value
                }
                val header = if (lang == "ru") {
                    "Текущая удочка: ${currentId?.let { id -> rods.find { it.id == id }?.let { I18n.rod(it.name, lang) } } ?: "не выбрана"}"
                } else {
                    "Current rod: ${currentId?.let { id -> rods.find { it.id == id }?.let { I18n.rod(it.name, lang) } } ?: "not selected"}"
                }
                val prompt = if (lang == "ru") "Выберите удочку:" else "Choose a rod:"
                val details = rods.joinToString("\n") { rod ->
                    val localizedName = I18n.rod(rod.name, lang)
                    val nameLine = buildString {
                        append("• ")
                        append(localizedName)
                        if (rod.id == currentId) append(" ✅")
                    }
                    val infoLine = if (rod.unlocked) {
                        rodBonusLabel(rod.bonusWater, rod.bonusPredator, lang)
                    } else {
                        val req = if (lang == "ru") "Требуется %.0f кг" else "Requires %.0f kg"
                        req.format(Locale.US, rod.unlockKg)
                    }
                    "$nameLine\n  $infoLine"
                }
                val text = buildString {
                    if (!prefix.isNullOrBlank()) {
                        append(prefix.trim())
                        append("\n\n")
                    }
                    append(header)
                    append("\n")
                    append(prompt)
                    if (details.isNotBlank()) {
                        append("\n\n")
                        append(details)
                    }
                }
                val buttons = rods.filter { it.unlocked }.map { rod ->
                    val localizedName = I18n.rod(rod.name, lang)
                    val label = if (rod.id == currentId) "$localizedName ✅" else localizedName
                    listOf(InlineKeyboardButton(label, "/rod ${ownedData(uid, rod.id)}"))
                }
                val markup = if (buttons.isEmpty()) null else Json.encodeToString(InlineKeyboardMarkup(buttons))
                trySend(chatId, text, markup, replyToMessageId)
            }

            fun sendLocationMenu(
                uid: Long,
                chatId: Long,
                lang: String,
                prefix: String? = null,
                replyToMessageId: Long? = null,
            ) {
                val locations = fishing.locations(uid)
                val unlocked = locations.filter { it.unlocked }
                val stored = transaction {
                    Users.select { Users.id eq uid }.single()[Users.currentLocationId]?.value
                }
                val currentId = stored?.takeIf { id -> unlocked.any { it.id == id } } ?: unlocked.firstOrNull()?.id
                val currentName = currentId?.let { id ->
                    unlocked.find { it.id == id }?.let { I18n.location(it.name, lang) }
                }
                val header = if (lang == "ru") {
                    "Текущая локация: ${currentName ?: "не выбрана"}"
                } else {
                    "Current location: ${currentName ?: "not selected"}"
                }
                val prompt = if (unlocked.isEmpty()) {
                    if (lang == "ru") "Доступных локаций нет" else "No locations available"
                } else {
                    if (lang == "ru") "Выберите локацию:" else "Choose a location:"
                }
                val text = buildString {
                    if (!prefix.isNullOrBlank()) {
                        append(prefix)
                        append("\n\n")
                    }
                    append(header)
                    append("\n")
                    append(prompt)
                }
                if (unlocked.isEmpty()) {
                    trySend(chatId, text, replyToMessageId = replyToMessageId)
                    return
                }
                val buttons = unlocked
                    .sortedBy { I18n.location(it.name, lang) }
                    .map { loc ->
                        val label = buildString {
                            append(I18n.location(loc.name, lang))
                            if (loc.id == currentId) append(" ✅")
                        }
                        InlineKeyboardButton(label, "/location ${ownedData(uid, loc.id)}")
                    }
                    .chunked(2)
                val markup = Json.encodeToString(InlineKeyboardMarkup(buttons))
                trySend(chatId, text, markup, replyToMessageId)
            }

            fun sendPrizes(
                uid: Long,
                chatId: Long,
                lang: String,
                prizes: List<UserPrize>? = null,
                prefix: String? = null,
                replyToMessageId: Long? = null,
            ) {
                val actual = prizes ?: run {
                    tournaments.distributePrizes()
                    tournaments.pendingPrizes(uid)
                }
                val packNames = fishing.listShop(lang).flatMap { it.packs }.associate { it.id to it.name }
                fun displayName(prize: UserPrize): String {
                    val name = packNames[prize.packageId]
                    if (name != null) return name
                    val lureName = I18n.lure(prize.packageId, lang)
                    if (lureName != prize.packageId) return lureName
                    return prize.packageId.replace('_', ' ')
                }
                val body = if (actual.isEmpty()) {
                    if (lang == "ru") {
                        "Нет призов для получения."
                    } else {
                        "No prizes to claim."
                    }
                } else {
                    val header = if (lang == "ru") {
                        "Призы, которые можно получить:"
                    } else {
                        "Prizes you can claim:"
                    }
                    val lines = actual.joinToString("\n") { prize ->
                        val name = displayName(prize)
                        val qty = if (prize.qty > 1) " x${prize.qty}" else ""
                        val place = if (prize.rank > 0) {
                            if (lang == "ru") " (место #${prize.rank})" else " (place #${prize.rank})"
                        } else ""
                        "• $name$qty$place"
                    }
                    "$header\n$lines"
                }
                val text = buildString {
                    if (!prefix.isNullOrBlank()) {
                        append(prefix.trim())
                        if (body.isNotBlank()) append("\n\n")
                    }
                    append(body)
                }
                val markup = if (actual.isEmpty()) {
                    null
                } else {
                    val buttons = actual.map { prize ->
                        val name = displayName(prize)
                        val short = if (name.length > 32) name.take(29) + "…" else name
                        val qty = if (prize.qty > 1) " x${prize.qty}" else ""
                        listOf(InlineKeyboardButton("🎁 $short$qty", "/prizeclaim ${ownedData(uid, prize.id)}"))
                    }
                    Json.encodeToString(InlineKeyboardMarkup(buttons))
                }
                trySend(chatId, text, markup, replyToMessageId)
            }

            fun sendShopMenu(
                uid: Long,
                chatId: Long,
                lang: String,
                replyToMessageId: Long? = null,
            ) {
                val shop = fishing.listShop(lang)
                if (shop.isEmpty()) {
                    val emptyText = if (lang == "ru") {
                        "Магазин пока пуст."
                    } else {
                        "The shop is empty right now."
                    }
                    trySend(chatId, emptyText, replyToMessageId = replyToMessageId)
                    return
                }
                val header = if (lang == "ru") {
                    "Магазин приманок за звёзды. Выберите набор:"
                } else {
                    "Star shop. Choose a bundle:"
                }
                val footer = if (lang == "ru") {
                    "Нажми на кнопку, чтобы получить счёт в звёздах."
                } else {
                    "Tap a button to receive a Stars invoice."
                }
                val text = buildString {
                    append(header)
                    shop.forEach { category ->
                        append("\n\n")
                        append(category.name)
                        category.packs.forEach { pack ->
                            append("\n• ")
                            append(pack.name)
                            append(" — ")
                            val priceText = if (pack.originalPrice != null && pack.originalPrice > pack.price) {
                                val until = pack.discountEnd?.format(DATE_FMT)?.let {
                                    if (lang == "ru") " (до $it)" else " (until $it)"
                                } ?: ""
                                "${strikethrough("${pack.originalPrice}⭐")} ${pack.price}⭐$until"
                            } else {
                                "${pack.price}⭐"
                            }
                            append(priceText)
                            if (pack.desc.isNotBlank()) {
                                append("\n  ")
                                append(pack.desc)
                            }
                        }
                    }
                    append("\n\n")
                    append(footer)
                }.trim()

                val buttons = shop.flatMap { category ->
                    category.packs.map { pack ->
                        val labelPrice = if (pack.originalPrice != null && pack.originalPrice > pack.price) {
                            "${strikethrough("${pack.originalPrice}⭐")} ${pack.price}⭐"
                        } else {
                            "${pack.price}⭐"
                        }
                        val label = "${pack.name} — $labelPrice"
                        InlineKeyboardButton(label, "/buy ${ownedData(uid, pack.id)}")
                    }
                }.chunked(2)

                val markup = Json.encodeToString(InlineKeyboardMarkup(buttons))
                trySend(chatId, text, markup, replyToMessageId)
            }

            suspend fun processUserCommand(
                rawText: String,
                from: TgUser?,
                chatId: Long,
                isCallback: Boolean = false,
                messageId: Long? = null,
                sourceOverride: String? = null,
            ): Boolean {
                if (!rawText.startsWith("/")) return false
                val text = rawText.trim()
                val commandLine = text.lineSequence().firstOrNull()?.trim().orEmpty()
                if (commandLine.isEmpty()) return false
                val parts = commandLine.split(" ", limit = 2)
                val command = parts[0]
                val commandTarget = command.substringAfter('@', "").takeIf { it.isNotEmpty() }
                if (commandTarget != null && !commandTarget.equals(env.botName, ignoreCase = true)) {
                    return false
                }
                val commandName = command.substringBefore('@')
                val arg = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
                val source = sourceOverride ?: if (isCallback) "callback" else "message"
                val replyTo = messageId
                fun ownedArg(raw: String?, uid: Long): Pair<String?, Boolean> {
                    if (!isCallback) return raw to false
                    if (raw == null) return null to false
                    val prefix = "$uid:"
                    return if (raw.startsWith(prefix)) {
                        raw.removePrefix(prefix) to false
                    } else {
                        null to true
                    }
                }
                when (commandName) {
                    "/startapp" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        logCommandMetric("startapp", source = source)
                        val link = "https://t.me/${env.botName}?startapp"
                        val responseText = if (lang == "ru") {
                            "Нажми кнопку, чтобы открыть игру"
                        } else {
                            "Tap the button to open the game"
                        }
                        val buttonText = if (lang == "ru") "Открыть игру" else "Open game"
                        val markup = """{"inline_keyboard":[[{"text":"$buttonText","url":"$link"}]]}"""
                        trySend(chatId, responseText, markup, replyTo)
                        return true
                    }
                    "/start" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val params = arg?.let { mapOf("payload" to it) } ?: emptyMap()
                        logCommandMetric("start", params, source)
                        val message = if (lang == "ru") {
                            """🎣 Привет! Это River King — игра про рыбалку. Играй через приложение или с помощью команд бота. Команды работают и в групповых чатах, если добавить туда бота и дать ему доступ к сообщениям.

Доступные команды:
/cast — забросить снасть
/bait — сменить приманку
/rod — выбрать удочку
/location — сменить локацию
/daily — получить ежедневную награду
/prizes — забрать призы турнира
/shop — купить приманки за звёзды
/tournament — таблица текущего турнира и твоя позиция
/stats — статистика по пойманной рыбе
/language — выбрать язык
/nickname — сменить ник""".trimIndent()
                        } else {
                            """🎣 Welcome to River King, a fishing game you can play in the app or via bot commands. Bot commands also work in group chats if you add the bot and allow it to access messages.

Available commands:
/cast — cast your line
/bait — change your bait
/rod — change your rod
/location — change your location
/daily — claim your daily reward
/prizes — claim tournament prizes
/shop — buy baits with Stars
/tournament — view the current tournament leaderboard and your rank
/stats — your fishing stats
/language — choose your language
/nickname — change your nickname""".trimIndent()
                        }
                        val openButtonText = if (lang == "ru") "Открыть игру" else "Open game"
                        val channelButtonText = if (lang == "ru") "Присоединиться к каналу" else "Join the channel"
                        val gameLink = "https://t.me/${env.botName}?startapp"
                        val channelLink = if (lang == "ru") {
                            "https://t.me/riverking_ru"
                        } else {
                            "https://t.me/riverking_en"
                        }
                        val markup = Json.encodeToString(
                            mapOf(
                                "inline_keyboard" to listOf(
                                    listOf(mapOf("text" to openButtonText, "url" to gameLink)),
                                    listOf(mapOf("text" to channelButtonText, "url" to channelLink)),
                                )
                            )
                        )
                        trySend(chatId, message, markup, replyTo)
                        return true
                    }
                    "/tournament" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val t = tournaments.currentTournament()
                        val reply = if (t != null) {
                            val tName = if (lang == "ru") t.nameRu else t.nameEn
                            val (list, mine) = tournaments.leaderboard(t, uid, t.prizePlaces)
                            val metric = t.metric.lowercase()
                            val showFish = metric != "count"
                            val header = "$tName\n"
                            val body = if (list.isEmpty()) {
                                if (lang == "ru") "Список пуст" else "Leaderboard is empty"
                            } else {
                                list.joinToString("\n") { e ->
                                    val valueText = if (metric == "count") {
                                        e.value.toInt().toString()
                                    } else {
                                        "%.2f".format(Locale.US, e.value)
                                    }
                                    val info = if (showFish) {
                                        val fishName = e.fish?.let { I18n.fish(it, lang) } ?: "-"
                                        "$fishName $valueText"
                                    } else {
                                        valueText
                                    }
                                    val userName = e.user?.let { "\u2066$it\u2069" } ?: "-"
                                    "\u200E${e.rank}. $userName — $info"
                                }
                            }
                            val mineLine = if (mine != null && mine.rank > t.prizePlaces) {
                                val valueText = if (metric == "count") {
                                    mine.value.toInt().toString()
                                } else {
                                    "%.2f".format(Locale.US, mine.value)
                                }
                                val line = if (lang == "ru") {
                                    if (showFish) {
                                        val fishName = mine.fish?.let { I18n.fish(it, lang) } ?: "-"
                                        "Твоя позиция: ${mine.rank}. Рыба: $fishName $valueText"
                                    } else {
                                        "Твоя позиция: ${mine.rank}. Поймано: $valueText"
                                    }
                                } else {
                                    if (showFish) {
                                        val fishName = mine.fish?.let { I18n.fish(it, lang) } ?: "-"
                                        "Your position: ${mine.rank}. Fish: $fishName $valueText"
                                    } else {
                                        "Your position: ${mine.rank}. Caught: $valueText"
                                    }
                                }
                                "\u200E$line"
                            } else null
                            buildString {
                                append(header)
                                append(body)
                                if (mineLine != null) {
                                    append("\n\n")
                                    append(mineLine)
                                }
                            }
                        } else {
                            if (lang == "ru") "Сейчас нет активного турнира" else "No active tournament"
                        }
                        val params = t?.let { mapOf("metric" to it.metric.lowercase()) } ?: emptyMap()
                        logCommandMetric("tournament", params, source)
                        trySend(chatId, reply, replyToMessageId = replyTo)
                        return true
                    }
                    "/prizes" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        tournaments.distributePrizes()
                        val prizes = tournaments.pendingPrizes(uid)
                        logCommandMetric("prizes", mapOf("count" to prizes.size.toString()), source)
                        sendPrizes(uid, chatId, lang, prizes = prizes, replyToMessageId = replyTo)
                        return true
                    }
                    "/daily" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val res = fishing.giveDailyBaits(uid)
                        if (res == null) {
                            val params = mapOf("result" to "not_ready")
                            logCommandMetric("daily", params, source)
                            val reply = if (lang == "ru") {
                                "Новая ежедневная награда пока недоступна"
                            } else {
                                "Your next daily reward is not ready yet"
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                        } else {
                            val streak = res.third
                            val params = mapOf("result" to "claimed", "streak" to streak.toString())
                            logCommandMetric("daily", params, source)
                            val schedule = fishing.dailyRewardSchedule(uid)
                            val displayDay = streak.coerceAtMost(7)
                            val rewards = schedule.getOrNull(displayDay - 1).orEmpty()
                            val lines = rewards.map { reward ->
                                val name = I18n.lure(reward.name, lang)
                                "• $name x${reward.qty}"
                            }
                            val body = if (lang == "ru") {
                                "Вы получили ежедневную награду (день $displayDay):"
                            } else {
                                "Daily reward claimed (day $displayDay):"
                            }
                            val text = buildString {
                                append(body)
                                if (lines.isNotEmpty()) {
                                    append("\n")
                                    append(lines.joinToString("\n"))
                                }
                            }
                            trySend(chatId, text, replyToMessageId = replyTo)
                        }
                        return true
                    }
                    "/stats" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val totalCount = fishing.totalCaughtCount(uid)
                        val totalWeight = fishing.totalCaughtKg(uid)
                        val todayCount = fishing.todayCaughtCount(uid)
                        val todayWeight = fishing.todayCaughtKg(uid)
                        val rarityStats = fishing.catchStatsByRarity(uid)
                        val unit = if (lang == "ru") "кг" else "kg"
                        val countLabel = if (lang == "ru") "шт." else "fish"
                        fun formatWeight(value: Double) = "%.2f".format(Locale.US, value) + " $unit"
                        val breakdown = if (rarityStats.isEmpty()) {
                            ""
                        } else {
                            val title = if (lang == "ru") "\n\nРедкость:" else "\n\nBy rarity:"
                            val lines = rarityStats.joinToString("\n") { stat ->
                                val rarity = RARITY_LABELS[lang]?.get(stat.rarity) ?: stat.rarity
                                "• $rarity — ${stat.count} $countLabel, ${formatWeight(stat.weight)}"
                            }
                            title + "\n" + lines
                        }
                        val reply = if (lang == "ru") {
                            """🐟 Всего поймано: $totalCount $countLabel (${formatWeight(totalWeight)})
📅 За сегодня: $todayCount $countLabel (${formatWeight(todayWeight)})$breakdown""".trimIndent()
                        } else {
                            """🐟 Total caught: $totalCount $countLabel (${formatWeight(totalWeight)})
📅 Today: $todayCount $countLabel (${formatWeight(todayWeight)})$breakdown""".trimIndent()
                        }
                        logCommandMetric("stats", mapOf("result" to "shown"), source)
                        trySend(chatId, reply, replyToMessageId = replyTo)
                        return true
                    }
                    "/shop" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        logCommandMetric("shop", mapOf("action" to "show"), source)
                        sendShopMenu(uid, chatId, lang, replyToMessageId = replyTo)
                        return true
                    }
                    "/buy" -> {
                        val buyerTgId = from?.id ?: return false
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val (value, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        if (value.isNullOrBlank()) {
                            logCommandMetric("buy", mapOf("result" to "missing_arg"), source)
                            val reply = if (lang == "ru") {
                                "Укажи набор или воспользуйся командой /shop."
                            } else {
                                "Specify a bundle id or use /shop."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        val packId = value ?: return true
                        val invoiceChatId = if (chatId < 0) buyerTgId else chatId
                        try {
                            stars.sendPackageInvoice(invoiceChatId, buyerTgId, packId, lang)
                            logCommandMetric("buy", mapOf("result" to "sent", "pack" to packId), source)
                            if (invoiceChatId != chatId) {
                                val info = if (lang == "ru") {
                                    "Счёт отправлен в личные сообщения."
                                } else {
                                    "The invoice was sent to your private chat."
                                }
                                trySend(chatId, info, replyToMessageId = replyTo)
                            }
                        } catch (e: StarsPaymentService.MissingPrivateChatAccessException) {
                            log.warn("sendInvoice missing private chat access chatId={} pack={}", chatId, packId)
                            logCommandMetric(
                                "buy",
                                mapOf("result" to "dm_required", "pack" to packId),
                                source,
                            )
                            if (invoiceChatId != chatId) {
                                val link = "https://t.me/${env.botName}?start=shop"
                                val info = if (lang == "ru") {
                                    "Бот не может отправить счёт в личные сообщения. Разреши боту писать, нажав Старт: $link"
                                } else {
                                    "I couldn't send the invoice to your private chat. Allow the bot to message you by pressing Start: $link"
                                }
                                trySend(chatId, info, replyToMessageId = replyTo)
                            } else {
                                val reply = if (lang == "ru") {
                                    "Не удалось отправить счёт. Попробуй ещё раз позже."
                                } else {
                                    "Failed to send the invoice. Please try again later."
                                }
                                trySend(chatId, reply, replyToMessageId = replyTo)
                            }
                        } catch (e: Exception) {
                            log.error("sendInvoice failed chatId={} pack={}", chatId, packId, e)
                            logCommandMetric("buy", mapOf("result" to "error", "pack" to packId), source)
                            val reply = if (invoiceChatId != chatId) {
                                val link = "https://t.me/${env.botName}?start=start"
                                if (lang == "ru") {
                                    "Не удалось отправить счёт. Возможно, бот не может писать тебе личные сообщения — разреши ему писать, перейдя по ссылке: $link. Попробуй ещё раз позже."
                                } else {
                                    "Failed to send the invoice. The bot might not be allowed to message you privately — allow it by opening: $link. Please try again later."
                                }
                            } else {
                                if (lang == "ru") {
                                    "Не удалось отправить счёт. Попробуй ещё раз позже."
                                } else {
                                    "Failed to send the invoice. Please try again later."
                                }
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                        }
                        return true
                    }
                    "/cast" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val knownFish = fishing.caughtFishIds(uid)
                        try {
                            fishing.startCast(uid)
                        } catch (e: Exception) {
                            val reason = e.message ?: "error"
                            val text = when (reason) {
                                "casting" -> if (lang == "ru") "Заброс уже выполняется." else "You are already casting."
                                "No lure selected" -> if (lang == "ru") "Сначала выберите приманку." else "Select a bait first."
                                "No baits" -> if (lang == "ru") "Приманки закончились." else "You have no baits left."
                                "No suitable fish" -> if (lang == "ru") {
                                    "На выбранной локации нет подходящей рыбы для этой приманки."
                                } else {
                                    "No suitable fish at this location for the selected bait."
                                }
                                else -> if (lang == "ru") "Не удалось забросить снасть." else "Failed to start the cast."
                            }
                            logCommandMetric("cast", mapOf("result" to "error", "stage" to "start", "reason" to reason), source)
                            trySend(chatId, text, replyToMessageId = replyTo)
                            return true
                        }
                        val waitMessage = if (lang == "ru") {
                            "Забросили снасть. Ждём поклёвку..."
                        } else {
                            "The line is in the water. Waiting for a bite..."
                        }
                        trySend(chatId, waitMessage, replyToMessageId = replyTo)
                        castScope.launch {
                            val waitSeconds = 5 + Random.nextInt(26)
                            delay(waitSeconds * 1000L)
                            try {
                                val escapeInfo = fishing.locationEscapeChance(uid)
                                val extraEscapeChance = if (escapeInfo.rodBonusMultiplier < 1.0) 0.15 else 0.30
                                val hookRes = fishing.hook(uid, waitSeconds, 0.0, extraEscapeChance)
                                if (!hookRes.success) {
                                    val escapedText = if (lang == "ru") "Рыба сорвалась!" else "The fish got away!"
                                    trySend(chatId, escapedText, replyToMessageId = replyTo)
                                    logCommandMetric(
                                        "cast",
                                        mapOf("result" to "escaped", "location" to escapeInfo.locationId.toString()),
                                        source,
                                    )
                                    return@launch
                                }
                                val castRes = fishing.cast(uid, waitSeconds, 0.0, true)
                                val catch = castRes.catch
                                if (catch == null) {
                                    val escapedText = if (lang == "ru") "Рыба сорвалась!" else "The fish got away!"
                                    trySend(chatId, escapedText, replyToMessageId = replyTo)
                                    logCommandMetric(
                                        "cast",
                                        mapOf(
                                            "result" to "escaped",
                                            "location" to escapeInfo.locationId.toString(),
                                            "stage" to "final",
                                        ),
                                        source,
                                    )
                                    return@launch
                                }
                                val fishName = I18n.fish(catch.fish, lang)
                                val locationName = I18n.location(catch.location, lang)
                                val isNew = catch.fishId?.let { it !in knownFish } == true
                                val newLine = if (isNew) {
                                    if (lang == "ru") "\n✨ Новая рыба!" else "\n✨ New fish!"
                                } else {
                                    ""
                                }
                                val unlockedLine = if (castRes.unlockedLocations.isNotEmpty()) {
                                    val localized = castRes.unlockedLocations.map { I18n.location(it, lang) }
                                    val prefix = if (lang == "ru") {
                                        if (localized.size > 1) "\n📍 Открыты новые локации: " else "\n📍 Открыта новая локация: "
                                    } else {
                                        if (localized.size > 1) "\n📍 New locations unlocked: " else "\n📍 New location unlocked: "
                                    }
                                    prefix + localized.joinToString(", ")
                                } else {
                                    ""
                                }
                                val rodLine = if (castRes.unlockedRods.isNotEmpty()) {
                                    val localized = castRes.unlockedRods.map { I18n.rod(it, lang) }
                                    val prefix = if (lang == "ru") {
                                        if (localized.size > 1) "\n🎣 Открыты новые удочки: " else "\n🎣 Открыта новая удочка: "
                                    } else {
                                        if (localized.size > 1) "\n🎣 New rods unlocked: " else "\n🎣 New rod unlocked: "
                                    }
                                    prefix + localized.joinToString(", ")
                                } else {
                                    ""
                                }
                                val caption = buildCatchCaption(
                                    lang = lang,
                                    fishName = fishName,
                                    rarity = catch.rarity,
                                    weightKg = catch.weight,
                                    locationName = locationName,
                                    extraLines = listOf(newLine, unlockedLine, rodLine),
                                )
                                val image = generateCatchImage(
                                    catch.fish,
                                    catch.location,
                                    fishName,
                                    catch.weight,
                                    catch.rarity,
                                    lang,
                                )
                                if (image != null) {
                                    try {
                                        bot.sendPhoto(chatId, image, caption, replyToMessageId = replyTo)
                                    } catch (e: Exception) {
                                        log.error("sendPhoto failed chatId={} fish={}", chatId, catch.fish, e)
                                        trySend(chatId, caption, replyToMessageId = replyTo)
                                    }
                                } else {
                                    trySend(chatId, caption, replyToMessageId = replyTo)
                                }
                                logCommandMetric(
                                    "cast",
                                    mapOf(
                                        "result" to "caught",
                                        "rarity" to catch.rarity,
                                        "location" to escapeInfo.locationId.toString(),
                                        "fish" to catch.fish,
                                    ),
                                    source,
                                )
                                Metrics.gauge(
                                    "catch_weight_kg",
                                    catch.weight,
                                    mapOf(
                                        "fish" to catch.fish,
                                        "location" to catch.location,
                                        "rarity" to catch.rarity,
                                    ),
                                )
                            } catch (e: Exception) {
                                log.error("cast command failed uid={} chatId={} source={}", uid, chatId, source, e)
                                fishing.resetCasting(uid)
                                val errorText = if (lang == "ru") "Не удалось завершить заброс." else "Failed to finish the cast."
                                trySend(chatId, errorText, replyToMessageId = replyTo)
                                logCommandMetric(
                                    "cast",
                                    mapOf("result" to "error", "stage" to "final"),
                                    source,
                                )
                            }
                        }
                        return true
                    }
                    "/language" -> {
                        val uid = ensureUserId(from) ?: return false
                        var lang = fishing.userLanguage(uid)
                        val (value, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        if (value == null) {
                            logCommandMetric("language", mapOf("action" to "show"), source)
                            sendLanguageMenu(uid, chatId, lang, replyToMessageId = replyTo)
                        } else {
                            val normalized = value.lowercase()
                            if (normalized in setOf("ru", "en")) {
                                fishing.setLanguage(uid, normalized)
                                lang = normalized
                                val params = mapOf("language" to normalized, "result" to "success")
                                logCommandMetric("language", params, source)
                                val prefix = if (normalized == "ru") {
                                    "Язык переключен на Русский"
                                } else {
                                    "Language switched to English"
                                }
                                sendLanguageMenu(uid, chatId, lang, prefix, replyTo)
                            } else {
                                val params = mapOf("language" to normalized, "result" to "invalid")
                                logCommandMetric("language", params, source)
                                val prefix = if (lang == "ru") {
                                    "Неизвестный язык. Используйте кнопки ниже."
                                } else {
                                    "Unknown language. Please use the buttons below."
                                }
                                sendLanguageMenu(uid, chatId, lang, prefix, replyTo)
                            }
                        }
                        return true
                    }
                    "/bait" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val (value, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        if (value == null) {
                            logCommandMetric("bait", mapOf("action" to "show"), source)
                            sendBaitMenu(uid, chatId, lang, replyToMessageId = replyTo)
                        } else {
                            val lureId = value.toLongOrNull()
                            if (lureId == null) {
                                logCommandMetric("bait", mapOf("result" to "invalid", "value" to value), source)
                                val reply = if (lang == "ru") "Неверный формат приманки" else "Invalid bait value"
                                trySend(chatId, reply, replyToMessageId = replyTo)
                            } else {
                                try {
                                    fishing.setLure(uid, lureId)
                                    logCommandMetric(
                                        "bait",
                                        mapOf("result" to "success", "value" to lureId.toString()),
                                        source
                                    )
                                    val prefix = if (lang == "ru") "Приманка обновлена" else "Bait updated"
                                    sendBaitMenu(uid, chatId, lang, prefix, replyTo)
                                } catch (e: Exception) {
                                    val reason = e.message ?: "error"
                                    logCommandMetric(
                                        "bait",
                                        mapOf("result" to "error", "value" to lureId.toString(), "reason" to reason),
                                        source
                                    )
                                    val msg = when (reason) {
                                        "casting" -> if (lang == "ru") "Нельзя менять приманку во время заброса" else "You can't change bait while casting"
                                        "no lure" -> if (lang == "ru") "Эта приманка недоступна" else "This bait is not available"
                                        else -> if (lang == "ru") "Не удалось сменить приманку" else "Failed to change bait"
                                    }
                                    trySend(chatId, msg, replyToMessageId = replyTo)
                                }
                            }
                        }
                        return true
                    }
                    "/rod" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val (value, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        if (value == null) {
                            logCommandMetric("rod", mapOf("action" to "show"), source)
                            sendRodMenu(uid, chatId, lang, replyToMessageId = replyTo)
                        } else {
                            val rodId = value.toLongOrNull()
                            if (rodId == null) {
                                logCommandMetric("rod", mapOf("result" to "invalid", "value" to value), source)
                                val reply = if (lang == "ru") "Неверное значение удочки" else "Invalid rod value"
                                trySend(chatId, reply, replyToMessageId = replyTo)
                            } else {
                                try {
                                    fishing.setRod(uid, rodId)
                                    logCommandMetric(
                                        "rod",
                                        mapOf("result" to "success", "value" to rodId.toString()),
                                        source,
                                    )
                                    val prefix = if (lang == "ru") "Удочка обновлена" else "Rod updated"
                                    sendRodMenu(uid, chatId, lang, prefix, replyTo)
                                } catch (e: Exception) {
                                    val reason = e.message ?: "error"
                                    logCommandMetric(
                                        "rod",
                                        mapOf("result" to "error", "value" to rodId.toString(), "reason" to reason),
                                        source,
                                    )
                                    val msg = when (reason) {
                                        "casting" -> if (lang == "ru") "Нельзя менять удочку во время заброса" else "You can't change the rod while casting"
                                        "locked" -> if (lang == "ru") "Эта удочка еще недоступна" else "This rod is locked"
                                        "no rod" -> if (lang == "ru") "Эта удочка недоступна" else "You don't have this rod"
                                        else -> if (lang == "ru") "Не удалось сменить удочку" else "Failed to change rod"
                                    }
                                    trySend(chatId, msg, replyToMessageId = replyTo)
                                }
                            }
                        }
                        return true
                    }
                    "/location" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val (value, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        if (value == null) {
                            logCommandMetric("location", mapOf("action" to "show"), source)
                            sendLocationMenu(uid, chatId, lang, replyToMessageId = replyTo)
                        } else {
                            val locId = value.toLongOrNull()
                            if (locId == null) {
                                logCommandMetric("location", mapOf("result" to "invalid", "value" to value), source)
                                val reply = if (lang == "ru") "Неверный формат локации" else "Invalid location value"
                                trySend(chatId, reply, replyToMessageId = replyTo)
                            } else {
                                try {
                                    fishing.setLocation(uid, locId)
                                    logCommandMetric(
                                        "location",
                                        mapOf("result" to "success", "value" to locId.toString()),
                                        source
                                    )
                                    val prefix = if (lang == "ru") "Локация обновлена" else "Location updated"
                                    sendLocationMenu(uid, chatId, lang, prefix, replyTo)
                                } catch (e: Exception) {
                                    val reason = e.message ?: "error"
                                    logCommandMetric(
                                        "location",
                                        mapOf("result" to "error", "value" to locId.toString(), "reason" to reason),
                                        source
                                    )
                                    val msg = when (reason) {
                                        "casting" -> if (lang == "ru") "Нельзя менять локацию во время заброса" else "You can't change location while casting"
                                        "locked" -> if (lang == "ru") "Локация еще недоступна" else "Location is still locked"
                                        "bad location" -> if (lang == "ru") "Локация не найдена" else "Location not found"
                                        else -> if (lang == "ru") "Не удалось сменить локацию" else "Failed to change location"
                                    }
                                    trySend(chatId, msg, replyToMessageId = replyTo)
                                }
                            }
                        }
                        return true
                    }
                    "/nickname" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        if (arg.isNullOrBlank()) {
                            logCommandMetric("nickname", mapOf("result" to "missing_arg"), source)
                            val currentLabel = fishing.displayName(uid)
                                ?.takeIf { it.isNotBlank() }
                                ?: if (lang == "ru") "не установлен" else "not set"
                            val reply = if (lang == "ru") {
                                "Текущий ник: \"$currentLabel\".\nУкажи новый ник после команды, например: /nickname Рыбак"
                            } else {
                                "Current nickname: \"$currentLabel\".\nProvide a new nickname after the command, e.g. /nickname Fisher"
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        val sanitized = sanitizeName(arg).replace('\n', ' ').trim()
                        if (sanitized.isEmpty()) {
                            logCommandMetric("nickname", mapOf("result" to "invalid"), source)
                            val reply = if (lang == "ru") {
                                "Ник не может быть пустым и должен быть короче 30 символов."
                            } else {
                                "Nickname can't be empty and must be under 30 characters."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        val currentNickname = transaction {
                            Users.select { Users.id eq uid }.single()[Users.nickname]
                        }
                        if (currentNickname != null && currentNickname == sanitized) {
                            logCommandMetric("nickname", mapOf("result" to "same"), source)
                            val reply = if (lang == "ru") {
                                "Этот ник уже установлен."
                            } else {
                                "This nickname is already set."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        val currentLabel = fishing.displayName(uid)
                            ?.takeIf { it.isNotBlank() }
                            ?: if (lang == "ru") "не установлен" else "not set"
                        val confirmText = if (lang == "ru") {
                            "Вы хотите изменить имя с \"$currentLabel\" на \"$sanitized\"?"
                        } else {
                            "Do you want to change your name from \"$currentLabel\" to \"$sanitized\"?"
                        }
                        val prompt = if (lang == "ru") {
                            "$confirmText\nПодтвердите действие кнопкой ниже."
                        } else {
                            "$confirmText\nConfirm the action using the buttons below."
                        }
                        val confirmLabel = if (lang == "ru") "✅ Да" else "✅ Yes"
                        val cancelLabel = if (lang == "ru") "❌ Нет" else "❌ No"
                        val markup = Json.encodeToString(
                            InlineKeyboardMarkup(
                                listOf(
                                    listOf(InlineKeyboardButton(confirmLabel, "/nickname_confirm ${ownedData(uid, sanitized)}")),
                                    listOf(InlineKeyboardButton(cancelLabel, "/nickname_cancel ${ownedData(uid, "cancel")}")),
                                )
                            )
                        )
                        logCommandMetric("nickname", mapOf("result" to "prompt"), source)
                        trySend(chatId, prompt, markup, replyTo)
                        return true
                    }
                    "/nickname_confirm" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val (value, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        val newName = value?.let { sanitizeName(it).replace('\n', ' ').trim() } ?: ""
                        if (newName.isEmpty()) {
                            logCommandMetric("nickname", mapOf("result" to "invalid_confirm"), source)
                            val reply = if (lang == "ru") {
                                "Не удалось изменить ник. Попробуй ещё раз."
                            } else {
                                "Couldn't update the nickname. Please try again."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        fishing.setNickname(uid, newName)
                        logCommandMetric("nickname", mapOf("result" to "updated"), source)
                        val updated = fishing.displayName(uid) ?: newName
                        val reply = if (lang == "ru") {
                            "Ник изменён на \"$updated\"."
                        } else {
                            "Nickname changed to \"$updated\"."
                        }
                        trySend(chatId, reply, replyToMessageId = replyTo)
                        return true
                    }
                    "/nickname_cancel" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val (_, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        logCommandMetric("nickname", mapOf("result" to "cancel"), source)
                        val reply = if (lang == "ru") {
                            "Изменение ника отменено."
                        } else {
                            "Nickname change cancelled."
                        }
                        trySend(chatId, reply, replyToMessageId = replyTo)
                        return true
                    }
                    "/prizeclaim" -> {
                        val uid = ensureUserId(from) ?: return false
                        val lang = fishing.userLanguage(uid)
                        val (value, mismatch) = ownedArg(arg, uid)
                        if (mismatch) return true
                        val prizeId = value?.toLongOrNull()
                        if (prizeId == null) {
                            logCommandMetric("prizes_claim", mapOf("result" to "invalid_id"), source)
                            val reply = if (lang == "ru") {
                                "Неверный идентификатор приза."
                            } else {
                                "Invalid prize identifier."
                            }
                            trySend(chatId, reply, replyToMessageId = replyTo)
                            return true
                        }
                        tournaments.distributePrizes()
                        val pending = tournaments.pendingPrizes(uid)
                        val prize = pending.find { it.id == prizeId }
                        if (prize == null) {
                            logCommandMetric("prizes_claim", mapOf("result" to "not_found"), source)
                            val reply = if (lang == "ru") {
                                "Приз не найден или уже получен."
                            } else {
                                "Prize not found or already claimed."
                            }
                            sendPrizes(uid, chatId, lang, prizes = pending, prefix = reply, replyToMessageId = replyTo)
                            return true
                        }
                        val pack = fishing.findPack(prize.packageId)
                        val packNames = fishing.listShop(lang).flatMap { it.packs }.associate { it.id to it.name }
                        val displayName = packNames[prize.packageId]
                            ?: pack?.name?.let { I18n.text(it, lang) }
                            ?: prize.packageId.replace('_', ' ')
                        val success = try {
                            tournaments.claimPrize(uid, prizeId, fishing)
                            logCommandMetric(
                                "prizes_claim",
                                mapOf(
                                    "result" to "claimed",
                                    "pack" to prize.packageId,
                                    "qty" to prize.qty.toString()
                                ),
                                source
                            )
                            val items = pack?.items.orEmpty()
                            val header = if (lang == "ru") {
                                val label = if (prize.qty > 1) "$displayName x${prize.qty}" else displayName
                                "Приз \"$label\" получен."
                            } else {
                                val label = if (prize.qty > 1) "$displayName x${prize.qty}" else displayName
                                "Prize \"$label\" claimed."
                            }
                            if (items.isEmpty()) {
                                header
                            } else {
                                val lines = items.joinToString("\n") { (lure, qty) ->
                                    val lureName = I18n.lure(lure, lang)
                                    val total = qty * prize.qty
                                    "• $lureName x$total"
                                }
                                "$header\n$lines"
                            }
                        } catch (e: Exception) {
                            logCommandMetric(
                                "prizes_claim",
                                mapOf("result" to "error", "pack" to prize.packageId),
                                source
                            )
                            if (lang == "ru") {
                                "Не удалось получить приз. Попробуй ещё раз позже."
                            } else {
                                "Couldn't claim the prize. Please try again later."
                            }
                        }
                        sendPrizes(uid, chatId, lang, prefix = success, replyToMessageId = replyTo)
                        return true
                    }
                }
                return false
            }

            update.callbackQuery?.let { cq ->
                val data = cq.data
                val chatId = cq.message?.chat?.id ?: cq.from.id
                if (data != null && processUserCommand(
                        data,
                        cq.from,
                        chatId,
                        isCallback = true,
                        messageId = cq.message?.message_id,
                    )
                ) {
                    try { bot.answerCallbackQuery(cq.id) } catch (_: Exception) {}
                    return@post call.respond(HttpStatusCode.OK)
                }
                if (cq.from.id == env.adminTgId) {
                    try { bot.answerCallbackQuery(cq.id) } catch (_: Exception) {}
                    val data = cq.data
                    val target = cq.message?.chat?.id ?: cq.from.id
                    when {
                        data == "create_tournament" -> {
                            adminStates[cq.from.id] = AdminDraft()
                            try {
                                bot.sendMessage(target, "Введите название турнира на русском")
                            } catch (e: Exception) {
                                log.error("sendMessage failed chatId={}", target, e)
                            }
                        }
                        data == "discounts_menu" -> {
                            sendDiscountMenu(target)
                        }
                        data == "create_discount" -> {
                            discountStates[cq.from.id] = DiscountDraft(step = DiscountStep.PACK)
                            sendDiscountPackPicker(target)
                        }
                        data == "discount_cancel" -> {
                            discountStates.remove(cq.from.id)
                        }
                        data != null && data.startsWith("discount_pack_") -> {
                            val id = data.removePrefix("discount_pack_")
                            val pack = fishing.findPack(id)
                            if (pack != null) {
                                val name = I18n.text(pack.name, "ru")
                                val existing = fishing.getDiscount(id)
                                val draft = DiscountDraft(
                                    step = DiscountStep.PRICE,
                                    packageId = id,
                                    packageName = name,
                                    basePrice = pack.price,
                                    price = existing?.price,
                                    start = existing?.startDate,
                                    end = existing?.endDate,
                                )
                                discountStates[cq.from.id] = draft
                                val current = existing?.let {
                                    val start = it.startDate.format(DATE_FMT)
                                    val end = it.endDate.format(DATE_FMT)
                                    "\nТекущая скидка: ${it.price}⭐ ($start — $end)"
                                } ?: ""
                                val message = "Введите цену в звёздах для \"$name\" (обычная цена: ${pack.price}⭐)$current"
                                try {
                                    bot.sendMessage(target, message)
                                } catch (e: Exception) {
                                    log.error("sendMessage failed chatId={}", target, e)
                                }
                            }
                        }
                        data != null && data.startsWith("remove_discount_") -> {
                            val id = data.removePrefix("remove_discount_")
                            val removed = fishing.removeDiscount(id) > 0
                            val packName = fishing.findPack(id)?.let { I18n.text(it.name, "ru") } ?: id
                            val reply = if (removed) {
                                "Скидка для \"$packName\" удалена"
                            } else {
                                "Скидка для \"$packName\" не найдена"
                            }
                            discountStates.remove(cq.from.id)
                            try {
                                bot.sendMessage(target, reply)
                            } catch (e: Exception) {
                                log.error("sendMessage failed chatId={}", target, e)
                            }
                            sendDiscountMenu(target)
                        }
                        data == "list_tournaments" -> {
                            val current = tournaments.currentTournament()
                            val upcoming = tournaments.upcomingTournaments()
                            val past = tournaments.pastTournaments(10)
                            val list = buildList {
                                if (current != null) add(current)
                                addAll(upcoming)
                                addAll(past)
                            }
                            if (list.isEmpty()) {
                                try { bot.sendMessage(target, "Турниров нет") } catch (e: Exception) { log.error("sendMessage failed chatId={}", target, e) }
                            } else {
                                val buttons = list.map { t ->
                                    listOf(InlineKeyboardButton(t.nameRu, "tournament_${t.id}"))
                                }
                                val markup = Json.encodeToString(InlineKeyboardMarkup(buttons))
                                try { bot.sendMessage(target, "Турниры", markup) } catch (e: Exception) { log.error("sendMessage failed chatId={}", target, e) }
                            }
                        }
                        data == "broadcast_message" -> {
                            broadcastStates[cq.from.id] = BroadcastDraft()
                            try { bot.sendMessage(target, "Введите текст на русском") } catch (e: Exception) { log.error("sendMessage failed chatId={}", target, e) }
                        }
                        data != null && data.startsWith("tournament_") -> {
                            val id = data.removePrefix("tournament_").toLongOrNull()
                            if (id != null) {
                                val t = tournaments.getTournament(id)
                                if (t != null) {
                                    val markup = """{"inline_keyboard":[[{"text":"Редактировать","callback_data":"edit_tournament_$id"},{"text":"Удалить","callback_data":"delete_tournament_$id"}]]}"""
                                    try { bot.sendMessage(target, "Турнир: ${t.nameRu}", markup) } catch (e: Exception) { log.error("sendMessage failed chatId={}", target, e) }
                                }
                            }
                        }
                        data != null && data.startsWith("delete_tournament_") -> {
                            val id = data.removePrefix("delete_tournament_").toLongOrNull()
                            if (id != null) {
                                try { tournaments.deleteTournament(id); bot.sendMessage(target, "Турнир удален") } catch (e: Exception) { log.error("deleteTournament failed", e) }
                            }
                        }
                        data != null && data.startsWith("edit_tournament_") -> {
                            val id = data.removePrefix("edit_tournament_").toLongOrNull()
                            if (id != null) {
                                val t = tournaments.getTournament(id)
                                if (t != null) {
                                    adminStates[cq.from.id] = AdminDraft(
                                        id = id,
                                        nameRu = t.nameRu,
                                        nameEn = t.nameEn,
                                        start = t.startTime,
                                        end = t.endTime,
                                        fish = t.fish,
                                        location = t.location,
                                        metric = t.metric,
                                        prizePlaces = t.prizePlaces,
                                        prizes = parsePrizes(t.prizesJson),
                                    )
                                    try { bot.sendMessage(target, "Введите название турнира на русском (сейчас: ${t.nameRu})") } catch (e: Exception) { log.error("sendMessage failed chatId={}", target, e) }
                                }
                            }
                        }
                    }
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            val message = update.message ?: return@post call.respond(HttpStatusCode.OK)

            message.successfulPayment?.let { sp ->
                val chatId = message.chat.id
                val userId = message.from?.id
                if (userId == null) {
                    log.warn("successfulPayment with no user id: chatId={} payload={}", chatId, sp.invoicePayload)
                    return@post call.respond(HttpStatusCode.OK)
                }
                val rawPayload = sp.invoicePayload
                val packId = parseInvoicePayload(rawPayload, userId)
                if (packId == null) {
                    val parts = rawPayload.split(';').mapNotNull {
                        val p = it.split('=', limit = 2)
                        if (p.size == 2) p[0] to p[1] else null
                    }.toMap()
                    val payloadUser = parts["user"]?.toLongOrNull()
                    when {
                        parts["pack"] == null ->
                            log.warn("successfulPayment with no packId: chatId={} payload={}", chatId, rawPayload)
                        payloadUser != null && payloadUser != userId ->
                            log.warn(
                                "successfulPayment payload user mismatch: userId={} payloadUser={} payload={}",
                                userId,
                                payloadUser,
                                rawPayload
                            )
                    }
                } else {
                    val uid = fishing.ensureUserByTgId(userId)
                    try {
                        fishing.buyPackage(uid, packId)
                    } catch (e: Exception) {
                        log.error("buyPackage failed uid={} packId={}", uid, packId, e)
                    }
                    try {
                        PayService.recordPayment(
                            uid,
                            packId,
                            PayService.PaymentInfo(
                                providerChargeId = sp.providerPaymentChargeId,
                                telegramChargeId = sp.telegramPaymentChargeId,
                                amount = sp.totalAmount,
                                currency = sp.currency,
                            )
                        )
                    } catch (e: Exception) {
                        log.error("recordPayment failed uid={} packId={}", uid, packId, e)
                    }
                    try {
                        fishing.findPack(packId)?.let { pack ->
                            ReferralService.onPurchase(uid, pack)
                        }
                    } catch (e: Exception) {
                        log.error("referral reward failed uid={} packId={}", uid, packId, e)
                    }
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            val chatId = message.chat.id
            val userId = message.from?.id ?: return@post call.respond(HttpStatusCode.OK)
            val text = message.text ?: ""
            val commandSource = if (message.viaBot != null) "inline" else "message"

            discountStates[userId]?.let { draft ->
                if (text == "/cancel") {
                    discountStates.remove(userId)
                    try {
                        bot.sendMessage(chatId, "Настройка скидки отменена")
                    } catch (e: Exception) {
                        log.error("sendMessage failed chatId={}", chatId, e)
                    }
                    return@post call.respond(HttpStatusCode.OK)
                }
                when (draft.step) {
                    DiscountStep.PACK -> {
                        try {
                            bot.sendMessage(chatId, "Выберите товар кнопкой ниже")
                        } catch (_: Exception) {}
                    }
                    DiscountStep.PRICE -> {
                        val value = text.trim().toIntOrNull()
                        if (value == null || value <= 0) {
                            try {
                                bot.sendMessage(chatId, "Укажите цену числом")
                            } catch (_: Exception) {}
                        } else {
                            draft.price = value
                            draft.step = DiscountStep.START
                            val current = draft.start?.format(DATE_FMT)?.let { " (сейчас: $it)" } ?: ""
                            try {
                                bot.sendMessage(chatId, "Дата начала скидки (дд.мм.гггг)$current")
                            } catch (e: Exception) {
                                log.error("sendMessage failed chatId={}", chatId, e)
                            }
                        }
                    }
                    DiscountStep.START -> {
                        val date = runCatching { LocalDate.parse(text.trim(), DATE_FMT) }.getOrNull()
                        if (date == null) {
                            try {
                                bot.sendMessage(chatId, "Неверный формат даты, используйте дд.мм.гггг")
                            } catch (_: Exception) {}
                        } else {
                            draft.start = date
                            draft.step = DiscountStep.END
                            val current = draft.end?.format(DATE_FMT)?.let { " (сейчас: $it)" } ?: ""
                            try {
                                bot.sendMessage(chatId, "Дата окончания скидки (дд.мм.гггг)$current")
                            } catch (e: Exception) {
                                log.error("sendMessage failed chatId={}", chatId, e)
                            }
                        }
                    }
                    DiscountStep.END -> {
                        val date = runCatching { LocalDate.parse(text.trim(), DATE_FMT) }.getOrNull()
                        val start = draft.start
                        val price = draft.price
                        if (date == null || start == null || price == null) {
                            try {
                                bot.sendMessage(chatId, "Неверный формат даты, используйте дд.мм.гггг")
                            } catch (_: Exception) {}
                        } else if (date.isBefore(start)) {
                            try {
                                bot.sendMessage(chatId, "Дата окончания не может быть раньше даты начала")
                            } catch (_: Exception) {}
                        } else {
                            draft.end = date
                            fishing.setDiscount(draft.packageId, price, start, date)
                            val startStr = start.format(DATE_FMT)
                            val endStr = date.format(DATE_FMT)
                            val messageText = buildString {
                                append("Скидка для \"${draft.packageName}\" сохранена: ")
                                append(strikethrough("${draft.basePrice}⭐"))
                                append(" → ${price}⭐ ($startStr — $endStr)")
                            }
                            discountStates.remove(userId)
                            try {
                                bot.sendMessage(chatId, messageText)
                            } catch (e: Exception) {
                                log.error("sendMessage failed chatId={}", chatId, e)
                            }
                            sendDiscountMenu(chatId)
                        }
                    }
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            adminStates[userId]?.let { draft ->
                when (draft.step) {
                    AdminStep.NAME_RU -> {
                        draft.nameRu = text
                        draft.step = AdminStep.NAME_EN
                        val current = draft.nameEn.takeIf { it.isNotBlank() }?.let { " (сейчас: $it)" } ?: ""
                        try { bot.sendMessage(chatId, "Название турнира на английском$current") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                    }
                    AdminStep.NAME_EN -> {
                        draft.nameEn = text
                        draft.step = AdminStep.START
                        val current = draft.start?.atZone(ZoneOffset.UTC)?.format(DATE_FMT)?.let { " (сейчас: $it)" } ?: ""
                        try { bot.sendMessage(chatId, "Дата начала (дд.мм.гггг)$current") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                    }
                    AdminStep.START -> {
                        val d = runCatching { LocalDate.parse(text, DATE_FMT) }.getOrNull()
                        if (d != null) {
                            draft.start = d.atStartOfDay(ZoneOffset.UTC).toInstant()
                            draft.step = AdminStep.END
                            val current = draft.end?.atZone(ZoneOffset.UTC)?.format(DATE_FMT)?.let { " (сейчас: $it)" } ?: ""
                            try { bot.sendMessage(chatId, "Дата окончания (дд.мм.гггг)$current") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        } else {
                            try { bot.sendMessage(chatId, "Неверный ввод, повторите") } catch (_: Exception) {}
                        }
                    }
                    AdminStep.END -> {
                        val d = runCatching { LocalDate.parse(text, DATE_FMT) }.getOrNull()
                        if (d != null) {
                            draft.end = d.atStartOfDay(ZoneOffset.UTC).toInstant()
                            draft.step = AdminStep.FISH
                            val current = draft.fish?.let { " (сейчас: $it)" } ?: ""
                            try { bot.sendMessage(chatId, "Введите рыбу на русском или группу (common/legendary и т.д.) (или '-' для любой)$current") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        } else {
                            try { bot.sendMessage(chatId, "Неверный ввод, повторите") } catch (_: Exception) {}
                        }
                    }
                    AdminStep.FISH -> {
                        val v = text.trim()
                        draft.fish = v.takeIf { it.isNotEmpty() && it != "-" }
                        draft.step = AdminStep.LOCATION
                        val current = draft.location?.let { " (сейчас: $it)" } ?: ""
                        try { bot.sendMessage(chatId, "Локация на русском (или '-' для любой)$current") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                    }
                    AdminStep.LOCATION -> {
                        val v = text.trim()
                        draft.location = v.takeIf { it.isNotEmpty() && it != "-" }
                        draft.step = AdminStep.METRIC
                        val current = if (draft.metric.isNotBlank()) " (сейчас: ${draft.metric})" else ""
                        try {
                            bot.sendMessage(chatId, "Метрика (largest/smallest/count)$current", METRIC_KEYBOARD)
                        } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                    }
                    AdminStep.METRIC -> {
                        val m = text.lowercase()
                        if (m in METRIC_OPTIONS) {
                            draft.metric = m
                            draft.step = AdminStep.PRIZE_PLACES
                            val current = if (draft.prizePlaces != 0) " (сейчас: ${draft.prizePlaces})" else ""
                            try {
                                bot.sendMessage(chatId, "Количество призовых мест$current", REMOVE_KEYBOARD)
                            } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        } else {
                            try { bot.sendMessage(chatId, "Неверная метрика, выберите largest, smallest или count", METRIC_KEYBOARD) } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        }
                    }
                    AdminStep.PRIZE_PLACES -> {
                        val n = text.toIntOrNull()
                        if (n != null) {
                            draft.prizePlaces = n
                            draft.prizes = MutableList(n) { i -> draft.prizes.getOrNull(i) ?: PrizeSpec("", 1) }
                            draft.currentPrize = 1
                            draft.step = AdminStep.PRIZE
                            val currentPrize = draft.prizes.getOrNull(0)?.takeIf { it.pack.isNotBlank() }?.let { " (сейчас: ${it.pack} x${it.qty})" } ?: ""
                            try { bot.sendMessage(chatId, "Награда за 1 место (pack qty)$currentPrize") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        } else {
                            try { bot.sendMessage(chatId, "Неверный ввод, повторите") } catch (_: Exception) {}
                        }
                    }
                    AdminStep.PRIZE -> {
                        val p = text.trim().split(' ', ':')
                        val pack = p.getOrNull(0)?.takeIf { it.isNotBlank() }
                        val qty = p.getOrNull(1)?.toIntOrNull() ?: 1
                        if (pack == null || qty <= 0) {
                            try { bot.sendMessage(chatId, "Неверный ввод, укажите приз как 'pack qty'") } catch (_: Exception) {}
                            return@post call.respond(HttpStatusCode.OK)
                        }
                        val prize = PrizeSpec(pack, qty)
                        val idx = draft.currentPrize - 1
                        if (draft.prizes.size > idx) {
                            draft.prizes[idx] = prize
                        } else {
                            draft.prizes.add(prize)
                        }
                        if (draft.currentPrize < draft.prizePlaces) {
                            draft.currentPrize += 1
                            val currentPrize = draft.prizes.getOrNull(draft.currentPrize - 1)?.takeIf { it.pack.isNotBlank() }?.let { " (сейчас: ${it.pack} x${it.qty})" } ?: ""
                            try { bot.sendMessage(chatId, "Награда за ${draft.currentPrize} место (pack qty)$currentPrize") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        } else {
                            val start = draft.start
                            val end = draft.end
                            if (start != null && end != null) {
                                val prizesJson = draft.prizes.take(draft.prizePlaces).joinToString(prefix = "[", postfix = "]") { Json.encodeToString(it) }
                                try {
                                    if (draft.id == null) {
                                        tournaments.createTournament(
                                            draft.nameRu,
                                            draft.nameEn,
                                            start,
                                            end,
                                            draft.fish,
                                            draft.location,
                                            draft.metric,
                                            draft.prizePlaces,
        prizesJson,
                                        )
                                        bot.sendMessage(chatId, "Турнир создан")
                                    } else {
                                        tournaments.updateTournament(
                                            draft.id!!,
                                            draft.nameRu,
                                            draft.nameEn,
                                            start,
                                            end,
                                            draft.fish,
                                            draft.location,
                                            draft.metric,
                                            draft.prizePlaces,
                                            prizesJson,
                                        )
                                        bot.sendMessage(chatId, "Турнир обновлен")
                                    }
                                } catch (e: Exception) {
                                    log.error("tournament save failed", e)
                                }
                            }
                            adminStates.remove(userId)
                        }
                    }
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            broadcastStates[userId]?.let { draft ->
                when (draft.step) {
                    BroadcastStep.TEXT_RU -> {
                        draft.textRu = text
                        draft.step = BroadcastStep.TEXT_EN
                        try { bot.sendMessage(chatId, "Введите текст на английском") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                    }
                    BroadcastStep.TEXT_EN -> {
                        draft.textEn = text
                        broadcastStates.remove(userId)
                        try { bot.sendMessage(chatId, "Начинаю рассылку") } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        val users = transaction {
                            Users.slice(Users.tgId, Users.language).selectAll().map { it[Users.tgId] to it[Users.language] }
                        }
                        broadcastScope.launch {
                            users.forEachIndexed { index, (uid, lang) ->
                                val msg = when (lang) {
                                    "ru" -> draft.textRu
                                    "en" -> draft.textEn
                                    else -> draft.textRu + "\n" + draft.textEn
                                }
                                try {
                                    bot.sendMessage(uid, msg)
                                } catch (e: TelegramApiException) {
                                    when (e.code) {
                                        403 -> log.warn("User {} blocked bot; skipping broadcast", uid)
                                        400, 404 -> log.warn("Failed to deliver to {}: {}", uid, e.message)
                                        else -> log.error(
                                            "sendMessage failed chatId={} code={} message={}",
                                            uid,
                                            e.code,
                                            e.message,
                                            e
                                        )
                                    }
                                } catch (e: Exception) {
                                    log.error("sendMessage failed chatId={}", uid, e)
                                }
                                if (index < users.lastIndex) {
                                    delay(30_000L)
                                }
                            }
                        }
                    }
                }
                return@post call.respond(HttpStatusCode.OK)
            }

            if (processUserCommand(
                    text,
                    message.from,
                    chatId,
                    messageId = message.message_id,
                    sourceOverride = commandSource,
                )
            ) {
                return@post call.respond(HttpStatusCode.OK)
            } else if (text.startsWith("/paysupport")) {
                val args = text.removePrefix("/paysupport").trim()
                val uid = fishing.ensureUserByTgId(userId)
                val lang = fishing.userLanguage(uid)
                val payments = PayService.listPayments(uid)
                val packNames = fishing.listShop(lang).flatMap { it.packs }.associate { it.id to it.name }
                val source = commandSource
                if (args.isEmpty()) {
                    if (payments.isEmpty()) {
                        val reply = if (lang == "ru") {
                            "Покупок не найдено"
                        } else {
                            "No purchases found"
                        }
                        logCommandMetric("paysupport", mapOf("result" to "empty"), source)
                        trySend(chatId, reply)
                    } else {
                        val list = payments.joinToString("\n") {
                            val label = packNames[it.packageId] ?: it.packageId
                            "${it.id}: $label — ${it.amount} ${it.currency}"
                        }
                        val reply = if (lang == "ru") {
                            "Выберите покупку для возврата, отправив /paysupport <ID> <причина>:\n$list"
                        } else {
                            "Choose a payment for refund by sending /paysupport <ID> <reason>:\n$list"
                        }
                        logCommandMetric("paysupport", mapOf("action" to "list"), source)
                        trySend(chatId, reply)
                    }
                } else {
                    val parts = args.split(" ", limit = 2)
                    val paymentId = parts.getOrNull(0)?.toLongOrNull()
                    val reason = parts.getOrNull(1)?.trim() ?: ""
                    if (paymentId == null) {
                        val reply = if (lang == "ru") {
                            "Неверный формат. Используйте /paysupport <ID> <причина>."
                        } else {
                            "Invalid format. Use /paysupport <ID> <reason>."
                        }
                        logCommandMetric("paysupport", mapOf("result" to "invalid_args"), source)
                        trySend(chatId, reply)
                    } else {
                        val payment = payments.find { it.id == paymentId }
                        if (payment != null) {
                            val reqId = PayService.createSupportRequest(uid, paymentId, reason)
                            val reply = if (lang == "ru") {
                                "Запрос #$reqId отправлен администрации"
                            } else {
                                "Request #$reqId has been sent to the admins"
                            }
                            logCommandMetric("paysupport", mapOf("result" to "submitted"), source)
                            trySend(chatId, reply)
                            if (env.adminTgId != 0L) {
                                val msg = "Запрос #$reqId от $chatId, платеж $paymentId: $reason\n" +
                                        "/refund $reqId — одобрить возврат\n" +
                                        "/reject $reqId <причина> — отклонить\n" +
                                        "/ask $reqId <вопрос> — запросить информацию"
                                try {
                                    bot.sendMessage(env.adminTgId, msg)
                                } catch (e: Exception) {
                                    log.error("sendMessage failed chatId={}", env.adminTgId, e)
                                }
                            }
                        } else {
                            val reply = if (lang == "ru") {
                                "Покупка не найдена"
                            } else {
                                "Purchase not found"
                            }
                            logCommandMetric("paysupport", mapOf("result" to "not_found"), source)
                            trySend(chatId, reply)
                        }
                    }
                }
            } else if (text.startsWith("/answer")) {
                val parts = text.split(" ", limit = 3)
                val uid = fishing.ensureUserByTgId(userId)
                val lang = fishing.userLanguage(uid)
                val source = commandSource
                var id = parts.getOrNull(1)?.toLongOrNull()
                val answer: String? = if (id != null) {
                    parts.getOrNull(2)
                } else {
                    id = PayService.latestInfoRequest(uid)?.id
                    parts.getOrNull(1)
                }
                if (id == null || answer.isNullOrBlank()) {
                    val reply = if (lang == "ru") {
                        "Укажи запрос и ответ: /answer <ID> <ответ>"
                    } else {
                        "Provide a request and reply: /answer <ID> <message>"
                    }
                    logCommandMetric("answer", mapOf("result" to "invalid_args"), source)
                    trySend(chatId, reply)
                } else {
                    val reqId = id!!
                    val req = PayService.findSupportRequest(reqId)
                    if (req != null && req.userId == uid) {
                        PayService.updateSupportRequest(reqId, "pending", answer)
                        if (env.adminTgId != 0L) {
                            try {
                                bot.sendMessage(
                                    env.adminTgId,
                                    "Ответ по запросу #$reqId от $chatId: $answer\n" +
                                            "/refund $reqId — одобрить возврат\n" +
                                            "/reject $reqId <причина> — отклонить\n" +
                                            "/ask $reqId <вопрос> — запросить информацию"
                                )
                            } catch (e: Exception) {
                                log.error("sendMessage failed chatId={}", env.adminTgId, e)
                            }
                        }
                        val reply = if (lang == "ru") {
                            "Ответ отправлен администрации"
                        } else {
                            "Your reply has been sent to the admins"
                        }
                        logCommandMetric("answer", mapOf("result" to "sent"), source)
                        trySend(chatId, reply)
                    } else {
                        val reply = if (lang == "ru") {
                            "Запрос не найден"
                        } else {
                            "Request not found"
                        }
                        logCommandMetric("answer", mapOf("result" to "not_found"), source)
                        trySend(chatId, reply)
                    }
                }
            } else if (chatId == env.adminTgId) {
                when {
                    text.startsWith("/admin") -> {
                        val markup = Json.encodeToString(
                            InlineKeyboardMarkup(
                                listOf(
                                    listOf(
                                        InlineKeyboardButton("Создать турнир", "create_tournament"),
                                        InlineKeyboardButton("Список турниров", "list_tournaments"),
                                    ),
                                    listOf(
                                        InlineKeyboardButton("Разослать сообщение", "broadcast_message"),
                                        InlineKeyboardButton("Скидки магазина", "discounts_menu"),
                                    ),
                                )
                            )
                        )
                        try { bot.sendMessage(chatId, "Админ меню", markup) } catch (e: Exception) { log.error("sendMessage failed chatId={}", chatId, e) }
                        discountStates.remove(userId)
                    }
                    text.startsWith("/refund") -> {
                        val id = text.split(" ").getOrNull(1)?.toLongOrNull()
                        if (id != null) {
                            val req = PayService.findSupportRequest(id)
                            if (req != null) {
                                val payment = req.paymentId?.let { PayService.findPayment(it) }
                                    ?: PayService.latestPayment(req.userId)
                                val tgId = fishing.userTgId(req.userId)
                                if (payment != null && tgId != null) {
                                    try {
                                        stars.refundStars(tgId, payment.telegramChargeId)
                                        PayService.markPaymentRefunded(payment.id)
                                        fishing.removeAutoFishMonth(req.userId)
                                        PayService.updateSupportRequest(id, "refunded", null)
                                        try {
                                            bot.sendMessage(tgId, "Ваш запрос #$id одобрен, возврат будет выполнен")
                                        } catch (e: Exception) {
                                            log.error("sendMessage failed chatId={}", tgId, e)
                                        }
                                    } catch (e: Exception) {
                                        log.error(
                                            "refund failed userId={} chargeId={}",
                                            req.userId,
                                            payment.telegramChargeId,
                                            e
                                        )
                                    }
                                } else {
                                    log.warn(
                                        "No payment or tgId found for refund userId={} requestId={}",
                                        req.userId,
                                        id
                                    )
                                }
                            }
                        }
                    }
                    text.startsWith("/reject") -> {
                        val parts = text.split(" ", limit = 3)
                        val id = parts.getOrNull(1)?.toLongOrNull()
                        val reason = parts.getOrNull(2) ?: "Причина не указана"
                        if (id != null) {
                            val req = PayService.findSupportRequest(id)
                            if (req != null) {
                                PayService.updateSupportRequest(id, "rejected", reason)
                                val tgId = fishing.userTgId(req.userId)
                                if (tgId != null) {
                                    try {
                                        bot.sendMessage(tgId, "Запрос #$id отклонен: $reason")
                                    } catch (e: Exception) {
                                        log.error("sendMessage failed chatId={}", tgId, e)
                                    }
                                } else {
                                    log.warn("No tgId found for userId={} requestId={}", req.userId, id)
                                }
                            }
                        }
                    }
                    text.startsWith("/ask") -> {
                        val parts = text.split(" ", limit = 3)
                        val id = parts.getOrNull(1)?.toLongOrNull()
                        val question = parts.getOrNull(2) ?: return@post call.respond(HttpStatusCode.OK)
                        if (id != null) {
                            val req = PayService.findSupportRequest(id)
                            if (req != null) {
                                PayService.updateSupportRequest(id, "info", question)
                                val tgId = fishing.userTgId(req.userId)
                                if (tgId != null) {
                                    try {
                                        bot.sendMessage(
                                            tgId,
                                            "Админ уточняет по запросу #$id: $question\n" +
                                                    "Ответьте командой /answer $id <ответ>"
                                        )
                                    } catch (e: Exception) {
                                        log.error("sendMessage failed chatId={}", tgId, e)
                                    }
                                } else {
                                    log.warn("No tgId found for userId={} requestId={}", req.userId, id)
                                }
                            }
                        }
                    }
                }
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}

@Serializable
private data class TgUpdate(
    val message: TgMessage? = null,
    @SerialName("inline_query") val inlineQuery: TgInlineQuery? = null,
    @SerialName("pre_checkout_query") val preCheckoutQuery: TgPreCheckoutQuery? = null,
    @SerialName("callback_query") val callbackQuery: TgCallbackQuery? = null,
)

@Serializable
private data class TgMessage(
    val message_id: Long,
    val chat: TgChat,
    val from: TgUser? = null,
    val text: String? = null,
    @SerialName("successful_payment") val successfulPayment: TgSuccessfulPayment? = null,
    @SerialName("via_bot") val viaBot: TgUser? = null,
)

@Serializable
private data class TgPreCheckoutQuery(val id: String)

@Serializable
private data class TgSuccessfulPayment(
    @SerialName("telegram_payment_charge_id") val telegramPaymentChargeId: String,
    @SerialName("provider_payment_charge_id") val providerPaymentChargeId: String? = null,
    @SerialName("total_amount") val totalAmount: Int,
    val currency: String,
    @SerialName("invoice_payload") val invoicePayload: String,
)

@Serializable
private data class TgCallbackQuery(
    val id: String,
    val from: TgUser,
    val message: TgMessage? = null,
    val data: String? = null,
)

@Serializable
private data class TgInlineQuery(
    val id: String,
    val from: TgUser,
    val query: String,
)

@Serializable
private data class TgChat(val id: Long)

@Serializable
private data class TgUser(
    val id: Long,
    @SerialName("first_name") val first_name: String? = null,
    @SerialName("last_name") val last_name: String? = null,
    val username: String? = null,
    @SerialName("language_code") val language_code: String? = null,
)
