package com.elogistic.backend.orders.api;
public final class PilotHandlingFailure extends RuntimeException {
    public enum Code { VALIDATION_FAILED, RESOURCE_NOT_FOUND, AUTHORITY_REQUIRED, INTAKE_POLICY_BLOCKED, PRECONDITION_STALE, ALREADY_ACCEPTED, PARCEL_SET_INCOMPLETE, INSPECTION_MISMATCH, IDEMPOTENCY_CONFLICT }
    private final Code code;
    public PilotHandlingFailure(Code code){super(code.name());this.code=code;}
    public Code code(){return code;}
}
