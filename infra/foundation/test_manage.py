"""Bootstrap safety tests; service availability is verified separately on real containers."""
import base64
import hashlib
import json
from pathlib import Path
import runpy
import tempfile
import unittest
from unittest.mock import patch

M = runpy.run_path(str(Path(__file__).with_name('manage.py')))
initialize = M['initialize']


class BootstrapSafetyTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(dir=M['ROOT'] / '.demo', prefix='foundation-bootstrap-test-')
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name) / 'private'
        self.calls = []
        def inventory(*args):
            self.calls.append(args)
            return ''
        self.docker_patch = patch.dict(initialize.__globals__, docker=inventory)
        self.docker_patch.start()
        self.addCleanup(self.docker_patch.stop)

    def test_private_credentials_hashes_and_least_privilege(self):
        settings = initialize(self.directory)
        acl = (self.directory / 'redis.acl').read_text()
        definitions = json.loads((self.directory / 'definitions.json').read_text())
        self.assertNotIn(settings['redisPassword'], acl)
        self.assertIn(hashlib.sha256(settings['redisPassword'].encode()).hexdigest(), acl)
        self.assertIn('user default off', acl)
        self.assertNotIn('+@all', acl)
        self.assertNotIn('+set ', acl)
        self.assertNotIn(settings['rabbitPassword'], json.dumps(definitions))
        digest = base64.b64decode(definitions['users'][0]['password_hash'])
        self.assertEqual(digest[4:], hashlib.sha256(digest[:4] + settings['rabbitPassword'].encode()).digest())
        self.assertEqual(definitions['permissions'][0]['configure'], '(?!)')
        self.assertEqual(definitions['users'][0]['tags'], [])

    def test_reinitialization_preserves_credentials_and_bytes(self):
        first = initialize(self.directory)
        before = {p.name: p.read_bytes() for p in self.directory.iterdir()}
        self.assertEqual(first, initialize(self.directory))
        self.assertEqual(before, {p.name: p.read_bytes() for p in self.directory.iterdir()})

    def test_existing_volume_without_settings_is_rejected_before_writes(self):
        with patch.dict(initialize.__globals__, docker=lambda *args: 'elogistics_d1_rabbitmq_dev_data'):
            with self.assertRaisesRegex(RuntimeError, 'collision'):
                initialize(self.directory)
        self.assertFalse(self.directory.exists())

    def test_existing_project_without_settings_is_rejected_before_writes(self):
        with patch.dict(initialize.__globals__, docker=lambda *args: 'owned-by-someone-else' if args[0] == 'ps' else ''):
            with self.assertRaisesRegex(RuntimeError, 'collision'):
                initialize(self.directory)
        self.assertFalse(self.directory.exists())

    def test_private_file_mismatch_is_not_overwritten(self):
        initialize(self.directory)
        path = self.directory / 'redis.acl'
        path.write_text('synthetic-collision')
        with self.assertRaisesRegex(RuntimeError, 'no overwrite'):
            initialize(self.directory)
        self.assertEqual(path.read_text(), 'synthetic-collision')

    def test_outside_ignored_directory_and_invalid_names_are_rejected(self):
        with self.assertRaisesRegex(RuntimeError, 'ignored'):
            initialize(M['ROOT'] / 'infra/foundation/forbidden-private')
        with self.assertRaisesRegex(RuntimeError, 'identifier'):
            initialize(self.directory, 'unsafe name')
        self.assertFalse(self.directory.exists())

    def test_settings_cannot_be_retargeted_or_owner_filter_widened(self):
        initialize(self.directory)
        with self.assertRaisesRegex(RuntimeError, 'another project'):
            initialize(self.directory, 'another-project')
        path = self.directory / 'settings.json'
        settings = json.loads(path.read_text()); settings['owner'] = ''
        path.write_text(json.dumps(settings))
        with self.assertRaisesRegex(RuntimeError, 'ownership'):
            initialize(self.directory)


if __name__ == '__main__':
    unittest.main()
