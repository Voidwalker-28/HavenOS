# Settings — Organised structure (mandatory)

*Source: Aurelius, 2026-10-20. Settings must feel premium, searchable, and consistent with the rest of HavenOS.*

## Top of every Settings screen
- Large search bar at the top that searches **all** settings across every category (live results as you type).
- One-line explanation under every option.
- Live preview (where visual) that updates instantly.
- “Reset to default” button per section.
- Changes apply immediately (destructive actions are undoable).

## Main Settings categories (exact order and grouping)

1. **Look & Feel**
   - Looks gallery (large cards with live preview of home + dock + sample widget)
   - Look Editor (full token control + duplicate / rename / export / import .havenlook)
   - Density slider (0–100 with live preview)
   - Appearance: System / Light / Dark / Auto
   - Fonts
   - Motion & Reduce Effects
   - Icon shape & size

2. **Wallpaper**
   - Source: System wallpaper ↔ HavenOS wallpaper (side-by-side live preview)
   - Picker, crop, dim, blur, tint, parallax
   - Per-profile / per-page images (HavenOS mode only)
   - “Apply to system wallpaper” and “Apply to lock screen”
   - Current glass quality level + reason (Full / Partial / Flat)

3. **Home Screen**
   - Templates (Calm, Productive, Glass, Dense) with live preview + Apply / Undo
   - Grid vs Free canvas
   - Columns / Rows (portrait + landscape)
   - Margins & gaps
   - Clock page toggle
   - At-a-glance line style
   - Lock layout
   - Automatically add new apps

4. **Dock**
   - Icon count
   - Sections (pinned / recent)
   - Position (bottom / side)
   - Auto-hide
   - Search pill on/off
   - App-drawer button on/off

5. **Drawer**
   - Mode: Grid / List / Categories / Search-first
   - Categories management
   - Hidden apps (with biometric option)
   - Scrollbar style
   - Sorting (alphabetical / most used / recently installed)

6. **Widgets**
   - Own-widget defaults
   - Widget stacks
   - Weather source, city, units (°C/°F), network toggle
   - Calendars to show
   - Third-party widgets list + clean-up / rebind

7. **Icons & Badges**
   - Icon pack
   - Themed / monochrome icons
   - Labels on/off + size
   - Notification badges (dots or counts)

8. **Gestures**
   - Full gesture editor (trigger → action table)
   - Conflict detection
   - Test button
   - Reset to defaults

9. **Search**
   - Sources (apps, contacts, calendar, settings, calculator…)
   - Ranking reset
   - Clear history

10. **Profiles**
    - Home / Work / Focus + custom profiles
    - Switch manually or by schedule
    - Per-profile Look, Density, pages, dock, wallpaper

11. **Backup & Restore**
    - Export / Import
    - Daily automatic backup
    - Include / exclude wallpapers

12. **Permissions & Access**
    - Live status of every permission
    - One-tap “Fix” button for each
    - Help card for restricted settings (sideload)

13. **About & Help**
    - Diagnostics (full device + glass quality report + Copy)
    - Replay intro
    - Replay tutorial
    - Crash log
    - Open-source licenses
    - Version + package name

## Visual rules for Settings
- Dark glass / frosted cards matching the current Look
- Large touch targets
- Clear section headers
- Smooth transitions between screens
- Never a long ungrouped list

Implement this structure completely. The Settings experience must feel as polished as the home screen being targeted.
