# HavenOS Website Plan

## Product and intent

Build a production-quality, static marketing website for HavenOS, an Android home-screen launcher for people who want a calmer home screen without giving up control. The site uses the supplied screen recording as the source for product imagery and the supplied brief as the only source of product claims. Any unknown fact stays visibly marked as `[TODO]`.

## Design read

Reading this as two intentionally separate experiences: a generated-light, near-black landing page from the first reference, followed by a glass/depth product page from the second reference. The supplied recording is evidence for the product page only; it does not define the landing-page visual system.

### Design directions considered

- Generated-light instrument — 0.08 probability: near-black ground, animated ribbons, mono furniture, explicit tracks, no bitmap hero.
- Glass depth product page — 0.06 probability: immersive evidence, rounded glass layers, emerald highlights, structured feature blocks.
- Modular gallery — 0.04 probability: workbench-like panels, larger crops, variable rhythm, more visual than explanatory.

Chosen direction: **two-route split: generated-light landing + glass depth product page**.

### Design system

- **Design movement:** generated-light landing paired with a glass/depth product narrative.
- **Core principles:** separate the visual systems; make the product mechanism visible; show actual interface evidence; explain trade-offs without sales fog.
- **Color philosophy:** landing uses near-black `#0A0A0A` with indigo generated light and no decorative chroma; product uses black/light zinc surfaces with emerald highlights and translucent glass.
- **Tokens:** landing ground `#0A0A0A`, panel `#0F0F11`, muted `#71717A`, indigo `#6366F1`, hairline `rgba(255,255,255,.10)`; product continues its independent light/dark glass tokens.
- **Typography:** Inter for display/body and JetBrains Mono for landing furniture; the product route retains Inter plus the existing editorial display treatment. Two families are used per route at most.
- **Layout paradigm:** landing uses full-screen hero, explicit 4/2/1 engine grid, spectrum field, stat row, FAQ, and centered close; product uses immersive hero, glass stat stack, feature grid, productivity block, and evidence plates.
- **Signature elements:** additive canvas ribbons and spectrum marker on landing; repository logo, glass stat cards, and recorded HavenOS UI on product.
- **Interaction philosophy:** landing makes the mechanism legible through live canvas surfaces; product uses direct section links, theme toggle, accordions, and a verified APK action.
- **Animation:** landing intro/ribbons/engine/spectrum are cancelled or made static under `prefers-reduced-motion`; product keeps restrained hover/reveal motion and honors the same preference.
- **Brand essence:** A configurable Android home that helps you see less, choose more. Personality: quiet, exact, grounded.
- **Brand voice:** direct, second person, specific. Example lines: “Keep the home you built. Keep a way back.” / “Choose the controls you want to notice.”
- **Wordmark and logo:** wordmark in a compact lower-case lockup with the offset-bar mark; no invented app icon asset.
- **Signature brand color:** landing indigo `#6366F1`; product emerald remains a separate route accent.

## Scope

### Routes

1. `/` — **HavenOS — A home screen with a way back**
2. `/product` — **HavenOS — The calm launcher**
3. `/privacy` — **HavenOS Privacy — Permissions in plain words**
4. `/faq` — **HavenOS FAQ — Answers before you install**

The landing page contains `#engine`, `#spectrum`, `#proof`, and `#questions`; the product page contains the feature anchors `#home`, `#calm-control`, `#today-calm`, `#drawer-search`, `#gestures`, `#style`, `#safety-net`, `#permissions`, and `#faq`.

### Product facts to communicate

Use only the supplied facts: customisable, tablet-aware Android launcher; home pages/grid/folders/stacks/widgets/dock/lock; layout snapshots and restore tools; Today column and schedulable Calm mode; drawer/search including hidden apps with biometric lock and typed sums; gestures and sensitivity controls; styling controls; Open-Meteo weather; notification badges and Now Playing through notification access; plain-language permissions; package `app.havenos.launcher`.

### Verified release data and remaining `[TODO]` items

The repository now verifies the download URL as `https://github.com/Voidwalker-28/HavenOS/releases/download/v1.0/HavenOS-vc10.apk`, license as MIT, minimum Android as 8.0 (API 26), and latest release as v1.0 (versionCode 10). Remaining unknowns are the tagline, price, developer name, and privacy policy effective date.

## Content architecture

### Landing narrative (`/`)

1. Intro stage: one short lowercase line that clears after roughly two seconds.
2. Full-viewport generated ribbon hero with bottom-aligned headline, metadata, lede, and two CTAs.
3. Four explicit engine tiles with distinct visual mini-animations.
4. Spectrum field with selected search band and typed-sum readout.
5. Four product facts, native FAQ rows, centered close, and mono footer.

### Product narrative (`/product`)

1. Header: HavenOS wordmark, section links, theme toggle, verified `Download APK` CTA.
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
