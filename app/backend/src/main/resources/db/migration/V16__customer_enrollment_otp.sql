-- Preserve previously issued 24-hour links; new enrollment proofs use a two-minute OTP.
DO $$
DECLARE clock_check name; policy_check name; template_check name;
BEGIN
 SELECT conname INTO clock_check FROM pg_constraint
 WHERE conrelid='identity.customer_proof'::regclass AND contype='c'
   AND pg_get_constraintdef(oid) LIKE '%expires_at%'
   AND pg_get_constraintdef(oid) LIKE '%CASE%'
   AND pg_get_constraintdef(oid) LIKE '%purpose%';
 IF clock_check IS NULL THEN RAISE EXCEPTION 'CUSTOMER_PROOF_CLOCK_CONSTRAINT_NOT_FOUND'; END IF;
 EXECUTE format('ALTER TABLE identity.customer_proof DROP CONSTRAINT %I',clock_check);
 SELECT conname INTO policy_check FROM pg_constraint
 WHERE conrelid='identity.customer_proof'::regclass AND contype='c'
   AND pg_get_constraintdef(oid) LIKE '%policy_version%'
   AND pg_get_constraintdef(oid) LIKE '%S003-2026-10-27%';
 IF policy_check IS NULL THEN RAISE EXCEPTION 'CUSTOMER_PROOF_POLICY_CONSTRAINT_NOT_FOUND'; END IF;
 EXECUTE format('ALTER TABLE identity.customer_proof DROP CONSTRAINT %I',policy_check);
 SELECT conname INTO template_check FROM pg_constraint
 WHERE conrelid='identity.customer_delivery'::regclass AND contype='c'
   AND pg_get_constraintdef(oid) LIKE '%template_version%'
   AND pg_get_constraintdef(oid) LIKE '%S003-EMAIL-1%';
 IF template_check IS NULL THEN RAISE EXCEPTION 'CUSTOMER_DELIVERY_TEMPLATE_CONSTRAINT_NOT_FOUND'; END IF;
 EXECUTE format('ALTER TABLE identity.customer_delivery DROP CONSTRAINT %I',template_check);
END $$;
ALTER TABLE identity.customer_proof ADD CONSTRAINT ck_customer_proof_otp_clock CHECK (
 (purpose='CUSTOMER_ENROLLMENT' AND expires_at IN (created_at+INTERVAL '24 hours',created_at+INTERVAL '2 minutes'))
 OR (purpose<>'CUSTOMER_ENROLLMENT' AND expires_at=created_at+INTERVAL '15 minutes')
);
ALTER TABLE identity.customer_proof ADD CONSTRAINT ck_customer_proof_otp_policy CHECK (
 policy_version IN ('S003-2026-10-27','S003-OTP-2026-10-30')
);
ALTER TABLE identity.customer_delivery ADD CONSTRAINT ck_customer_delivery_otp_template CHECK (
 template_version IN ('S003-EMAIL-1','S003-EMAIL-OTP-1')
);
