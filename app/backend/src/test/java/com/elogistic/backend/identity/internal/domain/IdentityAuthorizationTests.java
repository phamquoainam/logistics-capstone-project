package com.elogistic.backend.identity.internal.domain;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.elogistic.backend.identity.internal.domain.DomainFixtures.*;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.*;

class IdentityAuthorizationTests {
    @Test void authorizedReplacementKeepsMultipleScopesAndInvalidatesSessions() {
        var target = user(); var grants = Set.of(MERCHANT_A, MERCHANT_B);
        var changed = target.replaceGrants(admin(), authority(admin(), target, grants, grants), grants, 1, 1);
        assertEquals(grants, changed.grants()); assertEquals(new AccountVersion(2, 2), changed.version());
        assertEquals(Set.of(MERCHANT_A), target.grants());
        rejected(SESSION_INVALID, () -> IdentitySession.open(SESSION_ID, target, NOW).admit(changed, NOW));
    }

    @Test void replacementRequiresAuthorityOverUnionIncludingRemovedGrants() {
        var target = account(USER_ID, IdentityAccount.Status.ACTIVE, IdentityAccount.Activation.VERIFIED, Set.of(MERCHANT_A, MERCHANT_B));
        var partial = authority(admin(), target, Set.of(MERCHANT_A), Set.of(MERCHANT_A));
        rejected(AUTHORITY_REQUIRED, () -> target.replaceGrants(admin(), partial, List.of(MERCHANT_A), 1, 1));
        rejected(AUTHORITY_REQUIRED, () -> target.disable(admin(), partial, 1, 1));
    }

    @Test void proposedGrantsNeedExplicitAssignabilityWithoutScopeCrossProduct() {
        var target = user(); var all = Set.of(MERCHANT_A, MERCHANT_B, FINANCE);
        var limited = authority(admin(), target, all, Set.of(MERCHANT_A));
        rejected(AUTHORITY_REQUIRED, () -> target.replaceGrants(admin(), limited, List.of(FINANCE), 1, 1));
        // A role on scope A plus a different role on scope B never implies role A on B.
        var warehouse = grant("WAREHOUSE_OPERATOR", ResourceScope.Type.WAREHOUSE, 12);
        var cross = authority(admin(), target, Set.of(MERCHANT_A, warehouse), Set.of(MERCHANT_A, warehouse));
        rejected(AUTHORITY_REQUIRED, () -> target.replaceGrants(admin(), cross, List.of(MERCHANT_B), 1, 1));
    }

    @Test void missingProviderAndNonManagementActorDefaultDeny() {
        var target = user();
        rejected(AUTHORITY_REQUIRED, () -> target.replaceGrants(admin(), null, List.of(), 1, 1));
        rejected(AUTHORITY_REQUIRED, () -> target.replaceGrants(admin(), authority(admin(), target, Set.of(), Set.of()), List.of(), 1, 1));
        rejected(AUTHORITY_REQUIRED, () -> target.replaceGrants(user(), authority(user(), target, target.grants(), target.grants()), target.grants(), 1, 1));
        var disabledActor = account(ADMIN_ID, IdentityAccount.Status.DISABLED, IdentityAccount.Activation.VERIFIED, Set.of(ADMIN));
        rejected(AUTHORITY_REQUIRED, () -> target.disable(disabledActor, authority(disabledActor, target, target.grants(), Set.of()), 1, 1));
    }

    @Test void snapshotBindsActorAuthVersionTargetAndVersionEvenWhenGrantsEmpty() {
        var target = pending(); var good = authority(admin(), target, Set.of(), Set.of());
        var wrongActor = new ManagementAuthority(OTHER_ID, 1, target.id(), 1, Set.of(), Set.of());
        var staleActor = new ManagementAuthority(ADMIN_ID, 2, target.id(), 1, Set.of(), Set.of());
        var wrongTarget = new ManagementAuthority(ADMIN_ID, 1, OTHER_ID, 1, Set.of(), Set.of());
        var staleTarget = new ManagementAuthority(ADMIN_ID, 1, target.id(), 2, Set.of(), Set.of());
        for (var invalid : List.of(wrongActor, staleActor, wrongTarget, staleTarget))
            rejected(AUTHORITY_REQUIRED, () -> target.disable(admin(), invalid, 1, 1));
        assertEquals(IdentityAccount.Status.DISABLED, target.disable(admin(), good, 1, 1).status());
    }

    @Test void selfDisableAndSelfGrantReplacementAreAlwaysRejected() {
        var actor = admin(); var authority = authority(actor, actor, Set.of(ADMIN, FINANCE), Set.of(ADMIN, FINANCE));
        rejected(SELF_MANAGEMENT, () -> actor.disable(actor, authority, 1, 2));
        rejected(SELF_MANAGEMENT, () -> actor.replaceGrants(actor, authority, List.of(ADMIN, FINANCE), 1, 2));
    }

    @Test void lastActiveAdminCannotBeDisabledOrLoseManagementRole() {
        var target = account(OTHER_ID, IdentityAccount.Status.ACTIVE, IdentityAccount.Activation.VERIFIED, Set.of(ADMIN));
        var authority = authority(admin(), target, Set.of(ADMIN), Set.of(ADMIN));
        rejected(LAST_ACTIVE_ADMIN, () -> target.disable(admin(), authority, 1, 1));
        rejected(LAST_ACTIVE_ADMIN, () -> target.replaceGrants(admin(), authority, List.of(), 1, 1));
        rejected(INVALID_STATE, () -> target.disable(admin(), authority, 1, 0));
        assertEquals(Set.of(ADMIN), target.replaceGrants(admin(), authority, List.of(ADMIN), 1, 1).grants());
        assertEquals(IdentityAccount.Status.DISABLED, target.disable(admin(), authority, 1, 2).status());
    }

    @Test void emptyReplacementIsAllowedForNonAdminButDuplicatesAreRejected() {
        var target = user(); var authority = authority(admin(), target, target.grants(), target.grants());
        assertTrue(target.replaceGrants(admin(), authority, List.of(), 1, 1).grants().isEmpty());
        rejected(INVALID_GRANT, () -> target.replaceGrants(admin(), authority, List.of(MERCHANT_A, MERCHANT_A), 1, 1));
        rejected(INVALID_GRANT, () -> target.replaceGrants(admin(), authority, null, 1, 1));
    }

    @Test void functionalAccessRequiresExactRoleAndScopeWithoutAdminFinanceFallback() {
        assertTrue(user().hasGrant(MERCHANT_A));
        assertFalse(user().hasGrant(MERCHANT_B)); assertFalse(user().hasGrant(null));
        assertFalse(admin().hasGrant(FINANCE)); assertFalse(admin().hasGrant(MERCHANT_A));
        var disabled = account(USER_ID, IdentityAccount.Status.DISABLED, IdentityAccount.Activation.VERIFIED, Set.of(MERCHANT_A));
        assertFalse(disabled.hasGrant(MERCHANT_A));
        assertFalse(pending().hasGrant(MERCHANT_A));
    }

    @Test void authorityCollectionsAreCopiedAndStaleExpectedVersionIsRejected() {
        var grants = new java.util.HashSet<>(Set.of(MERCHANT_A)); var target = user();
        var authority = authority(admin(), target, grants, grants); grants.clear();
        assertEquals(Set.of(MERCHANT_A), authority.manageable());
        assertThrows(UnsupportedOperationException.class, () -> authority.assignable().clear());
        rejected(VERSION_CONFLICT, () -> target.replaceGrants(admin(), authority, List.of(), 9, 1));
    }
}
