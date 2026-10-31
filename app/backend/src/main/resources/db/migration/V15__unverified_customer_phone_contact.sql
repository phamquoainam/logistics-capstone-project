-- A phone supplied during personal enrollment is contact data only. It grants no login,
-- recovery, account association, uniqueness claim or Merchant authority.
ALTER TABLE identity.customer_enrollment
 ADD COLUMN unverified_phone VARCHAR(16)
 CHECK (unverified_phone IS NULL OR unverified_phone ~ '^\+[1-9][0-9]{7,14}$');

CREATE TABLE identity.customer_phone_contact (
 account_id UUID PRIMARY KEY REFERENCES identity.user_account(id),
 canonical_phone VARCHAR(16) NOT NULL CHECK (canonical_phone ~ '^\+[1-9][0-9]{7,14}$'),
 verification_status VARCHAR(12) NOT NULL CHECK (verification_status='UNVERIFIED'),
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL CHECK (updated_at>=created_at)
);
GRANT SELECT,INSERT ON identity.customer_phone_contact TO "${runtimeRole}";
