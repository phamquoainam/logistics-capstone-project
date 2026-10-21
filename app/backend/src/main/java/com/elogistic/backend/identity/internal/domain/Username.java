package com.elogistic.backend.identity.internal.domain;

import static com.elogistic.backend.identity.internal.domain.DomainValues.require;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.INVALID_USERNAME;

/** Exact accepted identity sequence, without trim/case-fold/normalization. */
public record Username(String value) {
    public Username {
        int length = DomainValues.codePoints(value, INVALID_USERNAME);
        require(length >= 1 && length <= 80, INVALID_USERNAME);
        require(value.codePoints().anyMatch(c -> !contractWhitespace(c)), INVALID_USERNAME);
    }

    // Same ECMAScript whitespace set as Username /\S/ and V3 validation.
    private static boolean contractWhitespace(int c) {
        return (c >= 0x09 && c <= 0x0d) || c == 0x20 || c == 0xa0 || c == 0x1680
            || (c >= 0x2000 && c <= 0x200a) || c == 0x2028 || c == 0x2029
            || c == 0x202f || c == 0x205f || c == 0x3000 || c == 0xfeff;
    }

    @Override public String toString() { return "Username[redacted]"; }
}
