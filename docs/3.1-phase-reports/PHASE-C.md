# HavenOS 3.1 — Phase C report

**Scope:** builder prompt §C, items C1–C8 in order. Applies the Phase B
Haven Glass system to every non-home surface. Baseline: the tree
including the coordination fix and Phase B (pre-C snapshot at
`~/workspace/havenos-pre-3.1-phase-C/`).

**Rules honored:** no touch to `~/workspace/havenos-github/`, no
publish/upload/signing change, no token use. Version stays 3.0/vc12
(3.1.0/vc13 is the final step's job, not this phase's). Ponytail full
level; fix-size rule; debug pass after each part; ECC skills read
before their steps (design-system, kotlin-patterns,
android-clean-architecture, kotlin-testing, tdd-workflow,
verification-loop) plus local-verification-before-completion.
Tests written with the code, pure functions first — recorded as
written-but-not-run (sandbox executor broken, pre-existing;
SettingsShellTest has 10 pre-existing compile errors). No
device/emulator: visual/gesture behavior is built-but-not-proven and
listed in `docs/UNVERIFIED.md` + `docs/DEVICE_CHECKLIST.md` (C-1…C-14).

## C1 — Settings (verified green 2026-10-30, BUILD SUCCESSFUL 10m 19s)

- Panel: legibility scrim + Glass2 tint + hairline (bare Glass2 tint
  alone is not legible over the live home without window blur, so the
  Phase B §B2 scrim rule is applied). Material setting no longer
  changes the panel surface; it still drives home glass tiers.
  Old `panelBackground()` deleted; DockedPanel/SheetPanel params
  removed.
- Rail 260dp tablet, rows 48dp, 24dp outline icon in icon tile;
  selected row = accent-soft pill with accent icon + label; labels
  wrap to two lines via HavenLongPressText, never truncate. Title
  bar: HavenIcon Back vector + one 28sp title (HavenType.title).
  Breadcrumb already depth ≥3 from Phase A A8 — no repeated
  "Settings › Settings" crumb, no change needed.
- Before pill: Eye vector icon + "Before" label, TalkBack
  description "Hold to see your home before changes"; hold behavior
  unchanged. ChangesChip left as-is (already the second pill).
- Hub screen: filled glass search field (copy unchanged); all four
  needs-attention banners → HavenBanner; Changes banner → GlassCard
  (GLASS1) because it needs two actions (Review + Revert session)
  and HavenBanner supports one. Hero → new `MiniLookPreview`
  (tokens-only live miniature: mini 3×2 grid in the current icon
  shape with one accent tile, dock pill in accentContainer, "Minimal"
  title, mode · material · grid line). No fallback to the Minimal
  card was needed. Quick tune → one GroupedSection (Icon size,
  Grid with Density/4×6/5×6/Custom incl. both-orientations write,
  Mode, Dock position, Labels). Hub list → one GroupedSection
  ("All settings") with HavenNavRow per hub.
- Quick tune Mode gained a System option (4 options: Light/Dark/
  Auto/System) — DELIBERATE FIX: HavenSegmented always shows a
  selection, so the old no-selection state for System mode would
  have lied. Selected mapping: darkMode 1→0, 2→1, 3→2, else→3;
  onSelect index 3→setDarkMode(0), else index+1. Parent may overrule
  this back to a 3-option control with a no-selection state.
- Shared hub rows restyled: HubRow (wrap-to-two-lines, 56dp min),
  HubSwitchRow (HavenSwitch, keeps On/Off text + onSpotlight),
  HubSegmented (delegates to HavenSegmented), HubSliderRow
  (HavenSlider, keeps live onPreview + onCommit on release with the
  dragged value), HubMore (rotated ChevronRight vector, no ⌄/›),
  HubHexDialog (Glass3 dialog: title + field + Cancel text + one
  filled accent "Use colour"). HubInfoButton → HavenHelp (old
  AlertDialog popover deleted). ShellRow/ShellSwitchRow/
  ShellSegmentedControl (thin HavenSegmented wrapper)/
  ShellExpander (rotated ChevronRight)/ShellSkeletonRow (HavenSkeleton)
  restyled. ShellSwitchRow gained optional `status: String?`
  rendered as a muted second line (for the APIs hub).
- Privacy/DeviceData inline help expansions (ⓘ button + inline
  block) → HavenHelp popovers, same help text. DevicePermRow:
  HavenIconTile, title + purpose, HavenHelp, status chip
  (accentContainer + accent text when granted, onSurface 8% + muted
  when not; "Allowed"/"Not allowed"), Manage/Settings/Allow kept;
  ●/○ dots deleted. DrawerSearchHub HelpButton → HavenHelp.
  HomeDockHub default-page picker ●/○ → RadioOn/RadioOff vectors.
- ApiHub: diagnostics loaded once (`apiStore.allDiagnostics()`,
  try/catch → emptyMap); new pure `apiHubStatusLine(enabled,
  needsKey, diag)`: off → "Off"; Crypto on + blank key → "Needs
  key"; diag null → null; SUCCESS → `lastUpdatedLabel`; OFFLINE →
  "Offline"; RATE_LIMITED → "Busy"; other A2 classes →
  "Couldn't load". New pure `githubUsernameError(name)`: blank
  allowed; else alphanumerics + hyphens, ≤39 chars, no leading/
  trailing hyphen. Key fields restructured into a GroupedSection
  ("Keys and account") with Paste (LocalClipboardManager)/
  Show-Hide/Save + validation error lines, no inner card (avoids
  double surfaces). Toggles keep ShellSwitchRow cards.
- GesturesMotionHub: material3 Switch → HavenSwitch; Test buttons
  untouched (real dispatcher); device diagram already token-only
  (no hardcoded Color(0x…) or purple literals found by grep).
- DeviceDataHub: default-launcher banner → HavenBanner (Home icon,
  two-line text, "Make default" → onSpotlight + requestHomeRole);
  else-branch DeviceValueRow kept. Diagnostics keeps A2 rows; the
  A13 restore row lives in Gestures and Motion, not here.
- Look hub: verified present per Phase B (accent Source + swatch
  grid with names + selected ring, strength, mode, material, label
  mode, icon shape previews, wallpaper source/style) — only the
  shared-row restyle applied. The Look hub's own hero card (accent
  preview surface) left in place; C1's hero requirement is the hub
  SCREEN hero (MiniLookPreview).
- HomeDockHub: Columns + Rows steppers added (GridStepperRow:
  48dp Minus/Plus vector buttons, 28dp centered value, coerced
  3..12) after the Orientation segmented, before the
  "Follows density" caption. Follow-density button: horizontal
  scroll + 148dp minimum width + no-wrap label, so it can never
  wrap letter-by-letter. Shrink preview moved out of the Grid HubRow
  into its own GroupedSection ("Grid change preview") with the dots
  row (max 12), a one-sentence summary (new pure
  `moveSummarySentence`: "0 items will move." / "1 item will move
  to Page 2 (1)." / "3 items will move to Page 2 (2) and Page 3
  (1)."; >8 → count only), and the expandable destination list
  (existing `visibleDestinationEntries` + `MoveDestinationsText`,
  ≤8 all shown, >8 first 8 + "+M more"). Density itself is a 0..100
  slider, not chips — the C1 density item is the Follows-density
  control. Both GridPickerRow dialogs → Glass3 custom dialogs
  matching the C4 anatomy (HavenDialog cannot host expandable
  lists); A9 Keep/Reset semantics preserved.
- Hub loading/empty/error states: DeviceDataHub import error dialog
  + diagnostics empty text ("No API results recorded yet.");
  ApiHub degrades to missing status lines; SettingsPanel "Screen
  not available" state. Other hubs read local SettingsState/
  DataStore (always present, synchronous defaults) — no
  missing-data path exists, so no state was invented.
- Search: "No results" → "No settings match" (closest-hub
  suggestion kept); SettingsSearchField = filled glass field
  (Glass2, control shape, Search vector, copy unchanged);
  InlineSwitch → HavenSwitch; InlineSlider → HavenSlider;
  SearchResultRow = GlassCard (GLASS1) with two-line titles.
- C1 compile fixes (3 builds): DeviceDataHub missing imports (Box,
  width, clip, RoundedCornerShape, Color, background); HomeDockHub
  duplicate HavenIcon imports + wrong LocalAccentRoles package
  (ui.glass → theme) + AlertDialog import re-added for the then-
  remaining Weather dialog; HubScreen `app` shadowing the package
  (import GlassLevel, use GlassLevel.GLASS1) + invalid mixed
  dp/percent RoundedCornerShape in the teardrop mapping (fixed to
  all-dp); unused Icon/CircleShape imports removed.
- C1 verification: BUILD SUCCESSFUL 10m 19s; APK 2,714,872 bytes
  (equals the Phase B size by coincidence; md5 differs), md5
  `9256e213827b8abb36b792d92b3f23a5`, aapt vc12/3.0/minSdk 26/
  targetSdk 36, apksigner havenos cert match.

## C2 — Drawer, Library, Search (verified green, BUILD SUCCESSFUL 11m 12s)

- Sheet: scrim + Glass2 tint layered directly (no glass() border —
  the sheet is edge-to-edge). Sheet-dark computed from palette.bg
  luminance (same rule as panelGlass). `.imePadding()` on the inner
  Column: search field stays visible, results resize. Sticky
  list-header wash deliberately left unchanged (a GLASS1 tint would
  not hide scrolling content under a sticky header).
- Toolbar row: search row holds only field + Close (the "⋮"
  TextButton removed from it); new single compact row (hidden while
  searching) = HavenSegmented (All/Categories/List; Categories
  only when categoriesContent != null) + 48dp sort IconButton
  (Sort vector, cycles A–Z/Most used/Recent, same cycleSort logic)
  + 48dp menu IconButton (Menu vector, same onOpenSettings("App
  drawer") action — the pre-existing drawer-settings affordance,
  not a new dropdown). Custom LibrarySegmentedControl deleted.
- LibrarySearchField: GlassSurface → Box + glass(GLASS2, 50),
  56dp; Search icon → HavenIconImage Search (20dp); Clear stays
  HavenIcon.Close. SearchCard shares this field, so its field also
  became Glass2; SearchCard's own card stays GlassSurface (its
  card, not the drawer sheet). Keyboard Search action already
  correct (ImeAction.Search + onSearch → onSubmit; Enter launches
  the top ranked app) — verified, unchanged.
- A-Z rail: strip untouched (48dp wide, drag + haptic + TalkBack
  adjustable); letter bubble → Box + glass(GLASS3, CircleShape)
  72dp, same offset/position logic.
- SectionHeader → HavenType.sectionLabel, accent. Match
  highlighting: new pure `matchSpans(text, query)` in
  LibrarySearch.kt (case-insensitive, non-overlapping, in order;
  empty when blank/never occurs; file stays framework-free) + new
  private `highlightedLabel` in Drawer.kt (accent + SemiBold spans).
  LibraryListRow gained optional `highlight` (null in the plain
  List view); SearchResultsList passes query for app rows; contact
  name rows highlighted. Settings rows (title + hub, no icon) and
  contact number rows keep text-only rows (no icons to tile) — a
  reading, not a gap. Ranking/sources/toggles unchanged; sections
  (Apps, Calculator, Settings, Actions, Contacts, Events, Currency/
  Dictionary via searchExtrasItems, Web fallback via the user's own
  engine intent, no web request by the app) kept.
- Empty state: the "No app, setting or contact matches" note →
  SAD MascotFigure 64dp (character mirrors MascotHub: 1 → MILA else
  MILO) + "No matches" headline + same note copy; Web fallback
  below still renders.
- CategoriesView line-474 ●/○ → RadioOn/RadioOff vectors (20dp,
  accent when current, onSurface 50% otherwise).
- C2 verification: BUILD SUCCESSFUL 11m 12s; APK 2,731,256 bytes,
  md5 `52f7aefa865ef3dfae627b3af7395612`, timestamp 2026-10-30
  13:23. No compile errors.

## C3 — Today online cards (verified green, BUILD SUCCESSFUL 10m 56s)

- File: `ui/TodayApiCards.kt`. Frame KEEPS GlassSurface (Today is a
  home surface; GlassSurface is the sanctioned home blur mechanism
  and tint-only glass() would lose blur over wallpaper). One
  anatomy unified within it: header = small accent label (no card
  has a header icon or overflow actions — no fake slots invented);
  body; footer = "Updated HH:MM · Source" on success paths only
  (`ApiStatusLine` gained `source`; applied to Updated, Fresh, and
  the `lastUpdatedMs > 0` fallback branches). A2 error/offline/
  empty wording untouched. Sources: Quote → Quotable, Scores →
  SportScore (in addition to the always-visible "Powered by
  SportScore" attribution row, taps to sportscore.com, never a
  betting page), Crypto → CoinGecko, APOD → NASA, GitHub → GitHub.
- Loading: new private `isCardLoading` (data == null &&
  isRefreshing && failureClass == null) + `ApiCardSkeleton` (three
  HavenSkeleton bars). Each card body: data → content; else if
  loading → skeleton; else → empty state. (ApiEmptyLine's
  "Loading…" branch remains reachable for retry-with-no-data.)
- Empty/offline/error: new `ApiEmptyState(state, what, character)`
  — Row with static SAD MascotFigure 48dp (blink=false) + 12dp
  Spacer + ApiEmptyLine text (A2 wording preserved). Character
  threaded from TodayApiCards (mascotCharacter == 1 → MILA else
  MILO) into every card signature. E4 poses (Phase E) may refine
  this pose later.
- Quote: 15sp → 18sp Regular; author "— name" 13sp muted → 12sp
  muted caption, maxLines 1. Scores: chip row → HavenSegmented
  (Cricket/Football; onSelect resets showAllScores); rows at most 4
  + Show more/Show less (was take(6)); A1 row model unchanged.
  Crypto: full names → symbols (BTC/ETH, 14sp SemiBold) + price +
  24h change with ArrowUp/ArrowDown icon (16dp, muted) + signed
  percent (formatChangePct, already signed) — never colour alone;
  NaN → no arrow/pct (unchanged). Key-gated early-return branch
  (blank CoinGecko key → "A free CoinGecko Demo key is needed…" +
  "Add key" deep-link) left unchanged (an A3 state, not a
  loading/empty state). APOD: thumbnail (16dp rounded, max 320dp),
  title, Set-as-HavenOS-wallpaper (HavenOS-mode only, app private
  files, no WallpaperManager call) already per spec — only
  frame/footer/skeleton/empty anatomy applied. GitHub: repos (max
  5) + events (max 5, "Recent activity (not live)") each gained
  skeleton/empty branches; combined status (Expired > Stale >
  Fresh) unchanged.
- Currency/Dictionary are NOT Today cards (file header: they belong
  to the search worker) — C3's "same anatomy" does not apply to
  them; their presentation follows the C2 search sections. Card
  order unchanged (quote, scores, crypto, APOD, GitHub). Toggle
  gating unchanged. No new pure-function tests in C3 (isCardLoading
  is private; take(4)/Show-more is UI logic); `failureLineFor` is
  covered by existing ApiCardStatesTest.
- C3 verification: BUILD SUCCESSFUL 10m 56s; APK 2,731,256 bytes
  (same size as C2 by coincidence; md5 differs), md5
  `7ea04bf0e294fd12c681b726ed277276`, timestamp 2026-10-30 13:36.
  No compile errors.

## C4 — Menus, dialogs, bubbles (verified green, BUILD SUCCESSFUL 10m 37s)

- New shared pieces in `ui/glass/GlassComponents.kt`:
  `HavenGlassDialog(onDismiss, title, modifier, buttons, content)`
  — Dialog + Column + glass(GLASS3, dialogShape) + 24dp padding,
  20sp SemiBold title, 12dp spacer, End-aligned button Row with
  24dp top padding; `RowScope.HavenDialogFilledButton(label,
  onClick, danger=false, enabled=true)` — accent filled (error red
  when danger, white text); `HavenMenuRow(icon?, label,
  destructive=false, onClick)` — 48dp min height, 20dp icon (muted,
  or error when destructive), 15sp label (onSurface, or error when
  destructive). The `enabled` param was added when the WidgetReal2
  Weather dialog needed its `query.isNotBlank() && !searching`
  guard preserved. HavenMenu itself NOT re-surfaced (its container
  is palette.surface, the sanctioned Phase B component).
- Scope decision: "everywhere" means all non-home dialogs/menus
  PLUS all home dialogs (Home.kt confirmations, PageOverview,
  PagedHome, WidgetReal2, Widgets, EditMode Calm editor) — the
  simple ones fit HavenDialog, the list/field ones fit the Glass3
  shell with bodies preserved. Nothing was left as "home surface,
  out of scope." No dialog hosted content that could not be
  preserved — no BLOCKED item.
- Converted (bodies and actions preserved unless stated):
  GesturesMotionHub ×4 (Lock screen → HavenDialog, Bind filled;
  Move action? → HavenDialog, Move filled; Pick app search +
  60-app list + radios → HavenGlassDialog, Close text; Icon swipe
  action radios → HavenGlassDialog, Close text). DrawerSearchHub
  ×2 (Rename category field → HavenGlassDialog, Cancel + filled
  Save; Merge category + target radios → HavenGlassDialog, Close).
  HomeDockHub Weather city → HavenGlassDialog (field + Search
  button + results + Cancel; geocode via WeatherNet.geocode 5).
  DeviceDataHub ×4 (permission rationale → HavenDialog, Allow
  filled; import confirm → HavenDialog, Apply filled, snapshot +
  applyBackupJson + syncNavBarPolicy + flags preserved; import
  error → HavenDialog, OK only; open-source licenses →
  HavenDialog, Close only, Apache 2.0 / CC BY 4.0 copy preserved).
  SettingsComponents ShellColorPickerDialog (zero callers —
  converted anyway so the shell holds no material dialog if ever
  wired again; 6-hex validation preserved). Dialogs.kt
  TextFieldDialog (drawer + scroll home "Edit label"; blank
  restores platform label) → HavenGlassDialog. PreviewAids
  ShellResetOverflow: "⋮" trigger → HavenIconImage Menu;
  DropdownMenu → HavenMenu with one HavenMenuItem (Undo icon,
  "Reset this screen…", destructive, HavenMenu auto-dismisses
  before onClick); confirm → HavenDialog (Reset filled,
  danger=true). Its showSnackbar call is the API, not the UI — its
  rendering is converted via the SettingsPanel hosts.
  CategoriesView ×2 (Rename → HavenGlassDialog, Cancel + filled
  Save; Move … to… list max 320dp RadioOn/RadioOff rows →
  HavenGlassDialog, Cancel). Drawer Library menu → HavenGlassDialog
  (title = entry label, Cancel) + HavenMenuRow rows (Launch
  MediaPlay, Add to home Home, dock Layers, Edit label Gear, Edit
  icon Image, Hide app Eye, App info Info, Uninstall Close
  destructive); private MenuRow deleted. Home dock menu →
  HavenGlassDialog + HavenMenuRow rows (Launch, Remove from dock,
  Move left Back, Move right ChevronRight, Move to home, Add to
  dock / Dock is full, Edit label, Edit icon, Hide app, App info,
  Uninstall destructive); public DockMenuRow deleted (no remaining
  callers). PagedHome icon menu → HavenGlassDialog + HavenMenuRow
  rows (Edit Home, Edit label Gear, Edit icon Image, Remove Close
  NOT destructive — removal from home is undoable via the 5s
  snackbar, like Remove from dock/Hide app, whereas Uninstall is
  system-level and permanent — App info Info, Uninstall Close
  destructive); private PagerMenuRow deleted. Quick-customize bar
  (M10 Level 1, over the live home above the dock) → Box +
  glass(GLASS3, 50) pill row; TextButton items → 48dp-min Row items
  with 20dp muted icon + 13sp label (Wallpaper Image, Layout Grid,
  Widgets Apps, Dock Layers, Look Eye, Home settings Gear, Edit
  home Home); destinations/actions preserved. Snackbars:
  SettingsPanel both hosts (docked + sheet) gained a snackbar
  lambda rendering HavenSnackbar (message + action + dismiss);
  EditUndoSnackbar (5s, Doc 1 §3.7; host owns the timer, untouched
  in Home.kt) now delegates to HavenSnackbar. EditMode
  CalmEditorDialog → HavenGlassDialog (Cancel + filled Save;
  checkbox rows + 8-pick cap + copy preserved). Home.kt remaining
  dialogs: Cannot delete this page → HavenDialog (OK only);
  Remove from home → HavenDialog (Remove filled, danger=true);
  Move to page → HavenGlassDialog (Cancel; wrapping Column dropped
  — shell content is itself a ColumnScope); Restore layout →
  HavenGlassDialog wrapper-only (LazyColumn body byte-identical);
  Reset page → HavenDialog (Reset filled, danger=true); At-a-glance
  options → HavenGlassDialog (Close + Settings text buttons, in
  that order; Column opener dropped); ResizeDialog → HavenGlassDialog
  (Cancel + filled Apply); AppIconPickerDialog → HavenGlassDialog
  (Cancel). WidgetReal2 Weather city → HavenGlassDialog (Cancel +
  filled Search with the enabled guard; "City not found — check the
  name and connection." preserved). Widgets ×5: Countdown (field +
  date line + Pick date → HavenGlassDialog, Cancel + filled Save),
  New task (field → HavenGlassDialog), Note (field →
  HavenGlassDialog), Gallery photo (body + Clear-when-present +
  Cancel + filled Choose image → HavenGlassDialog), Edit stack
  (member rows + Unstack + Save → HavenGlassDialog, Cancel +
  Unstack text + filled Save with members.isNotEmpty() guard).
  PageOverview delete-page confirm → HavenDialog (Delete filled,
  danger=true).
- BUG FIX (behavior, not styling — flag prominently): the
  quick-customize bar's WIDGET_SHEET branch was
  `{ showWidgetSheet = true }` — a lambda literal as a
  statement-when branch, which did nothing, so tapping Widgets
  never opened the sheet. Fixed to `showWidgetSheet = true`.
- C4 compile fixes (2 failed builds): orphan @Composable left in
  Drawer.kt after the MenuRow deletion (stacked onto SearchZeroRow's
  annotation); an extra brace in Home.kt At-a-glance (old Column
  close survived the Column drop); a split doc/annotation in
  GlassComponents.kt (the HavenGlassDialog insertion landed between
  HavenMenu's doc comment + @Composable and its function, and a
  second stray @Composable preceded the C4 doc) — all three fixed,
  plus two extra-brace fixes in ResizeDialog/AppIconPickerDialog
  (old Column-close lines removed) and the PageOverview HavenDialog
  import added. Remaining DropdownMenus after C4: EditMode's
  Add/overflow (C5's toolbar, converted there) and HavenMenu's
  internals (by design). DatePickerDialog (platform date picker)
  stays — not a Haven dialog candidate.
- C4 verification: BUILD SUCCESSFUL 10m 37s; APK 2,731,256 bytes,
  md5 `a6787e3b9bff5c50e321ccf87f0be13d`, aapt vc12/3.0/minSdk 26/
  targetSdk 36, apksigner CN=HavenOS SHA-256 match.

## C5 — Edit toolbar + context bar (verified green, BUILD SUCCESSFUL 10m 54s)

- 56dp toolbar, reserved space, every action kept. GlassSurface
  pill kept as the blur mechanism (tint-only glass() would lose
  blur over the home); Glass3 styling = icon + label buttons,
  accent-soft pill for the active tool, hairline dividers between
  Left (Undo/Redo/Snap), Center (Add/Pages/Wallpaper/Look/Calm/
  Settings), Right (Done/overflow) groups. Group boundaries are
  this session's reading — the prompt names the groups but does not
  enumerate them.
- EditToolButton rewritten: (icon: HavenIcon, label, expanded,
  enabled, active, trailingIcon, onClick); icon 16dp + label 13sp
  when expanded, icon only when collapsed; active = clip(50) +
  accentContainer background + accent icon/text; TalkBack label
  always the full label (A9). All glyphs cleared: ↩→Undo, ↪→Redo,
  ⊞→Grid, ▦→Layers, ▣→Image, ◐→Eye, ⋮→Menu vector. Snap: Grid icon
  + "Snap to grid" + Check trailing when on + active=snapOn; the
  off-state needs no marker — absence of the accent-soft background
  IS the off state (the ○ is gone). Add + overflow menus →
  HavenMenu (same actions, same order; HavenMenu auto-dismisses
  before onClick, so no manual close needed). Accent-soft =
  Color(roles.accentContainer), matching the segmented thumb.
- Context bar: same pill, icon + label buttons via new
  EditContextButton (Resize Grid, Remove Close, Move to page
  ChevronRight, Duplicate Plus, Replace Apps, Item settings Gear,
  Select more Check, Add to dock Layers, Hide Eye, Uninstall Close
  danger-tinted). All conditionals, 48dp mins, 56dp bar, and the
  selectedName first-position rule preserved.
- Selection ring untouched (2dp accent, PagedHome lines 1007–1008).
- C5 verification: BUILD SUCCESSFUL 10m 54s; APK 2,731,256 bytes,
  md5 `f8bea666710534483c3f5e7e2ca582e0`, aapt vc12/3.0/minSdk 26/
  targetSdk 36, apksigner havenos cert match.

## C6 — Page overview, widget picker, folders, page indicator, app info (verified green, BUILD SUCCESSFUL 10m 35s)

- Page overview sheet: opaque material Surface (24dp + tonal
  elevation) → Box with scrim + glass(GLASS2, 28dp) (same layering
  as the Settings panel). Thumbnails: surfaceVariant 45% 16dp →
  glass(GLASS1, 20dp) + 20dp border (selection/default border logic
  unchanged). Delete-page confirm already converted in C4.
- Widget picker: search field + size chips already existed (field
  filters own + provider widgets; chips pick from
  allowedCellSizes/builtinWidgetSizes) — C6 restyles them: sheet
  container takes the Glass2 tint (same luminance rule as the
  drawer sheet), search field becomes a filled Glass2 field (Box +
  glass(GLASS2, 50), 56dp, Search icon, placeholder "Search
  widgets", Clear when non-blank). Previews unchanged (live widget
  renders / provider preview images).
- Folders: FolderSheet already uses GlassSurface (home blur
  mechanism over the live grid); tint-only glass() would lose blur,
  so no surface change — recorded as an adaptation, not a gap.
- Page indicator already matches spec (8dp dots + 24dp accent dash;
  Today dot stays the Phase B4 sun icon). No change.
- App info: no HavenOS app-info surface exists — the "App info"
  action opens the system app-info screen. Nothing to restyle; the
  menu row itself was converted in C4 (Info icon).
- C6 verification: BUILD SUCCESSFUL 10m 35s; APK 2,731,256 bytes,
  md5 `bf47f311ebf2e1d8f89cb6ac9d66bc73`, aapt vc12/3.0/minSdk 26/
  targetSdk 36, apksigner havenos cert match.

## C7 — Calm (verified green with C8, BUILD SUCCESSFUL 11m 1s)

- CalmMode overlay already satisfies the calmer variant (44sp
  clock, 30sp favorites, white 70% secondary text, black 62% scrim,
  no accent anywhere) and mutates no home state, so exit restores
  home exactly. No change to the overlay.
- CalmEditorScreen restyled: material Switch → HavenSwitch (×2),
  Slider → HavenSlider (×3; 15-minute snap preserved on From/To;
  drag bubble shows the formatted value: HH:MM for From/To, % for
  Dim). Accent appears ONLY for schedule state: the Schedule label
  is accent when enabled, and selected days are accent-soft pills
  (replacing "[label]" brackets). The ↑/↓ reorder glyphs are
  functional labels and stay.

## C8 — Island (verified green with C7, BUILD SUCCESSFUL 11m 1s)

- Pill keeps GlassSurface (home blur mechanism; GlassSurface takes
  no per-surface level — its tier comes from theme glass settings,
  so there is no Glass3 param to set). The Glass3 treatment is the
  hairline divider (Glass3 hairline alpha) between stacked
  activities in the expanded panel. Doc comment updated to record
  this.
- Behavior, priority, ordering, controls, and visibility rules
  (off by default; hidden in edit mode, drawer, search, Settings,
  quick panel, Calm) UNCHANGED. All colors already tokens; no
  hardcoded color found in the file.
- C7+C8 verification: BUILD SUCCESSFUL 11m 1s; APK 2,731,256 bytes,
  md5 `79d5ba312a1f7c58aee72889cee1e187`, timestamp 2026-10-30
  14:40.

## Final glyph cleanup + final build

- A final tree scan found two rendered glyphs the earlier passes
  missed: TodayRail's expand/collapse "⌃"/"⌄" → ArrowUp/ArrowDown
  vectors (16dp, muted, "Collapse"/"Expand" descriptions), and the
  drawer List-row menu "⋮" → HavenIconImage Menu (24dp, 48dp box,
  same click). Two stale ⓘ mentions in comments (PrivacyHub doc,
  HomeDockHub comment) reworded. The › crumb separator at depth ≥3
  is spec'd (prompt example "Look › Wallpaper"), not a leftover.
  Rendered-glyph grep after cleanup: 0 hits for ↩↪⊞○●ⓘ⌃⌄⋮.
- Final verification build: BUILD SUCCESSFUL 10m 55s; APK
  `app/build/outputs/apk/release/app-release.apk`, 2,731,256
  bytes, md5 `5c47c1987785b4ed6cf1ec6eec05676f`, aapt package
  `app.havenos.launcher` versionCode 12 versionName 3.0 minSdk 26
  targetSdk 36, apksigner CN=HavenOS SHA-256
  `53bff2a09aedb516a8ee6193234a7aaf5d04ce901fb8931b799191b9845354a8`
  (keystore match). Verification build only — NOT delivered
  anywhere, nothing published or uploaded.

## Files changed (diff vs pre-C snapshot, main tree)

29 files, +2,536/−2,118 lines (per-file +/− below):

- ui/glass/GlassComponents.kt +98/−1 (HavenGlassDialog,
  HavenDialogFilledButton incl. enabled param, HavenMenuRow)
- ui/glass/HavenIcons.kt +61/−0 (Undo, Redo, Grid, RadioOff,
  RadioOn, Minus, Plus, Sort, ArrowUp, ArrowDown)
- ui/settings/shell/HubScreen.kt +237/−232 (banners, hero,
  Quick tune, hub list, search empty text)
- ui/settings/hubs/HomeDockHub.kt +226/−134 (steppers,
  Follow-density fix, shrink preview section, Weather dialog)
- ui/Drawer.kt +203/−162 (sheet, toolbar row, search field,
  bubble, highlighting, empty state, menu, ⋮ fix)
- ui/EditMode.kt +184/−204 (toolbar + context bar + Calm editor
  dialog + undo snackbar)
- ui/settings/hubs/ApiHub.kt +149/−34 (status lines, key fields)
- ui/TodayApiCards.kt +124/−55 (anatomy, skeleton, empty states,
  scores, quote, crypto)
- ui/settings/hubs/LookHub.kt +119/−129 (shared rows, hex dialog)
- ui/settings/hubs/GesturesMotionHub.kt +114/−126 (4 dialogs,
  HavenSwitch)
- ui/settings/hubs/DeviceDataHub.kt +108/−161 (perm rows, banner,
  4 dialogs, help popovers)
- ui/Widgets.kt +101/−108 (5 dialogs)
- ui/Home.kt +242/−253 (dock menu, quick-customize pill incl.
  Widgets fix, 8 dialogs, At-a-glance, Resize, Icon picker)
- ui/settings/shell/SettingsComponents.kt +75/−134 (rows, switch,
  segmented, expander, skeleton, color picker dialog)
- ui/settings/shell/SettingsPanel.kt +65/−29 (panel glass, rail,
  title, snackbar hosts)
- ui/settings/shell/SettingsSearch.kt +58/−34 (glass field,
  inline controls, result rows)
- ui/HostWidgets.kt +52/−10 (picker sheet tint + glass field)
- ui/CalmEditorScreen.kt +56/−14 (switches, sliders, schedule
  accent)
- ui/settings/hubs/DrawerSearchHub.kt +43/−53 (2 dialogs, help)
- ui/WidgetReal2.kt +41/−41 (Weather city dialog)
- ui/settings/shell/PreviewAids.kt +44/−73 (Before pill, reset
  menu + confirm)
- ui/PageOverview.kt +31/−34 (Glass2 sheet, Glass1 20dp thumbs)
- ui/CategoriesView.kt +29/−22 (2 dialogs, radio vectors)
- ui/island/IslandView.kt +18/−2 (Glass3 dividers, doc)
- ui/LibrarySearch.kt +18/−0 (matchSpans)
- ui/PagedHome.kt +16/−25 (icon menu)
- ui/settings/shell/Dialogs.kt +10/−11 (TextFieldDialog)
- ui/TodayRail.kt +7/−4 (chevron vectors)
- ui/settings/hubs/PrivacyHub.kt +7/−33 (help popovers, switches)

Tests added (written, NOT run):

- `app/src/test/java/app/havenos/launcher/ui/settings/hubs/C1HubHelpersTest.kt`
  (110 lines): apiHubStatusLine (Off beats everything; Needs key
  before diagnostics; on-with-no-diagnostics → null; SUCCESS with
  current time → startsWith "Updated "; OFFLINE→"Offline",
  RATE_LIMITED→"Busy", other A2 classes→"Couldn't load");
  githubUsernameError (blank → null; octocat/Voidwalker-28/a →
  null; -lead/trail-/space/underscore/40 chars → non-null);
  moveSummarySentence (0 → "0 items will move."; singular/plural;
  3-destination " and " joining; >8 → count only);
  visibleDestinationEntries (9, collapsed → 8 + 1 more; expanded
  → 9 + 0).
- `app/src/test/java/app/havenos/launcher/ui/SearchHighlightTest.kt`
  (42 lines, test-first): matchSpans (blank → empty; "fir"/"FIR"
  in "Firefox" → 0..2; "a" in "aXaXa" → 0..0, 3..3; no match →
  empty; longer query → empty).

## Scans run (script equivalents, main Kotlin + res values)

- Emoji (U+1F300–U+1FAFF, U+2600–U+27BF, U+2B50, U+FE0F): 0 hits.
- Hex literals `Color(0x…)` outside the theme/glass/Tokens/
  Legibility allowlist: 0 hits.
- Rendered leftover glyphs (↩↪⊞○●ⓘ⌃⌄⋮): 0 hits after cleanup.
- Secrets grep over new/changed code: no secrets (no keys, tokens,
  or credentials introduced; key fields only read user-entered
  values at runtime).

## Deviations and adaptations (all deliberate, none silent)

1. Panel/overview sheets add a legibility scrim over the Glass2
   tint — bare tint is not legible over the live home without
   window blur (Phase B §B2 rule).
2. Today frames, FolderSheet, toolbar, context bar, and island keep
   GlassSurface (the home blur mechanism — tint-only glass() would
   lose blur over the home). Drawer sheet uses scrim + tint
   directly (edge-to-edge, no border).
3. HavenMenu's container stays palette.surface (Phase B component
   as designed, not re-surfaced to glass tint).
4. Quick tune Mode gained a System option (see C1) — parent may
   overrule.
5. C5 Left/Center/Right group boundaries are this session's reading
   (prompt names the groups, does not enumerate them).
6. Danger tint only for permanent/destructive actions; undoable
   removals are NOT danger-tinted (see C4).
7. Today header icon/overflow slots unused (no card has them — no
   fake slots invented); Currency/Dictionary are search rows, not
   Today cards; app-info is a system screen, not a HavenOS surface;
   Privacy AI Ledger is Phase F; Mascot hub preview box + rows are
   Phase E; DatePickerDialog stays a platform picker.
8. ShellSwatchRow / ShellColorPickerDialog deletion still deferred
   to a cleanup pass (color picker converted in C4, still zero
   callers at last check).

## Unverified (device-only, see docs/UNVERIFIED.md + DEVICE_CHECKLIST.md C-1…C-14)

- All Phase C visuals: panel/drawer/sheet legibility over real
  wallpapers, Glass2/Glass3 tint + hairline rendering on the Pad,
  hero miniature fidelity, hub transitions (no ghost frames),
  TalkBack row readings (label, value, role), font-scale 1.3 label
  wrapping, contrast at real brightness.
- The two new test files: written, NOT run (executor broken).
- The quick-customize Widgets fix: code-verified only; Pad
  behavior (sheet actually opens from the bar) is check C-8.
- Calm exit restoring home exactly, and island visibility rules:
  unchanged code paths restyled only — still owed Pad re-checks
  (C-11, C-12).

## What the parent owns next

- APK delivery to Aurelius (direct download link + exact byte size
  + md5, per standing rule) — this session delivered nothing.
- Phase D (small home updates) on the same rules, or Pad checks
  C-1…C-14 first if he prefers verification before D.
- Decisions to confirm or overrule: Quick tune Mode System option
  (C1), C5 toolbar group boundaries, and whether the Phase C build
  swaps into 3.0 or waits for the 3.1.0/vc13 final step.
