package app.havenos.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Worker D (M9) tests: transactions, undo, lifecycle, grid plans, sanitation. */
class LayoutStateTest {

    private fun appItem(key: String, col: Int = 0, row: Int = 0, page: Int = 0) =
        HomeItem("app:$key", ItemKind.APP, page, col, row, 2, 2, key)

    // ------------------------------------------------------------- undo

    @Test
    fun undoStack_capsAt50AndRedoClearsOnPush() {
        val stack = UndoStack()
        val base = LayoutState()
        repeat(55) { stack.push(base.copy(dock = listOf("d$it"))) }
        assertEquals(50, stack.undoDepth)
        assertTrue(stack.canUndo)
        val (restored, _) = stack.popUndo(base)!!
        assertTrue(stack.canRedo)
        stack.push(base)
        assertFalse(stack.canRedo)
        assertEquals(50, stack.undoDepth)
        assertTrue(restored.dock.isNotEmpty())
    }

    @Test
    fun undoStack_emptyPopReturnsNull() {
        val stack = UndoStack()
        assertEquals(null, stack.popUndo(LayoutState()))
        assertEquals(null, stack.popRedo(LayoutState()))
        assertFalse(stack.canUndo)
    }

    // -------------------------------------------------------------- dock

    @Test
    fun dragToDock_insertsAndShifts() {
        val state = LayoutState(dock = listOf("a", "b", "c"))
        val result = applyDragToDock(state, "x", 1, 0, WindowClass.MEDIUM, 6)
        val committed = result as TxResult.Committed
        assertEquals(listOf("a", "x", "b", "c"), committed.state.dock)
    }

    @Test
    fun dragToDock_fullRefusesWithDockIsFull() {
        val state = LayoutState(dock = listOf("a", "b", "c", "d", "e", "f"))
        val result = applyDragToDock(state, "x", 0, 0, WindowClass.MEDIUM, 6)
        assertTrue(result is TxResult.Rejected)
        assertEquals("Dock is full", (result as TxResult.Rejected).toast)
    }

    @Test
    fun dragToDock_compactSideDockCapsAtFive() {
        val state = LayoutState(dock = listOf("a", "b", "c", "d", "e"))
        val result = applyDragToDock(state, "x", 0, 1, WindowClass.COMPACT, 6)
        assertEquals("Dock is full", (result as TxResult.Rejected).toast)
        // Same dockMax on a bottom dock still has room.
        val ok = applyDragToDock(state, "x", 0, 0, WindowClass.COMPACT, 6)
        assertTrue(ok is TxResult.Committed)
    }

    @Test
    fun dragToDock_reorderDoesNotDuplicate() {
        val state = LayoutState(dock = listOf("a", "b", "c"))
        val committed = applyDragToDock(state, "b", 0, 0, WindowClass.MEDIUM, 6) as TxResult.Committed
        assertEquals(listOf("b", "a", "c"), committed.state.dock)
    }

    @Test
    fun moveItem_lockedLayoutRejects() {
        val state = LayoutState(items = listOf(appItem("a")))
        val result = applyMoveItem(state, "app:a", 0, 2, 2, lockLayout = true)
        assertEquals("Layout is locked", (result as TxResult.Rejected).toast)
    }

    @Test
    fun moveItem_missingItemRejectsWithoutCorruption() {
        val state = LayoutState(items = listOf(appItem("a")))
        val result = applyMoveItem(state, "app:gone", 0, 2, 2)
        assertTrue(result is TxResult.Rejected)
        assertEquals(1, state.items.size)
    }

    // --------------------------------------------------------- lifecycle

    @Test
    fun appInstalled_placesOnFirstFreeSlotOnlyWhenAutoAdd() {
        val state = LayoutState(items = listOf(appItem("a", 0, 0)))
        val (placed, wasPlaced) = applyAppInstalled(state, "b", true, 8, 12)
        assertTrue(wasPlaced)
        assertEquals(2, placed.items.size)
        val (notPlaced, wasNotPlaced) = applyAppInstalled(state, "c", false, 8, 12)
        assertFalse(wasNotPlaced)
        assertEquals(state, notPlaced)
    }

    @Test
    fun appInstalled_hiddenAppNeverPlaced() {
        val state = LayoutState(hidden = setOf("b"))
        val (next, placed) = applyAppInstalled(state, "b", true, 8, 12)
        assertFalse(placed)
        assertTrue(next.items.isEmpty())
    }

    @Test
    fun appRemoved_clearsEverywhereAndDissolvesSingleFolder() {
        val folderItem = HomeItem("folder1", ItemKind.FOLDER, 0, 0, 0, 2, 2, "folder1")
        val state = LayoutState(
            items = listOf(folderItem, appItem("c", 2, 0)),
            folders = listOf(Folder("folder1", "F", encodeMembers(listOf("a", "b")))),
            dock = listOf("b", "c"),
            hidden = setOf("b"),
            recents = listOf("b", "c"),
        )
        val next = applyAppRemoved(state, "b")
        // Folder with one app left dissolves to a plain icon at its place.
        assertTrue(next.folders.isEmpty())
        val dissolved = next.items.firstOrNull { it.payload == "a" }
        assertTrue(dissolved != null)
        assertEquals(ItemKind.APP, dissolved!!.kind)
        assertEquals(0, dissolved.page)
        assertEquals(0, dissolved.col)
        // Dock, hidden, recents cleared of b; c untouched.
        assertEquals(listOf("c"), next.dock)
        assertTrue("b" !in next.hidden)
        assertEquals(listOf("c"), next.recents)
    }

    @Test
    fun appRemoved_emptyFolderDeleted() {
        val folderItem = HomeItem("folder1", ItemKind.FOLDER, 0, 0, 0, 2, 2, "folder1")
        val state = LayoutState(
            items = listOf(folderItem),
            folders = listOf(Folder("folder1", "F", encodeMembers(listOf("a")))),
        )
        val next = applyAppRemoved(state, "a")
        assertTrue(next.items.isEmpty())
        assertTrue(next.folders.isEmpty())
    }

    @Test
    fun appRemoved_multiMemberFolderKeepsContentsMinusApp() {
        val folderItem = HomeItem("folder1", ItemKind.FOLDER, 0, 0, 0, 2, 2, "folder1")
        val state = LayoutState(
            items = listOf(folderItem),
            folders = listOf(Folder("folder1", "F", encodeMembers(listOf("a", "b", "c")))),
        )
        val next = applyAppRemoved(state, "b")
        assertEquals(listOf("a", "c"), parseMembers(next.folders.single().membersJson))
        assertEquals(1, next.items.size) // folder item stays
    }

    @Test
    fun hideApp_movesToHiddenAndRemovesFromLayout() {
        val state = LayoutState(items = listOf(appItem("a")), dock = listOf("a"))
        val next = applyHideApp(state, "a")
        assertTrue(next.items.isEmpty())
        assertTrue(next.dock.isEmpty())
        assertEquals(setOf("a"), next.hidden)
    }

    // -------------------------------------------------------- grid plan

    @Test
    fun gridChangePlan_movesOutOfBoundsAndShrinksWidgets() {
        // Old grid: 5 cells wide (10 units). New grid: 3 cells (6 units).
        val edge = appItem("edge", 8, 0) // col 8..9 no longer fits
        val wide = HomeItem("wide", ItemKind.WEATHER, 0, 0, 2, 8, 4, "w")
        val plan = planGridChange(listOf(edge, wide), 3, 6)
        assertTrue(plan.needsConfirm)
        assertTrue(plan.overflowCount >= 1)
        assertTrue(plan.summaryLine()!!.contains("items will move"))
        // Nothing in the result exceeds the new grid, and nothing dropped.
        assertEquals(2, plan.resultItems.size)
        for (item in plan.resultItems) {
            assertTrue(item.col + item.colSpan <= 6)
            assertTrue(item.row + item.rowSpan <= 12)
        }
        assertTrue(plan.moves.any { it.itemId == "wide" && it.shrunk })
    }

    @Test
    fun gridChangePlan_fittingChangeNeedsNoConfirm() {
        val plan = planGridChange(listOf(appItem("a", 0, 0)), 5, 6)
        assertFalse(plan.needsConfirm)
        assertEquals(null, plan.summaryLine())
    }

    // --------------------------------------------------------- sanitation

    @Test
    fun sanitizeLayout_skipsBadRecordsKeepsRest() {
        val good = appItem("good", 0, 0)
        val blank = HomeItem("", ItemKind.APP, 0, 2, 0, 2, 2, "x")
        val dup = appItem("good", 2, 0)
        val negative = HomeItem("neg", ItemKind.APP, -1, 0, 0, 2, 2, "n")
        val zeroSpan = HomeItem("zero", ItemKind.APP, 0, 4, 0, 0, 2, "z")
        val (kept, _, log) = sanitizeLayout(
            listOf(good, blank, dup, negative, zeroSpan),
            listOf(Folder("", "Bad", "[]"), Folder("f1", "Good", "[]")),
        )
        assertEquals(listOf(good), kept)
        assertEquals(5, log.size) // 4 bad items + 1 blank-id folder
    }

    // -------------------------------------------------------- queue/drag

    @Test
    fun pendingQueue_queuesDuringDragAppliesOnDropInOrder() {
        val queue = PendingChangesQueue()
        val ran = mutableListOf<String>()
        queue.beginDrag()
        queue.submit { ran += "first" }
        queue.submit { ran += "second" }
        assertEquals(2, queue.queuedCount)
        assertTrue(ran.isEmpty())
        val changes = queue.endDrag()
        assertEquals(0, queue.queuedCount)
        changes.forEach { it() }
        assertEquals(listOf("first", "second"), ran)
    }

    @Test
    fun pendingQueue_runsImmediatelyWhenNotDragging() {
        val queue = PendingChangesQueue()
        var ran = false
        queue.submit { ran = true }
        assertTrue(ran)
    }
}
