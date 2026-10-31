param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$privateRoot = Join-Path $env:LOCALAPPDATA 'ELogistics\customer-local'
if (Test-Path -LiteralPath (Join-Path $privateRoot 'runtime.json')) { throw 'LOCAL_RUNTIME_ALREADY_INITIALIZED; preserve existing keys and data.' }
New-Item -ItemType Directory -Path $privateRoot -Force | Out-Null
$sid = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value
& icacls.exe $privateRoot /inheritance:r /grant:r "*${sid}:(OI)(CI)F" '*S-1-5-18:(OI)(CI)F' '*S-1-5-32-544:(OI)(CI)F' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'LOCAL_PRIVATE_ACL_FAILED' }
& node (Join-Path $PSScriptRoot 'provision-local.mjs') $privateRoot
if ($LASTEXITCODE -ne 0) { throw 'LOCAL_KEY_PROVISION_FAILED' }
$settings = Get-Content -LiteralPath (Join-Path $privateRoot 'runtime.json') -Raw | ConvertFrom-Json
$env:LOCAL_CERT_PASSWORD = $settings.CUSTOMER_HTTPS_KEY_STORE_PASSWORD
try {
    & keytool -genkeypair -alias elogistics-local -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 825 -dname 'CN=localhost' -ext 'SAN=DNS:localhost,IP:127.0.0.1' -storetype PKCS12 -keystore $settings.CUSTOMER_HTTPS_KEY_STORE -storepass:env LOCAL_CERT_PASSWORD -keypass:env LOCAL_CERT_PASSWORD -noprompt | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'LOCAL_CERT_CREATE_FAILED' }
    & keytool -exportcert -rfc -alias elogistics-local -keystore $settings.CUSTOMER_HTTPS_KEY_STORE -storepass:env LOCAL_CERT_PASSWORD -file (Join-Path $privateRoot 'localhost.crt') | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'LOCAL_CERT_EXPORT_FAILED' }
} finally { Remove-Item Env:LOCAL_CERT_PASSWORD -ErrorAction SilentlyContinue }
Write-Output 'Local identity material and HTTPS certificate created once outside the repository.'
Write-Output ('Certificate to review/trust manually: ' + (Join-Path $privateRoot 'localhost.crt'))
Write-Output 'Next: .\scripts\dev\set-gmail-secret.ps1, then .\scripts\dev\start-local.ps1 doctor'
