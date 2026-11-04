# PHASE-G — Verification, docs, report + final version stamp (2026-10-30)

Scope: builder prompt §G, items G1–G4, plus the prompt's final version
step. Phase G changed NO app source and NO test source — docs and the
manual source only. No APK delivered, nothing published, nothing
uploaded, `~/workspace/havenos-github/` untouched, no token used,
signing untouched (the version stamp is a version-name/code change
only, not a signing change). Snapshot first, as required:
`~/workspace/havenos-pre-3.1-phase-G/` (162 MB, rsync, excluding
`.gradle` and `*.apk`).

Skills read BEFORE their steps: Ponytail (full level), ecc-verification-loop
(six phases, adapted: Gradle build + Kotlin compile + scans + diff review;
no npm/tsc in this project), local-verification-before-completion (Iron Law:
no completion claim without fresh verification evidence — every check below
was run independently in Phase G, not trusted from a prior phase report).
Irrelevant tracks skipped.

No claim is made here of production readiness, accessibility conformance,
or security review.

## What changed per phase (A–G, one tight section each)

- **Phase A — data, coordination, edge bugs.** All 13 items: scores
  null/status table (Abnormal deliberately unmapped); one pure failure
  classifier (Offline only on validated internet; RateLimited shows Busy
  with NO Retry); `api_diagnostics` table (Room v6→v7) + Diagnostics rows;
  crypto three states with key deep-link; page-leak clip fix; glance rule;
  rail dock inset; 48dp dock tiles; shared-axis transitions + breadcrumb
  fix; durable undo grid-override frames (Room v7→v8) + Follow-density
  Keep/Reset dialog; 7-item long-press menu (Style→Look); flows 13–16;
  legibility split + island poll removal + disabled-worker cancel +
  refresh-on-resume; nav-bar restore + paused-service banner.
  Verification build: 2,682,104 bytes, md5
  59390b74a2bec6f58a49f0cc81bcec0f, vc12/3.0, havenos cert match.
- **Phase B — Haven Glass + accent system.** `ui/glass/` entry point
  (GlassTokens/Components/Motion/AccentTokens/HavenIcons/WallpaperAccent),
  Glass1/2/3 materials reusing existing blur-behind, HavenIcon enum,
  emoji scan 0 hits, all glass components, motion precedence, full accent
  system (Wallpaper suggestion-only / System / Curated 12 / Custom +
  strength + migration + Changes names). Purple Default card fixed
  properly (HavenTheme container slots were unset). Verification build:
  2,714,872 bytes, md5 d17d18d696fc6acf83778cf21d5147eb, vc12/3.0.
- **Phase C — Haven Glass on every non-home surface.** Settings (Glass2
  panel, 260dp rail, token-miniature hero, API status lines, HavenHelp),
  drawer (Glass2+scrim, IME insets, HavenSegmented, letter bubble, match
  highlighting, SAD empty state), Today cards (one anatomy, footers on
  success only, Skeleton, ≤4+Show more scores), menus/dialogs/snackbars
  (HavenDialog/Menu/Snackbar everywhere; danger tint only for permanent
  actions), toolbar/context bar (Glass3 pill, grouped, 2dp ring kept),
  overview/picker/folders/indicator, Calm (editor restyled, accent only
  for schedule state), island (Glass3 dividers, behavior unchanged).
  Real bug found+fixed: quick-customize Widgets branch was a lambda
  literal that did nothing — tapping Widgets never opened the sheet.
  Verification build: 2,731,256 bytes, md5
  5c47c1987785b4ed6cf1ec6eec05676f, vc12/3.0.
- **Phase D — small home updates.** Only source change: `ui/WidgetReal.kt`
  +11/−8 — home search widget is one Glass1 card (inner pill deleted,
  ⌕ glyph replaced with the Search vector; tap target widened to the
  full tile, behavior-compatible). Dock tiles (A7) and A4/A5/A6 re-verified
  by code review, not rebuilt. Verification build: 2,731,256 bytes, md5
  15460f553aadfa10c1ef522cabbea26f, vc12/3.0.
- **Phase E — pixel mascot rebuild.** Milo+Mila original 24×24 pixel
  sprites, 41 shared frames each (PNG sheets + atlas + PIL generator +
  preview at 4× on gray+dark); pure MascotEngine (12-state machine,
  ledge graph with clearance rule, BFS routing, weighted planner);
  Find flow tracked end-to-end over the existing search adapter;
  hub with animated preview + Preview button, tips Off/Rare/Normal,
  24h quiet-hours pickers, Reset mascot; help map 13→18 entries.
  Old vector kept behind `USE_PIXEL_MASCOT`. No new permission, no
  dependency, no Room migration. Verification build: 2,809,473 bytes,
  md5 eebdc1cf4fb21ad96432049fb4613b1d, vc12/3.0.
- **Phase F — on-device assist foundation.** AssistRouter seam (rules,
  synonyms, ranking, no model; LocalModelBrain behind flag delegating
  to ScriptedBrain); compound command grammar in drawer + panel search
  (all-or-nothing, one row per compound, whitelist router, preview +
  Changes + Undo); AI Privacy Ledger in the Privacy hub (5 features,
  counts+times only, per-feature switches, Clear learned data);
  model-pack scaffold only (URL + checksum EMPTY placeholders, no
  runtime wired). No new permission, no dependency, no Room migration.
  Verification build: 2,825,857 bytes, md5
  f4a56ac74624e44d58f1453bfb3bc0ba, vc12/3.0.
- **Phase G — verification, docs, report, version stamp (this phase).**
  G1 checks run independently (see below, including the debug assemble,
  lint, and unit-test attempts, each with its exact result). G2 docs:
  manual source updated to 3.1 (§1–§10, flows 13–16, A2/A3/CardStatus,
  pixel mascot + ledge behavior + Find, island restyle, honest §10);
  COVERAGE.md Phase G section; UNVERIFIED.md final consolidated state
  (Phase A section added — it was missing — plus Phase G section);
  DEVICE_CHECKLIST.md consolidated 3.1 FINAL top section (G-1…G-26).
  PDF NOT rebuilt (pipeline not reproducible — see Deviations).
  Final version stamp: 3.1.0/vc13 (see Final build below).

## Tests added (total across phases — all written, NONE run)

- Coordination fix (separate line, stated separately): 7 new files,
  462 lines.
- 3.1 Phase A: 9 new test files + 1 modified test file (line count
  not stated in PHASE-A.md — not invented here).
- 3.1 Phase B: 4 files, 332 lines.
- 3.1 Phase C: 2 files, 152 lines.
- 3.1 Phase D: 0 (no pure logic added).
- 3.1 Phase E: 2 files, 390 lines.
- 3.1 Phase F: 3 files, 296 lines.
- 3.1 Phase G: 0 (docs/verification only).
- 3.1 total: 20 new test files + 1 modified test file; 1,170 lines
  counted in the B/C/E/F reports.
- **Phase G finding (new, worse than "not run"):** the test tree does
  not compile. `:app:testDebugUnitTest` fails at
  `:app:compileDebugUnitTestKotlin` with 305 errors across 10 files
  (see Checks). Zero tests ran in Phase G, and no test can run until
  the tree compiles. The sandbox test-executor breakage reported by
  earlier phases is real but secondary: compilation fails first.

## Checks run + results (every command + outcome)

All commands run in Phase G on the Phase F tree (before the version
stamp), via the offline GradleMain recipe in `~/AGENTS.md` (never the
wrapper script). No Gradle invocations ran concurrently.

- **Release verification build:** `~/workspace/havenos/build.sh` →
  `GRADLE_EXIT=0`, `BUILD SUCCESSFUL in 35s` (incremental: 48
  actionable tasks, 1 executed, 47 up-to-date). APK
  `app/build/outputs/apk/release/app-release.apk`, 2,825,857 bytes,
  md5 f4a56ac74624e44d58f1453bfb3bc0ba — identical to the Phase F
  verification APK, as expected (no source changes since Phase F).
  aapt: package `app.havenos.launcher`, versionCode 12, versionName
  3.0, minSdk 26, targetSdk 36. apksigner (full path
  `~/android-sdk/build-tools/35.0.0/apksigner`, `JAVA_HOME=/home/hatch/jdk17`):
  Signer #1 CN=HavenOS, SHA-256
  53bff2a09aedb516a8ee6193234a7aaf5d04ce901fb8931b799191b9845354a8
  (keystore match), exit 0. Note: bare `apksigner` is NOT on PATH
  and produces no output; the full-path run is the valid evidence.
- **Debug assemble (feasibility check, NOT a deliverable):** same
  recipe with `:app:assembleDebug --offline --no-daemon` →
  `GRADLE_EXIT=0`, `BUILD SUCCESSFUL in 6m 50s` (36 actionable
  tasks, 32 executed, 4 up-to-date). Debug APK
  `app/build/outputs/apk/debug/app-debug.apk`, 16,205,713 bytes,
  md5 c4976baa8d5ea2596d355acae2ccae38; aapt: package
  `app.havenos.launcher`, versionCode 12, versionName 3.0, minSdk 26.
  Debug builds are unsigned release-candidates in no sense; this
  only proves a debug assemble is feasible in this environment.
- **Lint:** `:app:lintRelease --offline --no-daemon` → `GRADLE_EXIT=1`,
  `BUILD FAILED in 5m 5s` (25 actionable tasks, 7 executed, 18
  up-to-date). Lint itself ran fine offline; it fails the build
  because errors exist. Result: **16 errors, 89 warnings, 35 hints.**
  The 16 errors: 5× MissingPermission (Wallpaper.kt:83
  getWallpaperFile; WidgetReal2.kt:267–268 getLastKnownLocation),
  7× NonObservableLocale (DeviceDataHub.kt:424, Drawer.kt:1391,
  TodayRail.kt:104+246, WidgetReal2.kt:557, Widgets.kt:319+762),
  1× SuspiciousModifierThen (HomeA11y.kt:105), 1×
  UnusedBoxWithConstraintsScope (MascotLayer.kt:1120, Phase E file),
  2× UnusedContentLambdaTargetStateParameter (SettingsPanel.kt:
  597+613). ALL pre-existing relative to Phase G (Phase G changed
  no app source); none fixed here — lint cleanup is a follow-up,
  not a Phase G regression fix. Full report:
  `app/build/reports/lint-results-release.txt`.
- **Unit tests:** `:app:testDebugUnitTest --offline --no-daemon` →
  `GRADLE_EXIT=1`, `BUILD FAILED in 1m 12s` (22 actionable tasks,
  5 executed, 17 up-to-date). Failed at
  `:app:compileDebugUnitTestKotlin` with **305 compile errors**;
  zero tests ran. Per-file: MascotEngineTest.kt 126,
  AssistRouterTest.kt 53, MascotSpritesTest.kt 45, ModelPackTest.kt
  28, AssistLedgerTest.kt 28, SettingsShellTest.kt 10 (the
  pre-existing gridShrinkMessage/gridShrinkReport errors),
  LayoutStateTest.kt 6, EditModeTest.kt 5, HexLiteralTest.kt 2,
  EmojiScanTest.kt 2. Root causes, verified by reading the files:
  (a) ~280 errors: Phase E/F test files import `kotlin.test.*`,
  but the project declares only JUnit4 (`testImplementation(libs.junit)`);
  kotlin-test is not a declared test dependency, so `Test`,
  `assertEquals`, `assertTrue`, `assertFalse`, `assertNull`,
  `assertNotNull` are all unresolved. (b) Stale tests vs the
  coord-fix: LayoutStateTest expects the old UndoFrame shape,
  EditModeTest references deleted `EditUndoStack`/`EDIT_UNDO_MAX`,
  SettingsShellTest references never-landed gridShrink helpers.
  (c) 4 type errors in the Phase B scan tests. Fix direction (not
  done here): switch the E/F tests to `org.junit.Test` +
  `org.junit.Assert.*` (no new dependency) and update the stale
  tests — see "Next most useful improvement". The sandbox
  executor breakage (M7: TestResultProcessor NPE) was NOT reached
  in this attempt; it remains a second, separate blocker behind
  compilation.
- **Instrumentation tests:** NOT APPLICABLE — no `androidTest`
  directories exist under `app/src/`.
- **Screenshot tests:** NOT APPLICABLE — no `*screenshot*` /
  `*Screenshot*` files exist under `app/src/`.
- **Security grep** over Phase G changed files: clean. Hits are
  benign prose only ("design tokens", "no secrets in them",
  "never a token"). No credentials, keystore paths, or tokens.
- **Diff review** vs the pre-G snapshot (excluding `.gradle`,
  `build/`, `*.apk`): exactly two in-tree files differ —
  `docs/DEVICE_CHECKLIST.md` and `docs/UNVERIFIED.md`. (The manual
  source lives outside the havenos tree; it has its own backup,
  `.src/index.html.pre-3.1`.) `app/build.gradle.kts` and
  `AndroidManifest.xml` were byte-identical to the snapshot until
  the final version stamp below.
- **Manual source validation:** after editing, the HTML parses with
  no errors (html.parser), tags balanced (`<li>` 103/103,
  `<figure>` 7/7, `<b>` 88/88 — one unclosed `<b>` introduced by
  the Grid-shrink edit was caught by the parse and fixed with a
  one-line correction), all new 3.1 text present, all superseded
  3.0 text absent (spot-checked: flows, help map, restore
  thumbnails, crypto states, island poll, dock item, hero card).
  Figure 7 embeds `docs/mascot/preview.png` as a base64 data URI.
  Size: 954,430 → 1,089,956 bytes.
- **Per-phase verification builds** (A–F) are carried from their
  phase reports, each parent-verified at the time (sizes/md5s in
  "What changed per phase" above). Phase G re-verified the Phase F
  tree independently (release build above) rather than trusting
  the Phase F report.

## Dependencies added

NONE across all of 3.1. `app/build.gradle.kts` in Phase G is
byte-identical to the pre-Phase-A snapshot (verified by diff).
No new permission either: `AndroidManifest.xml` byte-identical to
the pre-G snapshot (and byte-identical through Phases E and F per
their reports).

## Files touched (summary counts per phase)

- Phase A: 27 modified source files + 9 new test files + 1 modified
  test file (diff vs pre-A snapshot).
- Phase B: 7 new glass files + ~19 modified files.
- Phase C: 29 main files, +2,536/−2,118.
- Phase D: 1 file, +11/−8.
- Phase E: MascotLayer +845/−182, MascotEngine NEW 477, MascotSprites
  NEW 222, MascotBrain +68/−15, MascotFind +46/−3, MascotHub +168/−64,
  LookHub +7/−5, HomeDockHub +7/−0, SettingsPanel +11/−0, Home +40/−6,
  Drawer +24/−8, WidgetReal2 +20/−0, TodayApiCards +24/−8, Settings
  +2/−2, 2 new test files, 2 PNG sheets, atlas, generator + preview,
  3 doc files.
- Phase F: 4 new files (702 lines) + 9 modified files + 3 test files
  (296 lines).
- Phase G: docs + manual source only — `docs/DEVICE_CHECKLIST.md`
  (consolidated 3.1 FINAL top section, G-1…G-26),
  `docs/UNVERIFIED.md` (Phase A section + Phase G section),
  `docs/COVERAGE.md` (Phase G section), this report,
  manual source `~/workspace/your_files/havenos-how-it-works/.src/index.html`
  (+135,526 chars, Figure 7 embedded, backup `.pre-3.1`). No app
  source, no test source, no build-config change except the final
  version stamp below.

## Assumptions

- No device or emulator exists here; every tablet-checklist item is
  NOT RUN, never claimed passed.
- Offline build (Gradle `--offline`); Google Maven reachable only for
  already-cached artifacts — no new dependency could be fetched,
  which is one reason the no-new-dependency rule held.
- Test executor broken (pre-existing) — now known to be secondary
  to test-compile failure (see Checks).
- PDF pipeline not reproducible from `.src/` (no render/validate
  scripts saved there; no Chromium/Chrome binary found) — so the
  manual SOURCE is updated to 3.1 and the PDF rebuild is a
  follow-up. Until it is rebuilt, the delivered PDF
  (`HavenOS-How-It-Works.pdf`, 997,377 bytes) remains the 3.0
  version and does not match the updated 3.1 source.
- Mascot art quality is judged only by a human from preview.png;
  Phase E's agent explicitly could not judge it, and neither can
  Phase G.
- The 3.1 prompt names the final line 3.1.0/vc13; the release
  question (ship 3.1.0/vc13 vs anything else) stays the parent's
  decision (see Open decisions).

## Deviations from the prompt (complete list — carried from A–F + new in G)

Coord-fix (6): density-level routing BLOCKED (no DensityChange in
the doc's mutation list; Hub Follow-density IS routed; slider density
converges via the host safety net); host drag/drop/dock/remove/hide
writes NOT routed through commit (install/Sync + grid/dock/Today
only); host renders from Room, not from profiles (commit writes
profiles; render-from-profiles is follow-up); SessionDiff
settings-restores and DeviceDataHub's snapshot-apply path left direct
(DeviceDataHub path does not clear undo stacks); grid-commit
effect/commit race can add one extra undo entry; Room + DataStore
cannot share one transaction (commit's Room writes are atomic,
settings writes separate).

Phase A (7): A9 stacked toolbar+panel premise does not occur in
shipped code (opening Settings exits edit mode); A9 density N=1 per
tap (grids are per-orientation); A9 undo labels session-only (grid
actions do survive restart); A4 layout test replaced by a Phase G
tablet check (JVM tests cannot assert Compose clipping; the pure
parallax math is tested); A12 macrobenchmark not applicable (project
does not use it); Flow 15 feed not rewired to the pure picker (ticker
owns window expiry); A13 uninstall nav-bar loss unfixable by code.

Phase B: icons are original outline vectors, NOT Material Symbols
(extended set is not a dependency, not in the Gradle cache, build is
offline); Glass3-light 85% inferred; two-blur-layer rule documented
not enforced; eyedropper samples WallpaperColors only (no bitmap
sampling exists); wallpaper seed refreshes on source/settings change,
system-wallpaper change picks up on next launch; out-of-range
geometric glyphs left for Phase C; mascot emotes slightly lossy
("✓"→"!"/"!!"); glance weather line glyph dropped (label+temp
remain); DEFAULT is legacy-only display; accent storage is DataStore
keys, not Room; HavenSegmented labels maxLines=1 (documented
exception).

Phase C: scrim added over Glass2 tint on panel/overview sheets (bare
tint illegible over live home); Today frames, FolderSheet, toolbar,
context bar, island keep GlassSurface (tint-only glass would lose
blur); HavenMenu container stays palette.surface (Phase B design);
Quick tune Mode gained a System option (agent judgment — OPEN
DECISION, parent may overrule); C5 group boundaries are the Phase C
agent's reading (prompt names groups, does not enumerate them);
Today header icon/overflow slots unused (no card has them — no fake
slots); DatePickerDialog stays a platform picker; ShellSwatchRow /
color-picker deletion still deferred.

Phase D (3): GlassCard deliberately NOT used on home (tint-only
glass loses blur; GlassSurface is the home Glass1 realization);
tap target widened to the full tile (behavior-compatible); no new
test file (no pure logic to unit-test).

Phase E deviations: apex frame 11 doubles as the Carried dangling
frame; linear tweens + jump frames (no ballistic curve); timer delays
at atlas fps (no withFrameNanos); timers restart on resume; side
docks get an obstacle but no top perch (y=0 clips); rail coverage is
the rail-top segment only; Normal tips = Rare + help-tip fallback
only; typed-question help keeps user-chosen Open (pointing exception
is Find-only); timeout covers waiting states only; close-bubble
during pointing also clears pointing (else the layer would strand);
gesture-block indentation uneven (cosmetic, compiles clean).
Phase E BLOCKED (never silent): no pixel-stepped bubble border; no
per-card rail ledges; no side-dock perch; dock-app Find = bubble +
Launch only (dock rects not plumbed); guide mid-trip resolves within
one step.

Phase F deviations: "silent" unmapped (no such setting exists —
falls back to search); panel-search command rows do NOT navigate to
the changed control (Changes footer stays the confirmation surface);
mixed app + setting compounds land on the LAST setting action's hub
(every action still executes exactly once); ledger switches NOT
session-revertible (same scope as mascot toggles); ledger history
NOT backed up (device-local ephemeral). Phase F BLOCKED: SearchCard
command rows (drawer + panel search cover §F2; wiring SearchCard is
a small follow-up); model-pack download/checksum/Wi-Fi/space/RAM/
cancel/delete paths scaffold-only and unprovable until a real model
URL + checksum is configured (placeholders empty by design); no
runtime picked (Phase 3 conclusion stands); F6 NOT built (Scenes,
Style DNA, Recall, notification digest, cloud AI, mascot voice, more
characters, online themes store).

Phase G (new): (1) PDF NOT rebuilt — the manual SOURCE is updated to
3.1, but the delivered PDF file remains the 3.0 version until the
render pipeline is reproduced (no scripts, no Chromium in this
environment). This is a deviation from G2's "update the manual" only
in the sense that the PDF artifact lags the source; stated here, not
hidden. (2) Lint errors (16) NOT fixed — all pre-existing, none
introduced by Phase G; fixing them is out of Phase G scope (fix only
Phase G regressions; Phase G introduced none). (3) Test-compile
errors (305) NOT fixed — same reason; the repair is the recommended
next improvement, not a Phase G fix. (4) The debug assemble produced
a debug APK (16,205,713 bytes) — a feasibility check only, never a
deliverable; the only release-candidate artifact is the final build
below.

## Unverified items (complete list)

- Every device check, all NOT RUN: COORD-1…15, B-1…B-15, C-1…C-14,
  D-1…D-5, E-1…E-15, F-1…F-13, G-1…G-26 (new consolidated section),
  V2-API-1…9, V3-MASCOT-1…8, V3-ISLAND-1…9, plus all older M-series
  checks never confirmed on device.
- All written tests: 0 ran (test-compile failure, 305 errors);
  executor breakage is a second blocker behind compilation.
- Lint findings: 16 errors, 89 warnings, 35 hints — unaddressed.
- WallpaperColors tablet hypothesis (Phase B); two-blur-layer
  behavior (documented, not enforced); model-pack states
  READY/FAILED/DOWNLOADING unreachable (placeholders empty);
  mascot art quality (human review from preview.png only).
- PDF rebuild (manual source is 3.1; delivered PDF is still 3.0).
- Visual/gesture behavior of every 3.1 phase (no device/emulator).

## Known gaps

The manual §10 list as updated in Phase G: folder preview modes;
themed monochrome/tinted icons, split-view/app pairs, media card,
widget-stack sync across pages, shared layout between devices; full
guided first-run flow (Replay intro/tutorial are placeholders);
Settings-value undo through the layout undo stack outside edit mode
(panel Before/Changes path covers it; grid overrides in edit mode DO
join the shared timeline); dock drag-reorder and folders in the dock
(edit-mode drag-out-to-home ships; menu actions ship in normal mode);
system-wallpaper label legibility fallback; widget frame radius row
preview-only; stack member configs don't persist; battery
time-remaining omitted; F6 assist items; SearchCard command rows;
pixel-stepped bubble border; per-card Today-rail ledges; side-dock
perch; rings for dock-app Find targets; local-model runtime; "silent"
command; density-level routing through commit; host drag/drop/dock/
remove/hide routing through commit; render-from-profiles; grid-commit
effect/commit race; Room + DataStore in one transaction; SessionDiff
settings-restores and DeviceDataHub snapshot-apply through commit;
online themes store, lockscreen replacement, cloud AI for the mascot,
mascot voice, mascot roster beyond Milo and Mila (considered and
dropped, or parked).

## Next most useful improvement

**Test-tree repair pass.** Switch the Phase E/F test files from
`kotlin.test.*` to the project's JUnit4 (`org.junit.Test` +
`org.junit.Assert.*`) — a mechanical import change, no new
dependency — update the stale LayoutState/EditMode/SettingsShell
tests to the coord-fix APIs, and fix the 2 Phase B scan-test type
errors. Reason: it unlocks every test written across the coord-fix
and 3.1 (27 files), which today cannot even compile; without it,
verification stays at build-only forever, and every future phase
adds more unrun tests. Runners-up, in order: SearchCard command rows
(small follow-up, completes §F2); render-from-profiles (closes the
coord-fix's biggest open item); host drag/drop routing through
commit; density-level routing.

## Open decisions for the parent (explicit)

1. **Quick tune Mode System option** — Phase C agent judgment
   (HavenSegmented always shows a selection; the old no-selection
   state would have lied). Overrule or keep?
2. **C5 toolbar group boundaries** — the Phase C agent's reading;
   the prompt names groups but does not enumerate them.
3. **SearchCard command rows follow-up** — Phase F BLOCKED, small
   (thread the handler through SearchCard's Home call site).
4. **Phase E BLOCKED items** — pixel-stepped bubble border,
   per-card rail ledges, side-dock perch, dock-app Find = bubble +
   Launch only, guide mid-trip resolves within one step.
5. **Release question** — ship the 3.1.0/vc13 final build below, or
   something else? Including the fate of the coord-fix vc12 test
   build, whose download link expires 2026-11-01 10:09 IST (fresh
   link mintable on his word without a rebuild).

## Final version-stamped build

Version stamp (the prompt's final step, done LAST, after G1–G4):
`app/build.gradle.kts` `defaultConfig` — `versionCode = 12` →
`versionCode = 13`, `versionName = "3.0"` → `versionName = "3.1.0"`
(two lines; no signing, dependency, or manifest change).

Final verification build: `~/workspace/havenos/build.sh` →
`GRADLE_EXIT=0`, `BUILD SUCCESSFUL in 8m 37s` (48 actionable tasks,
14 executed, 34 up-to-date).

Final APK: `~/workspace/havenos/app/build/outputs/apk/release/app-release.apk`,
**2,825,857 bytes, md5 cc45b27dcf53ca487af36fe68a9cc4b2**.
Same byte size as the Phase F verification APK by coincidence; the
md5 differs (the version stamp changes the manifest) and the
timestamp is fresh (2026-10-30 17:02 UTC) — this is a new build,
not a copy of the Phase F file.

aapt: package `app.havenos.launcher`, versionCode **13**, versionName
**3.1.0**, minSdk 26, targetSdk 36 — all required fields match.
apksigner: Signer #1 certificate DN: CN=HavenOS, OU=Dev, O=HavenOS,
C=IN; SHA-256 digest
53bff2a09aedb516a8ee6193234a7aaf5d04ce901fb8931b799191b9845354a8
(keystore match).

This APK is NOT delivered anywhere by Phase G and NOT published.
Delivery and the release decision belong to the parent. The
coord-fix vc12 test build link (expires 2026-11-01 10:09 IST) is
unaffected by this build.
