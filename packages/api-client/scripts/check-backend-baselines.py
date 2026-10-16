"""C-06 local Maven/PG regression, reusing CI provisioning and fail-closed reports."""
import json
import os
from pathlib import Path
import re
import runpy
import socket
import subprocess
import sys
import uuid

ROOT = Path(__file__).resolve().parents[3]
CI = runpy.run_path(str(ROOT / 'scripts/ci/prepare-postgres.py'))
# Resolve the same immutable bytes when the local image has no remaining tag.
# This affects only this in-memory local invocation, not the CI source/workflow.
CI['IMAGE'] = CI['IMAGE'].replace(':18.6-bookworm@sha256:', '@sha256:')
CI['start'].__globals__['IMAGE'] = CI['IMAGE']
OUT = ROOT / '.demo/d1-07-c06' / ('backend-' + uuid.uuid4().hex[:12])


def port_available():
    try:
        with socket.socket() as sock:
            sock.bind(('127.0.0.1', 5433))
        return True
    except OSError:
        return False


def owned(state, container_id):
    CI['assert_owned'](state)
    info = json.loads(CI['docker']('inspect', state['name']))[0]
    if info['Id'] != container_id or '/var/lib/postgresql' not in info['HostConfig']['Tmpfs']:
        raise RuntimeError('Disposable PostgreSQL identity/tmpfs mismatch')
    if any(mount['Type'] == 'volume' for mount in info['Mounts']):
        raise RuntimeError('Test PostgreSQL must have no persistent data volumes')
    return info


def run_mode(mode, env):
    folder = OUT / mode
    folder.mkdir()
    args = ['cmd.exe', '/d', '/c', 'mvnw.cmd', '-o', '-B', '-ntp',
            f'-Dmaven.repo.local={ROOT / ".demo/maven-repository"}', '-Ddebug=false', '-Dtrace=false']
    if mode == 'architecture':
        args.append('-Dtest=ArchitectureTests,ArchitectureNegativeFixtureTests')
    if mode == 'postgres':
        args.append('-Ddemo.test.database=postgres')
    args += ['clean', 'verify']
    with (folder / 'maven-private.log').open('wb') as log:
        result = subprocess.run(args, cwd=ROOT / 'app/backend', env=env, stdout=log,
                                stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL, timeout=240)
    guard = subprocess.run([sys.executable, '-B', str(ROOT / 'scripts/ci/verify-reports.py'),
        '--source', str(ROOT / 'app/backend/target/surefire-reports'), '--output', str(folder / 'safe-reports'),
        '--mode', mode, '--maven-exit', str(result.returncode)], cwd=ROOT, timeout=30)
    content = (folder / 'maven-private.log').read_text(encoding='utf-8', errors='replace')
    secret_values = [value for key, value in env.items() if key.startswith('PG_TEST_') and 'PASSWORD' in key]
    privacy = not any(value and value in content for value in secret_values) and not re.search(
        r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', content)
    record = dict(mode=mode, mavenExit=result.returncode, reportGuardExit=guard.returncode,
                  logPrivacy='PASS' if privacy else 'FAIL')
    (folder / 'safe-result.json').write_text(json.dumps(record, indent=2) + '\n', encoding='utf-8')
    print(f'{mode}: Maven exit{result.returncode}, guard exit{guard.returncode}, bounded log privacy {record["logPrivacy"]}', flush=True)
    if result.returncode or guard.returncode or not privacy:
        raise RuntimeError('Backend regression failed; raw diagnostics are private')
    return record


def run():
    OUT.mkdir(parents=True, exist_ok=False)
    OUT.chmod(0o700)
    baseline_containers = set(CI['docker']('ps', '-a', '-q', '--no-trunc').splitlines())
    baseline_volumes = set(CI['docker']('volume', 'ls', '-q').splitlines())
    # Do not download/change the approved image, stop a listener or retarget tests.
    CI['docker']('image', 'inspect', CI['IMAGE'])
    if not port_available():
        raise RuntimeError('BLOCKED: existing listener on5433; developer resources will not be stopped')
    env = {key: value for key, value in os.environ.items()
           if not key.startswith(('PG_', 'PG_TEST_', 'DEMO_', 'SPRING_'))}
    records = []
    state = None
    container_id = None
    cleanup = dict(ownedContainerRemoved=False, existingContainersPreserved=False,
                   volumesUnchanged=False, portReleased=False)
    try:
        records.append(run_mode('architecture', env))
        records.append(run_mode('h2', env))
        provisioning = OUT / 'private-postgres'
        try:
            CI['start'](provisioning)
        finally:
            state_path = provisioning / 'postgres-state.json'
            if state_path.exists():
                state = json.loads(state_path.read_text(encoding='utf-8'))
                container_id = json.loads(CI['docker']('inspect', state['name']))[0]['Id']
        info = owned(state, container_id)
        bindings = info['NetworkSettings']['Ports']['5433/tcp']
        if len(bindings) != 1 or bindings[0]['HostIp'] != '127.0.0.1' or bindings[0]['HostPort'] != '5433':
            raise RuntimeError('Expected test-only loopback5433 binding')
        test_env = dict(line.split('=', 1) for line in (provisioning / 'postgres.env').read_text(encoding='utf-8').splitlines())
        if not re.fullmatch(r'jdbc:postgresql://127\.0\.0\.1:5433/elogistics_test_[a-z0-9_]+', test_env['PG_TEST_DB_URL']):
            raise RuntimeError('Refusing non-test database')
        records.append(run_mode('postgres', dict(env, **test_env)))
        owned(state, container_id)
        def query(db, statement):
            return CI['docker']('exec', '-i', container_id, 'psql', '-X', '-q', '-A', '-t', '-p', '5433',
                '-U', CI['BOOTSTRAP'], '-d', db, sql=statement).strip()
        database = test_env['PG_TEST_DB_URL'].rsplit('/', 1)[1]
        upgrade = test_env['PG_TEST_UPGRADE_DB_URL'].rsplit('/', 1)[1]
        history_sql = 'SELECT string_agg(version,chr(44) ORDER BY installed_rank) FROM platform.flyway_schema_history WHERE success;'
        migrations = [query(db, history_sql) for db in (database, upgrade)]
        if migrations != ['1,2', '1,2']:
            raise RuntimeError('Expected unchanged V1/V2 in cold and upgrade test databases')
        report = dict(status='PASS', modes=records, database=database, upgradeDatabase=upgrade,
            postgresVersion=query(database, 'SHOW server_version;'), migrations=migrations,
            ownershipLabel=CI['LABEL'], hostPort=5433)
        (OUT / 'safe-backend-results.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    finally:
        if state and container_id:
            owned(state, container_id)
            CI['docker']('rm', '-f', container_id)  # Only captured owned ID, never -v/prune/down.
            cleanup['ownedContainerRemoved'] = container_id not in set(CI['docker']('ps', '-a', '-q', '--no-trunc').splitlines())
        cleanup['existingContainersPreserved'] = baseline_containers == set(CI['docker']('ps', '-a', '-q', '--no-trunc').splitlines())
        cleanup['volumesUnchanged'] = baseline_volumes == set(CI['docker']('volume', 'ls', '-q').splitlines())
        cleanup['portReleased'] = port_available()
        (OUT / 'safe-cleanup.json').write_text(json.dumps(cleanup, indent=2) + '\n', encoding='utf-8')
        print('Backend owned cleanup: ' + json.dumps(cleanup), flush=True)
        print('Ignored evidence: ' + str(OUT), flush=True)
    return 0 if all(cleanup.values()) else 1


if __name__ == '__main__':
    try:
        raise SystemExit(run())
    except (RuntimeError, OSError, KeyError, ValueError, subprocess.SubprocessError):
        print('FAIL/BLOCKED: C-06 backend baseline; private diagnostics suppressed', file=sys.stderr)
        raise SystemExit(1) from None
