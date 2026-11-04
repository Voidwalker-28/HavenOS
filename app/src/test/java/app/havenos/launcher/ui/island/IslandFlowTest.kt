package app.havenos.launcher.ui.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 3.1 Phase A11 flow 15: island feeds → pill state.
 * Written, not run (sandbox test executor broken, pre-existing).
 */
class IslandFlowTest {

    private val burst = IslandActivity.Notifications(3, emptyList())
    private val media = IslandActivity.Media("Song", "Artist", true, null)
    private val charging = IslandActivity.Charging(80)

    @Test
    fun pill_liveBurstBeatsMediaAndCharging() {
        val pill = pickIslandActivity(burst, 1000L, 0L, media, charging, 5000L)
        assertTrue(pill is IslandActivity.Notifications)
        assertEquals(3, (pill as IslandActivity.Notifications).total)
    }

    @Test
    fun pill_expiredBurstFallsBackToMedia() {
        // Window is 6s: at exactly +6000ms the burst is dead, but
        // 1ms earlier it is still live (strict < on the window).
        assertEquals(
            media,
            pickIslandActivity(burst, 1000L, 0L, media, charging, 7000L),
        )
        assertEquals(
            burst,
            pickIslandActivity(burst, 1000L, 0L, media, charging, 6999L),
        )
    }

    @Test
    fun pill_dismissedBurstDoesNotReappear() {
        // Burst older than (or equal to) the dismissed one stays dead,
        // even inside the window; a later burst re-arms (first test).
        assertEquals(
            media,
            pickIslandActivity(burst, 1000L, 1000L, media, charging, 1500L),
        )
    }

    @Test
    fun pill_noBurst_mediaThenCharging() {
        assertEquals(media, pickIslandActivity(null, 0L, 0L, media, charging, 0L))
        assertEquals(charging, pickIslandActivity(null, 0L, 0L, null, charging, 0L))
    }

    @Test
    fun pill_zeroTotalBurst_isNoBurst() {
        assertEquals(
            charging,
            pickIslandActivity(
                IslandActivity.Notifications(0, emptyList()),
                1000L, 0L, null, charging, 1500L,
            ),
        )
    }

    @Test
    fun pill_nothingLive_null() {
        assertNull(pickIslandActivity(null, 0L, 0L, null, null, 0L))
    }

    @Test
    fun priority_notificationThenMediaThenCharging() {
        assertEquals(0, islandPriority(burst))
        assertEquals(1, islandPriority(media))
        assertEquals(2, islandPriority(charging))
    }
}
