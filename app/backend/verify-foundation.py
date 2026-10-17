"""Real owned Redis/Rabbit/PG verification; no developer resource cleanup or reset."""
import json
import os
from pathlib import Path
import re
import runpy
import secrets
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[2]
MANAGE = runpy.run_path(str(ROOT / 'infra/foundation/manage.py'))
CI = runpy.run_path(str(ROOT / 'scripts/ci/prepare-postgres.py'))
CI['start'].__globals__['IMAGE'] = CI['IMAGE'].replace(':18.6-bookworm@', '@')
OUT = ROOT / '.demo/d1-08-foundation' / ('run-' + uuid.uuid4().hex[:12])


def safe_write(name, value):
    (OUT / name).write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')


def run():
    OUT.mkdir(parents=True, exist_ok=False)
    MANAGE['secure_directory'](OUT)
    docker = MANAGE['docker']
    baseline = {kind: set(docker(*args).splitlines()) for kind, args in {
        'containers': ('ps', '-aq', '--no-trunc'), 'volumes': ('volume', 'ls', '-q'),
        'networks': ('network', 'ls', '-q', '--no-trunc')}.items()}
    for port in (6380, 5673, 5433):
        with socket.socket() as sock: sock.bind(('127.0.0.1', port))
    suffix = uuid.uuid4().hex[:12]
    private = OUT / 'private-infrastructure'
    settings = MANAGE['initialize'](private, 'elogistics-b-' + suffix, 'elogistics_b_test_' + suffix)
    pg_volume = 'elogistics_b_pg_test_' + suffix
    pg_volume_created = False
    fixture_password = secrets.token_urlsafe(32)
    # Test-only key permissions; application ACL remains unchanged and cannot write.
    import hashlib
    with (private / 'redis.acl').open('a', encoding='utf-8') as acl:
        acl.write('user fixture_writer on #' + hashlib.sha256(fixture_password.encode()).hexdigest()
                  + ' -@all ~foundation_fixture +ping +set +get +exists\n')
    backend = log = None
    pg = None
    env = {}
    checks = []
    secrets_to_check = [settings['redisPassword'], settings['rabbitPassword'], settings['rabbitHash'], fixture_password]
    cleanup = {}

    def services():
        ids = MANAGE['assert_owned'](settings)
        result = {}
        for identifier in ids:
            info = json.loads(docker('inspect', identifier))[0]
            result[info['Config']['Labels']['com.docker.compose.service']] = identifier
        assert set(result) == {'redis', 'rabbitmq'}, 'Expected two owned services'
        return result

    def redis_command(user, password, *args):
        identifier = services()['redis']
        # Secret enters stdin only; never Docker CLI arguments or tracked configuration.
        return docker('exec', '-i', identifier, 'sh', '-c',
            'IFS= read -r REDISCLI_AUTH; export REDISCLI_AUTH; exec redis-cli --no-auth-warning --raw --user "$@" 2>&1',
            'probe', user, *args, input=password + '\n')

    try:
        ids = MANAGE['start'](private, settings)
        info = {service: json.loads(docker('inspect', identifier))[0] for service, identifier in services().items()}
        assert '/data' in info['redis']['HostConfig']['Tmpfs']
        assert not any(m['Type'] == 'volume' for m in info['redis']['Mounts'])
        assert any(m['Type'] == 'volume' and m['Name'] == settings['volume'] for m in info['rabbitmq']['Mounts'])
        checks.append(dict(gate='cold-start/image/platform/loopback-only', status='PASS'))
        print('Foundation cold-start/image/platform/exposure PASS', flush=True)
        assert redis_command('foundation_probe', settings['redisPassword'], 'PING') == 'PONG'
        assert 'NOPERM' in redis_command('foundation_probe', settings['redisPassword'], 'SET', 'foundation_fixture', 'forbidden')
        assert 'WRONGPASS' in redis_command('foundation_probe', 'synthetic-wrong-password', 'PING')
        assert 'WRONGPASS' in redis_command('default', 'synthetic-wrong-password', 'PING')
        checks.append(dict(gate='Redis authentication/default-disabled/write-denial', status='PASS'))
        print('Redis real authentication/default-user/write denial PASS', flush=True)
        # A stop/start test needs retained data. tmpfs intentionally disappears on stop.
        # Reuse unchanged CI role/bootstrap SQL with a new ownership-labeled test volume.
        assert pg_volume not in baseline['volumes'], 'Test PostgreSQL volume collision'
        docker('volume', 'create', '--label', MANAGE['LABEL'] + '=' + settings['owner'], pg_volume)
        pg_volume_created = True
        ci_docker = CI['docker']
        def retained_test_database(*args, sql=None):
            args = list(args)
            if args[0] == 'run':
                index = args.index('--tmpfs')
                assert args[index + 1] == '/var/lib/postgresql:rw'
                args[index:index + 2] = ['--mount', 'type=volume,source=' + pg_volume + ',target=/var/lib/postgresql']
            return ci_docker(*args, sql=sql)
        CI['start'].__globals__['docker'] = retained_test_database
        CI['start'](OUT / 'private-postgres')
        pg = json.loads((OUT / 'private-postgres/postgres-state.json').read_text())
        test_env = dict(line.split('=', 1) for line in (OUT / 'private-postgres/postgres.env').read_text().splitlines())
        secrets_to_check += [v for k, v in test_env.items() if 'PASSWORD' in k]
        env = {k: v for k, v in os.environ.items() if not k.startswith(('PG_', 'DEMO_', 'SPRING_', 'FOUNDATION_'))}
        env.update(test_env, FOUNDATION_REDIS_USER='foundation_probe', FOUNDATION_REDIS_PASSWORD=settings['redisPassword'],
            FOUNDATION_RABBIT_USER='foundation_probe', FOUNDATION_RABBIT_PASSWORD=settings['rabbitPassword'],
            FOUNDATION_RESOURCE_ID=settings['owner'])
        for service, identifier in services().items(): env['FOUNDATION_TEST_' + service.upper() + '_ID'] = identifier
        maven = ['cmd.exe', '/d', '/c', 'mvnw.cmd', '-o', '-B', '-ntp',
                 '-Dmaven.repo.local=' + str(ROOT / '.demo/maven-repository')]
        with (OUT / 'private-maven.log').open('wb') as output:
            result = subprocess.run(maven + ['-Ddemo.test.database=postgres', '-Ddemo.test.foundation=true',
                '-Dtest=FoundationDependencyTests', 'clean', 'verify'], cwd=ROOT / 'app/backend', env=env,
                stdout=output, stderr=subprocess.STDOUT, timeout=240)
        guard = subprocess.run([sys.executable, '-B', str(ROOT / 'scripts/ci/verify-reports.py'),
            '--mode', 'foundation', '--source', str(ROOT / 'app/backend/target/surefire-reports'),
            '--output', str(OUT / 'safe-reports'), '--maven-exit', str(result.returncode)], cwd=ROOT, timeout=30)
        assert result.returncode == guard.returncode == 0, 'Real foundation JUnit/guard failed; private Maven log retained'
        checks.append(dict(gate='real-foundation-JUnit', status='PASS', executed=5, skipped=0))
        with socket.socket() as sock:
            sock.bind(('127.0.0.1', 0)); port = sock.getsockname()[1]
        env.update(PG_DB_URL=test_env['PG_TEST_DB_URL'], PG_RUNTIME_USER=test_env['PG_TEST_RUNTIME_USER'],
            PG_RUNTIME_PASSWORD=test_env['PG_TEST_RUNTIME_PASSWORD'], PG_MIGRATOR_USER=test_env['PG_TEST_MIGRATOR_USER'],
            PG_MIGRATOR_PASSWORD=test_env['PG_TEST_MIGRATOR_PASSWORD'], PG_OWNER_ROLE=test_env['PG_TEST_OWNER_ROLE'],
            DEMO_TOKEN_SECRET=secrets.token_urlsafe(48), DEMO_PORT=str(port), DEMO_SEED_ENABLED='false')
        secrets_to_check.append(env['DEMO_TOKEN_SECRET'])
        for scenario in ('missing-credential', 'incomplete-profiles'):
            invalid_env = dict(env)
            profiles = 'demo,postgres,foundation'
            if scenario == 'missing-credential':
                del invalid_env['FOUNDATION_REDIS_PASSWORD']
                expected_error = 'FOUNDATION_REDIS_PASSWORD'
            else:
                profiles = 'demo,foundation'
                invalid_env.update(DEMO_DB_URL='jdbc:h2:mem:foundation_invalid_profiles;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE',
                                   DEMO_DB_USER='sa', DEMO_DB_PASSWORD='')
                expected_error = 'Foundation requires demo,postgres,foundation profiles'
            with (OUT / ('private-' + scenario + '.log')).open('wb') as output:
                result = subprocess.run(['java', '-jar', str(ROOT / 'app/backend/target/backend-0.0.1-SNAPSHOT.jar'),
                    '--spring.profiles.active=' + profiles, '--server.port=0'], env=invalid_env,
                    cwd=ROOT / 'app/backend', stdout=output, stderr=subprocess.STDOUT, timeout=40)
            assert result.returncode != 0 and expected_error in (OUT / ('private-' + scenario + '.log')).read_text(errors='replace'), 'Expected fail-fast configuration error'
            checks.append(dict(gate=scenario + '-actual-startup', status='PASS'))
        log = (OUT / 'private-backend.log').open('wb')
        backend = subprocess.Popen(['java', '-jar', str(ROOT / 'app/backend/target/backend-0.0.1-SNAPSHOT.jar'),
            '--spring.profiles.active=demo,postgres,foundation'], env=env, cwd=ROOT / 'app/backend',
            stdout=log, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL)
        pid = backend.pid

        def http(path, expected):
            assert backend.poll() is None and backend.pid == pid, 'Backend must stay alive without restart'
            start = time.monotonic()
            try:
                with urllib.request.urlopen(f'http://127.0.0.1:{port}' + path, timeout=13) as response:
                    status, raw = response.status, response.read()
            except urllib.error.HTTPError as error: status, raw = error.code, error.read()
            elapsed = round((time.monotonic() - start) * 1000)
            body = json.loads(raw)
            assert status in expected, 'Unexpected probe HTTP status'
            if path.startswith('/actuator/health/') and status in (200, 503):
                assert body == {'status': 'UP' if status == 200 else 'DOWN'}, 'Only status may be exposed'
            assert not any(secret.encode() in raw for secret in secrets_to_check)
            return dict(status=status, elapsedMs=elapsed)

        for attempt in range(90):
            try:
                http('/actuator/health/readiness', (200,)); break
            except (urllib.error.URLError, AssertionError):
                if backend.poll() is not None or attempt == 89: raise AssertionError('Full backend startup failed; private log retained')
                time.sleep(1)
        checks.append(dict(gate='full-profile-live/readiness', status='PASS', readiness=http('/actuator/health/readiness', (200,)),
                           liveness=http('/actuator/health/liveness', (200,))))
        for path in ('/actuator', '/actuator/health', '/actuator/health/redis', '/actuator/health/rabbit', '/actuator/env'):
            http(path, (401,))
        for service in ('redis', 'rabbitmq', 'postgres'):
            for fault in ('pause', 'stop'):
                if service == 'postgres':
                    CI['assert_owned'](pg)
                    identifier = json.loads(CI['docker']('inspect', pg['name']))[0]['Id']
                else: identifier = services()[service]
                try:
                    docker(fault, identifier)
                    state = json.loads(docker('inspect', identifier))[0]['State']
                    assert state['Paused'] if fault == 'pause' else not state['Running']
                    measurements = []
                    for _ in range(2):
                        result = http('/actuator/health/readiness', (503,))
                        assert result['elapsedMs'] < 12000, 'Approved readiness detection budget exceeded'
                        measurements.append(result)
                        http('/actuator/health/liveness', (200,))
                finally:
                    if service == 'postgres': CI['assert_owned'](pg)
                    else: assert services()[service] == identifier
                    docker('unpause' if fault == 'pause' else 'start', identifier)
                start = time.monotonic()
                while True:
                    recovery = http('/actuator/health/readiness', (200, 503))
                    if recovery['status'] == 200: break
                    assert time.monotonic() - start < 30, 'Approved recovery budget exceeded'
                    time.sleep(0.2)
                recovery_ms = round((time.monotonic() - start) * 1000)
                assert recovery_ms < 30000
                checks.append(dict(gate=service + '-' + fault, status='PASS', down=measurements,
                                   recoveryMs=recovery_ms, sameBackend=True))
                print(service + '-' + fault + ' DOWN/recovery PASS ' + json.dumps(dict(down=measurements, recoveryMs=recovery_ms)), flush=True)
        # Prove loss across both restart and recreate, with fixture-only key permissions.
        for action in ('restart', 'recreate'):
            assert redis_command('fixture_writer', fixture_password, 'SET', 'foundation_fixture', 'synthetic') == 'OK'
            assert redis_command('fixture_writer', fixture_password, 'EXISTS', 'foundation_fixture') == '1'
            before = services()['redis']
            args = ('restart', 'redis') if action == 'restart' else ('up', '-d', '--pull', 'never', '--force-recreate', '--no-deps', '--wait', 'redis')
            MANAGE['assert_owned'](settings); MANAGE['compose'](private, settings, *args)
            for attempt in range(30):
                try:
                    assert redis_command('fixture_writer', fixture_password, 'PING') == 'PONG'; break
                except (RuntimeError, AssertionError): time.sleep(0.2)
            assert redis_command('fixture_writer', fixture_password, 'EXISTS', 'foundation_fixture') == '0'
            after = services()['redis']; assert (before == after) if action == 'restart' else (before != after)
            checks.append(dict(gate='Redis-ephemeral-' + action, status='PASS'))
        # Marker vhost is created only by test-side broker administration, never runtime user.
        rabbit = services()['rabbitmq']
        docker('exec', rabbit, 'rabbitmqctl', 'add_vhost', '/foundation_persistence_fixture')
        def broker_metadata():
            rabbit = services()['rabbitmq']
            vhosts = docker('exec', rabbit, 'rabbitmqctl', '-q', 'list_vhosts', 'name')
            users = docker('exec', rabbit, 'rabbitmqctl', '-q', 'list_users')
            queues = docker('exec', rabbit, 'rabbitmqctl', '-q', 'list_queues', '--no-table-headers', '-p', '/elogistics-foundation', 'name')
            assert '/foundation_persistence_fixture' in vhosts and '/elogistics-foundation' in vhosts
            assert 'foundation_probe' in users and 'guest' not in users
            assert not queues.strip(), 'No queues may have been created'
        broker_metadata()
        for action in ('restart', 'recreate'):
            before = services()['rabbitmq']
            args = ('restart', 'rabbitmq') if action == 'restart' else ('up', '-d', '--pull', 'never', '--force-recreate', '--no-deps', '--wait', '--wait-timeout', '180', 'rabbitmq')
            MANAGE['assert_owned'](settings); MANAGE['compose'](private, settings, *args)
            for attempt in range(60):
                try: broker_metadata(); break
                except (RuntimeError, AssertionError):
                    if attempt == 59: raise
                    time.sleep(0.5)
            after = services()['rabbitmq']; assert (before == after) if action == 'restart' else (before != after)
            checks.append(dict(gate='Rabbit-metadata-' + action, status='PASS'))
        start = time.monotonic()
        while http('/actuator/health/readiness', (200, 503))['status'] != 200:
            assert time.monotonic() - start < 30; time.sleep(0.2)
        # Probe connections must not remain after responses; only application connections allowed.
        connections = docker('exec', services()['rabbitmq'], 'rabbitmqctl', '-q', 'list_connections', 'client_properties')
        assert 'elogistics-foundation-readiness' not in connections, 'Health AMQP connection leaked'
        safe_write('safe-results.json', dict(status='PASS', checks=checks, backendRestarted=False,
            imagePins=MANAGE['IMAGES'], platform='linux/amd64', nativeRabbitCachedMetadata='UP while paused; active probe DOWN',
            timeoutTargets=dict(redisConnectMs=1000, redisCommandMs=2000, rabbitConnectHandshakeRpcMs=2000,
                                heartbeatSeconds=10, readinessMs=12000, recoveryMs=30000)))
        print('Real foundation authentication/HTTP outages/recovery/persistence PASS', flush=True)
    finally:
        if backend:
            backend.terminate()
            try: backend.wait(timeout=15)
            except subprocess.TimeoutExpired: backend.kill(); backend.wait(timeout=5)
        if log: log.close()
        private_logs = [p.read_text(encoding='utf-8', errors='replace') for p in OUT.glob('private-*.log')]
        cleanup['logPrivacy'] = not any(secret in content for secret in secrets_to_check for content in private_logs) and not any(
            re.search(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', content) for content in private_logs)
        # These resources were newly created by this invocation and carry its recorded identity.
        for identifier in MANAGE['assert_owned'](settings):
            info = json.loads(docker('inspect', identifier))[0]
            if info['State']['Paused']: docker('unpause', identifier)
            docker('rm', '-f', identifier)
        if settings['volume'] in set(docker('volume', 'ls', '-q').splitlines()):
            MANAGE['assert_owned'](settings); docker('volume', 'rm', settings['volume'])
        networks = docker('network', 'ls', '-q', '--filter', 'label=' + MANAGE['LABEL'] + '=' + settings['owner']).splitlines()
        for identifier in networks:
            info = json.loads(docker('network', 'inspect', identifier))[0]
            assert info['Labels'][MANAGE['LABEL']] == settings['owner'] and not info['Containers']
            docker('network', 'rm', identifier)
        state_path = OUT / 'private-postgres/postgres-state.json'
        if state_path.exists():
            pg = json.loads(state_path.read_text()); CI['assert_owned'](pg)
            info = json.loads(CI['docker']('inspect', pg['name']))[0]
            if info['State']['Paused']: CI['docker']('unpause', info['Id'])
            CI['docker']('rm', '-f', info['Id'])
        if pg_volume_created:
            volume = json.loads(docker('volume', 'inspect', pg_volume))[0]
            assert volume['Name'] == pg_volume and volume['Labels'][MANAGE['LABEL']] == settings['owner']
            docker('volume', 'rm', pg_volume)
        for kind, args in {'containers': ('ps', '-aq', '--no-trunc'), 'volumes': ('volume', 'ls', '-q'),
                           'networks': ('network', 'ls', '-q', '--no-trunc')}.items():
            cleanup[kind + 'Preserved'] = baseline[kind] == set(docker(*args).splitlines())
        cleanup['backendStopped'] = backend is None or backend.poll() is not None
        cleanup['portsReleased'] = True
        for port in (6380, 5673, 5433):
            try:
                with socket.socket() as sock: sock.bind(('127.0.0.1', port))
            except OSError: cleanup['portsReleased'] = False
        safe_write('safe-cleanup.json', cleanup)
        safe_write('safe-progress.json', dict(checks=checks))
        print('Owned cleanup/privacy:', json.dumps(cleanup), flush=True)
        print('Ignored evidence:', OUT, flush=True)
    assert all(cleanup.values())
    return 0


if __name__ == '__main__':
    try: raise SystemExit(run())
    except (AssertionError, RuntimeError, OSError, ValueError, KeyError, subprocess.SubprocessError):
        print('FAIL/BLOCKED: real foundation verification; private diagnostics retained, no automatic reset', file=sys.stderr)
        raise SystemExit(1) from None
