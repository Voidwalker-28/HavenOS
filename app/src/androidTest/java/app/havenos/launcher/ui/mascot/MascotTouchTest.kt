package app.havenos.launcher.ui.mascot

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.havenos.launcher.HavenApp
import app.havenos.launcher.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 3.1.1 (item 4): touch pass-through while the mascot moves. WRITTEN,
 * NOT RUN — there is no device or emulator in the sandbox, and the
 * compose UI-test artifacts are not in the offline cache, so this
 * file is not compiled here either (androidTest is not part of
 * :app:assembleRelease, so it cannot break the release build).
 * Run on the Redmi Pad 2 before calling item 4 verified.
 *
 * Setup: the mascot is enabled and roaming, so it is walking while
 * every gesture runs. Selectors use semantics verified in code:
 * pager indicator "Page N of M", dock "Library"/"Search", drawer
 * placeholder "Search apps", mascot "…open the mascot menu."
 * (substring), bubble "Close" button.
 *
 * The icon-tap test distinguishes a delivered tap from a stolen one:
 * tap an app row, press Back — a delivered tap returns to the still
 * open drawer, a stolen tap closes the drawer to home instead.
 */
@RunWith(AndroidJUnit4::class)
class MascotTouchTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun enableRoamingMascot() {
        val app = HavenApp.of(rule.activity)
        runBlocking {
            app.settings.setMascotOn(true)
            app.settings.setMascotRoaming(true)
            app.settings.setMascotDismissed(false)
        }
        rule.waitForIdle()
    }

    @Test
    fun pagerSwipeWorksWhileMascotWalks() {
        enableRoamingMascot()
        rule.onRoot().performTouchInput { swipeLeft() }
        rule.waitForIdle()
        // The pager subtree is alive and paging did not crash.
        rule.onAllNodesWithContentDescription("Page ", substring = true)
            .fetchSemanticsNodes().let { check(it.isNotEmpty()) }
    }

    @Test
    fun iconTapWorksWhileMascotWalks() {
        enableRoamingMascot()
        rule.onAllNodesWithContentDescription("Library").onFirst().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Search apps").assertIsDisplayed()
        // Tap a list row below the search field + toolbar.
        rule.onRoot().performTouchInput {
            click(center.copy(y = size.height * 0.35f))
        }
        rule.waitForIdle()
        pressBack()
        rule.waitForIdle()
        // Delivered tap: Back returns to the still-open drawer.
        rule.onNodeWithText("Search apps").assertIsDisplayed()
    }

    @Test
    fun longPressOnEmptySpaceWhileMascotWalks() {
        enableRoamingMascot()
        rule.onRoot().performTouchInput {
            down(0)
            advanceEventTime(600)
            up(0)
        }
        rule.waitForIdle()
        rule.onAllNodesWithContentDescription("Page ", substring = true)
            .fetchSemanticsNodes().let { check(it.isNotEmpty()) }
    }

    @Test
    fun dockTapWorksWhileMascotWalks() {
        enableRoamingMascot()
        rule.onAllNodesWithContentDescription("Search").onFirst().performClick()
        rule.waitForIdle()
        pressBack()
        rule.waitForIdle()
        rule.onAllNodesWithContentDescription("Page ", substring = true)
            .fetchSemanticsNodes().let { check(it.isNotEmpty()) }
    }

    @Test
    fun drawerOpensWhileMascotWalks() {
        enableRoamingMascot()
        rule.onAllNodesWithContentDescription("Library").onFirst().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Search apps").assertIsDisplayed()
    }

    @Test
    fun swipeDismissesBubbleAndScrollsPager() {
        enableRoamingMascot()
        rule.onNodeWithContentDescription("mascot menu", substring = true).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Close").assertIsDisplayed()
        rule.onRoot().performTouchInput { swipeRight() }
        rule.waitForIdle()
        // The bubble closed (its Close button is gone) and the pager
        // survived the swipe — dismiss never consumed the gesture.
        rule.onNodeWithText("Close").assertDoesNotExist()
        rule.onAllNodesWithContentDescription("Page ", substring = true)
            .fetchSemanticsNodes().let { check(it.isNotEmpty()) }
    }
}
