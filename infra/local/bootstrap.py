"""Generate only the approved canonical prerequisites for a brand-new owned cluster."""
import json
import os
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent / 'canonical'))
import staff_role_prerequisites


def generate(root):
    meta = json.loads((root / 'installation.json').read_text())
    owner, database = meta['owner'], meta['database']
    if not re.fullmatch(r'elogistics_test_owner_[a-f0-9]{12}', owner):
        raise ValueError('EXACT_LOCAL_OWNER_REQUIRED')
    if not re.fullmatch(r'argopost_admin_uat_[a-f0-9]{12}', database):
        raise ValueError('EXACT_NEW_DATABASE_REQUIRED')
    passwords = json.loads((root / 'database-passwords.json').read_text())
    if any(not re.fullmatch(r'[A-Za-z0-9_-]{43}', p) for p in passwords.values()):
        raise ValueError('GENERATED_LOCAL_SECRET_REQUIRED')
    admin_password = os.environ.get('ARGOPOST_NEW_ADMIN_OPERATOR_PASSWORD', '')
    if not re.fullmatch(r'[A-Za-z0-9_-]{43}', admin_password):
        raise ValueError('EXACT_NEW_OPERATOR_SECRET_REQUIRED')
    sql = f"""CREATE ROLE {owner} NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
CREATE ROLE argopost_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD '{passwords['migrator']}';
CREATE ROLE argopost_runtime LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD '{passwords['runtime']}';
GRANT {owner} TO argopost_migrator;
""" + staff_role_prerequisites.roles(owner) + f"""CREATE ROLE argopost_local_admin_operator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD '{admin_password}';
GRANT elogistics_local_admin_operator TO argopost_local_admin_operator WITH INHERIT TRUE, SET FALSE;
CREATE DATABASE {database} OWNER {owner};
REVOKE ALL ON DATABASE {database} FROM PUBLIC;
GRANT CONNECT ON DATABASE {database} TO argopost_migrator,argopost_runtime;
GRANT CONNECT ON DATABASE {database} TO argopost_local_admin_operator;
\\connect {database}
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA platform AUTHORIZATION {owner};
""" + staff_role_prerequisites.database(owner)
    (root / 'bootstrap.sql').write_text(sql)
    (root / 'bootstrap.sql').chmod(0o600)


if __name__ == '__main__':
    generate(Path(sys.argv[1]))
