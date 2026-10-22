"""T-002-03 lexical/source controls, NOT PostgreSQL parsing or enforcement.

Stdlib only; fixed repository reads, no subprocess/network/credentials/DB writes.
Run from repository root: python -B app/backend/test_identity_migration.py
"""
import hashlib
import json
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
MIGRATIONS = ROOT / 'app/backend/src/main/resources/db/migration'
SQL_PATH = MIGRATIONS / 'V3__production_identity.sql'
TABLES = {'user_account', 'account_grant', 'auth_session', 'refresh_token',
          'action_proof', 'csrf_binding', 'command_receipt', 'audit_entry'}
CONTRACT_SHA = 'acf92380c325698a1370a2ade6accff82bb338c4524f6328a9613d3e4b7b6128'
IMMUTABLE = {
    'app/backend/src/main/resources/db/migration/V1__ownership_schemas.sql':
        'fc634d0cc30aeda75f894dd78aa4e987ba5feacccf4f30ce86a72e539abe9cca',
    'app/backend/src/main/resources/db/migration/V2__demo_identity_merchant_drafts.sql':
        '16bea5ac1a9f9e8df8d2ef8d5d11110f95ddd69b86a0c8a22aca648d1251d199',
    'app/backend/src/main/resources/db/demo-migration/V1__demo_identity_merchant_drafts.sql':
        'b8c95fb66c89f0bce9f84abdea16cc02657b542ca5e1bef998f420af4a67b8bf',
}


def code(sql):
    # V3 has no dollar-quoted functions. Keep string literals intact while removing comments.
    return re.sub(r"'(?:''|[^'])*'|--[^\n]*|/\*.*?\*/",
                  lambda m: m[0] if m[0].startswith("'") else ' ', sql, flags=re.S)


def findings(sql):
    """Independent source expectations. These do not replace executable DB assertions."""
    text = code(sql)
    tables = dict(re.findall(r'CREATE TABLE identity\.(\w+) \((.*?)\n\);', text, re.S))
    issues = set()
    if set(tables) != TABLES:
        issues.add('table-inventory')
    expected = {
        'username-exact': 'username VARCHAR(80) COLLATE "C" NOT NULL',
        'username-unique': 'CONSTRAINT uq_identity_username UNIQUE (username)',
        'username-nonblank': 'CONSTRAINT ck_identity_username_nonblank CHECK',
        'account-lifecycle': 'CONSTRAINT ck_identity_account_lifecycle CHECK',
        'account-version': 'version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0)',
        'auth-version': 'auth_version BIGINT NOT NULL DEFAULT 1 CHECK (auth_version > 0)',
        'grant-unique': 'PRIMARY KEY (account_id,role_code,scope_type,scope_id)',
        'session-absolute': "absolute_expires_at <= created_at + INTERVAL '8 hours'",
        'session-idle': "idle_expires_at <= last_activity_at + INTERVAL '30 minutes'",
        'refresh-hash': 'token_hash BYTEA NOT NULL UNIQUE CHECK (octet_length(token_hash) = 32)',
        'refresh-family': 'FOREIGN KEY (session_id,predecessor_id)',
        'refresh-parent': 'REFERENCES identity.refresh_token(session_id,id)',
        'refresh-successor': 'CONSTRAINT uq_identity_refresh_successor UNIQUE (predecessor_id)',
        'refresh-live': 'CREATE UNIQUE INDEX uq_identity_refresh_live ON identity.refresh_token(session_id)',
        'refresh-root': 'CREATE UNIQUE INDEX uq_identity_refresh_root ON identity.refresh_token(session_id)',
        'proof-live': 'CREATE UNIQUE INDEX uq_identity_proof_live ON identity.action_proof(account_id,purpose)',
        'proof-ttl': "WHEN 'INVITATION' THEN INTERVAL '24 hours' ELSE INTERVAL '15 minutes' END",
        'csrf-cookie': 'cookie_hash BYTEA NOT NULL UNIQUE CHECK (octet_length(cookie_hash) = 32)',
        'csrf-challenge': 'challenge_hash BYTEA NOT NULL CHECK (octet_length(challenge_hash) = 32)',
        'receipt-key': 'PRIMARY KEY (binding_kind,binding_id,operation,operation_key)',
        'receipt-binding': 'CONSTRAINT ck_identity_receipt_binding CHECK',
        'receipt-preauth': "target_id IS NULL AND operation = 'requestIdentityRecovery'",
        'receipt-hash': 'request_fingerprint BYTEA NOT NULL CHECK (octet_length(request_fingerprint) = 32)',
        'audit-append': 'GRANT SELECT, INSERT ON identity.command_receipt, identity.audit_entry TO "${runtimeRole}"',
        'public-deny': 'identity.command_receipt, identity.audit_entry FROM PUBLIC',
    }
    issues.update(key for key, snippet in expected.items() if snippet not in text)
    if any(schema != 'identity' for schema in re.findall(r'REFERENCES (\w+)\.', text)):
        issues.add('cross-module-fk')
    if re.search(r'\b(DROP|ALTER|TRUNCATE|DELETE|UPDATE|INSERT)\s+(TABLE|SCHEMA|FROM|INTO|identity\.)', text, re.I):
        issues.add('destructive-or-seed')
    if re.search(r'\bIF\s+NOT\s+EXISTS\b', text, re.I):
        issues.add('masked-drift')
    if re.search(r'\b(raw_token|refresh_token_value|password|response_body|payload)\s+(TEXT|VARCHAR|JSONB)', text, re.I):
        issues.add('secret-payload-column')
    return issues


class IdentityMigrationSourceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sql = SQL_PATH.read_text(encoding='utf-8')
        cls.text = code(cls.sql)
        cls.tables = dict(re.findall(r'CREATE TABLE identity\.(\w+) \((.*?)\n\);', cls.text, re.S))
        cls.contract = json.loads((ROOT / 'contracts/openapi/identity-access.yaml').read_text(encoding='utf-8'))

    def test_global_version_inventory_is_exact(self):
        self.assertEqual(['V1__ownership_schemas.sql', 'V2__demo_identity_merchant_drafts.sql',
                          'V3__production_identity.sql', 'V4__identity_system_realm.sql'], sorted(p.name for p in MIGRATIONS.glob('V*.sql')))

    def test_prior_postgres_and_h2_migrations_are_immutable(self):
        for path, expected in IMMUTABLE.items():
            with self.subTest(path=path):
                self.assertEqual(expected, hashlib.sha256((ROOT/path).read_bytes()).hexdigest())

    def test_accepted_contract_unchanged(self):
        self.assertEqual(CONTRACT_SHA, hashlib.sha256((ROOT/'contracts/openapi/identity-access.yaml').read_bytes()).hexdigest())

    def test_required_source_controls(self):
        self.assertEqual(set(), findings(self.sql))

    def test_eight_tables_are_identity_owned_with_primary_keys(self):
        self.assertEqual(TABLES, set(self.tables))
        for name, body in self.tables.items():
            with self.subTest(table=name): self.assertIn('PRIMARY KEY', body)
        self.assertNotIn('ON DELETE CASCADE', self.text)
        self.assertNotIn('CREATE EXTENSION', self.text)

    def test_account_wire_states_and_lengths_match_contract(self):
        schemas = self.contract['components']['schemas']
        for state in schemas['Account']['properties']['status']['enum']:
            self.assertIn("'" + state + "'", self.tables['user_account'])
        self.assertEqual(80, schemas['Username']['maxLength'])
        self.assertIn('display_name VARCHAR(160) NOT NULL', self.tables['user_account'])
        self.assertIn('activated_at TIMESTAMPTZ', self.tables['user_account'])
        self.assertIn('disabled_at TIMESTAMPTZ', self.tables['user_account'])

    def test_scope_reference_set_matches_contract_without_external_foreign_keys(self):
        for scope in self.contract['components']['schemas']['Grant']['properties']['scopeType']['enum']:
            self.assertIn("'" + scope + "'", self.tables['account_grant'])
        self.assertEqual({'identity'}, set(re.findall(r'REFERENCES (\w+)\.', self.text)))

    def test_policy_deadlines_match_accepted_contract(self):
        p = self.contract['x-identity-policy']
        self.assertEqual((28800,1800,86400,900), tuple(p[k] for k in
            ['absoluteSeconds','idleSeconds','invitationSeconds','recoverySeconds']))
        self.assertIn('consumed_at TIMESTAMPTZ', self.tables['refresh_token'])
        self.assertIn('revoked_at TIMESTAMPTZ', self.tables['refresh_token'])

    def test_tokens_and_csrf_are_hashes_not_plaintext(self):
        for table in ['refresh_token','action_proof']:
            self.assertIn('token_hash BYTEA NOT NULL UNIQUE', self.tables[table])
            self.assertIn('octet_length(token_hash) = 32', self.tables[table])
        self.assertNotRegex(self.text, r'\b(raw_token|password|response_body|payload)\s+(TEXT|VARCHAR|JSONB)')

    def test_receipts_cover_exact_idempotent_operation_inventory(self):
        operations = set()
        for path in self.contract['paths'].values():
            for method, operation in path.items():
                if method not in ['get','post','put','patch','delete']: continue
                if any(p.get('name') == 'Idempotency-Key' for p in operation.get('parameters', [])):
                    operations.add(operation['operationId'])
        self.assertEqual(8, len(operations))
        stored = set(re.findall(r"'([a-z]+Identity\w+)'", self.tables['command_receipt']))
        self.assertEqual(operations, stored)
        self.assertIn('proof_id UUID REFERENCES identity.action_proof(id)', self.tables['command_receipt'])
        self.assertIn('preauth_binding_id UUID REFERENCES identity.csrf_binding(id)', self.tables['command_receipt'])

    def test_privileges_are_explicit_and_append_only(self):
        grants = re.findall(r'GRANT (.*?) TO "\$\{runtimeRole\}";', self.text, re.S)
        self.assertEqual(3, len(grants))
        self.assertTrue(all(not any(word in g for word in ['TRUNCATE','ALL','CREATE']) for g in grants))
        self.assertEqual('SELECT, INSERT ON identity.command_receipt, identity.audit_entry', grants[-1])
        self.assertNotIn('DELETE ON identity.user_account', self.text)

    def test_runtime_suite_has_no_developer_fallback_or_skip_annotation(self):
        source = (ROOT/'app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityMigrationTests.java').read_text(encoding='utf-8')
        self.assertEqual(22, len(re.findall(r'@Test void ', source)))
        self.assertIn('@Tag("task-c")', source)
        self.assertIn('PostgresTestcontainersSupport.container()', source)
        self.assertIn('PostgresTestcontainersSupport.verifyRuntime(POSTGRES)', source)
        for forbidden in ['@Disabled', 'Assumptions.', 'PG_DB_URL', 'PG_TEST_DB_URL', '5433', '.clean()', '.repair()']:
            self.assertNotIn(forbidden, source)
        for state in ['23505','23514','23503','23502','42501']:
            self.assertIn('"'+state+'"', source)

    def test_sql_lexical_delimiters_balanced_not_a_postgres_parser(self):
        stripped = re.sub(r"'(?:''|[^'])*'|\"(?:\"\"|[^\"])*\"", '', self.text)
        self.assertNotIn("'", stripped)
        depth = 0
        for char in stripped:
            if char == '(': depth += 1
            elif char == ')': depth -= 1
            self.assertGreaterEqual(depth, 0)
        self.assertEqual(0, depth)
        self.assertTrue(stripped.rstrip().endswith(';'))

    def test_mutations_are_rejected_for_specific_reasons(self):
        mutations = [
            ('username-unique','CONSTRAINT uq_identity_username UNIQUE (username)','CONSTRAINT uq_identity_username CHECK (TRUE)'),
            ('username-exact','username VARCHAR(80) COLLATE "C" NOT NULL','username VARCHAR(80) NOT NULL'),
            ('account-version','version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0)','version BIGINT NOT NULL DEFAULT 1'),
            ('grant-unique','PRIMARY KEY (account_id,role_code,scope_type,scope_id)','PRIMARY KEY (account_id,role_code,scope_type)'),
            ('session-absolute',"absolute_expires_at <= created_at + INTERVAL '8 hours'",'TRUE'),
            ('session-idle',"idle_expires_at <= last_activity_at + INTERVAL '30 minutes'",'TRUE'),
            ('refresh-parent','REFERENCES identity.refresh_token(session_id,id)','REFERENCES identity.refresh_token(id)'),
            ('refresh-live','CREATE UNIQUE INDEX uq_identity_refresh_live','CREATE INDEX uq_identity_refresh_live'),
            ('refresh-root','CREATE UNIQUE INDEX uq_identity_refresh_root','CREATE INDEX uq_identity_refresh_root'),
            ('proof-ttl',"WHEN 'INVITATION' THEN INTERVAL '24 hours' ELSE INTERVAL '15 minutes' END", "WHEN 'INVITATION' THEN INTERVAL '48 hours' ELSE INTERVAL '24 hours' END"),
            ('receipt-preauth',"target_id IS NULL AND operation = 'requestIdentityRecovery'", "target_id IS NOT NULL AND operation = 'requestIdentityRecovery'"),
            ('audit-append','GRANT SELECT, INSERT ON identity.command_receipt, identity.audit_entry','GRANT SELECT, INSERT, UPDATE ON identity.command_receipt, identity.audit_entry'),
            ('cross-module-fk','REFERENCES identity.user_account(id)','REFERENCES merchant.user_account(id)'),
            ('masked-drift','CREATE TABLE identity.user_account','CREATE TABLE IF NOT EXISTS identity.user_account'),
            ('secret-payload-column','event_type VARCHAR(80)','raw_token TEXT, event_type VARCHAR(80)'),
        ]
        for expected, old, new in mutations:
            with self.subTest(mutation=expected):
                self.assertIn(old, self.sql)
                self.assertIn(expected, findings(self.sql.replace(old, new)))

    def test_nonpolicy_comment_positive_control(self):
        self.assertEqual(set(), findings('-- synthetic reviewer comment\n'+self.sql))


if __name__ == '__main__':
    unittest.main(verbosity=2)
