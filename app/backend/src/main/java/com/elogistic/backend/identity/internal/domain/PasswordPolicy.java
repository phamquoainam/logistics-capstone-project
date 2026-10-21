package com.elogistic.backend.identity.internal.domain;

import static com.elogistic.backend.identity.internal.domain.DomainValues.require;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.INVALID_PASSWORD;

/** Validation only. Does not retain, hash, transform or log passwords. */
public final class PasswordPolicy {
    public static void validateNew(String password) { validate(password, 15); }
    public static void validateLogin(String password) { validate(password, 1); }

    private static void validate(String password, int minimum) {
        int count = DomainValues.codePoints(password, INVALID_PASSWORD);
        require(count >= minimum && count <= 72, INVALID_PASSWORD);
        int bytes = 0;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isHighSurrogate(c)) { bytes += 4; i++; }
            else bytes += c < 0x80 ? 1 : c < 0x800 ? 2 : 3;
            require(bytes <= 72, INVALID_PASSWORD);
        }
    }

    private PasswordPolicy() {}
}
