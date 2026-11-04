# HavenOS Mascot — Plan (v2)

Status: **PARKED.** Build only after HavenOS v1 is polished and successful.
Not part of M6 or any v1 milestone. Online themes store: considered, dropped.

## Vision
A small, cute companion that lives on the home screen, roams between the
apps, has expressions, and helps when asked — or offers help when it is
genuinely useful (adding a widget, a customization, a how-do-I question).
Powered on-device (small local LLM for chat; scripted smarts for everything
else). No cloud, no account, no tracking — the HavenOS rule holds.

## Decisions already made
- **Launch pair:** one male + one female, small and cute.
- **Later roster:** 5–10 mascots the user picks from.
- **Movement:** roams between the apps on the home screen (home-only, not
  over other apps).
- **Names:** undecided. Shortlist — Milo & Mila, Hugo & Hazel, Pip & Pop,
  Boba & Mochi, Neo & Nia. Avoid Nova (other launcher) and Luna (other app).

## Design constraints (non-negotiable)
1. **Minimal-compatible look.** HavenOS is Minimal monochrome. The mascot is a
   small geometric blob in the same language — expression through
   squash/stretch, blink, bounce, tilt, not a loud cartoon face. Accent color
   may tint it; nothing else about Minimal changes.
2. **Never Clippy.** Opt-in. A mute/dismiss that sticks. Never blocks an icon
   or a tap target. Quiet by default; no nagging. Sleeps when the user is busy.
3. **Battery-safe.** Idle is cheap: mostly still, occasional blink or shift.
   No constant animation loop, no always-listening model.
4. **Home-only rendering.** Because mascots live on the home screen, they draw
   as a layer inside the launcher's own home UI — no system-wide overlay
   permission, no floating over other apps.
5. **Offline-first.** Everything except the optional chat model works with no
   network. The chat model itself runs on-device.

## Phases

### Phase 1 — Mascot engine
- Home-screen layer: mascot entity with position, state machine
  (idle / roaming / reacting / sleeping), and a roaming path that wanders
  between app rows and widgets, pauses, and reacts to taps.
- Expression set: blink, happy bounce, thinking tilt, celebrate, sleep.
- Tap mascot → small bubble menu (Help / Suggest / Dismiss).
- Settings: mascot on/off, pick mascot (pair first), roaming on/off.
- Acceptance: zero blocked taps, idle battery cost negligible, dismiss sticks
  across restarts.

### Phase 2 — Scripted help (the real value, zero AI cost)
- Tutorials: first edit mode, first folder, first widget — offered once, with
  a "don't show again" that is respected.
- Suggestions: contextual, rare, useful — e.g. "you open this app every
  morning, pin it?" or "your home has room for a clock widget."
- Help answers: a local Q&A map for the common questions (hide an app, change
  accent, add to dock, resize a folder) that opens the right Settings screen
  with a spotlight on the control.
- Proactive rules: at most one nudge per session, never twice for the same
  thing, never during a call/media, quiet hours respected.

### Phase 3 — Local LLM chat
- A small quantized on-device model (~1–3B) that wakes ONLY when the user
  opens a chat with the mascot. Scripted answers stay the default path.
- Honest constraints (Redmi Pad 2, Helio G99): GB-scale model download,
  RAM pressure, latency. Ship as an optional download, never bundled, never
  required. If the device can't run it comfortably, chat stays scripted and
  says so plainly.
- The model answers help questions in HavenOS's voice and can trigger the
  same actions as Phase 2 (open a setting, start a tutorial) — it never
  changes settings by itself without confirmation.

### Phase 4 — Roster expansion
- Grow from the launch pair to 5–10 selectable mascots.
- Picker in Settings with a live preview of each mascot's idle + expressions.
- Personalities stay light: each mascot differs in look, idle motion, and a
  few signature reactions — not in capability.

## Open decisions (user picks, before Phase 1)
1. Names for the launch pair (shortlist above).
2. First job priority: teach (tutorials) / suggest / answer (help chat).
3. Exact look: final blob design (designer brief, Minimal language).

## What this plan deliberately excludes
- Mascots over other apps (home-only by decision).
- Cloud AI, accounts, or any data leaving the device.
- An online mascot/theme store (dropped).
- Mascot voice (text bubbles only, unless revisited).
- Any v1 scope: this plan touches nothing in M6 or earlier milestones.
