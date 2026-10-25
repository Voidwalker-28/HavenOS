# HavenOS marketing website

This directory contains the static HavenOS marketing site alongside the Android launcher source. The Android app remains at the repository root; the website is intentionally isolated under `website/`.

## Run locally

```sh
cd website
pnpm install
pnpm dev
```

## Build

```sh
cd website
pnpm build
```

The site includes the following routes:

- `/` — HavenOS — The calm launcher
- `/privacy` — HavenOS Privacy — Permissions in plain words
- `/faq` — HavenOS FAQ — Answers before you install

The homepage uses a compressed loop derived from the supplied HavenOS screen recording at `public/assets/havenos-hero.mp4`, plus real UI stills from the same recording. Unknown release information remains marked `[TODO]` rather than being guessed.
