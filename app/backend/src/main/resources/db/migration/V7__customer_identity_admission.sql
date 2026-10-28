-- Spec003 / OWNER_SPEC003_FULL_IMPLEMENTATION. Identity remains the security master.
-- V1-V6 and SYSTEM lookups remain unchanged. Customer credentials cannot be read
-- by old SYSTEM session/refresh stores, even for a dual-purpose account.
CREATE TABLE identity.customer_identifier (
 id UUID PRIMARY KEY,
 account_id UUID NOT NULL REFERENCES identity.user_account(id),
 exact_email VARCHAR(254) COLLATE "C" NOT NULL CHECK (char_length(exact_email)>0),
 verified_at TIMESTAMPTZ NOT NULL,
 status VARCHAR(7) NOT NULL CHECK (status IN ('ACTIVE','RETIRED')),
 version BIGINT NOT NULL CHECK (version>0)
);
CREATE UNIQUE INDEX uq_customer_identifier_email ON identity.customer_identifier(exact_email) WHERE status='ACTIVE';
CREATE UNIQUE INDEX uq_customer_identifier_account ON identity.customer_identifier(account_id) WHERE status='ACTIVE';

-- LIKE copies immutable V3 column/check definitions, not SYSTEM foreign keys,
-- indexes, grants or data. Every customer FK/index below is purpose-specific.
CREATE TABLE identity.customer_auth_session (LIKE identity.auth_session INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
ALTER TABLE identity.customer_auth_session ADD PRIMARY KEY(id);
ALTER TABLE identity.customer_auth_session ADD FOREIGN KEY(account_id) REFERENCES identity.user_account(id);
CREATE INDEX idx_customer_session_account ON identity.customer_auth_session(account_id,id);
CREATE INDEX idx_customer_session_expiry ON identity.customer_auth_session(absolute_expires_at);

CREATE TABLE identity.customer_refresh_token (LIKE identity.refresh_token INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
ALTER TABLE identity.customer_refresh_token ADD PRIMARY KEY(id);
ALTER TABLE identity.customer_refresh_token ADD UNIQUE(token_hash);
ALTER TABLE identity.customer_refresh_token ADD UNIQUE(session_id,id);
ALTER TABLE identity.customer_refresh_token ADD UNIQUE(predecessor_id);
ALTER TABLE identity.customer_refresh_token ADD FOREIGN KEY(session_id) REFERENCES identity.customer_auth_session(id);
ALTER TABLE identity.customer_refresh_token ADD FOREIGN KEY(session_id,predecessor_id) REFERENCES identity.customer_refresh_token(session_id,id);
CREATE UNIQUE INDEX uq_customer_refresh_live ON identity.customer_refresh_token(session_id) WHERE consumed_at IS NULL AND revoked_at IS NULL;
CREATE UNIQUE INDEX uq_customer_refresh_root ON identity.customer_refresh_token(session_id) WHERE predecessor_id IS NULL;
CREATE INDEX idx_customer_refresh_expiry ON identity.customer_refresh_token(expires_at);

CREATE TABLE identity.customer_csrf_binding (LIKE identity.csrf_binding INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
ALTER TABLE identity.customer_csrf_binding ADD PRIMARY KEY(id);
ALTER TABLE identity.customer_csrf_binding ADD UNIQUE(cookie_hash);
ALTER TABLE identity.customer_csrf_binding ADD FOREIGN KEY(session_id) REFERENCES identity.customer_auth_session(id);
CREATE INDEX idx_customer_csrf_session ON identity.customer_csrf_binding(session_id);
CREATE INDEX idx_customer_csrf_expiry ON identity.customer_csrf_binding(expires_at);

REVOKE ALL ON identity.customer_identifier,identity.customer_auth_session,identity.customer_refresh_token,identity.customer_csrf_binding FROM PUBLIC;
GRANT SELECT,INSERT,UPDATE ON identity.customer_identifier,identity.customer_auth_session,identity.customer_refresh_token,identity.customer_csrf_binding TO "${runtimeRole}";
-- No runtime DDL/TRUNCATE/audit mutation privileges or business-module grants.
