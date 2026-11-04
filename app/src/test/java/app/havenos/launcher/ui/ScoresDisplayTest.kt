package app.havenos.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 3.1 Phase A1: scores display model — nullable fields, status-code
 * table, never render "null" or a dash pair.
 *
 * NOTE: written-but-not-run — the sandbox test executor is broken
 * (pre-existing) and SettingsShellTest has pre-existing compile errors.
 * JSON-based parse tests follow the ApiReviewFixTest precedent.
 */
class ScoresDisplayTest {

    private fun match(
        home: String = "India",
        away: String = "Australia",
        homeScore: String = "",
        awayScore: String = "",
        status: String = "upcoming",
        time: String = "",
        competition: String = "Test Series",
    ) = ScoreMatch(home, away, homeScore, awayScore, status, "", time, competition)

    @Test
    fun parse_nullScoresKeptBlankAndBothTeamsNullDropped() {
        val json = """{"sport":"cricket","matches":[
            {"home":"India","away":"Australia","home_score":null,"away_score":null,
             "status":"live","status_text":"","time":"","competition":"Test"},
            {"home":null,"away":null,"home_score":"1","away_score":"2",
             "status":"finished","status_text":"","time":"","competition":"Test"}
        ]}"""
        val matches = parseScores(json)
        assertEquals(1, matches.size)
        assertEquals("", matches[0].homeScore)
        assertEquals("", matches[0].awayScore)
    }

    @Test
    fun display_oneNullTeamShowsPresentTeamOnly() {
        val row = match(home = "", away = "England").toDisplay()!!
        assertEquals("England", row.teams)
        assertFalse(row.teams.contains("null", ignoreCase = true))
    }

    @Test
    fun display_bothTeamsMissingIsDropped() {
        assertNull(match(home = "", away = "").toDisplay())
    }

    @Test
    fun display_unknownStatusShowsNoStatusText() {
        // "Abnormal" is deliberately unmapped: its meaning is not
        // documented, so it must not appear on screen.
        val row = match(status = "abnormal").toDisplay()!!
        assertEquals("", row.right)
        val row2 = match(status = "weird-code").toDisplay()!!
        assertEquals("", row2.right)
    }

    @Test
    fun display_liveMatchShowsProviderScores() {
        val row = match(homeScore = "245/6", awayScore = "240/8", status = "live").toDisplay()!!
        assertEquals("India vs Australia", row.teams)
        assertEquals("245/6 – 240/8", row.right)
        assertEquals("Test Series", row.competition)
    }

    @Test
    fun display_finishedMatchShowsScores() {
        val row = match(homeScore = "2", awayScore = "1", status = "finished").toDisplay()!!
        assertEquals("2 – 1", row.right)
    }

    @Test
    fun display_upcomingMatchShowsVsAndLocalTime() {
        val row = match(status = "upcoming", time = "2026-10-30T14:30:00Z").toDisplay()!!
        assertTrue(row.teams.contains(" vs "))
        // Zone-independent: a HH:mm time is shown (converted to the
        // device zone), not a status word, and no "null" leaks.
        assertTrue(row.right.matches(Regex("\\d{2}:\\d{2}")))
        assertFalse(row.right.contains("Not started"))
    }

    @Test
    fun display_upcomingWithoutTimeShowsNotStarted() {
        val row = match(status = "upcoming", time = "").toDisplay()!!
        assertEquals("Not started", row.right)
    }

    @Test
    fun display_dashScoresShowStatusOnly() {
        // Live cricket pre-score "-" must not render as "- – -".
        val row = match(homeScore = "-", awayScore = "-", status = "live").toDisplay()!!
        assertEquals("Live", row.right)
    }

    @Test
    fun display_literalNullScoresShowStatusOnly() {
        val row = match(homeScore = "null", awayScore = "null", status = "finished").toDisplay()!!
        assertEquals("Finished", row.right)
    }

    @Test
    fun display_missingScoresOnLiveShowStatusOnly() {
        val row = match(status = "live").toDisplay()!!
        assertEquals("Live", row.right)
    }

    @Test
    fun display_noRenderedStringContainsNull() {
        val fixtures = listOf(
            match(homeScore = "null", awayScore = "null", status = "live"),
            match(home = "", away = "England"),
            match(homeScore = "-", awayScore = "-", status = "finished"),
            match(status = "abnormal"),
        )
        for (m in fixtures) {
            val row = m.toDisplay() ?: continue
            assertFalse(row.teams.contains("null", ignoreCase = true))
            assertFalse(row.right.contains("null", ignoreCase = true))
            assertFalse(row.competition.contains("null", ignoreCase = true))
        }
    }

    @Test
    fun statusTable_mapsKnownCodesOnly() {
        assertEquals("Not started", SCORE_STATUS_LABELS["upcoming"])
        assertEquals("Live", SCORE_STATUS_LABELS["live"])
        assertEquals("Finished", SCORE_STATUS_LABELS["finished"])
        assertEquals("Delayed", SCORE_STATUS_LABELS["delayed"])
        assertNull(SCORE_STATUS_LABELS["abnormal"])
    }
}
