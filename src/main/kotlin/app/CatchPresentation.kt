package app

import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.roundToInt
import service.I18n
import java.util.Locale
import service.FishingService

private val FISH_IMAGE_PATHS = mapOf(
    "Плотва" to "webapp/assets/fish/plotva.png",
    "Окунь" to "webapp/assets/fish/okun.png",
    "Карась" to "webapp/assets/fish/karas.png",
    "Лещ" to "webapp/assets/fish/lesch.png",
    "Щука" to "webapp/assets/fish/schuka.png",
    "Карп" to "webapp/assets/fish/karp.png",
    "Сом" to "webapp/assets/fish/som.png",
    "Осётр" to "webapp/assets/fish/osetr.png",
    "Уклейка" to "webapp/assets/fish/ukleyka.png",
    "Линь" to "webapp/assets/fish/lin.png",
    "Ротан" to "webapp/assets/fish/rotan.png",
    "Судак" to "webapp/assets/fish/sudak.png",
    "Чехонь" to "webapp/assets/fish/chehon.png",
    "Хариус" to "webapp/assets/fish/harius.png",
    "Форель ручьевая" to "webapp/assets/fish/forel_ruchevaya.png",
    "Таймень" to "webapp/assets/fish/taymen.png",
    "Налим" to "webapp/assets/fish/nalim.png",
    "Сиг" to "webapp/assets/fish/sig.png",
    "Голавль" to "webapp/assets/fish/golavl.png",
    "Жерех" to "webapp/assets/fish/zhereh.png",
    "Толстолобик" to "webapp/assets/fish/tolstolobik.png",
    "Белый амур" to "webapp/assets/fish/beliy_amur.png",
    "Угорь европейский" to "webapp/assets/fish/ugor_evropeyskiy.png",
    "Стерлядь" to "webapp/assets/fish/sterlyad.png",
    "Кефаль" to "webapp/assets/fish/kefal.png",
    "Камбала" to "webapp/assets/fish/kambala.png",
    "Сельдь" to "webapp/assets/fish/seld.png",
    "Ставрида" to "webapp/assets/fish/stavrida.png",
    "Треска" to "webapp/assets/fish/treska.png",
    "Сайда" to "webapp/assets/fish/sayda.png",
    "Морская форель" to "webapp/assets/fish/morskaya_forel.png",
    "Палтус" to "webapp/assets/fish/paltus.png",
    "Корюшка" to "webapp/assets/fish/koryushka.png",
    "Лосось атлантический" to "webapp/assets/fish/losos_atlanticheskiy.png",
    "Лаврак" to "webapp/assets/fish/lavrak.png",
    "Скумбрия атлантическая" to "webapp/assets/fish/skumbriya_atlanticheskaya.png",
    "Белуга" to "webapp/assets/fish/beluga.png",
    "Ёрш" to "webapp/assets/fish/yorsh.png",
    "Пескарь" to "webapp/assets/fish/peskar.png",
    "Густера" to "webapp/assets/fish/gustera.png",
    "Краснопёрка" to "webapp/assets/fish/krasnopyorka.png",
    "Елец" to "webapp/assets/fish/elets.png",
    "Верхоплавка" to "webapp/assets/fish/verhoplavka.png",
    "Гольян" to "webapp/assets/fish/golyan.png",
    "Язь" to "webapp/assets/fish/yaz.png",
    "Бычок" to "webapp/assets/fish/bychyok.png",
    "Килька" to "webapp/assets/fish/kilka.png",
    "Мойва" to "webapp/assets/fish/mojva.png",
    "Сардина" to "webapp/assets/fish/sardina.png",
    "Анчоус" to "webapp/assets/fish/anchous.png",
    "Дорадо" to "webapp/assets/fish/dorado.png",
    "Ваху" to "webapp/assets/fish/vahu.png",
    "Парусник" to "webapp/assets/fish/parusnik.png",
    "Рыба-меч" to "webapp/assets/fish/ryba_mech.png",
    "Марлин синий" to "webapp/assets/fish/marlin_siniy.png",
    "Тунец синеперый" to "webapp/assets/fish/tunets_sineperiy.png",
    "Акула мако" to "webapp/assets/fish/akula_mako.png",
    "Альбакор" to "webapp/assets/fish/albakor.png",
    "Голец арктический" to "webapp/assets/fish/golets_arkticheskiy.png",
    "Форель кумжа" to "webapp/assets/fish/forel_kumzha.png",
    "Пикша" to "webapp/assets/fish/piksha.png",
    "Тюрбо" to "webapp/assets/fish/tyurbo.png",
    "Сайра" to "webapp/assets/fish/sayra.png",
    "Летучая рыба" to "webapp/assets/fish/letuchaya_ryba.png",
    "Рыба-луна" to "webapp/assets/fish/ryba_luna.png",
    "Сельдяной король" to "webapp/assets/fish/seldyanoy_korol.png",
)

private val LOCATION_BACKGROUNDS = run {
    val base = mapOf(
        "Пруд" to "webapp/assets/originals/backgrounds/pond.png",
        "Река" to "webapp/assets/originals/backgrounds/river.png",
        "Озеро" to "webapp/assets/originals/backgrounds/lake.png",
        "Болото" to "webapp/assets/originals/backgrounds/swamp.png",
        "Горная река" to "webapp/assets/originals/backgrounds/mountain_river.png",
        "Водохранилище" to "webapp/assets/originals/backgrounds/reservoir.png",
        "Дельта реки" to "webapp/assets/originals/backgrounds/river_delta.png",
        "Прибрежье моря" to "webapp/assets/originals/backgrounds/sea_coast.png",
        "Фьорд" to "webapp/assets/originals/backgrounds/fjord.png",
        "Открытый океан" to "webapp/assets/originals/backgrounds/open_ocean.png",
    )
    base + base.mapKeys { (name, _) -> I18n.location(name, "en") }
}

val RARITY_LABELS = mapOf(
    "ru" to mapOf(
        "common" to "Простая",
        "uncommon" to "Необычная",
        "rare" to "Редкая",
        "epic" to "Эпическая",
        "legendary" to "Легендарная",
    ),
    "en" to mapOf(
        "common" to "Common",
        "uncommon" to "Uncommon",
        "rare" to "Rare",
        "epic" to "Epic",
        "legendary" to "Legendary",
    ),
)

private val RARITY_COLORS = mapOf(
    "common" to Color.WHITE,
    "uncommon" to Color(0x34, 0xD3, 0x99),
    "rare" to Color(0x60, 0xA5, 0xFA),
    "epic" to Color(0xC0, 0x84, 0xFC),
    "legendary" to Color(0xFA, 0xCC, 0x15),
)

private val DATE_FORMATTERS = mapOf(
    "ru" to DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),
    "en" to DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
)

private fun formatCatchDate(instant: Instant, lang: String): String {
    val zone = ZoneId.systemDefault()
    val formatter = DATE_FORMATTERS[lang] ?: DATE_FORMATTERS.getValue("en")
    return instant.atZone(zone).format(formatter)
}

private data class PreparedInfoEntry(
    val text: String,
    val font: Font,
    val metrics: java.awt.FontMetrics,
)

fun buildCatchCaption(
    lang: String,
    fishName: String,
    rarity: String,
    weightKg: Double,
    locationName: String,
    extraLines: List<String> = emptyList(),
): String {
    val rarityText = RARITY_LABELS[lang]?.get(rarity) ?: rarity
    val weightText = "%.2f".format(Locale.US, weightKg)
    val unit = if (lang == "ru") "кг" else "kg"
    val locationLabel = if (lang == "ru") "Локация" else "Location"
    return buildString {
        append("🐟 ")
        append(fishName)
        append(" (")
        append(rarityText)
        append(") — ")
        append(weightText)
        append(' ')
        append(unit)
        append('\n')
        append(locationLabel)
        append(": ")
        append(locationName)
        extraLines.forEach { line ->
            if (line.isNotBlank()) append(line)
        }
    }
}

private fun toHashtagValue(value: String): String {
    val parts = Regex("[\\p{L}\\p{N}]+")
        .findAll(value)
        .map { it.value }
        .filter { it.isNotEmpty() }
        .toList()
    if (parts.isEmpty()) {
        return value.filter { it.isLetterOrDigit() }
    }
    return parts.joinToString(separator = "") { part ->
        if (part.length == 1) {
            part.uppercase()
        } else {
            buildString {
                append(part.first().uppercaseChar())
                append(part.substring(1))
            }
        }
    }
}

fun catchHashtags(catch: FishingService.CatchDTO): String {
    val rarityLabel = RARITY_LABELS["en"]?.get(catch.rarity) ?: catch.rarity
    val rarityValue = toHashtagValue(rarityLabel)
    val fishNameEn = toHashtagValue(I18n.fish(catch.fish, "en"))
    val fishNameRu = toHashtagValue(I18n.fish(catch.fish, "ru"))
    return "#RiverKing #$rarityValue #$fishNameEn #$fishNameRu"
}

fun appendCatchTags(base: String, catch: FishingService.CatchDTO): String {
    val tags = catchHashtags(catch)
    return if (base.isBlank()) {
        tags
    } else {
        "$base\n\n$tags"
    }
}

private fun fitFont(g2d: java.awt.Graphics2D, text: String, baseFont: Font, maxWidth: Int): Font {
    var font = baseFont
    var metrics = g2d.getFontMetrics(font)
    while (metrics.stringWidth(text) > maxWidth && font.size > 24) {
        font = font.deriveFont((font.size - 2).toFloat())
        metrics = g2d.getFontMetrics(font)
    }
    return font
}

fun generateCatchImage(
    fishInternalName: String,
    locationInternalName: String,
    displayFishName: String,
    displayLocationName: String? = null,
    weightKg: Double,
    rarity: String?,
    lang: String,
    anglerName: String? = null,
    caughtAt: Instant? = null,
): ByteArray? {
    val path = FISH_IMAGE_PATHS[fishInternalName] ?: return null
    val classLoader = Thread.currentThread().contextClassLoader
    val originalPath = path.replace("webapp/assets/fish/", "webapp/assets/originals/fish/")
    val fishStream = classLoader.getResourceAsStream(originalPath) ?: return null
    fishStream.use { stream ->
        val fishImage = ImageIO.read(stream) ?: return null
        val size = 1024
        val finalImage = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g2d = finalImage.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val backgroundPath = LOCATION_BACKGROUNDS[locationInternalName]
            ?: displayLocationName?.let { LOCATION_BACKGROUNDS[it] }
        backgroundPath?.let { bgPath ->
            classLoader.getResourceAsStream(bgPath)?.use { bgStream ->
                val bgImage = ImageIO.read(bgStream)
                if (bgImage != null) {
                    val scale = max(size.toDouble() / bgImage.width, size.toDouble() / bgImage.height)
                    val newWidth = (bgImage.width * scale).roundToInt()
                    val newHeight = (bgImage.height * scale).roundToInt()
                    val offsetX = ((size - newWidth) / 2.0).roundToInt()
                    val offsetY = ((size - newHeight) / 2.0).roundToInt()
                    g2d.drawImage(
                        bgImage,
                        offsetX,
                        offsetY,
                        offsetX + newWidth,
                        offsetY + newHeight,
                        0,
                        0,
                        bgImage.width,
                        bgImage.height,
                        null,
                    )
                }
            }
        }

        val fishX = ((size - fishImage.width) / 2.0).roundToInt()
        val fishY = ((size - fishImage.height) / 2.0).roundToInt()
        g2d.drawImage(fishImage, fishX, fishY, null)

        val padding = (size * 0.06).roundToInt()
        val maxTextWidth = size - padding * 2
        val rarityColor = RARITY_COLORS[rarity?.lowercase(Locale.US)] ?: Color.WHITE

        val nameFont = fitFont(g2d, displayFishName, Font("SansSerif", Font.BOLD, (size * 0.09).roundToInt()), maxTextWidth)
        g2d.font = nameFont
        val nameMetrics = g2d.fontMetrics
        val topPadding = (padding * 0.6).roundToInt().coerceAtLeast(24)
        val nameY = topPadding + nameMetrics.ascent
        val topOverlayHeight = nameY + nameMetrics.descent + topPadding
        val topGradient = GradientPaint(0f, 0f, Color(0, 0, 0, 200), 0f, topOverlayHeight.toFloat(), Color(0, 0, 0, 0))
        g2d.paint = topGradient
        g2d.fillRect(0, 0, size, topOverlayHeight)
        g2d.color = rarityColor
        g2d.drawString(displayFishName, padding, nameY)

        val overlayHeight = (size * 0.32).roundToInt()
        val overlayY = size - overlayHeight
        val bottomGradient = GradientPaint(0f, overlayY.toFloat(), Color(0, 0, 0, 0), 0f, size.toFloat(), Color(0, 0, 0, 200))
        g2d.paint = bottomGradient
        g2d.fillRect(0, overlayY, size, overlayHeight)

        val unit = if (lang == "ru") "кг" else "kg"
        val infoItems = mutableListOf<String>()
        displayLocationName?.takeIf { it.isNotBlank() }?.let { infoItems += it }
        anglerName?.takeIf { it.isNotBlank() }?.let { infoItems += it }
        caughtAt?.let { infoItems += formatCatchDate(it, lang) }

        val infoEntries = infoItems.map { text ->
            var infoFont = Font("SansSerif", Font.PLAIN, (size * 0.045).roundToInt())
            var metrics = g2d.getFontMetrics(infoFont)
            while (metrics.stringWidth(text) > maxTextWidth && infoFont.size > 22) {
                infoFont = infoFont.deriveFont((infoFont.size - 2).toFloat())
                metrics = g2d.getFontMetrics(infoFont)
            }
            PreparedInfoEntry(text, infoFont, metrics)
        }

        val weightText = "%.2f %s".format(Locale.US, weightKg, unit)
        val weightFont = fitFont(g2d, weightText, Font("SansSerif", Font.PLAIN, (size * 0.07).roundToInt()), maxTextWidth)
        g2d.font = weightFont
        g2d.color = rarityColor
        val weightMetrics = g2d.getFontMetrics(weightFont)
        var weightBaseline = overlayY + overlayHeight - padding / 2
        val bottomMargin = (padding * 0.24).roundToInt().coerceAtLeast(12)
        val maxWeightBaseline = (size - bottomMargin).coerceAtLeast(weightBaseline)
        if (weightBaseline > maxWeightBaseline) {
            weightBaseline = maxWeightBaseline
        }

        val infoAnchor = overlayY
        val topSpacing = (padding * 0.35).roundToInt().coerceAtLeast(22)
        val minTopSpacing = (padding * 0.25).roundToInt().coerceAtLeast(16)
        var afterSpacing = (padding * 0.24).roundToInt()
        val minAfterSpacing = (padding * 0.14).roundToInt().coerceAtLeast(8)
        var betweenSpacing = if (infoEntries.size > 1) (padding * 0.16).roundToInt() else 0
        val minBetweenSpacing = if (infoEntries.size > 1) (padding * 0.08).roundToInt().coerceAtLeast(3) else 0
        val infoHeightSum = infoEntries.sumOf { it.metrics.ascent + it.metrics.descent }

        fun layoutInfoLines(): Triple<List<Int>, Int, Int> {
            if (infoEntries.isEmpty()) {
                val weightTop = weightBaseline - weightMetrics.ascent
                val reference = weightTop - afterSpacing
                return Triple(emptyList(), reference, reference)
            }
            val weightTop = weightBaseline - weightMetrics.ascent
            var currentTop = weightTop - afterSpacing
            val baselines = MutableList(infoEntries.size) { 0 }
            val spacingBetween = if (infoEntries.size > 1) betweenSpacing else 0
            for (i in infoEntries.indices.reversed()) {
                val entry = infoEntries[i]
                val baseline = currentTop - entry.metrics.descent
                baselines[i] = baseline
                val top = baseline - entry.metrics.ascent
                currentTop = top - spacingBetween
            }
            val firstEntry = infoEntries.first()
            val lastEntry = infoEntries.last()
            val infoTop = baselines.first() - firstEntry.metrics.ascent
            val infoBottom = baselines.last() + lastEntry.metrics.descent
            return Triple(baselines, infoTop, infoBottom)
        }

        var infoBlockHeight = if (infoEntries.isNotEmpty()) {
            infoHeightSum + betweenSpacing * (infoEntries.size - 1)
        } else {
            0
        }

        val maxIterations = 10
        var baselines: List<Int> = emptyList()
        var infoTop = 0
        var infoBottom = 0
        for (iteration in 0 until maxIterations) {
            val weightTop = weightBaseline - weightMetrics.ascent
            val desiredTop = infoAnchor + topSpacing
            val projectedTop = weightTop - afterSpacing - infoBlockHeight
            if (projectedTop < desiredTop) {
                val deficit = desiredTop - projectedTop
                val shift = (maxWeightBaseline - weightBaseline).coerceAtLeast(0)
                if (shift > 0 && deficit > 0) {
                    val appliedShift = deficit.coerceAtMost(shift)
                    weightBaseline += appliedShift
                    continue
                }
                if (betweenSpacing > minBetweenSpacing && infoEntries.size > 1) {
                    val perUnitGain = infoEntries.size - 1
                    val neededUnits = ((deficit + perUnitGain - 1) / perUnitGain).coerceAtLeast(1)
                    val reduce = neededUnits.coerceAtMost(betweenSpacing - minBetweenSpacing)
                    betweenSpacing -= reduce
                    infoBlockHeight = infoHeightSum + betweenSpacing * (infoEntries.size - 1)
                    continue
                }
                if (afterSpacing > minAfterSpacing) {
                    val reduce = deficit.coerceAtMost(afterSpacing - minAfterSpacing)
                    afterSpacing -= reduce
                    continue
                }
            }

            val layout = layoutInfoLines()
            baselines = layout.first
            infoTop = layout.second
            infoBottom = layout.third
            val desiredSpacingTop = infoAnchor + topSpacing
            if (infoTop < desiredSpacingTop) {
                val deficitTop = desiredSpacingTop - infoTop
                val availableShift = (maxWeightBaseline - weightBaseline).coerceAtLeast(0)
                if (availableShift > 0 && deficitTop > 0) {
                    val shift = deficitTop.coerceAtMost(availableShift)
                    weightBaseline += shift
                    continue
                }
            }

            val weightTopAfter = weightBaseline - weightMetrics.ascent
            val neededBottomSpacing = infoBottom + afterSpacing
            if (neededBottomSpacing > weightTopAfter) {
                val deficitBottom = neededBottomSpacing - weightTopAfter
                val availableShift = (maxWeightBaseline - weightBaseline).coerceAtLeast(0)
                if (availableShift > 0 && deficitBottom > 0) {
                    val shift = deficitBottom.coerceAtMost(availableShift)
                    weightBaseline += shift
                    continue
                }
                if (afterSpacing > minAfterSpacing && deficitBottom > 0) {
                    val reduce = deficitBottom.coerceAtMost(afterSpacing - minAfterSpacing)
                    afterSpacing -= reduce
                    continue
                }
                if (betweenSpacing > minBetweenSpacing && infoEntries.size > 1 && deficitBottom > 0) {
                    val perUnitGain = infoEntries.size - 1
                    val neededUnits = ((deficitBottom + perUnitGain - 1) / perUnitGain).coerceAtLeast(1)
                    val reduce = neededUnits.coerceAtMost(betweenSpacing - minBetweenSpacing)
                    betweenSpacing -= reduce
                    infoBlockHeight = infoHeightSum + betweenSpacing * (infoEntries.size - 1)
                    continue
                }
            }

            break
        }

        if (infoEntries.isNotEmpty() && baselines.isEmpty()) {
            val layout = layoutInfoLines()
            baselines = layout.first
            infoTop = layout.second
            infoBottom = layout.third
        }

        if (infoEntries.isNotEmpty()) {
            val desiredTop = infoAnchor + topSpacing
            val minimalTop = infoAnchor + minTopSpacing
            if (infoTop < desiredTop) {
                val availableShift = (maxWeightBaseline - weightBaseline).coerceAtLeast(0)
                val needed = desiredTop - infoTop
                val applied = needed.coerceAtMost(availableShift)
                if (applied > 0) {
                    weightBaseline += applied
                    baselines = baselines.map { it + applied }
                    infoTop += applied
                    infoBottom += applied
                }
            }
            if (infoTop < minimalTop) {
                val availableShift = (maxWeightBaseline - weightBaseline).coerceAtLeast(0)
                val needed = minimalTop - infoTop
                val applied = needed.coerceAtMost(availableShift)
                if (applied > 0) {
                    weightBaseline += applied
                    baselines = baselines.map { it + applied }
                    infoTop += applied
                    infoBottom += applied
                }
            }
            if (infoTop < minimalTop) {
                val offset = minimalTop - infoTop
                baselines = baselines.map { it + offset }
                weightBaseline += offset
                infoBottom += offset
                infoTop += offset
                if (weightBaseline > maxWeightBaseline) {
                    val overflow = weightBaseline - maxWeightBaseline
                    weightBaseline -= overflow
                    baselines = baselines.map { it - overflow }
                    infoTop -= overflow
                    infoBottom -= overflow
                }
            }
        }

        val textRightEdge = size - padding

        infoEntries.forEachIndexed { index, entry ->
            g2d.font = entry.font
            g2d.color = rarityColor
            val baseline = baselines.getOrNull(index)
                ?: (weightBaseline - weightMetrics.ascent - afterSpacing)
            val textWidth = entry.metrics.stringWidth(entry.text)
            val textX = textRightEdge - textWidth
            g2d.drawString(entry.text, textX, baseline)
        }

        g2d.font = weightFont
        g2d.color = rarityColor
        val weightWidth = weightMetrics.stringWidth(weightText)
        val weightX = textRightEdge - weightWidth
        g2d.drawString(weightText, weightX, weightBaseline)

        g2d.dispose()
        val baos = ByteArrayOutputStream()
        ImageIO.write(finalImage, "png", baos)
        return baos.toByteArray()
    }
}
