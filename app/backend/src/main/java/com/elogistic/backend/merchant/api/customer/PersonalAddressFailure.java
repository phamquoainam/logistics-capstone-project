package com.elogistic.backend.merchant.api.customer;

/** Address HTTP vocabulary is separately approved; existing onboarding status mappings stay intact. */
public final class PersonalAddressFailure extends RuntimeException {
    public enum Code {VALIDATION_FAILED,RESOURCE_NOT_FOUND,PRECONDITION_STALE,IDEMPOTENCY_CONFLICT}
    private final Code code;
    public PersonalAddressFailure(Code code){super(code.name());this.code=code;}
    public Code code(){return code;}
}
