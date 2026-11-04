package app.havenos.launcher.ui.mascot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HavenOS v3 Track A — mascot engine pure logic (MascotModel.kt).
 * NOTE: written but never run in this sandbox — the Gradle test
 * executor is broken here (known pre-existing baseline). Do not claim
 * these ran until a working executor reports them.
 */
class MascotModelTest {

    @Test
    fun transitions_explicitTable() {
        assertTrue(MascotState.OFF.canTransitionTo(MascotState.IDLE))
        assertFalse(MascotState.OFF.canTransitionTo(MascotState.ROAMING))
        assertTrue(MascotState.IDLE.canTransitionTo(MascotState.GUIDING))
        assertTrue(MascotState.GUIDING.canTransitionTo(MascotState.IDLE))
        assertFalse(MascotState.GUIDING.canTransitionTo(MascotState.ROAMING))
        assertFalse(MascotState.SLEEPING.canTransitionTo(MascotState.GUIDING))
        assertTrue(MascotState.IDLE.canTransitionTo(MascotState.DISMISSED))
        assertTrue(MascotState.DISMISSED.canTransitionTo(MascotState.IDLE))
        assertTrue(MascotState.IDLE.canTransitionTo(MascotState.IDLE))
    }

    @Test
    fun quietHours_overnightWrap() {
        // Default 22:00–07:00.
        assertTrue(isQuietHour(23 * 60, 1320, 420))
        assertTrue(isQuietHour(3 * 60, 1320, 420))
        assertTrue(isQuietHour(6 * 60 + 59, 1320, 420))
        assertFalse(isQuietHour(7 * 60, 1320, 420))
        assertFalse(isQuietHour(12 * 60, 1320, 420))
        assertFalse(isQuietHour(21 * 60 + 59, 1320, 420))
        // Daytime window, no wrap.
        assertTrue(isQuietHour(10 * 60, 540, 1020))
        assertFalse(isQuietHour(18 * 60, 540, 1020))
        // Degenerate window never quiet.
        assertFalse(isQuietHour(12 * 60, 600, 600))
    }

    @Test
    fun shouldSleep_anyTriggerSleeps() {
        assertTrue(shouldSleep(true, false, true, false))
        assertTrue(shouldSleep(false, true, true, false))
        assertTrue(shouldSleep(false, false, false, false))
        assertTrue(shouldSleep(false, false, true, true))
        assertFalse(shouldSleep(false, false, true, false))
    }

    @Test
    fun roamPlanner_mostlyStillAndNeverRepeats() {
        // Stay roll below 0.65 stays put.
        assertEquals(2, nextPerchIndex(2, 5, 0.64f, 0.0f))
        // Move roll picks a different perch, never the current one.
        repeat(100) { i ->
            val pick = (i % 100) / 100f
            val next = nextPerchIndex(2, 5, 0.9f, pick)
            assertTrue(next != 2)
            assertTrue(next in 0 until 5)
        }
        assertEquals(0, nextPerchIndex(0, 1, 0.9f, 0.9f))
        assertEquals(0, nextPerchIndex(0, 0, 0.9f, 0.9f))
    }

    @Test
    fun nudgeLimiter_capsCooldownQuietAndMute() {
        val now = 1_000_000_000L
        assertTrue(canNudge(0, 0L, now, false, false))
        assertFalse(canNudge(1, 0L, now, false, false)) // one per session
        assertFalse(canNudge(0, now - 1000L, now, false, false)) // cooldown
        assertTrue(canNudge(0, now - 86_400_000L, now, false, false)) // day passed
        assertFalse(canNudge(0, 0L, now, true, false)) // muted
        assertFalse(canNudge(0, 0L, now, false, true)) // quiet hours
    }

    @Test
    fun stringList_roundTripAndCorruptFallback() {
        assertEquals(setOf("a", "b"), parseMascotStringList("[\"a\",\"b\"]").toSet())
        assertTrue(parseMascotStringList("not json").isEmpty())
        assertTrue(parseMascotStringList("").isEmpty())
        val encoded = encodeMascotStringList(setOf("b", "a"))
        assertEquals(setOf("a", "b"), parseMascotStringList(encoded).toSet())
    }

    @Test
    fun emotes_neverEmojiFonts() {
        // Every emote is plain typographic text, never an emoji codepoint.
        MascotExpression.values().forEach { expression ->
            val emote = emoteFor(expression) ?: return@forEach
            emote.forEach { ch ->
                assertFalse(
                    "Emote for $expression contains a non-ASCII glyph: $emote",
                    ch.code > 0x2FFF,
                )
            }
        }
    }
}
