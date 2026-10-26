package app.havenos.launcher.ui

import app.havenos.launcher.data.network.ApiHosts
import app.havenos.launcher.data.network.ApiId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v2 M1 review-fix tests: pure functions only (no Android framework).
 * [parseScores] and [parseCurrencyQuery] are top-level functions in
 * this package; [ApiHosts] is the network allowlist.
 *
 * NOTE: execution is unverified in this sandbox (Gradle test executor
 * broken + pre-existing SettingsShellTest errors); this file was only
 * checked for parse/compile shape, not run.
 */
class ApiReviewFixTest {

    // ---------- allowlist: exact host match only ----------

    @Test
    fun allowlist_acceptsListedHost() {
        assertTrue(ApiHosts.allows(ApiId.QUOTE, "api.quotable.io"))
    }

    @Test
    fun allowlist_acceptsListedHostCaseInsensitively() {
        assertTrue(ApiHosts.allows(ApiId.QUOTE, "API.QUOTABLE.IO"))
    }

    @Test
    fun allowlist_rejectsWrongHost() {
        assertTrue(!ApiHosts.allows(ApiId.QUOTE, "evil.example.com"))
        assertTrue(!ApiHosts.allows(ApiId.QUOTE, "api.github.com"))
    }

    @Test
    fun allowlist_rejectsSubdomainOfListedHost() {
        // Exact match only: a subdomain is NOT the listed host.
        assertTrue(!ApiHosts.allows(ApiId.QUOTE, "sub.api.quotable.io"))
        assertTrue(!ApiHosts.allows(ApiId.QUOTE, "api.quotable.io.evil.example.com"))
    }

    // ---------- parseScores: verified live shape vs malformed ----------

    @Test
    fun parseScores_liveShape() {
        val json = """{"sport":"cricket","count":1,"matches":[
            {"home":"India","away":"Australia",
             "home_score":"250","away_score":"240",
             "status":"live","status_text":"Live",
             "time":"2026-10-26T10:00:00Z",
             "competition":"Test Series","competition_logo":"",
             "home_logo":"","away_logo":"","url":"/match/1"}]}"""
        val matches = parseScores(json)
        assertEquals(1, matches.size)
        val m = matches[0]
        assertEquals("India", m.home)
        assertEquals("Australia", m.away)
        assertEquals("250", m.homeScore)
        assertEquals("240", m.awayScore)
        assertEquals("live", m.status)
        assertEquals("Live", m.statusText)
        assertEquals("2026-10-26T10:00:00Z", m.time)
        assertEquals("Test Series", m.competition)
    }

    @Test
    fun parseScores_liveShapeNullScoresKeptBlank() {
        // Live cricket matches report "-" or null while no numeric
        // score exists yet; optString on a null yields "" here.
        val json = """{"sport":"cricket","count":1,"matches":[
            {"home":"India","away":"Australia",
             "home_score":null,"away_score":null,
             "status":"upcoming","status_text":"",
             "time":"2026-10-27T10:00:00Z","competition":""}]}"""
        val matches = parseScores(json)
        assertEquals(1, matches.size)
        assertEquals("", matches[0].homeScore)
        assertEquals("", matches[0].awayScore)
    }

    @Test
    fun parseScores_malformedShapeIsEmptyNotThrow() {
        assertTrue(parseScores("not json at all").isEmpty())
        assertTrue(parseScores("").isEmpty())
        assertTrue(parseScores("""{"sport":"cricket","count":0}""").isEmpty())
        assertTrue(parseScores("""{"matches":"not-an-array"}""").isEmpty())
        assertTrue(parseScores("""[{"home":"","away":""}]""").isEmpty())
    }

    // ---------- parseCurrencyQuery: documented edge cases ----------

    @Test
    fun parseCurrencyQuery_acceptsDocumentedForms() {
        val plain = parseCurrencyQuery("100 USD to INR")
        assertEquals(100.0, plain!!.amount, 0.0)
        assertEquals("USD", plain.from)
        assertEquals("INR", plain.to)

        val lowerIn = parseCurrencyQuery("100 usd in inr")
        assertEquals("USD", lowerIn!!.from)
        assertEquals("INR", lowerIn.to)

        val dollar = parseCurrencyQuery("\$100 USD to INR")
        assertEquals(100.0, dollar!!.amount, 0.0)

        val padded = parseCurrencyQuery("  2.5 EUR to GBP  ")
        assertEquals(2.5, padded!!.amount, 0.0)
        assertEquals("EUR", padded.from)
        assertEquals("GBP", padded.to)
    }

    @Test
    fun parseCurrencyQuery_rejectsDocumentedForms() {
        // Missing source code.
        assertNull(parseCurrencyQuery("\$100 to INR"))
        // Extra words.
        assertNull(parseCurrencyQuery("100 USD to INR extra"))
        assertNull(parseCurrencyQuery("convert 100 USD to INR"))
        // Non-numeric amount.
        assertNull(parseCurrencyQuery("abc USD to INR"))
        // Zero or negative amounts.
        assertNull(parseCurrencyQuery("0 USD to INR"))
        assertNull(parseCurrencyQuery("-5 USD to INR"))
        // Code that is not exactly 3 letters.
        assertNull(parseCurrencyQuery("100 US to INR"))
        assertNull(parseCurrencyQuery("100 USDD to INR"))
        assertNull(parseCurrencyQuery("100 USD to IN"))
        assertNull(parseCurrencyQuery("100 U5D to INR"))
        // Missing separator / codes.
        assertNull(parseCurrencyQuery("100 USD INR"))
        assertNull(parseCurrencyQuery("USD to INR"))
    }
}
