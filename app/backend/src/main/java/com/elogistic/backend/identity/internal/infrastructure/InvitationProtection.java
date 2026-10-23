package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.domain.ActionProof;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/** Explicit external key-ring injection. No environment lookup, default key, provider or logging.
 * Caller owns and must clear its configuration buffers. Keep retired keys until live proofs expire.
 * No raw token String is constructed; all returned token buffers must be closed/wiped by the caller. */
public final class InvitationProtection {
    private final Map<String,SecretKeySpec> verifiers, encryptors;
    private final String verifierId, encryptionId;
    private final SecureRandom random=new SecureRandom();

    public InvitationProtection(Map<String,byte[]> verifierKeys,String activeVerifier,
                                Map<String,byte[]> encryptionKeys,String activeEncryption) {
        verifiers=keys(verifierKeys,"HmacSHA256");encryptors=keys(encryptionKeys,"AES");
        if(!validId(activeVerifier) || !validId(activeEncryption) || !verifiers.containsKey(activeVerifier) || !encryptors.containsKey(activeEncryption)) throw invalid();
        for(var v:verifiers.values()) for(var e:encryptors.values()) {
            byte[] a=v.getEncoded(),b=e.getEncoded();
            try {if(MessageDigest.isEqual(a,b)) throw invalid();}
            finally {wipe(a);wipe(b);}
        }
        verifierId=activeVerifier;encryptionId=activeEncryption;
    }
    private static Map<String,SecretKeySpec> keys(Map<String,byte[]> source,String algorithm) {
        if(source==null || source.isEmpty() || source.size()>16) throw invalid();
        var result=new HashMap<String,SecretKeySpec>();
        source.forEach((id,value)->{
            if(!validId(id) || value==null || value.length!=32) throw invalid();
            int nonzero=0;for(byte b:value)nonzero|=b;
            if(nonzero==0)throw invalid();
            result.put(id,new SecretKeySpec(value,algorithm));
        });
        return Map.copyOf(result);
    }
    static boolean validId(String id) {return id!=null && id.matches("[A-Za-z0-9_-]{1,64}");}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("INVITATION_KEY_CONFIGURATION_INVALID");}
    private static IllegalStateException rejected(){return new IllegalStateException("INVITATION_PROTECTION_REJECTED");}
    static void wipe(byte[] value){if(value!=null)Arrays.fill(value,(byte)0);}

    /** Immutable non-secret AAD. Destination is private contact data: never log this object. */
    public record Context(UUID messageId,UUID proofId,UUID accountId,UUID realmId,String destination,
                          Instant createdAt,Instant expiresAt,String verifierKeyId,String encryptionKeyId) {
        public Context {
            if(messageId==null || proofId==null || accountId==null || realmId==null || destination==null || destination.isEmpty()
                || createdAt==null || expiresAt==null || !createdAt.equals(createdAt.truncatedTo(ChronoUnit.MICROS))
                || !expiresAt.equals(createdAt.plusSeconds(86400)) || !validId(verifierKeyId) || !validId(encryptionKeyId)) throw rejected();
        }
        @Override public String toString(){return "InvitationContext[redacted]";}
    }
    public Context context(ActionProof proof,UUID realm,String destination) {
        if(proof==null || proof.purpose()!=ActionProof.Purpose.INVITATION || proof.state()!=ActionProof.State.AVAILABLE)throw rejected();
        return new Context(UUID.randomUUID(),proof.id(),proof.accountId(),realm,destination,proof.createdAt(),proof.expiresAt(),verifierId,encryptionId);
    }
    /** Holds protected buffers only, no token. No plaintext enters the JDBC boundary. */
    public static final class Sealed implements AutoCloseable {
        private final byte[] verifier,nonce,ciphertext;
        private boolean closed;
        private Sealed(byte[] verifier,byte[] nonce,byte[] ciphertext){this.verifier=verifier;this.nonce=nonce;this.ciphertext=ciphertext;}
        private void check(){if(closed)throw rejected();}
        public byte[] verifier(){check();return verifier.clone();}
        public byte[] nonce(){check();return nonce.clone();}
        public byte[] ciphertext(){check();return ciphertext.clone();}
        @Override public void close(){wipe(verifier);wipe(nonce);wipe(ciphertext);closed=true;}
        @Override public String toString(){return "SealedInvitation[redacted]";}
    }
    public Sealed issue(Context c) {
        // Always select CURRENT configured keys for a new issuance; retired IDs are read-only.
        if(c==null || !verifierId.equals(c.verifierKeyId()) || !encryptionId.equals(c.encryptionKeyId()))throw rejected();
        byte[] entropy=new byte[32],token=null,nonce=new byte[12],hash=null,ciphertext=null;
        try {
            random.nextBytes(entropy);token=Base64.getUrlEncoder().withoutPadding().encode(entropy);random.nextBytes(nonce);
            hash=verifier(c.proofId(),c.accountId(),ActionProof.Purpose.INVITATION,c.createdAt(),c.expiresAt(),c.verifierKeyId(),token);
            ciphertext=crypt(Cipher.ENCRYPT_MODE,c,nonce,token);
            return new Sealed(hash,nonce,ciphertext);
        } catch(RuntimeException failure) {wipe(hash);wipe(nonce);wipe(ciphertext);throw failure;}
        finally {wipe(entropy);wipe(token);}
    }
    /** Caller must verify current DB proof state and atomically consume it in the later activation transaction. */
    public boolean matches(ActionProof proof,String keyId,byte[] supplied,byte[] expected,Instant now) {
        if(proof==null || proof.purpose()!=ActionProof.Purpose.INVITATION || proof.state()!=ActionProof.State.AVAILABLE
            || now==null || now.isBefore(proof.createdAt()) || !now.isBefore(proof.expiresAt())
            || expected==null || expected.length!=32 || !validId(keyId) || !verifiers.containsKey(keyId) || !canonicalToken(supplied))return false;
        byte[] actual=verifier(proof.id(),proof.accountId(),proof.purpose(),proof.createdAt(),proof.expiresAt(),keyId,supplied);
        try {return MessageDigest.isEqual(actual,expected);} finally {wipe(actual);}
    }
    public byte[] open(Context c,byte[] nonce,byte[] ciphertext,Instant now) {
        if(c==null || now==null || now.isBefore(c.createdAt()) || !now.isBefore(c.expiresAt())
            || nonce==null || nonce.length!=12 || ciphertext==null || ciphertext.length!=59)throw rejected();
        byte[] token=crypt(Cipher.DECRYPT_MODE,c,nonce,ciphertext);
        if(!canonicalToken(token)){wipe(token);throw rejected();}
        return token;
    }
    private static boolean canonicalToken(byte[] token) {
        if(token==null || token.length!=43)return false;
        byte[] decoded=null,encoded=null;
        try {
            decoded=Base64.getUrlDecoder().decode(token);encoded=Base64.getUrlEncoder().withoutPadding().encode(decoded);
            return decoded.length==32 && MessageDigest.isEqual(encoded,token);
        }catch(IllegalArgumentException rejected){return false;}
        finally {wipe(decoded);wipe(encoded);}
    }
    private byte[] verifier(UUID proof,UUID account,ActionProof.Purpose purpose,Instant created,Instant expires,String keyId,byte[] token) {
        try {
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(verifiers.get(keyId));
            mac.update(contextBytes("identity-invitation-verifier-v1",proof,account,purpose.name(),created,expires,keyId));
            return mac.doFinal(token);
        }catch(GeneralSecurityException | IllegalArgumentException failure){throw rejected();}
    }
    private byte[] crypt(int mode,Context c,byte[] nonce,byte[] input) {
        try {
            var key=encryptors.get(c.encryptionKeyId());if(key==null)throw rejected();
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(mode,key,new GCMParameterSpec(128,nonce));
            cipher.updateAAD(contextBytes("identity-invitation-delivery-v1",c.messageId(),c.proofId(),c.accountId(),c.realmId(),
                "INVITATION",c.destination(),c.createdAt(),c.expiresAt(),c.verifierKeyId(),c.encryptionKeyId()));
            return cipher.doFinal(input);
        }catch(GeneralSecurityException | IllegalArgumentException failure){throw rejected();}
    }
    // Length prefixes avoid ambiguous concatenation; only non-secret binding fields enter this buffer.
    private static byte[] contextBytes(Object... fields) {
        try {
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
            for(Object field:fields){byte[] value=field.toString().getBytes(StandardCharsets.UTF_8);out.writeInt(value.length);out.write(value);}
            return bytes.toByteArray();
        }catch(IOException impossible){throw rejected();}
    }
    @Override public String toString(){return "InvitationProtection[keys redacted]";}
}
