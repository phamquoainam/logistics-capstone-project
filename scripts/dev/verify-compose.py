"""Read-only catalog/health inventory of the explicit portable Compose target.

No mutation, role repair, business seed or secret values are printed.
Run from repository root; Python is a verification tool, not a startup prerequisite.
"""
import argparse
import json
import os
import pathlib
import shutil
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]


def docker_path():
    found = shutil.which('docker')
    if found:
        return found
    fallback = pathlib.Path(os.environ.get('LOCALAPPDATA', '')) / 'Programs/DockerDesktop/resources/bin/docker.exe'
    if fallback.is_file():
        return str(fallback)
    raise RuntimeError('DOCKER_CLI_REQUIRED')


def main(project):
    if not project or not all(c.islower() or c.isdigit() or c in '-_' for c in project):
        raise RuntimeError('EXACT_COMPOSE_PROJECT_REQUIRED')
    command = [docker_path(), 'compose', '-p', project]

    def run(args, text=None):
        result = subprocess.run(command + args, cwd=ROOT, input=text, capture_output=True, text=True)
        if result.returncode:
            raise RuntimeError('COMPOSE_VERIFICATION_COMMAND_FAILED_NO_SECRET_OUTPUT')
        return result.stdout.strip()

    source = "console.log(require('fs').readFileSync('/run/database/installation.json','utf8'))"
    meta = json.loads(run(['run', '--rm', '--no-deps', '--entrypoint', 'node', 'material', '-e', source]))
    database = meta['database']
    if not database.startswith('argopost_admin_uat_') or len(database.removeprefix('argopost_admin_uat_')) != 12:
        raise RuntimeError('EXACT_PORTABLE_TARGET_REQUIRED')
    catalog = run(['exec', '-T', 'postgres', 'psql', '-X', '-U', 'argopost_bootstrap', '-d', database,
                   '-qAt', '-v', 'ON_ERROR_STOP=1'], """BEGIN READ ONLY;
SELECT json_build_object(
 'postgresVersion',current_setting('server_version_num'),
 'migrationCount',(SELECT count(*) FROM platform.flyway_schema_history WHERE success),
 'migrationFailureCount',(SELECT count(*) FROM platform.flyway_schema_history WHERE NOT success),
 'migrations',(SELECT json_agg(json_build_object('version',version,'script',script,'checksum',checksum) ORDER BY installed_rank) FROM platform.flyway_schema_history),
 'loginOwnedDomainTables',(SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace JOIN pg_roles r ON r.oid=c.relowner WHERE c.relkind IN ('r','p') AND n.nspname IN ('identity','merchant','orders','dispatch','finance','platform','notifications','pricing','fleet') AND r.rolcanlogin),
 'operatorFlags',(SELECT json_build_object('super',rolsuper,'createRole',rolcreaterole,'createDb',rolcreatedb,'bypassRls',rolbypassrls,'replication',rolreplication) FROM pg_roles WHERE rolname='argopost_local_admin_operator'),
 'operatorMemberships',(SELECT json_agg(json_build_object('role',r.rolname,'inherit',m.inherit_option,'set',m.set_option,'admin',m.admin_option)) FROM pg_auth_members m JOIN pg_roles r ON r.oid=m.roleid JOIN pg_roles u ON u.oid=m.member WHERE u.rolname='argopost_local_admin_operator'),
 'runtimeControlSystem',has_function_privilege('argopost_runtime','pg_catalog.pg_control_system()','EXECUTE'),
 'operatorControlSystem',has_function_privilege('argopost_local_admin_operator','pg_catalog.pg_control_system()','EXECUTE'),
 'operatorPrivateRelationAccess',(SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relkind IN ('r','p') AND n.nspname IN ('identity','merchant','orders','dispatch','finance','notifications','pricing','fleet') AND (has_table_privilege('argopost_local_admin_operator',c.oid,'SELECT') OR has_table_privilege('argopost_local_admin_operator',c.oid,'INSERT') OR has_table_privilege('argopost_local_admin_operator',c.oid,'UPDATE') OR has_table_privilege('argopost_local_admin_operator',c.oid,'DELETE'))),
 'operatorIdentityExecute',(SELECT json_agg(p.proname ORDER BY p.proname) FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='identity' AND has_function_privilege('argopost_local_admin_operator',p.oid,'EXECUTE') AND EXISTS(SELECT 1 FROM aclexplode(COALESCE(p.proacl,acldefault('f',p.proowner))) a JOIN pg_roles r ON r.oid=a.grantee WHERE r.rolname='elogistics_local_admin_operator' AND a.privilege_type='EXECUTE')),
 'realmState',(SELECT status FROM identity.realm WHERE code='SYSTEM'),
 'accounts',(SELECT count(*) FROM identity.user_account),
 'applicationGrants',(SELECT count(*) FROM identity.account_grant),
 'adminEnvironmentBound',identity.local_admin_environment_matches('LOCAL'),
 'staffEnvironmentBound',identity.local_staff_environment_matches('LOCAL'),
 'envBindReceipts',(SELECT count(*) FROM identity.local_admin_operator_receipt WHERE operation='ENV_BIND'),
 'envBindAudit',(SELECT count(*) FROM identity.audit_entry WHERE event_type='ADMIN_ENVIRONMENT_BOUND')
);
COMMIT;
""")
    facts = json.loads(catalog)
    expected = sorted(p.name for p in (ROOT / 'app/backend/src/main/resources/db/migration').glob('V*.sql'))
    actual = sorted(m['script'] for m in facts['migrations'])
    facts['scriptsMatchSource'] = expected == actual
    flags = facts['operatorFlags']
    membership = facts['operatorMemberships']
    facts['operatorBoundary'] = (flags is not None and not any(flags.values()) and membership == [
        {'role': 'elogistics_local_admin_operator', 'inherit': True, 'set': False, 'admin': False}])
    services = [json.loads(line) for line in run(['ps', '--format', 'json']).splitlines() if line.strip()]
    facts['services'] = [{'service': s['Service'], 'state': s['State'], 'health': s.get('Health', '')} for s in services]
    facts['project'] = project
    facts['database'] = database
    facts['deploymentId'] = meta['deploymentId']
    facts['noAutomaticAccounts'] = facts['accounts'] == 0 and facts['applicationGrants'] == 0 and facts['realmState'] == 'INACTIVE'
    facts['healthPass'] = all(any(s['service'] == name and s['health'] == 'healthy' for s in facts['services']) for name in ['postgres', 'backend', 'web', 'mail', 'scanner'])
    passed = (int(facts['postgresVersion']) >= 180000 and int(facts['postgresVersion']) < 190000
              and facts['migrationCount'] == len(expected) == 48 and facts['migrationFailureCount'] == 0
              and facts['scriptsMatchSource'] and facts['loginOwnedDomainTables'] == 0 and facts['operatorBoundary']
              and not facts['runtimeControlSystem'] and not facts['operatorControlSystem'] and facts['operatorPrivateRelationAccess'] == 0
              and facts['adminEnvironmentBound'] and facts['staffEnvironmentBound']
              and facts['envBindReceipts'] == facts['envBindAudit'] == 1 and facts['healthPass'])
    facts['verification'] = 'PASS' if passed else 'FAIL'
    print(json.dumps(facts, indent=2))
    return 0 if passed else 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project', default='argopost-baseline')
    args = parser.parse_args()
    try:
        sys.exit(main(args.project))
    except Exception:
        print('COMPOSE_VERIFICATION_FAILED; no credentials or raw SQL diagnostics published', file=sys.stderr)
        sys.exit(1)
