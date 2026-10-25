package app.havenos.launcher.ui.settings.shell

import app.havenos.launcher.data.SettingsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M6 integration tests. Written, NOT RUN: the sandbox test executor
 * crashes (pre-existing).
 *
 * No settings migration is tested here because none happened in M6:
 * no store key changed, was renamed, or changed meaning (see
 * docs/COVERAGE.md M6), so there is no migration to verify.
 */
class M6IntegrationTest {

    // ---------- session diff (pure part) ----------

    @Test
    fun diff_emptyWhenNothingChanged() {
        val snapshot = SettingsState()
        assertTrue(sessionDiffFields(snapshot, SettingsState()).isEmpty())
    }

    @Test
    fun diff_oneRecordPerChangedFieldWithReadableText() {
        val snapshot = SettingsState()
        val current = snapshot.copy(darkMode = 2, dockPos = 1, animSpeed = 3)
        val keys = sessionDiffFields(snapshot, current).map { it.key }
        assertEquals(listOf("Dark mode", "Dock position", "Motion speed"), keys)
        val mode = sessionDiffFields(snapshot, current).first { it.key == "Dark mode" }
        assertEquals("System", mode.oldText)
        assertEquals("Dark", mode.newText)
    }

    @Test
    fun diff_groupedControlsProduceOneRecord() {
        // Accent mode + colour are one control, so one record.
        val snapshot = SettingsState()
        val current = snapshot.copy(accentMode = 1, accentArgb = 0xFF112233.toInt())
        val changes = sessionDiffFields(snapshot, current)
        assertEquals(1, changes.size)
        assertEquals("Accent", changes.first().key)
    }

    @Test
    fun diff_gridPortraitAndLandscapeAreSeparateRecords() {
        val snapshot = SettingsState()
        val current = snapshot.copy(colsPort = 4, rowsPort = 6, colsLand = 5, rowsLand = 6)
        val keys = sessionDiffFields(snapshot, current).map { it.key }
        assertEquals(listOf("Grid portrait", "Grid landscape"), keys)
    }

    // ---------- change log / undo (PreviewSession) ----------

    @Test
    fun session_recordUndoAndRevert() = runBlocking {
        val session = PreviewSession()
        var value = 0
        session.record(ChangeRecord("Density", "40", "55", revert = { value = 40 }))
        session.record(ChangeRecord("Dark mode", "System", "Dark", revert = { value = 99 }))
        assertEquals(2, session.changes.size)
        assertTrue(session.undoLast())
        assertEquals(99, value)
        assertEquals(1, session.changes.size)
        val remaining = session.changes.first()
        assertTrue(session.revertOne(remaining))
        assertEquals(40, value)
        assertTrue(session.changes.isEmpty())
    }

    @Test
    fun session_replaceChangesRebuildsFromSnapshotDiff() {
        val session = PreviewSession()
        session.record(ChangeRecord("Stale", "a", "b", revert = {}))
        session.replaceChanges(
            sessionDiffFields(SettingsState(), SettingsState(darkMode = 1))
                .map { ChangeRecord(it.key, it.oldText, it.newText, null) },
        )
        assertEquals(1, session.changes.size)
        assertEquals("Dark mode", session.changes.first().key)
    }
}
