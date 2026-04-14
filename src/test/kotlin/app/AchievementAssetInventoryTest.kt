package app

import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AchievementAssetInventoryTest {
    private val repoRoot = File(".").canonicalFile.toPath()
    private val sharedScript = repoRoot.resolve("src/main/resources/webapp/scripts/shared.js")
    private val webappAssetsDir = repoRoot.resolve("src/main/resources/webapp/assets/achievements")
    private val androidAssetsDir = repoRoot.resolve("mobile/android-app/app/src/main/assets/achievements")
    private val referencedPngs = Regex("/app/assets/achievements/([a-z0-9_]+\\.png)")
        .findAll(sharedScript.readText())
        .map { it.groupValues[1] }
        .toSortedSet()

    @Test
    fun referencedAchievementPngsExistWithTransparent1024Frames() {
        assertTrue(referencedPngs.isNotEmpty(), "No achievement PNGs were referenced in shared.js")
        referencedPngs.forEach { fileName ->
            val png = webappAssetsDir.resolve(fileName).toFile()
            assertTrue(png.isFile, "Missing webapp achievement asset: $fileName")
            val image = ImageIO.read(png)
            assertNotNull(image, "Unreadable PNG: $fileName")
            assertEquals(1024, image.width, "Achievement asset must be 1024px wide: $fileName")
            assertEquals(1024, image.height, "Achievement asset must be 1024px tall: $fileName")
            assertTrue(image.colorModel.hasAlpha(), "Achievement asset must keep transparent background: $fileName")
        }
    }

    @Test
    fun referencedAchievementPngsAreBundledIntoAndroidAssets() {
        referencedPngs.forEach { fileName ->
            val bundled = androidAssetsDir.resolve(fileName.removeSuffix(".png") + ".webp").toFile()
            assertTrue(bundled.isFile, "Missing Android achievement bundle asset: ${bundled.name}")
        }
    }
}
