package app.havenos.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 3.1.1 (item 3): crash-loop pure helpers, JUnit4.
 * Written, not run (sandbox test executor broken + test tree does not
 * compile, pre-existing — see the hotfix report for the exact status).
 */
class CrashLoopTest {

    @Test
    fun isCrashLoop_twoWithin60s_isTrue() {
        assertTrue(isCrashLoop(listOf(1000L, 31_000L)))
        // Exactly at the boundary counts as within the window.
        assertTrue(isCrashLoop(listOf(1000L, 61_000L)))
    }

    @Test
    fun isCrashLoop_twoBeyond60s_isFalse() {
        assertFalse(isCrashLoop(listOf(1000L, 62_000L)))
    }

    @Test
    fun isCrashLoop_singleOrEmpty_isFalse() {
        assertFalse(isCrashLoop(listOf(1000L)))
        assertFalse(isCrashLoop(emptyList()))
    }

    @Test
    fun isCrashLoop_unsortedInput_isSortedFirst() {
        assertTrue(isCrashLoop(listOf(61_000L, 1000L)))
        assertFalse(isCrashLoop(listOf(62_000L, 1000L)))
    }

    @Test
    fun isCrashLoop_laterPairTriggers() {
        // First pair is 61s apart, second pair 10s apart.
        assertTrue(isCrashLoop(listOf(1000L, 62_000L, 72_000L)))
    }

    @Test
    fun pruneCrashMarkers_keepsNewest20Sorted() {
        val markers = (1..25).map { it * 1000L }.reversed()
        val pruned = pruneCrashMarkers(markers)
        assertEquals(20, pruned.size)
        assertEquals((6..25).map { it * 1000L }, pruned)
    }

    @Test
    fun pruneCrashMarkers_fewerThan20_unchangedSorted() {
        assertEquals(
            listOf(1000L, 2000L, 3000L),
            pruneCrashMarkers(listOf(3000L, 1000L, 2000L)),
        )
    }
}
