"""Exercise the CI guard's fail-closed behavior and sanitized artifact boundary."""
import contextlib
import importlib.util
import io
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location('reports', Path(__file__).with_name('verify-reports.py'))
reports = importlib.util.module_from_spec(spec)
spec.loader.exec_module(reports)


class ReportGuardTests(unittest.TestCase):
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
        path = self.source / 'runtime.txt'
        path.write_text('anonymousVolume=' + 'c' * 64 + '\n'
                        + 'image=approved id=' + 'a' * 64 + ' privileged=false ports=loopback\n'
                        + 'image=approved id=' + 'b' * 64 + ' privileged=false ports=loopback\n'
                        + 'taskRun=11111111-1111-1111-1111-111111111111\n'
                        + 'session=22222222-2222-2222-2222-222222222222\n')
        return path

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
        for mode, passed, skipped in [('architecture', 23, 0), ('h2', 157, 17),
                                      ('postgres', 182, 5), ('foundation', 5, 0)]:
            with self.subTest(mode=mode):
                self.fixture(mode)
                self.assertTrue(self.check(mode))
                import json
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


if __name__ == '__main__':
    unittest.main()
