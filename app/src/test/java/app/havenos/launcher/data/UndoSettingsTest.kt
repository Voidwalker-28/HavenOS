package app.havenos.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 3.1 Phase A9: shared undo timeline — grid-override frames,
 * toolbar labels, density precedence, and the v7→v8 migration pin.
 * Written, not run (sandbox test executor broken, pre-existing).
 */
class UndoSettingsTest {

    // GridOverrideFrame codec ------------------------------------------

    @Test
    fun gridFrame_roundTrip_restore() {
        val frame = GridOverrideFrame(true, 8, 6)
        assertEquals(frame, decodeGridOverrideFrame(frame.encode()))
        assertEquals("L:8:6", frame.encode())
    }

    @Test
    fun gridFrame_roundTrip_clear() {
        val frame = GridOverrideFrame(false, -1, -1)
        assertEquals(frame, decodeGridOverrideFrame(frame.encode()))
        assertEquals("P:-1:-1", frame.encode())
    }

    @Test
    fun gridFrame_decode_blankAndMalformed_null() {
        assertNull(decodeGridOverrideFrame(""))
        assertNull(decodeGridOverrideFrame("L:8"))
        assertNull(decodeGridOverrideFrame("X:8:6"))
        assertNull(decodeGridOverrideFrame("L:eight:6"))
    }

    // gridActionForCurrentFrame ----------------------------------------

    @Test
    fun gridAction_nullUndone_null() {
        assertNull(gridActionForCurrentFrame(null, 8, 6, -1, -1))
    }

    @Test
    fun gridAction_undoneRestore_currentCleared_returnsClear() {
        // Undoing a Follow-density step: current settings have the
        // override cleared, so the redo frame must clear it again.
        assertEquals(
            GridOverrideFrame(true, -1, -1),
            gridActionForCurrentFrame(GridOverrideFrame(true, 8, 6), -1, -1, -1, -1),
        )
    }

    @Test
    fun gridAction_undoneRestore_currentSet_returnsCurrent() {
        // Redoing back: current settings hold the restored override.
        assertEquals(
            GridOverrideFrame(false, 5, 7),
            gridActionForCurrentFrame(GridOverrideFrame(false, 8, 6), -1, -1, 5, 7),
        )
    }

    @Test
    fun gridAction_undoneClear_currentSet_returnsRestore() {
        assertEquals(
            GridOverrideFrame(true, 8, 6),
            gridActionForCurrentFrame(GridOverrideFrame(true, -1, -1), 8, 6, -1, -1),
        )
    }

    // UndoStack labels ---------------------------------------------------

    @Test
    fun undoStack_labels_followFrames() {
        val stack = UndoStack()
        val layout = LayoutState()
        stack.push(UndoFrame(layout, LayoutProfiles()))
        assertNull(stack.undoLabel)
        stack.push(UndoFrame(layout, LayoutProfiles(), label = "density grid reset"))
        assertEquals("density grid reset", stack.undoLabel)
        // LayoutStore.undo() builds the redo frame with the undone
        // step's label, so Redo names the same change.
        val current = UndoFrame(layout, LayoutProfiles(), label = "density grid reset")
        val (prev, _) = stack.popUndo(current)!!
        assertEquals("density grid reset", prev.label)
        assertEquals("density grid reset", stack.redoLabel)
        assertNull(stack.undoLabel)
    }

    // Density precedence: per-page override beats global ---------------

    @Test
    fun gridCells_overrideBeatsDensity() {
        val overridden = SettingsState(density = 40, colsLand = 8, rowsLand = 6)
        assertEquals(8 to 6, overridden.gridCells(true))
        val plain = SettingsState(density = 40)
        // Density formula, land: 4 + (40*6+50)/100 = 6 cols,
        // 3 + (40*5+50)/100 = 5 rows; port: 4 cols, 7 rows.
        assertEquals(6 to 5, plain.gridCells(true))
        assertEquals(4 to 7, plain.gridCells(false))
        // Density changes never clear overrides: the override still
        // wins at a different density.
        assertEquals(8 to 6, overridden.copy(density = 80).gridCells(true))
    }

    // Migration pin ------------------------------------------------------

    @Test
    fun migration78_addsExactlyGridJsonColumn() {
        val sql = migration78Sql()
        assertEquals(1, sql.size)
        assertEquals(
            "ALTER TABLE `undo_entries` ADD COLUMN `gridJson` TEXT NOT NULL DEFAULT ''",
            sql[0],
        )
    }
}
