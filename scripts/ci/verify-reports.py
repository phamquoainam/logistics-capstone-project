"""Fail closed on missing/skipped tests; publish only allowlisted test metadata."""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET

BASE = 'com.elogistic.backend.'
REQUIRED = {
    BASE + 'architecture.ArchitectureTests': '''productionScopeIsRealAndExcludesNegativeFixtures
packagesMatchApprovedModuleStructure modulesOnlyUsePublicApis publicApisDoNotLeakInternals
modulesFollowApprovedDependencyDirection domainIsIndependentOfOuterLayers applicationDependsInward
infrastructureDoesNotDependOnWeb webDoesNotUsePersistenceAdapters controllersDoNotOwnTransactions
moduleGraphHasNoCycles'''.split(),
    BASE + 'architecture.ArchitectureNegativeFixtureTests': '''financeCannotUseOrderInternalRepository
identityCannotCallMerchantEvenThroughPublicApi publicApiCannotLeakOrderDomain moduleCycleIsDetected
domainCannotUseJdbc applicationCannotDependOnItsAdapter webCannotCallRepositoryAdapter
infrastructureCannotDependOnHttpAdapter controllerCannotDeclareTransaction ordersCannotUseBroadAccountView
ordersDomainCannotUseIdentityActor minimalTrustedActorIsAllowedAtApplicationBoundary'''.split(),
    BASE + 'BackendApplicationTests': ['contextLoads'],
    BASE + 'demo.DemoAuthenticationTests': '''loginMeAndHashedPassword wrongPasswordAndDisabledLogin
anonymousUnauthorizedAndWrongRoleForbidden accountStatusAndRoleRecheckedAfterIssue
tamperedTokenAndClientRoleInjectionRejected rateLimitFailedLogins signedExpiredTokenRejected'''.split(),
    BASE + 'demo.DemoSecurityEvidenceTests': '''credentialsAndBearerTokenDoNotAppearInApplicationLogs
signingConfigurationFailsFastWhenMissingOrWeak'''.split(),
    BASE + 'demo.FoundationHealthTests': '''healthyProbesUseConfiguredDatabaseAndHideDetails
refusingTrafficIs503WhileLivenessStaysUp onlyExactAnonymousGetProbesAreAllowed
exhaustedRealPoolReturns503WithinAcquisitionBudgetAndRecovers'''.split(),
    BASE + 'demo.PostgresHealthSafetyTests': '''longRuntimeReadAndPhysicalReuseRemainSafeAfterSuccessfulProbe
concurrentLongReadIsNotInterruptedByExhaustedPoolProbe terminatedPhysicalConnectionIsDiscardedAndReplacementIsSafe
stalledSocketProbeIsBoundedEvictsAndRecoversWithoutTimeoutLeak'''.split(),
    BASE + 'demo.DemoShipmentTests': '''actorMerchantOwnershipIsRecheckedFromDatabase ownDraftAndCrossMerchantIsolation
createReplayConflictAndReferenceUniqueness validationOwnerAndStatusInjectionRejected
cancelGuardReplayAndImmutableEvents concurrentCreateHasOneEffect databaseConstraintAndAdminSummary'''.split(),
    BASE + 'demo.PostgresFoundationTests': '''actualPostgresAndSeparateMigrationPrincipal
nineSchemasAndTablesBelongToOwner runtimeCannotDdlAssumeOwnerOrReadHistory
eventsAndReceiptsCannotBeChangedByRuntime validateAndRerunPreserveDataAndChecksums
versionOneToTwoUpgradePreservesExistingTechnicalData failedForeignKeyRollsBackWholeTransaction
concurrentCancelHasOneEventAndReceipt'''.split(),
}
PG = BASE + 'demo.PostgresFoundationTests'
PG_ONLY = {PG, BASE + 'demo.PostgresHealthSafetyTests'}
SUREFIRE_NAMES = {
    (BASE + 'demo.DemoSecurityEvidenceTests',
     'credentialsAndBearerTokenDoNotAppearInApplicationLogs(CapturedOutput)'):
        'credentialsAndBearerTokenDoNotAppearInApplicationLogs',
}


def verify(source, output, mode, maven_exit):
    problems, seen, rows = [], {}, []
    expected = {k: v for k, v in REQUIRED.items()
                if mode != 'architecture' or '.architecture.' in k}
    for path in sorted(Path(source).glob('TEST-*.xml')):
        try:
            suite = ET.parse(path).getroot()
            name = suite.attrib['name']
            if name not in expected or name in seen or suite.tag != 'testsuite':
                raise ValueError('Unexpected/duplicate suite')
            cases = suite.findall('testcase')
            names, counts = {}, dict(tests=len(cases), failures=0, errors=0, skipped=0)
            for case in cases:
                method = SUREFIRE_NAMES.get((name, case.attrib['name']), case.attrib['name'])
                # Do not publish arbitrary report strings (including secrets in dynamic names).
                if method not in REQUIRED[name] or method in names or case.attrib['classname'] != name:
                    raise ValueError('Unexpected/duplicate test')
                status = 'passed'
                if sum(case.find(tag) is not None for tag in ('failure', 'error', 'skipped')) > 1:
                    raise ValueError('Conflicting test outcomes')
                for tag in ('failure', 'error', 'skipped'):
                    if case.find(tag) is not None:
                        key = {'failure': 'failures', 'error': 'errors', 'skipped': 'skipped'}[tag]
                        counts[key] += 1
                        status = tag
                names[method] = status
                rows.append((name, method, status))
            if any(int(suite.attrib[k]) != value for k, value in counts.items()):
                raise ValueError('Inconsistent counts')
            seen[name] = names
        except (ET.ParseError, KeyError, ValueError, OSError):
            problems.append('Malformed, unexpected or inconsistent Surefire report')
    for name, methods in expected.items():
        actual = seen.get(name, {})
        for method in methods:
            wanted = 'skipped' if mode == 'h2' and name in PG_ONLY else 'passed'
            if actual.get(method) != wanted:
                problems.append(f'{name}.{method}: required {wanted}')
    if not rows:
        problems.append('No test cases discovered')
    if maven_exit:
        problems.append('Maven returned a nonzero exit code')
    counts = {status: sum(row[2] == status for row in rows)
              for status in ('passed', 'failure', 'error', 'skipped')}
    summary = dict(mode=mode, status='FAIL' if problems else 'PASS', discovered=len(rows),
                   executed=len(rows) - counts['skipped'], **counts, problems=problems)
    destination = Path(output)
    destination.mkdir(parents=True, exist_ok=True)
    (destination / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n', encoding='utf-8')
    safe = ET.Element('testsuite', name=f'CI-{mode}', tests=str(len(rows)),
                      failures=str(counts['failure']), errors=str(counts['error']), skipped=str(counts['skipped']))
    for name, method, status in rows:
        test = ET.SubElement(safe, 'testcase', classname=name, name=method)
        if status != 'passed':
            ET.SubElement(test, status).text = 'See local private diagnostics; raw output is not published.'
    ET.ElementTree(safe).write(destination / 'tests.xml', encoding='utf-8', xml_declaration=True)
    print(f"{mode}: {summary['status']}; discovered={len(rows)}, executed={summary['executed']}, "
          f"passed={counts['passed']}, skipped={counts['skipped']}")
    for problem in problems:
        print(problem)
    return not problems


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--output', required=True)
    parser.add_argument('--mode', choices=['architecture', 'h2', 'postgres'], required=True)
    parser.add_argument('--maven-exit', type=int, required=True)
    args = parser.parse_args()
    raise SystemExit(0 if verify(args.source, args.output, args.mode, args.maven_exit) else 1)
