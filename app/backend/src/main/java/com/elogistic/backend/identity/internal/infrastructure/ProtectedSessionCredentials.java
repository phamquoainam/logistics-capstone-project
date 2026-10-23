package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.internal.application.port.SessionCredentials;
import com.elogistic.backend.identity.internal.domain.PasswordPolicy;
import java.security.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Fresh 256-bit opaque refresh values; only SHA-256 enters persistence. No key/environment lookup. */
public final class ProtectedSessionCredentials implements SessionCredentials {
    private final SecureRandom random=new SecureRandom();
    private final BCryptPasswordEncoder passwords=new BCryptPasswordEncoder(12);
    private final String dummy=passwords.encode(UUID.randomUUID().toString());
    @Override public boolean verify(String password,String verifier) {
        PasswordPolicy.validateLogin(password);
        boolean usable=verifier!=null && verifier.matches("\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}");
        boolean matches=passwords.matches(password,usable?verifier:dummy);
        return usable && matches;
    }
    @Override public byte[] newRefreshToken() {
        byte[] entropy=new byte[32];random.nextBytes(entropy);
        try {return Base64.getUrlEncoder().withoutPadding().encode(entropy);}
        finally {Arrays.fill(entropy,(byte)0);}
    }
    @Override public byte[] digest(byte[] raw) {
        byte[] decoded=null,canonical=null;
        try {
            if(raw==null || raw.length!=43)throw invalid();
            decoded=Base64.getUrlDecoder().decode(raw);canonical=Base64.getUrlEncoder().withoutPadding().encode(decoded);
            if(decoded.length!=32 || !MessageDigest.isEqual(raw,canonical))throw invalid();
            return MessageDigest.getInstance("SHA-256").digest(raw);
        } catch(IllegalArgumentException invalid) {throw invalid();}
        catch(NoSuchAlgorithmException impossible) {throw new IllegalStateException("SESSION_CRYPTO_UNAVAILABLE");}
        finally {if(decoded!=null)Arrays.fill(decoded,(byte)0);if(canonical!=null)Arrays.fill(canonical,(byte)0);}
    }
    private static IdentityCommandFailure invalid() {return new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID);}
    @Override public String toString() {return "ProtectedSessionCredentials[redacted]";}
}
