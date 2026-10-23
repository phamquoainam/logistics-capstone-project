"""Bounded source/negative controls, not Java compilation, JCE execution or SQL enforcement.

Fixed repository source reads + stdlib only. No subprocess, JVM, network, DB, credentials or output files.
"""
import ast
import hashlib
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / 'app/backend/src/main/java/com/elogistic/backend/identity/internal'
SQL = ROOT / 'app/backend/src/main/resources/db/migration/V5__identity_invitation_outbox.sql'
IMMUTABLE = {
    'V1__ownership_schemas.sql': 'fc634d0cc30aeda75f894dd78aa4e987ba5feacccf4f30ce86a72e539abe9cca',
    'V2__demo_identity_merchant_drafts.sql': '16bea5ac1a9f9e8df8d2ef8d5d11110f95ddd69b86a0c8a22aca648d1251d199',
    'V3__production_identity.sql': 'c870bc7fa2225e7497534caf717ce6ad8d0f51c8fffbca272f8c5bdaf84985fb',
    'V4__identity_system_realm.sql': 'c2254b9fc8f292e171f4fde49cda15c38c63c940b1c74a2ccae780ec94643062',
}


def schema_findings(sql):
    code = re.sub(r'--[^\n]*', '', sql)
    required = {
        'one-owned-table': 'CREATE TABLE identity.invitation_outbox',
        'unique-proof': 'proof_id UUID NOT NULL UNIQUE',
        'subject-binding': 'REFERENCES identity.action_proof(id,account_id,purpose,created_at,expires_at,verifier_key_id)',
        'realm-binding': 'realm_id UUID NOT NULL REFERENCES identity.realm(id)',
        'protected-size': 'octet_length(ciphertext)=59',
        'nonce-size': 'octet_length(nonce)=12',
        'ttl': "expires_at=created_at+INTERVAL '24 hours'",
        'lease': 'CONSTRAINT ck_identity_outbox_lease CHECK',
        'terminal-purge': "status IN ('SENT','FAILED','EXPIRED') AND nonce IS NULL AND ciphertext IS NULL",
        'bounded-attempts': 'attempts BETWEEN 0 AND 10',
        'public-deny': 'REVOKE ALL ON TABLE identity.invitation_outbox FROM PUBLIC',
        'runtime-dml': 'GRANT SELECT,INSERT,UPDATE ON identity.invitation_outbox TO "${runtimeRole}"',
    }
    findings = {name for name, value in required.items() if value not in code}
    if re.search(r'\b(?:raw_token|token|invitation_link|payload)\s+(?:TEXT|VARCHAR|JSONB)', code, re.I):
        findings.add('plaintext-column')
    if re.search(r'\b(?:DROP|DELETE|TRUNCATE|SECURITY\s+DEFINER|IF\s+NOT\s+EXISTS)\b', code, re.I):
        findings.add('unsafe-ddl')
    if set(re.findall(r'CREATE TABLE (\w+\.\w+)', code)) != {'identity.invitation_outbox'}:
        findings.add('table-inventory')
    if set(re.findall(r'REFERENCES (\w+)\.', code)) != {'identity'}:
        findings.add('ownership')
    return findings


class InvitationOutboxSourceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sql = SQL.read_text(encoding='utf-8')
        cls.crypto = (MAIN/'infrastructure/InvitationProtection.java').read_text(encoding='utf-8')
        cls.worker = (MAIN/'infrastructure/JdbcInvitationOutbox.java').read_text(encoding='utf-8')
        cls.app = (MAIN/'application/ProvisionIdentityAccount.java').read_text(encoding='utf-8')
        cls.issuer = (MAIN/'infrastructure/SecureInvitationIssuer.java').read_text(encoding='utf-8')

    def test_v1_through_v4_bytes_remain_immutable(self):
        for name, digest in IMMUTABLE.items():
            with self.subTest(migration=name):
                self.assertEqual(digest, hashlib.sha256((SQL.parent/name).read_bytes()).hexdigest())

    def test_schema_controls_are_present_without_plaintext_or_destructive_ddl(self):
        self.assertEqual(set(), schema_findings(self.sql))

    def test_each_schema_security_mutation_is_rejected(self):
        for name, old, new in (
            ('unique-proof','proof_id UUID NOT NULL UNIQUE','proof_id UUID NOT NULL'),
            ('subject-binding','REFERENCES identity.action_proof(id,account_id,purpose,created_at,expires_at,verifier_key_id)','REFERENCES identity.action_proof(id)'),
            ('ttl',"expires_at=created_at+INTERVAL '24 hours'","expires_at=created_at+INTERVAL '48 hours'"),
            ('protected-size','octet_length(ciphertext)=59','octet_length(ciphertext)>0'),
            ('terminal-purge',"status IN ('SENT','FAILED','EXPIRED') AND nonce IS NULL AND ciphertext IS NULL","status IN ('SENT','FAILED','EXPIRED')"),
            ('bounded-attempts','attempts BETWEEN 0 AND 10','attempts>=0'),
            ('runtime-dml','GRANT SELECT,INSERT,UPDATE ON','GRANT ALL ON'),
            ('public-deny','REVOKE ALL ON TABLE identity.invitation_outbox FROM PUBLIC',''),
            ('plaintext-column','destination TEXT','raw_token TEXT, destination TEXT'),
            ('ownership','REFERENCES identity.realm(id)','REFERENCES merchant.realm(id)'),
        ):
            with self.subTest(control=name):
                self.assertIn(old, self.sql)
                self.assertIn(name, schema_findings(self.sql.replace(old, new)))
        self.assertEqual(set(), schema_findings('-- comment-only positive control\n'+self.sql))

    def test_issuer_uses_jce_csprng_hmac_aead_and_constant_time_comparison(self):
        for snippet in ('new SecureRandom()', 'new byte[32]', 'new byte[12]', 'withoutPadding().encode(entropy)',
                        'Mac.getInstance("HmacSHA256")', 'Cipher.getInstance("AES/GCM/NoPadding")',
                        'new GCMParameterSpec(128,nonce)', 'cipher.updateAAD(', 'MessageDigest.isEqual(actual,expected)',
                        'wipe(entropy);wipe(token)', 'keyId,supplied'):
            self.assertIn(snippet, self.crypto)
        self.assertNotRegex(self.crypto, r'new Random\(|System\.getenv|System\.getProperty|new String\(token|Logger|System\.out')
        self.assertIn('throw invalid()', self.crypto)
        self.assertIn('MessageDigest.isEqual(a,b)', self.crypto)

    def test_provision_replay_precedes_generation_and_all_effects_share_transaction(self):
        order = ['return store.transaction(', 'var receipt=tx.receipt(', 'if(receipt.isPresent()) {',
                 'if(invitations==null)', 'invitations.prepare(pending,input.email(),now)', 'tx.insertPending(',
                 'tx.appendProof(', 'issued.appendDeliveryIntent()', 'tx.appendAudit(', 'tx.appendReceipt(']
        positions = [self.app.index(value) for value in order]
        self.assertEqual(sorted(positions), positions)
        self.assertIn('ChronoUnit.MICROS', self.app)
        self.assertIn('Arrays.fill(hash,(byte)0)', self.app)
        self.assertIn('sealed.close()', self.issuer)

    def test_enqueue_requires_actual_writable_same_datasource_transaction(self):
        for snippet in ('isActualTransactionActive()', 'isCurrentTransactionReadOnly()', 'hasResource(dataSource)',
                        'INVITATION_SAME_TRANSACTION_REQUIRED', 'nonce,ciphertext', 'InvitationProtection.wipe(ciphertext)'):
            self.assertIn(snippet, self.worker)
        enqueue = self.worker.split('void enqueue(', 1)[1].split('// Current state', 1)[0]
        self.assertNotIn('token()', enqueue)
        self.assertNotIn('tokenHash()', enqueue)

    def test_worker_has_current_state_admission_leases_bounded_purge_and_no_external_io(self):
        for snippet in ('tx.lockAdministration()', 'FOR UPDATE OF o SKIP LOCKED', 'LIMIT 100',
                        'p.consumed_at IS NULL AND p.revoked_at IS NULL', "a.status='PENDING_ACTIVATION'",
                        "r.status='ACTIVE'", 'a.email=o.destination', 'o.lease_until<=?',
                        'lease_id=?', 'lease_until>?', 'nonce=NULL,ciphertext=NULL', 'Outcome.TRANSIENT_FAILURE',
                        'policy.maxAttempts()', 'protection.matches(', 'protection.open('):
            self.assertIn(snippet, self.worker)
        self.assertNotRegex(self.worker, r'(?i)DELETE FROM|TRUNCATE |UPDATE identity\.(?:user_account|action_proof|account_grant)')
        self.assertNotRegex(self.worker+self.issuer, r'(?i)java\.net|smtp|RabbitTemplate|RedisTemplate|@Scheduled')

    def test_public_api_and_domain_do_not_expose_delivery_or_crypto_infrastructure(self):
        for root in (MAIN/'domain', MAIN.parent/'api'):
            for path in root.glob('*.java'):
                self.assertNotRegex(path.read_text(encoding='utf-8'), r'import .*InvitationProtection|import .*JdbcInvitationOutbox|import .*SecureInvitationIssuer')
        self.assertNotIn('new Thread(', self.worker)

    def test_crypto_and_outbox_test_sources_have_meaningful_assertions(self):
        tests = ROOT/'app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure'
        unit = (tests/'InvitationProtectionTests.java').read_text(encoding='utf-8')
        pg = (tests/'IdentityGrantApplicationTests.java').read_text(encoding='utf-8')
        self.assertEqual(10, len(re.findall(r'@Test\s+void', unit)))
        self.assertEqual(15, len(re.findall(r'@Test\s+void\s+secure', pg)))
        for source in (unit, pg):
            self.assertIn('assertThrows(', source); self.assertIn('assertFalse(', source)
            self.assertNotIn('@Disabled', source)
        self.assertIn('new SecureInvitationIssuer(', pg)
        self.assertIn('ProductionIdentityProvisionComposition.secure(', pg)
        self.assertIn('secureProvisionEveryMandatoryPersistenceFailureRollsBack', pg)

    def test_report_inventory_is_finite_and_parses_without_execution(self):
        tree = ast.parse((ROOT/'scripts/ci/verify-reports.py').read_text(encoding='utf-8'))
        assignments = {n.targets[0].id:n.value for n in tree.body if isinstance(n, ast.Assign) and isinstance(n.targets[0], ast.Name)}
        values = ast.literal_eval(assignments['IDENTITY_INVITATION_PG_REQUIRED'])
        self.assertEqual(15, len(values));self.assertEqual(len(values),len(set(values)))
        self.assertTrue(all(value.startswith('secure') for value in values))


if __name__ == '__main__':
    unittest.main(verbosity=2)
