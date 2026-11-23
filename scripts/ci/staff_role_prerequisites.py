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
GRANT identity_staff_writer,support_grant_writer TO {owner} WITH INHERIT FALSE, SET TRUE;
""" + _admin.roles(owner)


def database(owner):
    owner = _owner(owner)
    return f'GRANT EXECUTE ON FUNCTION pg_catalog.pg_control_system() TO identity_staff_writer,{owner};\n' + _admin.database(owner)
