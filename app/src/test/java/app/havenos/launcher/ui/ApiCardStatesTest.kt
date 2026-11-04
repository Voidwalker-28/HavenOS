package app.havenos.launcher.ui

import app.havenos.launcher.data.network.ApiFailureClass
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 3.1 Phase A2/A3: failure-class → card line mapping, and the
 * crypto card's three states + deep-link destination.
 *
 * NOTE: written-but-not-run — the sandbox test executor is broken
 * (pre-existing) and SettingsShellTest has pre-existing compile errors.
 */
class ApiCardStatesTest {

    @Test
    fun failureLine_offlineWithCacheShowsUpdated() {
        assertEquals(
            FailureLine.Updated(123L),
            failureLineFor(ApiFailureClass.OFFLINE, hasData = true, lastUpdatedMs = 123L),
        )
    }

    @Test
    fun failureLine_offlineWithoutCacheShowsOfflineEmpty() {
        assertEquals(
            FailureLine.OfflineEmpty,
            failureLineFor(ApiFailureClass.OFFLINE, hasData = false, lastUpdatedMs = 0L),
        )
    }

    @Test
    fun failureLine_rateLimitedShowsBusy() {
        assertEquals(
            FailureLine.Busy,
            failureLineFor(ApiFailureClass.RATE_LIMITED, hasData = true, lastUpdatedMs = 1L),
        )
        assertEquals(
            FailureLine.Busy,
            failureLineFor(ApiFailureClass.RATE_LIMITED, hasData = false, lastUpdatedMs = 0L),
        )
    }

    @Test
    fun failureLine_everyOtherClassShowsCouldNotLoad() {
        for (cls in listOf(
            ApiFailureClass.NETWORK_ERROR,
            ApiFailureClass.NOT_FOUND,
            ApiFailureClass.PARSE_ERROR,
            ApiFailureClass.SERVER_ERROR,
            ApiFailureClass.OTHER,
        )) {
            assertEquals(
                FailureLine.CouldNotLoad,
                failureLineFor(cls, hasData = true, lastUpdatedMs = 1L),
            )
        }
    }

    @Test
    fun failureLine_nullFallsThroughToAgeStatus() {
        assertEquals(
            FailureLine.None,
            failureLineFor(null, hasData = true, lastUpdatedMs = 1L),
        )
    }

    @Test
    fun cryptoCard_threeStates() {
        assertEquals(CryptoCardState.HIDDEN, cryptoCardState(apiCryptoOn = false, keyBlank = true))
        assertEquals(CryptoCardState.HIDDEN, cryptoCardState(apiCryptoOn = false, keyBlank = false))
        assertEquals(CryptoCardState.KEY_NEEDED, cryptoCardState(apiCryptoOn = true, keyBlank = true))
        assertEquals(CryptoCardState.PRICES, cryptoCardState(apiCryptoOn = true, keyBlank = false))
    }

    @Test
    fun cryptoAddKey_deepLinksToApisKeyField() {
        val target = cryptoAddKeyTarget()
        assertEquals("apis", target.route)
        assertEquals("coingecko-key", target.field)
    }
}
