package com.elogistic.backend.merchant.api;

/** Safe public business codes; no submitted PII, proof, SQL or framework exception escapes. */
public final class OnboardingFailure extends RuntimeException {
    public enum Code {VALIDATION_FAILED,IDEMPOTENCY_CONFLICT,PRECONDITION_STALE,RESOURCE_NOT_FOUND,BUSINESS_CONFLICT}
    private final Code code;
    public OnboardingFailure(Code code){super(code.name());this.code=code;}
    public Code code(){return code;}
}
