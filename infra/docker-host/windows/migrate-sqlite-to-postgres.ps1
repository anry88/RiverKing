param(
  [ValidateSet("prod", "test")]
  [string]$Environment = "prod",

  [string]$Root = "D:\Apps\RiverKing",

  [string]$DockerConfig = "D:\HomeDataCenter\.docker-empty",

  [string]$PgloaderImage = "dimitri/pgloader:latest",

  [switch]$NoStartApp
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

function Invoke-Docker {
  param([string[]]$DockerArgs)

  & docker --config $DockerConfig @DockerArgs
  if ($LASTEXITCODE -ne 0) {
    throw "docker failed: $($DockerArgs -join ' ')"
  }
}

Assert-PathExists $envFile
Assert-PathExists $composeFile

$envMap = Get-EnvMap -Path $envFile
$projectName = $envMap["COMPOSE_PROJECT_NAME"]
$dataDir = $envMap["RIVERKING_DATA_DIR"]
$postgresDataDir = $envMap["RIVERKING_POSTGRES_DATA_DIR"]
$postgresDb = $envMap["POSTGRES_DB"]
$postgresUser = $envMap["POSTGRES_USER"]
$postgresPassword = $envMap["POSTGRES_PASSWORD"]

if ([string]::IsNullOrWhiteSpace($projectName)) {
  $projectName = "riverking-$Environment"
}
if ([string]::IsNullOrWhiteSpace($dataDir)) {
  throw "RIVERKING_DATA_DIR is required in $envFile"
}
if ([string]::IsNullOrWhiteSpace($postgresDataDir)) {
  $postgresDataDir = Join-Path $dataDir "postgres"
}
if ([string]::IsNullOrWhiteSpace($postgresDb)) {
  $postgresDb = "riverking"
}
if ([string]::IsNullOrWhiteSpace($postgresUser)) {
  $postgresUser = "riverking"
}
if ([string]::IsNullOrWhiteSpace($postgresPassword)) {
  throw "POSTGRES_PASSWORD is required in $envFile"
}

$dbFile = Join-Path $dataDir "riverking.db"
$loadFile = Join-Path $dataDir "sqlite-to-postgres.load"
$networkName = "${projectName}_default"
$postgresJdbcUrl = "jdbc:postgresql://postgres:5432/$postgresDb"

Assert-PathExists $dbFile
New-Item -ItemType Directory -Force -Path $postgresDataDir | Out-Null

Write-Host "Preparing PostgreSQL before app downtime..."
Invoke-Docker @("pull", $PgloaderImage)
Invoke-Compose @("up", "-d", "--wait", "postgres")

Write-Host "Stopping app for final SQLite snapshot and import..."
Invoke-Compose @("stop", "app")

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$dbBackup = "$dbFile.before-postgres-migration-$timestamp"
Copy-Item -Force -LiteralPath $dbFile -Destination $dbBackup
foreach ($suffix in @("-wal", "-shm")) {
  $sidecar = "$dbFile$suffix"
  if (Test-Path -LiteralPath $sidecar) {
    Copy-Item -Force -LiteralPath $sidecar -Destination "$sidecar.before-postgres-migration-$timestamp"
  }
}

$escapedUser = [System.Uri]::EscapeDataString($postgresUser)
$escapedPassword = [System.Uri]::EscapeDataString($postgresPassword)
$escapedDb = [System.Uri]::EscapeDataString($postgresDb)
$pgloaderTarget = "postgresql://$escapedUser`:$escapedPassword@postgres:5432/$escapedDb"

@"
LOAD DATABASE
     FROM sqlite:///data/riverking.db
     INTO $pgloaderTarget

 WITH include drop,
      create tables,
      create indexes,
      reset sequences,
      foreign keys

 SET work_mem to '32MB',
     maintenance_work_mem to '512MB';
"@ | Set-Content -LiteralPath $loadFile -Encoding ASCII

try {
  Write-Host "Importing SQLite data into PostgreSQL..."
  Invoke-Docker @(
    "run",
    "--rm",
    "--network", $networkName,
    "-v", "${dataDir}:/data",
    $PgloaderImage,
    "pgloader",
    "/data/sqlite-to-postgres.load"
  )
} finally {
  Remove-Item -Force -LiteralPath $loadFile -ErrorAction SilentlyContinue
}

$envBackup = "$envFile.before-postgres-switch-$timestamp"
Copy-Item -Force -LiteralPath $envFile -Destination $envBackup
Set-EnvValue -Path $envFile -Name "DATABASE_URL" -Value $postgresJdbcUrl
Set-EnvValue -Path $envFile -Name "DATABASE_USER" -Value $postgresUser
Set-EnvValue -Path $envFile -Name "DATABASE_PASSWORD" -Value $postgresPassword

Write-Host "SQLite backup: $dbBackup"
Write-Host "Env backup: $envBackup"
Write-Host "DATABASE_URL=$postgresJdbcUrl"

if (-not $NoStartApp) {
  Write-Host "Starting app on PostgreSQL..."
  Invoke-Compose @("up", "-d", "--no-build", "--wait", "app")
}

Write-Host "SQLite to PostgreSQL migration complete."
