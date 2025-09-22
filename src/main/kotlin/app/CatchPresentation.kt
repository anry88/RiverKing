package app

import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.roundToInt
import service.I18n
import java.util.Locale

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
    locationName: String,
    displayFishName: String,
    weightKg: Double,
    lang: String,
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

        LOCATION_BACKGROUNDS[locationName]?.let { bgPath ->
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

        val overlayHeight = (size * 0.28).roundToInt()
        val overlayY = size - overlayHeight
        val gradient = GradientPaint(0f, overlayY.toFloat(), Color(0, 0, 0, 0), 0f, size.toFloat(), Color(0, 0, 0, 200))
        g2d.paint = gradient
        g2d.fillRect(0, overlayY, size, overlayHeight)

        val padding = (size * 0.06).roundToInt()
        val maxTextWidth = size - padding * 2

        val nameFont = fitFont(g2d, displayFishName, Font("SansSerif", Font.BOLD, (size * 0.09).roundToInt()), maxTextWidth)
        g2d.font = nameFont
        g2d.color = Color.WHITE
        val nameMetrics = g2d.fontMetrics
        val nameX = padding
        val nameY = overlayY + nameMetrics.ascent + (padding / 3)
        g2d.drawString(displayFishName, nameX, nameY)

        val unit = if (lang == "ru") "кг" else "kg"
        val weightText = "%.2f %s".format(Locale.US, weightKg, unit)
        val weightFont = fitFont(g2d, weightText, Font("SansSerif", Font.PLAIN, (size * 0.07).roundToInt()), maxTextWidth)
        g2d.font = weightFont
        val weightMetrics = g2d.fontMetrics
        val weightX = padding
        val weightY = overlayY + overlayHeight - padding / 2
        g2d.drawString(weightText, weightX, weightY)

        g2d.dispose()
        val baos = ByteArrayOutputStream()
        ImageIO.write(finalImage, "png", baos)
        return baos.toByteArray()
    }
}
