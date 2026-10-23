package com.elogistic.backend.identity.api;

/** Accepted contract codes only; no HTTP dependency or submitted values in messages. */
public final class IdentityCommandFailure extends RuntimeException {
    public enum Code {
        VALIDATION_FAILED, SESSION_INVALID, ACCOUNT_NOT_ACCESSIBLE,
        SELF_MANAGEMENT_FORBIDDEN, PRECONDITION_STALE,
        IDEMPOTENCY_CONFLICT, LAST_ADMIN_PROTECTED, AUTHORITY_REQUIRED, USERNAME_CONFLICT
    }
    private final Code code;
    public IdentityCommandFailure(Code code) { super(code.name()); this.code = code; }
    public Code code() { return code; }
}
