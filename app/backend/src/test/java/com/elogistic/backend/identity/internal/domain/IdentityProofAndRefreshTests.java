package com.elogistic.backend.identity.internal.domain;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.elogistic.backend.identity.internal.domain.DomainFixtures.*;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.*;

class IdentityProofAndRefreshTests {
    @Test void proofLifetimesAndExactExpiryBoundariesMatchAcceptedPolicy() {
        var invitation = ActionProof.invitation(id(20), pending(), NOW);
        var recovery = ActionProof.recovery(id(21), user(), IdentityAccount.EmailMatch.MATCH, NOW);
        assertEquals(NOW.plusSeconds(86400), invitation.expiresAt());
        assertEquals(NOW.plusSeconds(900), recovery.expiresAt());
        assertEquals(ActionProof.State.CONSUMED, invitation.consume(ActionProof.Purpose.INVITATION, USER_ID, invitation.expiresAt().minusNanos(1)).state());
        assertEquals(ActionProof.State.CONSUMED, recovery.consume(ActionProof.Purpose.RECOVERY, USER_ID, recovery.expiresAt().minusNanos(1)).state());
        for (var proof : java.util.List.of(invitation, recovery)) {
            rejected(PROOF_INVALID, () -> proof.consume(proof.purpose(), USER_ID, proof.expiresAt()));
            rejected(PROOF_INVALID, () -> proof.consume(proof.purpose(), USER_ID, proof.expiresAt().plusNanos(1)));
        }
    }

    @Test void consumedOrRevokedProofCannotBeReusedOrResurrected() {
        var proof = ActionProof.invitation(id(20), pending(), NOW);
        var consumed = proof.consume(proof.purpose(), USER_ID, NOW);
        rejected(PROOF_INVALID, () -> consumed.consume(proof.purpose(), USER_ID, NOW));
        assertSame(consumed, consumed.revoke(NOW));
        var revoked = proof.revoke(NOW);
        rejected(PROOF_INVALID, () -> revoked.consume(proof.purpose(), USER_ID, NOW));
        assertSame(revoked, revoked.revoke(NOW));
        assertEquals("ActionProof[redacted]", proof.toString());
    }

    @Test void proofRejectsWrongPurposeSubjectAndInvalidTime() {
        var proof = ActionProof.invitation(id(20), pending(), NOW);
        rejected(PROOF_INVALID, () -> proof.consume(ActionProof.Purpose.RECOVERY, USER_ID, NOW));
        rejected(PROOF_INVALID, () -> proof.consume(proof.purpose(), OTHER_ID, NOW));
        rejected(PROOF_INVALID, () -> proof.consume(proof.purpose(), USER_ID, NOW.minusNanos(1)));
        rejected(PROOF_INVALID, () -> proof.consume(proof.purpose(), USER_ID, null));
    }

    @Test void proofRestoreAndIssueRejectImpossibleStatesOrEligibility() {
        rejected(PROOF_INVALID, () -> new ActionProof(id(20), USER_ID, ActionProof.Purpose.RECOVERY, NOW,
            NOW.plusSeconds(901), ActionProof.State.AVAILABLE, null));
        rejected(PROOF_INVALID, () -> new ActionProof(id(20), USER_ID, ActionProof.Purpose.INVITATION, NOW,
            NOW.plusSeconds(10), ActionProof.State.CONSUMED, NOW.plusSeconds(11)));
        rejected(PROOF_INVALID, () -> ActionProof.invitation(id(20), user(), NOW));
        rejected(PROOF_INVALID, () -> ActionProof.recovery(id(20), pending(), IdentityAccount.EmailMatch.MATCH, NOW));
    }

    @Test void refreshRotationConsumesPreviousMemberAndPreservesSessionDeadlines() {
        var session = IdentitySession.open(SESSION_ID, user(), NOW);
        var family = RefreshFamily.issue(session, user(), id(30), NOW);
        var rotated = family.rotate(session, user(), id(30), id(31), NOW.plusSeconds(1));
        assertEquals(RefreshFamily.Outcome.ROTATED, rotated.outcome());
        assertEquals(id(31), rotated.family().currentTokenId());
        assertEquals(Set.of(id(30)), rotated.family().consumedTokenIds());
        assertSame(session, rotated.session()); assertEquals(NOW.plusSeconds(1800), rotated.session().idleExpiresAt());
        assertTrue(family.consumedTokenIds().isEmpty());
        assertEquals("RefreshFamily[redacted]", family.toString());
    }

    @Test void consumedRefreshReplayRevokesFamilyAndSessionWithoutGrace() {
        var session = IdentitySession.open(SESSION_ID, user(), NOW);
        var family = RefreshFamily.issue(session, user(), id(30), NOW).rotate(session, user(), id(30), id(31), NOW).family();
        var replay = family.rotate(session, user(), id(30), id(32), NOW);
        assertEquals(RefreshFamily.Outcome.REPLAY_REVOKED, replay.outcome());
        assertEquals(RefreshFamily.State.REVOKED, replay.family().state());
        assertNotNull(replay.session().revokedAt());
        rejected(SESSION_INVALID, () -> replay.session().admit(user(), NOW));
    }

    @Test void revokedFamilyRejectsCurrentTokenRatherThanRotating() {
        var session = IdentitySession.open(SESSION_ID, user(), NOW);
        var family = RefreshFamily.issue(session, user(), id(30), NOW).revoke();
        var result = family.rotate(session, user(), id(30), id(31), NOW);
        assertEquals(RefreshFamily.Outcome.REJECTED, result.outcome());
        assertSame(family, result.family()); assertNotNull(result.session().revokedAt());
    }

    @Test void unknownRefreshDoesNotRevokeLegitimateFamilyAndSuccessorCannotBeReused() {
        var session = IdentitySession.open(SESSION_ID, user(), NOW);
        var family = RefreshFamily.issue(session, user(), id(30), NOW);
        rejected(REFRESH_INVALID, () -> family.rotate(session, user(), id(99), id(31), NOW));
        assertEquals(RefreshFamily.State.ACTIVE, family.state());
        rejected(REFRESH_INVALID, () -> family.rotate(session, user(), id(30), id(30), NOW));
        var rotated = family.rotate(session, user(), id(30), id(31), NOW).family();
        rejected(REFRESH_INVALID, () -> rotated.rotate(session, user(), id(31), id(30), NOW));
    }

    @Test void refreshRequiresMatchingFamilyAccountAndCurrentSession() {
        var session = IdentitySession.open(SESSION_ID, user(), NOW);
        var family = RefreshFamily.issue(session, user(), id(30), NOW);
        var otherSession = IdentitySession.open(id(5), user(), NOW);
        rejected(REFRESH_INVALID, () -> family.rotate(otherSession, user(), id(30), id(31), NOW));
        rejected(REFRESH_INVALID, () -> family.rotate(session, admin(), id(30), id(31), NOW));
        rejected(SESSION_INVALID, () -> family.rotate(session, user(), id(30), id(31), NOW.plusSeconds(1800)));
        rejected(SESSION_INVALID, () -> family.rotate(session.revoke(NOW), user(), id(30), id(31), NOW));
    }

    @Test void refreshExpiryAndRestoredMemberStateAreValidated() {
        var session = IdentitySession.open(SESSION_ID, user(), NOW);
        var family = new RefreshFamily(SESSION_ID, id(30), NOW.plusSeconds(10), Set.of(), RefreshFamily.State.ACTIVE);
        assertEquals(RefreshFamily.Outcome.ROTATED, family.rotate(session, user(), id(30), id(31), NOW.plusSeconds(9)).outcome());
        rejected(REFRESH_INVALID, () -> family.rotate(session, user(), id(30), id(31), NOW.plusSeconds(10)));
        rejected(REFRESH_INVALID, () -> new RefreshFamily(SESSION_ID, id(30), NOW, Set.of(id(30)), RefreshFamily.State.ACTIVE));
        assertThrows(UnsupportedOperationException.class, () -> family.consumedTokenIds().add(id(31)));
    }
}
