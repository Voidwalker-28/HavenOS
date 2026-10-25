# HavenOS Website Workflow

## 01 — Evidence audit

Read the supplied brief as the product contract. Inspect the APK archive for package evidence and use the screen recording as the visual source. Extract representative full-resolution stills for the home surface, edit mode, Look settings, Home & Dock settings, and Changes dialog. Separate observed UI from claims supplied in the brief.

## 02 — Plan and sitemap

Define the positioning, page names, section order, design tokens, content boundaries, routes, and the remaining `[TODO]` placeholders. Keep the page count intentional: one narrative homepage plus dedicated Privacy and FAQ pages.

## 03 — Design system

Implement the quiet tactile editorial direction with warm paper, charcoal ink, a single mineral-violet accent, Satoshi/IBM Plex Mono typography, asymmetrical layouts, evidence-led screenshot plates, and restrained depth. Make the same tokens drive light/dark mode, buttons, tables, cards, and focus states.

## 04 — Asset preparation

Create optimized JPEG/WebP derivatives from the recorded frames. Keep descriptive filenames and alt text. Do not publish the APK or the original screen recording. Use CSS perspective on the stills for the 3D staging requested by the brief, without fake phone frames or decorative floating shapes.

## 05 — Implementation

Create a Vite + React + TypeScript static site. Add a shared shell with header/footer, page-level metadata, theme toggle, responsive section primitives, screenshot evidence components, a calm/full-control switcher, permissions table, and accessible FAQ accordions. Implement `/`, `/privacy`, and `/faq` with browser-native routing/fallback-safe links.

## 06 — SEO and static delivery

Add semantic HTML, title/description/OG tags, favicon, `robots.txt`, `sitemap.xml`, and `manus-routes.json`. Keep the download action visibly marked `[TODO]` until a verified URL exists. Configure a clean static build to `dist/`.

## 07 — QA pass

Run typecheck/build, inspect route files, verify no broken relative links, check every product claim against the brief, and inspect screenshots at 375×812, 768×1024, and 1440×900. Confirm visible focus states, contrast, keyboard behavior, reduced-motion behavior, and dark/light theme persistence.

## 08 — Handoff

Checkpoint the intended files, report the route names and the exact remaining `[TODO]` items, and provide the user the plan/workflow/sitemap files plus the website preview/project handoff.
