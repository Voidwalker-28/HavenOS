# HavenOS 3.1 — Phase F report: on-device assist foundation (prompt §F)

Date: 2026-10-30. Tree: 3.0/vc12 + Phases A–E + this phase.
Verification build only — NOT a release, NOT delivered, nothing published.
Parent owns delivery and the version/release decision.

## What landed

- **F1 Seam.** `ui/assist/AssistRouter.kt` is the one assist provider
  (LocalAssist in effect: rules, synonyms, ranking, no model). The
  `MascotBrain` chat seam is kept; no cloud provider exists.
  `LocalModelBrain` (`ui/assist/ModelPack.kt`) implements `MascotBrain`
  behind `USE_MODEL_BRAIN = false` and delegates to `ScriptedBrain`.
- **F2 Command grammar.** Drawer search and panel (Settings home)
  search both understand compounds through `parseAssistCommands`:
  "dark mode" / "light mode" / "system theme", "bigger icons" /
  "smaller icons" (±8dp), "accent <curated name>", "open <app>",
  "go to <hub>", "toggle calm". Compounds split on " and " / ","
  and are all-or-nothing: one bad clause voids the whole query and
  normal search renders unchanged. One row carries the whole
  compound ("Dark mode: System → Dark · Icon size: 56dp → 64dp")
  with a single Apply tap. App resolution reuses `matchApps`
  (real app list + local launch stats); a band + confidence tie
  refuses rather than guesses. Unknown phrases fall back.
  App prediction is untouched (`suggestApps`: count, recency,
  same-hour boost); categories, suggestions, emotes stay rule-based.
- **F3 Action router.** All actions are the sealed `AssistAction`
  whitelist: OpenApp, OpenHub, SetDarkMode, StepIconSize,
  SetAccentCurated, ToggleCalm. There is no uninstall / hide-all /
  permission variant to emit. Names resolve against the real app
  list and the real settings index (`FIND_SETTING_INDEX` + a hub
  alias table); anything not found falls back to search.
  Setting changes go through the typed setters with range checks
  (dark mode coerced 0..3, icon size clamped 40..68, accent by
  curated name preserving strength/recents). Preview + Changes +
  Undo: panel search applies while the session is active, so
  Changes rebuilds with readable names ("Dark mode", "Icon size",
  "Accent") and Revert; drawer commands park in
  `pendingAssistActions` (a list — one compound can hold two
  setting actions) and the panel-open effect applies them AFTER
  `panelSession.begin()`, so the Before pill shows the pre-change
  home. The row label is the preview; tap is the confirmation;
  no destructive action exists in the whitelist. Hub routing for
  drawer commands uses real routes directly.
- **F4 AI Privacy Ledger.** Privacy hub, after Lock, before More
  options: all five features (Search commands, App prediction,
  Mascot Find, Mascot chat, Suggestions) with what local data
  each reads, an On-device badge, last used time + use count,
  and a per-feature switch. Storage is counts + times ONLY
  (`data/AssistLedger.kt`: hand-rolled flat JSON, no org.json, so
  ledger tests stay JVM-runnable; one DataStore write per record
  via `Settings.recordAssistUse`). Prediction's row derives from
  the launch stats themselves (no per-render write). Find's switch
  reuses `mascotFindOn`; Suggestions' switch maps to
  `mascotTips != Off` (re-enabling restores Rare; full
  Off/Rare/Normal stays in the Mascot hub). Chat's switch gates
  only the conversational branches (greeting/tutorial/help +
  HelpMenu/TutorialRequest inputs); Find always runs when chat
  is off. "Clear learned data" wipes in-memory + persisted launch
  stats and both ledger JSONs; mascot muted/don't-show lists are
  user preferences and are left alone. The prediction switch
  gates the Suggested widget's INPUT (empty stats → its existing
  empty state); ranking code is untouched.
- **F5 Model pack, scaffold only.** `ModelPackManager`
  (app-private `filesDir/model-pack`, sidecar JSON with sha +
  bytes): download on user request only, Wi-Fi-only by default,
  progress + cancel (coroutine cancellation deletes the partial
  file), SHA-256 verification before atomic rename, free-space
  (100 MB headroom) + RAM (1 GB scaffold floor) checks, delete
  button. The empty-URL gate fires before any network use, so "no
  new network calls" stays true in practice. `MODEL_PACK_URL`
  and `MODEL_PACK_SHA256` are EMPTY placeholders — no values
  invented. No runtime is wired (Phase 3 conclusion stands:
  MediaPipe / llama.cpp / ExecuTorch do not integrate cleanly
  here). The Mascot hub Chat model row shows the REAL pack
  state (today: "Not set up") with Delete when a pack is READY;
  there is deliberately no download button that does nothing.
  A model wakes only via the chat path and never returns a
  settings-changing action (delegation only).

## Files changed

New (702 lines total):
- `data/AssistLedger.kt` (66): feature ids, tolerant JSON
  parse/encode, `recordAssistUse`, `clearAssistLedgerJsons`.
- `ui/assist/AssistRouter.kt` (271): whitelist, grammar,
  hub/app resolution, `applyAssistSettingAction` executor.
- `ui/assist/AssistLedger.kt` (108): display entries,
  `clearAssistLearnedData`.
- `ui/assist/ModelPack.kt` (257): sha/gate/status pure
  functions, `ModelPackManager`, `LocalModelBrain`, flag.
- Tests (296 lines): `ui/assist/AssistRouterTest.kt` (143),
  `AssistLedgerTest.kt` (79), `ModelPackTest.kt` (74).

Modified (diff lines added + removed):
- `data/Settings.kt` (33): 5 fields + keys + mapping + setters
  + `recordAssistUse` member.
- `ui/Drawer.kt` (54): command section in `SearchResultsList`
  (first, above Apps), `onAssistCommand` threaded through
  `LibraryOverlay` (preview mini drawer passes nothing).
- `ui/Home.kt` (71): `pendingAssistActions`, panel-open apply
  after `begin()`, drawer command handler (two passes: immediate
  actions, then parked settings), `onEnterCalm` into the registry.
- `ui/settings/shell/HubScreen.kt` (98): panel-search command
  rows + execution, `onEnterCalm` param + registry threading.
- `ui/mascot/MascotBrain.kt` (41): `BrainContext.chatEnabled`
  (LAST param, so positional constructions keep working),
  three conversational gates + HelpMenu/TutorialRequest gates.
- `ui/mascot/MascotLayer.kt` (31): `LocalModelBrain` wiring,
  `chatEnabled` passed, Find/Chat/Suggestions usage recording.
- `ui/settings/hubs/PrivacyHub.kt` (84): ledger section +
  Clear learned data + search entry.
- `ui/settings/hubs/MascotHub.kt` (34): real Chat model row +
  Delete-when-READY.
- `ui/WidgetReal2.kt` (9): prediction input gate.
- Docs: `docs/COVERAGE.md` (Phase F section), this report,
  `docs/DEVICE_CHECKLIST.md` (F-1…F-13),
  `docs/UNVERIFIED.md` (Phase F section).

## Verification

- First build FAILED at compile (2m 13s) with 2 error groups,
  both fixed in place (no scope change): the second
  `SearchResultsList` call site is inside `SearchCard`, not
  `LibraryOverlay`, so it has no `preview`/`onAssistCommand` in
  scope (arg dropped — see BLOCKED); `BrainContext.chatEnabled`
  was inserted mid-constructor and broke positional callers
  (moved to last param).
- Second build: `build.sh` SUCCESS. APK
  `app/build/outputs/apk/release/app-release.apk`, 2,825,857
  bytes, md5 `f4a56ac74624e44d58f1453bfb3bc0ba`, aapt
  vc12 / 3.0, apksigner cert SHA-256
  `53bff2a09aedb516a8ee6193234a7aaf5d04ce901fb8931b799191b9845354a8`
  (havenos match).
- AndroidManifest.xml byte-identical (NO new permission),
  `app/build.gradle.kts` identical (NO new dependency).
  Security grep over new/changed files: no secrets, keys,
  tokens, or credentials (only "setup gaps" / "placeholder"
  comments about the intentionally empty model URL/checksum).
  Glyph scan: no emoji; `→` in command labels follows existing
  shipped precedent (Changes rows, gestures hub).
- Tests: 3 new files written (296 lines), NONE run (sandbox
  executor broken, pre-existing; `SettingsShellTest` still has
  its 10 pre-existing compile errors). No device/emulator: all
  Phase F behavior is built-but-not-proven (F-1…F-13 owed).
- Signing, version (3.0/vc12), and `~/workspace/havenos-github/`
  untouched. Snapshot: `~/workspace/havenos-pre-3.1-phase-F/`.

## Deviations and judgment calls (all with reasons)

- "silent" has NO mapping: no silent / do-not-disturb setting
  exists in `SettingsState` (grep confirmed), and inventing one
  would need new behavior + a permission beyond §F. It falls
  back to normal search. Documented, not silently mapped.
- Panel-search command rows do NOT navigate to the changed
  control: the row stays on the results so the Changes footer
  (with Revert) stays visible as the confirmation surface. The
  hub rows below still navigate normally.
- Mixed app + setting compounds land on the LAST setting
  action's hub; every action still executes exactly once (the
  app launches, the settings park and apply). A hub command
  inside such a compound is superseded as the landing page.
- Ledger switches are NOT session-revertible (same scope as
  `mascotFindOn` and the other mascot toggles, which
  `commitSnapshot` also omits). Router actions ARE revertible
  ("Dark mode", "Accent", "Icon size" already have diff names
  + reverters).
- Ledger history is NOT backed up (device-local ephemeral, same
  scope as mascot prefs which backup omits). On restore, ledger
  switches reset to their on-defaults.

## BLOCKED / not built (with reasons, never silent)

- SearchCard command rows: `SearchCard` shares
  `SearchResultsList` but its Home call site passes no command
  handler, so it renders no command rows in this pass. Drawer +
  panel search cover §F2; wiring SearchCard is a small follow-up
  (thread the handler through its Home call site).
- Model-pack download / checksum / Wi-Fi / space / RAM /
  cancel / delete paths: scaffold-only and unprovable until a
  real model URL + checksum is configured (placeholders empty
  by design). READY / FAILED / DOWNLOADING states are
  unreachable on device today.
- No runtime picked (Phase 3 conclusion stands); `LocalModelBrain`
  delegates until one integrates cleanly.
- F6 (Scenes, Style DNA, Recall, notification digest, cloud AI,
  mascot voice, more characters, online themes store): NOT in
  this pass, not touched.
- Phase E BLOCKED items carried unchanged (pixel-stepped bubble
  border, per-card rail ledges, side-dock perch, dock-app Find =
  bubble + Launch only): §F does not require them.

## Checks owed

F-1…F-13 in `docs/DEVICE_CHECKLIST.md` (command rows drawer +
panel, compounds, refusal fallback, accent command, calm
command, ledger switches, clear learned data, chat-model row
state, prediction switch, chat switch).
