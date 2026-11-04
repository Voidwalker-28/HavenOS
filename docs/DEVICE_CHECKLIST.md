# DEVICE CHECKLIST — HavenOS (Redmi Pad 2, HyperOS, Android 16)

## 3.1 FINAL — Consolidated tablet checklist (2026-10-30, 3.1.0/vc13 tree — device-only, ALL NOT RUN)

This is the consolidated pass for the 3.1 final build. It does NOT replace
the series below: run COORD-1…15, B-1…B-15, C-1…C-14, D-1…D-5,
E-1…E-15, F-1…F-13, V2-API-1…9, V3-MASCOT-1…8, and V3-ISLAND-1…9 as
well. The G-items below cover the prompt's G3 areas plus the Phase A
tablet items that never got their own checklist IDs. Mapping: home →
A4/A5/A6/D-4 + dock tiles A7/D-3; Settings → B-1…B-15 + C-1…C-14;
cards → COORD-6 + Phase A items; drawer → C-6; mascot →
V3-MASCOT-1…8 + E-1…E-15; safety → Phase A items; performance →
observation-only (note jank ONLY if observed, never invented).
Every item below is NOT RUN — nothing here was run on the Pad 2.

- G-1: Home, no next-page sliver — at rest, no neighbour page content
  leaks into the viewport (Phase A4 clip fix).
- G-2: Home, no ghost glance — with Today visible, no second glance
  appears beside the rail or under the strip (Phase A5).
- G-3: Home, last Today card clears the dock — with a bottom dock, the
  last card in the rail scrolls fully clear (dock height + 16dp).
- G-4: Home, dock Search/Library tiles — proper 48dp icon tiles
  matching the app icons beside them.
- G-5: Settings, every hub in dark, in light, and under Reduce Effects —
  no broken surfaces, no missing hairlines.
- G-6: Settings, no emoji anywhere; no truncation at font scale 1.0
  and at 1.3.
- G-7: Settings, clean hub transitions — no ghosting or overlap when
  switching hubs (Phase A8 shared-axis fix).
- G-8: Settings, accent system — Wallpaper suggestion chip (never
  auto-applied), System, Curated 12 colors, Custom clamp + 6 recents,
  strength Subtle/Balanced/Vivid; Changes list shows names
  ("Accent: Iris to Ember"), never raw numbers.
- G-9: Cards, Scores with null data — a live or finished match with
  missing scores shows status text only, never the word "null"; an
  unknown status code shows no status text; "Abnormal" shows nothing.
- G-10: Cards, Quote/NASA error classes with Wi-Fi on — Offline shows
  the offline line, RateLimited shows "Busy" with NO Retry,
  other failures show "Couldn't load" with Retry.
- G-11: Cards, Crypto three states — toggle off: no card, no fetch;
  on without a key: compact key-needed state, and "Add key" lands
  on the CoinGecko key field in the APIs hub; on with a key: prices.
- G-12: Cards, offline with cache — stale data stays visible with the
  correct state line; no spinner, no invented data.
- G-13: Drawer, keyboard open and closed — the search field stays
  visible, results resize, and no home text shows through the sheet.
- G-14: Mascot basics — turn on; roams without standing on or
  covering an icon; rotation while walking; page change; edit mode
  hides it; Settings hides it.
- G-15: Mascot Find — same-page, other-page (pager animates and
  settles), folder, widget, Settings-control (row ringed), and
  no-match ("I can't find that." + search offer).
- G-16: Mascot tap vs long-press-drag — tap opens the bubble;
  350ms long-press carries; dropping on a valid ledge places it,
  anywhere else returns it.
- G-17: Mascot Dismiss survives restart.
- G-18: Mascot Reduce Motion — parks on the dock top in a static
  idle pose.
- G-19: Safety, nav-bar restore — the "Restore navigation bar" row
  restores the bar, and launch restores it when a hidden-bar
  setting disagrees with it.
- G-20: Safety, stopped accessibility service banner — "Gestures
  are paused." with a Reconnect button to the system screen.
- G-21: Performance, observation only — Today rail scroll, Settings
  open, slider drag. Note jank ONLY if observed.
- G-22: Upgrade, Room v7→v8 on a shipped vc12 install — layout,
  settings, undo frames, and grid overrides survive the upgrade.
- G-23: Follow-density dialog + undo round-trip — Keep is the
  default; Reset is undoable and the toolbar label names it.
- G-24: Long-press menu destinations — all 7 items land on the right
  destination ("Home settings" opens Home and Dock, not the hub
  screen).
- G-25: Island media callbacks — media controls respond with no
  2-second poll; the charging pill shows and hides correctly.
- G-26: Refresh-on-resume — stale Today cards refresh when home
  resumes.

## M3 (versionCode 3) — install OVER your M2 build

Install the vc3 APK by tapping it in the file manager (check the size and
md5 quoted with the build before installing). It installs over vc2 without
uninstalling.

- **N1 Upgrade keeps your layout + settings** — Install vc3 over vc2 (size/md5
  verified before install): your vc2 home layout and settings should survive.
  Anything moved, missing or reset? Note it.
- **N2 Enable the accessibility service** — Settings → Gestures → Navigation
  gestures status card → Fix, and enable the service. If the toggle is greyed
  out: App info → ⋮ → Allow restricted settings first, because the app was
  sideloaded — then retry.
- **N3 Overlay permission** — Via the same status card's Fix button, grant the
  overlay permission (the edge strips need it).
- **N4 HyperOS survival** — Autostart ON + battery "No restrictions" for
  HavenOS, so the accessibility service survives HyperOS battery management.
- **N5 Gestures on home** — On the home screen: bottom-up = Home,
  bottom-up-hold = Recents, left/right edge swipe-in = Back.
- **N6 Gestures in other apps** — Repeat the N5 gesture tests inside 2–3 other
  apps. Note: the system bar briefly flashing when Recents opens under the
  hide-bar policy is expected Android behavior, not a bug.
- **N7 Master toggle OFF** — Turn the navigation-gestures master toggle OFF:
  overlays should be gone and the system buttons should work exactly as
  before.
- **N8 ADB grant + hide-bar (optional)** — Run the one-time ADB grant (see
  PERMISSIONS.md), then hide-bar on: the bar should be hidden everywhere.
  Hide-bar off: the previous bar behavior should be restored.
- **N9 Drawer conflict check** — A swipe-up starting ABOVE the bottom strip
  should still open the drawer; swipes starting IN the strip should trigger
  the nav gestures instead.
- **N10 Reboot** — Reboot the Pad: the service should rebind, and the status
  card should still tell the truth about what is enabled.

### Phase 2 spot-checks

- **P1 Widgets translucent** — Own widgets should sit on translucent glass
  surfaces over the wallpaper.
- **P2 Drawer search** — Search ranking (prefix > contains > fuzzy), the
  alphabet rail's letter bubble, and the category tabs should all work.
- **P3 Drag reflow** — Drag a wide widget to the right edge: it should settle
  in the nearest free spot, not snap back.
- **P4 Folder resize** — Resize a folder in edit mode: its popup columns
  should grow with the span.
- **P5 Dock add/remove** — Add an app to the dock from the drawer long-press
  menu, and again from edit mode.
- **P6 Widget picker** — The picker should show previews + sizes for own and
  third-party widgets, and search should filter them.

## M2 (versionCode 2) — install OVER your M1 build

Install the vc2 APK by tapping it in the file manager (**2,061,634 bytes**,
md5 `26d80039e0d7852a21252569962dd15f` — check the size first). It installs
over M1 without uninstalling; your M1 home layout should survive (E1 checks
exactly that).

- **E1 Upgrade keeps your layout** — After installing, open HavenOS: your M1
  apps/folders should be in the same positions (the grid now uses half-cell
  units under the hood — positions should look identical). Anything moved,
  missing or doubled? Note it.
- **E2 Glass look, first open** — Cards/dock should look like frosted glass
  over your wallpaper (soft blur, thin bright edge, shadow). Which wallpaper
  mode are you in — system or HavenOS (Settings → Wallpaper)? If the glass
  looks flat/opaque, open Settings → Wallpaper and tell me what "Glass
  quality" says (Full / Partial / Flat + the reason line).
- **E3 Template A home** — A fresh first-run home should show: big serif +
  analog clocks, a weather card, an agenda card, a now-playing card, then
  your apps, with the floating glass dock (6 icons + search). Screenshot it —
  this is the v2.2 reference look. Weather needs a city first (Settings →
  Widgets → Weather city) and internet on the Pad.
- **E4 Templates + Undo** — Settings → Home Screen → apply "Productive":
  page 1 should be a full clock page, page 2 dense with folders. Then tap
  "Undo template" — your previous layout should come back exactly.
- **E5 Density slider** — Settings → Look & Feel → Density, and also the
  Quick panel slider: drag it and watch columns/rows/icon size change live
  behind the settings. Any overlap or cut-off icons at the extremes (0 / 100)?
- **E6 Quick panel** — Swipe down on the home screen: the Quick panel drops
  from the top. Switch Look (e.g. to Retro, then Premium) — the home behind
  it should restyle instantly. Close it with the swipe-up gesture on the
  panel, a tap outside, or Close.
- **E7 Add widgets** — Long-press home → Edit → "Add widget": add a Stack,
  a Countdown (set a date), and one third-party widget (e.g. Spotify's).
  Does the third-party widget bind and show content? Resize the Countdown
  in edit mode (select it → Resize).
- **E8 Free canvas** — Settings → Home Screen → Placement → Free canvas.
  Drag icons anywhere — do snap guides appear? Add a new widget in free
  mode: it must NOT land on top of an existing one.
- **E9 Drawer modes** — Settings → Drawer: try Grid / List / Categories /
  Search-first. In the drawer search, try: an app name, `12*8` (calculator),
  a contact name, a settings word like "density" (opens Settings at the
  right section).
- **E10 Settings search** — Settings → search "wallpaper": results grouped
  by category; tapping one should open that section with the row
  highlighted and scrolled into view.
- **E11 Badges & Now Playing** — Settings → Icons & Badges → enable
  notification access. Play music: the Now Playing card should show the
  track with controls; app icons with notifications should show badges.
- **E12 Wallpaper modes** — Settings → Wallpaper: pick Solid (choose a
  colour), then a Gradient preset, then a Photo — each switch must actually
  change the home wallpaper (an earlier build hid it behind the solid
  colour; this is the regression check). Also try "Apply to system
  wallpaper".
- **E13 Gestures** — Settings → Gestures: set Double-tap → Lock screen,
  Test it, then double-tap the home (first time it may ask to activate
  device admin — accept and retry). Set Swipe down → Quick panel if not
  already.
- **E14 Backup** — Settings → Backup & Restore → Export to a file, then
  Import it back: the summary counts should match your home, and the layout
  must be unchanged after import.

## M1 (versionCode 1) — original checks, still valid where noted

Run these on the real tablet and report pass/fail per item. The build sandbox has no
device, so none of these are verified yet. Install the APK by tapping it in the file
manager (1,588,162 bytes — check the size first). Do NOT set HavenOS as the default
launcher unless an item says so; it opens as a normal app from its icon.

- **D1 Icon** — Find "HavenOS" in your current launcher: white arch/doorway mark on a
  dark rounded square? Does a long-press → themed icon (if you use them) look sane?
- **D2 First open** — Tap the HavenOS icon. It should open straight to a home screen
  (no intro in M1): clock card on top, your apps in a grid below, floating dock at the
  bottom, your current wallpaper behind everything. Screenshot anything that looks off.
- **D3 Home grid** — Are ALL your apps present across pages (swipe left/right)? Is the
  clock's time/date right? Any missing or duplicated apps?
- **D4 Dock** — Dock shows a few favorites (dialer/messages/browser/camera if you have
  them) + search and grid buttons. Tap a favorite: does the app open?
- **D5 Move apps** — Long-press any icon until edit mode starts (hint pill at the
  bottom), drag it to an empty cell, release. Did it land there? Tap "Done" (top right).
  Force-stop/reopen HavenOS — is the new position still there?
- **D6 Folders** — In edit mode, drag one app onto another: a folder should appear.
  Tap it: sheet opens with both apps. Rename it, close, reopen — name saved? Long-press
  a member inside the sheet to remove it; removing down to one app should dissolve the
  folder back into a single icon.
- **D7 Drawer** — Swipe up on the home (or tap the grid button in the dock): full app
  list with letters on the right rail. Tap/drag the rail — does the list jump? Type in
  the search pill — does it filter? Long-press an app that's NOT on home → it should be
  added to home (drawer closes). Tap an app — it opens.
- **D8 Settings** — Dock → Settings. Change Columns/Rows sliders: grid density changes.
  Pick a different Look (e.g. Retro): accent color changes (full Look designs land in
  M2 — M1 Looks are color/corner variants only). Switch Appearance Light/Dark.
- **D9 Live updates** — Install any new app from the Play Store / a file, return to
  HavenOS: does it appear (drawer at least)? Uninstall an app that's on home: its cell
  should empty without a crash. If you use a work profile: do its apps appear?
- **D10 Back key** — On home, press Back: nothing should happen (a launcher never
  exits). With the drawer open, Back should close the drawer. In Settings, Back returns
  to home.
- **D11 Rotation** — Rotate the tablet portrait ↔ landscape: layout adapts, nothing is
  lost, no crash.
- **D12 Smoothness** — Page swipes and drawer open/close: smooth or stuttery? Note
  anything that feels like jank (this decides M2 perf work). Also: after the tablet
  sleeps/wakes and after a reboot, does HavenOS still open fine?

## M4 checks (vc4)
- **M4-1 Widget add (own)**: Edit mode → Add widget → pick a large widget on a full page: you should get a "No room on this page" toast (not a silent nothing). On a page with room, it must appear.
- **M4-2 Widget add (third-party)**: Pick any third-party widget (e.g. Spotify/Instagram): does the system bind dialog appear? If you deny/dismiss it, the sheet must now show an error line (not silently do nothing). Retry from the sheet.
- **M4-3 Drag apps**: Long-press an app on the home → drag it to another cell (grid mode) and in free-canvas mode: does it lift on the first move and drop where you put it? Does it persist after restart? Locked layout must refuse drags with a toast.
- **M4-4 Edit shrink**: Enter edit mode (long-press → edit): the whole home should shrink slightly (~0.87) with the wallpaper breathing around it, then grow back on Done. With Reduce Motion on, it should snap instantly.
- **M4-5 Settings two-pane**: Open Settings on the tablet: left nav list + right content side by side? Search at the top of the left pane, live results? Tap a search result — does it jump + highlight?
- **M4-6 Dock**: Does the dock look like a floating frosted pill with evenly spaced icons and no labels? Search/drawer buttons still there?
- **M4-7 Scroll home**: Settings → Home Screen → Home style → Scroll list: favorites on top, full A–Z list, alphabet rail jump + letter bubble, long-press menu works, badges show. Switch back to Grid — layout intact?
- **M4-8 Swipe-up toggle**: Settings → Home Screen → turn "Swipe up for drawer" OFF: swipe-up on home must do nothing. Turn it back ON: drawer opens again.
- **M4-9 Spacious**: Fresh look — does the Glass/Calm home feel airier (bigger gaps)? Density slider still changes density?

## M5 checks (vc5) — DO FIRST: drag end-to-end
- **M5-1 Drag end-to-end (top bug, third report):** long-press empty space
  below the list → action bar → "Edit home" → banner + jiggling ≡ handles
  appear → drag a favorite's handle up/down → it reorders and stays after
  relaunch; drag a widget's handle → it reorders; drag an A–Z app onto
  Favorites → it pins; drag an A–Z app onto another A–Z row → toast "A–Z
  stays alphabetical…"; kill/relaunch and confirm order persists. If the
  banner/handles never appear, edit-mode ENTRY failed; if they appear but
  nothing drags, the drag PATH failed — report which.
- **M5-2 Upgrade path:** install over a vc4 build with widgets + folders on
  pages → all folders/widgets appear under "Widgets & folders" in page
  order, favorites on top, nothing missing; relaunch twice and confirm no
  duplication or re-sorting.
- **M5-3 Fresh install:** clock card + dock picks only, no template picker,
  A–Z complete. Settings → Style & Theme shows only Accent, Appearance,
  Surface, Corners, Fonts — no preset cards, no Look Editor, no .havenlook.
- **M5-4 Locked layout:** turn on Lock layout → action-bar Edit, menu Edit,
  and QuickPanel Edit all show the locked toast, and no drag starts.
- **M5-5 Edit-mode scrolling:** scroll without grabbing a handle; a vertical
  drag in edit mode never opens the drawer/quick panel.
- **M5-6 Long-press paths:** long-press an app row → menu (with Edit home);
  long-press a widget → edit mode with banner; widget taps still work when
  not editing.
- **M5-7 Widget add/remove:** add a widget → it appears at the flow end;
  remove a selected favorite + a selected widget via the toolbar → both
  disappear, widget host id freed.
- **M5-8 Nav gestures + rail:** with nav gestures ON, confirm rail taps jump
  letters, and note whether rail drags at the extreme right edge trigger
  Back (known limitation).
- **M5-9 Dock swipes:** swipe up / swipe down starting on the dock → drawer /
  quick panel; swipe-up with "Swipe up for drawer" OFF → nothing.
- **M5-10 New in vc5:** Settings → Gestures → per-icon swipe up: set an app's
  swipe to Open drawer, then fast-flick up on that icon (not a scroll) → the
  drawer opens; tap still launches. Settings → Icons → Silence dots: silence
  an app with notifications → its dot/count disappears everywhere (home,
  drawer). Widgets picker → "At a glance" appears and shows date + next
  event + weather. Settings → Backup & Profiles → Duplicate a profile → a
  "<name> (copy)" profile appears.
- **M5-11 §7 acceptance (M5 spec):** every setting reachable from search and
  from its contextual entry point (long-press empty home bar, edit-mode
  Settings, icon menu, drawer ⋮, launcher icon in drawer); instant apply +
  Revert this session works; destructive actions confirm; grid sliders show
  the overflow report while dragging; two-pane on tablet, single column on
  phone, no overflow at narrow width; TalkBack values, focus order,
  reduced-motion verified on device. Backup export/import round-trips and
  an invalid file is rejected without side effects.

## M6 (versionCode 6) — install OVER your vc5 build

Install the vc6 APK by tapping it in the file manager (check the size and
md5 quoted with the build before installing). It installs over vc5 without
uninstalling.

- **P1 Panel over live home** — Open Settings (long-press empty home →
  edit toolbar → Settings, or the drawer ⋮, or a gesture bound to
  Settings): the panel should sit over your live home (tablet landscape:
  home visible on the left, panel on the right; the home behind the panel
  keeps showing your wallpaper and icons). Close with ✕ or Back.
- **P2 Before / Undo / Changes** — Change a setting inside the panel
  (e.g. accent), then hold Before: home should show how it looked when
  you opened Settings. Release: your change returns. Open Changes (chip
  in the panel title bar, or the footer count on the hub screen): the
  changed setting should be listed with a Revert button; Revert should
  restore just that setting.
- **P3 Spotlight** — From a hub row's spotlight (e.g. tap a Look row's
  control, or open a setting from panel search): the matching home
  element (dock, home list, folder) should flash an accent ring.
- **P4 Search inline controls** — On the hub screen, search "bigger
  icons": the Icon size result should appear under Home & Dock; try its
  inline control if shown, and opening the result should land in the
  right hub.
- **P5 Pinch / drag wallpaper crop** — Look hub → Adjust wallpaper:
  pinch to zoom and drag to move; the home behind the panel should
  preview the zoom/pan live. Done commits it, Cancel leaves the old
  wallpaper untouched.
- **P6 Grid picker ghost** — Home & Dock hub → Grid picker: drag to a
  smaller grid; the ghost cells and "N icons will move" count should
  show before you Apply, and Cancel should change nothing.
- **P7 Gesture diagram + Test** — Gestures & Motion hub: the device
  diagram should show your edge zones; pick a trigger, set an action,
  and tap Test — the action should actually happen (e.g. Test on a
  drawer binding opens the drawer).
- **P8 Backup import + Undo** — Device & Data → Import a backup: the
  dialog should count changed settings before applying; after applying,
  Undo last import should restore the previous layout.
- **P9 Long-press drag + folder creation** — In edit mode, drag an app
  by its handle to move it; drop one app onto another to create a
  folder (the M5 drag fix, re-verified after the panel re-hosting).
- **P10 Snap toggle** — Home & Dock hub → Snap to grid off, then on:
  the home stays one continuous scroll in both modes; only the snapping
  behaviour changes, never the layout shape.
- **P11 Rotation / process-death reopen** — With the panel open, rotate
  the tablet; then leave HavenOS (open another app) and come back: the
  panel state should be sane (no blank panel, no lost home). Note
  anything odd — the panel is in-memory state, so a killed process
  reopens on the home screen, which is expected.

## M7 (versionCode 7) — install OVER your vc6 build

Install the vc7 APK by tapping it in the file manager (check the size and
md5 quoted with the build before installing). It installs over vc6 without
uninstalling.

- **P1 Drag end-to-end FIRST** — Enter edit mode (long-press empty home
  → Edit, or the toolbar), drag an app by its ≡ handle to a new spot in
  Favorites and in the A–Z list; drop one app onto another to create a
  folder. This is the third-report drag bug's re-check after the M7
  rework: if drag fails, note whether edit mode itself entered (banner
  + handles visible) or the drag itself failed — the two failures have
  different causes.
- **P2 Library drag-out to home/dock** — Open the Library (dock
  All-apps button), long-press an app icon and drag it out: the Library
  should dismiss and the drag should keep running; drop on the home
  flow (pins to Favorites) and on the dock (adds to dock; "Dock is
  full" / "Already in dock" toasts when they apply). Note: a dock drop
  appends — it cannot land at an exact dock position (platform gives
  no coordinates).
- **P3 Library view persistence** — In the Library, switch All →
  Categories → List; close and reopen: the last view should be the one
  that opens. (Previously the view reset every open.)
- **P4 Search card + Library search** — Dock Search button: results
  should not overlap, and the keyboard should not cover them. Same
  check inside the Library's own field. Try a settings query ("bigger
  icons") and a calculation from the Library field.
- **P5 Labels on a busy wallpaper** — Set a busy wallpaper, then in
  Home & Dock try label modes Auto / Pill / Off: Pill should put a
  readable pill behind labels, Off should hide them. Note: Auto still
  uses a mid-grey fallback (real per-cell wallpaper stats are not in
  yet), so Auto on a busy wallpaper is the known weak case — judge
  Pill/Off, and say if Auto alone is a blocker.
- **P6 Today on phone-width and tablet** — Compact/Medium: Today sits
  above the scroll flow and scrolls away with it (it is pinned above,
  not inside, the list — tell us if it should scroll differently).
  Tablet landscape: Today column beside the flow; turn Today off in
  Home & Dock: the flow should centre itself.
- **P7 Calm entries** — Enter Calm four ways: Glance "Calm mode" row,
  edit-toolbar Calm action, long-press the dock's All-apps button,
  and (if bound) a gesture. Exiting should restore the home exactly
  (same edit mode, selection, dock, flow). "Choose apps" inside Calm
  is a placeholder — do not judge it.
- **P8 Snap + sections overview** — Home & Dock → Snap to grid off,
  then on: home stays one continuous scroll, only snapping changes.
  Edit mode → Sections overview: reorder/hide a section, move an app
  between Favorites and A–Z; note that overview changes reset when
  HavenOS restarts (persistence is session-local for now).
- **P9 Settings session restore** — Change an M7 setting (e.g. label
  mode, snap, Library view, Today on/off), open Changes from the
  panel title bar, and use Revert session / Before: the M7 change
  should revert with the rest, not be left behind.
- **P10 A–Z scrubber + dock consistency** — Drag the alphabet rail:
  letters should be big enough to hit and the bubble should track.
  The dock should look identical (one frosted pill, same icons) on
  home, Library and search — no second dark pill, no "Q" variant.

## M8 (versionCode 8) — install OVER your vc7 build

Install the vc8 APK by tapping it in the file manager (check the size and
md5 quoted with the build before installing). It installs over vc7 without
uninstalling. The home is a horizontal pager again — judge paging, not
the old scroll flow.

- **M8-1 Drag end-to-end on pages FIRST** — Enter edit mode
  (long-press empty home → Edit, or the toolbar), drag an app by its
  ≡ handle to a new spot on the same page, then drag it across the
  page edge to the next page and drop it there. Also drag one app
  onto another to create a folder. If drag fails, note whether edit
  mode itself entered (banner + handles visible) or the drag itself
  failed — the two failures have different causes. This is the
  first check because app-moving has failed on the Pad three times
  before.
- **M8-2 Swipe pages + fling + parallax** — Swipe between pages and
  fling hard: pages should settle cleanly on one page, and the
  wallpaper should shift slightly (parallax) without jumping or
  tearing.
- **M8-3 Indicator dots + long-press → overview** — The page
  indicator dots should track the current page. Long-press the
  indicator: the page overview should open.
- **M8-4 Overview add / reorder / hide / default** — In the page
  overview: add a page, drag pages to reorder them, hide a page, and
  set a page as the default (the page HavenOS opens on). A hidden
  page's apps should not be lost — say where they went.
- **M8-5 Delete an empty page** — Delete an empty page from the
  overview: it should disappear with no confirmation fuss and no
  other page affected.
- **M8-6 Delete a non-empty page** — Delete a page that has icons:
  confirm first, then its icons should land on an adjacent page,
  and a 5s undo should restore them to the deleted page. Check
  nothing is lost.
- **M8-7 Last-page delete blocked** — Try to delete the only
  remaining page: it must be refused (no delete, no loss, no empty
  home).
- **M8-8 Upgrade migration** — On the vc7 → vc8 upgrade, your
  favorites, widgets and folders should land on pages in their old
  order and nothing should be lost. If anything is missing or reset,
  say exactly what.
- **M8-9 Legibility Auto vs Pill vs Off** — Set a busy HavenOS
  wallpaper, then in Home & Dock try label modes Auto / Pill / Off:
  Auto should now read real wallpaper stats (dark text on light
  patches, white on dark patches, pills only on busy or
  low-contrast patches), Pill should always pill, Off should hide
  labels. Note: Auto on a SYSTEM wallpaper still uses the mid-grey
  fallback (no readable system bitmap — see M8 notes), so judge
  Auto on a HavenOS wallpaper, not a system one.
- **M8-10 Edge-drag 600ms page turn** — Drag an app to the screen
  edge and hold: the page should turn after about 600ms, not
  instantly and not never.
- **M8-11 Folder hover-open 400ms** — Hover a dragged app over a
  folder for about 400ms: the folder should open so you can drop
  inside it.
- **M8-12 Dock identical on every page** — Swipe through all pages:
  the dock (same frosted pill, same icons) should look identical on
  every page.
- **M8-13 Today column / page placement** — Tablet landscape
  (Expanded): the Today column should sit on page 1. Phone-width
  (Compact/Medium): Today should be the leftmost page. Say if Today
  is missing or on the wrong page.
- **M8-14 Rotation / split-screen survival** — Rotate the Pad and
  try split-screen: pages, indicator, dock and Today should survive
  the layout change with nothing lost or overlapped.
- **M9-1 Drag end-to-end on pages (do first)** — Enter edit mode, drag an
  app across cells and to another page (600ms edge hold), drop on an icon
  (hold 400ms = folder; release earlier = neighbors shift), drop on a
  folder, drop on the dock (full dock = red + "Dock is full"). Say if any
  of these misbehaves; this is the fourth-generation drag check.
- **M9-2 Toolbar reserved row** — In edit mode the 56dp toolbar must sit
  in its own row with the grid pushed down, never overlapping icons. The
  old 11-action gold row and the permanent helper line must be gone.
- **M9-3 Context bar + selection** — Tap an item in edit mode: 2dp accent
  ring + corner handles ONLY on that item, context bar visible (Resize ·
  Remove · Move to page · Duplicate · Item settings · Select more). No
  outlines on other items, no handle dots everywhere. Tap in edit mode
  must SELECT, never launch an app.
- **M9-4 Dock visible everywhere** — Dock must be visible on every home
  page, the Today page, in edit mode, and with Settings open (it moves
  with the home, never under the panel). Try Bottom, Left, Right. Known
  gap: with Settings open on tablet the dock may sit partly under the
  panel — report it if you see it.
- **M9-5 Today page** — Today is now its own page (sun glyph in the
  indicator). Tablet landscape: 2-column card flow; phone: single column.
  Weather line must read locality only ("Delhi · 24°C · Clear"), never
  truncated mid-word. Turning Today off in Settings must remove the page
  without deleting its cards.
- **M9-6 Widgets real** — Weather, Agenda, Clock, Suggested, Battery must
  show real content, never blank dark rectangles. Only ONE Agenda on a
  fresh layout. Weather units must match Settings everywhere (widget,
  Glance, Today). Deny calendar permission: Agenda must show the
  Needs-permission chip, not a blank.
- **M9-7 Add-widget sheet** — From + Add → Widget: HavenOS widgets first,
  live previews, size chips; tap adds to the first free spot; drag places
  exactly. Add a third-party widget and cancel its configure screen:
  nothing must be added.
- **M9-8 Widget stacks** — Drop a widget onto a same-size widget: stack
  forms; vertical swipe cycles; dots on the right; long-press → Edit
  stack (reorder, remove, unstack).
- **M9-9 Snapshots + Restore** — Leave edit mode (snapshot saved, at most
  hourly), change the layout, then overflow ⋮ → Restore layout…: a list
  with dates + counts must appear; restoring one must bring that layout
  back (visual preview before applying is NOT built — counts only).
- **M9-10 Rotation profiles** — Arrange landscape, rotate to portrait,
  arrange differently, rotate back: neither orientation's layout may be
  scrambled. "Sync layouts" in overflow re-derives other profiles from
  the current one with a preview.
- **M9-11 App lifecycle** — Install an app (Library + New dot, optionally
  first free home slot); uninstall one (must vanish from home, folders,
  dock, hidden, recents; a 1-app folder must dissolve).
- **M9-12 Calm editor** — Toolbar Calm must open the real editor: pick
  up to 8 favorites, reorder, set a schedule. Calm itself must never
  change the layout; exiting Calm must restore the home exactly.
- **M9-13 Lock layout** — With lock on, drag/resize/remove must all be
  blocked with the one-line "Layout is locked" toast.
- **M9-14 Labels + grid fit** — On a busy wallpaper, Auto labels must be
  legible (white text + pill on dark areas); Pill and Off modes must work;
  Labels Off must re-centre icons. No clipped bottom row, no indicator
  overlapping a widget, on any size class.

## M10 checks (vc10) — install OVER your vc9 build

Install the vc10 APK by tapping it in the file manager (check the size
and md5 quoted with the build before installing). It installs over vc9
without uninstalling. DO FIRST: drag end-to-end (enter edit mode, drag
an app to another page, resize a widget) — the structural fixes stand
or fall on this.

- **M10-1 No overlaps, ever** — Add widgets until a page is nearly
  full, drag apps around, resize widgets. No two icons/widgets may
  share a cell or cover each other. If anything overlaps, note which
  two items and what you did just before.
- **M10-2 Dock stable every page + orientation** — Swipe through all
  pages, rotate the tablet, open Settings and close it. The dock must
  stay in the same place with the same icons, never covered by widgets
  or page content, never sitting under the Settings panel.
- **M10-3 Grid settings are real** — Settings hub screen → Quick tune
  → Grid → tap 4 × 6, then 5 × 6, then Density. Each tap must visibly
  change the grid. Then Home & Dock → Grid → shrink the grid: the
  preview must say how many items move and to which pages, and Apply
  must actually move them (nothing stranded off-grid). Undo must
  restore the previous layout whole.
- **M10-4 Resize + Replace + undo** — Edit mode → select a widget →
  Resize: the dialog must say whether the size fits, moves, or has no
  room BEFORE you Apply. Replace: pick another widget — it should
  swap in place (or move to the nearest free spot) and never delete
  your old widget silently. Undo after each step must restore the
  whole prior layout, not just one property.
- **M10-5 Settings master-detail (tablet landscape)** — Open Settings:
  the left rail must list All settings + the six hubs, the right side
  must show the hub's controls, and the home on the left must stay
  fully visible and live (not dimmed, not covered). Tapping a hub in
  the rail must switch the detail.
- **M10-6 Dock icons readable** — With a LIGHT wallpaper, every dock
  icon must be clearly visible (especially white/light icons). With a
  dark wallpaper, same check. Any icon that disappears into the dock
  is a bug — note which app.
- **M10-7 Labels on your wallpaper + a light wallpaper** — Icon labels
  must be readable on your current (busy) wallpaper AND after switching
  to a plain light wallpaper. The HNK letters in the recording are
  wallpaper pixels, not drawn by HavenOS — labels must stay readable
  over them via their pill/scrim, and the wallpaper itself must be
  untouched.
- **M10-8 Motion** — Page swipes must feel like ONE consistent motion
  (no stutter, no mixed speeds). With Reduce motion on, page changes
  must be instant/fades only, no sliding. With motion Off, same.
- **M10-9 Selection is obvious** — Edit mode → tap an icon: it must get
  the accent ring AND the context bar must show its NAME first (not
  just buttons). Multi-select must show "N items".
- **M10-10 Quick customize bar** — Long-press empty home space: a bar
  must appear with Wallpaper, Layout, Widgets, Dock, Style, Home
  settings, Edit home. Each must open the right place; tapping outside
  or Back must dismiss it. (Long-press no longer jumps straight into
  edit mode — Edit home in the bar is the way in.)
- **M10-11 Library scrubber** — Library All view: drag the A–Z strip on
  the right edge — the current-letter bubble must follow your finger,
  and the strip must be easy to grab (wider than before).
- **M10-12 Today rail (tablet landscape)** — With Today on, the home
  must show a Today rail on the left (greeting + date, clock/weather,
  agenda, battery, suggested, Search/Add widget/Calm) beside the app
  grid — NOT a Today page you swipe to. The grid must be centred in
  its 70% area, not stretched across the whole screen.
- **M10-13 Today strip (tablet portrait)** — Rotate to portrait with
  Today on: a "Today" header row must sit above the grid (date,
  weather, next event). Tap it to expand the Today cards above the
  grid; tap again to collapse. On a phone, the old Today page must
  still exist (swipe to it).
- **M10-14 Calm mode** — Enter Calm, exit Calm: the home must come back
  EXACTLY as it was (same icons, same pages, same order). Calm must
  never move or hide anything permanently.
- **M10-15 Rotation preserves layout** — Arrange a distinctive layout,
  rotate portrait ↔ landscape several times: every icon must keep its
  identity and a valid spot; nothing may collide or vanish. (If the
  smaller-orientation grid is tighter, icons may shift to free spots —
  that is intended — but none may overlap or disappear.)
- **M10-16 Large text** — Turn the system font size up large: labels,
  settings rows, and cards must reflow, never clip text silently.
  (Labels stay one line with … — that is visible truncation, not
  silent clipping.)

## v10 dock correction (2026-10-25, same versionCode 10 — fix swapped into vc10)
Device-only checks for the dock fix; none of these can be verified without the Pad:
- **V10-DOCK-1 Dock visible on every screen** — Dock visible on all grid pages, the Today page, the Today rail (tablet landscape), normal mode, edit mode, and while the Settings panel is open. Hidden ONLY in Calm (by design).
- **V10-DOCK-2 Grid bottom row clear of the screen edge** — On every page, the grid's bottom row must sit above the dock with visible space (dock thickness + indicator + 16dp margin), never touching the screen edge and never under the dock. Same check for side dock positions (Left/Right): grid must not run under the side pill.
- **V10-DOCK-3 Drag-in/out end-to-end** — Grid → dock move and Library → dock append still work, including the "Dock is full" refusal. NEW: in EDIT MODE, long-press a dock icon and drag it onto the home grid — it must land on the first free cell of the current page, show "Moved to home", and leave the dock (undo recorded for the grid add). In NORMAL mode, long-press on a dock icon must still open the icon menu (drag-out is edit-mode-only by design, so it cannot break the menu); the menu's "Move to home" row is the normal-mode move path.
- **V10-DOCK-4 Contrast on light + dark** — A white-foreground adaptive icon (e.g. ChatGPT) must read on a LIGHT dock, and a black-foreground adaptive icon must read on a DARK dock (both now get the derived tonal backdrop tile when their centre luminance is too close to the dock surface). Non-adaptive icons unchanged.
- **V10-DOCK-5 Dock contents/sizing unchanged** — Pinned apps | divider | up to 3 recents | Search | Library; icons never below 48dp; short space drops recents first, pinned beyond the limit scrolls inside the pill.

## v2 M1 — API integration (vc11, versionName 2.0) — Pad checks owed
- V2-API-1: Today cards show real data on the Pad (quote, cricket + football scores, APOD image, GitHub repos/activity) — Quotable could not be verified from the sandbox at all (curl empty replies); confirm a quote actually loads on device.
- V2-API-2: "Powered by SportScore" attribution visible on the scores card in all states; tapping it opens sportscore.com in a browser.
- V2-API-3: Crypto card shows the honest key-needed state with no Demo key, and real BTC/ETH prices + 24h change after entering a free CoinGecko Demo key in Settings → APIs.
- V2-API-4: APOD "Set as HavenOS wallpaper" applies the picture as HavenOS-mode wallpaper only (system wallpaper untouched); APOD video days show the thumbnail, never a broken embed.
- V2-API-5: Dictionary row returns a definition for a never-before-looked-up word (the pre-fix build could not fetch new words at all).
- V2-API-6: Currency row converts "100 USD to INR" with a dated rate label; dictionary row stays silent on 404 words; neither row appears when its toggle is off.
- V2-API-7: Toggle-off removes each Today card; WorkManager refresh cadences fire (scores ~15min, crypto ~30min, GitHub ~1h, quote/APOD/currency ~24h); rate-limit behavior (429 → stale cache + offline label, no retry storm).
- V2-API-8: Room 4→5 migration runs cleanly on an existing vc10 install (no data loss, no crash on upgrade); new Settings APIs hub renders on device (toggles, masked key fields, GitHub username field, hub reset preserving keys/username).
- V2-API-9: Cache self-healing after a genuinely poisoned API response on device (card recovers instead of staying blank).

## v3 Track A — Mascot (vc12, versionName 3.0) — Pad checks owed
No device/emulator exists in the sandbox, so every item below is
device-only. The mascot defaults OFF: all checks start in
Settings → Mascot → Mascot ON.
- **V3-MASCOT-1 Vertical slice** — Mascot ON → mascot appears and roams
  between edge perches (never on an icon) → tap it → Find → type
  "Camera" → mascot walks adjacent to the Camera icon and a ring
  spotlights the icon (icon stays tappable) → expression changes to
  found → Dismiss → kill and relaunch the launcher → mascot stays gone
  until Settings → Mascot → Bring back mascot.
- **V3-MASCOT-2 Zero blocked taps** — With the mascot on and roaming
  and the bubble closed, tap every icon, dock item, and widget position:
  every tap must reach its target; the mascot must never eat a tap.
  Long-press + drag on the mascot must move it (and persist the new
  home spot across relaunch); a quick tap must still open the menu
  (tap-vs-drag disambiguation is the known risk here).
- **V3-MASCOT-3 Find placements** — Find an app on another home page
  (page flips, then walk + ring); an app inside a folder (bubble names
  the folder + Launch works); a drawer-only app (Open drawer + Launch
  work); a Settings control ("accent" → Look screen); a multi-match
  query (≤4 chips, picking one guides correctly); a no-match query
  (confused + "try another word", no web offer).
- **V3-MASCOT-4 Yield + sleep** — Opening the drawer, a folder, edit
  mode, the widget sheet, or Settings hides the mascot; it returns only
  on calm home. During quiet hours (default 22:00–07:00) it parks and
  shows sleepy; with battery saver on it also parks.
- **V3-MASCOT-5 Tutorials + suggestions** — First bubble-menu open
  offers one tour (Yes / Not now / Don't show again all consume the
  one offer); completing a tour celebrates; Suggestions offers at most
  one nudge, "Don't suggest this" is respected, and with Tips Off no
  suggestion ever appears.
- **V3-MASCOT-6 Settings hub** — Settings → Mascot renders with the
  existing shell rows (no restyled screens): on/off, Milo/Mila picker
  with live idle + happy preview, roaming, size, Find, Tips Off/Rare,
  quiet-hours sliders, mascot reduce-motion, Chat model "Not set up"
  (no dead download button), Reset tips, and Bring back mascot visible
  ONLY after a dismiss.
- **V3-MASCOT-7 Reduced motion + screen reader** — With system animator
  scale 0 (or mascot reduce-motion on), roaming teleports instead of
  gliding and no bounces/particles appear. With TalkBack on, the
  mascot parks and the bubble buttons are reachable and announced.
- **V3-MASCOT-8 Battery sanity** — Leave the mascot on overnight on
  home: no visible battery anomaly vs mascot off (no measurement was
  possible in the sandbox — this is a Pad observation, not a claim).
## v3 Track B — Dynamic Island (vc12, versionName 3.0, track build — device-only)
- V3-ISLAND-1: Toggle on (Settings → Home & Dock → Dynamic Island): with no media/charging/notifications, home shows NOTHING new (hidden-when-idle).
- V3-ISLAND-2: Plug in charger: pill appears top-center with battery %; unplug: pill disappears.
- V3-ISLAND-3: Play music (notification access granted): pill shows track; tap expands; play/pause/prev/next actually work; pausing keeps the pill (session still active), swiping the player away clears it.
- V3-ISLAND-4: New notification arrives: pill pops with count, expands to per-app rows, tapping a row launches that app; the alert clears on its own after a few seconds (transient window) even if the notification is still unread.
- V3-ISLAND-5: Media + charging + notification at once: pill shows top-priority (notification, then media), expanded panel lists all three.
- V3-ISLAND-6: Toggle off: pill gone everywhere, including with media playing; home layout/dock untouched.
- V3-ISLAND-7: Edit mode: island never covers the edit toolbar. Drawer/search/Settings/quick panel open: island hidden behind them.
- V3-ISLAND-8: Rotate tablet: pill stays top-center, expanded panel stays readable, no overlap with grid content.
- V3-ISLAND-9: Without notification access granted: charging pill still works; media/notification activities simply absent (no placeholder, no prompt from the island itself).

## Coordination fix (2026-10-29, 3.0/vc12 tree — device-only)
- COORD-1: Rotate tablet portrait ↔ landscape after a grid change: the
  non-current profile repairs on switch (valid items unmoved, only
  out-of-bounds/overlapping items move); no crash, no lost icons.
- COORD-2: Shrink the grid with moves: one transaction, one undo step
  restores everything (watch for a possible extra undo entry from the
  host-effect/commit race — report the depth, do not treat one extra
  entry as corruption).
- COORD-3: Kill the app process, relaunch: undo depth survives (undo
  still steps back through pre-kill changes); redo survives likewise.
- COORD-4: Drag an app to the dock, then Undo: grid item returns AND
  dock entry disappears (the old M10 gap — undo left it in neither
  place — is closed).
- COORD-5: Edit-mode overflow → Restore layout…: each row shows a
  first-page thumbnail above the counts line; an empty snapshot shows
  counts only (no blank tile); tapping a row restores; tapping a
  thumbnail tile also restores (same action).
- COORD-6: With network off for >2× a card's max age (quote 24h,
  scores 15min, crypto 30min, APOD 24h, GitHub 1h): card shows
  "Couldn't refresh — showing {last updated} · Retry"; tapping Retry
  refreshes (or shows Loading while in flight); toggling the API off
  removes the Retry button (and the card).
- COORD-7: Grid picker with >8 destination pages: first 8 shown plus
  a "+M more" row expanding inline to the full list; count line never
  collapses ("N items will move" always full).
- COORD-8: Settings home: Changes banner appears under the
  needs-attention banner when changes exist (Review opens the list,
  Revert session works); footer count row is gone; title-bar chip
  still shows the count.
- COORD-9: Open Settings from edit mode: edit mode exits first
  (toolbar gone, no stacked toolbar-plus-panel frame); close Settings
  and the home is as left. Phone: panel scrim is flat 40%.
- COORD-10: Every hub (Look, Home & Dock, Drawer & Search, Gestures &
  Motion, Privacy, Device & Data, APIs, Mascot): NO second back/title
  row — the panel title bar's Back works and its title matches.
- COORD-11: Look hub: Material is one segmented row
  (Solid/Translucent/Frosted); Wallpaper is one collapsed row (expand
  shows Source/Style/photo/Adjust/icon packs/dim/tint — nothing lost).
- COORD-12: Accent: picking a swatch/hex shows "Previewing" in the
  Accent row value and a dashed hero circle; closing the panel
  commits it (buttons/toggles recolour); Cancel/discard drops it.
- COORD-13: Install an app, uninstall it, disable then re-enable one:
  home/Library/profiles agree everywhere; a removed app renders
  nowhere (including after rotate); Sync layouts produces one undo
  entry and undo restores pre-sync placements.
- COORD-14: Upgrade from the previous install (Room 5→6): layout,
  profiles, snapshots, API cache all survive; undo depth starts at 0
  (new table), dirty flags start empty.
- COORD-15: Density slider (Drawer/QuickPanel) still converges the
  grid via the host safety net (density routing itself is blocked —
  see COVERAGE — so this check is about convergence, not undo).

## 3.1 Phase B — Haven Glass + accent system (2026-10-30, 3.0/vc12 tree — device-only)

- B-1: Glass legibility: on a bright wallpaper and on a busy photo
  wallpaper, card text, drawer rows, and panel text stay readable
  (dark and light mode).
- B-2: Reduce Effects on: glass flattens to translucent tint, same
  layout, no blur, text backgrounds never fully transparent.
- B-3: Curated grid: all 12 names render; picking one recolours
  buttons/toggles/highlights; Strength Subtle/Balanced/Vivid visibly
  changes container tint; a Custom colour that needs clamping shows
  "Adjusted for readability" and renders the clamped colour.
- B-4: Wallpaper source: the "Match wallpaper" suggestion never
  applies on its own; tapping it applies the wallpaper colours.
  Changing the system wallpaper while Wallpaper is already selected
  picks up on next launch (documented limitation).
- B-5: System source shows on the Pad (Android 16) and follows the
  device Material You colour; on older Android it is hidden (not
  testable on the Pad).
- B-6: Migration on the existing install: the previously picked
  swatch maps to its nearest curated colour (see COVERAGE for the
  table); a stored custom colour is kept; a mode-3 install lands on
  Wallpaper. The visible accent does not jump.
- B-7: Changes list shows names ("Accent: Iris to Ember"), never
  raw ints; panel-close commits the draft, discard drops it.
- B-8: Emoji-free UI spot check: hub rows, permission rows, island
  collapsed/expanded, weather widgets, edit toolbar, drawer clear,
  icon-pack tick, wallpaper buttons — no emoji or text glyphs.
- B-9: Motion: hub transitions 220ms shared axis; switch/segmented
  thumb springs; haptic tick on switch toggle, slider step, grid snap;
  Motion Off = instant; Reduce Motion = 100ms fades.
- B-10: Help popover: 48dp target opens a Glass3 popover, dismisses
  on outside/back/click. Long-press on an ellipsized label shows the
  full text.
- B-11: Custom picker: hue strip + saturation/lightness square drag
  live-previews on the Look hero; recents hold up to 6 and re-apply
  on tap; eyedropper chips sample the system wallpaper colours; hex
  entry lives under Advanced and pushes a recent.
- B-12: Purple gone: the Default-launcher card, drawer selected
  colour, gestures background, and overview background are
  accent-toned, never baseline purple.
- B-13: Font scale 1.3: Settings rows do not clip or overlap.
- B-14: Island icons: collapsed media shows play/pause state icon,
  charging shows the bolt, notifications show the bell; expanded
  media shows previous / pause-or-play / next icons that work.
- B-15: Two-blur-layer behaviour: with a panel over home, at most two
  blurred layers render; a third surface falls back to tint (visual
  check only — the rule is documented, not enforced in code).

## 3.1 Phase C — Haven Glass on non-home surfaces (2026-10-30, 3.0/vc12 tree — device-only)

- C-1: Settings panel over a bright wallpaper and over a busy photo
  wallpaper: panel text readable, no home text showing through; rail
  260dp on tablet, selected row an accent-soft pill, labels wrap to
  two lines (never truncate) at font scale 1.0 and 1.3.
- C-2: Hub screen hero: the live miniature matches the current look
  (accent, mode, material, icon shape, grid); Quick tune, recent
  chips, and the "All settings" grouped hub list all render with
  icon tiles, live summaries, chevrons.
- C-3: Look hub: swatch names under swatches, selected ring visible,
  strength/mode/material/label mode/icon shape/wallpaper controls
  all present and live.
- C-4: Home and Dock: Columns/Rows steppers work; Follow-density
  button label never wraps letter-by-letter; grid-shrink preview
  shows the one-sentence move summary + expandable destination list.
- C-5: APIs hub: each row shows a status line (Updated HH:MM /
  Needs key / Off / Offline / Busy / Couldn't load); key fields
  Paste/Show/Hide + validation work (CoinGecko, NASA, GitHub
  username rule).
- C-6: Drawer: Glass2 sheet hides home text; IME open keeps the
  search field visible and results resized; toolbar row (segmented
  + sort + menu) works; A-Z rail 48dp hit area + Glass3 letter
  bubble; match highlighting in results; empty state shows the sad
  mascot pose.
- C-7: Today cards: one header/body/footer anatomy; Skeleton while
  loading; footer "Updated HH:MM · Source"; Scores at most 4 + Show
  more; Quote 18sp; APOD Set-as-HavenOS-wallpaper; Crypto arrow +
  sign (never colour alone); empty/error states show the static
  sad mascot pose.
- C-8: Menus/dialogs/snackbars: every dialog is a Haven dialog
  (title/body/text button/at most one filled button); menu rows
  48dp icon + label; destructive entries danger-tinted; snackbars
  are HavenSnackbar with Undo; long-press quick-customize bar is a
  Glass3 pill row — and tapping Widgets in it OPENS the widget
  sheet (this was broken before Phase C).
- C-9: Edit toolbar: 56dp Glass3 pill, icon + label buttons, Snap
  accent-soft when on, hairline dividers between Left/Center/Right
  groups, overflow menu unchanged; context bar same style;
  selection ring still 2dp accent.
- C-10: Page overview: Glass2 sheet, Glass1 20dp thumbnails,
  drag/tap-move/reorder/hide/default/delete all still work.
  Widget picker: Glass2 sheet + glass search field, previews
  unchanged, size chips work. Folders unchanged in behavior.
  Page indicator: 8dp dots + 24dp accent dash.
- C-11: Calm: overlay large type, low chrome contrast, exit
  restores home exactly; editor switches/sliders are glass; accent
  appears ONLY for schedule state (Schedule label when enabled,
  selected day pills).
- C-12: Island: Glass3 hairline dividers between stacked expanded
  activities; behavior/priority/visibility unchanged (off by
  default; hidden in edit mode, drawer, search, Settings, quick
  panel, Calm).
- C-13: TalkBack: each Settings row reads as label, value, role;
  toolbar buttons read their full label when collapsed; hub
  transitions show no ghost frames.
- C-14: No emoji anywhere in the UI; no truncated label at font
  scale 1.0 and 1.3 on tablet; all contrast rules pass.
- Phase C tests written but NOT run (sandbox executor broken,
  pre-existing; SettingsShellTest has 10 pre-existing compile
  errors):
  - ui/settings/hubs/C1HubHelpersTest.kt (API status mapping,
    GitHub username validation, move summary sentence, destination
    list capping).
  - ui/SearchHighlightTest.kt (matchSpans contract).
  The script equivalents of the emoji + hex scans were run against
  the tree and pass (0 hits each); the gradle tests themselves did
  not run.

## 3.1 Phase D — Small home updates (2026-10-30, 3.0/vc12 tree — device-only)

- D-1: Home search widget: add the Quick search widget to a home
  page. It renders as ONE Glass1 card matching the Today cards
  (same blurred surface, 28dp corners, 16dp padding) — no inner
  pill, no flat band inside the card. Tapping anywhere on the tile
  (including the padding ring) opens the Search card; in edit mode
  the tile does not open search (drag handles it).
- D-2: Search widget icon: a Search vector icon (not a ⌕ glyph)
  beside "Search apps, settings…" in muted colour.
- D-3: Dock Search/Library re-check: both tiles still 48dp, same
  tile style as the app icons beside them, Search and Library
  descriptions read correctly; tapping opens Search / Library.
- D-4: A4/A5/A6 re-check on the Pad: no next-page sliver at rest
  (portrait, landscape, rail on/off, Settings open); no glance row
  while the Today rail or Today page is visible; the last Today
  rail card scrolls fully clear of a bottom dock (bottom edge and
  corner radius visible).
- D-5: No other home change: grid, icon sizes, spacing, Today
  structure, and the glass look are exactly as before Phase D;
  the mascot layer is untouched (ledge-graph behavior is Phase E).
- Phase D tests: none added (no new pure logic; see COVERAGE.md
  Phase D). Nothing in Phase D is device-verified yet.

## 3.1 Phase E — Pixel mascot rebuild (2026-10-30, 3.0/vc12 tree — device-only)

- E-1: Pixel art on the Pad: enable Mascot in Settings, then
  Mascot. Milo and Mila both render as crisp pixel sprites (no
  blur, no smoothing) at Small/Medium/Large; the tuft tip
  (Milo) / ribbon (Mila) follows the current accent colour —
  change the accent in Look and the mascot's accent pixels
  change with it. Idle blinks, walk bobs, jump shows rise /
  apex / fall / land, sleep shows a rising Z, wave / happy /
  surprised / sad / think / point / poof are all distinct.
  Compare against docs/mascot/preview.png — the Pad render
  must match the preview frames.
- E-2: Roaming + ledges: with Roaming on, the mascot walks and
  jumps between ledges (dock top, empty cells, widget tops,
  rail top) and NEVER stands on or covers an icon, label, or
  widget hit area. It dwells 3-12s per perch, sits after
  60-120s idle, sleeps after 5 min. It never walks across pages
  while a page is swiping.
- E-3: Ledge loss + respawn: remove the widget / app the mascot
  stands on (or shrink the grid so its ledge vanishes) — it
  falls to the nearest valid ledge below; with no valid ledge
  anywhere it poofs and respawns on the dock top.
- E-4: Carry + drop: long-press (350ms) picks the sprite up
  (haptic tick, dangling apex pose); drop on a ledge places it
  there and the spot sticks across restarts; drop anywhere
  invalid returns it to the previous ledge. A short tap still
  opens the menu; taps and long-presses on anything else still
  do their home action (nothing is swallowed).
- E-5: Hide + poof: open edit mode, the drawer, search, quick
  panel, or Calm — the mascot poofs out (does not just vanish).
  Reopen home — it respawns (dock top or persisted spot).
  Reduce Motion: no roaming, sits on the dock top in static
  idle pose, still reacts to taps. Battery saver: idle and sit
  only.
- E-6: Find app on the visible page: menu → Find → type an app
  name → the mascot walks/jumps to the nearest ledge by the
  icon, faces it, points (arrow), ring + 40% dim around the
  icon. Any tap on the dim dismisses the ring (and the first
  tap does NOT launch the icon). Launch chip still launches.
- E-7: Find app on another page: the pager animates to that
  page, settles, then the mascot walks and points. App inside a
  folder or drawer-only: bubble with Launch chip, NO ring
  inside a closed folder, folder does NOT open unasked.
- E-8: Find widget: type a widget name (e.g. "clock") — the
  mascot walks to its frame and points with the ring.
- E-9: Find Settings control: type "accent" — Settings opens
  at Look with the Accent row ringed (2dp accent border) while
  the panel is open; Done ends Find WITHOUT closing Settings
  (user closes Settings). Same for dock, widgets, island,
  mascot, find, quiet, reset rows (each rings its row).
- E-10: Find no-match: type a nonsense word — bubble says
  "I can't find that." with [Search in drawer, Try again];
  Search in drawer opens the drawer; Try again reopens Find.
  No guessing, no invented target.
- E-11: Find field + keyboard: the Asking field is a styled
  field with label "Search apps, settings…" and a Search
  keyboard action (action key submits, Find button submits);
  the bubble stays above the keyboard (never covered by it).
  Back closes the bubble; tapping outside the bubble cancels
  Find (and the outside tap still does its home action).
  20s without progress in the waiting states ends Find.
- E-12: Help + Suggest + Teach: menu → Help → pick a question
  — the answer opens the right Settings screen (Open chip) and
  the row rings. Suggest: at most one per session, 24h
  cooldown, never during quiet hours 22:00-07:00; Normal tips
  adds a help-tip fallback when no pin/widget candidate
  exists; Rare behaves exactly as before. Tutorials are
  one-time; don't-show-again is respected. Dismiss sticks
  across restarts until re-enabled in Settings.
- E-13: Mascot hub: preview box shows the sprite animating;
  Preview plays idle → walk → jump → wave once, then rests on
  idle. Tips offers Off/Rare/Normal. Quiet hours rows open
  24-hour time pickers (minutes settable, not just hours).
  Reset row is now "Reset mascot". Character/roaming/size/
  Find rows unchanged.
- E-14: Empty states: drawer "No matches" shows the surprised
  pixel pose; API card offline/error shows the sad pixel pose;
  Agenda "No events today" shows the sleeping pixel pose.
  None of these animate or roam (static images only).
- E-15: Old vector fallback: none of the above may break if
  the pixel set fails to load (vector figure path preserved
  behind USE_PIXEL_MASCOT; not user-testable without a build
  flag flip — code-review item, noted here).
- Phase E tests: written, NOT run (see COVERAGE.md Phase E).
  Nothing in Phase E is device-verified yet.

## 3.1 Phase F — On-device assist foundation (2026-10-30, 3.0/vc12 tree — device-only)

- F-1: Drawer search "dark mode" — an "Assist commands" row reads
  "Dark mode: System → Dark" with Apply. Tapping Apply opens the
  panel at Look; Changes shows "Dark mode" with a working Revert;
  the Before pill shows the pre-change home. Revert restores it.
- F-2: Drawer "dark mode and bigger icons" — ONE row carries both
  labels. Apply changes both; Changes shows both; Revert session
  restores both. No partial application.
- F-3: Drawer "open clock" — row "Open Clock"; Apply launches
  Clock. If two cameras exist, the more-launched one wins and its
  exact label shows; a true tie shows no command row at all.
- F-4: Drawer "go to Privacy" — panel opens at Privacy with the
  ledger visible. "go to Narnia" shows no command row; normal
  search results render unchanged.
- F-5: Drawer "accent iris" — panel opens at Look; Changes shows
  "Accent" with Revert; strength and recents are untouched.
  "accent chartreuse" shows no command row.
- F-6: Drawer "silent" — NO command row and NO invented silent
  mode; normal search results render unchanged.
- F-7: Panel search (Settings home) "bigger icons" — command row
  sits above the settings hits; Apply grows the Changes footer
  count and Revert works. "toggle calm" opens Calm; exiting Calm
  returns to the panel.
- F-8: Ledger switches: Settings → Privacy → AI Privacy Ledger
  lists all five features with On-device text, last used time,
  and a switch. Search commands off → drawer "dark mode" shows
  no command row; on → the row returns.
- F-9: Ledger prediction off → the Suggested widget shows its
  empty state ("Launch some apps and suggestions will appear
  here"); on → suggestions return from launch history.
- F-10: Ledger chat off → mascot "hi" falls through to Find (no
  greeting), Help menu says chat is off, and Find still works.
  Chat on → greeting and help answers return.
- F-11: Ledger Clear learned data → launch counts reset, the
  Suggested widget empties, ledger counts/times reset; mascot
  muted/don't-show lists are untouched (dismissed tours stay
  dismissed).
- F-12: Mascot hub Chat model row reads "Not set up" (the real
  state: no model bundled, URL/checksum placeholders empty).
  No download button that does nothing.
- F-13: Compound refusal: "dark mode and frobnicate" shows NO
  command row at all — the bad clause voids the whole query,
  so nothing half-applies.
- Phase F tests: written, NOT run (see COVERAGE.md Phase F).
  Nothing in Phase F is device-verified yet.

## 3.1.1 hotfix (2026-10-31, 3.1.1/vc14 tree — device-only)

- H-1: Crash-loop entry: after two crashes within 60 seconds, the
  next start shows the banner "HavenOS recovered from a crash.
  Mascot turned off" with Re-enable and View details; mascot,
  island, online workers, and blur are off while the banner shows.
- H-2: Re-enable restores exactly the features that were on before
  Safe Mode (mascot/island/workers/blur as the user had them), and
  the banner clears. Safe Mode also clears on its own after 10
  minutes of stable running.
- H-3: View details shows the last crash (class, message, time,
  versionCode); Copy copies it and Share opens a chooser. Nothing
  is uploaded automatically. Settings → Device & Data → Advanced →
  Diagnostics shows the same last crash plus the feature-error list.
- H-4: Feature-disable toasts: trigger a mascot error (e.g. Find
  with no targets) and confirm the toast "Mascot paused after an
  error" appears exactly once, the mascot poofs out, and the rest of
  the home keeps working. Island and online-cards toasts read
  "Island paused after an error" / "Online cards paused after an
  error" and disable only that feature for the session.
- H-5: Touch pass-through while the mascot walks, jumps, sits,
  spawns, and poofs: pager swipes, icon taps, the long-press menu,
  drawer open, dock taps, Settings open, and edge gestures all work.
  With the bubble open, a swipe dismisses the bubble AND still
  scrolls the pager; a tap on the dim dismisses the bubble without
  firing the icon beneath it.
- H-6: Mascot hitbox: tapping the drawn sprite (not its old
  position) opens the menu; tapping just outside the sprite does
  nothing to the mascot and reaches the home beneath.
- H-7: Settings matrix spot checks on the Pad: every hub at narrow
  (~480dp equivalent window), 640dp, and 920dp panel widths, at
  font scales 1.0 and 1.3 — no label wraps one character per line,
  no clipped segmented labels ("Most used", "Newest", "Letters",
  "Thin"), no clipped Source/Mode controls in Look, API key fields
  show label above the field with Paste/Show/Save in one row, hub
  titles never ellipsize, and the Gestures device diagram renders
  at every width.
- H-8: Drawer solidity with a bright wallpaper: open the drawer,
  open search, raise the keyboard, toggle Reduce Effects on and
  off — in every state no home content shows through the sheet,
  the search field, A-Z rail, and letter bubble are solid, the
  sheet has 28dp top corners with a drag handle, and it slides in
  by translation only (no fade) over a dimmed home.
- H-9: Mascot bubble: the border is pixel-stepped (not smooth),
  text reads at 14sp+, and the menu shows four 48dp icon chips
  (Find, Help, Suggest, Dismiss). The bubble still anchors to the
  sprite, flips side to stay on screen, stays above the keyboard,
  and closes on Close and on back. Find flow states unchanged.
- H-10: Crash-sweep spot check: Find with no matching targets,
  an empty layout state, and dock picks with no installed apps
  must never crash — the mascot stays put or poofs to the dock top.
- Hotfix tests: written, NOT run (see COVERAGE.md 3.1.1).
  Nothing in the hotfix is device-verified yet.
