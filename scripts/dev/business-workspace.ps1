param(
    [Parameter(Mandatory=$true)][Guid]$RequestId,
    [Parameter(Mandatory=$true)][Guid]$OrganizationId,
    [Parameter(Mandatory=$true)][Guid]$AccountId,
    [ValidateSet('PROVISION','GRANT','REVOKE','SUSPEND')][string]$Action='PROVISION',
    [ValidateSet('OWNER','MANAGER','OPERATOR')][string]$Role='OWNER',
    [ValidateSet('dashboard.read','dashboard.finance.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read','facility.manage','defaults.manage')][string[]]$Permissions=@('dashboard.read'),
    [string]$Name,
    [Parameter(Mandatory=$true)][ValidateLength(1,300)][string]$Reason
)
$ErrorActionPreference='Stop'
if ($RequestId -eq [Guid]::Empty -or $OrganizationId -eq [Guid]::Empty -or $AccountId -eq [Guid]::Empty) { throw 'EXPLICIT_UUIDS_REQUIRED' }
if ($Reason -match '[\x00-\x1F\x7F]' -or [string]::IsNullOrWhiteSpace($Reason)) { throw 'BOUNDED_OPERATOR_REASON_REQUIRED' }
if ($Action -eq 'PROVISION' -and ($Role -ne 'OWNER' -or [string]::IsNullOrWhiteSpace($Name) -or $Name.Length -gt 160 -or $Name -match '[\x00-\x1F\x7F]')) { throw 'INITIAL_OWNER_AND_ORGANIZATION_NAME_REQUIRED' }
if ($Action -in @('PROVISION','GRANT') -and $Permissions -notcontains 'dashboard.read') { throw 'DASHBOARD_READ_PERMISSION_REQUIRED' }
if ($Action -in @('PROVISION','GRANT') -and @($Permissions | Where-Object { $_ -like 'shipment.*' -and $_ -ne 'shipment.draft.read' }).Count -gt 0 -and $Permissions -notcontains 'shipment.draft.read') { throw 'PREPARATION_READ_PERMISSION_REQUIRED' }
$taskRoot=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$taskSettings=Join-Path $taskRoot '.demo/postgres/settings.json'
if (!(Test-Path -LiteralPath $taskSettings -PathType Leaf)) { throw 'EXISTING_POSTGRES_SETTINGS_REQUIRED; NO_INIT_PERFORMED' }
$dockerCommand=Get-Command docker -ErrorAction SilentlyContinue
if ($dockerCommand) { $taskDocker=$dockerCommand.Source } else { $taskDocker=Join-Path $env:LOCALAPPDATA 'Programs/DockerDesktop/resources/bin/docker.exe' }
if (!(Test-Path -LiteralPath $taskDocker -PathType Leaf)) { throw 'DOCKER_CLI_UNAVAILABLE' }
$taskLabels=(& $taskDocker inspect --format '{{json .Config.Labels}}' elogistics-d1-postgres18) | ConvertFrom-Json
if ($LASTEXITCODE -ne 0 -or $taskLabels.'com.docker.compose.project' -ne 'elogistics-d1') { throw 'ORIGINAL_POSTGRES_IDENTITY_REQUIRED' }
$taskHealth=(& $taskDocker inspect --format '{{.State.Health.Status}}' elogistics-d1-postgres18).Trim()
if ($LASTEXITCODE -ne 0 -or $taskHealth -ne 'healthy') { throw 'EXISTING_POSTGRES_NOT_HEALTHY' }
function Sql-Literal([string]$value) { if ($null -eq $value) { return 'NULL' }; return "'"+$value.Replace("'","''")+"'" }
$taskPg=Get-Content -LiteralPath $taskSettings -Raw | ConvertFrom-Json
if ([string]::IsNullOrWhiteSpace($taskPg.migratorPassword)) { throw 'EXISTING_MIGRATOR_CONFIGURATION_REQUIRED' }
$taskPriorPassword=$env:PGPASSWORD
try {
    $env:PGPASSWORD=[string]$taskPg.migratorPassword
    $taskPermissionSql=(@($Permissions | Sort-Object -Unique | ForEach-Object { Sql-Literal $_ }) -join ',')
    $taskSql="BEGIN; SET LOCAL ROLE elogistics_owner; SET LOCAL argopost.operator_environment='LOCAL'; SELECT merchant.configure_local_workspace('$RequestId'::uuid,'$Action','$OrganizationId'::uuid,$(Sql-Literal $Name),'$AccountId'::uuid,'$Role',ARRAY[$taskPermissionSql]::text[],$(Sql-Literal $Reason)); COMMIT;"
    $taskResult=$taskSql | & $taskDocker exec --env PGPASSWORD -i elogistics-d1-postgres18 psql -X -q -tA -v ON_ERROR_STOP=1 -v VERBOSITY=sqlstate -U elogistics_migrator -d elogistics_dev 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'BUSINESS_OPERATOR_COMMAND_FAILED; NO_SUCCESS_CLAIM; CHECK_V23_TARGET_ELIGIBILITY_AND_POLICY' }
    Write-Output "BUSINESS_${Action}_PASS; REQUEST=$RequestId; DATABASE_OPERATOR_AUDIT_RECORDED"
} finally { $env:PGPASSWORD=$taskPriorPassword; $taskPg=$null; $taskResult=$null; $taskSql=$null }
