# URGENT VISUAL UPGRADE — Match the reference images (v2.2)

*Source: Aurelius, 2026-10-20. The four reference images were referenced but not attached; the text below fully describes each template — build from the text. (Builder note.)*

The current home screen is too basic. Rebuild the home experience so it looks like the four reference images (Calm/Glass iOS style, Dense One-UI productivity, Spider-Man themed, and the dual clock/lock-style pages).

## 1. Glass system (must be visible immediately)
- Implement the full LookTokens glass pipeline.
- Default Look on first launch = **Glass** (or Premium with glass enabled).
- Every card, dock, folder, and widget must use:
  - Backdrop blur (sampled wallpaper copy)
  - Tint + vibrancy
  - 1 px specular border
  - Soft inner highlight + drop shadow
- Reduce Effects toggle must exist and fall back to flat translucent surfaces.

## 2. Starter Templates — force the polished layouts

**A. Glass / Calm (iOS-style)**
- Top: huge serif digital clock + analog clock side-by-side
- Large weather card (current + 4-day)
- Agenda card with 3 timed events
- Now-playing card with art, progress, controls
- Two rows of labeled icons with generous spacing
- Floating glass dock (6 icons + search pill)

**B. Productive / Dense (One-UI style)**
- Full Clock page option (huge time + weather + corner shortcuts)
- Page 2: analog clock + temp, month calendar, two agenda cards, hosted inbox widget, storage + memory bars, quick tiles row, labeled folder cards, 10–11 icon dock ending with drawer button

**C. Premium Glass**
- Dark metallic glass, large soft shadows, refined springs
- Same structure as Glass but darker + gold/silver accent

**D. Free / Custom**
- User can start from any of the above and edit

Applying a template must show a live preview and be undoable.

## 3. Own widget kit — make them beautiful (priority order)
1. Clock (digital serif, analog, flip, editorial) — large sizes first
2. Weather (current + hourly strip + 4-day) using Open-Meteo
3. Agenda / Calendar month
4. Now Playing (media session)
5. Storage + Memory bars with clean button
6. Countdown, Tasks, Notes, Gallery, Quick tiles

All widgets must read LookTokens so they automatically restyle when the user changes Look.

## 4. Dock
- Floating, highly rounded, glass material
- Two sections (pinned | recent/suggested) with a subtle divider
- Optional search pill on the right
- Adjustable icon count and position (bottom or side)

## 5. Home screen placement
- Support both Grid (with half-cell) and Free canvas with snap guides
- Density slider must actually change columns/rows/icon size/widget content density live
- Long-press edit mode must feel fluid (drag, multi-select, alignment guides, undo)

## 6. First-run & everyday feel
- After the intro animation, the first home screen the user sees must already look like one of the reference images (apply the Glass or Productive template by default).
- Quick panel (pull down or long-press) must let the user switch Look + Density instantly with live preview.

## 7. Verification
After this upgrade, the home screen must visually match the spirit of the reference images:
- Soft glass blur and depth
- Large beautiful widgets
- Floating dock
- Dense or calm layouts depending on Density
- No more “plain icon grid only”

Update docs/COVERAGE.md. (Builder note: no device/emulator exists in the build sandbox, so real screenshots come from the user's tablet via DEVICE_CHECKLIST.md — never fabricate screenshots.)
