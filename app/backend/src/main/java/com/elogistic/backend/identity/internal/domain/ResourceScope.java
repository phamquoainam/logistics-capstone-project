package com.elogistic.backend.identity.internal.domain;

import java.util.UUID;
import static com.elogistic.backend.identity.internal.domain.DomainValues.require;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.INVALID_GRANT;

/** External IDs are references; this value alone proves no ownership/assignability. */
public record ResourceScope(Type type, UUID id) {
    public enum Type { IDENTITY, MERCHANT, WAREHOUSE, HUB, ASSIGNMENT }

    public ResourceScope { require(type != null && id != null, INVALID_GRANT); }
}
