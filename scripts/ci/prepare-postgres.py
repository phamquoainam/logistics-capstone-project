"""Provision/cleanup only a newly owned, disposable PostgreSQL 18 CI container."""
import argparse
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import time
import uuid

IMAGE = 'postgres:18.6-bookworm@sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c'
LABEL = 'com.elogistic.d1-05.run'
BOOTSTRAP = 'elogistics_ci_bootstrap'


def docker(*args, sql=None):
    executable = shutil.which('docker')
    if not executable and os.environ.get('LOCALAPPDATA'):
        executable = str(Path(os.environ['LOCALAPPDATA']) / 'Programs/DockerDesktop/resources/bin/docker.exe')
    result = subprocess.run([executable or 'docker', *args], input=sql, text=True, capture_output=True)
    if result.returncode:
        # Docker/psql error output can include credential-bearing SQL or environment.
        raise RuntimeError('Docker/psql operation failed; diagnostic output suppressed for credential safety')
    return result.stdout


def assert_owned(state):
    if state['name'] != 'elogistics-ci-postgres-' + state['run']:
        raise RuntimeError('Invalid CI container identity')
    info = json.loads(docker('inspect', state['name']))[0]
    if info['Config']['Labels'].get(LABEL) != state['run']:
        raise RuntimeError('Refusing operation on a container without the recorded CI ownership label')


def start(directory, network_container=None):
    directory.mkdir(parents=True, exist_ok=True)
    directory.chmod(0o700)
    state_path = directory / 'postgres-state.json'
    if state_path.exists():
        raise RuntimeError('Existing CI state; use a new temporary directory, never overwrite it')
    suffix = uuid.uuid4().hex[:12]
    name = 'elogistics-ci-postgres-' + suffix
    state = dict(name=name, run=suffix)
    bootstrap_password = secrets.token_urlsafe(32)
    migrator_password, runtime_password = secrets.token_urlsafe(32), secrets.token_urlsafe(32)
    password_file = directory / 'bootstrap-password'
    password_file.write_text(bootstrap_password, encoding='utf-8')
    password_file.chmod(0o600)
    # A fresh container with a tmpfs PG18 data directory: no developer/named volumes.
    args = ['run', '-d', '--name', name, '--label', LABEL + '=' + suffix,
            '--tmpfs', '/var/lib/postgresql:rw',
            '--mount', f'type=bind,source={password_file.resolve()},target=/run/ci-password,readonly',
            '-e', 'POSTGRES_USER=' + BOOTSTRAP, '-e', 'POSTGRES_PASSWORD_FILE=/run/ci-password']
    if network_container:
        # Local Linux simulation only. Shared loopback avoids the developer's host 5433.
        info = json.loads(docker('inspect', network_container))[0]
        if info['Config']['Labels'].get('com.elogistic.d1-05.runner') != 'true':
            raise RuntimeError('Local simulation requires an explicitly labeled CI runner container')
        args += ['--network', 'container:' + network_container]
    else:
        args += ['-p', '127.0.0.1:5433:5433']
    docker(*args, IMAGE, 'postgres', '-p', '5433')
    state_path.write_text(json.dumps(state), encoding='utf-8')
    assert_owned(state)
    for attempt in range(60):
        try:
            docker('exec', name, 'pg_isready', '-p', '5433', '-U', BOOTSTRAP, '-d', 'postgres')
            # pg_isready alone can detect the initialization server; verify TCP readiness.
            docker('exec', name, 'pg_isready', '-h', '127.0.0.1', '-p', '5433', '-U', BOOTSTRAP, '-d', 'postgres')
            break
        except RuntimeError:
            if attempt == 59:
                raise RuntimeError('Temporary PostgreSQL did not become ready')
            time.sleep(1)
    owner, migrator, runtime = (f'elogistics_test_{kind}_{suffix}' for kind in ('owner', 'migrator', 'runtime'))
    database, upgrade = 'elogistics_test_' + suffix, 'elogistics_test_upgrade_' + suffix
    # Same role/schema policy as infra/postgres/prepare-test.py; no hardcoded dev database/container.
    sql = f"""CREATE ROLE {owner} NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE {migrator} LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '{migrator_password}';
CREATE ROLE {runtime} LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '{runtime_password}';
GRANT {owner} TO {migrator};
"""
    for db in (database, upgrade):
        sql += rf"""CREATE DATABASE {db} OWNER {owner};
REVOKE ALL ON DATABASE {db} FROM PUBLIC;
GRANT CONNECT ON DATABASE {db} TO {migrator}, {runtime};
\connect {db}
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA platform AUTHORIZATION {owner};
\connect postgres
"""
    docker('exec', '-i', name, 'psql', '-X', '-p', '5433', '-v', 'ON_ERROR_STOP=1',
           '-U', BOOTSTRAP, '-d', 'postgres', sql=sql)
    env = dict(PG_TEST_DB_URL=f'jdbc:postgresql://127.0.0.1:5433/{database}',
               PG_TEST_UPGRADE_DB_URL=f'jdbc:postgresql://127.0.0.1:5433/{upgrade}',
               PG_TEST_OWNER_ROLE=owner, PG_TEST_MIGRATOR_USER=migrator,
               PG_TEST_MIGRATOR_PASSWORD=migrator_password, PG_TEST_RUNTIME_USER=runtime,
               PG_TEST_RUNTIME_PASSWORD=runtime_password)
    content = ''.join(f'{key}={value}\n' for key, value in env.items())
    env_path = directory / 'postgres.env'
    env_path.write_text(content, encoding='utf-8')
    env_path.chmod(0o600)
    if os.environ.get('GITHUB_ACTIONS') == 'true':
        for password in (bootstrap_password, migrator_password, runtime_password):
            print('::add-mask::' + password, flush=True)
        with open(os.environ['GITHUB_ENV'], 'a', encoding='utf-8') as destination:
            destination.write(content)
    print('Disposable PostgreSQL 18 ready; two isolated test databases and separate owner/migrator/runtime roles')


def cleanup(directory):
    path = directory / 'postgres-state.json'
    if not path.exists():
        print('No owned CI PostgreSQL container recorded; nothing to clean up')
        return
    state = json.loads(path.read_text(encoding='utf-8'))
    assert_owned(state)
    docker('rm', '-f', state['name'])  # Only this temporary labeled container; never -v/prune/down.
    print('Removed only the recorded disposable CI PostgreSQL container')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['start', 'cleanup'])
    parser.add_argument('--directory', type=Path, required=True)
    parser.add_argument('--network-container')
    args = parser.parse_args()
    try:
        if args.action == 'start':
            start(args.directory, args.network_container)
        else:
            cleanup(args.directory)
    except (RuntimeError, OSError, KeyError, ValueError) as error:
        # Never print exception bodies originating from processes or credential files.
        print('FAIL: isolated PostgreSQL provisioning/cleanup. Existing developer resources were not targeted.')
        raise SystemExit(1) from None
