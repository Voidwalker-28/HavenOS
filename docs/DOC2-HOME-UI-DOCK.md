# HavenOS — Doc 2 of 3 — Home Screen UI and the Dock

Paste into the builder with Docs 1 and 3. This doc defines **how the home looks and where everything sits**, including the dock. Behavior of edit mode and settings is in Doc 1; features, widgets and coordination are in Doc 3.

**The Settings UI is approved and must not change.** The home must adopt the same quality, the same tonal look and the same accent logic.

---

## 0. Rules

- Inspect first; keep what works; restart nothing. Preserve user data and settings keys.
- Primary test device: **Redmi Pad 2, landscape**. Also verify tablet portrait and phone portrait.
- Use Android window size classes: **Compact** (< 600dp), **Medium** (600–840dp), **Expanded** (≥ 840dp).
- Clean-room: no copied code or assets from other launchers or OS skins. No network beyond weather. No analytics. No publishing or release steps.
- Report only checks you actually ran.

---

## 1. Where is the dock?

**What the captures show:** the dock is **not visible** on the Today page, on the grid page, or in edit mode. An earlier recording showed it as a pill at the bottom, and Settings has **Dock position: Bottom / Left / Right**, so it is meant to exist. From the videos alone I cannot tell whether it is hidden by those modes or was lost in the rebuild. Treat it as a defect either way.

**The dock rules (non-negotiable):**
1. **One component, always visible** on every Home page, the Today page, edit mode and while Settings is open. It is hidden only in Calm mode and dimmed (not removed) under the full-screen Library.
2. **Default: Bottom floating pill**, centred, with 16dp clear above the gesture-navigation inset.
3. **Left or Right** (from Settings): a vertical pill centred on that edge, max 80% of screen height, same material and contents.
4. **Nothing may overlap it.** The grid reserves a matching inset. The page indicator sits just above a bottom dock, or at the bottom centre of the content area when the dock is on a side.
5. **Contents, in order:** pinned apps · divider · up to 3 recents (optional, from local launch history) · Search button · Library button.
6. **Capacity is computed**, not fixed: about 72dp per slot on a 64dp icon; if space is short, drop recents first, then pinned apps beyond the limit are kept but scroll (a short horizontal or vertical scroll inside the pill), never shrinking icons below 48dp.
7. **Behaviors:** tap launches; long-press opens the icon menu; drag to reorder; drag out to move to home; accepts folders; pointer hover shows a tooltip and a subtle magnify (1.12); right-click opens the menu.
8. **Same look on every page.** Material comes from the user's Look setting. No page may use a different pill style or content.
9. **Contrast:** icon backdrops are checked against the dock material in light and dark (no white-on-light icons like the ChatGPT icon in the earlier recording). Never dim an icon unless it is disabled by the system.
10. **Edit mode:** dock stays visible and is a drop target (Doc 1 §3.8).

| Size class | Bottom dock | Side dock |
|---|---|---|
| Expanded | height 76dp, up to ~10 slots, centred, max width ~720dp | 80dp wide, up to 8 slots |
| Medium | height 72dp, up to ~8 slots | 76dp wide, up to 7 slots |
| Compact | height 64dp, 4–5 pinned, no recents | 64dp wide, up to 5 slots |

---

## 2. Defects to fix (from your latest captures)

1. **Gold outline on every grid cell**, even where no edit toolbar is shown. Remove. Outlines appear only for the item being dragged, the drop target and the selected item.
2. **Every cell is filled with a near-black rectangle** behind the icon. Icons should float on the wallpaper (or sit on a glass tile only if the preset says so).
3. **Labels are nearly invisible** (dark text on dark cells). Use the legibility engine (§5.2).
4. **Widgets are blank** dark rectangles with only a name ("Weather", "Agenda"). They must show real content or a designed placeholder (§6). Two identical Agenda widgets are seeded; seed one.
5. **Edit toolbar and helper text overlap the first row of icons.** The toolbar must reserve its own space (Doc 1 §3.10).
6. **The grid is cut off by the screen edge** (the bottom row is clipped) and the page indicator overlaps a widget. The grid must fit the safe content area for every size class.
7. **Today page is one card on an almost empty screen**, with the city truncated ("Delhi, National Capital Territ…"). Use the Today layout in §4.3 and show the locality only.
8. **Weather unit is inconsistent** between recordings (28° in one, 75° in another). Follow the Settings unit and locale consistently.
9. **Gold is overused**: outlines, handles, toolbar text and indicators all use it. Reserve the accent for interactive and selected states; use tonal neutrals for everything else.
10. **Edit-mode handles on every icon** add noise. Handles belong only to the selected item.

---

## 3. Visual language (aligned with the approved Settings)

- **Surfaces:** wallpaper → scrim → glass or tonal surfaces. No hairline outlines around groups. Radii: dock = full pill, folders and widget frames 28dp (or the preset's value), chips 12dp.
- **Accent:** one accent (default warm gold in Minimal) used for: selected state, focus ring, active page dot, primary action. Never for large outlines or body text.
- **Type:** Glance 16sp medium · Today title 20sp semibold · Label 12sp (Compact/Medium) and 13sp (Expanded) · Section header 12sp caps muted.
- **Spacing scale:** 4 / 8 / 12 / 16 / 24 / 32 / 48dp. Content margins ≥ 5% of width on Expanded.
- **Icons:** shape from the preset; non-adaptive icons are normalized onto a derived backdrop; optional themed monochrome.
- **Light and dark** must both look intentional.

---

## 4. Layouts

### 4.1 Expanded, bottom dock (primary design)

```
┌────────────────────────────────────────────────────────────────┐
│ status bar                                                      │
│ ┌──────────────────────────────────────────────────────────┐   │
│ │ [icon] [icon] [icon] [icon] [icon] [icon] [icon] [icon]   │   │
│ │ [icon] [icon] [icon] [folder][folder][folder][folder]     │   │
│ │ [icon] [ Weather 3×2 ][ Agenda 4×2            ]           │   │
│ │ [icon] [ widget      ][ widget                ]           │   │
│ └──────────────────────────────────────────────────────────┘   │
│                           ● ○ ○                                 │
│        ╭────  pinned │ recents │ 🔍  ▦  ────╮                   │
└────────────────────────────────────────────────────────────────┘
```
- Grid fits the content area between the status-bar inset and the dock (plus indicator). Computed columns/rows from icon size and density; never clipped.
- Content is centred with max width ~880dp when the grid is sparse; widgets and icons keep one consistent gutter.

### 4.2 Expanded, side dock (Left or Right)
```
┌────────────────────────────────────────────────────────────────┐
│ ╭──╮ ┌────────────────────────────────────────────────────┐   │
│ │▢ │ │ grid (content area reduced by the dock inset)       │   │
│ │▢ │ │                                                     │   │
│ │▢ │ └────────────────────────────────────────────────────┘   │
│ │🔍│                     ● ○ ○                                  │
│ │▦ │                                                           │
│ ╰──╯                                                           │
└────────────────────────────────────────────────────────────────┘
```

### 4.3 Today page (card flow, not a cell grid)
- **Expanded landscape:** a 2-column flow of cards, the left column ~38% width (clamp 320–460dp), the right column fills the rest, with a 24dp gutter. Cards: Clock + date + weather (large), Agenda, Suggested apps, Battery. The dock remains visible.
- **Medium/Compact:** a single scrollable column of the same cards.
- Header "Today" as a title (keep the current style). The weather line shows the locality ("Delhi · 24°C · Clear"), not the full administrative name; if space is short it ellipsizes at the end of the locality, never mid-word.
- The stack indicator (gold dot + grey dots) stays, but is vertically centred on the card edge with a 48dp touch area.

### 4.4 Edit mode
- Toolbar in its **own reserved row** at the top (56dp) with a soft scrim; the grid is pushed down or scaled to fit (do not overlap).
- Items are lifted slightly (+2dp shadow), **no outlines**.
- Selected item: 2dp accent ring, small handles at corners, a floating context bar above or below it (never covering the item).
- While dragging: small plus-marks at cell corners (grid dots) at 30% opacity, a ghost at the landing spot, top drop zones **Remove** and **App info / Uninstall**, dock slot highlight.
- Hint chip (dismissible, shown up to 3 times) at the bottom of the toolbar row, not over icons.

### 4.5 Settings panel over home
- Keep as built: the home re-lays out in the free area, "Before" button available. The dock remains visible and respects the free area (it moves with the home, not under the panel).

---

## 5. Component specs

### 5.1 Icons and cells
- Icon default 56dp on Expanded (Auto from density), 48–52dp Medium, 48dp Compact. Touch target at least 48dp.
- Cell has no background by default. Press: scale 0.94 in 80ms, spring back.
- Badges (dot/count) top-right, only when notification access is granted; no reserved gap otherwise.

### 5.2 Label legibility engine
- Modes: **Auto** (default), Pill, Off (the existing Labels setting On/Off still applies).
- On wallpaper or size change, downsample the wallpaper (≈32px wide) and cache mean luminance and variance per cell.
- Auto: choose white or near-black text from luminance; add a soft shadow; if contrast is still below 4.5:1 or variance is high, add a small frosted pill behind the label.
- Runs off the main thread; never recomputes during scroll. Unit-test the contrast routine.
- Labels: 1 line, ellipsized.

### 5.3 Folders
- Closed: mini 3×3 live preview on a glass tile; label legible.
- Open: floating glass panel scaled from the folder, centred, 4×3 grid, editable title, internal pages with dots beyond 12 apps, drag-out to home.
- Large folder (2×2): shows 4 icons directly (tap icon launches, tap background opens).

### 5.4 Page indicator
- 8dp dots, current dot elongated, accent colour; 12dp above the dock (or bottom-centre of the content area for a side dock). Never overlaps a widget or icon.
- Special glyph for the Today page (a small sun) so users know where they are.

### 5.5 Glance row
- Shown on Home pages when the Today column is not visible: "Fri, 23 Oct · 28° Clear" plus one event chip with calendar access. Legible via the engine. Tap opens clock or calendar.

### 5.6 Wallpaper and scrim
- Adaptive scrim keeps text at least 4.5:1. Optional parallax ≤ 8%. Heavy blur is for Library and Search only; the home wallpaper stays crisp.

---

## 6. Widget visuals (behavior is in Doc 3)

- **One frame style for all built-in widgets:** radius 28dp (preset may change), material from Look, 16dp padding, title row optional.
- **Never blank.** Every widget has Loading (skeleton), Ready, Empty, Needs-permission (inline chip), Error (Retry) and Offline (cached + "Updated 20:34") states.
- **Weather**
  - 2×2: icon, temperature, condition.
  - 4×2: locality, temperature, condition, hi/lo, hourly (4 slots).
  - 4×3 and larger: add 5-day strip.
- **Agenda**
  - 2×2: next event (title, time).
  - 4×2: next 3 events.
  - 4×4: day view with time axis.
  - Empty: "No events today" + "Add event" button.
- **Clock:** digital or analog, date, optional second time zone.
- **Suggested apps:** 4–8 icons from local history.
- In edit mode widgets keep rendering **live content** under the selection ring; they are not replaced by name labels.

---

## 7. Motion and haptics

- Page scroll with parallax ≤ 8%; app open 320ms spring from the icon; folder open 280ms; dock position change 240ms; panel/overlay springs; press 0.94 scale.
- Animate transform and opacity only.
- Haptics: pickup, snap, drop, page-turn, long-press recognition.
- **Reduced motion:** 100ms fades only, no parallax, no wiggle, no springs.

---

## 8. Accessibility

- Every icon has label, role and state; pager announces "Page 2 of 4".
- Custom TalkBack actions replace dragging (Doc 1 §3.11).
- Touch targets ≥ 48dp; contrast text 4.5:1 and controls 3:1; font scale to 200% without clipping; RTL-safe.
- Keyboard and mouse: arrow-key focus on tablet, Enter opens, hover tooltips, right-click menu, scroll wheel changes pages.

---

## 9. Acceptance checklist

- [ ] Dock visible on every page, Today, edit mode and with Settings open; same look and contents everywhere; Bottom/Left/Right all work with no overlap
- [ ] No cell outlines at rest; outlines only for selected, dragged and drop-target items
- [ ] Icons float without black cell rectangles; labels legible on the fish wallpaper and at least two other wallpapers
- [ ] Toolbar in its own row; grid fully visible (no clipped bottom row or right column)
- [ ] Page indicator never overlaps content
- [ ] Today page uses the card flow and fills the screen; locality shown without truncation mid-word; unit consistent with Settings
- [ ] Widgets never blank; all states implemented; no duplicate seeded widgets
- [ ] Accent used only for interactive and selected states
- [ ] Light and dark both verified; reduced motion verified
- [ ] Redmi Pad 2 landscape, tablet portrait, phone portrait, split-screen verified with no horizontal overflow
- [ ] Settings UI visually unchanged

---

## 10. Checks and final report

Run the project's available checks (build, lint, type check, unit tests). Add tests for dock capacity, grid fit per size class and the label contrast routine. If an emulator or device is available, install and exercise the screens; capture screenshots only if the environment supports it.

Final report: what changed; each §2 defect marked fixed / partly / not fixed; checks actually run and results; what is not verified; known gaps; and the single most useful next improvement. Do not claim production readiness without verification.
