#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/../../.." && pwd)"
ANDROID_PROJECT_DIR="$REPO_ROOT/mobile/android-app"
GRADLEW="$REPO_ROOT/gradlew"
BUILD_PROFILE="${RIVERKING_BUILD_PROFILE:-}"
PROFILE_FILE=""
VERSION_FILE="$ANDROID_PROJECT_DIR/version.properties"
CURRENT_GIT_BRANCH=""

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
  RIVERKING_SKIP_BRANCH_PROFILE_GUARD
  RIVERKING_TEST_BUILD_NUMBER
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

resolve_git_branch() {
    if [[ -n "${RIVERKING_GIT_BRANCH:-}" ]]; then
        printf '%s' "$RIVERKING_GIT_BRANCH"
        return 0
    fi

    if [[ -n "${GITHUB_HEAD_REF:-}" ]]; then
        printf '%s' "$GITHUB_HEAD_REF"
        return 0
    fi

    if [[ -n "${GITHUB_REF_NAME:-}" ]]; then
        printf '%s' "$GITHUB_REF_NAME"
        return 0
    fi

    git -C "$REPO_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || true
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

tracked_version_value() {
    local property_name="$1"
    if [[ ! -f "$VERSION_FILE" ]]; then
        return 1
    fi

    local version_value=""
    version_value="$(read_property_file_value "$VERSION_FILE" "$property_name")"
    if [[ -n "$version_value" ]]; then
        printf '%s' "$version_value"
        return 0
    fi

    return 1
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

resolved_version_property_value() {
    local property_name="$1"
    local property_value=""

    property_value="$(resolved_property_value "$property_name" || true)"
    if [[ -n "$property_value" ]]; then
        printf '%s' "$property_value"
        return 0
    fi

    property_value="$(tracked_version_value "$property_name" || true)"
    if [[ -n "$property_value" ]]; then
        printf '%s' "$property_value"
        return 0
    fi

    local property_file
    for property_file in \
        "$ANDROID_PROJECT_DIR/gradle.properties" \
        "$HOME/.gradle/gradle.properties"; do
        if [[ -f "$property_file" ]]; then
            property_value="$(read_property_file_value "$property_file" "$property_name")"
            if [[ -n "$property_value" ]]; then
                printf '%s' "$property_value"
                return 0
            fi
        fi
    done

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

CURRENT_GIT_BRANCH="$(resolve_git_branch)"

if [[ "${RIVERKING_SKIP_BRANCH_PROFILE_GUARD:-false}" != "true" ]]; then
    required_profile=""
    case "$CURRENT_GIT_BRANCH" in
        develop)
            required_profile="test"
            ;;
        main)
            required_profile="prod"
            ;;
    esac

    if [[ -n "$required_profile" ]]; then
        if [[ -z "$BUILD_PROFILE" ]]; then
            BUILD_PROFILE="$required_profile"
        elif [[ "$BUILD_PROFILE" != "$required_profile" ]]; then
            die "branch '$CURRENT_GIT_BRANCH' only allows --profile $required_profile; set RIVERKING_SKIP_BRANCH_PROFILE_GUARD=true to bypass this intentionally"
        fi
    fi
fi

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

prod_version_code="$(resolved_version_property_value RIVERKING_VERSION_CODE || true)"
prod_version_name="$(resolved_version_property_value RIVERKING_VERSION_NAME || true)"

[[ -n "$prod_version_code" ]] || die "missing RIVERKING_VERSION_CODE; define it in $VERSION_FILE or pass it explicitly"
[[ "$prod_version_code" =~ ^[0-9]+$ ]] || die "RIVERKING_VERSION_CODE must be numeric, got '$prod_version_code'"
[[ -n "$prod_version_name" ]] || die "missing RIVERKING_VERSION_NAME; define it in $VERSION_FILE or pass it explicitly"

explicit_version_code="$(resolved_property_value RIVERKING_VERSION_CODE || true)"
explicit_version_name="$(resolved_property_value RIVERKING_VERSION_NAME || true)"
resolved_version_code="$prod_version_code"
resolved_version_name="$prod_version_name"

if [[ "$BUILD_PROFILE" == "test" ]]; then
    test_build_number="${RIVERKING_TEST_BUILD_NUMBER:-${GITHUB_RUN_NUMBER:-}}"
    if [[ -z "$test_build_number" ]]; then
        test_build_number="$(git -C "$REPO_ROOT" rev-list --count HEAD 2>/dev/null || printf '1')"
    fi
    [[ "$test_build_number" =~ ^[0-9]+$ ]] || die "test build number must be numeric, got '$test_build_number'"
    (( test_build_number > 0 )) || die "test build number must be positive"
    (( test_build_number <= 99999 )) || die "test build number must be <= 99999"
    (( prod_version_code <= 21474 )) || die "RIVERKING_VERSION_CODE is too large for generated test version codes"

    auto_test_version_code=$(( prod_version_code * 100000 + test_build_number ))
    auto_test_version_name="${prod_version_name}-test.$(date +%Y%m%d).${test_build_number}"

    resolved_version_code="${explicit_version_code:-$auto_test_version_code}"
    resolved_version_name="${explicit_version_name:-$auto_test_version_name}"
fi

for property_name in \
    RIVERKING_API_BASE_URL \
    RIVERKING_GOOGLE_AUTH_CLIENT_ID \
    RIVERKING_PUBLIC_WEB_URL \
    RIVERKING_ITCH_PROJECT_URL \
    RIVERKING_PLAY_STORE_URL \
    RIVERKING_SUPPORT_URL \
    RIVERKING_PRIVACY_POLICY_URL \
    RIVERKING_ACCOUNT_DELETION_URL \
    RIVERKING_CANONICAL_APPLICATION_ID; do
    property_value="$(resolved_property_value "$property_name" || true)"
    if [[ -n "$property_value" ]]; then
        gradle_args+=( "-P${property_name}=$property_value" )
    fi
done

gradle_args+=( "-PRIVERKING_VERSION_CODE=$resolved_version_code" )
gradle_args+=( "-PRIVERKING_VERSION_NAME=$resolved_version_name" )

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

if [[ "$release_build" == true ]] && [[ "${RIVERKING_SKIP_BRANCH_PROFILE_GUARD:-false}" != "true" ]]; then
    if [[ "$BUILD_PROFILE" != "prod" ]]; then
        die "release targets require --profile prod; use qa-release-apks for test builds or set RIVERKING_SKIP_BRANCH_PROFILE_GUARD=true to bypass intentionally"
    fi
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
if [[ -n "$CURRENT_GIT_BRANCH" ]]; then
    echo "branch: $CURRENT_GIT_BRANCH"
fi
if [[ -n "$BUILD_PROFILE" ]]; then
    echo "profile: $BUILD_PROFILE"
    echo "profile file: $PROFILE_FILE"
fi
echo "version code: $resolved_version_code"
echo "version name: $resolved_version_name"
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
