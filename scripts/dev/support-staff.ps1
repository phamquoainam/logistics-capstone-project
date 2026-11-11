param(
    [Parameter(Mandatory=$true)][Guid]$AccountId,
    [Parameter(Mandatory=$true)][ValidateSet('SUPPORT_TRIAGE','SUPPORT_HANDLE')][string]$Permission,
    [ValidateSet('GRANT','REVOKE')][string]$Action='GRANT'
)
$ErrorActionPreference='Stop'
if ($AccountId -eq [Guid]::Empty) { throw 'EXPLICIT_VERIFIED_ACCOUNT_REQUIRED' }
$taskRoot=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$taskSettings=Join-Path $taskRoot '.demo/postgres/settings.json'
if (!(Test-Path -LiteralPath $taskSettings -PathType Leaf)) { throw 'EXISTING_POSTGRES_SETTINGS_REQUIRED; NO_INITIALIZATION_PERFORMED' }
$dockerCommand=Get-Command docker -ErrorAction SilentlyContinue
if ($dockerCommand) { $taskDocker=$dockerCommand.Source } else { $taskDocker=Join-Path $env:LOCALAPPDATA 'Programs/DockerDesktop/resources/bin/docker.exe' }
if (!(Test-Path -LiteralPath $taskDocker -PathType Leaf)) { throw 'DOCKER_CLI_UNAVAILABLE' }
$taskLabels=(& $taskDocker inspect --format '{{json .Config.Labels}}' elogistics-d1-postgres18) | ConvertFrom-Json
if ($LASTEXITCODE -ne 0 -or $taskLabels.'com.docker.compose.project' -ne 'elogistics-d1') { throw 'ORIGINAL_POSTGRES_IDENTITY_REQUIRED' }
$taskHealth=(& $taskDocker inspect --format '{{.State.Health.Status}}' elogistics-d1-postgres18).Trim()
if ($LASTEXITCODE -ne 0 -or $taskHealth -ne 'healthy') { throw 'EXISTING_POSTGRES_NOT_HEALTHY' }
# Explicit operator action on one existing account. No account seed, runtime self-grant or secret output.
$taskPg=Get-Content -LiteralPath $taskSettings -Raw | ConvertFrom-Json
if ([string]::IsNullOrWhiteSpace($taskPg.migratorPassword)) { throw 'EXISTING_MIGRATOR_CONFIGURATION_REQUIRED' }
$priorPgPassword=$env:PGPASSWORD
try {
    $env:PGPASSWORD=[string]$taskPg.migratorPassword
    $taskSql="BEGIN; SET LOCAL ROLE elogistics_owner; SET LOCAL argopost.operator_environment='LOCAL'; SELECT orders.configure_local_support_staff('$($AccountId.ToString())'::uuid,'$Permission','$Action'); COMMIT;"
    $taskResult=$taskSql | & $taskDocker exec --env PGPASSWORD -i elogistics-d1-postgres18 psql -X -q -tA -v ON_ERROR_STOP=1 -v VERBOSITY=sqlstate -U elogistics_migrator -d elogistics_dev 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'SUPPORT_STAFF_CONFIGURATION_FAILED; CHECK_V22_OWNER_ROLE_AND_LOCAL_POLICY' }
    Write-Output "SUPPORT_STAFF_${Action}_PASS; $Permission; DATABASE_OPERATOR_AUDIT_RECORDED"
} finally {
    $env:PGPASSWORD=$priorPgPassword
    $taskPg=$null; $taskResult=$null; $taskSql=$null
}
