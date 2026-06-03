#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: scripts/deploy-hdc.sh [options]

Deploy the current checkout to the RiverKing Windows Docker host.

Options:
  --environment prod|test  Target Windows env file and compose project. Default: prod.
  --tag TAG                Image tag. Default: hdc-<current-git-short-sha>.
  --remote HOST            SSH host alias for the Windows machine. Default: hdc.
  --docker-context NAME    Docker context for the Windows Docker engine. Default: hdc.
  --root PATH              Windows root directory. Default: D:\Apps\RiverKing.
  --docker-config PATH     Windows Docker config path. Default: D:\HomeDataCenter\.docker-empty.
  --database sqlite|postgres
                           Runtime database for the app. Default: sqlite.
  --seed-config PATH       Copy an external config.properties to the Windows host before deploy.
  --seed-db PATH           Copy a SQLite backup to riverking.db before deploy.
  --force-seed-db          Allow --seed-db to replace an existing remote riverking.db.
  --skip-build             Reuse already-built image for TAG.
  --skip-smoke             Do not run public /health smoke check.
  --check                  Validate local/remote deploy prerequisites and exit.
  --help                   Show this help.
EOF
}

log() {
  printf '[riverking-hdc] %s\n' "$*"
}

die() {
  printf '[riverking-hdc] ERROR: %s\n' "$*" >&2
  exit 1
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"
}

ENVIRONMENT="prod"
REMOTE_HOST="${HDC_REMOTE_HOST:-hdc}"
DOCKER_CONTEXT="${HDC_DOCKER_CONTEXT:-hdc}"
WINDOWS_ROOT="${HDC_RIVERKING_ROOT:-D:\\Apps\\RiverKing}"
WINDOWS_DOCKER_CONFIG="${HDC_WINDOWS_DOCKER_CONFIG:-D:\\HomeDataCenter\\.docker-empty}"
IMAGE_TAG=""
DATABASE_ENGINE="${HDC_RIVERKING_DATABASE:-sqlite}"
SEED_CONFIG=""
SEED_DB=""
FORCE_SEED_DB=0
SKIP_BUILD=0
SKIP_SMOKE=0
CHECK_ONLY=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --environment)
      [[ $# -ge 2 ]] || die "--environment requires prod or test."
      ENVIRONMENT="$2"
      shift 2
      ;;
    --tag)
      [[ $# -ge 2 ]] || die "--tag requires a value."
      IMAGE_TAG="$2"
      shift 2
      ;;
    --remote)
      [[ $# -ge 2 ]] || die "--remote requires a host."
      REMOTE_HOST="$2"
      shift 2
      ;;
    --docker-context)
      [[ $# -ge 2 ]] || die "--docker-context requires a context name."
      DOCKER_CONTEXT="$2"
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
    --database)
      [[ $# -ge 2 ]] || die "--database requires sqlite or postgres."
      DATABASE_ENGINE="$2"
      shift 2
      ;;
    --seed-config)
      [[ $# -ge 2 ]] || die "--seed-config requires a local path."
      SEED_CONFIG="$2"
      shift 2
      ;;
    --seed-db)
      [[ $# -ge 2 ]] || die "--seed-db requires a local path."
      SEED_DB="$2"
      shift 2
      ;;
    --force-seed-db)
      FORCE_SEED_DB=1
      shift
      ;;
    --skip-build)
      SKIP_BUILD=1
      shift
      ;;
    --skip-smoke)
      SKIP_SMOKE=1
      shift
      ;;
    --check)
      CHECK_ONLY=1
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

case "$DATABASE_ENGINE" in
  sqlite|postgres) ;;
  *) die "--database must be sqlite or postgres." ;;
esac

require_cmd docker
require_cmd git
require_cmd scp
require_cmd ssh
require_cmd curl

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
compose_file="$repo_root/infra/docker-host/compose.yml"
remote_repo="$WINDOWS_ROOT\\deploy\\$ENVIRONMENT\\repo"
remote_env="$WINDOWS_ROOT\\env\\$ENVIRONMENT.env"
remote_config_dir="$WINDOWS_ROOT\\config\\$ENVIRONMENT"
remote_config="$remote_config_dir\\config.properties"
remote_data_dir="$WINDOWS_ROOT\\state\\$ENVIRONMENT"
remote_postgres_data_dir="$remote_data_dir\\postgres"
remote_db="$remote_data_dir\\riverking.db"
remote_deploy_ps1="$remote_repo\\infra\\docker-host\\windows\\deploy.ps1"
remote_infra_dir="$remote_repo\\infra\\docker-host"
remote_infra_windows_dir="$remote_infra_dir\\windows"

[[ -f "$compose_file" ]] || die "Compose file not found: $compose_file"
[[ -z "$SEED_CONFIG" || -f "$SEED_CONFIG" ]] || die "Seed config not found: $SEED_CONFIG"
[[ -z "$SEED_DB" || -f "$SEED_DB" ]] || die "Seed DB not found: $SEED_DB"

if [[ -z "$IMAGE_TAG" ]]; then
  IMAGE_TAG="hdc-$(git -C "$repo_root" rev-parse --short HEAD)"
fi

tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/riverking-hdc-deploy.XXXXXX")"
build_env="$tmp_dir/build.env"

cleanup() {
  rm -rf "$tmp_dir"
}
trap cleanup EXIT

windows_to_scp_path() {
  printf '%s' "${1//\\//}"
}

run_remote_ps() {
  local command="$1"
  ssh "$REMOTE_HOST" "powershell -NoProfile -ExecutionPolicy Bypass -Command \"$command\""
}

read_remote_env_value() {
  local name="$1"
  ssh "$REMOTE_HOST" "powershell -NoProfile -ExecutionPolicy Bypass -Command \"if (Test-Path -LiteralPath '$remote_env') { \\$line = Get-Content -LiteralPath '$remote_env' | Where-Object { \\$_ -match '^$name=' } | Select-Object -Last 1; if (\\$line) { \\$line.Substring($(( ${#name} + 1 ))) } }\"" 2>/dev/null | tr -d '\r' | tail -1 || true
}

generate_postgres_password() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 24
  elif command -v uuidgen >/dev/null 2>&1; then
    uuidgen | tr -d '-'
  else
    printf 'riverking%s%s' "$(date +%s)" "$RANDOM"
  fi
}

write_build_env() {
  local default_alias
  local postgres_db
  local postgres_user
  local postgres_password
  local database_url
  if [[ "$ENVIRONMENT" == "prod" ]]; then
    default_alias="riverking-prod-app"
  else
    default_alias="riverking-test-app"
  fi

  postgres_db="${RIVERKING_POSTGRES_DB:-riverking}"
  postgres_user="${RIVERKING_POSTGRES_USER:-riverking}"
  postgres_password="${RIVERKING_POSTGRES_PASSWORD:-${POSTGRES_PASSWORD:-}}"
  if [[ -z "$postgres_password" ]]; then
    postgres_password="$(read_remote_env_value "POSTGRES_PASSWORD")"
  fi
  if [[ -z "$postgres_password" ]]; then
    postgres_password="$(read_remote_env_value "DATABASE_PASSWORD")"
  fi
  if [[ -z "$postgres_password" ]]; then
    postgres_password="$(generate_postgres_password)"
  fi

  if [[ "$DATABASE_ENGINE" == "postgres" ]]; then
    database_url="jdbc:postgresql://postgres:5432/$postgres_db"
  else
    database_url="jdbc:sqlite:/data/riverking.db"
  fi

  cat > "$build_env" <<EOF
COMPOSE_PROJECT_NAME=riverking-$ENVIRONMENT
IMAGE_TAG=$IMAGE_TAG
RIVERKING_IMAGE=riverking:$IMAGE_TAG
RIVERKING_CONFIG_FILE=$remote_config
RIVERKING_DATA_DIR=$remote_data_dir
RIVERKING_POSTGRES_DATA_DIR=$remote_postgres_data_dir
POSTGRES_DB=$postgres_db
POSTGRES_USER=$postgres_user
POSTGRES_PASSWORD=$postgres_password
DATABASE_URL=$database_url
DATABASE_USER=$postgres_user
DATABASE_PASSWORD=$postgres_password
EVENT_ASSETS_DIR=/data/event-assets
PORT=5005
JAVA_OPTS=-Xms256m -Xmx1024m -XX:+UseG1GC
HDC_TUNNEL_NETWORK=hdc-tunnel
TUNNEL_APP_ALIAS=$default_alias
EOF
}

sync_remote_env() {
  log "Writing Windows env file $remote_env"
  run_remote_ps "New-Item -ItemType Directory -Force -Path '$WINDOWS_ROOT\\env' | Out-Null"
  scp "$build_env" "$REMOTE_HOST:$(windows_to_scp_path "$remote_env")"
}

sync_remote_infra() {
  log "Syncing Docker-host infra files to $REMOTE_HOST:$remote_infra_dir"
  run_remote_ps "New-Item -ItemType Directory -Force -Path '$remote_infra_dir' | Out-Null; New-Item -ItemType Directory -Force -Path '$remote_infra_windows_dir' | Out-Null"

  scp "$repo_root/infra/docker-host/compose.yml" "$REMOTE_HOST:$(windows_to_scp_path "$remote_infra_dir")/compose.yml"
  scp "$repo_root/infra/docker-host/Dockerfile" "$REMOTE_HOST:$(windows_to_scp_path "$remote_infra_dir")/Dockerfile"
  scp "$repo_root/infra/docker-host/windows/deploy.ps1" "$REMOTE_HOST:$(windows_to_scp_path "$remote_infra_windows_dir")/deploy.ps1"
  scp "$repo_root/infra/docker-host/windows/migrate-sqlite-to-postgres.ps1" "$REMOTE_HOST:$(windows_to_scp_path "$remote_infra_windows_dir")/migrate-sqlite-to-postgres.ps1"
}

seed_remote_files() {
  run_remote_ps "New-Item -ItemType Directory -Force -Path '$remote_config_dir' | Out-Null; New-Item -ItemType Directory -Force -Path '$remote_data_dir' | Out-Null; New-Item -ItemType Directory -Force -Path '$remote_data_dir\\logs' | Out-Null; New-Item -ItemType Directory -Force -Path '$remote_data_dir\\event-assets' | Out-Null; New-Item -ItemType Directory -Force -Path '$remote_postgres_data_dir' | Out-Null"

  if [[ -n "$SEED_CONFIG" ]]; then
    log "Seeding config to $remote_config"
    run_remote_ps "if (Test-Path -LiteralPath '$remote_config') { Copy-Item -Force -LiteralPath '$remote_config' -Destination ('$remote_config.before-seed-' + (Get-Date -Format 'yyyyMMdd-HHmmss')) }"
    scp "$SEED_CONFIG" "$REMOTE_HOST:$(windows_to_scp_path "$remote_config")"
  fi

  if [[ -n "$SEED_DB" ]]; then
    if [[ "$FORCE_SEED_DB" != "1" ]]; then
      run_remote_ps "if (Test-Path -LiteralPath '$remote_db') { throw 'Remote DB already exists. Re-run with --force-seed-db to replace it.' }"
    else
      run_remote_ps "if (Test-Path -LiteralPath '$remote_db') { Copy-Item -Force -LiteralPath '$remote_db' -Destination ('$remote_db.before-seed-' + (Get-Date -Format 'yyyyMMdd-HHmmss')) }"
    fi

    log "Seeding SQLite DB to $remote_db"
    scp "$SEED_DB" "$REMOTE_HOST:$(windows_to_scp_path "$remote_db")"
  fi
}

sync_event_assets() {
  local assets_dir="$repo_root/data/event-assets"
  [[ -d "$assets_dir" ]] || return
  [[ -n "$(find "$assets_dir" -maxdepth 1 -type f -print -quit)" ]] || return

  log "Syncing event assets to $remote_data_dir\\event-assets"
  scp -r "$assets_dir/." "$REMOTE_HOST:$(windows_to_scp_path "$remote_data_dir")/event-assets/"
}

run_remote_deploy() {
  local remote_cmd
  remote_cmd="powershell -NoProfile -ExecutionPolicy Bypass -File \"$remote_deploy_ps1\" -Environment \"$ENVIRONMENT\" -Root \"$WINDOWS_ROOT\" -DockerConfig \"$WINDOWS_DOCKER_CONFIG\" -ImageTag \"$IMAGE_TAG\""

  if [[ "$CHECK_ONLY" == "1" ]]; then
    remote_cmd+=" -CheckOnly"
  fi

  ssh "$REMOTE_HOST" "$remote_cmd"
}

smoke_test() {
  [[ "$SKIP_SMOKE" == "0" ]] || return 0
  [[ -n "$SEED_CONFIG" ]] || return 0

  local public_base_url
  public_base_url="$(grep -E '^PUBLIC_BASE_URL=' "$SEED_CONFIG" | tail -1 | cut -d= -f2- | tr -d '\r' || true)"
  [[ -n "$public_base_url" ]] || return 0

  log "Smoke testing $public_base_url/health"
  curl -fsS "$public_base_url/health" >/dev/null
}

log "Deploy target: environment=$ENVIRONMENT host=$REMOTE_HOST docker_context=$DOCKER_CONTEXT database=$DATABASE_ENGINE tag=$IMAGE_TAG"
write_build_env
sync_remote_env
sync_remote_infra
seed_remote_files
sync_event_assets

if [[ "$CHECK_ONLY" == "1" ]]; then
  log "Validating compose interpolation with generated build env."
  docker --context "$DOCKER_CONTEXT" compose --env-file "$build_env" -f "$compose_file" config >/dev/null
  run_remote_deploy
  log "Check complete."
  exit 0
fi

if [[ "$SKIP_BUILD" == "0" ]]; then
  log "Building Docker image on remote context $DOCKER_CONTEXT"
  docker --context "$DOCKER_CONTEXT" compose --env-file "$build_env" -f "$compose_file" build app
else
  log "Skipping image build; expecting tag $IMAGE_TAG to already exist on $DOCKER_CONTEXT."
fi

run_remote_deploy
smoke_test

log "Deploy complete: $IMAGE_TAG"
