package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.internal.domain.*;
import java.util.Set;
import java.util.UUID;

/** Approved REALM-P3 only. registeredActiveRealm is trusted registry input, not a request claim. */
public final class IdentityRealmGrantPolicy {
    public ManagementAuthority resolve(IdentityAccount actor, IdentityAccount target,
                                      Set<Grant> requested, UUID registeredActiveRealm) {
        if (registeredActiveRealm == null || actor == null || target == null || requested == null
                || !actor.active() || !actor.managementAdmin() || target.grants().isEmpty() || requested.isEmpty()) return null;
        var pair = new Grant(RoleCode.IDENTITY_MANAGEMENT,
            new ResourceScope(ResourceScope.Type.IDENTITY, registeredActiveRealm));
        // Current actual target grants provide the authoritative scope context; FK/existence alone cannot.
        if (!actor.hasGrant(pair) || !target.grants().stream().allMatch(pair::equals)
                || !requested.stream().allMatch(pair::equals)) return null;
        return new ManagementAuthority(actor.id(), actor.version().authVersion(), target.id(),
            target.version().version(), Set.of(pair), Set.of(pair));
        // The existing Domain guard owns self-management/last-admin/version and replay ordering.
    }
}
