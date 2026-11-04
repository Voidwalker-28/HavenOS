# HavenOS v3 — Status (v3.0 / vc12)

3-day budget. Two parallel tracks, integrated into one build. This file feeds
the user's 5-hour updates — factual only, never invented progress.

## Scope lock (2026-10-28, user decisions)
- v3 = **mascot + dynamic island**.
- Dynamic island part still gated on the user's instructions to start —
  that gate is satisfied by the v3 GO (parallel tracks, 3-day budget).
- Mascot spec: `~/workspace/user/files/HavenOS-Mascot-Builder-Prompt.md`
  (wins over `docs/mascot-plan.md` where they differ).
- Island reference: github.com/Angel-Studio/MaterialYou-Dynamic-Island —
  license check first, behavior-study only, original implementation.
- Rules: Ponytail; fix = ideally ONE totally-correct line, max TWO lines
  when one line cannot be correct, never a forced one-liner; debugging pass
  after EACH part/phase before the next part.

## Isolation note
`~/workspace/havenos` is NOT a git repository (the git clone is
`~/workspace/havenos-github/`), so git worktrees were not possible.
Tracks run in isolated directory copies instead:
- Track A (mascot): `~/workspace/havenos-v3-track-a/`
- Track B (island): `~/workspace/havenos-v3-track-b/`
Integration into `~/workspace/havenos` is done by the coordinator only.
Neither track touches `~/workspace/havenos` directly.

## Track A — Mascot
- Status: COMPLETE (code), 2026-10-28 — verification build done, awaiting integration
- Verification build: `~/workspace/havenos-v3-track-a/HavenOS-vc12-trackA.apk` — 2,616,576 bytes, md5 `113fd917d4b4144543d74327489c5232`; aapt vc12/3.0; apksigner pass (CN=HavenOS); full release build 12m 11s
- Part: Phase 1 (engine) — DONE (code): MascotModel (state machine, roam planner, nudge limiter), MascotFigure (original vector art, Milo squarer+tuft / Mila rounder+ribbon), MascotLayer (home-only slot, pointer-transparent), MascotHub (exact existing components)
- Part: Phase 2 (Find + teach + suggest + chat) — DONE (code): MascotFind adapter over existing searchMatchBand (never a competing index), guidance via real cell rects, tutorials (offered once), suggestions (1/session, cooldown, quiet hours), 13-entry help map
- Part: Phase 3 (MascotBrain seam + ScriptedBrain; LocalLlmBrain only if clean) — DONE (seam + scripted; LLM NOT wired): candidates MediaPipe/llama.cpp/ExecuTorch evaluated in docs/MASCOT-LLM-PLAN.md, none integrates cleanly here; model URL/checksum are null placeholders, never invented; no model bundled
- Part: Phase 4 (data model only, launch pair only) — DONE: HomePlacement + character enum, Milo/Mila only
- Vertical slice: on → roams → tap → Find "Camera" → walks + points → expression → dismiss sticks — DONE (code), needs Pad verification (V3-MASCOT-1)
- Permission check (§13): DONE, no stop needed — no QUERY_ALL_PACKAGES (manifest already has <queries> + LauncherApps), no new permission, no SYSTEM_ALERT_WINDOW use, no network use
- Debug passes: Pass 1 (static) found + fixed 4 bugs (graphicsLayer self-assignment, missing import, stale drag captures, zero-size Spacer); Pass 2 (compile) fixed 2 root causes minimally (Dp/size shadowing, nonexistent firstNotNullOfNull); compile BUILD SUCCESSFUL 3m 3s
- Tests written, NOT run (pre-existing sandbox executor breakage): MascotModelTest, MascotFindTest, MascotBrainTest
- Device checks: V3-MASCOT-1..8 in track docs/DEVICE_CHECKLIST.md (to merge at integration)
- Blockers: none. Note: shared Gradle user-home lock serialized track A/B builds (track A waited its turn, twice) — no code impact
- Open questions: none for the user; 6 assumptions recorded (tutorial "offered once" = any choice consumes the offer; offers surface in bubble menu; no full-screen tap catcher by design; folder targets get bubble + Launch chip; media/call sleep detection skipped for permissions; model runtime deferred)

## Track B — Dynamic Island
- Status: COMPLETE (code), 2026-10-28 — verification build done, awaiting integration
- Verification build: `~/workspace/havenos-v3-track-b/HavenOS-vc12-trackB.apk` — 2,583,012 bytes, md5 `e6e68aaf69333f81da8998490d9d3471`; aapt vc12/3.0; apksigner pass (CN=HavenOS); full release build 10m 57s
- License finding (checked FIRST): reference repo (HEAD `91dc9a1`) contains NO LICENSE file and no license headers — README says only "Now open-sourced !". Safe reading applied: all-rights-reserved for reuse, so NOTHING was copied (no code, assets, Lottie, strings, branding); behavior only, reimplemented from scratch. No attribution required by any license found; courtesy credit is an open question (Q4)
- Part: behavior study — DONE: states Closed/Open/Expanded, activities Notification > Media > Battery, overlay via accessibility service, edge cases (concurrent activities, grouped notifications, screen off)
- Part: island engine + home integration — DONE (code): IslandModel (priority + 6s transient window), IslandFeed (battery broadcast, BadgeStore counts, media via existing listener, 2s poll), IslandView (Minimal GlassSurface pill, tap expand/collapse, real MediaController controls, per-app rows that launch the app)
- Improvements over reference (named): expanded view lists ALL live activities (not just top-priority); hidden when idle (reference always draws its dot); notification transient 6s (stale counts don't keep it lit); no new permissions/service; reduced-motion snap path
- Stopped pieces (reported, not added): timers from other apps (unobservable), calls (phone permissions), notification reply/delete (listener keeps counts only — island launches the app instead)
- System-wide overlay: NOT built — single most useful next step (manifest already declares SYSTEM_ALERT_WINDOW + accessibility service exists, so no new permission needed)
- Debug passes: compile failure 1 (Settings.kt parse split the githubUsername chain — fixed by restoring the chain, islandOn as its own arg, 2 lines moved); failure 2 (anim type inference — 1 line explicit type); compile green 3m 15s; review found no further failures
- Device checks: V3-ISLAND-1..9 in track docs/DEVICE_CHECKLIST.md (to merge at integration)
- Open questions (not guessed): Q1 idle = hidden (chosen) vs always-visible dot (one-line flip); Q2 transient window 6s; Q3 home-only first vs overlay next; Q4 courtesy credit; Q5 media poll 2s vs callback-driven later

## Integration (coordinator)
- Status: COMPLETE (2026-10-28) — both tracks merged, final build verified
- Final build: `~/workspace/havenos/HavenOS-vc12.apk` — 2,632,960 bytes, md5 `cbe84ad33c307823dfb600419d0e3bc8`, built Oct 28 16:40 UTC; aapt: package app.havenos.launcher, versionCode 12, versionName 3.0, minSdk 26, targetSdk 36; apksigner pass, signer CN=HavenOS, SHA-256 `53bff2a09aedb516a8ee6193234a7aaf5d04ce901fb8931b799191b9845354a8` (matches havenos keystore); build.sh BUILD SUCCESSFUL in 10m 27s
- Dex/resource proof both tracks shipped: `mascot_on` + `Dynamic Island` in classes.dex; `Milo`/`Mila` in resources.arsc
- Merge method: `git merge-file` (3-way, base = pre-track tree) for Settings.kt, Home.kt, build.gradle.kts, DEVICE_CHECKLIST.md; single-track files copied directly (PagedHome, HubScreen, HomeDockHub, strings.xml); new dirs copied (ui/mascot, ui/island, mascot tests, MASCOT-LLM-PLAN.md). NOT copied: track build.sh fixes (main build.sh already points at havenos), .gradle state, APKs, track status files
- Merge incident + fix (honest record): first merge-file run used the wrong argument order (base/track-A swapped), which silently dropped Track A content from all four files. Caught by the diff-verification step (merged-vs-A must show ONLY island additions and vice versa), redone with correct order, and re-verified: Settings.kt merged = A (1142 lines) + B's 11 island lines; Home.kt clean with both hooks; build.gradle.kts vc12/3.0; checklist has all 17 V3 entries. A line-based resolver replaced the regex resolver after it truncated a file mid-edit (caught by line-count check before anything was installed)
- Post-install diff: `~/workspace/havenos` differs from each track ONLY by the other track's additions; no track-dir references in merged source; no conflict markers
- NOT uploaded, published, or committed to GitHub — parent owns delivery

## Open questions (materially-product-changing — do not guess)
- None recorded yet.
