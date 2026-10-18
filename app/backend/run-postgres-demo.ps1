param([switch]$MigrationOnly, [switch]$RuntimeOnly,
      [ValidateSet('legacy','protected')][string]$CredentialSource = 'legacy')
$ErrorActionPreference = 'Stop'
if ($MigrationOnly -and $RuntimeOnly) { throw 'Choose only one execution mode.' }
$foundationRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
if ($CredentialSource -eq 'protected') {
    $protectedPaths = & python -B (Join-Path $foundationRoot 'infra/security/credential_store.py') paths --mode postgres
    if ($LASTEXITCODE -ne 0) { throw 'Protected settings unavailable; no legacy fallback.' }
    $protectedPaths = $protectedPaths | ConvertFrom-Json
    $demoSettingsPath = $protectedPaths.'demo-settings.json'
    $pgSettingsPath = $protectedPaths.'postgres-settings.json'
} else {
    & (Join-Path $PSScriptRoot 'run-demo.ps1') -InitializeOnly
    $demoSettingsPath = Join-Path $foundationRoot '.demo/settings.json'
    $pgSettingsPath = Join-Path $foundationRoot '.demo/postgres/settings.json'
}
$demoSettings = Get-Content -LiteralPath $demoSettingsPath -Raw | ConvertFrom-Json
if (!(Test-Path -LiteralPath $pgSettingsPath)) { throw 'Run infra/postgres/start-local.ps1 first.' }
$pgSettings = Get-Content -LiteralPath $pgSettingsPath -Raw | ConvertFrom-Json
$env:SPRING_PROFILES_ACTIVE = 'demo,postgres'
if ($MigrationOnly) { $env:SPRING_PROFILES_ACTIVE = 'migration' }
$env:PG_DB_URL = 'jdbc:postgresql://127.0.0.1:5433/elogistics_dev'
$env:PG_RUNTIME_USER = 'elogistics_runtime'
$env:PG_RUNTIME_PASSWORD = $pgSettings.runtimePassword
$env:PG_MIGRATOR_USER = 'elogistics_migrator'
$env:PG_MIGRATOR_PASSWORD = $pgSettings.migratorPassword
$env:PG_OWNER_ROLE = 'elogistics_owner'
$env:PG_MIGRATION_ENABLED = 'true'
$env:DEMO_TOKEN_SECRET = $demoSettings.tokenSecret
$env:DEMO_SEED_ENABLED = 'true'
$env:DEMO_ADMIN_PASSWORD = $demoSettings.adminPassword
$env:DEMO_MERCHANT_A_PASSWORD = $demoSettings.merchantAPassword
$env:DEMO_MERCHANT_B_PASSWORD = $demoSettings.merchantBPassword
$env:DEMO_OPERATOR_PASSWORD = $demoSettings.operatorPassword
$env:DEMO_DISABLED_PASSWORD = $demoSettings.disabledPassword
if ($RuntimeOnly) {
    $env:PG_MIGRATION_ENABLED = 'false'
    $env:PG_MIGRATOR_USER = $null
    $env:PG_MIGRATOR_PASSWORD = $null
    $env:PG_OWNER_ROLE = $null
    $env:DEMO_SEED_ENABLED = 'false'
}
$jarPath = Join-Path $PSScriptRoot 'target/backend-0.0.1-SNAPSHOT.jar'
if (!(Test-Path -LiteralPath $jarPath)) { throw 'Build backend with mvnw.cmd verify first.' }
& java -jar $jarPath --debug=false --trace=false
if ($LASTEXITCODE -ne 0) { throw "Backend exited with code $LASTEXITCODE" }
