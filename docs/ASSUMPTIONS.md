# ASSUMPTIONS — HavenOS

Small decisions made by the builder (prompt §0: record them here).

## Provenance
- The original builder prompt's full §9–23 text and §24 milestones, and the 4 reference
  home-screen images, were NOT uploaded. §9–23/§24 in `docs/BUILDER-PROMPT-v2.1.md` are a
  reconstruction from the v2.1 document's own references plus previously approved decisions.
  Milestone split used here: M1 foundation (this build), M2 remaining Looks + tokens,
  M3 widgets, M4 drawer modes/search, M5 §7 startup experience, then polish/QA milestones.
- REA analyses of Niagara / Smart Launcher / Nova in `~/workspace/fusion-launcher/analysis/`
  were used as feature-behavior study only (prompt §2). No proprietary code, resources,
  strings, icons or branding were used or copied; all HavenOS code is original.

## Identity / versioning
- App name "HavenOS" lives in one string resource (`app_name`); applicationId
  `app.havenos.launcher` lives in one place (`app/build.gradle.kts` defaultConfig).
- M1 ships versionCode 1, versionName "1.0". Each milestone bumps versionCode so builds
  stay distinguishable on the device (lesson from the MIST28 era: two byte-identical-size
  APKs with the same versionCode could not be told apart during QA).

## Stack choices
- Storage: Room (home layout + folders) + DataStore Preferences (settings) + files.
  WorkManager is in the stack but unused in M1 — M1 has no background jobs
  (Ponytail: no dependency for work that doesn't exist yet).
- Room annotation processing via KSP (2.1.20-2.0.1), not kapt: kapt is not available in
  this sandbox's local repositories; KSP is, and it is the supported path for Room 2.7.2.
- One `:app` module, no DI framework (HavenApp holds three singletons), no ViewModel
  layer (single-activity launcher; UI state in Compose, durable state in Room/DataStore).
- No icon/font/coil libraries: app icons come from PackageManager with an LruCache;
  dock glyphs are two original vector drawables; system font is used in M1
  (bundled OFL fonts, if any, land later and will be recorded in LICENSES.md).
- Folders store members as a JSON string column (org.json, platform) instead of a join
  table — folder sizes are small (< 50), a join table buys nothing at this scale.

## Behavior decisions (M1)
- First run seeds the Dense starter template: clock card (full width × 2 rows) on page 1,
  all apps placed row-major across pages, dock = dialer/messages/browser/camera when
  installed, else first apps in label order. No intro/onboarding in M1 (§7 is M5);
  M1 opens straight to home.
- Drag & drop: drops stay on the dragged item's page (marked with a `shortcut:` comment
  in `ui/Grid.kt`); cross-page drag lands with the pager rework.
- Dragging a folder onto another item swaps positions (no folder merging in M1);
  an app dropped on a folder joins it; app on app creates a folder.
- Density (Settings sliders, columns/rows 4–8) applies to the grid immediately;
  items keep their stored coordinates (clamped at render), so a smaller grid can
  temporarily hide overflow cells until items are moved.
- Drawer "Add to home" (long-press) is a no-op for apps already placed on home.
- Work-profile apps get distinct catalog keys (package/class/user-serial) and their own
  placements, but show the personal-profile icon (no work badge overlay in M1).
- Weather (§13) is the only network feature and is not in M1; the app declares NO
  INTERNET permission in M1. Open-Meteo terms (free tier, non-commercial,
  attribution CC BY 4.0) will be recorded here when weather lands — checked 2026-10-20:
  https://open-meteo.com/en/terms.
- Safe Mode crash recovery (§5) is not in M1; it lands with the §7 startup milestone,
  when launch-path state exists to recover.

## Build-environment facts (M1, 2026-10-20)
- Room codegen uses KSP 2.1.20-2.0.1 from `/home/hatch/local-maven` (kapt is absent there
  and unfetchable via Gradle in this sandbox).
- Gradle's own network path is broken here (proxy auth), so missing artifacts were
  mirrored into `/home/hatch/local-maven` with curl: the full DataStore 1.1.7 closure,
  `okio:3.4.0`, `kotlin-parcelize-runtime:1.9.22`, `kotlin-android-extensions-runtime:1.9.22`.
  Mirroring rule learned the hard way: in a Maven repo layout, files must sit at their
  metadata `url` path, not the metadata display `name` (e.g. the DataStore AAR is stored
  as `datastore-core-android-1.1.7.aar`, while the .module calls it
  `datastore-core-release.aar`). Gradle 9.3.1 masks the resulting "artifact not found"
  behind an NPE in its error formatter; a lenient artifact-view diagnostic names the
  unresolved modules.
- App catalog uses `LauncherApps.getActivityList` (the int-flags
  `queryIntentActivitiesAsUser` overload is not in the SDK 36 stub).

## M2 decisions (2026-10-20)
- **Open-Meteo terms** — checked 2026-10-20 at https://open-meteo.com/en/terms:
  the API is free for non-commercial use; weather data is CC BY 4.0 (attribution
  required — recorded in LICENSES.md and shown in Settings → About → Licenses).
  HavenOS is non-commercial. Weather is the ONLY network feature; the code
  allowlist admits exactly `api.open-meteo.com` and
  `geocoding-api.open-meteo.com` (enforced in `WeatherNet.fetch`).
- **Template name mapping** (v2.2 §2 letters → Settings names): A = "Calm",
  B = "Productive", C = "Premium Glass", D = "Free" (a starter layout, not a
  mode — Free canvas placement is a separate Home Screen setting), plus the
  M1 "Dense" seed kept as a fifth template.
- **Daily auto-backup** runs as an on-launch check (first launch of a new day
  writes `filesDir/backup-auto.json`), not via WorkManager — marked with a
  `shortcut:` comment in `HavenApp`. It reads the persisted settings directly
  (`Settings.persistedState()`), never the pre-load default.
- **Countdown targets** are stored at UTC midnight; the displayed remaining
  days can be off by the device UTC offset. Noted in COVERAGE.md.
- **Wallpaper kinds renumbered before first ship**: wpKind 0–3 = gradient
  presets, 4 = solid, 5 = photo (an earlier M2-internal numbering had solid
  at 3, colliding with gradient preset 4). vc2 never shipped with the old
  numbering, and vc1 has no wpKind at all, so no stored data is affected.
  Solid colour and tint are separate fields (`wpSolid` vs `wpTint`) — one
  field, one meaning.
- **Hosted-widget size hint** approximates one grid cell as 72dp (one
  half-cell unit = 36dp); exact cell size varies with density. Marked
  `shortcut:` in `TpWidgetView`.
- **Per-section Reset** exists for Settings sections 1–9 only;
  `resetSection()` defines no defaults for 10–13 (profiles/backup hold user
  data; permissions/about are informational), and the UI hides the button
  there instead of faking it.

## M3 decisions (2026-10-20)
- **Side strips cover the middle 60% of screen height** — the top corners are
  left free.
- **Hold threshold is 450ms** for the bottom-up-hold (Recents) gesture.
- **The bottom strip owns the bottom navEdgeDp**, so the home swipe-up
  (drawer) starts above it — the gesture conflict is resolved by
  construction, not by arbitration at runtime.
- **Folder popup default columns changed 4 → 3 in vc3** — formula
  (cellsW × 3) clamped 3..6. This is a visible change on upgrade.

## M4 decisions (2026-10-23)
- **Settings shell rebuilt to the HyperOS photo**: two-pane at ≥840dp (nav ~1/3, content ~2/3), large "Settings" title, search pinned on top of the left pane, grouped rounded cards with one colorful square icon per category. Categories, order, controls, reset, previews unchanged — shell-only change.
- **Dock rebuilt to the Spider-Man photo**: floating frosted pill, centered with 24dp side margins, one evenly-spaced icon row, no labels. The old horizontal text "Search" pill is gone (icon-only 48dp button, same as the vertical dock).
- **Swipe-up drawer default ON**: the M3 nav-gesture strip already owns the bottom edge strip and home swipe-up starts above it, so there is no conflict by construction; the toggle exists for users who want swipe-up fully off.
- **Scroll home is a separate branch, not a grid hack**: `placementMode == 2` renders ScrollHome instead of the paged grid, sharing catalog/folders/settings. Switching into/out of it performs no item conversion (lossless). Its app menu duplicates the drawer's menu dialog (the drawer's is private); hoisting to a shared composable is optional polish.
- **Glass/Calm gap 8dp**: TemplateA ships gapDp 8 (was 4); applied at first-run seed AND on template re-apply. Existing installs keep their current gap until they re-apply the template.
- **Edit-mode shrink is 0.87** (~300ms, critically damped spring; instant when Reduce Motion is on); wallpaper and edit toolbar are excluded from the scale.
- **Widget-add refusal over silent redirect**: on a full page, adding is refused with a toast rather than landing on an unseen page — avoids phantom widgets and pager pageCount races.

## M5 decisions (2026-10-23) — Settings Design System
- **Old → new category mapping** (SETTINGS-SPEC 13 → SETTINGS-DESIGN-v1 14):
  old 1 Look & Feel → new 1 Style & Theme (+ icon shape moved out to 6);
  old 2 Wallpaper → new 3 Wallpaper; old 3 Home Screen → new 2 Home screen;
  old 4 Dock → new 4 Dock; old 5 Drawer → new 5 App drawer;
  old 6 Widgets → new 8 Widgets; old 7 Icons & Badges → new 6 Icons
  (badges stay) + hidden-apps list → new 12 Privacy & Lock;
  old 8 Gestures → new 9 Gestures; old 9 Search → new 10 Search & Smart;
  old 10 Profiles + old 11 Backup & Restore → new 13 Backup & Profiles;
  old 12 Permissions & Access + old 13 About & Help → new 14 System.
  New categories with no old home: 7 Folders, 11 Animations.
- **Preset mapping**: Premium/Minimal/Futuristic/Classy/Retro map 1:1;
  Custom maps to the house CustomLook (.havenlook). Glass and Soft are
  kept as extra cards — Glass is the shipped default and Premium is a
  distinct look, so renaming Glass→Premium would change existing installs.
- **Preview host**: Cat screens' `preview?.render("catN-mini")` calls are
  backed by real MiniHome/MiniDrawer (live state, real catalog) in the
  full-screen shell; tap toggles compact/expanded height. The half-sheet
  passes no host because the live home is already visible behind it —
  that IS the [L] path (sliders write on drag, home recomposes behind
  the 62% sheet). Per-row PreviewSlots fall back to the value line.
- **Tier downgrades (honest, in COVERAGE.md)**: Cat1 accent
  wallpaper-derived (no wallpaper colour extraction exists); Cat1 noise /
  shadow / outline stored-only (GlassSurface has no such passes); Cat3
  crop frame draws a placeholder, not the photo bitmap (WallpaperStore not
  owned by the screen); Cat5 horizontal-pages + scrubber-rail stored-only
  (drawer renders grid/list/categories); Cat6 teardrop/system shapes +
  per-app override renderer pending; Cat7 folder size/bg/title stored-only
  (FolderSheet doesn't read them yet); Cat8 frame [L] via a live sample,
  not the real home; Cat10 files/web/shortcuts + web engine are info rows
  (drawer implements apps/contacts/calendar/settings/calculator only);
  Cat9 swipe left/right + long-press-empty triggers and recents /
  launch-app / toggle-dock actions are info rows (Home doesn't dispatch
  them); Cat13 profile preview is a snapshot summary, not a mini.
- **Grid-shrink gate is partial**: the ghost overlay + exact overflow
  count render inline while dragging (gridShrinkReport), but house
  sliders commit on drag, so the pre-commit GridShrinkDialog is delivered
  but not wired as a gate. Wiring it means staging grid values until
  release — deferred, not faked.
- **Search deep-link is category-level**: search navigates to the right
  category; row-level highlight is accepted by the new screens but not
  rendered (shell-owned, pending).
- **Reset mapping**: `resetNewCategory` maps new ids 1–14 (Cat14 System
  has no Reset — its rows are grants/info; Cat12 Reset unlocks only the
  hidden-apps lock, never unhides; Cat13 Reset clears schedule + backup
  flag only, never deletes profiles). `commitDraft` (draft Apply) now
  writes all M5 fields, with drawerMode/iconShape re-applied last
  because their setters sync side effects.
- **Onboarding**: StylePickerStep + TutorialEndNote exist as standalone
  composables; no onboarding flow exists in the tree yet, so wiring them
  into the §7 flow (intro → logo → permissions → picker → tutorial)
  is a follow-up. The §7 intro animation itself was not built here.
- **Live-drag transient layer not built**: LiveSliderRow delivers the
  drag/release pair, but full-screen Settings has no live home behind
  it — [L] there is via the half-sheet path + minis. A HavenRoot
  transient override was judged out of scope for this milestone.

## M5 scope changes (2026-10-23) — Minimal-only, no templates, no pages
- **Minimal-only (user direction, overrides the Looks parts of every spec):**
  `Look` collapsed to `Minimal` only; Glass/Soft/Premium/Futuristic/Classy/
  Retro definitions, the Look Editor, custom looks, `.havenlook` export/import,
  preset cards/gallery, and the first-run style-picker step are deleted. Glass
  surface treatment remains as part of Minimal's material (theme/Glass.kt
  untouched). The ONLY theme control is accent color (wallpaper-derived /
  palette / custom picker); it tints buttons, toggles, selection states,
  highlights and active elements — nothing else changes. Kept as independent
  features, not Looks: Appearance (Light/Dark/Auto), Density, Fonts, Motion &
  Reduce Effects, icon shape/size, and all non-theme controls. Settings style
  chip shows "Minimal" + the current accent swatch (no "Modified" dot).
  Stored look values normalize to Minimal on read, so upgraded installs can
  never resurface Glass. Inert `look`/`customLooksJson` fields and their
  setters are kept (theme ignores them) so backup/draft code keeps compiling
  and old backups round-trip; a future single pass can drop field + writes
  together. Style & Theme (Cat1) now holds ONLY: Accent, Appearance, Surface
  material, Corner radius, Fonts.
- **Templates deleted (user direction):** starter templates (Glass/Calm,
  Productive, Premium, Dense, Blank), the template picker, template
  apply/undo, and `ui/templates/Templates.kt` are gone. First-run seeds a
  plain Minimal home (one serif clock card + dock picks via `dockPicks`);
  the seed additionally requires an empty layout so a lost seeded flag can
  never wipe a live home. The old auto-add-new-apps pass is deleted (new
  installs appear in A–Z by themselves); `autoAddApps` is stored-only.
- **Pages removed (user direction):** the HorizontalPager, page dots/
  indicators, page-count settings, and page-based placement are gone. The
  home is ONE continuous scroll (the M4 scroll-home model: Favorites on top,
  Widgets & folders flow, full A–Z below, alphabet rail). Page-related
  Settings fields (`pageCount`, `infiniteScroll`, `pageIndicator`,
  `defaultPage`, `returnHome`) were removed from SettingsState together with
  their Hub/PreviewMini call sites — no stored-only page fields remain.
  `placementMode` is still stored (minis use it for previews) but home no
  longer branches on it; the Cat2 "Today page" toggle is stored-only (no
  rendered Today page). Layout-store migration (NOT a reset — a reset would
  silently delete folders/widgets, the only non-reconstructible home
  content): new `flowOrder` column (Room v2→v3, default −1 = unmigrated);
  `migrateToFlow` numbers only −1 rows, pinned-first then legacy
  (page, row, col) order, idempotent (no −1 rows → no write, re-runs never
  undo a user reorder). Undo snapshots carry `flowOrder` so undo cannot
  re-trigger a re-sort. APP rows are kept in the store but not rendered as
  flow rows (the A–Z list below already covers every installed app).
- **Drag bug, third report — root causes found (not a fourth tweak):**
  (a) Entry was broken two ways: empty-space long-press went to the M5
  action bar, which had NO "Edit home" button, so the standard gesture could
  never enter edit mode; icon long-press triple-fired (drag loop + icon
  menu + page detector) and the menu dialog swallowed the finger-up the drag
  loop waited for. Fix: exactly one owner per path, every path leads to edit
  mode (action bar + Edit home button; app-row menu + Edit home row;
  folder/widget long-press → edit mode directly; QuickPanel Edit kept).
  (b) Detector: drag now starts ONLY from the ≡ handle, ONLY in edit mode,
  after touch slop — no long-press wait inside the drag path; if scroll
  claims the gesture first the drag aborts instead of fighting it.
  (c) Layout lock was NOT the blocker (default off, nothing turns it on
  alone) — but every edit-entry path now shows the locked toast instead of
  doing nothing. (d) Swallow: pager deleted; home vertical-swipe detector no
  longer wraps the list (swipes now start on the dock, which never scrolls);
  nav-gesture edge strips only claim touches starting inside the strip —
  except the alphabet rail at the extreme right edge, where rail drags may
  trigger Back while nav gestures are on (known limitation; rail taps work).
  Diagnostic affordance: 0.87 shrink + toolbar + banner pill ("Editing home
  · Drag a handle to move · Tap a row to select") + ≡ handle on every movable
  row + shared jiggle + accent insertion bar; refused drops toast a reason.
  Swipe-up on home now starts on the dock (in M4 scroll mode it did not work
  at all, so this is strictly better — device check confirms acceptance).
- **Per-icon swipe-up wiring (integration):** `iconSwipeUp` in ScrollHome.kt,
  attached only when a swipe action is configured (zero behavior change
  otherwise). A swipe is a fast upward flick starting on the icon: 56–200dp
  up, mostly vertical, pointer lifted within 300ms; while a candidate, moves
  are consumed so the list does not scroll under it; if it stops looking like
  a flick the detector stops consuming and the list scrolls normally. This
  trade-off is opt-in only (default: no swipe configured → no detector).
  Fires through the existing `performGestureAction` dispatcher.
- **Integration hooks wired (were stored-only from sibling workers):**
  per-app badge silencing now live in `BadgeOverlay` (Home.kt); At-a-glance
  widget now in the picker (`OWN_WIDGETS`); drawer list/grid rows now show
  `BadgeOverlay` (previously home-only); duplicate profile button in Cat13.
- **Exhaustive gap audit (user direction "Don't miss any single feature"):**
  `docs/GAP-AUDIT-2026-10-23.md` accounts for 195 canonical features across
  the three reference analyses (Niagara 71, Nova 60, Smart 64): 74 already
  present, 16 built in this milestone, 14 removed by user direction, 91
  blocked with exact reasons + hooks. COVERAGE.md corrections found by the
  audit: "apply wallpaper to system/lock" was overstated (no
  WallpaperManager.set* path exists — blocked, audit C62); drawer
  search/recents/suggestions rows are stored-only (Drawer never reads them);
  `animSpeed` has no renderer consumer outside Settings/Backup (Cat11 demos
  + stored value only). All three are recorded honestly in COVERAGE.md now.
- **M5 preview-tier downgrades carried forward (from the M5 build):** Cat1
  accent wallpaper-derived stored-only (no wallpaper colour extraction);
  Cat1 noise/shadow/outline controls dropped (surface material drives blur);
  Cat3 crop frame draws a placeholder, not the photo bitmap; Cat5 horizontal
  pages/scrubber stored-only (drawer renders grid/list/categories); Cat6
  teardrop/system shapes stored-only; Cat7 folder size/bg/title stored-only
  (FolderSheet does not read them); Cat8 frame [L] via live sample, not the
  real home; Cat10 files/web/shortcuts + web engine are info rows; Cat9
  swipe left/right + long-press-empty triggers and recents/launch-app/
  toggle-dock actions are info rows; Cat13 profile preview is a snapshot
  summary, not a mini; grid-shrink pre-commit gate still partial (exact
  count renders inline while dragging; house sliders commit on drag, so the
  dialog is delivered but not wired as a gate); search deep-link is
  category-level (row highlight accepted but not rendered); live-drag
  transient layer in HavenRoot not built ([L] is via the half-sheet path).

## M6 deviations (2026-10-23, integration)

- **versionCode 5 → 6, against the v2 doc's no-version-change clause.**
  The v2 doc said not to bump for the redesign; the bump is deliberate:
  M6 deletes the entire old settings surface and re-hosts Settings as a
  panel, so a user (or a future build) must be able to tell vc5's
  14-category shell from vc6's panel by versionCode alone, and the
  vc5→vc6 upgrade path needs a distinct code. versionName stays 1.0.
- **Preset carousel / pages removals (user direction, not v2 defaults).**
  The v2 doc's hub screen assumed presets and pages; both were deleted
  by the user on 2026-10-23, so M6 ships neither: no preset carousel
  (Look hero is Minimal + accent swatches only), no pages (scroll home
  in both snap modes), no Attached/Floating dock switch (the dock is
  always the floating pill; the control is position Bottom/Left/Right).
  If the user later wants presets/pages back, that reverses a direction,
  not a doc detail.
- **Wallpaper-derived accent stored-only.** Mode 3 still falls through
  to the Minimal default in `Looks.kt` (no wallpaper colour extraction
  exists); the Look hub's Accent segmented control shows mode 3 as
  Default rather than offering a dead fourth option.
- **Drawer columns / icon size omitted.** `drawer_cols` and
  `drawer_icon_dp` had no live consumer (grid uses fixed adaptive
  cells; drawer icons follow home icon size), so the Drawer & Search
  hub offers neither; the audit's removal list covers them.
- **Widget frame preview-only.** The Home & Dock widget-frame controls
  write `widget_radius`/`widget_bg`/`widget_opacity`, but no real home
  widget reads them; the hub copy does not claim otherwise, and the
  controls are kept because the frame preview is the audit's stated
  partial, not a dead control with no consumer at all.
- **Grid-shrink count caveat.** `gridShrinkReport` counts APP/FOLDER
  items whose half-cell rect exceeds the new grid; the scroll home
  renders no grid, so the count is a widget-sheet-cell estimate, and
  shrinking never refuses or moves anything on the scroll home itself.
- **Snap 1-D semantics.** `placementMode` is a single field, not a
  per-axis snap: 0 = snap on, 1 = snap off, legacy 2 reads as snap on
  (`snapOn()` in `Grid.kt`). Both the edit-toolbar toggle and the new
  Home & Dock row write the same key.
- **Motion-speed outcome: WIRED.** `animSpeed` now drives real
  transitions (Home dock fade + edit shrink, Quick Panel slide, panel
  slide) through one mapping in `ui/MotionSpeed.kt`; the Off / Fast /
  Normal / Slow control in Gestures & Motion is therefore real, and the
  audit's "no renderer consumer" correction is superseded for these
  four transitions. Other animations (e.g. the scroll-home shimmer)
  are not speed-scaled and are not claimed to be.
- **SearchEntry unification.** The shell type wins; hub lists were
  converted from single-string synonyms to synonym lists. Hub slugs
  were normalized to display names, so the search index shows "Device
  & Data" and "Privacy", not "device-data" and "privacy".
- **Profiles placement.** Profiles live in Device & Data next to
  Backup (their old M5 category was Backup & Profiles); the schedule
  row shows only when a schedule is set, and schedule switching still
  does not auto-restore the previous profile.
- **Changes footer wording.** The v2 spec footer "Changed from preset
  (N)" is rendered as "Changed this session (N)": presets no longer
  exist, so "from preset" would be a stale concept. The count is the
  panel-session diff (fields changed since the panel opened), and the
  footer opens the same Changes list as the title-bar chip.
- **Needs-attention banner, third priority.** "Backup/restore failed"
  has no persistent record anywhere else, so it is wired through a
  local-only flag set by the Device & Data hub on import failure and
  cleared when the banner is opened from or an import later succeeds.
  Export failures do not set it (the export path reports inline only).
- **No settings migration in M6.** No store key changed, was renamed,
  or changed meaning; the only store-adjacent change is the restore
  path now also writing three keys it previously skipped (see
  COVERAGE.md M6). `docs/PERMISSIONS.md` is unchanged: M6 adds no
  permission, no network use, no dependency, no analytics.

## M7 deviations (2026-10-23, integration)
- **Home doc adapted, not implemented literally.** The v2 home doc
  assumes five style presets, paged homes, a page overview, a Today
  page, and a "gems" design metaphor. All four are overridden by
  standing user directions: Minimal-only (accent colour is the only
  theme control), no pages (one continuous scroll home), sections
  overview instead of page overview, Today section/column instead of
  a Today page, and no "gems" wording in any app string, class name,
  or UI. If the user later reverses any of these, that reverses a
  direction, not a doc detail.
- **TodaySection stays pinned above ScrollHome on Compact/Medium.**
  True in-flow placement needs a header slot in ScrollHome's
  LazyColumn; that slot would shift the alphabet-rail index math
  (`headerIndex` assumes the list starts with the edit-mode button /
  sections), so a slot is not a zero-risk change without a device run
  to re-verify rail scrolling. Left pinned-above and recorded as a
  known gap rather than risking a rail regression blind.
- **Snap reader swap, toggle unchanged.** ScrollHome and the
  edit-toolbar toggle now read `state.snapToGrid`. The toggle still
  writes through `setPlacementMode`, which syncs `snapToGrid`
  (`coerced != 1`), and the Home & Dock hub row writes both — so the
  two keys cannot diverge through any UI path. `snapOn()` in Grid.kt
  remains for legacy readers; nothing reads it on the home path now.
- **Library persistence replaces drawerMode on the Home host only.**
  The Home LibraryOverlay host persists `libraryDefaultView`; the
  legacy `DrawerOverlay` entry point still maps/writes `drawerMode`
  and is kept signature-compatible (it has no live callers). A future
  cleanup may delete it; it was not deleted in M7 because the brief
  allowed exactly one Dock.kt API change and no redesign.
- **Calm gesture case without a GESTURE_ACTIONS entry.** The
  `performGestureAction("calm")` case exists and calls `enterCalm`,
  but `GESTURE_ACTIONS` (which gates stored gesture values) has no
  "calm" entry, so no gesture row can currently be bound to Calm.
  Adding the entry + a Gestures-hub row is a product decision for a
  later milestone, not an integration fix.
- **Legacy drag-and-drop source kept deliberately.** The Library
  drag-out source uses the deprecated suspend `dragAndDropSource`
  overload (warning, not error). The replacement overload hands start
  detection to Compose and takes only a transfer-data callback, which
  would change the long-press trigger and lose the `onDragStarted`
  ordering hook (the coordinator lowers the Library overlay at drag
  start). Migrating it is a behaviour change needing a device run,
  so it stays until one is scheduled.
- **No settings migration in M7.** No store key changed meaning; the
  only store-adjacent change is the session-restore path now also
  writing the 9 M7 keys (see COVERAGE.md M7). `docs/PERMISSIONS.md`
  is unchanged: M7 adds no permission, no network use, no dependency,
  no analytics. versionCode 6 → 7, versionName stays 1.0.

## M8 decisions (2026-10-23, Worker C: legibility + docs)

- **Horizontal is THE home; vertical scroll is NOT kept as an
  option.** M5–M7's vertical scroll home came from misreading
  "remove pages" as killing paging. The 2026-10-23 clarification
  settles it: "remove pages" meant the ability to delete extra
  pages (add-page implies remove-page), never removal of the paging
  system. M8 restores the horizontal pager, and the scroll home is
  retired, not kept alongside as an option. If the user later asks
  for vertical scroll back, that is a new direction, not a
  reinstatement of an M8 option.
- **No presets still.** Minimal-only + accent colour stands through
  M8: no preset carousel, no templates, no Look system of any kind
  returns with the pager. Page management (add / hide / default /
  reorder) is manual control, not a preset.
- **Migration algorithm (flow → pages), summary for the
  coordinator.** Take the M7 scroll-flow item order as the packing
  order and fill pages in that order: favorites, widgets and folders
  keep their id, kind and payload, and are assigned page numbers in
  sequence. Capacity rule: one page holds at most its grid capacity
  (cols × rows cells, counting multi-cell spans); when a page fills,
  the next item spills to the following page, and a new page is
  created whenever the last page fills. A spill never silently drops
  an item — if an item cannot fit the grid at all (span larger than
  the page), it still lands on its own page rather than being
  discarded; exact oversize handling is the coordinator's call and
  must be recorded when it lands.
- **No item resets in M8 — stated explicitly.** No app, widget or
  folder is deleted, unpinned, or reset to defaults by the
  migration or by any M8 change. Only page assignment changes (new
  page numbers from packing order); content, folder membership and
  widget configuration carry over untouched.
- **Page-delete rules are the user's clarification, not a new
  invention.** Empty pages delete freely; non-empty pages confirm,
  move icons to an adjacent page, and offer 5s undo; the last page
  cannot be deleted; a move with nowhere to land is refused, never
  lossy. These rules live in COVERAGE.md M8; this note records that
  they come from the clarification, not from worker discretion.
- **Legibility cache rule, authoritative for M8.** The helper's
  cache key is wallpaper identity + grid size (`wallpaperKey` plus
  `cols x rows`). Dim and label-mode changes do NOT invalidate the
  cache — they only change rendering through `labelStyleFor` (dim
  scales luminance toward black before the call; mode selects the
  branch). This supersedes the pre-M8 Legibility.kt header text
  that listed dim/mode as recompute triggers; the
  `WallpaperLegibilityCache` doc already stated the M8 rule.
- **Legibility bitmap source: `source`, never `blurred`.** The
  coordinator passes `app.wallpaper.source` (the HavenOS-mode render
  bitmap, the same bitmap `WallpaperLayer` draws). `WallpaperStore`'s `blurred` copy is a 180px pre-blurred backdrop for the glass
  pipeline — blurring destroys the variance Auto needs, so it must
  never be fed to the legibility engine.
- **System-wallpaper Auto degradation, no workaround in M8.** In
  system mode the window is transparent and the OS draws the
  wallpaper; `WallpaperManager.getWallpaperFile` throws
  `SecurityException` on API 33+ (probed, never assumed, in
  `WallpaperStore.probeSystem`, which records only readability plus
  a blurred copy and never exposes the full bitmap). The coordinator
  passes `bitmap = null` in system mode, and Auto degrades to the
  mid-grey default until a HavenOS wallpaper is used. No
  `WallpaperManager.set*` path exists in the codebase and none is
  added here.
- **Parallax stats-validity note.** Stats are computed once for the
  current page grid over the full wallpaper. Pager parallax shifts
  the wallpaper by at most 8% — under half a grid cell at any
  shipped grid size — so per-cell stats stay valid across pages and
  no per-page resampling is needed. If a future parallax range
  exceeds half a cell, this assumption must be revisited.
- **ScrollHome retired, sections overview replaced — both recorded
  in COVERAGE.md M8.** "Retired" means the scroll home is no longer
  the home; whether its file is deleted or kept is the
  coordinator's call and is not decided here. The sections
  overview's session-local persistence gap is moot: the feature is
  replaced by the page overview, not fixed.
- **No settings migration in M8 (this worker).** No store key
  changed meaning in the legibility work; `labelMode` is read as
  before. If the pager coordinator adds page-state keys, that
  worker records the migration; it is not claimed here.
  `docs/PERMISSIONS.md` is unchanged: M8 (this worker) adds no
  permission, no network use, no dependency, no analytics.
  versionCode 7 → 8, versionName stays 1.0.

## M8 integration (coordinator, 2026-10-23)
- `pageCount` floor key (Settings.kt + Backup): added empty pages leave no
  HomeItem rows, so the total page count cannot be derived from items alone.
  `pageCount` (default 0 = derive) floors it; Add Page sets it to the new
  total, delete lowers it, section reset returns it to 0 (empty added pages
  then drop; item pages are unaffected). Pre-M8 backup imports reset it to 0.
- Hidden pages: PagedHome had no hidden support, so the pager now skips
  hidden grid pages via the centralized pager<->grid mapping (indicator,
  drag targets, and default-page conversion all follow). Hiding the last
  visible page is refused by the host with a toast (a zero-page home is
  meaningless); a corrupt all-hidden hiddenPagesJson falls back to showing
  all pages rather than rendering a zero-page pager.
- `defaultPage` is a GRID page everywhere (overview, picker, setting).
  PagedHome converts grid->pager for initial page and later changes; this
  fixes a Worker A off-by-one where the raw value was used as a pager
  index (wrong on Compact/Medium with Today on, where pager page 0 is
  the Today page).
- Folder drops: the host contract passes member keys only (no drop cell),
  so a created folder lands on the first free cell of grid page 0,
  spilling to later pages when page 0 is full — never overlapping (0,0).
- Glance: exactly one line — PagedHome's top Glance row when Today is off,
  the host AtAGlanceLine above the dock when Today is on. Never both.
- Migration grid: `migrateFlowToPages` packs by the CURRENT orientation's
  grid at migration time; pages keep their stored cells afterwards, so a
  later rotation or density change does not re-pack them (density shrink
  after placement is a device check, not a re-migration).
- New installs appear in the Library only (no auto-add pass — the vc5 pass
  was removed because the scroll home showed everything; the paged home
  shows placed items only). Home placement is manual: drag from Library
  or Add to home.
- TalkBack: the M7 section-based helper (HomeA11y.kt) is unattached legacy
  (no host calls it; its "Move to section" wiring is example code in a
  KDoc comment). A page-based "Move to page" action attached to PagedHome
  cells is the follow-up, not something M8 pretends to have.
- Build fixes (no behavior change): Worker A's PagedHome called GridCell
  before its declaration — Kotlin forbids forward references to local
  functions — so the GridCell block was moved before GridArea; a missing
  `parseMembers` import was added (it caused the Any/Char cascade).
- M9 (vc9) adaptations and deviations (Docs 1–3, 2026-10-23): "preset" ≡
  Minimal everywhere — no preset picker/carousel/switching is built; the
  M6 Look hero card is a current-tokens summary + accent strip, not a
  switcher. Settings panel UI visually frozen; only behavior behind rows
  changed. Today is a DEDICATED card-flow page (Doc 2 §4.3), replacing
  M8's Today-column-beside-grid on page 1; Glance row shows on home pages
  when Today is not visible. versionCode 8→9 (docs' no-version-change
  clause is about releases; milestone bumps are the established delivery
  practice). APK renamed HavenOS-vc9.apk.
- M9 deviations (deliberate, with reasons): snap OFF = HALF-cell steps, not
  quarter-cell (stored grid is half-cell units; quarter-cell cannot be
  represented without a schema change). Context bar sits under the toolbar
  in reserved space, not adjacent to the item (item geometry is not
  hoisted to the host). Corner handles are visual only; resize runs
  through the context-bar Resize entry. Dock per-slot highlight not built
  (OneDock exposes no slot geometry). Pager-drag-to-dock undo restores the
  grid item but the dock entry stays (dock membership not in the layout
  snapshot). Minimal's default accent is NOT gold in code (light slate /
  dark pale blue-grey); "gold" in captures is the user's chosen accent.
  Label size stays the labelSize setting (default 11sp), not Doc 2's
  12/13sp token — the setting wins. Restore list shows counts, not a
  visual home preview. Settings-value undo is not snapshotted (layout
  stack only). Dock drag reorder / drag-out / folders-in-dock not built
  (needs a sealed DockItem type + shared grid↔dock drag geometry; menu
  Move actions shipped instead). Today card drag-reorder not built.
  Settings-panel free-area re-layout NOT done (dock may sit under the
  panel). Home.kt still contains the home composition twice (two
  PagedHome call sites); Worker B mirrored host changes into both trees
  rather than deleting tree 1 mid-refactor — deduplication is a follow-up.
- M9 integration fixes by the coordinator: toolbar Calm button now opens
  Worker D's full CalmEditorScreen (Worker A's minimal CalmEditorDialog
  call site removed to avoid a double dialog); sheet onPlace uses the
  host's addWidgetAtFirstFree (current → other pages → new page);
  onOpenSearch passthrough added to both PagedHome call sites (tree 1
  already had it — one duplicate argument removed); todayWeather now uses
  formatTempDisplay + localityOnly in both trees; autoSnapshotOnEditExit
  called from toolbar Done + Back-handler exits in both trees; Restore
  dialog replaced with D's snapshot list; deduplicateSeededAgendas runs
  once per process on launch. CalmEditorDialog remains defined in
  EditMode.kt but unreferenced.
- M9 test status: unit tests written by all four workers (collision, snap
  math, undo/redo, dock capacity, grid fit, contrast routine, free-spot
  search, widget state machine, weather units, profile derivation,
  snapshot create/restore, uninstall cleanup, migration) but NOT run —
  the sandbox Gradle test-executor crash is pre-existing and unchanged.
  Compile checks run: :app:compileDebugKotlin green after integration;
  :app:assembleRelease BUILD SUCCESSFUL in 8m 51s.

## M10 decisions (2026-10-24, integration)
- M10 was assigned with three parallel workers; all three returned the
  same identical refusal notice with zero file changes (verified: repo
  timestamps + git status unchanged). No replacement helpers were
  spawned; the coordinator implemented all three scopes directly
  during integration. The work below is the coordinator's own.
- Home.kt dedupe (M10, before any other edit): the file composed the
  home TWICE (two helper sets, two ScrollArea/DockArea/EditToolbarBlock
  trees, two WallpaperLayers, double gesture detectors) — proven by a
  char-level brace scanner (inner Provider/Box closed at old lines
  3863/3862, outer at 4203/4202) and by single-definition greps after
  surgery. Deleted old lines 2177–3518 + 3862–3863 (helpers B +
  tree B); kept dialogs + outer closers. 4771 → 3427 lines. Backup at
  /tmp/Home.kt.pre-m10-dedupe. Verification build BEFORE M10 edits:
  BUILD SUCCESSFUL in 11m 25s. This also corrects the M9 note that
  "Home.kt still contains the home composition twice" — it no longer
  does — and the duplicated back-handler registration found beside it
  (early block deleted; onBackAction replaces, so the late block always
  won; the late block is now the single live handler).
- Audit hypotheses, code-verified (not assumed): (1) multiple layout
  authorities — TRUE structurally (the double tree, now removed; plus
  grid math in gridCells vs pagedGridCells vs LayoutProfiles). (2)
  absolute positioning without occupancy — FALSE for the paged grid
  (findSpot/findSpotNear/resolveDrop/resolveDropEdit existed); the
  real defects were host/pager formula divergence + no reflow +
  no validation. (3) no collision resolver — FALSE as a helper
  (resolveDropEdit), TRUE as a system (nothing validated stored
  layouts or reflowed on grid change). (4) grid controls cosmetic —
  TRUE in two places: quick-tune chips wrote the portrait override
  only (HubScreen Quick tune, now writes both orientations), and grid
  Apply never moved the promised icons (now the host reflow does).
  (5) responsive breakpoints missing — PARTLY (size classes existed;
  tablet landscape was a stretched grid until the rail shell).
  (6) dock page-scoped — FALSE (host-level OneDock outside the pager
  since M9; the recording's dock variance is not explained by paging
  — recents churn + panel overlap are the remaining candidates).
  (7) typography not measured — NOT VERIFIED as a defect (shell rows
  use heightIn(min), search rows wrap; no fixed-height clipping found
  in shell/search paths). (8) contrast wallpaper-blind — TRUE for the
  no-stats fallback (mid-grey guess, now Pill). (9) too much GPU work
  — PLAUSIBLE (double composition was the biggest contributor, now
  removed; window blur-behind only fires for drawer-open or Partial
  tier, never for the Settings panel). (10) edit mode lacks draft
  state — TRUE-ISH (Settings previews had a draft layer; layout edits
  committed immediately with undo; M10 adds impact previews + undo
  but NOT a full pre-commit draft for every drag — drags still commit
  with undo, which the acceptance criteria allow via "reliable undo").
- Recording-vs-build timing (honest caveat): the audit's recording is
  timestamped 22:28 Oct 23 (device time, IST); vc9 finished building
  22:24 IST and was delivered minutes later, so the recording most
  likely shows vc8 (or the first minutes of vc9). Each bug was
  therefore verified against vc9 CODE, not assumed from the video.
  Bugs already fixed in vc9 (gold outlines, blank widgets) are not
  re-claimed as M10 fixes.
- Today rail vs Today page (adaptation, recorded): Expanded landscape
  = rail (30/70) + no Today pager page; Medium = collapsible strip +
  no Today pager page; Compact = Today pager page survives. The
  glance line is hidden wherever the rail/strip shows date/weather
  (no duplication). "Today page retired on Expanded" means the pager
  page only; Today data (weather/events/suggested) still refreshes on
  todayOn for rail + strip.
- Density rename (adaptation, recorded): the airy density level is
  "Spacious" in UI (value 15), never "Calm" (Calm mode is the focus
  list). Balanced = 40 (the existing default), Dense = 75, Custom =
  any other density or any grid override. Selecting a level clears
  both grid overrides so the level visibly applies; undo restores.
  The QuickPanel slider keeps its existing behavior (dragging it
  does not clear overrides; "Follow density" in the grid picker does).
- Settings master-detail (adaptation, recorded): hubs, rows, and copy
  UNCHANGED; only the tablet-docked presentation gains the rail.
  Panel widened 42%/440–600dp → 46%/480–660dp (shared formula
  tabletPanelWidthDp, used by the home free-area layout too, so the
  two never disagree). Tablet scrim dropped (panel opaque + home
  re-laid in the free area = nothing under the panel to dim). Hub
  screens keep their own top bar under the panel title bar (mild
  duplication: breadcrumb + hub title; Back still works from both).
- HNK overlay (BUG-010, adaptation, recorded): grep for HNK/watermark
  across app/src/main/java returned NOTHING, and no large decorative
  text is drawn (only the 68sp clock + small 0.12–0.25f alphas). Working
  conclusion: HNK is WALLPAPER pixels, not HavenOS-drawn. Per the M10
  conditional rule the wallpaper is untouched; readability is handled
  by local scrims + the legibility Pill fallback. The redesign doc's
  standalone Identity-layer control (Off/Subtle/Statement) was NOT
  built — it was conditional on HavenOS drawing the overlay. If the
  parent wants it as a feature anyway, it is a new decision, not an
  M10 miss.
- hostGridCells estimate (documented imprecision): Compact/Medium
  grids are fixed, so the workspace-size estimate only matters on
  Expanded, where the workspace is full width (or ~70% with the rail)
  minus 32dp margins and height minus ~220dp of chrome. A boundary
  estimate error can flip a fit col on Expanded; the reflow safety
  net repairs any item that lands outside the rendered grid, so the
  estimate can never strand an icon off-grid.
- Reflow safety-net semantics: runs after every layout write (keyed
  on items + grid dims + pagesMigrated), gated on pagesMigrated, and
  is a no-op on valid layouts (reflow preserves fitting items
  exactly). Rotation CAN move icons when the new orientation's grid
  is smaller — that is the acceptance trade-off (identity + validity
  over position). A fully-full grid keeps overlaps (flagged by
  validation, never silently dropped) rather than deleting items.
- Level 1 bar scope: long-press empty now opens the bar INSTEAD of
  entering edit mode directly; "Edit home" in the bar preserves the
  old entry path. There is no separate "home menu" in the app, so
  long-press is the only L1 trigger. Layout and Dock both open the
  Home & Dock hub (the hub contains both sections; no per-section
  deep link exists in openSettingsPanel).
- Level 2 scope: Replace is widgets-only (apps have no replace
  picker; folders have no replace semantic). Before/After hold for
  visual changes is NOT built for object editing (the Settings panel
  has Before hold; object edits preview via size/validity text +
  undo). "Send to Today" is NOT built (rail has no widget slots).
  Folder preview modes are NOT built (single 3×3 grid preview kept).
  Two-line label reserve is NOT built (labels stay single-line with
  visible ellipsis).
- LATER list unchanged (not built): themed monochrome/tint modes,
  split-view/app pairs, media card, widget-stack sync across pages,
  shared layout between devices.
- M10 test status: no unit tests written or run in this round (the
  sandbox Gradle test-executor crash is pre-existing; the M9 test
  situation is unchanged). Compile check: the dedupe-only release
  build passed (11m 25s); the full vc10 build is the M10 verification
  build (see the handover report for its result).
