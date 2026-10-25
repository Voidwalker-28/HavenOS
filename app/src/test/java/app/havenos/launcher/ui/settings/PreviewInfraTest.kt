package app.havenos.launcher.ui.settings

import app.havenos.launcher.data.SettingsState
import app.havenos.launcher.ui.speedDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Preview infrastructure tests. Written, NOT RUN: the sandbox test
 * executor crashes (pre-existing).
 *
 * M6 note: the M5 test-pad / demo / tier helpers this file used to
 * cover (classifyHomeGesture, actionForTrigger, matchKind,
 * scaledDurationMs, PreviewTier) were deleted with the old preview
 * infrastructure (PreviewTestPads / PreviewDemo / PreviewTiers). The
 * surviving equivalents are kept here: SettingsState draft copies
 * (the DraftLayer mechanism) and speedDuration (the M6 successor of
 * the demo speed scaler — now the one mapping every real transition
 * uses, see ui/MotionSpeed.kt).
 */
class PreviewInfraTest {

    // ---------- draft copy (DraftLayer) ----------

    @Test
    fun draftCopy_leavesLiveStateUntouched() {
        val live = SettingsState()
        val draft = live.copy(density = 80, iconShape = 3)
        assertEquals(40, live.density)
        assertEquals(1, live.iconShape)
        assertEquals(80, draft.density)
        assertEquals(3, draft.iconShape)
        assertNotEquals(live, draft)
    }

    @Test
    fun draftCopy_sameValues_equal() {
        val live = SettingsState(density = 55)
        assertEquals(live, live.copy())
    }

    // ---------- motion speed mapping (ui/MotionSpeed.kt) ----------

    @Test
    fun speedDuration_speeds() {
        // animSpeed: 0 Off, 1 Fast, 2 Normal, 3 Slow.
        assertEquals(0, speedDuration(1000, 0))
        assertEquals(500, speedDuration(1000, 1))
        assertEquals(1000, speedDuration(1000, 2))
        assertEquals(2000, speedDuration(1000, 3))
    }

    @Test
    fun speedDuration_unknownSpeedIsNormal() {
        assertEquals(300, speedDuration(300, 99))
        assertEquals(300, speedDuration(300, -1))
    }
}
