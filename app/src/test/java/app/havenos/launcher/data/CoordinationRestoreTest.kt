package app.havenos.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coordination fix step 6: Restore previews — first-page items from a
 * snapshot, the preview payload contract, and the thumbnail gate
 * (zero renderable items fall back to the counts-only row).
 * Written before the production code (TDD).
 *
 * NOTE: written-but-not-run — the sandbox test executor is broken
 * (pre-existing) and SettingsShellTest has pre-existing compile errors.
 */
class CoordinationRestoreTest {

    private fun item(id: String, page: Int) = HomeItem(
        id = id, kind = ItemKind.APP, page = page, col = 0, row = 0,
        colSpan = 2, rowSpan = 2, payload = "pkg/$id",
    )

    @Test
    fun firstPagePreviewItems_keepsOnlyPageZeroInOrder() {
        val items = listOf(item("a", 0), item("b", 1), item("c", 0), item("d", 2))
        assertEquals(listOf("a", "c"), firstPagePreviewItems(items).map { it.id })
    }

    @Test
    fun snapshotPreview_carriesIdsAndCountsLine() {
        val preview = SnapshotRecord(
            id = 1L, createdAtMs = 1L, pageCount = 3, itemCount = 28, folderCount = 2,
            layoutJson = "{}",
        ).toPreview(listOf("a", "b"))
        assertEquals(listOf("a", "b"), preview.itemIds)
        assertEquals("3 pages · 28 items · 2 folders", preview.summaryLine())
    }

    @Test
    fun showRestoreThumbnail_requiresIdsAndFirstPageItems() {
        val withIds = SnapshotPreview(1L, 1L, 1, 1, 0, listOf("a"))
        val noIds = SnapshotPreview(1L, 1L, 1, 0, 0, emptyList())
        assertTrue(showRestoreThumbnail(withIds, listOf(item("a", 0))))
        assertFalse(showRestoreThumbnail(noIds, listOf(item("a", 0))))
        assertFalse(showRestoreThumbnail(withIds, emptyList()))
    }
}
