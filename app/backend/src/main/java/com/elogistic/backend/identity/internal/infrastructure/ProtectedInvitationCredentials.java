package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.time.Instant;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Explicit V5 HMAC verifier + purpose-separated keyed input fingerprint + BCrypt12.
 * Never decrypts an outbox, scans all proofs, reads keys from disk or logs supplied values. */
public final class ProtectedInvitationCredentials implements InvitationCredentials {
    private final InvitationProtection protection;
    private final BCryptPasswordEncoder passwords=new BCryptPasswordEncoder(12);
    public ProtectedInvitationCredentials(InvitationProtection protection){this.protection=Objects.requireNonNull(protection);}
    @Override public boolean matches(IdentityInvitationStore.StoredProof stored,byte[] token,Instant now) {
        var p=stored.proof();
        if(p.purpose()!=ActionProof.Purpose.INVITATION || p.state()==ActionProof.State.REVOKED
            || !p.expiresAt().equals(p.createdAt().plusSeconds(86400)))return false;
        // Authenticate cryptographic identity even on a consumed row ONLY so the application
        // can check an exact retained receipt; this does not authorize consuming it again.
        var identity=new ActionProof(p.id(),p.accountId(),p.purpose(),p.createdAt(),p.expiresAt(),ActionProof.State.AVAILABLE,null);
        byte[] expected=stored.hash();
        try {return protection.matches(identity,stored.keyId(),token,expected,now);}
        finally {InvitationProtection.wipe(expected);}
    }
    @Override public byte[] fingerprint(IdentityInvitationStore.StoredProof stored,byte[] token,String password) {
        PasswordPolicy.validateNew(password);
        return protection.acceptanceFingerprint(stored.proof(),stored.keyId(),token,password);
    }
    @Override public String hashPassword(String password) {
        PasswordPolicy.validateNew(password);return passwords.encode(password);
    }
}
