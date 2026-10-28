package com.elogistic.backend.identity.internal.infrastructure;

/** Fixed Identity-owned names only; never selected by an HTTP value or a business module. */
public enum IdentitySessionStorage {
    SYSTEM("auth_session", "refresh_token", "csrf_binding"),
    CUSTOMER("customer_auth_session", "customer_refresh_token", "customer_csrf_binding");

    final String session, refresh, csrf;
    IdentitySessionStorage(String session, String refresh, String csrf) {
        this.session = "identity." + session;
        this.refresh = "identity." + refresh;
        this.csrf = "identity." + csrf;
    }
}
