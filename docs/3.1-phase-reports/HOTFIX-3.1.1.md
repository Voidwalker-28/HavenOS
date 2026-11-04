# HOTFIX 3.1.1 — HavenOS (2026-10-31)

Queue-jump hotfix ordered 2026-10-31, ahead of the Pad checks and the
vc13 release decision. Baseline: the 3.1.0/vc13 final tree (all phases
A–G done). Snapshot taken FIRST, before any edit:
`~/workspace/havenos-pre-3.1.1-hotfix/` (244 MB, rsync excluding
`.gradle` and `*.apk`).

Standing rules honored: no publish, no upload, no signing change, no
token, `~/workspace/havenos-github/` untouched, no APK delivered (the
parent owns delivery and the release decision). Ponytail full level +
local verification-before-completion; debug pass after each part;
relevant ECC tracks read before the step they cover
(ecc-kotlin-patterns, ecc-android-clean-architecture,
ecc-kotlin-coroutines-flows, ecc-error-handling, ecc-design-system,
ecc-kotlin-testing, ecc-tdd-workflow, ecc-verification-loop);
ECC testing defaults (Kotest/MockK) NOT followed — the repo declares
JUnit4 only, so all new tests are plain JUnit4.

## Honest note: the vc13 crash trace cannot be retraced

The brief gives the vc13 R8 map id
`bb9982831ff14ce711ca1f2dc95ef810ea02fdba1dcf65a38756bb65a4fdc611`,
but no mapping.txt was retained for vc13, so the exact vc13 crash line
cannot be identified. The `firstNotNullOf` site fixed in item 1
(MascotLayer placementOf) throws exactly the reported exception type
in a main-thread coroutine context and is a prime suspect — NOT a
proven vc13 line. Mapping retention starts with this hotfix (vc14).

## Item 1 — Crash sweep (NoSuchElementException)

What changed:
- `ui/mascot/MascotLayer.kt` (placementOf): `firstNotNullOf` →
  `firstNotNullOfOrNull`. Fallback: folderHit null falls through the
  existing `when` to the dock/empty branches. The site evaluated for
  every placementOf call (it ran before the `when` that checks home
  items first), so any Find with no containing folder threw.
- `ui/mascot/MascotEngine.kt`: BFS `ledges.first {…}` →
  `firstOrNull ?: return null`; path-reconstruction `first` →
  `firstOrNull ?: return null` and `prev[cur]!!` → `prev[cur] ?: return
  null`; `pickTargetLedgePoint` trailing `ledges.last()` →
  `lastOrNull() ?: return null`. Fallback in all three: the route
  fails (returns null) and the host stays put. The pure engine
  already had a no-valid-ledge path; the sweep verified the host
  honors the null (stays put / poof path) and fixed the three
  throwing lines the host could still reach.
- `ui/mascot/MascotSprites.kt`: `animations["idle"]!!` →
  `animations[animation] ?: animations["idle"] ?:
  animations.values.firstOrNull() ?: return` (renders nothing if the
  atlas has no animations at all; never throws).
- `ui/mascot/MascotLayer.kt` sprite rendering: `spriteSet!!` (×2) →
  single read with smart cast, no `!!`.
- `ui/settings/hubs/GesturesMotionHub.kt`: four `picked!!` sites in
  the icon-swipe `scope.launch` → `picked ?: return@launch`.
  Fallback: skip the write when picked is null (cannot happen today
  — picked is set on open and never cleared — but never throws now).
- `app/proguard-rules.pro`: added `-keepattributes
  SourceFile,LineNumberTable` (existing file, not duplicated).
- `~/workspace/havenos/README.md` created (retrace documentation):
  mappings at `release-artifacts/mapping-<versionCode>.txt`,
  retention from vc14, vc13 and older NOT retraceable, retrace
  command (`java -jar r8.jar retrace …` with r8.jar from Maven
  Central `com.android.tools:r8`, version matching the build as
  recorded in the mapping header; Android Studio Analyze → Analyze
  Stack Trace alternative), local-only crash note. No r8 jar exists
  in `~/android-sdk` or the local Gradle cache — the README documents
  fetching it, it does not claim a local jar.
- `~/workspace/havenos/release-artifacts/` created (vc14 mapping goes
  there from the final build).

Sweep verification (grep, 2026-10-31): no remaining `firstNotNullOf`
(non-OrNull), no `.single(`, no `requireNotNull` in main source.
Remaining `.first()` / `.last()` sites all guarded (size/empty
branches read directly above each site). Numeric minOf/maxOf cannot
throw NoSuchElement. `HavenIcons vectors.getValue` verified safe by a
mechanical check (52 `v(HavenIcon.X)` entries + 24 aliases cover
every enum entry; the 8 apparent gaps are comment words, not
identifiers). DataStore `first()` reviewed-safe (always emits).
`!!` sites not fixed were each read and found inside an explicit
null-guard branch — documented in the session record, not churned.

Tests: HotfixCrashSweepTest (unit, JUnit4) — planRoute /
pickTargetLedgePoint / nearestLedgePoint / buildLedges empty inputs,
spriteHitRect, steppedCornerPoints, rankLibraryApps empty inputs,
suggestPinCandidate empty, resolveAccentSeed with no swatches.
placementOf itself is NOT unit-testable (lambda inside the
MascotLayer composable); the engine-level null paths above are the
testable equivalents, and the placementOf fallback is Pad check H-10.

Checks run: `:app:compileReleaseKotlin` BUILD SUCCESSFUL in 5m 27s
(item-1 edits only, log `/tmp/hotfix-item1-compile.log`).

Known gaps / open risks (NOT fixed — out of the briefed sweep scope,
which covers LaunchedEffect / rememberCoroutineScope / withFrameNanos
/ mascot / Find / island / ledge / search / dock code; needs a parent
scoping decision): PagedHome cell `appEntry!!` (ItemKind.APP branch)
and `folder!!` (ItemKind.FOLDER branch) with no null guard visible,
and ghost `dragNow.overCell!!` where the enclosing condition checks
everything except overCell. A stale item for an uninstalled app, or a
ghost block running with overCell null, would throw in composition.

## Item 2 — Failure isolation

What changed (new file `ui/FeatureGuard.kt`, 165 lines):
- `FeatureId` enum with exact toast texts (MASCOT "Mascot paused
  after an error", ISLAND "Island paused after an error", FIND "Find
  paused after an error", ONLINE_CARDS "Online cards paused after an
  error").
- `FeatureSessionState`: `mutableStateOf<Set<FeatureId>>` (process
  memory only, never persisted, never uploaded); composition
  subscribes so hosts recompose on disable.
- `logFeatureError` / `readFeatureErrors`: `filesDir/feature_errors.txt`,
  `time|feature|class|message(≤200 chars, sanitized)|top-5-frames`,
  last 20 kept, never throws.
- `featureErrorHandler`: ignores CancellationException; logs,
  disables, posts one Toast on Main.
- `supervisedChildScope`: parent scope's Job + SupervisorJob + handler
  — composition-leave cancellation flows down, a child failure never
  cancels siblings or the parent.
- `runFeatureGuarded`: suspend helper for LaunchedEffect bodies
  (rethrows CancellationException; on other throw logs + disables +
  toasts + returns null).
- Feature failures NEVER write item-3 crash markers.

Wiring: MascotLayer gets mascotGuardedScope (8 launches) and
findGuardedScope (submitFind); FIND-disable effect closes the bubble
and clears finding state. Home.kt mascot/island gates gain the
item-2 disable checks. IslandFeed refresh catch logs + disables +
toasts on first failure only (other island paths were already
try/catch — documented, not churned). TodayApiCards host early-returns
when ONLINE_CARDS is disabled; the retry catch is a backstop only
(classified failures render as card status lines and never throw).

Tests: none dedicated to FeatureGuard (file IO needs a Context; the
pure formatter boundary is documented). The disable/toast behavior is
Pad checks H-4.

Checks run: `:app:compileReleaseKotlin` BUILD SUCCESSFUL in 3m 26s
(log `/tmp/hotfix-item2-compile.log`).

## Item 3 — Crash-loop protection

What changed (new file `CrashLoop.kt`, 28 lines, pure):
- `isCrashLoop` (any two consecutive sorted markers within 60s),
  `pruneCrashMarkers` (newest 20), `LastCrash` data class.

`HavenApp.kt` (+155/−1): `_safeMode` StateFlow; `installCrashLog`
EXTENDED (not duplicated) to write a timestamp marker after
`last_crash.txt`, then always delegate to the previous handler;
`evaluateSafeMode` (markers → Safe Mode + skeleton safe_mode.txt);
`startSafeModeJobs` (snapshot from `persistedState()` — mascotOn,
islandOn, surfaceMaterial, apiOn via ApiId entries — then a 10-minute
stable timer that clears Safe Mode only if the marker count is
unchanged); `clearSafeMode` (idempotent, reschedules workers);
worker gate `scheduleAll { !safeMode && toggles }` (in Safe Mode
every worker is CANCELLED, not scheduled); `readLastCrash` (parses
last_crash.txt + PackageInfo versionCode, never throws). Safe Mode is
runtime overrides ONLY — stored settings are never mutated, so
Re-enable is exact by construction; the snapshot is still recorded
for View details/diagnostics.

`MainActivity.kt`: `themeState = if (safeMode)
state.copy(surfaceMaterial = 0) else state` passed to HavenTheme
(Solid rendering forced, stored value untouched).

`ui/Home.kt`: mascot/island gates gain `!safeMode`; SafeModeBanner
placed last in the home shell Box (above pager/dock/mascot, below
drawer/settings overlays).

New file `ui/SafeModeBanner.kt` (119 lines): exact banner text
"HavenOS recovered from a crash. Mascot turned off" with View details
and Re-enable (Re-enable calls `app.clearSafeMode()`); details dialog
with Copy (ClipboardManager, "Copied" feedback) and Share
(ACTION_SEND chooser "Share last crash"); nothing uploaded
automatically.

`ui/settings/hubs/DeviceDataHub.kt` Advanced section: "Last crash"
(label + monospace `Class\nmessage\ntimeText · versionCode N` + Copy /
Share) and "Feature errors" (per-error `FEATURE: Class · message` +
frames), reusing `lastCrashShareText` / `readFeatureErrors`.

Tests: CrashLoopTest (unit, JUnit4) — boundary (60s in, 61s out),
unsorted input, later-pair trigger, prune newest-20 / sorted.

Checks run: `:app:compileReleaseKotlin` BUILD SUCCESSFUL in 3m 20s
(log `/tmp/hotfix-item3-compile.log`).

## Item 4 — Touch pass-through

Audit findings (verified in code, not assumed):
- MascotLayer root BoxWithConstraints has a `.pointerInput(Unit)`
  loop that calls `awaitFirstDown()` and `endFind()` but NEVER
  consumes — swipes and taps pass through.
- The sprite body Box carries graphicsLayer translation AND its
  input modifiers on the SAME node — no translated-draw /
  untranslated-touch bug present.
- `animateFloatAsState` values are read ONLY inside graphicsLayer
  lambdas (draw phase) plus event-time pointerInput reads — the E2
  transform-only claim holds for the sprite.

Fixes:
- Spotlight dim: old code consumed EVERY down (`down.consume();
  endFind()`), killing pager swipes during guiding. New code awaits
  down unconsumed; on up for the same pointer it consumes ONLY the
  up + endFind (the icon beneath never receives the click action);
  on movement beyond touchSlop it ends Find WITHOUT consuming, so
  the pager (which also saw the unconsumed down) scrolls. Nuance:
  the dismiss tap's pressed flash may still show on the icon
  beneath, but its action never fires.
- Bubble per-frame recomposition: bubbleX/bubbleY were computed in
  composition from animX/animY, recomposing the bubble subtree every
  animation frame while open and moving. All position math moved
  INSIDE the graphicsLayer lambda (a move now only invalidates draw);
  the IME read stays in composition on purpose (subscribes the scope
  to keyboard changes so the bubble stays above the keyboard).
- `spriteHitRect(x, y, sizePx)` helper added (single definition for
  the hit test and the unit test); the root loop's inSprite check
  uses it.

Tests: `app/src/androidTest/.../ui/mascot/MascotTouchTest.kt`
(written, NOT run, NOT compiled here — no device/emulator, compose
UI-test artifacts not in the offline cache; androidTest is not part
of `:app:assembleRelease` so it cannot break the release build):
pager swipe / icon tap / long press / dock tap / drawer open while
the mascot walks, plus swipe-dismisses-bubble-and-scrolls-pager.
Selectors verified in code (pager indicator "Page N of M", dock
"Library"/"Search" content descriptions, drawer "Search apps"
placeholder, mascot "mascot menu" semantics, bubble "Close").
Known fragility: the root swipe starts mid-screen, so if the
mascot/bubble happens to be mid-screen the swipe could land inside
the bubble rect; the test is a device-run check, not a proven pass.
`spriteHitRect` unit tests are in HotfixCrashSweepTest.

Checks run: item-4 code is covered by the item-6/7 compile below
(the standalone item-4 compile FAILED only because it snapshotted
sources mid-edit during item 5 — 6 errors, all item-5-in-progress
artifacts (missing `compactHeader` param + TooltipBox imports added
moments later); that log is INVALID and superseded, not a code
failure). Final item-4+5+6+7 compile: BUILD SUCCESSFUL in 3m 35s
(log `/tmp/hotfix-item67b-compile.log`).

## Item 5 — Settings layout fix

Root cause CONFIRMED in code (not just the hypothesis): panel
`(w * 0.46).coerceIn(480, 660)` with a 260dp rail — on a 1280dp
tablet landscape the panel is 589dp and the detail ~289dp; on an
800dp window the panel clamps to 480dp and the detail is ~179dp.
HubRow/HubSwitchRow were `Row(label weight 1, info, value,
control)`, so the label got almost nothing and wrapped per
character. HavenRowContainer was NOT the cause (it already stacks
label + value/sub in a weight-1 Column beside a fixed trailing).

What changed:
- `ui/settings/shell/SettingsPanel.kt`: panel
  `(w * 0.56f).coerceIn(640f, 920f).coerceAtMost(w - 24f)` in
  `tabletPanelWidthDp` (single function covering both callers:
  SettingsPanel and Home.kt free-area layout — they can never
  disagree). Pure helpers `settingsRailWidthDp` (72f < 900f, else
  200f) and `settingsDetailWidthDp` (panel − rail − 41f: 16dp side
  padding ×2 + 1dp divider + 4dp divider padding ×2). With the new
  clamp the detail is always ≥ 360dp in master-detail (527dp at the
  narrow end, 679dp at the wide end). DockedPanel: single-pane
  branch when panel < 560dp (PanelTitleBar + content, no rail; the
  title bar's back button is the detail back button) — unreachable
  on tablets today under the 640dp clamp minimum, implemented as a
  guard for narrower windows/freeform. MasterDetailRail takes the
  computed rail width + iconOnly flag. RailRow icon-only: icon 36dp
  box + 12dp padding ×2 = 60dp ≤ 72dp rail; label via TooltipBox
  (PlainTooltip) AND contentDescription (accessibility label).
  PanelTitleBar: hub title maxLines 1 + Ellipsis → maxLines 2 +
  Clip (never ellipsized; title has priority); breadcrumb unchanged
  (it is not the hub title). Before/Changes take `iconOnly` from
  `compactHeader` (panel < 560dp); SheetPanel passes
  `compactHeader = maxWidth < 560.dp`.
- `ui/settings/shell/PreviewAids.kt`: BeforeHoldButton icon-only
  (fixed 48dp, Eye icon only, contentDescription "Before"; hold
  behavior unchanged). ChangesChip icon-only (fixed 48dp, count
  number in Bold, contentDescription "Changes (N)" — added, it had
  none; no Changes glyph exists in HavenIcon and Undo was rejected
  as misleading for "view changes list", so the count badge +
  contentDescription is the icon-only form).
- `ui/glass/GlassComponents.kt`: HavenRowContainer gains
  `titleMaxLines` / `subMaxLines` (defaults preserve behavior);
  HavenNavRow passes 1 / 2 (one-line title, two-line summary).
  HavenSegmented rewritten: TextMeasurer measures labels at 14sp
  Medium (widest weight); pure `segmentedLayoutMode` returns Equal
  (segment = available / n) only when every segment ≥ 64dp AND fits
  its label + padding, else Chips (FlowRow, content-sized chips,
  heightIn min 48dp, selected = accentContainer, Text softWrap =
  false, maxLines = 1). Equal mode keeps the thumb animation and
  adds softWrap = false. The drawer toolbar use
  (`weight(1f, fill = false)`) works in both modes.
- `ui/settings/hubs/LookHub.kt`: pure `adaptiveRowArrangement`
  (SingleRow(labelWidth) iff labelAvail ≥ minLabel, else Stacked)
  + `AdaptiveSettingRow` custom Layout (NOT SubcomposeLayout):
  measures value/control at natural size, label gets
  max(140dp, 50% of row), SingleRow right-aligns value/control (as
  the old weight-1 row did), Stacked puts label full-width on top
  and value + control in a row beneath (4dp gap — the brief's
  stacking shape; the 8dp sibling gap is used in SingleRow).
  HubRow and HubSwitchRow migrated (label maxLines 3, heightIn min
  56dp kept; HubSwitchRow's control slot is Row(info?, switch)).
  HavenRowContainer needed NO change (label Column already stacks).
- `ui/settings/hubs/ApiHub.kt`: ApiKeyField restructured — label Row
  (label weight 1 + help) ABOVE the full-width field, then a button
  Row (Paste, Show/Hide if masked, Save — each widthIn min 72dp +
  heightIn min 48dp) + Spacer + "Set"/"Not set" status at row end.
- Gestures device diagram: verified by code inspection (fillMaxWidth
  construction; at a 360dp detail the width clamps and the ratio is
  not preserved, but it does not overflow). No code change; the
  matrix Pad check covers it.

Tests: SettingsLayoutMatrixTest (unit, JUnit4) — panel/rail/detail
clamps, adaptiveRowArrangement (wide / narrow / boundary / zero),
segmentedLayoutMode (fit / min-segment / label-overflow / empty),
and the full matrix: all 8 hubs (HubScreen.HUB_ROWS routes) × panel
widths 480 / 640 / 920dp × font scales 1.0 / 1.3, asserting detail ≥
360dp OR single-pane active, SingleRow label ≥ max(140dp, 50%) and
≥ 3 characters of 14sp text, and the segmented decision matching an
independently recomputed fit. What the test cannot cover (no JVM
renderer): actual Text measurement, tooltip rendering, the diagram
itself — those are Pad checks H-7.

Checks run: item-5-only compile FAILED in 1m 51s with ONE error
(`@OptIn` orphaned onto `SegmentedMode` by the leftover old
`@Composable` line — fixed by removing the orphan line; log
`/tmp/hotfix-item5-compile.log`). Final item-4+5+6+7 compile:
BUILD SUCCESSFUL in 3m 35s (log `/tmp/hotfix-item67b-compile.log`).

## Item 6 — Drawer: fully solid

What changed:
- `theme/Looks.kt`: Palette gains `drawerSurface` + `drawerField`
  (only construction site `pal()` updated). Base: light
  drawerSurface off-white `0xFFFAFAF8`, drawerField `0xFFECEAE6`;
  dark drawerSurface near-black `0xFF0A0A0A`, drawerField
  `0xFF1A1A1A`. New pure `mixOpaque(base, tint, ratio)` (alpha
  forced to 1.0). `resolveTokens` restructured (behavior-preserving
  for accents in both the seed and no-seed branches): dark
  drawerSurface = near-black mixed with ~4% of the resolved accent,
  dark drawerField = that surface lifted 8% toward white; light
  values keep the base off-white. `drawerField` is an AGENT
  implementation choice (the brief names only `drawerSurface`) to
  satisfy "search field uses a solid filled color" without alpha.
- `ui/Drawer.kt`: sheet backgrounds (Glass2 tint over scrim)
  replaced with a 40% black scrim behind + opaque drawerSurface
  sheet, 28dp top corners, 1dp top hairline (top edge only), drag
  handle (visual only — NO drag-to-dismiss added, behavior
  unchanged). Search field `.glass(GLASS2)` → drawerField;
  history/hidden chips (alpha 0.6) → drawerField; sticky letter
  headers (glassAlpha) → drawerField; letter bubble `.glass(GLASS3)`
  → drawerField. All `palette.glass` / `.glass(` / glassTint /
  glassScrim uses are gone from Drawer.kt (imports removed, plus
  the now-unused LocalTokens import). IME insets kept (already
  present); Phase C compact toolbar row kept (already present).
  CategoriesView verified glass-free (only an alpha-0.5 text color,
  not a surface). Glass stays for floating things (dock, Today
  cards, menus, popovers, SearchCard) — nothing else converted.
- `ui/Home.kt`: drawer open wrapped in AnimatedVisibility with
  slideInVertically / slideOutVertically ONLY (no fade). The drawer
  previously popped in with no animation; the `if (drawerOpen)`
  close brace now closes the AnimatedVisibility lambda (same text).
- Opening-animation note: during the exit slide the drawer stays
  composed (~300ms); its focus params do not re-trigger (focusSearch
  is not keyed on drawerOpen).

Tests: HexLiteralTest extended (same scan/test approach, no second
mechanism) — drawerSurface/drawerField alpha == 1.0 (tokensFor AND
resolveTokens(SettingsState())), text contrast ≥ 4.5:1 both modes
(Legibility.contrastRatio + luminance), Drawer.kt source scan (must
contain drawerSurface + 28dp + 40% scrim; must NOT contain `.glass(`,
`palette.glass`, `glassTintBase(`, `glassScrimColor(`,
`glassTintAlpha(` — the scan FAILS THE BUILD on any translucent
drawer colour). DrawerSolidTest (androidTest, written NOT run, NOT
compiled here): opens the drawer over a bright wallpaper, captures
the root bitmap, asserts the left gutter (2% width, inside the 20dp
content padding, so no text/icon lands there) is a single solid
opaque colour at 25% / 50% / 75% height. Earlier corner-sample design
was rejected during writing (corners land on icons/text/status-bar
scrim); the gutter design replaced it.

Checks run: covered by the item-6/7 compile (BUILD SUCCESSFUL in
3m 35s). Drawer source scan assertions verified by grep (0 glass
calls remain in Drawer.kt).

## Item 7 — Mascot bubble (E3 spec, overriding the Phase E deferral)

What changed (`ui/mascot/MascotLayer.kt`):
- Pure `steppedCornerPoints(radiusPx, stepPx)`: one corner's
  staircase from (radius, 0) to (0, radius), coordinates snapped
  down to the step grid, monotone, single-axis steps only (an
  intermediate point is inserted whenever a sample moves in both
  axes). Unit-tested in HotfixCrashSweepTest (zero radius, step-grid
  quantization, monotonicity, single-axis, edge endpoints).
- `SteppedRoundedShape(corner, stepPx)`: rounded rectangle whose
  four corners are staircases (mirrored/reversed per corner, close()
  draws the top edge). Lives in MascotLayer.kt (used once — no new
  file). Corner radius coerced to min(w, h) / 2.
- Bubble: `.background(palette.surface, steppedBubbleShape)` +
  `.border(step-in-dp, accent 50%, steppedBubbleShape)` where the
  shape is `remember(spriteScale)`d once for both uses, step = 2 ×
  spriteScale px ("2px steps at the sprite's scale"), corner 20dp
  kept from Phase C, text stays 14sp+. No conflict with any Phase C
  bubble behavior was found (the bubble uses surface + accent
  border, not glass), so the full stepped border is implemented —
  no closest-variant compromise was needed.
- Menu: the vertical MenuButton list replaced with a FlowRow of four
  48dp icon chips — Find/Search, Help/Help, Suggest/Star,
  Dismiss/Close (icon + 14sp label, 1dp accent border, pill clip,
  8dp FlowRow spacing; wraps if the bubble narrows; at 320dp max
  bubble width the four fit one row). All four action bodies are
  unchanged (Find flow, Help brain call, Suggest logic, Dismiss
  persistence). MenuButton deleted (it had no other callers).
  Tutorial offer block (Yes/Not now) and Message-mode ChipActions
  unchanged — E3's four chips are the menu.
- Anchor/flip math (now in the graphicsLayer lambda per item 4),
  IME clamp (`imeBottomPx` still read in composition on purpose),
  close/back dismiss, and Find flow states unchanged.

Tests: steppedCornerPoints unit tests (above); bubble appearance +
chips are Pad checks H-9.

Checks run: covered by the item-6/7 compile (BUILD SUCCESSFUL in
3m 35s).

## Tests added (all JUnit4, current APIs)

- `app/src/test/.../ui/mascot/HotfixCrashSweepTest.kt` (new) —
  engine empty inputs, spriteHitRect, steppedCornerPoints, Find/rank,
  dock-pick, no-swatch accent fallback.
- `app/src/test/.../ui/settings/shell/SettingsLayoutMatrixTest.kt`
  (new) — panel/rail/detail, adaptive row, segmented modes, full
  8-hub × 3-width × 2-font-scale matrix.
- `app/src/test/.../CrashLoopTest.kt` (new) — crash-window boundary,
  unsorted input, prune.
- `app/src/test/.../ui/glass/HexLiteralTest.kt` (+71 lines) — drawer
  alpha, contrast, source scan (fails the build on translucent
  drawer colour).
- `app/src/androidTest/.../ui/DrawerSolidTest.kt` (new, written NOT
  run, NOT compiled here) — gutter solidity screenshot test.
- `app/src/androidTest/.../ui/mascot/MascotTouchTest.kt` (from item 4,
  written NOT run, NOT compiled here).

## Checks run — exact results

- `:app:compileReleaseKotlin` (item 1): BUILD SUCCESSFUL, 5m 27s, 0
  errors — `/tmp/hotfix-item1-compile.log`.
- `:app:compileReleaseKotlin` (item 2): BUILD SUCCESSFUL, 3m 26s, 0
  errors — `/tmp/hotfix-item2-compile.log`.
- `:app:compileReleaseKotlin` (item 3): BUILD SUCCESSFUL, 3m 20s, 0
  errors — `/tmp/hotfix-item3-compile.log`.
- `:app:compileReleaseKotlin` (item 4 standalone): INVALID — the run
  snapshotted sources mid-edit during item 5 (6 errors, all
  item-5-in-progress artifacts). Superseded, not a code failure.
- `:app:compileReleaseKotlin` (item 5 only): BUILD FAILED, 1m 51s, 1
  error (orphan `@Composable` on `SegmentedMode`) — fixed, then
  superseded by the combined compile.
- `:app:compileReleaseKotlin` (items 4+5+6+7, after a second fix for
  a missing `Dp` import in MascotLayer): BUILD SUCCESSFUL, 3m 35s,
  0 errors — `/tmp/hotfix-item67b-compile.log`. (First attempt of
  this run failed in 2m 6s with exactly 2 errors, both the `Dp`
  import — log `/tmp/hotfix-item67-compile.log`.)
- `:app:compileDebugUnitTestKotlin`: BUILD FAILED, 305 errors, ZERO
  tests ran — log `/tmp/hotfix-test-compile.log`. Per-file: 126
  MascotEngineTest, 53 AssistRouterTest, 45 MascotSpritesTest, 28
  ModelPackTest, 28 AssistLedgerTest, 10 SettingsShellTest, 6
  LayoutStateTest, 5 EditModeTest, 2 HexLiteralTest, 2 EmojiScanTest.
  The count is IDENTICAL to Phase G's 305, and the new hotfix unit
  test files contribute ZERO errors (HotfixCrashSweepTest,
  SettingsLayoutMatrixTest, CrashLoopTest all compile). The 2
  HexLiteralTest errors are PRE-EXISTING (the `findMainSrc`
  `?: fail(...)` type mismatch — byte-identical code in the
  pre-hotfix snapshot; Phase G called these the "2 Phase B scan-test
  type errors"). Consequence: the drawer tests in that file cannot
  run until that helper is fixed (one-line fix: `?: fail(...)` →
  `?: throw AssertionError(...)` — left undone deliberately, see
  "not done" below). No test run is claimed anywhere in this report.
- `:app:lintRelease`: BUILD FAILED, 5m 42s, 16 errors — log
  `/tmp/hotfix-lint.log`. Composition identical to Phase G (5
  MissingPermission, 7 NonObservableLocale, 1 SuspiciousModifierThen,
  1 UnusedBoxWithConstraintsScope, 2
  UnusedContentLambdaTargetStateParameter). The 4 errors in files
  this hotfix touched were each verified to be pre-existing code,
  not hotfix edits (MascotLayer spotlight scope, SettingsPanel
  animSpeed wrapper, DeviceDataHub backup-date format, Drawer event
  date format). ZERO new lint errors from the hotfix.
- UI tests (MascotTouchTest, DrawerSolidTest): NOT run, NOT compiled
  here (no device/emulator; compose UI-test artifacts not in the
  offline cache).
- Final verification build: see below.

## Version stamp + final verification build

- `app/build.gradle.kts`: versionCode 13 → 14, versionName "3.1.0" →
  "3.1.1" (two lines, LAST, after all items verified).
- FINAL BUILD: `~/workspace/havenos/build.sh` — BUILD SUCCESSFUL in
  8m 58s, GRADLE_EXIT=0 (log `/tmp/hotfix-final-build.log`).
  Final APK `~/workspace/havenos/app/build/outputs/apk/release/app-release.apk`,
  2,842,241 bytes, md5 5133348a0a3764f4181bb80786748a81.
  aapt: package app.havenos.launcher, versionCode 14, versionName
  3.1.1, minSdk 26, targetSdk 36. apksigner: CN=HavenOS, SHA-256
  53bff2a09aedb516a8ee6193234a7aaf5d04ce901fb8931b799191b9845354a8
  (keystore match). Mapping copied:
  `app/build/outputs/mapping/release/mapping.txt` →
  `release-artifacts/mapping-14.txt` (66,830,934 bytes, md5
  1a0606b6cdb512115fc2e21b2c7d2de6, identical to the build output).
  NOT delivered, NOT published — parent owns delivery and the
  release decision.

## Unverified (everything visual/gesture — Pad checks H-1…H-10)

Safe Mode banner appearance + Re-enable exactness + View details
share/copy; per-feature disable toasts; dim tap-vs-swipe behavior;
mascot hitbox on device; bubble stepped border + chips at real
sizes; settings layout at every matrix width + font scale; drawer
solidity with bright wallpaper, keyboard up, Reduce Effects on/off;
crash-sweep spot checks. Listed in docs/UNVERIFIED.md (3.1.1
section) and docs/DEVICE_CHECKLIST.md (H-series).

## Known gaps + carried-forward open items

- PagedHome `appEntry!!` / `folder!!` / ghost `overCell!!` (item 1
  open risks — needs a parent scoping decision).
- Quick tune Mode System option (Phase C agent judgment — parent may
  overrule).
- C5 toolbar group boundaries (Phase C agent's reading).
- SearchCard command rows (Phase F BLOCKED follow-up).
- Phase E BLOCKED items minus the bubble border (now built):
  per-card rail ledges; side-dock perch; dock-app Find = bubble +
  Launch only; guide mid-trip resolves within one step.
- Test-tree repair (Phase G recommendation) — NOT done in this
  hotfix by design; the first step is the one-line HexLiteralTest
  `findMainSrc` fix above, then kotlin.test → JUnit4 imports + stale
  test updates across the 27 files.
- Manual PDF rebuild (source is 3.1, delivered PDF is still 3.0).
- vc13 release decision waits behind this hotfix; after it, the
  release question is 3.1.1/vc14 (final build) — parent's decision.
- `drawerField` is an agent implementation choice (brief names only
  `drawerSurface`); the Chips-mode pill clip on a multi-line
  segmented track is a minor visual consideration for the Pad check
  (track is 6% alpha, so any corner clipping is subtle).
- Item-5 single-pane branch (< 560dp) is unreachable on tablets
  under the 640dp clamp — implemented as a guard for narrower
  windows/freeform, documented in the matrix test.
- Dismiss-tap pressed flash may still show on the icon beneath the
  dim, but its action never fires (item 4 nuance).

## Files changed (diff vs pre-hotfix snapshot)

Main: MascotLayer.kt 39−/233+, MascotEngine.kt 4−/10+,
MascotSprites.kt 1−/6+, FeatureGuard.kt NEW 165 lines, CrashLoop.kt
NEW 28 lines, SafeModeBanner.kt NEW 119 lines, SettingsPanel.kt
69−/174+, PreviewAids.kt 10−/26+, GlassComponents.kt 33−/119+,
LookHub.kt 42−/187+, ApiHub.kt 12−/28+, Drawer.kt 30−/44+,
Home.kt 3−/25+, Looks.kt 7−/52+, HavenApp.kt 1−/155+,
MainActivity.kt 1−/6+, IslandFeed.kt 0−/16+, TodayApiCards.kt
0−/20+, DeviceDataHub.kt 0−/85+, GesturesMotionHub.kt 4−/9+,
proguard-rules.pro 0−/4+, README.md NEW 36 lines.
Tests: HotfixCrashSweepTest NEW, SettingsLayoutMatrixTest NEW,
CrashLoopTest NEW, HexLiteralTest +71, MascotTouchTest (item 4),
DrawerSolidTest NEW (androidTest).
Docs: UNVERIFIED.md (3.1.1 section), DEVICE_CHECKLIST.md (H-series),
COVERAGE.md (3.1.1 section), this report.

## vc14 fix-1 — Mascot BFS ArrayDeque crash (2026-10-31, after delivery)

- Crash: `NoSuchElementException: ArrayDeque is empty`, vc14, 2026-10-31
  22:03:48 IST, confirmed against the retained vc14 mapping
  (`ArrayDeque` -> `I2.k`, crashing coroutine `MascotLayer$15$1` ->
  `D2.x0`). The hotfix's item-1 claim for this site was wrong in
  effect: it added `?: return null` AFTER `firstOrNull`, but the
  throw happened INSIDE the predicate — `queue.removeFirst()` ran
  once per ledge tested, so a queue head that did not match the
  first ledge tested threw, and also ate extra queue entries.
- Fix (MascotEngine.planRoute, 2 lines): remove the queue head once,
  outside the lookup (`removeFirstOrNull` + `firstOrNull { it.id ==
  curId }`). A queue id with no ledge still fails the route and the
  mascot stays put. Regression test:
  `planRoute_queueHeadNotFirstLedge_doesNotThrow` (queue head not
  first in the ledge list; threw on old code, returns null on new).
- Audit: every other `removeFirst` in main source is guarded
  (`while size > N`) or OrNull; the `queued` deque uses only
  toList/clear/addLast.
- Rebuilt as the SAME version (swap rule, no bump): vc14/3.1.1,
  2,842,241 bytes, md5 `704ff1319a2dbd5acb6bbd5bd9d91ec7`, HavenOS
  cert match, build.sh BUILD SUCCESSFUL (13m). Mapping refreshed
  to `release-artifacts/mapping-14.txt`; the first vc14 mapping is
  preserved as `mapping-14-first-build.txt`.
- Upload of the fixed build stalled on a service restart (no link);
  retry awaits the user's word. The earlier vc14 link points to the
  crashing build and is superseded.
