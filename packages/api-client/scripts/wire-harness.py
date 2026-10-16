"""Owned disposable PG18 + actual backend HTTP; never target developer resources."""
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import socket
import subprocess
import sys
import time
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[3]
PACKAGE = ROOT / 'packages/api-client'
IMAGE = 'postgres:18.6-bookworm@sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c'
LABEL = 'com.elogistic.c03.run'
BOOTSTRAP = 'elogistics_wire_bootstrap'
DOCKER = shutil.which('docker') or str(Path(os.environ.get('LOCALAPPDATA', '')) / 'Programs/DockerDesktop/resources/bin/docker.exe')


def docker(*args, sql=None):
    result = subprocess.run([DOCKER, *args], input=sql, text=True, encoding='utf-8', capture_output=True, timeout=60)
    if result.returncode:
        raise RuntimeError('Docker/psql failed; private diagnostic values suppressed')
    return result.stdout.strip()


def owned(state):
    if not re.fullmatch(r'[a-f0-9]{12}', state['run']):
        raise RuntimeError('Invalid test ownership ID')
    if state['name'] != 'elogistics-c03-postgres-' + state['run'] or state['database'] != 'elogistics_wire_' + state['run']:
        raise RuntimeError('Refusing non-C03 resource')
    info = json.loads(docker('inspect', state['id']))[0]
    if info['Id'] != state['id'] or info['Name'] != '/' + state['name'] or info['Config']['Labels'].get(LABEL) != state['run']:
        raise RuntimeError('Container identity/ownership mismatch')
    if '/var/lib/postgresql' not in info['HostConfig']['Tmpfs'] or any(m['Type'] == 'volume' for m in info['Mounts']):
        raise RuntimeError('Test DB must use tmpfs, no volumes')
    return info


def sql(state, statement, database=None):
    owned(state)
    return docker('exec', '-i', state['id'], 'psql', '-X', '-q', '-A', '-t', '-v', 'ON_ERROR_STOP=1',
                  '-U', BOOTSTRAP, '-d', database or state['database'], sql=statement)


def private_json(path, value):
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')
    path.chmod(0o600)


def assert_fixture_sql(statement):
    # These are source-controlled fixture queries, not a general SQL parser.
    # Do not mistake the literal operation='create' for a CREATE statement.
    tokens = re.sub(r"'(?:''|[^'])*'", "''", statement)
    if not re.match(r'^\s*(SELECT|UPDATE)\b', tokens, re.I) or re.search(r'\b(DROP|TRUNCATE|CREATE|ALTER|GRANT|REVOKE|COPY)\b|\\', tokens, re.I):
        raise RuntimeError('Only bounded fixture reads/account updates allowed')


def run(consumer=None):
    suffix = uuid.uuid4().hex[:12]
    directory = ROOT / ('.demo/d1-07-c04' if consumer else '.demo/d1-07-c03') / ('run-' + suffix)
    directory.mkdir(parents=True, exist_ok=False)
    directory.chmod(0o700)
    state = {'run': suffix, 'name': 'elogistics-c03-postgres-' + suffix, 'database': 'elogistics_wire_' + suffix}
    baseline_containers = set(docker('ps', '-a', '-q', '--no-trunc').splitlines())
    baseline_volumes = set(docker('volume', 'ls', '-q').splitlines())
    backend = None
    result_code = 1
    cleanup = {'backendStopped': False, 'ownedContainerRemoved': False, 'existingContainersPreserved': False, 'volumesUnchanged': False}
    state_path = directory / 'private-state.json'
    log = None
    try:
        password_file = directory / 'bootstrap-password'
        password_file.write_text(secrets.token_urlsafe(32), encoding='utf-8')
        password_file.chmod(0o600)
        state['id'] = docker('run', '--pull=never', '-d', '--name', state['name'], '--label', LABEL + '=' + suffix,
            '--tmpfs', '/var/lib/postgresql:rw', '--mount', f'type=bind,source={password_file.resolve()},target=/run/wire-password,readonly',
            '-e', 'POSTGRES_USER=' + BOOTSTRAP, '-e', 'POSTGRES_PASSWORD_FILE=/run/wire-password',
            '-p', '127.0.0.1::5432', IMAGE)
        private_json(state_path, state)
        info = owned(state)
        bindings = info['NetworkSettings']['Ports']['5432/tcp']
        if len(bindings) != 1 or bindings[0]['HostIp'] != '127.0.0.1':
            raise RuntimeError('PostgreSQL must be published only on loopback')
        port = int(bindings[0]['HostPort'])
        for attempt in range(60):
            try:
                docker('exec', state['id'], 'pg_isready', '-h', '127.0.0.1', '-U', BOOTSTRAP, '-d', 'postgres')
                break
            except RuntimeError:
                if attempt == 59:
                    raise RuntimeError('Test PostgreSQL readiness timeout')
                time.sleep(1)
        owner, migrator, runtime = (f'elogistics_wire_{suffix}_{kind}' for kind in ('owner', 'migrator', 'runtime'))
        migrator_password, runtime_password = secrets.token_urlsafe(32), secrets.token_urlsafe(32)
        # Identifiers are generated locally; credentials are URL-safe and never logged.
        provision = f"""CREATE ROLE {owner} NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE {migrator} LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '{migrator_password}';
CREATE ROLE {runtime} LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '{runtime_password}';
GRANT {owner} TO {migrator};
CREATE DATABASE {state['database']} OWNER {owner};
REVOKE ALL ON DATABASE {state['database']} FROM PUBLIC;
GRANT CONNECT ON DATABASE {state['database']} TO {migrator}, {runtime};
"""
        sql(state, provision, 'postgres')
        sql(state, f'REVOKE ALL ON SCHEMA public FROM PUBLIC; CREATE SCHEMA platform AUTHORIZATION {owner};')
        with socket.socket() as listener:
            listener.bind(('127.0.0.1', 0))
            http_port = listener.getsockname()[1]
        context = {'baseUrl': f'http://127.0.0.1:{http_port}/api/demo/v1', 'password': secrets.token_urlsafe(24),
                   'tokenSecret': secrets.token_urlsafe(48), 'statePath': str(state_path), 'directory': str(directory)}
        context_path = directory / 'private-context.json'
        private_json(context_path, context)
        env = dict(os.environ)
        # Discard inherited DB/profile settings; this run uses only owned credentials.
        for key in list(env):
            if key.startswith(('PG_', 'PG_TEST_', 'DEMO_', 'SPRING_')):
                del env[key]
        env.update(PG_DB_URL=f'jdbc:postgresql://127.0.0.1:{port}/{state["database"]}', PG_RUNTIME_USER=runtime,
                   PG_RUNTIME_PASSWORD=runtime_password, PG_MIGRATOR_USER=migrator, PG_MIGRATOR_PASSWORD=migrator_password,
                   PG_OWNER_ROLE=owner, DEMO_PORT=str(http_port), DEMO_TOKEN_SECRET=context['tokenSecret'], DEMO_SEED_ENABLED='true')
        for account in ('ADMIN', 'MERCHANT_A', 'MERCHANT_B', 'OPERATOR', 'DISABLED'):
            env['DEMO_' + account + '_PASSWORD'] = context['password']
        jar = ROOT / 'app/backend/target/backend-0.0.1-SNAPSHOT.jar'
        if not jar.is_file():
            raise RuntimeError('Build the current backend JAR before running wire tests')
        log = (directory / 'backend-private.log').open('wb')
        backend = subprocess.Popen(['java', '-jar', str(jar), '--spring.profiles.active=demo,postgres'], cwd=ROOT / 'app/backend',
                                   env=env, stdout=log, stderr=subprocess.STDOUT)
        ready = False
        for attempt in range(90):
            if backend.poll() is not None:
                raise RuntimeError('Backend startup failed; private log retained, not printed')
            try:
                with urllib.request.urlopen(context['baseUrl'] + '/health', timeout=2) as response:
                    if response.status == 200 and sql(state, 'SELECT count(*) FROM identity.demo_account;') == '5':
                        ready = True
                        break
            except Exception:
                if attempt == 89:
                    raise RuntimeError('Backend/seed readiness timeout; private log retained') from None
                time.sleep(1)
        if not ready:
            raise RuntimeError('Backend/seed readiness was not verified')
        history = sql(state, 'SELECT string_agg(version, chr(44) ORDER BY installed_rank) FROM platform.flyway_schema_history WHERE success;')
        if history != '1,2':
            raise RuntimeError('Expected unchanged Flyway V1/V2 on a cold isolated DB')
        context.update(postgresVersion=sql(state, 'SHOW server_version;'), database=state['database'], postgresPort=port,
                       backendPort=http_port, migrations=history)
        private_json(context_path, context)
        env.update(C03_CONTEXT=str(context_path), C03_PYTHON=sys.executable)
        print('Owned PostgreSQL18/tmpfs + real backend ready; V1/V2, five synthetic accounts, loopback random ports', flush=True)
        if consumer:
            result = consumer(context_path, env)
        else:
            result = subprocess.run(['node', '--test', '--test-concurrency=1', 'tests/wire.test.mjs'], cwd=PACKAGE, env=env,
                                    stdin=subprocess.DEVNULL, timeout=180)
        result_code = result.returncode
        # Secret/token-bearing HTTP data is never saved; inspect application log in memory.
        log.flush()
        content = (directory / 'backend-private.log').read_text(encoding='utf-8', errors='replace')
        sensitive = [context['password'], context['tokenSecret'], migrator_password, runtime_password, password_file.read_text()]
        log_safe = not any(value in content for value in sensitive) and not re.search(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', content)
        if not log_safe:
            result_code = 1
        private_json(directory / 'safe-environment.json', dict(run=suffix, database=state['database'], postgresPort=port,
            backendPort=http_port, postgresVersion=context['postgresVersion'], migrations=history,
            logPrivacy='PASS' if log_safe else 'FAIL', testsExit=result.returncode))
        print('Backend log bounded privacy audit: ' + ('PASS' if log_safe else 'FAIL'), flush=True)
    finally:
        if backend is not None:
            backend.terminate()
            try:
                backend.wait(timeout=15)
            except subprocess.TimeoutExpired:
                backend.kill()
                backend.wait(timeout=10)
            cleanup['backendStopped'] = backend.poll() is not None
        if log is not None:
            log.close()
        if state.get('id'):
            owned(state)
            docker('rm', '-f', state['id'])  # Only this run's ID; never -v, prune or a named developer resource.
            cleanup['ownedContainerRemoved'] = state['id'] not in set(docker('ps', '-a', '-q', '--no-trunc').splitlines())
        cleanup['existingContainersPreserved'] = baseline_containers == set(docker('ps', '-a', '-q', '--no-trunc').splitlines())
        cleanup['volumesUnchanged'] = baseline_volumes == set(docker('volume', 'ls', '-q').splitlines())
        private_json(directory / 'safe-cleanup.json', cleanup)
        print('Cleanup: ' + json.dumps(cleanup), flush=True)
        print('Ignored evidence directory: ' + str(directory), flush=True)
        if not all(cleanup.values()):
            result_code = 1
    return result_code


if __name__ == '__main__':
    try:
        if len(sys.argv) == 2 and sys.argv[1] == 'sql':
            context = json.loads(Path(os.environ['C03_CONTEXT']).read_text(encoding='utf-8'))
            state = json.loads(Path(context['statePath']).read_text(encoding='utf-8'))
            statement = sys.stdin.read()
            assert_fixture_sql(statement)
            print(sql(state, statement))
        elif len(sys.argv) == 1:
            raise SystemExit(run())
        else:
            raise RuntimeError('Unsupported harness action')
    except (RuntimeError, OSError, KeyError, ValueError, subprocess.SubprocessError):
        print('FAIL: C-03 owned harness operation; secret-bearing diagnostics suppressed', file=sys.stderr)
        raise SystemExit(1) from None
