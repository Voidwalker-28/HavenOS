# HavenOS Mascot — v2 idea (DISCUSSION ONLY, not a build spec)

Status: PARKED. Discussed 2026-10-23. Build only after a successful, polished v1.
Online themes store: considered and DROPPED by the user ("leave it").

## Concept
A small cutesy on-screen companion that stays on screen, moves around slowly,
has expressions, and helps proactively: adding widgets, new customization,
any help the user needs — it asks/offers rather than lecturing.

## Shape (to be decided)
- Lives on the home screen (everywhere-over-apps is a bigger permission/battery question).
- Minimal-compatible design tension: HavenOS is Minimal monochrome; a colorful
  cartoon mascot would clash. Candidate: a small geometric blob in the Minimal
  language, expressing through squash/stretch/blink/bounce rather than a face.
- Tap to chat/ask; proactive nudges only when genuinely useful (first edit mode,
  empty states, new feature discovery). Opt-in, mute/dismiss sticks, never blocks
  icons, sleeps when busy. The Clippy problem is the main design risk.

## AI
Powered by a small LOCAL LLM (on-device, no cloud, no account — keeps the
HavenOS rule). Honest constraints on the Redmi Pad 2 (Helio G99): a ~1–3B
quantized model is runnable but heavy (GB-scale download, RAM, battery).
Likely shape: scripted smarts for common flows (tutorials, suggestions,
settings help — instant, zero battery) + tiny local model waking only for
actual chat.

## Open questions for the user
1. Home-only, or over every app?
2. First job: teach (tutorials), suggest (widgets/layouts), or answer (help chat)?
3. Name? (Half the character.)

## Decisions (2026-10-23)
- Choice: user picks from a roster of 5–10 mascots eventually.
- Launch pair: ONE male + ONE female, small and cute, to start.
- Movement: they roam BETWEEN THE APPS on the home screen (home-screen
  roaming, not over other apps) — wandering the home, pausing, reacting.
