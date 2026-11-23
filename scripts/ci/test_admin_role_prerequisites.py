import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('admin_prerequisites', Path(__file__).with_name('admin_role_prerequisites.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class AdminRolePrerequisiteTests(unittest.TestCase):
    def test_exact_three_nologin_roles_and_only_writer_owner_binding(self):
        owner = 'elogistics_test_owner_012345abcdef'
        sql = module.roles(owner)
        self.assertEqual(3, sql.count('CREATE ROLE '))
        for role in ('elogistics_local_admin_operator', 'identity_admin_writer', 'elogistics_local_realm_operator'):
            self.assertIn('CREATE ROLE ' + role + ' NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;', sql)
        grants = [line for line in sql.splitlines() if line.startswith('GRANT ')]
        self.assertEqual(['GRANT identity_admin_writer TO ' + owner + ' WITH INHERIT FALSE, SET TRUE;'], grants)
        self.assertNotIn('LOGIN PASSWORD', sql)

    def test_only_approved_owned_ci_identifier_grammar_is_accepted(self):
        for invalid in ('elogistics_owner', 'elogistics_runtime', 'owner_0123456789abcdef0123456789abcdef', 'elogistics_test_owner_012345abcdef; SELECT 1', None):
            for command in (module.roles, module.database):
                with self.assertRaises(ValueError):
                    command(invalid)

    def test_control_system_is_not_granted_to_runtime_or_operator(self):
        owner = 'elogistics_test_owner_012345abcdef'
        self.assertEqual('REVOKE EXECUTE ON FUNCTION pg_catalog.pg_control_system() FROM PUBLIC;\n'
                         + 'GRANT EXECUTE ON FUNCTION pg_catalog.pg_control_system() TO identity_admin_writer,' + owner + ';\n', module.database(owner))


if __name__ == '__main__':
    unittest.main()
