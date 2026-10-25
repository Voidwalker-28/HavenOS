#!/bin/bash
# HavenOS release build.
# Standard path: a local Gradle 9.3.1 installation.
# Restricted-environment path: invoke GradleMain directly (the wrapper's
# daemon fork cannot communicate on machines with broken loopback IPC).
# Set JAVA_HOME, ANDROID_HOME, and GRADLE_DIST; defaults suit a typical
# install. Runs from the repository root.
set -e
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
if [ -n "$GRADLE_DIST" ]; then
  "$JAVA_HOME/bin/java" -Xmx3072m -XX:MaxMetaspaceSize=512m \
    -Dorg.gradle.jvmargs="-Xmx3072m -XX:MaxMetaspaceSize=512m" \
    -javaagent:"$GRADLE_DIST/lib/agents/gradle-instrumentation-agent-9.3.1.jar" \
    -cp "$GRADLE_DIST/lib/gradle-launcher-9.3.1.jar" \
    org.gradle.launcher.GradleMain :app:assembleRelease --offline --no-daemon
else
  gradle :app:assembleRelease
fi
echo "APK: app/build/outputs/apk/release/app-release.apk"
echo "Rename before publishing: HavenOS-vc<versionCode>.apk (see RELEASING.md)"
