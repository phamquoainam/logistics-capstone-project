package com.elogistic.backend.identity.internal.application.port;
import java.util.*;
public interface IdentityRateLimiter {
    enum Policy {
        LOGIN_ACCOUNT(5,900), LOGIN_IP(30,900), REFRESH_SESSION(30,300),
        INVITATION_TOKEN(5,900), RESEND_ACCOUNT(3,3600), RESEND_IP(20,3600);
        public final int limit,seconds;
        Policy(int limit,int seconds){this.limit=limit;this.seconds=seconds;}
    }
    record Subject(Policy policy,String value) { @Override public String toString(){return "RateSubject[redacted]";} }
    interface Reservation { void finishLogin(boolean success); }
    Reservation reserve(List<Subject> subjects);
    int cleanup();
    final class Exhausted extends RuntimeException {
        public final int retryAfter;
        public Exhausted(int seconds){super("RATE_LIMITED");retryAfter=Math.max(1,seconds);}
    }
    final class Unavailable extends RuntimeException { public Unavailable(){super("SERVICE_UNAVAILABLE");} }
}
