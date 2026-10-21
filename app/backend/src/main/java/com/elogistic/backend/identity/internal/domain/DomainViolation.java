package com.elogistic.backend.identity.internal.domain;

/** Internal business reasons; HTTP mapping belongs to the adapter. Never echo input. */
public final class DomainViolation extends IllegalArgumentException {
    public enum Reason {
        INVALID_STATE, INVALID_USERNAME, INVALID_PASSWORD, INVALID_GRANT,
        AUTHORITY_REQUIRED, SELF_MANAGEMENT, LAST_ACTIVE_ADMIN,
        VERSION_CONFLICT, VERSION_EXHAUSTED, SESSION_INVALID, PROOF_INVALID, REFRESH_INVALID
    }

    private final Reason reason;

    public DomainViolation(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
