package com.elogistic.backend.pricing.api;
public final class PilotQuoteFailure extends RuntimeException {
    public enum Code { VALIDATION_FAILED,PILOT_DISABLED,UNSUPPORTED_ROUTE,PARCEL_NOT_ELIGIBLE,ORDINARY_GOODS_REQUIRED,RESOURCE_NOT_FOUND,IDEMPOTENCY_CONFLICT,QUOTE_EXPIRED,QUOTE_CONSUMED,PRECONDITION_STALE }
    private final Code code;public PilotQuoteFailure(Code code){super(code.name());this.code=code;}public Code code(){return code;}
}
