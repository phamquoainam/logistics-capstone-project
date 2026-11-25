$ErrorActionPreference = 'Stop'
$launcher = Join-Path $PSScriptRoot 'start-local.ps1'
$tokens=$null; $errors=$null
$ast=[System.Management.Automation.Language.Parser]::ParseFile($launcher,[ref]$tokens,[ref]$errors)
if ($errors.Count) { throw 'LAUNCHER_PARSE_FAILED' }
# Load function definitions only. Never execute launcher actions or read local credentials.
foreach ($definition in $ast.FindAll({ param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] },$false)) {
    . ([scriptblock]::Create($definition.Extent.Text))
}
$tempRoot=Join-Path ([IO.Path]::GetTempPath()) ('argopost-launcher-test-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tempRoot | Out-Null
function Expect-Failure([scriptblock]$action,[string]$code) {
    try { & $action; throw 'EXPECTED_FAILURE_NOT_RAISED' } catch { if ($_.Exception.Message -notlike ($code+'*')) { throw } }
}
try {
    Assert-DemoHistory @('elogistics_dev|35|0')
    foreach ($history in @('elogistics_dev|43|0','elogistics_dev|35|1','other_database|35|0','')) {
        Expect-Failure { Assert-DemoHistory @($history) } 'LOCAL_DATABASE_BASELINE_MISMATCH'
    }
    Expect-Failure { Assert-DemoHistory @('elogistics_dev|35|0','extra') } 'LOCAL_DATABASE_BASELINE_MISMATCH'
    $root=$tempRoot
    $artifactPath=Join-Path $tempRoot 'artifact.json'
    New-Item -ItemType Directory -Path (Join-Path $tempRoot '.demo/local-runtime') | Out-Null
    $syntheticJar=Join-Path $tempRoot '.demo/local-runtime/backend-v40.jar'
    [IO.File]::WriteAllText($syntheticJar,'synthetic artifact - not executable')
    $hash=(Get-FileHash $syntheticJar -Algorithm SHA256).Hash.ToLowerInvariant()
    @{path='.demo/local-runtime/backend-v40.jar';sha256=$hash;databaseVersion='35'} | ConvertTo-Json | Set-Content $artifactPath
    if ((Read-BackendArtifact) -ne $syntheticJar) { throw 'ARTIFACT_SELECTION_FAILED' }
    Add-Content $syntheticJar 'tamper'
    Expect-Failure { Read-BackendArtifact } 'LOCAL_BACKEND_ARTIFACT_HASH_MISMATCH'
    $statePath=Join-Path $tempRoot 'processes.json'
    @{backend=@{pid=123;startTicks='1'};frontend=@{pid=456;startTicks='2'}} | ConvertTo-Json | Set-Content $statePath
    function Owned-Process($record) { return $null }
    Clear-StaleProcessState | Out-Null
    if ((Test-Path $statePath) -or @(Get-ChildItem $tempRoot -Filter 'processes.json.stale-*').Count -ne 1) { throw 'STALE_STATE_NOT_PRESERVED' }
    '{}' | Set-Content $statePath
    function Owned-Process($record) { return @{pid=123} }
    Expect-Failure { Clear-StaleProcessState } 'LOCAL_STATE_EXISTS'
    if (!(Test-Path $statePath)) { throw 'LIVE_STATE_REMOVED' }
    # The start arm must never call the migration/build path.
    $switch=$ast.Find({param($node) $node -is [System.Management.Automation.Language.SwitchStatementAst]},$false)
    $startArm=($switch.Clauses | Where-Object {$_.Item1.Value -eq 'start'}).Item2.Extent.Text
    if ($startArm -match 'Prepare-Database|mvnw|profiles.active=migration' -or $startArm -notmatch 'Read-BackendArtifact' -or $startArm -notmatch 'spring.flyway.enabled=false') { throw 'START_MIGRATION_ISOLATION_FAILED' }
    Write-Output 'LOCAL_LAUNCHER_FOCUSED_TESTS_PASS'
} finally {
    # Remove only the exact synthetic files created above, never recursive cleanup.
    Get-ChildItem $tempRoot -File | Remove-Item
    if (Test-Path $syntheticJar) { Remove-Item -LiteralPath $syntheticJar }
    foreach ($directory in @((Join-Path $tempRoot '.demo/local-runtime'),(Join-Path $tempRoot '.demo'),$tempRoot)) { if(Test-Path $directory){[IO.Directory]::Delete($directory,$false)} }
}
