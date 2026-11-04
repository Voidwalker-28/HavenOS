# HavenOS — Designer Brief (Minimal-only correction — DRAFT v2, not yet adopted)

DRAFT prepared 2026-10-24 from the shipped M10 build. It supersedes
`DESIGNER-BRIEF-MINIMAL-DRAFT.md` (2026-10-23, M6 latest, one continuous
scroll home), which is now stale: M8 restored the horizontal paged home
and M10 rebuilt the home shell. The original `DESIGNER-BRIEF.md` is
untouched and still says "one engine, many Looks" with 7 Looks, starter
templates, and M3 as latest — do NOT forward that one. This draft does
not replace the original file until Aurelius says so.

Forward-ready summary for the UI designer. Current build state: M10
(vc10, 2,449,814 bytes, md5 `e967afa88365b48fd5f88be36fbd5d0b`) is the
latest build, installing over vc9. Design work should assume M10, not
M3, M5, or M6. Note: vc10 itself is built and verified; its download
link was still pending at the time of writing (upload-service retries).

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

- Home: horizontal paged home — swipe left/right between pages, dot
  indicator above the dock, page overview (reorder, hide, set default
  page, drag icons between pages). Add Page and Remove Page both
  exist: empty pages delete freely; a page with apps on it moves its
  icons to an adjacent page with undo; the last page cannot be
  deleted; nothing is ever silently lost.
- Four-zone shell (M10): Atmosphere (wallpaper full-bleed behind
  everything, readability fixed locally, never a whole-screen dark
  veil), Today rail, Workspace, and Dock. On tablet landscape the
  Today rail sits left (~30%) beside a centred workspace (~70%); on
  smaller widths it becomes a collapsible strip, and on phones the
  Today page/sheet is kept. The dock is a persistent shell component
  outside the pager — it never changes between pages and is never
  covered by widgets — in Bottom, Left, or Right positions.
- One occupancy rule: every icon, folder, and widget claims real
  cells, so nothing silently overlaps. Moves push other items or are
  rejected with a reason, and undo restores the whole layout.
- Grid and density are real: the grid setting produces exactly that
  grid (with a preview of what moves before anything commits), and
  density levels are Spacious / Balanced / Dense / Custom.
- App drawer (Library) with All / List / Categories, frosted search
  + alphabet rail, plus an optional swipe-up drawer (Settings switch,
  default ON).
- Widgets are real, with proper states (loading, empty, permission,
  error, offline) — no blank rectangles. Widget stacks and folders
  included.
- Settings: a panel over the live home, not a separate screen — on
  tablet a master-detail layout, with the home re-laid out in the
  free area so the dock is never hidden under the panel; six hubs
  (Look, Home & Dock, Drawer & Search, Gestures & Motion, Privacy,
  Device & Data), lean rows (title + live value, help behind ⓘ),
  search that acts inline, spotlight / Before / Undo / Changes, and
  wallpaper crop by pinch and drag on the live wallpaper. Only
  controls that are actually wired appear.
- Edit mode: one 56dp toolbar in its own row (never overlapping
  icons), a floating context bar that appears only when something is
  selected, 50-level undo, auto-snapshots with Restore layout, and
  long-press drag as the primary path (menu on release without
  moving; the ≡ handle stays as backup).

## The only theme control: accent colour

- Accent colour: Default, Palette, or Custom picker. There is no
  wallpaper-derived accent option in vc10 (see the honest note below).
- It recolours buttons, toggles, and highlights only. Minimal itself
  does not change.
- There are no Look presets, no Look Editor, no .havenlook files, no
  custom looks, no layout templates, and no first-run style picker.
  First run seeds a plain Minimal home (clock card + dock picks).
- Minimal's material is three surfaces only (canvas, tonal, elevated)
  — no per-card borders and shadows. Glass surfaces remain as part of
  Minimal: backdrop blur where available, tint + vibrancy, 1px
  specular border, soft inner highlight, drop shadow; a Reduce
  Effects switch falls back to flat translucent surfaces.

## Brand mark (already shipped)

A rounded arch / doorway — thick arch ring + small knob/sun dot, white
mark on deep slate-navy `#0E141B`. Legible at launcher-icon size.
Construction (108×108 viewport): arch ring outer x 30–78, inner inset
~10px, knob dot r 4 centred at (66, 66).

## Open questions for Aurelius before the designer starts

1. What are they designing first — Minimal refinements, icon set,
   widgets, or something else?
2. In what format should they deliver — Figma, images, tokens?
3. Is the accent palette itself in scope, or should the designer work
   within the existing accent options only?

---
Changes vs the previous draft (for Aurelius, not for forwarding):
- Home corrected: horizontal paged home with Add Page + Remove Page
  (M8), replacing the one-continuous-scroll home the old draft
  described. The old draft's "remove pages" reading was the
  misread corrected on 2026-10-23: it means deleting extra added
  pages, not removing paging.
- Shell corrected: four-zone shell with Today rail + workspace +
  persistent dock outside the pager (M10), replacing the old draft's
  floating pill dock and scroll flow.
- Latest build corrected: M10 (vc10) instead of M6 (vc6).
- Edit mode corrected: 56dp toolbar + context bar + 50-level undo +
  snapshots/Restore (M9/M10), replacing the M5-era 0.87 shrink +
  banner description.
- Settings corrected: tablet master-detail with home re-laid in the
  free area (M10 dock-under-panel gap closed).
- Kept as-is: tablet target, free model, brand mark, Minimal-only,
  accent-colour-only, glass as part of Minimal.
- Sources checked 2026-10-24: M10 handover (vc10, 2,449,814 bytes,
  md5 `e967afa88365b48fd5f88be36fbd5d0b`), verified again on disk
  this morning (size and md5 match exactly); M8/M9 handovers for the
  paged home and edit-mode rework.
- Honest notes, not for forwarding: checked against the vc10 code
  2026-10-24 — there is no wallpaper-derived accent in vc10 at all.
  The Settings Accent control offers Default / Palette / Custom
  only; a stored mode-3 value is kept for backup round-trip only,
  falls through to the Minimal default, and no wallpaper colour
  extraction exists (Looks.kt, LookHub.kt). Known M10 gaps a
  designer should not assume are
  built: folder preview modes (the one audit bug left not fixed),
  two-line label reserve, and "Send to Today". Two decisions are
  still open and are deliberately NOT settled in this brief:
  whether a standalone Identity-layer control (Off/Subtle/Statement)
  should exist at all (the "HNK" overlay was confirmed to be
  wallpaper pixels, not HavenOS-drawn, so no such control was
  built), and whether vertical scroll stays as an option alongside
  horizontal paging.
