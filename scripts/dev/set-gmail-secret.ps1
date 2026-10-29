param()
$ErrorActionPreference = 'Stop'
$privateRoot = Join-Path $env:LOCALAPPDATA 'ELogistics\customer-local'
if (!(Test-Path -LiteralPath (Join-Path $privateRoot 'runtime.json'))) { throw 'Run initialize-local.ps1 first.' }
$sender = Read-Host 'Gmail sender address'
if ($sender -notmatch '^[^@\s]+@gmail\.com$') { throw 'GMAIL_SENDER_INVALID' }
$password = Read-Host 'Google App Password (hidden; spaces may be pasted)' -AsSecureString
$credential = New-Object Security.Management.Automation.PSCredential($sender, $password)
$normalized = $credential.GetNetworkCredential().Password -replace '\s',''
if ($normalized.Length -ne 16) { throw 'GMAIL_APP_PASSWORD_LENGTH_INVALID' }
$sealed = ConvertFrom-SecureString -SecureString (ConvertTo-SecureString -String $normalized -AsPlainText -Force)
$normalized = $null
$path = Join-Path $privateRoot 'gmail.json'
$temporary = Join-Path $privateRoot ('gmail-' + [guid]::NewGuid().ToString('N') + '.tmp')
try {
    [IO.File]::WriteAllText($temporary, ([ordered]@{ sender=$sender; dpapi=$sealed } | ConvertTo-Json -Compress))
    Move-Item -LiteralPath $temporary -Destination $path -Force
} finally { if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary } }
Write-Output 'Gmail sender and DPAPI-protected App Password saved outside the repository for this Windows account.'
