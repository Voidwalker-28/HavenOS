package app.havenos.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M7 Wave 1 — Library + Search card pure logic (LibrarySearch.kt).
 * Framework-free: [LibraryApp] carries no Android types, so these run
 * on the JVM with plain JUnit.
 */
class LibrarySearchTest {

    private fun app(key: String, label: String, installTime: Long = 0L) =
        LibraryApp(key, label, "com.example.$key", installTime)

    private val dayMs = 24L * 60 * 60 * 1000

    // ---------- ranking: prefix > word > fuzzy ----------

    @Test
    fun rank_prefixBeatsWordBeatsFuzzy() {
        val apps = listOf(
            app("compass", "Compass"),
            app("gmaps", "Google Maps"),
            app("maps", "Maps"),
        )
        // "map": Maps = prefix, Google Maps = word, Compass = fuzzy
        // only via subsequence? "map" is not a subsequence of "compass"
        // (m-a-p in order? c-o-m-p-a-s-s: m, then a, then... no p after
        // the a — so Compass does not match at all here).
        val ranked = rankLibraryApps(apps, "map", emptyMap())
        assertEquals(listOf("Maps", "Google Maps"), ranked.map { it.label })
    }

    @Test
    fun rank_fuzzyMatchesSubsequence() {
        val apps = listOf(
            app("gmaps", "Google Maps"),
            app("maps", "Maps"),
            app("mps", "My Photo Studio"),
        )
        // "mps" is a subsequence of all three (M-P-S in order), so all
        // land in the fuzzy band and label order breaks the tie.
        val ranked = rankLibraryApps(apps, "mps", emptyMap())
        assertEquals(
            listOf("Google Maps", "Maps", "My Photo Studio"),
            ranked.map { it.label },
        )
    }

    @Test
    fun rank_wordBeatsContains() {
        val apps = listOf(
            app("seamap", "Seamap"),
            app("gmaps", "Google Maps"),
        )
        // "map": Google Maps = word match; Seamap = contains only.
        val ranked = rankLibraryApps(apps, "map", emptyMap())
        assertEquals(listOf("Google Maps", "Seamap"), ranked.map { it.label })
    }

    @Test
    fun rank_containsBeatsFuzzy() {
        // "amp" is contained mid-word in "Seamap" (band 2) but only a
        // subsequence of "Aardvark Music Player" (band 3).
        val ranked = rankLibraryApps(
            listOf(app("amp", "Aardvark Music Player"), app("seamap", "Seamap")),
            "amp", emptyMap(),
        )
        assertEquals(listOf("Seamap", "Aardvark Music Player"), ranked.map { it.label })
    }

    @Test
    fun rank_emptyQueryReturnsEmpty() {
        assertTrue(rankLibraryApps(listOf(app("a", "Alpha")), "  ", emptyMap()).isEmpty())
    }

    @Test
    fun rank_noMatchReturnsEmpty() {
        assertTrue(rankLibraryApps(listOf(app("a", "Alpha")), "zzz", emptyMap()).isEmpty())
    }

    // ---------- ranking: launch-count + recency boost ----------

    @Test
    fun rank_higherLaunchCountWinsWithinBand() {
        val apps = listOf(app("a", "Maps"), app("b", "Maple"))
        val stats = mapOf("a" to (2 to 1000L), "b" to (50 to 1000L))
        val ranked = rankLibraryApps(apps, "map", stats)
        assertEquals(listOf("Maple", "Maps"), ranked.map { it.label })
    }

    @Test
    fun rank_moreRecentWinsOnEqualCount() {
        val apps = listOf(app("a", "Maps"), app("b", "Maple"))
        val stats = mapOf("a" to (5 to 1000L), "b" to (5 to 9000L))
        val ranked = rankLibraryApps(apps, "map", stats)
        assertEquals(listOf("Maple", "Maps"), ranked.map { it.label })
    }

    @Test
    fun rank_bandBeatsCount() {
        // A heavily used word match still loses to a prefix match.
        val apps = listOf(app("a", "Maps"), app("b", "Google Maps"))
        val stats = mapOf("b" to (500 to 9000L))
        val ranked = rankLibraryApps(apps, "map", stats)
        assertEquals("Maps", ranked.first().label)
    }

    // ---------- scrubber letter -> position ----------

    @Test
    fun scrubber_firstLetterAtTop() {
        val letters = ('A'..'Z').toList() + '#'
        assertEquals('A', scrubberLetterForPosition(letters, 0f, 1000f))
        assertEquals('A', scrubberLetterForPosition(letters, -50f, 1000f))
    }

    @Test
    fun scrubber_lastLetterAtBottom() {
        val letters = ('A'..'Z').toList() + '#'
        assertEquals('#', scrubberLetterForPosition(letters, 999f, 1000f))
        assertEquals('#', scrubberLetterForPosition(letters, 5000f, 1000f))
    }

    @Test
    fun scrubber_middleMapsProportionally() {
        val letters = ('A'..'Z').toList() + '#'
        // Halfway down 27 letters: 0.5 * 27 = 13.5 -> index 13 -> 'N'
        // (0.5 is exact in float, so no boundary-rounding flakiness).
        assertEquals('N', scrubberLetterForPosition(letters, 135f, 270f))
    }

    @Test
    fun scrubber_emptyOrZeroHeightIsNull() {
        assertNull(scrubberLetterForPosition(emptyList(), 10f, 100f))
        assertNull(scrubberLetterForPosition(listOf('A'), 10f, 0f))
    }

    @Test
    fun scrubber_nearestAvailableFallsBack() {
        val available = listOf('A', 'M', 'Z')
        assertEquals('M', nearestAvailableLetter('M', available))
        // 'B' is closest to 'A' (distance 1 vs 11 to 'M').
        assertEquals('A', nearestAvailableLetter('B', available))
        // '#' sorts after 'Z': nearest to '#' is 'Z'.
        assertEquals('Z', nearestAvailableLetter('#', available))
        assertNull(nearestAvailableLetter('A', emptyList()))
    }

    // ---------- section-header grouping ----------

    @Test
    fun grouping_lettersOrderedWithHashLast() {
        val apps = listOf(
            app("b", "Banana"),
            app("n", "123 Notes"),
            app("a", "apple"),
            app("c", "Cherry"),
        )
        val groups = groupAppsByLetter(apps)
        assertEquals(listOf('A', 'B', 'C', '#'), groups.map { it.first })
        assertEquals(listOf("apple"), groups[0].second.map { it.label })
        assertEquals(listOf("123 Notes"), groups[3].second.map { it.label })
    }

    @Test
    fun grouping_headerIndexCountsHeadersPlusRows() {
        val apps = listOf(
            app("a1", "Apple"), app("a2", "Apricot"),
            app("b1", "Banana"),
        )
        val groups = groupAppsByLetter(apps)
        // Single column: A header at 0, B header at 1 + 2 = 3.
        assertEquals(mapOf('A' to 0, 'B' to 3), buildSectionHeaderIndex(groups, 1))
        // Two columns: A header at 0, B header at 1 + ceil(2/2) = 2.
        assertEquals(mapOf('A' to 0, 'B' to 2), buildSectionHeaderIndex(groups, 2))
    }

    @Test
    fun grouping_headerIndexThreeInOneSection() {
        val apps = listOf(
            app("a1", "Apple"), app("a2", "Apricot"), app("a3", "Avocado"),
            app("b1", "Banana"),
        )
        val groups = groupAppsByLetter(apps)
        // Two columns: B header at 1 + ceil(3/2) = 3.
        assertEquals(mapOf('A' to 0, 'B' to 3), buildSectionHeaderIndex(groups, 2))
    }

    // ---------- "New" dot ----------

    @Test
    fun newDot_neverOpenedAndRecentlyInstalled() {
        val now = 1_000_000_000_000L
        assertTrue(isNewApp(app("a", "Alpha", now - dayMs), emptyMap(), now))
    }

    @Test
    fun newDot_openedAppIsNotNew() {
        val now = 1_000_000_000_000L
        val stats = mapOf("a" to (1 to now))
        assertFalse(isNewApp(app("a", "Alpha", now - dayMs), stats, now))
    }

    @Test
    fun newDot_oldNeverOpenedIsNotNew() {
        val now = 1_000_000_000_000L
        assertFalse(isNewApp(app("a", "Alpha", now - 60 * dayMs), emptyMap(), now))
    }

    @Test
    fun newDot_unknownInstallTimeIsNotNew() {
        val now = 1_000_000_000_000L
        assertFalse(isNewApp(app("a", "Alpha", 0L), emptyMap(), now))
    }

    // ---------- drag payload round-trip ----------

    @Test
    fun dragPayload_roundTrip() {
        val payload = LibraryDragPayload("com.x/.Y#0", "com.x", ".Y", "My App")
        assertEquals(payload, decodeLibraryDragPayload(encodeLibraryDragPayload(payload)))
    }

    @Test
    fun dragPayload_malformedIsNull() {
        assertNull(decodeLibraryDragPayload("only-one-field"))
        assertNull(decodeLibraryDragPayload("a\t\tc\td"))
        assertNull(decodeLibraryDragPayload(""))
    }

    @Test
    fun dragPayload_labelTabsSanitized() {
        val payload = LibraryDragPayload("k", "p", "c", "A\tB\nC")
        val decoded = decodeLibraryDragPayload(encodeLibraryDragPayload(payload))
        assertEquals("A B C", decoded?.label)
    }

    // ---------- LibraryView keys ----------

    @Test
    fun libraryView_fromKey() {
        assertEquals(LibraryView.ALL, LibraryView.fromKey("all"))
        assertEquals(LibraryView.CATEGORIES, LibraryView.fromKey("categories"))
        assertEquals(LibraryView.LIST, LibraryView.fromKey("list"))
        assertEquals(LibraryView.ALL, LibraryView.fromKey("bogus"))
        assertEquals(LibraryView.ALL, LibraryView.fromKey(null))
        assertEquals(LIBRARY_DEFAULT_VIEW_KEY, "libraryDefaultView")
    }
}
