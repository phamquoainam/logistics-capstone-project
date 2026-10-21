package com.elogistic.backend.identity.internal.domain;

import static com.elogistic.backend.identity.internal.domain.DomainValues.require;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.INVALID_GRANT;

public record Grant(RoleCode role, ResourceScope scope) {
    public Grant { require(role != null && scope != null, INVALID_GRANT); }

    public boolean isIdentityManagement() {
        return role.equals(RoleCode.IDENTITY_MANAGEMENT) && scope.type() == ResourceScope.Type.IDENTITY;
    }
}
