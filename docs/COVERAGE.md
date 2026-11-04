# COVERAGE — HavenOS feature coverage (builder prompt §0 visibility rule)

Status key: `done` = built and reachable in UI · `partial` = built with a stated
limitation · `blocked` = cannot be built as specified, reason given · `planned` =
in scope, scheduled in a later milestone (not dropped).
Verification for M2 = full code review (incl. an independent ponytail-review pass) +
release build + artifact checks (aapt / apksigner). Nothing in M2 is device-verified —
see DEVICE_CHECKLIST.md and UNVERIFIED.md.

## M3 — Navigation gestures + vc2 feedback fixes (versionCode 3)

### Navigation gestures (Phase 1)
| Feature | UI path | Status | Verified how |
|---|---|---|---|
| Own navigation-gesture layer (accessibility service + edge overlay strips): bottom-up = Home, bottom-up-hold = Recents, side swipe-in = Back; all remappable | Settings → Gestures → Navigation gestures | done | code review; device checks N2–N7, N10 |
| Optional system-wide nav-bar hide via `Settings.Global` `policy_control` when WRITE_SECURE_SETTINGS is granted via ADB, with save/restore of the previous value | Settings → Gestures → Navigation gestures | done | code review; device check N8 |

### vc2 feedback fixes (Phase 2)
| Feature | Status | Verified how |
|---|---|---|
| Own widgets on translucent glass surfaces | done | code review; device check P1 |
| Settings spacing/grouping polish | done | code review |
| Drawer search rework (frosted glass, floating search pill, ranked prefix > contains > fuzzy results, alphabet rail with letter bubble, category tabs) | done | code review; device check P2 |
| Movable apps with nearest-free-spot reflow on blocked drops | done | code review; device check P3 |
| Resizable folders (whole-cell spans, span-aware popup columns) | done | code review; device check P4 |
| Dock add/remove via drawer long-press and edit-mode toggle | done | code review; device check P5 |
| Widget picker shows all widgets first with live/preview images + sizes + search | done | code review; device check P6 |

## M2 — Visual upgrade v2.2 + Settings structure (versionCode 2)

### v2.2 §1 — Glass system
| Feature | UI path | Status | Verified how |
|---|---|---|---|
| Glass pipeline (§8 ladder: Full sampled-blur / Partial window blur-behind / Flat tint) | Every card, dock, folder, widget via `GlassSurface` | done | code review; device check E2 |
| Tint + vibrancy + 1px specular border + inner highlight + drop shadow in all tiers | `GlassSurface` (theme/Glass.kt) | done | code review |
| Default Look on first launch = Glass | First-run seed (Template A) | done | code review; device check E2 |
| Reduce Effects → flat translucent | Settings → Look & Feel; Quick panel | done | code review |

### v2.2 §2 — Starter templates
| Feature | UI path | Status | Verified how |
|---|---|---|---|
| A Glass/Calm (serif + analog clocks, weather, agenda, now playing, flowing apps, dock ×6) | Auto-applied on first run; Settings → Home Screen → Templates → Calm | done | code review; device check E3 |
| B Productive (full Clock page + dense widget page, category folders, dock ×10) | Settings → Home Screen → Templates → Productive | done | code review |
| C Premium Glass (A's layout, Premium look, dark) | Settings → Home Screen → Templates → Premium Glass | done | code review |
| Dense (clock strip + all apps) | Settings → Home Screen → Templates → Dense | done | code review |
| D Free/Custom start (one digital clock + apps) | Settings → Home Screen → Templates → Free | done | code review |
| Live schematic preview + Apply + Undo | Template cards in Settings → Home Screen | done | code review |

### v2.2 §3 — Own widget kit (all read LookTokens; restyle with the Look)
| Widget | Status | Notes |
|---|---|---|
| Clock (serif / digital / analog / flip / editorial; edit-mode variant cycler) | done | |
| Weather (current + hourly + 4-day, Open-Meteo) | done | compact layout appears at ≤2-cell width |
| Agenda (today / tomorrow payload) | done | permission prompt when READ_CALENDAR denied |
| Calendar month | done | |
| Now Playing (media session) | done | "enable notification access" prompt when listener not granted — never silently empty |
| Storage (+ memory bars) | done | |
| Countdown | done | target stored at UTC midnight; displayed remaining can differ by the UTC offset |
| Tasks | done | persisted to filesDir/tasks.json |
| Notes | done | |
| Gallery | done | photo via document picker, persistable grant |
| Quick Tiles (incl. real torch toggle) | done | |
| Stack | partial | member payloads are not persisted — stack members are synthetic items, so a member's own configuration (e.g. a countdown date) does not survive; the stack's member kind list does |
| Clock Page (full-screen, Template B) | done | gated by Settings → Home Screen → Clock page |
| Third-party AppWidgets (bind + configure flow) | done | size hint uses a 72dp/cell approximation (marked `shortcut:` in code) |

### v2.2 §4 — Dock
| Feature | Status |
|---|---|
| Floating glass dock, pinned + recents sections with divider, optional search pill | done |
| Icon count (dockMax), bottom/left/right position, auto-hide with handle strip | done |
| Long-press pinned icon removes it; apps button long-press opens Settings | done — note: M1's dock "Settings" text chip is gone; Settings opens via the apps-button long-press, the Quick panel, or the settings gesture |

### v2.2 §5 — Placement & editing
| Feature | Status |
|---|---|
| Grid mode with half-cell units (Room v2; v1 layouts migrate ×2) | done — migration verified by code review; device check E1 |
| Free canvas with fraction positions, live drag, snap guides | done |
| Density slider 0–100 live-remaps columns/rows/icon size | done |
| Edit mode: multi-select, resize (widgets), remove, undo (10 steps), Add widget / Add page | done |
| Layout lock | done |

### v2.2 §6 — First-run feel & Quick panel
| Feature | Status |
|---|---|
| First home already looks like Template A | done |
| Quick panel (swipe-down gesture by default): Look chips (built-ins + customs), live Density slider, dark-mode chips, Reduce Effects/Motion switches, Settings / Wallpaper / Edit home shortcuts | done |
| At-a-glance line (off / date / date+weather) above the dock | done |

### SETTINGS-SPEC — 13 categories (exact order, search on top, glass cards, live previews, immediate apply)
| # | Category | Status | Notes |
|---|---|---|---|
| 1 | Look & Feel | done | gallery with live previews, Look Editor (accent/corner/opacity/blur), duplicate/rename/delete, .havenlook export/import, density + live grid preview, fonts, icon shape/size with live icon preview |
| 2 | Wallpaper | done | System vs HavenOS source cards, gradients/solid/photo, crop/dim/tint/parallax, glass-quality readout, apply to system/lock (HavenOS mode) |
| 3 | Home Screen | done | templates + undo, grid⇄free conversion, per-orientation grid, gap slider, clock page, at-a-glance, lock, auto-add |
| 4 | Dock | done | live preview + all controls |
| 5 | Drawer | done | mode/sort/scrollbar, category rename/hide, hidden apps behind device-credential gate when locked |
| 6 | Widgets | done | weather city search (Open-Meteo geocoding), units, network toggle, calendar selection, stacks, hosted-widget list with honest rebind |
| 7 | Icons & Badges | done | pack discovery, monochrome, labels, badge mode + notification-access status |
| 8 | Gestures | done | 5 triggers, conflict warning + swap-on-confirm, per-gesture Test |
| 9 | Search | done | source switches, ranking reset, history clear |
| 10 | Profiles | done | switch (auto-saves the profile being left), save/rename/delete, schedule |
| 11 | Backup & Restore | done | export/import with validation + summary, daily on-launch auto-backup |
| 12 | Permissions & Access | done | live statuses, one-tap Fix where the platform allows; informational rows carry no fake buttons |
| 13 | About & Help | done | diagnostics card, crash log view/copy/clear, licenses, version |
| — | Settings search (all categories, live, grouped, highlight + scroll to row) | done | |
| — | Per-section Reset with undo | partial | offered for sections 1–9 (the sections resetSection() defines defaults for); 10–13 hold user data (profiles, backups) or are informational, so they have no Reset button rather than a fake one |
| — | Replay intro / Replay tutorial (in About) | partial | honest placeholder — the intro/tutorial themselves arrive with the M5 startup flow |

### Carried-in scope built in M2 (from the v2.1 prompt)
| Feature | Status | Notes |
|---|---|---|
| Drawer modes (grid/list/categories/search-first) + sorts + scrollbar styles + fast-scroll rail | done | letter rail / thin bar apply in List & Categories modes (stated in the setting's explanation) |
| Drawer search sections: Apps (ranked), Calculator, Contacts, Events, Settings | done | each source toggleable in Settings → Search |
| Gestures (swipe up/down, double-tap, pinch-in, two-finger-down → drawer/search/quick panel/settings/lock/shade) | done | shade uses StatusBarManager reflection with quick-panel fallback — OEM-dependent (partial on some devices); lock uses device-admin force-lock |
| Icon packs (theme-intent discovery + appfilter.xml) | done | best-effort; a broken pack falls back to the app's own icon |
| Notification badges (dot/count) | done | requires notification access, prompted in Settings → Icons & Badges |
| Profiles + scheduled switching | done | |
| Backup/restore + diagnostics + crash log | done | restore fails loud: a backup whose items array parses to empty is refused |
| Weather — the only network feature, Open-Meteo only | done | host allowlist enforced in code (`WeatherNet.fetch`); manual city; °C/°F; network toggle |
| Async icon pipeline + cache (M1 should-fix) | done | icons load off the main thread (LruCache) |
| Stale-type-safe settings reads, self-healing | done | verified in review: all DataStore reads via `Preferences.safe()` |
| Reduce Motion | partial | consumed by the Quick panel transitions and the dock auto-hide fade (the Settings label says exactly this); no global animation system exists to gate |

## M1 — Foundation (versionCode 1, delivered)
Home activity (opens as an app without being default), app catalog with live
add/remove, paged grid, folders, drawer with search, Look engine, density, settings.
Superseded in M2 by the tables above; M1 details are in git-less project history —
the M1 COVERAGE table shipped with the vc1 build.

## Planned (in scope per prompt, scheduled — not dropped)
| Feature | Lands in |
|---|---|
| §7 startup flow: intro animation, logo, first-run permissions, tutorial; Safe Mode crash recovery | M5-later (style-picker step removed by user direction 2026-10-23; TutorialEndNote kept) |
| Per-page wallpapers | M5+ |
| Work-profile icon badge overlay | later |
| Stack member payload persistence | later |

## M4 — UI inspo redesign + bugs (versionCode 4)
| Feature | Status | Notes |
|---|---|---|
| BUG 1a — third-party widget add failures surfaced | done | bind/configure failures now show an error line in the picker sheet with retry; previously silently swallowed |
| BUG 1b — own-widget add on full page | done | refused with a "No room on this page" toast instead of silently landing on an unseen page at (0,0) |
| BUG 2 — app dragging (grid + free canvas) | done | drag-start rewritten: single pointer owner per icon; edit-mode drag starts on first slop move, no second long-press; locked layout respected |
| BUG 3 — edit-mode screen shrink | done | home content animates to 0.87 scale in edit mode (~300ms, critically damped); instant when Reduce Motion is on; wallpaper + edit toolbar stay full-size |
| BUG 4 — Settings two-pane shell (HyperOS photo) | done | ≥840dp: left nav (~1/3, large title, pinned search, grouped rounded cards, colorful icons) + right content (~2/3); phones keep single-pane flow; all 13 categories/controls preserved |
| Dock — Spider-Man floating frosted pill | done | full-width pill, 24dp side margins, one evenly-spaced row, no labels; pinned | recents divider, search/drawer buttons, dockMax preserved |
| Spacious defaults | done | Glass/Calm template ships gapDp 8 (was 4); first-run seed + template-apply both apply it; Density slider still governs |
| Swipe-up drawer toggle | done | Settings → Home Screen, default ON (= current behavior); OFF = swipe-up does nothing |
| Scroll home (Niagara-concept, original code) | done | Settings → Home Screen → Home style: Grid / Free canvas / Scroll list; favorites on top, full A–Z list, alphabet rail + letter bubble, same badges/menu; no folders rendered as rows in this branch |
| swipeUpDrawer in backup/restore | done | added to Backup.kt export + import (was missing) |
| Hosted-widget height estimate | partial | `spanFor` uses width-derived cell height; tablet cells aren't square so providers can clip ~a row — needs measured row height threaded from the grid (follow-up, not half-fixed) |
| Scroll-home labels | partial | rows always show labels (nameless rows can't be identified), ignoring the grid's labelsOn gate |

## M5 — Settings Design System + Minimal-only + no templates + no pages (versionCode 5)
14-category shell (SETTINGS-DESIGN-v1 wins as the shell over the old 13):
Style & Theme, Home screen, Wallpaper, Dock, App drawer, Icons, Folders,
Widgets, Gestures, Search & Smart, Animations, Privacy & Lock, Backup &
Profiles, System. Old→new mapping in ASSUMPTIONS.md (M5 decisions). M4's
two-pane shell evolved, not restarted: phone single column with search
indexing every setting by name AND synonyms (deep-link is category-level;
row highlight accepted but not rendered), collapsible live mini-home header
(state remembered), tablet two-pane with mini-preview at top of detail pane.
| Area | Status | Notes |
|---|---|---|
| Preview tiers (§1) | done/partial | [L] Live via half-height sheet over the real home (sliders write while dragging; home recomposes behind the 62% sheet); [M] Mini via real MiniHome/MiniDrawer from live state + real catalog (tap toggles compact/expanded); [A] Demo via DemoPlayer (overscroll/app-open/drawer/folder/dock-magnify/global-speed); [T] Test pads (gesture pad, sample badge, sample search "cal" running the real ranking); [–] None via result line + Undo. Every value also shown as text/number. Downgrades (honest, see ASSUMPTIONS.md): accent wallpaper-derived stored-only; crop frame placeholder not photo bitmap; Cat5 horizontal-pages/scrubber, Cat6 teardrop/system shapes, Cat7 folder size/bg/title stored-only; Cat8 frame [L] via live sample not the real home; Cat10 files/web/shortcuts + web engine info rows; Cat9 left/right/long-press-empty triggers + recents/launch-app/toggle-dock actions info rows; Cat13 profile preview is a snapshot summary; live-drag transient layer in HavenRoot not built |
| Entry points (§2.1) | done | Long-press empty home → bar (Wallpaper · Widgets · Style · Edit home · Settings); edit-mode Settings shortcut; icon long-press menu (Edit label · Edit icon · Hide · App info · Uninstall); drawer ⋮ → App drawer settings; any gesture bindable to Settings ("Open settings" action); launcher icon in drawer → full Settings; system launcher-settings intent (ACTION_APPLICATION_PREFERENCES) → full Settings |
| Apply model (§2.2) | done/partial | Instant apply, no Save button; top-bar overflow "Revert this session" (snapshot at Settings open; restores all fields incl. M5 ones, drawerMode/iconShape re-applied last); confirm dialogs only for destructive actions (reset, revert, restore, hide-all); grid shrink shows ghost overlay + exact overflow count inline while dragging — pre-commit gate NOT wired (house sliders commit on drag; dialog delivered, gate deferred, not faked); sliders in the live sheet update live while dragging and write on release; "Restart needed" badge component exists (no row currently needs it) |
| Screen layout (§3) | done | Row types: switch, slider with numeric value, segmented control, visual card picker, color swatch + validated #RRGGBB picker, nav row; every category has Reset section (System excepted — grants/info only; Privacy Reset unlocks only the hidden-apps lock, never unhides; Backup Reset clears schedule + flag only, never deletes profiles) + Advanced fold component (retrofit into old screens deferred) |
| Categories §4.1–4.14 | done/partial | All 14 built with listed settings wired where the feature exists; permission-gated rows use lock icon + "Needs X" → explain → system grant → re-check on return, never assumed; dependent rows disable with reason. Style & Theme reduced per direction (see removals below). Skeletons for icon-pack scan; empty states (Built-in only + link, hidden-apps explainer); errors leave state unchanged; offline note on weather/web rows; TalkBack values, ≥48dp house rows, reduced-motion respected (device verification owed) |
| REMOVED — Looks system (user direction 2026-10-23) | removed | All Look presets (Premium/Futuristic/Classy/Retro/Glass/Soft), Look Editor, custom looks, .havenlook export/import, preset cards/gallery, first-run style picker. Minimal is the one design language; glass treatment remains as part of Minimal's material. Stored look values normalize to Minimal on read; inert look/customLooksJson storage kept for backup round-trip only |
| REMOVED — Templates (user direction 2026-10-23) | removed | Starter templates (Glass/Calm, Productive, Premium, Dense, Blank), template picker, template apply/undo. First-run seeds a plain Minimal home (clock card + dock picks) |
| REMOVED — Pages (user direction 2026-10-23) | removed | Paged home, page dots/indicators, page-count/infinite-scroll/default-page/return-home settings and their Settings fields. Home is one continuous scroll (see below). Migration (not reset) via flowOrder — see ASSUMPTIONS.md |
| Home = scroll flow | done | Favorites on top, Widgets & folders flow, full A–Z below, slim alphabet rail; widgets/cards and folders live in the same flow; "Add to home" pins to Favorites; flow adds land visibly at the end (no "No room on this page" refusal anymore); swipe-up/down start on the dock (never scrolls), off in edit mode |
| BUG — apps cannot be moved (3rd report) | done | Root causes: (1) empty-space long-press → action bar had NO Edit home button, so edit mode was unreachable that way; (2) icon long-press triple-fired (drag loop + menu + page detector) and the menu dialog swallowed finger-up; (3) drag path contained a long-press wait that raced the other detectors. Fixes: one owner per path, every path leads to edit mode; drag only from the ≡ handle, only in edit mode, after slop; scroll-claim aborts drag instead of fighting it; layout lock was NOT the blocker (default off) but now toasts on every entry path; refused drops toast a reason. Diagnostic: 0.87 shrink + toolbar + banner pill + ≡ handles + shared jiggle + accent insertion bar, so entry failure is distinguishable from drag failure. Known limitation: rail drags at the extreme right edge may trigger Back while nav gestures are on (rail taps work) |
| Gap audit — essentials built | done | Per-icon swipe-up actions (model + Cat9 UI + live firing via ScrollHome flick detector, opt-in only); auto-categories (derived + merged, drawer tabs that actually filter); notification-dot group-summary dedup; per-app dot silencing (now live in BadgeOverlay + drawer rows); drawer badges (home-only before, now everywhere); at-a-glance widget (live in picker); duplicate profile |
| Gap audit — exhaustive (user direction "Don't miss any single feature") | done | `docs/GAP-AUDIT-2026-10-23.md`: 195 canonical features across Niagara (71), Nova (60), Smart Launcher (64) — 74 already present, 16 built in this milestone, 14 removed by user direction, 91 blocked with exact technical/platform reasons + hooks. This COVERAGE.md section is the summary; the audit file is the auditable line-by-line record |
| COVERAGE corrections (from the audit) | corrected | "Apply wallpaper to system/lock" was overstated — no WallpaperManager.set* path exists (blocked, audit C62); drawer search/recents/suggestions rows are stored-only (Drawer never reads them); animSpeed has no renderer consumer outside Settings/Backup (Cat11 demos + stored value only) |
| §7 intro/onboarding flow | partial | TutorialEndNote kept ("Customize anytime: long-press home."); style-picker step deleted per direction; full §7 flow (intro animation, permissions, tutorial) remains M5-later; replay intro/tutorial are placeholder dialogs matching old About behavior |
| Widget-stack member persistence, popup widgets | partial/blocked | Stack contents do not persist yet (needs a data-model change); popup widgets need a home container — both recorded in the audit with hooks, not half-built |

## M6 — Settings v2 redesign (versionCode 6)

The 14-category M5 shell is gone. Settings is now a panel hosted over
the live home (tablet landscape: home left ~58%, panel right ~42%;
phone: bottom sheet with peek / half / full detents), with a hub screen
plus six hubs. Old files deleted: `ui/settings/SettingsCat1–14.kt`,
`SettingsHub.kt` (old shell + search index + `SText` + category
scaffolds), `SettingsRows.kt`, `CatShared.kt`, `PermissionRows.kt`,
`PreviewLive.kt`, `PreviewMini.kt`, `PreviewTiers.kt`, `PreviewDemo.kt`,
`PreviewTestPads.kt`, `GridShrink.kt` (pure `gridShrinkReport` /
`gridShrinkMessage` moved to `ui/settings/shell/GridShrink.kt`; its
dialog is gone — the Home & Dock hub renders its own inline ghost +
confirm), `ui/OnboardingStylePicker.kt` (first-run style picker, already
deleted by direction in M5). Helpers that were still needed moved to
the shell first: `sessionChanged`, `restoreSettingsSnapshot` (now also
restores `catMergeJson`, `badgeHiddenJson`, `iconSwipesJson`, which the
M5 restore never wrote), `resetNewCategory` — all in
`ui/settings/shell/SessionDiff.kt`. `Settings.resetSection` is
unchanged (no store key changed in M6, so no settings migration).
| Area | Status | Notes |
|---|---|---|
| Hub screen (v2 §5, adapted) | done | Search field ("Search settings or try 'bigger icons'"); needs-attention banner, at most one, priority: not-default → Make default, navigation permission missing → Allow, backup/import failed → View in Device & Data; Minimal hero card + accent swatch strip (NO carousel — no presets exist); Quick tune (icon size slider + Auto, grid chips + Custom, mode Light/Dark/Auto, labels switch, dock position Bottom/Left/Right — position only, the dock is always the floating pill, so no Attached/Floating switch); Recent hub chips (up to 4, local SharedPreferences only); six hub rows (glyph tile tinted with the real glyph, never a letter tile; title + live one-line summary + chevron); footer "Changed this session (N)" → Changes list (wording adapted: there is no preset left to be changed from) |
| SearchEntry unification | done | Exactly one definition survives: the shell's `SearchEntry(title, synonyms: List<String>, hub, route, isAction=false)` in `shell/SettingsSearch.kt`. The Look hub's duplicate (synonyms as one string) is deleted; every hub list is converted to synonym lists and hub slugs normalized to display names ("Look", "Home & Dock", "Drawer & Search", "Gestures & Motion", "Privacy", "Device & Data"). All six hub lists aggregate through `searchEntries()`; search ranking is the shell's fuzzy ranker (title-prefix > synonym > subsequence, every query token must match) |
| Panel hosting + Before / Draft / Spotlight | done | Panel hosted in `HavenRoot` above the live home composition (inside `HavenTheme`); home renders the panel-open snapshot while Before is held, and the preview draft while a grid/wallpaper preview is active (grid picker and wallpaper pinch/drag write through `LocalDraftLayer`; Done commits + discards, Cancel discards). Spotlight rings wrap the scroll home (home/grid/icons/labels/widgets/weather/wallpaper keys), the dock (dock/search keys), and the open folder sheet (folder key), pulsed from the panel's spotlight key |
| Entry points (§2.1) | done | All route to the new panel: edit-toolbar Wallpaper / Personalization → Look hub, edit-mode Settings shortcut → hub screen; drawer ⋮ → Drawer settings → Drawer & Search hub (old 14-category names mapped to hubs); gesture → Settings → hub screen; launcher icon in drawer → hub screen; system launcher-settings intent → hub screen; long-press empty home → edit mode (whose toolbar carries the panel routes). The old half-sheet path is deleted |
| Test action wiring | done | `HavenRoot.performGestureAction` is passed as `onTestAction` into `GesturesMotionHubScreen`; the Test button fires the real gesture dispatcher. No dead Test button ships |
| Motion speed (animSpeed) | done | Wired, not faked: one mapping (`ui/MotionSpeed.kt` — Off = instant, Fast = half duration, Normal = base, Slow = double; springs scale stiffness inversely) drives Home's dock fade (300ms) and edit-mode shrink, the Quick Panel slide (220/180ms), and the panel slide (240ms). Reduce motion still wins over every speed. The Gestures & Motion hub gains the Off / Fast / Normal / Slow segmented control, plus a search entry |
| Profiles | done | Profiles were a working feature with no hub UI after the M5 categories were deleted; they are NOT dropped. A compact Profiles block now lives in Device & Data (next to Backup): switch active profile (auto-saves the profile left), duplicate, delete (deleting the active profile falls back to Default), save current home as a new profile, and the schedule display when a schedule is set |
| Snap toggle in Settings | done | Home & Dock hub gains "Snap to grid" (Smart Grid / Free Canvas) writing `placementMode` 0/1 (legacy 2 still reads as snap on, per `snapOn()`). Row copy states the home stays one continuous scroll in both modes — the row toggles snapping, not pages. The edit-toolbar Snap toggle writes the same key |
| Changes / Undo / Revert | done | Hubs write through typed setters and never record changes, so the host rebuilds the Changes list from the panel-open snapshot on every live-state change (`sessionDiffRecords`: one record per changed field, grouped where one control writes several, each with a revert writing that single field back to its snapshot value). Panel title-bar Changes chip, hub-screen footer count, per-item Revert, Revert session, and panel-open snapshot (Before) all flow from this list |
| REMOVED — 39 Not-wired keys + 2 placeholder actions (audit §3) | removed | All visible controls for: wallpaper-derived accent option (mode 3 now shows as Default in the Look hub), `font_weight`, `font_scale_pct`, `cols_tablet`/`rows_tablet`, `margin_dp`, `cell_spacing_dp`, `gap_dp`, `label_lines`, `label_color_argb`, `search_bar_pos`/`search_bar_style`/`search_bar_height_dp`, `today_page_on`/`today_page_content`, `status_bar_mode`, `wp_parallax`, `wp_blur_pct`, `wp_auto_dim`, `wp_per_page`, `dock_rows`, `dock_floating`, `dock_material`/`dock_opacity_pct`, `dock_magnify`, `dock_labels`, `drawer_cols`, `drawer_icon_dp`, `drawer_search_on`, `drawer_recents_on`, `drawer_suggestions_on`, `per_app_icon_json`, `folder_preview`, `folder_anim`, `folder_size_pct`, `folder_bg`, `folder_title_on`, `clock_page`, legacy `auto_add_apps` row, plus "Replay intro" and "Replay tutorial" placeholder actions. The store keys themselves are untouched (backup round-trip keeps them); only their dead UI is gone. Banned UI strings verified absent from final source: tier tags `[L]`/`[M]`/`[T]`/`[–]`, "stored only", "not yet driven", "Runs: None", "TODO" |
| REMOVED — old preview infra | removed | PreviewLive/PreviewMini/PreviewTiers/PreviewDemo/PreviewTestPads (tier tags, sample/mini homes, test pads, demo players) and the old `SettingsHub` search index (which indexed rows that did not exist, e.g. "Apply to system wallpaper"). The widget-frame controls (radius/background/opacity) are kept in Home & Dock as preview-only — the audit's partial status stands: no real home widget reads them yet |
| Partials carried into M6 | partial | Widget frame preview-only (above); scroll-home labels always show (the labels switch gates dock/grid icons only); grid size drives widget-sheet cells, not the scroll home itself (there is no grid to size); drawer horizontal-pages and A–Z scrubber styles stored but not rendered; icon teardrop/system shapes stored but not rendered; gesture sensitivity slider is nav-only (home gesture thresholds stay hardcoded); search history surfaces only in search-first drawer mode; profile-schedule switch does not auto-restore the previous profile |

## M7 — Home Screen v2, adapted (versionCode 7)

The home doc (HOME-SCREEN-V2-BUILDER-PROMPT.md) built ADAPTED to the
standing directions: Minimal-only (no presets, no preset carousel), no
pages (the scroll home from M4/M5 is the home; the doc's "page
overview" is the sections overview, its "Today page" is the Today
section/column), no "gems" metaphor anywhere in app strings or code
names, no new network calls (weather remains the one optional
exception, reused as in M6). The 8 feature workers' code landed
uncompiled; M7 integration (this section) fixed the compile errors,
wired the documented follow-ups, and shipped the verified vc7 build.
Nothing in M7 was redesigned at integration: every change below is a
fix or a documented wiring step.
| Area | Status | Notes |
|---|---|---|
| Home = scroll flow + size classes (§4, adapted) | done | One continuous scroll home in all size classes: Compact/Medium = TodaySection pinned above the flow (see gap below) + Favorites + Widgets & folders + A–Z; Expanded = Today column beside a centred flow (max ~880dp, ≥5% side breathing room), Today off centres the flow. No page dots, no page indicator — pages do not exist |
| One dock (§5.3, adapted) | done | OneDock is the one dock on every surface: floating frosted pill, pinned + recents + Search + All-apps, same content set everywhere. Long-press on the All-apps button now enters Calm (new optional `onLibraryLongPress` param, the only Dock.kt API change in M7) |
| Label legibility (§3, §10) | partial | `labelMode` ("auto"/"pill"/"off") is now wired from SettingsState into ScrollHome (`labelModeValue = state.labelMode`). PARTIAL: real per-cell wallpaper stats are not available yet, so "auto" still falls back to the mid-grey default — legibility improves only via the pill/off modes until stats land. See Known gaps |
| Snap (§4, §8) | done | ScrollHome and the edit-toolbar toggle now read `state.snapToGrid` (the M7 key). The hub toggle and `setPlacementMode` keep `snapToGrid` and `placementMode` in sync, so old and new readers agree; the home stays one continuous scroll in both modes |
| Library views + persistence (§6) | done | The Home host now opens `LibraryView.fromKey(state.libraryDefaultView)` and persists every view change with `setLibraryDefaultView(view.key)`. The legacy DrawerOverlay path still writes `drawerMode` only (kept as the signature-compatible legacy entry point; it has no live callers) |
| Search (§7, adapted) | done | Standalone Search card (dock Search button, "search" gesture) + Library inline field share SearchResultsList, so results are identical from either entry. Search overlap defect (§1.4) fixed by the single-scroll-container change from the M7 search worker; keyboard covering results is a device check (P4) |
| Sections overview (§8.3, adapted) | done/partial | Edit-mode button + pinch-in open the overview: reorder sections, hide a section, move apps between Favorites and A–Z. PARTIAL: overview changes are session-local via `onSectionsChanged` — no settings keys persist them yet (documented gap, not dropped) |
| Folders (§8, adapted) | done/partial | Folder panel, folder creation by drop-onto, "Move to home" strip. PARTIALS (documented, not attempted): panel scale origin is centre, not folder position; true finger-continuous drag from the folder panel into home rows is impossible (panel is a separate Dialog window) — the strip is the shipped path |
| Today + Smart Stack (§9, adapted) | done | TodaySection (Compact/Medium, above the flow) and TodayColumn (Expanded, beside the flow), weather from the existing feed's last-known value, calendar events, suggested apps, auto-rotate flag wired from `smartStackAutoRotate` |
| Calm mode (§2.6, adapted) | done/partial | Enter via Glance row, edit-toolbar Calm action, dock Library long-press, or `performGestureAction("calm")` (new case calling the existing `enterCalm` lambda; GESTURE_ACTIONS still has no "calm" entry, so no stored gesture value selects it yet). PARTIALS: "Choose apps" picker is a placeholder; background tap consumer not built (see Known gaps) |
| Gestures + motion + a11y (§8, §10) | done | Gesture dispatch unchanged except the "calm" case; motion-speed mapping (M6) untouched; HomeA11y helpers (focus ring, min touch target, keyboard moves, right-click menu, TalkBack actions) compile and are host-ready — TalkBack/keyboard behaviour itself is device-only (P8) |
| Session Undo / Before (§5, M6 model) | done | `commitSnapshot()` in SessionDiff.kt now also restores the 9 M7 keys (`labelMode`, `snapToGrid`, `dockRecentsOn`, `libraryDefaultView`, `todayOn`, `smartStackAutoRotate`, `categoryOverridesJson`, `calmFavoritesJson`, `calmScheduleJson`), so a full session restore no longer leaves an M7 edit behind. Per-key Revert entries for the new keys were NOT added (out of the integration scope; the full-restore path covers them) |
| §1 defects (home-doc §1, translated to the scroll home) | done/partial | See the M7 integration report: label legibility partly (stats gap above); dock fixed; tablet space fixed; search overlap fixed; stuck hint fixed; date line fixed; Today fixed as section/column; scrubber fixed; visual identity fixed via Minimal tokens |
| Compile integration fixes | done | Dock.kt: `awaitPointerEventScope` is a PointerInputScope member, not a top-level function — bad import deleted (its `isSecondaryPressed` import was valid). Drawer.kt: `DragAndDropTransferData` moved to `androidx.compose.ui.draganddrop`; BoxWithConstraints `maxHeight` captured into a local before the inner Box (BoxScope shadows it); drag source uses the legacy suspend `dragAndDropSource` with the `block =` parameter named (unnamed, overload resolution between the transfer-data and block forms is ambiguous). Home.kt: drag Event/Target imports moved to `androidx.compose.ui.draganddrop`; dock drop target rewritten on `toAndroidDragEvent()` + `mimeTypes()` (the Compose event has no clipData/clipDescription); local `DockMenuRow` lost its illegal `private`. HomeA11y.kt: `awaitPointerEventScope` import deleted; `sizeIn` imported and called unqualified; `LocalFocusManager` corrected to `androidx.compose.ui.platform` (not `.focus`); `isSecondaryPressed` import added. LibrarySearch.kt: split-by-predicate rewritten as an explicit scan (no such stdlib overload) |
| REMOVED — nothing new in M7 | — | No feature was removed in M7. Presets, templates and pages stay removed per the 2026-10-23 directions (see M5); the home doc's preset/page language was adapted, not implemented |
| Partials carried into M7 | partial | Label "auto" mid-grey fallback (no per-cell stats); sections-overview persistence session-local; Calm "Choose apps" placeholder; Calm background tap consumer missing; dock drop appends (platform drag gives no drop coordinates, so exact-position drop is impossible); widget frame still preview-only (M6 partial, unchanged); scroll-home labels always show (M6 partial, unchanged — the labels switch gates dock/grid icons only) |

## M8 — Horizontal paged home + real legibility stats (versionCode 8)

M8 restores the horizontal paged home (correcting the M5–M7
misreading: "remove pages" meant the ability to delete pages, never
removal of paging) and closes M7's legibility gap. Minimal-only +
accent stands: no presets, no preset carousel, no new network calls,
no new permissions. This section was written by Worker C (legibility
+ docs); the pager rows are M8 scope for the coordinator's
integration — only the legibility rows below are this worker's landed
code. Nothing in M8 is claimed built until the coordinator's build
and the Pad checks pass.
| Area | Status | Notes |
|---|---|---|
| Home = horizontal pager (§4, corrected) | in M8 (coordinator) | The home is a horizontally swiping pager again. Vertical scroll is NOT kept as an option unless the user later says so — horizontal is THE home |
| Page indicator | in M8 (coordinator) | Dots under the pager showing the current page; long-press on the indicator opens the page overview |
| Page overview | in M8 (coordinator) | Thumbnails of all pages: drag items between pages, reorder pages, add a page, hide a page, set a page as default. Replaces the M7 sections overview (see REMOVED) |
| Add Page + Remove Page | in M8 (coordinator) | Add Page appends an empty page. Remove Page: an empty page deletes freely; a non-empty page asks for confirmation, moves its icons to an adjacent page, and offers 5s undo; the last page cannot be deleted; a move that cannot land anywhere is refused rather than dropping items |
| Migration flow → pages | in M8 (coordinator) | One-time upgrade migration repacks the M7 scroll-flow order into pages in order (favorites, widgets, folders keep identity and payload); a page holds at most its grid capacity, overflow spills to the next page, new pages are created as needed. Nothing is lost; no item is reset (see ASSUMPTIONS.md M8) |
| Legibility Auto fed real per-cell stats | done (this worker) | `rememberLegibilityCells(wallpaperKey, bitmap, cols, rows)` in `ui/Legibility.kt` (the only M8 addition to that file; the pure core is untouched) holds a remembered `WallpaperLegibilityCache`, runs `analyzeWallpaper` on `Dispatchers.Default` when wallpaper identity / grid size / bitmap change, and returns `cols * rows` stats (index `row * cols + col`) or the mid-grey default while computing. The coordinator wires it once above the pager: `val cells = rememberLegibilityCells(wallpaperKey, bitmap, cols, rows)`, then per cell `labelStyleFor(labelModeFromKey(state.labelMode), cells[row * cols + col].luminance, cells[row * cols + col].variance)`. Bitmap source is `app.wallpaper.source` (HavenOS-mode bitmap); system mode passes null (see degradation note). Tests: `ui/LegibilityM8Test.kt` (boundary tests for the 0.19 switch, the mid-zone pill trap, variance threshold, cellStatsFor math, all three label modes, cache key semantics) — written, NOT run (sandbox test executor broken; see UNVERIFIED.md) |
| Parallax stats validity | done (this worker) | Stats are computed once for the current page grid over the full wallpaper; pager parallax shifts the wallpaper by ≤8% (under half a grid cell), so the stats stay valid across pages with no per-page resampling |
| System-wallpaper Auto degradation | known limit | System mode has no readable wallpaper bitmap (`WallpaperManager.getWallpaperFile` throws `SecurityException` on API 33+; probed in `WallpaperStore.probeSystem`), so Auto on a system wallpaper still degrades to the mid-grey default (near-black text, no pill) until a HavenOS wallpaper is used. No `WallpaperManager.set*` path exists or is added |
| REMOVED — ScrollHome | retired | The M4/M5/M7 one-continuous-scroll home is retired in M8: the paged home replaces it. Recorded explicitly so it is not mistaken for a kept option |
| REMOVED — sections overview | replaced | The M7 sections overview (session-local reorder/hide, Favorites ↔ A–Z moves) is replaced by the page overview. Its session-local persistence gap dies with it; page-overview state is the pager's, not the sections' |
| Partials carried into M8 | partial | Widget frame still preview-only (M6 partial, unchanged); Calm "Choose apps" still a placeholder; sections-overview persistence gap is moot (feature replaced, not fixed) |
## M9 — Docs 1–3 implementation: edit-mode behavior, home UI + dock, essentials + widgets + coordination (versionCode 9)

Docs 1–3 win over the Home v2 prompt where they conflict; the Settings v2
prompt stays valid for Settings. Settings panel UI visually FROZEN (layout,
visuals, copy, hubs, components, order unchanged) — only behavior behind
rows changed. Preset language adapted everywhere: there is NO preset
picker/carousel/switching — "preset" ≡ Minimal's tokens; "Modified" =
changed from Minimal defaults; "Reset to preset" = reset to Minimal
defaults; "Apply preset" = commit the draft (Minimal + user overrides).
The M6 Look hero card is the current-tokens summary + accent strip, not a
switcher. No new permissions, no new network calls beyond weather.

### Edit-mode behavior (Doc 1 §3)
| Area | Status | Notes |
|---|---|---|
| Toolbar (56dp, own reserved space) | done | ONE row, icons + short labels Expanded, icons only Compact; grid pushed down/scaled, never overlapped. Left Undo/Redo/Snap-to-grid (ON default, icon + tick); centre + Add (Widget · Page · Folder), Pages, Wallpaper; right Look (Settings at Look hub), Calm (opens Calm EDITOR, not Calm), Settings, Done; overflow Revert session · Restore layout… · Reset page. Every old toolbar action reachable |
| Context bar + selection | done with deviations | Floating bar appears only when selected: Resize · Remove · Move to page · Duplicate (widgets) · Item settings · Select more (+ Add to dock / Hide / Uninstall where they apply). Idle edit = +2dp shadow, NO outlines; selected = 2dp accent ring + corner handles only on this item. DEVIATION: bar sits under the toolbar in reserved space, not adjacent to the item (item geometry is not hoisted to the host); corner handles are visual only — resize runs through the context-bar Resize entry (dialog + chips + steppers) |
| Dragging + collision (§3.3) | done | Long-press 350ms (immediately in edit mode); haptic pickup + per-snap ticks; grid dots 30% + landing ghost + top zones Remove / App info-Uninstall; dock always a drop target (below-pager zone + host border highlight; full → red + "Dock is full"). Empty = place; on-icon held ≥400ms = folder, earlier = neighbors shift row by row; on-folder = add; widget on occupied = never overlap, red ghost, release → nearest free fit or back. Edge drag 600ms turns page + new page offered after last. DEVIATION: dock per-slot highlight not built (OneDock exposes no slot geometry); pager-drag-to-dock undo restores the grid item but the dock entry stays (dock membership not in the layout snapshot) |
| Snap | done with deviation | ON = nearest cell (default, labelled "Snap to grid" with icon + tick). DEVIATION: OFF = HALF-cell steps, not quarter-cell — the stored grid is half-cell units (DB v2); a quarter-cell position cannot be represented without a schema change |
| Resizing | done | Widgets + large folders only; size chips Small 2×2 / Wide 4×2 / Tall 2×4 / Large 4×4 + free resize inside provider limits; neighbors reflow live; impossible → red ghost + clamp ("No room for that size") |
| Adding | done | Add widget → sheet (Worker C), tap = first free spot (current → other pages → new page), drag = exact placement; Add page appends + scrolls to it; new folder = drop icon on icon OR context-bar action |
| Remove / Uninstall / Hide | done | Distinct with confirmation + Undo. Remove = home only; Uninstall = system confirm, user apps only; Hide = hidden list + layout + dock removal with undo restoring both. Folder with 1 app dissolves; empty folder deleted |
| Undo/redo | partly | 50-level stack (moves, resizes, adds, removals, page ops, folder changes); toolbar buttons + 5s snackbar after destructive actions; cleared on process end. PARTIAL: settings-value undo is NOT snapshotted — the shared stack object is the host's layout stack; the Settings panel keeps its existing Before/Changes path. Full settings-value integration needs panel-shell work |
| Pages overview | done | Thumbnails, tap to open, add, delete (confirm if not empty), hide, set default, move items between pages (picker dialog + drag thumbnail). Types: Home (cell grid) + Today (card flow). Today cannot be deleted, can be disabled in Settings |
| Enter/exit, hint, lock | done | Enter: long-press empty, icon menu Edit, Personalization/Settings route, gesture. Exit: Done; Back first clears selection, second exits; Esc exits. Changes live + saved as they happen; Done only leaves; Revert session restores entry snapshot. Hint chip "Drag to move · Tap to select" first 3 opens only, never covering icons. Lock layout blocks drag/resize/remove with "Layout is locked" toast |
| Tap correctness | done | In edit mode tap SELECTS, never launches (verified tap path; TP widgets shielded by a matchParentSize clickable overlay). Widgets render LIVE content under the selection ring, never name labels |

### Home UI + dock + Today page (Doc 2)
| Area | Status | Notes |
|---|---|---|
| Today = dedicated page (§4.3) | done | REPLACES M8's Today-column-beside-grid on page 1. Pager page 0 when todayOn; Expanded 2-column flow (left ~38% clamped 320–460dp, 24dp gutter, right fills); Medium/Compact single scrollable column; "Today" header; dock visible; indicator sun glyph; cannot be deleted; todayOn=false removes the page without touching its card list. Glance row shows on home pages when Today is not visible |
| Dock always visible | done with gaps | ONE OneDock, always composed on every home page, Today page, edit mode, and with Settings open. Bottom = centred floating pill, navigationBarsPadding + 16dp clear above gesture inset. Left/Right = vertical pill centred on that edge, max 80% height, same material + contents. Capacity computed (~72dp/slot, 48dp icons never shrunk): short → recents drop first, then pinned SCROLL inside the pill. Contents pinned · divider · ≤3 local recents · Search · Library. Tap launches, long-press menu, hover tooltip + 1.12 magnify, right-click menu. Size table enforced (bottom 76/72/64dp; side 80/76/64dp; Expanded max width 720dp). Icon-backdrop contrast checked light + dark (tonal tile, never white-on-light). GAPS: dimming under full-screen Library not explicitly implemented (dock stays composed under the overlay; dimming depends on the Library scrim, unverified); dock-internal drag reorder, drag-out-to-home, and folders in the dock NOT built (dock items are List<AppEntry> only — folders need a sealed DockItem type + persistence change; shipped instead: dock-menu Move left / Move right / Move to home, TalkBack/keyboard-safe); Settings-panel free-area re-layout NOT done (shrink Box stays full-size; the dock may sit partly under a tablet docked panel or phone bottom sheet — top follow-up) |
| Gold outline every cell (Doc 2 §2.1) | fixed, root-caused | PROVEN: GridCell's border branch was `dragNow?.overFolderId == folder?.id` — with no drag both sides are null and null == null is true → 2dp accent ring on every app icon, always. Fixed to require an active drag hovering that folder. Handle dot now renders ONLY on the selected item in edit mode (tap to select, then drag the handle) |
| Near-black cell rectangles (Doc 2 §2.2) | not reproduced | M8 code draws app cells as icon + badge + label only, no background. Remaining dark tiles are folder glass (spec-allowed) and widget surfaces — nothing to remove |
| Labels invisible (Doc 2 §2.3) | partly | M8 engine core sound, but host passes bitmap = null in system-wallpaper mode (OS bitmap unreadable API 33+), so Auto degrades to mid-grey (near-black text, no pill) on dark wallpapers. In scope fixes: PagerLabel + glance pills follow text colour (white text → black 0.35 pill; near-black → white 0.55 pill); glance lines resolve through glanceStyleFor on whole-wallpaper stats. Auto/Pill/Off + Labels On/Off exist. DEVIATION: label size stays the labelSize setting (default 11sp), not the Doc 2 token 12/13sp — the setting wins rather than being overridden |
| Grid clipping / indicator overlap (Doc 2 §2.6) | fixed | Grid fit by construction (clamped to content); real vectors were the missing nav inset (fixed) and the old 28dp overlay-indicator reserve (removed — grid uses the full safe area). Indicator is a layout row, never an overlay: 32dp row directly above the bottom dock (dots exactly 12dp above it) or bottom-centre of content area for side docks |
| Gold overuse (Doc 2 §2.9) | fixed | Drop-zone pills now tonal surface + accent border only when active; missing-icon fallbacks tonal + on-surface letter, not accent blocks. Accent only for interactive/selected: selected/target rings, focus, current page dot, primary "+" action. CORRECTION: Minimal's default accent is NOT gold in code (light 0xFF44586C slate, dark 0xFFB8C4D0 pale blue-grey; no hardcoded gold in ui/*.kt) — "gold" in captures is the user's chosen accent via palette.accent. The fix targets overuse, not hue |
| Layouts, glance, indicator | done | Expanded grid centred, max 880dp, ≥5% margins; side-dock content area reduced by dock inset. Glance row on home pages when Today is off ("Fri, 23 Oct · 28° Clear" + one event chip, legibility via glanceStyleFor, tap opens clock/calendar). Indicator: shared HomePageIndicator, 8dp dots, current elongated 24dp accent, sun glyph for Today, dot tap jumps, long-press opens overview, "+" chip (48dp, edit mode only) |
| Motion/haptics/a11y (Doc 2 §7–§8) | done, device-unverified | Parallax ≤8% (gated on parallaxOn && !reduceMotion); dock position change 240ms fade (100ms fade under reduced motion, instant at speed 0); dock press 0.94; page-turn haptic (one tick per settle); long-press haptic on dock icons; transform/opacity only in new additions. Pager announces "Page N of M"; arrows change pages, Tab moves focus, Enter opens, hover tooltips + right-click menus. 200% font-scale no-clipping and 4.5:1/3:1 contrast NOT device-verified |

### Widgets real (Doc 3 §2)
| Area | Status | Notes |
|---|---|---|
| One frame, all states | done | Frame 28dp radius, Look material, 16dp padding, optional title row. WidgetState + resolveWidgetState (removed → permission → error → offline-with-cache → loading → empty → ready). Loading = skeleton in frame; Empty = message + one action; Needs-permission = inline chip → rationale → system prompt → live update on return; Error = "Couldn't load · Retry"; Offline = cached + "Updated HH:MM"; Provider removed = "App removed · Remove widget"; TP failure = "Problem loading widget · Tap to reload", never a crash. PARTIAL: Settings widgetRadius row stays preview-only (pre-existing); the task fixes 28dp, so the row was not rewired |
| Weather | done | Locality only (shared localityOnly), formatTempDisplay in Settings units EVERYWHERE (widget, Clock-page, Glance — formatting never converts, which was the 28°/75° disagreement). 2×2 icon/temp/condition; 4×2 locality/temp/condition/hi-lo/hourly 4; 4×3+ adds 5-day strip. Refresh on first show, tap, Retry; stale >30min shows "Updated HH:MM"; error flow added. Location optional with one-sentence reason, fallback manually chosen city (search lists up to 5 matches); neither = Empty "Choose a city". Ellipsize at end of locality, never mid-word |
| Agenda | done | Per-calendar toggles from existing Settings list; 2×2 next event · 4×2 next 3 · 4×4 day view with time axis · all-day first; tap event → that event in calendar app, header → calendar; observer throttled 30s; midnight rollover via day key; Empty "No events today · Add event"; denied = Needs-permission. Seed: fresh layouts seed clock only (no Agenda); deduplicateSeededAgendas keeps first default-payload Agenda, drops further ones (glance/tomorrow variants untouched); host calls it once on launch (integration) |
| Clock / Suggested / Battery / Quick search | done | Clock: 12/24h from system, digital/analog/flip/editorial/serif, date, optional second zone (|tz:ZoneId payload suffix), next alarm, tap → clock app. Suggested: 4–8 apps, local history only, count + recency + same-hour boost, tap launches, long-press menu. Battery: level + charging; estimate OMITTED (no public Android API exposes time-remaining, none invented). Quick search: field opening the Search card (onOpenSearch passthrough wired at integration), no results inside |
| Third-party host | done with partial | Platform host, min size + resizeMode-derived chips, no extra polling; 28dp clip; frame background follows existing Home & Dock row; uninstalled = placeholder with working Remove; pause-on-screen-off via existing MainActivity stopListening(). PARTIAL: concurrency limit is the single shared AppWidgetHost with views only for pager-rendered items — no per-host update-cap API exists |
| Add-widget sheet | done | HavenOS widgets first, then installed apps, searchable; ≥600dp two-pane + size chips (provider-allowed only) + live previews (built-ins render real data); narrow = preview under list. Tap Add = first free spot (current → other pages → new page) via host addWidgetAtFirstFree; long-press own-widget row = onDragWidget exact-placement seam (home ghost belongs to the drag layer); bind/configure = system flow, cancel = nothing added, allocated id cleaned up |
| Placement/sizing, stacks, Today flow | done | Whole cells everywhere (picker's old half-cell spans fixed); findSpotForSizes row-major + try-smaller-sizes; clampSpan for impossible resizes. Stacks (P1): same-size drop = stack; vertical swipe cycles, dots right edge, time rotation (morning → weather/agenda, evening → suggested only — media card NOT built, it is LATER), paused 10s after touch; Edit-stack dialog (reorder/remove/unstack; unstack disabled unless every member fits). Stack sync across pages NOT built (LATER). Today flow: TodayFlowCard (same widget objects + frame, no cell geometry), todayFlowColumns (Expanded 2, else 1), todayCardSizes, reorderTodayCards — shell owned by Worker B, content + flow contract here. Today card drag-reorder NOT built (edit-mode drag area) — shell renders fixed Doc 2 §4.3 order |
| Blank-widget defects (Doc 2 §2.4) | fixed | Paged home grid cell now renders live WidgetContent instead of a labelled card ("Weather"/"Agenda"). Duplicate Agenda addressed as above |

### Profiles, safety, coordination (Doc 1 §5.10/§7, Doc 3 §3–§5)
| Area | Status | Notes |
|---|---|---|
| Layout profiles | done | One canonical ordered list per page + one profile per window class (Expanded landscape, Medium, Compact). First use derives by reflow from canonical order; afterwards the profile keeps its own positions. Add/remove app updates EVERY profile. "Sync layouts" overflow action re-derives others from current with preview. Coordinates are half-cell units. API in docs/M9-WORKER-D-STATE-LAYER.md |
| Transactions + single source of truth | done | LayoutState (pages, items, spans, positions per profile, dock, Today cards), SettingsState, LookTokens (derived Minimal-only), UiState (derived, never stored). UI reads state, sends events; only the state layer writes. One drag/drop = one atomic Room write (IO thread, off composition). Slider writes debounced 200ms. No whole-home relayout for one move |
| Event flows 1–12 (Doc 3 §3.3) | done as pure functions + named host calls | Icon-size, dock-position, drag-to-dock, add-widget, wallpaper (accent SUGGESTION chip only, never auto-applied), apply-look (draft → commit → Modified cleared → Changes reset), app installed, app uninstalled, rotation/split, Calm enter/exit, permission granted/denied, default-launcher transition — all implemented as pure functions or host calls, documented with test status in M9-WORKER-D-STATE-LAYER.md. During a drag, Settings changes queue until drop; conflicting transactions validate against current state, rejected with short toast, never corrupt layout |
| Errors | done | Widget failure contained to frame; corrupt layout record skipped + kept rest + logged in Diagnostics (filesDir/diagnostics_layout.txt) + one-time toast, never crash on start; invalid backup import changes nothing + reason; out of storage → keep in-memory + 5s retry + "Couldn't save" |
| Snapshots + Restore | done with partial | Auto-snapshot on edit-mode exit (integration: called from toolbar Done + Back-handler exits), at most once/hour, last 10 + one/day for 7 days, local only, no secrets. Restore: edit-mode overflow ⋮ → "Restore layout…" now lists D's snapshots (date + page/item/folder counts); selecting one restores atomically (current layout pushed to Undo first; corrupt payload → no change). PARTIAL: visual home preview before applying NOT built — the list shows counts only; the old single-snapshot (undoJson) dialog was replaced by this list |
| App lifecycle, pin requests, cold start | done | Install/update/uninstall/enable/disable/work-profile handled in layout, folders, dock, hidden list, recents; new apps → Library + optionally first free home slot (setting); updated icons refresh at read time. Pin requests: placement function (current page first free spot); host wires the system dialog. Cold start: cachedLayout Room-cache flow; widgets fill in after; never seeds over cache |
| Backup/restore, schema | done | Backup v1→v2 (adds layoutProfiles + calmEntryJson); v1 backups still import, unsupported versions change nothing. Room 3→4, additive-only migration (layout_profiles + layout_snapshots tables; HomeItem/Folder columns untouched, existing layouts survive verbatim). New Settings key: calmEntryJson (default ""); editHintOpens (Worker A). No other keys changed |
| Fresh-install defaults | done | Dock pinned from resolved system intents (Phone, Messages, Browser, Camera, Settings/Gallery) from installed apps, never hard-coded packages (hard-coded dockPicks kept only as fallback when intent resolution returns nothing). Page 1 = Weather or Clock widget + most-used apps after first run + folders only if user creates them, no duplicate seeds; Today on with Clock+weather, Agenda (one), Suggested; Labels On + Auto; Icon size Auto; Grid Density; Mode Auto |
| Permissions map | done | Point-of-use only, one-sentence reason first; denied → source toggle disabled with reason / Needs-permission states (never request before feature use) |
| Calm editor | done | Real editor (CalmEditorScreen): picker cap 8, up/down reorder (TalkBack/keyboard usable), schedule day chips + 15-min sliders, entry options; saving writes only calmFavoritesJson / calmScheduleJson / calmEntryJson; Calm never changes layout. Replaces M8 "Choose apps" placeholder. Integration: toolbar Calm button opens this screen (Worker A's minimal CalmEditorDialog call site removed to avoid a double dialog). Calm editor uses up/down reorder, not drag reorder |
| Settings behaviors (frozen UI) | done | Icon size Auto (f(cell,label,density); grid/rotation changes it) vs manual (fixed dp per profile; clamp + "Max for this grid" note; touch target ≥48dp); Grid Density/4×6/5×6/Custom (shrinking shows overflow ghost + "N items will move to page N" + Apply/Cancel, nothing committed until Apply; widgets shrink-or-move with summary; folders keep contents); Mode Light/Dark/Auto (200ms cross-fade; scrim + label colors re-derive); Labels On/Off (Off removes label space + re-centres icons); Dock position (insets, indicator placement, 240ms animation, Compact side dock ≤5 slots). Hub contracts per Doc 1 §4 table; Before (hold = snapshot at panel/edit open); Changes (differences from Minimal defaults + snapshot, each with revert); Undo shared across Settings + edit mode (layout stack; settings-value undo partial, see above). Default-launcher: banner stays while not default; once default, banner disappears at once, home button → page 1 + closes overlays. Wallpaper pipeline: palette → accent suggestion chip (NOT auto-applied) → legibility recompute → scrim recompute → Calm dim recompute. Precedence table (Doc 1 §6) implemented |
| Motion/haptics/a11y (state side) | done | Reduced motion overrides speed; TalkBack/keyboard actions exposed (a11yActionsForItem); dock position 240ms; press 0.94; haptics pickup/snap/drop/page-turn/long-press |

### Doc 3 §1 essential features
| # | Feature | Status |
|---|---|---|
| 1 Default-launcher flow | done | Banner + Make default → system chooser; once default, banner disappears at once, home button → page 1 + closes overlays |
| 2 Persistent dock | done with gaps | Always visible incl. edit mode + Today + Settings open; Bottom/Left/Right; gaps: Library dimming unverified, dock drag reorder/drag-out/folders-in-dock not built (menu Move actions instead) |
| 3 Pages and grid | done | Cell grid per size class, add/remove/reorder/hide pages, default page, page overview |
| 4 Icons with legible labels | partly | Normalized icons, icon packs, Auto/Pill/Off + Labels On/Off; system-wallpaper Auto still degrades to mid-grey (OS bitmap unreadable API 33+) |
| 5 Folders | done | Create, rename, reorder, drag in/out, dissolve at 1 app, delete when empty, large 2×2 |
| 6 Library | done | All + List (+ Categories), scrubber, search field, drag to home |
| 7 Search | done | Apps, contacts (permission), HavenOS settings (live inline), calculator, web; one scroll container, no overlapping rows, keyboard-safe |
| 8 Edit mode | done with deviations | Move, resize, add, remove, undo, snap per Doc 1 §3; deviations: snap OFF half-cell, context bar under toolbar, handles visual only, dock per-slot highlight missing |
| 9 Widgets host | done | Built-in + third-party (§2 above) |
| 10 App lifecycle | done | Install/update/uninstall/enable/disable/work-profile in layout, folders, dock, hidden, recents |
| 11 Pin requests | done | Placement function; host wires system dialog |
| 12 Wallpaper | done | System + HavenOS; per-page pipeline; legibility pipeline (system-bitmap limit noted) |
| 13 Settings panel live preview | done | Panel over live home; behaviors per Doc 1; UI frozen |
| 14 Backup and restore | done | v1→v2, migration-safe, old backups import |
| 15 Layout safety | done with partial | Auto-snapshots + Restore list (visual preview missing); lock layout |
| 16 Cold-start speed | done | Saved layout from Room cache immediately; widgets fill in after |
| 17 Rotation and size changes | done | Per-profile layouts; first-use reflow derivation |
| 18 Accessibility | partly | TalkBack actions, keyboard, mouse, pager announcements; 200% font-scale no-clipping + contrast NOT device-verified |
| 19 Notification badges | done | Dots/counts, notification access gated, no reserved space when denied (pre-existing) |
| 20 Icon quick actions | done | Long-press menu with app shortcuts (max 4); swipe-up on icon (pre-existing) |
| 21 Today page | done | Dedicated card-flow page (Clock+weather, Agenda, Suggested, Battery) |
| 22 Suggested apps and recents | done | Local launch counts + recency; clearable |
| 23 Library Categories | done | Auto-categories with local overrides |
| 24 Hidden apps with biometric lock | done | Hidden from layout, Library, search (pre-existing) |
| 25 Calm mode | done | Large-text favorites list, schedule, exit restores exactly; editor now real |
| 26 Gestures | done | Wired actions only + Test real (pre-existing M3/M6) |
| 27 Page overview | done | Reorder, move items between pages, add/delete/hide |
| 28 Large folders | done | 2×2 with 4 direct icons |
| LATER (not built) | not built, by direction | Themed monochrome/tint modes, split-view + app pairs, media card, widget-stack sync across pages, shared layout between devices |

### Known gaps carried into M9 (not silent)
- Settings-panel free-area re-layout NOT done: the shrink Box stays full-size, so the dock may sit partly under a tablet docked panel (~42%) or a phone bottom sheet. Top follow-up.
- Home.kt contains the home composition TWICE inside HavenRoot (two PagedHome call sites, two ScrollArea definitions); Worker B mirrored every host change into both trees instead of deleting tree 1 while Worker A refactored. Deleting tree 1 is the follow-up, not done here (mirroring is a holding pattern; tree 1's panel-sheet free height would use a stale detent if panel sizing is ever added).
- Restore list shows counts, not a visual home preview.
- Settings-value undo not snapshotted (layout stack only).
- Dock drag reorder / drag-out / folders-in-dock not built (needs DockItem type + shared grid↔dock drag geometry).
- Today card drag-reorder not built (shell renders fixed Doc 2 §4.3 order).
- App-open 320ms / folder 280ms springs: app-open animation is OS-owned; folder animation lives in FolderSheet.kt (outside M9 files).
- Grid-cell 0.94 press scale: press handling lives inside the custom gesture detector (drag-adjacent, untouched); dock press scale is wired.
- Widget frame radius row stays preview-only; battery estimate omitted (no API); TP concurrency limit is the shared host.
- Label size stays the setting (default 11sp), not Doc 2's 12/13sp token.

## M10 — Four-zone shell + occupancy engine + bug audit (versionCode 10)

Source: UI-REDESIGN-DIRECTION.md + BUG-AUDIT-2026-10-23.md (recording
22:28 Oct 23). Audit observations treated as real until code proved
otherwise; audit root causes were hypotheses, verified in code first.

### Phase 1 — structural correctness
| Feature | UI path | Status | Verified how |
|---|---|---|---|
| Single home composition (the M9 duplicated Home.kt tree — two ScrollArea/DockArea/EditToolbarBlock compositions, double gestures, double wallpaper — deleted; 4771 → 3427 lines) | Home (all pages) | done | code review (brace scanner + single-definition greps) + release build |
| One occupancy engine (ui/Occupancy.kt: validateLayout, pushForPlacement, resizePlacement, reflowForGrid, gridChangePlan, hostGridCells) | Home (all layout writes) | done | code review; device checks M10-1–M10-4 |
| Host grid = pager grid (hostGridCells delegates to pagedGridCells; fixes host 4×7 vs pager Compact 5×6 divergence and the sizeClass-as-landscape conflation) | Home (all pages) | done | code review; device check M10-3 |
| Reflow safety net after every layout write (out-of-bounds/overlapping items move to nearest free spot, same page → later visible pages → new page; never onto a hidden page; one undo step; idempotent, no-op on valid layouts) | Home (all pages) | done | code review; device checks M10-1–M10-4 |
| Grid shrink actually moves items (Apply previously only wrote the override; the host reflow now performs the promised moves) + impact preview with destination pages ("Moves to: page 2 (2)") | Settings → Home & Dock → Grid | done | code review; device check M10-3 |
| Quick-tune grid writes both orientations (was portrait-only: tapping 4×6/5×6 on a landscape tablet changed nothing visible) | Settings hub screen → Quick tune → Grid | done | code review; device check M10-3 |
| Density levels Spacious (15) / Balanced (40, default) / Dense (75) / Custom (manual slider or any grid override); selecting a level clears grid overrides so it visibly applies (undo restores) | Quick panel → Density | done | code review; device check M10-3 |
| Persistent dock outside the pager (already host-level in vc9 — the audit's page-scoped-dock hypothesis was FALSE; kept + verified) + dock never under the Settings panel (free-area re-layout) | Home (all pages) + Settings panel | done | code review; device checks M10-2, M10-5 |
| Settings master-detail on tablets (hub rail 180dp + detail; hubs/rows/copy unchanged; panel widened to 46%/480–660dp; full-screen scrim dropped on tablets — home re-laid in the free area, nothing sits under the panel) | Settings (tablet landscape) | done | code review; device check M10-5 |
| One search system (dock button, search gesture, home affordance all open the same search card; drawer field shares SearchResultsList + ranking + settings search — an entry point, not a second system) | Dock Search / gesture / Library field | done | code review |
| Icon contrast (DockIconImage derived backdrop for non-adaptive icons already existed in vc9; adaptive icons carry their own background) | Dock | done | code review; device check M10-6 |
| Legibility unknown-wallpaper fallback (Auto with no per-cell stats now forces the pill instead of guessing bare white text; same for the glance line) | Home labels + glance | done | code review; device check M10-7 |
| One page-transition spec (homePageSpec, critically-damped spring; all 7 programmatic page changes use it; finger swipes keep the pager's native fling — documented exception) | Page changes | done | code review; device check M10-8 |
| Selection ring (already existed in vc9) + NAME in the context bar (new: single label, or "N items") | Edit mode → select an item | done | code review; device check M10-9 |
| Resize validity preview (fits here / nearest free spot / another page / no room) + another-page fallback on save (was "No room" toast only) | Edit mode → Resize | done | code review; device check M10-4 |
| Widget Replace (context bar, widgets only: same position/size when it fits, nearest free spot otherwise, never a silent drop; refuses an already-placed third-party id) | Edit mode → select a widget → Replace | done | code review; device check M10-4 |
| Level 1 quick customize bar (long-press empty space → Wallpaper, Layout, Widgets, Dock, Style, Home settings, Edit home; home stays visible; tap-outside/Back dismisses) | Home → long-press empty space | done | code review; device check M10-10 |
| Scrubber strip widened 40dp → 48dp (touch-target rule; letter bubble already existed) | Library (All/Categories views) | done | code review; device check M10-11 |
| Suggested-apps row air (4dp → 6dp vertical padding) | Today rail / Today page → Suggested | done | code review |
| Single live back handler (a dead duplicate registration deleted; onBackAction replaces, so the late block always won) | Back key | done | code review |
| HNK overlay (BUG-010): NOT HavenOS-drawn — no `HNK`/watermark code exists; it is wallpaper pixels. Per the M10 rule the wallpaper is untouched; readability is handled by local scrims + legibility (see above). No Identity-layer control built (it was conditional on HavenOS drawing the overlay) | — | not built (conditional rule) | code grep; device check M10-7 |

### Phase 2 — composition
| Feature | UI path | Status | Verified how |
|---|---|---|---|
| Today rail on Expanded landscape (30% rail + 70% workspace, dock outside pager at workspace bottom; reuses M9 Today card bodies; greeting + date, clock/weather, agenda, battery, suggested, Search/Add widget/Calm actions) | Home (tablet landscape, Today on) | done | code review; device checks M10-12, M10-13 |
| Medium collapsible Today strip (48dp header: date, weather, next event; expands to rail cards, capped at 45% height, scrollable; fade/slide per motion rules) | Home (tablet portrait / phone landscape, Today on) | done | code review; device check M10-12 |
| Compact keeps the M9 Today pager page | Home (phone) | done | code review |
| Minimal three surfaces (Canvas/Tonal/Elevated as the documented model; GlassSurface is the single Tonal treatment — its specular edge is Minimal's glass material, kept, not stripped) | Home / Today / menus / settings | partial | code review |
| Folder inner icons share the home icon treatment (AppCell: same iconDp + label rules as home cells) | Folder panel | done | code review |
| Folder preview modes (Compact grid / Stack / Large folder) | Folder icon | not built | — (single 3×3 grid preview kept; modes are a follow-up, not silently dropped) |
| "Send to Today" as a widget-placement destination | Widget placement | not built | — (Today rail has no widget slots on tablet; Today page on phones already accepts widgets via existing flows) |

### Phase 3/4 — customization, motion, resilience
| Feature | UI path | Status | Verified how |
|---|---|---|---|
| M9 toolbar actions mapped, none dropped (Undo/Redo, Snap, Add Widget/Page/Folder, Pages, Wallpaper, Look, Calm, Settings, Done, Revert session, Restore layout, Reset page + context bar: Resize, Remove, Move to page, Duplicate, Replace (new), Item settings, Select more, Add to dock, Hide, Uninstall) | Edit mode | done | code review |
| Destructive changes show impact before commit (grid-shrink confirm with destinations; page delete confirm; remove confirm; layout undo restores the complete prior layout) | Edit mode / Grid / Pages | done | code review; device checks M10-3, M10-4 |
| Section reset never touches unrelated sections (existing per-section Reset + snapshot restore, unchanged) | Settings → Changes / Restore | done | code review |
| Calm mode kept + verified (overlay over unchanged home, exact restore on exit, real editor + schedule from M9) | Edit mode toolbar → Calm | done | code review; device check M10-14 |
| Reduced motion = fades + state emphasis (100ms fades via homeFadeSpec/homeSpringSpec; pager parallax gated on !reduceMotion; window blur gated on !reduceEffects; Flat tier falls back to translucent) | System-wide | done | code review; device check M10-8 |
| ≥48dp targets (context bar buttons, rail actions, strip header, scrubber strip, rail rows) + icon-only actions labelled (DockActionButton contentDescription + hover tooltip) | Home / Library / Settings | done | code review |
| Labels single-line with ellipsis (visible truncation, never silent clipping); two-line label reserve | Home labels | partial | code review (two-line reserve not built) |

## v2 M1 — API integration (versionCode 11, versionName 2.0)

### Rule change: offline-only is LIFTED for v2

The v2.1 prompt §0 rule ("only network use is optional weather") and the
"fully offline" stance are SUPERSEDED for v2, by user direction
2026-10-26. APIs are now in scope. What does NOT change: no ads, no
accounts, no analytics/telemetry, no cloud sync; every API is optional
behind its own toggle; the only network use beyond weather is the
batch-1 list below. Weather (Open-Meteo) remains as-is — same provider,
same host allowlist, same network toggle; it is not reworked in v2 M1.

v2 M1 scope, as built: network foundation (single client, allowlist,
cache, workers) + Today cards + search rows. Nothing in v2 M1 is
device-verified and no live fetch was made from this sandbox — the rows
below are documented from a code read of the landed M1 files; Pad checks
for the vc11 build are owed.

### Batch-1 APIs

| API | Host (allowlisted) | Auth | Endpoint / shape | Cache | Limits / honesty notes |
|---|---|---|---|---|---|
| Quotable | api.quotable.io | none | GET /quotes/random — returns an ARRAY (the /random endpoint is deprecated); no daily-quote endpoint exists, so "daily" is client-side behaviour: fetch once, cache 24h | 24h | Live shape UNCONFIRMED from this sandbox: the first fetch may fail, in which case the card shows an honest offline state. No quotes are bundled and none are invented |
| SportScore | sportscore.com | none | GET /api/widget/matches/ — cricket + football | 15min | ~10,000/day/IP (provider free-tier figure; the code does not enforce it, it only backs off on 429). Free tier requires visible "Powered by SportScore" attribution — rendered on the card in every state, tap opens sportscore.com. No betting content is rendered anywhere on the card |
| CoinGecko | api.coingecko.com | KEY-GATED | GET /api/v3/simple/price, free Demo key via the `x-cg-demo-api-key` header | 30min | Demo 30/min + 10,000 credits/month (provider figure; not enforced in code). The key is user-supplied in Settings → APIs, never hardcoded, never shipped: the card is off / no-fetch without a key, and shows an honest key-needed state ("Add your free CoinGecko Demo key in Settings → APIs to see prices"). Crypto defaults OFF |
| NASA APOD | api.nasa.gov | api_key required | GET /planetary/apod | 24h (daily) | Default key = public DEMO_KEY — a published demo identifier, not a secret — with shared limits 30/hr + 50/day per IP. An optional personal-key field in Settings → APIs overrides it. media_type=video shows the thumbnail only (thumbs=true). "Set as HavenOS wallpaper" is HavenOS-mode only, never the system wallpaper |
| GitHub public REST | api.github.com | none for public data | GET /users/{username}/repos + /users/{username}/events/public | 1h | Unauthenticated 60/hr/IP (provider figure; not enforced in code). Public data only — never private data, never any token. Events are labelled "recent activity (not live)" (docs latency 30s–6h). Username field in Settings → APIs; blank falls back to `Voidwalker-28` in code |
| currency-api | cdn.jsdelivr.net (primary) + latest.currency-api.pages.dev (fallback) | none | GET /v1/currencies/{base}.json; response is nested under the lowercase base code (`{date, <base>: {…}}`) — there is NO "rates" object | 24h | No limits quoted. DAILY rates only — never presented as live; the search row shows "Rates dated {date}". Search row for "<amount> <CODE> to\|in <CODE>" (e.g. "100 USD to INR"); tap copies the conversion line |
| Free Dictionary | api.dictionaryapi.dev | none | GET /api/v2/entries/en/{word}, on-demand (no worker) | cached by word, never treated as stale | No rate limit quoted (none verified). 404 = silent "no definition": the row simply does not render — normal, not an error. Only a single letters-only word qualifies as a lookup, and only when it has no app matches |

### Shared rules (all batch-1 APIs)

| Rule | Status | Notes |
|---|---|---|
| Per-API toggles in the new Settings APIs hub | done | One toggle per API (Quote of the day, Live scores, Crypto prices, NASA picture of the day, GitHub activity, Currency rates, Dictionary) plus three fields: CoinGecko Demo key, NASA personal key, GitHub username. A toggle gates BOTH its worker and its card/row: toggling off stops network use without cancelling/re-enqueuing work |
| Cache-first UI with "last updated / offline" states | done | Every Today card carries a status line: last-updated time when fresh, offline-with-cache when stale, honest offline/empty when there is no cache and no network. No card ever shows a spinner or placeholder data. Failed fetches never retry-storm: the worker returns success and the next scheduled run retries |
| Host allowlist enforced in code per API | done | `ApiHosts` in ApiClient.kt: exact host match, HTTPS only, checked BEFORE any connection is opened. Currency is the only API with two hosts (primary + fallback) |
| No new permissions | done | v2 adds no permission: the manifest already declared INTERNET (plus the pre-existing calendar/contacts/wallpaper/alert-window permissions, none of which v2 touches) |
| No analytics/telemetry | done | None in the v2 network files or the APIs hub |
| No hardcoded keys | done | Keys come from DataStore via the caller. The single exception is NASA's DEMO_KEY, a published demo identifier, not a secret |
| Rate-limit figures | stated, not code-verified | The limits in the table above are the providers' published free-tier figures as recorded in the v2 plan. The code does not enforce them; it only treats HTTP 429 as "back off, keep showing cache" |

### Where it lands in UI

| Surface | Status | Notes |
|---|---|---|
| Today page + Today rail cards | done | Quote, Live scores, Crypto, APOD, GitHub cards via `TodayApiCards` (Today page + rail); crypto card shows the key-needed state when no Demo key is set |
| Search rows | done | Currency conversion row + Dictionary row in the Library/search results, each gated by its API toggle |
| Settings → APIs hub | done | Seventh hub (hub screen + search entries); its panel Reset restores the 7 toggles only |
| Workers | done | One unique periodic WorkManager job per API (quote 24h, scores 15min, crypto 30min, APOD 24h, GitHub 1h, currency 24h; dictionary has none — on-demand only), network-connected constraint, no charging/idle constraints |

## Coordination fix (2026-10-29, 3.0/vc12 tree — verification build only, NOT a release)

Implements the seven-section coordination design document (§7 steps in
order). No version bump (stays 3.0/vc12); no publish; no GitHub touch.
Backup: `~/workspace/havenos-pre-coord-fix-2026-10-29/` (pre-fix tree).

### Step 1 — Profile scope in coordinator
- `commit` gains `targetProfiles` (default: current window class);
  `LayoutProfiles` gains `dirty: Set<WindowClass>` with JSON round-trip
  (old backup JSON without it reads as empty).
- Dirty reading (doc-literal would be vacuous for full-family tab
  commits, so implemented coherently): a grid commit's targets are its
  grid scope — portrait tab {COMPACT, MEDIUM}, landscape tab
  {EXPANDED_LANDSCAPE, MEDIUM}. The current profile's placements are
  rewritten from the live layout; other in-scope profiles are marked
  dirty for switch-time repair; non-targets untouched. Only grid
  commits set dirty flags; drag/dock/Today/install/uninstall/sync
  never do. Switch-time repair reflows (never re-derives) and clears
  the flag; a failed gate keeps it for retry. `deriveProfile` verbatim.

### Step 2 — Install/uninstall + Sync through commit(ALL)
- Sealed `LayoutMutation` (DragDrop, Resize, PlaceItem, RemoveItem,
  GridChange, DockPositionChange, TodayToggle, AppLifecycle,
  SyncFromCurrent, Restore) + `commitMutation`: lock gate (user
  mutations only — lifecycle/restore bypass it), drag gate (user
  mutations only — a dropped uninstall would leave a dead icon with
  no retry broadcast), apply-to-copy, validate + reflow safety net,
  one Room transaction, undo push. Removals never undo.
- New `withAppInstalled`: canonical ALWAYS gains the entry; placement
  ONLY to the current profile and ONLY when auto-add is on. Old
  `withAppAdded` (every profile) kept verbatim for its tests.
- App-listing receiver now routes add/remove/disable/enable through
  the coordinator; profile ids collected in both bare and `app:`
  forms. Deleted: `profilesWithAppAdded`/`profilesWithAppRemoved`/old
  `applySync(source, …)` (zero callers before deletion).
- Known scope fact: before this fix the profile system was dormant
  for rendering (host renders Room items directly; profiles had no
  writers/readers except Backup). Commit now writes profiles, but
  host render-from-profiles is NOT in §7 — follow-up, not silently
  dropped. Host drag/drop/dock/remove/hide write sites are likewise
  NOT routed (the §7 routing list covers install/Sync + grid/dock/
  Today only).

### Step 3 — Grid/density, dock position, Today toggle through commit
- Picker Apply → `GridChange` (writes override, existing semantics);
  Follow-density → `GridChange(followDensity = true)` (clears the
  override; the flag is defaulted so the doc's 3-arg form works).
  Dock position → `DockPositionChange` with NO gridCells (dock never
  changes `hostGridCells`, so reflow is a no-op by the doc's rule).
  Today → `TodayToggle` (placements untouched).
- Rejection plumbing: hub code ignores TxResult, so the public
  `commitMutation` wrapper raises `rejectionToast`, collected by the
  host toast — a locked-layout hub change no longer fails silently.
- Host reflow effect stays as a safety net only (still converges
  density-slider writes, see blocker below).
- BLOCKED, exact reason: density-level routing (Drawer/QuickPanel
  sliders) is NOT routed — the doc's sealed mutation list contains no
  DensityChange, and per-tick slider writes cannot each be a commit +
  undo entry. Inventing a mutation would violate "no new concepts".
  The hub's Follow-density action IS routed. SessionDiff
  settings-restores left direct (settings-domain restores).
- Known race (benign, device check watches it): a grid commit writes
  the override (settings flow emit) then the layout; the host effect
  may interleave first with the same deterministic plan, so results
  converge but one extra undo entry is possible.

### Step 4 — Settings UI batch (no state-model change)
- Look hub: three MaterialCards → one `Material` HubSegmented (same
  `setSurfaceMaterial` calls; blur-fallback note as the row's info);
  entire Wallpaper section → one collapsed `HubMore(title =
  "Wallpaper")` (inner "More options" block unchanged inside).
  `HubSegmented` gains a defaulted `value` param; `HubMore` a
  defaulted `title` param.
- Hub screen: Changes banner under the needs-attention banner
  (`{N} changes · Review · Revert session`, existing actions);
  footer count row deleted (title-bar ChangesChip already shows it).
- Grid overflow: one shared `MoveDestinationsText` for ghost text
  and confirm dialog — count line never collapsed; entries
  `page {pos} ({count})` in page order (pos = grid page + 1, or + 2
  while Today occupies pager position 1); ≤8 all shown, >8 first 8 +
  `+{M} more` expanding inline; dots cap (12) unchanged.
- Panel layering: edit toolbar gated on `!showSettingsPanel` AND a
  panel-open effect exits edit mode under Done semantics
  (auto-snapshot, then panel opens); phone scrim flat 40% (replaces
  adaptive 0.12–0.42); all 7 hub top rows removed — the panel title
  bar owns Back/Changes/Close (HubTopBar, DsTopBar,
  PrivacyHubHeader deleted; `onBack` params kept for the registry
  signature). This overrides the frozen-visuals rule on this one
  point: the newer design doc wins on conflict.
- Accent through `LocalDraftLayer`: selection writes draft only;
  Accent row value shows `Previewing` while draft differs; hero
  accent circle gains a dashed outline; panel close commits in one
  silent `setAccent`. Known interaction: the draft is one shared
  SettingsState copy — a grid Apply/Cancel or wallpaper commit that
  discards the draft also drops an accent preview, and a settings
  restore running while a draft is open can have its accent change
  overwritten by a stale draft commit on close (rare; documented,
  not redesigned — the doc specifies the shared layer).

### Step 5 — Durable undo (Room 5→6, additive only)
- Migration 5→6 (pinned by `migration56Sql()`): `undo_entries`
  (seq AUTOINCREMENT PK, direction, layoutJson, profilesJson),
  `profile_dirty` (windowClass PK), `api_cache.consecutiveFailures`
  INTEGER NOT NULL DEFAULT 0. Fresh installs get v6 from entities;
  v5 installs upgrade with layouts/profiles/snapshots/API cache
  untouched. No DDL+DML mix, no backfill (new tables start empty).
- `UndoFrame(layout, profiles)`; `UndoStack` generalized to frames
  (50-level FIFO, push clears redo, + `restore()` for rehydration).
  Full snapshot format: `snapshotJson` gains defaulted dock/hidden/
  todayCards/recents; `parseFullSnapshot` defaults missing keys to
  current (old snapshots restore layout-only, as before).
- Memory stack is authoritative in-session; Room is the restart
  backup. Commit pushes ride the layout transaction; host pushes
  capture synchronously then persist under a mutex (single order);
  undo/redo apply frame + mirror the stack move. Restart rehydrates
  stacks from rows (blank rows skipped); depth on restart is a
  prefix of the session's (a persist in flight at death is lost,
  never corrupt). Tail-order caveat: a commit insert racing a host
  push persist can reorder the backup's tail only — never live
  layout or in-session undo.
- Host rewired: `EditUndoStack` deleted; toolbar reads coordinator
  `undoDepth`/`redoDepth` flows; Hide uses generic `doUndo` (frames
  carry hidden, so the special case is gone); auto-snapshots use
  the full format; coordinator `restoreSnapshot` applies full state
  and CLEARS both stacks (doc §3 decision — this also retires the
  Restore dialog's pre-restore push, which was dead and racy once
  restore clears). Session-revert (`sessionSnapshot`) stays
  layout-only by design. NOT changed: DeviceDataHub's
  `applyLayoutSnapshot` restore/profile/import path (separate legacy
  path; it does not clear stacks — follow-up, not a silent change).

### Step 6 — Restore previews
- `snapshotPreviews()`: each record + its item ids in snapshot order
  (the payload the list previously ignored). Rows render a
  first-page thumbnail via the existing `PageThumbnail` (widened
  private → internal, inert tap/drag handlers; NO new renderer).
  Geometry comes from parsing the record (ids carry no coordinates);
  labels resolve against the live catalog (known app → live label,
  unknown → existing package-derived fallback letter, folders →
  snapshot names). Zero renderable items → counts-only row (never a
  blank tile). Apply path semantics unchanged (atomic restore);
  tile taps route to the same apply action (tiles consume presses).

### Step 7 — Worker failure surface
- `CardStatus` sealed (Fresh/Stale/Expired/EmptyOffline) + pure
  `cardStatus(api, row, nowMs)`: Fresh age ≤ maxAge, Stale maxAge <
  age < 2×maxAge, Expired age ≥ 2×maxAge (carries lastUpdatedMs),
  EmptyOffline row-less. Dictionary exempt by construction
  (maxAge Long.MAX_VALUE). Age ratio alone drives UI; the counter
  is diagnostics only.
- `refreshApi` (single place): success upserts fresh row (counter
  reset by default); failure keeps payload + timestamp and bumps
  ONLY `consecutiveFailures` (per row, so cricket/football and
  repos/events are independent); no row → no bump; toggle-off and
  no-key gates return before any touch, never counted as failures.
  Worker UNCHANGED (still returns success on failure; cadences and
  toggle gating unchanged) — no retry storm.
- Status line: Fresh → last-updated label; Stale → offline-with-cache
  line; Expired → `Couldn't refresh — showing {lastUpdatedLabel} ·
  Retry` (text button in the status line only, calls
  `refreshApi(api, state)`, rendered only while the toggle is on;
  in-flight retry shows the refreshing state); null status (initial
  states) falls back to the old lines. Retry on all 5 Today cards
  (quote, scores, crypto, APOD, GitHub — GitHub combines both rows:
  Expired wins with the oldest time, then Stale, then Fresh).
  Drawer search rows gain `cardStatus` via the flow but their
  rendering is unchanged (they have no status line to extend).

### Verification (this fix)
- Mid-build after steps 1–3: BUILD SUCCESSFUL (12m 55s); APK
  verified (app.havenos.launcher, vc12/3.0, minSdk 26, targetSdk 36,
  CN=HavenOS cert match). Per-step compile checks after steps 4, 5,
  6, 7: each BUILD SUCCESSFUL (`:app:compileReleaseKotlin`).
- Tests written but NOT run (sandbox executor broken, pre-existing;
  SettingsShellTest has 10 pre-existing compile errors): see
  UNVERIFIED.md. Final full verification build: BUILD SUCCESSFUL in
  7m 28s (`build.sh`, 2026-10-29); APK
  `app/build/outputs/apk/release/app-release.apk`, 2,665,728 bytes,
  md5 `2a2688ec8c0398749465ca7c4d309722`, aapt vc12/3.0/minSdk 26/
  targetSdk 36, apksigner CN=HavenOS SHA-256
  `53bff2a09aedb516a8ee6193234a7aaf5d04ce901fb8931b799191b9845354a8`
  (keystore match). Verification build only — the delivered
  `HavenOS-vc12.apk` (2,632,960 bytes, md5
  `cbe84ad33c307823dfb600419d0e3bc8`) was left untouched. Build log
  saved as `docs/coord-final-build.log`.

## 3.1 Phase B — Haven Glass + accent system (2026-10-30, 3.0/vc12 tree — verification build only, NOT a release)

Phase B delivers the design system and the Look accent UI. It does NOT
restyle every hub, the drawer, cards, menus, toolbar, overview, Calm, or
the island — that is Phase C, which consumes this system. The only
surfaces changed beyond the system itself are the Look accent section
and the minimum content swaps the new icon/lint rules forced (emoji
glyphs become HavenIcon vectors; two hex literals become tokens).

### B1 — One entry point: ui/glass

- `ui/glass/` exposes `Modifier.glass(level)`, all components, and
  HavenTheme tokens (HavenSpacing, HavenRadii, HavenType, GlassLevel
  specs, AccentRoles). `LocalAccentRoles` is provided by HavenTheme in
  `theme/Looks.kt` (the glass module holds the pure token type; the
  theme holds the provided value — no second HavenTheme composable).
- `glass()` draws level tint + hairline only. It adds NO new blur
  mechanism: blur comes from the existing window blur-behind where the
  host set it (the same Partial-tier mechanism GlassSurface uses).
  GlassSurface itself is untouched (home is Phase D territory).
  Depth comes from blur/tint/hairline, never shadows: `glass()` draws
  no shadow.

### B2 — Materials

- Glass1 (cards/widget frames/grouped lists): dark white 7% + 12%
  hairline; light black 5% + 10% hairline; card 28dp, grouped-list 20dp.
- Glass2 (panels/sheets/drawer): dark white 12%, light white 70%,
  hairline 16%, 28dp top corners for sheets.
- Glass3 (menus/popovers/dialogs/bubbles): dark white 16%, hairline
  18%, menu 20dp, dialog 28dp. Glass3-light tint is an INFERENCE
  (prompt silent): white 85%.
- Legibility: text-carrying glass sits on ≥40% dark scrim (dark) /
  60% light scrim (light) for 4.5:1 on any wallpaper.
- Fallback (Reduce Effects, blur unavailable below API 31 or
  disabled, frame-time trouble): same layout, no blur, tint alpha +6
  points, never fully transparent text backgrounds.
- At most two blurred layers on screen: DOCUMENTED, NOT ENFORCED.
  No layer counter was built (a new mechanism, out of minimal scope);
  a third layer becomes a dialog or uses the fallback by convention.
  Phase G / device check B-15 covers the behaviour.

### B3 — Shape / spacing / type

- Radii: card 28, group 20, control 14, chip full, icon tile 12.
- Spacing: 4/8/12/16/24/32dp. Row height 56dp min (grows for two
  lines). Touch targets 48dp.
- Type: display 56 Light, hero numerals 34 Light, title 28 Regular,
  section label 12 Medium 0.6sp tracking in accent, row label 16, row
  value 14 muted, caption 12 muted; line height ~1.35.
- Labels wrap to two lines before ellipsizing; long-press on an
  ellipsized label shows the full text in a Glass3 popover.
- Exception (documented): HavenSegmented labels are maxLines=1 —
  segments are fixed-height and wrapping would break thumb geometry.
- Nothing in Settings truncates at font scale 1.0 on tablet; font
  scale to 1.3 is honored without clipping/overlap (device check).

### B4 — Icons: one outline family, zero emoji

- DEVIATION (honest): the icons are ORIGINAL outline vectors, NOT
  Material Symbols. The extended set is not a dependency, is not in
  the Gradle cache, and the build is offline, so no symbol font could
  be fetched; hand-authoring true Material Symbols paths from memory
  would be fabrication. Style matches the prompt (24dp viewport,
  ~1.7 stroke, round caps/joins, stroke Black, transparent fill, tint
  recolors at call sites).
- One `HavenIcon` enum maps every hub, row, permission, dock, and
  menu entry, plus weather kinds and media controls (new in Phase B:
  MediaPrevious/MediaNext for island skip buttons). Icon tile 36dp,
  12dp radius, accent-soft background.
- EVERY in-range emoji/pictograph is removed from the UI, verified by
  a code-point scan (0 hits in main Kotlin + res values): weather
  symbols -> WeatherKind + `weatherIcon()` vectors (data/Weather.kt
  now returns `Pair<String, WeatherKind>`); hub registry emojis ->
  HavenIcon hub entries; permission glyphs -> HavenIcon permission
  entries; island play/pause/skip/charging/bell -> vectors; edit
  toolbar Done/Calm/Settings/snap-tick/selection/dismiss -> vectors;
  drawer clear, icon-pack tick, wallpaper tick, GitHub star,
  Today-page sun, music placeholder -> vectors or dropped ticks.
- Unit tests (written, NOT run — sandbox executor broken):
  `ui/glass/EmojiScanTest.kt` scans main Kotlin + res values/drawable
  XML for U+1F300–U+1FAFF, U+2600–U+27BF, U+2B50, U+FE0F and fails
  listing offenders; `ui/glass/HexLiteralTest.kt` fails on
  `Color(0x…)` literals outside the token module.
- LEFT FOR PHASE C (documented, not silent): out-of-range geometric
  glyphs — edit toolbar ↩↪⊞, snap off-state ○, DevicePermRow ●/○
  dots and ⓘ help glyph, overlay ▣ in old comments. They are outside
  the test ranges and belong to the Phase C toolbar/menu restyle.
- Slightly lossy, documented: mascot emotes FOUND/CELEBRATE were a
  check mark and are now "!" / "!!" (expressions never coincide with
  HAPPY/SURPRISED "!"); the at-a-glance weather line is a plain
  String, so it drops its glyph entirely (label + temperature remain);
  the wallpaper "Solid ✓"/"Photo ✓" buttons drop the tick (selection
  already shows by border).

### B5 — Components (each: pressed ripple, focused accent ring, disabled dim + reason)

- GlassCard; GroupedSection (label + rows in ONE Glass1 container,
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
  Phase C wires it into every help button).

### B6 — Motion

- App open 320ms, folders 280ms, dock 240ms stay; hub transitions
  220ms shared axis; one spring for switch/segmented; haptic ticks on
  switch/step/snap. `havenDurationMs` precedence (B6 + A8 + §0
  "Reduce Motion always wins"): Reduce Motion -> 100ms fade; else
  Motion Off -> instant; else scaled via speedDuration. Thumb springs
  snap under Reduce Motion or Off (a thumb has no fade target).

### B7 — Accent system

- Sources as a segmented choice in Look -> Accent: Wallpaper
  (WallpaperManager.getWallpaperColors, API 27+, suggestion chip
  "Match wallpaper" only, NEVER auto-applied), System (Material You
  dynamic color, Android 12+, hidden below API 31), Curated (12 muted
  colors, prompt starting values + names kept: Ember #F0A35E, Clay
  #D98C7A, Rose #E58AA6, Orchid #C490E4, Iris #9B8CFF, Cobalt
  #6FA0FF, Sky #7CC4F2, Lagoon #5CCBC0, Sage #8FC9A0, Moss #B5CF7A,
  Sand #E3CFA0, Mono #E8E8EA), Custom (hue strip +
  lightness/saturation square in HSL, live preview on the Look hero,
  "Adjusted for readability" note when clamped, up to six recents,
  eyedropper sampling the wallpaper preview, hex entry under
  Advanced).
- One seed -> all roles via one function `accentRolesFor`
  (accent, onAccent, accentContainer with strength alpha,
  accentOutline; dark + light). No Material Color Utilities
  dependency (not present, offline): perceptual tone mapping +
  contrast clamp. Components read only these roles. Contrast enforced
  in code: accent text on glass ≥4.5:1, icons/borders ≥3:1, onAccent
  on accent ≥4.5:1; failing seeds clamped, never rejected.
- Strength Subtle/Balanced/Vivid (default Subtle; container alpha
  ~10%/18%/30%). Accent marks state + small section labels only,
  never fills large areas by default.
- Storage is DataStore, NOT a Room migration (migration-skill
  discipline applied: additive new preference keys, pure mapping,
  old keys kept for backup round-trip, never dropped). Load rule: new
  key present -> read new keys; else old keys present ->
  `migrateAccent(oldMode, oldArgb)`; else fresh install ->
  Curated/Ember. Old-key compat writes in the new setter (system maps
  to mode 1 / argb 0; new keys authoritative).
- Migration mapping (old 8 swatches -> nearest curated by redmean RGB
  distance, pinned in tests): blue -> Cobalt, teal -> Lagoon, purple
  -> Iris, pink -> Clay, amber -> Ember, green -> Lagoon, rust ->
  Clay, slate -> Clay. Mode 0 -> DEFAULT (visible accent unchanged);
  mode 1 + argb 0 -> DEFAULT; mode 2 + nonzero -> CUSTOM with colour
  kept + seeded into recents; mode 3 -> WALLPAPER (stored argb was
  never rendered, nothing visible to preserve).
- Changes list shows names (e.g. "Accent: Iris to Ember"), never raw
  ints (`accentDisplayName`). Backup gains the five new accent fields
  (unconditional write, `has()`-guarded read; old backups unaffected).
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
- Tests (written, NOT run): `ui/glass/AccentTokensTest.kt`
  (every curated color passes accent ≥4.5 + outline ≥3.0 + onAccent
  ≥4.5 in dark AND light; determinism; strength alphas; Sand-light
  clamps; Cobalt-dark needs no clamp; migration incl. the 8 pinned
  swatch mappings; display names; recents round-trip + cap 6; HSV
  round-trip within 1 per channel), `ui/glass/GlassTokensTest.kt`
  (tint/hairline alphas, fallback 0.06, scrims, radii, spacing).

### Purple Default launcher card — FIXED in Phase B (not grandfathered)

- The card was NOT a hex literal at all: HavenTheme never set
  primaryContainer/onPrimaryContainer/secondary/tertiary, so they
  fell back to Material3 baseline purple. HavenTheme now sets every
  container slot from accent roles (primary = roles.accent, onPrimary
  = roles.onAccent, primaryContainer = roles.accentContainer,
  onPrimaryContainer = palette.onSurface, secondary/tertiary +
  containers = accent roles). This fixes all four sites at once
  (DeviceDataHub card, DrawerSearchHub selected colour,
  GesturesMotionHub background, PageOverview background).

### Hex-literal fixes in Phase B

- Home.kt red badges (2x `Color(0xFFE53935)`) -> the HomeColors
  badge token (dark-aware: light red / dark light-red).
- Widgets.kt flip-tile `Color(0xFF17181C)` -> named token
  `FlipTileBackground` in HomeTokens.kt (allowlisted Tokens file).
- Wallpaper data tables (data/Wallpaper.kt) and LookHub wallpaper
  swatch lists stay as data, not composable literals — out of the
  lint test's scope by design (Phase C may tokenize them).
- SettingsComponents.kt hex-parse arithmetic `(0xFF000000L or
  parsed)` is not a `Color(0x…)` literal and is untouched.

### Verification (Phase B)

- Compile after accent wiring: BUILD SUCCESSFUL in 11m 11s
  (`./build.sh compile`) — after repairing the pre-compaction glass
  files, which had never been compiled (see PHASE-B.md: wrong Dialog
  import, Int-vs-Color roles, `cubicTo` vs PathBuilder `curveTo`,
  HavenIcon function/enum name collision renamed to
  HavenIconImage, BoxWithConstraintsScope maxWidth leak,
  DARK_GLASS_BG Long literal).
- Compile after emoji swaps: BUILD SUCCESSFUL in 11m 33s (after
  fixing 8 missing layout imports). Final full verification build:
  BUILD SUCCESSFUL in 34s (`build.sh`); APK
  `app/build/outputs/apk/release/app-release.apk`, 2,714,872 bytes,
  md5 `d17d18d696fc6acf83778cf21d5147eb`, aapt vc12/3.0/minSdk 26/
  targetSdk 36, apksigner CN=HavenOS SHA-256
  `53bff2a09aedb516a8ee6193234a7aaf5d04ce901fb8931b799191b9845354a8`
  (keystore match). Verification build only — NOT delivered anywhere.
  Full per-step record: `docs/3.1-phase-reports/PHASE-B.md`.
- Tests written but NOT run (sandbox executor broken, pre-existing;
  SettingsShellTest has 10 pre-existing compile errors): see
  UNVERIFIED.md. Final full verification build: see PHASE-B.md.
- No dependency added. No Room migration in Phase B. Signing,
  version (3.0/vc12), and `~/workspace/havenos-github/` untouched.
  Nothing published or uploaded.

## 3.1 Phase C — Haven Glass on every non-home surface (2026-10-30, 3.0/vc12 tree — verification build only, NOT a release)

Phase C applies the Phase B system to Settings, drawer/library/search,
Today online cards, menus/dialogs/snackbars, the edit toolbar +
context bar, page overview / widget picker / folders / page indicator,
Calm, and the island, in C1–C8 order. No behavior, ranking, source,
toggle, priority, or visibility rule changed except where noted below
(one genuine bug fix: the quick-customize Widgets entry). Per-part
verification builds ran green after C1, C2, C3, C4, C5, C6, and C7+C8;
the final build covers all of Phase C including the glyph cleanup.

### C1 — Settings

- Panel: legibility scrim + Glass2 tint + hairline (bare Glass2 tint
  alone is not legible over the live home without window blur, so the
  Phase B §B2 scrim rule is applied). Material setting no longer
  changes the panel surface; it still drives home glass tiers.
- Rail 260dp tablet, rows 48dp, 24dp outline icon in icon tile;
  selected row = accent-soft pill with accent icon + label; labels
  wrap to two lines, never truncate. Title bar: back chevron vector +
  one 28sp title; crumb only at depth ≥3 (already true from Phase A
  A8, so no repeated "Settings › Settings" crumb). Before = Eye icon
  + "Before" pill ("Hold to see your home before changes" as its
  TalkBack description); Changes (N) second pill; Close stays.
- Hub screen: filled glass search field (copy unchanged);
  needs-attention banners as HavenBanner; hero = MiniLookPreview, a
  tokens-only live miniature (mini 3×2 grid in the current icon
  shape with one accent tile, dock pill in accentContainer, mode ·
  material · grid line) — no fallback to the Minimal card was needed.
  Quick tune = one grouped section (Icon size, Grid incl. both-
  orientations write, Mode, Dock position, Labels); Quick tune Mode
  gained a System option (4 options) because HavenSegmented always
  shows a selection and the old no-selection state for System mode
  would have lied. Hub list = one grouped section ("All settings")
  with HavenNavRow per hub (icon tile, title, live summary, chevron).
- Shared hub rows restyled (HubRow/HubSwitchRow/HubSegmented/
  HubSliderRow/HubMore/HubHexDialog); ShellRow/ShellSwitchRow/
  ShellSegmentedControl/ShellExpander/ShellSkeletonRow restyled;
  help popovers are HavenHelp everywhere (Privacy/DeviceData inline
  help expansions converted to popovers, same help text).
- Look hub: accent Source + curated swatch grid (names under
  swatches, selected ring), strength, mode, material, label mode,
  icon shape previews, wallpaper source/style — all present per
  Phase B; only the shared-row restyle applied.
- Home and Dock: Columns + Rows steppers added (48dp Minus/Plus
  vector buttons, coerced 3..12); Follow-density button can never
  wrap letter-by-letter (horizontal scroll + 148dp minimum width +
  no-wrap label). Grid-shrink preview = its own grouped section
  ("Grid change preview") with the dots row, a one-sentence move
  summary, and the expandable destination list (>8 only). Density
  itself is a 0..100 slider, not chips — the C1 density item is the
  Follows-density control. Shrink-confirm and follow-density
  dialogs are Glass3 custom dialogs matching the C4 anatomy
  (HavenDialog cannot host expandable lists).
- APIs: each toggle row carries a status line (Off / Needs key /
  Updated HH:MM / Offline / Busy / Couldn't load, from A2 classes +
  diagnostics); key fields (CoinGecko, NASA, GitHub username) in a
  grouped section with Paste/Show/Hide + validation (GitHub
  username rule: alphanumerics + hyphens, ≤39 chars, no leading/
  trailing hyphen). Toggles keep their ShellSwitchRow cards.
- Gestures and Motion: device diagram already token-only (no
  hardcoded purple found); toggles are HavenSwitch; Test buttons
  still fire the real dispatcher.
- Device and Data: permission rows with icon tile, status chip
  (Allowed/Not allowed), Manage; default-launcher banner as
  HavenBanner; Diagnostics keeps the A2 rows. The A13 restore row
  lives in Gestures and Motion, not here, so nothing was added.
- Privacy: restyled only. The AI Privacy Ledger entry was NOT built
  (Phase F scope, per prompt).
- Mascot hub: chrome restyled only; preview box + rows come in
  Phase E.
- Hub loading/empty/error states: DeviceDataHub has the import error
  dialog + diagnostics empty text; ApiHub degrades to missing status
  lines when diagnostics fail; SettingsPanel has the "Screen not
  available" state. Other hubs read local SettingsState/DataStore
  (always present, synchronous defaults) — no missing-data path
  exists, so no state was invented.
- Search results use the same rows + "No settings match" empty state.

### C2 — Drawer, Library, Search

- Sheet: scrim + Glass2 tint layered directly (no glass() border —
  the sheet is edge-to-edge). IME insets: search field stays
  visible, results resize, nothing overlaps. Sticky list-header wash
  deliberately left unchanged (a GLASS1 tint would not hide scrolling
  content under a sticky header).
- One compact toolbar row: HavenSegmented (All/Categories/List) +
  48dp sort button (cycles A–Z/Most used/Recent, same logic) + 48dp
  menu button (the pre-existing drawer-settings affordance, not a
  new dropdown). The custom LibrarySegmentedControl is deleted.
- Search field = Glass2 field (Search vector icon, Clear button);
  keyboard Search action already ran the search and is unchanged.
  A-Z rail keeps its 48dp hit area; the letter bubble is Glass3.
- Results grouped under section labels with icon tiles + match
  highlighting (new pure `matchSpans` in LibrarySearch.kt, kept
  framework-free for tests). Settings/contact rows keep text-only
  rows (they have no icons to tile); app rows carry the tiles +
  highlighting. Ranking/sources/toggles unchanged.
- Empty state: SAD MascotFigure 64dp + "No matches" + the same note
  copy; the Web fallback section below still renders.
- CategoriesView selection glyphs are RadioOn/RadioOff vectors.

### C3 — Today online cards

- One anatomy inside the existing frame: the frame KEEPS
  GlassSurface (Today is a home surface; GlassSurface is the
  sanctioned home blur mechanism and tint-only glass() would lose
  blur over wallpaper). Header = small accent label (no card has a
  header icon or overflow actions, so no fake slots were invented);
  footer = "Updated HH:MM · Source" on success paths only (A2 error/
  offline/empty wording untouched). Skeleton while loading; empty/
  offline/error states use a static SAD MascotFigure 48dp (E4 poses
  may refine it later).
- Scores: Cricket/Football HavenSegmented, A1 rows, at most 4 +
  Show more/Show less. Quote 18sp regular + author caption.
  APOD: rounded thumbnail, title, Set-as-HavenOS-wallpaper
  (HavenOS wallpapers only, as before). Crypto: symbol, price, 24h
  change with arrow icon + sign, never colour alone. GitHub: repos
  + events with skeleton/empty branches, combined status unchanged.
- Currency/Dictionary are NOT Today cards (they belong to the
  search worker) — C3's "same anatomy" does not apply to them; their
  presentation follows the C2 search sections. Card order unchanged
  (quote, scores, crypto, APOD, GitHub). Toggle gating unchanged.

### C4 — Menus, dialogs, bubbles

- New shared pieces in ui/glass: HavenGlassDialog (Glass3 shell for
  field/list/row bodies HavenDialog cannot host) +
  HavenDialogFilledButton (accent, or danger red, optional enabled)
  + HavenMenuRow (48dp, 20dp icon, destructive = error tint + text).
  HavenMenu itself is unchanged (its container is palette.surface,
  the sanctioned Phase B component, not glass tint).
- Converted: all Settings dialogs (Gestures ×4, DrawerSearch ×2,
  HomeDock Weather, DeviceData ×4, ShellColorPickerDialog, Dialogs
  TextFieldDialog), drawer Library menu, home dock menu, PagedHome
  icon menu, CategoriesView ×2, WidgetReal2 Weather city dialog,
  Widgets ×5 (Countdown, New task, Note, Gallery photo, Edit stack),
  Home.kt confirmations (Cannot delete page, Remove from home,
  Move to page, Restore layout, Reset page, At-a-glance options,
  Resize, Icon picker), PreviewAids reset overflow menu (HavenMenu)
  + reset confirm (HavenDialog, danger), EditMode Calm editor
  (HavenGlassDialog) + EditUndoSnackbar (HavenSnackbar), SettingsPanel
  both snackbar hosts (HavenSnackbar with Undo action).
- Danger tint only for permanent/destructive actions (Uninstall,
  Remove-from-home dialog, Reset page, Delete page, Reset-this-
  screen). Undoable removals (Remove from home/dock menus, Hide
  app) are NOT danger-tinted. DatePickerDialog (platform date
  picker) is not a Haven dialog candidate and stays.
- BUG FIX (behavior, not styling): the quick-customize bar's
  WIDGET_SHEET branch was a lambda literal as a statement-when
  branch, which did nothing — tapping Widgets never opened the
  sheet. Fixed to `showWidgetSheet = true`.

### C5 — Edit toolbar + context bar

- 56dp toolbar, reserved space, every action kept. GlassSurface pill
  kept as the blur mechanism (tint-only glass() would lose blur over
  the home); Glass3 styling = icon + label buttons, accent-soft pill
  for the active tool (Snap), hairline dividers between Left
  (Undo/Redo/Snap), Center (Add/Pages/Wallpaper/Look/Calm/Settings),
  and Right (Done/overflow) groups. All glyphs cleared (↩↪⊞○▦▣◐⋮
  become Undo/Redo/Grid/Layers/Image/Eye/Menu vectors); Snap's
  off-state needs no marker — absence of the accent-soft background
  IS the off state. Add + overflow menus are HavenMenu (same
  actions, same order).
- Context bar: same pill, icon + label buttons (Resize Grid, Remove
  Close, Move to page ChevronRight, Duplicate Plus, Replace Apps,
  Item settings Gear, Select more Check, Add to dock Layers, Hide
  Eye, Uninstall Close danger-tinted). Selection ring untouched
  (2dp accent, PagedHome).

### C6 — Page overview, widget picker, folders, page indicator, app info

- Page overview sheet: scrim + Glass2 tint + hairline, 28dp
  (replaces the opaque material surface). Thumbnails: Glass1, 20dp
  (was surfaceVariant 16dp). Delete-page confirm already converted
  in C4.
- Widget picker: search field + size chips already existed (field
  filters own + provider widgets; chips pick from allowedCellSizes/
  builtinWidgetSizes) — C6 restyles them: sheet container takes the
  Glass2 tint, search field becomes a filled Glass2 field (Search
  icon + Clear). Previews unchanged.
- Folders: FolderSheet already uses GlassSurface (home blur
  mechanism over the live grid); tint-only glass() would lose blur,
  so no surface change — recorded as an adaptation, not a gap.
- Page indicator already matches spec (8dp dots + 24dp accent dash;
  Today dot stays the Phase B4 sun icon). No change.
- App info: no HavenOS app-info surface exists — the "App info"
  action opens the system app-info screen. Nothing to restyle; the
  menu row itself was converted in C4 (Info icon).

### C7 — Calm

- CalmMode overlay already satisfies the calmer variant (44sp
  clock, 30sp favorites, white 70% secondary text, black 62% scrim,
  no accent anywhere) and mutates no home state, so exit restores
  home exactly. No change to the overlay.
- CalmEditorScreen restyled: material Switch → HavenSwitch (×2),
  Slider → HavenSlider (×3, 15-minute snap preserved on From/To,
  drag bubble shows the formatted value). Accent appears ONLY for
  schedule state: the Schedule label is accent when enabled, and
  selected days are accent-soft pills (replacing "[label]"
  brackets). The ↑/↓ reorder glyphs are functional labels and stay.

### C8 — Island

- Pill keeps GlassSurface (home blur mechanism; GlassSurface takes
  no per-surface level — its tier comes from theme glass settings).
  The Glass3 treatment is the hairline divider (Glass3 hairline
  alpha) between stacked activities in the expanded panel.
  Behavior, priority, ordering, controls, and visibility rules
  (off by default; hidden in edit mode, drawer, search, Settings,
  quick panel, Calm) UNCHANGED. All colors already tokens; no
  hardcoded color found.

### Phase C BLOCKED / not built (with reasons, never silent)

- NONE of the C1–C8 items is unbuilt, except the two deliberate
  non-builds above (Privacy AI Ledger = Phase F; app-info surface
  does not exist). Adaptations (not blocks): panel/overview sheets
  add a legibility scrim over the Glass2 tint (bare tint is not
  legible over the live home); Today frames, FolderSheet, toolbar,
  context bar, and island keep GlassSurface (the home blur
  mechanism — tint-only glass() would lose blur); HavenMenu's
  container stays palette.surface (Phase B component as designed).
- Quick tune Mode System option: this session's deliberate fix (see
  C1 above); the parent may overrule it back to a 3-option control
  with a no-selection state.
- C5 Left/Center/Right group boundaries are this session's reading
  (the prompt names the groups but does not enumerate them).

### Verification (Phase C)

- Per-part builds: C1 BUILD SUCCESSFUL in 10m 19s; C2 in 11m 12s;
  C3 in 10m 56s; C4 in 10m 37s (after fixing 3 edit artifacts:
  orphan @Composable in Drawer, an extra brace in Home At-a-glance,
  a split doc/annotation in GlassComponents); C5 in 10m 54s; C6 in
  10m 35s; C7+C8 in 11m 1s. Final build (all of Phase C + glyph
  cleanup): see PHASE-C.md.
- Script scans (main Kotlin + res values): emoji 0 hits, hex-literal
  0 hits (outside the theme/glass/Tokens/Legibility allowlist),
  rendered leftover glyphs 0 after the final cleanup (the › crumb
  separator at depth ≥3 is spec'd, not a leftover).
- Tests written but NOT run (sandbox executor broken, pre-existing;
  SettingsShellTest has 10 pre-existing compile errors): see
  UNVERIFIED.md. No dependency added. No Room migration in Phase C.
  Signing, version (3.0/vc12), and ~/workspace/havenos-github/
  untouched. Nothing published or uploaded.

## 3.1 Phase D — Small home updates (2026-10-30, 3.0/vc12 tree — verification build only, NOT a release)

Scope is prompt §D only. Everything else on the home is unchanged:
no home-grid, icon-size, spacing, Today-structure, or glass-look
file was touched, and the mascot layer was not touched at all.

### D — Home search widget becomes a Glass1 card

- The search widget is `QuickSearchWidget` (ui/WidgetReal.kt, sole
  caller Widgets.kt ItemKind.QUICK_SEARCH). Its outer frame already
  was the home Glass1 realization: WidgetFrame is GlassSurface at
  28dp radius with 16dp padding — the same frame the Today cards
  use (TodayApiCards ApiCardFrame). What broke the match was the
  inner pill: a 50-radius Box with flat surface at 55% alpha, plus
  a "⌕" text glyph as the icon.
- Change (ui/WidgetReal.kt only, +2 imports, +9/−8 body lines):
  the inner pill Box is deleted and the row sits directly in the
  frame, so the widget presents as ONE Glass1 card, not a card
  containing a pill; the "⌕" glyph is replaced with
  HavenIconImage(HavenIcon.Search, null, muted, 20dp) per the
  Phase C drawer-search convention (null description: the adjacent
  "Search apps, settings…" text already labels the row for
  TalkBack). Copy, text style, and widget footprint are unchanged.
- Deliberate non-use of GlassCard: tint-only glass() loses blur
  over wallpaper (Phase C C3 adaptation), so on a home surface the
  Phase B Glass1 component is GlassSurface, not GlassCard. A
  GlassCard here would have made the widget flatter than the Today
  cards, not matching them.
- Side effect, behavior-compatible: the tap target is now the whole
  widget tile (clickable moved to the WidgetFrame modifier); before,
  the 16dp padding ring around the pill was not tappable. Edit-mode
  behavior is unchanged (no click in edit mode; the host owns drag).

### D — Dock Search and Library tiles (A7): verified, NOT rebuilt

- Still in place in both dock layouts (ui/Dock.kt): DockActionButton
  with contentDescription "Search"/"Library", vector drawables
  ic_search/ic_apps (no glyphs), 48dp target (DOCK_ICON_DP = 48),
  14dp tile with palette.surface + 1dp glassBorder — the same tile
  the app icons beside it use (DockAppIcon). No regression from
  Phase C (Phase C never touched DockActionButton; its icon work
  was glyph-to-vector replacement, and these buttons already use
  vectors). No change made. The stale "Phase B's Haven Glass tile
  replaces these literals" comment in DockActionButton is left
  untouched — comment-only, out of Phase D scope.

### D — A4, A5, A6 re-verified against the current tree (no regressions)

- A4 (page width + clip): HorizontalPager uses the default Fill
  page size, so each page equals the pager viewport width (the
  pager itself fills the ScrollArea, 0.70f beside the Today rail
  when the rail is on); every page Box is fillMaxSize with
  clipToBounds (ui/PagedHome.kt), which is what stops the
  parallax shift from leaking a neighbour sliver at rest. Phase C
  touched PagedHome only for the icon menu and glyph fixes.
- A5 (glance rule): `glanceVisible(todayVisible) = !todayVisible`
  (pure, ui/PagedHome.kt) gates the single GlanceRow, which lives
  only in the grid-page branch — the Today-page branch has no
  glance; Home.kt passes the raw toggle (rail and strip both count
  as Today visible). Each page's glance is inside its clipped page
  Box, so a neighbour page's glance cannot leak.
- A6 (rail inset): `railBottomInsetDp` (pure, ui/TodayRail.kt)
  returns dockHeight + 16 for bottom docks, 0 otherwise; the host
  passes dockThicknessFor(sizeClass, Bottom) + 16dp for bottom
  docks (ui/Home.kt) and the rail applies it as bottom padding
  INSIDE its verticalScroll Column, so the padding extends the
  scrollable extent and the last card scrolls fully clear. The
  extra 16dp in the rail's base padding is the rail's standard
  bottom spacing, present with or without a dock — not an A6
  deviation.

### D — Mascot overlap: no new overlap created

- Phase D changes no mascot code and no widget footprint: the
  search widget keeps its cells, size, and position; only its inner
  surface changes. The ledge-graph guarantee (mascot never sits on
  an icon or card content) remains Phase E's job; nothing here
  makes it harder (no new surface, no moved content).

### Phase D BLOCKED / not built (with reasons, never silent)

- NONE of the §D items is unbuilt. No §D change required touching
  the grid, icon sizes, spacing, Today structure, or glass look,
  so no piece had to be stopped.
- No new test file: the change adds no pure logic (a composable
  layout swap + icon swap inside one existing function), so per
  Ponytail there is nothing unit-testable to add; the existing
  A4/A5/A6 pure functions (pageWidth/glanceVisible/
  railBottomInsetDp) are covered by Phase A tests. Recorded here,
  not hidden.

### Verification (Phase D)

- Verification build: BUILD SUCCESSFUL in 10m 29s (build.sh);
  APK app/build/outputs/apk/release/app-release.apk, 2,731,256
  bytes (same size as the Phase C final by coincidence; md5
  differs), md5 15460f553aadfa10c1ef522cabbea26f, aapt package
  app.havenos.launcher versionCode 12 versionName 3.0 minSdk 26
  targetSdk 36, apksigner CN=HavenOS SHA-256 match (keystore
  match). Verification build only — NOT delivered anywhere,
  nothing published or uploaded.
- Diff review vs the pre-D snapshot: only ui/WidgetReal.kt differs
  in source (plus build outputs); the diff is exactly the intended
  change. Security grep over the changed file: no secrets, keys,
  tokens, or credentials. Glyph scan: no "⌕" remains anywhere in
  main Kotlin (Phase C's glyph list did not include ⌕, so this
  widget was the last rendered-glyph holdout).
- Tests: none added (see above); none run (sandbox executor
  broken, pre-existing; SettingsShellTest has 10 pre-existing
  compile errors). No dependency added. No Room migration.
  Signing, version (3.0/vc12), and ~/workspace/havenos-github/
  untouched.

## 3.1 Phase E — Pixel mascot rebuild (2026-10-30, 3.0/vc12 tree — verification build only, NOT a release)

### E1 — Pixel art (Milo + Mila, original, 24x24 cells)

- One PNG sheet per character, 41 shared frames, transparent
  background, 1px dark outline, 8 colours each (outline, base,
  shade, highlight, eye, blush, accent-detail sentinel, white):
  app/src/main/res/drawable-nodpi/mascot_milo.png (984x24, 1950
  bytes; amber base) and mascot_mila.png (984x24, 2067 bytes;
  pink base). Milo squarer + tuft, Mila rounder + ribbon; no red
  cap, mustache, overalls, or borrowed assets/sounds. Frame
  layout: idle 0-3 (blink, 4fps), walk 4-9 (10fps), jump 10-14
  (rise, apex, fall, land x2, 8fps), sit 15-16, sleep 17-19 (1fps,
  Z rising), wave 20-23, happy 24-27, surprised 28-29, sad 30-31,
  think 32-34, point 35-36 (arm + arrow), poof 37-40 (cloud only
  on 39-40). Face right, mirrored for left.
- Atlas: app/src/main/res/raw/mascot_frames.json (version 1,
  cell 24, 41 frames, animations + fps). Loader
  ui/mascot/MascotSprites.kt palette-swaps the magenta sentinel
  pixels to the current accent at load, splits the sheet into
  41 per-frame bitmaps (no per-frame allocation at draw), and
  draws at integer scale (round(targetPx/24), minimum 2) with
  FilterQuality.None: Small 56dp / Medium 72dp / Large 96dp.
  DEFAULT_ATLAS in code is the fallback if the JSON fails to
  parse. Preview: docs/mascot/preview.png — all 41 frames at 4x
  on gray AND dark backgrounds, per character. Generator:
  docs/mascot/gen_sprites.py (Python PIL; PNG production
  succeeded, so no palette-indexed maps in code were needed).
- Old vector mascot KEPT behind the same interface behind
  USE_PIXEL_MASCOT = true in MascotSprites.kt (false restores
  MascotFigure everywhere: layer, hub preview, empty states).
  Not deleted until the new art is accepted.
- HONEST QUALITY NOTE: the agent cannot judge whether the art
  "looks good". Preview.png is committed for human review; only
  functional facts are claimed (distinct frames, correct
  layout, transparency, sentinel presence, verified by pixel
  counts: 39 body frames carry exactly 1 sentinel pixel each,
  poof frames 39-40 carry 0, frame 0 opaque pixels Milo 258 /
  Mila 259, frame 40 opaque pixels 6).

### E2 — Behavior engine (state machine, ledge graph, planner)

- PixelMascotState: Hidden, Spawning, Idle, Walking, Jumping,
  Falling, Sitting, Sleeping, Reacting, Carried, Pointing,
  Despawning; transitions as pure canTransitionTo (hide ALWAYS
  goes through Despawning/poof first, never straight to Hidden
  from a visible state). ui/mascot/MascotEngine.kt, pure Kotlin,
  no Compose/Android imports.
- Ledge graph from the LIVE layout: row tops span the full layer
  width (cutting yields empty-cell/gutter perches without grid
  geometry), plus widget tops (from live cell rects), the dock
  top (bottom dock only), empty rows above/below, and the Today
  rail top (from the rail container rect). Clearance rule:
  every segment is cut at overlaps with the band above it as
  tall as the sprite (strict overlap, so an obstacle bottom
  exactly at band top does NOT block), parts narrower than the
  sprite are dropped — the mascot never stands on or covers an
  icon. Edges: walk along one ledge; jump up to two icon-rows
  high (rise = 2x live row pitch, gap = 3x live cell width —
  never a hardcoded dp guess) between ledges whose ranges
  overlap or are within the gap; drop to any lower ledge with
  fall animation; no edges across pages while a page is
  swiping (graph keys on the settled page). Graph covers the
  visible page + Today rail + dock.
- Planner: weighted pick (DockTop 3x, EmptyCell/PageBottom 2x,
  else 1x), walk/jump at 40-60dp/s (seeded Random injected;
  tripSpeedDpPerSec), dwell 3-12s, sit after 60-120s idle, sleep
  after 5 min. Route execution is one shared helper
  (executeRoute) for planner trips and Find guidance, so the
  two never drift; a Find guide starting mid-trip aborts the
  planner route at the next step boundary. Graph rebuild keys:
  live cell rects, layer size, page, home items, dock side +
  thickness, rail rect, sprite scale, density — which covers
  every prompt trigger (layout version, rotation/size class,
  page, density/icon size, dock position, keyboard inset via
  layer size, Settings open/close via the host gate, edit mode
  via the host gate). Ledge loss: fall to the nearest valid
  ledge below, else poof and respawn on the dock top (or the
  layer-bottom fallback when there is no bottom dock).
- Visibility: home pages + Today rail only; hide with poof when
  edit mode, a drag, the drawer, search, Settings (EXCEPT Find
  pointing), quick panel, Calm, or home not resumed. Host poof
  handoff: gate closing while visible keeps the layer composed
  with hideRequested until the 700ms poof finishes, then
  unmounts; gate reopening mid-poof cancels it. Timers + frame
  loop pause when home is stopped (LaunchedEffect scope);
  timers restart (not resume-from-pause) on resume. Reduce
  Motion / battery saver / roaming-off: park on the dock top
  in static idle pose, taps still work; planner frozen during
  poof. Rendering: single overlay layer, touch-transparent
  except sprite hitbox + bubble; position via animateFloatAsState
  read ONLY in graphicsLayer lambdas (draw-phase invalidation,
  never recomposition); frame stepping time-based (timer delays
  at atlas fps, no per-frame callbacks); no per-frame
  allocations (frames pre-split at load).
- Long-press + drag: custom 350ms long-press detection
  (awaitFirstDown + withTimeoutOrNull; movement before the
  timeout is NOT our gesture and is ignored, never consumed).
  Carry: Carried state + haptic tick via LocalHapticFeedback
  (gated by pressFeedback/hapticStrength, never Vibrator) +
  drag loop consuming only post-long-press moves; drop snaps
  to a ledge within 48dp vertically and persists the home
  spot, else returns to the previous ledge; scope-cancel
  mid-carry restores the previous ledge (never hangs Carried).
  Short tap toggles the menu (semantics onClick for TalkBack).
  Carried renders the jump apex frame (frame 11) as the
  dangling frame — the atlas has no dedicated dangling frame;
  the apex (legs tucked) is the closest read.

### E3 — Interaction + Find (flow 14, tracked end to end)

- Bubble: system-font text at 14sp throughout (all bubble texts
  raised from 13sp), four 48dp chips (Find, Help, Suggest,
  Dismiss) with labels; bubble anchors to the sprite, flips
  side to stay on screen, clamps above the IME (WindowInsets.ime
  bottom), and closes on Close button, back (BackHandler), or
  touch outside (root pointerInput that never consumes, ignores
  downs inside the sprite or bubble rects, so home taps still
  do their home action). Pixel-stepped border is NOT built (see
  BLOCKED).
- Find flow states tracked in MascotEngine (Idle, Asking,
  Searching, Choosing, Navigating, Pointing, Done, Cancelled)
  with pure nextFindFlowState; the layer sends Start on Find
  open, QuerySubmitted on submit, Results from the brain's
  returned FindDecision (MascotResponse gained a findDecision
  field — default null, no interface change; ScriptedBrain sets
  it for Act/Choices/NoMatch), CandidatePicked on choice,
  Arrived on reaching the target, Done on terminal answers.
  Cancel on touch outside, back, Find switched off mid-flow,
  or 20s without a transition out of the waiting states
  (Searching, Choosing — Navigating/Pointing are not timed out:
  movement and the ring are themselves progress and both end
  on user action). Mode change is read as Find toggled off;
  other mode changes (character/size/roaming) do not cancel.
- Asking field: styled OutlinedTextField with label "Search
  apps, settings…", Search IME action (submits via action key
  or Find button), accent focus border + cursor. Resolution
  reuses the EXISTING search adapter (rankFind, no second
  index): exact, prefix, synonym, fuzzy, plus widget targets
  (FindTarget.Widget, matched in the app group so a widget
  exact match beats a setting prefix match). No-match says
  "I can't find that." with chips [Search in drawer, Try
  again]; never guesses.
- Target kinds: app on visible page → walk/jump to the nearest
  valid ledge by the icon, face it, Point, spotlight ring; app
  on another page → pager animates, waits for the settled page
  (hostCurrentGridPage), graph rebuilds, then walk + Point;
  app in folder or drawer-only → bubble with Launch chip
  (never a ring inside a closed folder; folders are not opened
  unasked); widget → Point at its frame (same guide path, page
  resolved from homeItems); Settings control → panel opens at
  the hub with the row deep-linked AND ringed (DeepLinkAnchor
  now draws a static 2dp accent border on the linked row while
  the panel is open — this also rings the existing wallpaper
  and coingecko-key deep links, consistently showing where you
  landed), mascot stays composed via the pointing exception,
  Done chip ends Find WITHOUT closing the panel (user closes
  Settings). Spotlight ring: accent, 2dp, soft pulse (static
  under Reduce Motion), 40% dim around the target (Canvas with
  offscreen compositing + BlendMode.Clear hole); any tap on
  the dim dismisses the guide (that tap is consumed by design;
  the next tap reaches home).
- Help map expanded 13 → 18 entries (island, mascot-find,
  mascot-move, today-cards, offline-cards). controlKeys filled
  where anchors exist: accent, wallpaper, dock, widgets, grid
  ("home"), mascot, find, quiet, reset, island. The grid answer
  text was CORRECTED (old text said "Settings, then Look, and
  adjust density" — wrong, grid lives in Home and Dock; new
  text says "Settings, then Home and Dock, and adjust the
  grid"). Help answers that carry a controlKey ring that row
  via the Open chip's deep-link. Suggest: ≤1/session, 24h
  cooldown, quiet hours 22:00-07:00 (existing canNudge gates,
  unchanged). Teach: one-time tutorials, don't-show-again
  respected. Dismiss sticks across restarts until re-enabled.
  Chat stays scripted (ScriptedBrain behind MascotBrain; no
  model brain in this phase).

### E4 — Mascot hub + empty states

- Hub preview box (120dp) with the animating pixel sprite
  (vector figure fallback) + Preview button playing
  idle → walk → jump → wave once (~1500ms each), then resting
  on idle. Rows: on/off (default off), character, roaming,
  size, Find, tips Off/Rare/Normal (Settings coercion widened
  0..1 → 0..2 at read and setter; stored values migrate by
  widening only, no Room change), quiet hours as rows opening
  24-hour TimePicker dialogs (minutes now settable; the old
  hour sliders are gone), Reduce motion, Chat model "Not set
  up" (unchanged), Reset mascot (renamed from "Reset mascot
  tips & tutorials"; scope unchanged), Bring back mascot
  (unchanged). Search entries gained "Reset mascot". Roaming
  help text corrected (ledges/dock top, not perches/home spot).
- Static poses as empty-state hosts (static images only, never
  the roaming overlay): drawer "No matches" → pixel surprised
  frame 28 (vector SAD fallback); API card offline/error →
  pixel sad frame 30 (vector SAD fallback, Phase C's pose);
  Agenda "No events today" → pixel sleeping frame 17
  (pixel-only addition; the vector path keeps today's
  figure-less layout). Find no-match bubble → surprised via
  expression mapping; Find Searching bubble → think pose via
  expression mapping. General loading skeletons untouched:
  thinking-as-loading is covered by the Find Searching state,
  not by replacing skeletons.

### E5 — Tests (written, NOT run — sandbox executor broken)

- MascotEngineTest.kt (NEW, 318 lines): state machine (spawn
  path, hide-always-poofs, jump/fall cycle, illegal
  transitions); ledge graph (clearance cut into 2 parts,
  exact-fit band does not block, narrow/full-block → empty,
  widget-top + dock segments survive); segment derivation (row
  tops + empty row below, above dropped when negative, dock +
  rail segments, nearest-point clamping); routing (same-ledge
  WalkTo, jump up, drop down, no-route when rise too high,
  snapDrop valid/invalid); planner determinism (same seed →
  same pick; pick within range; dwell/sit/sleep ranges; speed
  40-60); invalidation triggers (null → changed, same →
  unchanged, page/layoutHash/settingsOpen/keyboardVisible/
  editMode/iconSizeDp/dockSide changes → changed); Find flow
  (happy path incl. NoMatch → Done → restart,
  Choices → Navigating → Pointing → Done, Cancel/Timeout/
  ModeChanged win, timeout const 20000, Done from Searching/
  Choosing/Navigating → Done); Find resolution kinds (app
  exact band, uncertain non-empty, no-match empty + NoMatch
  decision, Setting + Action targets resolve).
- MascotSpritesTest.kt (NEW, 72 lines): size targets (56/72/96,
  else 72); integer scale (56dp@2.0 → 5, 72dp@1.0 → 3, tiny
  density → min 2, 96dp@2.5 → 10); time-based frame stepping
  (0 → frame 0, 100ms → frame 1 at 10fps, 600ms wraps, 350ms →
  index 3, empty anim → 0); atlas parsing (valid JSON, empty
  frames skipped, invalid JSON → null, empty animations →
  null); DEFAULT_ATLAS matches the sheet layout (idle 0-3,
  walk 4-9, poof 37-40, 41 distinct frames); expression mapping
  (9 cases).
- Existing tests untouched except MascotEngineTest additions
  above; MascotBrainTest still passes its shape (BrainContext
  gained a defaulted widgets field; GuideSetting gained a
  defaulted controlKey). Tests written, NONE run (sandbox
  executor broken, pre-existing; SettingsShellTest still has
  its 10 pre-existing compile errors).

### Phase E BLOCKED / not built (with reasons, never silent)

- Pixel-stepped bubble border (2px steps at sprite scale): NOT
  built. The bubble is a standard rounded Glass-style box
  (20dp corners, accent hairline) with 14sp+ text. Reason: a
  stepped border needs a custom border draw (stepped path
  around a variable-size bubble) that would fight the shell's
  rounded-rectangle bubble anatomy from Phase C; the readable
  text + 48dp chips (the accessibility substance) are built,
  the stepped outline is cosmetic. Follow-up, not silent.
- Per-card Today rail ledges: NOT built. The graph has one
  rail-top segment from the rail container rect; per-card tops
  need per-card rects, which are not plumbed (TodayRail does
  not export card rects). Rail coverage is therefore the rail
  top + page + dock, not each card top. Follow-up: plumb
  per-card rects the way PagedHome plumbs cell rects.
- Side-dock top perch: NOT built. A vertical dock's top edge
  sits at y=0, so a sprite standing on it would clip off the
  top of the layer; side docks contribute a dock obstacle but
  no perch. Bottom-dock top perch works. The sprite can still
  reach side-dock-adjacent ledges (page rows, rail top).
- Dock-app Find targets: bubble + Launch chip only, no
  walk/ring. Dock item rects are not plumbed (dockKeys are keys,
  not rects), so there is no ring target. Follow-up: plumb dock
  item rects like cell rects.
- Find pointing while a page is swiping: no edges across pages
  while swiping (graph keys on the settled page); a guide
  started mid-swipe waits for the settled page via the page
  guard in the guide effect.
- Jump arc is linear position + jump frames (rise/apex/fall/
  land carry the arc read); no ballistic easing curve. The
  prompt's frame list carries the arc; a curved tween would
  double-apply it.
- Timers restart (not resume-from-pause) when home resumes;
  they never fire while home is stopped. A dwell/sit/sleep
  clock that started before a stop restarts from zero after
  resume — bounded (max one extra dwell cycle) and simpler
  than persisting timer phase.
- Guide starting mid-planner-trip resolves within one step
  (planner abort check at step boundaries); the two routes do
  not interleave beyond that bound.
- MascotSearchEntries covers Reset mascot; folders/drawer/
  search/gestures/animations/privacy/backup/profiles/apis/look/
  theme controlKeys (where they exist in the help map) fall
  back to hub level: the panel opens at the hub, no row ring,
  the bubble names the control. Documented per-key in
  PHASE-E.md; no row was redesigned to force an anchor.

### Verification (Phase E)

- Verification build: BUILD SUCCESSFUL in 11m 17s (build.sh);
  APK app/build/outputs/apk/release/app-release.apk, 2,809,473
  bytes, md5 eebdc1cf4fb21ad96432049fb4613b1d, aapt package
  app.havenos.launcher versionCode 12 versionName 3.0 minSdk 26
  targetSdk 36, apksigner CN=HavenOS SHA-256 match (keystore
  match). Verification build only — NOT delivered anywhere,
  nothing published or uploaded.
- First build attempt FAILED at compile (4m 30s) with 8 error
  groups, all fixed in the layer/host (no scope change):
  awaitPointerEventScope is a PointerInputScope member, not a
  top-level import (deleted the import; existing files
  CalmMode/HomeA11y/Dock use it unimported); missing
  animateFloat + layout.ime imports; graph block referenced
  undefined ox/oy (now layerOrigin.x/y); moveDurationMs was
  Int state assigned Long (now Long state); two `run {
  continue }` inline-lambda continues (restructured to
  if-null + continue, language version < 2.2); PointerChange
  .positionChange() is not a function in this Compose version
  (now position - previousPosition); Home.kt lacked the
  boundsInWindow import. Second build passed clean.
- Diff review vs the pre-E snapshot: only the files listed in
  PHASE-E.md differ (plus the three doc files and the new
  art/test/doc dirs); AndroidManifest.xml is byte-identical,
  so Phase E adds NO permission. Security grep over the
  changed files: no secrets, keys, tokens, or credentials
  (only benign query-tokenization matches in MascotBrain/
  MascotFind). No dependency added. No Room migration
  (tips widening is a DataStore coercion change only).
  Signing, version (3.0/vc12), and ~/workspace/havenos-github/
  untouched.
- Tests: 2 new files written (390 lines total), NONE run
  (sandbox executor broken, pre-existing; SettingsShellTest
  still has its 10 pre-existing compile errors).

## 3.1 Phase F — On-device assist foundation (2026-10-30, 3.0/vc12 tree — verification build only, NOT a release)

Phase F builds prompt §F (F1–F5) on the device-only principle (F0):
assist works from data already on the device, runs on the device,
and never sends screen-derived or usage data off it. No cloud AI,
no new network calls, no new permissions. F6 is NOT in this pass.

### F1 — Seam
- `ui/assist/AssistRouter.kt`: the one assist provider (rules,
  synonyms, ranking, no model). `MascotBrain` stays the chat seam;
  no cloud provider exists. `LocalModelBrain` implements
  `MascotBrain` behind `USE_MODEL_BRAIN = false` and delegates to
  `ScriptedBrain`.

### F2 — Lite improvements (no model, no new dependency)
- Drawer search + panel (Settings home) search parse compounds
  via `parseAssistCommands`: dark/light/system theme, bigger /
  smaller icons (±8dp), accent by curated name, open app, go to
  hub, toggle calm. Compounds are all-or-nothing; one bad clause
  voids the whole query and normal search renders unchanged.
  One row per compound, one Apply tap, labels show old → new.
- App resolution reuses `matchApps` (real app list + local launch
  stats); a band + confidence tie refuses rather than guesses.
- App prediction untouched (`suggestApps`: count, recency,
  same-hour boost). Categories, suggestions, emotes rule-based.
- Clear learned data control lives in the ledger (F4).

### F3 — Action router
- Sealed whitelist only: OpenApp, OpenHub, SetDarkMode,
  StepIconSize, SetAccentCurated, ToggleCalm. No uninstall /
  hide-all / permission variant can be emitted. Names resolve
  against the real app list and the real settings index
  (`FIND_SETTING_INDEX` + hub alias table); not found → fallback.
- Setting changes use typed setters with range checks (dark mode
  0..3, icon size clamped 40..68, accent by curated name keeping
  strength/recents). Panel path applies in-session → Changes
  shows readable names + Revert. Drawer path parks actions in
  `pendingAssistActions` and the panel-open effect applies them
  AFTER `panelSession.begin()`, so Before shows the pre-change
  home. Row label is the preview; tap is the confirmation.

### F4 — AI Privacy Ledger (Privacy hub)
- All five features (Search commands, App prediction, Mascot
  Find, Mascot chat, Suggestions) with what local data each
  reads, an On-device badge, last used time + use count, and a
  per-feature switch. Storage is counts + times ONLY (hand-rolled
  flat JSON; one DataStore write per record).
- Prediction's row derives from launch stats (no per-render
  write). Find's switch reuses `mascotFindOn`; Suggestions'
  switch maps to `mascotTips != Off` (re-enable restores Rare).
  Chat's switch gates conversational branches only — Find keeps
  working with chat off. Prediction's switch gates the Suggested
  widget's input (off → its existing empty state).
- Clear learned data wipes in-memory + persisted launch stats
  and both ledger JSONs; mascot muted/don't-show lists stay
  (they are preferences, not learned data).

### F5 — Optional local model pack (SCAFFOLD ONLY)
- `ModelPackManager` (app-private storage, sidecar sha + bytes):
  user-request-only download, Wi-Fi-only by default, progress +
  cancel, SHA-256 verification before atomic rename, free-space
  (100 MB headroom) + RAM (1 GB scaffold floor) checks, delete.
  The empty-URL gate fires before any network use.
- `MODEL_PACK_URL` / `MODEL_PACK_SHA256` are EMPTY placeholders
  (no values invented). No runtime picked (Phase 3 conclusion
  stands). Mascot hub Chat model row shows the REAL pack state
  ("Not set up" today) with Delete when READY; no download
  button that does nothing. A model wakes only via chat and
  never returns a settings-changing action.

### Deviations (with reasons)
- "silent" has NO mapping: no silent / do-not-disturb setting
  exists in `SettingsState`, and inventing one would need new
  behavior + a permission beyond §F. It falls back to search.
- Panel-search command rows do not navigate to the changed
  control: the row stays so the Changes footer (with Revert)
  stays visible as the confirmation surface.
- Mixed app + setting compounds land on the last setting
  action's hub; every action still executes exactly once.
- Ledger switches are not session-revertible (same scope as
  `mascotFindOn`, which `commitSnapshot` also omits). Router
  actions ARE revertible (Dark mode / Accent / Icon size).
- Ledger history is not backed up (device-local ephemeral, same
  scope as mascot prefs which backup omits).

### BLOCKED (with reasons, never silent)
- SearchCard command rows: `SearchCard` shares
  `SearchResultsList` but its Home call site passes no command
  handler, so it renders no command rows. Drawer + panel search
  cover §F2; wiring SearchCard is a small follow-up.
- Model-pack download / checksum / Wi-Fi / space / RAM / cancel
  / delete paths: scaffold-only until a real model URL +
  checksum is configured (placeholders empty by design).
- No runtime picked; `LocalModelBrain` delegates until one
  integrates cleanly.
- F6 (Scenes, Style DNA, Recall, notification digest, cloud AI,
  mascot voice, more characters, online themes store): NOT in
  this pass.
- Phase E BLOCKED items carried unchanged (pixel-stepped bubble
  border, per-card rail ledges, side-dock perch, dock-app Find =
  bubble + Launch only).

### Verification (Phase F)
- First build attempt FAILED at compile (2m 13s) with 2 error
  groups, both fixed in place (no scope change): the second
  `SearchResultsList` call site is inside `SearchCard`, not
  `LibraryOverlay` (arg dropped); `BrainContext.chatEnabled`
  was inserted mid-constructor and broke positional callers
  (moved to last param). Second build passed clean.
- Verification build: 2,825,857 bytes, md5
  f4a56ac74624e44d58f1453bfb3bc0ba, vc12 / 3.0, havenos cert
  match (apksigner SHA-256
  53bff2a09aedb516a8ee6193234a7aaf5d04ce901fb8931b799191b9845354a8).
  Verification build only — NOT delivered anywhere, nothing
  published or uploaded.
- AndroidManifest.xml byte-identical (NO new permission);
  `app/build.gradle.kts` identical (NO new dependency). No Room
  migration (DataStore keys only). Security grep over the
  changed files: no secrets, keys, tokens, or credentials.
  Glyph scan: no emoji; `→` in command labels follows existing
  shipped precedent (Changes rows, gestures hub).
  Signing, version (3.0/vc12), and ~/workspace/havenos-github/
  untouched.
- Tests: 3 new files written (296 lines total), NONE run
  (sandbox executor broken, pre-existing; SettingsShellTest
  still has its 10 pre-existing compile errors).
- Full per-step record: `docs/3.1-phase-reports/PHASE-F.md`.

## 3.1 Phase G — Verification, docs, report + final version stamp (2026-10-30, 3.1.0/vc13 tree — final build, NOT delivered)

- G1 checks, all run independently in Phase G (not trusted from
  prior reports): release build.sh BUILD SUCCESSFUL in 35s
  (incremental, APK identical to Phase F: 2,825,857 bytes, md5
  f4a56ac74624e44d58f1453bfb3bc0ba, vc12/3.0, havenos cert match);
  debug assemble feasible via the same recipe (BUILD SUCCESSFUL
  in 6m 50s, debug APK 16,205,713 bytes, md5
  c4976baa8d5ea2596d355acae2ccae38 — feasibility check only,
  never a deliverable); lint ran offline (BUILD FAILED in 5m 5s,
  16 errors, 89 warnings, 35 hints — all pre-existing, none fixed
  here); unit tests attempted (BUILD FAILED in 1m 12s at test
  compile, 305 errors, zero tests ran — see below);
  instrumentation NOT APPLICABLE (no androidTest dirs); screenshot
  tests NOT APPLICABLE (no screenshot test files).
- G1 test finding (new detail, not just "not run"): the test tree
  does not compile. Per-file errors: MascotEngineTest 126,
  AssistRouterTest 53, MascotSpritesTest 45, ModelPackTest 28,
  AssistLedgerTest 28, SettingsShellTest 10 (pre-existing
  gridShrink helpers), LayoutStateTest 6, EditModeTest 5,
  HexLiteralTest 2, EmojiScanTest 2. Root causes: Phase E/F tests
  import kotlin.test.* but the project declares only JUnit4
  (kotlin-test is not a declared test dependency); LayoutState /
  EditMode / SettingsShell tests are stale vs the coord-fix APIs;
  2 type errors in the Phase B scan tests. The sandbox executor
  breakage is a second blocker behind compilation. Repair is the
  recommended next improvement, not a Phase G fix (Phase G changed
  no app or test source, so it introduced no regression).
- G2 manual: source
  ~/workspace/your_files/havenos-how-it-works/.src/index.html
  updated to 3.1 (sections 1–3, flows 13–16, A2/A3/CardStatus,
  pixel mascot + ledges + Find, island restyle, honest section
  10; Figure 7 embeds docs/mascot/preview.png; backup
  .src/index.html.pre-3.1; HTML parses clean, tags balanced).
  PDF NOT rebuilt: the render pipeline is not reproducible from
  .src/ (no scripts saved, no Chromium binary here) — the
  delivered PDF remains the 3.0 version until rebuilt; follow-up,
  not hidden, and it did not block the build.
- G2 docs: DEVICE_CHECKLIST.md gained the consolidated 3.1 FINAL
  top section (G-1…G-26, all NOT RUN, mapping the G3 areas to the
  existing series); UNVERIFIED.md gained the missing Phase A
  section plus the Phase G section; this Phase G section.
- G3 consolidated checklist: G-1…G-26 in DEVICE_CHECKLIST.md
  (home, Settings, cards, drawer, mascot, safety, performance,
  upgrade, Follow-density, menu destinations, island callbacks,
  refresh-on-resume). Every item NOT RUN.
- G4 report: docs/3.1-phase-reports/PHASE-G.md (per-phase changes,
  tests, checks + results, dependencies, files, assumptions,
  deviations, unverified, gaps, next improvement, open decisions).
  No claim of production readiness, accessibility conformance, or
  security review.
- Dependencies: NONE across all of 3.1 (build.gradle.kts identical
  to the pre-Phase-A snapshot, verified by diff). No new permission
  (manifest byte-identical to the pre-G snapshot).
- Final version stamp (LAST, after G1–G4): versionCode 12 → 13,
  versionName "3.0" → "3.1.0" in app/build.gradle.kts (two lines).
  Final build: build.sh BUILD SUCCESSFUL in 8m 37s, GRADLE_EXIT=0.
  Final APK app/build/outputs/apk/release/app-release.apk,
  2,825,857 bytes, md5 cc45b27dcf53ca487af36fe68a9cc4b2;
  aapt package app.havenos.launcher, versionCode 13, versionName
  3.1.0, minSdk 26, targetSdk 36; apksigner CN=HavenOS, SHA-256
  53bff2a09aedb516a8ee6193234a7aaf5d04ce901fb8931b799191b9845354a8
  (keystore match). NOT delivered, NOT published — parent owns
  delivery and the release decision.
- BLOCKED / carried: PDF rebuild (follow-up); lint errors (16) and
  test-compile errors (305) unfixed (pre-existing, out of scope);
  all Phase A–F deviations and BLOCKED items carried in PHASE-G.md.
- Full per-step record: docs/3.1-phase-reports/PHASE-G.md.

## 3.1.1 hotfix (2026-10-31, 3.1.0/vc13 tree → 3.1.1/vc14 — verification build only, NOT delivered)

- Item 1 crash sweep: `firstNotNullOf` → `firstNotNullOfOrNull` in
  MascotLayer placementOf (the prime suspect for the reported
  NoSuchElementException class — NOT a proven vc13 line, because no
  vc13 mapping.txt was retained); MascotEngine BFS / reconstruction
  / pickTargetLedgePoint trailing lines converted to OrNull +
  return-null (host stays put on route failure); MascotSprites idle
  fallback; spriteSet `!!` removed; GesturesMotionHub picked `!!`
  race in the icon-swipe launch fixed. All other first/last/getValue
  /`!!` sites triaged in the hotfix report as guarded or reviewed-safe
  (not churned). `-keepattributes SourceFile,LineNumberTable` added
  to app/proguard-rules.pro; mapping.txt retention starts with vc14
  (release-artifacts/mapping-14.txt); retrace documented in
  ~/workspace/havenos/README.md (created by this hotfix).
- Item 2 failure isolation: new ui/FeatureGuard.kt (supervised
  scopes + CoroutineExceptionHandler; local-only feature_errors.txt,
  last 20; session-only disable; one toast per feature — mascot
  "Mascot paused after an error"). Mascot/Find scopes in
  MascotLayer, island refresh catch in IslandFeed, online-cards
  retry catch + host early return in TodayApiCards, host gates in
  Home.kt. Feature failures never write crash markers.
- Item 3 crash-loop protection: extends HavenApp.installCrashLog
  (no second handler) with a small timestamp marker + delegation;
  2+ crashes in 60s → Safe Mode (mascot, island, workers, blur off;
  stored settings never mutated, so Re-enable is exact by
  construction; 10-minute stable timer; snapshot in safe_mode.txt).
  Banner (exact text "HavenOS recovered from a crash. Mascot turned
  off") with Re-enable / View details in ui/SafeModeBanner.kt;
  theme-level Solid override in MainActivity; gates + banner in
  Home.kt; last crash + feature errors in DeviceDataHub Diagnostics
  (user-triggered Copy/Share only, nothing uploaded).
- Item 4 touch pass-through: MascotLayer root verified
  pointer-transparent (observing loop, never consuming); sprite Box
  carries graphicsLayer translation AND input on the same node (no
  translated-draw/untranslated-touch bug); spotlight dim fixed to
  consume only the dismiss tap's up event and release swipes
  unconsumed; bubble position math moved into the graphicsLayer
  lambda (no per-frame recomposition); spriteHitRect helper added.
  UI test MascotTouchTest written, NOT run.
- Item 5 settings layout: root cause confirmed in code (panel
  ~46% / 480–660dp + 260dp rail → ~240dp detail; weighted label
  rows wrapped per character). Panel now clamp(640dp, 56%, 920dp);
  rail 72dp icon-only (tooltip + accessibility label) under 900dp,
  200dp at 900dp+; single-pane under 560dp; detail ≥ 360dp in
  master-detail; AdaptiveSettingRow custom Layout (label ≥
  max(140dp, 50%), stacks otherwise) behind HubRow/HubSwitchRow;
  HavenSegmented equal-≥64dp or wrapping chips (softWrap off,
  maxLines 1); ApiKeyField label-above + 72dp button row; header
  title maxLines 2 (never ellipsized), Before/Changes icon-only
  under 560dp; hub rows 1-line title / 2-line summary. Matrix unit
  tests written, NOT run.
- Item 6 drawer fully solid: drawerSurface + drawerField tokens in
  LookTokens (dark near-black + ~4% accent at alpha 1.0, light
  off-white; ≥ 4.5:1 contrast; no hex outside the token module);
  sheet converted from Glass2 to opaque drawerSurface with 28dp top
  corners, 1dp top hairline, drag handle, 40% scrim behind;
  translation-only open (AnimatedVisibility slide, no fade);
  search field, history/hidden chips, sticky headers, letter bubble
  all solid drawerField; IME insets + compact toolbar row kept;
  glass stays for floating things. Drawer tests extend
  HexLiteralTest (alpha, contrast, source scan — the scan FAILS THE
  BUILD on any translucent drawer colour); screenshot test
  DrawerSolidTest written, NOT run.
- Item 7 mascot bubble (E3, overriding the Phase E deferral):
  SteppedRoundedShape + steppedCornerPoints (2px steps at sprite
  scale, 20dp corner kept, 14sp+ text kept); menu rebuilt as a
  FlowRow of four 48dp icon chips (Find/Search, Help/Help,
  Suggest/Star, Dismiss/Close); anchor/flip, IME clamp, close/back
  dismiss, and Find flow states unchanged. No Phase C bubble
  behavior conflicted, so no closest-variant compromise was needed.
- Deliberately NOT changed in this hotfix: PagedHome cell
  `appEntry!!` / `folder!!` and ghost `overCell!!` (observed in the
  sweep, out of the briefed scope — needs a scoping decision);
  SearchCard (a floating surface, not the drawer sheet);
  HavenSegmented's use in non-drawer hubs (same component, same
  modes); the delivered explainer PDF (still 3.0 — rebuild is a
  separate follow-up).
- Full per-item record: docs/3.1-phase-reports/HOTFIX-3.1.1.md.
  Tests: HotfixCrashSweepTest, SettingsLayoutMatrixTest,
  CrashLoopTest, HexLiteralTest drawer tests (unit, written NOT
  run); MascotTouchTest, DrawerSolidTest (UI, written NOT run, NOT
  compiled here). Nothing in the hotfix is device-verified.
- Final build: build.sh BUILD SUCCESSFUL in 8m 58s; final APK
  2,842,241 bytes, md5 5133348a0a3764f4181bb80786748a81, vc14/3.1.1,
  havenos cert match; mapping retained as
  release-artifacts/mapping-14.txt. NOT delivered, NOT published —
  parent owns delivery and the release decision.
