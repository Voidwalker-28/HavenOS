package app.havenos.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 3.1 Phase A13: paused-service detection.
 * Written, not run (sandbox test executor broken, pre-existing).
 * The launch restore itself needs Settings.Global + a Settings
 * store, so it is verified by the Phase G checklist, not here.
 */
class NavBarSafetyTest {

    @Test
    fun paused_onlyWhenWantedAndGrantedButDisconnected() {
        assertTrue(gestureServicePaused(true, true, false))
    }

    @Test
    fun notPaused_whenConnected() {
        assertFalse(gestureServicePaused(true, true, true))
    }

    @Test
    fun notPaused_whenPermissionMissing() {
        // Permission-missing has its own banner ("Allow"), not Reconnect.
        assertFalse(gestureServicePaused(true, false, false))
        assertFalse(gestureServicePaused(true, false, true))
    }

    @Test
    fun notPaused_whenGesturesOff() {
        assertFalse(gestureServicePaused(false, true, false))
    }
}
