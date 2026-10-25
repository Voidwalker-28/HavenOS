package app.havenos.launcher.data

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteFullException
import android.net.Uri
import android.provider.MediaStore
import app.havenos.launcher.ui.parseSnapshot
import app.havenos.launcher.ui.snapshotJson
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Worker D (M9) — the Android half of the state layer.
 *
 * The pure half lives in LayoutState.kt / LayoutProfiles.kt /
 * LayoutCoordination.kt / LayoutSnapshots.kt / CalmEditor.kt; this
 * class is the ONLY writer to Room for layout data, and the single
 * instance other workers use (via HavenApp.layoutStore).
 *
 * API OTHER WORKERS MUST USE:
 * - [undoStack] — the ONE shared undo stack (50 levels, edit mode +
 *   Settings). Worker A: toolbar Undo/Redo call popUndo/popRedo and
 *   persist the returned state with [commit]; push the pre-change
 *   state BEFORE every committed transaction.
 * - [pendingQueue] — submit Settings changes through it during a drag;
 *   they run on [endDrag].
 * - [cachedLayout] — cold-start flow: Room cache first, immediately
 *   (Doc 3 §1 #16). Widgets fill in after; never seed over it.
 * - [commit] — one transaction = one atomic Room write. On
 *   out-of-storage the in-memory state is kept, one retry is scheduled,
 *   and [lastSaveError] becomes "Couldn't save" (Doc 3 §3.5).
 * - [ensureProfile] — first use of a window class derives its profile
 *   by reflow from canonical order, then persists it. After that the
 *   profile keeps its own positions.
 * - [profiles] — the current profile store (read-only flow).
 * - [autoSnapshotOnEditExit] / [snapshots] / [restoreSnapshot] —
 *   Doc 3 §3.6 safety net.
 * - [sanitizeOnStart] — corrupt-record handling (Doc 3 §3.5): skip the
 *   bad record, keep the rest, log to Diagnostics, raise a one-time
 *   toast via [diagnosticsToast].
 * - [debouncedWrite] — slider writes, 200ms after the last change.
 * - [resolveDockPackages] — intent-resolved dock candidates for the
 *   fresh-install seed (Doc 3 §5); no hard-coded packages here.
 *
 * The write path runs on Dispatchers.IO, off the composition path, so
 * dragging stays at 60fps on a mid-range tablet (Doc 1 §7).
 */
class LayoutCoordinator(
    private val appCtx: Context,
    private val db: HavenDb,
    private val settings: Settings,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The shared undo stack (see class doc). */
    val undoStack = UndoStack()

    /** Settings changes queued during a drag (Doc 3 §3.4). */
    val pendingQueue = PendingChangesQueue()

    /** In-memory layout kept across a failed write (Doc 3 §3.5). */
    private val _inMemory = MutableStateFlow(LayoutState())
    val inMemory: StateFlow<LayoutState> = _inMemory

    /** Last save failure toast text, or null when saves are healthy. */
    private val _lastSaveError = MutableStateFlow<String?>(null)
    val lastSaveError: StateFlow<String?> = _lastSaveError

    /** One-time diagnostics toast (corrupt records skipped on start). */
    private val _diagnosticsToast = MutableStateFlow<String?>(null)
    val diagnosticsToast: StateFlow<String?> = _diagnosticsToast

    private val _profiles = MutableStateFlow(LayoutProfiles())
    val profiles: StateFlow<LayoutProfiles> = _profiles

    private val debounceJobs = mutableMapOf<String, Job>()

    init {
        scope.launch { loadProfiles() }
        scope.launch { sanitizeOnStart() }
    }

    /** Cold-start layout: Room cache + folders + dock, as a flow. */
    val cachedLayout: Flow<LayoutState> = combine(
        db.homeItems().observeAll(),
        db.folders().observeAll(),
        settings.state,
    ) { items, folders, state ->
        LayoutState(
            items = items,
            folders = folders,
            dock = state.dock,
            hidden = state.hiddenApps(),
        )
    }

    // ------------------------------------------------------------ commit

    /**
     * Persist one transaction atomically (Room @Transaction replaceAll
     * for items + folders). The dock lives in Settings and is written
     * by the caller through the Settings setters when it changes.
     * Returns false (and keeps the in-memory state + schedules one
     * retry) when storage fails.
     */
    suspend fun commit(next: LayoutState): Boolean {
        _inMemory.value = next
        return try {
            db.homeItems().replaceAll(next.items)
            db.folders().replaceAll(next.folders)
            _lastSaveError.value = null
            true
        } catch (t: Throwable) {
            if (t is SQLiteFullException || t is IOException) {
                _lastSaveError.value = LayerToasts.COULDNT_SAVE
                scope.launch {
                    delay(5_000)
                    runCatching {
                        db.homeItems().replaceAll(_inMemory.value.items)
                        db.folders().replaceAll(_inMemory.value.folders)
                        _lastSaveError.value = null
                    }
                }
            }
            false
        }
    }

    // ----------------------------------------------------------- profiles

    /** Load persisted profiles into [_profiles] (empty on first run). */
    suspend fun loadProfiles() {
        val rows = try {
            db.layoutProfiles().all()
        } catch (t: Throwable) {
            emptyList()
        }
        val byClass = mutableMapOf<WindowClass, MutableMap<String, ProfilePlacement>>()
        for (row in rows) {
            val windowClass = try {
                WindowClass.valueOf(row.windowClass)
            } catch (t: Throwable) {
                continue
            }
            byClass.getOrPut(windowClass) { mutableMapOf() }[row.itemId] =
                ProfilePlacement(row.page, row.col, row.row, row.colSpan, row.rowSpan)
        }
        val canonical = canonicalOrderOf(
            try {
                db.homeItems().all()
            } catch (t: Throwable) {
                emptyList()
            },
        )
        _profiles.value = LayoutProfiles(canonical, byClass)
    }

    private suspend fun saveProfiles(profiles: LayoutProfiles) {
        val rows = profiles.profiles.flatMap { (windowClass, map) ->
            map.map { (id, p) ->
                LayoutProfileRow(
                    key = "${windowClass.name}|$id",
                    windowClass = windowClass.name,
                    itemId = id,
                    page = p.page, col = p.col, row = p.row,
                    colSpan = p.colSpan, rowSpan = p.rowSpan,
                )
            }
        }
        try {
            db.layoutProfiles().replaceAll(rows)
        } catch (t: Throwable) {
            _lastSaveError.value = LayerToasts.COULDNT_SAVE
        }
        _profiles.value = profiles
    }

    /**
     * First use of [windowClass]: derive by reflow from canonical order
     * and persist. Later calls return the stored profile untouched, so
     * edits in one orientation never scramble another (Doc 1 §5.10).
     */
    suspend fun ensureProfile(
        windowClass: WindowClass,
        items: List<HomeItem>,
        colsCells: Int,
        rowsCells: Int,
    ): Map<String, ProfilePlacement> {
        val current = _profiles.value
        current.profiles[windowClass]?.let { return it }
        val canonical = canonicalOrderOf(items)
        val derived = deriveProfile(canonical, spansOf(items), colsCells, rowsCells)
        val updated = current.copy(
            canonical = canonical,
            profiles = current.profiles + (windowClass to derived),
        )
        saveProfiles(updated)
        return derived
    }

    /** Add/remove an item in EVERY profile + canonical order, then persist. */
    suspend fun profilesWithAppAdded(
        itemId: String,
        colSpan: Int,
        rowSpan: Int,
        gridCells: (WindowClass) -> Pair<Int, Int>,
    ) {
        val updated = _profiles.value.withAppAdded(itemId, colSpan, rowSpan, null, gridCells)
        saveProfiles(updated)
    }

    suspend fun profilesWithAppRemoved(itemId: String) {
        saveProfiles(_profiles.value.withAppRemoved(itemId))
    }

    /** "Sync layouts": preview first, apply only when the host confirms. */
    fun syncPreview(
        source: WindowClass,
        gridCells: (WindowClass) -> Pair<Int, Int>,
    ): List<SyncPreviewEntry> = syncLayoutsPreview(_profiles.value, source, gridCells).first

    suspend fun applySync(
        source: WindowClass,
        gridCells: (WindowClass) -> Pair<Int, Int>,
    ) {
        saveProfiles(syncLayoutsFrom(_profiles.value, source, gridCells))
    }

    // ---------------------------------------------------------- snapshots

    /** Snapshot rows, newest first, for the "Restore layout…" list. */
    suspend fun snapshots(): List<SnapshotRecord> = try {
        db.layoutSnapshots().allNewestFirst().map {
            SnapshotRecord(it.id, it.createdAtMs, it.pageCount, it.itemCount, it.folderCount, it.layoutJson)
        }
    } catch (t: Throwable) {
        emptyList()
    }

    /**
     * Auto-snapshot on edit-mode exit (Doc 3 §3.6): at most once/hour,
     * then prune to last 10 + one/day for 7 days. Call from the host's
     * edit-mode exit path (Worker A owns that call site).
     */
    suspend fun autoSnapshotOnEditExit(nowMs: Long = System.currentTimeMillis()) {
        val existing = snapshots()
        if (!shouldAutoSnapshot(existing.firstOrNull()?.createdAtMs, nowMs)) return
        val items = try {
            db.homeItems().all()
        } catch (t: Throwable) {
            return
        }
        val folders = try {
            db.folders().all()
        } catch (t: Throwable) {
            emptyList()
        }
        val json = snapshotJson(items, folders)
        val record = buildSnapshotRecord(nowMs, items, folders, json)
        try {
            db.layoutSnapshots().upsert(
                LayoutSnapshotRow(
                    record.id, record.createdAtMs, record.pageCount,
                    record.itemCount, record.folderCount, record.layoutJson,
                ),
            )
            val kept = pruneSnapshots(existing + record, nowMs).map { it.id }.toSet()
            val pruneIds = (existing + record).map { it.id }.filter { it !in kept }
            for (id in pruneIds) db.layoutSnapshots().delete(id)
        } catch (t: Throwable) {
            // Snapshots are a safety net, never a crash reason.
        }
    }

    /**
     * Restore a snapshot after the host showed its preview: one atomic
     * transaction replacing items + folders. Returns false when the
     * snapshot payload is corrupt (nothing changes).
     */
    suspend fun restoreSnapshot(record: SnapshotRecord): Boolean {
        val (items, folders) = parseSnapshot(record.layoutJson)
        if (items.isEmpty() && record.itemCount > 0) return false
        return commit(LayoutState(items = items, folders = folders, dock = _inMemory.value.dock))
    }

    // ---------------------------------------------------------- corruption

    /**
     * Start-up sanitation (Doc 3 §3.5): skip corrupt records, keep the
     * rest, append to the Diagnostics log, raise a one-time toast.
     * Never throws, never wipes a layout.
     */
    suspend fun sanitizeOnStart() {
        val items = try {
            db.homeItems().all()
        } catch (t: Throwable) {
            return
        }
        val folders = try {
            db.folders().all()
        } catch (t: Throwable) {
            emptyList()
        }
        val (keptItems, keptFolders, log) = sanitizeLayout(items, folders)
        if (log.isEmpty()) return
        try {
            db.homeItems().replaceAll(keptItems)
            db.folders().replaceAll(keptFolders)
        } catch (t: Throwable) {
            return // keep in-memory state; the next write retries
        }
        try {
            val line = log.joinToString("; ") { "${it.kind} ${it.id}: ${it.reason}" }
            File(appCtx.filesDir, "diagnostics_layout.txt").appendText(
                "${java.util.Date()}: skipped ${log.size} corrupt record(s): $line\n",
            )
        } catch (t: Throwable) {
            // Diagnostics logging must never break start-up.
        }
        _diagnosticsToast.value =
            "Skipped ${log.size} damaged layout item(s); the rest of your home is unchanged."
    }

    fun clearDiagnosticsToast() {
        _diagnosticsToast.value = null
    }

    // ------------------------------------------------------------- misc

    /** Drag lifecycle for the queued-settings rule (Doc 3 §3.4). */
    fun beginDrag() = pendingQueue.beginDrag()

    /** Apply queued Settings changes, in order. Returns how many ran. */
    fun endDrag(): Int {
        val queued = pendingQueue.endDrag()
        for (change in queued) {
            try {
                change()
            } catch (t: Throwable) {
                // One bad queued change must not block the rest.
            }
        }
        return queued.size
    }

    /** Slider writes: only the last value in a 200ms window is written. */
    fun debouncedWrite(key: String, write: suspend () -> Unit) {
        debounceJobs[key]?.cancel()
        debounceJobs[key] = scope.launch {
            delay(SLIDER_DEBOUNCE_MS)
            try {
                write()
            } catch (t: Throwable) {
                // A failed slider write surfaces on the next change; never crashes.
            }
        }
    }

    /**
     * Intent-resolved dock candidates (Doc 3 §5): each role maps to the
     * packages the system resolves for that intent, in system order.
     * No package name is hard-coded here — resolution is the system's.
     */
    fun resolveDockPackages(): Map<DockIntentRole, List<String>> {
        val pm = appCtx.packageManager
        fun resolve(intent: Intent): List<String> = try {
            pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.distinct()
        } catch (t: Throwable) {
            emptyList()
        }
        return mapOf(
            DockIntentRole.PHONE to resolve(Intent(Intent.ACTION_DIAL)),
            DockIntentRole.MESSAGES to resolve(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))),
            DockIntentRole.BROWSER to resolve(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
            ),
            DockIntentRole.CAMERA to resolve(Intent(MediaStore.ACTION_IMAGE_CAPTURE)),
            DockIntentRole.SETTINGS to resolve(Intent(android.provider.Settings.ACTION_SETTINGS)),
            DockIntentRole.GALLERY to resolve(
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            ),
        )
    }
}
