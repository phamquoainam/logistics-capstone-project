package com.elogistic.backend.identity.internal.application.port;

import com.elogistic.backend.identity.internal.domain.Grant;
import com.elogistic.backend.identity.internal.domain.IdentityAccount;
import com.elogistic.backend.identity.internal.domain.ManagementAuthority;
import java.util.Set;

/** Trusted, synchronous in-process owner authority port, never request-supplied grants.
 * Implementations must establish full-target access (including empty targets), validity
 * and assignability through approved public owner contracts. No network/side effects
 * while Identity locks are held. Missing/unimplemented providers deny, never wildcard.
 * No permissive production provider or cross-module dependency is supplied by this slice. */
@FunctionalInterface
public interface GrantAuthority {
    ManagementAuthority resolve(IdentityAccount actor, IdentityAccount target, Set<Grant> requested);
}
