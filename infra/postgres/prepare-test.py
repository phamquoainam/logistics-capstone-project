"""Create new isolated test databases/roles; never drop or reset existing resources."""
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import uuid

root = Path(__file__).resolve().parents[2]
docker = shutil.which('docker') or str(Path(os.environ['LOCALAPPDATA']) / 'Programs/DockerDesktop/resources/bin/docker.exe')
suffix = uuid.uuid4().hex[:12]
owner, migrator, runtime = (f'elogistics_test_{kind}_{suffix}' for kind in ('owner', 'migrator', 'runtime'))
database = f'elogistics_test_{suffix}'
upgrade = f'elogistics_test_upgrade_{suffix}'
migrator_password, runtime_password = secrets.token_urlsafe(32), secrets.token_urlsafe(32)
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
result = subprocess.run([docker, 'exec', '-i', 'elogistics-d1-postgres18', 'psql', '-X', '-v', 'ON_ERROR_STOP=1', '-U', 'elogistics_bootstrap', '-d', 'postgres'], input=sql, text=True, capture_output=True)
if result.returncode:
    raise SystemExit('Isolated test provisioning failed. Preserve resources; diagnostic output suppressed to protect credentials.')
settings = dict(database=database, upgradeDatabase=upgrade, ownerRole=owner, migratorUser=migrator, runtimeUser=runtime, migratorPassword=migrator_password, runtimePassword=runtime_password)
directory = root / '.demo/postgres'
directory.mkdir(parents=True, exist_ok=True)
path = directory / f'test-run-{suffix}.json'
path.write_text(json.dumps(settings, indent=2), encoding='utf-8')
print(path)
