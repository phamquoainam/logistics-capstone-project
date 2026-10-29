# One-shot local readiness check without Gmail or account mutation. Never used as live SMTP evidence.
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$privateRoot = Join-Path $env:LOCALAPPDATA 'ELogistics\customer-local'
$settings = Get-Content -LiteralPath (Join-Path $privateRoot 'runtime.json') -Raw | ConvertFrom-Json
$pg = Get-Content -LiteralPath (Join-Path $root '.demo/postgres/settings.json') -Raw | ConvertFrom-Json
foreach ($property in $settings.PSObject.Properties) { [Environment]::SetEnvironmentVariable($property.Name,[string]$property.Value,'Process') }
$env:PG_DB_URL = 'jdbc:postgresql://127.0.0.1:5433/elogistics_dev'
$env:PG_RUNTIME_USER = 'elogistics_runtime'
$env:PG_RUNTIME_PASSWORD = $pg.runtimePassword
$env:PG_MIGRATION_ENABLED = 'false'
$env:NODE_EXTRA_CA_CERTS = Join-Path $privateRoot 'localhost.crt'
$env:PUBLIC_MEDIA_LOCAL_ROOT = Join-Path $root 'app/frontend/web/public/media'
$env:CUSTOMER_SMTP_MODE = 'TEST_LOOPBACK'
$env:CUSTOMER_SMTP_HOST = '127.0.0.1'
$env:CUSTOMER_SMTP_PORT = '2525'
$env:CUSTOMER_SMTP_USERNAME = $null
$env:CUSTOMER_SMTP_PASSWORD = $null
$env:CUSTOMER_SMTP_SENDER = 'local@example.invalid'
$env:CUSTOMER_SMTP_APPROVED_SENDER_DOMAINS = 'example.invalid'
$jar = Join-Path $root 'app/backend/target/backend-0.0.1-SNAPSHOT.jar'
if (!(Test-Path -LiteralPath $jar)) { throw 'BACKEND_JAR_MISSING' }
$process = Start-Process -FilePath (Get-Command java).Source -ArgumentList @('-jar',('"'+$jar+'"'),'--spring.profiles.active=identity,customer,postgres','--debug=false','--trace=false') -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $privateRoot 'backend-smoke.out.log') -RedirectStandardError (Join-Path $privateRoot 'backend-smoke.err.log')
try {
    for ($attempt=0; $attempt -lt 60; $attempt++) {
        if ($process.HasExited) { throw 'BACKEND_SMOKE_EXITED; inspect private backend-smoke logs' }
        & node (Join-Path $PSScriptRoot 'check-https.mjs') 'https://127.0.0.1:8443/api/v1/public-media/hero-network.svg' 2>$null
        if ($LASTEXITCODE -eq 0) { Write-Output 'BACKEND_HTTPS_POSTGRES_S3_MEDIA_READY; SMTP_TEST_LOOPBACK_ONLY'; return }
        Start-Sleep -Seconds 2
    }
    throw 'BACKEND_SMOKE_READINESS_TIMEOUT; inspect private backend-smoke logs'
} finally {
    if (!$process.HasExited) { Stop-Process -Id $process.Id }
    $env:PG_RUNTIME_PASSWORD = $null
}
