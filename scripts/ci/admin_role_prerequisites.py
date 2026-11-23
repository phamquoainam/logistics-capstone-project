"""AON Option B: fixed prerequisites for an already-owned isolated PG18 cluster.

No connection, Docker client, operator login/membership or executable mutation CLI.
Only the existing privileged bootstrap calls this pure SQL generator.
"""
import re


def _owner(owner):
    if not isinstance(owner, str) or not re.fullmatch(r'elogistics_test_owner_[a-f0-9]{12}', owner):
        raise ValueError('OWNED_CI_OWNER_REQUIRED')
    return owner


def roles(owner):
    owner = _owner(owner)
    return f"""CREATE ROLE elogistics_local_admin_operator NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
CREATE ROLE identity_admin_writer NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
CREATE ROLE elogistics_local_realm_operator NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
GRANT identity_admin_writer TO {owner} WITH INHERIT FALSE, SET TRUE;
"""


def database(owner):
    owner = _owner(owner)
    # Owner AON-ACL-02: only the freshly owned isolated database catalog.
    return ('REVOKE EXECUTE ON FUNCTION pg_catalog.pg_control_system() FROM PUBLIC;\n'
            + f'GRANT EXECUTE ON FUNCTION pg_catalog.pg_control_system() TO identity_admin_writer,{owner};\n')
