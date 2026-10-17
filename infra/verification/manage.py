"""Bounded, session-owned PostgreSQL/Redis/Rabbit verification. No developer defaults."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import runpy
import secrets
import shutil
import subprocess
import sys
import uuid

ROOT = Path(__file__).resolve().parents[2]
LABEL = 'com.elogistic.verification.session'
PORTS = {'postgres': 5432, 'redis': 6379, 'rabbitmq': 5672}
RABBIT_HEALTH_TEST = ['CMD', 'gosu', 'rabbitmq', 'sh', '-eu', '-c',
    'rabbitmq-diagnostics -q ping && rabbitmq-diagnostics -q check_running && rabbitmq-diagnostics -q check_port_connectivity']
FORBIDDEN = ('elogistics_dev', 'elogistics-d1-postgres18', 'elogistics_d1_pg18_dev_data',
             'elogistics_d1_rabbitmq_dev_data', '.demo/postgres', '.demo/settings.json')
PREFIXES = ('PG_', 'DEMO_', 'SPRING_', 'FOUNDATION_', 'VERIFY_', 'COMPOSE_', 'DOCKER_', 'VITE_')
JAVA_OPTIONS = {'JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS', 'MAVEN_OPTS', 'NODE_OPTIONS', 'BASH_ENV'}


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def guard_environment(env):
    require(not any(k.upper().startswith(PREFIXES) or k.upper() in JAVA_OPTIONS for k in env),
            'Inherited connection/tool configuration rejected; use a clean process environment')


def clean_environment():
    keep = {'PATH', 'SYSTEMROOT', 'WINDIR', 'COMSPEC', 'PATHEXT', 'TEMP', 'TMP', 'USERPROFILE',
            'HOME', 'LOCALAPPDATA', 'APPDATA', 'JAVA_HOME', 'SYSTEMDRIVE', 'LANG', 'TZ'}
    return {k: v for k, v in os.environ.items() if k.upper() in keep}


def identities(session):
    require(bool(re.fullmatch('[a-f0-9]{32}', session)), 'Invalid session identity')
    project = 'elogistics-verify-' + session
    return dict(version=1, session=session, project=project, database='elogistics_verify_' + session,
                bootstrap='verify_bootstrap_' + session, owner='verify_owner_' + session,
                migrator='verify_migrator_' + session, runtime='verify_runtime_' + session,
                pgVolume=project + '-pg', rabbitVolume=project + '-rabbit', network=project + '-network')


def canonical(image):
    require(bool(re.fullmatch(r'[a-z0-9/]+:[a-z0-9.\-]+@sha256:[a-f0-9]{64}', image)), 'Unpinned source image')
    return image.split(':', 1)[0] + '@' + image.split('@', 1)[1]


def approved_images():
    result = {}
    for service, path in [('postgres', 'infra/compose/postgres.yaml'), ('redis', 'infra/compose/foundation.yaml'),
                          ('rabbitmq', 'infra/compose/foundation.yaml')]:
        matches = re.findall(r'^\s+image:\s+(' + service + r':[^\s]+)\s*$', (ROOT / path).read_text(), re.M)
        require(len(matches) == 1, 'Approved source image missing/ambiguous')
        result[service] = canonical(matches[0])
    foundation = runpy.run_path(str(ROOT / 'infra/foundation/manage.py'))
    require(all(canonical(foundation['IMAGES'][s]) == result[s] for s in ('redis', 'rabbitmq')),
            'Existing approved pin sources disagree')
    return result


def private_files(settings):
    pw = settings['credentials']
    salt = base64.b64decode(settings['rabbitSalt'])
    rabbit_hash = base64.b64encode(salt + hashlib.sha256(salt + pw['rabbit'].encode()).digest()).decode()
    definitions = dict(vhosts=[{'name': '/elogistics-foundation'}], users=[dict(name='foundation_probe',
        password_hash=rabbit_hash, hashing_algorithm='rabbit_password_hashing_sha256', tags=[])],
        permissions=[dict(user='foundation_probe', vhost='/elogistics-foundation', configure='(?!)', write='(?!)', read='(?!)')])
    return {'empty.env': '', 'pg-bootstrap': pw['bootstrap'], 'pg-migrator': pw['migrator'],
            'pg-runtime': pw['runtime'], 'redis-password': pw['redis'],
            'redis.acl': 'user default off\nuser foundation_probe on #' + hashlib.sha256(pw['redis'].encode()).hexdigest()
                + ' -@all +ping +info +hello +select +client|setname +client|setinfo\n',
            'definitions.json': json.dumps(definitions, indent=2) + '\n'}


def validate_settings(settings, session):
    expected = identities(session)
    require(set(settings) == set(expected) | {'credentials', 'rabbitSalt'}, 'Unexpected/missing settings keys')
    require(all(settings[k] == v for k, v in expected.items()), 'Session settings identity mismatch')
    names = {'bootstrap', 'migrator', 'runtime', 'redis', 'rabbit', 'token', 'admin', 'merchantA', 'merchantB', 'operator', 'disabled'}
    require(set(settings['credentials']) == names, 'Missing synthetic credentials')
    require(all(re.fullmatch('[A-Za-z0-9_-]{43}', v) for v in settings['credentials'].values()), 'Invalid synthetic credentials')
    try:
        require(len(base64.b64decode(settings['rabbitSalt'], validate=True)) == 4, 'Invalid Rabbit salt')
    except ValueError:
        raise RuntimeError('Invalid Rabbit salt') from None


def validate_effective(config, settings, images, directory):
    """Inspect resolved Compose output, never assume inheritance/interpolation is safe."""
    s = settings
    raw = json.dumps(config).replace('\\\\', '/').lower()
    require(not any(x.lower() in raw for x in FORBIDDEN), 'Developer identity/path in effective configuration')
    require(config['name'] == s['project'] and set(config['services']) == set(PORTS), 'Unexpected Compose project/services')
    require(set(config.get('networks', {})) == {'session'} and set(config.get('volumes', {})) == {'pg_data', 'rabbit_data'},
            'Unexpected resource declarations')
    for item, name in [(config['networks']['session'], s['network']),
                       (config['volumes']['pg_data'], s['pgVolume']), (config['volumes']['rabbit_data'], s['rabbitVolume'])]:
        require(item.get('name') == name and item.get('labels', {}).get(LABEL) == s['session'], 'Resource identity/label mismatch')
        require(not item.get('external') and not item.get('driver_opts'), 'External/redirected resource rejected')
    require(config['networks']['session'].get('driver', 'bridge') == 'bridge'
            and all(v.get('driver', 'local') == 'local' for v in config['volumes'].values()), 'Unexpected resource driver')
    require(set(config.get('secrets', {})) == {'pg_bootstrap', 'pg_migrator', 'pg_runtime', 'redis_password'}, 'Unexpected secrets')
    for key, filename in [('pg_bootstrap', 'pg-bootstrap'), ('pg_migrator', 'pg-migrator'), ('pg_runtime', 'pg-runtime'), ('redis_password', 'redis-password')]:
        secret = config['secrets'][key]
        require(set(secret) <= {'file', 'name'} and normalize_path(secret.get('file', '')) == normalize_path((directory / filename).resolve())
                and secret.get('name', s['project'] + '_' + key) == s['project'] + '_' + key, 'Unexpected private secret path/identity')
    mounts = expected_mounts(directory)
    for service, port in PORTS.items():
        item = config['services'][service]
        require(item['image'] == images[service] and item.get('platform') == 'linux/amd64', 'Incorrect image/platform')
        require(item.get('labels', {}).get(LABEL) == s['session'] and not item.get('container_name'), 'Unexpected container identity')
        require(set(item.get('networks', {})) == {'session'}, 'Unexpected service network')
        require(not any(item.get(k) for k in ('privileged', 'network_mode', 'pid', 'devices', 'cap_add', 'volumes_from', 'env_file', 'entrypoint')),
                'Unsafe service override')
        published = item.get('ports', [])
        require(len(published) == 1 and published[0].get('host_ip') == '127.0.0.1'
                and str(published[0].get('published')) == '0' and published[0]['target'] == port
                and published[0].get('protocol', 'tcp') == 'tcp', 'Incorrect loopback/random binding')
        actual = {(m['type'], normalize_path(m['source']), m['target'], bool(m.get('read_only'))) for m in item.get('volumes', [])}
        require(actual == mounts[service], 'Unexpected volume/bind source or access')
        require(item.get('environment', {}) == postgres_environment(s) if service == 'postgres' else not item.get('environment'),
                'Unexpected service environment')
        expected_secrets = {'postgres': ['pg_bootstrap', 'pg_migrator', 'pg_runtime'], 'redis': ['redis_password'], 'rabbitmq': []}[service]
        actual_secrets = item.get('secrets') or []
        require(actual_secrets == [{'source': name, 'target': '/run/secrets/' + name} for name in expected_secrets], 'Unexpected service secret mapping')
        require(item.get('command') == ['redis-server', '/usr/local/etc/redis/redis.conf'] if service == 'redis' else not item.get('command'), 'Unexpected service command')
        require(item.get('hostname') == 'verification-rabbit' if service == 'rabbitmq' else not item.get('hostname'), 'Unexpected service hostname')
    require(config['services']['redis'].get('tmpfs') == ['/data'], 'Redis must be ephemeral')
    health = config['services']['rabbitmq'].get('healthcheck', {})
    require(health.get('test') == RABBIT_HEALTH_TEST and not health.get('disable'),
            'Rabbit health CLI must run as rabbitmq with all three diagnostics')
    return config


def normalize_path(value):
    value = str(value).replace('\\', '/')
    value = re.sub(r'^/run/desktop/mnt/host/([a-z])/', lambda m: m[1] + ':/', value)
    return value.lower() if os.name == 'nt' else value


def expected_mounts(directory):
    def bind(path, target): return ('bind', normalize_path(path.resolve()), target, True)
    return {'postgres': {('volume', 'pg_data', '/var/lib/postgresql', False),
                bind(ROOT / 'infra/verification/bootstrap.sh', '/docker-entrypoint-initdb.d/10-verification.sh'),
                bind(ROOT / 'infra/verification/bootstrap.sql', '/opt/verification/bootstrap.sql')},
            'redis': {bind(ROOT / 'infra/redis/redis.conf', '/usr/local/etc/redis/redis.conf'), bind(directory / 'redis.acl', '/run/redis.acl')},
            'rabbitmq': {('volume', 'rabbit_data', '/var/lib/rabbitmq', False),
                bind(ROOT / 'infra/rabbitmq/rabbitmq.conf', '/etc/rabbitmq/rabbitmq.conf'), bind(directory / 'definitions.json', '/run/foundation-definitions.json')}}


def postgres_environment(s):
    return dict(POSTGRES_USER=s['bootstrap'], POSTGRES_DB='postgres', POSTGRES_PASSWORD_FILE='/run/secrets/pg_bootstrap',
                VERIFY_DATABASE=s['database'], VERIFY_OWNER=s['owner'], VERIFY_MIGRATOR=s['migrator'], VERIFY_RUNTIME=s['runtime'])


class Session:
    def __init__(self, session):
        guard_environment(os.environ)
        self.s = identities(session)
        self.directory = ROOT / '.demo/verification' / session
        for path in [ROOT / '.demo', ROOT / '.demo/verification', self.directory]:
            require(not path.is_symlink() and not path.is_junction(), 'Redirected private directory rejected')
        self.executable = shutil.which('docker') or str(Path(os.environ.get('LOCALAPPDATA', '')) / 'Programs/DockerDesktop/resources/bin/docker.exe')
        self.context = self.command([self.executable, 'context', 'show']).strip()
        endpoint = json.loads(self.command([self.executable, 'context', 'inspect', self.context]))[0]['Endpoints']['docker']['Host']
        require(endpoint.startswith('npipe:////./pipe/') if os.name == 'nt' else endpoint == 'unix:///var/run/docker.sock', 'Non-local Docker endpoint rejected')
        self.images = approved_images()

    def command(self, args, env=None, input=None, timeout=240):
        try:
            p = subprocess.run(args, cwd=ROOT, env=env or clean_environment(), input=input.encode() if input is not None else None,
                               capture_output=True, timeout=timeout)
        except (OSError, subprocess.TimeoutExpired):
            raise RuntimeError('Tool unavailable/timed out; no private diagnostics published') from None
        if p.returncode != 0:
            if self.directory.is_dir():
                path = self.directory / ('tool-failure-' + uuid.uuid4().hex + '.log')
                path.write_bytes(p.stdout + b'\n' + p.stderr); path.chmod(0o600)
            raise RuntimeError('Tool failed; private diagnostics retained without public output')
        return p.stdout.decode('utf-8').strip()

    def docker(self, *args, input=None):
        return self.command([self.executable, '--context', self.context, *args], input=input)

    def inventory(self):
        return {k: sorted(self.docker(*args).splitlines()) for k, args in {
            'containers': ('ps', '-aq', '--no-trunc'), 'volumes': ('volume', 'ls', '-q'), 'networks': ('network', 'ls', '-q', '--no-trunc')}.items()}

    def save(self, name, value):
        path = self.directory / name
        require(not path.is_symlink() and not path.is_junction(), 'Redirected state rejected')
        path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8'); path.chmod(0o600)

    def load(self):
        require((self.directory / 'settings.json').is_file(), 'Missing private settings; explicit init required')
        require(not any(p.is_symlink() or p.is_junction() for p in self.directory.rglob('*')), 'Redirected private files rejected')
        self.s = json.loads((self.directory / 'settings.json').read_text())
        validate_settings(self.s, self.directory.name)
        for name, value in private_files(self.s).items():
            require((self.directory / name).is_file() and (self.directory / name).read_text() == value, 'Missing/inconsistent private configuration; no regeneration')
        state = json.loads((self.directory / 'state.json').read_text())
        require(state['session'] == self.s['session'] and state['daemon'] == self.docker('info', '--format', '{{.ID}}'), 'State/daemon mismatch')
        return state

    def init(self):
        if self.directory.exists():
            self.load(); return self.s['session']
        self.reject_collisions()
        self.directory.mkdir(parents=True, exist_ok=False)
        runpy.run_path(str(ROOT / 'infra/foundation/manage.py'))['secure_directory'](self.directory)
        self.s.update(credentials={k: secrets.token_urlsafe(32) for k in
            ('bootstrap', 'migrator', 'runtime', 'redis', 'rabbit', 'token', 'admin', 'merchantA', 'merchantB', 'operator', 'disabled')},
            rabbitSalt=base64.b64encode(secrets.token_bytes(4)).decode())
        self.save('settings.json', self.s)
        for name, value in private_files(self.s).items():
            path = self.directory / name; path.write_text(value, encoding='utf-8'); path.chmod(0o600)
        self.save('state.json', dict(session=self.s['session'], daemon=self.docker('info', '--format', '{{.ID}}'),
            phase='initialized', baseline=self.inventory(), containers={}, volumes={}, networks={}))
        return self.s['session']

    def reject_collisions(self):
        inv = self.inventory()
        require(self.s['pgVolume'] not in inv['volumes'] and self.s['rabbitVolume'] not in inv['volumes'], 'Volume collision')
        for identifier in inv['containers']:
            item = json.loads(self.docker('inspect', identifier))[0]
            require(not item['Name'].lstrip('/').startswith(self.s['project'])
                    and item['Config'].get('Labels', {}).get('com.docker.compose.project') != self.s['project'], 'Container/project collision')
        for identifier in inv['networks']:
            item = json.loads(self.docker('network', 'inspect', identifier))[0]
            require(item['Name'] != self.s['network'] and item.get('Labels', {}).get('com.docker.compose.project') != self.s['project'], 'Network collision')

    def compose(self, *args):
        env = clean_environment()
        env.update(VERIFY_SESSION=self.s['session'], VERIFY_PRIVATE=self.directory.as_posix(), VERIFY_DATABASE=self.s['database'],
            VERIFY_BOOTSTRAP=self.s['bootstrap'], VERIFY_OWNER=self.s['owner'], VERIFY_MIGRATOR=self.s['migrator'], VERIFY_RUNTIME=self.s['runtime'],
            VERIFY_PG_VOLUME=self.s['pgVolume'], VERIFY_RABBIT_VOLUME=self.s['rabbitVolume'], VERIFY_NETWORK=self.s['network'])
        env.update({'VERIFY_' + k.upper() + '_IMAGE': v for k, v in self.images.items()})
        return self.command([self.executable, '--context', self.context, 'compose', '--project-name', self.s['project'],
            '--project-directory', str(ROOT / 'infra/verification'), '--env-file', str(self.directory / 'empty.env'),
            '-f', str(ROOT / 'infra/verification/compose.yaml'), *args], env=env)

    def dry_run(self):
        state = self.load()
        if state['phase'] == 'initialized': self.reject_collisions()
        config = json.loads(self.compose('config', '--format', 'json'))
        validate_effective(config, self.s, self.images, self.directory)
        self.save('effective-compose.json', config)
        return dict(session=self.s['session'], project=self.s['project'], database=self.s['database'], images=self.images, status='DRY_RUN_PASS')

    def label(self, item):
        labels = item.get('Labels') or item.get('Config', {}).get('Labels') or {}
        require(labels.get(LABEL) == self.s['session'] and labels.get('com.docker.compose.project') == self.s['project'], 'Foreign ownership rejected')

    def discover(self, state):
        """Capture exact IDs, including an interrupted start. Never remove by prefix/filter."""
        inv = self.inventory()
        found = dict(containers={}, volumes={}, networks={})
        for identifier in inv['containers']:
            item = json.loads(self.docker('inspect', identifier))[0]
            labels = item['Config'].get('Labels') or {}
            if labels.get('com.docker.compose.project') == self.s['project'] or item['Name'].lstrip('/').startswith(self.s['project']):
                self.label(item); service = labels.get('com.docker.compose.service')
                require(service in PORTS and service not in found['containers'], 'Unexpected/duplicate service')
                require(item['Name'].lstrip('/') == self.s['project'] + '-' + service + '-1', 'Unexpected container name')
                require(item['Config']['Image'] == self.images[service] and not item['HostConfig'].get('Privileged')
                        and item['HostConfig'].get('NetworkMode') != 'host', 'Unsafe actual container')
                require(not any('docker.sock' in m.get('Source', '') or 'docker.sock' in m.get('Destination', '') for m in item['Mounts']), 'Socket mount rejected')
                self.verify_mounts(service, item)
                found['containers'][service] = item['Id']
        for key, name in [('pg', self.s['pgVolume']), ('rabbit', self.s['rabbitVolume'])]:
            if name in inv['volumes']:
                item = json.loads(self.docker('volume', 'inspect', name))[0]; self.label(item)
                require(not item.get('Options') and item.get('Driver', 'local') == 'local', 'Redirected volume rejected')
                require(set(self.docker('ps', '-aq', '--no-trunc', '--filter', 'volume=' + name).splitlines()) <= set(found['containers'].values()), 'Foreign volume consumer')
                found['volumes'][key] = name
        for identifier in inv['networks']:
            item = json.loads(self.docker('network', 'inspect', identifier))[0]
            if item['Name'] == self.s['network']:
                self.label(item)
                require(item.get('Driver', 'bridge') == 'bridge', 'Unexpected actual network driver')
                require(set((item.get('Containers') or {})) <= set(found['containers'].values()), 'Foreign network attachment')
                found['networks']['session'] = item['Id']
        for kind in found:
            for key, identifier in state[kind].items():
                require(key not in found[kind] or found[kind][key] == identifier, 'Captured resource replaced')
            require(not set(found[kind].values()).intersection(state['baseline'][kind]), 'Resource belongs to pre-existing inventory')
        state.update(found); self.save('state.json', state)
        return state

    def up(self):
        state = self.load(); self.dry_run()
        require(state['phase'] != 'cleaned', 'Cleaned sessions cannot be reused; init a new session')
        if state['phase'] == 'ready': return self.status()
        require(state['phase'] == 'initialized', 'Interrupted session requires inspect/owned cleanup before retry')
        self.reject_collisions()
        for image in self.images.values():
            info = json.loads(self.docker('image', 'inspect', image))[0]
            require(image in info['RepoDigests'] and info['Os'] == 'linux' and info['Architecture'] == 'amd64', 'Approved image not cached/incorrect; pull exact pin first')
        state['phase'] = 'starting'; self.save('state.json', state)
        try:
            self.compose('up', '-d', '--pull', 'never', '--wait', '--wait-timeout', '180')
        finally:
            self.discover(state)
        result = self.status(); state = self.load(); state['phase'] = 'ready'; self.save('state.json', state)
        return result

    def status(self):
        state = self.discover(self.load())
        require(set(state['containers']) == set(PORTS) and len(state['volumes']) == 2 and len(state['networks']) == 1, 'Incomplete owned stack')
        bindings = {}
        for service, identifier in state['containers'].items():
            info = json.loads(self.docker('inspect', identifier))[0]
            require(info['State']['Running'] and info['State'].get('Health', {}).get('Status') == 'healthy', 'Owned service not healthy')
            ports = {k: v for k, v in info['NetworkSettings']['Ports'].items() if v}
            require(set(ports) == {str(PORTS[service]) + '/tcp'}, 'Unexpected published ports')
            binding = next(iter(ports.values()))
            require(len(binding) == 1 and binding[0]['HostIp'] == '127.0.0.1' and 1024 <= int(binding[0]['HostPort']) <= 65535, 'Incorrect actual loopback binding')
            require(set(info['NetworkSettings']['Networks']) == {self.s['network']}, 'Unexpected actual network')
            self.verify_mounts(service, info)
            bindings[service] = int(binding[0]['HostPort'])
        require(len(set(bindings.values())) == 3, 'Overlapping service ports')
        result = dict(session=self.s['session'], project=self.s['project'], database=self.s['database'], bindings=bindings,
                      containers=state['containers'], volumes=state['volumes'], networks=state['networks'])
        self.save('owned-stack.json', result)
        return result

    def verify_mounts(self, service, info):
        expected = expected_mounts(self.directory)[service].copy()
        mapped = {'pg_data': self.s['pgVolume'], 'rabbit_data': self.s['rabbitVolume']}
        expected = {(t, mapped.get(src, src), dst, ro) for t, src, dst, ro in expected}
        secret_files = {'postgres': [('pg_bootstrap', 'pg-bootstrap'), ('pg_migrator', 'pg-migrator'), ('pg_runtime', 'pg-runtime')],
                        'redis': [('redis_password', 'redis-password')], 'rabbitmq': []}
        expected.update(('bind', normalize_path(self.directory / file), '/run/secrets/' + key, True)
                        for key, file in secret_files[service])
        actual = set()
        for mount in info['Mounts']:
            if mount['Type'] == 'tmpfs':
                require(service == 'redis' and mount['Destination'] == '/data', 'Unexpected tmpfs')
                continue
            source = mount.get('Name') if mount['Type'] == 'volume' else normalize_path(mount['Source'])
            actual.add((mount['Type'], source, mount['Destination'], not mount['RW']))
        require(actual == expected, 'Unexpected actual mounts; refuse access/cleanup')
        require('/data' in (info['HostConfig'].get('Tmpfs') or {}) if service == 'redis' else True, 'Missing Redis tmpfs')

    def down(self):
        state = self.discover(self.load())  # Prove ALL ownership before any destructive call.
        before = self.inventory()
        require(all(set(state['baseline'][kind]) <= set(before[kind]) for kind in before), 'Foreign inventory changed before cleanup')
        for identifier in state['containers'].values(): self.docker('rm', '-f', identifier)
        for identifier in state['volumes'].values(): self.docker('volume', 'rm', identifier)
        for identifier in state['networks'].values(): self.docker('network', 'rm', identifier)
        after = self.inventory()
        for kind in ('containers', 'volumes', 'networks'):
            require(set(state['baseline'][kind]) <= set(after[kind]), 'Foreign inventory changed; no broad cleanup attempted')
            require(not set(state[kind].values()).intersection(after[kind]), 'Owned cleanup incomplete')
        state.update(phase='cleaned', containers={}, volumes={}, networks={}); self.save('state.json', state)
        result = dict(status='CLEANUP_PASS', baselinePreserved=True, inventoryEqualsBaseline=after == state['baseline'])
        self.save('cleanup.json', result)
        return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['init', 'dry-run', 'up', 'status', 'check', 'serve', 'down'])
    parser.add_argument('--session', help='Exact 32-hex session from init; never a developer resource')
    args = parser.parse_args()
    try:
        require(args.command == 'init' or args.session is not None, 'Explicit session required')
        session = Session(args.session or uuid.uuid4().hex)
        if args.command in ('check', 'serve'):
            sys.path.insert(0, str(Path(__file__).parent))
            from checks import check, serve
            result = check(session) if args.command == 'check' else serve(session)
        else:
            action = 'dry_run' if args.command == 'dry-run' else args.command
            result = getattr(session, action)()
        print(json.dumps(result, indent=2))
    except RuntimeError as error:
        raise SystemExit('FAIL/BLOCKED: ' + str(error)) from None
    except (OSError, ValueError, KeyError, subprocess.SubprocessError):
        # Never print underlying exceptions: Docker/HTTP/process diagnostics can carry credentials.
        raise SystemExit('FAIL/BLOCKED: isolated verification; inspect private session evidence; no foreign cleanup performed') from None


if __name__ == '__main__': main()
