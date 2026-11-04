# HavenOS — Designer Brief (Minimal-only correction — DRAFT, not yet adopted)

DRAFT prepared 2026-10-23 from the shipped M6 build (refreshed from M5
the same day; M7 was still building at refresh time and is not reflected
here). The original `DESIGNER-BRIEF.md` is untouched and still says "one
engine, many Looks" with 7 Looks, starter templates, and M3 as latest.
This draft replaces those parts only; it does not replace the original
file until Aurelius says so.

Forward-ready summary for the UI designer. Current build state: M6 (vc6,
2,198,078 bytes, md5 `158dcb9bde491dac2e9f68a6eb778dec`) is the latest
delivered build, installing over vc5. Design work should assume M6, not
M3 or M5.

## The idea

HavenOS is a tablet-first Android launcher. Minimal is the one design
language — no theme presets, no style picker, no layout presets. The
only theme control is accent colour (see below).

## Target and feel

- Target: 10–11" tablet in landscape (reference device: Redmi Pad 2,
  Android 16).
- Feel: premium and calm, but dense when wanted, with smooth,
  stutter-free motion, and a spacious home.
- Model: all features free, no ads, no accounts, no tracking. The only
  network feature is optional weather (Open-Meteo).

## Core surfaces

- Home: one continuous scroll (no pages, no page dots) — favorites on
  top, widgets and folders in the same flow, full A–Z list below, slim
  alphabet rail.
- Floating frosted dock: centered pill, wallpaper showing through,
  icons evenly spaced (pinned + recents).
- App drawer with frosted search + alphabet rail, plus an optional
  swipe-up drawer (Settings switch, default ON).
- Widget stacks, folders, quick panel.
- Settings: a panel over the live home, not a separate screen — on
  tablet the home stays live on the left (~58%) with the settings panel
  docked right (~42%); six hubs (Look, Home & Dock, Drawer & Search,
  Gestures & Motion, Privacy, Device & Data), lean rows (title + live
  value, help behind ⓘ), search that acts inline, spotlight /
  Before / Undo / Changes, and wallpaper crop by pinch and drag on
  the live wallpaper. Only controls that are actually wired appear —
  the M6 honesty audit removed 39 not-wired settings from the UI.
- Edit mode: home shrinks to 0.87, toolbar + "Editing home" banner,
  long-press icon drag restored as the primary path (menu on release
  without moving; the ≡ handle stays as backup), plus a snap-to-grid
  toggle (Smart Grid / Free Canvas).

## The only theme control: accent colour

- Accent colour: wallpaper-derived, palette, or custom picker.
- It recolours buttons, toggles, and highlights only. Minimal itself
  does not change.
- There are no Look presets, no Look Editor, no .havenlook files, no
  custom looks, no layout templates, and no first-run style picker.
  First run seeds a plain Minimal home (clock card + dock picks).

## Brand mark (already shipped)

A rounded arch / doorway — thick arch ring + small knob/sun dot, white
mark on deep slate-navy `#0E141B`. Legible at launcher-icon size.
Construction (108×108 viewport): arch ring outer x 30–78, inner inset
~10px, knob dot r 4 centred at (66, 66).

## Glass language (part of Minimal's material)

Glass surfaces remain: backdrop blur where available, tint +
vibrancy, 1px specular border, soft inner highlight, drop shadow; a
Reduce Effects switch falls back to flat translucent surfaces.

## Open questions for Aurelius before the designer starts

1. What are they designing first — Minimal refinements, icon set,
   widgets, or something else?
2. In what format should they deliver — Figma, images, tokens?
3. Is the accent palette itself in scope, or should the designer work
   within the existing accent options only?

---
Changes vs the original brief (for Aurelius, not for forwarding):
- Deleted: "one engine, many Looks", all 7 Looks and their tokens,
  Look Editor, .havenlook, starter homes (Glass/Calm,
  Productive/Dense, Premium Glass), 13-category Settings, M3-latest.
- Added: Minimal-only, accent-colour-only, one continuous scroll
  home, six-hub Settings panel over the live home, M6 as latest
  (superseding the M5 draft's 14-category two-pane Settings).
- Kept as-is: tablet target, free model, brand mark, glass surfaces.
- Sources checked 2026-10-23: the M6 handover (vc6, 2,198,078 bytes,
  md5 `158dcb9bde491dac2e9f68a6eb778dec`, panel-over-live-home,
  six hubs, 39 not-wired settings removed) and the on-disk build,
  whose md5 matches the M6 handover exactly.
- Honest note: accent wallpaper-derived was stored-only at M5; the
  M6 audit removed not-wired controls from the UI, but whether
  wallpaper-derived accent now live-applies was not re-verified for
  this refresh, so the brief above still does not promise it
  live-applies.
