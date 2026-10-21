package com.elogistic.backend.identity.internal.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Synthetic domain state only; not a fake production repository or authority provider. */
final class DomainFixtures {
    static final Instant NOW = Instant.parse("2026-10-21T00:00:00Z");
    static final UUID ADMIN_ID = id(1), USER_ID = id(2), OTHER_ID = id(3), SESSION_ID = id(4);
    static final Grant ADMIN = grant("IDENTITY_MANAGEMENT", ResourceScope.Type.IDENTITY, 10);
    static final Grant MERCHANT_A = grant("MERCHANT", ResourceScope.Type.MERCHANT, 11);
    static final Grant MERCHANT_B = grant("MERCHANT", ResourceScope.Type.MERCHANT, 12);
    static final Grant FINANCE = grant("FINANCE", ResourceScope.Type.IDENTITY, 10);
    static final String PASSWORD = "Synthetic-only-passphrase";

    static UUID id(long value) { return new UUID(0, value); }
    static Grant grant(String role, ResourceScope.Type type, long id) {
        return new Grant(new RoleCode(role), new ResourceScope(type, id(id)));
    }
    static IdentityAccount account(UUID id, IdentityAccount.Status status, IdentityAccount.Activation activation,
                                     Set<Grant> grants) {
        return new IdentityAccount(id, new Username("fixture.identity"), status, activation,
            new AccountVersion(1, 1), grants);
    }
    static IdentityAccount admin() {
        return account(ADMIN_ID, IdentityAccount.Status.ACTIVE, IdentityAccount.Activation.VERIFIED, Set.of(ADMIN));
    }
    static IdentityAccount user() {
        return account(USER_ID, IdentityAccount.Status.ACTIVE, IdentityAccount.Activation.VERIFIED, Set.of(MERCHANT_A));
    }
    static IdentityAccount pending() {
        return account(USER_ID, IdentityAccount.Status.PENDING_ACTIVATION, IdentityAccount.Activation.UNVERIFIED, Set.of());
    }
    static ManagementAuthority authority(IdentityAccount actor, IdentityAccount target,
                                           Set<Grant> manageable, Set<Grant> assignable) {
        return new ManagementAuthority(actor.id(), actor.version().authVersion(), target.id(),
            target.version().version(), manageable, assignable);
    }
    static void rejected(DomainViolation.Reason reason, Runnable operation) {
        DomainViolation ex = assertThrows(DomainViolation.class, operation::run);
        assertEquals(reason, ex.reason());
        assertEquals(reason.name(), ex.getMessage());
    }
    private DomainFixtures() {}
}
