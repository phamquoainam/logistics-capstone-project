package com.elogistic.backend.merchant.api;

/** Finite P0-ERROR-01 codes only; never carry a database exception or applicant data. */
public final class GovernanceFailure extends RuntimeException {
    public enum Code {VALIDATION_FAILED,ACCESS_DENIED,RESOURCE_NOT_FOUND,IDEMPOTENCY_CONFLICT,STATE_CONFLICT,
        BUSINESS_CONFLICT,SNAPSHOT_EXPIRED,PRECONDITION_STALE,POLICY_UNAVAILABLE,EVIDENCE_NOT_READY,
        CONTACT_PROOF_REQUIRED,LEGACY_KEY_CLEARANCE_REQUIRED,SERVICE_UNAVAILABLE}
    private final Code code;
    public GovernanceFailure(Code code){super(code.name());this.code=code;}
    public Code code(){return code;}
}
