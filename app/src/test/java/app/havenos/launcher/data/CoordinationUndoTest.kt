package app.havenos.launcher.data

import app.havenos.launcher.ui.parseFullSnapshot
import app.havenos.launcher.ui.snapshotJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coordination fix step 5: durable undo — full LayoutState snapshot
 * format (dock, hidden, Today cards, recents, not just items/folders),
 * old-format forward compatibility, the Room 5→6 migration SQL pin,
 * and the frame-carrying undo stack semantics.
 * Written before the production code (TDD).
 *
 * NOTE: written-but-not-run — the sandbox test executor is broken
 * (pre-existing) and SettingsShellTest has pre-existing compile errors.
 */
class CoordinationUndoTest {

    private fun sampleState() = LayoutState(
        items = listOf(
            HomeItem(id = "app:a/b", kind = ItemKind.APP, page = 0, col = 0, row = 0, colSpan = 2, rowSpan = 2, payload = "a/b"),
        ),
        folders = listOf(Folder(id = "f1", name = "Tools", membersJson = "[\"a/b\"]")),
        dock = listOf("a/b", "c/d"),
        todayCards = listOf(TodayCard(id = "weather", kind = 0)),
        hidden = setOf("x/y"),
        recents = listOf("a/b"),
    )

    @Test
    fun fullSnapshot_roundTripsDockHiddenTodayRecents() {
        val state = sampleState()
        val json = snapshotJson(
            state.items, state.folders,
            dock = state.dock, hidden = state.hidden,
            todayCards = state.todayCards, recents = state.recents,
        )
        assertEquals(state, parseFullSnapshot(json, LayoutState()))
    }

    @Test
    fun fullSnapshot_oldFormatDefaultsToCurrent() {
        val state = sampleState()
        val current = LayoutState(dock = listOf("keep/dock"), hidden = setOf("keep/hidden"))
        val oldJson = snapshotJson(state.items, state.folders)
        val parsed = parseFullSnapshot(oldJson, current)
        assertEquals(state.items, parsed.items)
        assertEquals(state.folders, parsed.folders)
        assertEquals(current.dock, parsed.dock)
        assertEquals(current.hidden, parsed.hidden)
        assertEquals(current.todayCards, parsed.todayCards)
        assertEquals(current.recents, parsed.recents)
    }

    @Test
    fun migration56Sql_pinsNewTablesAndColumn() {
        val sql = migration56Sql().joinToString("\n")
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `undo_entries`"))
        assertTrue(sql.contains("`seq` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
        assertTrue(sql.contains("`direction` TEXT NOT NULL"))
        assertTrue(sql.contains("`layoutJson` TEXT NOT NULL"))
        assertTrue(sql.contains("`profilesJson` TEXT NOT NULL"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `profile_dirty`"))
        assertTrue(sql.contains("ALTER TABLE `api_cache` ADD COLUMN `consecutiveFailures` INTEGER NOT NULL DEFAULT 0"))
    }

    @Test
    fun undoStack_framesPushClearsRedoAndCapsAt50() {
        val stack = UndoStack()
        val a = UndoFrame(LayoutState(dock = listOf("a")), LayoutProfiles())
        val b = UndoFrame(LayoutState(dock = listOf("b")), LayoutProfiles())
        stack.push(a)
        stack.push(b)
        assertEquals(2, stack.undoDepth)
        val (prev, _) = stack.popUndo(UndoFrame(LayoutState(), LayoutProfiles()))!!
        assertEquals(b.layout.dock, prev.layout.dock)
        assertEquals(1, stack.undoDepth)
        assertEquals(1, stack.redoDepth)
        // A new push clears redo.
        stack.push(UndoFrame(LayoutState(dock = listOf("c")), LayoutProfiles()))
        assertEquals(0, stack.redoDepth)
        // Cap: 51 pushes keep 50.
        repeat(51) { stack.push(UndoFrame(LayoutState(dock = listOf("d$it")), LayoutProfiles())) }
        assertEquals(50, stack.undoDepth)
    }
}
