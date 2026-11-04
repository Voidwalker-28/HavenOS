# HavenOS 3.1 Phase B — Haven Glass design system + accent system

Date: 2026-10-30. Baseline: coordination fix + Phase A tree.
Version stays 3.0/vc12 (3.1.0/vc13 only in the final step).
No dependency added. No Room migration. Signing untouched.
`~/workspace/havenos-github/` untouched. Nothing published, uploaded,
or delivered — verification build only; the parent owns delivery.

## Baseline caveat (read first)

The Phase B snapshot `~/workspace/havenos-pre-3.1-phase-B/` does NOT
contain `ui/glass/` or its tests, although the glass files were
written before this session's compile runs. The snapshot is therefore
a valid baseline for every non-glass file, and the glass files are
new-in-Phase-B relative to it (written pre-compaction, repaired and
verified this session — see "Glass repair" below). The snapshot was
NOT re-taken: re-snapshotting would destroy the baseline.

## What was built

### B1 — One entry point: ui/glass

- `ui/glass/` exposes `Modifier.glass(level)`, all components, and
  HavenTheme tokens (HavenSpacing, HavenRadii, HavenType, GlassLevel
  specs, AccentRoles). `LocalAccentRoles` is provided by HavenTheme
  in `theme/Looks.kt` (glass holds the pure token type; the theme
  holds the provided value — no second HavenTheme composable).
- `glass()` draws level tint + hairline only and adds NO new blur
  mechanism: blur comes from the existing window blur-behind where
  the host set it (the same Partial-tier mechanism GlassSurface
  uses). GlassSurface itself is untouched. Depth comes from
  blur/tint/hairline, never shadows: `glass()` draws no shadow.

### B2 — Materials

- Glass1: dark white 7% + 12% hairline; light black 5% + 10%
  hairline; card 28dp, grouped-list 20dp.
- Glass2: dark white 12%, light white 70%, hairline 16%, 28dp top
  corners for sheets.
- Glass3: dark white 16%, hairline 18%, menu 20dp, dialog 28dp.
  Glass3-light tint is an INFERENCE (prompt silent): white 85%.
- Legibility: text-carrying glass sits on ≥40% dark scrim (dark) /
  60% light scrim (light) for 4.5:1 on any wallpaper.
- Fallback (Reduce Effects, blur unavailable below API 31 or
  disabled, frame-time trouble): same layout, no blur, tint alpha +6
  points, never fully transparent text backgrounds.
- At most two blurred layers: DOCUMENTED, NOT ENFORCED. No layer
  counter was built (a new mechanism, out of minimal scope).

### B3 — Shape / spacing / type

- Radii card 28, group 20, control 14, chip full, icon tile 12.
  Spacing 4/8/12/16/24/32dp. Row height 56dp min (grows for two
  lines). Touch targets 48dp.
- Type: display 56 Light, hero numerals 34 Light, title 28 Regular,
  section label 12 Medium 0.6sp tracking in accent, row label 16,
  row value 14 muted, caption 12 muted; line height ~1.35.
- Labels wrap to two lines before ellipsizing; long-press on an
  ellipsized label shows the full text in a Glass3 popover.
- Exception (documented): HavenSegmented labels are maxLines=1 —
  segments are fixed-height and wrapping would break thumb geometry.

### B4 — Icons: one outline family, zero emoji

- DEVIATION (honest): the icons are ORIGINAL outline vectors, NOT
  Material Symbols. The extended set is not a dependency, is not in
  the Gradle cache, and the build is offline, so no symbol font
  could be fetched; hand-authoring true Material Symbols paths from
  memory would be fabrication. Style matches the prompt (24dp
  viewport, ~1.7 stroke, round caps/joins, stroke Black, transparent
  fill, tint recolors at call sites).
- One `HavenIcon` enum maps every hub, row, permission, dock, and
  menu entry, plus weather kinds and media controls (new in this
  session: MediaPrevious/MediaNext for island skip buttons). Icon
  tile 36dp, 12dp radius, accent-soft background.
- EVERY in-range emoji/pictograph removed from the UI, verified by
  code-point scan (0 hits in main Kotlin + res values): weather
  symbols -> WeatherKind + `weatherIcon()` vectors (data/Weather.kt
  now returns `Pair<String, WeatherKind>`); hub registry emojis ->
  HavenIcon hub entries; permission glyphs -> HavenIcon permission
  entries; island play/pause/skip/charging/bell -> vectors; edit
  toolbar Done/Calm/Settings/snap-tick/selection/dismiss -> vectors;
  drawer clear, icon-pack tick, wallpaper ticks, GitHub star,
  Today-page sun, music placeholder -> vectors or dropped ticks.
- The pre-compaction emoji inventory MISSED 14 hits, all fixed this
  session: hub registry emojis (HubScreen), panel close + "All
  settings" glyphs (SettingsPanel), wallpaper "Solid ✓"/"Photo ✓"
  (LookHub), and a check mark inside a MascotModel comment.
- LEFT FOR PHASE C (documented, not silent): out-of-range geometric
  glyphs — edit toolbar ↩↪⊞, snap off-state ○, DevicePermRow ●/○
  dots and ⓘ help glyph. They are outside the test ranges and belong
  to the Phase C toolbar/menu restyle.
- Slightly lossy, documented: mascot emotes FOUND/CELEBRATE were a
  check mark and are now "!" / "!!"; the at-a-glance weather line is
  a plain String, so it drops its glyph entirely (label + temperature
  remain); wallpaper buttons drop the tick (selection already shows
  by border).

### B5 — Components

GlassCard; GroupedSection (label + rows in ONE Glass1 container,
hairline dividers, rows as lambdas so dividers land between them);
SettingRow variants (navigation, switch, segmented, slider, value,
action, info); HavenSegmented (recessed track dark white 6% / light
black 6%, sliding accent-soft thumb, spring, each label drawn once);
HavenSwitch (52×32dp, check on thumb when on, accent track on /
neutral off); HavenSlider (no −/+ buttons, value bubble while
dragging, haptic tick per step, Auto chip where applicable,
debounced writes via onCommit on release, as before); HavenBanner
(tonal accent-container bg, icon tile, one action, no hardcoded
colors); HavenDialog, HavenMenu, HavenSnackbar (Glass3); EmptyState
(small mascot pose slot, one-line headline/body, one verb button);
Skeleton (shimmer, static under Reduce Motion). Help: 48dp touch
target opening a Glass3 popover (HavenHelp, delivered standalone;
Phase C wires it into every help button). Each component carries
pressed ripple, focused accent ring, and disabled dim + reason.

### B6 — Motion

App open 320ms, folders 280ms, dock 240ms stay; hub transitions 220ms
shared axis; one spring for switch/segmented; haptic ticks on
switch/step/snap. `havenDurationMs` precedence (B6 + A8 + §0 "Reduce
Motion always wins"): Reduce Motion -> 100ms fade; else Motion Off
-> instant; else scaled via speedDuration. Thumb springs snap under
Reduce Motion or Off (a thumb has no fade target).

### B7 — Accent system

- Sources as a segmented choice in Look -> Accent: Wallpaper
  (WallpaperManager.getWallpaperColors, API 27+, suggestion chip
  "Match wallpaper" only, NEVER auto-applied), System (Material You
  dynamic color, Android 12+, hidden below API 31), Curated (12 muted
  colors, prompt starting values + names kept), Custom (hue strip +
  lightness/saturation square in HSL, live preview on the Look hero,
  "Adjusted for readability" note when clamped, up to six recents,
  eyedropper sampling the wallpaper preview, hex entry under
  Advanced).
- One seed -> all roles via one function `accentRolesFor` (accent,
  onAccent, accentContainer with strength alpha, accentOutline; dark
  + light). No Material Color Utilities dependency (not present,
  offline): perceptual tone mapping + contrast clamp. Components
  read only these roles. Contrast enforced in code: accent text on
  glass ≥4.5:1, icons/borders ≥3:1, onAccent on accent ≥4.5:1;
  failing seeds clamped, never rejected.
- Strength Subtle/Balanced/Vivid (default Subtle; container alpha
  ~10%/18%/30%). Accent marks state + small section labels only,
  never fills large areas by default.
- Storage is DataStore, NOT a Room migration (additive new
  preference keys, pure mapping, old keys kept for backup round-trip,
  never dropped). Load rule: new key present -> read new keys; else
  old keys present -> `migrateAccent(oldMode, oldArgb)`; else fresh
  install -> Curated/Ember. Old-key compat writes in the new setter
  (system maps to mode 1 / argb 0; new keys authoritative).
- Migration mapping (old 8 swatches -> nearest curated by redmean
  RGB distance, pinned in tests): blue -> Cobalt, teal -> Lagoon,
  purple -> Iris, pink -> Clay, amber -> Ember, green -> Lagoon,
  rust -> Clay, slate -> Clay. Mode 0 -> DEFAULT (visible accent
  unchanged); mode 1 + argb 0 -> DEFAULT; mode 2 + nonzero -> CUSTOM
  with colour kept + seeded into recents; mode 3 -> WALLPAPER
  (stored argb was never rendered, nothing visible to preserve).
- Changes list shows names (e.g. "Accent: Iris to Ember"), never raw
  ints (`accentDisplayName`). Backup gains the five new accent
  fields (unconditional write, `has()`-guarded read; old backups
  unaffected). The old `setAccent` setter is deleted (zero callers
  after migration); panel-close commit, Changes diff, Accent revert,
  and Look reset all use `setAccentSelection`.
- DEFAULT is a legacy-only display: the source segmented shows
  "Default" only while the stored selection is the migrated default;
  any other pick leaves it behind (it was never a real choice, just
  the absence of one).
- Eyedropper scope (honest): samples the SYSTEM wallpaper's reported
  colours (WallpaperManager) as pickable chips. No bitmap sampling
  exists and no HavenOS-wallpaper palette extraction exists, so
  HavenOS wallpapers are not sampled.
- Wallpaper seed refresh: re-read when the source or the
  HavenOS-wallpaper settings change; a system-wallpaper change while
  Wallpaper is already selected picks up on next launch (no
  wallpaper-change listener exists).

### Purple Default launcher card — FIXED (not grandfathered)

The card was NOT a hex literal: HavenTheme never set
primaryContainer/onPrimaryContainer/secondary/tertiary, so they fell
back to Material3 baseline purple. HavenTheme now sets every
container slot from accent roles (primary = roles.accent, onPrimary
= roles.onAccent, primaryContainer = roles.accentContainer,
onPrimaryContainer = palette.onSurface, secondary/tertiary +
containers = accent roles). This fixes all four sites at once
(DeviceDataHub card, DrawerSearchHub selected colour,
GesturesMotionHub background, PageOverview background).

### Hex-literal fixes

- Home.kt red badges (2x `Color(0xFFE53935)`) -> the HomeColors
  badge token (dark-aware).
- Widgets.kt flip-tile `Color(0xFF17181C)` -> named token
  `FlipTileBackground` in HomeTokens.kt (allowlisted Tokens file).
- Wallpaper data tables (data/Wallpaper.kt) and LookHub wallpaper
  swatch lists stay as data, not composable literals — out of the
  lint test's scope by design (Phase C may tokenize them).

### Glass repair (this session)

The pre-compaction glass files had never been compiled. The first
compile failed with ~40 errors, all in `ui/glass/`; fixed minimally:
wrong Dialog import (`material3.Dialog` -> `ui.window.Dialog`);
roles are Int argb but were passed where Color was expected (wrapped
in `Color(...)` at 20+ sites); PathBuilder uses `curveTo`, not
`cubicTo` (26 sites); the `HavenIcon` composable collided with the
`HavenIcon` enum (compiler resolved calls to the private enum
constructor) -> composable renamed to `HavenIconImage`; a
BoxWithConstraintsScope `maxWidth` leaked into a ColumnScope (moved
to the BoxWithConstraints content top); `DARK_GLASS_BG` Long literal
(-> `.toInt()`); missing `animateFloat` import; a bad
`androidx.compose.ui.graphics.ImageVector` import (correct package
is `graphics.vector`).

## Files changed (line counts are current totals; deltas in brackets)

New, main:
- ui/glass/AccentTokens.kt — 345 lines (roles, curated 12, migration,
  recents, HSL helpers, wallpaper/system seed resolution)
- ui/glass/GlassTokens.kt — 142 lines (materials, radii, spacing, type,
  `Modifier.glass`)
- ui/glass/GlassComponents.kt — 866 lines (all B5 components)
- ui/glass/GlassMotion.kt — 53 lines (durations, spring, precedence)
- ui/glass/HavenIcons.kt — 450 lines (enum + original outline vectors
  + MediaPrevious/MediaNext)
- ui/glass/WallpaperAccent.kt — 55 lines (wallpaper/system seeds)
- ui/WeatherIcon.kt — 23 lines (`weatherIcon` mapper)

New, tests (written, NOT run):
- ui/glass/AccentTokensTest.kt — 170 lines
- ui/glass/GlassTokensTest.kt — 56 lines
- ui/glass/EmojiScanTest.kt — 55 lines
- ui/glass/HexLiteralTest.kt — 51 lines

Modified, main (delta vs snapshot):
- data/Settings.kt (+83): new accent fields + keys, load-time
  migration, `setAccentSelection`, resetSection(1) B7 default,
  `setAccent` deleted
- data/Backup.kt (+17): five new accent fields, guarded read
- data/Weather.kt (+5): WeatherKind enum, `weatherInfo` returns kind
- theme/Looks.kt (+76): `resolveTokens`/`resolveAccentRoles` via
  seeds, `LocalAccentRoles`, full scheme containers (purple fix)
- ui/settings/hubs/LookHub.kt (+341): source segmented, curated grid,
  Custom picker (hue strip + square + recents + eyedropper + Advanced
  hex), strength, hero seed preview, ACCENT_SWATCHES deleted
- ui/settings/shell/SessionDiff.kt (+21): commit/diff/revert/reset via
  `setAccentSelection` + display names
- ui/Home.kt (+24): panel onClose via `setAccentSelection`, glance
  glyph dropped, pack tick + badge tokens
- ui/HomeTokens.kt (+3): `FlipTileBackground` token
- ui/Widgets.kt (+28): weather icons, music icon, flip-tile token
- ui/WidgetReal2.kt (+22): weather icons (2 sizes + hourly + daily)
- ui/TodayPage.kt (+3): Today sun icon
- ui/TodayApiCards.kt (+17): wallpaper tick dropped, star icon
- ui/Drawer.kt (+7): clear icon
- ui/EditMode.kt (+51): `iconVector` toolbar path, snap/check/selection/
  dismiss icons
- ui/island/IslandView.kt (+18): collapsed/expanded/skip icons
- ui/mascot/MascotModel.kt (+2): FOUND/CELEBRATE emotes
- ui/settings/hubs/DeviceDataHub.kt (+9): permission icons
- ui/settings/shell/HubScreen.kt (+4): hub icon registry + row icons
- ui/settings/shell/SettingsPanel.kt (+9): rail icons + close icon

Modified, docs:
- docs/COVERAGE.md (+224): Phase B section + BLOCKED/limitations
- docs/DEVICE_CHECKLIST.md (+48): checks B-1…B-15
- docs/UNVERIFIED.md (+32): Phase B unverified section

## Tests added

- AccentTokensTest: every curated color passes accent ≥4.5 +
  outline ≥3.0 + onAccent ≥4.5 in dark AND light; determinism;
  strength alphas; Sand-light clamps; Cobalt-dark needs no clamp;
  migration incl. the 8 pinned swatch mappings; display names;
  recents round-trip + cap 6; HSV round-trip within 1 per channel.
- GlassTokensTest: tint/hairline alphas, fallback 0.06, scrims 0.40 /
  0.60, radii 28/20/14/12, spacing scale.
- EmojiScanTest: scans main Kotlin + res values/drawable XML for
  U+1F300–U+1FAFF, U+2600–U+27BF, U+2B50, U+FE0F; fails listing the
  first 20 offenders; fails if sources are not found.
- HexLiteralTest: regex `Color\s*\(\s*0x[0-9A-Fa-f]+`; allowlist =
  theme/**, ui/glass/**, *Tokens.kt, ui/Legibility.kt; fails listing
  the first 20 offenders; fails if sources are not found.
- NONE RAN: sandbox test executor broken (pre-existing);
  SettingsShellTest has 10 pre-existing compile errors. The script
  equivalents of the emoji + hex scans WERE run against the tree and
  pass (0 hits each, main Kotlin + res values).

## Checks run + exact results

- `./build.sh compile` after accent wiring: first run FAILED (~40
  errors, all pre-compaction glass files, never compiled before);
  after the glass repair: BUILD SUCCESSFUL in 11m 11s.
- `./build.sh compile` after emoji swaps: first run FAILED (8 errors:
  missing `size`/`width` layout imports in EditMode, TodayApiCards,
  DeviceDataHub); after the import fixes: BUILD SUCCESSFUL in
  11m 33s, zero errors.
- Final `./build.sh`: BUILD SUCCESSFUL in 34s (incremental).
- Verification APK: `app/build/outputs/apk/release/app-release.apk`,
  2,714,872 bytes, md5 `d17d18d696fc6acf83778cf21d5147eb`
  (verification build only — NOT delivered anywhere).
- aapt dump badging: package `app.havenos.launcher`, versionCode 12,
  versionName 3.0, minSdk 26, targetSdk 36, label HavenOS.
- apksigner verify --print-certs: exit 0; CN=HavenOS, SHA-256
  `53bff2a09aedb516a8ee6193234a7aaf5d04ce901fb8931b799191b9845354a8`
  — matches the persisted havenos keystore.
- Secrets grep over new/changed code: clean (only false positives:
  the words "tokens" and "Durations" in comments).
- Emoji scan (script equivalent): 0 hits. Hex scan (script
  equivalent, test allowlist): 0 hits. Res values scan: 0 hits.

## Deviations (with reasons)

1. Icons are original outline vectors, not Material Symbols
   (offline build, no such dependency, no cache entry; authoring
   symbol paths from memory would be fabrication).
2. Glass3-light tint white 85% is an inference (prompt silent).
3. Two-blur-layer rule documented, not enforced (no layer counter).
4. Eyedropper samples system WallpaperColors only (no bitmap
   sampling or HavenOS-wallpaper extraction exists).
5. Wallpaper seed refreshes on source/wallpaper-settings change;
   system-wallpaper change while Wallpaper is selected picks up on
   next launch (no listener).
6. HavenHelp delivered standalone; Phase C wires it everywhere.
7. Out-of-range geometric glyphs left for Phase C (see B4).
8. Mascot emotes + glance line slightly lossy (see B4).
9. DEFAULT source is legacy-only display (see B7).
10. Accent storage is DataStore keys, not a Room migration (additive,
    old keys kept; a Room table would be a gratuitous layer).
11. `Settings.kt` importing `ui.glass` pure tokens is accepted on the
    codebase's own precedent (data/Backup.kt imports ui snapshot
    helpers; data/LayoutProfiles.kt imports ui reflow/validate).
12. HavenSegmented labels maxLines=1 (documented exception).
13. Wallpaper data tables stay hex data (not composable literals).
14. No piece was stopped as unimplementable; every scoped item is
    built or documented above.

## Unverified items (need Redmi Pad 2 — Phase G checklist)

All Phase B visual/gesture behaviour; the WallpaperColors tablet
hypothesis; migration on a real existing install (mapping covered by
written-not-run tests; the DataStore load path is unproven);
two-blur-layer behaviour on screen; Custom picker drag feel and
thumb tracking; HavenIcon rendering at 12–24dp; font scale 1.3;
purple-card fix on device; all four Phase B test files (executor
broken). Full list: docs/DEVICE_CHECKLIST.md B-1…B-15 and
docs/UNVERIFIED.md Phase B section.
