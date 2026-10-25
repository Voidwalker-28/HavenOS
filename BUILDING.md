# Building HavenOS

## Prerequisites

- **JDK 17** (the build targets Java 17).
- **Android SDK** with `compileSdk 36` and `build-tools 35.0.0`
  installed. Set `ANDROID_HOME` to the SDK directory.
- **Gradle 9.3.1** installed locally.
- Android 8.0 (API 26) or newer is the minimum runtime target; the
  `targetSdk` is 36.

> This repository does **not** ship a Gradle wrapper (`gradlew`). Use a
> local Gradle 9.3.1 installation. The wrapper script is intentionally
> absent: on some restricted environments the wrapper's daemon fork
> cannot communicate, and invoking `GradleMain` directly (below) is the
> reliable path there.

## Standard build

```sh
export ANDROID_HOME=/path/to/android-sdk
gradle :app:assembleRelease
```

The APK lands at `app/build/outputs/apk/release/app-release.apk`.
Release builds are minified with R8 (`isMinifyEnabled`, `isShrinkResources`).

For a debug build:

```sh
gradle :app:assembleDebug
```

Unit tests live under `app/src/test` (JUnit 4):

```sh
gradle :app:testDebugUnitTest
```

## Offline / restricted-environment build

If the Gradle daemon cannot fork on your machine (broken loopback IPC),
invoke `GradleMain` directly with matching client and daemon JVM args and
`--no-daemon --offline`:

```sh
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
$JAVA_HOME/bin/java -Xmx3072m -XX:MaxMetaspaceSize=512m \
  -Dorg.gradle.jvmargs="-Xmx3072m -XX:MaxMetaspaceSize=512m" \
  -javaagent:<gradle-9.3.1-dist>/lib/agents/gradle-instrumentation-agent-9.3.1.jar \
  -cp <gradle-9.3.1-dist>/lib/gradle-launcher-9.3.1.jar \
  org.gradle.launcher.GradleMain :app:assembleRelease --offline --no-daemon
```

The repository's `build.sh` is a parameterized version of this invocation
(environment variables override the defaults; it runs from the repo root).

## Dependency notes

- The build scripts list a `file:///…/local-maven` repository first.
  That entry points at an **optional local Maven cache** used for offline
  builds. If the directory does not exist, Gradle treats it as empty and
  resolves everything from Google Maven and Maven Central — the build
  works fully online without it.
- The Compose stack is pinned to the versions the declared material3
  version expects (Compose 1.11.0-beta02 for material3 1.5.0-alpha17).
  Do not force an older Compose UI version: the build will succeed but
  the app will crash at runtime with `NoSuchMethodError`.
- The AndroidX lifecycle group is pinned to 2.8.3, `window-core` to
  1.5.1, `savedstate` to 1.4.0 / `savedstate-ktx` to 1.3.1, and `core-ktx`
  to 1.13.1, for the same offline-cache reason. See `app/build.gradle.kts`
  for the full pin list.

## Signing (developer-supplied, never committed)

The repository contains **no keystore and no signing passwords**, on
purpose. Debug builds use Android Gradle Plugin's default debug keystore.
For a signed release build, add your own `signingConfig` locally (point
it at a keystore that lives **outside** this repository) and reference it
from the `release` build type. Never commit a `*.keystore`, `*.jks`,
`local.properties`, or any password — `.gitignore` blocks them, but the
rule is yours to keep regardless.

Published HavenOS releases are signed with the maintainer's own keystore;
installing a differently-signed build over an existing install will
require uninstalling first (Android rejects the signature mismatch).

## Naming the release APK

Before publishing, rename the built APK to `HavenOS-vc<versionCode>.apk`
(e.g. `HavenOS-vc10.apk`) so the file name itself identifies the build.
See [RELEASING.md](RELEASING.md).
