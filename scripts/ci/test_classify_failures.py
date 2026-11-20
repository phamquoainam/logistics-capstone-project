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


if __name__ == '__main__':
    unittest.main()
