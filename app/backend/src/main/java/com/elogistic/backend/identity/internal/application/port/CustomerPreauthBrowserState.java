package com.elogistic.backend.identity.internal.application.port;

import java.util.UUID;

/** Resolves only a current CUSTOMER, session-unbound preauthentication CSRF row. */
@FunctionalInterface
public interface CustomerPreauthBrowserState {
    UUID requirePreauthentication(String cookie,String challenge);
}
