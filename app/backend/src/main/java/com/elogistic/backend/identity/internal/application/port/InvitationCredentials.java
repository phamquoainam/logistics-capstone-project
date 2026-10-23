package com.elogistic.backend.identity.internal.application.port;

import java.time.Instant;

/** Trusted crypto/password adapter. No plaintext persistence, transport or environment lookup.
 * Matching authenticates retained proof identity; the application alone admits consumption/replay. */
public interface InvitationCredentials {
    boolean matches(IdentityInvitationStore.StoredProof proof,byte[] token,Instant now);
    byte[] fingerprint(IdentityInvitationStore.StoredProof proof,byte[] token,String password);
    String hashPassword(String password);
}
