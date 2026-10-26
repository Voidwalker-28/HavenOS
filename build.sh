#!/bin/bash
# HavenOS release build — direct GradleMain, offline (wrapper script cannot
# fork a daemon in this sandbox; see AGENTS.md Styro recipe).
cd ~/workspace/havenos || exit 1
export JAVA_HOME=/home/hatch/jdk17
export ANDROID_HOME=/home/hatch/android-sdk
/home/hatch/jdk17/bin/java -Xmx3072m -XX:MaxMetaspaceSize=512m \
  -Dorg.gradle.jvmargs="-Xmx3072m -XX:MaxMetaspaceSize=512m" \
  -javaagent:/home/hatch/.gradle/wrapper/dists/gradle-9.3.1-bin/gradle-9.3.1/lib/agents/gradle-instrumentation-agent-9.3.1.jar \
  -cp /home/hatch/.gradle/wrapper/dists/gradle-9.3.1-bin/gradle-9.3.1/lib/gradle-launcher-9.3.1.jar \
  org.gradle.launcher.GradleMain :app:assembleRelease --offline --no-daemon > /tmp/build.log 2>&1
echo "GRADLE_EXIT=$?"
grep -E "^e: |error:|BUILD" /tmp/build.log | head -40
