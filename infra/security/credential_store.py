"""Protected credential primitives and READ-ONLY Phase-1 CLI.

No init, migration, activation or retirement command is exposed. Callers must
explicitly select protected mode; absence is an error, never a legacy fallback.
Provisioning APIs are exercised only with synthetic fixtures in Phase 1.
"""
import argparse
import contextlib
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import sys
import tempfile
import uuid

sys.path.insert(0, str(Path(__file__).parent))
from windows_store import SecurityError, Windows, require, Presence, query_failure, verified_absent

PROJECT = 'logistics-capstone-project'
FILES = ('demo-settings.json', 'postgres-settings.json', 'bootstrap-password.txt',
         'migrator-password.txt', 'runtime-password.txt')
HISTORICAL = ('1876bd2b6a60', '3c64289463f4', '586ff01132d0', '66edc4202fde',
              '73164a35c5e5', '91be82a1b660', 'fad8435bd7d6')
DEMO_FIELDS = ('tokenSecret', 'databasePassword', 'adminPassword', 'merchantAPassword',
               'merchantBPassword', 'operatorPassword', 'disabledPassword')


def leaf(name):
    require(isinstance(name, str) and bool(re.fullmatch(r'[a-zA-Z0-9][a-zA-Z0-9._-]{0,100}', name))
            and not name.endswith('.') and '..' not in name and name.split('.')[0].upper() not in
            {'CON', 'PRN', 'AUX', 'NUL', *(f'COM{i}' for i in range(10)), *(f'LPT{i}' for i in range(10))}, 'INVALID_LEAF')
    return name


def metadata_leaf(name):
    name = leaf(name)
    key = name.casefold()
    require(key not in {'operation.lock', 'transactions', 'store.json', 'complete.json', *FILES}
            and not key.startswith(('pending-', 'backup-', 'failed-', 'transaction-')), 'RESERVED_METADATA_NAME')
    return name


def object_identity(info):
    return {key: info[key] for key in ('volume', 'file', 'directory')}


def digest(value):
    return hashlib.sha256(value).hexdigest()


def encode(value):
    return (json.dumps(value, sort_keys=True, separators=(',', ':')) + '\n').encode()


def decode(data):
    try:
        return json.loads(data.decode('utf-8-sig'))
    except (UnicodeError, ValueError):
        raise SecurityError('INVALID_JSON') from None


def metadata_value(value):
    if isinstance(value, dict):
        require(all(isinstance(k, str) and not re.search('password|secret|token|credential', k, re.I)
                    for k in value), 'SECRET_FIELD_IN_METADATA')
        for item in value.values():
            metadata_value(item)
    elif isinstance(value, list):
        for item in value:
            metadata_value(item)


class PosixFixture:
    """Portable synthetic tests only; normal protected mode is Windows-only."""
    sid = 'posix-fixture'

    def _presence_stat(self, path):
        return path.lstat()

    def presence(self, path):
        path = Path(path).absolute()
        parent_verified = False
        for node in list(reversed(path.parents)) + [path]:
            try:
                info = self._presence_stat(node)
            except OSError as error:
                return query_failure(error, parent_verified)
            if stat.S_ISLNK(info.st_mode) or (not stat.S_ISDIR(info.st_mode) and
                                             (not stat.S_ISREG(info.st_mode) or info.st_nlink != 1)):
                return Presence('UNKNOWN', 'UNSAFE_OBJECT')
            if node == path:
                return Presence('PRESENT', 'VERIFIED_OBJECT')
            if (not stat.S_ISDIR(info.st_mode) or info.st_uid not in (0, os.getuid()) or
                    (info.st_mode & 0o022 and not info.st_mode & stat.S_ISVTX)):
                return Presence('UNKNOWN', 'UNSAFE_NAMESPACE')
            parent_verified = True

    @contextlib.contextmanager
    def pin(self, path):
        for parent in reversed([path, *path.parents]):
            info = parent.lstat()
            require(not stat.S_ISLNK(info.st_mode), 'REPARSE_REJECTED')
            require(info.st_uid in (0, os.getuid()), 'UNTRUSTED_ANCESTOR_OWNER')
            require(not info.st_mode & 0o022 or bool(info.st_mode & stat.S_ISVTX), 'UNSAFE_ANCESTOR_ACCESS')
        yield

    def inspect(self, path):
        s = path.lstat()
        require(not stat.S_ISLNK(s.st_mode) and (stat.S_ISDIR(s.st_mode) or s.st_nlink == 1), 'UNSAFE_FILE_TYPE')
        return {'volume': s.st_dev, 'file': s.st_ino, 'directory': stat.S_ISDIR(s.st_mode),
                'size': s.st_size, 'write': s.st_mtime_ns, 'owner': s.st_uid}

    def validate(self, path, kind):
        i = self.inspect(path)
        mode = stat.S_IMODE(path.lstat().st_mode)
        require(i['owner'] == os.getuid() and mode == {'directory': 0o700, 'credential': 0o400, 'metadata': 0o600}[kind], 'PRIVATE_POLICY_MISMATCH')
        require(i['directory'] == (kind == 'directory'), 'OBJECT_TYPE_MISMATCH')
        return i

    def mkdir(self, path):
        with self.pin(path.parent):
            path.mkdir(mode=0o700)

    def create(self, path, data, kind):
        with self.pin(path.parent):
            descriptor = os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY | os.O_NOFOLLOW,
                                 0o400 if kind == 'credential' else 0o600)
            with os.fdopen(descriptor, 'wb') as stream:
                stream.write(data)
                stream.flush()
                os.fsync(stream.fileno())
            self.validate(path, kind)

    def read(self, path, kind):
        with self.pin(path.parent):
            self.validate(path, kind)
            with os.fdopen(os.open(path, os.O_RDONLY | os.O_NOFOLLOW), 'rb') as stream:
                return stream.read(1024 * 1024 + 1)

    def replace(self, target, temporary, backup):
        self.validate(target, 'metadata')
        self.validate(temporary, 'metadata')
        require(verified_absent(backup, self), 'BACKUP_COLLISION')
        # Fixture only: two renames preserve identities, not Windows atomicity.
        os.rename(target, backup)
        os.replace(temporary, target)

    @contextlib.contextmanager
    def hold_lock(self, path):
        import fcntl
        with os.fdopen(os.open(path, os.O_RDWR | os.O_NOFOLLOW), 'r+b') as stream:
            fcntl.flock(stream, fcntl.LOCK_EX | fcntl.LOCK_NB)
            yield self.validate(path, 'metadata')

    def remove_owned(self, path, identity):
        current = self.inspect(path)
        require(all(current[k] == v for k, v in identity.items()), 'CLEANUP_IDENTITY_CHANGED')
        (path.rmdir if identity['directory'] else path.unlink)()


def production_root(backend=None):
    backend = backend or Windows()
    return backend.known_folder() / 'ELogistics-Credentials' / PROJECT


class Store:
    def __init__(self, root, backend, fixture=False):
        self.root, self.backend, self.fixture = Path(root).absolute(), backend, fixture

    @classmethod
    def open_development(cls):
        backend = Windows()
        store = cls(production_root(backend) / 'development', backend)
        store.validate()
        return store

    @classmethod
    def synthetic(cls):
        backend = Windows() if os.name == 'nt' else PosixFixture()
        parent = backend.known_folder() if os.name == 'nt' else Path(tempfile.gettempdir())
        store = cls(parent / ('ELogistics-Credential-Test-' + uuid.uuid4().hex), backend, fixture=True)
        store._create()
        return store

    def _create(self):
        # Phase 1 has no real-store provisioning entry point.
        require(self.fixture and self.root.name.startswith('ELogistics-Credential-Test-'), 'SYNTHETIC_ONLY')
        self.backend.mkdir(self.root)
        self.created_identity = self.backend.inspect(self.root)
        self.backend.create(self.root / 'store.json', encode({'version': 1, 'project': PROJECT,
            'owner': self.backend.sid, 'purpose': 'synthetic', 'fixture': True}), 'credential')
        self.backend.mkdir(self.root / 'generations')
        self.backend.mkdir(self.root / 'metadata')
        self.backend.mkdir(self.root / 'metadata' / 'transactions')
        self.backend.mkdir(self.root / 'sessions')

    def validate(self):
        with self.backend.pin(self.root):
            self.backend.validate(self.root, 'directory')
            marker = decode(self.backend.read(self.root / 'store.json', 'credential'))
            require(marker == {'version': 1, 'project': PROJECT, 'owner': self.backend.sid,
                               'purpose': 'synthetic' if self.fixture else 'development',
                               'fixture': self.fixture}, 'STORE_IDENTITY_MISMATCH')
            for name in ('generations', 'metadata', 'sessions'):
                self.backend.validate(self.root / name, 'directory')
            self.backend.validate(self.root / 'metadata' / 'transactions', 'directory')

    def prepare_test_record(self, value):
        """Fresh session BEFORE provisioning; failure must retain this receipt.

        No developer credential read. The caller generates the test values;
        this does not authorize database operations or legacy migration.
        """
        self.validate()
        session = uuid.uuid4().hex
        path = self.root / 'sessions' / session
        self.backend.mkdir(path)
        data = encode(value)
        self.backend.create(path / 'record.json', data, 'credential')
        self.backend.create(path / 'complete.json', encode({'session': session,
            'purpose': 'postgres-test', 'owner': self.backend.sid, 'sha256': digest(data)}), 'credential')
        return session

    def test_record_path(self, session):
        require(isinstance(session, str) and bool(re.fullmatch('[a-f0-9]{32}', session)), 'INVALID_SESSION')
        self.validate()
        path = self.root / 'sessions' / session
        self.backend.validate(path, 'directory')
        manifest = decode(self.backend.read(path / 'complete.json', 'credential'))
        data = self.backend.read(path / 'record.json', 'credential')
        require(manifest == {'session': session, 'purpose': 'postgres-test', 'owner': self.backend.sid,
                             'sha256': digest(data)}, 'SESSION_IDENTITY_MISMATCH')
        return path / 'record.json'

    def publish(self, values):
        """Synthetic generation provisioning; never called by Phase-1 launchers."""
        require(self.fixture and isinstance(values, dict) and bool(values), 'SYNTHETIC_ONLY')
        self.validate()
        require(all(leaf(name) not in {'complete.json', 'store.json'} and isinstance(value, bytes)
                    for name, value in values.items()), 'INVALID_GENERATION')
        generation = uuid.uuid4().hex
        path = self.root / 'generations' / generation
        self.backend.mkdir(path)
        for name, value in values.items():
            self.backend.create(path / name, value, 'credential')
        self.backend.create(path / 'complete.json', encode({'version': 1, 'generation': generation,
            'files': {name: digest(value) for name, value in values.items()}}), 'credential')
        return generation

    def generation(self, generation):
        require(isinstance(generation, str) and bool(re.fullmatch('[a-f0-9]{32}', generation)), 'INVALID_GENERATION')
        self.validate()
        path = self.root / 'generations' / generation
        self.backend.validate(path, 'directory')
        complete = decode(self.backend.read(path / 'complete.json', 'credential'))
        require(isinstance(complete, dict) and set(complete) == {'version', 'generation', 'files'}
                and complete['version'] == 1 and complete['generation'] == generation
                and isinstance(complete['files'], dict) and complete['files'], 'INCOMPLETE_GENERATION')
        for name, expected in complete['files'].items():
            leaf(name)
            require(isinstance(expected, str) and expected == digest(self.backend.read(path / name, 'credential')), 'CONTENT_MISMATCH')
        require(set(p.name for p in path.iterdir()) == set(complete['files']) | {'complete.json'}, 'UNEXPECTED_GENERATION_FILE')
        return path, complete

    def read(self, generation, name):
        path, manifest = self.generation(generation)
        require(leaf(name) in manifest['files'], 'FILE_NOT_DECLARED')
        return self.backend.read(path / name, 'credential')

    def create_metadata(self, name, value):
        name = metadata_leaf(name)  # No filesystem mutation before name/value checks.
        metadata_value(value)
        self.validate()
        with self._metadata_lock():
            self._history(resolved=True)
            self.backend.create(self.root / 'metadata' / name, encode(value), 'metadata')

    def metadata(self, name):
        name = metadata_leaf(name)
        self.validate()
        return decode(self.backend.read(self.root / 'metadata' / name, 'metadata'))

    @contextlib.contextmanager
    def _metadata_lock(self, reconcile=False):
        path = self.root / 'metadata' / 'operation.lock'
        if not reconcile or verified_absent(path, self.backend):
            self.backend.create(path, b'{}', 'metadata')
        # Kept open with no sharing throughout the operation. An explicit
        # reconciler cannot claim a live writer's lock, including across processes.
        identity = None
        try:
            with self.backend.hold_lock(path) as identity:
                yield
        except OSError:
            raise SecurityError('METADATA_IO_FAILED') from None
        finally:
            # A failed claim must never delete another live process's lock.
            if identity is not None:
                self.backend.remove_owned(path, identity)

    def _history(self, resolved=False):
        """Replay immutable records; missing/corrupt evidence always blocks writers.

        File flushes improve process-restart recovery; directory/power-loss
        durability is NOT guaranteed by this protocol.
        """
        folder = self.root / 'metadata' / 'transactions'
        entries = []
        for path in folder.iterdir():
            require(bool(re.fullmatch('[a-f0-9]{32}', path.name)), 'INVALID_TRANSACTION_DIRECTORY')
            self.backend.validate(path, 'directory')
            require(not verified_absent(path / 'intent.json', self.backend), 'RECOVERY_REQUIRED_NO_INTENT')
            intent = decode(self.backend.read(path / 'intent.json', 'credential'))
            keys = {'version', 'transaction', 'revision', 'priorRevision', 'operation', 'target',
                    'temporary', 'backup', 'before', 'after', 'beforeIdentity', 'afterIdentity', 'store'}
            require(isinstance(intent, dict) and set(intent) == keys and intent['version'] == 1
                    and intent['transaction'] == path.name and type(intent['revision']) is int
                    and intent['revision'] > 0 and type(intent['priorRevision']) is int
                    and 0 <= intent['priorRevision'] < intent['revision']
                    and intent['operation'] in ('COMMITTED', 'ROLLED_BACK'), 'INVALID_TRANSACTION_INTENT')
            metadata_leaf(intent['target'])
            require(intent['temporary'] == 'pending-' + path.name + '.json'
                    and intent['backup'] == 'backup-' + path.name + '.json'
                    and intent['store'] == object_identity(self.backend.validate(self.root, 'directory')),
                    'TRANSACTION_IDENTITY_MISMATCH')
            for key in ('before', 'after'):
                require(isinstance(intent[key], str) and bool(re.fullmatch('[a-f0-9]{64}', intent[key])), 'INVALID_TRANSACTION_DIGEST')
            for key in ('beforeIdentity', 'afterIdentity'):
                identity = intent[key]
                require(isinstance(identity, dict) and set(identity) == {'volume', 'file', 'directory'}
                        and type(identity['volume']) is int and type(identity['file']) is int
                        and identity['directory'] is False, 'INVALID_TRANSACTION_OBJECT')
            names = {p.name for p in path.iterdir()}
            require(names <= {'intent.json', 'terminal.json', 'recovery-required.json', 'resolution.json'}, 'UNKNOWN_TRANSACTION_EVIDENCE')
            events = {}
            for name in names - {'intent.json'}:
                event = decode(self.backend.read(path / name, 'credential'))
                require(isinstance(event, dict) and set(event) == {'transaction', 'revision', 'state'}
                        and event['transaction'] == path.name and event['revision'] == intent['revision'], 'INVALID_TRANSACTION_EVENT')
                expected = {'terminal.json': {intent['operation']}, 'recovery-required.json': {'RECOVERY_REQUIRED'},
                            'resolution.json': {intent['operation'], 'ABORTED'}}[name]
                require(event['state'] in expected, 'INVALID_TRANSACTION_STATE')
                events[name] = event['state']
            state = events.get('resolution.json', events.get('recovery-required.json', events.get('terminal.json', 'PREPARED')))
            entries.append({'intent': intent, 'state': state})
        entries.sort(key=lambda entry: entry['intent']['revision'])
        revisions = [e['intent']['revision'] for e in entries]
        require(len(set(revisions)) == len(revisions), 'DUPLICATE_REVISION')
        # Verify the per-target revision chain, including terminal histories after restart.
        latest = {}
        for entry in entries:
            intent, state = entry['intent'], entry['state']
            key = intent['target'].casefold()
            require(intent['priorRevision'] == latest.get(key, 0), 'REVISION_CHAIN_MISMATCH')
            if state in ('COMMITTED', 'ROLLED_BACK'):
                latest[key] = intent['revision']
        if resolved:
            require(all(e['state'] in ('COMMITTED', 'ROLLED_BACK', 'ABORTED') for e in entries), 'METADATA_RECOVERY_REQUIRED')
        return entries

    @staticmethod
    def _latest(entries, name):
        return next((e for e in reversed(entries) if e['intent']['target'].casefold() == name.casefold()
                     and e['state'] in ('COMMITTED', 'ROLLED_BACK')), None)

    def _matches(self, path, identity, expected):
        if verified_absent(path, self.backend):
            return False
        data = self.backend.read(path, 'metadata')
        return object_identity(self.backend.validate(path, 'metadata')) == identity and digest(data) == expected

    @staticmethod
    def _receipt(intent):
        return {key: intent[key] for key in ('transaction', 'revision', 'target', 'backup',
                                            'before', 'after', 'beforeIdentity', 'afterIdentity', 'store')}

    def _event(self, intent, filename, state):
        self.backend.create(self.root / 'metadata' / 'transactions' / intent['transaction'] / filename,
                            encode({'transaction': intent['transaction'], 'revision': intent['revision'], 'state': state}), 'credential')

    def _replace_metadata(self, name, data, entries, operation='COMMITTED'):
        folder = self.root / 'metadata'
        path = folder / name
        before = self.backend.read(path, 'metadata')
        before_identity = object_identity(self.backend.validate(path, 'metadata'))
        previous = self._latest(entries, name)
        if previous:
            require(self._matches(path, previous['intent']['afterIdentity'], previous['intent']['after']), 'METADATA_POSTSTATE_CHANGED')
        transaction = uuid.uuid4().hex
        revision = max((e['intent']['revision'] for e in entries), default=0) + 1
        journal = folder / 'transactions' / transaction
        self.backend.mkdir(journal)
        temporary, backup = folder / ('pending-' + transaction + '.json'), folder / ('backup-' + transaction + '.json')
        self.backend.create(temporary, data, 'metadata')
        intent = {'version': 1, 'transaction': transaction, 'revision': revision,
                  'priorRevision': previous['intent']['revision'] if previous else 0,
                  'operation': operation, 'target': name, 'temporary': temporary.name, 'backup': backup.name,
                  'before': digest(before), 'after': digest(data), 'beforeIdentity': before_identity,
                  'afterIdentity': object_identity(self.backend.validate(temporary, 'metadata')),
                  'store': object_identity(self.backend.validate(self.root, 'directory'))}
        # CREATE_NEW + protected immutable descriptor + FlushFileBuffers/fsync.
        # If persistence is incomplete, the transaction directory itself blocks writers.
        self.backend.create(journal / 'intent.json', encode(intent), 'credential')
        require(decode(self.backend.read(journal / 'intent.json', 'credential')) == intent, 'INTENT_PERSISTENCE_FAILED')
        try:
            self.backend.replace(path, temporary, backup)
            require(self._matches(path, intent['afterIdentity'], intent['after'])
                    and self._matches(backup, intent['beforeIdentity'], intent['before'])
                    and verified_absent(temporary, self.backend), 'REPLACE_POSTSTATE_UNCERTAIN')
            self._event(intent, 'terminal.json', operation)
        except (OSError, SecurityError):
            # Intent is already sufficient to block; if this event also fails,
            # PREPARED remains unresolved. Never erase backup/temporary evidence.
            try:
                self._event(intent, 'recovery-required.json', 'RECOVERY_REQUIRED')
            except (OSError, SecurityError):
                pass
            raise SecurityError('METADATA_RECOVERY_REQUIRED') from None
        return self._receipt(intent)

    def update_metadata(self, name, value):
        name = metadata_leaf(name)
        metadata_value(value)
        self.validate()
        with self._metadata_lock():
            return self._replace_metadata(name, encode(value), self._history(resolved=True))

    def rollback_metadata(self, receipt):
        require(isinstance(receipt, dict) and isinstance(receipt.get('transaction'), str)
                and bool(re.fullmatch('[a-f0-9]{32}', receipt['transaction'])), 'INVALID_RECEIPT')
        metadata_leaf(receipt.get('target'))
        self.validate()
        with self._metadata_lock():
            entries = self._history(resolved=True)
            entry = next((e for e in entries if e['intent']['transaction'] == receipt['transaction']), None)
            require(entry is not None and receipt == self._receipt(entry['intent'])
                    and entry['state'] in ('COMMITTED', 'ROLLED_BACK'), 'INVALID_RECEIPT')
            require(self._latest(entries, receipt['target']) == entry, 'ROLLBACK_STALE_REVISION')
            folder = self.root / 'metadata'
            path, backup = folder / receipt['target'], folder / entry['intent']['backup']
            require(self._matches(path, receipt['afterIdentity'], receipt['after']), 'ROLLBACK_POSTSTATE_CHANGED')
            require(self._matches(backup, receipt['beforeIdentity'], receipt['before']), 'ROLLBACK_BACKUP_CHANGED')
            value = decode(self.backend.read(backup, 'metadata'))
            metadata_value(value)
            # New revision; never reuse the old backup as a mutable replacement.
            return self._replace_metadata(receipt['target'], encode(value), entries, 'ROLLED_BACK')

    def reconcile_metadata(self, transaction, decision):
        """Explicit observation-only reconciliation; no automatic restore or evidence deletion.

        accept: exact completed replace. abort: provably unchanged target with
        exact pending object and no backup. Other partial outcomes remain blocked.
        No CLI entry point and no real-store call is authorized by Phase 1.
        """
        require(isinstance(transaction, str) and bool(re.fullmatch('[a-f0-9]{32}', transaction))
                and decision in ('accept', 'abort'), 'INVALID_RECONCILIATION')
        self.validate()
        with self._metadata_lock(reconcile=True):
            entries = self._history()
            entry = next((e for e in entries if e['intent']['transaction'] == transaction), None)
            require(entry is not None, 'UNKNOWN_TRANSACTION')
            require(all(e is entry or e['state'] in ('COMMITTED', 'ROLLED_BACK', 'ABORTED') for e in entries), 'OTHER_UNRESOLVED_TRANSACTION')
            intent = entry['intent']
            require(not any(e['intent']['target'].casefold() == intent['target'].casefold()
                            and e['intent']['revision'] > intent['revision'] for e in entries), 'RECONCILIATION_STALE_REVISION')
            folder = self.root / 'metadata'
            target, temporary, backup = (folder / intent[key] for key in ('target', 'temporary', 'backup'))
            if decision == 'accept':
                require(self._matches(target, intent['afterIdentity'], intent['after'])
                        and self._matches(backup, intent['beforeIdentity'], intent['before'])
                        and verified_absent(temporary, self.backend), 'RECONCILIATION_UNPROVEN')
                state = intent['operation']
            else:
                require(entry['state'] in ('PREPARED', 'RECOVERY_REQUIRED')
                        and self._matches(target, intent['beforeIdentity'], intent['before'])
                        and self._matches(temporary, intent['afterIdentity'], intent['after'])
                        and verified_absent(backup, self.backend), 'RECONCILIATION_UNPROVEN')
                state = 'ABORTED'
            if entry['state'] in ('PREPARED', 'RECOVERY_REQUIRED'):
                self._event(intent, 'resolution.json', state)
            else:
                require(entry['state'] == state, 'RECONCILIATION_UNPROVEN')
            return state

    def active_paths(self, required):
        pointer = self.metadata('active.json')
        require(isinstance(pointer, dict) and set(pointer) == {'generation'}, 'INVALID_ACTIVE_POINTER')
        path, manifest = self.generation(pointer['generation'])
        require(set(required) <= set(manifest['files']), 'REQUIRED_FILE_MISSING')
        for name, fields in [('demo-settings.json', DEMO_FIELDS), ('postgres-settings.json',
                            ('bootstrapPassword', 'migratorPassword', 'runtimePassword'))]:
            if name not in required:
                continue
            value = decode(self.backend.read(path / name, 'credential'))
            require(isinstance(value, dict) and all(isinstance(value.get(k), str) and
                    0 < len(value[k]) <= 8192 for k in fields), 'INVALID_SETTINGS_SCHEMA')
            if name == 'postgres-settings.json':
                for kind in ('bootstrap', 'migrator', 'runtime'):
                    password = self.backend.read(path / f'{kind}-password.txt', 'credential')
                    require(password == value[f'{kind}Password'].encode('utf-8'), 'PASSWORD_FILE_MISMATCH')
        return {name: str(path / name) for name in required}

    def cleanup_synthetic(self):
        require(self.fixture, 'SYNTHETIC_ONLY')
        expected_parent = self.backend.known_folder() if os.name == 'nt' else Path(tempfile.gettempdir())
        require(self.root.parent == expected_parent and bool(re.fullmatch('ELogistics-Credential-Test-[a-f0-9]{32}', self.root.name)), 'CLEANUP_SCOPE_MISMATCH')
        self.validate()
        current_root = self.backend.inspect(self.root)
        require(hasattr(self, 'created_identity') and all(current_root[k] == self.created_identity[k]
                for k in ('volume', 'file')), 'CLEANUP_ROOT_REPLACED')
        # Capture exact identities before deletion; no recursive delete API.
        # Inspect BEFORE descending: never follow a junction during cleanup.
        paths, pending = [self.root], [self.root]
        while pending:
            for path in pending.pop().iterdir():
                info = self.backend.inspect(path)
                paths.append(path)
                if info['directory']:
                    pending.append(path)
        captured = []
        for path in paths:
            info = self.backend.inspect(path)
            require(info['owner'] == (self.backend.sid if os.name == 'nt' else os.getuid()), 'CLEANUP_OWNER_CHANGED')
            captured.append((path, {k: info[k] for k in ('volume', 'file', 'directory', 'size', 'write')}))
        for path, identity in sorted(captured, key=lambda pair: (pair[1]['directory'], -len(pair[0].parts))):
            # Directory timestamps/sizes change as own children are removed.
            current = self.backend.inspect(path)
            require(all(current[k] == identity[k] for k in ('volume', 'file', 'directory')), 'CLEANUP_IDENTITY_CHANGED')
            self.backend.remove_owned(path, {k: current[k] for k in ('volume', 'file', 'directory', 'size', 'write')})


def namespace_assessment(path, backend, allow_missing=False):
    """Read-only metadata assessment, not a namespace lease or migration approval."""
    result = backend.presence(path)
    if result.state == 'UNKNOWN':
        return 'UNSAFE' if result.category in ('UNSAFE_NAMESPACE', 'UNSAFE_OBJECT') else 'UNVERIFIED'
    if result.state == 'ABSENT':
        return 'SAFE_EXISTING_ANCESTORS' if allow_missing else 'UNVERIFIED'
    try:
        with backend.pin(path):  # Include the existing final directory's policy.
            pass
        return 'SAFE_EXISTING_ANCESTORS'
    except SecurityError:
        return 'UNSAFE'
    except OSError:
        return 'UNVERIFIED'


def migration_dry_run(repository, backend=None, expected_owners=None):
    """Allowlisted metadata only. No secret read, hash, write, subprocess or Docker."""
    backend = backend or Windows()
    repository = Path(repository).absolute()
    target = production_root(backend)
    # Optional expectations come from an explicitly reviewed private inventory;
    # reusable policy never hardcodes a machine SID or adopts an observed owner.
    expected_owners = expected_owners or {}
    default_owner = backend.sid if isinstance(backend, Windows) else os.getuid()
    target_presence, parent_presence = backend.presence(target), backend.presence(target.parent)
    if 'PRESENT' in (target_presence.state, parent_presence.state):
        target_collision = True  # Preserve conservative no-adoption of existing parent.
    elif target_presence.state == parent_presence.state == 'ABSENT':
        target_collision = False
    else:
        target_collision = 'UNKNOWN'
    target_safety = namespace_assessment(target, backend, allow_missing=True)
    mapping = [('.demo/settings.json', FILES[0]), ('.demo/postgres/settings.json', FILES[1]),
               *((f'.demo/postgres/{kind}-password.txt', f'{kind}-password.txt') for kind in ('bootstrap', 'migrator', 'runtime')),
               *((f'.demo/postgres/test-run-{suffix}.json', None) for suffix in HISTORICAL)]
    rows = []
    for relative, destination in mapping:
        path = repository / relative
        source_presence = backend.presence(path)
        row = {'source': relative, 'category': 'active' if destination else 'historical',
               'target': 'development/generations/<pending>/' + destination if destination else 'OWNER_RETENTION_DECISION_REQUIRED',
               'retirement': 'OWNER_ACTION_REQUIRED' if relative == '.demo/settings.json' else 'NOT_AUTHORIZED',
               'status': 'UNVERIFIED', 'inspectionStatus': 'UNVERIFIED', 'ownerCompatibility': 'UNVERIFIED',
               'sourcePresence': source_presence.record(),
               'namespaceSafety': namespace_assessment(path.parent, backend), 'targetCollision': target_collision,
               'migrationEligibility': 'BLOCKED',
               'requiredOwnerAction': 'OWNER_ACTION_REQUIRED' if relative == '.demo/settings.json' else
                                      ('HISTORICAL_RETENTION_DECISION_REQUIRED' if not destination else 'NONE'),
               'blockers': []}
        if source_presence.state == 'ABSENT':
            row.update(status='MISSING', inspectionStatus='MISSING')
        elif source_presence.state == 'PRESENT' or source_presence.category == 'UNSAFE_NAMESPACE':
            # Explicit legacy allowlist may still collect accessible owner/ACL
            # metadata in an unsafe namespace. This NEVER upgrades presence or
            # eligibility, and query-error UNKNOWN never reaches this inspection.
            try:
                # Independent metadata snapshots only; deliberately no claim of pinning.
                for ancestor in reversed([path.parent, *path.parent.parents]):
                    backend.inspect(ancestor)
                info = backend.inspect(path)
                require(not info['directory'], 'FILE_REQUIRED')
                row.update(status='METADATA_ONLY_NO_MIGRATION', inspectionStatus='INSPECTED_METADATA_ONLY', identity=info,
                           ownerCompatibility='EXPECTED_OWNER' if info['owner'] == expected_owners.get(relative, default_owner)
                                              else 'UNEXPECTED_OWNER')
            except (SecurityError, OSError):
                row['status'] = 'UNSAFE_OR_INACCESSIBLE'
                row['inspectionStatus'] = 'UNSAFE_OR_INACCESSIBLE'
        for condition, reason in (
                (row['inspectionStatus'] != 'INSPECTED_METADATA_ONLY', 'SOURCE_INSPECTION_INCOMPLETE'),
                (source_presence.state == 'UNKNOWN', 'SOURCE_PRESENCE_UNVERIFIED'),
                (row['ownerCompatibility'] != 'EXPECTED_OWNER', 'OWNER_INCOMPATIBLE_OR_UNVERIFIED'),
                (row['namespaceSafety'] != 'SAFE_EXISTING_ANCESTORS', 'SOURCE_NAMESPACE_UNSAFE_OR_UNVERIFIED'),
                (target_safety != 'SAFE_EXISTING_ANCESTORS', 'TARGET_NAMESPACE_UNSAFE_OR_UNVERIFIED'),
                (target_collision is True, 'TARGET_COLLISION'),
                (target_presence.state == 'UNKNOWN' or parent_presence.state == 'UNKNOWN', 'TARGET_PRESENCE_UNVERIFIED'),
                (row['requiredOwnerAction'] != 'NONE', row['requiredOwnerAction'])):
            if condition:
                row['blockers'].append(reason)
        if not row['blockers']:
            row['migrationEligibility'] = 'UNVERIFIED_RUNTIME_AND_AUTHORIZATION'
        rows.append(row)
    references = []
    for relative in ('app/backend/run-demo.ps1', 'app/backend/run-postgres-demo.ps1',
                     'infra/postgres/start-local.ps1', 'app/backend/verify-postgres.ps1',
                     'app/backend/smoke-demo.py', 'infra/foundation/manage.py',
                     'app/frontend/web/tests/support/settings.ts', 'infra/compose/postgres.yaml'):
        if (repository / relative).is_file():
            references.append(relative)
    return {'writeOperations': 0, 'contentReads': 0, 'activeCount': 5, 'historicalCount': 7,
            'targetCollision': target_collision, 'rows': rows, 'consumerReview': references,
            'targetNamespaceSafety': target_safety,
            'targetPresence': target_presence.record(), 'targetParentPresence': parent_presence.record(),
            'migrationEligibility': 'BLOCKED',
            'blockers': ['REAL_MIGRATION_NOT_AUTHORIZED', 'CONSUMER_BINDINGS_UNVERIFIED', 'POWER_LOSS_DURABILITY_UNVERIFIED'],
            'dockerConsumers': 'UNVERIFIED_NO_DOCKER_EXECUTION', 'realMigration': 'NOT_AUTHORIZED',
            'ownerAction': 'ROOT_SETTINGS_RETIREMENT_REQUIRED'}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['paths', 'dry-run', 'test-record-path'])
    parser.add_argument('--mode', choices=['demo', 'postgres'], default='demo')
    parser.add_argument('--session')
    args = parser.parse_args(argv)
    try:
        if args.command == 'dry-run':
            result = migration_dry_run(Path(__file__).resolve().parents[2])
        elif args.command == 'test-record-path':
            result = str(Store.open_development().test_record_path(args.session))
        else:
            required = FILES[:1] if args.mode == 'demo' else FILES
            result = Store.open_development().active_paths(required)
        print(json.dumps(result, sort_keys=True))
        return 0
    except (SecurityError, OSError, ValueError, KeyError, TypeError):
        print('PROTECTED_STORE_UNAVAILABLE_OR_INVALID; no fallback; inspect private metadata; migration not authorized', file=sys.stderr)
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
