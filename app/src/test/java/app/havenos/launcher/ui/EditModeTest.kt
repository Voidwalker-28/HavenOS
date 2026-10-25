package app.havenos.launcher.ui

import app.havenos.launcher.data.Folder
import app.havenos.launcher.data.HomeItem
import app.havenos.launcher.data.ItemKind
import app.havenos.launcher.data.encodeMembers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M9 edit-mode pure logic (EditMode.kt + Grid.kt helpers it layers on).
 * Coordinates are HALF-CELL units (DB v2): an app icon is 2x2, the test
 * grid below is 8x12 units (4x6 cells).
 */
class EditModeTest {

    private fun app(id: String, key: String, col: Int, row: Int, page: Int = 0) =
        HomeItem(id, ItemKind.APP, page, col, row, 2, 2, key)

    private fun widget(id: String, col: Int, row: Int, w: Int = 4, h: Int = 4, page: Int = 0) =
        HomeItem(id, ItemKind.WEATHER, page, col, row, w, h, "weather")

    private fun folderItem(id: String, folderId: String, col: Int, row: Int, page: Int = 0) =
        HomeItem(id, ItemKind.FOLDER, page, col, row, 2, 2, folderId)

    private fun noOverlaps(items: List<HomeItem>): Boolean {
        for (a in items) {
            for (b in items) {
                if (a.id != b.id && a.page == b.page && rectOf(a).overlaps(rectOf(b))) {
                    return false
                }
            }
        }
        return true
    }

    // ---------- undo / redo ----------

    @Test
    fun undoRedo_roundTrip() {
        val stack = EditUndoStack()
        assertFalse(stack.canUndo)
        assertFalse(stack.canRedo)
        stack.push("s0")
        stack.push("s1")
        assertEquals("s1", stack.undo("current"))
        assertTrue(stack.canRedo)
        assertEquals("current", stack.redo("s1"))
        assertFalse(stack.canRedo)
        assertEquals("s1", stack.undo("current2"))
    }

    @Test
    fun undo_empty_returnsNull() {
        assertNull(EditUndoStack().undo("x"))
        assertNull(EditUndoStack().redo("x"))
    }

    @Test
    fun undo_cappedAt50_andRedoClearedOnNewPush() {
        val stack = EditUndoStack()
        for (i in 0..60) stack.push("s$i")
        assertEquals(EDIT_UNDO_MAX, stack.depth)
        stack.push("a")
        stack.undo("cur")
        assertTrue(stack.canRedo)
        stack.push("b")
        assertFalse(stack.canRedo)
    }

    // ---------- snap math ----------

    @Test
    fun snapOn_landsOnWholeCells() {
        // Cell = 100px. Finger at 149px -> nearest cell 1 (unit 2);
        // at 151px -> cell 2 (unit 4).
        assertEquals(2, snapUnit(149f, 100f, true, 2, 8))
        assertEquals(4, snapUnit(151f, 100f, true, 2, 8))
        assertTrue(isCellAligned(snapUnit(149f, 100f, true, 2, 8)))
    }

    @Test
    fun snapOff_allowsHalfCellSteps() {
        // Finger at 50px (half a cell) -> unit 1 with snap off, unit 0/2 with snap on.
        assertEquals(1, snapUnit(50f, 100f, false, 2, 8))
        assertFalse(isCellAligned(snapUnit(50f, 100f, false, 2, 8)))
    }

    @Test
    fun snapUnit_clampsToGrid() {
        assertEquals(0, snapUnit(-50f, 100f, true, 2, 8))
        assertEquals(6, snapUnit(999f, 100f, true, 2, 8))
        assertEquals(6, snapUnit(999f, 100f, false, 2, 8))
    }

    // ---------- row-by-row shift ----------

    @Test
    fun shiftRowByRow_makesRoomWithoutOverlap() {
        // Row 0 full: a b c d. Drag e onto b's cell.
        val items = listOf(
            app("app:a", "a", 0, 0),
            app("app:b", "b", 2, 0),
            app("app:c", "c", 4, 0),
            app("app:d", "d", 6, 0),
            app("app:e", "e", 0, 2),
        )
        val shifted = shiftRowByRow(items, "app:e", 2, 0, 8, 12)
        assertTrue(shifted != null)
        assertTrue(noOverlaps(shifted!!))
        assertEquals(2 to 0, shifted.first { it.id == "app:e" }.let { it.col to it.row })
        assertEquals(5, shifted.size)
    }

    @Test
    fun shiftRowByRow_fullGrid_permutesWithoutOverlap() {
        val items = (0 until 24).map { i ->
            app("app:$i", "k$i", (i % 4) * 2, (i / 4) * 2)
        }
        // A full grid still shifts: every item lands on a distinct cell
        // (the dragged item's old cell frees up as items move).
        val shifted = shiftRowByRow(items, "app:0", 2, 0, 8, 12)
        assertTrue(shifted != null)
        assertTrue(noOverlaps(shifted!!))
        assertEquals(24, shifted.size)
        assertEquals(2 to 0, shifted.first { it.id == "app:0" }.let { it.col to it.row })
    }

    @Test
    fun shiftRowByRow_outOfBounds_returnsNull() {
        val items = listOf(app("app:a", "a", 0, 0))
        assertNull(shiftRowByRow(items, "app:a", 7, 0, 8, 12))
        assertNull(shiftRowByRow(items, "missing", 0, 0, 8, 12))
    }

    // ---------- §3.3 drop policy ----------

    @Test
    fun dropEdit_earlyRelease_shiftsInsteadOfFolder() {
        val items = listOf(
            app("app:a", "a", 0, 0),
            app("app:b", "b", 2, 0),
            app("app:c", "c", 4, 0),
        )
        val result = resolveDropEdit(items, emptyMap(), "app:c", 0, 0, 8, 12, "f1", holdMs = 0L)
        assertNull(result.createdFolder)
        assertTrue(noOverlaps(result.items))
        assertEquals(0 to 0, result.items.first { it.id == "app:c" }.let { it.col to it.row })
    }

    @Test
    fun dropEdit_longHold_createsFolder() {
        val items = listOf(
            app("app:a", "a", 0, 0),
            app("app:b", "b", 2, 0),
        )
        val result = resolveDropEdit(items, emptyMap(), "app:b", 0, 0, 8, 12, "f1", holdMs = 500L)
        assertTrue(result.createdFolder != null)
        assertEquals(
            listOf("a", "b"),
            app.havenos.launcher.data.parseMembers(result.createdFolder!!.membersJson).sorted(),
        )
    }

    @Test
    fun dropEdit_widgetNeverOverlaps_nearestFreeOrBack() {
        // Widget 4x4 dropped onto an occupied cell: must land on a free
        // 4x4 spot, never overlapping.
        val items = listOf(
            app("app:a", "a", 0, 0),
            app("app:b", "b", 2, 0),
            widget("w:1", 0, 4),
        )
        val result = resolveDropEdit(items, emptyMap(), "w:1", 0, 0, 8, 12, "f1", holdMs = 0L)
        assertTrue(noOverlaps(result.items))
        assertFalse(result.items.first { it.id == "w:1" }.let { it.col == 0 && it.row == 0 })
    }

    @Test
    fun dropEdit_widgetNowhereToFit_staysPut() {
        // 4x4 widget, grid full except its own spot: release -> back.
        val items = mutableListOf(widget("w:1", 4, 4))
        for (r in 0 until 6) {
            for (c in 0 until 4) {
                if (c * 2 >= 4 && c * 2 < 8 && r * 2 >= 4 && r * 2 < 8) continue
                items += app("app:$c-$r", "k$c$r", c * 2, r * 2)
            }
        }
        val result = resolveDropEdit(items, emptyMap(), "w:1", 0, 0, 8, 12, "f1", holdMs = 0L)
        assertEquals(items, result.items)
    }

    @Test
    fun dropEdit_appOntoFolder_addsToFolder() {
        val folder = Folder("f1", "Folder", encodeMembers(listOf("a")))
        val items = listOf(
            app("app:a", "a", 0, 0),
            folderItem("folder:f1", "f1", 2, 0),
            app("app:b", "b", 4, 0),
        )
        val folders = mapOf("f1" to folder)
        val result = resolveDropEdit(items, folders, "app:b", 2, 0, 8, 12, "fx", holdMs = 0L)
        assertTrue(result.updatedFolder != null)
        assertEquals(
            listOf("a", "b"),
            app.havenos.launcher.data.parseMembers(result.updatedFolder!!.membersJson).sorted(),
        )
        assertFalse(result.items.any { it.id == "app:b" })
    }

    // ---------- resize ----------

    @Test
    fun resizeClamp_andChipsStayInGrid() {
        assertEquals(2 to 2, resizeClamp(1, 1, 2, 2, 8, 12))
        assertEquals(8 to 12, resizeClamp(99, 99, 2, 2, 8, 12))
        for ((_, cells) in WIDGET_SIZE_CHIPS) {
            val (w, h) = widgetChipUnits(cells, 8, 12)
            assertTrue(w in 2..8 && h in 2..12)
        }
        assertEquals(4 to 4, widgetChipUnits(2 to 2, 8, 12))
        assertEquals(8 to 4, widgetChipUnits(4 to 2, 8, 12))
    }

    @Test
    fun isResizable_rules() {
        assertTrue(isResizable(widget("w:1", 0, 0)))
        assertFalse(isResizable(app("app:a", "a", 0, 0)))
        assertFalse(isResizable(folderItem("folder:f1", "f1", 0, 0))) // 1x1-cell folder
        assertTrue(
            isResizable(folderItem("folder:f1", "f1", 0, 0).copy(colSpan = 4, rowSpan = 4)),
        )
    }

    @Test
    fun widgetFits_detectsOverlapAndBounds() {
        val items = listOf(app("app:a", "a", 0, 0))
        assertFalse(widgetFits(items, 0, 4, 4, 0, 0, "w:1", 8, 12))
        assertTrue(widgetFits(items, 0, 4, 4, 4, 0, "w:1", 8, 12))
        assertFalse(widgetFits(items, 0, 4, 4, 6, 0, "w:1", 8, 12)) // overflows grid
        assertTrue(widgetFits(items, 0, 2, 2, 0, 0, "app:a", 8, 12)) // self excluded
    }

    // ---------- adding ----------

    @Test
    fun firstFreeSpot_currentPageFirstThenOthers() {
        val items = listOf(
            app("app:a", "a", 0, 0, page = 1),
            app("app:b", "b", 2, 0, page = 1),
        )
        // Current page 0 is empty -> first spot is page 0.
        assertEquals(Triple(0, 0, 0), firstFreeSpotAcrossPages(items, listOf(0, 1), 2, 2, 8, 12))
        // Current page 1, first free 2x2 there is (4, 0).
        assertEquals(Triple(1, 4, 0), firstFreeSpotAcrossPages(items, listOf(1, 0), 2, 2, 8, 12))
    }

    @Test
    fun firstFreeSpot_fullPages_returnsNull() {
        val items = (0 until 24).map { i ->
            app("app:$i", "k$i", (i % 4) * 2, (i / 4) * 2)
        }
        assertNull(firstFreeSpotAcrossPages(items, listOf(0), 2, 2, 8, 12))
    }

    @Test
    fun duplicateWidget_freshIdSameContent() {
        val original = widget("w:1", 0, 0)
        val copy = duplicateWidget(original, "w:2")
        assertEquals("w:2", copy.id)
        assertEquals(original.kind, copy.kind)
        assertEquals(original.colSpan, copy.colSpan)
        assertEquals(original.payload, copy.payload)
    }
}
