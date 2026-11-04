# UNVERIFIED — HavenOS

Things not measured or tested. Builder prompt §0: never claim these.

## 3.1.1 hotfix (2026-10-31) — NOT verified in the build sandbox
- Everything visual or gesture-related in the hotfix is built but NOT
  proven: the Safe Mode banner (appearance, Re-enable exactness, View
  details share/copy), the per-feature disable toasts, the spotlight
  dim tap-vs-swipe behavior, the mascot hitbox following the drawn
  sprite on device, the bubble's pixel-stepped border and four chips
  at real sizes, the settings layout at every matrix width and font
  scale, and the drawer looking fully solid with a bright wallpaper,
  keyboard up, and Reduce Effects on/off. See DEVICE_CHECKLIST.md
  section "3.1.1 hotfix".
- The hotfix unit tests (HotfixCrashSweepTest, SettingsLayoutMatrixTest,
  CrashLoopTest, drawer tests in HexLiteralTest) were written but NOT
  run: the test tree still does not compile (pre-existing 305 errors
  plus the new files unmeasured against them) and the sandbox test
  executor is broken. The test-compile error split is reported exactly
  in docs/3.1-phase-reports/HOTFIX-3.1.1.md.
- The hotfix UI tests (MascotTouchTest, DrawerSolidTest) were written
  but NOT run and NOT compiled here: no device/emulator in the
  sandbox and compose UI-test artifacts are not in the offline cache.
- The vc13 crash trace in the hotfix brief CANNOT be retraced: its R8
  map id is given but no mapping.txt was retained for vc13. The
  firstNotNullOf site fixed in MascotLayer placementOf is a prime
  suspect for the crash class, NOT a proven vc13 crash line.
  Mapping retention starts with vc14 (release-artifacts/mapping-14.txt).
- The PagedHome cell `appEntry!!` / `folder!!` and ghost
  `overCell!!` sites observed during the item-1 sweep were NOT fixed
  (out of the briefed sweep scope) and are NOT device-verified either
  way; they need a scoping decision before any fix.

## M3 (2026-10-20) — NOT verified in the build sandbox
- All N-section gesture behavior is device-only and has never run on the real
  Pad: accessibility-service survival under HyperOS battery management,
  overlay rendering, the `policy_control` hide/restore round-trip, and the
  Recents bar reveal-flash. See DEVICE_CHECKLIST.md section N.
- Phase 2 interactions are likewise code-reviewed only: drag reflow feel,
  folder resize, and widget-picker preview loading on device.
- The new unit tests (NavGesturesTest, GridTest additions) were written but
  NOT run — same pre-existing sandbox test-executor crash as M1/M2.

## M2 (2026-10-20) — NOT verified in the build sandbox
- Everything visual is device-only: whether the glass tiers actually look like
  the v2.2 references on the Redmi Pad 2 (blur radius/quality, specular border
  visibility, shadow softness), template layouts at real catalog sizes, quick
  panel feel, free-canvas drag feel, gesture reliability (pinch / two-finger /
  shade on HyperOS). See DEVICE_CHECKLIST.md section E.
- The vc1 → vc2 upgrade path (Room migration 1→2, all M1 settings carrying
  over) is code-reviewed but has never run on a device. Device check E1 is
  the first real test of it.
- Unit tests were again written but NOT run (same pre-existing sandbox
  test-executor crash).
- Weather has never fetched live data from this sandbox (no device network
  test); parsing is code-reviewed against the Open-Meteo response shape, and
  the file cache path is untested at runtime.
- Notification listener, media-session polling, device-admin lock, Keyguard
  credential prompt, icon-pack parsing against a real pack, hosted third-party
  widget bind/configure flows: all code-reviewed, none device-run.
- Profile schedule switching and the daily auto-backup have never fired at
  runtime.
- No performance numbers (cold start, scroll fps, blur cost on the Pad's GPU).

## M1 (2026-10-20) — NOT verified in the build sandbox
- **No device or emulator exists in the build sandbox.** Everything about visual
  quality, blur feel, haptics, frame rate, and gesture feel on the Redmi Pad 2 is
  device-only — see DEVICE_CHECKLIST.md.
- Unit tests (app/src/test) were written but NOT run: the Gradle test executor
  crashes in this sandbox (pre-existing, verified with changes stashed on prior
  builds). Marked not-run; correctness claims for grid logic come from code review.
- Compose version alignment (material3 1.5.0-alpha17 ↔ compose 1.11.0-beta02) was
  verified against material3's POM + pinned in the build; a green build does not
  prove runtime compatibility — the first device run does.
- Room schema, DataStore reads/writes, KSP codegen: compile-verified, never
  executed.
- Package receiver live add/remove, work-profile querying, icon loading under
  load: code-reviewed only.
- Blur-behind (`FLAG_BLUR_BEHIND`) on HyperOS V816: OEM/runtime-revocable;
  failure path is a silent try/catch fallback to the translucent surface.
- No performance numbers are claimed (no cold-start time, no frame stats).
- No accessibility audit was run (TalkBack/large fonts are untested on device).
- No security review, store readiness, or production-readiness claims are made.
- Adaptive icon: XML validated; how it renders in the system launcher's icon
  shape + themed-icon (monochrome) slot is device-only.
- M7 (2026-10-23): `:app:testDebugUnitTest` re-attempted for vc7 — same
  pre-existing Gradle test-executor crash (NullPointerException in
  TestResultProcessor, no test results produced). No test was run, none
  is claimed to pass; see the M7 integration report for the test files
  that exist.

- Coordination fix (2026-10-29): all 7 steps implemented; per-step
  `:app:compileReleaseKotlin` checks passed, mid-build after steps
  1–3 was BUILD SUCCESSFUL with APK verified, and the FINAL full
  verification build (`build.sh`, 2026-10-29) was BUILD SUCCESSFUL
  in 7m 28s: APK 2,665,728 bytes, md5
  `2a2688ec8c0398749465ca7c4d309722`, aapt vc12/3.0/minSdk 26/
  targetSdk 36, apksigner CN=HavenOS cert match (verification build
  only; delivered vc12 file untouched). Build log:
  `docs/coord-final-build.log`. Tests written but NOT run (same
  pre-existing executor breakage; none claimed to pass):
  - data/CoordinationProfilesTest.kt (7 tests: materialize overlay,
    dirty JSON round-trip, old-JSON default, oversize-first-use gate,
    repair moves-overlaps-only + identity).
  - data/CoordinationInstallTest.kt (5 tests: auto-add on/off,
    absent-profile, existing/blank, spill to next free spot).
  - data/CoordinationGridTest.kt (2 tests: densityGridCells ignores
    overrides, clamps to picker range).
  - ui/settings/hubs/CoordinationOverflowTest.kt (4 tests: pager
    positions ±Today, ≤8 all shown, >8 first-8 + more, expanded all).
  - data/CoordinationUndoTest.kt (4 tests: full-snapshot round-trip,
    old-format defaults, migration 5→6 SQL pin, frame stack
    push/pop/cap-50).
  - data/CoordinationRestoreTest.kt (3 tests: first-page filter,
    preview payload + counts line, thumbnail gate).
  - data/CoordinationWorkerTest.kt (6 tests: Fresh/Stale/Expired
    boundaries incl. exactly 2×, EmptyOffline, dictionary never
    expires, negative age Fresh).
- Coordination fix device checks owed: COORD-1…COORD-15 in
  DEVICE_CHECKLIST.md (profile repair, grid undo depth, durable undo
  across process death, dock-drag-out undo, restore thumbnails,
  expired-card Retry, overflow collapse, Changes banner, panel
  layering, hub title bars, Material/Wallpaper collapse, accent
  preview/commit, install/uninstall/disable-enable, Sync undo, Room
  5→6 migration on an existing install).

## 3.1 Phase B (2026-10-30) — NOT verified in the build sandbox

- All Phase B visual/gesture behaviour (no device/emulator): glass
  legibility on bright + busy wallpapers, Reduce Effects fallback,
  curated/strength/clamp rendering, wallpaper suggestion behaviour,
  System source rendering, Custom picker drag feel and thumb
  tracking, hub transitions, switch/segmented springs, slider
  bubble/ticks, Help popover, long-press full text, font scale 1.3,
  island icon rendering, purple-card fix on device.
- WallpaperManager.getWallpaperColors tablet hypothesis: the system
  returns colours for the Pad's wallpaper (Phase B assumes it does;
  empty results render the "No wallpaper colours found" states).
- Migration on a real existing install (old swatch -> nearest
  curated, stored custom kept, mode-3 -> Wallpaper) — pure-function
  tests cover the mapping; the DataStore load path is unproven.
- Two-blur-layer behaviour on screen (documented, not enforced).
- Phase B tests written but NOT run (sandbox executor broken,
  pre-existing; SettingsShellTest has 10 pre-existing compile
  errors):
  - ui/glass/AccentTokensTest.kt (curated contrast dark+light,
    determinism, strength alphas, migration incl. 8 pinned swatch
    mappings, display names, recents, HSV round-trip).
  - ui/glass/GlassTokensTest.kt (tint/hairline alphas, fallback,
    scrims, radii, spacing).
  - ui/glass/EmojiScanTest.kt (code-point scan, fails on offenders).
  - ui/glass/HexLiteralTest.kt (Color(0x…) lint, fails on offenders).
  The script equivalents of the emoji + hex scans were run against
  the tree and pass (0 hits each); the gradle tests themselves did
  not run.
- HavenIcon vector rendering at 12–24dp (outline legibility at small
  sizes is a device check, not a static one).

## 3.1 Phase C (2026-10-30) — NOT verified in the build sandbox

- Everything visual in Phase C is device-only: panel/drawer/sheet
  legibility over real wallpapers, Glass2/Glass3 tint and hairline
  rendering on the Pad, hero miniature fidelity, hub transitions
  (no ghost frames), TalkBack row readings, font-scale 1.3 label
  wrapping, and contrast at real brightness. See DEVICE_CHECKLIST.md
  section 3.1 Phase C (C-1…C-14).
- The new unit tests (C1HubHelpersTest, SearchHighlightTest) were
  written but NOT run — same pre-existing sandbox test-executor
  crash as Phases A/B; SettingsShellTest still has its 10
  pre-existing compile errors.
- The quick-customize Widgets-entry fix (C4) is code-verified only:
  the branch now assigns `showWidgetSheet = true` instead of
  evaluating a lambda literal. Its Pad behavior (sheet actually
  opens from the bar) is check C-8.
- Calm exit restoring home exactly, and island visibility rules
  (hidden in edit mode, drawer, search, Settings, quick panel,
  Calm), are unchanged code paths restyled only — still owed their
  Pad re-checks (C-11, C-12).

## 3.1 Phase D (2026-10-30) — NOT verified in the build sandbox

- Everything visual in Phase D is device-only: the search widget
  as a single Glass1 card over real wallpapers (blur + hairline +
  28dp corners matching the Today cards), the Search vector icon
  at 20dp, and the full-tile tap target. See DEVICE_CHECKLIST.md
  section 3.1 Phase D (D-1…D-5).
- A4/A5/A6 and the A7 dock tiles were re-verified by code review
  against the current tree (no regressions from Phase C); their
  Pad re-checks are still owed (D-3, D-4) — code review is not a
  device check.
- No new tests in Phase D (no new pure logic — see COVERAGE.md
  Phase D); the sandbox test executor is still broken
  (pre-existing) and SettingsShellTest still has its 10
  pre-existing compile errors.

## 3.1 Phase E (2026-10-30) — NOT verified in the build sandbox

- Everything visual/gestural in Phase E is device-only: pixel
  art rendering at integer scales, accent palette-swap, ledge
  standing (never on an icon), roaming/jumping/dropping, carry
  + drop + haptic tick, hide poofs, Find walk + point + ring +
  dim, panel row rings, hub preview animation, TimePicker
  quiet hours, empty-state poses. See DEVICE_CHECKLIST.md
  section 3.1 Phase E (E-1…E-15).
- The ledge graph, planner, route execution, Find flow machine,
  sprite loading/scaling, and atlas parsing are pure functions
  with written tests (MascotEngineTest, MascotSpritesTest) —
  but NONE ran: the sandbox test executor is still broken
  (pre-existing) and SettingsShellTest still has its 10
  pre-existing compile errors. Code review + build success are
  the only verification so far; they are not device checks and
  not test runs.
- No device/emulator exists in the sandbox, so the following
  Phase E behaviors are built-but-not-proven and must not be
  described as working until the Pad checks confirm them:
  roaming weights and dwell/sit/sleep timing, jump reach
  (2 rows up, 3 cells across) on real grids, drop tolerance
  (48dp), poof handoff timing (700ms), IME bubble clamping on
  a real keyboard, dim-tap dismissal, and the Settings row
  ring on every anchored row (accent, dock, widgets, island,
  mascot, find, quiet, reset, home).
- Known Phase E residuals carried openly (see COVERAGE.md
  Phase E BLOCKED): no pixel-stepped bubble border; no
  per-card rail ledges; no side-dock top perch; dock-app Find
  targets get bubble + Launch only (no ring); timers restart
  on resume; guide starting mid-trip resolves within one step.

## 3.1 Phase F (2026-10-30) — NOT verified in the build sandbox

- Everything behavioral in Phase F is device-only: command rows
  in drawer + panel search, compound parsing on real queries,
  the drawer-to-panel pending apply (after begin, so Changes
  shows it), Calm opened from panel search (panel + Calm
  coexistence on exit), ledger switches gating their features,
  Clear learned data, and the Chat model row state. See
  DEVICE_CHECKLIST.md section 3.1 Phase F (F-1…F-13).
- The router, ledger JSON handling, and model-pack gates are
  pure functions with written tests (AssistRouterTest,
  AssistLedgerTest, ModelPackTest) — but NONE ran: the sandbox
  test executor is still broken (pre-existing) and
  SettingsShellTest still has its 10 pre-existing compile
  errors. Code review + build success are the only verification
  so far; they are not device checks and not test runs.
- Model-pack states READY / FAILED / DOWNLOADING are
  unreachable on device until a real model URL + checksum is
  configured (placeholders are empty by design), so the
  download, checksum, Wi-Fi, space, RAM, cancel, and delete
  paths are scaffold-only and unproven. LocalModelBrain
  delegation (flag off → scripted brain) is code-reviewed only.
- Known Phase F residuals carried openly (see COVERAGE.md
  Phase F BLOCKED): no "silent" mapping (no such setting
  exists); mixed app + setting compounds land on the setting
  hub (app still launches); ledger switches are not session-revertible
  (same scope as mascot toggles); ledger history
  is not backed up (device-local ephemeral); panel-search
  command rows do not navigate to the changed control (Changes
  footer is the confirmation surface).

## 3.1 Phase A (2026-10-30) — NOT verified in the build sandbox

- Phase A had no device-check section of its own when Phase G
  started; its tablet items are now consolidated as G-1…G-26 in
  DEVICE_CHECKLIST.md (3.1 FINAL section). The items: next-page
  sliver, glance/rail/strip behaviour, last Today card clearing the
  dock, dock tile look, ghost-free transitions, Follow-density
  dialog + undo round-trip, long-press menu destinations, crypto
  deep-link landing, Scores/A2 card states + Diagnostics rows on
  device, island media callbacks, refresh-on-resume, nav-bar
  restore + paused-service banner, and the Room v7→v8 upgrade
  from a shipped vc12 install.
- Tests: 10 new files + 1 addition written (scores, failure
  classification, card states, quick menu, page geometry,
  undo/settings, island flow, worker actions, legibility,
  nav-bar safety, mascot-find strictly-best) — NONE ran: the
  sandbox test executor is broken (pre-existing) and
  SettingsShellTest still has its 10 pre-existing compile
  errors. The pure parallax math is tested; the clip itself is
  a Phase G tablet check (G-1), because JVM tests cannot assert
  Compose clipping.
- Observation recorded, not fixed: late-arriving HavenOS bitmaps
  with an unchanged wallpaper key keep the first (mid-grey)
  legibility sample — pre-existing behaviour, preserved exactly.
- Phase A deviations carried (see PHASE-A.md): the A9 stacked
  toolbar+panel premise does not occur in shipped code (opening
  Settings exits edit mode); density N=1 per tap; undo labels
  are session-only; macrobenchmark not applicable; flow 15's
  feed was not rewired to the pure picker; uninstall-time
  nav-bar loss is unfixable by code.

## 3.1 Phase G (2026-10-30) — final consolidated state

- G1 checks, each run in Phase G with its exact result: release
  build.sh BUILD SUCCESSFUL in 35s (GRADLE_EXIT=0; APK identical
  to Phase F, 2,825,857 bytes, md5 f4a56ac7…); debug assemble
  BUILD SUCCESSFUL in 6m 50s (GRADLE_EXIT=0; debug APK 16,205,713
  bytes — feasibility check only, not a deliverable); lint
  :app:lintRelease BUILD FAILED in 5m 5s (GRADLE_EXIT=1; 16 errors,
  89 warnings, 35 hints — all pre-existing, none introduced by
  Phase G, none fixed here); unit tests :app:testDebugUnitTest
  BUILD FAILED in 1m 12s (GRADLE_EXIT=1) at
  :app:compileDebugUnitTestKotlin with 305 errors across 10 files
  (MascotEngineTest 126, AssistRouterTest 53, MascotSpritesTest
  45, ModelPackTest 28, AssistLedgerTest 28, SettingsShellTest 10,
  LayoutStateTest 6, EditModeTest 5, HexLiteralTest 2,
  EmojiScanTest 2) — ZERO tests ran. Root causes: Phase E/F tests
  import kotlin.test.* but only JUnit4 is declared; LayoutState /
  EditMode / SettingsShell tests are stale vs the coord-fix APIs;
  2 Phase B scan-test type errors. The sandbox executor breakage
  is a second blocker behind compilation. Instrumentation tests:
  NOT APPLICABLE (no androidTest dirs). Screenshot tests: NOT
  APPLICABLE (no screenshot test files).
- Security grep over Phase G changed files: clean (benign prose
  only). Diff review vs the pre-G snapshot: only
  DEVICE_CHECKLIST.md and UNVERIFIED.md differ in-tree (plus the
  manual source outside the tree, with its own .pre-3.1 backup).
- Manual: SOURCE updated to 3.1 (index.html 954,430 → 1,089,956
  bytes, Figure 7 embedded, HTML parses clean). The delivered PDF
  is NOT rebuilt (pipeline not reproducible: no scripts, no
  Chromium) and therefore still describes 3.0 — rebuild is a
  follow-up; the source is the current record.
- Consolidated tablet checklist: DEVICE_CHECKLIST.md 3.1 FINAL
  section, G-1…G-26, every item NOT RUN, plus all earlier series
  still owed (COORD-1…15, B-1…B-15, C-1…C-14, D-1…D-5, E-1…E-15,
  F-1…F-13, V2-API-1…9, V3-MASCOT-1…8, V3-ISLAND-1…9, older
  M-series). Nothing in 3.1 counts as proven until the Pad runs
  these.
- Final version-stamped build: versionCode 13, versionName 3.1.0;
  build.sh BUILD SUCCESSFUL in 8m 37s, GRADLE_EXIT=0; final APK
  2,825,857 bytes, md5 cc45b27dcf53ca487af36fe68a9cc4b2; aapt
  package app.havenos.launcher, vc13/3.1.0, minSdk 26, targetSdk
  36; apksigner CN=HavenOS SHA-256 match. NOT delivered, NOT
  published — parent owns delivery and the release decision.
- Still unproven, carried openly: WallpaperColors tablet
  hypothesis; two-blur-layer behavior; model-pack states
  READY/FAILED/DOWNLOADING (placeholders empty); mascot art
  quality (human review from preview.png only); lint findings;
  every written test (compile fails first).
- No claim of production readiness, accessibility conformance, or
  security review is made in Phase G.
