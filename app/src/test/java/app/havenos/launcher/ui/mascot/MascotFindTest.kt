package app.havenos.launcher.ui.mascot

import app.havenos.launcher.ui.LibraryApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HavenOS v3 Track A — Find matcher fixtures (MascotFind.kt).
 * NOTE: written but never run in this sandbox — the Gradle test
 * executor is broken here (known pre-existing baseline).
 */
class MascotFindTest {

    private fun app(key: String, label: String) = LibraryApp(key, label, "com.example.$key")

    private val apps = listOf(
        app("camera", "Camera"),
        app("gallery", "Gallery"),
        app("messages", "Messages"),
        app("maps", "Google Maps"),
        app("settings", "Settings"),
        app("chrome", "Chrome"),
    )

    @Test
    fun match_exactBeatsPrefixBeatsToken() {
        val ranked = matchApps(apps, "Camera", emptyMap())
        assertEquals("camera", (ranked[0].target as FindTarget.App).key)
        assertEquals(FindBand.EXACT, ranked[0].band)
        val prefix = matchApps(apps, "Cam", emptyMap())
        assertEquals("camera", (prefix[0].target as FindTarget.App).key)
        assertEquals(FindBand.PREFIX, prefix[0].band)
        val token = matchApps(apps, "Maps", emptyMap())
        assertEquals("maps", (token[0].target as FindTarget.App).key)
        assertTrue(token[0].band <= FindBand.WORD)
    }

    @Test
    fun match_synonymFindsCamera() {
        val ranked = matchApps(apps, "photo", emptyMap())
        assertTrue(ranked.any { (it.target as FindTarget.App).key == "camera" })
        val camera = ranked.first { (it.target as FindTarget.App).key == "camera" }
        assertEquals(FindBand.SYNONYM, camera.band)
    }

    @Test
    fun match_typoTolerance() {
        // Transposition: "camrea" is 2 edits from "camera".
        val ranked = matchApps(apps, "camrea", emptyMap())
        assertTrue(ranked.any { (it.target as FindTarget.App).key == "camera" })
    }

    @Test
    fun match_emptyAndLongAndNonLatin() {
        assertTrue(matchApps(apps, "", emptyMap()).isEmpty())
        assertTrue(matchApps(apps, "   ", emptyMap()).isEmpty())
        assertTrue(matchApps(apps, "x".repeat(500), emptyMap()).isEmpty())
        // Non-Latin query: no crash, and no false match on Latin labels.
        val nonLatin = matchApps(apps, "カメラ", emptyMap())
        assertTrue(nonLatin.none { (it.target as FindTarget.App).key == "camera" })
        // A non-Latin label matches its own non-Latin query.
        val hindi = listOf(app("hindi", "कैमरा"))
        assertTrue(matchApps(hindi, "कैमरा", emptyMap()).isNotEmpty())
    }

    @Test
    fun match_diacriticsStripped() {
        val accented = listOf(app("cafe", "Café"))
        assertTrue(matchApps(accented, "cafe", emptyMap()).isNotEmpty())
    }

    @Test
    fun decide_singleActsTiesOfferChoices() {
        val single = rankFind(apps, "Camera", emptyMap())
        val decisionSingle = decideFind(single)
        assertTrue(decisionSingle is FindDecision.Act)
        // "a" is a prefix/word hit for several labels: choices, capped at 4.
        val many = rankFind(apps, "a", emptyMap())
        val decisionMany = decideFind(many)
        assertTrue(decisionMany is FindDecision.Choices)
        assertTrue((decisionMany as FindDecision.Choices).candidates.size <= 4)
        assertTrue(decideFind(emptyList()) is FindDecision.NoMatch)
    }

    @Test
    fun decide_strictlyBestFirstActs() {
        // 3.1 A11 flow 14: a strictly-best first result acts at once,
        // even with other candidates — only true ties offer choices.
        val best = FindCandidate(FindTarget.App("a", "Camera", "com.a"), FindBand.EXACT, 0.98)
        val second = FindCandidate(FindTarget.App("b", "Cam", "com.b"), FindBand.PREFIX, 0.92)
        val decision = decideFind(listOf(best, second))
        assertTrue(decision is FindDecision.Act)
        assertEquals("a", ((decision as FindDecision.Act).candidate.target as FindTarget.App).key)
    }

    @Test
    fun match_settingsAndActions() {
        val settings = matchSettings("accent")
        assertTrue(settings.any { (it.target as FindTarget.Setting).route == "look" })
        val actions = matchActions("add widget")
        assertTrue(actions.any { (it.target as FindTarget.Action).id == "add-widget" })
        assertTrue(matchSettings("zzz-no-such-setting").isEmpty())
    }

    @Test
    fun normalize_collapsesPunctuation() {
        assertEquals("hello world", normalizeFindText("  Hello,   World! "))
        assertEquals("cafe", normalizeFindText("café"))
    }
}
