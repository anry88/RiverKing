param(
  [ValidateSet("prod", "test")]
  [string]$Environment = "prod",

  [string]$Root = "D:\Apps\RiverKing",

  [string]$DockerConfig = "D:\HomeDataCenter\.docker-empty",

  [Parameter(Mandatory = $true)]
  [string]$ImageTag,

  [switch]$NoWait,
  [switch]$CheckOnly
)

$ErrorActionPreference = "Stop"

$envFile = Join-Path $Root "env\$Environment.env"
$repoRoot = Join-Path $Root "deploy\$Environment\repo"
$composeDir = Join-Path $repoRoot "infra\docker-host"
$composeFile = Join-Path $composeDir "compose.yml"

function Assert-PathExists {
  param([string]$Path)
  if (-not (Test-Path -LiteralPath $Path)) {
    throw "Required path not found: $Path"
  }
}

function Get-EnvMap {
  param([string]$Path)

  $map = @{}
  foreach ($line in Get-Content -LiteralPath $Path) {
    $trimmed = $line.Trim()
    if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) {
      continue
    }

    $parts = $trimmed -split "=", 2
    if ($parts.Length -eq 2) {
      $map[$parts[0]] = $parts[1]
    }
  }
  return $map
}

function Set-EnvValue {
  param(
    [string]$Path,
    [string]$Name,
    [string]$Value
  )

  $lines = if (Test-Path -LiteralPath $Path) {
    @(Get-Content -LiteralPath $Path)
  } else {
    @()
  }

  $updated = $false
  $nextLines = foreach ($line in $lines) {
    if ($line -match "^$([regex]::Escape($Name))=") {
      "$Name=$Value"
      $updated = $true
    } else {
      $line
    }
  }

  if (-not $updated) {
    $nextLines += "$Name=$Value"
  }

  Set-Content -LiteralPath $Path -Encoding ASCII -Value $nextLines
}

function Invoke-Compose {
  param([string[]]$ComposeArgs)

  & docker --config $DockerConfig compose --env-file $envFile -f $composeFile @ComposeArgs
  if ($LASTEXITCODE -ne 0) {
    throw "docker compose failed: $($ComposeArgs -join ' ')"
  }
}

Assert-PathExists $envFile
Assert-PathExists $composeFile

$envMap = Get-EnvMap -Path $envFile
$configFile = $envMap["RIVERKING_CONFIG_FILE"]
$dataDir = $envMap["RIVERKING_DATA_DIR"]
if ([string]::IsNullOrWhiteSpace($configFile)) {
  throw "RIVERKING_CONFIG_FILE is required in $envFile"
}
if ([string]::IsNullOrWhiteSpace($dataDir)) {
  throw "RIVERKING_DATA_DIR is required in $envFile"
}

New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $dataDir "logs") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $dataDir "event-assets") | Out-Null

Assert-PathExists $configFile
Assert-PathExists (Join-Path $dataDir "riverking.db")

if ($CheckOnly) {
  Write-Host "Docker-host deploy check passed."
  Write-Host "Environment: $Environment"
  Write-Host "Env file: $envFile"
  Write-Host "Compose file: $composeFile"
  Write-Host "Config file: $configFile"
  Write-Host "Data dir: $dataDir"
  Write-Host "Image tag: $ImageTag"
  exit 0
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$envBackup = "$envFile.before-deploy-$timestamp"
Copy-Item -Force -LiteralPath $envFile -Destination $envBackup
Set-EnvValue -Path $envFile -Name "IMAGE_TAG" -Value $ImageTag
Set-EnvValue -Path $envFile -Name "RIVERKING_IMAGE" -Value "riverking:$ImageTag"

$dbFile = Join-Path $dataDir "riverking.db"
$dbBackup = "$dbFile.before-deploy-$timestamp"
Copy-Item -Force -LiteralPath $dbFile -Destination $dbBackup

Write-Host "Updated IMAGE_TAG=$ImageTag"
Write-Host "Env backup: $envBackup"
Write-Host "Database backup: $dbBackup"

Push-Location $composeDir
try {
  $upArgs = @("up", "-d", "--no-build")
  if (-not $NoWait) {
    $upArgs += "--wait"
  }

  Invoke-Compose ($upArgs + @("app"))
} finally {
  Pop-Location
}

Write-Host "Docker-host deploy complete."
