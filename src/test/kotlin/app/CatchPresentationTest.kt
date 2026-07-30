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
                corners.all { pixel -> (pixel ushr 24) == 0 },
                "Catch card source must have transparent corners: $path",
            )
        }
    }
}
