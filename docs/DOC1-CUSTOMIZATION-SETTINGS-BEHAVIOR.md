# HavenOS — Doc 1 of 3 — How Customization and Settings Work

Paste into the builder with Docs 2 and 3. The three docs are one set:
- **Doc 1 (this):** behavior of customization (edit mode) and of every setting, and how settings change the home.
- **Doc 2:** home-screen UI, including the dock.
- **Doc 3:** essential features, widgets, and how all parts coordinate.

Where these docs conflict with the earlier "Home v2" prompt, **these win**. The Settings v2 prompt stays valid for Settings.

---

## 0. Rules

- **The Settings UI is approved. Do not change its layout, visuals, copy, hubs, components or order.** Only the behavior behind the rows may change. If a visible row has no working behavior, implement it. If you cannot, leave the UI untouched and list the row in the report.
- **Inspect first.** The current build already has: Today page, grid pages, folders, widget placeholders, an edit-mode toolbar, a Settings panel with a "Before" button, Calm, and a default-launcher banner. Keep what works. Do not restart from scratch.
- **Preserve user data** (icons, folders, widgets, pages, hidden apps, settings). Version the layout schema and write tested migrations.
- Keep the existing stack and settings keys. Add new keys with defaults. No new dependencies unless justified. No network calls beyond the existing weather feature. No analytics. Clean-room: patterns only, no copied code or assets.
- Do not publish, release, push, or change signing, package name or version.
- Report only checks you actually ran.

---

## 1. What your captures show (customization)

**Works**
- Settings opens as a panel over the live home, and the home re-lays out behind it. "Before" exists.
- A banner explains "HavenOS is not the default launcher. Home-button presses still open the old launcher." (This is a state, not a bug. Test with HavenOS set as default.)
- Quick tune has Icon size (with Auto), Grid (Density / 4×6 / 5×6 / Custom), Mode (Light / Dark / Auto), Labels, Dock position (Bottom / Left / Right).

**Problems to fix**
1. Edit mode draws a **gold outline around every grid cell** and a **gold handle dot on every icon**. It looks like a debug overlay. A similar outline is visible in a capture with no edit toolbar, so it may be stuck on.
2. The edit toolbar is **11 text-only gold actions in one row** (Add widget, Add page, Pages, Wallpaper, Personalization, Snap: Off, Undo, Resize, Remove (1), Settings, Calm, Done). It **overlaps the first row of icons**, and the permanent helper line overlaps icons too.
3. "Snap: Off" has no clear meaning, and off is the default.
4. Resize and Remove are global buttons that depend on a hidden selection ("Remove (1)").
5. The **dock is not visible in edit mode**, so you cannot drag an icon to it.
6. Widgets show as **blank dark rectangles** labelled only "Weather" and "Agenda", and two identical Agenda widgets are seeded.
7. The grid is **cut off by the screen edge** (bottom row clipped), and the page indicator sits on top of a widget.
8. A toast from another app ("Themes aren't supported in your region") appears over the grid, which suggests apps may still **launch on tap while editing**. ⚠️ Verify. In edit mode a tap must select, never launch.

---

## 2. The customization model

Every setting belongs to one of three layers and has one scope.

| Layer | What it is | Examples |
|---|---|---|
| **Look** | tokens that change appearance | preset, mode, accent, material, icon shape, font, label mode |
| **Layout** | what is where | pages, grid, dock contents, widgets, folders |
| **Behavior** | how it responds | gestures, search, motion, haptics, Calm |

**Scopes:** Global → Per-profile (see §5.10) → Per-page → Per-item.
**Precedence (highest wins):** per-item override > per-page > per-profile > global > preset default.
A setting that differs from its preset shows **"Modified"** and appears in the Changes list (existing UI). Reset exists at four scopes: item, page, screen/hub, all.

---

## 3. Edit mode — behavior

### 3.1 Enter and exit
- **Enter:** long-press empty space, the icon menu's Edit, the Personalization or Settings route, or a gesture.
- **Exit:** Done, Back (first press clears selection, second exits), or tapping Done on the toolbar.
- Changes apply **live** and are saved as they happen. Done only leaves the mode. "Revert session" is in the overflow menu.
- The Settings panel may be open during edit mode. Both share the same state and the same undo stack.

### 3.2 States

| State | Behavior |
|---|---|
| Idle edit | items lifted slightly, no outlines; dock visible; toolbar visible |
| Selected | one item shows a selection ring and a floating context bar (§3.10); handles only on this item |
| Multi-select | tap "Select" (or tap more items after the first); the context bar acts on all |
| Dragging | item follows the finger; soft grid dots and a ghost of the landing spot appear; drop zones show |
| Resizing | edge or corner handles; live ghost; size label ("4×2") |

### 3.3 Moving items
- Long-press 350ms starts a drag (immediately in edit mode). Haptic tick on pickup and on each snap.
- **Snap:** On (default) snaps to the nearest cell. Off snaps to a finer quarter-cell sub-grid. Label it "Snap to grid", and show the state with an icon and a tick, not "Snap: Off".
- **Collision policy:**
  - Icon dropped on an empty cell: place.
  - Icon dropped on an icon, held 400ms: create a folder. Released earlier: neighbors shift row by row to make room.
  - Icon dropped on a folder: add to it.
  - Widget dropped on occupied cells: never overlaps. A red ghost shows, and release moves it to the nearest free fit (or back if none).
- **Edge drag:** holding at a screen edge for 600ms turns the page; a new page is offered after the last one.
- **Dock:** the dock is a drop target at all times (§3.8).
- **Remove and Uninstall zones** appear at the top while dragging (left Remove, right App info/Uninstall).
- **Taps in edit mode never launch apps.** Tap selects.

### 3.4 Resizing
- Widgets and large folders only. Handles on edges and corners; min and max come from the widget provider; built-in widgets offer fixed size chips (Small 2×2, Wide 4×2, Tall 2×4, Large 4×4) plus free resize inside limits.
- Neighbor icons reflow live while resizing; if they cannot fit, the ghost turns red and the resize is clamped.
- The old "Resize" toolbar button becomes an entry in the context bar that shows the size chips.

### 3.5 Adding
- **Add widget:** opens the widget sheet (Doc 3 §2.2). Tap adds to the first free spot (auto-search all pages, then offers a new page). Drag places exactly.
- **Add page:** appends a page and scrolls to it.
- **New folder:** drop one icon on another, or the context bar action on a selected icon.
- **Pin requests from other apps** (shortcuts and widgets) are accepted through the system dialog and placed on the current page's first free spot.

### 3.6 Remove, Uninstall, Hide
- **Remove from home** only removes the item from the layout (the app stays in the Library). Undo available.
- **Uninstall** only for user apps, always via the system confirm.
- **Hide** moves the app to Hidden (Privacy) and removes it from the layout and search.
- A folder with one app left dissolves to a plain icon. An empty folder is deleted.

### 3.7 Undo and redo
- 50-level stack covering moves, resizes, adds, removals, page operations, folder changes, and Settings changes made in the session.
- Undo and Redo are icon buttons on the toolbar plus a 5-second snackbar after each destructive action.
- The stack is cleared when the app process ends. Persisted snapshots handle longer recovery (Doc 3 §3.6).

### 3.8 Dock in edit mode
- Always visible. A dragged icon over the dock highlights the target slot. Dropping inserts at that slot and shifts others; if the dock is full, the last slot is highlighted red and the drop is refused with a short message ("Dock is full").
- Dock icons can be reordered by drag and dragged out to the home.

### 3.9 Pages
- **Pages** opens the page overview: thumbnails, drag to reorder, tap to open, add, delete (confirm if not empty), hide, set default page, move items between pages by dragging an icon thumbnail onto a page.
- Page types: Home (cell grid) and Today (card flow, Doc 3). Today cannot be deleted but can be disabled in Settings.

### 3.10 Toolbar and context bar — action map (nothing removed, everything regrouped)

**Top toolbar (one row, 56dp, icons + short labels on Expanded, icons only on Compact; reserves its own space, never overlaps content):**

| Slot | Action | Behavior |
|---|---|---|
| Left | Undo, Redo | as §3.7 |
| Left | Snap to grid | toggle, see §3.3 |
| Centre | **+ Add** | menu: Widget · Page · Folder |
| Centre | Pages | page overview |
| Centre | Wallpaper | opens the existing wallpaper screen, returns to edit mode |
| Right | **Look** (was "Personalization") | opens the Settings panel at the Look hub |
| Right | Calm | opens the Calm editor (choose favorites, order, schedule). It does not enter Calm. |
| Right | Settings | opens the Settings panel at the hub |
| Right | Done | exits edit mode |
| Overflow ⋮ | Revert session · Restore layout… · Reset page | see Doc 3 §3.6 |

**Context bar (floating near the selected item, appears only when something is selected):** Resize (widgets) · Remove · Move to page · Duplicate (widgets) · Item settings (widget configure / folder style) · Select more.
The count ("Remove (1)") appears only when more than one item is selected.

**Hint:** a single dismissible chip the first three times edit mode opens: "Drag to move · Tap to select". It never covers icons and never reappears afterwards.

### 3.11 Accessibility in edit mode
- TalkBack custom actions on every item: Move left/right/up/down, Move to page, Add to dock, Resize (widgets), Remove, Edit label, Hide.
- Keyboard: arrows move focus, Alt+arrows move an item, Space selects, Delete removes (undoable), Esc exits.
- All handles and buttons ≥ 48dp touch target.

---

## 4. How every Settings row behaves on Home

**Rule for all rows:** changes apply live to the real home behind the panel. Anything that can lose or displace content asks first with a preview. Every change is undoable and listed in Changes.

### Quick tune and hub-level rows (as seen)

| Setting | What it changes | Behavior |
|---|---|---|
| Preset / Look card (Minimal · Dark · Translucent, accent swatches) | all Look tokens | draft preview on the home; Apply commits; Cancel restores |
| Icon size (slider, Auto chip) | icon, label, dock-icon and folder-preview metrics | Auto = derived from cell size; moving the slider sets a manual value; the Auto chip returns to derived; value clamped so icon + label fits the cell, with the note "Max for this grid" |
| Grid (Density / 4×6 / 5×6 / Custom) | columns × rows of every Home page in the current profile | Density = computed from window size and icon size; shrinking shows the overflow ghost (§5.2) |
| Mode (Light / Dark / Auto) | panel, dock, folders, widget frames, label colors, scrim | Auto follows the system or the schedule; transitions with a 200ms cross-fade |
| Labels (On/Off) | icon labels everywhere (home, dock, folders, Library) | Off removes label space and re-centres icons; legibility engine runs when On |
| Dock position (Bottom / Left / Right) | dock placement and the grid's insets | see §5.3 |

### Remaining hubs (behavior contract; UI unchanged)

| Hub | Settings | Behavior |
|---|---|---|
| **Look** | material, blur/tint/noise, radius, shadow, font, icon shape, icon pack, themed icons, wallpaper | live; blur falls back to translucent if unsupported or janky; icon pack applies to home, dock, folders, Library, search; wallpaper changes re-run the legibility engine and re-suggest accent |
| **Home & Dock** | pages, margins, spacing, labels, search position, Today on/off, status-bar, lock layout, dock (slots, material, recents), folders, widget frame | Lock layout blocks drag, resize and remove everywhere, with a one-line toast "Layout is locked"; Today off removes the Today page/column without deleting its widget list; dock slots change shows overflow preview if apps would be dropped |
| **Drawer & Search** | drawer style, columns, sort, recents, categories, search sources, engine, history | takes effect the next time the Library or Search opens, and immediately if it is open |
| **Gestures & Motion** | gesture map, sensitivity, haptics, animation speed, transitions, intro replay | only wired gestures fire; Test fires the real action; reduced-motion overrides animation speed |
| **Privacy** | hidden apps, biometric lock, hide from search | hiding removes the app from layout, Library and search immediately, with Undo |
| **Device & Data** | default launcher, permissions, backup/restore, saved looks, reset, about, advanced | Make default opens the system chooser; permission grants enable dependent rows on return |

---

## 5. Special behavior rules

### 5.1 Icon size Auto vs manual
- Auto: size = f(cell width, cell height, label on/off, density). Changing grid or rotation changes the icon size automatically.
- Manual: size fixed in dp per profile; grid changes do not alter it unless it no longer fits, in which case clamp and show a note.

### 5.2 Grid change safety
- Any grid change that would displace items shows a ghost overlay on the home plus "N items will move to page 2" with Apply and Cancel. Nothing is committed until Apply.
- Widgets that no longer fit are shrunk to the nearest valid size; if impossible, moved to the next page with the same position order. The summary lists them.
- Folders keep their contents; positions reflow row by row.

### 5.3 Dock position
- **Bottom:** floating pill centred. **Left / Right:** a vertical pill centred on that edge, same material, same contents.
- The grid reserves inset for the dock so nothing overlaps; page indicator sits above a bottom dock, or at the opposite edge's bottom for a side dock.
- Changing position animates the dock (240ms) while the grid re-fits.
- On Compact (phone portrait) a side dock is allowed but limited to 5 slots.

### 5.4 Mode (Light / Dark / Auto)
- Auto uses system dark mode, or a time schedule if set. Wallpaper scrim and label colors re-derive. The preset's palette provides both variants.

### 5.5 Presets
- A preset is a bundle of Look and Behavior tokens (never Layout). Applying a preset never moves items.
- Changing any token afterwards marks the preset "Modified". Reset to preset is available from Changes.

### 5.6 Before, Changes, Undo
- **Before:** hold to show the snapshot taken when the panel or edit mode opened; release returns to current.
- **Changes:** lists every difference from the preset and from the snapshot, each with a revert.
- Undo is shared across Settings and edit mode.

### 5.7 Default launcher state
- While not default, the banner stays at the top of Settings. Everything else works. After HavenOS becomes default, the banner disappears at once and the home button returns to page 1 and closes overlays.

### 5.8 Wallpaper
- Changing wallpaper (system or HavenOS) triggers: palette extraction → accent suggestion chip (not auto-applied), label legibility recompute, scrim recompute, Calm dim recompute. Per-page wallpaper, if enabled, uses the same pipeline.

### 5.9 Calm configuration
- Calm editor (toolbar Calm): pick up to 8 favorites, reorder by drag, set schedule (days/times) and entry options. Favorites reuse existing data. Calm never changes the layout.

### 5.10 Layout profiles per size class
- Store one canonical ordered list of items per page, plus a **layout profile** per window class: Expanded landscape, Medium (tablet portrait), Compact.
- First time a profile is used, derive its positions from the canonical order by reflow. After that, the profile keeps its own positions, so editing in landscape never scrambles portrait.
- Adding or removing an app updates every profile. A "Sync layouts" action (overflow) re-derives other profiles from the current one with a preview.

---

## 6. Precedence and conflict table

| Conflict | Winner | Rule |
|---|---|---|
| Preset says icon shape X, user chose Y | user | mark Modified |
| Auto icon size vs manual | manual | Auto only when chip is on |
| Per-page wallpaper vs global | per-page | falls back to global if file missing |
| Dock position vs grid size | dock | grid re-fits, overflow previewed |
| Lock layout vs any edit | lock | every edit blocked with a toast |
| Reduced motion vs motion preset | reduced motion | fades only |
| Large font vs icon size | accessibility | label wraps or hides; icon never below 48dp touch target |
| Permission denied vs feature on | permission | row disabled with reason and Allow chip |
| Calm active vs home edit | edit mode exits Calm first | state restored on exit |
| System dynamic colour vs chosen accent | chosen accent | system colour only if "Wallpaper-derived" is selected |

---

## 7. Persistence, migration, performance

- Single source of truth for layout and settings; UI reads state, never owns it.
- Layout edits are **transactions**: one drag/drop = one atomic write; crash-safe (write-ahead or equivalent).
- Debounce slider writes (200ms after last change).
- Schema version bump with migration + test; old backups import.
- 60fps while dragging on a mid-range tablet; no relayout of the whole home for a single item move.
- Survive rotation, split-screen, process death and wallpaper changes without losing the edit session's undo stack where the process lives, and without corrupting layout where it does not.

---

## 8. Acceptance checklist

- [ ] No gold outline on every cell; outlines only for the selected item, drag ghost and drop targets; no stuck overlay outside edit mode
- [ ] Toolbar reserves its own space and does not overlap icons; hint chip dismissible and shown max 3 times
- [ ] Every old toolbar action still reachable (§3.10 map)
- [ ] Taps in edit mode select and never launch apps
- [ ] Dock visible in edit mode and works as a drop target; "Dock is full" refusal works
- [ ] Snap behavior defined and labelled "Snap to grid"; collisions follow §3.3
- [ ] Widget resize via handles and size chips; never overlaps; neighbors reflow
- [ ] Remove ≠ Uninstall ≠ Hide, each with the right confirmation and Undo
- [ ] Undo/redo covers edit-mode and Settings changes
- [ ] Grid change shows overflow ghost + count before commit; widgets handled per §5.2
- [ ] Icon size Auto/manual per §5.1; clamp note shown
- [ ] Dock position Bottom/Left/Right works and nothing overlaps the dock
- [ ] Mode, Labels, presets and Before/Changes behave per §4–§5
- [ ] Layout profiles: rotating the tablet does not scramble either orientation
- [ ] Lock layout blocks all edits
- [ ] TalkBack actions and keyboard moves work in edit mode
- [ ] Settings UI visually unchanged (before/after screenshots if the environment supports them)
- [ ] Existing user layout preserved after update (migration tested)

---

## 9. Checks and final report

Run the project's available checks (build, lint, type check, unit tests). Add tests for: collision resolution, snap math, grid-overflow calculation, undo/redo stack, layout-profile derivation, migration. If an emulator or device is available, exercise the flows on a tablet landscape build.

Final report:
1. **Changed:** files and behavior, short bullets.
2. **Fixed** from §1, each marked fixed / partly / not fixed.
3. **Checks actually run** and results; pre-existing failures separated.
4. **Not verified** and **known gaps**, plus the single most useful next improvement.

Do not call the result production-ready unless it was verified.
