package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.internal.application.port.GrantAuthority;
import com.elogistic.backend.identity.internal.domain.Grant;
import com.elogistic.backend.identity.internal.domain.IdentityAccount;
import com.elogistic.backend.identity.internal.domain.ManagementAuthority;
import java.util.Set;

/** Availability boundary around the EXISTING trusted owner-authority port.
 * This does not infer resource ownership, implement a role catalog, authenticate
 * an actor or replace Domain authorization. ReplaceIdentityGrants must first load
 * locked current actor/session/target state and subsequently apply the Domain guard.
 * A future delegate must use approved in-process public owner contracts and the
 * same transaction/locking protocol; request claims and network lookups are invalid.
 */
public final class FailClosedGrantAuthority implements GrantAuthority {
    private final GrantAuthority source;

    public FailClosedGrantAuthority(GrantAuthority source) { this.source = source; }

    @Override public ManagementAuthority resolve(IdentityAccount actor, IdentityAccount target,
                                                  Set<Grant> requested) {
        if (source == null || actor == null || target == null || requested == null
                || !actor.active() || !actor.managementAdmin()) return null;
        // OWNER_AUTHC_DECISION: replacement is not an initial provisioning path.
        // No approved separate authority mapping exists for an unscoped target.
        if (target.grants().isEmpty()) return null;
        try {
            // Do not pass mutable request collections into a trusted lookup.
            // The returned snapshot still MUST pass the existing full-target Domain guard.
            return source.resolve(actor, target, Set.copyOf(requested));
        } catch (RuntimeException unavailable) {
            // Lookup failure never permits a mutation, receipt disclosure or exception/PII echo.
            // Do not catch VM Errors, log provider messages or synthesize an allow snapshot.
            return null;
        }
    }
}
