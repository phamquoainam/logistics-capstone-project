"""Print finite failure kinds, never raw Surefire messages, stacks or properties.

Diagnostic only: the existing report collector remains the acceptance authority.
This program cannot turn a failed/cancelled verification into a success.
"""
import argparse
import importlib.util
import json
from pathlib import Path
import xml.etree.ElementTree as ET


KINDS = (
    ('TEST_DATABASE_IN_USE', ('test_database_still_connected', 'is being accessed by other users')),
    ('TEST_DATABASE_OWNER_CHANGED', ('test_database_owner_changed',)),
    ('TEST_DATABASE_TARGET_DENIED', ('owned_test_database_required', 'owned_test_database_binding_required')),
    ('PG_SHARED_MEMORY', ('out of shared memory',)),
    ('DISK_FULL', ('no space left on device',)),
    ('PG_CONNECTION_LIMIT', ('remaining connection slots are reserved', 'too many clients already')),
    ('OS_MEMORY', ('cannot allocate memory', 'java.lang.outofmemoryerror')),
    ('FILE_LIMIT', ('too many open files',)),
    ('PG_LOCK_TIMEOUT', ('canceling statement due to lock timeout',)),
    ('PG_STATEMENT_TIMEOUT', ('canceling statement due to statement timeout',)),
    ('PG_DEADLOCK', ('deadlock detected',)),
    ('PG_PERMISSION', ('error: permission denied',)),
    ('PG_UNDEFINED_RELATION', ('does not exist', 'undefined_table')),
    ('MIGRATION_VALIDATE', ('flywayvalidateexception',)),
    ('CONNECTION_FAILURE', ('connection refused', 'connection has been closed', 'the connection attempt failed')),
    ('TEST_TIMEOUT', ('test timeout of ', 'timeoutexception')),
    ('BROWSER_RUNTIME', ('customer_browser_runtime_failed', 'staff_browser_runtime_failed', 'claim_browser_runtime_failed')),
    ('ASSERTION', ('assertionfailederror', 'assertionerror')),
)


def kind(text):
    lowered = text.lower()
    return next((code for code, markers in KINDS if any(marker in lowered for marker in markers)), 'UNCLASSIFIED')


def classify(source, inventory, max_file_bytes=32 * 1024 * 1024):
    result = {'cases': [], 'unknown_cases': 0, 'unavailable_reports': 0}
    if not source.is_dir() or source.is_symlink():
        result['unavailable_reports'] = 1
        return result
    remaining = 64 * 1024 * 1024
    files = sorted(source.glob('TEST-*.xml'))
    for path in files[:128]:
        try:
            if path.is_symlink() or not path.is_file():
                raise ValueError('UNAVAILABLE')
            with path.open('rb') as stream:
                data = stream.read(min(max_file_bytes, remaining) + 1)
            if len(data) > min(max_file_bytes, remaining):
                raise ValueError('UNAVAILABLE')
            remaining -= len(data)
            suite = ET.fromstring(data)
            for case in suite.iter('testcase'):
                error = case.find('error')
                if error is None:
                    error = case.find('failure')
                if error is None:
                    continue
                name, method = case.get('classname'), case.get('name')
                if method not in inventory.get(name, ()):
                    result['unknown_cases'] += 1
                    continue
                if len(result['cases']) >= 100:
                    result['unknown_cases'] += 1
                    continue
                # Message/stack are examined in memory only; never serialized.
                text = ' '.join((error.get('type', ''), error.get('message', ''), error.text or ''))
                result['cases'].append({'suite': name, 'test': method, 'kind': kind(text)})
        except (OSError, ValueError, ET.ParseError):
            result['unavailable_reports'] += 1
    result['unavailable_reports'] += max(0, len(files) - 128)
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, required=True)
    args = parser.parse_args()
    spec = importlib.util.spec_from_file_location('report_inventory', Path(__file__).with_name('verify-reports.py'))
    reports = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(reports)
    print('SAFE_FAILURE_KINDS ' + json.dumps(classify(args.source, reports.REQUIRED), sort_keys=True))
