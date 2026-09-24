#!/usr/bin/env bash
# Build both debug and release APKs for the Deepwoken Builder app.
#
# Exports the SDK/JDK locations used on this machine, then runs the Gradle
# wrapper from the android/ project. Release builds are minified (R8),
# resource-shrunk and signed with android/keystore/release.jks when
# android/keystore.properties exists (falls back to the debug key otherwise).
set -euo pipefail
cd "$(dirname "$0")"

export ANDROID_HOME="${ANDROID_HOME:-/home/yamana/android-sdk}"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk}"

echo "==> ANDROID_HOME=$ANDROID_HOME"
echo "==> JAVA_HOME=$JAVA_HOME"

cd android
./gradlew :app:assembleDebug :app:assembleRelease --console=plain "$@"

echo
echo "==> Debug:   $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
echo "==> Release: $(pwd)/app/build/outputs/apk/release/app-release.apk"