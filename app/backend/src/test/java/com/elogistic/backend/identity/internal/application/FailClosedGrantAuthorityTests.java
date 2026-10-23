package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.internal.domain.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pure composition protocol fixtures; no production owner integration or principal proof. */
class FailClosedGrantAuthorityTests {
    private static final UUID ACTOR = new UUID(0, 1), TARGET = new UUID(0, 2), SCOPE = new UUID(0, 3);
    private static final Grant ADMIN = new Grant(RoleCode.IDENTITY_MANAGEMENT,
        new ResourceScope(ResourceScope.Type.IDENTITY, SCOPE));
    private static IdentityAccount account(UUID id, IdentityAccount.Status status, Set<Grant> grants) {
        return new IdentityAccount(id, new Username("fixture.identity"), status,
            IdentityAccount.Activation.VERIFIED, new AccountVersion(1, 1), grants);
    }
    private static IdentityAccount actor() { return account(ACTOR, IdentityAccount.Status.ACTIVE, Set.of(ADMIN)); }
    private static IdentityAccount target() { return account(TARGET, IdentityAccount.Status.ACTIVE, Set.of(ADMIN)); }
    private static ManagementAuthority snapshot(IdentityAccount actor, IdentityAccount target) {
        return new ManagementAuthority(actor.id(), actor.version().authVersion(), target.id(),
            target.version().version(), Set.of(ADMIN), Set.of(ADMIN));
    }

    @Test void missingProviderDeniesWithoutInventingIdentityScopeAuthority() {
        assertNull(new FailClosedGrantAuthority(null).resolve(actor(), target(), Set.of(ADMIN)));
    }
    @Test void nullProviderResultDeniesWithoutFallback() {
        assertNull(new FailClosedGrantAuthority((a, t, p) -> null).resolve(actor(), target(), Set.of(ADMIN)));
    }
    @Test void providerFailureIsDeniedWithoutExposingItsMessage() {
        var lookup = new FailClosedGrantAuthority((a, t, p) -> { throw new IllegalStateException("synthetic-private-owner-diagnostic"); });
        assertNull(assertDoesNotThrow(() -> lookup.resolve(actor(), target(), Set.of(ADMIN))));
    }
    @Test void disabledActorNeverReachesProvider() {
        AtomicInteger calls = new AtomicInteger();
        var lookup = new FailClosedGrantAuthority((a, t, p) -> { calls.incrementAndGet(); return snapshot(a, t); });
        assertNull(lookup.resolve(account(ACTOR, IdentityAccount.Status.DISABLED, Set.of(ADMIN)), target(), Set.of(ADMIN)));
        assertEquals(0, calls.get());
    }
    @Test void unrelatedRoleNeverEstablishesManagementAuthority() {
        AtomicInteger calls = new AtomicInteger();
        var lookup = new FailClosedGrantAuthority((a, t, p) -> { calls.incrementAndGet(); return snapshot(a, t); });
        Grant unrelated = new Grant(new RoleCode("FINANCE"), ADMIN.scope());
        assertNull(lookup.resolve(account(ACTOR, IdentityAccount.Status.ACTIVE, Set.of(unrelated)), target(), Set.of(ADMIN)));
        assertEquals(0, calls.get());
    }
    @Test void missingActorTargetOrRequestedContextDeniesBeforeProvider() {
        AtomicInteger calls = new AtomicInteger();
        var lookup = new FailClosedGrantAuthority((a, t, p) -> { calls.incrementAndGet(); return snapshot(a, t); });
        assertNull(lookup.resolve(null, target(), Set.of(ADMIN)));
        assertNull(lookup.resolve(actor(), null, Set.of(ADMIN)));
        assertNull(lookup.resolve(actor(), target(), null));
        assertEquals(0, calls.get());
    }
    @Test void trustedDelegateReceivesExactCurrentSnapshotsAndImmutableRequestedSet() {
        IdentityAccount actor = actor(), target = target(); var expected = snapshot(actor, target);
        AtomicInteger calls = new AtomicInteger();
        var lookup = new FailClosedGrantAuthority((a, t, p) -> {
            assertSame(actor, a); assertSame(target, t); assertEquals(Set.of(ADMIN), p);
            assertThrows(UnsupportedOperationException.class, p::clear); calls.incrementAndGet(); return expected;
        });
        assertSame(expected, lookup.resolve(actor, target, new HashSet<>(Set.of(ADMIN))));
        assertEquals(1, calls.get()); // Delegation fixture only, not a production permission grant.
    }
    @Test void vmErrorsAreNotSwallowedAsOrdinaryProviderUnavailability() {
        var failure = new AssertionError("synthetic-owner-error");
        var lookup = new FailClosedGrantAuthority((a, t, p) -> { throw failure; });
        assertSame(failure, assertThrows(AssertionError.class, () -> lookup.resolve(actor(), target(), Set.of(ADMIN))));
    }

    @Test void emptyTargetIsDeniedBeforeAnAllowingDelegate() {
        AtomicInteger calls = new AtomicInteger();
        var lookup = new FailClosedGrantAuthority((a, t, p) -> {
            calls.incrementAndGet(); return snapshot(a, t);
        });
        var empty = account(TARGET, IdentityAccount.Status.ACTIVE, Set.of());
        assertNull(lookup.resolve(actor(), empty, Set.of(ADMIN)));
        assertEquals(0, calls.get());
    }
}
