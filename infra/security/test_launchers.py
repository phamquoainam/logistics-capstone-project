"""Launcher regression with private synthetic files and mocked Docker/Java."""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import unittest
from unittest.mock import patch, Mock

sys.path.insert(0, str(Path(__file__).parent))
import credential_store as S

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('prepare_postgres', ROOT / 'infra/postgres/prepare-test.py')
PREPARE = importlib.util.module_from_spec(spec)
spec.loader.exec_module(PREPARE)  # Import must NOT execute a Docker command.


class PrepareTests(unittest.TestCase):
    def setUp(self):
        self.store = S.Store.synthetic()
        self.addCleanup(self.store.cleanup_synthetic)

    def test_missing_protected_store_stops_before_docker_or_legacy_write(self):
        with patch.object(S.Store, 'open_development', side_effect=S.SecurityError('missing')):
            with patch.object(PREPARE.subprocess, 'run') as docker:
                with self.assertRaises(S.SecurityError):
                    PREPARE.prepare('protected')
                docker.assert_not_called()

    def test_protected_record_precedes_sql_and_roles_remain_restricted(self):
        def provision(*args, **kwargs):
            sessions = list((self.store.root / 'sessions').iterdir())
            self.assertEqual(len(sessions), 1)
            value = S.decode(self.store.backend.read(self.store.test_record_path(sessions[0].name), 'credential'))
            sql = kwargs['input']
            self.assertIn(f"CREATE ROLE {value['ownerRole']} NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE", sql)
            self.assertIn('REVOKE ALL ON DATABASE', sql)
            self.assertIn('REVOKE ALL ON SCHEMA public FROM PUBLIC', sql)
            self.assertIn(f"GRANT {value['ownerRole']} TO {value['migratorUser']}", sql)
            return Mock(returncode=0)
        with patch.object(S.Store, 'open_development', return_value=self.store):
            with patch.object(PREPARE.subprocess, 'run', side_effect=provision):
                session = PREPARE.prepare('protected')
        self.assertTrue(self.store.test_record_path(session).is_file())

    def test_provisioning_failure_retains_private_recovery_record(self):
        with patch.object(S.Store, 'open_development', return_value=self.store):
            with patch.object(PREPARE.subprocess, 'run', return_value=Mock(returncode=1)):
                with self.assertRaisesRegex(RuntimeError, 'preserve private record'):
                    PREPARE.prepare('protected')
        sessions = list((self.store.root / 'sessions').iterdir())
        self.assertEqual(len(sessions), 1)
        self.assertTrue(self.store.test_record_path(sessions[0].name).is_file())

    def test_legacy_default_keeps_record_path_and_does_not_open_protected_store(self):
        directory = self.store.root / 'repository'
        self.store.backend.mkdir(directory)
        # Legacy filesystem writes use synthetic repository, never real .demo.
        with patch.object(S.Store, 'open_development', side_effect=AssertionError('unexpected protected lookup')):
            with patch.object(PREPARE.subprocess, 'run', return_value=Mock(returncode=0)):
                path = Path(PREPARE.prepare(repository=directory))
        self.assertEqual(path.parent, directory / '.demo/postgres')
        self.assertTrue(path.name.startswith('test-run-'))
        value = json.loads(path.read_text())
        self.assertEqual(set(value), {'database', 'upgradeDatabase', 'ownerRole', 'migratorUser',
                                      'runtimeUser', 'migratorPassword', 'runtimePassword'})


@unittest.skipUnless(os.name == 'nt', 'PowerShell launcher integration is Windows-only')
class PowerShellTests(unittest.TestCase):
    def setUp(self):
        self.store = S.Store.synthetic()
        self.addCleanup(self.store.cleanup_synthetic)
        values = {'demo-settings.json': S.encode({k: 'synthetic' for k in S.DEMO_FIELDS}),
                  'postgres-settings.json': S.encode({k + 'Password': 'synthetic-' + k for k in ('bootstrap', 'migrator', 'runtime')})}
        values.update({k + '-password.txt': ('synthetic-' + k).encode() for k in ('bootstrap', 'migrator', 'runtime')})
        generation = self.store.publish(values)
        self.store.create_metadata('active.json', {'generation': generation})
        self.paths = self.store.active_paths(S.FILES)

    def powershell(self, body):
        environment = dict(os.environ, SYNTHETIC_STORE_PATHS=json.dumps(self.paths))
        # Process-local policy for local test scripts; no machine/user policy change.
        return subprocess.run(['powershell.exe', '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'RemoteSigned',
                               '-Command', "$ErrorActionPreference='Stop';" + body],
                              cwd=ROOT, env=environment, capture_output=True, text=True, timeout=30)

    def test_protected_initializers_use_only_selected_synthetic_paths(self):
        result = self.powershell("function python { $global:LASTEXITCODE=0; $env:SYNTHETIC_STORE_PATHS };"
            "function docker { throw 'Docker must not run' };"
            "function New-Item { throw 'Legacy initialization must not run' };"
            "& ./app/backend/run-demo.ps1 -CredentialSource protected -InitializeOnly;"
            "& ./infra/postgres/start-local.ps1 -CredentialSource protected -InitializeOnly")
        self.assertEqual(result.returncode, 0, 'Synthetic protected initialization failed; no raw output published')

    def test_missing_protected_configuration_never_falls_back(self):
        for script in ('app/backend/run-demo.ps1', 'infra/postgres/start-local.ps1', 'app/backend/run-postgres-demo.ps1'):
            result = self.powershell("function python { $global:LASTEXITCODE=1 };"
                "function Get-Content { throw 'UNEXPECTED_LEGACY_READ' };"
                "function docker { throw 'UNEXPECTED_DOCKER' };"
                "function java { throw 'UNEXPECTED_JAVA' };"
                f"& './{script}' -CredentialSource protected")
            self.assertNotEqual(result.returncode, 0)
            self.assertNotIn('UNEXPECTED_', result.stdout + result.stderr)

    def test_postgres_rebind_requires_separate_cutover_approval(self):
        result = self.powershell("function python { $global:LASTEXITCODE=0; $env:SYNTHETIC_STORE_PATHS };"
            "function docker { throw 'UNEXPECTED_DOCKER' };"
            "& ./infra/postgres/start-local.ps1 -CredentialSource protected")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('cutover is not authorized', result.stderr)
        self.assertNotIn('UNEXPECTED_DOCKER', result.stderr)

    def test_protected_backend_modes_preserve_role_and_seed_environment(self):
        for flag, profile, seed in [('-RuntimeOnly', 'demo,postgres', 'false'), ('-MigrationOnly', 'migration', 'true')]:
            result = self.powershell("function python { $global:LASTEXITCODE=0; $env:SYNTHETIC_STORE_PATHS };"
                "function java { $global:LASTEXITCODE=0 };"
                f"& ./app/backend/run-postgres-demo.ps1 -CredentialSource protected {flag};"
                f"if($env:SPRING_PROFILES_ACTIVE -ne '{profile}' -or $env:DEMO_SEED_ENABLED -ne '{seed}'"
                " -or $env:PG_RUNTIME_USER -ne 'elogistics_runtime'){throw 'Mode semantics changed'}")
            self.assertEqual(result.returncode, 0, 'Synthetic mode control failed; no secret output published')

    def test_all_modified_powershell_scripts_parse(self):
        result = self.powershell("$files=@('app/backend/run-demo.ps1','infra/postgres/start-local.ps1',"
            "'app/backend/run-postgres-demo.ps1','app/backend/verify-postgres.ps1');"
            "foreach($file in $files){$t=$null;$e=$null;"
            "[Management.Automation.Language.Parser]::ParseFile((Join-Path $pwd $file),[ref]$t,[ref]$e)|Out-Null;"
            "if($e.Count){throw 'Parser failure'}}")
        self.assertEqual(result.returncode, 0)


if __name__ == '__main__':
    unittest.main()
