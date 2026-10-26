# Changelog

All notable changes to HavenOS are documented here. HavenOS is a launcher;
`versionName` marks the major line, `versionCode` increments per release
so builds stay distinguishable.

## [2.0] — 2026-10-26 — versionCode 11

API integration. The offline-only rule is lifted: the launcher may now use
public network APIs, each behind its own toggle, with cache-first behavior
so it stays instant offline.

### Network foundation

- One shared API client with a per-API host allowlist enforced in code.
- Room cache + WorkManager refresh per API; stale data shows with a
  "last updated / offline" state, never a blank screen or endless spinner.
- New Settings → APIs hub: one toggle per API, no new permissions beyond
  INTERNET, no analytics, no hardcoded keys.

### Today feed cards (each toggleable)

- Quote of the day (Quotable).
- Live scores (SportScore — cricket + football, with attribution).
- Crypto ticker (CoinGecko — key-gated: needs a free user-supplied Demo
  key in Settings → APIs; off by default until a key is entered).
- NASA picture of the day (public DEMO_KEY by default, personal-key
  override available; optional set-as-HavenOS-wallpaper action).
- GitHub card (public data only, username configurable).
- Weather (Open-Meteo) unchanged.

### Search extras

- Currency conversion rows in Library search (currency-api, daily rates).
- Dictionary definition rows in Library search (Free Dictionary).

### Known limitations (device checks still open)

- Quote card could not be verified from the build environment (source
  unreachable there); on-device check pending.
- Refresh cadences, 429 behavior, and the Room 4→5 migration on an
  existing install need on-device confirmation.

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
