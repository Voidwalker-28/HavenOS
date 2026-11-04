# HavenOS — Designer Brief

Forward-ready summary for the UI designer. Current build state: M3 (vc3,
2,112,018 bytes, md5 `c9f49c27782e277cdf0a2a6399686bc8`) is the latest
build. M4 direction still waits on Aurelius's on-device M3 verdict, so
design work should not assume M4 features.

## The idea

HavenOS is a tablet-first Android launcher. One engine, many Looks: the
user picks a Look plus a Density and the whole home restyles — glass
surfaces, widgets, dock, folders, drawer, settings.

## Target and feel

- Target: 10–11" tablet in landscape (reference device: Redmi Pad 2,
  Android 16).
- Feel: premium and calm, but dense when wanted, with smooth,
  stutter-free motion.
- Model: all features free, no ads, no accounts, no tracking. The only
  network feature is optional weather (Open-Meteo).

## Core surfaces

- Home grid / free canvas
- Floating glass dock (pinned + recents, optional search pill)
- App drawer with frosted search + alphabet rail
- Widget stacks
- Folders
- Quick panel (Look + Density switching)
- 13-category Settings

## Starter homes a designer should know

- Glass/Calm: huge serif + analog clocks, large weather, agenda,
  now-playing, floating dock.
- Productive/Dense: full clock page + dense widget/folder page.
- Premium Glass: same structure as Calm, darker + gold/silver accent.

## Brand mark (already shipped)

A rounded arch / doorway — thick arch ring + small knob/sun dot, white
mark on deep slate-navy `#0E141B`. Legible at launcher-icon size.
Construction (108×108 viewport): arch ring outer x 30–78, inner inset
~10px, knob dot r 4 centred at (66, 66).

## The 7 built-in Looks

Dark accent / corner radius / glass alpha / blur:

- Glass: `#7FB2F0` / 24dp / 0.62 / blur on
- Soft: `#8FD0C2` / 32dp / 0.78 / blur on
- Minimal: `#B8C4D0` / 12dp / 0.92 / no blur
- Futuristic: `#6EE7FF` / 16dp / 0.58 / blur on
- Classy: `#D8B26A` / 20dp / 0.74 / blur on, serif clock
- Retro: `#FF9E64` / 8dp / 0.88 / no blur, serif clock
- Premium: `#E5C97B` / 28dp / 0.66 / blur on, serif clock

Glass language everywhere: backdrop blur where available, tint +
vibrancy, 1px specular border, soft inner highlight, drop shadow; a
Reduce Effects switch falls back to flat translucent surfaces.

## Open questions for Aurelius before the designer starts

1. What are they designing first — Look refinements, icon set, widgets,
   or something else?
2. In what format should they deliver — Figma, images, tokens?
3. Should they work from the current shipped Looks above, or propose a
   new direction?
