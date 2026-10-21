package com.elogistic.backend.identity.internal.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import static com.elogistic.backend.identity.internal.domain.DomainValues.require;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.REFRESH_INVALID;

/** No raw tokens. IDs must come from backend hash lookup; persist rotation/replay effects atomically. */
public record RefreshFamily(UUID sessionId, UUID currentTokenId, Instant expiresAt,
                            Set<UUID> consumedTokenIds, State state) {
    public enum State { ACTIVE, REVOKED }
    public enum Outcome { ROTATED, REPLAY_REVOKED, REJECTED }
    public record Rotation(RefreshFamily family, IdentitySession session, Outcome outcome) {}

    public RefreshFamily {
        require(sessionId != null && currentTokenId != null && expiresAt != null
            && consumedTokenIds != null && state != null
            && consumedTokenIds.stream().noneMatch(java.util.Objects::isNull), REFRESH_INVALID);
        consumedTokenIds = Set.copyOf(consumedTokenIds);
        require(!consumedTokenIds.contains(currentTokenId), REFRESH_INVALID);
    }

    public static RefreshFamily issue(IdentitySession session, IdentityAccount account, UUID tokenId, Instant now) {
        require(session != null, REFRESH_INVALID);
        session.admit(account, now);
        return new RefreshFamily(session.id(), tokenId, session.absoluteExpiresAt(), Set.of(), State.ACTIVE);
    }

    public Rotation rotate(IdentitySession session, IdentityAccount account, UUID presentedId, UUID nextId, Instant now) {
        require(session != null && session.id().equals(sessionId) && account != null
            && session.accountId().equals(account.id()) && presentedId != null, REFRESH_INVALID);
        if (state == State.REVOKED) return new Rotation(this, session.revoke(now), Outcome.REJECTED);
        session.admit(account, now);
        require(!expiresAt.isAfter(session.absoluteExpiresAt()) && now.isBefore(expiresAt), REFRESH_INVALID);
        if (consumedTokenIds.contains(presentedId))
            return new Rotation(revoke(), session.revoke(now), Outcome.REPLAY_REVOKED);
        require(presentedId.equals(currentTokenId) && nextId != null && !nextId.equals(currentTokenId)
            && !consumedTokenIds.contains(nextId), REFRESH_INVALID);
        Set<UUID> consumed = new HashSet<>(consumedTokenIds);
        consumed.add(currentTokenId);
        return new Rotation(new RefreshFamily(sessionId, nextId, expiresAt, consumed, State.ACTIVE),
            session, Outcome.ROTATED);
    }

    public RefreshFamily revoke() {
        return state == State.REVOKED ? this : new RefreshFamily(sessionId, currentTokenId, expiresAt,
            consumedTokenIds, State.REVOKED);
    }

    @Override public String toString() { return "RefreshFamily[redacted]"; }
}
