#!/bin/bash
set -e
echo "Building River King Admin App..."
cd "$(dirname "$0")"

# Compile and package Release APK
./gradlew :app:assembleRelease

echo "Build complete. APK is located at: app/build/outputs/apk/release/app-release-unsigned.apk"
echo "Note: The CI/CD pipeline signs the APK automatically, but you can configure gradle to sign it locally using admin.jks."
