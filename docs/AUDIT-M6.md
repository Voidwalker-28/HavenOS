# AUDIT-M6 — Honesty audit (M6 Phase 0, v2 §12)

Date: 2026-10-23. Scope: EVERY user-facing setting in the app as shipped in vc5.
Method: each store key in `app/src/main/java/app/havenos/launcher/data/Settings.kt`
(`SettingsState`, DataStore keys in `K`) was traced to (a) its Settings UI control
in `ui/settings/SettingsCat1–14.kt` / `SettingsHub.kt`, and (b) every consumer
outside `ui/settings/` — `ui/Home.kt`, `ui/ScrollHome.kt`, `ui/Drawer.kt`,
`theme/Looks.kt`, `theme/Glass.kt`, `data/*`, `HavenAccessibilityService.kt`,
`MainActivity.kt`, `HavenApp.kt`. Backup/profile round-tripping (`data/Backup.kt`,
`data/Profiles.kt`) and the Settings preview samples/minis do NOT count as a
live effect. A key counts as **Working** only if a store write visibly changes
home / drawer / dock / widget / gesture / search behaviour in real code.

Status key:
- **Working** — store write → real behaviour change, code path verified end-to-end.
- **Partial** — part works (stated exactly); the rest is sample-, pad- or
  preview-only, or unreachable from the UI.
- **Not wired** — a visible control (or option) that changes nothing live.
  These are the removal candidates for the next phase. Stored-only keys with
  NO control are listed separately (§5) — they cannot be "removed from the UI"
  because they are not in it.

Category numbering below is the 14-category M5 shell (SettingsHub.kt:88-105):
1 Style & Theme · 2 Home screen · 3 Wallpaper · 4 Dock · 5 App drawer ·
6 Icons · 7 Folders · 8 Widgets · 9 Gestures · 10 Search & Smart ·
11 Animations · 12 Privacy & Lock · 13 Backup & Profiles · 14 System.
"Quick panel" = the swipe-down panel (`ui/QuickPanel.kt`).

## 1. Working (62 keys)

| Key (exact store key) | Screen / category | Control | Effect (one line) |
|---|---|---|---|
| `accent_mode` (values 0–2) | 1 Style & Theme | Choice (Minimal default / Palette / Custom hex) | `theme/Looks.kt:97` applies palette/custom accent to buttons, toggles, highlights. Value 3 is dead — see §3. |
| `accent_argb` | 1 Style & Theme | Swatches + validated #RRGGBB dialog | Sets the palette/custom accent colour (with `accent_mode` 1/2). |
| `dark_mode` | 1 Style & Theme (+ Quick panel chips) | Choice (System / Light / Dark / Auto 19:00–07:00) | `theme/Looks.kt:124` switches the whole palette incl. time-of-day Auto. |
| `surface_material` | 1 Style & Theme | Choice (Solid / Translucent / Frosted) | `theme/Looks.kt:87` switches `GlassSurface` tier on every card/dock/folder. |
| `corner_dp` | 1 Style & Theme | Slider 0–40dp + "Reset to 12dp" | `theme/Looks.kt:92` rounds cards, dock and folders; −1 = Minimal 12dp. |
| `font_choice` | 1 Style & Theme | Choice (System / Serif / Monospace) | `theme/Looks.kt:151` changes the launcher typeface. |
| `density` | Quick panel only (NO Settings row — see §6) | Quick-panel slider 0–100 | `SettingsState.iconDp()` = 42 + density·25/100 drives icon size on scroll home (`ScrollHome.kt:526,728`) and dock (`Home.kt:1418`); also widget-sheet cell size. Grid itself is NOT rendered (see `cols_port` etc.). |
| `reduce_effects` | Quick panel only (no Settings row) | Quick-panel switch | `theme/Glass.kt:60` forces the Flat glass tier everywhere. |
| `icon_size_override` | 2 Home screen | Slider 40–68dp + "Auto" reset | Overrides `iconDp()` on home and dock; −1 = density-driven. |
| `label_size` | 2 Home screen | Slider 9–16sp | Label text size on scroll-home rows (`ScrollHome.kt:534,584,736`) and dock icons (`Home.kt:1270`). |
| `lock_layout` | 2 Home screen + 12 Privacy & Lock | Switch (both screens) | Blocks edit-mode entry, moves, adds and removes; toasts on every entry path (`Home.kt:609,676,949`). |
| `wallpaper_mode` | 3 Wallpaper | Choice (System / HavenOS) | Switches between device wallpaper and the HavenOS renderer (`Home.kt:1105`, `data/Wallpaper.kt:62`). |
| `wp_kind` | 3 Wallpaper | Gradient preset tiles / Solid / Photo… | Selects gradient 0–3, solid 4, or photo 5 in the HavenOS renderer (`data/Wallpaper.kt:111`). |
| `wp_uri` | 3 Wallpaper | System photo picker (persistable grant) | Photo wallpaper decoded and drawn (`data/Wallpaper.kt:112`). |
| `wp_dim` | 3 Wallpaper | Slider 0–60% | Black dim overlay over the wallpaper (`Home.kt:1124`). |
| `wp_zoom` | 3 Wallpaper | Slider 100–200% | Wallpaper zoom (`Home.kt:1117`). |
| `wp_off_x` / `wp_off_y` | 3 Wallpaper | Sliders −1…1 | Wallpaper pan (`Home.kt:1120-1121`). |
| `wp_tint` | 3 Wallpaper | None + tint swatches | Tint overlay over the wallpaper (`Home.kt:1127`). |
| `wp_solid` | 3 Wallpaper | Solid colour swatches | Solid wallpaper colour (`data/Wallpaper.kt:114`). |
| `dock_json` (field `dock`) | Drawer / scroll-home long-press menus + edit mode (no Settings row) | Add to / Remove from dock | Pinned dock list, capped by `dock_max` (`Home.kt:411`). |
| `dock_max` | 4 Dock | Slider 3–6 slots | Caps pinned dock icons (`Home.kt:412`, `ScrollHome.kt:370`). |
| `dock_pos` | 4 Dock | Choice (Bottom / Left / Right) | Bottom pill vs left/right side rail (`Home.kt:757,765,767`). |
| `swipe_up_drawer` | 5 App drawer | Switch | Swipe-up on home opens the drawer only when this is on AND the gesture map binds swipe-up to drawer (`Home.kt:706`). |
| `drawer_sort` (values 0–2) | 5 App drawer | Choice (A–Z / Most used / Recently installed) | Drawer ordering (`Drawer.kt:249,354`). Value "Custom" is a dead option — see §6. |
| `cat_hidden_json` | 5 App drawer (category editor) | Per-category show/hide switch | Hides categories in Categories mode (`Drawer.kt:324`). |
| `cat_names_json` | 5 App drawer (category editor) | Rename dialog / Add name | Category display names in drawer (`Drawer.kt:323`). |
| `cat_merge_json` | 5 App drawer (category editor) | Merge dialog | Merged categories show under the target (`Drawer.kt:327`). |
| `hidden_json` | 5 App drawer (result list) + 12 Privacy & Lock | Long-press Hide / Unhide buttons | Hidden apps vanish from home, drawer and search (`Home.kt:210`, `ScrollHome.kt:225`, `Drawer.kt:245`). |
| `icon_pack` | 6 Icons | Pack list (Use / Active) + System default | Active pack applied to all icons (`Home.kt:250` → `AppRepository.activePack`). |
| `mono_icons` | 6 Icons | Switch | Desaturates all app icons (`Home.kt:1172`, MonoFilter). |
| `badge_mode` | 6 Icons | Choice (Off / Dot / Count) | BadgeOverlay dot/count on icons (`Home.kt:1194,1207`); real counts need notification access (gated, honest). |
| `badge_hidden_json` | 6 Icons ("Silence dots per app") | Add-by-package dialog + Unsilence | Suppresses dot/count for listed packages (`Home.kt:1197`). |
| `label_overrides_json` | Drawer / scroll-home long-press "Edit label" | Text dialog | Custom app labels on home and drawer (`Drawer.kt:858`, `ScrollHome.kt:898`, `data/Apps.kt:206`). |
| `at_a_glance` | 8 Widgets | Choice (Off / Date / Date+weather) | At-a-glance line above the dock (`Home.kt:769,1002`); mode 2 refreshes weather. |
| `weather_city` / `weather_lat` / `weather_lon` | 8 Widgets | City search dialog (Open-Meteo geocoding) | City shown by weather widgets and at-a-glance (`Widgets.kt:324`, `data/Weather.kt:103`). |
| `weather_units` | 8 Widgets | Choice (°C / °F) | Temperature units in widgets and at-a-glance (`Widgets.kt:359`). |
| `weather_network` | 8 Widgets | Switch | Off = cached data only, no fetch (`data/Weather.kt:102`). |
| `calendars_json` | 8 Widgets | Per-calendar switches (permission-gated) | Filters Agenda/Calendar widget events (`Widgets.kt:480,545`); "" = all. |
| `stacks_on` | 8 Widgets | Switch | Stack widgets swipe through members; off = first member only (`Widgets.kt:1323,1349`). |
| `gestures_json` | 9 Gestures | Per-trigger picker (5 triggers) + Test | All five triggers dispatch in `Home.performGestureAction` (`Home.kt:451`): drawer, search, quick panel, settings, lock (device-admin-gated), shade (quick-panel fallback if expand fails), none. |
| `icon_swipes_json` | 9 Gestures ("Per-icon swipe up") | App picker + action picker | Fires on scroll-home icon flick (56–200dp, ≤300ms) on favorites AND A–Z rows (`ScrollHome.kt:522,724` → `Home.kt:674`). Cat9's "do not fire yet" text is stale — see §6. |
| `nav_gestures_on` | 9 Gestures | Switch + setup-status Fix buttons | Enables the accessibility edge layer (`MainActivity.kt:62`, `HavenAccessibilityService.kt:191`); gated on service + overlay grants. |
| `nav_map_json` | 9 Gestures | Per-trigger picker (up / upHold / left / right) | Edge-swipe actions in every app (`HavenAccessibilityService.kt:200`); rows disabled until the service is on (honest gating). |
| `nav_edge_dp` | 9 Gestures | Slider 12–48dp | Edge-zone thickness (`HavenAccessibilityService.kt:204`). |
| `nav_sensitivity` | 9 Gestures | Slider 0–100 | Swipe distance 56dp→20dp (`HavenAccessibilityService.kt:203`, `data/NavGestures.kt`). |
| `nav_haptics` | 9 Gestures | Switch | Vibration on fired nav gestures (`HavenAccessibilityService.kt:201`). |
| `nav_hide_bar` | 9 Gestures | Switch (disabled until ADB grant) | Hides the system button bar everywhere via saved `policy_control` (`HavenAccessibilityService.kt:73`); ADB-grant command shown until granted. |
| `search_contacts` | 10 Search & Smart | Permission-gated switch | Contacts section in drawer search (`Drawer.kt:294`). |
| `search_calendar` | 10 Search & Smart | Permission-gated switch | Calendar-events section in drawer search (`Drawer.kt:302`). |
| `search_settings` | 10 Search & Smart | Switch | Settings sections in drawer search (`Drawer.kt:314`). |
| `search_calc` | 10 Search & Smart | Switch | Arithmetic answers in drawer search (`Drawer.kt:282`). |
| `launch_stats_json` | 10 Search & Smart ("Reset ranking" action) | Action button | Launch counts drive Most-used sort and drawer suggestions; reset clears both (`data/Apps.kt:145`). |
| `hidden_lock` | 12 Privacy & Lock | Switch + "Test biometric lock" | Device-credential gate on the hidden-apps list in Cat12 (`SettingsCat12.kt:52-54,106`). |
| `backup_include_wp` | 13 Backup & Profiles | Switch | Daily auto-backup includes wallpaper choice (`HavenApp.kt:60`); also honoured by export. |
| `profiles_json` | 13 Backup & Profiles | Save / Switch / Rename / Duplicate / Delete | Profile snapshots incl. layout, dock, density, wallpaper (`data/Profiles.kt:110`). |
| `active_profile` | 13 Backup & Profiles | "Active" marker / Switch buttons | Current profile; switching away auto-saves the profile left (`data/Profiles.kt:112-115`). |
| `profile_schedule` / `sched_start` / `sched_end` / `sched_profile` | 13 Backup & Profiles | Switch + start/end sliders + profile picker | Timed auto-switch in `HavenRoot` (`Home.kt:478-494`); previous profile is NOT auto-restored (stated in UI). |

Working non-key actions (no store key; verified live): Mini-home header collapse
(`mini_home_collapsed`, `SettingsHub.kt:965`); top-bar "Revert this session"
(snapshot restore, `SettingsHub.kt:407`); shell Reset buttons cats 1–13
(`SettingsHub.kt:797`, `resetNewCategory`); Cat13 "Reset all (categories 8–14)"
(`SettingsCat13.kt:410` dialog); backup Export/Import with validation + summary;
third-party widget Remove/Rebind (Cat8); Set-as-default-launcher chooser (Cat14);
permission Fix buttons (Cat14 dashboard, Cat9 nav status); diagnostics Copy
(Cat14); licenses dialog (Cat14); Cat10 ranking-reset + history-clear actions;
Cat12 credential-test action.

## 2. Partial (19 keys) — exactly what works and what does not

| Key | Screen | Control | Works | Does NOT work |
|---|---|---|---|---|
| `labels_on` | 2 Home screen | Switch "Show labels" | Dock/grid `AppIcon` path honours it (`Home.kt:1265`). | Scroll-home rows ALWAYS show labels (unconditional `Text` at `ScrollHome.kt:534,584,736`); the switch changes nothing on the actual home. |
| `cols_port` / `rows_port` / `cols_land` / `rows_land` | 2 Home screen | 4 sliders + "Follow density" reset | Feed `gridCells()` → widget-sheet cell size (`Home.kt:907`) and resize-dialog steps; Cat2 grid overlay + ghost-overflow report. | The scroll home renders no grid, so grid size changes nothing on home itself. |
| `drawer_mode` | 5 App drawer (indirect only) | No direct picker; synced from `drawer_style` | Grid (0), List (1), Categories (2) render (`Drawer.kt:604,615`). | Search-first (3) is unreachable from any picker — only a restored backup can set it. |
| `drawer_style` | 5 App drawer | Choice (Vertical grid / Horizontal pages / List / A–Z scrubber / Auto-categories) + [M] sample | Values 0, 2, 4 sync `drawer_mode` live (`Settings.kt:setDrawerStyle`). | Values 1 (horizontal pages) and 3 (A–Z scrubber) are stored + sample-only; the live Drawer has no pages or scrubber rail. The choice text admits this. |
| `drawer_gesture` | 5 App drawer | Choice (Swipe up / Swipe up+hold / Double-tap) | Value 0 = the live swipe-up gesture (via `swipe_up_drawer`). | Values 1 and 2 are stored for a future Gestures worker; home detects no hold/double-tap drawer trigger. |
| `scrollbar_style` | 5 App drawer | Choice (None / Letter rail / Thin bar) | Letter rail (List mode, `Drawer.kt:703`) and thin bar (Categories/Search-first, `Drawer.kt:624,751`). | No scrollbar aid renders in Grid mode (0). |
| `icon_shape_ext` | 6 Icons | Choice (Circle / Squircle / Rounded / Teardrop / System) + separate "Square" action | Values 0–3 sync `icon_shape` and render live (`Home.kt:1160`). | Values 4 (teardrop) and 5 (system) are stored only — the renderer has no teardrop/system mask (admitted in the choice text). |
| `widget_radius` / `widget_bg` / `widget_opacity` | 8 Widgets | Slider / Choice / Slider | The Cat8 frame SAMPLE renders radius, background and transparency live (`SettingsCat8.kt:99-102`). | No real home widget reads them — the file header says the sample "is the field's real consumer until home widget rendering reads the fields". |
| `gesture_sensitivity` | 9 Gestures | Slider 0–100 | The Cat9 gesture test pad threshold moves with it (`SettingsCat9.kt:285` + pad). | Home gesture detection uses hardcoded thresholds (80dp pinch/two-finger, 120dp swipe at `Home.kt:146,702,709`) and never reads this value. |
| `anim_speed` | 11 Animations | Choice (Off / Fast / Normal / Slow) | The three Cat11 demo rows play at the chosen speed (`SettingsCat11.kt:126-128`). | No renderer outside Settings/Backup consumes it (COVERAGE correction stands); real transitions do not speed up/slow down. |
| `press_feedback` | 11 Animations | Switch | Cat11 press test pad highlight + buzz honour it (`SettingsCat11.kt:178,181`). | No launcher-wide press feedback; taps elsewhere ignore it. |
| `haptic_strength` | 11 Animations | Slider 0–100 | Cat11 test pad and "Try haptic" vibrate at this strength. | Nothing outside Cat11 reads it (nav haptics use the separate `nav_haptics` switch). |
| `reduce_motion` | 11 Animations (+ Quick panel) | Switch | Honoured by Quick-panel transitions (`QuickPanel.kt:72`), edit-mode shrink (`Home.kt:515`) and dock fade. | The label "everywhere in HavenOS" overstates it: there is no global animation system to gate (COVERAGE M2 note stands). |
| `search_history_json` | 10 Search & Smart (Clear action) + Drawer | Recorded on search launch; Clear button | Recording and clearing work (`Drawer.kt:268`, Cat10 clear). | History is SURFACED only in Search-first drawer mode (`Drawer.kt:637-645`); in Grid/List/Categories it is invisible. |

## 3. Not wired — visible controls that change nothing (39 keys + 2 placeholder actions)

REMOVAL CANDIDATES for the next phase. Each was verified to have zero consumers
outside `ui/settings/` + Backup/Profile round-trip (Backup/Profile persistence
alone is not a live effect).

| Key | Screen | Control | Evidence it is dead |
|---|---|---|---|
| `accent_mode` value 3 ("Wallpaper-derived") | 1 Style & Theme | 4th option of the Accent choice | `theme/Looks.kt:97` applies accent only for modes 1/2; mode 3 falls through to Minimal default. UI text says "(stored only)". |
| `font_weight` | 1 Style & Theme | Choice (Normal / Medium / Bold) | Zero reads outside Settings; Cat1 text: "Stored only — launcher text uses fixed weights". Only the Cat1 sample text changes. |
| `font_scale_pct` | 1 Style & Theme | Slider 80–130% | Zero reads outside Settings; only the Cat1 sample scales. |
| `cols_tablet` / `rows_tablet` | 2 Home screen | Sliders ("Tablet columns/rows") | `gridCells()` has no tablet branch (admitted in the row text); zero home consumers. Note: this is the tablet the user actually uses. |
| `margin_dp` | 2 Home screen | Slider "Home margins" 0–32dp | Zero reads; row text admits the live padding is `gap_dp` (which is itself dead — see below). |
| `cell_spacing_dp` | 2 Home screen | Slider "Cell spacing" 0–24dp | Zero reads; same admission as `margin_dp`. |
| `gap_dp` ("Cell padding") | 2 Home screen | Slider 0–12dp, labelled "live on home" | FALSE LABEL: zero reads in `Home.kt`/`ScrollHome.kt`; scroll rows hardcode their own padding (`ScrollHome.kt:516,573,605,687,718`). Only the Cat2 GridOverlay sample uses it. |
| `label_lines` | 2 Home screen | Choice (1/2/3 lines) | Zero reads; scroll rows hardcode `maxLines = 1` (`ScrollHome.kt:534` region). |
| `label_color_argb` | 2 Home screen | Colour swatches | Zero reads; labels always use `palette.onSurface`. |
| `search_bar_pos` / `search_bar_style` / `search_bar_height_dp` | 2 Home screen | Choice / Choice / Slider + mini sample | Zero reads; the scroll home renders no search bar (dock search pill is the separate, working `dock_search` — which itself has no Settings row, see §5). Only the Cat2 sample box changes. |
| `today_page_on` / `today_page_content` | 2 Home screen | Switch + Choice + sample card | Zero reads; no Today page is rendered by home (pages were deleted in M5). Sample card only. |
| `status_bar_mode` | 2 Home screen | Choice (System / Dark / Light / Hidden icons) | Zero reads; row text admits system status-bar theming is limited. |
| `wp_parallax` | 3 Wallpaper | Switch, labelled "live" | FALSE LABEL: zero reads in home/wallpaper code; pages (the thing parallax would track) no longer exist. |
| `wp_blur_pct` | 3 Wallpaper | Slider 0–100% | Zero reads; row text: "wallpaper blur is not yet driven by this value". |
| `wp_auto_dim` | 3 Wallpaper | Switch | Zero reads; row text: "not yet applied by the wallpaper renderer". |
| `wp_per_page` | 3 Wallpaper | Switch | Zero reads; row text: "one wallpaper per home page is not yet rendered". |
| `dock_rows` | 4 Dock | Choice (1 row / 2 rows) | Zero reads; row text: "the dock renders one row today". |
| `dock_floating` | 4 Dock | Switch | Zero reads; dock is always the floating pill. |
| `dock_material` / `dock_opacity_pct` | 4 Dock | Choice (Glass/Solid/Transparent) + Slider | Zero reads; dock background is always the glass pill. |
| `dock_magnify` | 4 Dock | Switch + [A] demo | Zero reads; the demo loops regardless of the switch ("demo above still plays the sample" when off). The switch changes only its own result line. |
| `dock_labels` | 4 Dock | Switch | Zero reads; dock icons never render labels. |
| `drawer_cols` | 5 App drawer | Slider + "Auto" reset | Zero reads once moved off Auto; grid uses fixed `GridCells.Adaptive(76.dp)` (`Drawer.kt:605`). Row text admits it. |
| `drawer_icon_dp` | 5 App drawer | Slider + "Auto" reset | Zero reads once moved; drawer icons follow the home icon size. |
| `drawer_search_on` | 5 App drawer | Switch | Zero reads; the drawer search field is always rendered (`Drawer.kt:390` region, unconditional). |
| `drawer_recents_on` | 5 App drawer | Switch | Zero reads; no recents row is gated by it (drawer recents exist only as search-first suggestions/history, ungated). |
| `drawer_suggestions_on` | 5 App drawer | Switch | Zero reads; suggestions in search-first mode are ungated (`Drawer.kt:629`). |
| `per_app_icon_json` | 6 Icons ("Per-app override") | Add-by-key dialog + list + Remove | The icon renderer does not read it (admitted in the section's own InfoRow). `data/Apps.kt:298` defines the lookup but has no callers; the "Edit icon" dialog in `Home.kt:1485` only displays the stored value. Note: `Home.kt:1473`'s comment claiming `AppRepository.icon` reads it is stale/wrong. |
| `folder_preview` | 7 Folders | Choice (Grid / Stack / Fan) + sample | Zero reads; file header: "All folder values are stored; the FolderSheet renderer does not read them yet." Sample only. |
| `folder_anim` | 7 Folders | Choice (Fade / Scale / Slide) + [A] demo | Zero reads; demo is the generic sample, not the chosen animation. |
| `folder_size_pct` | 7 Folders | Slider 50–150% | Zero reads; sample only ("sample above scales with it"). |
| `folder_bg` | 7 Folders | Choice (Glass / Solid / Transparent) | Zero reads; sample only ("sample above changes with it"). |
| `folder_title_on` | 7 Folders | Switch | Zero reads; sample only ("sample above hides/shows it"). |
| `clock_page` | 8 Widgets ("Built-in clock") | Switch, described as "the full-screen clock page at the end of the home pages" | Zero reads anywhere. The `CLOCK_PAGE` widget kind (`Widgets.kt:160,1203`) is an independent widget-picker item, not gated by this switch; home has no pages for it to sit at the end of. |
| `auto_add_apps` | 2 Home screen (old row; value still stored) | (Legacy switch; no current Cat2 row) | Explicitly stored-only per `Home.kt:390` comment: the scroll home's A–Z list shows new installs by itself, so the old auto-add pass was removed and the flag with it. Still exported/imported by Backup. |

Placeholder actions (no store key; buttons that open an honest "does not exist yet" dialog):
- "Replay intro" (11 Animations, `SettingsCat11.kt:217-218` + dialog) — the §7 intro does not exist; the dialog says so.
- "Replay tutorial" (14 System, `SettingsCat14.kt:321` + dialog) — the §7 tutorial does not exist; the dialog says so.

NOT counted as dead (honest by design, keep): a gesture or nav trigger deliberately
bound to "None"/"Nothing" does nothing because the user chose no action
(`SettingsCat9.kt:613-615`, `:368`, `:380` — "Runs: …" lines, see §6); the
Cat14 "Hide navigation bar everywhere" row with no Fix button (ADB-only, stated);
permission-gated rows that disable with a reason until granted.

## 4. Defects found (v2 §1 items, with file references)

### 4.1 Tier tags [L] / [M] / [T] / [–] / [A] in user-visible text
The tier-tag system itself is still rendered throughout. Section labels:
`SettingsCat1.kt:94,147,155,164` ([L]), `:183` ([M sample]); `SettingsCat2.kt:113`
([L + ghost]), `:252,281,346` ([L]), `:382` ([M sample]), `:414` ([–/L]);
`SettingsCat3.kt:117,214` ([L]), `:277` ([M]); `SettingsCat4.kt:59` ([L sample]),
`:93,121` ([L]), `:138` ([A demo]), `:154` ([L/–]); `SettingsCat5.kt:175` ([M]),
`:190` ([T test pad]), `:214` ([L]), `:250` ([M sample]), `:287` ([L]), `:310`
([M]), `:357` ([–]); `SettingsCat6.kt:132` ([M, 12 sample icons]), `:197,222`
([L]), `:229` ([M — stored list]), `:262` ([T sample badge]), `:296` ([stored]);
`SettingsCat7.kt:139` ([M]), `:162` ([A demo]), `:179` ([L]). PreviewSlot tier
lines: `SettingsCat8.kt:92` ("[L] live"), `SettingsCat9.kt:206` ("[T] test pad"),
`SettingsCat10.kt:88` ("[T] sample query"), `SettingsCat11.kt:98` ("[A] demo"),
`SettingsCat12.kt:88` ("[–] none"), `SettingsCat13.kt:221` ("[M] mini"),
`SettingsCat14.kt:197` ("[–] none"). Tag definitions: `PreviewTiers.kt:22-26`.

### 4.2 "stored only" / "not yet driven" / "does not read" texts (all §3 keys above)
`SettingsCat1.kt:56-58` (comment), `:112,192,199`; `SettingsCat2.kt:171,179,186,
256,265,315,340,349,355,361,385,390,422`; `SettingsCat3.kt:231,239,244`;
`SettingsCat4.kt:62,105,117,124,131,141,157`; `SettingsCat5.kt:220,237,290,295,300`;
`SettingsCat6.kt:233`; `SettingsCat7.kt:39` (header comment), `:142,165,183,191,197`.
"sample above" wording: `SettingsCat7.kt:183,191,197`. No literal `TODO` was
found anywhere under `ui/settings/` (grep clean) — the dishonesty is in shipped
explanation strings, not TODO markers.

### 4.3 "Runs: None" / "Runs: Nothing" texts
`SettingsCat9.kt:613-615` (TriggerRow: "Runs: ${actionName(action)}", hence
"Runs: None" when bound to none, and "Runs: … · also bound to …" on conflict);
`:368` and `:380` (nav rows: "Runs: ${navActionName(action)}", hence
"Runs: Nothing"). These are honest displays of a deliberate no-op binding, NOT
dead controls — do not remove the None option; only the "Runs:" phrasing is a
§1 flag if the redesign drops it.

### 4.4 False "live" / "everywhere" labels (label contradicts the code)
- `SettingsCat2.kt:271` "Cell padding — live on home" (`gap_dp` dead, §3).
- `SettingsCat3.kt:218` "Parallax — live" (`wp_parallax` dead, §3).
- `SettingsCat2.kt:252` "Margins, spacing, padding [L]" — all three sliders dead.
- `SettingsCat2.kt:346` "Search bar [L]" — all three controls dead.
- `SettingsCat4.kt:93` "Slots & rows [L]" and `:121` "Background [L]" — rows/material/opacity dead; only Slots + Position in those sections work.
- `SettingsCat5.kt:214` "Columns & icon size [L]" — both sliders dead once moved.
- `SettingsCat5.kt:287` "Search, Recents, Suggestions [L]" — all three switches dead.
- `SettingsCat7.kt:179` "Size, background, title [L]" — all three dead (their own explanations say "Stored").
- `SettingsCat8.kt:92` widget frame "[L] live" — sample only (§2).
- `SettingsCat11.kt:104` "Global speed — How fast transitions run everywhere" — demos only (§2).
- `SettingsCat11.kt:134` "Reduce motion — everywhere in HavenOS" — scoped (§2).

### 4.5 Stale / wrong in-UI text (opposite direction: working things described as broken, or promises with no backing)
- `SettingsCat9.kt:253-258`: per-icon swipes "stored but do not fire yet" — WRONG.
  They fire via `ScrollHome.kt:522,724`. The `IconSwipes.kt:50` "WIRING HOOK"
  comment is likewise stale (wiring already landed).
- `SettingsCat13.kt` import dialog: "Settings → Home Screen → Undo restores the
  previous layout" — there is NO Undo row in Cat2, and `undo_json` has no reader
  anywhere (only written on import and by `PreviewMini.kt:147`). The promise is unfulfillable.
- `Home.kt:1473` comment: per-app icon "read by AppRepository.icon" — no caller
  of `data/Apps.kt:298` exists; the renderer ignores `per_app_icon_json` (§3).
- `SettingsCat5.kt:266-270` "Custom sort [–]" — Custom appears in the Sort
  choice list but picking it "leaves the sort unchanged" by code
  (`SettingsCat5.kt:259-264` maps only 0–2). A dead option inside a working control.
- `SettingsCat3.kt:277-281` crop frame: for a stored photo it "draws the
  gradient/solid placeholder — the photo bitmap needs the WallpaperStore, not
  this screen" — the crop preview never shows the user's actual photo.
- `SettingsCat4.kt:138-152` dock-magnify demo plays even when the switch is off
  (stated in UI) — the demo is not driven by the control it sits under.
- `SettingsCat6.kt:197-214` shape choice omits Square from the main list
  (Square only via a separate ActionRow) while offering two dead options
  (Teardrop, System) in the main list.

### 4.6 Duplicated Reset controls (three layers, with scope mismatches)
1. Shell Reset button on every category screen 1–13 (`SettingsHub.kt:797-800`,
   confirm dialog `:800-810`, implementation `resetNewCategory`,
   `SettingsHub.kt:335-398`).
2. In-screen `ResetSectionRow` in Cat8 (`SettingsCat8.kt:381`), Cat9
   (`SettingsCat9.kt:458`), Cat10 (`SettingsCat10.kt:257`), Cat11
   (`SettingsCat11.kt:225`), Cat12 (`SettingsCat12.kt:166`), Cat13
   (`SettingsCat13.kt:397`).
3. Cat13 "Reset all (categories 8–14)" (`SettingsCat13.kt:410` + dialog
   `:420-470`), plus the Cat14 InfoRow (`SettingsCat14.kt:366-370`) explaining
   that resets live elsewhere.
Scope mismatches: Cat12's shell Reset ONLY unlocks the hidden-apps lock
(`resetNewCategory` id 12 → `setHiddenLock(false)`; dialog even says "Hidden
apps stay hidden"), while Cat12's in-screen Reset unhides ALL apps AND unlocks
the lock AND unlocks the layout. Cat8's in-screen Reset covers clock page,
at-a-glance, weather units/network, calendars and stacks; the shell Cat8 Reset
covers only `resetSection(6)` + widget frame. Cat13's in-screen Reset keeps
profiles; "Reset all" deletes them — two same-screen resets with opposite
profile behaviour. Cat14 has no Reset at all (correct — grants/info only).

### 4.7 Letter-tile category tiles
`SettingsHub.kt:822` `CategoryIcon`: a coloured rounded square showing
`title.first()` — every hub/tablet nav row carries a first-letter tile. The
same letter-tile device is used for sample icons in `SettingsCat4.kt:59-90`
(dock preview chips), `SettingsCat5.kt` `StyleMini`, and `SettingsCat6.kt`
sample icons — samples, not navigation, but the same visual device.

### 4.8 Dotted slider tracks — NOT FOUND in code
`SliderRow` (`SettingsHub.kt:536-559`) uses a stock Material3 `Slider` with no
custom track, thumb, or dotted styling anywhere in `ui/settings/` or the
theme. If dotted tracks appear on device they come from the M3 default
rendering/theme, not from app code — no dotted-track code exists to remove.
(Flagged here so the v2 phase does not hunt for code that is not there.)

### 4.9 1px-outlined group boxes
There is no separate "group box" component. Cards ARE `GlassSurface`
(`theme/Glass.kt`: 1px specular border + inner highlight is the Minimal glass
treatment itself). Explicit 1.dp borders elsewhere: Cat2 ghost-overflow card
(red, `SettingsCat2.kt:233`); `PreviewDemo.kt:90,188`,
`PreviewTestPads.kt:84,178`, `PreviewTiers.kt:35` (glass-border sample frames);
highlight borders are 2dp (`SettingsHub.kt:503`). Removing "1px group boxes"
therefore means changing `GlassSurface`, not deleting a box component.

### 4.10 Paragraph helper cards (InfoRows carrying multi-sentence explanations)
Longest offenders (each is a full paragraph inside a settings row, not a
one-line helper): `SettingsCat10.kt:205-222` (Files / Shortcuts / Web-search
"not built yet" cards + Suggestions/Learning cards); `SettingsCat9.kt:233-243`
(Swipe left/right + Recents/launch-app/toggle-dock info cards — honest, but
paragraph-length); `SettingsCat14.kt:220-228` ("Denied permissions lock their
rows" card) and the Sideloaded-install `GlassSurface` card
(`SettingsCat14.kt:295-320`); `SettingsCat8.kt:183-190` (No-location-needed +
source cards); `SettingsCat3.kt:277-281` (crop-frame explainer). The Cat10
Files/Shortcuts/Web rows are honest "not built, no toggle" placeholders —
they are info, not fake controls — but they are paragraph cards under §1.

### 4.11 Stale settings-search index (search leads to rows that do not exist)
`SettingsHub.kt:140-260` search entries are category-level only (row highlight
"accepted but not rendered" per COVERAGE) and still index the OLD layout:
"Density" → cat 1 (no density row in Cat1; the control lives in the Quick
panel); "Icon shape" / "Icon labels" → cat 1 (shape lives in Cat6, labels in
Cat2); "Placement" → cat 2 (no placement picker; home is scroll-only);
"Clock page" → cat 2 (the switch lives in Cat8); "Glass quality" → cat 3 (no
such row); "Apply to system wallpaper" / "Apply to lock screen" → cat 3 (no
such action exists anywhere — no `WallpaperManager.set*` call in the app;
COVERAGE already corrected this claim); "Dock recent apps" / "Dock auto-hide" / "Dock search pill" /
"Dock app-drawer button" → cat 4 (no such rows in Cat4 —
see §5). Searching any of these lands the user in a category that does not
contain the named control.

## 5. Live values with NO control (not removal candidates — decision needed)

These keys are read by real code but have no Settings row at all, so the user
cannot change them from Settings today (only via a restored backup or an older
build). They are NOT in the §3 removal list — the next phase must either add a
control or delete the key:
- `dock_recent`, `dock_auto_hide`, `dock_search`, `dock_drawer_btn` — all read
  by the dock (`Home.kt:414,503,1296,1357-1394`); Cat4 shows no switches for
  them, only search-index entries (§4.11) and a preview line mentioning recents.
- `surface_blur_pct`, `surface_noise_pct`, `shadow_dp`, `outline_on` — stored,
  reset by the shell Cat1 Reset, applied by `PreviewMini`, but no Cat1 rows;
  code comments confirm blur/noise/shadow/outline are not consumed
  (`Settings.kt` field comments; `GlassSurface` always draws its border).
- `look`, `custom_looks_json` — Looks-system remnants; `look` normalizes to
  "Minimal" on read, `custom_looks_json` is never read by the theme. Kept only
  for backup round-trip.
- `placement_mode`, `scroll_effect` — page-era remnants; home always renders
  `ScrollHome` (`Home.kt:653`) and never reads either.
- `undo_json` — written on backup import (`SettingsCat13.kt` import flow) and
  by `PreviewMini`, but never read; the promised Undo row does not exist (§4.5).
- `nav_saved_policy` / `nav_saved_policy_set` — internal bookkeeping for the
  nav-bar hide/restore cycle (`HavenAccessibilityService.kt:79-112`); not a
  user setting.
- `seeded` — first-run seed flag (`Home.kt:372`); internal.
- `last_backup_day` — displayed as info in Cat13 ("Last ran …"), not a control.

## 6. Counts

| Status | Keys | Notes |
|---|---|---|
| Working | 62 | §1. Plus 11 working non-key actions (export/import, resets, fixes, etc.). Gated-but-working items (permissions, ADB grant, device admin, notification access) counted here, with their gate stated. |
| Partial | 19 | §2. Of these, 7 are sample/pad/demo-only in effect (widget frame ×3, gesture sensitivity, anim speed, press feedback, haptic strength); the rest work in a subset of modes/places. |
| Not wired (visible, removal candidates) | 39 keys + 2 placeholder actions | §3: Cat1 3 (incl. the wallpaper-derived accent option) · Cat2 13 · Cat3 4 · Cat4 6 · Cat5 5 · Cat6 1 · Cat7 5 · Cat8 1 · legacy `auto_add_apps` 1; plus Replay intro + Replay tutorial. |
| Live but no control (decision needed, NOT removal candidates) | 16 | §5. |
| Total store keys inventoried | 137 | 62 + 19 + 39 + 16 + `last_backup_day` (info display, §5 note) ≈ every field of `SettingsState`. |

Headline for the next phase: over a quarter of the visible settings surface
(39 of 120 visible keys) is dead, and the single biggest clusters are Cat2
Home screen (13 dead keys — the home is scroll-only, so the entire grid /
search-bar / Today-page / margin / spacing apparatus is vestigial) and Cat7
Folders (all 5 keys dead — the folder renderer reads none of them). The most
misleading rows are the ones labelled "[L] live" that are in fact dead:
`gap_dp` ("Cell padding — live on home"), `wp_parallax` ("live"), the Cat5
Search/Recents/Suggestions trio, and the Cat7 trio.
