-- Add customer-purpose quotas without reinterpreting any accepted SYSTEM limiter row.
ALTER TABLE identity.rate_limit_subject DROP CONSTRAINT rate_limit_subject_policy_check;
ALTER TABLE identity.rate_limit_subject ADD CONSTRAINT rate_limit_subject_policy_check CHECK (policy IN (
 'LOGIN_ACCOUNT','LOGIN_IP','REFRESH_SESSION','INVITATION_TOKEN','RESEND_ACCOUNT','RESEND_IP',
 'CUSTOMER_LOGIN_EMAIL','CUSTOMER_LOGIN_IP','CUSTOMER_REFRESH_SESSION','CUSTOMER_ENROLL_EMAIL','CUSTOMER_ENROLL_IP',
 'CUSTOMER_RESEND_EMAIL','CUSTOMER_VERIFY_PROOF','CUSTOMER_RECOVERY_EMAIL','CUSTOMER_RECOVERY_IP','CUSTOMER_RECOVERY_PROOF','CUSTOMER_EMAIL_CHANGE'));
ALTER TABLE identity.rate_limit_attempt DROP CONSTRAINT rate_limit_attempt_check2;
ALTER TABLE identity.rate_limit_attempt ADD CONSTRAINT rate_limit_attempt_password_state CHECK (
 policy IN ('LOGIN_ACCOUNT','CUSTOMER_LOGIN_EMAIL') OR state='COUNTED');
-- Existing limits, windows, leases, runtime privileges and all old rows remain unchanged.
