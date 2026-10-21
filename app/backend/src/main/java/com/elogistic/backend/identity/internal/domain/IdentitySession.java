package com.elogistic.backend.identity.internal.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import static com.elogistic.backend.identity.internal.domain.DomainValues.require;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.SESSION_INVALID;

public record IdentitySession(UUID id, UUID accountId, long authVersion, Instant createdAt,
                              Instant absoluteExpiresAt, Instant lastActivityAt,
                              Instant idleExpiresAt, Instant revokedAt) {
    public static final Duration ABSOLUTE = Duration.ofHours(8);
    public static final Duration IDLE = Duration.ofMinutes(30);
    public static final Duration ACCESS = Duration.ofMinutes(15);
    public enum Activity { USER_ACTION, REFRESH, CSRF, HEALTH, ME_POLL, BACKGROUND_POLL }

    public IdentitySession {
        require(id != null && accountId != null && authVersion > 0 && createdAt != null
            && absoluteExpiresAt != null && lastActivityAt != null && idleExpiresAt != null, SESSION_INVALID);
        require(absoluteExpiresAt.isAfter(createdAt) && !absoluteExpiresAt.isAfter(createdAt.plus(ABSOLUTE))
            && !lastActivityAt.isBefore(createdAt) && lastActivityAt.isBefore(absoluteExpiresAt)
            && idleExpiresAt.isAfter(lastActivityAt) && !idleExpiresAt.isAfter(absoluteExpiresAt)
            && !idleExpiresAt.isAfter(lastActivityAt.plus(IDLE))
            && (revokedAt == null || !revokedAt.isBefore(createdAt)), SESSION_INVALID);
    }

    public static IdentitySession open(UUID id, IdentityAccount account, Instant now) {
        require(account != null && account.active() && now != null, SESSION_INVALID);
        return new IdentitySession(id, account.id(), account.version().authVersion(), now,
            now.plus(ABSOLUTE), now, now.plus(IDLE), null);
    }

    public void admit(IdentityAccount currentAccount, Instant now) {
        require(currentAccount != null && now != null && currentAccount.id().equals(accountId)
            && currentAccount.active() && currentAccount.version().authVersion() == authVersion
            && revokedAt == null && !now.isBefore(lastActivityAt)
            && now.isBefore(absoluteExpiresAt) && now.isBefore(idleExpiresAt), SESSION_INVALID);
    }

    /** Classification comes from the trusted use case, never a request-supplied activity flag. */
    public IdentitySession activity(IdentityAccount account, Activity activity, Instant now) {
        admit(account, now);
        require(activity != null, SESSION_INVALID);
        if (activity != Activity.USER_ACTION) return this;
        return new IdentitySession(id, accountId, authVersion, createdAt, absoluteExpiresAt,
            now, earlier(now.plus(IDLE), absoluteExpiresAt), null);
    }

    public Instant accessExpiresAt(IdentityAccount account, Instant now) {
        admit(account, now);
        return earlier(now.plus(ACCESS), earlier(absoluteExpiresAt, idleExpiresAt));
    }

    public IdentitySession revoke(Instant now) {
        require(now != null && !now.isBefore(createdAt), SESSION_INVALID);
        return revokedAt != null ? this : new IdentitySession(id, accountId, authVersion, createdAt,
            absoluteExpiresAt, lastActivityAt, idleExpiresAt, now);
    }

    private static Instant earlier(Instant left, Instant right) { return left.isBefore(right) ? left : right; }
    @Override public String toString() { return "IdentitySession[redacted]"; }
}
