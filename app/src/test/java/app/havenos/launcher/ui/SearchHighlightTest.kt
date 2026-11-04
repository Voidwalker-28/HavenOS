package app.havenos.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 3.1 Phase C (C2): case-insensitive match spans for search-result
 * highlighting in the drawer/Library.
 *
 * NOTE: written-but-not-run — the sandbox test executor is broken
 * (pre-existing) and SettingsShellTest has pre-existing compile errors.
 */
class SearchHighlightTest {

    @Test
    fun blankQueryHasNoSpans() {
        assertTrue(matchSpans("Firefox", "").isEmpty())
        assertTrue(matchSpans("Firefox", "   ").isEmpty())
    }

    @Test
    fun singleMatchCaseInsensitive() {
        assertEquals(listOf(0..2), matchSpans("Firefox", "fir"))
        assertEquals(listOf(0..2), matchSpans("Firefox", "FIR"))
    }

    @Test
    fun multipleNonOverlappingMatches() {
        assertEquals(listOf(0..0, 3..3), matchSpans("aXaXa", "a"))
    }

    @Test
    fun noMatchIsEmpty() {
        assertTrue(matchSpans("Firefox", "zzz").isEmpty())
    }

    @Test
    fun queryLongerThanTextIsEmpty() {
        assertTrue(matchSpans("Hi", "Hello").isEmpty())
    }
}
