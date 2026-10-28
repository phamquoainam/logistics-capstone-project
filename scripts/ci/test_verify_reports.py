"""Exercise the CI guard's fail-closed behavior and sanitized artifact boundary."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import re
import tempfile
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location('reports', Path(__file__).with_name('verify-reports.py'))
reports = importlib.util.module_from_spec(spec)
spec.loader.exec_module(reports)


class ReportGuardTests(unittest.TestCase):
    def test_customer_admission_unit_methods_cannot_be_missing_or_skipped(self):
        for mode in ('h2','postgres'):
            for suffix,methods in reports.CUSTOMER_ADMISSION_UNIT_REQUIRED.items():
                name=reports.BASE+suffix
                for change in ('missing','skipped'):
                    with self.subTest(mode=mode,suite=suffix,change=change):
                        self.fixture(mode)
                        suite=ET.parse(self.source/('TEST-'+name+'.xml')).getroot()
                        case=next(c for c in suite.findall('testcase') if c.get('name')==methods[0])
                        if change=='missing':suite.remove(case);suite.set('tests',str(len(methods)-1))
                        else:ET.SubElement(case,'skipped');suite.set('skipped','1')
                        self.save(suite);self.assertFalse(self.check(mode))

    def test_customer_pg_methods_cannot_be_omitted_or_skipped(self):
        for name,methods in [(reports.IDENTITY_GRANTS,reports.CUSTOMER_ADMISSION_PG_REQUIRED),
                             (reports.IDENTITY_MIGRATION,reports.CUSTOMER_MIGRATION_PG_REQUIRED)]:
            for method in methods:
                for change in ('missing','skipped'):
                    with self.subTest(method=method,change=change):
                        self.fixture('postgres')
                        suite=ET.parse(self.source/('TEST-'+name+'.xml')).getroot()
                        case=next(c for c in suite.findall('testcase') if c.get('name')==method)
                        if change=='missing':suite.remove(case);suite.set('tests',str(len(suite.findall('testcase'))))
                        else:ET.SubElement(case,'skipped');suite.set('skipped','1')
                        self.save(suite);self.assertFalse(self.check('postgres'))

    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.source = Path(self.directory.name) / 'source'
        self.output = Path(self.directory.name) / 'public'
        self.source.mkdir()

    def fixture(self, mode='architecture'):
        for old in self.source.glob('TEST-*.xml'):
            old.unlink()  # Only synthetic fixture reports in this TemporaryDirectory.
        for name, methods in reports.expected_suites(mode).items():
            skip = (mode == 'h2' and name in reports.PG_ONLY) or (mode in ('h2', 'postgres') and name in reports.FOUNDATION_ONLY)
            suite = ET.Element('testsuite', name=name, tests=str(len(methods)), failures='0', errors='0',
                               skipped=str(len(methods) if skip else 0))
            for method in methods:
                case = ET.SubElement(suite, 'testcase', classname=name, name=method)
                if skip:
                    ET.SubElement(case, 'skipped')
            self.save(suite)

    def save(self, suite):
        ET.ElementTree(suite).write(self.source / ('TEST-' + suite.attrib['name'] + '.xml'), encoding='utf-8')

    def check(self, mode='architecture', exit_code=0):
        with contextlib.redirect_stdout(io.StringIO()):
            return reports.verify(self.source, self.output, mode, exit_code)

    def negative(self):
        path = self.source / ('TEST-' + reports.BASE + 'architecture.ArchitectureNegativeFixtureTests.xml')
        return ET.parse(path).getroot()

    def test_valid_architecture_and_both_database_modes(self):
        for mode in ('architecture', 'h2', 'postgres', 'foundation'):
            with self.subTest(mode=mode):
                self.fixture(mode)
                self.assertTrue(self.check(mode))

    def test_foundation_skips_are_rejected_in_real_infrastructure_mode(self):
        self.fixture('foundation')
        path = self.source / ('TEST-' + reports.BASE + 'demo.FoundationDependencyTests.xml')
        suite = ET.parse(path).getroot()
        ET.SubElement(suite.find('testcase'), 'skipped')
        suite.set('skipped', '1')
        self.save(suite)
        self.assertFalse(self.check('foundation'))

    def test_missing_profile_or_foundation_suite_fails_existing_modes(self):
        for mode in ('h2', 'postgres'):
            for suffix in ('demo.FoundationProfileTests', 'demo.FoundationDependencyTests',
                           'platform.internal.infrastructure.FoundationConfigurationTests'):
                with self.subTest(mode=mode, suffix=suffix):
                    self.fixture(mode)
                    (self.source / ('TEST-' + reports.BASE + suffix + '.xml')).unlink()
                    self.assertFalse(self.check(mode))

    def test_empty_reports_fail_even_when_maven_succeeds(self):
        self.assertFalse(self.check())

    def test_testcontainers_missing_suite_or_method_fails_postgres(self):
        for name in reports.TESTCONTAINERS_ONLY:
            with self.subTest(name=name):
                self.fixture('postgres')
                path = self.source / ('TEST-' + name + '.xml')
                path.unlink()
                self.assertFalse(self.check('postgres'))
                self.fixture('postgres')
                suite = ET.parse(path).getroot()
                suite.remove(suite.find('testcase'))
                suite.set('tests', str(len(suite.findall('testcase'))))
                self.save(suite)
                self.assertFalse(self.check('postgres'))

    def test_testcontainers_skips_fail_postgres(self):
        self.fixture('postgres')
        name = reports.BASE + 'demo.PostgresTestcontainersTests'
        suite = ET.parse(self.source / ('TEST-' + name + '.xml')).getroot()
        ET.SubElement(suite.find('testcase'), 'skipped')
        suite.set('skipped', '1')
        self.save(suite)
        self.assertFalse(self.check('postgres'))

    def test_testcontainers_reports_are_not_accepted_in_h2(self):
        self.fixture('postgres')
        self.assertFalse(self.check('h2'))

    def test_generated_credentials_are_checked_without_disclosure(self):
        secret = 'synthetic_task_c_credential_'.ljust(43, 'x')
        path = self.source / 'credentials-private.txt'
        path.write_text(secret + '\n')
        log = self.source / 'private-diagnostic.txt'
        log.write_text('safe metadata')
        self.assertTrue(reports.audit_task_c_credentials(path, [log]))
        log.write_text('unsafe: ' + secret)
        self.assertFalse(reports.audit_task_c_credentials(path, [log]))

    def test_missing_empty_or_malformed_credential_evidence_fails_closed(self):
        path = self.source / 'missing-private.txt'
        log = self.source / 'diagnostic.txt'
        log.write_text('safe')
        self.assertFalse(reports.audit_task_c_credentials(path, [log]))
        for content in ('', '\n', 'bad\n'):
            path.write_text(content)
            self.assertFalse(reports.audit_task_c_credentials(path, [log]))
        path.write_text('x' * 43 + '\n')
        self.assertFalse(reports.audit_task_c_credentials(path, []))
        self.assertFalse(reports.audit_task_c_credentials(path, [path]))

    def runtime_evidence(self):
        self.enterContext(patch.dict(reports.os.environ, {'GITHUB_ACTIONS': 'false', 'TASK_C_EXECUTION_ID': LIFECYCLE_LOCAL}))
        path = self.source / 'task-c-runtime.txt'
        return path if path.exists() else lifecycle_fixture(self.source)

    def test_cleanup_audit_requires_complete_evidence(self):
        self.assertFalse(reports.audit_task_c_cleanup(self.source / 'missing'))
        path = self.runtime_evidence()
        path.write_text(path.read_text().replace('taskRun=', 'invalid='))
        self.assertFalse(reports.audit_task_c_cleanup(path))

    def test_cleanup_audit_is_read_only_and_accepts_no_owned_resources(self):
        with patch.object(reports.subprocess, 'run') as run:
            run.return_value.returncode = 0
            run.return_value.stdout = ''
            self.assertTrue(reports.audit_task_c_cleanup(self.runtime_evidence(), timeout=0))
            self.assertEqual(3, run.call_count)
            for call in run.call_args_list:
                self.assertIn(call.args[0][1], ('ps', 'volume'))
                self.assertFalse(set(call.args[0]).intersection({'rm', 'prune', 'stop', 'kill'}))

    def test_cleanup_audit_rejects_leaked_resources_or_docker_failure(self):
        with patch.object(reports.subprocess, 'run') as run:
            for outputs in (('a' * 64, '', ''), ('', 'c' * 64, ''), ('', '', 'd' * 64)):
                run.side_effect = [reports.subprocess.CompletedProcess([], 0, stdout=value) for value in outputs]
                self.assertFalse(reports.audit_task_c_cleanup(self.runtime_evidence(), timeout=0))
            run.side_effect = None
            run.return_value.returncode = 1
            self.assertFalse(reports.audit_task_c_cleanup(self.runtime_evidence(), timeout=0))

    def test_missing_health_suite_fails_full_backend_modes(self):
        for mode in ('h2', 'postgres'):
            for suite in ('FoundationHealthTests', 'PostgresHealthSafetyTests'):
                with self.subTest(mode=mode, suite=suite):
                    self.fixture(mode)
                    path = self.source / ('TEST-' + reports.BASE + 'demo.' + suite + '.xml')
                    path.unlink()  # Only synthetic XML in this test's TemporaryDirectory.
                    self.assertFalse(self.check(mode))

    def test_real_junit_injected_parameter_name_is_supported(self):
        self.fixture('h2')
        name = reports.BASE + 'demo.DemoSecurityEvidenceTests'
        suite = ET.parse(self.source / ('TEST-' + name + '.xml')).getroot()
        suite.find('testcase').set('name', 'credentialsAndBearerTokenDoNotAppearInApplicationLogs(CapturedOutput)')
        self.save(suite)
        self.assertTrue(self.check('h2'))

    def test_arbitrary_dynamic_parameter_name_is_rejected_and_not_published(self):
        self.fixture()
        suite = self.negative()
        suite.find('testcase').set('name', 'financeCannotUseOrderInternalRepository(private-token)')
        self.save(suite)
        self.assertFalse(self.check())
        self.assertNotIn('private-token', (self.output / 'tests.xml').read_text())

    def test_missing_negative_method_fails(self):
        self.fixture()
        suite = self.negative()
        suite.remove(suite.find('testcase'))
        suite.set('tests', str(len(suite.findall('testcase'))))
        self.save(suite)
        self.assertFalse(self.check())

    def test_skipped_negative_fixture_fails(self):
        self.fixture()
        suite = self.negative()
        ET.SubElement(suite.find('testcase'), 'skipped')
        suite.set('skipped', '1')
        self.save(suite)
        self.assertFalse(self.check())

    def test_failed_fixture_and_secret_output_are_not_published(self):
        self.fixture()
        suite = self.negative()
        suite.set('failures', '1')
        secret = 'synthetic-token-must-never-be-published'
        ET.SubElement(suite.find('testcase'), 'failure', message=secret).text = secret
        ET.SubElement(suite, 'system-out').text = secret
        ET.SubElement(suite, 'properties', password=secret)
        self.save(suite)
        self.assertFalse(self.check())
        for artifact in self.output.iterdir():
            self.assertNotIn(secret, artifact.read_text(encoding='utf-8'))

    def test_maven_failure_is_not_hidden_by_green_reports(self):
        self.fixture()
        self.assertFalse(self.check(exit_code=1))

    def test_postgres_conditional_skips_fail_in_postgres_mode(self):
        self.fixture('h2')
        self.assertFalse(self.check('postgres'))

    def test_failure_cannot_be_hidden_as_an_allowed_postgres_skip(self):
        self.fixture('h2')
        suite = ET.parse(self.source / ('TEST-' + reports.PG + '.xml')).getroot()
        ET.SubElement(suite.find('testcase'), 'failure')
        suite.set('failures', '1')
        self.save(suite)
        self.assertFalse(self.check('h2'))

    def test_missing_suite_fails(self):
        self.fixture()
        next(self.source.glob('*NegativeFixtureTests.xml')).unlink()
        self.assertFalse(self.check())

    def test_inconsistent_counts_fail(self):
        self.fixture()
        suite = self.negative()
        suite.set('tests', '0')
        self.save(suite)
        self.assertFalse(self.check())

    def test_duplicate_test_fails(self):
        self.fixture()
        suite = self.negative()
        ET.SubElement(suite, 'testcase', **suite.find('testcase').attrib)
        suite.set('tests', '13')
        self.save(suite)
        self.assertFalse(self.check())

    def test_malformed_xml_fails_without_exposing_content(self):
        self.fixture()
        (self.source / 'TEST-broken.xml').write_text('<not-xml secret-content', encoding='utf-8')
        self.assertFalse(self.check())
        self.assertNotIn('secret-content', (self.output / 'summary.json').read_text())

    def s2_suite(self, name=None):
        name = name or reports.BASE + 'identity.internal.application.TokenConfigurationEvidenceTests'
        return ET.parse(self.source / ('TEST-' + name + '.xml')).getroot()

    def test_s2_exact_source_reviewed_counts_and_job_totals(self):
        self.assertEqual([20, 38, 17, 34], [len(v) for v in reports.S2_REQUIRED.values()])
        for name, methods in reports.S2_REQUIRED.items():
            self.assertEqual(len(methods), len(set(methods)), name)
        # Prior cases retained; authority composition requires9 unit and12 PostgreSQL identities.
        for mode, passed, skipped in [('architecture', 23, 0), ('h2', 635, 17),
                                      ('postgres', 987, 5), ('foundation', 5, 0)]:
            with self.subTest(mode=mode):
                self.fixture(mode)
                self.assertTrue(self.check(mode))
                summary = json.loads((self.output / 'summary.json').read_text())
                self.assertEqual(passed, summary['passed'])
                self.assertEqual(skipped, summary['skipped'])
                self.assertEqual(passed + skipped, summary['discovered'])

    def test_original_architecture_and_foundation_inventory_remains_unchanged(self):
        self.assertEqual(2, len(reports.expected_suites('architecture')))
        self.assertEqual({reports.BASE + 'demo.FoundationDependencyTests'}, set(reports.expected_suites('foundation')))
        self.assertTrue(set(reports.S2_REQUIRED).isdisjoint(reports.expected_suites('architecture')))
        self.assertTrue(set(reports.S2_REQUIRED).isdisjoint(reports.expected_suites('foundation')))
        for mode in ('architecture', 'foundation'):
            self.fixture(mode)
            self.assertTrue(self.check(mode))

    def test_each_missing_s2_class_rejected_in_both_database_modes(self):
        for mode in ('h2', 'postgres'):
            for name in reports.S2_REQUIRED:
                with self.subTest(mode=mode, name=name):
                    self.fixture(mode)
                    (self.source / ('TEST-' + name + '.xml')).unlink()
                    self.assertFalse(self.check(mode))

    def test_pre_s2_database_inventory_no_longer_silently_passes(self):
        for mode in ('h2', 'postgres'):
            self.fixture(mode)
            for name in reports.S2_REQUIRED:
                (self.source / ('TEST-' + name + '.xml')).unlink()
            self.assertFalse(self.check(mode))

    def test_missing_s2_invocation_rejected(self):
        for name in reports.S2_REQUIRED:
            self.fixture('h2')
            suite = self.s2_suite(name)
            suite.remove(suite.find('testcase'))
            suite.set('tests', str(len(suite.findall('testcase'))))
            self.save(suite)
            self.assertFalse(self.check('h2'))

    def test_duplicate_s2_invocation_or_suite_rejected(self):
        self.fixture('h2')
        suite = self.s2_suite()
        ET.SubElement(suite, 'testcase', **suite.find('testcase').attrib)
        suite.set('tests', str(len(suite.findall('testcase'))))
        self.save(suite)
        self.assertFalse(self.check('h2'))
        self.fixture('h2')
        ET.ElementTree(self.s2_suite()).write(self.source / 'TEST-duplicate.xml', encoding='utf-8')
        self.assertFalse(self.check('h2'))

    def test_s2_skips_failures_and_errors_rejected_in_both_database_modes(self):
        for mode in ('h2', 'postgres'):
            for tag, counter in [('skipped', 'skipped'), ('failure', 'failures'), ('error', 'errors')]:
                with self.subTest(mode=mode, tag=tag):
                    self.fixture(mode)
                    suite = self.s2_suite()
                    ET.SubElement(suite.find('testcase'), tag)
                    suite.set(counter, '1')
                    self.save(suite)
                    self.assertFalse(self.check(mode))

    def test_s2_out_of_range_index_or_wrong_signature_is_rejected(self):
        for method in ['whitespaceOnlySecretMustRejectContext(String, String)[4]',
                       'whitespaceOnlySecretMustRejectContext(String)[1]',
                       'whitespaceOnlySecretMustRejectContext(String, String)[0]',
                       'unapprovedMethod']:
            self.fixture('h2')
            suite = self.s2_suite()
            suite.find('testcase').set('name', method)
            self.save(suite)
            self.assertFalse(self.check('h2'))

    def test_arbitrary_s2_parameter_payload_rejected_without_publication(self):
        self.fixture('h2')
        suite = self.s2_suite()
        marker = 'synthetic-private-parameter'
        suite.find('testcase').set('name', 'whitespaceOnlySecretMustRejectContext(' + marker + ')[1]')
        self.save(suite)
        self.assertFalse(self.check('h2'))
        for artifact in self.output.iterdir():
            self.assertNotIn(marker, artifact.read_text(encoding='utf-8'))

    def test_s2_suite_not_accepted_in_architecture_or_foundation_job(self):
        self.fixture('h2')
        extra = self.s2_suite()
        for mode in ('architecture', 'foundation'):
            self.fixture(mode)
            self.save(extra)
            self.assertFalse(self.check(mode))

    def test_wrong_s2_classname_and_unknown_suite_rejected(self):
        self.fixture('h2')
        suite = self.s2_suite()
        suite.find('testcase').set('classname', reports.BASE + 'architecture.ArchitectureTests')
        self.save(suite)
        self.assertFalse(self.check('h2'))
        self.fixture('h2')
        ET.ElementTree(ET.Element('testsuite', name=reports.BASE + 'UnapprovedTests',
                                 tests='0', failures='0', errors='0', skipped='0')).write(
                                     self.source / 'TEST-unexpected.xml', encoding='utf-8')
        self.assertFalse(self.check('h2'))

    def test_s2_invalid_counts_or_incomplete_artifact_rejected(self):
        self.fixture('h2')
        suite = self.s2_suite()
        suite.set('failures', '1')
        self.save(suite)
        self.assertFalse(self.check('h2'))
        self.fixture('h2')
        suite = self.s2_suite()
        del suite.attrib['tests']
        self.save(suite)
        self.assertFalse(self.check('h2'))

    def seed_suite(self):
        return ET.parse(self.source / ('TEST-' + reports.SEED_PREFLIGHT + '.xml')).getroot()

    def seed_rejection(self, mode, missing=None, malformed=False):
        self.assertFalse(self.check(mode))
        summary = json.loads((self.output / 'summary.json').read_text(encoding='utf-8'))
        self.assertEqual('FAIL', summary['status'])
        if missing is not None:
            # Consistent fixture counters: rejection must be the target identity,
            # not an unrelated XML/count error masquerading as a successful test.
            self.assertEqual([reports.SEED_PREFLIGHT + '.' + missing + ': required passed'], summary['problems'])
        if malformed:
            self.assertIn('Malformed, unexpected or inconsistent Surefire report', summary['problems'])
        return summary

    def test_seed_finite_inventory_has_65_invocations_and_nine_singletons(self):
        methods = reports.SEED_PREFLIGHT_REQUIRED
        self.assertEqual(74, len(methods))
        self.assertEqual(74, len(set(methods)))
        groups = {
            'invalidMissingAccountFailsPreflightWithoutEncodingOrWriting(String, String, String)': 25,
            'pinnedBCryptCharacterization(String, String, int, int, boolean, boolean)': 15,
            'preflightKeepsCharacterRulesAndRejectsEncoderByteOverflow(String, String, int, int, boolean, boolean)': 15,
            'byteOverflowFailsBeforeEncodingForEveryMissingAccount(String)': 5,
            'existingAccountExemptionIgnoresUnusedInvalidPasswords(String, String)': 5,
        }
        for signature, count in groups.items():
            self.assertEqual({f'{signature}[{i}]' for i in range(1, count + 1)},
                             {method for method in methods if method.startswith(signature + '[')})
        self.assertEqual(9, len([method for method in methods if '(' not in method]))

    def test_seed_and_all_s2_identities_pass_in_both_database_jobs(self):
        for mode in ('h2', 'postgres'):
            with self.subTest(mode=mode):
                self.fixture(mode)
                self.assertTrue(self.check(mode))
                published = ET.parse(self.output / 'tests.xml').getroot().findall('testcase')
                seed = [c for c in published if c.attrib['classname'] == reports.SEED_PREFLIGHT]
                s2 = [c for c in published if c.attrib['classname'] in reports.S2_REQUIRED]
                self.assertEqual(74, len(seed))
                self.assertEqual(set(reports.SEED_PREFLIGHT_REQUIRED), {c.attrib['name'] for c in seed})
                self.assertEqual(109, len(s2))
                self.assertTrue(all(len(c) == 0 for c in seed + s2))

    def test_each_missing_seed_invocation_is_rejected_for_identity_not_counts(self):
        for mode in ('h2', 'postgres'):
            for method in [m for m in reports.SEED_PREFLIGHT_REQUIRED if '(' in m]:
                with self.subTest(mode=mode, method=method):
                    self.fixture(mode)
                    self.assertTrue(self.check(mode))
                    suite = self.seed_suite()
                    suite.remove(next(c for c in suite if c.attrib['name'] == method))
                    suite.set('tests', '73')
                    self.save(suite)
                    self.seed_rejection(mode, missing=method)

    def test_each_missing_seed_singleton_is_rejected_for_identity_not_counts(self):
        for mode in ('h2', 'postgres'):
            for method in [m for m in reports.SEED_PREFLIGHT_REQUIRED if '(' not in m]:
                with self.subTest(mode=mode, method=method):
                    self.fixture(mode)
                    self.assertTrue(self.check(mode))
                    suite = self.seed_suite()
                    suite.remove(next(c for c in suite if c.attrib['name'] == method))
                    suite.set('tests', '73')
                    self.save(suite)
                    self.seed_rejection(mode, missing=method)

    def test_duplicate_seed_invocation_and_suite_are_rejected(self):
        for mode in ('h2', 'postgres'):
            for duplicate in ('invocation', 'suite'):
                with self.subTest(mode=mode, duplicate=duplicate):
                    self.fixture(mode)
                    self.assertTrue(self.check(mode))
                    suite = self.seed_suite()
                    if duplicate == 'invocation':
                        ET.SubElement(suite, 'testcase', **suite.find('testcase').attrib)
                        suite.set('tests', '75')
                        self.save(suite)
                    else:
                        ET.ElementTree(suite).write(self.source / 'TEST-duplicate.xml', encoding='utf-8')
                    self.seed_rejection(mode, malformed=True)

    def test_seed_rejects_both_index_boundaries_and_changed_signatures_for_every_group(self):
        signatures = {m.rsplit('[', 1)[0] for m in reports.SEED_PREFLIGHT_REQUIRED if '(' in m}
        for mode in ('h2', 'postgres'):
            for signature in sorted(signatures):
                count = sum(m.startswith(signature + '[') for m in reports.SEED_PREFLIGHT_REQUIRED)
                for invalid in (signature + '[0]', signature + f'[{count + 1}]',
                                signature.split('(')[0] + '(Object)[1]'):
                    with self.subTest(mode=mode, invalid=invalid):
                        self.fixture(mode)
                        self.assertTrue(self.check(mode))
                        suite = self.seed_suite()
                        suite.find('testcase').set('name', invalid)
                        self.save(suite)
                        self.seed_rejection(mode, malformed=True)

    def test_seed_unexpected_extra_case_is_rejected_with_consistent_counters(self):
        for mode in ('h2', 'postgres'):
            self.fixture(mode)
            self.assertTrue(self.check(mode))
            suite = self.seed_suite()
            ET.SubElement(suite, 'testcase', classname=reports.SEED_PREFLIGHT, name='unapprovedExtraTest')
            suite.set('tests', '75')
            self.save(suite)
            self.seed_rejection(mode, malformed=True)

    def test_seed_wrong_testcase_class_is_rejected(self):
        for mode in ('h2', 'postgres'):
            self.fixture(mode)
            self.assertTrue(self.check(mode))
            suite = self.seed_suite()
            suite.find('testcase').set('classname', reports.BASE + 'architecture.ArchitectureTests')
            self.save(suite)
            self.seed_rejection(mode, malformed=True)

    def test_seed_unknown_suite_cannot_borrow_approved_method_names(self):
        for mode in ('h2', 'postgres'):
            self.fixture(mode)
            self.assertTrue(self.check(mode))
            suite = self.seed_suite()
            (self.source / ('TEST-' + reports.SEED_PREFLIGHT + '.xml')).unlink()
            suite.set('name', reports.SEED_PREFLIGHT + 'Unapproved')
            for case in suite:
                case.set('classname', suite.attrib['name'])
            self.save(suite)
            self.seed_rejection(mode, malformed=True)

    def test_seed_cannot_be_assigned_to_architecture_or_foundation_job(self):
        self.fixture('h2')
        seed = self.seed_suite()
        for mode in ('architecture', 'foundation'):
            self.fixture(mode)
            self.assertTrue(self.check(mode))
            self.assertNotIn(reports.SEED_PREFLIGHT, reports.expected_suites(mode))
            self.save(seed)
            self.seed_rejection(mode, malformed=True)

    def test_seed_skip_failure_and_error_each_require_pass_in_both_database_jobs(self):
        for mode in ('h2', 'postgres'):
            for tag, counter in [('skipped', 'skipped'), ('failure', 'failures'), ('error', 'errors')]:
                with self.subTest(mode=mode, tag=tag):
                    self.fixture(mode)
                    self.assertTrue(self.check(mode))
                    suite = self.seed_suite()
                    case = suite.find('testcase')
                    ET.SubElement(case, tag)
                    suite.set(counter, '1')
                    self.save(suite)
                    self.seed_rejection(mode, missing=case.attrib['name'])

    def test_seed_conflicting_outcomes_are_rejected(self):
        for mode in ('h2', 'postgres'):
            self.fixture(mode)
            self.assertTrue(self.check(mode))
            suite = self.seed_suite()
            for tag, counter in [('failure', 'failures'), ('skipped', 'skipped')]:
                ET.SubElement(suite.find('testcase'), tag)
                suite.set(counter, '1')
            self.save(suite)
            self.seed_rejection(mode, malformed=True)

    def test_seed_malformed_xml_and_incomplete_metadata_fail_closed(self):
        for mode in ('h2', 'postgres'):
            for damage in ('xml', 'tests', 'name', 'classname'):
                with self.subTest(mode=mode, damage=damage):
                    self.fixture(mode)
                    self.assertTrue(self.check(mode))
                    suite = self.seed_suite()
                    if damage == 'xml':
                        (self.source / ('TEST-' + reports.SEED_PREFLIGHT + '.xml')).write_text('<testsuite', encoding='utf-8')
                    else:
                        target = suite.find('testcase') if damage == 'classname' else suite
                        del target.attrib[damage]
                        ET.ElementTree(suite).write(
                            self.source / ('TEST-' + reports.SEED_PREFLIGHT + '.xml'), encoding='utf-8')
                    self.seed_rejection(mode, malformed=True)

    def test_missing_seed_report_rejects_all_74_required_identities(self):
        for mode in ('h2', 'postgres'):
            self.fixture(mode)
            self.assertTrue(self.check(mode))
            (self.source / ('TEST-' + reports.SEED_PREFLIGHT + '.xml')).unlink()
            summary = self.seed_rejection(mode)
            self.assertEqual({reports.SEED_PREFLIGHT + '.' + m + ': required passed'
                              for m in reports.SEED_PREFLIGHT_REQUIRED}, set(summary['problems']))

    def test_seed_inconsistent_aggregate_counters_are_rejected(self):
        for mode in ('h2', 'postgres'):
            for counter in ('tests', 'failures', 'errors', 'skipped'):
                with self.subTest(mode=mode, counter=counter):
                    self.fixture(mode)
                    self.assertTrue(self.check(mode))
                    suite = self.seed_suite()
                    suite.set(counter, str(int(suite.attrib[counter]) + 1))
                    self.save(suite)
                    self.seed_rejection(mode, malformed=True)

    def test_seed_unsafe_parameter_payload_is_rejected_without_publication(self):
        marker = 'synthetic-private-seed-parameter'
        for mode in ('h2', 'postgres'):
            self.fixture(mode)
            self.assertTrue(self.check(mode))
            suite = self.seed_suite()
            suite.find('testcase').set('name', 'byteOverflowFailsBeforeEncodingForEveryMissingAccount(' + marker + ')[1]')
            self.save(suite)
            self.seed_rejection(mode, malformed=True)
            for artifact in self.output.iterdir():
                self.assertNotIn(marker, artifact.read_text(encoding='utf-8'))

    def test_seed_failure_skip_and_error_payloads_are_never_published(self):
        marker = 'synthetic-private-seed-diagnostics'
        for mode in ('h2', 'postgres'):
            for tag, counter in [('skipped', 'skipped'), ('failure', 'failures'), ('error', 'errors')]:
                with self.subTest(mode=mode, tag=tag):
                    self.fixture(mode)
                    self.assertTrue(self.check(mode))
                    suite = self.seed_suite()
                    case = suite.find('testcase')
                    ET.SubElement(case, tag, message=marker).text = marker
                    suite.set(counter, '1')
                    ET.SubElement(suite, 'system-out').text = marker
                    ET.SubElement(suite, 'system-err').text = marker
                    ET.SubElement(suite, 'properties', password=marker)
                    self.save(suite)
                    self.seed_rejection(mode, missing=case.attrib['name'])
                    self.assertEqual({'summary.json', 'tests.xml'}, {p.name for p in self.output.iterdir()})
                    for artifact in self.output.iterdir():
                        self.assertNotIn(marker, artifact.read_text(encoding='utf-8'))

    def test_seed_valid_prior_reports_do_not_hide_nonzero_maven_exit(self):
        for mode in ('h2', 'postgres'):
            self.fixture(mode)
            self.assertTrue(self.check(mode))
            self.assertFalse(self.check(mode, exit_code=1))
            summary = json.loads((self.output / 'summary.json').read_text(encoding='utf-8'))
            self.assertEqual(['Maven returned a nonzero exit code'], summary['problems'])

    def test_empty_database_reports_are_rejected_despite_successful_maven_exit(self):
        for mode in ('h2', 'postgres'):
            self.fixture(mode)
            self.assertTrue(self.check(mode))
            for path in self.source.glob('TEST-*.xml'):
                path.unlink()  # Only this test's synthetic TemporaryDirectory.
            summary = self.seed_rejection(mode)
            self.assertEqual(0, summary['discovered'])
            self.assertIn('No test cases discovered', summary['problems'])


# Independent expected identities; neither this list nor collector acceptance is
# generated from the reports under test.
IDENTITY_METHODS = (
    'coldStartValidateAndRerunPreserveHistoryAndIdentityData',
    'upgradeV2PreservesAllDemoTablesWithoutImportingAccounts',
    'ownerOwnsEightTablesIndexesAndOnlyIdentityForeignKeys',
    'usernamesRemainExactCaseUnicodeAndRejectDuplicatesBlankAndNull',
    'accountLifecycleRequiresVerifiedActivatedCredentialAndPositiveVersions',
    'grantsAllowMultipleScopesButRejectDuplicatesInvalidScopesAndMissingAccounts',
    'sessionsRejectInvalidDeadlinesMissingAccountsAndNullExpiry',
    'refreshHashesMustBeUniqueCorrectSizeAndHaveExistingSession',
    'refreshLineageCannotCrossFamiliesForkOrHaveTwoLiveMembers',
    'proofPurposeLifetimeHashAndSingleOutstandingProofAreEnforced',
    'csrfSupportsPreauthAndSessionBindingButRejectsBadHashesAndReferences',
    'receiptsBindActorProofOrPreauthWithoutExistenceDisclosureOrDuplicateKey',
    'runtimeCanAppendAuditButCannotDdlDeleteAccountsOrMutateAuditAndReceipts',
    'realmRegistryStartsInactiveAndRuntimeCannotRegisterOrActivate',
    'realmForeignKeyRejectsUnknownIdentityButPreservesExternalScope',
    'realmUpgradePreservesV3DataAndChecksums',
    'unmatchedLegacyIdentityScopeStopsRealmUpgradeAtomically',
    'bootstrapCreatesPendingOnlyAndDuplicateCannotTakeOverAnAccount',
    'realmActivationRequiresVerifiedInvitationAndCannotBeReplayedOrReactivated',
    'bootstrapAuditFailureRollsBackPendingAccountGrantAndRealmBinding',
    'inactiveRealmIsNotEnoughToCreateActiveAdministrativeAuthority',
    'invitationV5UpgradePreservesLegacyProofsDataAndChecksums',
    'invitationOutboxEnforcesProofAssociationUniquenessAndProtectedShape',
    'invitationOutboxOwnershipAndRuntimePrivilegesRemainNarrow',
    'preexistingSchemaDriftFailsWithoutBaselineOrRepair',
    'customerV7UpgradePreservesV6DataChecksumsAndCreatesNoBindings',
    'customerIdentifierHasExactUniqueActiveAccountBinding',
    'customerSessionsRefreshAndCsrfEnforceOwnPurposeForeignKeys',
    'customerTablesRemainOwnerOwnedWithoutRuntimeDdlOrAuditEscalation',
    'enrollmentV8UpgradePreservesPopulatedV7AndEarlierChecksums',
    'enrollmentAndProofConstraintsDenyPlaintextWrongSubjectAndDeadline',
    'enrollmentPersistenceHasLeastPrivilegeAndImmutableReceipts',
    'customerQuotaV9PreservesV8ChecksumsAndExistingSystemReservations',
    'customerQuotaConstraintsPreserveCountedNonPasswordAndRejectUnknownPolicies',
    'recoveryV10UpgradePreservesV9AndRevokesOnlyUnsupportedUnboundRecovery',
    'recoveryProofDatabaseRequiresSecurityVersionWithoutSession',
    'proofReceiptDatabaseEnforcesPurposeRetentionAndNarrowPrivileges',
    'profileV11UpgradePreservesV10IdentityDataChecksumsAndOldSystemWrites',
    'profileDatabaseRequiresHistoryAndPreservesImmutableAccountVersionBinding',
    'profileRuntimeCannotRewriteHistoryAuditReceiptOrDeleteEvidence',
    'profileReceiptRequiresExactSubjectVersionRetentionAndContainsNoRawContact',
    'reauthenticationV12UpgradePreservesV11ChecksumsAccountsAndSystemSessions',
    'reauthenticationReceiptRequiresExactProofSessionVersionRetentionAndImmutablePrivileges',
    'ownerConsentV13UpgradePreservesV12ReceiptsAndNarrowPrivileges',
    'applicationV14UpgradePreservesV13ProfileAndRestrictsDraftHistory',
)


class IdentityMigrationInventoryTests(unittest.TestCase):
    # Reuse only synthetic-fixture helpers, not inherited test methods.
    setUp = ReportGuardTests.setUp
    fixture = ReportGuardTests.fixture
    save = ReportGuardTests.save
    check = ReportGuardTests.check

    def identity_suite(self):
        return ET.parse(self.source / ('TEST-' + reports.IDENTITY_MIGRATION + '.xml')).getroot()

    def assert_rejected(self, missing=None, malformed=False):
        self.assertFalse(self.check('postgres'))
        summary = json.loads((self.output / 'summary.json').read_text(encoding='utf-8'))
        self.assertEqual('FAIL', summary['status'])
        if missing is not None:
            self.assertEqual([reports.IDENTITY_MIGRATION + '.' + missing + ': required passed'], summary['problems'])
        if malformed:
            self.assertIn('Malformed, unexpected or inconsistent Surefire report', summary['problems'])
        return summary

    def test_identity_exact_source_reviewed_inventory(self):
        source = (Path(__file__).resolve().parents[2] / 'app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityMigrationTests.java').read_text(encoding='utf-8')
        self.assertEqual(list(IDENTITY_METHODS), re.findall(r'@Test\s+void\s+(\w+)\s*\(', source))
        self.assertIn('package com.elogistic.backend.identity.internal.infrastructure;', source)
        self.assertEqual('com.elogistic.backend.identity.internal.infrastructure.IdentityMigrationTests', reports.IDENTITY_MIGRATION)
        self.assertEqual(45, len(set(IDENTITY_METHODS)))
        self.assertEqual(list(IDENTITY_METHODS), reports.REQUIRED[reports.IDENTITY_MIGRATION])
        self.assertIn(reports.IDENTITY_MIGRATION, reports.TESTCONTAINERS_ONLY)
        self.assertNotIn(reports.IDENTITY_MIGRATION, reports.PG_ONLY)

    def test_identity_passes_only_in_postgres_and_publishes_all_methods(self):
        for mode in ('architecture', 'h2', 'postgres', 'foundation'):
            with self.subTest(mode=mode):
                self.assertEqual(mode == 'postgres', reports.IDENTITY_MIGRATION in reports.expected_suites(mode))
                self.fixture(mode)
                self.assertTrue(self.check(mode))
                cases = ET.parse(self.output / 'tests.xml').getroot().findall('testcase')
                identity = [c for c in cases if c.attrib['classname'] == reports.IDENTITY_MIGRATION]
                self.assertEqual(set(IDENTITY_METHODS) if mode == 'postgres' else set(), {c.attrib['name'] for c in identity})
                self.assertTrue(all(len(c) == 0 for c in identity))

    def test_each_missing_identity_method_rejected_for_exact_reason(self):
        for method in IDENTITY_METHODS:
            with self.subTest(method=method):
                self.fixture('postgres')
                self.assertTrue(self.check('postgres'))
                suite = self.identity_suite()
                suite.remove(next(c for c in suite if c.attrib['name'] == method))
                suite.set('tests', str(len(IDENTITY_METHODS) - 1))
                self.save(suite)
                self.assert_rejected(missing=method)

    def test_missing_identity_suite_requires_all_fourteen_methods(self):
        self.fixture('postgres')
        (self.source / ('TEST-' + reports.IDENTITY_MIGRATION + '.xml')).unlink()
        summary = self.assert_rejected()
        self.assertEqual({reports.IDENTITY_MIGRATION + '.' + m + ': required passed' for m in IDENTITY_METHODS}, set(summary['problems']))

    def test_each_renamed_identity_method_rejected_without_publication(self):
        marker = 'synthetic-unapproved-identity-method'
        for method in IDENTITY_METHODS:
            with self.subTest(method=method):
                self.fixture('postgres')
                suite = self.identity_suite()
                next(c for c in suite if c.attrib['name'] == method).set('name', marker)
                self.save(suite)
                self.assert_rejected(malformed=True)
                for artifact in self.output.iterdir():
                    self.assertNotIn(marker, artifact.read_text(encoding='utf-8'))

    def test_each_identity_skip_failure_or_error_is_rejected_and_redacted(self):
        marker = 'synthetic-private-identity-diagnostic'
        for method in IDENTITY_METHODS:
            for tag, counter in [('skipped', 'skipped'), ('failure', 'failures'), ('error', 'errors')]:
                with self.subTest(method=method, outcome=tag):
                    self.fixture('postgres')
                    suite = self.identity_suite()
                    case = next(c for c in suite if c.attrib['name'] == method)
                    ET.SubElement(case, tag, message=marker).text = marker
                    suite.set(counter, '1')
                    ET.SubElement(suite, 'system-out').text = marker
                    ET.SubElement(suite, 'system-err').text = marker
                    ET.SubElement(suite, 'properties', password=marker)
                    self.save(suite)
                    self.assert_rejected(missing=method)
                    self.assertEqual({'summary.json', 'tests.xml'}, {p.name for p in self.output.iterdir()})
                    for artifact in self.output.iterdir():
                        self.assertNotIn(marker, artifact.read_text(encoding='utf-8'))

    def test_duplicate_identity_case_or_suite_is_rejected(self):
        for duplicate in ('case', 'suite'):
            with self.subTest(duplicate=duplicate):
                self.fixture('postgres')
                suite = self.identity_suite()
                if duplicate == 'case':
                    ET.SubElement(suite, 'testcase', **suite.find('testcase').attrib)
                    suite.set('tests', '15')
                    self.save(suite)
                else:
                    ET.ElementTree(suite).write(self.source / 'TEST-duplicate-identity.xml', encoding='utf-8')
                self.assert_rejected(malformed=True)

    def test_forged_identity_class_or_extra_method_is_rejected(self):
        for damage in ('case-class', 'suite-class', 'extra-method'):
            with self.subTest(damage=damage):
                self.fixture('postgres')
                suite = self.identity_suite()
                if damage == 'case-class':
                    suite.find('testcase').set('classname', reports.PG)
                elif damage == 'suite-class':
                    (self.source / ('TEST-' + reports.IDENTITY_MIGRATION + '.xml')).unlink()
                    suite.set('name', reports.IDENTITY_MIGRATION + 'Unapproved')
                    for case in suite: case.set('classname', suite.attrib['name'])
                else:
                    ET.SubElement(suite, 'testcase', classname=reports.IDENTITY_MIGRATION, name='unapprovedExtraMethod')
                    suite.set('tests', '15')
                self.save(suite)
                self.assert_rejected(malformed=True)

    def test_identity_report_is_rejected_outside_postgres(self):
        self.fixture('postgres')
        suite = self.identity_suite()
        for mode in ('architecture', 'h2', 'foundation'):
            with self.subTest(mode=mode):
                self.fixture(mode)
                self.assertTrue(self.check(mode))
                self.save(suite)
                self.assertFalse(self.check(mode))

    def test_identity_incomplete_metadata_counts_or_xml_fail_closed(self):
        for damage in ('tests', 'failures', 'errors', 'skipped', 'name', 'classname', 'xml'):
            with self.subTest(damage=damage):
                self.fixture('postgres')
                suite = self.identity_suite()
                path = self.source / ('TEST-' + reports.IDENTITY_MIGRATION + '.xml')
                if damage == 'xml':
                    path.write_text('<testsuite synthetic-private-malformed', encoding='utf-8')
                else:
                    target = suite.find('testcase') if damage == 'classname' else suite
                    del target.attrib[damage]
                    ET.ElementTree(suite).write(path, encoding='utf-8')
                self.assert_rejected(malformed=True)
                for artifact in self.output.iterdir():
                    self.assertNotIn('synthetic-private-malformed', artifact.read_text(encoding='utf-8'))

    def test_identity_positive_reports_do_not_mask_maven_failure(self):
        self.fixture('postgres')
        self.assertTrue(self.check('postgres'))
        self.assertFalse(self.check('postgres', exit_code=1))
        summary = json.loads((self.output / 'summary.json').read_text(encoding='utf-8'))
        self.assertEqual(['Maven returned a nonzero exit code'], summary['problems'])


LIFECYCLE_LOCAL = '44444444-4444-4444-8444-444444444444'
LIFECYCLE_RUN = '11111111-1111-4111-8111-111111111111'
LIFECYCLE_SESSION = '22222222-2222-4222-8222-222222222222'
LIFECYCLE_SUITES = {
    'demo-postgres': 'com.elogistic.backend.demo.PostgresTestcontainersTests',
    'identity-migration': 'com.elogistic.backend.identity.internal.infrastructure.IdentityMigrationTests',
    'identity-grants': 'com.elogistic.backend.identity.internal.infrastructure.IdentityGrantApplicationTests',
}
LIFECYCLE_STAGES = ('reserved', 'observed', 'created', 'verified', 'stopped')


def lifecycle_fixture(directory, reverse=False):
    """Independent synthetic protocol fixtures; no Docker/Java execution or secret."""
    path = directory / 'task-c-runtime.txt'
    child = directory / 'task-c-runtime.d'
    child.mkdir()
    base = {'format': 'task-c-lifecycle-v2', 'execution': 'local:' + LIFECYCLE_LOCAL,
            'taskRun': LIFECYCLE_RUN, 'session': LIFECYCLE_SESSION}
    manifest = dict(base, **{'suite.' + key: value for key, value in LIFECYCLE_SUITES.items()})
    fixture_record(path, manifest)
    entries = list(LIFECYCLE_SUITES.items())
    if reverse: entries.reverse()
    for suite, name in entries:
        pg, volume = {'demo-postgres': ('a' * 64, 'c' * 64),
                      'identity-migration': ('d' * 64, 'e' * 64),
                      'identity-grants': ('1' * 64, '2' * 64)}[suite]
        facts = {'pgContainer': pg, 'pgVolume': volume, 'volumeContainer': pg, 'ryukContainer': 'b' * 64,
                 'pgImage': 'postgres:18.6-bookworm@sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c',
                 'ryukImage': 'testcontainers/ryuk:0.14.0@sha256:7c1a8a9a47c780ed0f983770a662f80deb115d95cce3e2daa3d12115b8cd28f0',
                 'pgLabelRun': LIFECYCLE_RUN, 'pgLabelSession': LIFECYCLE_SESSION, 'ryukLabelRun': LIFECYCLE_RUN,
                 'ryukName': '/testcontainers-ryuk-' + LIFECYCLE_SESSION}
        stages = list(LIFECYCLE_STAGES)
        if reverse: stages.reverse()
        for stage in stages:
            record = dict(base, suite=name, stage=stage)
            if stage == 'observed': record['pgContainer'] = pg
            elif stage in ('created', 'verified', 'stopped'): record.update(facts)
            fixture_record(child / (suite + '.' + stage), record)
    return path


def fixture_record(path, values):
    with path.open('x', encoding='ascii', newline='\n') as stream:
        stream.write(''.join(key + '=' + value + '\n' for key, value in values.items()))


class LifecycleAuditTests(unittest.TestCase):
    def setUp(self):
        ReportGuardTests.setUp(self)
        self.enterContext(patch.dict(reports.os.environ, {'GITHUB_ACTIONS': 'false', 'TASK_C_EXECUTION_ID': LIFECYCLE_LOCAL}))
        self.path = lifecycle_fixture(self.source)
        self.child = self.source / 'task-c-runtime.d'

    def alter(self, path, key, value):
        # Mutations of this test's synthetic files only, never real runtime evidence.
        lines = path.read_text(encoding='ascii').splitlines()
        path.write_text(''.join((key + '=' + value if line.startswith(key + '=') else line) + '\n' for line in lines), encoding='ascii')

    def facts(self, suite, key, value):
        for stage in ('created', 'verified', 'stopped'):
            self.alter(self.child / (suite + '.' + stage), key, value)

    def rejected(self, reason):
        with self.assertRaisesRegex(ValueError, '^' + re.escape(reason) + '$'):
            reports.task_c_resource_inventory(self.path)
        with patch.object(reports.subprocess, 'run') as run:
            self.assertFalse(reports.audit_task_c_cleanup(self.path, timeout=0))
            run.assert_not_called()  # Invalid evidence cannot even select a Docker action.

    def test_all_suite_records_are_distinct_and_complete_inventory_is_exact(self):
        self.assertEqual(LIFECYCLE_SUITES, reports.TASK_C_SUITES)
        ids, volumes, run = reports.task_c_resource_inventory(self.path)
        self.assertEqual({'a' * 64, 'b' * 64, 'd' * 64, '1' * 64}, ids)
        self.assertEqual({'c' * 64, 'e' * 64, '2' * 64}, volumes)
        self.assertEqual(LIFECYCLE_RUN, run)
        before = {p.name: p.read_bytes() for p in self.child.iterdir()}
        with self.assertRaises(FileExistsError):
            fixture_record(self.child / 'demo-postgres.created', {'unapproved': 'overwrite'})
        self.assertEqual(before, {p.name: p.read_bytes() for p in self.child.iterdir()})

    def test_complete_cleanup_accepts_foreign_resources_without_targeting_them(self):
        with patch.object(reports.subprocess, 'run') as run:
            run.side_effect = [reports.subprocess.CompletedProcess([], 0, stdout=value) for value in ('f' * 64, '9' * 64, '')]
            self.assertTrue(reports.audit_task_c_cleanup(self.path, timeout=0))
            self.assertEqual(3, run.call_count)
            self.assertEqual([['docker', 'ps', '-aq', '--no-trunc'], ['docker', 'volume', 'ls', '-q'],
                              ['docker', 'ps', '-aq', '--filter', 'label=com.elogistic.task-c.run=' + LIFECYCLE_RUN]],
                             [c.args[0] for c in run.call_args_list])

    def test_every_missing_suite_stage_rejects_before_docker(self):
        for suite in LIFECYCLE_SUITES:
            for stage in LIFECYCLE_STAGES:
                with self.subTest(suite=suite, stage=stage):
                    path = self.child / (suite + '.' + stage); before = path.read_bytes();path.unlink()
                    self.rejected('Missing, failed, duplicate or unexpected lifecycle record')
                    path.write_bytes(before)

    def test_suite_failure_before_completion_preserves_created_id_but_never_passes(self):
        for suite in LIFECYCLE_SUITES:
            with self.subTest(suite=suite):
                path = self.child / (suite + '.verified');before = path.read_bytes();path.unlink()
                observed = (self.child / (suite + '.observed')).read_bytes()
                self.rejected('Missing, failed, duplicate or unexpected lifecycle record')
                self.assertIn(b'pgContainer=', observed)
                self.assertEqual(observed, (self.child / (suite + '.observed')).read_bytes())
                path.write_bytes(before)

    def test_never_started_suite_and_explicit_failed_record_are_rejected(self):
        paths = [self.child / ('identity-migration.' + stage) for stage in LIFECYCLE_STAGES]
        before = {p: p.read_bytes() for p in paths}
        for p in paths: p.unlink()
        self.rejected('Missing, failed, duplicate or unexpected lifecycle record')
        for p, content in before.items(): p.write_bytes(content)
        fixture_record(self.child / 'identity-migration.failed', {'reason': 'START_FAILED'})
        self.rejected('Missing, failed, duplicate or unexpected lifecycle record')

    def test_duplicate_suite_or_record_is_not_deduplicated(self):
        duplicate = self.child / 'demo-postgres.created.copy'
        duplicate.write_bytes((self.child / 'demo-postgres.created').read_bytes())
        self.rejected('Missing, failed, duplicate or unexpected lifecycle record')
        duplicate.unlink()
        self.alter(self.path, 'suite.identity-migration', LIFECYCLE_SUITES['demo-postgres'])
        self.rejected('Unknown or duplicate suite')

    def test_truncated_duplicate_unknown_and_missing_fields_fail_for_exact_reasons(self):
        path = self.child / 'demo-postgres.created'; before = path.read_bytes()
        for payload, reason in [(before.rstrip(b'\n'), 'Incomplete evidence'),
                                (before + b'pgContainer=' + b'a' * 64 + b'\n', 'Invalid evidence field'),
                                (before + b'arbitraryResource=synthetic-private-value\n', 'Invalid evidence field'),
                                (before.replace(b'pgVolume=' + b'c' * 64 + b'\n', b''), 'Missing evidence field')]:
            with self.subTest(reason=reason):
                path.write_bytes(payload); self.rejected(reason)
        path.write_bytes(before)

    def test_stale_self_consistent_execution_is_rejected_against_current_job(self):
        for p in [self.path, *self.child.iterdir()]: self.alter(p, 'execution', 'local:55555555-5555-4555-8555-555555555555')
        self.rejected('Stale or unsupported execution')

    def test_hosted_job_attempt_binding_has_no_local_override(self):
        with patch.dict(reports.os.environ, {'GITHUB_ACTIONS': 'true', 'GITHUB_RUN_ID': '420', 'GITHUB_RUN_ATTEMPT': '2', 'GITHUB_JOB': 'verify'}):
            for p in [self.path, *self.child.iterdir()]: self.alter(p, 'execution', 'gha:420:2:verify')
            reports.task_c_resource_inventory(self.path)
            with patch.dict(reports.os.environ, {'GITHUB_RUN_ATTEMPT': '3'}):
                self.rejected('Stale or unsupported execution')
            with patch.dict(reports.os.environ, {'GITHUB_JOB': ''}):
                self.rejected('Invalid execution identity')

    def test_cross_run_session_or_suite_record_substitution_is_rejected(self):
        path = self.child / 'identity-migration.created'; before = path.read_bytes()
        for key, value in [('taskRun', '55555555-5555-4555-8555-555555555555'), ('session', LIFECYCLE_RUN),
                           ('suite', LIFECYCLE_SUITES['demo-postgres']), ('stage', 'verified')]:
            with self.subTest(key=key):
                path.write_bytes(before);self.alter(path, key, value);self.rejected('Lifecycle identity mismatch')
        path.write_bytes(before)

    def test_ownership_labels_and_reaper_session_must_match(self):
        for key, value in [('pgLabelRun', LIFECYCLE_LOCAL), ('pgLabelSession', LIFECYCLE_LOCAL),
                           ('ryukLabelRun', LIFECYCLE_LOCAL), ('ryukName', '/testcontainers-ryuk-' + LIFECYCLE_LOCAL)]:
            with self.subTest(key=key):
                paths = [self.child / ('identity-migration.' + s) for s in ('created','verified','stopped')]
                before = {p: p.read_bytes() for p in paths}
                self.facts('identity-migration', key, value);self.rejected('Resource ownership mismatch')
                for p, content in before.items(): p.write_bytes(content)

    def test_resource_substitution_bad_types_and_duplicate_resources_are_rejected(self):
        path = self.child / 'identity-migration.verified';before = path.read_bytes()
        self.alter(path, 'pgVolume', 'f' * 64);self.rejected('Resource substitution');path.write_bytes(before)
        paths = [self.child / ('identity-migration.' + s) for s in ('created','verified','stopped')]
        before = {p: p.read_bytes() for p in paths}
        for key, value, reason in [('pgVolume', 'bad-id', 'Invalid resource identity'),
                                   ('pgVolume', 'c' * 64, 'Duplicate suite resource'),
                                   ('pgImage', 'unapproved-image', 'Unapproved image'),
                                   ('volumeContainer', 'f' * 64, 'Volume/observation binding mismatch'),
                                   ('ryukContainer', 'f' * 64, 'Shared reaper identity mismatch')]:
            with self.subTest(key=key, reason=reason):
                for p, content in before.items(): p.write_bytes(content)
                self.facts('identity-migration', key, value);self.rejected(reason)
        for p, content in before.items(): p.write_bytes(content)

    def test_duplicate_pg_identity_across_suites_is_rejected(self):
        self.facts('identity-migration','pgContainer','a' * 64)
        self.facts('identity-migration','volumeContainer','a' * 64)
        self.alter(self.child / 'identity-migration.observed','pgContainer','a' * 64)
        self.rejected('Duplicate suite resource')

    def test_each_owned_container_or_volume_leftover_fails_read_only_audit(self):
        for container, volume, owned in [('a'*64,'',''),('b'*64,'',''),('d'*64,'',''),('1'*64,'',''),
                                         ('','c'*64,''),('','e'*64,''),('','2'*64,''),('','','f'*64)]:
            with self.subTest(container=container, volume=volume, owned=owned):
                with patch.object(reports.subprocess,'run') as run:
                    run.side_effect=[reports.subprocess.CompletedProcess([],0,stdout=v) for v in (container,volume,owned)]
                    self.assertFalse(reports.audit_task_c_cleanup(self.path,timeout=0))
                    self.assertTrue(all(c.args[0][1] in ('ps','volume') for c in run.call_args_list))

    def test_docker_failure_timeout_and_false_cleanup_flag_cannot_pass(self):
        with patch.object(reports.subprocess,'run') as run:
            run.return_value=reports.subprocess.CompletedProcess([],1,stdout='')
            self.assertFalse(reports.audit_task_c_cleanup(self.path,timeout=0))
            run.side_effect=reports.subprocess.TimeoutExpired('docker',1)
            self.assertFalse(reports.audit_task_c_cleanup(self.path,timeout=0))
        with self.path.open('a',encoding='ascii') as stream:stream.write('cleaned=true\n')
        self.rejected('Invalid evidence field')

    def test_suite_and_stage_file_order_does_not_change_accounting(self):
        other=self.source/'reverse';other.mkdir();path=lifecycle_fixture(other,reverse=True)
        self.assertEqual(reports.task_c_resource_inventory(self.path),reports.task_c_resource_inventory(path))

    def test_legacy_single_suite_evidence_is_not_a_combined_run_fallback(self):
        self.path.write_text('anonymousVolume='+'c'*64+'\n',encoding='ascii')
        self.rejected('Invalid evidence field')

    def test_aliased_file_is_rejected_without_docker_or_deletion(self):
        alias=self.source/'alias'
        reports.os.link(self.path,alias)
        self.rejected('Unsafe evidence file')

    def test_sensitive_malformed_metadata_is_never_printed_or_used(self):
        marker='synthetic-private-lifecycle-payload'
        with self.path.open('a',encoding='ascii') as stream:stream.write('unknown='+marker+'\n')
        stdout,stderr=io.StringIO(),io.StringIO()
        with contextlib.redirect_stdout(stdout),contextlib.redirect_stderr(stderr),patch.object(reports.subprocess,'run') as run:
            self.assertFalse(reports.audit_task_c_cleanup(self.path,timeout=0));run.assert_not_called()
        self.assertNotIn(marker,stdout.getvalue()+stderr.getvalue())

    def test_java_writer_protocol_hooks_and_fail_closed_boundaries_are_present(self):
        source=(Path(__file__).resolve().parents[2]/'app/backend/src/test/java/com/elogistic/backend/testsupport/PostgresTestcontainersSupport.java').read_text(encoding='utf-8')
        for name,key in [(name,key) for key,name in LIFECYCLE_SUITES.items()]:
            self.assertIn('"'+name+'", "'+key+'"',source)
        self.assertIn('StandardOpenOption.CREATE_NEW',source)
        self.assertIn('channel.force(true)',source)
        self.assertIn('containerIsCreated(String id)',source)
        self.assertLess(source.index('record("observed"'),source.index('resources=resourceFacts(this,false)'))
        self.assertLess(source.index('record("observed"'),source.index('if (!id.equals(getContainerId()))'))
        self.assertIn('owned.record("verified",facts,true)',source)
        self.assertLess(source.index('resources.equals(resourceFacts(this,false))'),source.index('super.stop();'))
        self.assertIn('requireAbsent(this,resources);',source)
        self.assertIn('withShowAll(true)',source)
        self.assertNotIn('Files.write(PRIVATE.resolve("task-c-runtime.txt")',source)
        self.assertIn('withStartupAttempts(1)',source)
        self.assertIn('StackWalker.getInstance()',source)

    def test_existing_postgres_workflow_consumes_the_same_manifest_entrypoint(self):
        root=Path(__file__).resolve().parents[2]
        launcher=(root/'scripts/ci/run-backend.sh').read_text(encoding='utf-8')
        workflow=(root/'.github/workflows/backend-verification.yml').read_text(encoding='utf-8')
        self.assertIn("cleanup = reports.audit_task_c_cleanup(private / 'task-c-runtime.txt')",launcher)
        self.assertIn('export TASK_C_PRIVATE_DIRECTORY="$private"',launcher)
        self.assertIn('bash scripts/ci/run-backend.sh "$VERIFICATION_MODE"',workflow)
        self.assertIn('name: Backend PostgreSQL 18',workflow)
        self.assertIn('runs-on: ubuntu-24.04',workflow)


class IdentityDomainInventoryTests(unittest.TestCase):
    # Reuse only synthetic report fixtures, without duplicating inherited tests.
    setUp = ReportGuardTests.setUp
    fixture = ReportGuardTests.fixture
    save = ReportGuardTests.save
    check = ReportGuardTests.check

    def test_domain_source_inventory_and_plain_jupiter_categories(self):
        root = Path(__file__).resolve().parents[2]
        sources = root / 'app/backend/src/test/java/com/elogistic/backend/identity/internal/domain'
        expected_names = {'IdentityAccountTests', 'IdentityAuthorizationTests',
                          'IdentitySessionTests', 'IdentityProofAndRefreshTests', 'IdentityValueTests'}
        self.assertEqual({p.stem for p in sources.glob('*Tests.java')}, expected_names)
        self.assertEqual(sum(map(len, reports.IDENTITY_DOMAIN_REQUIRED.values())), 48)
        for suffix in sorted(expected_names):
            source = (sources / (suffix + '.java')).read_text(encoding='utf-8')
            name = reports.BASE + 'identity.internal.domain.' + suffix
            methods = re.findall(r'@Test\s+void\s+(\w+)\s*\(', source)
            self.assertEqual(methods, reports.IDENTITY_DOMAIN_REQUIRED[name])
            self.assertEqual(len(methods), len(set(methods)))
            for forbidden in ('@Disabled', '@Testcontainers', '@Tag(', '@ParameterizedTest'):
                self.assertNotIn(forbidden, source)
            self.assertNotIn(name, reports.TESTCONTAINERS_ONLY | reports.PG_ONLY | reports.FOUNDATION_ONLY)
            for mode in ('h2', 'postgres'):
                self.assertIn(name, reports.expected_suites(mode))
            for mode in ('architecture', 'foundation'):
                self.assertNotIn(name, reports.expected_suites(mode))

    def test_missing_domain_class_and_each_method_fail_closed(self):
        for mode in ('h2', 'postgres'):
            self.fixture(mode)
            for name, methods in reports.IDENTITY_DOMAIN_REQUIRED.items():
                with self.subTest(mode=mode, suite=name):
                    path = self.source / ('TEST-' + name + '.xml')
                    original = path.read_bytes()
                    path.unlink()
                    self.assertFalse(self.check(mode))
                    path.write_bytes(original)
                    for method in methods:
                        suite = ET.fromstring(original)
                        case = next(c for c in suite if c.get('name') == method)
                        suite.remove(case)
                        suite.set('tests', str(len(suite.findall('testcase'))))
                        self.save(suite)
                        self.assertFalse(self.check(mode), method)
                    path.write_bytes(original)
            self.assertTrue(self.check(mode))

    def test_domain_skip_failure_and_error_each_reject_in_both_modes(self):
        for mode in ('h2', 'postgres'):
            self.fixture(mode)
            for name in reports.IDENTITY_DOMAIN_REQUIRED:
                path = self.source / ('TEST-' + name + '.xml')
                original = path.read_bytes()
                for status, count in [('skipped', 'skipped'), ('failure', 'failures'), ('error', 'errors')]:
                    with self.subTest(mode=mode, suite=name, status=status):
                        suite = ET.fromstring(original)
                        ET.SubElement(suite.find('testcase'), status)
                        suite.set(count, '1')
                        self.save(suite)
                        self.assertFalse(self.check(mode))
                path.write_bytes(original)

    def test_renamed_or_unknown_domain_invocation_is_rejected(self):
        self.fixture('h2')
        for name in reports.IDENTITY_DOMAIN_REQUIRED:
            path = self.source / ('TEST-' + name + '.xml')
            original = path.read_bytes()
            suite = ET.fromstring(original)
            suite.find('testcase').set('name', 'unreviewedInvocation[1]')
            self.save(suite)
            self.assertFalse(self.check('h2'))
            path.write_bytes(original)

    def test_domain_failure_diagnostics_are_not_published(self):
        self.fixture('h2')
        name = reports.BASE + 'identity.internal.domain.IdentityValueTests'
        path = self.source / ('TEST-' + name + '.xml')
        suite = ET.parse(path).getroot()
        marker = 'synthetic-private-domain-diagnostic'
        ET.SubElement(suite.find('testcase'), 'failure', message=marker).text = marker
        suite.set('failures', '1')
        ET.SubElement(suite, 'system-out').text = marker
        self.save(suite)
        self.assertFalse(self.check('h2'))
        published = ''.join(p.read_text(encoding='utf-8') for p in self.output.iterdir())
        self.assertNotIn(marker, published)
        self.assertIn('See local private diagnostics; raw output is not published.', published)

class IdentityGrantApplicationInventoryTests(unittest.TestCase):
    setUp = ReportGuardTests.setUp
    fixture = ReportGuardTests.fixture
    save = ReportGuardTests.save
    check = ReportGuardTests.check

    def test_grant_source_inventory_and_postgres_only_discovery(self):
        root = Path(__file__).resolve().parents[2]
        source = (root / 'app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityGrantApplicationTests.java').read_text(encoding='utf-8')
        methods = re.findall(r'@Test\s+void\s+(\w+)\s*\(', source)
        self.assertEqual(33, len(methods) - len(reports.IDENTITY_AUTHORITY_PG_REQUIRED) - len(reports.IDENTITY_REALM_PG_REQUIRED) - len(reports.IDENTITY_PROVISION_PG_REQUIRED) - len(reports.IDENTITY_INVITATION_PG_REQUIRED) - len(reports.IDENTITY_ACTIVATION_PG_REQUIRED) - len(reports.IDENTITY_SESSION_PG_REQUIRED) - len(reports.IDENTITY_CURRENT_SESSION_PG_REQUIRED) - len(reports.IDENTITY_ADMIN_PG_REQUIRED) - len(reports.IDENTITY_HTTP_PG_REQUIRED) - len(reports.IDENTITY_AUTH_PG_REQUIRED) - len(reports.CUSTOMER_ADMISSION_PG_REQUIRED))
        self.assertEqual(12, len(reports.IDENTITY_AUTHORITY_PG_REQUIRED))
        self.assertEqual(len(methods), len(set(methods)))
        self.assertEqual(methods, reports.REQUIRED[reports.IDENTITY_GRANTS])
        self.assertIn('@Tag("task-c")', source)
        self.assertNotIn('@Disabled', source)
        for mode in ('architecture', 'h2', 'foundation'):
            self.assertNotIn(reports.IDENTITY_GRANTS, reports.expected_suites(mode))
        self.assertIn(reports.IDENTITY_GRANTS, reports.expected_suites('postgres'))
        self.assertEqual(reports.IDENTITY_GRANTS, reports.TASK_C_SUITES['identity-grants'])

    def test_each_missing_grant_method_rejects_even_with_zero_maven_exit(self):
        self.fixture('postgres')
        path = self.source / ('TEST-' + reports.IDENTITY_GRANTS + '.xml')
        original = path.read_bytes()
        for method in reports.REQUIRED[reports.IDENTITY_GRANTS]:
            with self.subTest(method=method):
                suite = ET.fromstring(original)
                suite.remove(next(c for c in suite if c.get('name') == method))
                suite.set('tests', str(len(suite.findall('testcase'))))
                self.save(suite)
                self.assertFalse(self.check('postgres'))
        path.write_bytes(original)
        self.assertTrue(self.check('postgres'))

    def test_grant_skip_failure_error_and_rename_are_never_accepted(self):
        self.fixture('postgres')
        path = self.source / ('TEST-' + reports.IDENTITY_GRANTS + '.xml')
        original = path.read_bytes()
        for status, count in [('skipped', 'skipped'), ('failure', 'failures'), ('error', 'errors')]:
            suite = ET.fromstring(original)
            ET.SubElement(suite.find('testcase'), status)
            suite.set(count, '1')
            self.save(suite)
            self.assertFalse(self.check('postgres'))
        suite = ET.fromstring(original)
        suite.find('testcase').set('name', 'unapprovedMethod')
        self.save(suite)
        self.assertFalse(self.check('postgres'))

    def test_grant_failure_diagnostics_remain_private(self):
        self.fixture('postgres')
        path = self.source / ('TEST-' + reports.IDENTITY_GRANTS + '.xml')
        suite = ET.parse(path).getroot()
        marker = 'synthetic-private-grant-failure'
        ET.SubElement(suite.find('testcase'), 'failure', message=marker).text = marker
        ET.SubElement(suite, 'system-out').text = marker
        suite.set('failures', '1')
        self.save(suite)
        self.assertFalse(self.check('postgres'))
        self.assertNotIn(marker, ''.join(p.read_text(encoding='utf-8') for p in self.output.iterdir()))


class IdentityAuthorityCompositionInventoryTests(unittest.TestCase):
    setUp = ReportGuardTests.setUp
    fixture = ReportGuardTests.fixture
    save = ReportGuardTests.save
    check = ReportGuardTests.check

    def test_authority_source_identities_and_modes_are_exact(self):
        root = Path(__file__).resolve().parents[2]
        source = (root / 'app/backend/src/test/java/com/elogistic/backend/identity/internal/application/FailClosedGrantAuthorityTests.java').read_text(encoding='utf-8')
        methods = re.findall(r'@Test\s+void\s+(\w+)\s*\(', source)
        self.assertEqual(9, len(methods)); self.assertEqual(len(methods), len(set(methods)))
        self.assertEqual(methods, reports.REQUIRED[reports.IDENTITY_AUTHORITY_UNIT])
        self.assertNotIn('@Disabled', source); self.assertNotIn('@Testcontainers', source)
        for mode in ('h2', 'postgres'):
            self.assertIn(reports.IDENTITY_AUTHORITY_UNIT, reports.expected_suites(mode))
        for mode in ('architecture', 'foundation'):
            self.assertNotIn(reports.IDENTITY_AUTHORITY_UNIT, reports.expected_suites(mode))

    def test_missing_authority_unit_suite_and_every_method_fail_closed(self):
        for mode in ('h2', 'postgres'):
            self.fixture(mode)
            path = self.source / ('TEST-' + reports.IDENTITY_AUTHORITY_UNIT + '.xml')
            original = path.read_bytes(); path.unlink(); self.assertFalse(self.check(mode)); path.write_bytes(original)
            for method in reports.REQUIRED[reports.IDENTITY_AUTHORITY_UNIT]:
                suite = ET.fromstring(original)
                suite.remove(next(c for c in suite if c.get('name') == method))
                suite.set('tests', str(len(suite.findall('testcase')))); self.save(suite)
                self.assertFalse(self.check(mode), method)
            path.write_bytes(original); self.assertTrue(self.check(mode))

    def test_authority_skip_failure_error_and_rename_are_rejected(self):
        for mode in ('h2', 'postgres'):
            self.fixture(mode)
            path = self.source / ('TEST-' + reports.IDENTITY_AUTHORITY_UNIT + '.xml'); original = path.read_bytes()
            for status, count in [('skipped', 'skipped'), ('failure', 'failures'), ('error', 'errors')]:
                suite = ET.fromstring(original); ET.SubElement(suite.find('testcase'), status); suite.set(count, '1')
                self.save(suite); self.assertFalse(self.check(mode))
            suite = ET.fromstring(original); suite.find('testcase').set('name', 'unapprovedAuthorityCase')
            self.save(suite); self.assertFalse(self.check(mode))

    def test_authority_private_failure_diagnostic_is_not_published(self):
        self.fixture('h2'); path = self.source / ('TEST-' + reports.IDENTITY_AUTHORITY_UNIT + '.xml')
        suite = ET.parse(path).getroot(); marker = 'synthetic-private-authority-failure'
        ET.SubElement(suite.find('testcase'), 'failure', message=marker).text = marker
        ET.SubElement(suite, 'system-out').text = marker; suite.set('failures', '1'); self.save(suite)
        self.assertFalse(self.check('h2'))
        self.assertNotIn(marker, ''.join(p.read_text(encoding='utf-8') for p in self.output.iterdir()))

    def test_empty_target_denial_cases_are_mandatory_and_cannot_skip(self):
        cases = [
            (reports.IDENTITY_AUTHORITY_UNIT, 'emptyTargetIsDeniedBeforeAnAllowingDelegate', ('h2', 'postgres')),
            (reports.IDENTITY_GRANTS, 'productionEmptyTargetIsDeniedBeforeAnAllowingDelegate', ('postgres',)),
        ]
        for name, method, modes in cases:
            self.assertIn(method, reports.REQUIRED[name])
            for mode in modes:
                self.fixture(mode)
                path = self.source / ('TEST-' + name + '.xml'); original = path.read_bytes()
                suite = ET.fromstring(original)
                case = next(c for c in suite if c.get('name') == method)
                suite.remove(case); suite.set('tests', str(len(suite.findall('testcase')))); self.save(suite)
                self.assertFalse(self.check(mode), method)
                suite = ET.fromstring(original)
                ET.SubElement(next(c for c in suite if c.get('name') == method), 'skipped')
                suite.set('skipped', '1'); self.save(suite)
                self.assertFalse(self.check(mode), method)
                path.write_bytes(original); self.assertTrue(self.check(mode))


class IdentityRealmInventoryTests(unittest.TestCase):
    setUp = ReportGuardTests.setUp
    fixture = ReportGuardTests.fixture
    save = ReportGuardTests.save
    check = ReportGuardTests.check

    def test_realm_source_identities_and_modes_are_exact(self):
        root = Path(__file__).resolve().parents[2]
        source = (root / 'app/backend/src/test/java/com/elogistic/backend/identity/internal/application/IdentityRealmGrantPolicyTests.java').read_text(encoding='utf-8')
        methods = re.findall(r'@Test\s+void\s+(\w+)\s*\(', source)
        self.assertEqual(8, len(methods)); self.assertEqual(methods, reports.REQUIRED[reports.IDENTITY_REALM_UNIT])
        self.assertEqual(7, len(reports.IDENTITY_REALM_PG_REQUIRED))
        self.assertNotIn('@Disabled', source)
        for mode in ('h2', 'postgres'):
            self.assertIn(reports.IDENTITY_REALM_UNIT, reports.expected_suites(mode))
        self.assertNotIn(reports.IDENTITY_REALM_UNIT, reports.expected_suites('architecture'))

    def test_realm_missing_skipped_failed_and_private_reports_are_rejected(self):
        for mode in ('h2', 'postgres'):
            self.fixture(mode)
            path = self.source / ('TEST-' + reports.IDENTITY_REALM_UNIT + '.xml'); original = path.read_bytes()
            path.unlink(); self.assertFalse(self.check(mode))
            for method in reports.REQUIRED[reports.IDENTITY_REALM_UNIT]:
                for status in ('missing', 'skipped', 'failure', 'error'):
                    suite = ET.fromstring(original); case = next(c for c in suite if c.get('name') == method)
                    if status == 'missing':
                        suite.remove(case); suite.set('tests', str(len(suite.findall('testcase'))))
                    else:
                        ET.SubElement(case, status, message='synthetic-private-realm-diagnostic')
                        suite.set({'skipped':'skipped', 'failure':'failures', 'error':'errors'}[status], '1')
                    self.save(suite); self.assertFalse(self.check(mode))
                    self.assertNotIn('synthetic-private-realm-diagnostic', ''.join(p.read_text(encoding='utf-8') for p in self.output.iterdir()))
            path.write_bytes(original); self.assertTrue(self.check(mode))


PROVISION_UNIT_METHODS = (
    'exactUsernameAndContactAreNotNormalized',
    'displayNameCountsUnicodeCodePointsAndRejectsMalformedText',
    'missingKeysOrRequiredFieldsAreRejectedWithoutEcho',
    'grantSetRejectsEmptyDuplicateAndMalformedMembers',
    'emailMatchesAcceptedFullFormatRatherThanTrimming',
    'fingerprintBindsEveryPayloadFieldAndIgnoresGrantOrder',
    'commandAndValidatedInputAreImmutableAndRedacted',
    'explicitInitialPairBindsActorAndNewPendingTarget',
    'unsupportedOrCrossRealmInitialGrantsCannotDelegate',
    'initialAuthorityRequiresActiveActorRealmAndFreshUnverifiedTarget',
    'provisioningDoesNotRelaxReplacementEmptyTargetGuard',
    'domainProvisioningRemainsPendingUnverifiedAndVersionOne',
)
PROVISION_PG_METHODS = (
    'provisionPersistsPendingInitialPairProofAuditReceiptAndTestIntent',
    'provisionExactReplayDoesNotRepeatAccountProofAuditOrDelivery',
    'provisionConflictingPayloadKeyDoesNotDiscloseOrRepeatEffects',
    'provisionDuplicateUsernameCannotTakeOverExistingAccount',
    'provisionUnauthenticatedAndUnknownSessionsMakeNoWrites',
    'provisionRevokedSessionCannotUseCurrentAdministrativeGrant',
    'provisionExpiredSessionFailsBeforeContactLookup',
    'provisionDisabledActorAndStaleAuthVersionAreRejected',
    'provisionInactiveUnknownAndCrossRealmAuthorityFailClosed',
    'provisionExternalMixedAndOrdinaryRoleRequestsNeverAssignGrants',
    'provisionMissingDurableInvitationAdapterFailsBeforeWrites',
    'provisionInvalidPreparedProofClosesMaterialWithoutPartialAccount',
    'provisionGrantProofDeliveryAuditAndReceiptFaultsRollBackAtomically',
    'provisionConcurrentSameKeyHasOneAccountProofAndTestIntent',
    'provisionConcurrentUsernameConflictHasNoLoserEffects',
    'provisionReplayRechecksStoredTargetGrantUnionAndCurrentSession',
    'provisionCannotActivateRealmAccountOrIssueSessions',
    'provisionWaitsForConcurrentActorAndRealmStateBeforeAdmission',
)


class IdentityProvisionInventoryTests(unittest.TestCase):
    setUp = ReportGuardTests.setUp
    fixture = ReportGuardTests.fixture
    save = ReportGuardTests.save
    check = ReportGuardTests.check

    def test_provision_source_inventory_and_execution_modes_are_exact(self):
        root = Path(__file__).resolve().parents[2]
        unit = (root / 'app/backend/src/test/java/com/elogistic/backend/identity/internal/application/IdentityProvisionInputTests.java').read_text(encoding='utf-8')
        pg = (root / 'app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityGrantApplicationTests.java').read_text(encoding='utf-8')
        self.assertEqual(12, len(PROVISION_UNIT_METHODS)); self.assertEqual(18, len(PROVISION_PG_METHODS))
        self.assertEqual(list(PROVISION_UNIT_METHODS), re.findall(r'@Test\s+void\s+(\w+)\s*\(', unit))
        self.assertEqual(list(PROVISION_UNIT_METHODS), reports.REQUIRED[reports.IDENTITY_PROVISION_UNIT])
        self.assertEqual(list(PROVISION_PG_METHODS), reports.IDENTITY_PROVISION_PG_REQUIRED)
        self.assertEqual(list(PROVISION_PG_METHODS), re.findall(r'@Test\s+void\s+(provision\w+)\s*\(', pg))
        for mode in ('h2', 'postgres'):
            self.assertIn(reports.IDENTITY_PROVISION_UNIT, reports.expected_suites(mode))
        self.assertNotIn(reports.IDENTITY_PROVISION_UNIT, reports.expected_suites('architecture'))
        self.assertNotIn(reports.IDENTITY_GRANTS, reports.expected_suites('h2'))
        self.assertEqual(3, len(reports.TASK_C_SUITES))
        self.assertNotIn('@Disabled', unit); self.assertNotIn('@Disabled', pg)

    def test_provision_missing_failed_skipped_and_renamed_cases_fail_closed(self):
        for name, methods, modes in ((reports.IDENTITY_PROVISION_UNIT, PROVISION_UNIT_METHODS, ('h2', 'postgres')),
                                     (reports.IDENTITY_GRANTS, PROVISION_PG_METHODS, ('postgres',))):
            for mode in modes:
                self.fixture(mode); path = self.source / ('TEST-' + name + '.xml'); original = path.read_bytes()
                path.unlink(); self.assertFalse(self.check(mode)); path.write_bytes(original)
                for method in methods:
                    for status in ('missing', 'skipped', 'failure', 'error', 'renamed'):
                        with self.subTest(mode=mode, method=method, status=status):
                            suite = ET.fromstring(original); case = next(c for c in suite if c.get('name') == method)
                            if status == 'missing':
                                suite.remove(case); suite.set('tests', str(len(suite.findall('testcase'))))
                            elif status == 'renamed':
                                case.set('name', 'unapprovedProvisionCase')
                            else:
                                ET.SubElement(case, status); suite.set({'skipped':'skipped', 'failure':'failures', 'error':'errors'}[status], '1')
                            self.save(suite); self.assertFalse(self.check(mode))
                path.write_bytes(original); self.assertTrue(self.check(mode))

    def test_provision_private_failure_details_never_reach_public_reports(self):
        self.fixture('postgres'); path = self.source / ('TEST-' + reports.IDENTITY_GRANTS + '.xml')
        suite = ET.parse(path).getroot(); case = next(c for c in suite if c.get('name') == PROVISION_PG_METHODS[0])
        marker = 'synthetic-private-provision-invitation-diagnostic'
        ET.SubElement(case, 'failure', message=marker).text = marker
        ET.SubElement(suite, 'system-out').text = marker; suite.set('failures', '1'); self.save(suite)
        self.assertFalse(self.check('postgres'))
        self.assertNotIn(marker, ''.join(p.read_text(encoding='utf-8') for p in self.output.iterdir()))


INVITATION_UNIT_METHODS = (
    'freshTokensHaveFullUrlSafeEncodingAndDistinctProtectedValues',
    'verifierAcceptsOnlyExactTokenAndBoundProofAccountPurposeAndTimes',
    'exactExpiryAndConsumedRevokedOrFutureProofFailClosed',
    'missingMalformedAndSharedPurposeKeysAreRejectedWithoutValues',
    'keysAreDefensivelyCopiedAndOldKeysSupportRotation',
    'aeadRejectsCiphertextNonceAndContextTampering',
    'malformedNoncanonicalOrUnknownVerifierNeverAuthenticates',
    'protectedBuffersAreDefensiveRedactedAndDestroyedOnClose',
    'persistedMicrosecondPrecisionIsMandatoryForAuthenticatedContext',
    'deliveryPolicyRequiresExplicitBoundedAttemptsAndBudgets',
)
INVITATION_PG_METHODS = (
    'secureProvisionQueuesEncryptedIntentWithMatchingHmac',
    'secureProvisionReplayAndConflictingKeyNeverRegenerateOutbox',
    'secureProvisionEveryMandatoryPersistenceFailureRollsBack',
    'secureProvisionConcurrentKeyAndUsernameHaveOneDurableEffect',
    'secureOutboxClaimLeaseRecoveryRejectsStaleOwner',
    'secureOutboxConcurrentClaimsHaveExactlyOneLease',
    'secureOutboxSentClearsPayloadWithoutConsumingOrActivating',
    'secureOutboxTransientRetriesAreBoundedAndExhaustionPurges',
    'secureOutboxPermanentFailurePurgesWithoutBusinessMutation',
    'secureOutboxExpiryRevocationAndConsumptionPurgeOnlyPayloads',
    'secureOutboxCurrentRealmAndGrantStateGateDelivery',
    'secureOutboxTamperingCannotProducePlaintextDelivery',
    'secureOutboxRotationReadsPersistedOldKeyIdentity',
    'secureOutboxMissingKeysOrRuntimeInsertPrivilegeCannotCommit',
    'secureOutboxRejectsIntentOutsideTheProvisionTransaction',
)


class IdentityInvitationInventoryTests(unittest.TestCase):
    setUp = ReportGuardTests.setUp
    fixture = ReportGuardTests.fixture
    save = ReportGuardTests.save
    check = ReportGuardTests.check

    def test_invitation_crypto_and_postgres_exact_reviewed_inventory(self):
        root = Path(__file__).resolve().parents[2] / 'app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure'
        unit = (root / 'InvitationProtectionTests.java').read_text(encoding='utf-8')
        pg = (root / 'IdentityGrantApplicationTests.java').read_text(encoding='utf-8')
        self.assertEqual(10, len(INVITATION_UNIT_METHODS)); self.assertEqual(15, len(INVITATION_PG_METHODS))
        self.assertEqual(list(INVITATION_UNIT_METHODS), re.findall(r'@Test\s+void\s+(\w+)\s*\(', unit))
        self.assertEqual(list(INVITATION_UNIT_METHODS), reports.REQUIRED[reports.IDENTITY_INVITATION_UNIT])
        self.assertEqual(list(INVITATION_PG_METHODS), re.findall(r'@Test\s+void\s+(secure\w+)\s*\(', pg))
        self.assertEqual(list(INVITATION_PG_METHODS), reports.IDENTITY_INVITATION_PG_REQUIRED)
        for mode in ('h2', 'postgres'):
            self.assertIn(reports.IDENTITY_INVITATION_UNIT, reports.expected_suites(mode))
        self.assertNotIn(reports.IDENTITY_INVITATION_UNIT, reports.expected_suites('architecture'))
        self.assertNotIn(reports.IDENTITY_GRANTS, reports.expected_suites('h2'))
        self.assertEqual(3, len(reports.TASK_C_SUITES))
        self.assertNotIn('@Disabled', unit); self.assertNotIn('@Disabled', pg)

    def test_invitation_missing_renamed_skipped_or_failed_methods_are_rejected(self):
        for name, methods, modes in ((reports.IDENTITY_INVITATION_UNIT, INVITATION_UNIT_METHODS, ('h2', 'postgres')),
                                     (reports.IDENTITY_GRANTS, INVITATION_PG_METHODS, ('postgres',))):
            for mode in modes:
                self.fixture(mode); path = self.source / ('TEST-' + name + '.xml'); original = path.read_bytes()
                path.unlink(); self.assertFalse(self.check(mode)); path.write_bytes(original)
                for method in methods:
                    for status in ('missing', 'renamed', 'skipped', 'failure', 'error'):
                        with self.subTest(mode=mode, method=method, status=status):
                            suite = ET.fromstring(original); case = next(c for c in suite if c.get('name') == method)
                            if status == 'missing':
                                suite.remove(case); suite.set('tests', str(len(suite.findall('testcase'))))
                            elif status == 'renamed':
                                case.set('name', 'unapprovedInvitationCase')
                            else:
                                ET.SubElement(case, status); suite.set({'skipped':'skipped', 'failure':'failures', 'error':'errors'}[status], '1')
                            self.save(suite); self.assertFalse(self.check(mode))
                path.write_bytes(original); self.assertTrue(self.check(mode))

    def test_invitation_diagnostics_properties_and_private_markers_are_never_published(self):
        self.fixture('postgres'); path = self.source / ('TEST-' + reports.IDENTITY_GRANTS + '.xml')
        suite = ET.parse(path).getroot(); case = next(c for c in suite if c.get('name') == INVITATION_PG_METHODS[0])
        marker = 'synthetic-private-invitation-secret-marker'
        ET.SubElement(case, 'failure', message=marker).text = marker
        ET.SubElement(suite, 'system-out').text = marker; ET.SubElement(suite, 'properties', token=marker)
        suite.set('failures', '1'); self.save(suite); self.assertFalse(self.check('postgres'))
        self.assertNotIn(marker, ''.join(p.read_text(encoding='utf-8') for p in self.output.iterdir()))




ACTIVATION_UNIT_METHODS = ('validActivationInvokesAllWritesInsideTheTransaction', 'missingCommandIdentifiersFailBeforeTheStore', 'invalidNewPasswordsNeverReachPersistence', 'passwordUnicodeAndUtf8BoundsReuseTheDomainPolicy', 'invalidTokenCannotReachReceiptOrPasswordHashing', 'unavailableRealmCannotReachPasswordHashing', 'consumedProofWithoutExactReceiptCannotActivate', 'exactConsumedProofReceiptReplayHasNoWrites', 'changedReceiptFingerprintConflictsBeforeAnyWrite', 'replayRequiresCurrentActiveAccountAndRetainedVersion', 'receiptRetentionCannotBeShortenedBelowTwentyFourHours', 'commandBuffersAreCopiedRedactedAndClosed')
ACTIVATION_PG_METHODS = ('activationCommitsVerifierProofVersionsAuditReceiptWithoutSession', 'activationExactReceiptReplayHasNoSecondEffect', 'activationChangedPasswordConflictsOnTheBoundReceipt', 'activationWrongTokenAndUnknownLocatorCannotAuthenticate', 'activationExactExpiryAndFutureProofFailClosed', 'activationConsumedAndRevokedProofCannotBeUsedAgain', 'activationWrongPurposeAndAccountBindingAreRejected', 'activationDisabledPendingAccountIsNotReactivated', 'activationInactiveRealmAndRemovedGrantDeny', 'activationPasswordBoundsPrecedeEveryPersistentWrite', 'activationPasswordWriteFailureRollsBackAllEffects', 'activationAuditFailureRollsBackAllEffects', 'activationReceiptFailureRollsBackAllEffects', 'activationProofConsumptionFailureRollsBackPassword', 'activationPayloadClearFailureRollsBackConsumption', 'activationConcurrentDifferentKeysConsumeExactlyOnce', 'activationConcurrentSameKeyHasOneDurableEffect', 'activationRetiredKeyRotationSupportsReplayAndMissingKeyDenies', 'activationOptimisticVersionMismatchRollsBackTheTransaction', 'activationRedactsSecretsRevokesOldSessionsAndDeniesStaleDelivery')


class IdentityActivationInventoryTests(unittest.TestCase):
    setUp = ReportGuardTests.setUp
    fixture = ReportGuardTests.fixture
    save = ReportGuardTests.save
    check = ReportGuardTests.check

    def test_activation_source_and_finite_execution_inventory_are_exact(self):
        root = Path(__file__).resolve().parents[2] / 'app/backend/src/test/java/com/elogistic/backend/identity/internal'
        unit = (root / 'application/IdentityInvitationActivationTests.java').read_text(encoding='utf-8')
        pg = (root / 'infrastructure/IdentityGrantApplicationTests.java').read_text(encoding='utf-8')
        self.assertEqual(list(ACTIVATION_UNIT_METHODS), re.findall(r'@Test\s+void\s+(\w+)\s*\(', unit))
        self.assertEqual(list(ACTIVATION_PG_METHODS), re.findall(r'@Test\s+void\s+(activation\w+)\s*\(', pg))
        self.assertEqual(list(ACTIVATION_UNIT_METHODS), reports.REQUIRED[reports.IDENTITY_ACTIVATION_UNIT])
        self.assertEqual(list(ACTIVATION_PG_METHODS), reports.IDENTITY_ACTIVATION_PG_REQUIRED)
        self.assertEqual(12, len(ACTIVATION_UNIT_METHODS)); self.assertEqual(20, len(ACTIVATION_PG_METHODS))
        self.assertEqual(282, len(reports.REQUIRED[reports.IDENTITY_GRANTS]))
        self.assertEqual(3, len(reports.TASK_C_SUITES))
        self.assertNotIn(reports.IDENTITY_GRANTS, reports.expected_suites('h2'))
        for mode in ('h2', 'postgres'): self.assertIn(reports.IDENTITY_ACTIVATION_UNIT, reports.expected_suites(mode))
        self.assertNotIn(reports.IDENTITY_ACTIVATION_UNIT, reports.expected_suites('architecture'))
        self.assertNotIn('@Disabled', unit); self.assertNotIn('@Disabled', pg)

    def test_each_activation_missing_renamed_skipped_failed_or_error_case_is_rejected(self):
        for classname, methods, modes in ((reports.IDENTITY_ACTIVATION_UNIT, ACTIVATION_UNIT_METHODS, ('h2', 'postgres')),
                                          (reports.IDENTITY_GRANTS, ACTIVATION_PG_METHODS, ('postgres',))):
            for mode in modes:
                self.fixture(mode); path = self.source / ('TEST-' + classname + '.xml'); original = path.read_bytes()
                path.unlink(); self.assertFalse(self.check(mode)); path.write_bytes(original)
                for method in methods:
                    for status in ('missing', 'renamed', 'skipped', 'failure', 'error'):
                        with self.subTest(mode=mode, method=method, status=status):
                            suite = ET.fromstring(original); case = next(c for c in suite if c.get('name') == method)
                            if status == 'missing':
                                suite.remove(case); suite.set('tests', str(len(suite.findall('testcase'))))
                            elif status == 'renamed': case.set('name', 'unapprovedActivationCase')
                            else:
                                ET.SubElement(case, status); suite.set({'skipped':'skipped', 'failure':'failures', 'error':'errors'}[status], '1')
                            self.save(suite); self.assertFalse(self.check(mode))
                path.write_bytes(original); self.assertTrue(self.check(mode))

    def test_activation_secret_diagnostics_never_enter_public_reports(self):
        self.fixture('postgres'); path = self.source / ('TEST-' + reports.IDENTITY_GRANTS + '.xml')
        suite = ET.parse(path).getroot(); case = next(c for c in suite if c.get('name') == ACTIVATION_PG_METHODS[0])
        marker = 'synthetic-private-activation-proof-password-marker'
        ET.SubElement(case, 'failure', message=marker).text = marker
        ET.SubElement(suite, 'system-out').text = marker; suite.set('failures', '1'); self.save(suite)
        self.assertFalse(self.check('postgres'))
        self.assertNotIn(marker, ''.join(p.read_text(encoding='utf-8') for p in self.output.iterdir()))



class IdentitySessionInventoryTests(unittest.TestCase):
    setUp = ReportGuardTests.setUp
    fixture = ReportGuardTests.fixture
    save = ReportGuardTests.save
    check = ReportGuardTests.check

    def test_session_source_and_execution_inventory_are_exact(self):
        root = Path(__file__).resolve().parents[2] / 'app/backend/src/test/java/com/elogistic/backend/identity/internal'
        unit = (root / 'application/IdentitySessionApplicationTests.java').read_text(encoding='utf-8')
        pg = (root / 'infrastructure/IdentityGrantApplicationTests.java').read_text(encoding='utf-8')
        self.assertEqual(re.findall(r'@Test\s+void\s+(\w+)\s*\(', unit), reports.IDENTITY_SESSION_UNIT_REQUIRED)
        self.assertEqual(re.findall(r'@Test\s+void\s+(session\w+)\s*\(', pg), reports.IDENTITY_SESSION_PG_REQUIRED)
        self.assertEqual(24, len(reports.IDENTITY_SESSION_UNIT_REQUIRED))
        self.assertEqual(23, len(reports.IDENTITY_SESSION_PG_REQUIRED))
        self.assertEqual(282, len(reports.REQUIRED[reports.IDENTITY_GRANTS]))
        self.assertEqual(3, len(reports.TASK_C_SUITES))
        for mode in ('h2', 'postgres'): self.assertIn(reports.IDENTITY_SESSION_UNIT, reports.expected_suites(mode))
        self.assertNotIn(reports.IDENTITY_SESSION_UNIT, reports.expected_suites('architecture'))
        self.assertNotIn('@Disabled', unit)

    def test_each_session_identity_is_mandatory_and_cannot_skip_or_hide_failure(self):
        for cls, methods, modes in ((reports.IDENTITY_SESSION_UNIT, reports.IDENTITY_SESSION_UNIT_REQUIRED, ('h2', 'postgres')),
                                    (reports.IDENTITY_GRANTS, reports.IDENTITY_SESSION_PG_REQUIRED, ('postgres',))):
            for mode in modes:
                self.fixture(mode)
                path = self.source / ('TEST-' + cls + '.xml')
                original = path.read_bytes()
                for method in methods:
                    for status in ('missing', 'renamed', 'skipped', 'failure', 'error'):
                        with self.subTest(mode=mode, method=method, status=status):
                            suite = ET.fromstring(original)
                            case = next(c for c in suite if c.get('name') == method)
                            if status == 'missing':
                                suite.remove(case); suite.set('tests', str(len(suite.findall('testcase'))))
                            elif status == 'renamed': case.set('name', 'unapprovedSessionCase')
                            else:
                                ET.SubElement(case, status)
                                suite.set({'skipped':'skipped', 'failure':'failures', 'error':'errors'}[status], '1')
                            self.save(suite); self.assertFalse(self.check(mode))
                path.write_bytes(original); self.assertTrue(self.check(mode))

    def test_session_secret_diagnostics_are_not_published(self):
        self.fixture('postgres')
        path = self.source / ('TEST-' + reports.IDENTITY_GRANTS + '.xml')
        suite = ET.parse(path).getroot()
        case = next(c for c in suite if c.get('name') == reports.IDENTITY_SESSION_PG_REQUIRED[0])
        marker = 'synthetic-private-session-password-refresh-marker'
        ET.SubElement(case, 'failure', message=marker).text = marker
        ET.SubElement(suite, 'system-out').text = marker
        suite.set('failures', '1'); self.save(suite)
        self.assertFalse(self.check('postgres'))
        self.assertNotIn(marker, ''.join(p.read_text(encoding='utf-8') for p in self.output.iterdir()))



class IdentityCurrentSessionInventoryTests(unittest.TestCase):
    setUp = ReportGuardTests.setUp
    fixture = ReportGuardTests.fixture
    save = ReportGuardTests.save
    check = ReportGuardTests.check

    def test_current_session_source_and_execution_inventory_are_exact(self):
        root = Path(__file__).resolve().parents[2] / 'app/backend/src/test/java/com/elogistic/backend/identity/internal'
        unit = (root / 'application/IdentityCurrentSessionApplicationTests.java').read_text(encoding='utf-8')
        pg = (root / 'infrastructure/IdentityGrantApplicationTests.java').read_text(encoding='utf-8')
        self.assertEqual(re.findall(r'@Test\s+void\s+(\w+)\s*\(', unit), reports.IDENTITY_CURRENT_SESSION_UNIT_REQUIRED)
        self.assertEqual(re.findall(r'@Test\s+void\s+(currentSession\w+)\s*\(', pg), reports.IDENTITY_CURRENT_SESSION_PG_REQUIRED)
        self.assertEqual(21, len(reports.IDENTITY_CURRENT_SESSION_UNIT_REQUIRED))
        self.assertEqual(14, len(reports.IDENTITY_CURRENT_SESSION_PG_REQUIRED))
        self.assertEqual(282, len(reports.REQUIRED[reports.IDENTITY_GRANTS]))
        self.assertEqual(3, len(reports.TASK_C_SUITES))
        self.assertNotIn('@Disabled', unit)
        for mode in ('h2', 'postgres'): self.assertIn(reports.IDENTITY_CURRENT_SESSION_UNIT, reports.expected_suites(mode))
        self.assertNotIn(reports.IDENTITY_CURRENT_SESSION_UNIT, reports.expected_suites('architecture'))

    def test_each_current_session_case_cannot_be_missing_skipped_or_failed(self):
        for cls, methods, modes in ((reports.IDENTITY_CURRENT_SESSION_UNIT, reports.IDENTITY_CURRENT_SESSION_UNIT_REQUIRED, ('h2', 'postgres')),
                                   (reports.IDENTITY_GRANTS, reports.IDENTITY_CURRENT_SESSION_PG_REQUIRED, ('postgres',))):
            for mode in modes:
                self.fixture(mode); path = self.source / ('TEST-' + cls + '.xml'); original = path.read_bytes()
                for method in methods:
                    for status in ('missing', 'renamed', 'skipped', 'failure', 'error'):
                        with self.subTest(mode=mode, method=method, status=status):
                            suite = ET.fromstring(original); case = next(c for c in suite if c.get('name') == method)
                            if status == 'missing':
                                suite.remove(case); suite.set('tests', str(len(suite.findall('testcase'))))
                            elif status == 'renamed': case.set('name', 'unapprovedCurrentSessionCase')
                            else:
                                ET.SubElement(case, status); suite.set({'skipped':'skipped', 'failure':'failures', 'error':'errors'}[status], '1')
                            self.save(suite); self.assertFalse(self.check(mode))
                path.write_bytes(original); self.assertTrue(self.check(mode))

    def test_current_session_private_diagnostics_are_not_published(self):
        self.fixture('postgres'); path = self.source / ('TEST-' + reports.IDENTITY_GRANTS + '.xml')
        suite = ET.parse(path).getroot(); case = next(c for c in suite if c.get('name') == reports.IDENTITY_CURRENT_SESSION_PG_REQUIRED[0])
        marker = 'synthetic-private-current-session-access-credential-marker'
        ET.SubElement(case, 'failure', message=marker).text = marker; ET.SubElement(suite, 'system-out').text = marker
        suite.set('failures', '1'); self.save(suite); self.assertFalse(self.check('postgres'))
        self.assertNotIn(marker, ''.join(p.read_text(encoding='utf-8') for p in self.output.iterdir()))



class IdentityAdministrationInventoryTests(unittest.TestCase):
    setUp = ReportGuardTests.setUp
    fixture = ReportGuardTests.fixture
    save = ReportGuardTests.save
    check = ReportGuardTests.check

    def test_administration_inventory_and_unregistered_boundary_are_exact(self):
        root = Path(__file__).resolve().parents[2]
        source = root / 'app/backend/src/test/java/com/elogistic/backend/identity/internal'
        unit = (source / 'application/IdentityAdministrationTests.java').read_text(encoding='utf-8')
        pg = (source / 'infrastructure/IdentityGrantApplicationTests.java').read_text(encoding='utf-8')
        self.assertEqual(re.findall(r'@Test\s+void\s+(\w+)\s*\(', unit), reports.IDENTITY_ADMIN_UNIT_REQUIRED)
        self.assertEqual(re.findall(r'@Test\s+void\s+(admin\w+)\s*\(', pg), reports.IDENTITY_ADMIN_PG_REQUIRED)
        self.assertEqual(36, len(reports.IDENTITY_ADMIN_UNIT_REQUIRED))
        self.assertEqual(34, len(reports.IDENTITY_ADMIN_PG_REQUIRED))
        self.assertEqual(282, len(reports.REQUIRED[reports.IDENTITY_GRANTS]))
        self.assertEqual(3, len(reports.TASK_C_SUITES))
        composition = (root / 'app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/ProductionIdentityAdministrationComposition.java').read_text(encoding='utf-8')
        self.assertNotRegex(composition, r'@(Bean|Component|Configuration|Controller|RestController)\b')
        self.assertNotIn('@Disabled', unit)

    def test_each_administration_case_must_execute_with_exact_identity(self):
        for cls, names, modes in ((reports.IDENTITY_ADMIN_UNIT, reports.IDENTITY_ADMIN_UNIT_REQUIRED, ('h2', 'postgres')),
                                  (reports.IDENTITY_GRANTS, reports.IDENTITY_ADMIN_PG_REQUIRED, ('postgres',))):
            for mode in modes:
                self.fixture(mode); path = self.source / ('TEST-' + cls + '.xml'); original = path.read_bytes()
                for name in names:
                    for status in ('missing', 'renamed', 'skipped', 'failure', 'error'):
                        with self.subTest(mode=mode, name=name, status=status):
                            suite = ET.fromstring(original); case = next(c for c in suite if c.get('name') == name)
                            if status == 'missing': suite.remove(case); suite.set('tests', str(len(suite.findall('testcase'))))
                            elif status == 'renamed': case.set('name', 'unapprovedAdministrationCase')
                            else:
                                ET.SubElement(case, status)
                                suite.set({'skipped':'skipped', 'failure':'failures', 'error':'errors'}[status], '1')
                            self.save(suite); self.assertFalse(self.check(mode))
                path.write_bytes(original); self.assertTrue(self.check(mode))

    def test_administration_diagnostics_are_not_public_artifacts(self):
        self.fixture('postgres'); path = self.source / ('TEST-' + reports.IDENTITY_GRANTS + '.xml')
        suite = ET.parse(path).getroot(); case = next(c for c in suite if c.get('name') == reports.IDENTITY_ADMIN_PG_REQUIRED[0])
        marker = 'synthetic-private-administration-secret-marker'
        ET.SubElement(case, 'failure', message=marker).text = marker
        ET.SubElement(suite, 'system-out').text = marker; suite.set('failures', '1')
        self.save(suite); self.assertFalse(self.check('postgres'))
        self.assertNotIn(marker, ''.join(p.read_text(encoding='utf-8') for p in self.output.iterdir()))


class ProductionIdentityHttpInventoryTests(unittest.TestCase):
    setUp = ReportGuardTests.setUp
    fixture = ReportGuardTests.fixture
    save = ReportGuardTests.save
    check = ReportGuardTests.check

    def test_http_source_and_finite_inventory_are_exact(self):
        root = Path(__file__).resolve().parents[2]
        unit = (root/'app/backend/src/test/java/com/elogistic/backend/identity/internal/web/ProductionIdentitySecurityTests.java').read_text(encoding='utf-8')
        pg = (root/'app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityGrantApplicationTests.java').read_text(encoding='utf-8')
        self.assertEqual(re.findall(r'@Test\s+void\s+(\w+)\s*\(',unit),reports.IDENTITY_HTTP_UNIT_REQUIRED)
        self.assertEqual(re.findall(r'@Test\s+void\s+(httpRead\w+)\s*\(',pg),reports.IDENTITY_HTTP_PG_REQUIRED)
        self.assertEqual(43,len(reports.IDENTITY_HTTP_UNIT_REQUIRED));self.assertEqual(18,len(reports.IDENTITY_HTTP_PG_REQUIRED))
        self.assertEqual(282,len(reports.REQUIRED[reports.IDENTITY_GRANTS]));self.assertEqual(3,len(reports.TASK_C_SUITES))
        self.assertNotIn('@Disabled',unit)

    def test_each_http_security_case_must_execute_and_cannot_be_skipped(self):
        for cls,names,modes in ((reports.IDENTITY_HTTP_UNIT,reports.IDENTITY_HTTP_UNIT_REQUIRED,('h2','postgres')),
                                (reports.IDENTITY_GRANTS,reports.IDENTITY_HTTP_PG_REQUIRED,('postgres',))):
            for mode in modes:
                self.fixture(mode);path=self.source/('TEST-'+cls+'.xml');original=path.read_bytes()
                for name in names:
                    for outcome in ('missing','renamed','skipped','failure','error'):
                        with self.subTest(mode=mode,name=name,outcome=outcome):
                            suite=ET.fromstring(original);case=next(c for c in suite if c.get('name')==name)
                            if outcome=='missing':suite.remove(case);suite.set('tests',str(len(suite.findall('testcase'))))
                            elif outcome=='renamed':case.set('name','unapprovedHttpCase')
                            else:ET.SubElement(case,outcome);suite.set({'skipped':'skipped','failure':'failures','error':'errors'}[outcome],'1')
                            self.save(suite);self.assertFalse(self.check(mode))
                path.write_bytes(original);self.assertTrue(self.check(mode))

    def test_http_private_diagnostics_are_excluded_from_sanitized_artifacts(self):
        self.fixture('postgres');path=self.source/('TEST-'+reports.IDENTITY_HTTP_UNIT+'.xml');suite=ET.parse(path).getroot()
        marker='synthetic-private-http-security-marker';ET.SubElement(suite.find('testcase'),'failure',message=marker).text=marker
        ET.SubElement(suite,'system-out').text=marker;suite.set('failures','1');self.save(suite);self.assertFalse(self.check('postgres'))
        self.assertNotIn(marker,''.join(p.read_text(encoding='utf-8') for p in self.output.iterdir()))


class AuthenticationHttpReportTests(unittest.TestCase):
    setUp=ReportGuardTests.setUp
    fixture=ReportGuardTests.fixture
    save=ReportGuardTests.save
    check=ReportGuardTests.check
    def test_authentication_finite_inventory_matches_reviewed_source(self):
        root=Path(__file__).resolve().parents[2]
        unit=(root/'app/backend/src/test/java/com/elogistic/backend/identity/internal/web/ProductionAuthenticationHttpTests.java').read_text(encoding='utf-8')
        pg=(root/'app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityGrantApplicationTests.java').read_text(encoding='utf-8')
        self.assertEqual(re.findall(r'@Test\s+void\s+(\w+)\s*\(',unit),reports.IDENTITY_AUTH_UNIT_REQUIRED)
        self.assertEqual(re.findall(r'@Test\s+void\s+(auth(?:Rate|Browser|Http)\w+)\s*\(',pg),reports.IDENTITY_AUTH_PG_REQUIRED)
    def test_every_authentication_method_is_required_and_must_execute(self):
        for mode,cls,names in [('h2',reports.IDENTITY_AUTH_UNIT,reports.IDENTITY_AUTH_UNIT_REQUIRED),('postgres',reports.IDENTITY_AUTH_UNIT,reports.IDENTITY_AUTH_UNIT_REQUIRED),('postgres',reports.IDENTITY_GRANTS,reports.IDENTITY_AUTH_PG_REQUIRED)]:
            self.fixture(mode);path=self.source/('TEST-'+cls+'.xml');original=path.read_bytes()
            for name in names:
                for outcome in ('missing','renamed','skipped','failure','error'):
                    with self.subTest(mode=mode,name=name,outcome=outcome):
                        suite=ET.fromstring(original);case=next(c for c in suite if c.get('name')==name)
                        if outcome=='missing':suite.remove(case);suite.set('tests',str(len(suite.findall('testcase'))))
                        elif outcome=='renamed':case.set('name','unapprovedAuthenticationCase')
                        else:ET.SubElement(case,outcome);suite.set({'skipped':'skipped','failure':'failures','error':'errors'}[outcome],'1')
                        self.save(suite);self.assertFalse(self.check(mode))
            path.write_bytes(original);self.assertTrue(self.check(mode))
    def test_authentication_diagnostics_never_enter_public_artifacts(self):
        self.fixture('postgres');suite=ET.parse(self.source/('TEST-'+reports.IDENTITY_AUTH_UNIT+'.xml')).getroot()
        marker='synthetic-private-authentication-marker';ET.SubElement(suite.find('testcase'),'error',message=marker).text=marker
        ET.SubElement(suite,'system-out').text=marker;suite.set('errors','1');self.save(suite);self.assertFalse(self.check('postgres'))
        self.assertNotIn(marker,''.join(p.read_text(encoding='utf-8') for p in self.output.iterdir()))

if __name__ == '__main__':
    unittest.main()
