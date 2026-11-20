param(
    [Parameter(Mandatory=$true)][Guid]$AccountId,
    [Parameter(Mandatory=$true)][ValidateSet('SUPPORT_TRIAGE','SUPPORT_HANDLE')][string]$Permission,
    [ValidateSet('GRANT','REVOKE')][string]$Action='GRANT',
    [Parameter(Mandatory=$true)][Guid]$RequestId,
    [Parameter(Mandatory=$true)][ValidateLength(1,300)][string]$Reason,
    [Parameter(Mandatory=$true)][System.Management.Automation.PSCredential]$OperatorCredential
)
$ErrorActionPreference='Stop'
if ($AccountId -eq [Guid]::Empty -or $RequestId -eq [Guid]::Empty) { throw 'EXPLICIT_VERIFIED_ACCOUNT_REQUIRED' }
$taskRoot=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
if ($Reason -ne $Reason.Trim() -or $Reason -match '[\x00-\x1f\x7f]' -or $OperatorCredential.UserName -notmatch '^[a-z_][a-z0-9_]{0,62}$') { throw 'EXPLICIT_OPERATOR_AND_REASON_REQUIRED' }
$dockerCommand=Get-Command docker -ErrorAction SilentlyContinue
if ($dockerCommand) { $taskDocker=$dockerCommand.Source } else { $taskDocker=Join-Path $env:LOCALAPPDATA 'Programs/DockerDesktop/resources/bin/docker.exe' }
if (!(Test-Path -LiteralPath $taskDocker -PathType Leaf)) { throw 'DOCKER_CLI_UNAVAILABLE' }
$taskLabels=(& $taskDocker inspect --format '{{json .Config.Labels}}' elogistics-d1-postgres18) | ConvertFrom-Json
if ($LASTEXITCODE -ne 0 -or $taskLabels.'com.docker.compose.project' -ne 'elogistics-d1') { throw 'ORIGINAL_POSTGRES_IDENTITY_REQUIRED' }
$taskHealth=(& $taskDocker inspect --format '{{.State.Health.Status}}' elogistics-d1-postgres18).Trim()
if ($LASTEXITCODE -ne 0 -or $taskHealth -ne 'healthy') { throw 'EXISTING_POSTGRES_NOT_HEALTHY' }
# V37 only. Caller supplies an independently authorized operator login; never schema-owner fallback.
# Obtaining/configuring this credential and actual demo execution require separate deployment approval.
$priorPgPassword=$env:PGPASSWORD
try {
    $env:PGPASSWORD=$OperatorCredential.GetNetworkCredential().Password
    $taskUser=$OperatorCredential.UserName
    $taskPreflight="SELECT NOT rolsuper AND NOT rolcreaterole AND NOT rolcreatedb AND pg_has_role(session_user,'elogistics_local_staff_operator','MEMBER') AND NOT pg_has_role(session_user,'identity_staff_writer','SET') AND NOT pg_has_role(session_user,'support_grant_writer','SET') AND identity.local_staff_environment_matches() AND to_regprocedure('orders.configure_local_support_staff(uuid,uuid,text,text,text)') IS NOT NULL FROM pg_roles WHERE rolname=session_user;"
    $taskReady=$taskPreflight | & $taskDocker exec --env PGPASSWORD -i elogistics-d1-postgres18 psql -X -q -tA -v ON_ERROR_STOP=1 -v VERBOSITY=sqlstate -U $taskUser -d elogistics_dev 2>&1
    if ($LASTEXITCODE -ne 0 -or ([string]$taskReady).Trim() -ne 't') { throw 'V37_CURRENT_INDEPENDENT_OPERATOR_REQUIRED; NO_MUTATION_PERFORMED' }
    $taskReason=$Reason.Replace("'","''")
    $taskSql="BEGIN; SELECT * FROM orders.configure_local_support_staff('$($RequestId.ToString())'::uuid,'$($AccountId.ToString())'::uuid,'$Permission','$Action','$taskReason'); COMMIT;"
    $taskResult=$taskSql | & $taskDocker exec --env PGPASSWORD -i elogistics-d1-postgres18 psql -X -q -tA -v ON_ERROR_STOP=1 -v VERBOSITY=sqlstate -U $taskUser -d elogistics_dev 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'REQUEST_BOUND_SUPPORT_STAFF_CONFIGURATION_FAILED' }
    Write-Output "SUPPORT_STAFF_${Action}_PASS; $Permission; REQUEST_BOUND_OPERATOR_RECEIPT_RECORDED"
} finally {
    $env:PGPASSWORD=$priorPgPassword
    $taskResult=$null; $taskSql=$null; $taskReason=$null; $taskReady=$null
}
