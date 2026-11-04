# HavenOS 3.1 — Phase D report

**Scope:** builder prompt §D, in order: home search widget as a
Glass1 card, Dock Search/Library tiles (A7), A4/A5/A6 verification,
mascot-overlap confirmation. Baseline: the tree including the
coordination fix and Phases A–C (pre-D snapshot at
`~/workspace/havenos-pre-3.1-phase-D/`).

**Rules honored:** no touch to `~/workspace/havenos-github/`, no
publish/upload/signing change, no token use. Version stays 3.0/vc12
(3.1.0/vc13 is the final step's job, not this phase's). Ponytail
full level; fix-size rule; debug pass after each part; ECC skills
read before their steps (design-system, kotlin-patterns,
kotlin-testing, tdd-workflow, verification-loop) plus
local-verification-before-completion. Tests written with the code
where there is logic to test — Phase D adds no pure logic (see
below), so no new test file; recorded honestly, not hidden. No
device/emulator: visual behavior is built-but-not-proven and listed
in `docs/UNVERIFIED.md` + `docs/DEVICE_CHECKLIST.md` (D-1…D-5).

## D1 — Home search widget becomes a Glass1 card

- The widget is `QuickSearchWidget` (`ui/WidgetReal.kt`, sole caller
  `Widgets.kt` ItemKind.QUICK_SEARCH). Its outer frame already was
  the home Glass1 realization: `WidgetFrame` is `GlassSurface` at
  28dp radius with 16dp padding — the same frame the Today cards
  use (`TodayApiCards` ApiCardFrame). What broke the match was the
  inner pill: a 50-radius Box with flat surface at 55% alpha, plus
  a "⌕" text glyph as the icon.
- Change (`ui/WidgetReal.kt` only, +2 imports, +9/−8 body lines):
  the inner pill Box is deleted and the row sits directly in the
  frame, so the widget presents as ONE Glass1 card, not a card
  containing a pill; the "⌕" glyph is replaced with
  `HavenIconImage(HavenIcon.Search, null, muted, 20dp)` per the
  Phase C drawer-search convention (null description: the adjacent
  "Search apps, settings…" text already labels the row for
  TalkBack). Copy, text style, and widget footprint are unchanged.
- Deliberate non-use of `GlassCard`: tint-only `glass()` loses blur
  over wallpaper (Phase C C3 adaptation), so on a home surface the
  Phase B Glass1 component is `GlassSurface`, not `GlassCard`. A
  `GlassCard` here would have made the widget flatter than the
  Today cards, not matching them.
- Side effect, behavior-compatible: the tap target is now the whole
  widget tile (clickable moved to the `WidgetFrame` modifier);
  before, the 16dp padding ring around the pill was not tappable.
  Edit-mode behavior is unchanged (no click in edit mode; the host
  owns drag).

## D2 — Dock Search and Library tiles (A7): verified, NOT rebuilt

- Still in place in both dock layouts (`ui/Dock.kt`):
  `DockActionButton` with contentDescription "Search"/"Library",
  vector drawables `ic_search`/`ic_apps` (no glyphs), 48dp target
  (`DOCK_ICON_DP = 48`), 14dp tile with `palette.surface` + 1dp
  `glassBorder` — the same tile the app icons beside it use
  (`DockAppIcon`). No regression from Phase C (Phase C never
  touched `DockActionButton`; its icon work was glyph-to-vector
  replacement, and these buttons already use vectors). No change
  made. The stale "Phase B's Haven Glass tile replaces these
  literals" comment in `DockActionButton` is left untouched —
  comment-only, out of Phase D scope.

## D3 — A4, A5, A6 re-verified against the current tree (no regressions)

- A4 (page width + clip): `HorizontalPager` uses the default Fill
  page size, so each page equals the pager viewport width (the
  pager itself fills the ScrollArea, 0.70f beside the Today rail
  when the rail is on); every page Box is `fillMaxSize` with
  `clipToBounds` (`ui/PagedHome.kt`), which is what stops the
  parallax shift from leaking a neighbour sliver at rest. Phase C
  touched PagedHome only for the icon menu and glyph fixes.
- A5 (glance rule): `glanceVisible(todayVisible) = !todayVisible`
  (pure, `ui/PagedHome.kt`) gates the single `GlanceRow`, which
  lives only in the grid-page branch — the Today-page branch has
  no glance; `Home.kt` passes the raw toggle (rail and strip both
  count as Today visible). Each page's glance is inside its
  clipped page Box, so a neighbour page's glance cannot leak.
- A6 (rail inset): `railBottomInsetDp` (pure, `ui/TodayRail.kt`)
  returns dockHeight + 16 for bottom docks, 0 otherwise; the host
  passes `dockThicknessFor(sizeClass, Bottom) + 16dp` for bottom
  docks (`ui/Home.kt`) and the rail applies it as bottom padding
  INSIDE its `verticalScroll` Column, so the padding extends the
  scrollable extent and the last card scrolls fully clear. The
  extra 16dp in the rail's base padding is the rail's standard
  bottom spacing, present with or without a dock — not an A6
  deviation.

## D4 — Mascot overlap: no new overlap created

- Phase D changes no mascot code and no widget footprint: the
  search widget keeps its cells, size, and position; only its inner
  surface changes. The ledge-graph guarantee (mascot never sits on
  an icon or card content) remains Phase E's job; nothing here
  makes it harder (no new surface, no moved content).

## Untouched, as required

- No home-grid, icon-size, spacing, Today-structure, or glass-look
  file was touched. No §D change required touching them, so no
  piece had to be stopped. The Quick tune Mode System option from
  Phase C was not touched (parent may overrule; not Phase D's
  business). The C5 group boundaries were not revisited.

## Files changed (diff vs pre-D snapshot, main tree)

1 file, +11/−8 lines (plus build outputs):

- `ui/WidgetReal.kt` +11/−8 (2 imports; QuickSearchWidget body:
  pill removed, Search vector icon, clickable moved to the frame)

Docs updated (short, factual):

- `docs/COVERAGE.md` — new Phase D section (change, A7/A4/A5/A6
  verification, mascot note, BLOCKED: none, verification).
- `docs/DEVICE_CHECKLIST.md` — new Phase D checks D-1…D-5.
- `docs/UNVERIFIED.md` — new Phase D section.

## Tests

- None added: the change adds no pure logic (a composable layout
  swap + icon swap inside one existing function), so per Ponytail
  there is nothing unit-testable to add; the existing A4/A5/A6
  pure functions (`pageWidth`/`glanceVisible`/`railBottomInsetDp`)
  are covered by Phase A tests. No test was run: the sandbox test
  executor is broken (pre-existing) and `SettingsShellTest` still
  has its 10 pre-existing compile errors. Nothing about test
  status is claimed beyond this paragraph.

## Scans run (main Kotlin)

- Rendered glyph "⌕": 0 hits after the change (Phase C's glyph
  list did not include ⌕, so this widget was the last
  rendered-glyph holdout).
- Secrets grep over the changed file: no secrets, keys, tokens, or
  credentials.

## Verification

- Verification build: BUILD SUCCESSFUL in 10m 29s (`build.sh`).
- APK `app/build/outputs/apk/release/app-release.apk`, 2,731,256
  bytes (same size as the Phase C final by coincidence; md5
  differs), md5 `15460f553aadfa10c1ef522cabbea26f`, aapt package
  `app.havenos.launcher` versionCode 12 versionName 3.0 minSdk 26
  targetSdk 36, apksigner CN=HavenOS SHA-256
  `53bff2a09aedb516a8ee6193234a7aaf5d04ce901fb8931b799191b9845354a8`
  (keystore match). Verification build only — NOT delivered
  anywhere, nothing published or uploaded.
- Diff review vs the pre-D snapshot: only `ui/WidgetReal.kt`
  differs in source; the diff is exactly the intended change.
- No dependency added. No Room migration. Signing, version
  (3.0/vc12), and `~/workspace/havenos-github/` untouched.

## Deviations and adaptations (all deliberate, none silent)

1. `GlassCard` deliberately NOT used for the search widget: on a
   home surface the Phase B Glass1 realization is `GlassSurface`
   (blur mechanism); tint-only `glass()` would lose blur over
   wallpaper and make the widget flatter than the Today cards
   (Phase C C3 adaptation). The widget's frame already is that
   realization; Phase D removed the inner pill that broke the
   match.
2. Tap target widened to the full tile (see D1) — behavior-
   compatible, noted, not a redesign.
3. No new test file (see Tests above) — the change has no pure
   logic to unit-test.

## Unverified (device-only, see docs/UNVERIFIED.md + DEVICE_CHECKLIST.md D-1…D-5)

- Search widget as a single Glass1 card over real wallpapers,
  Search vector at 20dp, full-tile tap target.
- A4/A5/A6 and A7 Pad re-checks (D-3, D-4): code review is not a
  device check.
- The C4 quick-customize Widgets fix Pad check (C-8) is still
  owed from Phase C and is unaffected by Phase D.

## What the parent owns next

- APK delivery to Aurelius (direct download link + exact byte size
  + md5, per standing rule) — this session delivered nothing.
- Phase E (pixel mascot rebuild) on the same rules, or Pad checks
  D-1…D-5 first if he prefers verification before E.
- Decisions still open from Phase C: Quick tune Mode System option
  (overrule or keep), C5 toolbar group boundaries, and whether the
  3.1 builds swap into 3.0 or wait for the 3.1.0/vc13 final step.
