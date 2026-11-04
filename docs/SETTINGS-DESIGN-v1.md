# HavenOS — Settings Design & Workflow

Status: DRAFT design spec. Nothing built or tested. Structure also fits MIST28 / Nexus.
Scope: which settings exist, how they are organized, how the user reaches them, and how each one previews.

---

## 1. Preview tiers (tag on every setting)

| Tag | Tier | What the user sees |
|---|---|---|
| [L] | Live | Setting applies to the real home screen behind a half-height settings sheet; result visible while dragging |
| [M] | Mini | Miniature home/drawer rendered from a *draft* copy of settings; tap to preview, **Apply** to commit |
| [A] | Demo | Play button runs a looped animation sample |
| [T] | Test pad | Interactive area to try it (gesture pad, sample badge, sample search query) |
| [–] | None | No preview; row shows a result line ("12 apps hidden") + Undo snackbar |

Rule: every value is also shown as text/number. A preview is never the only feedback.

---

## 2. Workflow

### 2.1 Entry points (each opens the relevant category as a half-sheet over the live home)
- Long-press empty home → bar: Wallpaper · Widgets · Style · Settings
- Home edit mode (drag/jiggle) → Settings shortcut
- Long-press icon → Edit (label/icon) · Hide · App info · Uninstall
- Drawer ⋮ → Drawer settings
- Any gesture can be bound to Settings
- Launcher icon in drawer → full Settings app
- System "launcher settings" intent → full Settings app

### 2.2 Apply model
- Instant apply; no Save button.
- Top-bar overflow: **Revert this session**.
- Confirm dialog only for destructive actions: grid shrink, reset, restore, hide-all.
- Grid shrink shows a ghost overlay + "N icons will move to the next page" before commit.
- Sliders update the live layer while dragging; write on release.
- "Restart needed" is rare; the row carries a badge.

### 2.3 Architecture note
- One typed settings store (single source of truth) exposed as a Flow.
- Previews render from a **draft** settings object using the **same composables** as the real home/drawer, so preview and result cannot drift.
- Style preset cards: tap = preview in mini only; **Apply** = commit.

---

## 3. Settings screen layout

**Phone (single column)**
1. Search bar — indexes every setting by name and synonyms; result tap deep-links and highlights the row.
2. Collapsible live mini-home header (state remembered).
3. Style chip: current preset + "Modified" dot.
4. Category rows: icon + one-line summary (e.g. "5×6 · Circle · Frosted").

**Tablet (two panes)**
- Left: categories. Right: detail. Mini-preview floats at top of the detail pane.

**Every category**
- "Reset section" button.
- "Advanced" fold for rare options.

**Row types**
- Switch · slider with numeric value · segmented control · visual card picker · color swatch + picker · nav row.

---

## 4. Categories and settings

### 4.1 Style & Theme
- Preset: Premium / Minimal / Futuristic / Classy / Retro / Custom (swipeable cards) [M]
- Mode: Light / Dark / Auto (system or schedule) [L]
- Accent: wallpaper-derived / palette / custom picker [L]
- Surface material: solid / translucent / frosted glass; blur, tint, noise sliders [L]
- Corner radius, shadow depth, outline [L]
- Font family, weight, scale [M]
- Export / import theme file [–]

### 4.2 Home screen
- Pages: count, infinite scroll, indicator style [L]
- Grid rows × cols — separate for portrait, landscape, tablet [L + overflow report]
- Margins, cell spacing, padding (grid overlay while dragging) [L]
- Icon size [L]
- Labels: show/hide, size, lines, color [L]
- Search bar: top / bottom / hidden; style; height [L]
- Left "Today" page: on/off, content [M]
- Default page; return-to-home rules [–]
- Overscroll / page-scroll effect [A]
- Lock layout [–]
- Status bar icon color / hide [L]

### 4.3 Wallpaper
- Source: system wallpaper or HavenOS own [L]
- Parallax on scroll [L]
- Dim, blur, auto-dim in dark mode [L]
- Per-page wallpaper [L]
- Picker: full screen; crop on a phone-shaped frame [M]

### 4.4 Dock
- Slots (3–6), rows; floating vs attached [L]
- Background material, opacity [L]
- Magnify on touch [A]
- Show labels [L]
- Same dock on all pages [–]

### 4.5 App drawer
- Style: vertical grid / horizontal pages / list / A–Z list with scrubber / auto-categories [M]
- Open gesture [T]
- Columns and icon size, independent of home [L]
- Sort: A–Z / recent / most used / custom [M]
- Search at top; Recents row; Suggestions row [L]
- Category editor [M]
- Hidden apps list [–]

### 4.6 Icons
- Icon pack: built-in + installed (skeleton while scanning) [M, 12 sample icons]
- Shape: circle / squircle / rounded / teardrop / system [L]
- Themed monochrome icons [L]
- Per-app override via long-press [M]
- Badges: dot / count / none [T, sample badge]

### 4.7 Folders
- Preview style: grid / stack / fan [M]
- Open animation [A]
- Size, background, title visibility [L]

### 4.8 Widgets
- Widget frame: radius, background, transparency [L]
- Resize and stacking [L]
- Built-in clock and weather. Weather needs INTERNET; location optional and explained before request [L]

### 4.9 Gestures
- Triggers: swipe up/down/left/right, double-tap, pinch, two-finger swipe, long-press empty space [T]
- Sensitivity slider [T]
- Actions: drawer, search, notifications, quick settings, recents, lock screen, launch app/shortcut, toggle dock, none
- Gesture pad shows detected gesture and the action it triggers
- Lock screen needs an accessibility/admin permission; row explains before requesting

### 4.10 Search & Smart
- Sources (toggle, permission-gated): apps, contacts, files, web, calculator, shortcuts [T, sample query]
- Web engine [–]
- Suggestions + on-device learning; Clear history [–]

### 4.11 Animations
- Global speed: Off / Fast / Normal / Slow [A]
- App-open, drawer, page transitions [A]
- Press feedback, haptic strength [T]
- Replay intro (logo + animation) [A]
- Follows system "remove animations"; previews go static when on

### 4.12 Privacy & Lock
- Hide apps; hide from search [–]
- Biometric lock for hidden apps [T]
- Lock layout [–]

### 4.13 Backup & Profiles
- Export / import settings file via system file picker; confirm shows count of settings that will change; invalid file changes nothing [–]
- Saved looks (named profiles) [M]
- Reset section / reset all [–]

### 4.14 System
- Set as default launcher → system chooser [–]
- **Permissions dashboard**: purpose, state, Grant button per permission; denied permission disables dependent rows with a reason
- Replay tutorial, diagnostics, licenses, about [–]

---

## 5. States and accessibility

- **Loading:** skeletons for icon-pack scan and wallpaper list.
- **Empty:** no icon packs → "Built-in only" + "Get icon packs" link; no hidden apps → short explainer.
- **Error:** bad backup file or failed apply → message, state unchanged.
- **Offline:** only weather and web search affected; row says so.
- **Permission-gated rows:** lock icon + "Needs X"; tap explains → system grant → setting enables on return.
- **Accessibility:** TalkBack reads values ("Icon size, 72 percent"); targets ≥ 48dp; visible focus; survives large font scale; contrast checked per preset; reduced-motion respected.

---

## 6. First-run link

First run: intro animation → logo → permissions → tutorial.
Add a **style picker** step before the tutorial. It seeds a preset (changeable any time). Tutorial ends with "Customize anytime: long-press home."

---

## 7. Acceptance checklist (for the builder)

- [ ] Every setting above reachable from search and from its contextual entry point
- [ ] Each setting uses its tagged preview tier
- [ ] Instant apply + Revert session works; destructive actions confirm
- [ ] Grid shrink shows overflow report before commit
- [ ] Preview and real home share the same composables
- [ ] Permission-gated rows handle granted / denied / never-asked
- [ ] Backup export/import round-trips; invalid file is rejected without side effects
- [ ] Two-pane on tablet, single column on phone, no overflow at narrow width
- [ ] TalkBack labels, focus order, reduced-motion verified on device

---

## 8. Risks (⚠️ unverified)

- ⚠️ Blur/glass cost on low-end devices → auto-fallback to translucent.
- ⚠️ Lock-screen and some gesture actions need sensitive permissions; may affect store policy.
- ⚠️ Widget host behaves differently across OEMs/versions; test on a real device.
- ⚠️ Status-bar theming is limited by the system.
