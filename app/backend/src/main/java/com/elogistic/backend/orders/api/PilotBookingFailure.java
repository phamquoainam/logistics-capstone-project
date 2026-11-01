package com.elogistic.backend.orders.api;
public final class PilotBookingFailure extends RuntimeException {
    public enum Code { VALIDATION_FAILED,PILOT_DISABLED,RESOURCE_NOT_FOUND,PRECONDITION_STALE,IDEMPOTENCY_CONFLICT,DRAFT_ALREADY_BOOKED,QUOTE_INVALID }
    private final Code code;public PilotBookingFailure(Code code){super(code.name());this.code=code;}public Code code(){return code;}
}
