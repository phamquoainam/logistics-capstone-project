package com.elogistic.backend.identity.internal.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.elogistic.backend.identity.internal.domain.DomainFixtures.*;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.*;

class IdentitySessionTests {
    @Test void openingSessionSetsAcceptedAbsoluteIdleAndAccessBounds() {
        var session = IdentitySession.open(SESSION_ID, user(), NOW);
        assertEquals(NOW.plusSeconds(28800), session.absoluteExpiresAt());
        assertEquals(NOW.plusSeconds(1800), session.idleExpiresAt());
        assertEquals(NOW.plusSeconds(900), session.accessExpiresAt(user(), NOW));
        assertEquals(user().version().authVersion(), session.authVersion());
    }

    @Test void idleAdmissionIsValidBeforeDeadlineButNotAtOrAfter() {
        var session = IdentitySession.open(SESSION_ID, user(), NOW);
        session.admit(user(), NOW.plusSeconds(1799));
        rejected(SESSION_INVALID, () -> session.admit(user(), NOW.plusSeconds(1800)));
        rejected(SESSION_INVALID, () -> session.admit(user(), NOW.plusSeconds(1801)));
        rejected(SESSION_INVALID, () -> session.admit(user(), NOW.minusNanos(1)));
    }

    @Test void explicitActivityExtendsIdleButPollingAndRefreshDoNot() {
        var session = IdentitySession.open(SESSION_ID, user(), NOW);
        var active = session.activity(user(), IdentitySession.Activity.USER_ACTION, NOW.plusSeconds(60));
        assertEquals(NOW.plusSeconds(1860), active.idleExpiresAt());
        assertEquals(NOW.plusSeconds(60), active.lastActivityAt());
        for (var kind : IdentitySession.Activity.values())
            if (kind != IdentitySession.Activity.USER_ACTION)
                assertSame(session, session.activity(user(), kind, NOW.plusSeconds(60)));
        rejected(SESSION_INVALID, () -> session.activity(user(), null, NOW));
    }

    @Test void absoluteBoundaryCapsIdleAndJwtEvenAfterMeaningfulActivity() {
        var session = new IdentitySession(SESSION_ID, USER_ID, 1, NOW, NOW.plusSeconds(28800),
            NOW.plusSeconds(28000), NOW.plusSeconds(28800), null);
        assertEquals(NOW.plusSeconds(28800), session.accessExpiresAt(user(), NOW.plusSeconds(28799)));
        assertEquals(NOW.plusSeconds(28800), session.activity(user(), IdentitySession.Activity.USER_ACTION,
            NOW.plusSeconds(28799)).idleExpiresAt());
        rejected(SESSION_INVALID, () -> session.admit(user(), NOW.plusSeconds(28800)));
        rejected(SESSION_INVALID, () -> session.admit(user(), NOW.plusSeconds(28801)));
    }

    @Test void jwtLifetimeAlsoCannotExceedIdleDeadline() {
        var session = IdentitySession.open(SESSION_ID, user(), NOW);
        assertEquals(NOW.plusSeconds(1800), session.accessExpiresAt(user(), NOW.plusSeconds(1799)));
    }

    @Test void revokedSessionCannotBeAdmittedAndRevocationIsIdempotent() {
        var session = IdentitySession.open(SESSION_ID, user(), NOW);
        var revoked = session.revoke(NOW.plusSeconds(1));
        rejected(SESSION_INVALID, () -> revoked.admit(user(), NOW.plusSeconds(1)));
        assertSame(revoked, revoked.revoke(NOW.plusSeconds(2)));
        assertEquals(NOW.plusSeconds(1), revoked.revokedAt());
        assertEquals("IdentitySession[redacted]", revoked.toString());
    }

    @Test void accountBindingStatusAndAuthVersionAreRechecked() {
        var session = IdentitySession.open(SESSION_ID, user(), NOW);
        var foreign = account(OTHER_ID, IdentityAccount.Status.ACTIVE, IdentityAccount.Activation.VERIFIED, user().grants());
        var newer = new IdentityAccount(USER_ID, user().username(), user().status(), user().activation(), new AccountVersion(2, 2), user().grants());
        var disabled = account(USER_ID, IdentityAccount.Status.DISABLED, IdentityAccount.Activation.VERIFIED, user().grants());
        for (var invalid : java.util.List.of(foreign, newer, disabled, pending()))
            rejected(SESSION_INVALID, () -> session.admit(invalid, NOW));
        rejected(SESSION_INVALID, () -> session.admit(null, NOW));
    }

    @Test void invalidRestoredDeadlinesAreRejected() {
        rejected(SESSION_INVALID, () -> new IdentitySession(SESSION_ID, USER_ID, 1, NOW, NOW.plusSeconds(28801), NOW, NOW.plusSeconds(1800), null));
        rejected(SESSION_INVALID, () -> new IdentitySession(SESSION_ID, USER_ID, 1, NOW, NOW.plusSeconds(28800), NOW, NOW.plusSeconds(1801), null));
        rejected(SESSION_INVALID, () -> new IdentitySession(SESSION_ID, USER_ID, 1, NOW, NOW.plusSeconds(28800), NOW, NOW, null));
        rejected(SESSION_INVALID, () -> new IdentitySession(SESSION_ID, USER_ID, 1, NOW, NOW.plusSeconds(28800), NOW, NOW.plusSeconds(1800), NOW.minusSeconds(1)));
        rejected(SESSION_INVALID, () -> IdentitySession.open(SESSION_ID, pending(), NOW));
    }

    @Test void expiredActivityCannotResurrectSessionAndClockRollbackIsDenied() {
        var session = IdentitySession.open(SESSION_ID, user(), NOW);
        rejected(SESSION_INVALID, () -> session.activity(user(), IdentitySession.Activity.USER_ACTION, NOW.plusSeconds(1800)));
        var updated = session.activity(user(), IdentitySession.Activity.USER_ACTION, NOW.plusSeconds(60));
        rejected(SESSION_INVALID, () -> updated.admit(user(), NOW.plusSeconds(59)));
        rejected(SESSION_INVALID, () -> session.revoke(NOW.minusNanos(1)));
    }
}
