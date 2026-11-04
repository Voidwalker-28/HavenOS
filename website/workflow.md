# HavenOS Website Workflow

## 01 — Evidence audit

Read the supplied brief as the product contract. Inspect the APK archive for package evidence and use the screen recording as the visual source. Extract representative full-resolution stills for the home surface, edit mode, Look settings, Home & Dock settings, and Changes dialog. Separate observed UI from claims supplied in the brief.

## 02 — Plan and sitemap

Define two intentionally separate experiences: `/` is the generated-light landing page from the first reference; `/product` is the glass/depth product page from the second reference; `/privacy` and `/faq` remain dedicated reading pages. Keep the visual systems separate rather than blending them.

## 03 — Design system

Implement the landing system with near-black ground, indigo generated ribbons, Inter/JetBrains Mono, hairlines, explicit track counts, independent canvas animations, and square controls. Keep the product route on its own dark/light glass system with Inter, emerald highlights, 20px blur, rounded containers, and the supplied recording/stills as evidence.

## 04 — Asset preparation

Create optimized JPEG/WebP derivatives from the recorded frames. Keep descriptive filenames and alt text. Do not publish the APK or the original screen recording. Use the recording only on `/product`; the landing route uses generated canvas light and no bitmap hero.

## 05 — Implementation

Create a Vite + React + TypeScript static site. Add route-aware metadata, a generated-light landing page with intro stage/ribbon/spectrum/FAQ canvases, a separate glass/depth product page with screenshot evidence, shared privacy/FAQ pages, accessible controls, and fallback-safe links for `/`, `/product`, `/privacy`, and `/faq`.

## 06 — SEO and static delivery

Add semantic HTML, title/description/OG tags, favicon, `robots.txt`, `sitemap.xml`, and `manus-routes.json`. Use the verified GitHub APK release URL in all download actions. Configure a clean static build to `dist/`.

## 07 — QA pass

Run typecheck/build, inspect route files, verify no broken relative links, check every product claim against the brief, and inspect screenshots at 375×812, 768×1024, and 1440×900. Confirm visible focus states, contrast, keyboard behavior, reduced-motion behavior, and dark/light theme persistence.

## 08 — Handoff

Checkpoint the intended files, report the route names and the exact remaining `[TODO]` items, and provide the user the plan/workflow/sitemap files plus the website preview/project handoff.
