# Changelog

All notable changes to HavenOS are documented here. HavenOS is a launcher;
`versionName` marks the major line, `versionCode` increments per release
so builds stay distinguishable.

## [3.1.1] — 2026-10-31 — versionCode 14

Crash hotfix on top of 3.1, plus one follow-up fix to the same build
(the fixed APK swaps into this release in place; the version number
does not change for a fix).

### Crash sweep and hardening

- Every throwing lookup (`firstNotNullOf`, `first`, `single`, `last`,
  `maxOf`, `minOf`, `getValue`, `!!`, `requireNotNull`) reachable from
  coroutines or the mascot, Find, island, ledge, search and dock code
  replaced with an OrNull variant and a defined fallback. The mascot
  planner never throws on zero ledges: it stays put or respawns on the
  dock top.
- Failure isolation: mascot, island, Find and online-card code run in
  supervised scopes. On an exception the feature is disabled for the
  session, the error is logged to local-only Diagnostics, and one toast
  is shown. Feature failures never reach the Activity.
- Crash-loop protection: an UncaughtExceptionHandler writes a small
  timestamped marker, then delegates. Two or more crashes within 60
  seconds start the next launch in Safe Mode (mascot, island, online
  workers and blur off) with a recovery banner, Re-enable and View
  details. Safe Mode clears after 10 minutes of stable running or on
  re-enable. Diagnostics shows the last crash (class, message, time,
  versionCode), local only, shareable by the user.
- Release traces keep line numbers (`-keepattributes
  SourceFile,LineNumberTable`). Mapping files are retained per release
  under `release-artifacts/mapping-<versionCode>.txt`, starting with
  vc14 — the vc13 trace cannot be retraced because no mapping was kept
  for it. Retrace command documented in BUILDING.md.

### Touch, Settings, drawer, bubble

- Touch pass-through: the mascot overlay never intercepts touches
  outside the sprite hitbox and the bubble; the hitbox follows the
  drawn position; a swipe dismisses the bubble and still scrolls the
  pager. The frame loop runs only while moving.
- Settings layout fix: panel width clamps to 640–920 dp (56% of the
  window); rail collapses to icon-only 72 dp under 900 dp and expands
  to 200 dp above; single-pane navigation under 560 dp; detail pane
  at least 360 dp. Setting rows stack (label on top, value + control
  beneath) instead of crushing labels. Segmented controls use equal
  segments of at least 64 dp or wrap/scroll. API key fields put the
  label above a full-width field with a 72 dp button row.
- Drawer fully solid: a new `drawerSurface` token (dark near-black
  with ~4% accent at alpha 1.0, light off-white, 4.5:1 contrast),
  translation-only opening, 40% scrim behind, solid search field,
  A-Z rail and letter bubble. Glass remains only for floating things
  (dock, Today cards, menus, popovers).
- Mascot bubble to the E3 spec: pixel-stepped border, readable text,
  four 48 dp icon chips (Find, Help, Suggest, Dismiss),
  keyboard-aware placement.

### Fix-1 to this build (same versionCode 14)

- The first vc14 build crashed on device with `NoSuchElementException:
  ArrayDeque is empty`, retraced via the retained vc14 mapping to the
  mascot route planner's BFS: the 3.1.1 fix had added a fallback after
  the lookup while the throw happened inside it (one `removeFirst`
  per ledge tested). Fixed by removing the queue head once, outside
  the lookup, with a regression test. All other `removeFirst` sites
  audited guarded or OrNull.

### Known limitations (device checks still open)

- Everything visual or gesture-related in this hotfix is built but not
  device-verified: Pad checks H-1…H-10 in `docs/DEVICE_CHECKLIST.md`.
- The PagedHome `!!` sites (`appEntry`, `folder`, ghost `overCell`)
  were deliberately left out of this sweep and need a scoping decision.

## [3.1] — 2026-10-30 — versionCode 13

Haven Glass design system plus the pixel mascot and on-device assist,
built in phases A–G on top of the 3.0 coordination fix.

- Phase A: 13 data, coordination and edge fixes (score/status
  tables, one-place failure classification, crypto 3-state + key
  deep-link, page-leak clip, glance rule, rail dock inset, 48 dp dock
  tiles, shared-axis transitions, durable undo grid-override frames,
  legibility cache split, nav-bar restore, paused-service banner).
- Phase B: `ui/glass/` design system (Glass1/2/3, HavenIcon vectors,
  full accent system — Wallpaper as suggestion only, System, 12 named
  Curated colors, Custom + strength) and the purple-Default-card root
  fix.
- Phase C: Glass on every non-home surface; fixed a real bug where the
  quick-customize Widgets branch did nothing when tapped.
- Phase D: home search widget as one Glass card.
- Phase E: pixel mascot rebuilt (Milo + Mila original 24×24 sprites,
  ledge engine with clearance rule, BFS routing, tracked Find flow,
  hub with animated preview, tips and quiet hours). Old vector kept
  behind a flag until the pixel art is accepted.
- Phase F: on-device assist seam (compound command grammar, sealed
  whitelist router, AI Privacy Ledger) with the model pack as a
  scaffold only (no runtime wired, no URL or checksum configured).
- Phase G: verification, docs, consolidated checklist, version stamp.

### Known limitations (device checks still open)

- Zero tests ran across 3.1: the test tree does not compile
  (305 pre-existing errors, mostly `kotlin.test` imports against a
  JUnit4-only declaration). Lint shows 16 pre-existing errors.
- The manual PDF still describes 3.0; its source is updated to 3.1.
- Quick tune Mode gained a System option and the C5 toolbar group
  boundaries are the implementer's reading — both await confirmation.
- SearchCard command rows and the remaining Phase E blocked items
  (pixel-stepped bubble border, per-card rail ledges, side-dock perch,
  dock-app Find rings, guide mid-trip) were deferred; the bubble
  border landed later in 3.1.1.

## [3.0] — 2026-10-28 — versionCode 12

Vector mascot and dynamic island, both default OFF, on top of the 2.0
API foundation.

- Mascot (Milo/Mila): original vector character with scripted
  behavior only — no language model wired. Roams the home, Find flow,
  hub with tips and quiet hours.
- Dynamic island: original implementation (not copied from any
  reference launcher), default OFF, with legibility and polling fixes
  from the coordination pass.
- Coordination fix (all 7 design-doc sections): occupancy engine
  contract, unified mutation path, durable undo/snapshot strategy,
  profile derivation, online worker failure surface, Settings hub and
  panel simplification, implementation order. Six documented
  deviations (density routing blocked, host drag/drop writes not yet
  routed through commit, profiles written but host still renders from
  Room, one grid effect/commit race, Room+DataStore not one
  transaction, DeviceDataHub restore path left direct).

### Known limitations (device checks still open)

- Pad checks COORD-1…15, V3-MASCOT-1…8 and V3-ISLAND-1…9 were still
  owed at release time; nothing in this version was device-verified
  before shipping.

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
