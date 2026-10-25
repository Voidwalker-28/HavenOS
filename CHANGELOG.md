# Changelog

All notable changes to HavenOS are documented here. HavenOS is a launcher;
version numbers below are the launcher's own (`versionName` stays `1.0`,
`versionCode` increments per release so builds stay distinguishable).

## [1.0] — 2026-10-25 — versionCode 10

First public version.

### Dock correction (2026-10-25)

The dock reported missing on every screen is restored as a single
persistent component:

- One dock, living outside the pager as a persistent shell component —
  the same dock, with the same contents, on every page.
- The grid reserves exactly the dock's space, so the bottom row no longer
  touches the screen edge; the page indicator sits above the dock.
- Floating glass pill, bottom-centre by default, with Left/Right vertical
  rails.
- Contents: pinned apps · divider · up to 3 recents · Search · Library.
  Slot count is computed from width; icons never shrink below 48dp.
- Drag in from the Library and drag out in edit mode.
- Icon contrast checked against the dock material.
- Visible across home pages, the Today page, and edit mode. Full-screen
  overlays sit above it; Calm mode covers it; optional auto-hide dims it.

### Four-zone shell (M10)

- Home shell split into four zones: Atmosphere, Today rail, Workspace,
  Dock — the dock moved outside the pager as a persistent shell
  component.
- Today rail (~30%) beside the workspace (~70%) on Expanded landscape;
  collapsible Today strip on Medium; Today pager page on Compact.
- One occupancy-aware layout engine (validate / push / reflow) that never
  silently overlaps items; widget host grid now matches the pager grid.
- Grid settings are real: quick-tune writes both orientations and reflows
  with ghost cells and destination pages.
- Density levels: Spacious, Balanced (default), Dense, Custom.
- Tablet master-detail Settings with no scrim; the dock no longer sits
  under the Settings panel.
- Two-level Customize: quick bar plus focused editing, with the selected
  item's name and widget Replace.
- Minimal made truly minimal (three semantic surfaces only).
- One page-transition spec; legibility Pill fallback when no per-cell
  wallpaper statistics exist; Library scrubber enlarged to 48dp.
- Home composition deduplicated (the home was previously composed twice).

### Carried from earlier milestones

- Horizontal paged home with Add Page / Remove Page (last-page guard,
  adjacent-move with undo, no silent item loss).
- Library: All / List / Categories, ranked instant search, alphabet rail
  with scrubber.
- Edit mode: 56dp toolbar, context bar, collision policy, 50-level undo,
  snapshots, Restore layout.
- Real hosted widgets (including third-party) with live picker previews;
  one clock card seeded on fresh install.
- Calm mode with a real editor (focus list up to 8 apps).
- Accent colour: Default / Palette / Custom only (no wallpaper-derived
  accent; a stored legacy value falls back to the Minimal default).
- Accessibility-powered own gesture layer (off by default).
- Optional Open-Meteo weather (switchable off; cached data only when off).

### Known limitations (honest list)

- Folder preview modes are not built (previews are size-driven only).
- Two-line label reserve is not built.
- "Send to Today" is not built.
- The full first-run startup experience (intro animation, welcome,
  permissions screen, build-your-home, interactive tutorial) is partial;
  Replay intro/tutorial are placeholders.
- TalkBack Move-to-page is not attached; pinch-in page overview is not
  wired.
- Widget-stack member payload persistence is partial.
