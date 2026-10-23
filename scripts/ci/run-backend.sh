#!/usr/bin/env bash
set -euo pipefail
mode="${1:?Expected architecture, h2 or postgres}"
case "$mode" in architecture|h2|postgres) ;; *) exit 2 ;; esac
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
: "${RUNNER_TEMP:?Provide an isolated temporary directory}"
export MAVEN_USER_HOME="$RUNNER_TEMP/d1-05-maven"
if [[ -d "$MAVEN_USER_HOME" ]] && [[ -n "$(ls -A "$MAVEN_USER_HOME")" ]]; then
  echo 'FAIL: clean-runner Maven home must be empty'; exit 1
fi
private="$RUNNER_TEMP/d1-05-private"
public="$RUNNER_TEMP/d1-05-reports"
mkdir -p "$private" "$public" "$MAVEN_USER_HOME"
chmod 700 "$private" "$MAVEN_USER_HOME"
cd "$root/app/backend"
java --version > "$private/java-version.txt" 2>&1
grep -Eq '^(openjdk|java) 21[.]' "$private/java-version.txt" || { echo 'FAIL: Java 21 required'; exit 1; }
# Bootstrap capture is private, bounded when classified, and never an artifact.
bootstrap_log="$private/maven-version.txt"
(umask 077; set -o noclobber; : > "$bootstrap_log")
trap 'rm -f -- "$bootstrap_log" 2>/dev/null' EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM
if bash ./mvnw --version > "$bootstrap_log" 2>&1; then
  :
else
  bootstrap_rc=$?
  if ! python3 - "$bootstrap_log" "$bootstrap_rc" 2>/dev/null <<'BOOTSTRAP_DIAGNOSTIC'
import os, re, sys
category = 'UNKNOWN'
try:
    with open(sys.argv[1], 'rb') as source:
        captured = source.read(65537)
    if len(captured) <= 65536:
        lines = captured.decode('utf-8', errors='replace').splitlines()
        # Only literal failures emitted by the tracked wrapper; never infer from rc alone.
        signatures = {
            'DOWNLOAD_FAILED': (r'(?:wget|curl): Failed to fetch \S+',),
            'EXTRACT_FAILED': (r'failed to unzip', r'failed to untar',
                               r'Could not find Maven distribution directory in extracted archive'),
            'FILESYSTEM_FAILED': (r'cannot create temp dir', r'fail to move MAVEN_HOME'),
            'JAVA_CONFIG_FAILED': (r'The JAVA_HOME environment variable is not defined correctly, so mvnw cannot run\.',
                                   r'The java/javac command does not exist in PATH nor is JAVA_HOME set, so mvnw cannot run\.'),
            'EXEC_FAILED': (r'cannot exec \S+',),
        }
        matches = {name for name, patterns in signatures.items()
                   if any(re.fullmatch(pattern, line) for pattern in patterns for line in lines)}
        if len(matches) == 1:
            category = matches.pop()
except (OSError, ValueError):
    pass
runner_os = os.environ.get('RUNNER_OS', 'UNKNOWN')
runner_arch = os.environ.get('RUNNER_ARCH', 'UNKNOWN')
runner_os = runner_os if runner_os in {'Linux', 'Windows', 'macOS'} else 'UNKNOWN'
runner_arch = runner_arch if runner_arch in {'X64', 'X86', 'ARM', 'ARM64'} else 'UNKNOWN'
print('MAVEN_BOOTSTRAP_DIAGNOSTIC stage=WRAPPER_VERSION exit_code=' + str(int(sys.argv[2]))
      + ' category=' + category + ' runner_os=' + runner_os + ' runner_arch=' + runner_arch)
BOOTSTRAP_DIAGNOSTIC
  then
    printf 'MAVEN_BOOTSTRAP_DIAGNOSTIC stage=WRAPPER_VERSION exit_code=%s category=UNKNOWN runner_os=UNKNOWN runner_arch=UNKNOWN\n' "$bootstrap_rc"
  fi
  exit "$bootstrap_rc"
fi
grep -q 'Apache Maven 3.10.0' "$bootstrap_log" || { echo 'FAIL: Maven 3.10.0 required'; exit 1; }
rm -f -- "$bootstrap_log"
trap - EXIT HUP INT TERM
echo 'Java 21 / Maven Wrapper 3.10.0 verified with a fresh Maven home'
args=(-B -ntp -Dstyle.color=never --settings "$root/scripts/ci/maven-settings.xml"
      "-Dmaven.repo.local=$MAVEN_USER_HOME/repository" -Ddebug=false -Dtrace=false)
case "$mode" in
  architecture) args+=('-Dtest=ArchitectureTests,ArchitectureNegativeFixtureTests') ;;
  postgres)
    args+=(-Ddemo.test.database=postgres -Ptestcontainers-postgres)
    export TASK_C_PRIVATE_DIRECTORY="$private"
    export TESTCONTAINERS_RYUK_DISABLED=false
    export TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=false
    export TESTCONTAINERS_REUSE_ENABLE=false
    export TESTCONTAINERS_CHECKS_DISABLE=false
    export TESTCONTAINERS_HOST_OVERRIDE=127.0.0.1
    ;;
esac
code=0
if bash ./mvnw "${args[@]}" clean verify > "$private/maven.log" 2>&1; then code=0; else code=$?; fi
# MockMvc failure diagnostics can contain bearer tokens. Never stream or upload raw logs/XML.
if ! python3 "$root/scripts/ci/verify-reports.py" --source target/surefire-reports \
  --output "$public" --mode "$mode" --maven-exit "$code"; then code=1; fi
if ! python3 - "$private/maven.log" <<'PY'
import os, sys
from pathlib import Path
log = Path(sys.argv[1]).read_text(encoding='utf-8', errors='replace')
leak = any(value and value in log for key, value in os.environ.items()
           if key.startswith('PG_TEST_') and 'PASSWORD' in key)
print('Temporary database credential log audit: ' + ('FAIL' if leak else 'PASS'))
raise SystemExit(1 if leak else 0)
PY
then code=1; fi
if [[ "$mode" == postgres ]]; then
  if ! python3 - "$root/scripts/ci/verify-reports.py" "$private" "$public" <<'PY'
import importlib.util, sys
from pathlib import Path
spec = importlib.util.spec_from_file_location('reports', sys.argv[1])
reports = importlib.util.module_from_spec(spec)
spec.loader.exec_module(reports)
private, public = Path(sys.argv[2]), Path(sys.argv[3])
paths = [private / 'maven.log', *Path('target/surefire-reports').glob('*'), *public.glob('*')]
ok = reports.audit_task_c_credentials(private / 'task-c-secrets.txt', paths)
print('Generated Testcontainers credential privacy audit: ' + ('PASS' if ok else 'FAIL'))
cleanup = reports.audit_task_c_cleanup(private / 'task-c-runtime.txt')
print('Testcontainers owned resource cleanup audit: ' + ('PASS' if cleanup else 'FAIL'))
raise SystemExit(0 if ok and cleanup else 1)
PY
  then code=1; fi
fi
if [[ "$code" != 0 ]]; then echo 'FAIL: private Maven diagnostics retained on runner; raw logs are not artifacts'; fi
exit "$code"
