package com.elogistic.backend.identity.internal.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import static com.elogistic.backend.identity.internal.domain.DomainValues.require;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.PROOF_INVALID;

/** Lifecycle of a server-verified proof subject. Raw proof/hash validation is an adapter responsibility. */
public record ActionProof(UUID id, UUID accountId, Purpose purpose, Instant createdAt,
                          Instant expiresAt, State state, Instant endedAt) {
    public enum Purpose {
        INVITATION(Duration.ofHours(24)), RECOVERY(Duration.ofMinutes(15));
        private final Duration lifetime;
        Purpose(Duration lifetime) { this.lifetime = lifetime; }
        public Duration lifetime() { return lifetime; }
    }
    public enum State { AVAILABLE, CONSUMED, REVOKED }

    public ActionProof {
        require(id != null && accountId != null && purpose != null && createdAt != null
            && expiresAt != null && state != null, PROOF_INVALID);
        require(expiresAt.isAfter(createdAt) && !expiresAt.isAfter(createdAt.plus(purpose.lifetime())), PROOF_INVALID);
        require(state == State.AVAILABLE ? endedAt == null : endedAt != null && !endedAt.isBefore(createdAt), PROOF_INVALID);
        require(state != State.CONSUMED || !endedAt.isAfter(expiresAt), PROOF_INVALID);
    }

    public static ActionProof invitation(UUID id, IdentityAccount account, Instant now) {
        require(account != null && account.status() == IdentityAccount.Status.PENDING_ACTIVATION, PROOF_INVALID);
        return issue(id, account, Purpose.INVITATION, now);
    }

    /** A backend comparison with the stored verified destination is mandatory; no implicit match. */
    public static ActionProof recovery(UUID id, IdentityAccount account,
                                        IdentityAccount.EmailMatch storedVerifiedEmailMatch, Instant now) {
        require(account != null && account.recoveryEligible(storedVerifiedEmailMatch), PROOF_INVALID);
        return issue(id, account, Purpose.RECOVERY, now);
    }

    private static ActionProof issue(UUID id, IdentityAccount account, Purpose purpose, Instant now) {
        require(account != null && purpose != null && now != null, PROOF_INVALID);
        return new ActionProof(id, account.id(), purpose, now, now.plus(purpose.lifetime()), State.AVAILABLE, null);
    }

    public ActionProof consume(Purpose expected, UUID subject, Instant now) {
        require(expected != null && expected == purpose && accountId.equals(subject) && state == State.AVAILABLE
            && now != null && !now.isBefore(createdAt) && now.isBefore(expiresAt), PROOF_INVALID);
        return new ActionProof(id, accountId, purpose, createdAt, expiresAt, State.CONSUMED, now);
    }

    public ActionProof revoke(Instant now) {
        require(now != null && !now.isBefore(createdAt), PROOF_INVALID);
        return state != State.AVAILABLE ? this : new ActionProof(id, accountId, purpose, createdAt,
            expiresAt, State.REVOKED, now);
    }

    @Override public String toString() { return "ActionProof[redacted]"; }
}
