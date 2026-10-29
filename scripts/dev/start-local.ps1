param([ValidateSet('doctor','migrate','start','status','stop')][string]$Action = 'start')
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$privateRoot = Join-Path $env:LOCALAPPDATA 'ELogistics\customer-local'
$settingsPath = Join-Path $privateRoot 'runtime.json'
$gmailPath = Join-Path $privateRoot 'gmail.json'
$statePath = Join-Path $privateRoot 'processes.json'
$jar = Join-Path $root 'app/backend/target/backend-0.0.1-SNAPSHOT.jar'
$frontend = Join-Path $root 'app/frontend/web'
$postgresSettingsPath = Join-Path $root '.demo/postgres/settings.json'

function Require-File([string]$path, [string]$code) { if (!(Test-Path -LiteralPath $path -PathType Leaf)) { throw $code } }
function Docker-Path {
    $command = Get-Command docker -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $fallback = Join-Path $env:LOCALAPPDATA 'Programs/DockerDesktop/resources/bin/docker.exe'
    if (Test-Path -LiteralPath $fallback -PathType Leaf) { return $fallback }
    throw 'DOCKER_CLI_UNAVAILABLE'
}
function Read-Settings {
    Require-File $settingsPath 'LOCAL_RUNTIME_NOT_INITIALIZED'
    return Get-Content -LiteralPath $settingsPath -Raw | ConvertFrom-Json
}
function Set-ProcessEnvironment($settings) {
    foreach ($property in $settings.PSObject.Properties) { [Environment]::SetEnvironmentVariable($property.Name, [string]$property.Value, 'Process') }
    $env:PG_DB_URL = 'jdbc:postgresql://127.0.0.1:5433/elogistics_dev'
    $env:PG_RUNTIME_USER = 'elogistics_runtime'
    $env:PG_MIGRATOR_USER = 'elogistics_migrator'
    $env:PG_OWNER_ROLE = 'elogistics_owner'
    $pg = Get-Content -LiteralPath $postgresSettingsPath -Raw | ConvertFrom-Json
    $env:PG_RUNTIME_PASSWORD = $pg.runtimePassword
    $env:PG_MIGRATOR_PASSWORD = $pg.migratorPassword
    $env:NODE_EXTRA_CA_CERTS = Join-Path $privateRoot 'localhost.crt'
    $env:ELOGISTICS_CUSTOMER_DEV_TLS_PFX = $settings.CUSTOMER_HTTPS_KEY_STORE
    $env:ELOGISTICS_CUSTOMER_DEV_TLS_PFX_PASSWORD = $settings.CUSTOMER_HTTPS_KEY_STORE_PASSWORD
    $env:ELOGISTICS_CUSTOMER_DEV_API_ORIGIN = 'https://127.0.0.1:8443'
    $env:PUBLIC_MEDIA_LOCAL_ROOT = Join-Path $frontend 'public/media'
    $env:SPRING_PROFILES_ACTIVE = $null
    $env:JAVA_TOOL_OPTIONS = $null
}
function Test-Https([string]$url) {
    & node (Join-Path $PSScriptRoot 'check-https.mjs') $url 2>$null
    return $LASTEXITCODE -eq 0
}
function Process-Record($process, [string]$role) {
    if ($null -eq $process) { throw "${role}_PROCESS_NOT_CREATED" }
    try {
        # Pin the original OS process handle before recording its identity.
        $handle = $process.Handle
        if ($null -eq $handle -or $handle -eq [IntPtr]::Zero) { throw "${role}_HANDLE_UNAVAILABLE" }
        if ($process.HasExited) { throw "${role}_EXITED_BEFORE_OWNERSHIP" }
        $started = $process.StartTime
        if ($null -eq $started) { throw "${role}_START_TIME_UNAVAILABLE" }
        if ([int]$process.Id -le 0) { throw "${role}_PID_INVALID" }
        $record = @{ pid=[int]$process.Id; startTicks=$started.ToUniversalTime().Ticks.ToString() }
        if ($process.HasExited) { throw "${role}_EXITED_BEFORE_OWNERSHIP" }
        return $record
    } catch [System.InvalidOperationException] { throw "${role}_EXITED_BEFORE_OWNERSHIP" }
}
function Owned-Process($record) {
    if ($null -eq $record -or [string]$record.pid -notmatch '^[1-9][0-9]*$' -or [string]$record.startTicks -notmatch '^[1-9][0-9]*$') { return $null }
    $process = Get-Process -Id ([int]$record.pid) -ErrorAction SilentlyContinue
    if ($null -eq $process) { return $null }
    try {
        $handle = $process.Handle
        if ($null -eq $handle -or $handle -eq [IntPtr]::Zero -or $process.HasExited -or $process.Id -ne [int]$record.pid) { return $null }
        $started = $process.StartTime
        if ($null -ne $started -and $started.ToUniversalTime().Ticks.ToString() -eq [string]$record.startTicks) { return $process }
    } catch [System.InvalidOperationException] { return $null }
    catch [System.ComponentModel.Win32Exception] { return $null }
    return $null
}
function Stop-OwnedProcess($record) {
    $owned = Owned-Process $record
    if ($null -eq $owned) { return $false }
    try { $owned.Kill(); if (!$owned.WaitForExit(5000)) { throw 'OWNED_PROCESS_STOP_TIMEOUT' }; return $true }
    catch [System.InvalidOperationException] { return $false }
}
function Save-Process($backendRecord, $frontendRecord) {
    if ($null -eq (Owned-Process $backendRecord) -or $null -eq (Owned-Process $frontendRecord)) { throw 'LOCAL_PROCESS_EXITED_BEFORE_REGISTRATION' }
    [ordered]@{
        backend=$backendRecord
        frontend=$frontendRecord
    } | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $statePath -Encoding UTF8
}
function Doctor {
    $settings = Read-Settings
    Require-File $postgresSettingsPath 'POSTGRES_SETTINGS_MISSING'
    Require-File $settings.CUSTOMER_HTTPS_KEY_STORE 'HTTPS_KEYSTORE_MISSING'
    Require-File (Join-Path $privateRoot 'localhost.crt') 'HTTPS_CERTIFICATE_MISSING'
    foreach ($command in @('java','node','npm.cmd','aws','keytool')) { if (!(Get-Command $command -ErrorAction SilentlyContinue)) { throw ($command + '_UNAVAILABLE') } }
    if (!(Test-Path -LiteralPath (Join-Path $frontend 'node_modules/vite/bin/vite.js'))) { throw 'FRONTEND_DEPENDENCIES_MISSING; run npm.cmd ci in app/frontend/web' }
    $docker = Docker-Path
    & $docker version --format '{{.Server.Version}}' 1>$null 2>$null
    if ($LASTEXITCODE -ne 0) { throw 'DOCKER_ENGINE_UNAVAILABLE' }
    $postgresHealth = & $docker inspect elogistics-d1-postgres18 --format '{{.State.Health.Status}}' 2>$null
    if ($LASTEXITCODE -eq 0 -and $postgresHealth -eq 'healthy') { Write-Output 'POSTGRES_18_CONTAINER_HEALTHY' }
    else { Write-Output 'POSTGRES_NOT_HEALTHY; start will use the existing PostgreSQL launcher.' }
    & aws s3api head-bucket --bucket $settings.PUBLIC_MEDIA_S3_BUCKET --region $settings.PUBLIC_MEDIA_S3_REGION --profile $settings.AWS_PROFILE 1>$null 2>$null
    if ($LASTEXITCODE -ne 0) { throw 'S3_PROFILE_OR_BUCKET_UNAVAILABLE' }
    $cert = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2((Join-Path $privateRoot 'localhost.crt'))
    $trusted = @(Get-ChildItem Cert:\CurrentUser\Root | Where-Object { $_.Thumbprint -eq $cert.Thumbprint }).Count -gt 0
    Write-Output 'JAVA_NODE_DOCKER_AWS_CONFIG_PASS'
    if ($trusted) { Write-Output 'BROWSER_CERT_TRUST_PASS' } else { Write-Output 'BROWSER_CERT_TRUST_PENDING: import localhost.crt into Current User Trusted Root only after reviewing it.' }
    if (Test-Path -LiteralPath $gmailPath) { Write-Output 'GMAIL_CONFIG_PRESENT; LIVE_DELIVERY_UNVERIFIED' }
    else { Write-Output 'GMAIL_SECRET_PENDING; run local.cmd gmail before start.' }
}
function Prepare-Database($settings) {
    $docker = Docker-Path
    & (Join-Path $root 'infra/postgres/start-local.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'POSTGRES_START_FAILED' }
    $health = & $docker inspect elogistics-d1-postgres18 --format '{{.State.Health.Status}}' 2>$null
    if ($LASTEXITCODE -ne 0 -or $health -ne 'healthy') { throw 'POSTGRES_NOT_HEALTHY' }
    & (Join-Path $root 'app/backend/mvnw.cmd') -q -f (Join-Path $root 'app/backend/pom.xml') '-DskipTests' package
    if ($LASTEXITCODE -ne 0) { throw 'BACKEND_BUILD_FAILED' }
    Require-File $jar 'BACKEND_JAR_MISSING'
    Set-ProcessEnvironment $settings
    try {
        # One-shot migration principal; runtime receives only the runtime DB principal.
        & java -jar $jar '--spring.profiles.active=migration' '--debug=false' '--trace=false' *> (Join-Path $privateRoot 'migration.log')
        if ($LASTEXITCODE -ne 0) { throw 'FLYWAY_MIGRATION_FAILED; inspect private migration.log' }
    } finally { $env:PG_MIGRATOR_PASSWORD = $null; $env:PG_MIGRATOR_USER = $null }
    Write-Output 'POSTGRES_HEALTHY_FLYWAY_MIGRATE_PASS'
}
switch ($Action) {
    'doctor' { Doctor; break }
    'migrate' {
        Doctor
        $settings = Read-Settings
        try { Prepare-Database $settings } finally { $env:PG_RUNTIME_PASSWORD=$null; $env:ELOGISTICS_CUSTOMER_DEV_TLS_PFX_PASSWORD=$null }
        break
    }
    'status' {
        if (!(Test-Path -LiteralPath $statePath)) { Write-Output 'LOCAL_RUNTIME_STOPPED'; break }
        $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
        $settings = Read-Settings
        $env:NODE_EXTRA_CA_CERTS = Join-Path $privateRoot 'localhost.crt'
        $backendUp = (Owned-Process $state.backend) -and (Test-Https 'https://127.0.0.1:8443/api/v1/public-media/hero-network.svg')
        $frontendUp = (Owned-Process $state.frontend) -and (Test-Https 'https://127.0.0.1:5173/')
        if ($backendUp -and $frontendUp) { Write-Output 'LOCAL_FULL_STACK_HTTP_READY' } else { Write-Output 'LOCAL_RUNTIME_NOT_READY' }
        break
    }
    'stop' {
        if (!(Test-Path -LiteralPath $statePath)) { Write-Output 'LOCAL_RUNTIME_STOPPED'; break }
        $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
        $stopped = 0
        foreach ($record in @($state.frontend,$state.backend)) { if (Stop-OwnedProcess $record) { $stopped++ } }
        Remove-Item -LiteralPath $statePath
        Write-Output ("OWNED_LOCAL_PROCESSES_STOPPED=$stopped; PostgreSQL and data preserved.")
        break
    }
    'start' {
        Doctor
        Require-File $gmailPath 'GMAIL_SECRET_PENDING; run local.cmd gmail before start.'
        if (Test-Path -LiteralPath $statePath) { throw 'LOCAL_STATE_EXISTS; inspect status or stop before restart.' }
        $settings = Read-Settings
        Prepare-Database $settings
        $env:PG_MIGRATION_ENABLED = 'false'
        $gmail = Get-Content -LiteralPath $gmailPath -Raw | ConvertFrom-Json
        $secure = ConvertTo-SecureString -String $gmail.dpapi
        $credential = [System.Management.Automation.PSCredential]::new($gmail.sender, $secure)
        $env:CUSTOMER_SMTP_USERNAME = $gmail.sender
        $env:CUSTOMER_SMTP_SENDER = $gmail.sender
        $env:CUSTOMER_SMTP_APPROVED_SENDER_DOMAINS = ($gmail.sender -split '@')[-1]
        $env:CUSTOMER_SMTP_PASSWORD = $credential.GetNetworkCredential().Password
        $backendRecord = $null; $frontendRecord = $null
        try {
            $backendProcess = Start-Process -FilePath (Get-Command java).Source -ArgumentList @('-jar',('"'+$jar+'"'),'--spring.profiles.active=identity,customer,postgres','--debug=false','--trace=false') -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $privateRoot 'backend.out.log') -RedirectStandardError (Join-Path $privateRoot 'backend.err.log')
            $backendRecord = Process-Record $backendProcess 'BACKEND'
            $env:CUSTOMER_SMTP_PASSWORD = $null
            for ($attempt=0; $attempt -lt 60; $attempt++) { if ($null -eq (Owned-Process $backendRecord)) { throw 'BACKEND_EXITED_BEFORE_READY' }; if (Test-Https 'https://127.0.0.1:8443/api/v1/public-media/hero-network.svg') { break }; Start-Sleep -Seconds 2 }
            if (!(Test-Https 'https://127.0.0.1:8443/api/v1/public-media/hero-network.svg')) { throw 'BACKEND_READINESS_TIMEOUT' }
            # The frontend child must never inherit database, AWS profile, SMTP or signing material.
            foreach ($property in $settings.PSObject.Properties) { [Environment]::SetEnvironmentVariable($property.Name,$null,'Process') }
            foreach ($name in @('PG_DB_URL','PG_RUNTIME_USER','PG_RUNTIME_PASSWORD','PG_MIGRATION_ENABLED','PG_OWNER_ROLE','PG_MIGRATOR_USER','PG_MIGRATOR_PASSWORD','CUSTOMER_SMTP_USERNAME','CUSTOMER_SMTP_SENDER','CUSTOMER_SMTP_APPROVED_SENDER_DOMAINS','CUSTOMER_SMTP_PASSWORD','PUBLIC_MEDIA_LOCAL_ROOT')) { [Environment]::SetEnvironmentVariable($name,$null,'Process') }
            $vite = Join-Path $frontend 'node_modules/vite/bin/vite.js'
            $frontendProcess = Start-Process -FilePath (Get-Command node).Source -ArgumentList @(('"'+$vite+'"'),'--host','127.0.0.1','--mode','customer') -WorkingDirectory $frontend -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $privateRoot 'frontend.out.log') -RedirectStandardError (Join-Path $privateRoot 'frontend.err.log')
            $frontendRecord = Process-Record $frontendProcess 'FRONTEND'
            for ($attempt=0; $attempt -lt 30; $attempt++) { if ($null -eq (Owned-Process $frontendRecord)) { throw 'FRONTEND_EXITED_BEFORE_READY' }; if (Test-Https 'https://127.0.0.1:5173/') { break }; Start-Sleep -Seconds 2 }
            if (!(Test-Https 'https://127.0.0.1:5173/')) { throw 'FRONTEND_READINESS_TIMEOUT' }
            Save-Process $backendRecord $frontendRecord
            Write-Output 'LOCAL_FULL_STACK_HTTP_READY; Gmail real delivery requires an Owner-controlled inbox test.'
            Write-Output 'Public Website: https://127.0.0.1:5173/'
            Write-Output 'Registration: https://127.0.0.1:5173/register'
            Write-Output 'Login: https://127.0.0.1:5173/login'
            Write-Output 'Recovery: https://127.0.0.1:5173/recover'
        } catch {
            $startupFailure = $_
            $cleanupErrors = @()
            foreach ($record in @($frontendRecord,$backendRecord)) {
                if ($null -ne $record) { try { $null = Stop-OwnedProcess $record } catch { $cleanupErrors += $_.Exception } }
            }
            if ($cleanupErrors.Count -gt 0) { throw [AggregateException]::new('LOCAL_STARTUP_AND_CLEANUP_FAILED', [Exception[]](@($startupFailure.Exception) + $cleanupErrors)) }
            throw $startupFailure
        } finally { $env:CUSTOMER_SMTP_PASSWORD=$null; $env:PG_RUNTIME_PASSWORD=$null; $env:ELOGISTICS_CUSTOMER_DEV_TLS_PFX_PASSWORD=$null }
        break
    }
}
