package com.elogistic.backend.identity.internal.domain;

import java.util.Set;
import java.util.UUID;
import static com.elogistic.backend.identity.internal.domain.DomainValues.require;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.*;

/**
 * Trusted application input resolved from CURRENT Identity and public owner APIs.
 * Never construct from request claims. Exact grant pairs prevent role/scope cross-products.
 * Target binding is required even for an empty target grant set. A missing owner
 * provider supplies no authority, never a wildcard. Not a cache or persistence model.
 */
public record ManagementAuthority(UUID actorId, long actorAuthVersion, UUID targetId,
                                  long targetVersion, Set<Grant> manageable, Set<Grant> assignable) {
    public ManagementAuthority {
        require(actorId != null && targetId != null && actorAuthVersion > 0 && targetVersion > 0,
            AUTHORITY_REQUIRED);
        manageable = DomainValues.unique(manageable);
        assignable = DomainValues.unique(assignable);
    }

    void requireManagement(IdentityAccount actor, IdentityAccount target, Set<Grant> proposed) {
        require(actor != null && actor.active() && actor.managementAdmin(), AUTHORITY_REQUIRED);
        require(actor.id().equals(actorId) && actor.version().authVersion() == actorAuthVersion
            && target.id().equals(targetId) && target.version().version() == targetVersion, AUTHORITY_REQUIRED);
        require(!actor.id().equals(target.id()), SELF_MANAGEMENT);
        require(manageable.containsAll(target.grants()) && manageable.containsAll(proposed)
            && assignable.containsAll(proposed), AUTHORITY_REQUIRED);
    }
}
