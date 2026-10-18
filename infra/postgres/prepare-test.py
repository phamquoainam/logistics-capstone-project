"""Create isolated test databases/roles; legacy default retained before migration."""
import argparse
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import sys
import uuid

ROOT = Path(__file__).resolve().parents[2]


def prepare(credential_source='legacy', repository=ROOT):
    if credential_source not in ('legacy', 'protected'):
        raise RuntimeError('Invalid credential source')
    store = None
    if credential_source == 'protected':
        sys.path.insert(0, str(ROOT / 'infra/security'))
        from credential_store import Store
        store = Store.open_development()  # Fail BEFORE any provisioning.
    docker = shutil.which('docker') or str(Path(os.environ['LOCALAPPDATA']) / 'Programs/DockerDesktop/resources/bin/docker.exe')
    suffix = uuid.uuid4().hex[:12]
    owner, migrator, runtime = (f'elogistics_test_{kind}_{suffix}' for kind in ('owner', 'migrator', 'runtime'))
    database, upgrade = f'elogistics_test_{suffix}', f'elogistics_test_upgrade_{suffix}'
    migrator_password, runtime_password = secrets.token_urlsafe(32), secrets.token_urlsafe(32)
    settings = dict(database=database, upgradeDatabase=upgrade, ownerRole=owner,
                    migratorUser=migrator, runtimeUser=runtime,
                    migratorPassword=migrator_password, runtimePassword=runtime_password)
    # Protected record is complete before SQL; failed provisioning retains it.
    session = store.prepare_test_record(settings) if store else None
    sql = f"""CREATE ROLE {owner} NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE {migrator} LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '{migrator_password}';
CREATE ROLE {runtime} LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '{runtime_password}';
GRANT {owner} TO {migrator};
"""
    for name in (database, upgrade):
        sql += f"""CREATE DATABASE {name} OWNER {owner};
REVOKE ALL ON DATABASE {name} FROM PUBLIC;
GRANT CONNECT ON DATABASE {name} TO {migrator}, {runtime};
\\connect {name}
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA platform AUTHORIZATION {owner};
\\connect postgres
"""
    result = subprocess.run([docker, 'exec', '-i', 'elogistics-d1-postgres18', 'psql', '-X',
        '-v', 'ON_ERROR_STOP=1', '-U', 'elogistics_bootstrap', '-d', 'postgres'],
        input=sql, text=True, capture_output=True)
    if result.returncode:
        raise RuntimeError('Test provisioning failed; preserve private record/resources; diagnostics suppressed')
    if store:
        return session
    directory = repository / '.demo/postgres'
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / f'test-run-{suffix}.json'
    path.write_text(json.dumps(settings, indent=2), encoding='utf-8')
    return str(path)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--credential-source', choices=['legacy', 'protected'], default='legacy')
    args = parser.parse_args()
    try:
        print(prepare(args.credential_source))
    except (OSError, RuntimeError, ValueError):
        raise SystemExit('Test provisioning blocked; preserve private state; no fallback or automatic cleanup') from None
