package com.elogistic.backend.identity.api;

import java.util.UUID;

/** Minimal trusted principal view populated by backend authentication, never request data. */
public interface AuthenticatedActor {
    UUID id();
    boolean enabled();
    String role();
    UUID merchantId();
}
