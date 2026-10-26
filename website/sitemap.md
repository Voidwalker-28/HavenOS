# HavenOS Sitemap

## Public pages

| Route | Page name | Purpose |
|---|---|---|
| `/` | **HavenOS — A home screen with a way back** | Generated-light landing page: product promise, animated surfaces, search spectrum, facts, FAQ, and entry CTA. |
| `/product` | **HavenOS — The calm launcher** | Glass/depth product narrative: product promise, video proof, feature themes, safety net, permissions, FAQ preview, and download CTA. |
| `/privacy` | **HavenOS Privacy — Permissions in plain words** | Focused explanation of why HavenOS asks for each Android permission and which items are optional. |
| `/faq` | **HavenOS FAQ — Answers before you install** | Direct answers to product, setup, permissions, feature, and unknown-fact questions. |

## Homepage sections and anchor names

| Anchor | Section name | Primary job |
|---|---|---|
| `#home` | **The home you can undo** | Hero and product proof from the real home screen. |
| `#calm-control` | **Calm when you want it. Control when you need it.** | Frame the central trade-off without inventing benefits. |
| `#today-calm` | **Your day, without the noise** | Today column and schedulable Calm mode. |
| `#drawer-search` | **One drawer. One search.** | Library, sorting, hidden apps, unified search, and typed sums. |
| `#gestures` | **Make the edges useful** | Gesture vocabulary, haptics, and sensitivity controls. |
| `#style` | **Make the surface yours** | Look, icon, wallpaper, motion, and color controls. |
| `#safety-net` | **Keep a way back** | Snapshots, undo/revert, profiles, and backup export/import. |
| `#permissions` | **Permissions, plainly** | Trust-led table with why/optional status and a Privacy link. |
| `#faq` | **Before you install** | Short FAQ preview and full FAQ route. |

## Global navigation names

- **Landing** → `/`
- **Product** → `/product#home`
- **Calm vs control** → `/product#calm-control`
- **Safety net** → `/product#safety-net`
- **Permissions** → `/privacy`
- **FAQ** → `/faq`
- **Download APK** → `https://github.com/Voidwalker-28/HavenOS/releases/download/v1.0/HavenOS-vc10.apk`

## SEO / crawler files

- `/manus-routes.json` — route manifest for the managed preview/runtime.
- `/sitemap.xml` — public XML sitemap for the four public routes.
- `/robots.txt` — permits public crawling and points to the sitemap.
