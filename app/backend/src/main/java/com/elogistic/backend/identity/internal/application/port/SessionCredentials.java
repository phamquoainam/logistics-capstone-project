package com.elogistic.backend.identity.internal.application.port;

/** Crypto port; verify must perform a dummy BCrypt comparison when no stored verifier exists. */
public interface SessionCredentials {
    boolean verify(String password, String verifier);
    byte[] newRefreshToken();
    byte[] digest(byte[] token);
}
