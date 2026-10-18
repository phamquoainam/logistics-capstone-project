"""Focused synthetic tests. Never initialize/migrate the real project store."""
import contextlib
import ctypes
import errno
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import unittest
import uuid
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).parent))
import credential_store as S
from windows_store import SecurityError, Windows, namespace_safe, READ


@contextlib.contextmanager
def query_fault(backend, path, code):
    """Inject only the raw existence query; policy/consumers still execute."""
    method = '_presence_open' if os.name == 'nt' else '_presence_stat'
    original = getattr(backend, method)
    def query(candidate):
        if (callable(path) and path(candidate)) or candidate == path:
            error = OSError(errno.EIO, 'synthetic-sensitive-query-message')
            error.winerror = code
            raise error
        return original(candidate)
    with patch.object(backend, method, side_effect=query):
        yield


@contextlib.contextmanager
def unsafe_ancestors(backend, paths):
    """Synthetic owner metadata injection; no actual ACL/owner mutation."""
    if os.name == 'nt':
        locations = {}
        original_info, original_security = backend.info, backend.security
        def info(handle, path):
            locations[handle] = path
            return original_info(handle, path)
        def security(handle):
            result = original_security(handle)
            if locations.get(handle) in paths:
                result['owner'] = 'synthetic-untrusted-ancestor'
            return result
        with patch.object(backend, 'info', side_effect=info), patch.object(backend, 'security', side_effect=security):
            yield
    else:
        original = backend._presence_stat
        original_pin = backend.pin
        def stat_result(path):
            result = original(Path(path))  # Plain Path avoids adapter recursion.
            if path in paths:
                fields = list(result); fields[4] = -1
                return os.stat_result(fields)
            return result
        class SyntheticOwnerPath(type(Path())):
            def lstat(self):
                return stat_result(self)
        @contextlib.contextmanager
        def pin(path):
            # Run the real pin policy with the same scoped metadata snapshot.
            # Only this backend instance sees the adapter; Path is not patched.
            with original_pin(SyntheticOwnerPath(path)):
                yield
        with patch.object(backend, '_presence_stat', side_effect=stat_result), \
                patch.object(backend, 'pin', side_effect=pin):
            yield


class NamespaceTests(unittest.TestCase):
    def test_broad_mutation_rejected_but_inherit_only_ignored(self):
        for mask in (2, 4, 0x40, 0x100, 0x10000, 0x40000, 0x80000, 0x10000000):
            with self.subTest(mask=mask), self.assertRaises(SecurityError):
                namespace_safe('dev', [('S-1-5-11', mask, 0, 0)], 'dev')
        namespace_safe('dev', [('S-1-5-11', 0x10000000, 8, 0)], 'dev')

    def test_untrusted_owner_or_ambiguous_ace_rejected(self):
        with self.assertRaises(SecurityError):
            namespace_safe('other', [], 'dev')
        with self.assertRaises(SecurityError):
            namespace_safe('dev', [('dev', READ, 0, 1)], 'dev')

    def test_volume_root_exception_is_creation_only(self):
        namespace_safe('S-1-5-18', [('S-1-5-11', 4, 0, 0)], 'dev', True)
        with self.assertRaises(SecurityError):
            namespace_safe('S-1-5-18', [('S-1-5-11', 0x40, 0, 0)], 'dev', True)

    def test_leaf_rejects_traversal_streams_and_device_names(self):
        for name in ('../x', '..', '/x', 'x/y', 'x\\y', 'x:stream', 'CON.txt', 'NUL', 'x..y'):
            with self.subTest(name=name), self.assertRaises(SecurityError):
                S.leaf(name)

    def test_metadata_rejects_secret_fields_recursively(self):
        with self.assertRaises(SecurityError):
            S.metadata_value({'safe': [{'runtimePassword': 'synthetic'}]})


class StoreTests(unittest.TestCase):
    def setUp(self):
        self.store = S.Store.synthetic()
        self.addCleanup(self.store.cleanup_synthetic)

    def test_exclusive_create_collision_preserves_original(self):
        g = self.store.publish({'demo-settings.json': b'{"marker":"synthetic"}'})
        path, _ = self.store.generation(g)
        with self.assertRaises((SecurityError, OSError)):
            self.store.backend.create(path / 'demo-settings.json', b'overwrite', 'credential')
        self.assertEqual(self.store.read(g, 'demo-settings.json'), b'{"marker":"synthetic"}')

    def test_missing_active_never_reads_legacy(self):
        with self.assertRaises((SecurityError, OSError)):
            self.store.active_paths(S.FILES)

    def test_active_generation_requires_declared_complete_files(self):
        g = self.store.publish({'demo-settings.json': S.encode({k: 'synthetic' for k in S.DEMO_FIELDS})})
        self.store.create_metadata('active.json', {'generation': g})
        self.assertEqual(set(self.store.active_paths(S.FILES[:1])), {S.FILES[0]})
        with self.assertRaises(SecurityError):
            self.store.active_paths(S.FILES)

    def test_settings_schema_rejected_before_consumer_can_log_parser_details(self):
        g = self.store.publish({'demo-settings.json': b'{"tokenSecret":"synthetic-sensitive"}'})
        self.store.create_metadata('active.json', {'generation': g})
        with self.assertRaisesRegex(SecurityError, '^INVALID_SETTINGS_SCHEMA$'):
            self.store.active_paths(S.FILES[:1])

    def test_incomplete_generation_rejected(self):
        g = 'a' * 32
        self.store.backend.mkdir(self.store.root / 'generations' / g)
        with self.assertRaises((SecurityError, OSError)):
            self.store.generation(g)

    def test_corrupted_json_is_sanitized(self):
        with self.assertRaisesRegex(SecurityError, '^INVALID_JSON$'):
            S.decode(b'{synthetic-sensitive-content')

    def test_digest_mismatch_detects_bad_generation(self):
        g = 'b' * 32
        path = self.store.root / 'generations' / g
        self.store.backend.mkdir(path)
        self.store.backend.create(path / 'value.json', b'{}', 'credential')
        self.store.backend.create(path / 'complete.json', S.encode({'version': 1, 'generation': g,
            'files': {'value.json': '0' * 64}}), 'credential')
        with self.assertRaisesRegex(SecurityError, 'CONTENT_MISMATCH'):
            self.store.generation(g)

    def test_metadata_replace_and_guarded_rollback(self):
        self.store.create_metadata('state.json', {'phase': 'before'})
        receipt = self.store.update_metadata('state.json', {'phase': 'after'})
        self.assertEqual(self.store.metadata('state.json'), {'phase': 'after'})
        self.store.rollback_metadata(receipt)
        self.assertEqual(self.store.metadata('state.json'), {'phase': 'before'})

    def test_rollback_refuses_concurrent_poststate_change(self):
        self.store.create_metadata('state.json', {'phase': 1})
        receipt = self.store.update_metadata('state.json', {'phase': 2})
        self.store.update_metadata('state.json', {'phase': 3})
        with self.assertRaisesRegex(SecurityError, 'ROLLBACK_STALE_REVISION'):
            self.store.rollback_metadata(receipt)
        self.assertEqual(self.store.metadata('state.json'), {'phase': 3})

    def test_rollback_refuses_modified_backup(self):
        self.store.create_metadata('state.json', {'phase': 1})
        receipt = self.store.update_metadata('state.json', {'phase': 2})
        folder = self.store.root / 'metadata'
        replacement = folder / 'pending-test.json'
        self.store.backend.create(replacement, S.encode({'phase': 'unexpected'}), 'metadata')
        self.store.backend.replace(folder / receipt['backup'], replacement, folder / 'failed-test.json')
        with self.assertRaisesRegex(SecurityError, 'ROLLBACK_BACKUP_CHANGED'):
            self.store.rollback_metadata(receipt)
        self.assertEqual(self.store.metadata('state.json'), {'phase': 2})

    def test_existing_lock_blocks_writer(self):
        self.store.create_metadata('state.json', {'phase': 1})
        self.store.backend.create(self.store.root / 'metadata' / 'operation.lock', b'{}', 'metadata')
        with self.assertRaises((SecurityError, OSError)):
            self.store.update_metadata('state.json', {'phase': 2})
        self.assertEqual(self.store.metadata('state.json'), {'phase': 1})

    def test_metadata_api_cannot_update_an_immutable_file(self):
        path = self.store.root / 'metadata' / 'immutable.json'
        self.store.backend.create(path, b'{"fixture":true}', 'credential')
        with self.assertRaises(SecurityError):
            self.store.update_metadata('immutable.json', {'fixture': False})
        self.assertEqual(self.store.backend.read(path, 'credential'), b'{"fixture":true}')

    def test_test_record_is_private_session_not_a_permanent_generation(self):
        session = self.store.prepare_test_record({'runtimePassword': 'synthetic-only'})
        path = self.store.test_record_path(session)
        self.assertEqual(S.decode(self.store.backend.read(path, 'credential')), {'runtimePassword': 'synthetic-only'})
        self.assertEqual(list((self.store.root / 'generations').iterdir()), [])
        with self.assertRaises(SecurityError):
            self.store.test_record_path('../other')

    def test_no_live_provisioning_cli(self):
        with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            S.main(['migrate'])

    def test_failure_output_excludes_exception_secret_and_path(self):
        error = io.StringIO()
        with patch.object(S.Store, 'open_development', side_effect=SecurityError('synthetic-secret-sensitive-path')):
            with contextlib.redirect_stderr(error):
                self.assertEqual(S.main(['paths']), 1)
        self.assertNotIn('synthetic-secret', error.getvalue())
        self.assertIn('no fallback', error.getvalue())

    def test_synthetic_dry_run_reads_only_metadata(self):
        repository = self.store.root / 'repository'
        self.store.backend.mkdir(repository)
        self.store.backend.mkdir(repository / '.demo')
        self.store.backend.mkdir(repository / '.demo/postgres')
        self.store.backend.create(repository / '.demo/settings.json', b'not-json-synthetic', 'credential')
        before = self.store.backend.inspect(repository / '.demo/settings.json')
        with patch.object(self.store.backend, 'read', side_effect=AssertionError('content read forbidden')):
            with patch.object(S, 'production_root', return_value=self.store.root / 'absent/target'):
                result = S.migration_dry_run(repository, self.store.backend)
        self.assertEqual(result['contentReads'], 0)
        self.assertEqual(len(result['rows']), 12)
        self.assertEqual(result['rows'][0]['retirement'], 'OWNER_ACTION_REQUIRED')
        self.assertEqual(self.store.backend.inspect(repository / '.demo/settings.json'), before)
        self.assertFalse((self.store.root / 'absent').exists())


class CorrectiveTests(unittest.TestCase):
    setUp = StoreTests.setUp

    def start(self):
        self.store.create_metadata('state.json', {'phase': 'A'})

    def transaction(self):
        entries = self.store._history()
        self.assertEqual(len(entries), 1)
        return entries[0]['intent']['transaction']

    def restarted(self):
        return S.Store(self.store.root, Windows() if os.name == 'nt' else S.PosixFixture(), fixture=True)

    def child(self, body):
        program = ("import sys,os;sys.path.insert(0,sys.argv[1]);import credential_store as S;"
                   "s=S.Store(sys.argv[2],S.Windows() if os.name=='nt' else S.PosixFixture(),fixture=True);" + body)
        return subprocess.run([sys.executable, '-B', '-c', program, str(Path(__file__).parent), str(self.store.root)],
                              capture_output=True, text=True, timeout=20)

    def test_invalid_control_names_rejected_before_any_mutation(self):
        names = ('operation.lock', 'OPERATION.LOCK', 'Operation.Lock', 'operation.lock.', 'operation.lock ',
                 'state.json.', 'state.json ', 'transactions', 'TRANSACTIONS', 'pending-a.json',
                 'BACKUP-a.json', 'failed-a.json', 'transaction-a.json', 'demo-settings.json',
                 'postgres-settings.json', 'runtime-password.txt', 'store.json', 'complete.json',
                 '../x', 'x/y', 'x\\y', 'x:stream', 'NUL.txt', 'COM1.json')
        for name in names:
            with self.subTest(name=name), patch.object(self.store.backend, 'create') as create, \
                    patch.object(self.store.backend, 'mkdir') as mkdir, patch.object(self.store.backend, 'replace') as replace:
                for method in (self.store.create_metadata, self.store.update_metadata):
                    with self.assertRaises(SecurityError):
                        method(name, {})
                create.assert_not_called(); mkdir.assert_not_called(); replace.assert_not_called()

    def test_ordinary_metadata_names_and_case_preserved(self):
        self.store.create_metadata('Status-v1.json', {'phase': 1})
        self.store.update_metadata('Status-v1.json', {'phase': 2})
        self.assertEqual(self.store.metadata('Status-v1.json'), {'phase': 2})

    def test_aba_rollback_denied_after_restart(self):
        self.start()
        receipt = self.store.update_metadata('state.json', {'phase': 'B'})
        self.store.update_metadata('state.json', {'phase': 'C'})
        latest = self.store.update_metadata('state.json', {'phase': 'B'})
        self.assertGreater(latest['revision'], receipt['revision'])
        with self.assertRaisesRegex(SecurityError, 'ROLLBACK_STALE_REVISION'):
            self.restarted().rollback_metadata(receipt)
        self.assertEqual(self.store.metadata('state.json'), {'phase': 'B'})

    def test_identical_backup_bytes_different_identity_rejected(self):
        self.start()
        receipt = self.store.update_metadata('state.json', {'phase': 'B'})
        folder = self.store.root / 'metadata'
        replacement = folder / 'pending-substitution.json'
        self.store.backend.create(replacement, S.encode({'phase': 'A'}), 'metadata')
        self.store.backend.replace(folder / receipt['backup'], replacement, folder / 'failed-substitution.json')
        with self.assertRaisesRegex(SecurityError, 'ROLLBACK_BACKUP_CHANGED'):
            self.store.rollback_metadata(receipt)
        self.assertEqual(self.store.metadata('state.json'), {'phase': 'B'})

    def test_identical_target_bytes_different_identity_rejected(self):
        self.start()
        receipt = self.store.update_metadata('state.json', {'phase': 'B'})
        folder = self.store.root / 'metadata'
        replacement = folder / 'pending-substitution.json'
        self.store.backend.create(replacement, S.encode({'phase': 'B'}), 'metadata')
        self.store.backend.replace(folder / 'state.json', replacement, folder / 'failed-substitution.json')
        with self.assertRaisesRegex(SecurityError, 'ROLLBACK_POSTSTATE_CHANGED'):
            self.store.rollback_metadata(receipt)

    def test_legitimate_rollback_creates_new_durable_revision(self):
        self.start()
        first = self.store.update_metadata('state.json', {'phase': 'B'})
        restored = self.restarted().rollback_metadata(first)
        self.assertGreater(restored['revision'], first['revision'])
        self.assertEqual(self.restarted()._history(resolved=True)[-1]['state'], 'ROLLED_BACK')
        self.assertEqual(self.store.metadata('state.json'), {'phase': 'A'})
        with self.assertRaisesRegex(SecurityError, 'ROLLBACK_STALE_REVISION'):
            self.store.rollback_metadata(first)

    def test_failure_before_intent_never_replaces_and_blocks_unknown_evidence(self):
        self.start()
        original = self.store.backend.create
        def fail(path, data, kind):
            if path.name == 'intent.json':
                raise OSError('synthetic-fault')
            return original(path, data, kind)
        with patch.object(self.store.backend, 'create', side_effect=fail), patch.object(self.store.backend, 'replace') as replace:
            with self.assertRaisesRegex(SecurityError, '^METADATA_IO_FAILED$'):
                self.store.update_metadata('state.json', {'phase': 'B'})
            replace.assert_not_called()
        self.assertEqual(self.store.metadata('state.json'), {'phase': 'A'})
        with self.assertRaisesRegex(SecurityError, 'RECOVERY_REQUIRED_NO_INTENT'):
            self.restarted().update_metadata('state.json', {'phase': 'C'})

    def test_failure_before_replace_abort_consumes_revision_and_allows_update(self):
        self.start()
        with patch.object(self.store.backend, 'replace', side_effect=OSError('synthetic-fault')):
            with self.assertRaisesRegex(SecurityError, '^METADATA_RECOVERY_REQUIRED$'):
                self.store.update_metadata('state.json', {'phase': 'B'})
        transaction = self.transaction()
        with self.assertRaisesRegex(SecurityError, 'METADATA_RECOVERY_REQUIRED'):
            self.store.update_metadata('state.json', {'phase': 'C'})
        self.assertEqual(self.restarted().reconcile_metadata(transaction, 'abort'), 'ABORTED')
        receipt = self.store.update_metadata('state.json', {'phase': 'C'})
        self.assertEqual(receipt['revision'], 2)

    def test_fault_after_applied_replace_preserves_backup_and_requires_acceptance(self):
        self.start()
        original = self.store.backend.replace
        def applied(*args):
            original(*args)
            raise OSError('simulated-return-failure-after-replace')
        with patch.object(self.store.backend, 'replace', side_effect=applied):
            with self.assertRaisesRegex(SecurityError, 'METADATA_RECOVERY_REQUIRED'):
                self.store.update_metadata('state.json', {'phase': 'B'})
        entry = self.store._history()[0]
        self.assertEqual(entry['state'], 'RECOVERY_REQUIRED')
        self.assertTrue((self.store.root / 'metadata' / entry['intent']['backup']).exists())
        with self.assertRaises(SecurityError):
            self.store.create_metadata('other.json', {})
        self.assertEqual(self.restarted().reconcile_metadata(self.transaction(), 'accept'), 'COMMITTED')
        self.store.update_metadata('state.json', {'phase': 'C'})

    def test_partial_replace_missing_target_stays_blocked_and_keeps_evidence(self):
        self.start()
        def partial(target, temporary, backup):
            os.rename(target, backup)
            raise OSError('simulated-partial-rename')
        with patch.object(self.store.backend, 'replace', side_effect=partial):
            with self.assertRaisesRegex(SecurityError, 'METADATA_RECOVERY_REQUIRED'):
                self.store.update_metadata('state.json', {'phase': 'B'})
        intent = self.store._history()[0]['intent']
        for decision in ('accept', 'abort'):
            with self.assertRaisesRegex(SecurityError, 'RECONCILIATION_UNPROVEN'):
                self.restarted().reconcile_metadata(intent['transaction'], decision)
        for key in ('backup', 'temporary'):
            self.assertTrue((self.store.root / 'metadata' / intent[key]).exists())
        with self.assertRaisesRegex(SecurityError, 'METADATA_RECOVERY_REQUIRED'):
            self.store.update_metadata('state.json', {'phase': 'C'})

    def test_postreplace_validation_failure_requires_reconciliation(self):
        self.start()
        with patch.object(self.store, '_matches', side_effect=SecurityError('synthetic-validation-fault')):
            with self.assertRaisesRegex(SecurityError, '^METADATA_RECOVERY_REQUIRED$'):
                self.store.update_metadata('state.json', {'phase': 'B'})
        self.assertEqual(self.restarted().reconcile_metadata(self.transaction(), 'accept'), 'COMMITTED')

    def test_terminal_persistence_failure_preserves_prepared_evidence(self):
        self.start()
        original = self.store._event
        def fail(intent, filename, state):
            if filename in ('terminal.json', 'recovery-required.json'):
                raise OSError('synthetic-event-write-fault')
            return original(intent, filename, state)
        with patch.object(self.store, '_event', side_effect=fail):
            with self.assertRaisesRegex(SecurityError, 'METADATA_RECOVERY_REQUIRED'):
                self.store.update_metadata('state.json', {'phase': 'B'})
        self.assertEqual(self.store._history()[0]['state'], 'PREPARED')
        self.assertEqual(self.restarted().reconcile_metadata(self.transaction(), 'accept'), 'COMMITTED')

    def test_partial_terminal_persistence_blocks_without_deleting_artifact(self):
        self.start()
        original = self.store.backend.create
        def fail(path, data, kind):
            if path.name == 'terminal.json':
                original(path, b'{', kind)
                raise OSError('synthetic-short-event')
            return original(path, data, kind)
        with patch.object(self.store.backend, 'create', side_effect=fail):
            with self.assertRaisesRegex(SecurityError, 'METADATA_RECOVERY_REQUIRED'):
                self.store.update_metadata('state.json', {'phase': 'B'})
        with self.assertRaisesRegex(SecurityError, 'INVALID_JSON'):
            self.restarted().update_metadata('state.json', {'phase': 'C'})
        journals = list((self.store.root / 'metadata/transactions').iterdir())
        self.assertTrue((journals[0] / 'terminal.json').exists())
        self.assertTrue((journals[0] / 'recovery-required.json').exists())

    def test_journal_has_identity_hash_revision_and_no_metadata_payload(self):
        self.start()
        self.store.update_metadata('state.json', {'phase': 'synthetic-private-marker'})
        intent = self.store._history()[0]['intent']
        self.assertEqual(intent['revision'], 1)
        self.assertNotEqual(intent['beforeIdentity'], intent['afterIdentity'])
        data = self.store.backend.read(self.store.root / 'metadata/transactions' / intent['transaction'] / 'intent.json', 'credential')
        self.assertNotIn(b'synthetic-private-marker', data)
        self.assertNotIn(b'"phase"', data)

    def test_other_process_writer_denied_while_lock_held(self):
        self.start()
        with self.store._metadata_lock():
            result = self.child("\ntry:s.update_metadata('state.json',{'phase':'B'})\nexcept (S.SecurityError,OSError):sys.exit(0)\nelse:sys.exit(9)")
        self.assertEqual(result.returncode, 0, 'Synthetic competing writer was not denied')
        self.assertEqual(self.store.metadata('state.json'), {'phase': 'A'})

    def test_reconciler_cannot_claim_live_process_lock(self):
        self.start()
        receipt = self.store.update_metadata('state.json', {'phase': 'B'})
        with self.store._metadata_lock():
            result = self.child(f"\ntry:s.reconcile_metadata('{receipt['transaction']}','accept')\nexcept (S.SecurityError,OSError):sys.exit(0)\nelse:sys.exit(9)")
        self.assertEqual(result.returncode, 0, 'Synthetic live lock was not protected')

    def test_actual_process_exit_leaves_prepared_then_explicit_abort_recovers(self):
        self.start()
        result = self.child("s.backend.replace=lambda *args:os._exit(73);s.update_metadata('state.json',{'phase':'B'})")
        self.assertEqual(result.returncode, 73, 'Synthetic child did not reach bounded exit point')
        self.assertTrue((self.store.root / 'metadata/operation.lock').exists())
        with self.assertRaises((SecurityError, OSError)):
            self.store.update_metadata('state.json', {'phase': 'C'})
        restarted = self.restarted()
        self.assertEqual(restarted.reconcile_metadata(self.transaction(), 'abort'), 'ABORTED')
        self.assertFalse((self.store.root / 'metadata/operation.lock').exists())
        self.assertEqual(restarted.update_metadata('state.json', {'phase': 'C'})['revision'], 2)

    def test_actual_process_exit_after_replace_accepts_exact_poststate(self):
        self.start()
        body = ("original=s.backend.replace\ndef applied(*args):\n original(*args)\n os._exit(74)\n"
                "s.backend.replace=applied\ns.update_metadata('state.json',{'phase':'B'})")
        result = self.child(body)
        self.assertEqual(result.returncode, 74, 'Synthetic child did not finish replace before bounded exit')
        self.assertEqual(self.store._history()[0]['state'], 'PREPARED')
        restarted = self.restarted()
        self.assertEqual(restarted.reconcile_metadata(self.transaction(), 'accept'), 'COMMITTED')
        self.assertEqual(restarted.metadata('state.json'), {'phase': 'B'})
        self.assertEqual(restarted.update_metadata('state.json', {'phase': 'C'})['revision'], 2)

    def test_dry_run_owner_namespace_and_collision_classification(self):
        repository = self.store.root / 'repository'
        for path in (repository, repository / '.demo', repository / '.demo/postgres'):
            self.store.backend.mkdir(path)
        root_file = repository / '.demo/settings.json'
        pg_file = repository / '.demo/postgres/settings.json'
        for path in (root_file, pg_file):
            self.store.backend.create(path, b'synthetic-not-json', 'credential')
        original = self.store.backend.inspect
        def inspected(path):
            info = original(path)
            if path == pg_file:
                info['owner'] = 'synthetic-unexpected-owner'
            return info
        target = self.store.root / 'new-parent/project'
        with patch.object(self.store.backend, 'read', side_effect=AssertionError('content read forbidden')), \
                patch.object(S, 'production_root', return_value=target), patch.object(self.store.backend, 'inspect', side_effect=inspected):
            report = S.migration_dry_run(repository, self.store.backend)
            rows = report['rows']
            self.assertEqual(rows[0]['ownerCompatibility'], 'EXPECTED_OWNER')
            self.assertEqual(rows[0]['requiredOwnerAction'], 'OWNER_ACTION_REQUIRED')
            self.assertEqual(rows[1]['ownerCompatibility'], 'UNEXPECTED_OWNER')
            self.assertIn('OWNER_INCOMPATIBLE_OR_UNVERIFIED', rows[1]['blockers'])
            self.assertEqual(rows[2]['inspectionStatus'], 'MISSING')
            self.assertEqual(rows[2]['ownerCompatibility'], 'UNVERIFIED')
            authorized = S.migration_dry_run(repository, self.store.backend, {'.demo/postgres/settings.json': 'synthetic-unexpected-owner'})
            self.assertEqual(authorized['rows'][1]['ownerCompatibility'], 'EXPECTED_OWNER')
        self.store.backend.mkdir(target.parent)
        with patch.object(S, 'production_root', return_value=target):
            report = S.migration_dry_run(repository, self.store.backend)
        self.assertTrue(report['targetCollision'])
        self.assertIn('TARGET_COLLISION', report['rows'][0]['blockers'])
        self.assertEqual((report['contentReads'], report['writeOperations']), (0, 0))
        self.assertEqual((report['activeCount'], report['historicalCount']), (5, 7))

    def test_dry_run_unsafe_source_and_target_not_reported_safe(self):
        repository = self.store.root / 'repository'
        for path in (repository, repository / '.demo', repository / '.demo/postgres'):
            self.store.backend.mkdir(path)
        self.store.backend.create(repository / '.demo/settings.json', b'synthetic', 'credential')
        target_parent = self.store.root / 'target-parent'
        self.store.backend.mkdir(target_parent)
        source_parent = repository / '.demo'
        self.assertEqual(S.namespace_assessment(source_parent, self.store.backend), 'SAFE_EXISTING_ANCESTORS')
        with unsafe_ancestors(self.store.backend, {repository / '.demo', target_parent}), \
                patch.object(S, 'production_root', return_value=target_parent / 'project'):
            presence = self.store.backend.presence(source_parent / 'missing')
            self.assertEqual((presence.state, presence.category), ('UNKNOWN', 'UNSAFE_NAMESPACE'))
            with self.assertRaisesRegex(SecurityError, 'UNTRUSTED_ANCESTOR_OWNER'):
                with self.store.backend.pin(source_parent):
                    pass
            self.assertEqual(S.namespace_assessment(source_parent, self.store.backend), 'UNSAFE')
            self.assertEqual(S.namespace_assessment(source_parent / 'missing', self.store.backend, allow_missing=True), 'UNSAFE')
            self.assertEqual(S.namespace_assessment(self.store.root, self.store.backend), 'SAFE_EXISTING_ANCESTORS')
            report = S.migration_dry_run(repository, self.store.backend)
        self.assertEqual(report['rows'][0]['namespaceSafety'], 'UNSAFE')
        self.assertEqual(report['targetNamespaceSafety'], 'UNSAFE')
        self.assertIn('TARGET_NAMESPACE_UNSAFE_OR_UNVERIFIED', report['rows'][0]['blockers'])
        self.assertEqual(report['migrationEligibility'], 'BLOCKED')
        self.assertEqual(S.namespace_assessment(source_parent, self.store.backend), 'SAFE_EXISTING_ANCESTORS')

    def test_dry_run_inaccessible_target_is_unverified_without_content_or_writes(self):
        with query_fault(self.store.backend, self.store.root, 5), \
                patch.object(self.store.backend, 'read', side_effect=AssertionError('content forbidden')), \
                patch.object(self.store.backend, 'create', side_effect=AssertionError('write forbidden')), \
                patch.object(self.store.backend, 'mkdir', side_effect=AssertionError('mkdir forbidden')), \
                patch.object(S, 'production_root', return_value=self.store.root / 'absent/project'):
            report = S.migration_dry_run(self.store.root / 'missing-repository', self.store.backend)
        self.assertEqual(report['targetNamespaceSafety'], 'UNVERIFIED')
        self.assertNotIn('synthetic-private-path', json.dumps(report))


class PresenceTests(unittest.TestCase):
    setUp = StoreTests.setUp

    def prepared(self, applied=False):
        self.store.create_metadata('state.json', {'phase': 'A'})
        original = self.store.backend.replace
        def fail(*args):
            if applied:
                original(*args)
            raise OSError('synthetic-replace-fault')
        with patch.object(self.store.backend, 'replace', side_effect=fail):
            with self.assertRaisesRegex(SecurityError, 'METADATA_RECOVERY_REQUIRED'):
                self.store.update_metadata('state.json', {'phase': 'B'})
        return self.store._history()[0]['intent']

    def assert_unresolved(self, intent):
        journal = self.store.root / 'metadata/transactions' / intent['transaction']
        self.assertEqual(self.store.backend.presence(journal / 'resolution.json').state, 'ABSENT')
        self.assertEqual(self.store._history()[0]['state'], 'RECOVERY_REQUIRED')
        with self.assertRaisesRegex(SecurityError, 'METADATA_RECOVERY_REQUIRED'):
            self.store.update_metadata('state.json', {'phase': 'C'})

    def test_present_is_metadata_only_and_absent_requires_verified_ancestors(self):
        backend = self.store.backend
        before = {p.name for p in self.store.root.iterdir()}
        with patch.object(backend, 'read', side_effect=AssertionError('content forbidden')), \
                patch.object(backend, 'create', side_effect=AssertionError('write forbidden')):
            self.assertEqual(backend.presence(self.store.root / 'store.json').state, 'PRESENT')
            result = backend.presence(self.store.root / 'missing-parent/child/file.json')
            self.assertEqual((result.state, result.category), ('ABSENT', 'VERIFIED_NOT_FOUND'))
        self.assertEqual({p.name for p in self.store.root.iterdir()}, before)

    def test_raw_query_access_sharing_io_and_unexpected_errors_are_unknown(self):
        path = self.store.root / 'missing.json'
        for code, category in ((5, 'ACCESS_DENIED'), (32, 'SHARING_VIOLATION'), (1117, 'IO_ERROR'), (87, 'OS_ERROR')):
            with self.subTest(code=code), query_fault(self.store.backend, path, code):
                result = self.store.backend.presence(path)
                self.assertEqual((result.state, result.category, result.code), ('UNKNOWN', category, code))
                with self.assertRaisesRegex(SecurityError, 'PRESENCE_UNKNOWN_' + category):
                    S.verified_absent(path, self.store.backend)
                self.assertNotIn('synthetic-sensitive', json.dumps(result.record()))

    def test_missing_root_is_unknown_and_unavailable_parent_is_not_absence(self):
        path = self.store.root / 'missing-parent/child'
        with query_fault(self.store.backend, Path(path.anchor), 2):
            result = self.store.backend.presence(path)
            self.assertEqual((result.state, result.category), ('UNKNOWN', 'UNVERIFIED_ROOT'))
        with query_fault(self.store.backend, self.store.root / 'missing-parent', 21):
            self.assertEqual(self.store.backend.presence(path).state, 'UNKNOWN')

    def test_unsafe_ancestor_cannot_prove_absence(self):
        path = self.store.root / 'missing-parent/child'
        with unsafe_ancestors(self.store.backend, {self.store.root}):
            result = self.store.backend.presence(path)
            self.assertEqual((result.state, result.category), ('UNKNOWN', 'UNSAFE_NAMESPACE'))

    def test_unknown_target_collision_stops_before_downstream_inspection(self):
        target = self.store.root / 'missing-parent/project'
        for code in (5, 32, 1117):
            with self.subTest(code=code), query_fault(self.store.backend, target.parent, code), \
                    patch.object(S, 'production_root', return_value=target), \
                    patch.object(self.store.backend, 'inspect', side_effect=AssertionError('downstream inspection forbidden')), \
                    patch.object(self.store.backend, 'read', side_effect=AssertionError('content forbidden')), \
                    patch.object(self.store.backend, 'create', side_effect=AssertionError('write forbidden')):
                report = S.migration_dry_run(self.store.root / 'absent-repository', self.store.backend)
            self.assertEqual(report['targetPresence']['state'], 'UNKNOWN')
            self.assertEqual(report['targetCollision'], 'UNKNOWN')
            self.assertEqual(report['targetNamespaceSafety'], 'UNVERIFIED')
            self.assertEqual(report['migrationEligibility'], 'BLOCKED')
            self.assertIn('TARGET_PRESENCE_UNVERIFIED', report['rows'][0]['blockers'])
            self.assertEqual((report['contentReads'], report['writeOperations']), (0, 0))

    def test_collision_positive_and_verified_absence_controls(self):
        present = self.store.root / 'present-project'
        self.store.backend.mkdir(present)
        with patch.object(S, 'production_root', return_value=present):
            report = S.migration_dry_run(self.store.root / 'absent-repository', self.store.backend)
        self.assertEqual(report['targetPresence']['state'], 'PRESENT')
        self.assertIs(report['targetCollision'], True)
        with patch.object(S, 'production_root', return_value=self.store.root / 'absent-parent/project'):
            report = S.migration_dry_run(self.store.root / 'absent-repository', self.store.backend)
        self.assertEqual(report['targetPresence']['state'], 'ABSENT')
        self.assertIs(report['targetCollision'], False)
        self.assertEqual(report['targetNamespaceSafety'], 'SAFE_EXISTING_ANCESTORS')

    def test_unknown_source_query_never_becomes_missing_or_reaches_inspection(self):
        repository = self.store.root / 'repository'
        for path in (repository, repository / '.demo', repository / '.demo/postgres'):
            self.store.backend.mkdir(path)
        source = repository / '.demo/settings.json'
        self.store.backend.create(source, b'synthetic-not-json', 'credential')
        original = self.store.backend.inspect
        def inspected(path):
            self.assertNotEqual(path, source, 'Query UNKNOWN reached downstream inspection')
            return original(path)
        with query_fault(self.store.backend, source, 5), \
                patch.object(self.store.backend, 'inspect', side_effect=inspected), \
                patch.object(self.store.backend, 'read', side_effect=AssertionError('content forbidden')), \
                patch.object(S, 'production_root', return_value=self.store.root / 'absent-parent/project'):
            report = S.migration_dry_run(repository, self.store.backend)
        row = report['rows'][0]
        self.assertEqual(row['sourcePresence']['state'], 'UNKNOWN')
        self.assertEqual(row['inspectionStatus'], 'UNVERIFIED')
        self.assertEqual(row['migrationEligibility'], 'BLOCKED')

    def test_unknown_temporary_during_accept_keeps_recovery_barrier(self):
        intent = self.prepared(applied=True)
        folder = self.store.root / 'metadata'
        backup_before = self.store.backend.inspect(folder / intent['backup'])
        for code in (5, 32, 1117):
            with self.subTest(code=code), query_fault(self.store.backend, folder / intent['temporary'], code), \
                    patch.object(self.store.backend, 'replace') as replace:
                with self.assertRaisesRegex(SecurityError, 'PRESENCE_UNKNOWN_'):
                    self.store.reconcile_metadata(intent['transaction'], 'accept')
                replace.assert_not_called()
            self.assertEqual(self.store.backend.inspect(folder / intent['backup']), backup_before)
            self.assert_unresolved(intent)
        self.assertEqual(self.store.reconcile_metadata(intent['transaction'], 'accept'), 'COMMITTED')
        self.store.update_metadata('state.json', {'phase': 'C'})

    def test_unknown_backup_during_abort_keeps_pending_and_barrier(self):
        intent = self.prepared(applied=False)
        folder = self.store.root / 'metadata'
        pending_before = self.store.backend.inspect(folder / intent['temporary'])
        for code in (5, 32, 1117):
            with self.subTest(code=code), query_fault(self.store.backend, folder / intent['backup'], code), \
                    patch.object(self.store.backend, 'replace') as replace:
                with self.assertRaisesRegex(SecurityError, 'PRESENCE_UNKNOWN_'):
                    self.store.reconcile_metadata(intent['transaction'], 'abort')
                replace.assert_not_called()
            self.assertEqual(self.store.backend.inspect(folder / intent['temporary']), pending_before)
            self.assert_unresolved(intent)
        self.assertEqual(self.store.reconcile_metadata(intent['transaction'], 'abort'), 'ABORTED')
        self.store.update_metadata('state.json', {'phase': 'C'})

    def test_unknown_postreplace_absence_retains_backup_without_terminal_event(self):
        self.store.create_metadata('state.json', {'phase': 'A'})
        with query_fault(self.store.backend, lambda path: path.name.startswith('pending-'), 1117):
            with self.assertRaisesRegex(SecurityError, 'METADATA_RECOVERY_REQUIRED'):
                self.store.update_metadata('state.json', {'phase': 'B'})
        intent = self.store._history()[0]['intent']
        journal = self.store.root / 'metadata/transactions' / intent['transaction']
        self.assertEqual(self.store.backend.presence(journal / 'terminal.json').state, 'ABSENT')
        self.assertEqual(self.store.backend.presence(self.store.root / 'metadata' / intent['backup']).state, 'PRESENT')
        self.assert_unresolved(intent)
        self.assertEqual(self.store.reconcile_metadata(intent['transaction'], 'accept'), 'COMMITTED')

    def test_unknown_backup_collision_prevents_native_replace_and_preserves_objects(self):
        self.store.create_metadata('state.json', {'phase': 'A'})
        folder = self.store.root / 'metadata'
        target, temporary, backup = folder / 'state.json', folder / 'pending-native.json', folder / 'backup-native.json'
        self.store.backend.create(temporary, S.encode({'phase': 'B'}), 'metadata')
        before = [self.store.backend.inspect(path) for path in (target, temporary)]
        for code in (5, 32, 1117):
            native_owner = self.store.backend.k if os.name == 'nt' else os
            native_name = 'ReplaceFileW' if os.name == 'nt' else 'rename'
            with self.subTest(code=code), query_fault(self.store.backend, backup, code), \
                    patch.object(native_owner, native_name) as native:
                with self.assertRaisesRegex(SecurityError, 'PRESENCE_UNKNOWN_'):
                    self.store.backend.replace(target, temporary, backup)
                native.assert_not_called()
        self.assertEqual([self.store.backend.inspect(path) for path in (target, temporary)], before)
        self.assertEqual(self.store.backend.presence(backup).state, 'ABSENT')

    def test_existing_backup_collision_cannot_be_overwritten(self):
        folder = self.store.root / 'metadata'
        for name in ('state.json', 'pending-native.json', 'backup-native.json'):
            self.store.backend.create(folder / name, b'{}', 'metadata')
        native_owner = self.store.backend.k if os.name == 'nt' else os
        native_name = 'ReplaceFileW' if os.name == 'nt' else 'rename'
        with patch.object(native_owner, native_name) as native:
            with self.assertRaisesRegex(SecurityError, '^BACKUP_COLLISION$'):
                self.store.backend.replace(folder / 'state.json', folder / 'pending-native.json', folder / 'backup-native.json')
            native.assert_not_called()

    def test_unknown_backup_before_update_retains_intent_and_blocks_writer(self):
        self.store.create_metadata('state.json', {'phase': 'A'})
        native_owner = self.store.backend.k if os.name == 'nt' else os
        native_name = 'ReplaceFileW' if os.name == 'nt' else 'rename'
        with query_fault(self.store.backend, lambda path: path.name.startswith('backup-'), 5), \
                patch.object(native_owner, native_name) as native:
            with self.assertRaisesRegex(SecurityError, 'METADATA_RECOVERY_REQUIRED'):
                self.store.update_metadata('state.json', {'phase': 'B'})
            native.assert_not_called()
        intent = self.store._history()[0]['intent']
        self.assertEqual(self.store.metadata('state.json'), {'phase': 'A'})
        self.assertEqual(self.store.backend.presence(self.store.root / 'metadata' / intent['temporary']).state, 'PRESENT')
        self.assert_unresolved(intent)
        self.assertEqual(self.store.reconcile_metadata(intent['transaction'], 'abort'), 'ABORTED')


@unittest.skipUnless(os.name == 'nt', 'Windows native security test; not portable evidence')
class WindowsTests(unittest.TestCase):
    setUp = StoreTests.setUp

    def test_presence_security_query_failure_cannot_become_unsafe_audit_fallback(self):
        with patch.object(self.store.backend, 'security', side_effect=SecurityError('SECURITY_QUERY_FAILED')), \
                patch.object(self.store.backend, 'inspect', side_effect=AssertionError('downstream inspection forbidden')), \
                patch.object(S, 'production_root', return_value=self.store.root / 'absent-parent/project'):
            result = self.store.backend.presence(self.store.root / 'missing')
            self.assertEqual((result.state, result.category), ('UNKNOWN', 'NAMESPACE_VERIFICATION_FAILED'))
            report = S.migration_dry_run(self.store.root / 'absent-repository', self.store.backend)
        self.assertEqual(report['targetCollision'], 'UNKNOWN')
        self.assertEqual(report['targetNamespaceSafety'], 'UNVERIFIED')
        self.assertEqual(report['rows'][0]['inspectionStatus'], 'UNVERIFIED')

    def test_presence_postopen_io_error_remains_unknown_with_sanitized_category(self):
        error = OSError(errno.EIO, 'synthetic-sensitive-inspection')
        with patch.object(self.store.backend, 'info', side_effect=error):
            result = self.store.backend.presence(self.store.root / 'missing')
        self.assertEqual((result.state, result.category, result.code), ('UNKNOWN', 'IO_ERROR', errno.EIO))
        self.assertNotIn('synthetic-sensitive', json.dumps(result.record()))

    def test_broad_directory_grant_rejected(self):
        path = self.store.root / 'unsafe-synthetic'
        self.store.backend.mkdir(path)
        result = subprocess.run(['icacls', str(path), '/grant', '*S-1-5-11:(OI)(CI)M'], capture_output=True)
        self.assertEqual(result.returncode, 0)
        with self.assertRaises(SecurityError):
            with self.store.backend.pin(path):
                pass

    def test_known_folder_ignores_environment_override(self):
        backend = Windows()
        expected = backend.known_folder()
        with patch.dict(os.environ, {'LOCALAPPDATA': 'Z:\\synthetic-untrusted'}):
            self.assertEqual(backend.known_folder(), expected)

    def test_protected_dacl_and_denied_credential_rewrite(self):
        g = self.store.publish({'value.txt': b'synthetic'})
        path, _ = self.store.generation(g)
        info = self.store.backend.inspect(path / 'value.txt')
        self.assertTrue(info['protected'])
        self.assertEqual({a[0] for a in info['aces']}, {self.store.backend.sid, 'S-1-5-32-544'})
        with self.assertRaises(PermissionError):
            (path / 'value.txt').write_bytes(b'overwrite')
        self.assertEqual(self.store.read(g, 'value.txt'), b'synthetic')

    def test_existing_directory_never_adopted(self):
        before = self.store.backend.inspect(self.store.root)
        with self.assertRaises(SecurityError):
            self.store.backend.mkdir(self.store.root)
        self.assertEqual(self.store.backend.inspect(self.store.root), before)

    def test_credential_delete_and_native_replace_denied(self):
        generation = self.store.publish({'value.txt': b'synthetic'})
        path, _ = self.store.generation(generation)
        source = path / 'value.txt'
        temporary, backup = path / 'pending.json', path / 'backup.json'
        self.store.backend.create(temporary, b'{}', 'metadata')
        with self.assertRaises(PermissionError):
            source.unlink()
        result = self.store.backend.k.ReplaceFileW(str(source), str(temporary), str(backup), 0, None, None)
        self.assertFalse(result)
        self.assertEqual(ctypes.get_last_error(), 5)
        self.assertEqual(self.store.backend.read(source, 'credential'), b'synthetic')

    def test_reparse_and_hardlink_rejected(self):
        g = self.store.publish({'value.txt': b'synthetic'})
        path, _ = self.store.generation(g)
        # Native directory junction requires no symlink privilege/elevation.
        junction = self.store.backend.known_folder() / ('ELogistics-Credential-Junction-' + uuid.uuid4().hex)
        result = subprocess.run(['cmd', '/c', 'mklink', '/J', str(junction), str(path)], capture_output=True)
        self.assertEqual(result.returncode, 0)
        try:
            with self.assertRaises(SecurityError):
                self.store.backend.validate(junction, 'directory')
            self.assertEqual(self.store.backend.presence(junction).state, 'UNKNOWN')
            self.assertEqual(self.store.backend.presence(junction / 'missing').state, 'UNKNOWN')
        finally:
            junction.rmdir()  # exact owned junction, never traverse/delete target
        # Immutable credentials already deny hard-link creation on this host.
        # Use writable synthetic metadata to exercise the link-count guard.
        self.store.create_metadata('linked.json', {'fixture': True})
        source = self.store.root / 'metadata/linked.json'
        link = self.store.backend.known_folder() / ('ELogistics-Credential-Link-' + uuid.uuid4().hex)
        os.link(source, link)
        try:
            with self.assertRaises(SecurityError):
                self.store.backend.read(source, 'metadata')
            self.assertEqual(self.store.backend.presence(source).state, 'UNKNOWN')
        finally:
            link.unlink()


if __name__ == '__main__':
    unittest.main()
