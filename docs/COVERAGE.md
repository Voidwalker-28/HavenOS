# Coverage — what HavenOS does, and what it does not (yet)

HavenOS is a launcher. This page is the honest ledger: **Built** means it
ships and works in the current release; **Partial** means it ships with a
known gap; **Not built** means it is absent, with the reason. Nothing is
silently dropped — if a feature is not here, it is listed below.

Current release: **v1.0 (versionCode 10)**.

## Built

- Horizontal paged home: pager, indicator, Add Page, Remove Page
  (last-page guard, adjacent-move with undo, no silent item loss).
- One occupancy-aware layout engine (validate / push / reflow); widget
  host grid matches the pager grid.
- Grid settings for both orientations with reflow, ghost cells, and
  destination pages; density Spacious / Balanced / Dense / Custom.
- Edit mode: 56dp toolbar, context bar, collision policy, 50-level undo,
  layout snapshots, Restore layout.
- Persistent taskbar dock outside the pager: floating glass pill,
  Bottom / Left / Right positions, pinned · divider · up to 3 recents ·
  Search · Library, width-computed slots, icons never below 48dp, drag
  in/out, grid reservation, page indicator above the dock,
  contrast-checked icons.
- Today: rail on Expanded landscape (~30/70), collapsible strip on
  Medium, pager page on Compact; card flow with agenda, weather, Smart
  Stack.
- Library: All / List / Categories, ranked instant search, search pill,
  48dp A–Z scrubber.
- Folders: size-driven previews (3×3, large 2×2 with quadrant tap),
  4-column panel.
- Real hosted widgets via `AppWidgetHost`, including third-party
  widgets, with live picker previews; one clock card seeded on fresh
  install; transparent glass widget surfaces.
- Calm mode: focus list up to 8 apps, real editor (apps, schedule, entry
  options).
- Accent colour: Default / Palette / Custom only. No wallpaper-derived
  accent; a stored legacy value falls back to the Minimal default.
- Accessibility-powered own gesture layer (off by default): bottom edge
  up = Home, up-and-hold = Recents, side edges = Back; remappable.
- Optional Open-Meteo weather (switchable off; cached data only when
  off). The app's only network feature.
- Settings: six hubs, search-that-acts, Before/Undo/Changes session
  trail, backup import with undo, tablet master-detail layout.
- Label legibility engine with Pill fallback when no per-cell wallpaper
  statistics exist.
- App lifecycle handling, pin requests, layout profiles per size class.
- No accounts, ads, analytics, or cloud sync.

## Partial

- First-run startup experience: splash, welcome, and settings entry
  exist, but the full guided flow (intro animation, permissions screen,
  build-your-home, interactive tutorial) is incomplete; Replay
  intro/tutorial are placeholders.
- Widget-stack member payload persistence.
- Hosted-widget measured row heights.
- Per-section Reset for older settings sections.
- Countdown uses UTC rather than local midnight.
- TalkBack Move-to-page is not attached.
- Pinch-in page overview is not wired.
- Wallpaper: reading the system wallpaper bitmap is blocked by the OS
  (SecurityException on API 33+), so full wallpaper-based legibility is
  only available for HavenOS-mode wallpapers.

## Not built

- Folder preview modes (Compact grid / Stack / Large) — previews are
  size-driven only.
- Two-line label reserve.
- "Send to Today" action.
- Themed monochrome / tint icon treatment.
- Split-view / app pairs.
- Media card.
- Stack sync across pages.
- Shared layout between devices.
- Online themes store (deliberately dropped: the launcher stays
  offline-first with weather as the only optional network feature).
- Lockscreen replacement (not possible for a third-party app on Android).

## Design removals (deliberate, not gaps)

- Look presets, Look Editor, custom looks, and look export/import were
  removed: HavenOS is Minimal-only, with accent colour as the single
  theme control.
- Home layout templates were removed: layouts are built by hand, not
  picked from presets.
