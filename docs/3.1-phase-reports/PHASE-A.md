# HavenOS 3.1 — Phase A report

Phase A (data / coordination / edge fixes, prompt §A) is complete.
Baseline: the 2026-10-29 coordination fix, extended, never redone.
Version stays 3.0 / versionCode 12 throughout (3.1.0 / 13 only in the
final step, per the prompt). No dependency added. No file under
`~/workspace/havenos-github/` touched, nothing published, nothing
uploaded, no token used, signing untouched.

## What changed, per item

### A1 — Scores null / raw statuses (done, pre-compaction session)
- `ui/TodayApiCards.kt`: `scoreTimeLabel` converts ISO-8601 to
  device-local HH:mm (unparseable input falls back to the old HH:mm
  slice, never crashes, never renders "null"; blank → "").
- Single status table `SCORE_STATUS_LABELS`: Not started / Live /
  Finished / Delayed. "Abnormal" deliberately NOT mapped — its meaning
  appears nowhere in `docs/API-INTEGRATION.md` or provider docs visible
  to the code; guessing would invent a meaning. Raw `status_text` is
  never rendered anymore.
- `isMissingScore` treats blank, lone "-", and literal "null"
  (case-insensitive) as missing. `ScoreRowDisplay` + `ScoreMatch.toDisplay()`
  (null = both teams missing → row dropped): teams left, result/time
  right, competition muted second line; upcoming = "vs" + local start
  time if known else "Not started"; live/finished with missing scores =
  status text only; unknown status = no status text. "Powered by
  SportScore" attribution untouched, always visible. Max 6 rows kept
  (Phase C3 changes this to 4 + Show more).
- Note on the symptom: `JSONObject.optString` returns the fallback for
  JSON null, so the literal "null" in recordings likely came from an
  older parser path or a string "null" value; the fix covers both.

### A2 — Offline state classification (foundation + cards + hub rows)
- `data/network/ApiClient.kt`: `ApiErrorKind` gained `SERVER_ERROR`;
  `get()` maps 5xx → SERVER_ERROR (429 → RATE_LIMITED, 404 →
  NOT_FOUND, 3xx → OTHER unchanged; transport failures still OFFLINE
  at transport level). Pure `ApiFailureClass` (OFFLINE, NETWORK_ERROR,
  RATE_LIMITED, NOT_FOUND, PARSE_ERROR, SERVER_ERROR, OTHER) +
  `classifyFailure(kind, httpCode, hasValidatedInternet)`: OFFLINE +
  validated internet → NETWORK_ERROR, OFFLINE + unvalidated → OFFLINE,
  other classes pass through, OTHER + 5xx → SERVER_ERROR. Pure
  `Context.hasValidatedInternet()` via ConnectivityManager
  `NET_CAPABILITY_VALIDATED`, never throws. Manifest gained
  `ACCESS_NETWORK_STATE` (normal permission, no runtime prompt).
- `data/network/ApiCache.kt`: `ApiDiagnosticsRow` (api PK, resultClass,
  httpCode, timeMs, bodySnippet) + DAO. Snippet rule: first 200 chars,
  parse errors ONLY, otherwise empty; keys travel in headers, never
  bodies, so no key/token/personal data can appear.
- `data/Db.kt`: schema v6 → v7, ADDITIVE ONLY (`MIGRATION_6_7` +
  `migration67Sql()`), `api_diagnostics` table, DAO registered.
- `data/network/ApiStore.kt`: constructor gained optional
  `diagnosticsDao` + `hasValidatedInternet` lambda (defaults keep old
  constructions compiling). `CachedState` gained `failureClass`
  (null = last refresh succeeded or none run yet). `recordDiagnostic`
  (best-effort, never breaks refresh) + public `allDiagnostics()`.
  `cacheFirstFlow` and `refreshApi`'s `store()` record SUCCESS / the
  classified failure on every path; parse failure on a fresh body
  records PARSE_ERROR and never poisons the cache. Toggle-off / no-key
  gates still return before touching rows. `isOffline` semantics
  unchanged. Workers still return success on failure (no retry storms).
- `HavenApp.kt`: `apiStore = ApiStore(db.apiCache(), db.apiDiagnostics())
  { hasValidatedInternet() }`.
- `ui/TodayApiCards.kt`: `ApiStatusLine` priority — retrying →
  "Loading…", then pure `failureLineFor`: OFFLINE + data → "Updated
  HH:MM"; OFFLINE no data → "Offline. Nothing saved yet.";
  RATE_LIMITED → "Busy right now. Try again later." (no Retry button,
  respecting backoff); every other class → "Couldn't load." + Retry
  (only when the toggle is on); null → existing age-based CardStatus
  logic. `ApiEmptyLine`: "check your connection" ONLY for OFFLINE.
- `ui/settings/hubs/DeviceDataHub.kt` (this session): "API diagnostics"
  subsection under Advanced → Diagnostics, loaded once when Advanced
  opens: per API, last result class, HTTP code (when any), HH:mm time,
  body snippet when present. Empty state: "No API results recorded yet."

### A3 — Crypto key nag (done, pre-compaction session)
- Pure `CryptoCardState` (HIDDEN / KEY_NEEDED / PRICES) +
  `cryptoCardState(apiCryptoOn, keyBlank)`; `cryptoAddKeyTarget()` =
  route "apis", field "coingecko-key". `SettingsState.apiCrypto`
  defaults false (new installs Off). Never-fetch-without-key already
  held (`refreshApi` returns false when the key is blank).
- CryptoCard key-blank state is compact: header + "A free CoinGecko
  Demo key is needed to see prices." + "No prices are fetched without
  a key." + ONE "Add key" button (rendered when the host opener exists;
  all real compositions are under the home host).
- Shared deep-link mechanism (also serves A10): `SettingsPanel.kt`
  gained `PanelDeepLink(route, field, nonce)`, `LocalPanelDeepLink`,
  `DeepLinkAnchor` (one-shot scroll-into-view + optional focus,
  best-effort), `SettingsPanelState.deepLink` + `deepLinkTo` /
  `clearDeepLink`; `PanelScreenContent` split so an anchor in another
  hub can never fire on a stale link. `Home.kt` gained
  `LocalOpenSettingsPanel` + `openSettingsPanel(route, field)`; the
  home host provides it (covers TodayRail + PagedHome → TodayPage).
  `ApiHub.kt`: `ApiKeyField` gained `deepLinkField`; the CoinGecko
  field passes `"coingecko-key"`.

### A4 — Next page leaks
- Root cause (confirmed in code, not assumed): the neighbour page at
  rest sits at pageOffset ±1, so the 8% parallax shift moves its
  content 8% of a page width INTO the current viewport, and
  `graphicsLayer` never clips — that is the sliver (icons + glance
  fragment at the right edge).
- Fix: `.clipToBounds()` on the page Box in `ui/PagedHome.kt`
  (parallax value unchanged, now contained). Page width = viewport
  width already holds structurally (HorizontalPager default page size
  + pager filling the remaining column after rail/panel); no width
  code was changed. Parallax math extracted to pure `pageParallaxX`
  (tested). A JVM test cannot assert Compose modifier clipping, so
  the on-device check is a Phase G tablet item ("no next-page sliver"),
  not a faked unit test.

### A5 — Glance ghost
- Root cause (confirmed in code): PagedHome received the pager-page
  flag (`todayForPager`), so in rail and strip modes its glance row
  was composed beside/under Today surfaces that already show
  date/weather.
- Fix: PagedHome gained `todayVisible` (raw Today toggle; defaults to
  `todayOn` for existing callers); the host passes the raw toggle.
  Pure `glanceVisible(todayVisible) = !todayVisible` (tested for all
  four modes). Net behaviour change: glance hidden in rail + strip
  modes; pager-page mode unchanged (host line still shows on grid
  pages); Today-off unchanged (PagedHome glance shows). The Medium
  strip's collapsed header already shows date/weather/event, so
  hiding the glance there removes a duplicate, not information.

### A6 — Today rail under dock
- `TodayRail` gained `bottomInset: Dp = 0.dp`; scroll padding bottom
  is now `16.dp + bottomInset`. Host passes dock height + 16dp for a
  bottom dock, 0 for side docks (which sit outside the rail column).
  Pure `railBottomInsetDp` (tested). The Medium strip is above the
  workspace and needs no inset.

### A7 — Dock Search / Library glyphs
- `DockActionButton` (`ui/Dock.kt`) is now a proper icon tile: same
  48dp size (`DOCK_ICON_DP`, unchanged), same 14dp tile shape, surface
  and 1dp hairline border as the app-icon tiles beside it (was a bare
  glyph in a translucent circle). Glyph sizes kept (22dp Search,
  26dp Library) — the complaint was the missing tile, not the glyphs;
  Phase B's HavenIcon pass normalises glyphs. Dock capacity math
  (~72dp pitch) untouched. Content descriptions are now "Search" and
  "Library" ("All apps" renamed; no test referenced the old string).
  Tile restyle is compositional, so it is covered by the Phase G
  tablet check, not a unit test.

### A8 — Ghost text during transitions (done, pre-compaction session)
- Root cause: the old transition slid by 1/3 width, leaving a middle
  third where outgoing + incoming rows drew on top of each other.
- Fix (`SettingsPanel.kt`): shared-axis with FULL-width offsets
  (incoming from `it`, outgoing to `-it`); `AnimatedContent` still
  removes outgoing from composition when finished. Duration:
  `speedDuration(220, animSpeed)` (220 ms at Normal, scaled by the
  Motion speed control); Reduce Motion → 100 ms fade only (was
  instant); Motion Off → instant (unchanged).
- Header one title: `breadcrumb` returns "" below depth 3, removing
  "Settings › Settings" and "Settings › Look" duplications.
- Segmented controls verified already drawing each label once (no
  sliding thumbs exist yet): `HubSegmented`, `ShellSegmentedControl`,
  `DsSegmentedRow`, `InlineSegmented`, `LibrarySegmentedControl`.
  Phase B's HavenSegmented (with a real sliding thumb) must preserve this.

### A9 — Two undo systems + density (scoped adaptation — see Deviations)
- What shipped: opening Settings from edit mode FIRST exits edit
  mode (coordination-fix §6 invariant, deliberate — "never a stacked
  toolbar-plus-panel state"). So no settings change can be made
  "while edit mode is open" through the panel; the prompt's premise
  does not occur in shipped code. The panel's Changes list therefore
  remains the revert path for ALL panel sessions, per the prompt's
  own "outside edit mode" clause. No stacked toolbar+panel state was
  built.
- What was built instead, at the seam where settings and layout
  actually intersect (the commit path):
  - `UndoFrame` gained `gridOverride: GridOverrideFrame?`
    (landscape + cols/rows to restore, -1/-1 = clear) and
    `label: String?`. `GridOverrideFrame.encode/decode`
    ("L:8:6" / "P:-1:-1", "" = none) +
    `gridActionForCurrentFrame` (mirror logic for redo frames) +
    `UndoStack.peekUndo/peekRedo/undoLabel/redoLabel`, all tested.
  - `LayoutStore.commitMutationInternal` GridChange with
    `followDensity=true` now captures the doomed override BEFORE the
    clear (from the pre-commit `settingsState`) and pushes a frame
    carrying it + label "density grid reset". The no-op early return
    no longer swallows a cleared override (clearing changes future
    density behaviour even when the layout is unchanged).
  - `applyFrame` restores or clears the override. `undo()`/`redo()`
    build their mirror frames from the current settings (peek, then
    pop) so redo re-clears and re-undo re-restores.
  - Durable: `undo_entries` gained `gridJson` TEXT (default ""),
    schema v7 → v8, ADDITIVE ONLY (`MIGRATION_7_8` +
    `migration78Sql()`); all four durable inserts write it;
    rehydration parses it. Labels are session-only (plain Undo/Redo
    after a restart — documented degradation, not a second column).
  - Toolbar: `EditToolbar` gained `undoLabel`/`redoLabel` params
    (defaults "Undo"/"Redo"); the host passes "Undo $label" /
    "Redo $label" from new `LayoutStore.undoLabel/redoLabel` flows.
    `EditToolButton` sets an accessibility `contentDescription` =
    label even when collapsed (collapsed shows the glyph only).
  - Density confirmation (`HomeDockHub` GridPickerRow): tapping
    "Follow density" while the current tab has a custom grid now
    shows "Follow density? / Density will reset 1 custom size. Keep
    it or reset?" with Keep (default, dismiss) + Reset (commits
    through the same undoable GridChange path, so toolbar Undo
    restores the override). Grids are per-orientation, so one tap
    resets exactly one custom size — that is the N=1 mapping
    (documented deviation from the prompt's generic N).
  - Precedence verified + tested: `gridCells` honours per-page
    overrides over density (`SettingsState(density=40, colsLand=8,
    rowsLand=6).gridCells(true) == 8×6`; density-only = 6×5 land,
    4×7 port); density changes never clear overrides in code — the
    only density-driven reset is Follow density, now confirmed +
    undoable. Per-item spans survive reflow except clamping (existing
    `reflowForGrid` contract, unchanged).
  - The old "undo-after-dock-drag-out leaves app in neither place"
    issue was already closed by the coordination fix's full-state
    undo payload; A9 does not regress it (dock/hidden restore in
    `applyFrame` untouched).

### A10 — Long-press menu names = hubs (done, pre-compaction session)
- `Home.kt`: `QuickMenuDestination` (7 values) + `quickCustomizeMenu()`
  = [Wallpaper→WALLPAPER, Layout→GRID, Widgets→WIDGET_SHEET,
  Dock→DOCK, Look→LOOK, Home settings→HOME_DOCK, Edit home→EDIT_HOME].
  "Style" renamed to "Look" (old label must not survive — tested).
  Host bar rewritten to iterate the table (statement-when in onClick;
  the earlier val-action form never compiled — a when branch's braces
  parse as a block, not a lambda — fixed this session, see Checks).
  BEHAVIOUR CHANGE per prompt, noted: "Home settings" previously
  opened route "home" (hub screen); it now opens "home-dock".
- Anchors: `HomeDockHub` GridPickerRow wrapped in
  `DeepLinkAnchor("grid")`; dock-position segmented wrapped in
  `DeepLinkAnchor("dock")`; `LookHub` `HubMore` gained `deepLinkField`
  (header wrapped, `forceOpen` when the link matches, later manual
  close sticks); Wallpaper call site passes `"wallpaper"` — so
  "Wallpaper opens the wallpaper picker" is real despite the section
  being collapsed by the coordination fix.
- This table is event flow 13 (A11).

### A11 — Four new event flows
- Flow 13 (long-press menu → destination): the `quickCustomizeMenu()`
  table + `QuickMenuTest` (seven items in order, destinations unique
  and complete, no "Style" label, full prompt mapping).
- Flow 14 (Mascot Find): resolution reuses the existing adapter —
  `rankFind` (query → ranked candidates via the existing
  `searchMatchBand` adapter; NO second index) + `decideFind` (Act /
  Choices ≤4 / NoMatch). Existing `MascotFindTest` already covered
  single → Act, ties → Choices ≤4, empty → NoMatch; this session
  added the strictly-best-first rule (first band strictly less →
  Act). Navigation / pointing / cancel are Phase E's MascotLayer
  work, not Phase A — the flow's pure core is what A11 asks for.
- Flow 15 (Island feeds): pure `pickIslandActivity(notifications,
  lastIncreaseMs, dismissedBurstAt, media, charging, nowMs)` in
  `IslandModel.kt`, mirroring `rememberIslandActivities` exactly
  (burst live only while total > 0, newer than dismissed, inside the
  6s window; priority Notification > Media > Charging via
  `islandPriority`; else null). The feed keeps its list for the
  expanded view; the pill is this function's result. Tested
  (live/expired/dismissed/zero-total/fallbacks/priority).
- Flow 16 (toggle/key change): pure `apiWorkerAction(api, isEnabled)`
  (SCHEDULE / CANCEL / NONE) in `ApiRefreshScheduler`, tested for
  every API (disabled → CANCEL always; enabled + cadence → SCHEDULE;
  dictionary → NONE). Wiring: `scheduleAll(context, isEnabled)` now
  cancels disabled workers (was KEEP-only); `applyWorkerAction` for
  single-API edits; `HavenApp` passes live toggles at launch;
  `ApiHub.toggle()` reschedules at once on every toggle edit.
  Key edits need no scheduler call: the worker is already scheduled
  and `refreshApi` gates on the key at run time. Card state follows
  via A2/A3 machinery; the Diagnostics row via A2 recording.
  All four flows will be documented in the manual in Phase G.

### A12 — Performance
- Hypothesis check (legibility): the prompt's "every debounced
  tick" claim is PARTIALLY true in code — `rememberLegibilityCells`
  is keyed on wallpaper + grid, so density ticks (which change the
  grid) redid the whole Bitmap analysis, while icon-size ticks did
  not (grid unchanged). Live-preview home recomposition on ticks is
  by design (the preview IS the home rendering new state).
- Fix (`ui/Legibility.kt`): analysis split into `downsampleLuminance`
  (Bitmap → `LuminanceMap`, cached ONCE per wallpaper identity via
  `WallpaperLegibilityCache.mapFor`) + pure `aggregateCells` (map +
  grid → cells, tested). `analyzeWallpaper` kept as the composed
  entry point (same signature). `rememberLegibilityCells` now
  downsamples once per wallpaper and re-aggregates from the cached
  map on grid change; Bitmap work never reruns on a slider tick.
  Semantics preserved exactly (first-wins caching, same as before;
  a late bitmap with an unchanged key keeps the first sample — see
  Unverified/observations).
- Island polling REMOVED (`ui/island/IslandFeed.kt`): the 2s
  `while(true) { readMediaSession; delay(2_000) }` loop is replaced
  by `MediaSessionManager.OnActiveSessionsChangedListener` +
  per-controller `MediaController.Callback` (metadata → refresh,
  session destroyed → refresh, playback-state → flip the playing
  flag only when it flips, so position ticks on some players cause
  no churn). All registrations owned by one `DisposableEffect` with
  cleanup. `readMediaSession` (only caller was the loop) replaced by
  pure `mediaFromSessions`; the three stale "next poll clears the
  activity" comments updated. Charging (sticky broadcast) and
  notifications (BadgeStore counts + 6s window) were already
  callback-driven and are untouched. NOTE: the Now Playing widget
  (`Widgets.kt`) has its own separate media read — different surface,
  on-demand per composition, NOT a 2s loop; left untouched (scope
  boundary, not a second poll loop).
- Workers: `scheduleAll` cancels disabled APIs' unique work
  (`cancelUniqueWork`), keeps one unique job per enabled API, and
  still returns success on worker failure (no retry storms). Toggle
  edits reschedule immediately (A11 flow 16).
- Refresh-on-open: `cacheFirstFlow` re-checks staleness only on fresh
  collection, and home composition survives overlays — so a
  deferred-worker stale cache could sit forever. Fix: `HavenApp`
  gained `homeResumedTick` + `noteHomeResumed()`; `MainActivity`
  bumps it in `onResume`; all five Today cards key their flow
  collection on it (`homeResumeKey()`), so returning to home
  re-runs the stale check and refreshes ONLY when stale (fresh
  caches emit with one Room read each, no network). Drawer search
  rows compose fresh on every drawer open, so they already had
  refresh-on-open and were left alone.
- Baseline profile / macrobenchmark: the project does NOT use
  macrobenchmark (grep: no matches) — per the prompt, nothing was
  added; recorded as not-applicable. NO numbers are reported (none
  were measured; no device exists here).

### A13 — Safety
- Launch restore: new `restoreNavBarOnLaunch(ctx, settings)` in
  `HavenAccessibilityService.kt` — on every launch, if
  `policy_control == immersive.navigation=*` but the settings say
  the bar should be shown (gestures off or hide toggle off),
  restore the saved value, or the system default when nothing was
  saved. Deliberately refuses when the settings still say hide
  (a connected service owns that case; a paused one must not fight
  the user). Never throws (grant may be gone). Wired in
  `HavenApp.onCreate`'s IO coroutine (best-effort, separate try).
- Manual row: "Restore navigation bar" in Gestures and Motion
  (`GesturesMotionHub.kt`), after the navigation-setup buttons:
  value shows the Hide setting ("Hide on"/"Hide off"); tapping sets
  Hide OFF first (required — the launch-restore refuses while the
  settings still say hide, and a connected service would re-hide on
  next sync), then restores. Help text states both effects.
- Paused-service banner: service gained a `companion isConnected`
  flag (set in `onServiceConnected`, cleared in `teardown()` — which
  runs only from `onUnbind`/`onDestroy`; same process, no IPC).
  Pure `gestureServicePaused(gesturesOn, serviceEnabled,
  serviceConnected)` (tested). `HubScreen` gained the banner
  ("Gestures are paused." + "The system stopped the gesture service.
  Reconnect it to restore edge swipes." + "Reconnect" button opening
  `ACTION_ACCESSIBILITY_SETTINGS`), slotted after the
  permission-missing banner and before backup-failed; priority
  comment updated. Freshness uses the gestures hub's own pattern
  (`navTick` + `LifecycleEventEffect ON_RESUME`), so returning from
  the system screen clears the banner.
- Honest limitation (not a bug to fix in code): uninstall cannot run
  any code, so an uninstall while hidden cannot self-restore. The
  mitigations are the launch restore, the teardown restore (service
  going away already restored via `restoreNavBarPolicy`), and the
  manual row. Crash/kill are covered by the launch restore.

## Tests added (all written, NONE run — see Checks)
- `ui/ScoresDisplayTest.kt` (A1): null scores, one-null-team,
  both-missing → null, unknown status, live/finished/upcoming,
  dash scores, literal "null" scores, no rendered string contains
  "null" (case-insensitive), status table maps only known codes.
  Upcoming-time assertion uses a `\d{2}:\d{2}` regex, NOT a hardcoded
  time (device timezone changes the conversion).
- `data/network/ApiClassificationTest.kt` (A2): `classifyFailure`
  per class; OFFLINE + validated → NETWORK_ERROR, never OFFLINE;
  OTHER + 503 → SERVER_ERROR, OTHER + 301 → OTHER; class set equals
  exactly the seven classes; snippet capped at 200;
  `migration67Sql()` pins one statement with all five columns + PK.
- `ui/ApiCardStatesTest.kt` (A2/A3): `failureLineFor` per class;
  `cryptoCardState` three states; `cryptoAddKeyTarget()` route +
  field.
- `ui/QuickMenuTest.kt` (A10/flow 13): seven items in order,
  destinations unique + complete, no "Style" label, full mapping.
- `ui/PageGeometryTest.kt` (A4/A5/A6): parallax values (rest ±8%,
  mid-swipe, off/reduced-motion zero); glance rule for all four Today
  forms; rail inset (bottom = height + 16, side = 0).
- `data/UndoSettingsTest.kt` (A9): grid-frame codec round-trips
  (restore + clear) and malformed → null; `gridActionForCurrentFrame`
  (null / cleared → Clear / set → Restore / Clear-frame → Restore);
  stack labels follow frames (undo + redo); `gridCells` precedence
  (override 8×6 beats density 40; density-only 6×5 land, 4×7 port;
  override still wins at density 80); `migration78Sql()` pins the
  single ALTER statement.
- `ui/island/IslandFlowTest.kt` (flow 15): live burst wins; expiry
  boundary (5999ms live, 6000ms dead); dismissed burst stays dead;
  media → charging fallbacks; zero-total burst is no burst; nothing
  → null; priority numbers 0/1/2.
- `data/network/ApiWorkerActionTest.kt` (flow 16/A12): disabled →
  CANCEL for every API; enabled + cadence → SCHEDULE; dictionary →
  NONE.
- `ui/LegibilityA12Test.kt` (A12): empty map → mid-grey cells;
  whole-map mean/variance; column split; one map serves every grid.
- `NavBarSafetyTest.kt` (A13): paused only when wanted + granted +
  disconnected; connected / permission-missing / gestures-off all false.
- `ui/mascot/MascotFindTest.kt` (flow 14, added): strictly-best-first
  → Act (existing tests already covered single/ties/empty).
- Pre-existing tests untouched except that addition. No test file was
  modified to weaken an assertion.

## Checks run + exact results
- `./build.sh compile` (after A4–A7 + latent-error fixes):
  GRADLE_EXIT=0, BUILD SUCCESSFUL in 34s (incremental).
- `./build.sh compile` (after A9): GRADLE_EXIT=0, BUILD SUCCESSFUL
  in 10m 44s (Room schema change → KSP + full recompile).
- `./build.sh compile` (after A2-diag + A11 + A12 + A13):
  GRADLE_EXIT=0, BUILD SUCCESSFUL in 10m 15s.
- `./build.sh` (full Phase A verification build): GRADLE_EXIT=0,
  BUILD SUCCESSFUL in 37s (incremental on the passing compile).
- Verification APK: `app/build/outputs/apk/release/app-release.apk`,
  2,682,104 bytes, md5 `59390b74a2bec6f58a49f0cc81bcec0f`
  (verification build only — NOT delivered anywhere).
- `aapt dump badging`: package `app.havenos.launcher`,
  versionCode 12, versionName 3.0, minSdk 26, targetSdk 36,
  label HavenOS.
- `apksigner verify --print-certs`: CN=HavenOS, SHA-256
  `53bff2a09aedb516a8ee6193234a7aaf5d04ce901fb8931b799191b9845354a8`
  — matches the persisted havenos keystore. Exit 0.
- Secrets grep over new/changed code: no keystore paths, passwords,
  or tokens (diagnostics snippets are parse-errors-only by
  construction; keys travel in headers).
- Diff vs `~/workspace/havenos-pre-3.1-phase-A/`: 27 modified source
  files + 9 new test files + 1 modified test file (list in Files
  touched). Build outputs differ as expected (excluded from the
  report's scope).
- Tests: NOT RUN. The sandbox test executor is broken
  (pre-existing) and `SettingsShellTest` has 10 pre-existing compile
  errors. Every test above is recorded as written-but-not-run. No
  test result is claimed anywhere in this report.

## Latent errors fixed this session (not Phase A features)
The pre-compaction Phase A work had never been compiled. The first
Phase A compile failed on it (not on A4–A7):
- `ApiStore.kt:250`: `result.kind` / `result.httpCode` unresolved
  after the Success early-return (smart cast did not apply) → explicit
  safe cast `result as? ApiResult.Failure ?: return false` (fail
  closed: keep cache, no diagnostic for an unknown shape).
- `TodayApiCards.kt`: `ApiFailureClass` unresolved → added the
  `data.network` import (other network symbols were already imported).
- `Home.kt` quick-customize bar: the `val action: () -> Unit = when`
  form never compiled (a when branch's braces parse as a block of
  type Unit, not a lambda) → replaced with a statement-when inside
  onClick (same mapping, no behaviour change).

## Deviations from the prompt (with reasons)
1. A9 premise ("settings changes made while edit mode is open"):
   shipped code exits edit mode when Settings opens (coord-fix §6,
   deliberate). Trusted the code: no stacked toolbar+panel state was
   built; the Changes list remains the panel revert path, and the
   shared timeline was built where settings and layout intersect
   (commit path: grid-override restore + toolbar labels).
2. A9 density "N custom sizes": grids are per-orientation, so one
   Follow-density tap resets exactly one custom size; the dialog
   says "1 custom size" (singularised prompt sentence), Keep default,
   Reset undoable. Sliders never reset overrides (precedence holds),
   so no dialog was added to any slider.
3. A9 undo labels after restart: plain Undo/Redo (labels not
   persisted — one migration column, not two). Grid actions DO
   survive restart.
4. A4 "layout test asserting page width = viewport width": page
   width holds structurally (pager fills the remaining column);
   a JVM test cannot assert Compose modifier clipping, so the
   clip is covered by the Phase G tablet check, and the pure
   parallax math is unit-tested instead. No fake test was written.
5. A12 baseline profile / macrobenchmark: project does not use
   macrobenchmark — nothing added, recorded as not-applicable.
6. A12 legibility: prompt's "every tick" hypothesis was partially
   true (grid-changing ticks only); the prescribed split was
   implemented anyway, semantics preserved.
7. Flow 15: the pure picker pins the rule; the feed itself was NOT
   rewired to call it (its ticker loop owns the window expiry and
   rewiring risked that behaviour for no user-visible gain).
8. A13 uninstall: no code can run after uninstall — documented as a
   limitation, not presented as fixed. Crash/kill are covered.

## Dependencies added
None.

## Files touched (source)
- `AndroidManifest.xml` (ACCESS_NETWORK_STATE)
- `HavenAccessibilityService.kt` (paused predicate, launch restore,
  connected flag)
- `HavenApp.kt` (diagnostics DAO + validated check, scheduler
  toggles, launch restore, resume tick)
- `MainActivity.kt` (onResume → resume tick)
- `data/Db.kt` (v7 diagnostics table, v8 gridJson column, migrations)
- `data/LayoutState.kt` (GridOverrideFrame, codec, mirror logic,
  UndoFrame fields, stack label/peek accessors)
- `data/LayoutStore.kt` (cleared-override capture, frame push,
  applyFrame grid action, undo/redo mirrors, rehydrate, label flows)
- `data/network/ApiCache.kt` (diagnostics row + DAO)
- `data/network/ApiClient.kt` (SERVER_ERROR, ApiFailureClass,
  classifyFailure, hasValidatedInternet)
- `data/network/ApiStore.kt` (diagnostics recording, failureClass,
  allDiagnostics, safe-cast fix)
- `data/network/ApiWorkers.kt` (worker actions, cancel, enqueue split)
- `ui/Dock.kt` (action-button tile, "Library" description)
- `ui/EditMode.kt` (toolbar labels, button accessibility labels)
- `ui/Home.kt` (todayVisible, rail bottomInset, toolbar labels,
  quick-menu statement-when fix)
- `ui/Legibility.kt` (luminance-map split, map cache)
- `ui/PagedHome.kt` (pageParallaxX, clipToBounds, glanceVisible)
- `ui/TodayApiCards.kt` (A1 display model, A2 status lines,
  A3 key-needed state, resume-keyed flows, ApiFailureClass import)
- `ui/TodayRail.kt` (railBottomInsetDp, bottomInset param)
- `ui/island/IslandFeed.kt` (callback media, no poll, mediaFromSessions)
- `ui/island/IslandModel.kt` (pickIslandActivity)
- `ui/settings/hubs/ApiHub.kt` (deep-link field, toggle reschedule)
- `ui/settings/hubs/DeviceDataHub.kt` (API diagnostics rows)
- `ui/settings/hubs/GesturesMotionHub.kt` (Restore nav-bar row)
- `ui/settings/hubs/HomeDockHub.kt` (grid/dock deep-link anchors,
  follow-density confirmation)
- `ui/settings/hubs/LookHub.kt` (wallpaper deep-link anchor)
- `ui/settings/shell/HubScreen.kt` (paused banner, resume tick)
- `ui/settings/shell/SettingsPanel.kt` (shared-axis transition,
  breadcrumb fix, deep-link mechanism)
- Tests: new `ScoresDisplayTest`, `ApiCardStatesTest`,
  `QuickMenuTest`, `PageGeometryTest`, `UndoSettingsTest`,
  `IslandFlowTest`, `ApiWorkerActionTest`, `LegibilityA12Test`,
  `NavBarSafetyTest`, `ApiClassificationTest`; modified
  `MascotFindTest` (one test added).

## Unverified items (need the Redmi Pad 2 — Phase G checklist)
- No next-page sliver at rest (A4 clip) — portrait/landscape,
  rail on/off, Settings open.
- Glance hidden beside rail and under strip; shown when Today off (A5).
- Last Today card scrolls fully clear of a bottom dock (A6).
- Dock Search/Library tiles look like app-icon tiles; descriptions
  read "Search"/"Library" (A7).
- Hub transitions show no ghost frames; segmented labels drawn once (A8).
- Follow-density dialog copy + Keep/Reset; toolbar Undo reads
  "Undo density grid reset" and restores the override; Redo
  re-clears it (A9). Undo label after restart is plain (documented).
- Long-press menu destinations + labels on device (A10).
- Crypto three states + Add-key deep-link lands on the CoinGecko
  field, scrolled into view and focused (A3).
- Scores with null data on device (A1); every A2 card state with
  Wi-Fi on (Offline / NetworkError / RateLimited / NotFound /
  ParseError / ServerError) + Diagnostics rows populated (A2).
- Island media callbacks: track change / play-pause / session end
  update the pill with no poll (A12). Notification burst window
  behaviour unchanged.
- Today cards refresh on home return after a deferred worker (A12).
- Nav-bar launch restore after a kill while hidden; Restore row;
  paused banner appears when the system stops the service and clears
  on Reconnect + return (A13).
- Room v7→v8 migration on an existing install (upgrade from the
  shipped vc12 / coord-fix vc12 test build): layouts, backups,
  snapshots, undo depth all intact.
- All Phase A tests, once the sandbox executor works (written,
  not run).

## Observations (not Phase A scope, recorded not fixed)
- Late-bitmap legibility: `rememberLegibilityCells` caches the first
  sample per key; a HavenOS bitmap arriving late with an unchanged
  wallpaper key keeps the first (mid-grey) sample. Pre-existing
  behaviour, preserved exactly by the A12 split — flagged for a later
  pass, not changed here.
- The pre-compaction Phase A work had never been compiled; three
  latent errors were fixed this session (see above). Nothing else
  in the tree was found uncompilable.

## Snapshots
- Pre-phase: `~/workspace/havenos-pre-3.1-phase-A/` (155 MB, intact).
- Post-phase: `~/workspace/havenos-post-3.1-phase-A/` (156 MB).
- Pre-3.1 baseline: `~/workspace/havenos-pre-3.1-2026-10-30/` (155 MB).

## Next
Phase B (Haven Glass + accent system) — blocked only on the parent's
go-ahead. Snapshot `havenos-pre-3.1-phase-B` will be taken first, per
the standing rule. No APK is delivered with this report; the
verification APK above stays in the build directory for the parent's
delivery decision.
