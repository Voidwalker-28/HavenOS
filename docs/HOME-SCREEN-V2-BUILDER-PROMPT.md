# HavenOS — Home Screen Redesign (v2) — Builder Prompt

Paste everything below into the builder, in the existing HavenOS project.
Companion to the Settings v2 prompt: that one governs Settings; this one governs Home, Dock, Library (drawer), Search, Folders, Edit mode, Today and Calm. Both share the same design tokens and settings keys.

---

## 0. Role, scope, rules of engagement

You are a senior Android engineer and product designer. **Fully redesign the HavenOS home experience** (`app.havenos.launcher`): home pages, dock, glance, folders, widgets and Today, app Library (drawer), search, edit mode, gestures and motion. The goal is a *universal best-of-all-worlds* launcher: the best ideas from every major launcher UI combined into one coherent, original design, tablet-first (primary test device: **Redmi Pad 2**, landscape) and fully working on phone and tablet portrait.

Rules:
- **Inspect first.** Read the project structure, README, build files, UI toolkit, navigation, the settings store, the Home host, the layout/database model for icons, folders and widgets, the theme/material system, icon-pack handling, wallpaper, gesture dispatch, permissions and the first-run flow. Keep the existing stack and architecture.
- **Do not wipe user data.** Existing placed icons, folders, widgets, pages, hidden apps and settings must survive. If the data model changes, write and test a migration. Do not re-flow an existing layout because defaults changed; new defaults apply to fresh installs or when the user opts in.
- Keep: first-run flow (intro animation → logo → permissions → tutorial), system wallpaper **and** HavenOS wallpaper, the five style presets (Premium, Minimal, Futuristic, Classy, Retro), Settings v2 hubs, and the existing settings keys. Add new keys with defaults.
- Check installed dependencies before adding any; prefer none. Use the project's icon set or one well-maintained library; no improvised icon paths.
- **Clean-room:** take interaction patterns only. Do not copy code, branding, icons, wallpapers or assets from iOS, One UI, HyperOS, Pixel, Nova, Niagara, Smart Launcher, Action Launcher, Nothing OS or any other product. Names of products are used here only to describe patterns. The "gems" metaphor in this document is for design thinking only; **never use it in app strings, class names or UI**.
- No secrets in source. No new network calls beyond the existing weather feature. No analytics or telemetry. Usage-based ranking (suggestions, recents) is **local only**.
- Do **not** publish, release, push, change signing, package name or version code, or touch production data.
- Proceed on reasonable assumptions and state them in one line. Ask only if a choice would materially change the product, permissions or data.
- Report only checks you actually ran. Never claim something works if you did not run it.

---

## 1. Defects seen in the current home (from a screen recording, tablet landscape) — fix all of them

1. **Labels are unreadable on a busy wallpaper.** Icon names sit on top of wallpaper artwork and get lost.
2. **Dock is inconsistent.** On the grid page it is a light frosted pill with 9 icons; on the Favorites, drawer and Today pages it is a dark pill with 12 icons plus a "Q" search button. The last icon in the light pill (ChatGPT, white on light) is nearly invisible. The dock must be one component with one look and one content set on every page.
3. **Tablet space is wasted.** The Favorites list and A–Z list hug the left edge as a single narrow column; the Today page is a single column of stacked boxes that clips at the bottom. On a wide screen the centre is empty.
4. **Search overlay renders overlapping rows.** In the Suggested list, rows draw on top of the list behind them (Settings and Clock overlap other app rows and text). The keyboard also covers the results.
5. **A stuck hint** ("Long-press an app for options") sits on top of the dock.
6. **A date line floats above the dock** on some pages and not others, with no clear role.
7. **Today page** uses outlined boxes and does not adapt to the screen; the analog clock card is an island.
8. **A–Z scrubber letters are tiny** (hard to hit) and the section headers are dark bars that feel heavy.
9. **No visual identity.** Flat dark boxes, no hierarchy, no depth or material, no personality.

---

## 2. Concept — "The Gauntlet": six gems in one home

One home, six design pillars. Each pillar takes the best of several UIs:

| Gem | Pillar | Takes the best of |
|---|---|---|
| **Space** | Adaptive layout that uses the whole screen | iPadOS (widgets beside icons), One UI (comfortable density), Pixel Tablet (persistent taskbar-style dock) |
| **Mind** | Local intelligence: suggestions, categories, search that acts | Smart Launcher (auto-categories, ambient), iOS (App Library, Spotlight, Smart Stack), Pixel (At a Glance) |
| **Reality** | Look and material, always legible | iOS (glass, tint modes), Material You (themed icons, dynamic accent), Nothing OS (mono/retro character), HyperOS (card consistency) |
| **Power** | Depth of customization | Nova (grid, gestures, icon actions, backup), Action Launcher (peek actions on icons), One UI (per-page layout, folder styles) |
| **Time** | Motion and gestures that feel physical | iOS/Android predictive open and close, One UI swipe-anywhere drawer, macOS dock hover |
| **Soul** | Calm, focus, minimalism | Niagara (list-first home, alphabet scrubber with bubble, thumb-zone favorites) |

**The universal idea:** *one pager, three ways to see your apps.* Grid pages (iOS/One UI), a Library with All · Categories · List views (iOS/Smart/Niagara), and a **Calm mode** (Niagara-style focus list) that can replace the home on demand. The user never has to pick a different launcher to get a different feel.

### 2.1 What every launcher misses (HavenOS answers)

| Gap | HavenOS answer |
|---|---|
| Tablet landscape gets a stretched phone layout | Today column beside a centred grid; size-class layouts |
| Labels vanish on busy wallpapers | Per-cell legibility engine (auto colour, shadow, pill) |
| Rearranging needs dragging | TalkBack actions, keyboard moves, and a page overview |
| Dock changes meaning between screens | One persistent dock, identical on every page |
| Drawer is one fixed style | Library with three live views, same data |
| Focus mode is a bolt-on or a separate launcher | Calm mode built in, entered in one gesture |
| Search only finds apps | Search with inline settings, actions and calculator |

⚠️ These are patterns from general knowledge of those products. They change over time; do not reproduce any specific implementation.

---

## 3. Design system for Home

### 3.1 Window size classes (use Android window size classes, not device checks)
- **Compact** (< 600dp wide): phone portrait.
- **Medium** (600–840dp): tablet portrait, large phone landscape.
- **Expanded** (≥ 840dp): tablet landscape (the Redmi Pad 2 case).
- Layout recalculates on rotation, split-screen and resizing. Nothing may be clipped in any class.

### 3.2 Tokens (shared with Settings v2; extend, don't fork)
- **Spacing scale:** 4 / 8 / 12 / 16 / 24 / 32 / 48dp.
- **Radii:** icon shape per preset; dock = full pill; cards, folders and panels = 28dp; chips = 12dp.
- **Type:** Glance 16sp medium · Today title 20sp semibold · Label 12sp (Compact/Medium) or 13sp (Expanded) · Section header 12sp caps, muted · Search field 17sp · Calm list 28–34sp light.
- **Semantic colours:** wallpaperScrim, surfaceGlass, surfaceSolid, textOnWallpaper (auto), textPrimary, textSecondary, accent, badge, focusRing, danger. Light and dark variants, each passing contrast.
- **Material:** the user's chosen material (solid / translucent / frosted) from Look, applied consistently to dock, folders, Library and Search panels. Capability and frame-time guard with automatic fallback to translucent.

### 3.3 Preset overrides (home tokens; tune so each is clearly distinct and passes contrast)

| Token | Premium | Minimal | Futuristic | Classy | Retro |
|---|---|---|---|---|---|
| Icon shape | squircle | circle | rounded square, thin rim | rounded square | rounded square, chunky |
| Icon background | normalized, soft shadow | normalized, flat | dark glass tile | matte tile | flat colour tile |
| Label | 1 line, medium | 1 line, light | mono-ish, tracked | serif-leaning, small caps optional | chunky, round |
| Dock | frosted pill, soft shadow | solid or hairline pill | translucent, faint glow edge | matte pill | solid, chunky, bouncy |
| Spacing density | comfortable | airy | compact | comfortable | cosy |
| Accent | warm metallic | single mono accent | cool neon | muted jewel | warm retro |
| Motion | slow smooth springs | fast, no overshoot | snappy | calm | bouncy |

---

## 4. Layout blueprint

### 4.1 Expanded (tablet landscape) — the primary design

```
┌─────────────────────────────────────────────────────────────────┐
│ status bar (system)                                              │
│  ┌──────────────┐    ┌───────────────────────────────────────┐  │
│  │ TODAY STACK  │    │ [icon] [icon] [icon] [icon]            │  │
│  │ clock+date   │    │ [icon] [icon] [folder] [icon]          │  │
│  │ weather      │    │ [icon] [large folder 2×2] [icon]       │  │
│  │ next event   │    │ [icon] [icon] [icon] [icon]            │  │
│  │ suggested    │    └───────────────────────────────────────┘  │
│  └──────────────┘                  ● ○ ○                         │
│      ╭────  dock: pinned │ recents │ 🔍  ▦  ────╮                 │
└─────────────────────────────────────────────────────────────────┘
```
- **Page 1:** Today column (left, ~34% of width, clamp 280–420dp) + icon grid (right). Today is optional; turning it off centres the grid.
- **Page 2+:** full-width grid, centred, content max-width ~880dp so icons don't scatter across the screen; the remaining space becomes breathing room (side margin ≥ 5%).
- **Grid (computed, not fixed):** columns = floor(contentWidth / cellWidth), rows = floor(contentHeight / cellHeight), clamped to the supported min/max. Cell width ≈ icon + 56dp. Defaults: Today visible → about 4 × 4; Today off → about 6 × 4. User can switch density Comfortable / Dense / Compact.
- **Glance row** (top-left, 48dp high, only when Today is not visible): "Fri, 23 Oct · 28° Clear" plus one event chip if calendar access is granted. Tap opens the clock or calendar app.
- **Page indicator:** centred, 12dp above the dock, 8dp dots, current dot elongated.
- **Dock zone:** bottom inset + 16dp, height 72–80dp.

### 4.2 Medium (tablet portrait) and Compact (phone)
- Pager: **Today** (leftmost page) · Home 1…n.
- Grid 5 × 6 (Compact) / 6 × 6 (Medium), Comfortable density by default.
- Glance row at top; **search pill** (48dp high, 70–90% width) above the dock on Compact.
- Dock: 4–5 pinned on Compact (no recents); Library via swipe-up; Calm via long-press on the Glance clock or a gesture.
- Thumb-zone: frequently used actions (search, dock, Calm exit) sit in the lower half.

---

## 5. Components

### 5.1 Icons
- Shape follows the preset and the Look setting. **Normalization:** non-adaptive icons are scaled and placed on a derived backdrop so they never appear as a stray white square inside a squircle.
- Optional themed monochrome and tint modes (Light / Dark / Tinted) where the icon provides a monochrome layer; otherwise fall back gracefully.
- Press state: scale to 0.94 in 80ms, release with a spring. Icon pack (if selected) applies everywhere: grid, dock, folders, Library, search.
- **Badges:** small dot or count, top-right; needs notification access; if denied, no badges and no empty space reserved.
- Minimum tap target 48dp even when the icon is smaller.

### 5.2 Labels — the legibility engine (fixes defect 1)
- Label modes: **Auto** (default), **Pill**, **Off**.
- On wallpaper change, downsample the wallpaper (e.g. to ~32px wide) and cache mean luminance and variance per grid cell and page.
- **Auto:** choose white or near-black text from cell luminance; add a soft shadow; if variance is high or contrast would still be < 4.5:1, draw a small frosted pill behind the label.
- Recompute on wallpaper, scroll-parallax, dim or mode change. Never run this on the main thread during scrolling.
- Max 1 line, ellipsized; Expanded may use 2 lines if the user chooses.

### 5.3 Dock — one component, everywhere (fixes defect 2)
- Floating glass pill, centred, full pill radius, material from Look. Identical on every page, in the Library, in Search and in edit mode.
- Contents, in order: **pinned apps** | divider | **recents** (up to 3, from local launch history, can be turned off) | **Search** button | **Library** button.
- Slot count is computed from width (about 72dp per slot incl. gap); if space is short, drop recents first, then reduce pinned, never shrink icons below 48dp.
- Pointer support: hover tooltip with app name, subtle magnify (1.12) on hover, right-click opens the icon menu.
- Dim/disabled icon states are never used for ordinary apps. Icon backdrop contrast is verified against the dock material in light and dark (no white-on-light).
- Dock is editable by drag in edit mode; it also accepts folders.

### 5.4 Page indicator and pager
- Pager with subtle wallpaper parallax (≤ 8% of page width) and a scroll effect option.
- Pages can be added, removed, reordered and hidden from the page overview (see §8.3). Empty pages are removable.

### 5.5 Folders
- **Closed:** mini 3 × 3 live preview of the first 9 icons on a glass tile.
- **Open:** a floating glass panel scaled from the folder's position, centred, 4 × 3 grid, editable title, internal pages with dots when > 12 apps, background scrim. Drag an icon out to move it onto the home.
- **Large folder (P1):** a 2 × 2 cell folder that shows 4 icons directly; tapping an icon launches it, tapping elsewhere opens the folder.
- Styles (grid / stack / fan) only if each is actually implemented and visible; otherwise do not expose them.

### 5.6 Widgets, Today column and Smart Stack
- Built-in cards share one frame (radius = 28dp or the preset's value, material from Look): **Clock + weather**, **Calendar next events** (calendar permission), **Suggested apps** (local), **Battery**; **Media** (P2; needs notification access).
- The Today column (Expanded) and Today page (Compact/Medium) show the same cards in a **Smart Stack**: swipe vertically to cycle, small dots on the right edge, optional time-based auto-rotation (rules: morning → weather and calendar; evening → media and suggested), paused for 10 s after any touch.
- Third-party widgets: host as today. Resize snaps to grid cells. A widget gallery opens from edit mode as a bottom sheet with live previews.
- Weather uses the existing feed; location permission is optional and explained before the request. Offline: show last known value with "Updated 14:05".
- No outlined stacked boxes; no clipping at any size class (fixes defect 7).

### 5.7 Glance
- One line, 16sp, on wallpaper with the legibility engine. Content: date, weather, next event. Tap → relevant app. Long-press → enter Calm mode (Compact/Medium) or open Glance options.

---

## 6. Library (app drawer)

Open: swipe up anywhere on empty home or on the dock, or the Library button. Full-screen overlay on a blurred and dimmed wallpaper (blur and dim from Settings; consistent scrim so text stays ≥ 4.5:1).

**Top (Expanded):** centred search field (56dp, full pill, width ~560dp), below it a segmented control **All · Categories · List**, a sort chip, and a **Suggested** row of 6–8 icons (local launch history; hidden if empty).

**All:** icon grid (Expanded ~7 columns), section headers by letter shown as small floating labels, not dark bars. Scrubber on the right.

**Categories:** auto-grouped cards in a grid (Expanded 3 × 2, Compact 2 × 2); each card is a glass tile with 4 mini icons and the category name. Tap opens a popover with all apps in that category. Categories come from the system app category plus a keyword rule table; user can rename a category and move apps between categories (long-press → Move to…). Local only.

**List (Niagara-style):** on Expanded use **two columns** with sticky letter headers; on Compact one column. Large rows (56dp), 16sp.

**Scrubber (fixes defect 8):** vertical strip ≥ 40dp wide, A–Z and #, current letter bubble (large, ~72dp) appears near the finger, haptic tick per letter, and works with TalkBack as a slider.

**Bottom:** if hidden apps are enabled, a "Hidden" chip leads to the locked list (biometric).

**Behaviour:** drag an app from the Library onto the home or dock; recently installed apps carry a subtle "New" dot until opened; the Library remembers the last view per user setting.

---

## 7. Search (fixes defect 4)

- Open: swipe down on empty home, the dock Search button, or the Library field.
- **Expanded:** floating glass card, centred near the top, ~640dp wide, so the keyboard never covers the results. **Compact:** full-width sheet that resizes with the keyboard insets.
- Zero state: Suggested apps + recent searches (local, clearable).
- Sources (each with a toggle, permission-gated where needed): apps, contacts, HavenOS settings, calculator ("12*8" evaluates inline), shortcuts, web fallback with the user's engine.
- Results render in **one list with grouped sections** (Apps, Settings, Actions, Contacts, Web). No overlapping rows, no duplicate lists, one scroll container.
- **HavenOS settings results are live:** typing "bigger icons" shows the icon-size slider in the result and it works in place.
- Ranking: prefix match > word match > fuzzy, boosted by local launch count and recency. Enter launches the top result. Tests cover ranking.
- Closing Search clears focus and hides the keyboard.

---

## 8. Interactions

### 8.1 Long-press icon menu (replaces the stuck hint, fixes defect 5)
- Popover anchored to the icon with the app's shortcuts (max 4) and: **Edit · Remove · Widgets (if any) · App info · Hide · Uninstall** (user apps only). P2: **Open in split view**.
- A one-time hint is shown in the tutorial only; never permanent text near the dock.

### 8.2 Edit mode
- Enter: long-press empty space, or from the menu. Top centre toolbar: **+ Widgets · Wallpaper · Look · Settings · Done**. Icons may wiggle gently (setting; off by default; off under reduced motion).
- Drag icons with snap-to-grid; while dragging, two drop zones appear at the top: **Remove** (left) and **App info / Uninstall** (right).
- Dragging near the left/right edge for 600ms turns the page; hovering over a folder for 400ms opens it; dropping on an icon creates a folder.
- Undo snackbar for remove and move actions (5 s).

### 8.3 Page overview
- Pinch in on home, or the page indicator long-press, or a toolbar button. Shows page thumbnails in a grid; drag icons between pages, reorder pages, add or delete a page, hide a page, set the default page.

### 8.4 Gesture defaults (only wired actions are offered)
| Gesture | Default |
|---|---|
| Swipe up on empty home / dock | Library |
| Swipe down on empty home | Search |
| Two-finger swipe down | Notifications |
| Pinch in | Page overview |
| Long-press empty | Edit mode |
| Icon swipe-up (P1) | Icon shortcuts menu |
| Long-press Glance (Compact/Medium) | Calm mode |
| Double-tap | None (lock-screen needs a sensitive permission; opt-in only) |
All remappable in Settings → Gestures & Motion.

### 8.5 Calm mode (Soul)
- Entry: dock long-press on Library, gesture, Glance long-press, edit toolbar, or a schedule (user-defined days and times).
- Content: wallpaper dimmed and blurred; thin large clock top-left; **Favorites as a large text list** (up to 8, 28–34sp) anchored in the thumb zone (bottom-left on Compact; centred column on Expanded); "All apps" row opens the List view; no dock, no widgets.
- Exit: tap "Exit Calm" at the top or swipe down; the home restores exactly as it was.
- Favorites reuse the existing Favorites data; if it is empty, show a prompt to choose apps.

---

## 9. Motion and haptics

- **App open:** 320ms spring, window expands from the icon's bounds with a scrim; return animates back to the icon.
- **Library:** 360ms spring translate + blur ramp (0 → configured blur); drag-down to close follows the finger.
- **Folder:** 280ms scale from the icon; **page scroll:** standard fling + parallax; **press:** scale 0.94.
- **Calm:** 400ms cross-fade, home items fade and drop 12dp.
- Animate transform and opacity only; no layout animation per frame; no "animate all".
- Haptics: tick at page snap, scrubber letters and edit-mode grid snaps; confirm pulse on long-press recognition. Strength follows Settings.
- **Reduced motion:** 100ms fades only, no parallax, no wiggle, no springs.
- No animation may block input.

---

## 10. Accessibility and input

- **TalkBack:** every icon has label + role; custom actions **Move left / right / up / down, Move to page, Add to dock, Remove, Edit label, Hide**, so nothing requires dragging. Pager, scrubber and Smart Stack announce position ("Page 2 of 4").
- **Keyboard / D-pad (tablet with keyboard):** arrow keys move focus across icons, Enter opens, Menu key or Shift+F10 opens the icon menu, Alt+arrows move an icon in edit mode, Esc closes Library, Search, folders and Calm. Visible focus ring.
- **Mouse:** hover tooltips and highlights, right-click context menu, scroll wheel changes pages and scrolls Library.
- Touch targets ≥ 48dp; large font scale up to 200% without clipping; no colour-only state; contrast: text 4.5:1, controls and focus 3:1; RTL-safe.

---

## 11. Wallpaper and contrast

- System wallpaper and HavenOS wallpaper both supported; adaptive scrim and dim keep text legible.
- Parallax subtle and optional. Per-page wallpaper only if already supported.
- Calm mode may use its own dim level.
- Wallpaper sampling for the legibility engine uses a downscaled copy and is cached per wallpaper and size.

---

## 12. Data, migration and performance

- Reuse the existing layout store (icons, folders, widgets, pages, dock). Add only what is needed: Today column on/off, label mode, dock recents on/off, Library default view, category overrides, Calm favorites/schedule, large-folder spans.
- Migration: version the layout schema; old data keeps positions; if the grid default differs, keep the user's grid and offer "Use new default" in Settings.
- Backup/restore: include the new keys; old backups still import.
- Performance: 60fps on a mid-range tablet while scrolling pages and dragging; previews and Settings changes must not rebuild Home; wallpaper analysis and category building run off the main thread; icon loading is cached; no blur on low-end without a fallback.
- Robustness: survive rotation, split-screen, process death and wallpaper change without losing layout; no duplicated lists, no overlapping rows, no stuck overlays.
- Verify layout at: Redmi Pad 2 landscape, tablet portrait, phone 390 × 844, split-screen, 200% font scale.

---

## 13. Priorities and build order

**P0 (must ship, vertical slice first):**
1. Audit current Home and fix defects §1: single dock component, label legibility engine, search overlap and keyboard insets, stuck hint removed, date-line cleanup.
2. Size-class layout engine and tokens; Glance; grid; page indicator; dock with pinned + recents + Search + Library.
3. Library with **All** and **List** (two columns on Expanded) + proper scrubber; Search card with grouped results and live settings results.
4. Folders (closed preview + open panel); long-press icon menu; edit mode with remove/uninstall drop zones; page overview.
5. Presets applied to home tokens; motion and haptics; accessibility actions and keyboard/mouse basics.

**P1:**
6. Today column / Smart Stack (Expanded) and Today page (Compact/Medium), consistent frame, no clipping.
7. Library **Categories** view; large folders; icon swipe-up shortcuts; **Calm mode**.

**P2 (only if everything above is finished and verified):**
8. Themed monochrome / tint modes, split-view and app pairs, media card, per-view dock tweaks.

If time or budget runs short, finish P0 completely rather than doing everything partially, and list exactly what remains.

---

## 14. Acceptance checklist

- [ ] Dock is one component with identical look and content on every page, Library, Search and edit mode; no icon is unreadable on it in light or dark
- [ ] Labels are legible on a busy wallpaper (Auto mode verified on at least 3 wallpapers); unit test covers the contrast routine
- [ ] Expanded layout uses the whole screen: Today column + centred grid; no left-hugging single-column lists; no clipped cards
- [ ] Library: All and List work (two columns on Expanded), scrubber ≥ 40dp with bubble and haptics
- [ ] Search: one scroll container, no overlapping rows, keyboard never covers results, live settings results work
- [ ] No stuck hint text anywhere; hint appears only in the tutorial
- [ ] Folders open as a floating panel; drag in/out works; mini 3 × 3 preview closed
- [ ] Edit mode: Remove and Uninstall drop zones, page-turn on edge drag, undo snackbar
- [ ] Page overview: reorder, add, delete, hide, move icons between pages
- [ ] TalkBack custom actions replace dragging; keyboard and mouse flows work on tablet
- [ ] Presets visibly change icon shape, dock, labels and motion; reduced motion verified
- [ ] Existing layouts, folders, widgets, hidden apps and settings preserved after update (migration tested)
- [ ] Light and dark both verified; Redmi Pad 2 landscape, tablet portrait, phone and split-screen verified with no horizontal overflow
- [ ] P1 items either completed and verified, or explicitly listed as not done

---

## 15. Checks and final report

Run the project's available checks (build, lint, type check, unit tests). Add tests for: layout migration, grid computation per size class, label contrast routine, search ranking, category assignment, dock slot calculation. If an emulator or device is available, install and exercise the flows; capture screenshots only if the environment supports it.

Finish with:
1. **Changed:** screens and files, short bullets.
2. **Defects fixed** from §1, each marked fixed / partly / not fixed.
3. **Checks actually run** and results; pre-existing failures separated from new ones.
4. **Not verified** (for example: behaviour on real devices, blur performance, TalkBack, notification-access features).
5. **Known gaps** (P1/P2 items not done) and the single most useful next improvement.

Do not describe the result as production-ready, accessibility-conformant or store-approved unless that was actually verified.
