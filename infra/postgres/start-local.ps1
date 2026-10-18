param([switch]$InitializeOnly, [ValidateSet('legacy','protected')][string]$CredentialSource = 'legacy')
$ErrorActionPreference = 'Stop'
$foundationRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$foundationDirectory = Join-Path $foundationRoot '.demo/postgres'
$foundationSettingsPath = Join-Path $foundationDirectory 'settings.json'
if ($CredentialSource -eq 'protected') {
    $protectedPaths = & python -B (Join-Path $foundationRoot 'infra/security/credential_store.py') paths --mode postgres
    if ($LASTEXITCODE -ne 0) { throw 'Protected PostgreSQL settings unavailable; no legacy fallback.' }
    $protectedPaths = $protectedPaths | ConvertFrom-Json
    $foundationSettingsPath = $protectedPaths.'postgres-settings.json'
    $saved = Get-Content -LiteralPath $foundationSettingsPath -Raw | ConvertFrom-Json
    foreach ($kind in @('bootstrap','migrator','runtime')) {
        $value = $saved.PSObject.Properties["${kind}Password"].Value
        if ([IO.File]::ReadAllText($protectedPaths.PSObject.Properties["$kind-password.txt"].Value) -cne $value) {
            throw 'Protected PostgreSQL credential consistency check failed; no rotation.'
        }
    }
    if ($InitializeOnly) { return }
    throw 'Protected PostgreSQL container cutover is not authorized in Phase 1; no Docker operations performed.'
}
$dockerCommand = Get-Command docker -ErrorAction SilentlyContinue
if ($dockerCommand) { $dockerExecutable = $dockerCommand.Source }
else { $dockerExecutable = Join-Path $env:LOCALAPPDATA 'Programs/DockerDesktop/resources/bin/docker.exe' }
if (!(Test-Path -LiteralPath $dockerExecutable)) { throw 'Docker CLI not found. Configure Docker Desktop CLI on PATH.' }
function New-FoundationSecret {
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes); [Convert]::ToBase64String($bytes) } finally { $rng.Dispose() }
}
$existingVolumes = @(& $dockerExecutable volume ls --format '{{.Name}}')
if ($LASTEXITCODE -ne 0) { throw 'Docker Engine unavailable.' }
if (!(Test-Path -LiteralPath $foundationSettingsPath)) {
    if ($existingVolumes -contains 'elogistics_d1_pg18_dev_data') { throw 'Named volume already exists without local settings. Stop and investigate; do not initialize or reset.' }
    New-Item -ItemType Directory -Path $foundationDirectory -Force | Out-Null
    [ordered]@{ bootstrapPassword=(New-FoundationSecret); migratorPassword=(New-FoundationSecret); runtimePassword=(New-FoundationSecret) } |
        ConvertTo-Json | Set-Content -LiteralPath $foundationSettingsPath -Encoding UTF8
}
$foundationSettings = Get-Content -LiteralPath $foundationSettingsPath -Raw | ConvertFrom-Json
foreach ($kind in @('bootstrap','migrator','runtime')) {
    $secretPath = Join-Path $foundationDirectory "$kind-password.txt"
    $value = $foundationSettings.PSObject.Properties["${kind}Password"].Value
    if (!(Test-Path -LiteralPath $secretPath)) { [IO.File]::WriteAllText($secretPath, $value) }
    elseif ([IO.File]::ReadAllText($secretPath) -cne $value) { throw "Private $kind secret differs from saved settings. Stop; do not rotate automatically." }
}
$env:ELOGISTICS_SECRETS_DIR = $foundationDirectory.Replace('\','/')
if ($InitializeOnly) { return }
if ($existingVolumes -contains 'elogistics_d1_pg18_dev_data') {
    $labelsJson = & $dockerExecutable volume inspect elogistics_d1_pg18_dev_data --format '{{json .Labels}}'
    if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect volume ownership.' }
    $labels = $labelsJson | ConvertFrom-Json
    if ($labels.'com.docker.compose.project' -ne 'elogistics-d1') { throw 'Volume ownership collision; no changes performed.' }
}
$existingContainer = & $dockerExecutable ps -a --filter 'name=^/elogistics-d1-postgres18$' --format '{{.Names}}'
if ($existingContainer) {
    $labelsJson = & $dockerExecutable inspect elogistics-d1-postgres18 --format '{{json .Config.Labels}}'
    if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect container ownership.' }
    $labels = $labelsJson | ConvertFrom-Json
    if ($labels.'com.docker.compose.project' -ne 'elogistics-d1') { throw 'Container ownership collision; no changes performed.' }
}
& $dockerExecutable compose -f (Join-Path $foundationRoot 'infra/compose/postgres.yaml') config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Compose validation failed.' }
& $dockerExecutable compose -f (Join-Path $foundationRoot 'infra/compose/postgres.yaml') up -d --wait --wait-timeout 120
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL startup failed. Preserve volume and inspect logs; never reset.' }
& $dockerExecutable exec elogistics-d1-postgres18 psql -X -v ON_ERROR_STOP=1 -U elogistics_bootstrap -d elogistics_dev -c 'SELECT current_database(), version();'
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL connectivity failed.' }
