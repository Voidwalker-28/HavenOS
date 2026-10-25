package app.havenos.launcher.data

/**
 * Worker D (M9) — layout snapshots + restore, Doc 3 §3.6.
 *
 * - Auto-snapshot on edit-mode exit, at most once per hour.
 * - Keep the last 10, plus one per day for 7 days. Local only, no secrets
 *   (a snapshot holds item ids/kinds/positions, folder names, dock keys
 *   and the page/item counts — never credentials, tokens, or file paths).
 * - Restore: edit-mode overflow ⋮ -> "Restore layout…" shows a list
 *   (date, page + item counts) with a PREVIEW of the home before
 *   applying. Preview is [SnapshotPreview]: the host renders the
 *   snapshot's items through its normal home renderer in a preview
 *   surface; applying replaces the live layout in one transaction.
 * - Lock layout blocks accidental drags: the flag lives in
 *   SettingsState.lockLayout and is enforced in LayoutState.kt
 *   ([checkEditAllowed]); Worker A owns the toast.
 *
 * Persistence: Room table layout_snapshots (Db.kt, schema v4). The
 * policy functions here are pure over [SnapshotRecord] lists so the
 * retention math is unit-testable without a database.
 */

/** One stored snapshot (Room row shape, minus the JSON payload). */
data class SnapshotRecord(
    val id: Long, // createdAt ms; also the primary key
    val createdAtMs: Long,
    val pageCount: Int,
    val itemCount: Int,
    val folderCount: Int,
    val layoutJson: String,
)

/** Restore-list entry: date + page/item counts + preview payload. */
data class SnapshotPreview(
    val id: Long,
    val createdAtMs: Long,
    val pageCount: Int,
    val itemCount: Int,
    val folderCount: Int,
    /** Item ids in snapshot order, for the preview renderer. */
    val itemIds: List<String>,
) {
    /** "12 Oct, 14:32 · 3 pages · 28 items" style line (host formats the date). */
    fun summaryLine(): String = "$pageCount pages · $itemCount items · $folderCount folders"
}

fun SnapshotRecord.toPreview(itemIds: List<String>): SnapshotPreview =
    SnapshotPreview(id, createdAtMs, pageCount, itemCount, folderCount, itemIds)

/** Auto-snapshot throttle: at most once per hour (Doc 3 §3.6). */
const val SNAPSHOT_MIN_INTERVAL_MS: Long = 3_600_000L

/** True when an auto-snapshot may be taken now. */
fun shouldAutoSnapshot(lastSnapshotMs: Long?, nowMs: Long): Boolean =
    lastSnapshotMs == null || nowMs - lastSnapshotMs >= SNAPSHOT_MIN_INTERVAL_MS

/**
 * Retention (Doc 3 §3.6): keep the last 10 snapshots, plus one per day
 * for 7 days (the latest snapshot of each of the last 7 days, even when
 * it falls outside the last 10). Everything older is pruned. The day
 * key is the UTC epoch day, so retention is timezone-stable.
 */
fun pruneSnapshots(records: List<SnapshotRecord>, nowMs: Long): List<SnapshotRecord> {
    if (records.isEmpty()) return emptyList()
    val sorted = records.sortedByDescending { it.createdAtMs }
    val keep = LinkedHashSet<Long>()
    for (r in sorted.take(10)) keep += r.id
    val sevenDaysAgo = nowMs - 7L * 86_400_000L
    val byDay = sorted.filter { it.createdAtMs >= sevenDaysAgo }
        .groupBy { it.createdAtMs / 86_400_000L }
    for ((_, dayRecords) in byDay) {
        keep += dayRecords.maxByOrNull { it.createdAtMs }!!.id
    }
    return sorted.filter { it.id in keep }
}

/**
 * Build a snapshot record from a live layout. [layoutJson] is the
 * existing snapshotJson(items, folders) format (Grid.kt) so restore
 * reuses parseSnapshot — no second format to keep in sync.
 */
fun buildSnapshotRecord(
    nowMs: Long,
    items: List<HomeItem>,
    folders: List<Folder>,
    layoutJson: String,
): SnapshotRecord = SnapshotRecord(
    id = nowMs,
    createdAtMs = nowMs,
    pageCount = (items.maxOfOrNull { it.page } ?: -1) + 1,
    itemCount = items.size,
    folderCount = folders.size,
    layoutJson = layoutJson,
)
