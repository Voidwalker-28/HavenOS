# HavenOS — Doc 3 of 3 — Essential Features, Widgets and How Everything Coordinates

Paste into the builder with Docs 1 and 2. This doc defines **what a launcher must have**, **how widgets work**, and **how every part talks to every other part**.

**The Settings UI is approved and must not change.**

---

## 0. Rules

- Inspect first; keep what works; restart nothing. Preserve user data; version the schema; test migrations.
- Keep the existing stack. No new dependencies unless justified. No network beyond the existing weather feed. No analytics or telemetry; all suggestions and ranking are local.
- Clean-room: patterns only, no copied code or assets.
- Do not publish, release, push, or change signing, package name or version.
- Report only checks you actually ran.

---

## 1. Essential features (most needed first)

**MUST — a launcher is not usable without these.** Check each against the current build; implement or fix what is missing.

| # | Feature | What it must do |
|---|---|---|
| 1 | **Default-launcher flow** | Detect state; banner with "Make default"; after setting, the home button goes to page 1, closes overlays and folders |
| 2 | **Persistent dock** | Always visible, drop target, Bottom/Left/Right (Doc 2 §1) |
| 3 | **Pages and grid** | Cell grid per size class, add/remove/reorder/hide pages, default page |
| 4 | **Icons with legible labels** | Normalized icons, auto-contrast labels, icon packs |
| 5 | **Folders** | Create, rename, reorder, drag in/out, dissolve at 1 app |
| 6 | **Library (drawer)** | All and List views at least, scrubber, search field, drag to home |
| 7 | **Search** | Apps, contacts (permission), HavenOS settings, calculator, web; no overlapping rows; keyboard-safe |
| 8 | **Edit mode** | As Doc 1: move, resize, add, remove, undo, snap |
| 9 | **Widgets host** | Built-in and third-party widgets (§2) |
| 10 | **App lifecycle** | Handle install, update, uninstall, enable/disable and work-profile apps: new apps go to the Library and optionally to the first free home slot (setting); removed apps vanish from home, folders and dock; updated icons refresh |
| 11 | **Pin requests** | Accept shortcut and widget pin requests from other apps through the system flow |
| 12 | **Wallpaper** | System and HavenOS wallpaper; per-page optional; legibility pipeline |
| 13 | **Settings panel with live preview** | As built; behavior per Doc 1 |
| 14 | **Backup and restore** | Export/import; layout and settings; migration-safe |
| 15 | **Layout safety** | Auto-snapshots and restore (§3.6); lock layout |
| 16 | **Cold-start speed** | Draw the saved layout immediately from cache; widgets fill in after |
| 17 | **Rotation and size changes** | Per-profile layouts (Doc 1 §5.10); no scrambling |
| 18 | **Accessibility** | TalkBack actions, keyboard, mouse, 200% font scale |

**SHOULD — expected in a launcher people choose over the stock one.**

| # | Feature | What it must do |
|---|---|---|
| 19 | **Notification badges** | Dots/counts; needs notification access; graceful when denied |
| 20 | **Icon quick actions** | Long-press menu with the app's shortcuts (max 4); optional swipe-up on icon |
| 21 | **Today page / column** | Card flow with Clock+weather, Agenda, Suggested, Battery |
| 22 | **Suggested apps and recents** | Local launch counts and recency; can be cleared |
| 23 | **Library Categories view** | Auto-categories with local overrides |
| 24 | **Hidden apps with biometric lock** | Hidden from layout, Library and search |
| 25 | **Calm mode** | Large-text favorites list, schedule, exit restores everything |
| 26 | **Gestures** | Wired actions only: swipe up/down, pinch, two-finger, long-press, double-tap (opt-in) |
| 27 | **Page overview** | Reorder, move items between pages, add/delete/hide |
| 28 | **Large folders** | 2×2 with 4 direct icons |

**LATER (only after everything above is verified).** Themed monochrome and tint modes · split-view and app pairs · media card · widget stacks sync across pages · shared layout between devices.

---

## 2. Widgets — how they work

### 2.1 Widget kinds
1. **Built-in (HavenOS):** Clock, Weather, Agenda, Suggested apps, Battery, Quick search, Calm favorites. Rendered natively with the shared frame (Doc 2 §6).
2. **Third-party Android widgets** hosted through the platform widget host.
3. **Today cards:** the same built-in widgets placed on the Today page flow (not on the cell grid).

### 2.2 Add-widget sheet
- Opens from **+ Add → Widget** (edit mode toolbar) or the long-press-empty quick bar.
- Left list: **HavenOS widgets first**, then installed apps with widgets, searchable.
- Right: preview carousel with size chips (Small 2×2 · Wide 4×2 · Tall 2×4 · Large 4×4, only sizes the provider allows) and live previews (built-in widgets render real data).
- **Tap** = add to the first free spot searching the current page, then other pages, then offer a new page. **Drag** = place exactly; the home shows a ghost while dragging.
- If a third-party widget needs binding permission or a configure screen, show the system flow, then place it. If the user cancels, nothing is added.

### 2.3 Placement and sizing
- Widgets occupy whole cells (w × h) on the grid. Min and max come from the provider (`minWidth/minHeight`, `resizeMode`, target cell size) or from the built-in table.
- Never overlap another item. Free-spot search: row-major scan for the first rectangle that fits; try smaller allowed sizes if nothing fits.
- Resize via handles and size chips (Doc 1 §3.4). Neighbors reflow live; if impossible the resize clamps.
- Per-profile positions (Doc 1 §5.10): a widget may differ in span between landscape and portrait if its provider requires.

### 2.4 Rendering states (every widget, no exceptions)
| State | Look |
|---|---|
| Loading | skeleton in the frame (never a bare name) |
| Ready | real content |
| Empty | short message + one action (e.g. "No events today · Add event") |
| Needs permission | inline chip "Allow calendar" → rationale → system prompt → live update on return |
| Error | "Couldn't load · Retry" |
| Offline | last good data + "Updated 20:34" |
| Provider removed | "App removed · Remove widget" |

A third-party widget that fails to load shows "Problem loading widget · Tap to reload". It must never crash the launcher.

### 2.5 Built-in widgets in detail

**Weather**
- Data: locality name, temperature, condition, hi/lo, hourly, 5-day.
- Units follow Settings (°C/°F) and are the **same everywhere** (widget, Glance, Today).
- Location: optional permission with a one-sentence reason; fallback is a manually chosen city. Without either: Empty state "Choose a city".
- Name display: locality only ("Delhi"); administrative suffixes are not shown; ellipsize only at the end.
- Refresh: on first show, when stale > 30 minutes and the screen is on, and on tap or pull. No background polling beyond what the existing feature does. Cache last result and timestamp.

**Agenda**
- Source: system calendars (permission). Calendar selection uses the existing list in Settings (toggle per calendar).
- 2×2 next event · 4×2 next 3 events · 4×4 day view with time axis · all-day events first.
- Tap an event opens it in the calendar app; tap the header opens the calendar.
- Updates through a calendar content observer, throttled to once per 30 s; midnight rollover refresh.
- Empty: "No events today · Add event". Permission denied: Needs-permission state.
- **Seed at most one Agenda widget** on a fresh layout.

**Clock:** digital (12/24h follows system) or analog; date; optional second time zone; next alarm; tap opens the clock app.

**Suggested apps:** 4–8 apps from local launch history, recent and time-of-day aware; never uses the network; tap launches; long-press menu works.

**Battery:** level, charging state, estimate if the system exposes it.

**Quick search:** a search field that opens the Search card; no results inside the widget.

### 2.6 Third-party widgets
- Host through the platform widget host; honor provider min/max and update periods; no extra polling.
- Rounded corners: use the system's corner-radius API where available; otherwise clip to the frame radius.
- Optional frame background (None / Glass) from the Home & Dock settings (existing row).
- Uninstalled provider: placeholder with Remove.
- Limit concurrently updating hosts; pause updates when the screen is off.

### 2.7 Widget stacks
- Dropping a widget onto another widget of the same size forms a stack (P1). Vertical swipe cycles; small dots on the right; optional time-based auto-rotation (rules: morning → weather and agenda; evening → media and suggested), paused for 10 s after touch.
- Long-press a stack → Edit stack (reorder, remove, unstack).

### 2.8 Widgets in edit mode
- Keep rendering live content. Selection ring + handles on the selected widget. Context bar: Resize · Remove · Duplicate · Widget settings · Stack.
- Never replace a widget with a name label.

### 2.9 Today page flow
- Cards are laid out as a **flow**, not by cell coordinates: Expanded 2 columns, Medium/Compact 1 column. Drag to reorder cards; resize only between allowed card sizes.
- Today and Home use the same widget objects and the same frame.

---

## 3. How everything coordinates

### 3.1 Single source of truth
- **LayoutState:** pages, items (icon, folder, widget), spans, positions per profile, dock contents, Today cards.
- **SettingsState:** every setting row's value.
- **LookTokens:** derived from preset + overrides + mode + wallpaper-derived values.
- **UiState:** derived (never stored): metrics, label colors, insets, visible zones.
- UI reads state and sends **events**; only the state layer writes. Edits are **transactions** (one gesture = one atomic write).

### 3.2 Who owns what

| Component | Owns | Reads | Emits |
|---|---|---|---|
| Pager | page index, scroll | LayoutState, UiState | page-changed |
| Grid | cell metrics, hit-testing | LayoutState, SettingsState | drop, resize, remove events |
| Dock | slot layout, capacity | LayoutState, SettingsState (position, slots), LookTokens | drop, reorder, launch |
| Folder | open/closed, inner pages | LayoutState | move-in, move-out |
| Widget host | provider bindings, render state | LayoutState, permissions | bind, update, error |
| Edit controller | mode, selection, undo stack | everything | transactions |
| Settings panel | draft layer, Changes list | SettingsState | setting-changed |
| Label/legibility engine | luminance cache | wallpaper, LayoutState | label styles |
| Library and Search | filters, ranking | app list, launch history, SettingsState | launch, drag-start |
| Calm | mode flag, favorites | LayoutState (read-only) | enter, exit |
| Wallpaper pipeline | wallpaper, palette | system | wallpaper-changed |

### 3.3 Event flows (write these as integration tests)

1. **Icon size slider moves:** SettingsState updates → cell metrics recompute → icons, labels, dock icons, folder previews re-measure (120ms cross-fade) → legibility engine re-runs for new label positions → write debounced 200ms → Undo entry on release.
2. **Dock position changes:** SettingsState → dock layout and grid insets recompute → grid re-fits; overflow check → ghost and confirm if items would be displaced → page indicator relocates → widgets re-fit; summary lists any that moved.
3. **Drag icon to dock:** hit-test → dock capacity check → insert and shift (or refuse with "Dock is full") → transaction → Undo entry → recents unaffected.
4. **Add widget:** sheet → provider bind (permission/config) → free-spot search → LayoutState transaction → render Loading → Ready.
5. **Wallpaper changes:** pipeline → palette → accent suggestion chip (not auto-applied) → luminance cache rebuild → label styles update → Calm dim recompute.
6. **Apply preset:** draft layer on the visible home → Apply → LookTokens commit → every surface updates → "Modified" flag cleared → Changes list reset.
7. **App installed:** LauncherApps callback → Library updated (New dot) → optionally placed on the first free home slot (setting) → persisted.
8. **App uninstalled or disabled:** callback → remove from layout, folders, dock, hidden list, recents → folder with 1 app dissolves → one transaction → no Undo (the app is gone).
9. **Rotation or split-screen:** size class changes → pick the profile → if first time, derive positions by reflow from canonical order → render; edits only affect the active profile.
10. **Calm enter/exit:** snapshot UiState → hide dock and home → show Calm; exit restores the snapshot exactly.
11. **Permission granted/denied:** dependent rows and widgets update without leaving the screen.
12. **Default launcher becomes default:** banner disappears; home button handler switches to HavenOS behavior.

### 3.4 Ordering and conflict rules
- During a drag, Settings changes are queued and applied on drop.
- When two transactions conflict (for example, a widget bind finishing while a page is deleted), the later one validates against the current state and is rejected with a short toast rather than corrupting the layout.
- Settings and edit mode share one undo stack.

### 3.5 Error handling
- Widget load failure: contained to the widget frame (§2.4).
- Corrupt layout record: skip the bad item, keep the rest, log it in Diagnostics, show a one-time toast; never crash on start.
- Backup import invalid: change nothing, show the reason.
- Out of storage on write: keep the in-memory state, retry, show "Couldn't save".

### 3.6 Layout safety net (the feature most launchers miss)
- **Auto-snapshots:** save a local snapshot of the layout when edit mode is exited, at most once per hour, keeping the last 10 plus one per day for 7 days.
- **Restore:** edit-mode overflow ⋮ → **Restore layout…** shows a list with date, page and item counts and a **preview of the home** before applying. This avoids any change to the Settings UI.
- **Lock layout** (existing setting) blocks accidental drags.
- Snapshots never include secrets and stay on the device.

---

## 4. Permissions map

| Feature | Permission or access | If denied |
|---|---|---|
| Weather (precise or approximate) | location (optional) | choose a city manually |
| Agenda, Glance event | calendar | Needs-permission state |
| Contact search | contacts | source toggle disabled with reason |
| Notification badges | notification access | no badges; no reserved space |
| Recents from system usage | none required; use launcher's own launch history | feature still works locally |
| Hidden apps lock | biometric/credential | fall back to device credential |
| Lock-screen gesture | accessibility or admin (sensitive) | stays off; opt-in only |
| Hide navigation bar | secure settings (advanced) | only under Advanced → Developer options |
| Default launcher | system role | banner persists |

Ask only at the point of use, with a one-sentence reason first.

---

## 5. Fresh-install defaults

- **Dock:** pinned apps chosen by resolving system intents (Phone, Messages, Browser, Camera, Settings/Gallery) from what is installed; never hard-coded package names.
- **Home page 1:** Weather or Clock widget + the user's most-used apps (after first run) + folders only if the user creates them. No duplicate seeded widgets.
- **Today:** on, with Clock+weather, Agenda (one), Suggested.
- **Labels:** On, Auto legibility. **Icon size:** Auto. **Grid:** Density. **Mode:** Auto.

---

## 6. Acceptance checklist

- [ ] Dock persistent and consistent (Doc 2 §1)
- [ ] Every widget shows Loading, Ready, Empty, Needs-permission, Error and Offline correctly; none is ever blank
- [ ] Add-widget sheet: tap adds to first free spot, drag places exactly, new page offered when full
- [ ] Weather: locality only, unit consistent everywhere, cached with timestamp
- [ ] Agenda: sizes 2×2 / 4×2 / 4×4, empty state, permission state, one seeded instance
- [ ] Third-party widget: bind, configure, resize, failure placeholder, provider-removed placeholder
- [ ] Today flow fills the screen on Expanded; single column on Compact
- [ ] Event flows 1–12 each have an integration test or a documented manual test
- [ ] App install/uninstall/update handled in layout, folders, dock, hidden list and recents
- [ ] Pin requests from other apps handled
- [ ] Snapshots created and **Restore layout…** previews and restores correctly
- [ ] Rotation does not scramble layouts (per-profile)
- [ ] No permission is requested before its feature is used
- [ ] Settings UI visually unchanged
- [ ] Existing user data survives the update (migration test)

---

## 7. Checks and final report

Run the project's available checks (build, lint, type check, unit tests). Add tests for: free-spot search, collision resolution, dock capacity, layout-profile derivation, snapshot create/restore, app uninstall cleanup, widget state machine, weather unit formatting. If an emulator or device is available, install and exercise: add each widget type, rotate, install and uninstall an app, deny and grant permissions, and restore a snapshot.

Final report:
1. **Changed:** short bullets.
2. **Essential-feature table (§1):** each row marked done / partly / missing.
3. **Checks actually run** and results; pre-existing failures separated.
4. **Not verified** and **known gaps**, plus the single most useful next improvement.

Do not call the result production-ready, accessibility-conformant or store-approved unless that was verified.
