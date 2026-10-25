# HavenOS Website Plan

## Product and intent

Build a production-quality, static marketing website for HavenOS, an Android home-screen launcher for people who want a calmer home screen without giving up control. The site uses the supplied screen recording as the source for product imagery and the supplied brief as the only source of product claims. Any unknown fact stays visibly marked as `[TODO]`.

## Design read

Reading this as: a consumer Android launcher landing site for tweak-minded users, with a quiet tactile/editorial language, leaning toward warm paper, ink, mineral violet, and restrained product depth.

### Design directions considered

- Quiet tactile editorial — 0.08 probability: warm paper, ink-heavy type, measured asymmetry, product screenshots treated like printed plates.
- Dark utility instrument — 0.06 probability: near-black canvas, compact labels, monospace accents, settings-led storytelling.
- Modular gallery — 0.04 probability: workbench-like panels, larger crops, variable rhythm, more visual than explanatory.

Chosen direction: **quiet tactile editorial**.

### Design system

- **Design movement:** quiet tactile editorial with Android-native product evidence and print-like spacing.
- **Core principles:** calm before clever; show the actual interface; make control feel legible; explain trade-offs without sales fog.
- **Color philosophy:** warm off-white and charcoal create a reading surface; one mineral-violet accent echoes the visible HavenOS settings UI without turning into a gradient or glow system.
- **Tokens:** paper `#F3F0EA`, ink `#171719`, muted ink `#716F6B`, line `#D9D3CB`, accent `#7B4DFF`, accent-dark `#5B2CCF`, surface-dark `#141416`, surface-mid `#242328`, light text `#F7F5F1`.
- **Typography:** Satoshi for display and body, IBM Plex Mono for labels, metadata, permissions, and app facts. Two families max, both self-hosted through local CSS/font import fallback.
- **Layout paradigm:** editorial asymmetry; wide hero split, horizontal evidence rail, bento crops for feature proof, and a table-led permissions section. Avoid repeated three-card grids.
- **Signature elements:** a small HavenOS mark made from two offset rounded bars; violet “control marks” on section labels; screenshot plates with a gentle perspective tilt and paper-like shadow.
- **Interaction philosophy:** interactions should clarify state: tabs reveal the alternate Calm/full-control framing, the screenshot rail advances evidence, accordions expose detail, and the theme toggle changes the reading surface.
- **Animation:** short opacity/translate reveals and subtle screenshot drift. No looping spectacle. Disable transforms and reduce motion under `prefers-reduced-motion`.
- **Brand essence:** A configurable Android home that helps you see less, choose more. Personality: quiet, exact, grounded.
- **Brand voice:** direct, second person, specific. Example lines: “Keep the home you built. Keep a way back.” / “Choose the controls you want to notice.”
- **Wordmark and logo:** wordmark in a compact lower-case lockup with the offset-bar mark; no invented app icon asset.
- **Signature brand color:** mineral violet `#7B4DFF`.

## Scope

### Routes

1. `/` — **HavenOS — The calm launcher**
2. `/privacy` — **HavenOS Privacy — Permissions in plain words**
3. `/faq` — **HavenOS FAQ — Answers before you install**

The homepage contains the full narrative and anchors to: `#calm-control`, `#home`, `#today-calm`, `#drawer-search`, `#gestures`, `#style`, `#safety-net`, `#permissions`, and `#faq`.

### Product facts to communicate

Use only the supplied facts: customisable, tablet-aware Android launcher; home pages/grid/folders/stacks/widgets/dock/lock; layout snapshots and restore tools; Today column and schedulable Calm mode; drawer/search including hidden apps with biometric lock and typed sums; gestures and sensitivity controls; styling controls; Open-Meteo weather; notification badges and Now Playing through notification access; plain-language permissions; package `app.havenos.launcher`.

### Explicit `[TODO]` items

- Tagline
- Price
- License
- Download URL
- Minimum Android version
- Developer name
- Version

## Content architecture

### Homepage narrative

1. Header: HavenOS wordmark, section links, theme toggle, `Download APK` CTA marked `[TODO]` until a URL exists.
2. Hero: sharp headline, one-line subhead, one primary download CTA, and the real home-screen screenshot presented as a tilted product plate.
3. Calm vs full control: two-state comparison with the supplied home/edit/settings evidence.
4. Home and dock: density, snap-to-grid, folders, stacks, widgets, dock behavior, lock layout.
5. Today & Calm: date, weather, events, now playing, and up to eight chosen favorites.
6. Drawer & Search: library, sorting, hidden apps with biometric lock, one search surface, typed sums.
7. Gestures & motion: icon/edge swipes, double tap, pinch, two-finger down, swipe-up drawer, haptics, sensitivity.
8. Look: blurred-glass or flat surfaces, themed icons, icon packs, shapes, radius, wallpaper controls, custom hex, reduced motion.
9. Undo anything: layout snapshots, revert session, restore layout, profiles with schedules, backup export/import.
10. Permissions: concise table with why and optionality.
11. FAQ preview: links into `/faq`.
12. Footer: package name, `[TODO]` fields, route links, and claim boundary.

### Privacy page

A focused explanation of permissions and data-related surfaces. It includes an intro, a plain-language permissions table, the optional ADB nav-bar grant note, the package name, and a `[TODO]` block for the privacy policy owner/developer name and effective date.

### FAQ page

Answers to what HavenOS is, who it is for, what Calm mode does, how search works, what permissions are for, what the safety net does, how weather and Now Playing work, and which facts remain `[TODO]`. No invented compatibility, pricing, license, or download promises.

## Asset approach

- Use full-resolution frames extracted from the supplied recording for hero, editor, settings, and change-history plates.
- Use CSS perspective and layered shadows for a restrained 3D device/mockup presentation.
- Do not create generic phones, stock images, fake UI screenshots, or decorative 3D shapes.
- Give every screenshot a meaningful alt attribute describing the observed screen.
- Keep the original APK and video outside the public website; publish only optimized stills derived from the recording.

## Technical approach

- Plain Vite + React + TypeScript, with CSS variables for the token system and no server/database.
- Semantic HTML landmarks, responsive CSS Grid, accessible buttons/links, visible focus rings, `aria-expanded` for FAQ items, and `prefers-color-scheme` plus an explicit theme toggle.
- Use a tiny local state layer for theme, calm/full-control framing, and FAQ accordion; no heavy animation dependency unless the starter requires it.
- Include `public/manus-routes.json`, `sitemap.xml`, `robots.txt`, Open Graph metadata, favicon, and a static build output under `dist/`.
- Use relative browser-facing URLs only.

## Evidence notes

Observed from the supplied recording: dark home surface, “Good evening”, date, 19:00/19:01 time cards, weather `80°F · Clear · Delhi`, `Next events`, `Battery 69%`, `Suggested`, app icon clusters, `Edit home` controls with Undo/Redo/Snap to grid/Add/Pages/Wallpaper/Look/Calm/Settings/Done, settings categories including Look and Home & Dock, `Minimal`, `Dark · Frosted · Auto`, accent palette controls, Light/Dark/Auto modes, material choices, and a visible Changes dialog with Revert/Revert session/Done. Do not treat other visible labels or inferred behavior as confirmed product facts unless they also appear in the supplied brief.

## Deliverable quality bar

The build should be fast, readable, and visually specific at 375px, 768px, and 1440px. Links and CTAs must resolve. Every claim must map to the brief or an observed recording detail. The final handoff must list all remaining `[TODO]` items.
