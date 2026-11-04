# HavenOS Launcher: Builder Prompt (v2.1, tablet-first, expanded)

*Source: pasted by Aurelius, 2026-10-20. App name: **HavenOS**. Package / applicationId: `app.havenos.launcher` (single source of truth — change in one place only, keep every reference in sync).*

*Note from builder: the "original prompt" with full §9–23 and §24 text, and the 4 reference images, were NOT in the workspace when this arrived. §9–23 are reconstructed from this document's own references plus previously approved decisions; the reconstruction is recorded in docs/ASSUMPTIONS.md.*

## 0. Rules for the builder (read first — non-negotiable)

- You are building **HavenOS**, a complete Android home-screen launcher (an Android *app*, never an operating system). First inspect the workspace: README, Gradle files, existing source, docs/. Continue an existing project and preserve what works; if the project is empty, scaffold it per section 4.
- Every feature in this document is **IN scope**. The milestones (section 24) set build order only; they are not permission to drop features. If a feature cannot be built, mark it **BLOCKED** in `docs/COVERAGE.md` with the exact technical reason and the fallback you shipped. Never drop a feature silently.
- **Visibility rule**: every feature must be reachable from the UI (home screen, edit mode, drawer, quick panel or Settings) **and** listed in `docs/COVERAGE.md` with its UI path, status (`done` / `partial` / `blocked`) and how it was verified. Code that nothing in the UI can reach does not count as done.
- Decide small implementation details yourself and record them in `docs/ASSUMPTIONS.md`. Ask the user only when a missing choice changes permissions, data handling, or product behavior.
- Report only checks you actually ran. Do not claim performance numbers, accessibility conformance, security review, store readiness or production readiness that you did not verify. List anything unverified in `docs/UNVERIFIED.md`.
- Do **not** publish, upload, sign with a real release key, add paid services, or add any ad, analytics or tracking SDK.
- Work milestone by milestone (section 24). After each milestone: build, run the available checks, update `docs/COVERAGE.md`, and keep the app installable.
- You cannot run the app on the user’s tablet. Anything that needs the real device goes into `docs/DEVICE_CHECKLIST.md` for the user to run and report back. Never write “works on device” without a reported result.

## 1. Product and identity

- **Name**: HavenOS (single string resource + single applicationId so it can be changed in one place).
- **Package**: `app.havenos.launcher`.
- HavenOS is a *launcher*. Never describe it as an operating system in the UI or in any user-facing documentation beyond the product name itself.
- **Core idea**: one launcher engine with many **Looks**. It fuses iOS-style polish (glass, widget stacks, floating dock, App Library feel, pull-down search), One UI density & productivity (large widgets, folder cards, tablet layouts, labeled widgets) and power-launcher control (grid, free canvas, gestures, icon packs, drawer modes, backup). The user picks a Look (Glass, Soft, Minimal, Futuristic, Classy, Retro, Premium) and a Density, then can change every token.
- **Targets**: tablets first (test class: Redmi Pad / ~10–11 inch, landscape-first). Phones receive an adaptive layout of the same app. `minSdk 26`.
- **Business rules**: all features free, no ads, no accounts, no analytics, no cloud sync in v1. The *only* network use is the optional weather feature (section 13).
- **Brand**: original mark and wordmark (section 7). Copy is short, friendly and plain. Make no claims about speed, battery or privacy that you have not measured or verified.
- **Name risk**: HavenOS may collide with existing names. Do not register or publish anything; the user will check availability.

## 2. Originality, licensing and sources (expanded)

- HavenOS is inspired by the *concepts* of existing launchers and system home screens. It must be an **original product** with its own name, code structure, visuals, copy and assets.
- **You may study the visible behavior of any existing launcher and re-implement the same user-visible features in completely original code.** That is the meaning of “make your own version”.
- You may reuse source code **only** from projects whose license explicitly permits it. Record every source, its license and exactly what you reused in `docs/LICENSES.md`, and ship the required notices.
  - Allowed starting points: your own code; the AOSP Launcher3 sources (Apache-2.0); other open-source launchers under licenses compatible with the final license of HavenOS.
  - A GPL-3.0 launcher may be reused **only** if HavenOS itself is also released under GPL-3.0; record the decision in `LICENSES.md` and ask the user before taking that step.
- **Strict prohibition**: Do **NOT** use, copy, translate, adapt, decompile or disassemble the proprietary code, resources, layouts, strings, icons, animations, theme packs, wallpapers or branding of Nova Launcher, Niagara Launcher, Smart Launcher, Samsung One UI apps, Apple apps, or any other closed-source launcher. “Make your own version” means reading the feature description in *this document* and implementing it from scratch in original code.
- Do not use Apple, Samsung, Google or other trademarks, logos or system icons in HavenOS artwork. Demo icons are generic originals; real apps always show their own installed icons.
- Fonts: bundle only open-licensed fonts (SIL OFL or equivalent) and list them in `LICENSES.md`. Bundled wallpapers and illustrations must be original or carry a license you can record.
- Third-party libraries: list each one (name, version, license, reason for inclusion) in `LICENSES.md` and in `docs/ASSUMPTIONS.md`.

## 3. Non-goals and platform limits

Keep the existing list. Do not attempt lock-screen replacement, status-bar restyling, root/Shizuku, reading other apps’ screens, accounts/cloud, etc.

## 4. Platform and stack

Kotlin + Jetpack Compose, LookTokens-driven theme, Room + DataStore + files, WorkManager, adaptive layouts, package structure as previously specified.

## 5. Launcher mechanics

HOME intent, singleTask, back never exits, edge-to-edge, gesture exclusion, LauncherApps callbacks, pin requests, work profiles, Safe Mode crash recovery, etc.

## 6. Reference layouts and starter templates

Four Starter Templates: Calm, Productive, Glass, Dense. Clock page is a normal home-page type.

## 7. Startup experience: splash → intro animation → logo → permissions → tutorial (expanded & strengthened)

**Goal**: The first time HavenOS opens it feels crafted, explains itself, asks for permissions clearly in one place, and teaches the gestures on the real screen. After that, HavenOS opens instantly like any home screen.

### When things play
- Full intro + onboarding: **first launch only** (when the user taps the HavenOS icon after install). Fully replayable from Settings → About and Help → “Replay intro” and “Replay tutorial”.
- Every later cold start: only the short system splash, then the home screen. **Never** play any intro animation, logo animation or onboarding when the user presses Home or returns to the launcher from another app. A launcher that animates on every Home press is unusable.
- Optional setting “Play intro on every cold start” — **off by default**.
- Everything is skippable at any moment (tap anywhere or a Skip button that appears after 1 second). If the process is killed mid-flow, resume at the exact same step. Store `onboardingVersion` and `currentStep` in DataStore so a major update can show a short “What’s new” card instead of the whole flow.
- Reduced motion (system animation scale = 0 or the in-app toggle): no motion, static logo + 300 ms crossfade only.

### Exact sequence (must be implemented in this order)

1. **System splash** (~0.4 s) — platform SplashScreen API with the HavenOS mark on a dark brand background. Hand off to the Compose intro with zero visible jump or flash.
2. **Intro animation** (~3 s, pure Compose, vector only, no video file) — dark canvas with a soft glow. Seven translucent glass panes (one per Look) drift in with slight blur, converge and settle into the HavenOS mark. A single light sweep crosses the mark, then the wordmark “HavenOS” fades in beneath it with one short line of copy. A final blur-through transition leads to the welcome screen. No sound by default.
3. **Logo** — original mark (suggestion: a rounded arch / doorway made of two overlapping glass shapes suggesting a safe home; any original design is fine). Deliver as: adaptive icon (foreground + background + monochrome layer for themed icons) + vector for in-app use. Open-licensed font for the wordmark. Record design decisions in `docs/BRAND.md`.
4. **Welcome screen** — logo + one sentence about what HavenOS is + two buttons: “Get started” and “Use defaults” (skips style questions but still shows the permissions screen and offers the tutorial).
5. **Style questions** (each with a live preview behind the sheet) — (a) Look — cards for all seven; (b) Density slider; (c) Wallpaper source — System wallpaper **or** HavenOS wallpaper, with live side-by-side previews of both (see section 8); (d) Starter Template; (e) Drawer mode.
6. **Permissions screen** titled “Make HavenOS yours” — one screen of cards in this exact order: Default home app → Calendar → Contacts → Notification access (badges + Now Playing) → Usage access (suggestions) → Accessibility (lock / shade / recents gestures) → Weather location (coarse or city search). Each card: icon, one-line “what this enables”, one-line “if you skip”, Allow button, “Skip for now” link. An “Allow all” button walks the user through the cards one after another. Use the correct flow for each permission type. Re-check the real grant state when the user returns. Never assume a result. Include a help card for restricted settings on sideloaded installs.
7. **Build your home** — short progress view that does real work (scan apps, categorize, apply template, place widgets). Then the finished home screen animates in with a staggered spring entrance. Widgets whose permissions were skipped are left out and replaced by a placeholder that offers to enable them.
8. **Interactive tutorial on the real home screen** (coach marks) — each step requires the user to perform the gesture. Skip-step and Skip-tutorial buttons always present. Steps: swipe up → drawer; pull down → search; long-press → edit mode; drag icon into folder; add & resize widget; open quick panel (Look + Density); switch wallpaper source; find Settings & Backup. **Practice happens on a temporary practice page that is deleted at the end.**
9. **Finish** — “You are set” card with three short tips + button to Settings. If HavenOS is not yet the default home app, show a persistent banner on the home screen and in Settings until it is. Later, one-time contextual tips appear the first time the user opens edit mode, folders or stacks (can be turned off).

### Quality bars for the flow
- Every step works with TalkBack, large fonts and a hardware keyboard. No step relies on animation to convey information.
- Every permission card correctly handles: granted / denied once / denied permanently (offer app-settings page) / revoked later.
- The intro and onboarding must **never trap the user**: Skip works everywhere, and pressing Home always leaves the app.
- Measure splash-to-welcome time and intro frame times on the tablet and record the numbers in `docs/SPIKES.md`. Do not invent numbers.

## 8. Wallpaper — System wallpaper **and** HavenOS wallpaper (both mandatory, expanded)

The user must be able to choose **either** source at any time. Switching sources never changes the layout, only the wallpaper layer and the glass quality ladder.

| Capability | System wallpaper | HavenOS wallpaper |
|---|---|---|
| Static image | Yes (OS draws it behind transparent launcher window) | Yes |
| Live wallpaper | Yes | No (static / gradient / generated only) |
| Per-panel glass blur | Only if bitmap is readable; otherwise Partial or Flat | Full |
| Per-profile / per-page | No (system wallpaper is global) | Yes |
| Parallax | Send offsets to the system while paging | Own parallax layer |
| Light / dark variants | Follows system | Separate image per mode |
| Adaptive colors | From system wallpaper colors | From the chosen image |

### System wallpaper mode
- Window is transparent (`windowShowWallpaper`). Static and live wallpapers appear natively.
- While paging, send wallpaper offsets; forward tap/drop commands for live wallpapers that support them.
- Colors: read system wallpaper colors for the dynamic palette and contrast guard; listen for changes (no storage permission needed).
- **Glass quality ladder** (detected at runtime and shown in Diagnostics):
  - **Full** — wallpaper bitmap is readable → same pipeline as HavenOS wallpaper.
  - **Partial** — Android 12+ with cross-window blur enabled → window blur-behind + tint + borders.
  - **Flat** — neither works → tinted translucent surfaces with borders and shadows.
- A live wallpaper is **never** sampled; it always receives Partial or Flat.
- Whether the bitmap is readable depends on Android version, OEM and permissions. Prove it on the real tablet (spike S5); do not assume.

### HavenOS wallpaper mode
- Launcher draws its own wallpaper layer. Sources: bundled original wallpapers, any image from the system Photo Picker, generated gradient/mesh, solid colors.
- Controls: crop, dim, blur, tint, parallax (off by default; uses motion sensors only while home screen is visible), separate dark-mode image, per-profile and optional per-page images.
- Keep a downscaled, pre-blurred copy for glass effects. Cache in memory + disk within a documented size budget; rebuild on change; free under memory pressure.

### Apply to system (optional, both modes)
- “Set as system wallpaper” and “Set as lock-screen wallpaper” buttons for the current HavenOS wallpaper (normal `SET_WALLPAPER` permission). Never change the system wallpaper without an explicit user tap + confirmation.

### Required behavior
- First-run and Settings → Wallpaper both show **live side-by-side previews** of System vs HavenOS sources.
- Default: System wallpaper if its colors can be read, otherwise HavenOS wallpaper.
- Switching sources cross-fades, keeps the layout, recomputes palette and glass level. If the system wallpaper becomes unreadable, drop to Partial/Flat automatically and explain why in Settings → Wallpaper.
- Both sources must pass the same visual and contrast tests (section 22).

## 9–23. Remaining sections

Keep all existing content from the original pr
...[truncated 1687 chars]