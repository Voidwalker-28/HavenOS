# Releasing HavenOS

This is the standing process for every HavenOS version, so the repository,
its docs, and its Releases never drift apart.

## Version convention

- `versionName` marks the major line (1.0, 2.0, 3.0, 3.1, 3.1.1, …).
- `versionCode` increments by 1 per release (10 for v1.0, 11 for 2.0,
  12 for 3.0, 13 for 3.1, 14 for 3.1.1, and so on), so builds stay
  distinguishable by file name and by install.
- Tags are `vX.Y` or `vX.Y.Z` matching the `versionName` (e.g.
  `v3.1.1`) — the tag marks the release commit; the `versionCode` is
  what distinguishes builds.

## Release checklist (every version)

1. **Code.** Land the changes on `main`. `versionCode` bumped in
   `app/build.gradle.kts`.
2. **CHANGELOG.md.** New entry at the top: version, date, versionCode,
   what changed, and any known limitations. Keep it factual.
3. **README.md.** Version badge and "Latest release" line updated.
4. **Docs.** `docs/` updated for any behavior change (COVERAGE,
   ARCHITECTURE, LICENSES as applicable). Nothing user-visible ships
   undocumented.
5. **Build.** `gradle :app:assembleRelease` (see [BUILDING.md](BUILDING.md)).
   Verify the APK: package `app.havenos.launcher`, versionCode, signature,
   byte size, and md5.
6. **Rename.** Rename the built APK to `HavenOS-vc<versionCode>.apk`
   before publishing.
7. **Tag.** `git tag vX.Y.Z && git push origin vX.Y.Z`.
8. **GitHub Release.** Create a Release named after the tag with the
   APK attached and release notes matching the CHANGELOG entry. The APK
   goes on the Release, never into the repository tree.
9. **Deliver first.** The APK reaches users before any other publication
   step is treated as done.

## Fixes to an existing release

A fix to a shipped version swaps the fixed APK into the existing Release
in place — do not mint a new version number for a fix. If the APK file
changes, its byte size and md5 change with it; quote the new values
wherever the build is announced.
