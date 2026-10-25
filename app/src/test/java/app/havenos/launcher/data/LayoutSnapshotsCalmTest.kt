package app.havenos.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Worker D (M9) tests: snapshot policy (Doc 3 §3.6) + Calm editor store (Doc 1 §5.9). */
class LayoutSnapshotsCalmTest {

    private fun record(day: Long, hour: Int, seq: Int = 0): SnapshotRecord {
        val ms = day * 86_400_000L + hour * 3_600_000L + seq
        return SnapshotRecord(ms, ms, 2, 5, 1, "{}")
    }

    @Test
    fun snapshotThrottle_atMostOncePerHour() {
        assertTrue(shouldAutoSnapshot(null, 1_000L))
        assertFalse(shouldAutoSnapshot(1_000L, 1_000L + 59 * 60_000L))
        assertTrue(shouldAutoSnapshot(1_000L, 1_000L + 60 * 60_000L))
    }

    @Test
    fun pruneSnapshots_keepsLast10PlusOnePerDayFor7Days() {
        val now = 100L * 86_400_000L
        // 15 snapshots over the last 3 days + one 6 days old + one 8 days old.
        val records = mutableListOf<SnapshotRecord>()
        for (day in 98L..100L) {
            for (hour in listOf(8, 12, 18, 22, 23)) records += record(day, hour)
        }
        records += record(94L, 10) // 6 days old: kept as that day's latest
        records += record(92L, 10) // 8 days old, outside last-10 and 7-day window: pruned
        val kept = pruneSnapshots(records, now)
        assertTrue(kept.any { it.createdAtMs == record(94L, 10).createdAtMs })
        assertFalse(kept.any { it.createdAtMs == record(92L, 10).createdAtMs })
        assertTrue(kept.size <= 10 + 7)
        // The newest 10 are always kept.
        val newest10 = records.sortedByDescending { it.createdAtMs }.take(10)
        for (r in newest10) assertTrue(r.id in kept.map { it.id }.toSet())
    }

    @Test
    fun snapshotRecord_countsPagesAndItems() {
        val items = listOf(
            HomeItem("a", ItemKind.APP, 0, 0, 0, 2, 2, "a"),
            HomeItem("b", ItemKind.APP, 2, 0, 0, 2, 2, "b"),
        )
        val rec = buildSnapshotRecord(5_000L, items, emptyList(), "{}")
        assertEquals(3, rec.pageCount) // pages 0..2
        assertEquals(2, rec.itemCount)
        assertEquals("3 pages · 2 items · 0 folders", rec.toPreview(listOf("a", "b")).summaryLine())
    }

    @Test
    fun calmEditor_capAtEightAndNoDuplicates() {
        var state = CalmEditorState()
        for (i in 1..10) state = state.withAdded("app$i")
        assertEquals(8, state.favorites.size)
        assertTrue(state.isFull)
        val dup = state.withAdded("app1")
        assertEquals(state, dup)
        assertTrue(state.validate().isEmpty())
    }

    @Test
    fun calmEditor_reorderAndRemove() {
        val state = CalmEditorState(favorites = listOf("a", "b", "c"))
        assertEquals(listOf("b", "a", "c"), state.withMoved(1, 0).favorites)
        assertEquals(listOf("a", "c"), state.withRemoved("b").favorites)
        assertEquals(state, state.withMoved(0, 99)) // out of range: no-op
    }

    @Test
    fun calmEditor_scheduleOvernightWindow() {
        val state = CalmEditorState(
            scheduleEnabled = true,
            days = setOf(2), // Monday
            startMinutes = 22 * 60,
            endMinutes = 6 * 60,
        )
        assertTrue(state.isScheduleActiveAt(2, 23 * 60))
        assertFalse(state.isScheduleActiveAt(2, 10 * 60))
        assertFalse(state.isScheduleActiveAt(3, 3 * 60)) // day not selected
        val off = state.copy(scheduleEnabled = false)
        assertFalse(off.isScheduleActiveAt(2, 23 * 60))
    }

    @Test
    fun calmEditor_encodeDecodeRoundTrip() {
        val state = CalmEditorState(
            favorites = listOf("x", "y"),
            scheduleEnabled = true,
            days = setOf(1, 6),
            startMinutes = 540,
            endMinutes = 1020,
            entry = CalmEntryOptions(autoEnter = false, dimPct = 40),
        )
        val restored = calmEditorStateFromKeys(
            state.encodeFavorites(),
            state.encodeSchedule(),
            state.entry.encode(),
        )
        assertEquals(state, restored)
    }

    @Test
    fun calmEditor_malformedInputYieldsDefaults() {
        assertEquals(CalmEditorState(), calmEditorStateFromKeys("{bad", "[bad", "{bad"))
        assertTrue(parseCalmEditorFavorites("[]").isEmpty())
    }

    @Test
    fun calmEditor_validationCatchesBadState() {
        val tooMany = CalmEditorState(favorites = (1..9).map { "a$it" })
        assertTrue(tooMany.validate().isNotEmpty())
        val badDay = CalmEditorState(days = setOf(9))
        assertTrue(badDay.validate().isNotEmpty())
    }
}
