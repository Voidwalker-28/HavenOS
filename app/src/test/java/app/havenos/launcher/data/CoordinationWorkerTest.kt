package app.havenos.launcher.data

import app.havenos.launcher.data.network.ApiCacheRow
import app.havenos.launcher.data.network.ApiId
import app.havenos.launcher.data.network.CardStatus
import app.havenos.launcher.data.network.cardStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coordination fix step 7: worker failure surface — card status
 * derived from cache age alone (failure counter is diagnostics only).
 * Written before the production code (TDD).
 *
 * NOTE: written-but-not-run — the sandbox test executor is broken
 * (pre-existing) and SettingsShellTest has pre-existing compile errors.
 */
class CoordinationWorkerTest {

    private val now = 1_800_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun row(updatedAtMs: Long) = ApiCacheRow("quote", "{}", updatedAtMs)

    @Test
    fun cardStatus_freshAtAndBelowMaxAge() {
        assertEquals(CardStatus.Fresh, cardStatus(ApiId.QUOTE, row(now - day), now))
        assertEquals(CardStatus.Fresh, cardStatus(ApiId.QUOTE, row(now - day / 2), now))
    }

    @Test
    fun cardStatus_staleBetweenOneAndTwoMaxAges() {
        assertEquals(CardStatus.Stale, cardStatus(ApiId.QUOTE, row(now - day - 1), now))
        assertEquals(CardStatus.Stale, cardStatus(ApiId.QUOTE, row(now - 2 * day + 1), now))
    }

    @Test
    fun cardStatus_expiredAtAndBeyondTwiceMaxAge() {
        assertEquals(CardStatus.Expired(now - 2 * day), cardStatus(ApiId.QUOTE, row(now - 2 * day), now))
        assertEquals(CardStatus.Expired(now - 5 * day), cardStatus(ApiId.QUOTE, row(now - 5 * day), now))
    }

    @Test
    fun cardStatus_emptyOfflineWithoutRow() {
        assertEquals(CardStatus.EmptyOffline, cardStatus(ApiId.QUOTE, null, now))
    }

    @Test
    fun cardStatus_dictionaryNeverExpires() {
        // maxAgeMs(DICTIONARY) is Long.MAX_VALUE, so the ratio can never reach 2.
        assertEquals(CardStatus.Fresh, cardStatus(ApiId.DICTIONARY, row(now - 365 * day), now))
    }

    @Test
    fun cardStatus_negativeAgeIsFreshByTheRule() {
        assertTrue(cardStatus(ApiId.QUOTE, row(now + 1000), now) is CardStatus.Fresh)
    }
}
