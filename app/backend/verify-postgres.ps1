param([switch]$Offline, [ValidateSet('legacy','protected')][string]$CredentialSource = 'legacy')
$ErrorActionPreference = 'Stop'
$foundationRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$testSettingsPath = & python -B (Join-Path $foundationRoot 'infra/postgres/prepare-test.py') --credential-source $CredentialSource
if ($LASTEXITCODE -ne 0) { throw 'Test database provisioning failed.' }
if ($CredentialSource -eq 'protected') {
    $testSettingsPath = & python -B (Join-Path $foundationRoot 'infra/security/credential_store.py') test-record-path --session $testSettingsPath
    if ($LASTEXITCODE -ne 0) { throw 'Protected test record invalid; no fallback.' }
    $testSettingsPath = $testSettingsPath | ConvertFrom-Json
}
$testSettings = Get-Content -LiteralPath $testSettingsPath -Raw | ConvertFrom-Json
$env:PG_TEST_DB_URL = "jdbc:postgresql://127.0.0.1:5433/$($testSettings.database)"
$env:PG_TEST_UPGRADE_DB_URL = "jdbc:postgresql://127.0.0.1:5433/$($testSettings.upgradeDatabase)"
$env:PG_TEST_OWNER_ROLE = $testSettings.ownerRole
$env:PG_TEST_RUNTIME_USER = $testSettings.runtimeUser
$env:PG_TEST_RUNTIME_PASSWORD = $testSettings.runtimePassword
$env:PG_TEST_MIGRATOR_USER = $testSettings.migratorUser
$env:PG_TEST_MIGRATOR_PASSWORD = $testSettings.migratorPassword
$verifyArguments = @('-B','-ntp',"-Dmaven.repo.local=$(Join-Path $foundationRoot '.demo/maven-repository')",'-Ddebug=false','-Dtrace=false','-Ddemo.test.database=postgres','verify')
if ($Offline) { $verifyArguments = @('-o') + $verifyArguments }
Write-Host "Isolated PostgreSQL tests: $($testSettings.database), upgrade: $($testSettings.upgradeDatabase). Databases are retained."
Push-Location $PSScriptRoot
try {
    & .\mvnw.cmd @verifyArguments
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL Maven verify failed. Inspect Surefire reports; preserve test databases.' }
} finally { Pop-Location }
