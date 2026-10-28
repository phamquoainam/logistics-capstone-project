package com.elogistic.backend.identity.internal.application.port;

/** Durable customer password budgets reserved before starting the application transaction. */
@FunctionalInterface
public interface CustomerPasswordAttemptAdmission {
    IdentityRateLimiter.Reservation reserve(String exactEmail);
}
