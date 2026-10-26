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
