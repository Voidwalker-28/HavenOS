package app.havenos.launcher.ui

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.havenos.launcher.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 3.1.1 (item 6): drawer solidity screenshot test.
 *
 * WRITTEN, NOT RUN — no device/emulator in the sandbox, and compose
 * UI-test artifacts are not in the offline cache, so this file is not
 * compiled here either (androidTest is not part of
 * :app:assembleRelease, so it cannot break the release build). Run on
 * the Redmi Pad 2 before calling item 6 verified.
 *
 * Method: open the drawer over a home with a bright wallpaper, capture
 * the root bitmap, and assert the left gutter (2% of width, where
 * every drawer view keeps its 20dp content padding, so no text or
 * icon ever lands) is a single solid opaque colour at three heights.
 * A translucent sheet over varied wallpaper would show variation
 * there; a fully opaque sheet cannot. The drawer search field
 * ("Search apps" placeholder) must be displayed, and the test runs
 * twice on the Pad: once with Reduce Effects off, once on, over a
 * bright wallpaper set by the tester before running.
 */
class DrawerSolidTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun drawerGutter_isSolidOpaque() {
        // Open the drawer from the dock Library button.
        rule.onNodeWithContentDescription("Library").performClick()
        rule.onNodeWithText("Search apps").assertExists()

        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val w = bitmap.width
        val h = bitmap.height
        // Left gutter samples: 2% of width sits inside the 20dp
        // content padding on any tablet width, so no row, chip, or
        // icon lands there; three heights stay clear of the top
        // search row and the bottom gesture bar.
        val regions = listOf(0.25f, 0.50f, 0.75f).map { frac ->
            (w * 0.02).toInt() to (h * frac).toInt()
        }
        for ((cx, cy) in regions) {
            val centre = bitmap.getPixel(cx, cy)
            // Every pixel in a 20x20 block around the sample must
            // match the centre pixel exactly: a solid fill.
            for (dx in -10..10) {
                for (dy in -10..10) {
                    val px = (cx + dx).coerceIn(0, w - 1)
                    val py = (cy + dy).coerceIn(0, h - 1)
                    assertTrue(
                        "drawer gutter ($cx,$cy) not solid at ($px,$py)",
                        bitmap.getPixel(px, py) == centre,
                    )
                }
            }
            // The gutter must also be opaque (alpha channel full).
            val alpha = centre ushr 24
            assertTrue("drawer gutter ($cx,$cy) not opaque", alpha == 255)
        }
    }
}
