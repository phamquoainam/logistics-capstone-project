param()
$ErrorActionPreference = 'Stop'
$privateRoot = Join-Path $env:LOCALAPPDATA 'ELogistics\customer-local'
if (!(Test-Path -LiteralPath (Join-Path $privateRoot 'runtime.json'))) { throw 'Run initialize-local.ps1 first.' }
$sender = Read-Host 'Gmail sender address'
if ($sender -notmatch '^[^@\s]+@gmail\.com$') { throw 'GMAIL_SENDER_INVALID' }
$password = Read-Host 'Google App Password (hidden)' -AsSecureString
if ($password.Length -eq 0) { throw 'GMAIL_SECRET_EMPTY' }
$sealed = ConvertFrom-SecureString -SecureString $password
$path = Join-Path $privateRoot 'gmail.json'
$temporary = Join-Path $privateRoot ('gmail-' + [guid]::NewGuid().ToString('N') + '.tmp')
try {
    [IO.File]::WriteAllText($temporary, ([ordered]@{ sender=$sender; dpapi=$sealed } | ConvertTo-Json -Compress))
    Move-Item -LiteralPath $temporary -Destination $path -Force
} finally { if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary } }
Write-Output 'Gmail sender and DPAPI-protected App Password saved outside the repository for this Windows account.'
