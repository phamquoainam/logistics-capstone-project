package com.elogistic.backend.identity.internal.domain;

import static com.elogistic.backend.identity.internal.domain.DomainValues.require;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.INVALID_GRANT;

/** Contract codes are open strings, not an invented permission catalog. */
public record RoleCode(String value) {
    public static final RoleCode IDENTITY_MANAGEMENT = new RoleCode("IDENTITY_MANAGEMENT");

    public RoleCode {
        int length = DomainValues.codePoints(value, INVALID_GRANT);
        require(length >= 1 && length <= 80, INVALID_GRANT);
    }
}
