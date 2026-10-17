"""Exercise the CI guard's fail-closed behavior and sanitized artifact boundary."""
import contextlib
import importlib.util
import io
from pathlib import Path
import tempfile
import unittest
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
        for name, methods in reports.REQUIRED.items():
            if mode == 'foundation' and name not in reports.FOUNDATION_ONLY:
                continue
            if mode == 'architecture' and '.architecture.' not in name:
                continue
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


if __name__ == '__main__':
    unittest.main()
