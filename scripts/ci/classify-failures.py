"""Print finite failure kinds, never raw Surefire messages, stacks or properties.

Diagnostic only: the existing report collector remains the acceptance authority.
This program cannot turn a failed/cancelled verification into a success.
"""
import argparse
import importlib.util
import json
import re
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

# Literal test-stage/check labels only. Never serialize a caller-supplied value.
BIA_STAGES = frozenset(('CUSTOMER_LOGIN', 'APPLICATION_DRAFT', 'CONTACT_VERIFICATION',
    'EVIDENCE', 'SUBMISSION', 'ASSIGNMENT', 'REVIEW', 'APPROVAL', 'DASHBOARD',
    'AUDIT_AND_NOTICES', 'EXPLICIT_SHIPPING_SETUP', 'BUSINESS_QUOTE_BOOKING',
    'GENUINE_OPS_VERIFICATION', 'BUSINESS_SCAN_ACCEPTANCE', 'BUSINESS_AT_STATION_VERIFIED', 'UNKNOWN'))
BIA_CHECKS = frozenset(('LABEL_PAGES', 'LABEL_UNPAID', 'LABEL_SIMULATION',
    'LABEL_BARCODES', 'SMTP_MESSAGES', 'SCANNER_REQUESTS'))
BIA_SUITE = 'com.elogistic.backend.identity.internal.infrastructure.IdentityGrantApplicationTests'
BIA_CASES = frozenset(('biaRealHttpsCustomerApplicationToBusinessStation',
    'adminCoreRealHttpsCustomerToIndependentActivation'))


def bounded_browser_kind(suite, method, text, original):
    if suite != BIA_SUITE or method not in BIA_CASES or original not in ('ASSERTION', 'BROWSER_RUNTIME', 'TEST_TIMEOUT'):
        return original
    markers = set(re.findall(r'\bBIA_SAFE_(STAGE|CHECK)_([A-Z_]+)\b', text))
    if len(markers) != 1:
        return original
    category, value = next(iter(markers))
    if value not in (BIA_STAGES if category == 'STAGE' else BIA_CHECKS):
        return original
    return 'BIA_' + category + '_' + value


def kind(text):
    lowered = text.lower()
    return next((code for code, markers in KINDS if any(marker in lowered for marker in markers)), 'UNCLASSIFIED')


_BROWSER_SOURCES = {
    'genuine HTTPS SYSTEM reviewer uses scoped queue, immutable facts and current PG18 checklist': ('GOVERNANCE', 'admin-governance.spec.ts'),
    'Customer submission through independent review activates real Organization and reconciles Dashboard Work and Audit': ('CORE', 'admin-core.spec.ts'),
}


def browser_failures(directory):
    """Reduce private diagnostics to fixed labels, bounded source lines and HTTP codes.

    Never publish names, paths, messages, snippets, stacks, DOM, request or fixture
    values. This diagnostic cannot modify the acceptance result.
    """
    result = {'cases': [], 'unavailable': 0}
    if not directory.is_dir() or directory.is_symlink():
        result['unavailable'] = 1
        return result
    directories = sorted(set(directory.glob('admin-browser-*')) | set(directory.glob('admin-core-browser-*')))
    budget = 16 * 1024 * 1024
    def read(path, limit=512 * 1024):
        nonlocal budget
        if path.is_symlink() or not path.is_file() or path.stat().st_size > min(limit, budget):
            raise ValueError('UNAVAILABLE')
        with path.open('rb') as stream:
            data = stream.read(min(limit, budget) + 1)
        if len(data) > min(limit, budget):
            raise ValueError('UNAVAILABLE')
        budget -= len(data)
        return json.loads(data)
    for child in directories[:64]:
        try:
            if child.is_symlink() or not child.is_dir():
                raise ValueError('UNAVAILABLE')
            report = read(child / 'safe-browser-results.json')
            if not isinstance(report, dict) or not isinstance(report.get('cases'), list) or len(report['cases']) > 100:
                raise ValueError('UNAVAILABLE')
            failed = [c for c in report['cases'] if isinstance(c, dict) and isinstance(c.get('name'), str) and c['name'] in _BROWSER_SOURCES and c.get('status') in ('failed', 'timedOut', 'interrupted')]
            if not failed:
                continue
            diagnostics = read(child / 'browser-private-diagnostics.json')
            if not isinstance(diagnostics, list) or len(diagnostics) > 20:
                raise ValueError('UNAVAILABLE')
            if any(not isinstance(d, dict) or not isinstance(d.get('errors'), list) or len(d['errors']) > 20 for d in diagnostics):
                raise ValueError('UNAVAILABLE')
            for case in failed[:20]:
                source, filename = _BROWSER_SOURCES[case['name']]
                errors = [e for d in diagnostics if isinstance(d, dict) and d.get('name') == case['name'] for e in d.get('errors', []) if isinstance(e, dict)]
                text = '\n'.join(str(e.get('message', '')) + '\n' + str(e.get('stack', '')) for e in errors)
                text = re.sub(r'\x1b\[[0-9;]*m', '', text)
                category = next((label for label, needle in (
                    ('STRICT_SELECTOR', 'strict mode violation'), ('TEST_TIMEOUT', 'Test timeout of'),
                    ('ACTION_TIMEOUT', 'TimeoutError'), ('SECURITY_ORIGIN', 'SecurityError')) if needle in text), 'ASSERTION_OR_OTHER')
                lines = {int(v) for v in re.findall(re.escape(filename) + r':([0-9]{1,4}):[0-9]{1,4}\b', text) if 0 < int(v) <= 2000}
                entry = {'source': source, 'status': case['status'], 'kind': category, 'line': next(iter(lines)) if len(lines) == 1 else None}
                codes = {}
                for label in ('Expected', 'Received'):
                    values = {int(v) for v in re.findall(r'^\s*' + label + r':\s*([1-5][0-9]{2})\s*$', text, re.M)}
                    if len(values) == 1:
                        codes[label.lower()] = next(iter(values))
                if len(codes) == 2:
                    entry['httpComparison'] = codes
                if len(result['cases']) < 20:
                    result['cases'].append(entry)
        except (OSError, ValueError, KeyError, TypeError):
            result['unavailable'] += 1
    result['unavailable'] += max(0, len(directories) - 64)
    return result


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
                result['cases'].append({'suite': name, 'test': method,
                    'kind': bounded_browser_kind(name, method, text, kind(text))})
        except (OSError, ValueError, ET.ParseError):
            result['unavailable_reports'] += 1
    result['unavailable_reports'] += max(0, len(files) - 128)
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--browser-private', type=Path)
    args = parser.parse_args()
    spec = importlib.util.spec_from_file_location('report_inventory', Path(__file__).with_name('verify-reports.py'))
    reports = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(reports)
    print('SAFE_FAILURE_KINDS ' + json.dumps(classify(args.source, reports.REQUIRED), sort_keys=True))
    if args.browser_private is not None:
        print('SAFE_BROWSER_FAILURES ' + json.dumps(browser_failures(args.browser_private), sort_keys=True))
