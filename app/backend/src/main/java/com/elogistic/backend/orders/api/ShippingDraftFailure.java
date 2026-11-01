package com.elogistic.backend.orders.api;
public final class ShippingDraftFailure extends RuntimeException {
    public enum Code { VALIDATION_FAILED,RESOURCE_NOT_FOUND,PRECONDITION_STALE,IDEMPOTENCY_CONFLICT }
    private final Code code;
    public ShippingDraftFailure(Code code){super(code.name());this.code=code;}
    public Code code(){return code;}
}
