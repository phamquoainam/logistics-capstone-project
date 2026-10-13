param([switch]$InitializeOnly)
$ErrorActionPreference = 'Stop'
$demoRepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$demoDirectory = Join-Path $demoRepoRoot '.demo'
$demoSettingsPath = Join-Path $demoDirectory 'settings.json'
function New-DemoSecret {
    $randomBytes = New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($randomBytes)
    [Convert]::ToBase64String($randomBytes)
}
New-Item -ItemType Directory -Path (Join-Path $demoDirectory 'data') -Force | Out-Null
if (!(Test-Path -LiteralPath $demoSettingsPath)) {
    [ordered]@{ tokenSecret=(New-DemoSecret); databasePassword=(New-DemoSecret); adminPassword=(New-DemoSecret); merchantAPassword=(New-DemoSecret); merchantBPassword=(New-DemoSecret); operatorPassword=(New-DemoSecret); disabledPassword=(New-DemoSecret) } |
        ConvertTo-Json | Set-Content -LiteralPath $demoSettingsPath -Encoding UTF8
    Write-Host 'Generated local credentials in ignored .demo/settings.json. Keep this file private.'
}
if ($InitializeOnly) { return }
$demoSettings = Get-Content -LiteralPath $demoSettingsPath -Raw | ConvertFrom-Json
$demoDatabasePath = (Join-Path $demoDirectory 'data/elogistics').Replace('\','/')
$env:SPRING_PROFILES_ACTIVE = 'demo'
$env:DEMO_DB_URL = "jdbc:h2:file:$demoDatabasePath;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_ON_EXIT=FALSE"
$env:DEMO_DB_USER = 'demo_local'
$env:DEMO_DB_PASSWORD = $demoSettings.databasePassword
$env:DEMO_TOKEN_SECRET = $demoSettings.tokenSecret
$env:DEMO_SEED_ENABLED = 'true'
$env:DEMO_ADMIN_PASSWORD = $demoSettings.adminPassword
$env:DEMO_MERCHANT_A_PASSWORD = $demoSettings.merchantAPassword
$env:DEMO_MERCHANT_B_PASSWORD = $demoSettings.merchantBPassword
$env:DEMO_OPERATOR_PASSWORD = $demoSettings.operatorPassword
$env:DEMO_DISABLED_PASSWORD = $demoSettings.disabledPassword
Push-Location $PSScriptRoot
try {
    $demoJar = Join-Path $PSScriptRoot 'target/backend-0.0.1-SNAPSHOT.jar'
    if (!(Test-Path -LiteralPath $demoJar)) { throw 'Run mvnw.cmd verify first to build the demo jar.' }
    & java -jar $demoJar --debug=false --trace=false
    if ($LASTEXITCODE -ne 0) { throw "Backend exited with code $LASTEXITCODE" }
} finally { Pop-Location }
