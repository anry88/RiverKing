package app

import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CatchPresentationTest {
    @Test
    fun `webp event backgrounds are readable by imageio`() {
        assertTrue(
            ImageIO.getImageReadersByFormatName("webp").hasNext(),
            "WebP event location backgrounds must be readable for catch cards",
        )
    }

    @Test
    fun `new event fish catch card sources have transparent corners`() {
        listOf(
            "webapp/assets/originals/fish/morskaya_minoga.png",
            "webapp/assets/originals/fish/evropeiskaya_aloza.png",
            "webapp/assets/originals/fish/pikeminnou_severniy.png",
            "webapp/assets/originals/fish/chizelmaut.png",
            "webapp/assets/originals/fish/pimut.png",
            "webapp/assets/originals/fish/okun_malorotiy.png",
            "webapp/assets/originals/fish/somik_kanalniy.png",
            "webapp/assets/originals/fish/sudak_svetlopery.png",
            "webapp/assets/originals/fish/aloza_amerikanskaya.png",
            "webapp/assets/originals/fish/kizhuch.png",
            "webapp/assets/originals/fish/nerka.png",
            "webapp/assets/originals/fish/forel_stalnogolovaya.png",
            "webapp/assets/originals/fish/minoga_tihookeanskaya.png",
            "webapp/assets/originals/fish/chavycha.png",
            "webapp/assets/originals/fish/osetr_beliy.png",
            "webapp/assets/originals/fish/curimbata.png",
            "webapp/assets/originals/fish/piraputanga.png",
            "webapp/assets/originals/fish/piavucu.png",
            "webapp/assets/originals/fish/cachara.png",
            "webapp/assets/originals/fish/falkneri_stingray.png",
            "webapp/assets/originals/fish/golden_dorado.png",
            "webapp/assets/originals/fish/pintado.png",
            "webapp/assets/originals/fish/black_phantom_tetra.png",
            "webapp/assets/originals/fish/jurupoca.png",
            "webapp/assets/originals/fish/palmito.png",
            "webapp/assets/originals/fish/barbado.png",
            "webapp/assets/originals/fish/pati.png",
            "webapp/assets/originals/fish/jau.png",
        ).forEach { path ->
            val image = javaClass.classLoader.getResourceAsStream(path)?.use(ImageIO::read)
            assertNotNull(image, "Missing catch card source: $path")
            assertEquals(1024, image.width, "Catch card source width must be square: $path")
            assertEquals(1024, image.height, "Catch card source height must be square: $path")

            val corners = listOf(
                image.getRGB(0, 0),
                image.getRGB(image.width - 1, 0),
                image.getRGB(0, image.height - 1),
                image.getRGB(image.width - 1, image.height - 1),
            )
            assertTrue(
                // Generated transparent PNGs can retain one alpha quantization level.
                corners.all { pixel -> (pixel ushr 24) <= 1 },
                "Catch card source must have transparent corners: $path",
            )
        }
    }

    @Test
    fun `porto amolar event fish render localized catch cards`() {
        assertEquals("Porto Amolar", service.I18n.location("Порту-Амолар", "en"))
        listOf("Куримбата", "Пирапутанга", "Пиавусу", "Качара", "Скат Фалькнера", "Дорадо золотой", "Пинтадо",
            "Чёрный фантом", "Журупока", "Пальмито", "Барбадо", "Пати", "Жау")
            .forEach { name ->
                val english = service.I18n.fish(name, "en")
                assertTrue(english != name, "Missing English fish name: $name")
                val bytes = generateCatchImage(
                    fishInternalName = name,
                    locationInternalName = "Порту-Амолар",
                    displayFishName = english,
                    displayLocationName = "Porto Amolar",
                    weightKg = 2.0,
                    rarity = "legendary",
                    lang = "en",
                )
                assertNotNull(bytes, "Missing share card for $name")
                val card = bytes.inputStream().use(ImageIO::read)
                assertEquals(1024, card.width)
                assertEquals(1024, card.height)
            }
    }
}
