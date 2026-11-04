# M9 Worker D — Layout profiles + safety + coordination (state layer)

Date: 2026-10-23. Scope: state layer, profiles, safety, coordination ONLY.
No edit-mode drag UI, dock visuals, widget content, or Settings panel
visuals were changed. The only Home.kt touches are (a) the first-run
dock seed now resolving intents first (fallback: old hard-coded picks)
and (b) CalmMode's "Choose apps" opening the new Calm editor screen.
Settings panel UI is visually unchanged (one new stored key only:
`calmEntryJson`).

## State-layer API — what other workers must use

All pure functions live in `app/src/main/java/app/havenos/launcher/data/`
and are unit-testable without Android. The Android writer is
`LayoutCoordinator` (LayoutStore.kt), reached via `HavenApp.layoutStore`.

### Profiles (LayoutProfiles.kt) — the store other workers read positions from

- `WindowClass`: `COMPACT | MEDIUM | EXPANDED_LANDSCAPE`.
- `windowClassFor(widthDp, landscape)`: >=840dp + landscape =
  EXPANDED_LANDSCAPE, >=600dp = MEDIUM, else COMPACT.
- `LayoutProfiles(canonical, profiles)`: canonical order
  (`CanonicalEntry(id, page)`, display order) + one placement map per
  class. Coordinates are HALF-CELL units (1 cell = 2 units).
- `canonicalOrderOf(items)` / `spansOf(items)`: build canonical + spans
  from live HomeItems.
- `deriveProfile(canonical, spans, colsCells, rowsCells)`: FIRST USE of
  a profile — reflow from canonical order, grouped by canonical page,
  row-major, spilling forward; oversize items get their own fresh page
  (never dropped, never silently resized). After derivation the profile
  keeps its own positions.
- `LayoutProfiles.withAppAdded(...)` / `.withAppRemoved(...)`: add/remove
  updates EVERY existing profile (absent profiles derive from the
  updated canonical order on first use, so they include the change).
- `syncLayoutsPreview(source, gridCells)` -> (preview, synced):
  "Sync layouts" overflow action — re-derives other profiles from the
  SOURCE profile's own order, with a per-item preview. Nothing commits
  until the host calls `syncLayoutsFrom(...)` on Apply.
- JSON: `LayoutProfiles.toJson()` / `parseLayoutProfiles()` (never throws).
- Coordinator: `layoutStore.ensureProfile(windowClass, items, cols, rows)`
  (derive-on-first-use + persist), `layoutStore.profiles` (flow),
  `layoutStore.profilesWithAppAdded/Removed`, `syncPreview/applySync`.
  Room table: `layout_profiles` (schema v4, see below).

### Transactions (LayoutState.kt + LayoutStore.kt)

- `LayoutState(items, folders, dock, todayCards, hidden, recents)`:
  the layout half of the single source of truth. `SettingsState` is the
  settings half; `LookTokens` (LayoutCoordination.kt) and UiState are
  DERIVED, never stored.
- `TxResult.Committed(state, undoSnapshot)` / `Rejected(toast)`:
  a rejected transaction changes NOTHING and names the reason.
- `UndoStack` (50 levels, in-memory, clears on process end): THE shared
  stack for edit mode + Settings. **Worker A: adopt this object for the
  toolbar Undo/Redo** — push the pre-change state BEFORE every commit,
  persist the popped state with `layoutStore.commit(...)`. The old
  UI-local 10-step list in Home.kt is superseded by it (left in place
  for Worker A to swap, to avoid a parallel-edit conflict).
- `PendingChangesQueue`: during a drag, Settings changes queue and run
  on drop, in order (`layoutStore.beginDrag()` / `endDrag()`).
- `applyMoveItem`, `applyDragToDock` ("Dock is full" refusal;
  Compact side dock capped at 5 via `effectiveDockMax`),
  `applyRemoveFromDock`, `placeOnFirstFreeSpot` (current page, then
  others, then a new page), `planGridChange` (ghost + summary, Apply
  only — see "Grid change" below).
- `layoutStore.commit(next)`: ONE atomic Room transaction
  (items + folders). Write path is Dispatchers.IO, off composition.
  Out-of-storage: in-memory state kept, one retry after 5s,
  `lastSaveError` = "Couldn't save" (Doc 3 §3.5).
- Slider writes: `layoutStore.debouncedWrite(key) { ... }`, 200ms
  after the last change (`SLIDER_DEBOUNCE_MS`, pure gate
  `sliderWriteDue` for tests).
- `checkEditAllowed(lockLayout)`: lock enforcement lives HERE in the
  state layer; Worker A owns the toast text ("Layout is locked").

### App lifecycle (LayoutState.kt)

- `applyAppInstalled(state, key, autoAdd, colsU, rowsU)`: Library gains
  the app via the catalog; home placement only when `autoAdd` is on,
  the app is not hidden, and it has no home item yet (first free slot).
- `applyAppRemoved(state, key)`: ONE transaction, NO undo (the app is
  gone): removed from layout, folders (1-app folder dissolves to a
  plain icon, empty folder deleted), dock, hidden, recents.
- `applyAppUpdated`: no layout write — icons refresh from the catalog
  at read time. Work-profile keys (`pkg/cls#serial`) keep profiles apart.
- `applyHideApp`: layout + dock + recents removal, added to hidden,
  undoable (unlike uninstall).
- Pin requests: `placeOnFirstFreeSpot(..., startPage = currentPage)`
  is the placement for the system pin flow (host wires the system
  dialog; the flow places on the current page's first free spot).

### Snapshots + restore (LayoutSnapshots.kt + LayoutStore.kt)

- `shouldAutoSnapshot(lastMs, nowMs)`: at most once/hour, on
  edit-mode exit — host calls `layoutStore.autoSnapshotOnEditExit()`
  (**Worker A: call this from the edit-mode exit path**).
- `pruneSnapshots(records, nowMs)`: last 10 + one/day for 7 days.
- Room table `layout_snapshots` (schema v4). Restore list:
  `layoutStore.snapshots()` (newest first, with page/item/folder
  counts); preview renders the snapshot's items through the normal home
  renderer; `layoutStore.restoreSnapshot(record)` applies in one
  transaction (false + no change when the payload is corrupt).
- Restore UI entry ("Restore layout…" in the edit-mode overflow ⋮) is
  Worker A's surface; this layer owns the list, preview data, and apply.

### Errors (Doc 3 §3.5)

- `sanitizeLayout(items, folders)`: blank ids, duplicate ids,
  non-positive spans, negative pages are skipped; the rest kept.
  `layoutStore.sanitizeOnStart()` runs it at start-up, writes the kept
  layout atomically, appends to `filesDir/diagnostics_layout.txt`, and
  raises a one-time toast via `diagnosticsToast` (clear with
  `clearDiagnosticsToast()`). Never crashes on start.
- Invalid backup: `validateBackupImport` + Backup.kt guards — an
  invalid file changes nothing (unsupported version rejected; v1 still
  imports, missing version treated as v1).
- Widget failure containment is Worker C's frame; this layer only
  guarantees a failed widget bind never corrupts the layout (placement
  commits only after a successful bind — host order: bind, then
  `placeOnFirstFreeSpot`, then `commit`).

### Calm editor (CalmEditor.kt + ui/CalmEditorScreen.kt)

- `CalmEditorState(favorites ≤ 8, scheduleEnabled, days, start/end
  minutes, entry)`: `withAdded/withRemoved/withMoved/withDayToggled`,
  `validate()`, `isScheduleActiveAt(day, minute)` (overnight windows
  wrap past midnight; empty days never activate).
- Keys (unchanged formats): `calmFavoritesJson` (array of app keys),
  `calmScheduleJson` (`{enabled,days,start,end}`, Calendar.DAY_OF_WEEK),
  `calmEntryJson` (NEW M9: `{autoEnter,dimPct}`, default "" = defaults).
- `CalmEditorScreen(apps, initial, onSave, onClose)`: favorites picker
  (cap 8), up/down reorder (TalkBack/keyboard usable), schedule
  (day chips + 15-minute sliders), entry options. Calm never changes
  the layout — saving writes only the three Calm keys.
- Calm enter/exit snapshot: `calmEnterSnapshot(page, editMode,
  overlays)` (LayoutCoordination.kt); exit restores exactly. Edit mode
  exits Calm first (Doc 1 §6) — host order, state restored on exit.

### Settings behaviors behind the frozen UI (LayoutCoordination.kt)

- Icon size: `resolveIconSize(auto, manualDp, cellW, cellH, labelsOn)`
  — Auto = f(cell, label, density); manual fixed per profile, clamped
  with note "Max for this grid" when it no longer fits; touch target
  never below 48dp (`labelSpaceDp` removes label space when Off).
- Grid: `planGridChange(items, cols, rows)` — ghost + "N items will
  move to page N" + Apply/Cancel; widgets shrink to the nearest valid
  size or move to the next page in position order, with a per-item
  summary; folders keep contents, positions reflow row by row.
- Mode: `resolveDark(darkMode, systemDark, scheduleDark)` + 200ms
  cross-fade (`MODE_CROSSFADE_MS`); scrim/label colors re-derive from
  the same inputs.
- Dock: `dockInsets(pos)` (grid reserves the inset, never overlaps),
  `indicatorEdge(pos)`, 240ms move animation (`DOCK_MOVE_ANIM_MS`),
  `planDockPositionChange` (re-fit plan; a dock move alone displaces
  nothing — overflow preview triggers when combined with a grid change).
- Look (Minimal ONLY): `deriveLookTokens(settings, systemDark, ...)` —
  chosen accent beats system dynamic colour UNLESS accentMode 3
  ("Wallpaper-derived"); `lookChanges(before, current)` = Changes list
  (differences from Minimal defaults + Before snapshot, each with its
  revert); `applyLookCommit(draft)` commits the draft — applying a look
  never moves items, and the host snapshots the committed state as the
  new Before baseline (Changes reset).
- Wallpaper: `wallpaperPipelineResult(...)` — palette -> accent
  SUGGESTION chip only (never auto-applied) -> legibility/scrim/Calm
  dim recompute flags; per-page falls back to global when missing.
- Motion/haptics: `effectiveMotion(baseMs, animSpeed, reducedMotion)` —
  reduced motion overrides speed (fades only); `ICON_RESIZE_CROSSFADE_MS`
  = 120ms; `hapticEnabled(strength, pressFeedback)`.
- A11y actions exposed for Worker A's UI: `a11yActionsForItem(isWidget)`
  (Move ×4, Move to page, Add to dock, Resize for widgets, Remove,
  Edit label, Hide).
- Permissions map: `permissionMap()` — one-sentence reason first, ask
  only at point of use; denied states: source toggle disabled with
  reason / Needs-permission chip / no badges, no reserved space /
  device-credential fallback / banner persists. `permissionStateUpdate`
  updates rows + widgets without leaving the screen.
- Default launcher: `defaultLauncherTransition(isDefault)` — banner
  stays while not default; once default, banner disappears at once and
  the home button returns to page 1 + closes overlays.
- Fresh install: `resolveDockFromIntents(resolvedByRole, installedPkgs)`
  — intent-resolved, never hard-coded packages (the coordinator helper
  `layoutStore.resolveDockPackages()` does the PackageManager queries);
  `freshInstallTodayCards()` = Clock+weather, Agenda (ONE),
  Suggested; `agendaSeedWouldDuplicate(items)` guards the seed.
  Fresh defaults: Labels On + Auto legibility, Icon size Auto, Grid
  Density, Mode Auto (system; schedule = darkMode 3), Today on.

## Event flows 1–12 (Doc 3 §3.3) — implementation + test status

Pure-function coverage is in the unit tests (see below); the
Compose/host wiring for flows that need UI is a named host call, listed
here so the coordinator can verify each on device:

1. Icon-size slider → `resolveIconSize` → metrics recompute (host) →
   120ms cross-fade → legibility re-run (host) → `debouncedWrite`
   200ms → Undo on release (host pushes pre-change state, then
   commits). Pure parts tested; host wiring: Settings panel slider.
2. Dock position → `dockInsets` + `planDockPositionChange` → re-fit +
   overflow ghost/confirm if displaced → indicator relocates (host) →
   widgets re-fit + summary (plan's moves). Pure parts tested.
3. Drag to dock → `applyDragToDock` → capacity/"Dock is full" →
   `commit` → Undo entry → recents untouched. Tested.
4. Add widget → bind (Worker C) → `placeOnFirstFreeSpot` → `commit` →
   Loading → Ready (Worker C states). Placement tested.
5. Wallpaper → `wallpaperPipelineResult` → suggestion chip (host, NOT
   auto-applied) → luminance rebuild → label styles → Calm dim
   (`calmDimAlpha`). Tested.
6. Apply look → draft → `applyLookCommit` → all surfaces update
   (host reads `deriveLookTokens`) → Modified cleared (new Before
   baseline) → Changes reset. Tested.
7. App installed → catalog (Library + New dot, host) →
   `applyAppInstalled` (optional first free slot) → `commit`. Tested.
8. App uninstalled/disabled → `applyAppRemoved` → one transaction, no
   Undo. Tested (incl. folder dissolve).
9. Rotation/split → `windowClassFor` → `ensureProfile` (first-time
   reflow derive) → render; edits affect the active profile only
   (host passes the active class to `commit` paths). Tested.
10. Calm enter/exit → `calmEnterSnapshot` → hide dock+home → show Calm
    → exit restores exactly (CalmMode overlay already restores by
    construction; snapshot type now exists for hosts that need it).
    Pure parts tested; device check pending.
11. Permission granted/denied → `permissionStateUpdate` → rows +
    widgets update without leaving the screen (host applies flags).
    Tested.
12. Default launcher set → `defaultLauncherTransition` → banner
    disappears, home-button handler switches (host). Tested.

## Schema + backup

- Room 3 → 4 (`MIGRATION_3_4`, additive only: creates
  `layout_profiles` + `layout_snapshots`; HomeItem/Folder columns
  untouched, so existing layouts survive verbatim). SQL pinned by
  `migration34Sql()` and asserted in LayoutCoordinationTest.
- Backup version 1 → 2 (`BACKUP_VERSION`): adds `layoutProfiles` +
  `calmEntryJson`. Old (v1) backups import (absent keys keep current
  values); unsupported versions change nothing.

## Files changed

New (pure state layer):
- `app/src/main/java/app/havenos/launcher/data/LayoutProfiles.kt`
- `app/src/main/java/app/havenos/launcher/data/LayoutState.kt`
- `app/src/main/java/app/havenos/launcher/data/LayoutCoordination.kt`
- `app/src/main/java/app/havenos/launcher/data/LayoutSnapshots.kt`
- `app/src/main/java/app/havenos/launcher/data/CalmEditor.kt`
- `app/src/main/java/app/havenos/launcher/data/LayoutStore.kt`
- `app/src/main/java/app/havenos/launcher/ui/CalmEditorScreen.kt`
- `app/src/test/java/app/havenos/launcher/data/LayoutProfilesTest.kt`
- `app/src/test/java/app/havenos/launcher/data/LayoutStateTest.kt`
- `app/src/test/java/app/havenos/launcher/data/LayoutCoordinationTest.kt`
- `app/src/test/java/app/havenos/launcher/data/LayoutSnapshotsCalmTest.kt`

Edited (minimal, no visuals):
- `app/src/main/java/app/havenos/launcher/data/Db.kt` (v4 + 2 tables + DAOs)
- `app/src/main/java/app/havenos/launcher/data/Settings.kt` (one new key:
  `calmEntryJson`, default "")
- `app/src/main/java/app/havenos/launcher/data/Backup.kt` (v2: profiles + entry key)
- `app/src/main/java/app/havenos/launcher/HavenApp.kt` (`layoutStore` init)
- `app/src/main/java/app/havenos/launcher/ui/Home.kt` (intent-resolved
  dock seed with fallback; "Choose apps" → Calm editor screen)

NOT changed, deliberately: edit-mode drag UI, dock visuals, widget
content, Settings panel visuals/copy/hubs/order, versionCode (still
8), signing, package.

## Checks actually run

- `:app:compileDebugKotlin --offline` via the AGENTS.md direct-GradleMain
  recipe, run 3 times (2026-10-23 evening IST):
  - Run 1: FAILED, 5m07s. All errors in ui/ files concurrently edited
    by parallel workers (Home.kt had grown 2411 -> 4000+ lines during
    the run, plus new WidgetReal.kt/EditMode.kt from other workers).
    Zero errors in any Worker D file.
  - Run 2: FAILED at Gradle start only — journal-cache lock held by
    another worker's build process (PID 3336). Not a code failure.
  - Run 3 (after removing my duplicate `showCalmEditor` declaration,
    which collided with Worker A's same-name state in Home.kt):
    FAILED, 67 errors, ALL in ui/ files under concurrent edit
    (Home.kt 22, PagedHome.kt 10, Drawer.kt 10, WidgetReal2.kt 5,
    Widgets.kt 4, HostWidgets.kt 4, TodayStack.kt 3, CategoriesView.kt
    3, LookHub.kt 2, WidgetReal.kt/FolderSheet.kt/EditMode.kt/
    CalmMode.kt 1 each). ZERO errors in `data/` (all 6 new state-layer
    files, Db.kt v4, Settings.kt, Backup.kt, HavenApp.kt) and zero in
    CalmEditorScreen.kt. The duplicate-declaration pair is resolved;
    the remaining Home.kt errors are Worker A's in-flight symbols
    (WallpaperLayer, AtAGlanceLine, StepperRow, ResizeDialog,
    AppIconPickerDialog) and similar cross-worker references.
  - Conclusion: the Worker D code itself compiles clean as far as the
    compiler got; the tree as a whole does not compile because
    parallel workers' UI work is mid-flight. The coordinator must
    re-run this task once workers A/B/C land.
- Unit tests for profile derivation, sync, transactions, undo,
  uninstall cleanup, grid plans, sanitation, snapshot policy, Calm
  editor, coordination, and migration SQL are WRITTEN in the files
  above but NOT RUN: the test executor cannot pass while the main
  source tree does not compile (parallel workers' in-flight UI edits),
  and the sandbox test executor is separately pre-existing-broken per
  the task brief. Written-not-run, honestly reported.

## Partly done / not done (with reason)

- Host wiring that belongs to other workers' surfaces is specified
  here but not pasted into their UI: Worker A — adopt `UndoStack` for
  toolbar Undo/Redo, call `autoSnapshotOnEditExit()` on edit exit,
  add the "Restore layout…" overflow entry, open `CalmEditorScreen`
  from the toolbar Calm button, pass the active `WindowClass` into
  edit commits; Worker B — read dock positions from
  `layoutStore.profiles` for the active class; Worker C — bind-before-
  commit order for widgets. These are adoption steps, not missing
  state-layer behavior.
- Device verification (Pad rotation, install/uninstall, restore
  preview rendering, Calm editor on tablet) is NOT done: no device in
  this sandbox. Each flow above names its host call for the device pass.
- The Calm editor uses up/down reorder buttons rather than drag
  reorder (TalkBack/keyboard usable; drag reorder is polish, and drag
  UI belongs to Worker A).
- `planDockPositionChange` returns the grid re-fit plan: a dock move
  alone (same grid) displaces nothing by construction; the overflow
  preview path triggers when the host also changes the grid.
- One known pre-existing behavior kept as fallback: the first-run dock
  seed still falls back to the hard-coded `dockPicks` list when intent
  resolution returns nothing on a device (defensive; the intent path
  is tried first and is the specified behavior).

## Most useful next improvement

Wire the host call sites (Worker A adoption list above) and run the
device pass for flows 9 (rotation profiles), 8 (uninstall dissolve),
and Restore-preview — those three are where a state-layer bug would
be visible to the user first.
