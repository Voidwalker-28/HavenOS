# HavenOS 3.1 — Phase E report: pixel mascot rebuild

Date: 2026-10-30. Tree: 3.0/vc12 (coord-fix included), verification
build only — NOT delivered, NOT published, version unchanged.
Snapshot before: `~/workspace/havenos-pre-3.1-phase-E/` (158M).
Parent rules honored: `~/workspace/havenos-github/` untouched, no
upload, no signing change, no token used, no new dependency, no
Room migration, mascot stays default OFF / home-only / no network /
scripted chat only.

## What landed

### E1 — Art (original pixel sprites, PNG + atlas + preview)

- Sheets: `app/src/main/res/drawable-nodpi/mascot_milo.png`
  (984×24, 1950 bytes, amber) and `mascot_mila.png` (984×24,
  2067 bytes, pink). 41 shared frames, 24×24 cells, transparent
  background, 1px dark outline, 8 colours each. Milo squarer +
  tuft, Mila rounder + ribbon; no red cap, mustache, overalls,
  or borrowed assets/sounds. Layout: idle 0–3 (blink, 4fps),
  walk 4–9 (10fps), jump 10–14 (rise, apex, fall, land ×2, 8fps),
  sit 15–16, sleep 17–19 (1fps, Z rising), wave 20–23, happy
  24–27, surprised 28–29, sad 30–31, think 32–34, point 35–36
  (arm + arrow), poof 37–40 (cloud only on 39–40). Face right,
  mirrored for left. Generator: `docs/mascot/gen_sprites.py`
  (Python PIL — PNG production succeeded, so no
  palette-indexed maps in code were needed).
- Atlas: `app/src/main/res/raw/mascot_frames.json` (1308 bytes,
  version 1, cell 24, 41 frames, animations + fps).
- Preview: `docs/mascot/preview.png` (760×3088, 93,243 bytes) —
  all 41 frames at 4× on gray AND dark backgrounds, per
  character. Pixel-count verified: 39 body frames carry exactly
  1 sentinel pixel each; poof frames 39–40 carry 0; frame 0
  opaque pixels Milo 258 / Mila 259; frame 40 opaque pixels 6.
- Loader/rendering (`ui/mascot/MascotSprites.kt`, 222 lines):
  magenta sentinel pixels palette-swapped to the current accent
  at load; sheet split into 41 per-frame bitmaps (no per-frame
  allocation at draw); integer scale `max(2, round(targetPx/24))`
  with FilterQuality.None for Small 56dp / Medium 72dp / Large
  96dp; `DEFAULT_ATLAS` fallback if the JSON fails to parse;
  `PixelSprite` (timer loop at atlas fps) + `StaticSpriteFrame`
  (single static frame) + `rememberSpriteSet` (null → caller
  falls back to vector).
- Old vector mascot KEPT behind `USE_PIXEL_MASCOT = true`
  (false restores MascotFigure everywhere). Not deleted until
  the new art is accepted.

**Honest quality disclaimer: the agent cannot judge whether
the art "looks good".** Aesthetics are for human review from
`docs/mascot/preview.png`. Only functional facts are claimed
above (distinct frames, correct layout, transparency, sentinel
presence). Preview was visually read: all frames render
distinct on both backgrounds, transparent background works (no
white boxes), tuft/ribbon sentinel visible as magenta (becomes
accent at load), walk bob subtle, point arrow visible at right
edge, sleep Z visible, wave arm visible, surprised white eye
sparkle visible, poof frames 2–3 very faint on gray (white
puffs on gray, inherent, transient).

### E2 — Behavior engine

- `ui/mascot/MascotEngine.kt` (NEW, 477 lines, pure Kotlin, no
  Compose/Android imports): `PixelMascotState` (Hidden,
  Spawning, Idle, Walking, Jumping, Falling, Sitting, Sleeping,
  Reacting, Carried, Pointing, Despawning) + `canTransitionTo`
  (hide ALWAYS goes through Despawning/poof first); ledge
  graph (`buildSegments` + `buildLedges` with the clearance
  rule — segments cut at clearance-band overlaps with strict
  overlap, parts narrower than the sprite dropped);
  `planRoute` (BFS: walk along a ledge, jump up to 2 rows /
  3 cells, drop to any lower ledge); `snapDropToLedge`;
  `nearestLedgePoint`; planner (`pickTargetLedgePoint` weighted
  DockTop 3× / EmptyCell-PageBottom 2× / else 1×, seeded
  Random injected; dwell 3–12s; sit 60–120s; sleep 5 min;
  speed 40–60dp/s); `GraphInputs` + `graphInputsChanged`;
  `FindFlowState` + `FindFlowEvent` + `nextFindFlowState`
  (20s timeout const; Cancel/Timeout/ModeChanged win from any
  live state; Done from Searching/Choosing/Navigating → Done,
  added this phase so terminal answers don't strand the flow).
- Layer (`ui/mascot/MascotLayer.kt`, +845/−182): graph from the
  LIVE layout (row tops span full width, widget tops from live
  cell rects, bottom-dock top, empty rows, rail-top from the
  rail container rect; jump rise = 2× live row pitch, gap =
  3× live cell width — never a hardcoded dp guess); graph
  rebuild keys cover every prompt trigger (live rects, layer
  size, page, home items, dock side + thickness, rail rect,
  sprite scale, density); ledge loss → fall to nearest valid
  ledge below, else poof + respawn on dock top (or
  layer-bottom fallback); host poof handoff (gate closing
  while visible keeps the layer composed with `hideRequested`
  until the 700ms poof finishes; reopening mid-poof cancels
  it); planner frozen during poof; timers restart (not
  resume) on home resume and never fire while stopped; Reduce
  Motion / battery saver / roaming-off park on the dock top
  in static idle pose, taps still work; position via
  `animateFloatAsState` read ONLY in `graphicsLayer` lambdas;
  frame stepping time-based (timer delays at atlas fps, no
  per-frame callbacks); single overlay layer, touch-transparent
  except sprite hitbox + bubble.
- Long-press + drag: custom 350ms detection (`awaitFirstDown`
  + `withTimeoutOrNull`; movement before the timeout is NOT
  our gesture and is never consumed); Carried + haptic tick
  via `LocalHapticFeedback` (gated by pressFeedback /
  hapticStrength, never Vibrator); drag loop consumes only
  post-long-press moves; drop snaps to a ledge within 48dp
  vertically and persists the home spot, else returns to the
  previous ledge; scope-cancel mid-carry restores the previous
  ledge (never hangs Carried). Short tap toggles the menu
  (semantics `onClick` for TalkBack). Carried renders jump
  apex frame 11 as the dangling frame — the atlas has no
  dedicated dangling frame; apex (legs tucked) is the closest
  read (agent decision, flagged).

### E3 — Interaction + Find (flow 14, tracked end to end)

- Bubble: 14sp system-font text throughout (all bubble texts
  raised from 13sp); four 48dp chips (Find, Help, Suggest,
  Dismiss); anchors to sprite, flips side, clamps above the
  IME (`WindowInsets.ime` bottom); closes on Close, back
  (`BackHandler`), or touch outside (root `pointerInput` that
  never consumes, ignores downs inside sprite/bubble rects, so
  home taps still do their home action).
- Flow tracking: layer sends Start on Find open,
  QuerySubmitted on submit, Results from the brain's returned
  `FindDecision` (`MascotResponse.findDecision`, NEW defaulted
  field — no interface change; ScriptedBrain sets it for
  Act/Choices/NoMatch), CandidatePicked on choice, Arrived on
  reaching the target, Done on terminal answers. Cancel on
  touch outside, back, Find toggled off mid-flow, or 20s
  without a transition out of Searching/Choosing
  (Navigating/Pointing not timed out: movement and the ring
  are themselves progress, both end on user action). "Mode
  change" is read as Find toggled off; character/size/roaming
  changes do not cancel.
- Asking field: styled `OutlinedTextField`, label "Search
  apps, settings…", Search IME action (action key or Find
  button submits), accent focus border + cursor. Resolution
  reuses the EXISTING adapter (`rankFind`, no second index):
  exact, prefix, synonym, fuzzy + widget targets
  (`FindTarget.Widget`, matched in the app group so a widget
  exact match beats a setting prefix match). No-match says
  "I can't find that." with [Search in drawer, Try again];
  never guesses.
- Target kinds: visible-page app → walk/jump to nearest valid
  ledge by the icon, face it, Point, spotlight ring;
  other-page app → pager animates, waits for the settled page
  (`hostCurrentGridPage`), graph rebuilds, then walk + Point;
  folder/drawer-only app → bubble + Launch chip (never a ring
  inside a closed folder; folders never opened unasked);
  widget → Point at its frame (same guide path, page resolved
  from homeItems); Settings control → panel opens at the hub
  with the row deep-linked AND ringed (see below), mascot
  stays composed via the pointing exception, Done ends Find
  WITHOUT closing the panel. Spotlight: accent 2dp ring, soft
  pulse (static under Reduce Motion), 40% dim (Canvas,
  offscreen compositing + `BlendMode.Clear` hole); any tap on
  the dim dismisses the guide (that tap is consumed by design;
  the next tap reaches home). One shared `executeRoute`
  helper for planner trips and Find guidance; a guide starting
  mid-trip aborts the planner route at the next step boundary.
- Settings row ring: `DeepLinkAnchor` now draws a static 2dp
  accent border on the linked row while the panel is open.
  This also rings the existing wallpaper and coingecko-key
  deep links — consistent (shows where you landed).
- Anchors added (wrap row content, existing pattern):
  LookHub Accent → "accent" (wallpaper already existed);
  MascotHub Mascot switch → "mascot", Find switch → "find",
  quiet-hours start → "quiet", Reset → "reset"; HomeDockHub
  Grid row → "home" (nested with the existing "grid" anchor),
  Widgets → "widgets", Dynamic Island → "island". Dock
  position already had "dock". All other help-map controlKeys
  (folders, drawer, search, gestures, animations, privacy,
  backup, profiles, apis, look, theme) fall back to hub level:
  panel opens at the hub, no row ring, bubble names the
  control. No row was redesigned to force an anchor.
- Help map 13 → 18 entries (island, mascot-find, mascot-move,
  today-cards, offline-cards). controlKeys filled where
  anchors exist: accent, wallpaper, dock, widgets, grid
  ("home"), mascot, find, quiet, reset, island. **Grid answer
  text CORRECTED**: old text said "Settings, then Look, and
  adjust density" — wrong, grid lives in Home and Dock; new
  text says "Settings, then Home and Dock, and adjust the
  grid". Suggest keeps existing gates (≤1/session, 24h
  cooldown, quiet hours 22:00–07:00). Teach one-time,
  don't-show-again respected. Dismiss sticks across restarts.
  Chat stays scripted (ScriptedBrain behind MascotBrain; no
  model brain here — Phase F).
- GuideSetting from typed-question help keeps user-chosen Open
  (now passing controlKey so the row rings); the pointing
  exception applies to the Find path only (agent decision:
  auto-opening Settings on every help answer would be a
  behavior change beyond the prompt).

### E4 — Mascot hub + empty states

- Hub (`ui/settings/hubs/MascotHub.kt`, +168/−64): 120dp
  preview box with the animating pixel sprite (vector fallback)
  + Preview button playing idle → walk → jump → wave once
  (~1500ms each), then resting on idle. Tips Off/Rare/Normal
  (Settings coercion widened 0..1 → 0..2 at read line 823 and
  setter line 1136; stored values migrate by widening only, no
  Room change). Quiet hours are rows opening 24-hour
  TimePicker dialogs (minutes now settable; hour sliders
  removed). Reset row renamed "Reset mascot" (scope unchanged)
  + "Reset mascot" search entry. Roaming help corrected
  (ledges/dock top, not perches/home spot). On/off, character,
  roaming, size, Find, Reduce motion, Chat model "Not set up",
  Bring back mascot unchanged.
- Tips gates widened: every `mascotTips == 1` check → `!= 0`
  (auto-nudge, Suggest menu, tutorial offer). Normal (2) adds
  ONLY a help-tip fallback when no pin/widget candidate exists
  (first unmuted help entry; Open chip opens its route);
  Rare keeps today's behavior exactly (agent decision,
  flagged). All E3 gates (≤1/session, 24h, quiet hours) apply
  to all non-Off.
- Static poses as empty-state hosts (static images only, never
  the roaming overlay): drawer "No matches" → pixel surprised
  frame 28 (vector SAD fallback); API card offline/error →
  pixel sad frame 30 (vector SAD fallback, Phase C's pose);
  Agenda "No events today" → pixel sleeping frame 17
  (pixel-only addition; vector path keeps today's figure-less
  layout). Find no-match bubble → surprised, Find Searching →
  think pose, both via expression mapping. General loading
  skeletons untouched (thinking-as-loading is covered by the
  Find Searching state, not by replacing skeletons).

### E5 — Tests (written, NOT run)

- `app/src/test/.../ui/mascot/MascotEngineTest.kt` (NEW, 318
  lines) + `MascotSpritesTest.kt` (NEW, 72 lines). Coverage:
  state machine; ledge graph incl. clearance rule + no-valid-
  ledge layout; segment derivation; routing incl. no-route;
  planner determinism (seeded); invalidation triggers; Find
  flow incl. Done from Searching/Choosing/Navigating → Done
  (added with the engine change); Find resolution kinds;
  sprite size targets, integer scale, time-based frame
  stepping, atlas parsing, DEFAULT_ATLAS layout match,
  expression mapping.
- NONE ran: sandbox test executor broken (pre-existing);
  SettingsShellTest still has its 10 pre-existing compile
  errors. Existing tests untouched in shape (BrainContext
  gained a defaulted `widgets` field; GuideSetting gained a
  defaulted `controlKey`).

## Files changed (vs `~/workspace/havenos-pre-3.1-phase-E/`)

| File | Change |
|---|---|
| ui/mascot/MascotLayer.kt | +845/−182 (rewrite: graph, planner, gestures, bubble, Find, spotlight, tips) |
| ui/mascot/MascotEngine.kt | NEW, 477 lines |
| ui/mascot/MascotSprites.kt | NEW, 222 lines |
| ui/mascot/MascotBrain.kt | +68/−15 (Widget target, controlKeys, 5 help entries, findDecision) |
| ui/mascot/MascotFind.kt | +46/−3 (Widget target + ranking) |
| ui/settings/hubs/MascotHub.kt | +168/−64 (preview, Normal tips, TimePickers, anchors, rename) |
| ui/settings/hubs/LookHub.kt | +7/−5 (accent anchor) |
| ui/settings/hubs/HomeDockHub.kt | +7/−0 (home/widgets/island anchors) |
| ui/settings/shell/SettingsPanel.kt | +11/−0 (row ring) |
| ui/Home.kt | +40/−6 (pointing state, poof handoff, rail rect, new params) |
| ui/Drawer.kt | +24/−8 (surprised pose) |
| ui/WidgetReal2.kt | +20/−0 (sleeping pose) |
| ui/TodayApiCards.kt | +24/−8 (sad pose) |
| data/Settings.kt | +2/−2 (tips coercion 0..2) |
| test/.../MascotEngineTest.kt | NEW, 318 lines |
| test/.../MascotSpritesTest.kt | NEW, 72 lines |
| res/drawable-nodpi/mascot_milo.png, mascot_mila.png | NEW (art) |
| res/raw/mascot_frames.json | NEW (atlas) |
| docs/mascot/gen_sprites.py, preview.png | NEW (generator, preview) |
| docs/COVERAGE.md, DEVICE_CHECKLIST.md, UNVERIFIED.md | Phase E sections appended |

Diff review: only the files above differ (plus the three doc
files); `AndroidManifest.xml` byte-identical — Phase E adds NO
permission. No dependency added. No Room migration. Signing,
version (3.0/vc12), `~/workspace/havenos-github/` untouched.

## Checks run + exact results

- `~/workspace/havenos/build.sh`: first attempt FAILED at
  compile (4m 30s, 8 error groups, all in the layer/host — no
  scope change): `awaitPointerEventScope` is a
  PointerInputScope member, not a top-level import (import
  deleted; CalmMode/HomeA11y/Dock use it unimported); missing
  `animateFloat` + `layout.ime` imports; graph block referenced
  undefined `ox`/`oy` (now `layerOrigin.x/y`); `moveDurationMs`
  was Int state assigned Long (now Long state); two `run {
  continue }` inline-lambda continues (restructured to
  if-null + continue, language version < 2.2);
  `PointerChange.positionChange()` is not a function in this
  Compose version (now `position − previousPosition`); Home.kt
  lacked the `boundsInWindow` import. Second attempt: **BUILD
  SUCCESSFUL in 11m 17s**.
- APK: `app/build/outputs/apk/release/app-release.apk`,
  **2,809,473 bytes, md5 eebdc1cf4fb21ad96432049fb4613b1d**.
- aapt: package `app.havenos.launcher`, versionCode 12,
  versionName 3.0, minSdk 26, targetSdk 36.
- apksigner: CN=HavenOS, SHA-256
  `53bff2a09aedb516a8ee6193234a7aaf5d04ce901fb8931b799191b9845354a8`
  — matches the persisted havenos keystore cert.
- Permissions in APK: INTERNET, ACCESS_NETWORK_STATE,
  READ_CALENDAR, READ_CONTACTS, SET_WALLPAPER,
  SYSTEM_ALERT_WINDOW, WRITE_SECURE_SETTINGS, WAKE_LOCK,
  RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE (+ custom
  permission) — all pre-existing; Phase E adds none.
- Secrets grep over changed files: no secrets, keys, tokens,
  or credentials (only benign query-tokenization matches).
- Tests: written, NOT run (see E5).

## Deviations (agent decisions, parent may overrule)

1. Pixel art aesthetics NOT judged (see E1 disclaimer);
   preview committed for human review.
2. Jump apex frame 11 doubles as the Carried dangling frame
   (no dedicated dangling frame in the prompt's frame list).
3. Jump/drop moves are linear tweens; jump frames carry the
   arc read (a curved tween would double-apply it).
4. Frame loop = timer delays at atlas fps for all animations
   (no withFrameNanos, no per-frame callbacks); position via
   `animateFloatAsState` read only in `graphicsLayer`. This is
   the reading of "frame loop only while moving; idle/sleep
   use timers".
5. Timers restart (not resume-from-pause) on home resume;
   they never fire while home is stopped.
6. Side docks contribute a dock obstacle but NO dock-top perch
   (a vertical dock's top edge at y=0 cannot hold a sprite
   without clipping); dock-top perch applies to bottom dock
   only.
7. Rail coverage = rail-top segment from the rail container
   rect only; per-card rail ledges need per-card rects (not
   plumbed) — see BLOCKED.
8. Normal tips = Rare gating + help-tip fallback only.
9. Grid help answer corrected (Look → Home and Dock).
10. GuideSetting from typed-question help keeps user-chosen
    Open (with controlKey); the pointing exception applies to
    the Find path only.
11. Find timeout covers the waiting states (Searching,
    Choosing) only; Navigating/Pointing end on user action.
12. "Mode change" cancel is read as Find toggled off mid-flow.
13. Close-bubble during Settings pointing also clears the
    pointing state + flow (otherwise the layer would stay
    composed and the planner paused with no way back).
14. The gesture block in MascotLayer has uneven indentation
    from the try/finally edit (cosmetic only; compiles clean).

## BLOCKED / not built (with reasons, never silent)

- Pixel-stepped bubble border (2px steps at sprite scale):
  needs a custom stepped border draw that would fight the
  shell's rounded-rectangle bubble anatomy from Phase C; the
  readable 14sp+ text + 48dp chips (the accessibility
  substance) are built, the stepped outline is cosmetic.
- Per-card Today rail ledges: need per-card rects, not plumbed
  (TodayRail exports no card rects). Follow-up: plumb per-card
  rects the way PagedHome plumbs cell rects.
- Side-dock top perch: top edge at y=0 clips the sprite.
- Dock-app Find targets: bubble + Launch only, no walk/ring
  (dock item rects not plumbed). Follow-up: plumb dock rects.
- Find pointing while a page is swiping: graph keys on the
  settled page; guides wait via the page guard.
- Guide starting mid-planner-trip resolves within one step
  (planner abort check at step boundaries).

## Unverified (device-only, Phase G checklist via DEVICE_CHECKLIST)

- All visual/gesture behavior: E-1…E-15 in
  `docs/DEVICE_CHECKLIST.md` (art at integer scales, accent
  swap, ledge standing, roaming/jump/drop, carry + haptic,
  poofs, Find walk + point + ring + dim, panel row rings, hub
  preview, TimePickers, empty-state poses).
- All written tests (executor broken, pre-existing).
- No APK delivered, nothing published — parent owns delivery
  and the version decision (3.1.0/vc13 final step).
