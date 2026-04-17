#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/../../.." && pwd)"
ANDROID_PROJECT_DIR="$REPO_ROOT/mobile/android-app"
GRADLEW="$REPO_ROOT/gradlew"
BUILD_PROFILE="${RIVERKING_BUILD_PROFILE:-}"
PROFILE_FILE=""

usage() {
    cat <<'EOF'
Usage:
  mobile/android-app/scripts/build-android.sh [--profile <name>] <target> [extra gradle args...]

Targets:
  direct-debug-apk     Build direct debug APK
  direct-debug-install Build and install direct debug APK on a chosen Android device
  qa-release-apks      Build non-canonical direct + play release APKs for internal QA
  direct-release-apk   Build direct release APK
  play-debug-apk       Build play debug APK
  play-debug-install   Build and install play debug APK on a chosen Android device
  play-release-apk     Build play release APK
  play-release-aab     Build play release AAB
  debug-apks           Build both debug APKs
  release-artifacts    Build direct release APK + play release AAB

Aliases:
  direct-debug         -> direct-debug-apk
  install-direct-debug -> direct-debug-install
  qa-release           -> qa-release-apks
  internal-release     -> qa-release-apks
  staging-release      -> qa-release-apks
  direct-release       -> direct-release-apk
  play-debug           -> play-debug-apk
  install-play-debug   -> play-debug-install
  play-release         -> play-release-apk
  play-bundle          -> play-release-aab
  debug-all            -> debug-apks
  release-all          -> release-artifacts

Environment:
  RIVERKING_BUILD_PROFILE
  ANDROID_SERIAL / ADB_SERIAL / RIVERKING_ANDROID_SERIAL
  RIVERKING_API_BASE_URL
  RIVERKING_PUBLIC_WEB_URL
  RIVERKING_ITCH_PROJECT_URL
  RIVERKING_PLAY_STORE_URL
  RIVERKING_SUPPORT_URL
  RIVERKING_PRIVACY_POLICY_URL
  RIVERKING_ACCOUNT_DELETION_URL
  RIVERKING_CANONICAL_APPLICATION_ID
  RIVERKING_VERSION_CODE
  RIVERKING_VERSION_NAME
  RIVERKING_GOOGLE_AUTH_CLIENT_ID
  RIVERKING_SIGNING_STORE_FILE
  RIVERKING_SIGNING_STORE_PASSWORD
  RIVERKING_SIGNING_KEY_ALIAS
  RIVERKING_SIGNING_KEY_PASSWORD

Examples:
  mobile/android-app/scripts/build-android.sh direct-debug-apk
  mobile/android-app/scripts/build-android.sh --profile test qa-release-apks
  mobile/android-app/scripts/build-android.sh --profile prod release-artifacts
  mobile/android-app/scripts/build-android.sh --profile test direct-debug-install
  mobile/android-app/scripts/build-android.sh direct-debug-install
  RIVERKING_API_BASE_URL=http://10.0.2.2:8080 mobile/android-app/scripts/build-android.sh release-artifacts --stacktrace
EOF
}

die() {
    echo "error: $*" >&2
    exit 1
}

has_gradle_property() {
    local property_name="$1"
    if [[ -n "${!property_name:-}" ]]; then
        return 0
    fi

    if [[ -n "$PROFILE_FILE" ]] && [[ -f "$PROFILE_FILE" ]] && grep -Eq "^[[:space:]]*${property_name}[[:space:]]*=" "$PROFILE_FILE"; then
        return 0
    fi

    local property_file
    for property_file in \
        "$ANDROID_PROJECT_DIR/gradle.properties" \
        "$HOME/.gradle/gradle.properties"; do
        if [[ -f "$property_file" ]] && grep -Eq "^[[:space:]]*${property_name}[[:space:]]*=" "$property_file"; then
            return 0
        fi
    done

    return 1
}

read_property_file_value() {
    local property_file="$1"
    local property_name="$2"
    awk -v target="$property_name" '
        /^[[:space:]]*#/ { next }
        {
            split($0, parts, "=")
            key = parts[1]
            sub(/^[[:space:]]+/, "", key)
            sub(/[[:space:]]+$/, "", key)
            if (key == target) {
                value = substr($0, index($0, "=") + 1)
                sub(/\r$/, "", value)
                print value
                exit
            }
        }
    ' "$property_file"
}

resolved_property_value() {
    local property_name="$1"
    if [[ -n "${!property_name:-}" ]]; then
        printf '%s' "${!property_name}"
        return 0
    fi

    if [[ -n "$PROFILE_FILE" ]] && [[ -f "$PROFILE_FILE" ]]; then
        local profile_value=""
        profile_value="$(read_property_file_value "$PROFILE_FILE" "$property_name")"
        if [[ -n "$profile_value" ]]; then
            printf '%s' "$profile_value"
            return 0
        fi
    fi

    return 1
}

if [[ ! -x "$GRADLEW" ]]; then
    die "gradle wrapper not found at $GRADLEW"
fi

resolve_adb() {
    if [[ -n "${ANDROID_ADB:-}" ]]; then
        echo "$ANDROID_ADB"
        return
    fi
    if command -v adb >/dev/null 2>&1; then
        command -v adb
        return
    fi
    if [[ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]]; then
        echo "$HOME/Library/Android/sdk/platform-tools/adb"
        return
    fi
    die "adb not found; set ANDROID_ADB or install Android platform-tools"
}

pick_install_serial() {
    if [[ -n "${RIVERKING_ANDROID_SERIAL:-}" ]]; then
        echo "$RIVERKING_ANDROID_SERIAL"
        return
    fi
    if [[ -n "${ANDROID_SERIAL:-}" ]]; then
        echo "$ANDROID_SERIAL"
        return
    fi
    if [[ -n "${ADB_SERIAL:-}" ]]; then
        echo "$ADB_SERIAL"
        return
    fi

    local adb_bin="$1"
    local connected_devices=()
    local emulator_devices=()
    local line
    while IFS= read -r line; do
        [[ -n "$line" ]] && connected_devices+=( "$line" )
    done < <("$adb_bin" devices | awk 'NR > 1 && $2 == "device" { print $1 }')

    if [[ "${#connected_devices[@]}" -eq 0 ]]; then
        die "no Android devices connected"
    fi

    while IFS= read -r line; do
        [[ -n "$line" ]] && emulator_devices+=( "$line" )
    done < <(printf '%s\n' "${connected_devices[@]}" | awk '/^emulator-/ { print }')

    if [[ "${#emulator_devices[@]}" -eq 1 ]]; then
        echo "${emulator_devices[0]}"
        return
    fi

    if [[ "${#connected_devices[@]}" -eq 1 ]]; then
        echo "${connected_devices[0]}"
        return
    fi

    die "multiple Android devices are connected (${connected_devices[*]}); set ANDROID_SERIAL to choose the install target"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --profile)
            [[ $# -ge 2 ]] || die "--profile requires a value"
            BUILD_PROFILE="$2"
            shift 2
            ;;
        --profile=*)
            BUILD_PROFILE="${1#*=}"
            shift
            ;;
        *)
            break
            ;;
    esac
done

if [[ -n "$BUILD_PROFILE" ]]; then
    PROFILE_FILE="$ANDROID_PROJECT_DIR/profiles/$BUILD_PROFILE.properties"
    [[ -f "$PROFILE_FILE" ]] || die "build profile '$BUILD_PROFILE' not found at $PROFILE_FILE"
fi

target="${1:-help}"
if [[ $# -gt 0 ]]; then
    shift
fi

declare -a tasks=()
declare -a artifacts=()
release_build=false
force_noncanonical_build=false
install_build=false
install_allow_downgrade=false

case "$target" in
    help|-h|--help)
        usage
        exit 0
        ;;
    direct-debug|direct-debug-apk)
        tasks=( ":app:assembleDirectDebug" )
        artifacts=( "$ANDROID_PROJECT_DIR/app/build/outputs/apk/direct/debug/app-direct-debug.apk" )
        ;;
    direct-debug-install|install-direct-debug)
        tasks=( ":app:assembleDirectDebug" )
        artifacts=( "$ANDROID_PROJECT_DIR/app/build/outputs/apk/direct/debug/app-direct-debug.apk" )
        install_build=true
        install_allow_downgrade=true
        ;;
    qa-release-apks|qa-release|internal-release|staging-release)
        tasks=( ":app:assembleDirectRelease" ":app:assemblePlayRelease" )
        artifacts=(
            "$ANDROID_PROJECT_DIR/app/build/outputs/apk/direct/release/app-direct-release.apk"
            "$ANDROID_PROJECT_DIR/app/build/outputs/apk/play/release/app-play-release.apk"
        )
        force_noncanonical_build=true
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
    play-debug-install|install-play-debug)
        tasks=( ":app:assemblePlayDebug" )
        artifacts=( "$ANDROID_PROJECT_DIR/app/build/outputs/apk/play/debug/app-play-debug.apk" )
        install_build=true
        install_allow_downgrade=true
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

for property_name in \
    RIVERKING_API_BASE_URL \
    RIVERKING_GOOGLE_AUTH_CLIENT_ID \
    RIVERKING_PUBLIC_WEB_URL \
    RIVERKING_ITCH_PROJECT_URL \
    RIVERKING_PLAY_STORE_URL \
    RIVERKING_SUPPORT_URL \
    RIVERKING_PRIVACY_POLICY_URL \
    RIVERKING_ACCOUNT_DELETION_URL \
    RIVERKING_CANONICAL_APPLICATION_ID \
    RIVERKING_VERSION_CODE \
    RIVERKING_VERSION_NAME; do
    property_value="$(resolved_property_value "$property_name" || true)"
    if [[ -n "$property_value" ]]; then
        gradle_args+=( "-P${property_name}=$property_value" )
    fi
done

for property_name in \
    RIVERKING_SIGNING_STORE_FILE \
    RIVERKING_SIGNING_STORE_PASSWORD \
    RIVERKING_SIGNING_KEY_ALIAS \
    RIVERKING_SIGNING_KEY_PASSWORD; do
    property_value="$(resolved_property_value "$property_name" || true)"
    if [[ -n "$property_value" ]]; then
        gradle_args+=( "-P${property_name}=$property_value" )
    fi
done

if [[ "$force_noncanonical_build" == true ]]; then
    gradle_args+=( "-PRIVERKING_CANONICAL_APPLICATION_ID=false" )
fi

if [[ "$release_build" == true ]]; then
    effective_canonical_application_id="${RIVERKING_CANONICAL_APPLICATION_ID:-}"
    if [[ -z "$effective_canonical_application_id" ]]; then
        gradle_args+=( "-PRIVERKING_CANONICAL_APPLICATION_ID=true" )
        effective_canonical_application_id=true
    fi
    if [[ "$effective_canonical_application_id" != "true" ]]; then
        die "release targets require RIVERKING_CANONICAL_APPLICATION_ID=true so the artifacts use the canonical store package name"
    fi
    missing_signing=0
    for property_name in \
        RIVERKING_SIGNING_STORE_FILE \
        RIVERKING_SIGNING_STORE_PASSWORD \
        RIVERKING_SIGNING_KEY_ALIAS \
        RIVERKING_SIGNING_KEY_PASSWORD; do
        if ! has_gradle_property "$property_name"; then
            missing_signing=1
            break
        fi
    done
    if [[ "$missing_signing" -eq 1 ]]; then
        die "release targets require RIVERKING_SIGNING_STORE_FILE, RIVERKING_SIGNING_STORE_PASSWORD, RIVERKING_SIGNING_KEY_ALIAS, and RIVERKING_SIGNING_KEY_PASSWORD in env or standard Gradle property files"
    fi
fi

if [[ $# -gt 0 ]]; then
    gradle_args+=( "$@" )
fi

echo "==> RiverKing Android build"
echo "target: $target"
if [[ -n "$BUILD_PROFILE" ]]; then
    echo "profile: $BUILD_PROFILE"
    echo "profile file: $PROFILE_FILE"
fi
api_base_url="$(resolved_property_value RIVERKING_API_BASE_URL || true)"
if [[ -n "$api_base_url" ]]; then
    echo "api base url: $api_base_url"
else
    echo "api base url: <from Gradle properties or module default>"
fi
echo "tasks: ${tasks[*]}"

"$GRADLEW" "${gradle_args[@]}"

if [[ "$install_build" == true ]]; then
    if [[ "${#artifacts[@]}" -ne 1 ]]; then
        die "install targets expect exactly one APK artifact"
    fi

    artifact="${artifacts[0]}"
    if [[ ! -f "$artifact" ]]; then
        die "missing APK artifact for install: $artifact"
    fi

    adb_bin="$(resolve_adb)"
    install_serial="$(pick_install_serial "$adb_bin")"

    declare -a install_args=( -s "$install_serial" install -r )
    if [[ "$install_allow_downgrade" == true ]]; then
        install_args+=( -d )
    fi
    install_args+=( "$artifact" )

    echo "install target: $install_serial"
    echo "adb: $adb_bin"
    echo "apk: $artifact"
    "$adb_bin" "${install_args[@]}"
fi

echo
echo "Artifacts:"
for artifact in "${artifacts[@]}"; do
    if [[ -f "$artifact" ]]; then
        echo "  $artifact"
    else
        echo "  missing: $artifact" >&2
    fi
done

if [[ -n "$BUILD_PROFILE" ]]; then
    dist_dir="$ANDROID_PROJECT_DIR/dist/$BUILD_PROFILE"
    mkdir -p "$dist_dir"

    echo
    echo "Profile artifacts:"
    for artifact in "${artifacts[@]}"; do
        if [[ -f "$artifact" ]]; then
            artifact_name="$(basename "$artifact")"
            artifact_stem="${artifact_name%.*}"
            artifact_ext="${artifact_name##*.}"
            profile_artifact="$dist_dir/${artifact_stem}-${BUILD_PROFILE}.${artifact_ext}"
            cp "$artifact" "$profile_artifact"
            echo "  $profile_artifact"
        fi
    done
fi
