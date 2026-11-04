package app.havenos.launcher.data

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteFullException
import android.net.Uri
import android.provider.MediaStore
import app.havenos.launcher.ui.parseFullSnapshot
import app.havenos.launcher.ui.parseSnapshot
import app.havenos.launcher.ui.reflowForGrid
import app.havenos.launcher.ui.resizePlacement
import app.havenos.launcher.ui.snapshotJson
import androidx.room.withTransaction
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Worker D (M9) — the Android half of the state layer.
 *
 * The pure half lives in LayoutState.kt / LayoutProfiles.kt /
 * LayoutCoordination.kt / LayoutSnapshots.kt / CalmEditor.kt; this
 * class is the ONLY writer to Room for layout data, and the single
 * instance other workers use (via HavenApp.layoutStore).
 *
 * API OTHER WORKERS MUST USE:
 * - [undoStack] — the ONE shared undo stack (50 levels, frames of
 *   pre-transaction layout + profiles). It is authoritative
 *   in-session and mirrored into Room (undo_entries) so restart
 *   retains depth: [pushUndoSnapshot] (host, synchronous capture),
 *   commit pushes (same Room transaction as the layout write),
 *   [undo]/[redo] (apply + mirror the stack move), [undoDepth]/
 *   [redoDepth] (toolbar enable state). Restore clears both stacks.
 * - [pendingQueue] — submit Settings changes through it during a drag;
 *   they run on [endDrag].
 * - [cachedLayout] — cold-start flow: Room cache first, immediately
 *   (Doc 3 §1 #16). Widgets fill in after; never seed over it.
 * - [commit] — one transaction = one atomic Room write. On
 *   out-of-storage the in-memory state is kept, one retry is scheduled,
 *   and [lastSaveError] becomes "Couldn't save" (Doc 3 §3.5).
 * - [commitMutation] — the SINGLE path for layout writes (§2):
 *   validate, safety-net reflow, one Room transaction, undo push.
 *   Every new writer goes through it; direct DAO writes from the host
 *   are legacy paths, not the pattern.
 * - [currentLayoutState] — live layout from Room + Settings, the base
 *   every mutation applies to.
 * - [addAppsToProfiles] / [removeAppsEverywhere] /
 *   [removePackageEverywhere] — install/uninstall through commit(ALL).
 * - [applySync] — "Sync layouts" through commit(ALL), one undo entry.
 * - [ensureProfile] — first use of a window class derives its profile
 *   by reflow from canonical order, then persists it. After that the
 *   profile keeps its own positions.
 * - [profiles] — the current profile store (read-only flow).
 * - [currentWindowClass] — the host's window class from configuration;
 *   the coordinator owns this computation so commit targets match it.
 * - [repairDirtyProfile] — switch-time repair (§1): a dirty profile
 *   is reflowed under the new grid before first frame, then its flag
 *   clears. A dirty profile is never re-derived (that would destroy
 *   manual arrangement); repair preserves it.
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

    /**
     * Serializes undo-table bookkeeping (host pushes, undo/redo
     * moves) so rows persist in submission order. Commit inserts
     * deliberately do NOT take this lock: the doc requires them in
     * the same Room transaction as the layout write, and a commit
     * racing a host push can only reorder the restart backup's tail,
     * never the live layout or the in-session stack.
     */
    private val undoMutex = Mutex()

    /** Toolbar enable state, derived from the in-memory stack. */
    private val _undoDepth = MutableStateFlow(0)
    val undoDepth: StateFlow<Int> = _undoDepth
    private val _redoDepth = MutableStateFlow(0)
    val redoDepth: StateFlow<Int> = _redoDepth

    /** 3.1 A9: toolbar Undo/Redo labels ("Undo density grid reset"). */
    private val _undoLabel = MutableStateFlow<String?>(null)
    val undoLabel: StateFlow<String?> = _undoLabel
    private val _redoLabel = MutableStateFlow<String?>(null)
    val redoLabel: StateFlow<String?> = _redoLabel

    private fun refreshUndoDepths() {
        _undoDepth.value = undoStack.undoDepth
        _redoDepth.value = undoStack.redoDepth
        _undoLabel.value = undoStack.undoLabel
        _redoLabel.value = undoStack.redoLabel
    }

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

    /** One-time toast for a rejected commit (lock, drag, missing item). */
    private val _rejectionToast = MutableStateFlow<String?>(null)
    val rejectionToast: StateFlow<String?> = _rejectionToast

    fun clearRejectionToast() {
        _rejectionToast.value = null
    }

    private val _profiles = MutableStateFlow(LayoutProfiles())
    val profiles: StateFlow<LayoutProfiles> = _profiles

    private val debounceJobs = mutableMapOf<String, Job>()

    init {
        scope.launch { loadProfiles() }
        scope.launch { sanitizeOnStart() }
        scope.launch { rehydrateUndoStacks() }
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

    // ------------------------------------------------------- commitMutation

    /**
     * Live layout from Room + Settings, the base every mutation applies
     * to. Today cards and recents are preserved from the last committed
     * state (the host does not mutate them through this layer).
     */
    suspend fun currentLayoutState(): LayoutState {
        val items = try {
            db.homeItems().all()
        } catch (t: Throwable) {
            emptyList()
        }
        val folders = try {
            db.folders().all()
        } catch (t: Throwable) {
            emptyList()
        }
        val s = settings.state.value
        return LayoutState(
            items = items,
            folders = folders,
            dock = s.dock,
            hidden = s.hiddenApps(),
            todayCards = _inMemory.value.todayCards,
            recents = _inMemory.value.recents,
        )
    }

    /** Placements of a live item list, for the current profile map. */
    private fun placementsOf(items: List<HomeItem>): Map<String, ProfilePlacement> =
        items.associate { it.id to ProfilePlacement(it.page, it.col, it.row, it.colSpan, it.rowSpan) }

    /** Room rows for a profile store (shared by saveProfiles + commitMutation). */
    private fun profileRows(profiles: LayoutProfiles): List<LayoutProfileRow> =
        profiles.profiles.flatMap { (windowClass, map) ->
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

    /** Room rows for a profile store's dirty set. */
    private fun dirtyRows(profiles: LayoutProfiles): List<ProfileDirtyRow> =
        profiles.dirty.map { ProfileDirtyRow(it.name) }

    /**
     * The single path for layout writes (§2). Order: lock gate for user
     * mutations (lifecycle + restore bypass it — a removed app must
     * vanish even from a locked layout, and restore is the recovery
     * path); apply to a copy; validate + reflow safety net; persist
     * items + folders + profiles in one Room transaction; push the
     * pre-commit state to undo — except removals, which never undo
     * (undo would resurrect a dead icon), and restore, which clears
     * the stacks. [TxResult.Rejected] writes nothing and carries the
     * toast, also raised on [rejectionToast] for the host toast, so a
     * Settings-panel commit that fails never fails silently. Must not
     * be called while dragging (user mutations); call sites route
     * through [pendingQueue], drained on drop.
     *
     * Settings-owned parts (dock, hidden, grid override, dock position,
     * Today toggle) live in DataStore, not Room, so their setters run
     * as part of the mutation; a later Room failure leaves the setting
     * written and the layout to the host reflow safety net, which
     * converges on next composition. On out-of-storage the in-memory
     * state is kept and one retry scheduled, mirroring [commit].
     */
    suspend fun commitMutation(
        mutation: LayoutMutation,
        targetProfiles: Set<WindowClass> = setOf(currentWindowClass()),
        visiblePages: List<Int>? = null,
        gridCells: Pair<Int, Int>? = null,
    ): TxResult {
        val result = commitMutationInternal(mutation, targetProfiles, visiblePages, gridCells)
        if (result is TxResult.Rejected) _rejectionToast.value = result.toast
        return result
    }

    /** Implementation of [commitMutation] (toast raising lives in the wrapper). */
    private suspend fun commitMutationInternal(
        mutation: LayoutMutation,
        targetProfiles: Set<WindowClass> = setOf(currentWindowClass()),
        visiblePages: List<Int>? = null,
        gridCells: Pair<Int, Int>? = null,
    ): TxResult {
        val settingsState = settings.state.value
        val currentClass = currentWindowClass()
        val isSystem = mutation is LayoutMutation.AppLifecycle ||
            mutation is LayoutMutation.Restore
        if (!isSystem) {
            checkEditAllowed(settingsState.lockLayout)?.let { return TxResult.Rejected(it) }
        }
        // The drag gate applies to user mutations only: a system
        // lifecycle event (install/uninstall) must never be dropped
        // because a drag happened to be in flight — a dropped removal
        // would leave a dead icon with no later broadcast to retry it.
        if (!isSystem && pendingQueue.dragging) return TxResult.Rejected("Drag in progress")
        val current = currentLayoutState()
        val oldProfiles = _profiles.value
        var candidate = current
        var newProfiles = oldProfiles
        var pushUndo = true
        var skipLayoutWrite = false
        // 3.1 A9: the grid override a Follow-density commit clears,
        // captured BEFORE the clear so the pushed frame can restore
        // it on undo. Null for every other mutation.
        var clearedGridOverride: GridOverrideFrame? = null
        val gridForClass: (WindowClass) -> Pair<Int, Int> = { wc ->
            if (wc == WindowClass.EXPANDED_LANDSCAPE) settingsState.gridCells(true)
            else settingsState.gridCells(false)
        }
        when (mutation) {
            is LayoutMutation.DragDrop -> {
                candidate = current.copy(items = mutation.items, dock = mutation.dock)
                if (gridCells != null) {
                    candidate = candidate.copy(
                        items = reflowForGrid(
                            candidate.items, gridCells.first * 2, gridCells.second * 2, visiblePages,
                        ).items,
                    )
                }
            }
            is LayoutMutation.Resize -> {
                val item = current.itemById(mutation.itemId)
                    ?: return TxResult.Rejected("Item no longer exists")
                val grid = gridCells ?: return TxResult.Rejected("Grid unknown")
                val plan = resizePlacement(
                    item, mutation.colSpanU, mutation.rowSpanU, current.items,
                    grid.first * 2, grid.second * 2,
                )
                val resized = when {
                    plan.valid -> item.copy(colSpan = mutation.colSpanU, rowSpan = mutation.rowSpanU)
                    plan.fallback != null -> {
                        val (fc, fr) = plan.fallback
                        item.copy(colSpan = mutation.colSpanU, rowSpan = mutation.rowSpanU, col = fc, row = fr)
                    }
                    else -> return TxResult.Rejected("No room for that size")
                }
                candidate = current.copy(
                    items = current.items.map { if (it.id == mutation.itemId) resized else it },
                )
            }
            is LayoutMutation.PlaceItem -> {
                val placed = mutation.item.copy(page = mutation.page)
                var items = current.items.filterNot { it.id == placed.id } + placed
                if (gridCells != null) {
                    items = reflowForGrid(
                        items, gridCells.first * 2, gridCells.second * 2, visiblePages,
                    ).items
                }
                candidate = current.copy(items = items)
            }
            is LayoutMutation.RemoveItem -> {
                validateItemExists(current, mutation.itemId)?.let { return TxResult.Rejected(it) }
                candidate = current.copy(
                    items = current.items.filterNot { it.id == mutation.itemId },
                    folders = current.folders.filterNot { it.id == mutation.itemId },
                )
            }
            is LayoutMutation.GridChange -> {
                if (mutation.followDensity) {
                    // 3.1 A9: capture the doomed override first —
                    // settingsState is the pre-commit snapshot.
                    val (oc, orow) = if (mutation.landscape) {
                        settingsState.colsLand to settingsState.rowsLand
                    } else {
                        settingsState.colsPort to settingsState.rowsPort
                    }
                    if (oc > 0 && orow > 0) {
                        clearedGridOverride = GridOverrideFrame(mutation.landscape, oc, orow)
                    }
                    settings.clearGridOverride(mutation.landscape)
                } else {
                    settings.setGridOverride(mutation.landscape, mutation.colsCells, mutation.rowsCells)
                }
                candidate = current.copy(
                    items = reflowForGrid(
                        current.items, mutation.colsCells * 2, mutation.rowsCells * 2, visiblePages,
                    ).items,
                )
            }
            is LayoutMutation.DockPositionChange -> {
                settings.setDockPos(mutation.position)
                if (gridCells != null) {
                    candidate = candidate.copy(
                        items = reflowForGrid(
                            current.items, gridCells.first * 2, gridCells.second * 2, visiblePages,
                        ).items,
                    )
                }
            }
            is LayoutMutation.TodayToggle -> {
                settings.setTodayOn(mutation.on)
            }
            is LayoutMutation.AppLifecycle -> {
                pushUndo = mutation.removed.isEmpty()
                var layout = current
                var profs = newProfiles
                for (key in mutation.added) {
                    profs = profs.withAppInstalled(key, 2, 2, settingsState.autoAddApps, currentClass, gridForClass)
                }
                for (key in mutation.removed) {
                    layout = applyAppRemoved(layout, key)
                    profs = profs.withAppRemoved(key)
                }
                candidate = layout
                newProfiles = profs
                if (layout == current && mutation.removed.isEmpty()) skipLayoutWrite = true
            }
            is LayoutMutation.SyncFromCurrent -> {
                newProfiles = syncLayoutsFrom(newProfiles, currentClass, gridForClass)
                skipLayoutWrite = true
            }
            is LayoutMutation.Restore -> {
                candidate = mutation.snapshot
                pushUndo = false
            }
        }
        // Current profile map refresh: the live layout IS the current
        // profile's placements, so a layout change rewrites its map
        // when one exists. Absent maps stay absent (first use derives).
        if (candidate.items != current.items && newProfiles.profiles.containsKey(currentClass)) {
            newProfiles = newProfiles.copy(
                profiles = newProfiles.profiles + (currentClass to placementsOf(candidate.items)),
            )
        }
        // Dirty flags: ONLY grid/density commits set them, on
        // non-current targets (their grids changed but their
        // placements were not rewritten here; switch-time repair
        // reflows them). Drag, dock, Today, install, uninstall, and
        // sync never set them.
        if (mutation is LayoutMutation.GridChange) {
            newProfiles = newProfiles.copy(dirty = newProfiles.dirty + (targetProfiles - currentClass))
        }
        // No-op: nothing to persist, no undo to record. Settings writes
        // above (grid/dock/Today) already landed via their setters.
        // 3.1 A9 exception: a cleared grid override is NOT a no-op —
        // it changes future density behaviour, so it still needs its
        // undo frame even when the layout is unchanged.
        if (candidate == current && newProfiles == oldProfiles && clearedGridOverride == null) {
            if (mutation is LayoutMutation.Restore) {
                undoStack.clear()
                scope.launch { runCatching { db.undoEntries().clearAll() } }
                refreshUndoDepths()
            }
            return TxResult.Committed(candidate, current)
        }
        return try {
            db.withTransaction {
                if (!skipLayoutWrite) {
                    db.homeItems().replaceAll(candidate.items)
                    db.folders().replaceAll(candidate.folders)
                }
                db.layoutProfiles().replaceAll(profileRows(newProfiles))
                db.profileDirty().replaceAll(dirtyRows(newProfiles))
                // Durable undo (§5): the pre-commit frame is written in
                // the SAME transaction as the layout write, so a failed
                // write never records an undo step for a change that
                // did not land. Removals never undo (they would
                // resurrect a dead icon); restore clears both stacks.
                if (mutation is LayoutMutation.Restore) {
                    undoStack.clear()
                    db.undoEntries().clearAll()
                } else if (pushUndo) {
                    // 3.1 A9: the frame carries the cleared override
                    // (if any) plus its toolbar label; the grid action
                    // persists in gridJson, the label does not.
                    val frame = UndoFrame(
                        current, oldProfiles,
                        gridOverride = clearedGridOverride,
                        label = if (clearedGridOverride != null) "density grid reset" else null,
                    )
                    undoStack.push(frame)
                    db.undoEntries().insert(
                        UndoEntryRow(
                            direction = "UNDO",
                            layoutJson = snapshotJson(
                                current.items, current.folders, current.dock,
                                current.hidden, current.todayCards, current.recents,
                            ),
                            profilesJson = oldProfiles.toJson(),
                            gridJson = frame.gridOverride?.encode() ?: "",
                        ),
                    )
                    db.undoEntries().clearDirection("REDO")
                    db.undoEntries().prune("UNDO", 50)
                }
            }
            if (candidate.dock != current.dock) settings.setDock(candidate.dock)
            if (candidate.hidden != current.hidden) settings.setHiddenJson(encodeMembers(candidate.hidden.toList()))
            _profiles.value = newProfiles
            _inMemory.value = candidate
            _lastSaveError.value = null
            refreshUndoDepths()
            TxResult.Committed(candidate, current)
        } catch (t: Throwable) {
            if (t is SQLiteFullException || t is IOException) {
                _lastSaveError.value = LayerToasts.COULDNT_SAVE
                _inMemory.value = candidate
                _profiles.value = newProfiles
                scope.launch {
                    delay(5_000)
                    runCatching {
                        db.withTransaction {
                            if (!skipLayoutWrite) {
                                db.homeItems().replaceAll(_inMemory.value.items)
                                db.folders().replaceAll(_inMemory.value.folders)
                            }
                            db.layoutProfiles().replaceAll(profileRows(newProfiles))
                            db.profileDirty().replaceAll(dirtyRows(newProfiles))
                        }
                        _lastSaveError.value = null
                    }
                }
                TxResult.Committed(candidate, current)
            } else {
                TxResult.Rejected(LayerToasts.COULDNT_SAVE)
            }
        }
    }

    /** Install: canonical membership (+ current-profile placement when auto-add is on), through commit(ALL). */
    suspend fun addAppsToProfiles(appKeys: List<String>): TxResult =
        commitMutation(
            LayoutMutation.AppLifecycle(added = appKeys, removed = emptyList()),
            targetProfiles = WindowClass.entries.toSet(),
        )

    /** Uninstall: vanish from layout, folders, dock, hidden, recents, and every profile, through commit(ALL). Never undoable. */
    suspend fun removeAppsEverywhere(appKeys: List<String>): TxResult =
        commitMutation(
            LayoutMutation.AppLifecycle(added = emptyList(), removed = appKeys),
            targetProfiles = WindowClass.entries.toSet(),
        )

    /**
     * Uninstall by package prefix (no catalog key needed): collects
     * every key belonging to [pkg] — app items by payload, folder
     * members, dock entries, hidden entries, profile ids — then removes
     * them all. No-op when nothing matches (no write, no undo).
     */
    suspend fun removePackageEverywhere(pkg: String): TxResult {
        if (pkg.isBlank()) return TxResult.Rejected("Invalid package")
        val prefix = "$pkg/"
        val current = currentLayoutState()
        val keys = mutableSetOf<String>()
        for (item in current.items) {
            if (item.kind == ItemKind.APP && item.payload.startsWith(prefix)) keys += item.payload
        }
        for (folder in current.folders) {
            for (member in parseMembers(folder.membersJson)) {
                if (member.startsWith(prefix)) keys += member
            }
        }
        for (key in current.dock) if (key.startsWith(prefix)) keys += key
        for (key in current.hidden) if (key.startsWith(prefix)) keys += key
        // Profile ids are HomeItem ids ("app:$key"), not bare keys, so
        // both forms are collected; applyAppRemoved / withAppRemoved
        // each match only their own form, and both are idempotent.
        for (map in _profiles.value.profiles.values) {
            for (id in map.keys) {
                if (id.startsWith(prefix) || id.startsWith("app:$prefix")) keys += id
            }
        }
        if (keys.isEmpty()) return TxResult.Committed(current, current)
        return removeAppsEverywhere(keys.toList())
    }

    /** "Sync layouts": preview first ([syncPreview]), apply only when the host confirms — from the CURRENT profile, one undo entry. */
    suspend fun applySync(): TxResult =
        commitMutation(
            LayoutMutation.SyncFromCurrent,
            targetProfiles = WindowClass.entries.toSet(),
        )

    // ------------------------------------------------------- durable undo

    /**
     * Host push (§5): captures the pre-change layout SYNCHRONOUSLY —
     * the host calls this before its own write, so the frame cannot
     * race the mutation — then mirrors it to Room under [undoMutex].
     * Dock/hidden come from live Settings, Today cards/recents from
     * the last committed state (the host never mutates them). The
     * in-memory stack stays authoritative; Room is the restart backup
     * (a persist still in flight at process death is lost, never
     * corrupt — depth on restart is a prefix of the session's).
     */
    fun pushUndoSnapshot(items: List<HomeItem>, folders: List<Folder>) {
        val s = settings.state.value
        val frame = UndoFrame(
            layout = LayoutState(
                items = items, folders = folders, dock = s.dock, hidden = s.hiddenApps(),
                todayCards = _inMemory.value.todayCards, recents = _inMemory.value.recents,
            ),
            profiles = _profiles.value,
        )
        undoStack.push(frame)
        refreshUndoDepths()
        scope.launch {
            try {
                undoMutex.withLock {
                    db.withTransaction {
                        db.undoEntries().insert(
                            UndoEntryRow(
                                direction = "UNDO",
                                layoutJson = snapshotJson(
                                    frame.layout.items, frame.layout.folders, frame.layout.dock,
                                    frame.layout.hidden, frame.layout.todayCards, frame.layout.recents,
                                ),
                                profilesJson = frame.profiles.toJson(),
                            ),
                        )
                        db.undoEntries().clearDirection("REDO")
                        db.undoEntries().prune("UNDO", 50)
                    }
                }
            } catch (t: Throwable) {
                // Backup only: the in-memory frame still undoes.
            }
        }
    }

    /** Apply one undo/redo frame: layout + profiles + dock + hidden + grid override. */
    private suspend fun applyFrame(frame: UndoFrame, current: LayoutState): Boolean {
        return try {
            db.withTransaction {
                db.homeItems().replaceAll(frame.layout.items)
                db.folders().replaceAll(frame.layout.folders)
                db.layoutProfiles().replaceAll(profileRows(frame.profiles))
                db.profileDirty().replaceAll(dirtyRows(frame.profiles))
            }
            if (frame.layout.dock != current.dock) settings.setDock(frame.layout.dock)
            if (frame.layout.hidden != current.hidden) {
                settings.setHiddenJson(encodeMembers(frame.layout.hidden.toList()))
            }
            // 3.1 A9: the frame's grid action, if any — restore the
            // override, or clear it (redo of a Follow-density step).
            frame.gridOverride?.let { g ->
                if (g.cols > 0 && g.rows > 0) settings.setGridOverride(g.landscape, g.cols, g.rows)
                else settings.clearGridOverride(g.landscape)
            }
            _profiles.value = frame.profiles
            _inMemory.value = frame.layout
            true
        } catch (t: Throwable) {
            if (t is SQLiteFullException || t is IOException) {
                _lastSaveError.value = LayerToasts.COULDNT_SAVE
            }
            false
        }
    }

    /** Undo one step. False when the stack is empty or the apply failed (the step is consumed; redo still holds the pre-undo state). */
    suspend fun undo(): Boolean {
        val current = currentLayoutState()
        // 3.1 A9: the redo frame mirrors the undone step's grid
        // action from the CURRENT settings (cleared now → redo
        // clears again); it carries the same label for Redo.
        val top = undoStack.peekUndo() ?: return false
        val s = settings.state.value
        val currentFrame = UndoFrame(
            current, _profiles.value,
            gridOverride = gridActionForCurrentFrame(
                top.gridOverride, s.colsLand, s.rowsLand, s.colsPort, s.rowsPort,
            ),
            label = top.label,
        )
        val (prev, _) = undoStack.popUndo(currentFrame) ?: return false
        refreshUndoDepths()
        if (!applyFrame(prev, current)) return false
        scope.launch {
            try {
                undoMutex.withLock {
                    db.withTransaction {
                        db.undoEntries().deleteLatest("UNDO")
                        db.undoEntries().insert(
                            UndoEntryRow(
                                direction = "REDO",
                                layoutJson = snapshotJson(
                                    currentFrame.layout.items, currentFrame.layout.folders,
                                    currentFrame.layout.dock, currentFrame.layout.hidden,
                                    currentFrame.layout.todayCards, currentFrame.layout.recents,
                                ),
                                profilesJson = currentFrame.profiles.toJson(),
                                gridJson = currentFrame.gridOverride?.encode() ?: "",
                            ),
                        )
                        db.undoEntries().prune("REDO", 50)
                    }
                }
            } catch (t: Throwable) {
                // Backup only.
            }
        }
        return true
    }

    /** Redo one step. False when the stack is empty or the apply failed. */
    suspend fun redo(): Boolean {
        val current = currentLayoutState()
        // 3.1 A9: mirror of undo() — the frame pushed back onto the
        // undo stack mirrors the redone step's grid action.
        val top = undoStack.peekRedo() ?: return false
        val s = settings.state.value
        val currentFrame = UndoFrame(
            current, _profiles.value,
            gridOverride = gridActionForCurrentFrame(
                top.gridOverride, s.colsLand, s.rowsLand, s.colsPort, s.rowsPort,
            ),
            label = top.label,
        )
        val next = undoStack.popRedo(currentFrame) ?: return false
        refreshUndoDepths()
        if (!applyFrame(next, current)) return false
        scope.launch {
            try {
                undoMutex.withLock {
                    db.withTransaction {
                        db.undoEntries().deleteLatest("REDO")
                        db.undoEntries().insert(
                            UndoEntryRow(
                                direction = "UNDO",
                                layoutJson = snapshotJson(
                                    currentFrame.layout.items, currentFrame.layout.folders,
                                    currentFrame.layout.dock, currentFrame.layout.hidden,
                                    currentFrame.layout.todayCards, currentFrame.layout.recents,
                                ),
                                profilesJson = currentFrame.profiles.toJson(),
                                gridJson = currentFrame.gridOverride?.encode() ?: "",
                            ),
                        )
                        db.undoEntries().prune("UNDO", 50)
                    }
                }
            } catch (t: Throwable) {
                // Backup only.
            }
        }
        return true
    }

    /** Restart: rebuild the in-memory stacks from Room rows, oldest first. Blank rows are skipped, never applied. */
    private suspend fun rehydrateUndoStacks() {
        suspend fun frames(direction: String): List<UndoFrame> {
            return try {
                db.undoEntries().all(direction).mapNotNull { row ->
                    if (row.layoutJson.isBlank()) null
                    else UndoFrame(
                        layout = parseFullSnapshot(row.layoutJson, LayoutState()),
                        profiles = if (row.profilesJson.isBlank()) LayoutProfiles()
                        else parseLayoutProfiles(row.profilesJson),
                        // 3.1 A9: grid action survives restart; the
                        // label does not (plain Undo/Redo after one).
                        gridOverride = decodeGridOverrideFrame(row.gridJson),
                    )
                }
            } catch (t: Throwable) {
                emptyList()
            }
        }
        undoStack.restore(frames("UNDO"), frames("REDO"))
        refreshUndoDepths()
    }

    // ----------------------------------------------------------- profiles

    /** Load persisted profiles into [_profiles] (empty on first run), including the durable dirty set. */
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
        val dirty = try {
            db.profileDirty().all().mapNotNull { row ->
                try {
                    WindowClass.valueOf(row.windowClass)
                } catch (t: Throwable) {
                    null
                }
            }.toSet()
        } catch (t: Throwable) {
            emptySet()
        }
        val canonical = canonicalOrderOf(
            try {
                db.homeItems().all()
            } catch (t: Throwable) {
                emptyList()
            },
        )
        _profiles.value = LayoutProfiles(canonical, byClass, dirty)
    }

    private suspend fun saveProfiles(profiles: LayoutProfiles) {
        try {
            db.layoutProfiles().replaceAll(profileRows(profiles))
            db.profileDirty().replaceAll(dirtyRows(profiles))
        } catch (t: Throwable) {
            _lastSaveError.value = LayerToasts.COULDNT_SAVE
        }
        _profiles.value = profiles
    }

    /**
     * First use of [windowClass]: derive by reflow from canonical order
     * and persist. Later calls return the stored profile untouched, so
     * edits in one orientation never scramble another (Doc 1 §5.10).
     * The first-use gate (§4) applies: a derived layout that is valid,
     * or invalid only by oversize singles, stores as derived; any
     * other invalidity reflows first. Never re-derives a dirty profile.
     */
    suspend fun ensureProfile(
        windowClass: WindowClass,
        items: List<HomeItem>,
        colsCells: Int,
        rowsCells: Int,
    ): Map<String, ProfilePlacement> {
        val current = _profiles.value
        current.profiles[windowClass]?.let { return it }
        val firstUse = firstUseProfileItems(items, colsCells, rowsCells)
        val placed = firstUse.associate {
            it.id to ProfilePlacement(it.page, it.col, it.row, it.colSpan, it.rowSpan)
        }
        val updated = current.copy(
            canonical = canonicalOrderOf(items),
            profiles = current.profiles + (windowClass to placed),
        )
        saveProfiles(updated)
        return placed
    }

    /** The host's window class, computed here so commit targets match it. */
    fun currentWindowClass(): WindowClass {
        val config = appCtx.resources.configuration
        val landscape =
            config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        return windowClassFor(config.screenWidthDp, landscape)
    }

    /**
     * Switch-time dirty repair (§1): when [windowClass] carries a dirty
     * flag, reflow its stored placements under the new grid and store
     * the repaired profile, clearing the flag. No undo entry: repair
     * moves only items that are out of bounds or overlapping under the
     * new grid; valid items keep exact positions. Returns false when
     * there was nothing to repair or the gate failed (flag kept, so the
     * next switch retries). Flags are durable since the Room v6
     * migration (profile_dirty table, via loadProfiles/saveProfiles).
     */
    suspend fun repairDirtyProfile(
        windowClass: WindowClass,
        colsCells: Int,
        rowsCells: Int,
        visiblePages: List<Int>? = null,
    ): Boolean {
        val current = _profiles.value
        if (windowClass !in current.dirty) return false
        val stored = current.profiles[windowClass]
        if (stored == null) {
            // No placements to repair; the profile derives fresh on first use.
            val cleared = current.copy(dirty = current.dirty - windowClass)
            saveProfiles(cleared)
            return false
        }
        val items = try {
            db.homeItems().all()
        } catch (t: Throwable) {
            return false
        }
        val (repaired, gateOk) =
            repairProfileItems(materializeProfile(items, stored), colsCells, rowsCells, visiblePages)
        if (!gateOk) return false
        val placed = repaired.associate {
            it.id to ProfilePlacement(it.page, it.col, it.row, it.colSpan, it.rowSpan)
        }
        val updated = current.copy(
            profiles = current.profiles + (windowClass to placed),
            dirty = current.dirty - windowClass,
        )
        saveProfiles(updated)
        return true
    }

    /** "Sync layouts": preview first, apply only when the host confirms (see [applySync]). */
    fun syncPreview(
        source: WindowClass,
        gridCells: (WindowClass) -> Pair<Int, Int>,
    ): List<SyncPreviewEntry> = syncLayoutsPreview(_profiles.value, source, gridCells).first

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
     * Restore-list previews (§6): each record with its item ids in
     * snapshot order — the payload the list previously ignored. The
     * row renders its first-page thumbnail from these ids (gated by
     * [showRestoreThumbnail]); geometry comes from parsing the
     * record, since ids alone carry no coordinates. Never throws.
     */
    suspend fun snapshotPreviews(): List<SnapshotPreview> = snapshots().map { rec ->
        val (items, _) = parseSnapshot(rec.layoutJson)
        rec.toPreview(items.map { it.id })
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
        val s = settings.state.value
        val json = snapshotJson(
            items, folders, dock = s.dock, hidden = s.hiddenApps(),
            todayCards = _inMemory.value.todayCards, recents = _inMemory.value.recents,
        )
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
     * transaction replacing items + folders, applying the snapshot's
     * dock/hidden when it carries them, and clearing both undo stacks
     * (§3: a restore is recovery, not an undoable step). Old-format
     * snapshots (items/folders only) restore layout-only, exactly as
     * before. Returns false when the payload is corrupt (nothing
     * changes) or the write failed.
     */
    suspend fun restoreSnapshot(record: SnapshotRecord): Boolean {
        val current = currentLayoutState()
        val full = parseFullSnapshot(record.layoutJson, current)
        if (full.items.isEmpty() && record.itemCount > 0) return false
        return try {
            db.withTransaction {
                db.homeItems().replaceAll(full.items)
                db.folders().replaceAll(full.folders)
                db.undoEntries().clearAll()
            }
            if (full.dock != current.dock) settings.setDock(full.dock)
            if (full.hidden != current.hidden) settings.setHiddenJson(encodeMembers(full.hidden.toList()))
            _inMemory.value = full
            undoStack.clear()
            refreshUndoDepths()
            true
        } catch (t: Throwable) {
            false
        }
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
