package com.elogistic.backend.identity.internal.domain;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.elogistic.backend.identity.internal.domain.DomainFixtures.*;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.*;

class IdentityAccountTests {
    @Test void provisioningRequiresExplicitAuthorityAndCreatesOnlyPendingAccount() {
        var pending = pending(); var authority = authority(admin(), pending, Set.of(MERCHANT_A), Set.of(MERCHANT_A));
        var result = IdentityAccount.provision(USER_ID, new Username("New User"), List.of(MERCHANT_A), admin(), authority);
        assertEquals(IdentityAccount.Status.PENDING_ACTIVATION, result.status());
        assertEquals(IdentityAccount.Activation.UNVERIFIED, result.activation());
        assertEquals(new AccountVersion(1, 1), result.version());
        assertEquals(Set.of(MERCHANT_A), result.grants());
        assertFalse(result.hasGrant(MERCHANT_A));
        rejected(AUTHORITY_REQUIRED, () -> IdentityAccount.provision(USER_ID, pending.username(), List.of(), admin(), null));
    }

    @Test void activationReturnsAccountAndConsumedInvitationWithoutMutatingOriginals() {
        var pending = pending(); var proof = ActionProof.invitation(id(20), pending, NOW);
        var result = pending.activate(proof, PASSWORD, 1, NOW.plusSeconds(1));
        assertTrue(result.account().active()); assertEquals(IdentityAccount.Activation.VERIFIED, result.account().activation());
        assertEquals(new AccountVersion(2, 2), result.account().version());
        assertEquals(ActionProof.State.CONSUMED, result.consumedProof().state());
        assertTrue(result.requiresSessionInvalidation());
        assertEquals(IdentityAccount.Status.PENDING_ACTIVATION, pending.status());
        assertEquals(ActionProof.State.AVAILABLE, proof.state());
    }

    @Test void activationRejectsWrongSubjectPurposeExpiredProofAndWeakPassword() {
        var pending = pending(); var proof = ActionProof.invitation(id(20), pending, NOW);
        var foreign = ActionProof.invitation(id(21), account(OTHER_ID, pending.status(), pending.activation(), Set.of()), NOW);
        rejected(PROOF_INVALID, () -> pending.activate(foreign, PASSWORD, 1, NOW));
        rejected(PROOF_INVALID, () -> pending.activate(ActionProof.recovery(id(22), user(), IdentityAccount.EmailMatch.MATCH, NOW), PASSWORD, 1, NOW));
        rejected(PROOF_INVALID, () -> pending.activate(proof, PASSWORD, 1, NOW.plusSeconds(86400)));
        rejected(INVALID_PASSWORD, () -> pending.activate(proof, "short", 1, NOW));
    }

    @Test void activationRejectsDisabledPendingAndAlreadyActiveAccounts() {
        var pending = pending(); var proof = ActionProof.invitation(id(20), pending, NOW);
        var disabled = pending.disable(admin(), authority(admin(), pending, Set.of(), Set.of()), 1, 1);
        rejected(INVALID_STATE, () -> disabled.activate(proof, PASSWORD, 2, NOW));
        rejected(INVALID_STATE, () -> user().activate(proof, PASSWORD, 1, NOW));
        rejected(INVALID_STATE, () -> disabled.reactivate(admin(), authority(admin(), disabled, Set.of(), Set.of()), 2));
    }

    @Test void disableAndReactivatePreserveActivationButNeverRestoreOldSessions() {
        var user = user(); var old = IdentitySession.open(SESSION_ID, user, NOW);
        var disabled = user.disable(admin(), authority(admin(), user, user.grants(), Set.of()), 1, 1);
        assertEquals(IdentityAccount.Status.DISABLED, disabled.status());
        var restored = disabled.reactivate(admin(), authority(admin(), disabled, disabled.grants(), Set.of()), 2);
        assertTrue(restored.active()); assertEquals(new AccountVersion(3, 3), restored.version());
        rejected(SESSION_INVALID, () -> old.admit(disabled, NOW));
        rejected(SESSION_INVALID, () -> old.admit(restored, NOW));
        old.admit(user, NOW); // Original immutable snapshot remains unchanged, not current authority.
    }

    @Test void repeatedDisableAndInvalidReactivationAreRejected() {
        var user = user(); var disabled = user.disable(admin(), authority(admin(), user, user.grants(), Set.of()), 1, 1);
        rejected(INVALID_STATE, () -> disabled.disable(admin(), authority(admin(), disabled, disabled.grants(), Set.of()), 2, 1));
        rejected(INVALID_STATE, () -> user.reactivate(admin(), authority(admin(), user, user.grants(), Set.of()), 1));
        var pending = pending();
        rejected(INVALID_STATE, () -> pending.reactivate(admin(), authority(admin(), pending, Set.of(), Set.of()), 1));
        var disabledPending = pending.disable(admin(), authority(admin(), pending, Set.of(), Set.of()), 1, 1);
        rejected(INVALID_STATE, () -> disabledPending.disable(admin(), authority(admin(), disabledPending, Set.of(), Set.of()), 2, 1));
    }

    @Test void passwordResetConsumesRecoveryAndInvalidatesPreviousAuthVersion() {
        var user = user(); var proof = ActionProof.recovery(id(21), user, IdentityAccount.EmailMatch.MATCH, NOW);
        var result = user.resetPassword(proof, PASSWORD, 1, NOW.plusSeconds(1));
        assertEquals(new AccountVersion(2, 2), result.account().version());
        assertEquals(ActionProof.State.CONSUMED, result.consumedProof().state());
        rejected(SESSION_INVALID, () -> IdentitySession.open(SESSION_ID, user, NOW).admit(result.account(), NOW));
        rejected(PROOF_INVALID, () -> user.resetPassword(result.consumedProof(), PASSWORD, 1, NOW.plusSeconds(2)));
    }

    @Test void recoveryRequiresActiveVerifiedAccountAndStoredEmailMatch() {
        assertTrue(user().recoveryEligible(IdentityAccount.EmailMatch.MATCH));
        assertFalse(user().recoveryEligible(IdentityAccount.EmailMatch.NO_MATCH));
        assertFalse(user().recoveryEligible(null));
        rejected(PROOF_INVALID, () -> ActionProof.recovery(id(22), user(), IdentityAccount.EmailMatch.NO_MATCH, NOW));
        rejected(PROOF_INVALID, () -> ActionProof.recovery(id(22), user(), null, NOW));
        assertFalse(pending().recoveryEligible(IdentityAccount.EmailMatch.MATCH));
        var disabled = account(USER_ID, IdentityAccount.Status.DISABLED, IdentityAccount.Activation.VERIFIED, Set.of());
        assertFalse(disabled.recoveryEligible(IdentityAccount.EmailMatch.MATCH));
        rejected(PROOF_INVALID, () -> ActionProof.recovery(id(22), disabled, IdentityAccount.EmailMatch.MATCH, NOW));
    }

    @Test void restoreRejectsImpossibleActivationCombinations() {
        rejected(INVALID_STATE, () -> account(USER_ID, IdentityAccount.Status.ACTIVE, IdentityAccount.Activation.UNVERIFIED, Set.of()));
        rejected(INVALID_STATE, () -> account(USER_ID, IdentityAccount.Status.PENDING_ACTIVATION, IdentityAccount.Activation.VERIFIED, Set.of()));
        assertDoesNotThrow(() -> account(USER_ID, IdentityAccount.Status.DISABLED, IdentityAccount.Activation.UNVERIFIED, Set.of()));
        assertDoesNotThrow(() -> account(USER_ID, IdentityAccount.Status.DISABLED, IdentityAccount.Activation.VERIFIED, Set.of()));
    }

    @Test void staleVersionPreventsLifecycleOrProofCommitResult() {
        var user = user(); var pending = pending();
        rejected(VERSION_CONFLICT, () -> user.disable(admin(), authority(admin(), user, user.grants(), Set.of()), 2, 1));
        var proof = ActionProof.invitation(id(20), pending, NOW);
        rejected(VERSION_CONFLICT, () -> pending.activate(proof, PASSWORD, 9, NOW));
        assertEquals(ActionProof.State.AVAILABLE, proof.state());
    }
}
