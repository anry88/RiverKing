package app

import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

class CatchPresentationTest {
    @Test
    fun `webp event backgrounds are readable by imageio`() {
        assertTrue(
            ImageIO.getImageReadersByFormatName("webp").hasNext(),
            "WebP event location backgrounds must be readable for catch cards",
        )
    }
}
