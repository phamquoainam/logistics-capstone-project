-- Owner-approved Identity-only rolling limiter. No credential material or developer ACL changes.
CREATE TABLE identity.rate_limit_subject (
 policy VARCHAR(24) NOT NULL CHECK (policy IN ('LOGIN_ACCOUNT','LOGIN_IP','REFRESH_SESSION','INVITATION_TOKEN','RESEND_ACCOUNT','RESEND_IP')),
 subject_hash BYTEA NOT NULL CHECK (octet_length(subject_hash)=32),
 touched_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(policy,subject_hash)
);
CREATE TABLE identity.rate_limit_attempt (
 reservation_id UUID NOT NULL,
 policy VARCHAR(24) NOT NULL,
 subject_hash BYTEA NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,
 lease_expires_at TIMESTAMPTZ NOT NULL,
 state VARCHAR(10) NOT NULL CHECK (state IN ('PENDING','COUNTED','FAILED','RELEASED')),
 PRIMARY KEY(reservation_id,policy,subject_hash),
 FOREIGN KEY(policy,subject_hash) REFERENCES identity.rate_limit_subject(policy,subject_hash),
 CHECK (expires_at>created_at AND expires_at<=created_at+INTERVAL '1 hour'),
 CHECK (lease_expires_at>created_at AND lease_expires_at<=created_at+INTERVAL '60 seconds'),
 CHECK (policy='LOGIN_ACCOUNT' OR state='COUNTED')
);
CREATE INDEX idx_identity_rate_window ON identity.rate_limit_attempt(policy,subject_hash,expires_at);
CREATE INDEX idx_identity_rate_retention ON identity.rate_limit_attempt(expires_at);
REVOKE ALL ON identity.rate_limit_subject, identity.rate_limit_attempt FROM PUBLIC;
GRANT SELECT,INSERT,UPDATE,DELETE ON identity.rate_limit_subject, identity.rate_limit_attempt TO "${runtimeRole}";
-- Runtime cannot DDL/TRUNCATE/own either table; all old object privileges are unchanged.
