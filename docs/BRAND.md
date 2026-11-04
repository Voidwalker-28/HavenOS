# HavenOS Brand — M1

## Mark concept
A rounded arch / doorway: "haven" = home, shelter. The original exploration
of two overlapping glass shapes was simplified to a single thick arch ring
plus a small knob/sun dot — legible at launcher-icon size, one concept, no
compromises.

Construction (108×108 viewport, `res/drawable/ic_launcher_foreground.xml`):
- Arch ring: outer arch path minus inner arch path via `evenOdd` fill.
  Outer arch spans x 30–78 (width 48, height 34: wall top at y 52, semicircular
  cap of radius 24), closed at the flat bottom y 86. Inner arch inset by ~10px
  all round (x 40–68, cap radius 14), leaving a ~10px stroke.
- Knob dot: filled circle r 4 centred at (66, 66), like a doorknob catching
  light — also reads as a sun rising in the doorway.

## Colors
- Background: `#0E141B` (deep slate-navy — night calm).
- Mark: `#FFFFFF` (pure white).

## Files
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — the mark vector.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` — adaptive icon:
  background `@color/ic_launcher_background`, foreground + monochrome
  `@drawable/ic_launcher_foreground`.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` — same as above.
- `app/src/main/res/values/colors.xml` — `ic_launcher_background #0E141B`.
- (minSdk 26 guarantees `mipmap-anydpi-v26` works; no fallback needed.)

## Roadmap
- §7-grade refinement — tinted variants (Light / Dark / Tinted / Clear),
  wordmark, icon-mask tuning — lands in M5.
- Wordmark font: system default in M1 (no bundled font yet).
