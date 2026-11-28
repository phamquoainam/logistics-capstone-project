"""Exact ACL-05 prerequisites for an already-owned disposable CI PG18 cluster.

No Docker client, runtime credentials, role mutation entry point or demo target.
Caller is the existing privileged CI bootstrap after container ownership checks.
"""
import re
import importlib.util
from pathlib import Path

_admin_spec = importlib.util.spec_from_file_location('aon_prerequisites', Path(__file__).with_name('admin_role_prerequisites.py'))
_admin = importlib.util.module_from_spec(_admin_spec)
_admin_spec.loader.exec_module(_admin)


def _owner(owner):
    if not isinstance(owner, str) or not re.fullmatch(r'elogistics_test_owner_[a-f0-9]{12}', owner):
        raise ValueError('OWNED_CI_OWNER_REQUIRED')
    return owner


def roles(owner):
    owner = _owner(owner)
    return f"""CREATE ROLE elogistics_local_staff_operator NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
CREATE ROLE identity_staff_writer NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
CREATE ROLE support_grant_writer NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
CREATE ROLE elogistics_local_operations_operator NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
CREATE ROLE dispatch_network_writer NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
CREATE ROLE merchant_coverage_reader NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
GRANT dispatch_network_writer,merchant_coverage_reader TO {owner} WITH INHERIT FALSE, SET TRUE;
GRANT identity_staff_writer,support_grant_writer TO {owner} WITH INHERIT FALSE, SET TRUE;
""" + _admin.roles(owner) + _admin.merchant_governance_roles(owner)


def database(owner):
    owner = _owner(owner)
    return f'GRANT EXECUTE ON FUNCTION pg_catalog.pg_control_system() TO identity_staff_writer,{owner};\n' + _admin.database(owner) + """DO $$ BEGIN
IF EXISTS(SELECT 1 FROM pg_proc p CROSS JOIN LATERAL aclexplode(COALESCE(p.proacl,acldefault('f',p.proowner))) a WHERE p.oid='pg_catalog.pg_control_system()'::regprocedure AND a.grantee=0 AND a.privilege_type='EXECUTE') THEN RAISE EXCEPTION 'NWS_PUBLIC_CONTROL_EXECUTE_BLOCKED'; END IF;
END $$;
GRANT EXECUTE ON FUNCTION pg_catalog.pg_control_system() TO dispatch_network_writer;
"""
