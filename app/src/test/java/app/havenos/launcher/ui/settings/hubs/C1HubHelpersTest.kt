package app.havenos.launcher.ui.settings.hubs

import app.havenos.launcher.data.network.ApiDiagnosticsRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 3.1 Phase C (C1): API hub status lines, GitHub username validation,
 * and the grid-shrink one-sentence summary.
 *
 * NOTE: written-but-not-run — the sandbox test executor is broken
 * (pre-existing) and SettingsShellTest has pre-existing compile errors.
 */
class C1HubHelpersTest {

    private fun diag(resultClass: String, timeMs: Long = 1_000L) =
        ApiDiagnosticsRow(api = "QUOTE", resultClass = resultClass, httpCode = 200, timeMs = timeMs)

    @Test
    fun statusLine_offBeatsEverything() {
        assertEquals("Off", apiHubStatusLine(false, true, diag("SUCCESS")))
        assertEquals("Off", apiHubStatusLine(false, false, null))
    }

    @Test
    fun statusLine_needsKeyBeforeDiagnostics() {
        assertEquals("Needs key", apiHubStatusLine(true, true, diag("SUCCESS")))
    }

    @Test
    fun statusLine_onWithNoDiagnosticsHasNoLine() {
        assertNull(apiHubStatusLine(true, false, null))
    }

    @Test
    fun statusLine_successShowsUpdatedTime() {
        val line = apiHubStatusLine(true, false, diag("SUCCESS", System.currentTimeMillis()))
        assertTrue(line != null && line.startsWith("Updated "))
    }

    @Test
    fun statusLine_a2ErrorTexts() {
        assertEquals("Offline", apiHubStatusLine(true, false, diag("OFFLINE")))
        assertEquals("Busy", apiHubStatusLine(true, false, diag("RATE_LIMITED")))
        assertEquals("Couldn't load", apiHubStatusLine(true, false, diag("NETWORK_ERROR")))
        assertEquals("Couldn't load", apiHubStatusLine(true, false, diag("NOT_FOUND")))
        assertEquals("Couldn't load", apiHubStatusLine(true, false, diag("PARSE_ERROR")))
        assertEquals("Couldn't load", apiHubStatusLine(true, false, diag("SERVER_ERROR")))
        assertEquals("Couldn't load", apiHubStatusLine(true, false, diag("OTHER")))
    }

    @Test
    fun githubUsername_blankIsUnset() {
        assertNull(githubUsernameError(""))
        assertNull(githubUsernameError("   "))
    }

    @Test
    fun githubUsername_validShapes() {
        assertNull(githubUsernameError("octocat"))
        assertNull(githubUsernameError("Voidwalker-28"))
        assertNull(githubUsernameError("a"))
    }

    @Test
    fun githubUsername_rejectsBadShapes() {
        assertTrue(githubUsernameError("-lead") != null)
        assertTrue(githubUsernameError("trail-") != null)
        assertTrue(githubUsernameError("has space") != null)
        assertTrue(githubUsernameError("under_score") != null)
        assertTrue(githubUsernameError("x".repeat(40)) != null)
    }

    @Test
    fun moveSentence_zeroDestinations() {
        assertEquals("0 items will move.", moveSummarySentence(0, emptyList()))
    }

    @Test
    fun moveSentence_singularAndPlural() {
        assertEquals("1 item will move to Page 2 (1).", moveSummarySentence(1, listOf("Page 2 (1)")))
        assertEquals(
            "3 items will move to Page 2 (2) and Page 3 (1).",
            moveSummarySentence(3, listOf("Page 2 (2)", "Page 3 (1)")),
        )
        assertEquals(
            "5 items will move to Page 2 (2), Page 3 (2) and Page 4 (1).",
            moveSummarySentence(5, listOf("Page 2 (2)", "Page 3 (2)", "Page 4 (1)")),
        )
    }

    @Test
    fun moveSentence_pastEightStopsAtCount() {
        val nine = (2..10).map { "Page $it (1)" }
        assertEquals("9 items will move.", moveSummarySentence(9, nine))
    }

    @Test
    fun visibleDestinations_collapsePastEight() {
        val nine = (2..10).map { "Page $it (1)" }
        val (shown, more) = visibleDestinationEntries(nine, expanded = false)
        assertEquals(8, shown.size)
        assertEquals(1, more)
        val (all, none) = visibleDestinationEntries(nine, expanded = true)
        assertEquals(9, all.size)
        assertEquals(0, none)
    }
}
