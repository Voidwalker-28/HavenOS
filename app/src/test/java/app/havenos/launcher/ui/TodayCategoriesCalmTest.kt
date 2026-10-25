package app.havenos.launcher.ui

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M7 Wave 1 (P1) unit tests — pure logic only, no Compose runtime:
 * category assignment, Smart Stack rotation rules, Calm favorites cap
 * (plus the override plumbing and schedule helpers these files expose).
 */
class TodayCategoriesCalmTest {

    // ---------------------------------------------------------- category

    @Test
    fun assignCategory_systemCategoryBeatsKeyword() {
        // A chat-labelled app that the platform calls a game stays a game.
        assertEquals(
            "game",
            assignCategory(ApplicationInfo.CATEGORY_GAME, "Super Chat", "com.example.chat"),
        )
        assertEquals(
            "social",
            assignCategory(ApplicationInfo.CATEGORY_SOCIAL, "Super Game", "com.example.game"),
        )
    }

    @Test
    fun assignCategory_keywordRules() {
        val undefined = ApplicationInfo.CATEGORY_UNDEFINED
        assertEquals("finance", assignCategory(undefined, "My Bank", "com.example.bankapp"))
        assertEquals("finance", assignCategory(undefined, "Wallet", "com.example.wallet"))
        assertEquals("shopping", assignCategory(undefined, "Shop Now", "com.example.shop"))
        assertEquals("health", assignCategory(undefined, "Fit Tracker", "com.example.fit"))
        assertEquals("travel", assignCategory(undefined, "Flight Booker", "com.example.flights"))
        assertEquals("audio", assignCategory(undefined, "Tunes", "com.spotify.music"))
        assertEquals("video", assignCategory(undefined, "Tube Player", "com.example.tube"))
        assertEquals("image", assignCategory(undefined, "Gallery", "com.example.gallery"))
        assertEquals("social", assignCategory(undefined, "Chat App", "com.example.chat"))
        assertEquals("maps", assignCategory(undefined, "Maps", "com.example.maps"))
        assertEquals("news", assignCategory(undefined, "Daily News", "com.example.news"))
        assertEquals("productivity", assignCategory(undefined, "Notes", "com.example.notes"))
        assertEquals("tools", assignCategory(undefined, "Calculator", "com.example.calc"))
    }

    @Test
    fun assignCategory_unknownFallsBackToOther() {
        assertEquals(
            "other",
            assignCategory(ApplicationInfo.CATEGORY_UNDEFINED, "Zqxw", "com.example.zqxw"),
        )
    }

    @Test
    fun assignCategory_overrideBeatsSystemAndKeyword() {
        val undefined = ApplicationInfo.CATEGORY_UNDEFINED
        val overrides = mapOf("com.example.bankapp/MainActivity#0" to "tools")
        assertEquals(
            "tools",
            assignCategoryWithOverrides(
                undefined, "My Bank", "com.example.bankapp",
                "com.example.bankapp/MainActivity#0", overrides,
            ),
        )
        // An app with no override keeps its computed category.
        assertEquals(
            "finance",
            assignCategoryWithOverrides(
                undefined, "My Bank", "com.example.bankapp",
                "com.example.other/MainActivity#0", overrides,
            ),
        )
        // An override also beats a system category.
        assertEquals(
            "tools",
            assignCategoryWithOverrides(
                ApplicationInfo.CATEGORY_GAME, "Game", "com.example.game",
                "com.example.game/MainActivity#0",
                mapOf("com.example.game/MainActivity#0" to "tools"),
            ),
        )
    }

    @Test
    fun categoryOverrides_moveAndRenameRoundTrip() {
        val moved = categoryOverridesWithMove("", "appKey1", "finance")
        val parsed = parseCategoryOverrides(moved)
        assertEquals("finance", parsed.moves["appKey1"])
        // Re-encoding the parsed form is stable.
        assertEquals(parsed, parseCategoryOverrides(encodeCategoryOverrides(parsed)))

        val renamed = categoryOverridesWithRename(moved, "finance", "Money")
        val parsed2 = parseCategoryOverrides(renamed)
        assertEquals("Money", parsed2.names["finance"])
        assertEquals("Money", categoryDisplayName("finance", parsed2))
        // Blank rename clears back to the default name.
        val cleared = categoryOverridesWithRename(renamed, "finance", "  ")
        assertEquals("Finance", categoryDisplayName("finance", parseCategoryOverrides(cleared)))
        // Malformed input never throws and yields empty overrides.
        assertEquals(CategoryOverrides(), parseCategoryOverrides("{not json"))
        assertEquals(CategoryOverrides(), parseCategoryOverrides(""))
    }

    // -------------------------------------------------------- smart stack

    @Test
    fun autoRotatePreferredCard_timeOfDayMapping() {
        assertEquals(TodayCard.CLOCK_WEATHER, autoRotatePreferredCard(0))
        assertEquals(TodayCard.CLOCK_WEATHER, autoRotatePreferredCard(4))
        assertEquals(TodayCard.CLOCK_WEATHER, autoRotatePreferredCard(5))
        assertEquals(TodayCard.CLOCK_WEATHER, autoRotatePreferredCard(11))
        assertEquals(TodayCard.CALENDAR, autoRotatePreferredCard(12))
        assertEquals(TodayCard.CALENDAR, autoRotatePreferredCard(17))
        assertEquals(TodayCard.SUGGESTED, autoRotatePreferredCard(18))
        assertEquals(TodayCard.SUGGESTED, autoRotatePreferredCard(23))
    }

    @Test
    fun smartStack_nextAndPrevWrap() {
        assertEquals(1, smartStackNext(0, 4))
        assertEquals(0, smartStackNext(3, 4))
        assertEquals(3, smartStackPrev(0, 4))
        assertEquals(2, smartStackPrev(3, 4))
        assertEquals(0, smartStackNext(0, 0))
    }

    @Test
    fun smartStack_pauseWindowIsTenSeconds() {
        val now = 1_000_000L
        assertFalse(isSmartStackPaused(0L, now))
        assertTrue(isSmartStackPaused(now - 9_999L, now))
        assertFalse(isSmartStackPaused(now - 10_000L, now))
        assertFalse(isSmartStackPaused(now - 60_000L, now))
    }

    @Test
    fun todayColumnWidth_clampedTo280_420() {
        assertEquals(280f, todayColumnWidthDp(500f), 0.01f)
        assertEquals(340f, todayColumnWidthDp(1000f), 0.01f)
        assertEquals(420f, todayColumnWidthDp(2000f), 0.01f)
        assertEquals(280f, todayColumnWidthDp(823f), 0.01f)
    }

    // -------------------------------------------------------------- calm

    @Test
    fun calmFavorites_cappedAtEightPreservingOrder() {
        val keys = (1..10).map { "app$it" }
        val capped = calmFavoritesCapped(keys)
        assertEquals(8, capped.size)
        assertEquals((1..8).map { "app$it" }, capped)
        assertEquals(listOf("a", "b"), calmFavoritesCapped(listOf("a", "b")))
    }

    @Test
    fun calmFavorites_parseEncodeRoundTrip() {
        val json = encodeCalmFavorites(listOf("k1", "k2"))
        assertEquals(listOf("k1", "k2"), parseCalmFavorites(json))
        assertEquals(emptyList<String>(), parseCalmFavorites(""))
        assertEquals(emptyList<String>(), parseCalmFavorites("{bad"))
    }

    @Test
    fun calmSchedule_activeSameDayAndOvernight() {
        // Monday=2 … Friday=6, 22:00–23:00.
        val evening = CalmSchedule(
            enabled = true,
            days = setOf(2, 3, 4, 5, 6),
            startMinutes = 22 * 60,
            endMinutes = 23 * 60,
        )
        assertTrue(evening.isActiveAt(3, 22 * 60 + 30)) // Wed 22:30
        assertFalse(evening.isActiveAt(3, 21 * 60 + 59))
        assertFalse(evening.isActiveAt(7, 22 * 60 + 30)) // Sat: not a day
        assertFalse(evening.copy(enabled = false).isActiveAt(3, 22 * 60 + 30))
        assertFalse(evening.copy(days = emptySet()).isActiveAt(3, 22 * 60 + 30))

        // Overnight 22:00–07:00 wraps past midnight.
        val night = CalmSchedule(
            enabled = true,
            days = setOf(3),
            startMinutes = 22 * 60,
            endMinutes = 7 * 60,
        )
        assertTrue(night.isActiveAt(3, 23 * 60))
        assertTrue(night.isActiveAt(3, 2 * 60))
        assertFalse(night.isActiveAt(3, 12 * 60))

        // Encode / parse round-trip preserves the schedule.
        assertEquals(evening, parseCalmSchedule(encodeCalmSchedule(evening)))
        assertEquals(CalmSchedule(), parseCalmSchedule("{bad"))
    }
}
