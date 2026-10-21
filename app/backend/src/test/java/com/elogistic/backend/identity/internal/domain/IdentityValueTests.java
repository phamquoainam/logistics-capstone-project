package com.elogistic.backend.identity.internal.domain;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.elogistic.backend.identity.internal.domain.DomainFixtures.*;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.*;

class IdentityValueTests {
    @Test void usernamePreservesExactCaseUnicodeAndPadding() {
        assertNotEquals(new Username("User"), new Username("user"));
        assertNotEquals(new Username("\u00e9"), new Username("e\u0301"));
        assertEquals(" user ", new Username(" user ").value());
    }

    @Test void usernameUsesCodePointsAndContractWhitespaceBoundaries() {
        assertEquals(80, new Username("\ud83d\ude80".repeat(80)).value().codePointCount(0, 160));
        for (String value : List.of("", " \t\n", "\u00a0\u3000\ufeff", "a".repeat(81), "\ud800", "\udc00"))
            rejected(INVALID_USERNAME, () -> new Username(value));
        rejected(INVALID_USERNAME, () -> new Username(null));
        // U+0085 is not ECMAScript WhiteSpace: do not silently switch to Java isBlank semantics.
        assertEquals("\u0085", new Username("\u0085").value());
    }

    @Test void newPasswordEnforcesCodePointMinimumAndUtf8Maximum() {
        PasswordPolicy.validateNew("x".repeat(15));
        PasswordPolicy.validateNew("x".repeat(72));
        PasswordPolicy.validateNew("\u00e9".repeat(36));
        PasswordPolicy.validateNew("\ud83d\ude80".repeat(18));
        for (String value : List.of("x".repeat(14), "x".repeat(73), "\u00e9".repeat(37), "\ud83d\ude80".repeat(19),
                "\ud83d\ude80".repeat(14))) rejected(INVALID_PASSWORD, () -> PasswordPolicy.validateNew(value));
    }

    @Test void loginAllowsLegacyMinimumWithoutRelaxingByteLimit() {
        PasswordPolicy.validateLogin("x");
        PasswordPolicy.validateLogin("\u00e9".repeat(36));
        rejected(INVALID_PASSWORD, () -> PasswordPolicy.validateLogin(""));
        rejected(INVALID_PASSWORD, () -> PasswordPolicy.validateLogin("\u00e9".repeat(37)));
    }

    @Test void passwordRejectsMalformedUnicodeAndDoesNotTransformInput() {
        String padded = "  Synthetic-password  ";
        PasswordPolicy.validateNew(padded);
        assertEquals("  Synthetic-password  ", padded);
        for (String value : List.of("x".repeat(15) + "\ud800", "x".repeat(15) + "\udc00")) {
            rejected(INVALID_PASSWORD, () -> PasswordPolicy.validateNew(value));
            rejected(INVALID_PASSWORD, () -> PasswordPolicy.validateLogin(value));
        }
        rejected(INVALID_PASSWORD, () -> PasswordPolicy.validateNew(null));
        rejected(INVALID_PASSWORD, () -> PasswordPolicy.validateLogin(null));
    }

    @Test void valueObjectsRejectMissingScopeAndDoNotInventRoleCatalog() {
        assertEquals("custom.reviewed.role", new RoleCode("custom.reviewed.role").value());
        rejected(INVALID_GRANT, () -> new RoleCode(""));
        rejected(INVALID_GRANT, () -> new RoleCode("x".repeat(81)));
        rejected(INVALID_GRANT, () -> new ResourceScope(null, id(1)));
        rejected(INVALID_GRANT, () -> new ResourceScope(ResourceScope.Type.MERCHANT, null));
        rejected(INVALID_GRANT, () -> new Grant(null, MERCHANT_A.scope()));
        assertFalse(grant("IDENTITY_MANAGEMENT", ResourceScope.Type.MERCHANT, 10).isIdentityManagement());
    }

    @Test void versionsRejectInvalidStaleAndOverflowWithoutWrapping() {
        rejected(INVALID_STATE, () -> new AccountVersion(0, 1));
        rejected(INVALID_STATE, () -> new AccountVersion(1, 0));
        rejected(VERSION_CONFLICT, () -> new AccountVersion(2, 3).requireExpected(1));
        assertEquals(new AccountVersion(3, 4), new AccountVersion(2, 3).next());
        rejected(VERSION_EXHAUSTED, () -> new AccountVersion(Long.MAX_VALUE, 1).next());
        rejected(VERSION_EXHAUSTED, () -> new AccountVersion(1, Long.MAX_VALUE).next());
    }

    @Test void collectionsAreDefensiveAndDuplicateInputIsRejected() {
        var grants = new java.util.HashSet<>(Set.of(MERCHANT_A));
        var user = account(USER_ID, IdentityAccount.Status.ACTIVE, IdentityAccount.Activation.VERIFIED, grants);
        grants.clear(); assertEquals(Set.of(MERCHANT_A), user.grants());
        assertThrows(UnsupportedOperationException.class, () -> user.grants().clear());
        rejected(INVALID_GRANT, () -> DomainValues.unique(List.of(MERCHANT_A, MERCHANT_A)));
        rejected(INVALID_GRANT, () -> DomainValues.unique(java.util.Arrays.asList(MERCHANT_A, null)));
    }

    @Test void sensitiveValuesAndExceptionsHaveRedactedRepresentations() {
        assertEquals("Username[redacted]", new Username("fixture-private-name").toString());
        assertEquals("IdentityAccount[redacted]", user().toString());
        String input = "synthetic-invalid-password-".repeat(4);
        var error = assertThrows(DomainViolation.class, () -> PasswordPolicy.validateNew(input));
        assertFalse(error.toString().contains(input));
        assertNull(error.getCause());
    }
}
