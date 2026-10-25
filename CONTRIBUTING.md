# Contributing to HavenOS

Thanks for looking. HavenOS is a small, opinionated launcher; contributions
that respect that shape are welcome.

## Design law (non-negotiable)

- **HavenOS is a launcher.** Never describe it as an OS, in code, UI, or
  docs.
- **Minimal-only.** One design language. No theme presets, no look
  switcher, no templates, no preset picker. The only theme control is the
  accent colour (Default / Palette / Custom).
- **Horizontal paged home.** Pages swipe horizontally. Add Page and
  Remove Page must never silently lose items (adjacent-move with undo,
  last-page guard).
- **Original code only.** Do not copy code, resources, strings, icons,
  or branding from any other launcher or proprietary product.
  Re-implementing a user-visible behavior in your own code is fine;
  copying someone else's implementation is not.
- **Offline-first.** The only network feature is the optional weather
  widget (Open-Meteo). No accounts, ads, analytics, or cloud sync.

## How to contribute

1. Open an issue first for anything larger than a typo fix, so design
   direction can be agreed before code is written.
2. Keep the change small and complete: the smallest change that fully
   does the job. Delete rather than add when deletion does the job.
3. Match the existing code style (Kotlin, official style, Compose).
4. Add or update unit tests under `app/src/test` for behavior changes.
5. Update docs in the same change: `CHANGELOG.md` for user-visible
   changes, and `docs/` when behavior changes.
6. Verify before claiming done: build with `gradle :app:assembleRelease`
   (see [BUILDING.md](BUILDING.md)) and say what you actually ran.
   Device-only behavior (rendering, widgets, gestures) must be labeled as
   unverified until it has run on a real device — never claim
   "works on device" from a clean build alone.

## What not to send

- Keystores, passwords, `local.properties`, or any credential.
- Screenshots presented as real that are mockups, and vice versa.
- New theme systems, preset pickers, or template galleries — these were
  deliberately removed; they come back only by maintainer decision.
- Silent feature drops: if a change removes or blocks a feature, say so
  in `docs/COVERAGE.md` with the reason and the fallback.
