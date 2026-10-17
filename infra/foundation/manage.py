"""Safe additive local Redis/Rabbit bootstrap. No reset/down/volume deletion command."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import socket
import subprocess
import uuid

ROOT = Path(__file__).resolve().parents[2]
LABEL = 'com.elogistic.foundation.owner'
IMAGES = {
    'redis': 'redis:8.4.7-trixie@sha256:43afe4a9a43080e64dfa29699eef903fc9d54991fe8b03d57237c05bed2a396c',
    'rabbitmq': 'rabbitmq:4.3.6@sha256:25736a5d515fe5dcf4c7eec2125b09a0e2043cc934ac0a7aca198c1058564620',
}


def docker(*args, input=None):
    executable = shutil.which('docker') or str(Path(os.environ.get('LOCALAPPDATA', '')) / 'Programs/DockerDesktop/resources/bin/docker.exe')
    # Binary stdin preserves LF on Windows; text-mode CRLF would change a piped password.
    result = subprocess.run([executable, *args], input=input.encode('utf-8') if input is not None else None,
                            capture_output=True, timeout=180)
    if result.returncode:
        raise RuntimeError('Docker operation failed; private diagnostics suppressed')
    return result.stdout.decode('utf-8').strip()


def private_write(path, value):
    if path.exists():
        if path.read_text(encoding='utf-8') != value:
            raise RuntimeError('Existing private configuration differs; no overwrite or credential rotation')
    else:
        path.write_text(value, encoding='utf-8')
        path.chmod(0o600)


def secure_directory(directory):
    directory = directory.resolve()
    if not directory.is_relative_to(ROOT / '.demo'):
        raise RuntimeError('Refusing permissions change outside ignored task configuration')
    if os.name == 'nt':
        sid = subprocess.run(['powershell', '-NoProfile', '-Command',
            '[Security.Principal.WindowsIdentity]::GetCurrent().User.Value'],
            capture_output=True, text=True, check=True, timeout=10).stdout.strip()
        if not re.fullmatch(r'S-1-[0-9-]+', sid):
            raise RuntimeError('Cannot identify credential directory owner')
        if any(p.is_symlink() or p.is_junction() for p in directory.rglob('*')):
            raise RuntimeError('Private directory must not contain redirected paths')
        subprocess.run(['icacls', str(directory), '/inheritance:r', '/grant:r',
            '*' + sid + ':(OI)(CI)F', '*S-1-5-18:(OI)(CI)F', '*S-1-5-32-544:(OI)(CI)F', '/Q'],
            capture_output=True, check=True, timeout=30)
        if any(directory.iterdir()):
            # Children inherit the protected root. Applying inheritance removal to
            # every file instead would leave files without effective access entries.
            subprocess.run(['icacls', str(directory / '*'), '/reset', '/T', '/Q'],
                capture_output=True, check=True, timeout=30)
    else:
        directory.chmod(0o700)


def initialize(directory, project='elogistics-foundation', volume='elogistics_d1_rabbitmq_dev_data'):
    directory = directory.resolve()
    if not directory.is_relative_to(ROOT / '.demo'):
        raise RuntimeError('Private configuration must stay under ignored repository .demo')
    if not re.fullmatch(r'[a-z0-9][a-z0-9_-]+', project) or not re.fullmatch(r'[a-z0-9][a-z0-9_-]+', volume):
        raise RuntimeError('Invalid project/volume identifier')
    settings_path = directory / 'settings.json'
    volumes = set(docker('volume', 'ls', '-q').splitlines())
    if not settings_path.exists() and (volume in volumes or docker('ps', '-aq', '--filter', 'label=com.docker.compose.project=' + project)):
        raise RuntimeError('Resource collision without private settings; preserve existing resources')
    directory.mkdir(parents=True, exist_ok=True)
    secure_directory(directory)
    if settings_path.exists():
        settings = json.loads(settings_path.read_text(encoding='utf-8'))
        if settings['project'] != project or settings['volume'] != volume:
            raise RuntimeError('Existing settings identify another project/volume')
    else:
        password = secrets.token_urlsafe(32)
        salt = secrets.token_bytes(4)
        settings = dict(owner=uuid.uuid4().hex, project=project, volume=volume,
            redisPassword=secrets.token_urlsafe(32), rabbitPassword=password,
            rabbitHash=base64.b64encode(salt + hashlib.sha256(salt + password.encode()).digest()).decode())
        private_write(settings_path, json.dumps(settings, indent=2) + '\n')
    if not re.fullmatch(r'[a-f0-9]{32}', settings['owner']):
        raise RuntimeError('Invalid saved resource ownership identifier')
    for key in ('redisPassword', 'rabbitPassword'):
        if not re.fullmatch(r'[A-Za-z0-9_-]{40,100}', settings[key]):
            raise RuntimeError('Invalid saved credentials; will not rotate automatically')
    acl = 'user default off\nuser foundation_probe on #' + hashlib.sha256(settings['redisPassword'].encode()).hexdigest()
    acl += ' -@all +ping +info +hello +select +client|setname +client|setinfo\n'
    private_write(directory / 'redis.acl', acl)
    private_write(directory / 'redis-password', settings['redisPassword'])
    definitions = dict(vhosts=[{'name': '/elogistics-foundation'}],
        users=[dict(name='foundation_probe', password_hash=settings['rabbitHash'],
                    hashing_algorithm='rabbit_password_hashing_sha256', tags=[])],
        permissions=[dict(user='foundation_probe', vhost='/elogistics-foundation',
                          configure='(?!)', write='(?!)', read='(?!)')])
    private_write(directory / 'definitions.json', json.dumps(definitions, indent=2) + '\n')
    return settings


def environment(directory, settings):
    return dict(os.environ, FOUNDATION_PRIVATE_DIR=directory.resolve().as_posix(),
        FOUNDATION_RESOURCE_ID=settings['owner'], FOUNDATION_RABBIT_VOLUME=settings['volume'])


def compose(directory, settings, *args):
    executable = shutil.which('docker') or str(Path(os.environ.get('LOCALAPPDATA', '')) / 'Programs/DockerDesktop/resources/bin/docker.exe')
    result = subprocess.run([executable, 'compose', '-p', settings['project'], '-f', str(ROOT / 'infra/compose/foundation.yaml'), *args],
        env=environment(directory, settings), text=True, encoding='utf-8', capture_output=True, timeout=240)
    if result.returncode:
        raise RuntimeError('Foundation Compose operation failed; private diagnostics suppressed')
    return result.stdout.strip()


def assert_owned(settings):
    ids = docker('ps', '-aq', '--no-trunc', '--filter', 'label=com.docker.compose.project=' + settings['project']).splitlines()
    for identifier in ids:
        info = json.loads(docker('inspect', identifier))[0]
        if info['Config']['Labels'].get(LABEL) != settings['owner']:
            raise RuntimeError('Container ownership collision')
    if settings['volume'] in set(docker('volume', 'ls', '-q').splitlines()):
        volume = json.loads(docker('volume', 'inspect', settings['volume']))[0]
        if volume.get('Labels', {}).get(LABEL) != settings['owner']:
            raise RuntimeError('Volume ownership collision')
    network_ids = docker('network', 'ls', '-q', '--filter', 'label=com.docker.compose.project=' + settings['project']).splitlines()
    for identifier in network_ids:
        if json.loads(docker('network', 'inspect', identifier))[0].get('Labels', {}).get(LABEL) != settings['owner']:
            raise RuntimeError('Network ownership collision')
    return ids


def verify_images():
    for service, image in IMAGES.items():
        info = json.loads(docker('image', 'inspect', image))[0]
        expected = image.split(':', 1)[0] + '@' + image.split('@', 1)[1]
        if info['Architecture'] != 'amd64' or info['Os'] != 'linux' or expected not in info['RepoDigests']:
            raise RuntimeError('Approved image/platform mismatch; no substitutions permitted')


def start(directory, settings):
    verify_images()
    ids = assert_owned(settings)
    existing_services = {json.loads(docker('inspect', identifier))[0]['Config']['Labels'].get('com.docker.compose.service') for identifier in ids}
    if not existing_services <= {'redis', 'rabbitmq'}:
        raise RuntimeError('Unexpected service in foundation project')
    for service, port in (('redis', 6380), ('rabbitmq', 5673)):
        if service not in existing_services:
            with socket.socket() as sock:
                sock.bind(('127.0.0.1', port))
    compose(directory, settings, 'config', '--quiet')
    compose(directory, settings, 'up', '-d', '--pull', 'never', '--wait', '--wait-timeout', '180')
    ids = assert_owned(settings)
    if len(ids) != 2:
        raise RuntimeError('Expected exactly two foundation services')
    for identifier in ids:
        info = json.loads(docker('inspect', identifier))[0]
        service = info['Config']['Labels']['com.docker.compose.service']
        target, host = ('6379/tcp', '6380') if service == 'redis' else ('5672/tcp', '5673')
        ports = info['NetworkSettings']['Ports']
        published = {key: value for key, value in ports.items() if value}
        if published != {target: [{'HostIp': '127.0.0.1', 'HostPort': host}]}:
            raise RuntimeError('Unexpected network exposure')
    return ids


def run_backend(settings):
    # Uses existing approved PostgreSQL/Demo private settings; never starts/modifies their containers.
    pg = json.loads((ROOT / '.demo/postgres/settings.json').read_text(encoding='utf-8-sig'))
    demo = json.loads((ROOT / '.demo/settings.json').read_text(encoding='utf-8-sig'))
    jar = ROOT / 'app/backend/target/backend-0.0.1-SNAPSHOT.jar'
    if not jar.is_file():
        raise RuntimeError('Build backend JAR first')
    env = {k: v for k, v in os.environ.items() if not k.startswith(('PG_', 'DEMO_', 'SPRING_', 'FOUNDATION_'))}
    env.update(PG_DB_URL='jdbc:postgresql://127.0.0.1:5433/elogistics_dev', PG_RUNTIME_USER='elogistics_runtime',
        PG_RUNTIME_PASSWORD=pg['runtimePassword'], PG_MIGRATOR_USER='elogistics_migrator',
        PG_MIGRATOR_PASSWORD=pg['migratorPassword'], PG_OWNER_ROLE='elogistics_owner',
        DEMO_TOKEN_SECRET=demo['tokenSecret'], DEMO_SEED_ENABLED='false',
        FOUNDATION_REDIS_USER='foundation_probe', FOUNDATION_REDIS_PASSWORD=settings['redisPassword'],
        FOUNDATION_RABBIT_USER='foundation_probe', FOUNDATION_RABBIT_PASSWORD=settings['rabbitPassword'])
    result = subprocess.run(['java', '-jar', str(jar), '--spring.profiles.active=demo,postgres,foundation',
        '--debug=false', '--trace=false'], env=env, cwd=ROOT / 'app/backend')
    if result.returncode:
        raise RuntimeError('Foundation backend exited unsuccessfully')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['init', 'up', 'status', 'run-backend'])
    args = parser.parse_args()
    try:
        directory = ROOT / '.demo/foundation'
        settings = initialize(directory)
        if args.command == 'up':
            start(directory, settings)
        elif args.command == 'status':
            assert_owned(settings)
        elif args.command == 'run-backend':
            assert_owned(settings)
            run_backend(settings)
        print('Foundation ' + args.command + ' PASS; private credentials retained under ignored .demo/foundation')
    except (RuntimeError, OSError, ValueError, KeyError, subprocess.SubprocessError):
        raise SystemExit('FAIL/BLOCKED: foundation bootstrap; no automatic reset/rotation, inspect private state') from None
