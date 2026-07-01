package app

import javax.imageio.ImageIO
import kotlin.test.Test
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
        ).forEach { path ->
            val image = javaClass.classLoader.getResourceAsStream(path)?.use(ImageIO::read)
            assertNotNull(image, "Missing catch card source: $path")

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
