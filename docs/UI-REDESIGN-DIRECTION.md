# HavenOS Launcher — UI Redesign Direction

**Continuity note:** Settings remains conceptually strong and should not be replaced. This document improves the home experience and the way customization behaves around it, continuing the direction from the shared conversation.

## Product vision

HavenOS should feel like a personal command center rather than a wallpaper with icons placed on top. It should combine the emotional beauty of an aesthetic launcher with the reliability of a major operating system surface.

The user’s vision is not “more widgets everywhere.” It is a powerful, cinematic, highly customizable workspace where visual identity, useful information, and fast app access coexist without fighting each other.

The core promise is:

> **Make the home screen feel authored, while keeping every object predictable, readable, and reachable.**

The design should borrow the best qualities of familiar systems without becoming a collage of them:

- iPadOS: clear spatial composition and tablet-aware side-by-side layout.
- One UI: comfortable reach, strong hierarchy, and calm motion.
- Pixel: intelligent suggestions and restrained surfaces.
- Niagara: a focused Calm mode for low-noise use.
- Nova/Action Launcher: deep customization and power-user controls.
- Nothing OS: distinctive visual character.
- Material You: dynamic color and wallpaper-aware theming.
- macOS: a stable dock that behaves like an anchor.

The result must still feel like HavenOS, not a clone of those products.

## The new mental model: one shell, four zones

Do not treat the home screen as one freeform canvas. Treat it as a responsive shell with four coordinated zones:

1. **Atmosphere:** wallpaper, ambient type, clock, and optional identity layer.
2. **Today rail:** a compact, optional information rail for weather, agenda, battery, and quick actions.
3. **Workspace:** the actual app grid, folders, and intentional widgets.
4. **Dock:** the persistent navigation anchor shared across every page.

The zones adapt by width, but their responsibility never changes.

### Tablet landscape

Use a two-column composition:

- Left: Today rail, approximately 28–32% of the usable width.
- Right: centred workspace with a maximum content width, approximately 68–72%.
- Dock: persistent at the bottom of the workspace, never inside a page’s scroll or pager.
- Wallpaper: full-bleed behind the shell, with a readability treatment that changes locally rather than covering the whole screen with a dark veil.

Do not stretch a phone grid across the entire tablet. A centred workspace with generous margins makes the screen feel designed, not merely enlarged.

### Tablet portrait

Use a single workspace with an optional collapsible Today rail. The dock remains fixed at the bottom. The rail can become a top module or a swipe-in panel if width is insufficient.

### Phone

Use one primary workspace. Today becomes a full-height page or a bottom sheet. The dock remains persistent. Never show four dense widget cards competing with the app grid on a narrow screen.

## Home hierarchy

The home screen should answer these questions in order:

1. Where am I?
2. What needs my attention?
3. What can I open immediately?
4. How do I reach everything else?

### Layer 1: atmosphere

- Keep the user’s wallpaper fully visible where it is not behind text.
- Make the large decorative text optional. It must never be a hidden, hardcoded overlay.
- If the wallpaper is visually busy, use a soft local scrim behind labels and cards, not a blanket dark filter.
- Provide an “Identity layer” control with three states: Off, Subtle, and Statement.
- The default should be Subtle, positioned in a safe visual area and automatically moved away from functional content.

### Layer 2: Today rail

The Today rail is the strongest idea in the current concept. Keep it, but make it a composed product surface.

At the top:

- date and time context;
- weather with a clear offline state;
- one primary greeting or mode label.

Below it:

- Agenda card;
- next event or “Nothing scheduled” state;
- battery/device card;
- two or three quick actions;
- optional suggested apps.

Rules:

- The rail has a vertical rhythm and a maximum number of visible cards.
- Empty cards collapse to their minimum useful height instead of occupying large blank rectangles.
- Cards never float over the workspace.
- Cards can be reordered only through a deliberate Customize mode.
- Every card has a clear tap target and an accessible label.

### Layer 3: workspace

The workspace is a real occupancy-aware grid. It is not a freeform artboard by default.

- Every item claims one or more cells.
- Widgets have declared minimum and maximum sizes.
- A placement is either valid or visibly invalid; it is never silently overlapping.
- When an item moves, affected items push away or the user receives a clear placement alternative.
- The user can enable Freeform mode as an advanced option, but Freeform mode must still protect the dock, safe areas, and interactive hit regions.

Use deliberate density presets:

- **Calm:** fewer, larger items and more breathing room.
- **Balanced:** recommended default.
- **Dense:** more apps and smaller gaps for power users.
- **Custom:** manual rows, columns, margins, and item size.

The visual grid should not be permanently visible. Show a subtle grid overlay only while dragging or changing density.

## Dock: the non-negotiable anchor

The dock must be a persistent shell component, not a separate copy rendered per page.

### Dock behavior

- One dock configuration across all home pages by default.
- Optional page-specific overrides are allowed only as an explicit advanced feature.
- 4–8 primary slots on phones; 5–10 on tablets depending on width.
- One fixed optional action slot for Search or Library.
- Stable horizontal position and stable icon order.
- Dock never changes from light to dark unpredictably between pages.
- A local contrast surface or adaptive icon treatment ensures every icon remains readable.
- Dock remains above the wallpaper, widgets, Today content, and page transitions.
- Dock can be compact, floating, or attached, but the chosen treatment is consistent until changed.

### Dock visual design

Use a tonal surface rather than a heavy outlined pill:

- translucent material with a restrained blur;
- no bright border around every icon;
- 16–24dp internal padding;
- consistent icon optical size;
- subtle active indicator under the current context;
- optional hover/magnification only on supported pointer devices.

The dock should feel like an island that holds navigation, not another widget card.

## Library and search

Search and Library should be one coherent system with two entry points:

- Search is for intent: “open WhatsApp,” “bigger icons,” “weather,” or “settings.”
- Library is for browsing: All apps, Categories, Recent, Frequent, and custom groups.

### Search behavior

- Search opens from the dock, a gesture, or the configured home search affordance.
- Suggestions must be vertically stacked with reserved height; never paint over one another.
- Search results can show a direct action, for example “Icon size — 72%,” without forcing the user through multiple screens.
- Settings actions need a clear “Change setting” affordance and confirmation when the action has broad impact.
- Search must have loading, no-results, offline, and permission-denied states.

### Library behavior

- All, Categories, and List are views of the same app source.
- The scrubber must have a larger touch target and a visible current-letter bubble.
- Favorites and A–Z content must align to the content container, not hug the extreme screen edge.
- The Library never changes dock configuration.

## Widgets: useful, calm, and spatially honest

Widgets should feel like first-class objects with predictable contracts.

### Widget contract

Each widget declares:

- minimum width and height;
- preferred width and height;
- supported breakpoints;
- refresh behavior;
- permissions required;
- offline state;
- empty state;
- loading state;
- interaction priority;
- whether it may be resized.

### Widget placement rules

1. Preview the new size before committing.
2. Show occupied cells during placement.
3. Push other items when possible.
4. If there is no valid placement, explain why and offer a smaller size or another page.
5. Never cover the dock or make another interactive item unreachable.
6. Allow “Send to Today” as a deliberate destination for status widgets.

### Widget card design

- Use one shared card language: radius, padding, title placement, and action treatment.
- Do not put a giant empty black rectangle around a tiny empty state.
- Use a short title, one useful fact, and one primary action.
- “No events today” should look intentional and calm, with a small Add event action.
- Weather should show its data age and an offline message when needed.
- Cards should use local contrast adaptation against the wallpaper.

## Folders

Folders are mini-workspaces, not tiny app drawers.

- Preview modes: Compact grid, Stack, and Large folder.
- Large folders may occupy a defined 2×2 or 2×3 region and must use the same occupancy engine as widgets.
- Opening a folder should expand from its location into a stable surface.
- Folder contents use the same icon optical size and label rules as the home grid.
- A folder title is optional, but if enabled it should have a consistent baseline.
- Folder background inherits the current material and accent instead of introducing a random new card style.

## Customize mode: the major interaction improvement

The current concept has good intentions but exposes too many controls at once. Replace ad hoc edit controls with a clear two-level model.

### Level 1: quick customize

Long-press empty space or use the home menu to open a compact action bar:

- Wallpaper
- Layout
- Widgets
- Dock
- Style
- Home settings

The home remains visible. The user is editing the real composition, not entering a disconnected form screen.

### Level 2: focused edit mode

When an object is selected:

- show a clear selection ring and name;
- show only actions relevant to that object;
- provide Move, Resize, Replace, Remove, and Settings;
- place rare actions under More;
- show a Before/After hold interaction for visual changes;
- show Apply and Undo only when the change affects other objects.

### Layout preview

For grid or density changes, show:

- current state;
- proposed state;
- ghost positions for moved items;
- overflow count;
- destination pages;
- one-tap Undo.

Example:

> Changing from 5×6 to 4×6 will move 7 apps to Page 2. Preview layout · Apply · Cancel

Do not expose implementation concepts such as `[L]`, `[M]`, `[A]`, or `[–]` in the interface. Those are builder notes, not product language.

## Motion language

Motion should explain spatial relationships.

- Today rail slides from the edge it occupies.
- A folder expands from its icon.
- A widget resize shows the cells it will claim.
- A dock stays spatially stable while pages move behind it.
- Page transitions use one consistent curve and duration.
- Avoid animating large blurred surfaces and many icons simultaneously.
- Respect the system reduced-motion setting by replacing movement with fades and state emphasis.

Recommended principles:

- fast for direct manipulation;
- soft for mode changes;
- no decorative looping motion on the home screen;
- haptic feedback only at meaningful thresholds such as snap, commit, and invalid placement.

## Visual system

### Materials

Use three semantic surfaces only:

1. **Canvas:** wallpaper and ambient color.
2. **Tonal surface:** Today, dock, and widgets.
3. **Elevated surface:** menus, folder expansion, and settings sheets.

Avoid every card having its own border, shadow, and opacity. Hierarchy should come from material level, spacing, and typography.

### Typography

- Time/display typography may be expressive.
- Functional labels should use a highly legible system or chosen UI font.
- Never use the decorative wallpaper typeface for functional text.
- Use consistent baselines and line heights.
- Reserve two lines for labels when the setting allows it; never clip silently.

### Icons

- Normalize optical size, not just bounding-box size.
- Use one icon treatment for system actions.
- App icons may retain their identity, but their container, label, and spacing must be consistent.
- Test light, dark, monochrome, and low-contrast app icons.

### Color and contrast

- Accent comes from wallpaper or a chosen palette.
- Functional text always wins over accent expression.
- Apply a local label scrim or tonal chip when contrast fails.
- Do not rely on a single global dark overlay.
- Provide a high-contrast mode that keeps layout unchanged.

## Accessibility and resilience

- Minimum 48dp touch targets.
- Clear focus ring for keyboard, pointer, and TalkBack navigation.
- Meaningful content descriptions for icons and widgets.
- Large text mode reflows cards instead of clipping them.
- Landscape and portrait use different composition rules when necessary.
- Offline weather and failed data loads have useful states.
- Permission-dependent features explain the reason before requesting access.
- A broken widget cannot destabilize the rest of the home layout.

## Implementation sequence

### Phase 1 — structural correctness

1. Build the persistent shell: canvas, Today rail, workspace, dock.
2. Create one occupancy-aware layout model for icons, folders, and widgets.
3. Make grid settings real.
4. Add collision prevention, push behavior, invalid placement feedback, and undo.
5. Move the dock outside the page pager.

### Phase 2 — composition

1. Implement tablet landscape two-column behavior.
2. Add content-width constraints and adaptive margins.
3. Implement wallpaper-aware local contrast.
4. Normalize icon, folder, and card tokens.
5. Remove the ambiguous HNK overlay or make it an explicit Identity layer.

### Phase 3 — customization

1. Build quick Customize mode.
2. Build focused object editing.
3. Add layout-change preview and impact reports.
4. Add density presets and advanced custom controls.
5. Connect Settings to the live home without hiding the result.

### Phase 4 — intelligence and polish

1. Improve Search intent actions.
2. Add Library views and the scrubber.
3. Add Calm mode.
4. Add refined folder and widget transitions.
5. Tune blur, rendering, and reduced-motion performance.

## Acceptance checklist

The redesign is successful when:

- the dock is stable on every page;
- the Today rail is useful but never competes with the workspace;
- widgets push or reject instead of overlapping;
- grid settings match the actual home layout;
- the supplied wallpaper remains expressive while labels remain readable;
- settings can change the home while the user still sees the home;
- the tablet layout feels authored for landscape;
- edit mode is understandable without documentation;
- the launcher remains beautiful with zero widgets, with many widgets, and in Calm mode;
- a user can recover from every customization mistake with Undo.

## Final design principle

Do not add more surfaces until the existing surfaces have clear ownership.

Every pixel should answer one question: is this atmosphere, attention, workspace, or navigation? When those responsibilities are respected, HavenOS can be expressive and powerful without becoming visually chaotic.
