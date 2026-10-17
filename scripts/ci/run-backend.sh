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
if ! bash ./mvnw --version > "$private/maven-version.txt" 2>&1; then
  echo 'FAIL: Maven Wrapper bootstrap; private diagnostics retained'; exit 1
fi
grep -q 'Apache Maven 3.10.0' "$private/maven-version.txt" || { echo 'FAIL: Maven 3.10.0 required'; exit 1; }
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
# Temporary AC-001-03 reporter: only an exact, allowlisted ArchUnit witness.
# Does not affect Maven exit status, report guards or artifact policy.
if [[ "$code" != 0 ]]; then
  python3 - "$code" <<'PY'
import re, sys, xml.etree.ElementTree as ET
from pathlib import Path
suite = 'com.elogistic.backend.architecture.ArchitectureNegativeFixtureTests'
source = 'com.elogistic.backend.finance.internal.application.ActualCiForbiddenDependency'
target = 'com.elogistic.backend.orders.internal.infrastructure.ShipmentRepository'
path = Path('target/surefire-reports') / ('TEST-' + suite + '.xml')
pattern = (re.escape('Forbidden cross-module implementation dependency: ' + source + ' -> ' + target)
           + r'; Field <' + re.escape(source) + r'\.forbidden> has type <' + re.escape(target)
           + r'> in \(ActualCiForbiddenDependency\.java:\d+\)')
try:
    root = ET.parse(path).getroot()
    for case in root.findall('testcase'):
        if case.get('classname') != suite or case.get('name') != 'financeCannotUseOrderInternalRepository':
            continue
        failure = case.find('failure')
        if failure is not None and failure.get('type') == 'java.lang.AssertionError':
            for line in (failure.text or '').splitlines():
                if re.fullmatch(pattern, line):
                    print('AC-001-03 actual ArchUnit witness: ' + line)
                    print('AC-001-03 Maven exit status: ' + str(int(sys.argv[1])))
except (OSError, ET.ParseError):
    print('AC-001-03 witness unavailable; no private diagnostics published')
PY
fi
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
