#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/../../.." && pwd)"
ANDROID_PROJECT_DIR="$REPO_ROOT/mobile/android-app"
GRADLEW="$REPO_ROOT/gradlew"
DEFAULT_API_BASE_URL="http://10.0.2.2:8080"
API_BASE_URL="${RIVERKING_API_BASE_URL:-$DEFAULT_API_BASE_URL}"

usage() {
    cat <<'EOF'
Usage:
  mobile/android-app/scripts/build-android.sh <target> [extra gradle args...]

Targets:
  direct-debug-apk     Build direct debug APK
  direct-release-apk   Build direct release APK
  play-debug-apk       Build play debug APK
  play-release-apk     Build play release APK
  play-release-aab     Build play release AAB
  debug-apks           Build both debug APKs
  release-artifacts    Build direct release APK + play release AAB

Aliases:
  direct-debug         -> direct-debug-apk
  direct-release       -> direct-release-apk
  play-debug           -> play-debug-apk
  play-release         -> play-release-apk
  play-bundle          -> play-release-aab
  debug-all            -> debug-apks
  release-all          -> release-artifacts

Environment:
  RIVERKING_API_BASE_URL
  RIVERKING_GOOGLE_AUTH_CLIENT_ID
  RIVERKING_SIGNING_STORE_FILE
  RIVERKING_SIGNING_STORE_PASSWORD
  RIVERKING_SIGNING_KEY_ALIAS
  RIVERKING_SIGNING_KEY_PASSWORD

Examples:
  mobile/android-app/scripts/build-android.sh direct-debug-apk
  RIVERKING_API_BASE_URL=http://10.0.2.2:8080 mobile/android-app/scripts/build-android.sh release-artifacts --stacktrace
EOF
}

die() {
    echo "error: $*" >&2
    exit 1
}

if [[ ! -x "$GRADLEW" ]]; then
    die "gradle wrapper not found at $GRADLEW"
fi

target="${1:-help}"
if [[ $# -gt 0 ]]; then
    shift
fi

declare -a tasks=()
declare -a artifacts=()
release_build=false

case "$target" in
    help|-h|--help)
        usage
        exit 0
        ;;
    direct-debug|direct-debug-apk)
        tasks=( ":app:assembleDirectDebug" )
        artifacts=( "$ANDROID_PROJECT_DIR/app/build/outputs/apk/direct/debug/app-direct-debug.apk" )
        ;;
    direct-release|direct-release-apk)
        tasks=( ":app:assembleDirectRelease" )
        artifacts=( "$ANDROID_PROJECT_DIR/app/build/outputs/apk/direct/release/app-direct-release.apk" )
        release_build=true
        ;;
    play-debug|play-debug-apk)
        tasks=( ":app:assemblePlayDebug" )
        artifacts=( "$ANDROID_PROJECT_DIR/app/build/outputs/apk/play/debug/app-play-debug.apk" )
        ;;
    play-release|play-release-apk)
        tasks=( ":app:assemblePlayRelease" )
        artifacts=( "$ANDROID_PROJECT_DIR/app/build/outputs/apk/play/release/app-play-release.apk" )
        release_build=true
        ;;
    play-bundle|play-release-aab)
        tasks=( ":app:bundlePlayRelease" )
        artifacts=( "$ANDROID_PROJECT_DIR/app/build/outputs/bundle/playRelease/app-play-release.aab" )
        release_build=true
        ;;
    debug-all|debug-apks)
        tasks=( ":app:assembleDirectDebug" ":app:assemblePlayDebug" )
        artifacts=(
            "$ANDROID_PROJECT_DIR/app/build/outputs/apk/direct/debug/app-direct-debug.apk"
            "$ANDROID_PROJECT_DIR/app/build/outputs/apk/play/debug/app-play-debug.apk"
        )
        ;;
    release-all|release-artifacts)
        tasks=( ":app:assembleDirectRelease" ":app:bundlePlayRelease" )
        artifacts=(
            "$ANDROID_PROJECT_DIR/app/build/outputs/apk/direct/release/app-direct-release.apk"
            "$ANDROID_PROJECT_DIR/app/build/outputs/bundle/playRelease/app-play-release.aab"
        )
        release_build=true
        ;;
    *)
        usage >&2
        exit 1
        ;;
esac

declare -a gradle_args=(
    -p "$ANDROID_PROJECT_DIR"
)

for task in "${tasks[@]}"; do
    gradle_args+=( "$task" )
done

gradle_args+=( "-PRIVERKING_API_BASE_URL=$API_BASE_URL" )

if [[ -n "${RIVERKING_GOOGLE_AUTH_CLIENT_ID:-}" ]]; then
    gradle_args+=( "-PRIVERKING_GOOGLE_AUTH_CLIENT_ID=$RIVERKING_GOOGLE_AUTH_CLIENT_ID" )
fi

for property_name in \
    RIVERKING_SIGNING_STORE_FILE \
    RIVERKING_SIGNING_STORE_PASSWORD \
    RIVERKING_SIGNING_KEY_ALIAS \
    RIVERKING_SIGNING_KEY_PASSWORD; do
    property_value="${!property_name:-}"
    if [[ -n "$property_value" ]]; then
        gradle_args+=( "-P${property_name}=$property_value" )
    fi
done

if [[ "$release_build" == true ]]; then
    missing_signing=0
    for property_name in \
        RIVERKING_SIGNING_STORE_FILE \
        RIVERKING_SIGNING_STORE_PASSWORD \
        RIVERKING_SIGNING_KEY_ALIAS \
        RIVERKING_SIGNING_KEY_PASSWORD; do
        if [[ -z "${!property_name:-}" ]]; then
            missing_signing=1
            break
        fi
    done
    if [[ "$missing_signing" -eq 1 ]]; then
        echo "warning: release build will fall back to debug signing because release signing env vars are not fully set" >&2
    fi
fi

if [[ $# -gt 0 ]]; then
    gradle_args+=( "$@" )
fi

echo "==> RiverKing Android build"
echo "target: $target"
echo "api base url: $API_BASE_URL"
echo "tasks: ${tasks[*]}"

"$GRADLEW" "${gradle_args[@]}"

echo
echo "Artifacts:"
for artifact in "${artifacts[@]}"; do
    if [[ -f "$artifact" ]]; then
        echo "  $artifact"
    else
        echo "  missing: $artifact" >&2
    fi
done
