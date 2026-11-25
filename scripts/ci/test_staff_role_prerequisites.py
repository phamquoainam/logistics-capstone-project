import unittest
import importlib.util
from pathlib import Path
_spec=importlib.util.spec_from_file_location('staff_role_prerequisites',Path(__file__).with_name('staff_role_prerequisites.py'))
_module=importlib.util.module_from_spec(_spec);_spec.loader.exec_module(_module)
roles,database=_module.roles,_module.database


class StaffRolePrerequisiteTests(unittest.TestCase):
    def test_only_exact_owned_owner_and_eight_approved_nologin_roles(self):
        owner='elogistics_test_owner_012345abcdef'
        sql=roles(owner)
        self.assertEqual(8,sql.count('CREATE ROLE '))
        for role in ('elogistics_local_staff_operator','identity_staff_writer','support_grant_writer',
                     'elogistics_local_admin_operator','identity_admin_writer','elogistics_local_realm_operator',
                     'merchant_governance_operator','merchant_governance_writer'):
            self.assertIn('CREATE ROLE '+role+' NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;',sql)
        self.assertIn('TO '+owner+' WITH INHERIT FALSE, SET TRUE;',sql)
        self.assertEqual('GRANT EXECUTE ON FUNCTION pg_catalog.pg_control_system() TO identity_staff_writer,'+owner+';\n'
                         +'REVOKE EXECUTE ON FUNCTION pg_catalog.pg_control_system() FROM PUBLIC;\n'
                         +'GRANT EXECUTE ON FUNCTION pg_catalog.pg_control_system() TO identity_admin_writer,'+owner+';\n',database(owner))
        for bad in ('elogistics_owner','elogistics_runtime',owner+'; SELECT 1','elogistics_test_owner_GHIJKL123456',None):
            for operation in (roles,database):
                with self.assertRaises(ValueError):operation(bad)
