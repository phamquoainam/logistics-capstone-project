package com.elogistic.backend.identity.internal.application.port;

import com.elogistic.backend.identity.internal.domain.*;
import java.util.Set;

/** Trusted, current same-transaction initial assignability, separate from replacement. */
public interface ProvisionAuthority {
    ManagementAuthority forNewAccount(IdentityAccount actor, IdentityAccount pending, Set<Grant> requested);
    ManagementAuthority forReplay(IdentityAccount actor, IdentityAccount currentTarget, Set<Grant> requested);
}
