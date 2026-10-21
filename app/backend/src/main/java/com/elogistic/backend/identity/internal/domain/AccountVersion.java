package com.elogistic.backend.identity.internal.domain;

import static com.elogistic.backend.identity.internal.domain.DomainValues.require;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.*;

public record AccountVersion(long version, long authVersion) {
    public AccountVersion { require(version > 0 && authVersion > 0, INVALID_STATE); }

    public void requireExpected(long expected) { require(expected == version, VERSION_CONFLICT); }

    /** Every modeled security change invalidates earlier session authVersions. */
    public AccountVersion next() {
        require(version < Long.MAX_VALUE && authVersion < Long.MAX_VALUE, VERSION_EXHAUSTED);
        return new AccountVersion(version + 1, authVersion + 1);
    }
}
