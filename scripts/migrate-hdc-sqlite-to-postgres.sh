#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: scripts/migrate-hdc-sqlite-to-postgres.sh [options]

Run the final production SQLite -> PostgreSQL migration on the Windows Docker host.

This script expects scripts/deploy-hdc.sh to have already synced the current
infra files and written an env file that contains POSTGRES_* values.

Options:
  --environment prod|test  Target Windows environment. Default: prod.
  --remote HOST            SSH host alias for the Windows machine. Default: hdc.
  --root PATH              Windows root directory. Default: D:\Apps\RiverKing.
  --docker-config PATH     Windows Docker config path. Default: D:\HomeDataCenter\.docker-empty.
  --pgloader-image IMAGE   pgloader image. Default: dimitri/pgloader:latest.
  --no-start-app           Import data and switch env, but do not start the app.
  --help                   Show this help.
EOF
}

log() {
  printf '[riverking-pg-migrate] %s\n' "$*"
}

die() {
  printf '[riverking-pg-migrate] ERROR: %s\n' "$*" >&2
  exit 1
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"
}

ENVIRONMENT="prod"
REMOTE_HOST="${HDC_REMOTE_HOST:-hdc}"
WINDOWS_ROOT="${HDC_RIVERKING_ROOT:-D:\\Apps\\RiverKing}"
WINDOWS_DOCKER_CONFIG="${HDC_WINDOWS_DOCKER_CONFIG:-D:\\HomeDataCenter\\.docker-empty}"
PGLOADER_IMAGE="${RIVERKING_PGLOADER_IMAGE:-dimitri/pgloader:latest}"
NO_START_APP=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --environment)
      [[ $# -ge 2 ]] || die "--environment requires prod or test."
      ENVIRONMENT="$2"
      shift 2
      ;;
    --remote)
      [[ $# -ge 2 ]] || die "--remote requires a host."
      REMOTE_HOST="$2"
      shift 2
      ;;
    --root)
      [[ $# -ge 2 ]] || die "--root requires a Windows path."
      WINDOWS_ROOT="$2"
      shift 2
      ;;
    --docker-config)
      [[ $# -ge 2 ]] || die "--docker-config requires a Windows path."
      WINDOWS_DOCKER_CONFIG="$2"
      shift 2
      ;;
    --pgloader-image)
      [[ $# -ge 2 ]] || die "--pgloader-image requires an image."
      PGLOADER_IMAGE="$2"
      shift 2
      ;;
    --no-start-app)
      NO_START_APP=1
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      die "Unknown option: $1"
      ;;
  esac
done

case "$ENVIRONMENT" in
  prod|test) ;;
  *) die "--environment must be prod or test." ;;
esac

require_cmd scp
require_cmd ssh

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
remote_repo="$WINDOWS_ROOT\\deploy\\$ENVIRONMENT\\repo"
remote_infra_windows_dir="$remote_repo\\infra\\docker-host\\windows"
remote_script="$remote_infra_windows_dir\\migrate-sqlite-to-postgres.ps1"

windows_to_scp_path() {
  printf '%s' "${1//\\//}"
}

log "Syncing migration script to $REMOTE_HOST:$remote_script"
ssh "$REMOTE_HOST" "powershell -NoProfile -ExecutionPolicy Bypass -Command \"New-Item -ItemType Directory -Force -Path '$remote_infra_windows_dir' | Out-Null\""
scp "$repo_root/infra/docker-host/windows/migrate-sqlite-to-postgres.ps1" "$REMOTE_HOST:$(windows_to_scp_path "$remote_script")"

remote_cmd="powershell -NoProfile -ExecutionPolicy Bypass -File \"$remote_script\" -Environment \"$ENVIRONMENT\" -Root \"$WINDOWS_ROOT\" -DockerConfig \"$WINDOWS_DOCKER_CONFIG\" -PgloaderImage \"$PGLOADER_IMAGE\""
if [[ "$NO_START_APP" == "1" ]]; then
  remote_cmd+=" -NoStartApp"
fi

log "Running SQLite -> PostgreSQL migration on $REMOTE_HOST"
ssh "$REMOTE_HOST" "$remote_cmd"
