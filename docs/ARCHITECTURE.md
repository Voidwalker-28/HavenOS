# Architecture

Short map of the HavenOS codebase. Package: `app.havenos.launcher`.
Kotlin + Jetpack Compose, Room + DataStore, minSdk 26, targetSdk 36.

## Entry points

- `HavenApp.kt` — application setup.
- `MainActivity.kt` — the launcher activity and home host.
- `HavenAccessibilityService.kt` — the accessibility service behind the
  own gesture layer (edge strips, Home / Recents / Back). Declared in
  `AndroidManifest.xml` with its `accessibility_service_config.xml`.

## UI (`ui/`)

- `Home.kt` — the home shell: four zones (Atmosphere, Today rail,
  Workspace, Dock), the persistent `DockHost` outside the pager, grid
  reservation, page indicator, and overlay routing.
- `PagedHome.kt` — the horizontal pager, page overview, Add / Remove
  Page, drag handling between pages.
- `Dock.kt` — the taskbar dock: pill / vertical rails, pinned · divider ·
  recents · Search · Library, width-computed slots (icons ≥ 48dp),
  drag in/out, icon contrast check.
- `TodayRail.kt`, `TodayPage.kt`, `TodayStack.kt` — Today on Expanded
  (rail), Medium (strip), and Compact (pager page), plus the card flow.
- `Drawer.kt`, `LibrarySearch.kt`, `CategoriesView.kt` — the Library:
  All / List / Categories, ranked search, A–Z scrubber.
- `Grid.kt`, `Occupancy.kt`, `PageMigration.kt` — grid layout and the
  single occupancy-aware engine (validate / push / reflow), plus
  flow→pages migration.
- `EditMode.kt` — edit-mode toolbar, context bar, collision policy,
  undo, snapshots, Restore layout.
- `FolderSheet.kt` — folder panel (4-column grid) and previews.
- `HostWidgets.kt`, `WidgetReal.kt`, `WidgetReal2.kt`, `Widgets.kt` —
  real `AppWidgetHost` hosting, the widget picker with live previews,
  and the built-in widgets (clock seed, at-a-glance).
- `CalmMode.kt`, `CalmEditorScreen.kt` — Calm mode and its editor.
- `Legibility.kt` — label legibility engine and Pill fallback.
- `HomeTokens.kt`, `HomeMotion.kt`, `HomeHaptics.kt`, `HomeA11y.kt`,
  `MotionSpeed.kt`, `QuickPanel.kt` — tokens, motion, haptics,
  accessibility hooks, and the quick panel.
- `ui/settings/` — the Settings shell (`SettingsPanel.kt`,
  `HubScreen.kt`, `SettingsSearch.kt`, `SessionDiff.kt`, …) and the six
  hubs (`HomeDockHub`, `DrawerSearchHub`, `GesturesMotionHub`, `LookHub`,
  `DeviceDataHub`, `PrivacyHub`).

## Data (`data/`)

- `Db.kt` — Room database: layout, folders, snapshots.
- `LayoutStore.kt`, `LayoutState.kt`, `LayoutProfiles.kt`,
  `LayoutSnapshots.kt`, `LayoutCoordination.kt` — layout state, profiles
  per size class, snapshots, and coordination.
- `Settings.kt` — DataStore settings, including accent mode (Default /
  Palette / Custom) and the gesture and weather toggles.
- `Apps.kt`, `Backup.kt`, `CalendarRepo.kt`, `Tasks.kt`,
  `Notifications.kt`, `Profiles.kt`, `IconPacks.kt`, `IconSwipes.kt`,
  `Wallpaper.kt`, `Weather.kt`, `Gestures.kt`, `NavGestures.kt`,
  `Calc.kt`, `CalmEditor.kt` — app listing, backup, calendar, tasks,
  notifications, profiles, icons, wallpaper, weather (Open-Meteo only),
  and gesture configuration.

## Theme (`theme/`)

- `Looks.kt` — Minimal-only tokens. There is exactly one look:
  `Look.Minimal`; stored legacy look values are ignored backup history.
- `Glass.kt` — the glass surface treatment used sparingly inside
  Minimal (dock pill, widget surfaces, panels).

## Permissions

`INTERNET` (weather only), `READ_CALENDAR`, `READ_CONTACTS`,
`SET_WALLPAPER`, `SYSTEM_ALERT_WINDOW`, `WRITE_SECURE_SETTINGS`.
See `AndroidManifest.xml` for the authoritative list.
