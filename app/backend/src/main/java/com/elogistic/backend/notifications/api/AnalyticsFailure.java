package com.elogistic.backend.notifications.api;

public final class AnalyticsFailure extends RuntimeException {
    public enum Code { VALIDATION_FAILED,ANALYTICS_RANGE_TOO_LARGE,PILOT_DISABLED,ANALYTICS_UNAVAILABLE }
    private final Code code;
    public AnalyticsFailure(Code code){super(code.name());this.code=code;}
    public Code code(){return code;}
}
