-- Forward-only amendment: preserve historical recovery links and accept new two-minute OTPs.
ALTER TABLE identity.customer_proof DROP CONSTRAINT ck_customer_proof_otp_clock;
ALTER TABLE identity.customer_proof ADD CONSTRAINT ck_customer_recovery_otp_clock CHECK (
 (purpose='CUSTOMER_ENROLLMENT' AND expires_at IN (created_at+INTERVAL '24 hours',created_at+INTERVAL '2 minutes'))
 OR (purpose='CUSTOMER_RECOVERY' AND expires_at IN (created_at+INTERVAL '15 minutes',created_at+INTERVAL '2 minutes'))
 OR (purpose NOT IN ('CUSTOMER_ENROLLMENT','CUSTOMER_RECOVERY') AND expires_at=created_at+INTERVAL '15 minutes')
);
