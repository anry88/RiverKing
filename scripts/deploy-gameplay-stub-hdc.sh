#!/usr/bin/env bash
set -euo pipefail

# Deploy the HTTP client; provision ordinary bot sessions through the protected server API.
environment="prod"
remote="${HDC_REMOTE_HOST:-hdc}"
context="${HDC_DOCKER_CONTEXT:-hdc}"
root="${HDC_RIVERKING_ROOT:-D:\\Apps\\RiverKing}"
action="deploy"
tag=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --environment) environment="$2"; shift 2 ;;
    --tag) tag="$2"; shift 2 ;;
    --stop) action="stop"; shift ;;
    --help) echo 'Usage: scripts/deploy-gameplay-stub-hdc.sh [--environment prod|test] [--tag TAG] [--stop]'; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done
[[ "$environment" == "prod" || "$environment" == "test" ]]
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
[[ "$(git -C "$repo" branch --show-current)" == "develop" ]] || { echo 'Run from develop.' >&2; exit 1; }
tag="${tag:-hdc-$(git -C "$repo" rev-parse --short HEAD)}"
[[ "$tag" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.-]*$ ]] || { echo 'Invalid image tag.' >&2; exit 1; }
image="riverking-gameplay-stub:$tag"
remote_dir="$root\\deploy\\$environment\\gameplay-stub"
compose_file="$remote_dir\\compose.gameplay-stub.yml"
env_file="$root\\env\\$environment.env"
state_dir="$root\\state\\$environment\\gameplay-stub"
ps_file="$remote_dir\\deploy-gameplay-stub.ps1"
tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/riverking-stub-deploy.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT
cat > "$tmp_dir/deploy.ps1" <<'PS'
param([string]$EnvFile, [string]$ComposeFile, [string]$Image, [string]$Environment, [string]$Action)
$ErrorActionPreference = "Stop"
if (-not (Test-Path -LiteralPath $EnvFile)) { throw "Backend environment file not found" }
function Read-Value([string]$Path, [string]$Key) {
    $line = Get-Content -LiteralPath $Path | Where-Object { $_ -match ("^" + [regex]::Escape($Key) + "=") } | Select-Object -Last 1
    if (-not $line) { throw "Missing setting: $Key" }
    return $line.Substring($Key.Length + 1).Trim()
}
if ($Action -ne 'stop') {
    $dataDir = Read-Value $EnvFile 'RIVERKING_DATA_DIR'
    $sessionsFile = Join-Path $dataDir 'gameplay-stub\accounts.json'
    if (-not (Test-Path -LiteralPath $sessionsFile)) {
        $configFile = Read-Value $EnvFile 'RIVERKING_CONFIG_FILE'
        $baseUrl = (Read-Value $configFile 'PUBLIC_BASE_URL').TrimEnd('/')
        $adminToken = Read-Value $configFile 'ADMIN_API_TOKEN'
        $policy = Invoke-RestMethod -Uri "$baseUrl/api/mobile/update" -TimeoutSec 30
        $headers = @{ Authorization = "Bearer $adminToken"; 'X-RiverKing-App-Version-Code' = [string]$policy.latestVersionCode }
        $sessions = Invoke-RestMethod -Uri "$baseUrl/api/admin/gameplay-bots/sessions" -Method Post -Headers $headers -TimeoutSec 90
        if (@($sessions).Count -ne 50) { throw "Expected 50 bot sessions" }
        $json = ConvertTo-Json -InputObject @($sessions) -Depth 10 -Compress
        [System.IO.File]::WriteAllText($sessionsFile, $json, [System.Text.UTF8Encoding]::new($false))
        Write-Host 'Provisioned 50 account sessions through server API.'
    }
}
$env:STUB_IMAGE = $Image
$env:STUB_COMPOSE_PROJECT_NAME = "riverking-$Environment-stub"
$env:STUB_BACKEND_NETWORK = "riverking-${Environment}_default"
$composeArgs = @('--config', 'D:\HomeDataCenter\.docker-empty', 'compose', '--project-name', $env:STUB_COMPOSE_PROJECT_NAME, '--env-file', $EnvFile, '-f', $ComposeFile)
if ($Action -eq 'stop') {
    & docker @composeArgs stop gameplay-stub
} else {
    & docker @composeArgs up -d --no-build --wait --wait-timeout 180 gameplay-stub
}
if ($LASTEXITCODE -ne 0) { throw "Worker compose action failed" }
PS

if [[ "$action" == "deploy" ]]; then
  docker --context "$context" build -f "$repo/gameplay-stub/Dockerfile" -t "$image" "$repo"
fi
ssh "$remote" "powershell -NoProfile -Command \"New-Item -ItemType Directory -Force -Path '$remote_dir', '$state_dir' | Out-Null\""
scp "$repo/infra/docker-host/compose.gameplay-stub.yml" "$remote:${remote_dir//\\//}/compose.gameplay-stub.yml"
scp "$tmp_dir/deploy.ps1" "$remote:${ps_file//\\//}"
ssh "$remote" "powershell -NoProfile -ExecutionPolicy Bypass -File \"$ps_file\" -EnvFile \"$env_file\" -ComposeFile \"$compose_file\" -Image \"$image\" -Environment \"$environment\" -Action \"$action\""
