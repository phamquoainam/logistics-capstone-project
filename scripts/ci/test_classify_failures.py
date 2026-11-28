import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location('classify_failures', Path(__file__).with_name('classify-failures.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class SafeFailureClassificationTests(unittest.TestCase):
    def test_known_error_kind_never_publishes_message_stack_or_identity(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            suite = ET.Element('testsuite')
            case = ET.SubElement(suite, 'testcase', classname='known.Suite', name='knownCase')
            error = ET.SubElement(case, 'error', message='secret-app-password; customer@example.invalid')
            error.text = 'org.postgresql.util.PSQLException: ERROR: out of shared memory\nBearer private-token'
            ET.SubElement(suite, 'system-out').text = 'credential-value'
            ET.ElementTree(suite).write(source / 'TEST-known.xml')
            result = module.classify(source, {'known.Suite': ['knownCase']})
            self.assertEqual([{'suite': 'known.Suite', 'test': 'knownCase', 'kind': 'PG_SHARED_MEMORY'}], result['cases'])
            rendered = json.dumps(result)
            for value in ('secret-app-password', 'customer@example.invalid', 'private-token', 'credential-value', 'PSQLException'):
                self.assertNotIn(value, rendered)

    def test_unknown_case_or_error_cannot_smuggle_diagnostic_values(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            suite = ET.Element('testsuite')
            for name in ('knownCase', 'unapproved-secret-token'):
                ET.SubElement(ET.SubElement(suite, 'testcase', classname='known.Suite', name=name), 'error').text = 'arbitrary-private-text'
            ET.ElementTree(suite).write(source / 'TEST-known.xml')
            result = module.classify(source, {'known.Suite': ['knownCase']})
            self.assertEqual('UNCLASSIFIED', result['cases'][0]['kind'])
            self.assertEqual(1, result['unknown_cases'])
            self.assertNotIn('unapproved-secret-token', json.dumps(result))
            self.assertNotIn('arbitrary-private-text', json.dumps(result))

    def test_malformed_or_oversize_input_is_bounded_and_not_echoed(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            (source / 'TEST-invalid.xml').write_text('<secret-invalid')
            (source / 'TEST-large.xml').write_bytes(b'private' * 20)
            result = module.classify(source, {}, max_file_bytes=100)
            self.assertEqual(2, result['unavailable_reports'])
            self.assertEqual([], result['cases'])

    def test_resource_and_sql_error_kinds_are_finite(self):
        for message, expected in [
            ('TEST_DATABASE_STILL_CONNECTED', 'TEST_DATABASE_IN_USE'),
            ('TEST_DATABASE_OWNER_CHANGED', 'TEST_DATABASE_OWNER_CHANGED'),
            ('No space left on device', 'DISK_FULL'),
            ('remaining connection slots are reserved', 'PG_CONNECTION_LIMIT'),
            ('too many clients already', 'PG_CONNECTION_LIMIT'),
            ('canceling statement due to lock timeout', 'PG_LOCK_TIMEOUT'),
            ('ERROR: permission denied for relation private-name', 'PG_PERMISSION'),
            ('deadlock detected', 'PG_DEADLOCK'),
            ('Test timeout of 25000ms exceeded.', 'TEST_TIMEOUT'),
        ]:
            self.assertEqual(expected, module.kind(message))

    def test_browser_stage_is_literal_case_bound_and_never_exposes_private_values(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory); suite = ET.Element('testsuite')
            name = module.BIA_SUITE; method = 'biaRealHttpsCustomerApplicationToBusinessStation'
            error = ET.SubElement(ET.SubElement(suite, 'testcase', classname=name, name=method), 'failure',
                type='org.opentest4j.AssertionFailedError', message='BIA_SAFE_STAGE_BUSINESS_SCAN_ACCEPTANCE private-token@example.invalid')
            error.text = 'Bearer hidden-proof; original request and password must stay private'
            ET.ElementTree(suite).write(source / 'TEST-bia.xml')
            result = module.classify(source, {name: [method]})
            self.assertEqual('BIA_STAGE_BUSINESS_SCAN_ACCEPTANCE', result['cases'][0]['kind'])
            for secret in ('private-token', 'example.invalid', 'hidden-proof', 'password', 'Bearer'):
                self.assertNotIn(secret, json.dumps(result))
        for text in ('BIA_SAFE_STAGE_PRIVATE_TOKEN', 'BIA_SAFE_STAGE_REVIEW BIA_SAFE_STAGE_APPROVAL', 'BIA_SAFE_CHECK_UNKNOWN'):
            self.assertEqual('ASSERTION', module.bounded_browser_kind(name, method, text, 'ASSERTION'))
        self.assertEqual('ASSERTION', module.bounded_browser_kind('other.Suite', method, 'BIA_SAFE_STAGE_REVIEW', 'ASSERTION'))
        self.assertEqual('ASSERTION', module.bounded_browser_kind(name, 'otherCase', 'BIA_SAFE_STAGE_REVIEW', 'ASSERTION'))
        self.assertEqual('PG_PERMISSION', module.bounded_browser_kind(name, method, 'BIA_SAFE_STAGE_REVIEW', 'PG_PERMISSION'))
        self.assertEqual('BIA_CHECK_SMTP_MESSAGES', module.bounded_browser_kind(name, method, 'BIA_SAFE_CHECK_SMTP_MESSAGES', 'ASSERTION'))


class PrivateBrowserDiagnosticTests(unittest.TestCase):
    def test_cli_emits_each_safe_record_once_without_private_fields(self):
        import subprocess
        import sys
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary);self.fixture(root)
            run = subprocess.run([sys.executable, module.__file__, '--source', str(root), '--browser-private', str(root)], capture_output=True, text=True, check=False)
            self.assertEqual(0, run.returncode, run.stderr)
            self.assertEqual(1, run.stdout.count('SAFE_FAILURE_KINDS '))
            self.assertEqual(1, run.stdout.count('SAFE_BROWSER_FAILURES '))
            for secret in ('private-token', 'password', 'proof', 'example.invalid'):
                self.assertNotIn(secret, run.stdout)

    def fixture(self, root, status='failed', message='strict mode violation Bearer private-token', stack='at /owned/runtime-tests/admin-governance.spec.ts:52:138'):
        directory = root / 'admin-browser-owned';directory.mkdir()
        name = next(iter(module._BROWSER_SOURCES))
        (directory / 'safe-browser-results.json').write_text(json.dumps({'cases': [{'name': name, 'status': status}]}), encoding='utf-8')
        (directory / 'browser-private-diagnostics.json').write_text(json.dumps([{'name': name, 'errors': [{'message': message, 'stack': stack, 'snippet': 'password proof email@example.invalid', 'location': {'file': '/private/account-identifier'}}]}]), encoding='utf-8')
        return directory

    def test_fixed_labels_source_line_and_status_never_publish_private_fields(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary);self.fixture(root)
            result = module.browser_failures(root)
            self.assertEqual({'source': 'GOVERNANCE', 'status': 'failed', 'kind': 'STRICT_SELECTOR', 'line': 52}, result['cases'][0])
            for secret in ('Bearer', 'private-token', 'password', 'proof', 'email', 'account-identifier', '/owned', 'snippet'):
                self.assertNotIn(secret, json.dumps(result))

    def test_timeout_and_http_status_are_bounded_and_foreign_source_is_not_trusted(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary);self.fixture(root, 'timedOut', 'Test timeout of 60000ms\nExpected: 200\nReceived: 503\nsecret: 123456', 'other.spec.ts:99:1')
            case = module.browser_failures(root)['cases'][0]
            self.assertEqual('TEST_TIMEOUT', case['kind']);self.assertIsNone(case['line'])
            self.assertEqual({'expected': 200, 'received': 503}, case['httpComparison'])
            self.assertNotIn('123456', json.dumps(case))

    def test_unknown_names_malformed_and_oversize_reports_fail_closed(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary);directory = self.fixture(root)
            p = directory / 'safe-browser-results.json'
            p.write_text(json.dumps({'cases': [{'name': 'secret@example.invalid', 'status': 'failed'}]}), encoding='utf-8')
            self.assertEqual([], module.browser_failures(root)['cases'])
            p.write_text('[]', encoding='utf-8');self.assertEqual(1, module.browser_failures(root)['unavailable'])
            p.write_bytes(b'x' * (512 * 1024 + 1));self.assertEqual(1, module.browser_failures(root)['unavailable'])
            self.assertEqual(1, module.browser_failures(root / 'missing')['unavailable'])


if __name__ == '__main__':
    unittest.main()
