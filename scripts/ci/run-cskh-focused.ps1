param(
    [Parameter(Mandatory=$true)][string]$Tests,
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$private = Join-Path $root ('.demo/cskh-focused-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $private | Out-Null
$acl = Get-Acl -LiteralPath $private
$acl.SetAccessRuleProtection($true, $false)
foreach ($sid in @([Security.Principal.WindowsIdentity]::GetCurrent().User.Value,'S-1-5-18','S-1-5-32-544')) {
    $acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new(
        [Security.Principal.SecurityIdentifier]::new($sid),'FullControl','ContainerInherit,ObjectInherit','None','Allow'))
}
Set-Acl -LiteralPath $private -AclObject $acl
$docker = (Get-Command docker -ErrorAction SilentlyContinue).Source
if (!$docker) { $docker = Join-Path $env:LOCALAPPDATA 'Programs/DockerDesktop/resources/bin/docker.exe' }
if (!(Test-Path -LiteralPath $docker)) { throw 'DOCKER_CLI_UNAVAILABLE' }
$env:DOCKER_HOST = (& $docker context inspect --format '{{.Endpoints.docker.Host}}')
if ($LASTEXITCODE -ne 0) { throw 'DOCKER_CONTEXT_UNAVAILABLE' }
$env:TESTCONTAINERS_HOST_OVERRIDE = '127.0.0.1'
$env:TESTCONTAINERS_RYUK_DISABLED = 'false'
$env:TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED = 'false'
$env:TESTCONTAINERS_REUSE_ENABLE = 'false'
$env:TESTCONTAINERS_CHECKS_DISABLE = 'false'
$env:TASK_C_PRIVATE_DIRECTORY = $private
$env:TASK_C_EXECUTION_ID = [guid]::NewGuid().ToString()
$mavenArgs = @('-B','-ntp','--settings',(Join-Path $PSScriptRoot 'maven-settings.xml'),
    ('-Dmaven.repo.local=' + (Join-Path $root '.demo/maven-repository')),
    '-Ptestcontainers-postgres',('-Dtest=' + $Tests),'test')
if ($Offline) { $mavenArgs = @('-o') + $mavenArgs }
function Source-Fingerprint {
    $records = @()
    foreach ($name in (& git -C $root ls-files --modified --others --exclude-standard | Sort-Object -Unique)) {
        if ($name -notmatch '^(app/|scripts/ci/|scripts/dev/support-staff\.ps1$)') { continue }
        $target = [IO.Path]::GetFullPath((Join-Path $root $name))
        if (!$target.StartsWith($root + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) { throw 'SOURCE_PATH_OUTSIDE_REPOSITORY' }
        $hash = if (Test-Path -LiteralPath $target -PathType Leaf) { (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant() } else { 'DELETED' }
        $records += [ordered]@{path=$name;sha256=$hash}
    }
    return ConvertTo-Json -InputObject $records -Compress
}
$sourceSnapshot = Source-Fingerprint
$snapshotFile = Join-Path $private 'source-fingerprints.json'
[IO.File]::WriteAllText($snapshotFile,$sourceSnapshot,[Text.UTF8Encoding]::new($false))
$snapshotHash = (Get-FileHash -LiteralPath $snapshotFile -Algorithm SHA256).Hash.ToLowerInvariant()
$started = [DateTime]::UtcNow
Write-Output ('PRIVATE_EVIDENCE=' + $private)
Write-Output ('SOURCE_SNAPSHOT_SHA256=' + $snapshotHash)
Push-Location (Join-Path $root 'app/backend')
try {
    & .\mvnw.cmd @mavenArgs *> (Join-Path $private 'maven.log')
    $code = $LASTEXITCODE
    $results = @()
    foreach ($file in Get-ChildItem 'target/surefire-reports/TEST-*.xml' -ErrorAction SilentlyContinue) {
        if ($file.LastWriteTimeUtc -lt $started) { continue }
        Copy-Item -LiteralPath $file.FullName -Destination (Join-Path $private $file.Name)
        [xml]$xml = [IO.File]::ReadAllText($file.FullName)
        foreach ($case in $xml.testsuite.testcase) {
            $status = if ($case.failure) {'FAIL'} elseif ($case.error) {'ERROR'} elseif ($case.skipped) {'SKIP'} else {'PASS'}
            $results += [pscustomobject]@{suite=[string]$case.classname;test=[string]$case.name;status=$status}
        }
    }
    $unchanged = (Source-Fingerprint) -ceq $sourceSnapshot
    $summary = [pscustomobject]@{mavenExit=$code;sourceSnapshotSha256=$snapshotHash;sourceUnchanged=$unchanged;tests=$results.Count;passed=@($results | Where-Object status -eq 'PASS').Count;
        failed=@($results | Where-Object status -eq 'FAIL').Count;errors=@($results | Where-Object status -eq 'ERROR').Count;
        skipped=@($results | Where-Object status -eq 'SKIP').Count;cases=$results}
    $summary | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 (Join-Path $private 'summary.json')
    Write-Output ('MAVEN_EXIT=' + $code + ' FRESH_TESTS=' + $summary.tests + ' PASS=' + $summary.passed + ' FAIL=' + $summary.failed + ' ERROR=' + $summary.errors + ' SKIP=' + $summary.skipped)
    Write-Output ('SOURCE_UNCHANGED=' + $unchanged)
    foreach ($result in $results | Where-Object { $_.status -in @('FAIL','ERROR') }) {
        Write-Output ($result.status + ' ' + $result.suite + '.' + $result.test)
    }
    if ($results.Count -eq 0 -and $code -eq 0) { $code=1 }
} finally { Pop-Location }
exit $code
