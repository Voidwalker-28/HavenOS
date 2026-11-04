# HavenOS — Settings Redesign (v2) — Builder Prompt

Paste everything below into the builder, in the existing HavenOS project.

---

## 0. Role, scope, rules of engagement

You are a senior Android engineer and product designer. Redesign the **Settings experience of the existing HavenOS launcher** (`app.havenos.launcher`) to the polish of a top-tier consumer app (the quality bar of WhatsApp, Instagram, ChatGPT, iOS and One UI settings) **and fix what those apps get wrong**: effects are invisible while you change them, features are listed by component instead of by intent, search only navigates, there is no before/after, and settings are buried.

Rules:
- **Inspect first.** Read the project structure, README, build files, UI toolkit, navigation, the settings store (keys, defaults, types), the Home host, the theme/material system, icon-pack handling, wallpaper, gesture dispatch, permissions and the backup format. Keep the existing stack and architecture.
- This is a **UI/UX-layer redesign**. Reuse the existing settings store and keys. No user data loss. If a key must change, write and test a migration.
- Check installed dependencies before adding any. Prefer none. Use the project's existing icon set or one well-maintained library; never hand-drawn improvised icon paths.
- Do not change unrelated launcher behavior. The only allowed change outside Settings is a minimal preview hook in the Home host, if needed.
- Clean-room: take inspiration from interaction patterns only. Do not copy code, branding, icons or assets from Nova, Niagara, Smart Launcher, iOS, One UI, WhatsApp, Instagram or ChatGPT.
- No secrets in source. No new network calls, analytics or telemetry. Any "recent/most used" ranking is local only.
- Do **not** publish, release, push, change signing, package name or version code, or touch production data.
- Proceed on reasonable assumptions and state them in one line. Ask only if a choice would materially change the product, permissions or data.
- Report only checks you actually ran. Never claim something works if you did not run it.

---

## 1. Known defects in the current build (from a screen recording, tablet landscape) — remove all of them

1. Spec jargon shown as UI text: `[L]`, `[M]`, `[T]`, `[–]`, "Preview · [T] test pad", "Preview · [–] none", "Picker — phone-shaped crop frame [M]", "Appearance [L]".
2. Controls that do nothing are visible: "Stored only — folder cell size is not yet driven by this value (sample above scales with it)" and similar on Folders.
3. The preview is hidden behind a "Mini-home · Show" strip. Settings replaces the home screen instead of sitting over it, so changes can't be seen live.
4. Wallpaper crop is three sliders (Crop zoom, Crop offset X, Crop offset Y) instead of dragging/pinching the image. The phone frame is left-aligned with a large empty area beside it.
5. Gestures is a long list of rows reading "Runs: None". The "test pad" is only a text row. Triggers that don't dispatch anything are listed with explanatory paragraphs.
6. A raw `adb shell pm grant …WRITE_SECURE_SETTINGS` command and a Copy button sit in an ordinary settings row.
7. A paragraph card ("Denied permissions lock their rows…") instead of a compact state.
8. A paragraph of helper text under almost every row.
9. Sliders are drawn as long dotted tracks with dozens of tick dots (visual noise).
10. Every group is a box with a 1px outline.
11. Category tiles are single letters ("S" appears twice: Style & Theme and Search & Smart).
12. The left rail scrolls independently and shows only ~6 of 14 categories; the right pane often opens already scrolled mid-page.
13. "Reset" appears at the top right of every page **and** as a bottom row.
14. "Minimal ●" under the title is ambiguous.

---

## 2. North star — 8 principles

1. **The home screen is the preview.** Settings is a panel over the live home. There is no second fake mini-home.
2. **Organize by intent, not by component.** Six hubs, not fourteen categories.
3. **First screen does the 80%.** A hero Look card with preset carousel and a Quick tune strip so common changes need no navigation.
4. **Lean rows.** Title + live value. Help behind ⓘ. About 7 rows per screen. Rare options in an Advanced fold.
5. **Show, don't list.** Visual pickers: tap-to-size grid, device diagram for gestures, animated thumbnails for styles, drag/pinch for wallpaper.
6. **Search that acts.** Simple settings are controllable inside the search result.
7. **Honest controls.** A setting appears only if it visibly works. Permissions are requested at the point of use.
8. **The panel wears the user's style.** It uses the user's chosen material and accent, so customization is demonstrated by the settings themselves.

---

## 3. Layout architecture

### 3.1 Live preview mechanism
- The preview is the **real Home composition** reading the same live settings state. Apply changes to the live state immediately, with debounced persistence on release or about 200 ms after the last change.
- Choose the approach that fits the current architecture and explain it in one line:
  - (a) a Settings overlay hosted inside the Home host (preferred), or
  - (b) a translucent Settings Activity above Home, provided Home stays visible and keeps updating.
- ⚠️ Verify on a real device that Home keeps updating behind the panel.
- Preview-only state (preset carousel, grid shrink) uses a **draft layer** over committed settings. Draft is applied to the visible Home but not persisted until Apply.

### 3.2 Tablet landscape (width ≥ 840dp)
- Home occupies the left ~58%. The Settings panel docks on the right, ~42% of the width, clamped to 440–600dp. Panel corner radius 28dp, inset 12dp from the edges.
- **No permanent left rail.** Navigation is a stack inside the panel: large title, back chevron, breadcrumb ("Settings › Look › Material").
- Tapping the Home area behind does not dismiss the panel. A close (✕) button does, and system Back steps up the stack.
- The panel scrolls independently. Each screen opens at the top.

### 3.3 Tablet portrait and phone
- Bottom sheet with three detents: **peek ~30%**, **half ~58%**, **full ~92%**. Drag handle, springy snapping, and a "Peek" button on the title bar that drops to the smallest detent.
- Home stays visible above the sheet in peek and half. At full, the sheet behaves as a full page.
- The sheet remembers its last detent per screen type.

### 3.4 Preview aids (all screens that change visuals)
- **Spotlight:** the affected Home element (dock, a grid cell outline, search bar, a folder) pulses a soft accent ring once (≈600 ms) when a related control is touched. Reduced motion: no pulse, a static 2dp ring for 600 ms.
- **Before:** a "Before" button in the title bar. While held, Home shows the state captured when the panel opened. Release returns to current.
- **Undo:** after each committed change a snackbar offers Undo for 5 s. Keep a session undo stack of the last 20 changes.
- **Changes:** a toolbar chip "Changes (N)" opens a list of everything changed from the current preset, with per-item revert and a "Revert session" action.
- One Reset per screen, in the title-bar overflow menu ("Reset this screen…"), with a confirm that names how many settings change.

### 3.5 Entry points
Each opens the relevant hub or screen **in context**, with the spotlight on the target:
- Long-press empty home → quick bar: Wallpaper · Widgets · Look · Settings.
- Home edit mode → Settings shortcut.
- Long-press icon → Edit icon, Hide, App info, Uninstall.
- Drawer ⋮ → Drawer settings.
- Any gesture bound to "Settings".
- The launcher icon in the drawer, and the system launcher-settings intent → hub screen.

---

## 4. Information architecture — six hubs

Map **every** existing working setting into one of these. Do not drop a working setting.

| Hub | Contains |
|---|---|
| **Look** | Presets (Premium, Minimal, Futuristic, Classy, Retro, Custom); mode Light/Dark/Auto; accent; material (solid/translucent/frosted + blur, tint, noise); corner radius, shadow, outline; font; icon shape; icon pack; themed icons; wallpaper |
| **Home & Dock** | Pages; grid; spacing/padding; icon size; labels; search bar position/style; Today page; status bar; lock layout; dock; folders; widgets frame and built-ins |
| **Drawer & Search** | Drawer style; columns and icon size; sort; recents/suggestions; categories; hidden-apps link; search sources; web engine; suggestions; history |
| **Gestures & Motion** | Gesture triggers → actions; navigation gestures, edge thickness, sensitivity; haptics; animation speed and transitions; press feedback; replay intro |
| **Privacy** | Hidden apps; biometric lock; hide from search; lock layout |
| **Device & Data** | Default launcher; permissions; backup and restore; saved looks; reset; about and licenses; Advanced (diagnostics, developer options) |

---

## 5. Settings home (hub screen)

Top to bottom:
1. **Search field** (see §8). Placeholder: "Search settings or try 'bigger icons'".
2. **Look hero card** — a large card showing the current preset name and its swatch strip (accent, surface, text). Below it, a horizontal preset carousel of five cards plus Custom. Swiping or tapping a card **previews it live on Home behind** (draft layer). Two buttons appear: **Apply** and **Cancel**. Dismissing the panel or Cancel restores the previous look.
3. **Quick tune** — one card with five inline controls:
   - Icon size: slider with an Auto chip.
   - Grid: chips for common sizes plus "Custom…" which opens the grid picker.
   - Mode: segmented Light / Dark / Auto.
   - Labels: switch.
   - Dock: segmented Attached / Floating.
4. **Recent** — up to 4 chips of the last-changed settings (local history). Tap to jump to it.
5. **Six hub rows** — rounded-square glyph tile (tinted, real glyph, no letters), title, one-line **live summary** (e.g. "5×6 · Circle · Frosted"), chevron.
6. **Needs attention** banner — appears only when needed, at most one at a time, in priority order:
   1. HavenOS is not the default launcher → button "Make default".
   2. A permission needed by an enabled feature is missing → button "Allow".
   3. A backup or restore failed.
7. Footer row: "Changed from preset (N)" → opens the Changes list.

---

## 6. Components and hard rules

### Rows
- Height 56dp phone, 64dp tablet; minimum touch target 48dp.
- Title ≤ 3 words, sentence case. Live value on the right, ≤ ~14 characters, muted.
- Help text only behind ⓘ (tap opens a small popover, ≤ 90 characters). No always-visible paragraphs.
- Max ~7 rows per screen. Overflow goes into a "More options" expander. Rare or technical options go to Advanced.

### Controls
- **Slider:** smooth continuous track, 4dp, large thumb (≥ 28dp), a value bubble above the thumb while dragging, − / + step buttons for accessibility, haptic tick at snap points only, an "Auto" chip where an automatic value exists. **No dotted tick tracks.**
- **Segmented control** for 2–4 mutually exclusive options.
- **Visual card pickers** for styles (see §7).
- **Color:** swatch row plus a "Custom" button opening a picker with hex input.
- **Switch** for on/off. The row title says what happens when on.
- **Permission chip:** inline "Allow camera" style chip on rows that need a permission. Tap → short rationale sheet (one sentence + Allow) → system prompt → the row enables on return.

### Banned in UI
- Any text containing `[L]`, `[M]`, `[T]`, `[–]`, "stored only", "not yet driven", "TODO", "sample above", "Runs: None".
- Paragraph cards; 1px outlined boxes around groups; dotted sliders; letter tiles; always-visible helper text; a collapsed or hidden preview; duplicated titles; more than one Reset per screen.

### States on every screen
- **Loading:** skeleton rows (icon-pack scan, wallpaper list).
- **Empty:** one-line explanation + one action (e.g. "No icon packs installed · Get icon packs").
- **Error:** inline message, state unchanged, retry action.
- **Disabled:** dimmed with a one-line reason under the title ("Needs calendar access").
- **Offline:** only weather and web search; the row says so.

---

## 7. Screen-by-screen

**Look → Presets.** Five bundles; tune the exact values so each is clearly distinct and passes contrast:
- Premium: frosted material, deep neutral surfaces with a warm metallic accent, squircle icons, refined sans, soft shadow, slow smooth motion.
- Minimal: solid, monochrome with one accent, circle icons, thin sans, no shadow, fast motion.
- Futuristic: translucent dark, cool neon accent, rounded-square icons, geometric font, faint glow, snappy motion.
- Classy: matte, muted jewel accent, serif titles, subtle shadow, calm motion.
- Retro: solid warm palette, chunky font, higher radius, bouncy motion.
Custom = any individual override. Changing any token marks the preset "Modified", with Reset to preset available from Changes.

**Look → Material.** Three large visual cards (Solid / Translucent / Frosted) that render the real material over the current wallpaper. Blur strength, tint, noise as sliders (Frosted only; hidden otherwise). Automatic fallback to translucent if blur is unsupported or the device drops frames; show a one-line note when the fallback is active.

**Look → Icons.** Shape chips drawn with real sample icons. Icon pack list (built-in + installed) with skeleton while scanning; selecting one previews on Home. Themed monochrome switch. Per-app override is reached through long-press, not listed here.

**Look → Wallpaper.** Two source cards: **System wallpaper** and **HavenOS wallpaper** (gradient, solid, photo). Crop is **direct manipulation**: pinch and drag the live wallpaper behind the panel (an "Adjust" mode with Done). Remove the zoom and X/Y sliders. Sliders only for dim, blur and parallax. Per-page wallpaper toggle.

**Home & Dock → Grid.** An interactive grid picker: tap or drag to choose columns × rows within the min/max the code already supports. Landscape, portrait and tablet each have their own picker tab. Shrinking shows a **ghost overlay** on Home and "N icons will move to page 2" with Apply / Cancel. Nothing is committed until Apply.

**Home & Dock → Layout.** Spacing, padding and margins as sliders with a grid overlay shown on Home while dragging. Icon size and label options. Search bar position as a three-way segmented control (Top / Bottom / Hidden).

**Home & Dock → Dock.** Two cards (Attached / Floating) with real previews; slots stepper (3–6); material inherits from Look unless overridden.

**Home & Dock → Folders.** Only options that actually work. Preview style as three animated thumbnails (grid / stack / fan). If an option is not wired (for example folder size or background), **do not show it**.

**Home & Dock → Widgets.** Frame options (radius, background, transparency) with live preview on a placed widget. Built-in clock and weather. Weather location and units appear only after the location permission step. Third-party widgets are added via edit mode, with one line saying so.

**Drawer & Search → Drawer style.** Five animated wireframe thumbnails (vertical grid, horizontal pages, list, A–Z with scrubber, categories). Tapping one applies it live to the drawer with a "Try it" button that opens the drawer. Columns and icon size as sliders, sort as a segmented control.

**Gestures & Motion → Gestures.** A **device diagram** (matching the current orientation) with hotspots for gestures the code actually dispatches. Tap a hotspot → bottom sheet action picker with a radio list (only offered actions) and a Test button that fires the action immediately. A "Try it here" area on the diagram shows the detected gesture and the action name. Hide triggers that do nothing. Navigation-gesture sensitivity and edge thickness are sliders in a "Fine tune" expander, with edge zones highlighted on the diagram while dragging. Haptics is a switch.

**Gestures & Motion → Motion.** Speed as a four-way segmented control (Off / Fast / Normal / Slow). Each transition (app open, drawer, page scroll) is a card with a small looping demo and a Play button. Replay intro opens the intro animation. Follows the system "remove animations" setting; when it is on, demos are static and a one-line note says why.

**Privacy.** Hidden apps list with search, empty state "Nothing hidden". Biometric lock switch (checks that biometrics exist first). Hide-from-search switch. One-line result summary after each change ("12 apps hidden") with Undo.

**Device & Data.**
- **Default launcher card** at the top with state and a "Make default" button.
- **Permissions:** compact rows, each with a glyph, name, purpose in ≤ 6 words, a status dot **plus** text ("Allowed" / "Not allowed") and an Allow or Manage chip. No paragraph cards.
- **Backup:** Export and Import through the system file picker. Import shows "N settings will change" before applying. An invalid file changes nothing and shows a clear error.
- **Saved looks:** named profiles with a live preview on tap.
- **Advanced** (collapsed): diagnostics, and **Developer options** with the `adb … WRITE_SECURE_SETTINGS` command, a warning ("For advanced users"), and a Copy button. The "Hide navigation bar everywhere" row appears only here, never in Gestures.
- About and licenses.

---

## 8. Search

- Local fuzzy index over setting titles, hub path and synonyms. No network.
- Results grouped **Settings** and **Actions** (e.g. "Make default launcher", "Replay intro", "Export backup").
- **Inline controls:** switches, segmented controls and sliders render inside the result row and work in place. Tapping the row body navigates to the screen and spotlights the setting.
- Synonym examples: "bigger icons / icon size / scale"; "dark / night / theme"; "blur / glass / frosted / transparent"; "hide apps / private / secret"; "grid / rows / columns / layout"; "swipe / gesture"; "default / home app / launcher"; "backup / export / restore".
- Empty query shows 5 suggestions plus recent searches (local, clearable). No-result state suggests the closest hub.

---

## 9. Visual language

- **Panel material:** the user's chosen material (frosted/translucent/solid) with a **minimum adaptive scrim** so text contrast stays ≥ 4.5:1 on any wallpaper.
- **Cards:** tonal surfaces (panel + ~6% lift), no outlines. Radii: panel 28dp, cards 20dp, chips and inputs 12dp.
- **Spacing scale:** 4 / 8 / 12 / 16 / 24 / 32dp. Card padding 16dp, gap between cards 12dp, 24dp between sections.
- **Type scale:** Title 28/34 semibold · Section label 12 caps, +0.8 tracking, muted · Row title 16 medium · Value 15 regular, muted · Help 13.
- **Icons:** one consistent outline glyph set in rounded-square tiles. Each hub gets a fixed hue from a six-hue palette, tuned so the white glyph passes contrast. Tiles do not follow the accent.
- **Accent:** drives interactive elements (switch, slider, selected states, focus ring) and re-themes the panel live as it changes.
- **Semantic tokens:** panelSurface, cardSurface, cardSelected (accent at ~16%), textPrimary, textSecondary, accent, danger, success, focusRing. Define each for light and dark. Both modes must look intentional.

---

## 10. Motion

- Sheet and panel use spring physics; stack navigation uses a 240 ms shared-axis slide plus fade; value changes cross-fade 120 ms; spotlight pulse ≈ 600 ms.
- Animate transform and opacity. No layout-property animation per frame; no "animate all".
- **Reduced motion** (system setting): no pulse, no slide, instant state changes, static demos.
- No animation blocks input.

---

## 11. Accessibility

- TalkBack: every control has a label, role and state ("Icon size, 72 percent, slider"). Previews and diagrams have text equivalents. Value bubbles are announced.
- Touch targets ≥ 48dp; visible focus ring; logical focus order; full operation with a physical keyboard or D-pad (the test device is a tablet).
- Font scale up to 200%: rows wrap, nothing clips, no horizontal overflow.
- Contrast: text 4.5:1, controls and focus 3:1. Never use color alone to show state.
- Layout is RTL-safe.

---

## 12. Honesty audit (do this first, before any UI work)

Produce an inventory of **every existing setting**: key, screen, control type, and a status of **Working** (visibly changes behavior; verified), **Partial**, or **Not wired**.
- Working → keep, place per §4.
- Partial → keep only the part that works.
- Not wired → **remove from the UI** (keep the key). List all of them in the final report.
- Rewrite all user-facing copy: short, sentence case, no developer notes anywhere in the UI.

---

## 13. State and data

- Reuse the existing settings store, keys and defaults. Single source of truth exposed as a reactive stream.
- Draft layer for previews (preset carousel, grid shrink, wallpaper adjust); commit on Apply or Done.
- "Before" and "Revert session" use a snapshot taken when the panel opens.
- Undo stack and Changes list derive from a change log of key, old value, new value, in memory for the session. Only the "Recent" chips persist, locally.
- Backup format stays compatible. If it changes, version it and migrate old files.
- Debounce writes; never block the main thread; icon-pack scanning runs off the main thread with cancellation.

---

## 14. Permissions UX

- Ask only at the point of use, with a one-sentence reason first.
- Denied permission → dependent rows stay visible but disabled, with the reason and an Allow chip. Granting re-enables them without leaving the screen.
- "Make default launcher" appears in the hub banner only while HavenOS is not the default.
- Sensitive permissions (accessibility or admin for lock-screen gestures, WRITE_SECURE_SETTINGS) are explained clearly and never auto-requested. ⚠️ Store policies can restrict some of these, so flag any you are unsure about in the report.

---

## 15. Performance and robustness

- Previews must not rebuild Home. Reuse it with state changes only, and avoid unnecessary recomposition or relayout.
- Blur: capability check plus frame-time guard with automatic fallback to translucent.
- Target a smooth 60fps while dragging sliders on a mid-range tablet.
- Survive rotation, split-screen and process death: reopen at the same hub and screen, with no loss of unsaved draft (or discard the draft cleanly and say so).
- Responsive checks at: tablet landscape (~1728×1080 class), tablet portrait, phone 390×844, split-screen. No horizontal overflow.

---

## 16. Build order (vertical slice first)

1. **Audit** (§12) and the settings-store map → short written inventory.
2. **Shell:** tokens, components (§6), panel / bottom sheet with detents, stack navigation, Before / Undo / Changes, spotlight.
3. **Hub screen** (§5) + **Search** (§8), end to end with real settings.
4. **Look** (presets, material, icons, wallpaper) and **Home & Dock** (grid, layout, dock) with live preview. Stop and make this slice fully working before continuing.
5. **Drawer & Search**, **Gestures & Motion**.
6. **Privacy**, **Device & Data**.
7. Polish, accessibility pass, responsive pass, remove dead code and old strings.

If time or budget runs short, finish steps 1–4 completely rather than doing everything partially, and list exactly what remains.

---

## 17. Acceptance checklist

- [ ] No string containing `[L]`, `[M]`, `[T]`, `[–]`, "stored only", "not yet driven", "Runs: None" anywhere in the UI
- [ ] Settings never replaces Home: panel (tablet) or sheet (phone) over the live Home; no collapsed mini-home
- [ ] Hub screen has Search, Look hero with live preset preview + Apply/Cancel, Quick tune, Recent, six glyph rows with live summaries, conditional Needs-attention banner
- [ ] Every control that changes visuals updates Home live; Before, Undo and Changes work
- [ ] Wallpaper crop is pinch/drag; no zoom/X/Y sliders
- [ ] Grid picker shows overflow ghost + "N icons will move" before commit
- [ ] Gestures uses the device diagram, shows only dispatched triggers, and Test works
- [ ] No dotted sliders, no outlined boxes, no letter tiles, no paragraph cards, one Reset per screen
- [ ] Not-wired settings removed from UI and listed in the report
- [ ] ADB/WRITE_SECURE_SETTINGS only in Advanced → Developer options
- [ ] Search finds settings by synonym and controls simple ones inline
- [ ] Permissions flow: granted / denied / never asked all handled, with no paragraph cards
- [ ] Backup export/import round-trips; invalid file changes nothing; existing user settings preserved
- [ ] Light and dark both verified; reduced motion verified
- [ ] TalkBack labels, keyboard operation, 200% font scale verified
- [ ] Tablet landscape, tablet portrait, phone, split-screen verified with no horizontal overflow

---

## 18. Checks and final report

Run the project's available checks (build, lint, type check, unit tests; add tests for settings migration, change log/undo, grid-overflow calculation and search ranking) and, if an emulator or device is available, install and exercise the flows above. Capture screenshots only if the environment supports it.

Finish with a report containing:
1. **Changed:** screens and files, in short bullets.
2. **Removed from UI:** the not-wired settings list.
3. **Checks actually run** and their results. Separate pre-existing failures from new ones.
4. **Not verified** (for example: behavior on real devices, blur performance, TalkBack, store-policy items).
5. **Known gaps** and the single most useful next improvement.

Do not describe the result as production-ready, accessibility-conformant or store-approved unless that was actually verified.
