"""Actual authenticated checks, one-shot Flyway and bounded owned backend process."""
from contextlib import contextmanager
import json
from pathlib import Path
import re
import subprocess
import time
import urllib.error
import urllib.request
import uuid

from manage import ROOT, clean_environment, require


def sql(session, statement, role='bootstrap', expected=0):
    info = session.status()
    s = session.s
    # TCP password authentication inside the captured owned container. No secret in argv.
    command = ('export PGPASSWORD="$(cat /run/secrets/pg_' + role + ')"; '
               'exec psql -X -q -A -t -h 127.0.0.1 -U "$' + {'bootstrap': 'POSTGRES_USER', 'migrator': 'VERIFY_MIGRATOR', 'runtime': 'VERIFY_RUNTIME'}[role]
               + '" -d "$VERIFY_DATABASE" -v ON_ERROR_STOP=1')
    args = [session.executable, '--context', session.context, 'exec', '-i', info['containers']['postgres'], 'sh', '-eu', '-c', command]
    p = subprocess.run(args, input=statement.encode(), cwd=ROOT, env=clean_environment(), capture_output=True, timeout=30)
    require((p.returncode == 0) == (expected == 0), 'Unexpected owned SQL result; diagnostics suppressed')
    if expected:
        require(b'permission denied' in p.stderr or b'must be owner' in p.stderr, 'DDL denial must be permissions, not SQL syntax/network')
    return p.stdout.decode().strip()


def application_environment(session):
    info = session.status()
    s, pw = session.s, session.s['credentials']
    env = clean_environment()
    env.update(PG_DB_URL=f'jdbc:postgresql://127.0.0.1:{info["bindings"]["postgres"]}/{s["database"]}',
        PG_RUNTIME_USER=s['runtime'], PG_RUNTIME_PASSWORD=pw['runtime'], PG_MIGRATOR_USER=s['migrator'],
        PG_MIGRATOR_PASSWORD=pw['migrator'], PG_OWNER_ROLE=s['owner'],
        FOUNDATION_REDIS_USER='foundation_probe', FOUNDATION_REDIS_PASSWORD=pw['redis'],
        FOUNDATION_REDIS_PORT=str(info['bindings']['redis']), FOUNDATION_RABBIT_USER='foundation_probe',
        FOUNDATION_RABBIT_PASSWORD=pw['rabbit'], FOUNDATION_RABBIT_PORT=str(info['bindings']['rabbitmq']),
        DEMO_TOKEN_SECRET=pw['token'], DEMO_SEED_ENABLED='true', DEMO_ADMIN_PASSWORD=pw['admin'],
        DEMO_MERCHANT_A_PASSWORD=pw['merchantA'], DEMO_MERCHANT_B_PASSWORD=pw['merchantB'],
        DEMO_OPERATOR_PASSWORD=pw['operator'], DEMO_DISABLED_PASSWORD=pw['disabled'])
    require(session.s['database'] in env['PG_DB_URL'] and 'elogistics_dev' not in env['PG_DB_URL'], 'Unexpected database URL')
    return env


def java_arguments(profiles):
    jar = ROOT / 'app/backend/target/backend-0.0.1-SNAPSHOT.jar'
    require(jar.is_file(), 'Build backend JAR with Wrapper verify first')
    return ['java', '-jar', str(jar), '--spring.profiles.active=' + profiles,
            '--spring.config.location=classpath:/', '--debug=false', '--trace=false',
            '--server.address=127.0.0.1', '--server.port=0']


def privacy(session, paths, extra=()):
    values = list(session.s['credentials'].values()) + list(extra)
    for path in paths:
        data = Path(path).read_bytes()
        require(not any(value.encode() in data for value in values), 'Credential in private diagnostic; do not publish')
        require(not re.search(rb'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+|-----BEGIN .*PRIVATE KEY', data), 'Token/key in diagnostic; do not publish')


def migrate(session):
    env = application_environment(session)
    env['DEMO_SEED_ENABLED'] = 'false'
    path = session.directory / ('migration-' + uuid.uuid4().hex + '.log')
    with path.open('wb') as output:
        p = subprocess.run(java_arguments('migration'), cwd=session.directory, env=env, stdout=output, stderr=subprocess.STDOUT, timeout=90)
    privacy(session, [path])
    require(p.returncode == 0, 'One-shot Flyway failed; private log retained')
    return path


@contextmanager
def backend(session):
    env = application_environment(session)
    path = session.directory / ('backend-' + uuid.uuid4().hex + '.log')
    with path.open('wb') as output:
        process = subprocess.Popen(java_arguments('demo,postgres,foundation'), cwd=session.directory, env=env,
                                   stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT)
        try:
            started = time.monotonic()
            while True:
                require(process.poll() is None, 'Owned backend exited; private log retained')
                text = path.read_text(errors='replace')
                match = re.search(r'Tomcat started on port (\d+)', text)
                if match:
                    port = int(match[1]); require(1024 <= port <= 65535, 'Unexpected actual backend port')
                    base = 'http://127.0.0.1:' + str(port)
                    try:
                        request(base, '/actuator/health/readiness', 200, expected_body={'status': 'UP'})
                        break
                    except (RuntimeError, OSError):
                        pass
                require(time.monotonic() - started < 90, 'Owned backend startup timeout')
                time.sleep(0.5)
            session.save('backend-public.json', dict(pid=process.pid, baseUrl=base, profiles='demo,postgres,foundation'))
            # Explicit temporary config, never edit the developer Vite config/defaults.
            source = json.dumps((ROOT / 'app/frontend/web/vite.config.ts').as_posix())
            target = json.dumps(base)
            (session.directory / 'vite.config.mjs').write_text(
                f'import base from {source};\nconst proxy={{"/api":{{target:{target},changeOrigin:false}}}};\n'
                'export default {...base,server:{...base.server,host:"127.0.0.1",port:0,strictPort:true,proxy},'
                'preview:{...base.preview,host:"127.0.0.1",port:0,strictPort:true,proxy}};\n', encoding='utf-8')
            yield base
        finally:
            if process.poll() is None:
                process.terminate()
                try: process.wait(timeout=15)
                except subprocess.TimeoutExpired: process.kill(); process.wait(timeout=10)
            privacy(session, [path])


def request(base, path, status, body=None, token=None, expected_body=None):
    headers = {'Content-Type': 'application/json'}
    if token: headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(base + path, data=json.dumps(body).encode() if body is not None else None, headers=headers)
    try:
        with urllib.request.build_opener(urllib.request.ProxyHandler({})).open(req, timeout=12) as response:
            actual, data = response.status, response.read()
    except urllib.error.HTTPError as e: actual, data = e.code, e.read()
    require(actual == status, 'Unexpected HTTP result; body suppressed')
    parsed = json.loads(data)
    if expected_body is not None: require(parsed == expected_body, 'Unexpected probe body')
    return parsed


def history(session):
    return sql(session, 'SELECT version||\':\'||checksum FROM platform.flyway_schema_history WHERE success ORDER BY installed_rank;')


def rabbit_cookie_metadata(session, container):
    """Readability/owner/mode only: never read or report the Erlang cookie payload."""
    metadata = session.docker('exec', container, 'gosu', 'rabbitmq', 'sh', '-eu', '-c',
        'test "$HOME" = /var/lib/rabbitmq; test -r "$HOME/.erlang.cookie"; '
        'printf "%s:%s\\n" "$(id -u)" "$(id -g)"; stat -c "%u:%g:%a" "$HOME/.erlang.cookie"')
    lines = metadata.splitlines()
    require(len(lines) == 2 and bool(re.fullmatch(r'[1-9][0-9]*:[1-9][0-9]*', lines[0]))
            and lines[1] in [lines[0] + ':400', lines[0] + ':600'],
            'Rabbit cookie owner/mode must match the non-root service user')


def check(session):
    session.dry_run(); info = session.status(); start = time.monotonic(); checks = []
    def passed(name): checks.append(dict(gate=name, status='PASS'))
    s = session.s
    require(sql(session, 'SELECT current_database()||\':\'||current_user;', 'runtime') == s['database'] + ':' + s['runtime'], 'Runtime authentication identity')
    require(sql(session, 'SELECT current_user;', 'migrator') == s['migrator'], 'Migrator authentication identity')
    passed('postgres-TCP-authentication/isolated-database-and-roles')
    redis = info['containers']['redis']
    result = session.docker('exec', redis, 'sh', '-eu', '-c',
        'export REDISCLI_AUTH="$(cat /run/secrets/redis_password)"; redis-cli --user foundation_probe --no-auth-warning --raw PING; '
        'redis-cli --user foundation_probe --no-auth-warning --raw SET forbidden value')
    require(result.startswith('PONG\n') and 'NOPERM' in result, 'Redis authenticated probe/write denial')
    passed('redis-authenticated-PING/least-privilege')
    require('NOAUTH' in session.docker('exec', redis, 'redis-cli', '--raw', 'PING'), 'Redis default/anonymous access enabled')
    rabbit = info['containers']['rabbitmq']
    rabbit_cookie_metadata(session, rabbit)
    permissions = session.docker('exec', rabbit, 'gosu', 'rabbitmq', 'rabbitmqctl', '-q', 'list_user_permissions', 'foundation_probe')
    users = session.docker('exec', rabbit, 'gosu', 'rabbitmq', 'rabbitmqctl', '-q', 'list_users')
    require('/elogistics-foundation' in permissions and permissions.count('(?!)') == 3
            and re.search(r'^foundation_probe\s+\[\]$', users, re.M), 'Rabbit user/vhost permissions differ')
    passed('Redis-anonymous-denial/Rabbit-non-admin-and-vhost-permissions')
    require(sql(session, "SELECT count(*) FROM pg_tables WHERE schemaname IN ('platform','identity','merchant','orders');") == '0', 'Cold-start database not empty')
    migrate(session); initial = history(session)
    require(len(initial.splitlines()) == 2 and initial.startswith('1:') and '\n2:' in initial, 'V1/V2 history')
    require(sql(session, f"SELECT count(*) FROM pg_namespace WHERE nspowner=(SELECT oid FROM pg_roles WHERE rolname='{s['owner']}') AND nspname IN ('platform','identity','merchant','orders','pricing','warehouse','dispatch','finance','routing','notification');") == '10', 'Schema ownership')
    require(sql(session, f"SELECT count(*) FROM pg_tables WHERE tableowner='{s['owner']}' AND schemaname IN ('platform','identity','merchant','orders');") == '6', 'Table ownership')
    require(sql(session, f"SELECT rolcanlogin FROM pg_roles WHERE rolname='{s['owner']}';") == 'f', 'Owner must be NOLOGIN')
    require(sql(session, f"SELECT count(*) FROM pg_roles WHERE rolname IN ('{s['runtime']}','{s['migrator']}') AND (rolsuper OR rolcreatedb OR rolcreaterole);") == '0', 'Migration/runtime elevated role privileges')
    for statement in ['CREATE SCHEMA forbidden;', 'CREATE TABLE public.forbidden(id int);', 'CREATE TABLE orders.forbidden(id int);',
                      'ALTER TABLE orders.demo_shipment ADD COLUMN forbidden int;', 'SELECT * FROM platform.flyway_schema_history;', f'SET ROLE {s["owner"]};']:
        sql(session, statement, 'runtime', expected=1)
    passed('empty-V1/V2-and-owner/migrator/runtime-permissions')
    sql(session, f'SET ROLE {s["owner"]}; CREATE TABLE platform.verification_marker(id int PRIMARY KEY); INSERT INTO platform.verification_marker VALUES(1);', 'migrator')
    migrate(session); require(history(session) == initial, 'Migration rerun drift')
    passed('Flyway-validate/rerun/checksums')
    with backend(session) as base:
        request(base, '/actuator/health/liveness', 200, expected_body={'status': 'UP'})
        request(base, '/actuator/health/readiness', 200, expected_body={'status': 'UP'})
        request(base, '/api/demo/v1/health', 200, expected_body={'status': 'UP', 'scope': 'local-demo'})
        request(base, '/actuator/health', 401)
        request(base, '/api/demo/v1/auth/me', 401)
        login = request(base, '/api/demo/v1/auth/login', 200, {'username': 'demo.admin', 'password': s['credentials']['admin']})
        token = login['accessToken']
        require(request(base, '/api/demo/v1/auth/me', 200, token=token)['role'] == 'ADMIN', 'Trusted authenticated admin smoke')
        passed('full-profile/liveness/readiness/active-AMQP-and-Demo-API')
        # Restart only captured own PostgreSQL, then refresh Docker bindings (ports can change).
        captured = session.status()['containers']['postgres']; session.docker('restart', captured)
        wait_healthy(session)
        after = session.status()
        require(after['containers']['postgres'] == captured, 'Restart replaced PostgreSQL')
        require(sql(session, 'SELECT count(*) FROM platform.verification_marker;') == '1' and history(session) == initial, 'Persistence/checksum drift')
        passed('owned-PostgreSQL-restart/persistence')
        # Existing backend uses original bindings: only assert same-process recovery when unchanged.
        if after['bindings'] == info['bindings']:
            deadline = time.monotonic() + 30
            while True:
                try: request(base, '/actuator/health/readiness', 200, expected_body={'status': 'UP'}); break
                except (RuntimeError, OSError): require(time.monotonic() < deadline, 'Readiness recovery timeout'); time.sleep(1)
            passed('same-backend-PostgreSQL-recovery')
    migrate(session); require(history(session) == initial, 'Restart migration rerun drift')
    passed('post-restart-Flyway-validate/rerun')
    with backend(session) as base:
        request(base, '/actuator/health/readiness', 200, expected_body={'status': 'UP'})
        passed('post-restart-backend/verified-live-bindings')
    result = dict(status='CONTROLLED_LOCAL_PASS', session=s['session'], checks=checks,
                  seconds=round(time.monotonic() - start, 3), officialAC00101='UNVERIFIED_PENDING_INTEGRATED_FRESH_CLONE')
    session.save('checks.json', result)
    return result


def wait_healthy(session):
    deadline = time.monotonic() + 90
    while True:
        try: return session.status()
        except RuntimeError: require(time.monotonic() < deadline, 'Owned service recovery timeout'); time.sleep(1)


def serve(session):
    with backend(session) as base:
        print(json.dumps(dict(baseUrl=base, viteConfig=str(session.directory / 'vite.config.mjs'), notice='Owned backend; Ctrl+C stops only this child')), flush=True)
        try:
            while True: time.sleep(1)
        except KeyboardInterrupt: pass
    return dict(status='OWNED_BACKEND_STOPPED')
