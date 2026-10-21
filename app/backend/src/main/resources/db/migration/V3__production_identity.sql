-- T-002-03 / accepted Identity contract, DEC-03/04 and O1-O7.
-- PostgreSQL only. Flyway owns the transaction/version stream; do not mask drift.
-- Production accounts are NOT imported from, or authenticated through, demo_account.
-- All IDs are supplied by Identity. External scope IDs have no cross-module FK.
CREATE TABLE identity.user_account (
 id UUID PRIMARY KEY,
 username VARCHAR(80) COLLATE "C" NOT NULL,
 display_name VARCHAR(160) NOT NULL CHECK (char_length(display_name) > 0),
 email TEXT NOT NULL CHECK (char_length(email) > 0),
 password_hash VARCHAR(60),
 status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING_ACTIVATION','ACTIVE','DISABLED')),
 email_verified BOOLEAN NOT NULL DEFAULT FALSE,
 version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
 auth_version BIGINT NOT NULL DEFAULT 1 CHECK (auth_version > 0),
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL,
 activated_at TIMESTAMPTZ,
 disabled_at TIMESTAMPTZ,
 CONSTRAINT uq_identity_username UNIQUE (username),
 -- ECMAScript WhiteSpace + LineTerminator set used by contract Username /\S/.
 -- btrim is validation ONLY; stored identity is never trimmed or normalized.
 CONSTRAINT ck_identity_username_nonblank CHECK (char_length(btrim(username,
   chr(9)||chr(10)||chr(11)||chr(12)||chr(13)||chr(32)||chr(160)||chr(5760)||
   chr(8192)||chr(8193)||chr(8194)||chr(8195)||chr(8196)||chr(8197)||chr(8198)||
   chr(8199)||chr(8200)||chr(8201)||chr(8202)||chr(8232)||chr(8233)||chr(8239)||
   chr(8287)||chr(12288)||chr(65279))) > 0),
 CONSTRAINT ck_identity_password_hash CHECK (password_hash IS NULL OR
   password_hash ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$'),
 CONSTRAINT ck_identity_account_clock CHECK (updated_at >= created_at AND
   (activated_at IS NULL OR activated_at >= created_at) AND
   (disabled_at IS NULL OR disabled_at >= created_at)),
 CONSTRAINT ck_identity_account_lifecycle CHECK (
   (status = 'PENDING_ACTIVATION' AND activated_at IS NULL AND password_hash IS NULL AND NOT email_verified AND disabled_at IS NULL) OR
   (status = 'ACTIVE' AND activated_at IS NOT NULL AND password_hash IS NOT NULL AND email_verified AND disabled_at IS NULL) OR
   (status = 'DISABLED' AND disabled_at IS NOT NULL AND
     ((activated_at IS NULL AND password_hash IS NULL AND NOT email_verified) OR
      (activated_at IS NOT NULL AND password_hash IS NOT NULL AND email_verified))))
);

-- No role catalog seed implies authority. Role-code validity / assignability is
-- default-deny application policy; resource-owner APIs validate public scope IDs.
CREATE TABLE identity.account_grant (
 account_id UUID NOT NULL REFERENCES identity.user_account(id),
 role_code VARCHAR(80) COLLATE "C" NOT NULL CHECK (char_length(role_code) > 0),
 scope_type VARCHAR(10) NOT NULL CHECK (scope_type IN ('IDENTITY','MERCHANT','WAREHOUSE','HUB','ASSIGNMENT')),
 scope_id UUID NOT NULL,
 granted_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY (account_id,role_code,scope_type,scope_id)
);
CREATE INDEX idx_identity_grant_scope ON identity.account_grant(scope_type,scope_id,account_id);

-- One login session IS one refresh family; the family identity is session.id.
-- Effective admission also checks current account/auth_version and revocation.
CREATE TABLE identity.auth_session (
 id UUID PRIMARY KEY,
 account_id UUID NOT NULL REFERENCES identity.user_account(id),
 auth_version BIGINT NOT NULL CHECK (auth_version > 0),
 created_at TIMESTAMPTZ NOT NULL,
 absolute_expires_at TIMESTAMPTZ NOT NULL,
 last_activity_at TIMESTAMPTZ NOT NULL,
 idle_expires_at TIMESTAMPTZ NOT NULL,
 revoked_at TIMESTAMPTZ,
 CONSTRAINT ck_identity_session_deadlines CHECK (
   absolute_expires_at > created_at AND absolute_expires_at <= created_at + INTERVAL '8 hours' AND
   last_activity_at >= created_at AND last_activity_at < absolute_expires_at AND
   idle_expires_at > last_activity_at AND idle_expires_at <= absolute_expires_at AND
   idle_expires_at <= last_activity_at + INTERVAL '30 minutes' AND
   (revoked_at IS NULL OR revoked_at >= created_at))
);
CREATE INDEX idx_identity_session_account ON identity.auth_session(account_id,id);
CREATE INDEX idx_identity_session_expiry ON identity.auth_session(absolute_expires_at);

-- SHA-256 digests of high-entropy opaque tokens, never bearer values.
-- Consumed rows are retained for strict replay detection (no grace period).
CREATE TABLE identity.refresh_token (
 id UUID PRIMARY KEY,
 session_id UUID NOT NULL REFERENCES identity.auth_session(id),
 token_hash BYTEA NOT NULL UNIQUE CHECK (octet_length(token_hash) = 32),
 predecessor_id UUID,
 issued_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,
 consumed_at TIMESTAMPTZ,
 revoked_at TIMESTAMPTZ,
 CONSTRAINT uq_identity_refresh_family_member UNIQUE (session_id,id),
 CONSTRAINT uq_identity_refresh_successor UNIQUE (predecessor_id),
 CONSTRAINT fk_identity_refresh_predecessor FOREIGN KEY (session_id,predecessor_id)
   REFERENCES identity.refresh_token(session_id,id),
 CONSTRAINT ck_identity_refresh_lineage CHECK (predecessor_id IS NULL OR predecessor_id <> id),
 CONSTRAINT ck_identity_refresh_clock CHECK (expires_at > issued_at AND expires_at <= issued_at + INTERVAL '8 hours' AND
   (consumed_at IS NULL OR (consumed_at >= issued_at AND consumed_at <= expires_at)) AND
   (revoked_at IS NULL OR revoked_at >= issued_at))
);
-- Revoke/consume the prior row before inserting its successor in ONE transaction.
CREATE UNIQUE INDEX uq_identity_refresh_live ON identity.refresh_token(session_id)
 WHERE consumed_at IS NULL AND revoked_at IS NULL;
CREATE UNIQUE INDEX uq_identity_refresh_root ON identity.refresh_token(session_id)
 WHERE predecessor_id IS NULL;
CREATE INDEX idx_identity_refresh_expiry ON identity.refresh_token(expires_at);

CREATE TABLE identity.action_proof (
 id UUID PRIMARY KEY,
 account_id UUID NOT NULL REFERENCES identity.user_account(id),
 purpose VARCHAR(10) NOT NULL CHECK (purpose IN ('INVITATION','RECOVERY')),
 token_hash BYTEA NOT NULL UNIQUE CHECK (octet_length(token_hash) = 32),
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,
 consumed_at TIMESTAMPTZ,
 revoked_at TIMESTAMPTZ,
 CONSTRAINT ck_identity_proof_clock CHECK (expires_at > created_at AND
   expires_at <= created_at + CASE purpose WHEN 'INVITATION' THEN INTERVAL '24 hours' ELSE INTERVAL '15 minutes' END AND
   (consumed_at IS NULL OR (consumed_at >= created_at AND consumed_at <= expires_at)) AND
   (revoked_at IS NULL OR revoked_at >= created_at))
);
CREATE UNIQUE INDEX uq_identity_proof_live ON identity.action_proof(account_id,purpose)
 WHERE consumed_at IS NULL AND revoked_at IS NULL;
CREATE INDEX idx_identity_proof_expiry ON identity.action_proof(expires_at);

CREATE TABLE identity.csrf_binding (
 id UUID PRIMARY KEY,
 cookie_hash BYTEA NOT NULL UNIQUE CHECK (octet_length(cookie_hash) = 32),
 challenge_hash BYTEA NOT NULL CHECK (octet_length(challenge_hash) = 32),
 session_id UUID REFERENCES identity.auth_session(id),
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,
 revoked_at TIMESTAMPTZ,
 CHECK (expires_at > created_at AND (revoked_at IS NULL OR revoked_at >= created_at))
);
CREATE INDEX idx_identity_csrf_session ON identity.csrf_binding(session_id);
CREATE INDEX idx_identity_csrf_expiry ON identity.csrf_binding(expires_at);

-- Typed bindings support uniform recovery receipts without account existence.
-- No arbitrary JSON, input values, cookie, password, bearer or proof response cache.
-- Fingerprinting secret-bearing inputs requires keyed hashing in later use cases.
CREATE TABLE identity.command_receipt (
 binding_kind VARCHAR(7) NOT NULL CHECK (binding_kind IN ('ACTOR','PROOF','PREAUTH')),
 actor_id UUID REFERENCES identity.user_account(id),
 proof_id UUID REFERENCES identity.action_proof(id),
 preauth_binding_id UUID REFERENCES identity.csrf_binding(id),
 binding_id UUID GENERATED ALWAYS AS (COALESCE(actor_id,proof_id,preauth_binding_id)) STORED,
 operation VARCHAR(120) NOT NULL CHECK (operation IN (
   'provisionIdentityAccount','replaceIdentityGrants','disableIdentityAccount','reactivateIdentityAccount',
   'resendIdentityInvitation','requestIdentityRecovery','confirmIdentityRecovery','acceptIdentityInvitation')),
 operation_key UUID NOT NULL,
 request_fingerprint BYTEA NOT NULL CHECK (octet_length(request_fingerprint) = 32),
 target_id UUID REFERENCES identity.user_account(id),
 original_version BIGINT CHECK (original_version > 0),
 result_version BIGINT CHECK (result_version > 0),
 result_status SMALLINT NOT NULL CHECK (result_status IN (200,201,202,204)),
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL CHECK (expires_at > created_at),
 PRIMARY KEY (binding_kind,binding_id,operation,operation_key),
 CONSTRAINT ck_identity_receipt_binding CHECK (
   (binding_kind = 'ACTOR' AND actor_id IS NOT NULL AND proof_id IS NULL AND preauth_binding_id IS NULL AND
     target_id IS NOT NULL AND operation IN ('provisionIdentityAccount','replaceIdentityGrants','disableIdentityAccount','reactivateIdentityAccount','resendIdentityInvitation')) OR
   (binding_kind = 'PROOF' AND actor_id IS NULL AND proof_id IS NOT NULL AND preauth_binding_id IS NULL AND
     target_id IS NOT NULL AND operation IN ('confirmIdentityRecovery','acceptIdentityInvitation')) OR
   (binding_kind = 'PREAUTH' AND actor_id IS NULL AND proof_id IS NULL AND preauth_binding_id IS NOT NULL AND
     target_id IS NULL AND operation = 'requestIdentityRecovery'))
);
CREATE INDEX idx_identity_receipt_expiry ON identity.command_receipt(expires_at);
CREATE INDEX idx_identity_receipt_target ON identity.command_receipt(target_id);
CREATE INDEX idx_identity_receipt_proof ON identity.command_receipt(proof_id);
CREATE INDEX idx_identity_receipt_preauth ON identity.command_receipt(preauth_binding_id);

-- Identity-local audit, not a general event/outbox platform. No secret payload.
CREATE TABLE identity.audit_entry (
 id UUID PRIMARY KEY,
 actor_id UUID REFERENCES identity.user_account(id),
 target_id UUID REFERENCES identity.user_account(id),
 event_type VARCHAR(80) NOT NULL CHECK (char_length(event_type) > 0),
 source VARCHAR(80) NOT NULL CHECK (char_length(source) > 0),
 reason VARCHAR(300),
 request_id VARCHAR(128) NOT NULL CHECK (char_length(request_id) > 0),
 occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_identity_audit_target_time ON identity.audit_entry(target_id,occurred_at,id);
CREATE INDEX idx_identity_audit_actor_time ON identity.audit_entry(actor_id,occurred_at,id);

REVOKE ALL ON TABLE identity.user_account, identity.account_grant, identity.auth_session,
 identity.refresh_token, identity.action_proof, identity.csrf_binding,
 identity.command_receipt, identity.audit_entry FROM PUBLIC;
GRANT SELECT, INSERT, UPDATE ON identity.user_account, identity.auth_session,
 identity.refresh_token, identity.action_proof, identity.csrf_binding TO "${runtimeRole}";
GRANT SELECT, INSERT, DELETE ON identity.account_grant TO "${runtimeRole}";
GRANT SELECT, INSERT ON identity.command_receipt, identity.audit_entry TO "${runtimeRole}";
-- No runtime DDL/TRUNCATE/ownership, account deletion, audit/receipt update/delete.
-- Application transactions must still prove last-admin locking, state transitions,
-- compare-and-consume rotation, revocation and proof/receipt/audit atomicity.
