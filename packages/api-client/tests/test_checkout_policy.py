"""Exercise actual Git checkout conversion without changing the developer's Git config."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[3]
PACKAGE = ROOT / 'packages/api-client'


class GeneratedCheckoutPolicyTests(unittest.TestCase):
    def checkout(self, autocrlf):
        parent = ROOT / '.demo/checkout-policy-tests'
        parent.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=parent) as temporary:
            directory = Path(temporary).resolve()
            self.assertTrue(directory.is_relative_to(parent.resolve()))
            repository = directory / 'repository'
            repository.mkdir()
            env = {k: v for k, v in os.environ.items() if not k.startswith('GIT_')}
            env.update(GIT_CONFIG_NOSYSTEM='1', GIT_CONFIG_GLOBAL=os.devnull)

            def git(*args, cwd=repository):
                return subprocess.check_output(['git', '-c', 'core.autocrlf=' + autocrlf,
                    '-c', 'core.eol=crlf', *args], cwd=cwd, env=env, stderr=subprocess.PIPE)

            git('init', '--quiet')
            policy = PACKAGE / '.gitattributes'
            if policy.exists():
                (repository / '.gitattributes').write_bytes(policy.read_bytes())
            paths = subprocess.check_output(['git', 'ls-files', 'src/generated'],
                cwd=PACKAGE, env=env, text=True).splitlines()
            self.assertEqual(len(paths), 16)
            expected = {}
            for name in paths:
                content = subprocess.check_output(['git', 'show', 'HEAD:packages/api-client/' + name],
                    cwd=ROOT, env=env)
                self.assertNotIn(b'\r\n', content)
                file = repository / name
                file.parent.mkdir(parents=True, exist_ok=True)
                file.write_bytes(content)
                expected[name] = content
            (repository / 'src/handwritten.ts').write_bytes(b'// unrelated fixture\n')
            git('add', '--all')  # Private test index only; no commit or real index mutation.
            output = directory / 'checkout'
            git('checkout-index', '--all', '--prefix=' + output.as_posix() + '/')
            for name, content in expected.items():
                self.assertEqual((output / name).read_bytes(), content, name)
            return (output / 'src/handwritten.ts').read_bytes()

    def test_windows_autocrlf_preserves_all_sixteen_generated_blobs(self):
        self.assertEqual(self.checkout('true'), b'// unrelated fixture\r\n')

    def test_lf_checkout_remains_compatible(self):
        self.assertEqual(self.checkout('input'), b'// unrelated fixture\n')


if __name__ == '__main__':
    unittest.main()
