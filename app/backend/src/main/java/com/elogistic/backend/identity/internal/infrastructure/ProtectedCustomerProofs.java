package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.application.port.CustomerProofProtection;
import com.elogistic.backend.identity.internal.domain.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.*;
import java.time.Instant;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Customer-specific JCE contexts; accepted INVITATION-only V5 protection is never widened. */
public final class ProtectedCustomerProofs implements CustomerProofProtection {
    private final Map<String,SecretKeySpec> verifiers,encryptors;
    private final String verifierId,encryptionId;
    private final SecureRandom random=new SecureRandom();
    private final BCryptPasswordEncoder passwords=new BCryptPasswordEncoder(12);
    public ProtectedCustomerProofs(Map<String,byte[]> verifierKeys,String activeVerifier,Map<String,byte[]> encryptionKeys,String activeEncryption) {
        verifiers=keys(verifierKeys,"HmacSHA256");encryptors=keys(encryptionKeys,"AES");
        if(activeVerifier==null || activeEncryption==null || !verifiers.containsKey(activeVerifier) || !encryptors.containsKey(activeEncryption))throw configuration();
        for(var a:verifiers.values())for(var b:encryptors.values()) {
            byte[] x=a.getEncoded(),y=b.getEncoded();try{if(MessageDigest.isEqual(x,y))throw configuration();}finally{wipe(x);wipe(y);}
        }
        verifierId=activeVerifier;encryptionId=activeEncryption;
    }
    private static Map<String,SecretKeySpec> keys(Map<String,byte[]> values,String algorithm) {
        if(values==null || values.isEmpty() || values.size()>16)throw configuration();
        Map<String,SecretKeySpec> result=new HashMap<>();values.forEach((id,key)->{
            if(id==null || !id.matches("[A-Za-z0-9_-]{1,64}") || key==null || key.length!=32 || Arrays.equals(key,new byte[32]))throw configuration();
            result.put(id,new SecretKeySpec(key,algorithm));
        });return Map.copyOf(result);
    }
    private static IllegalArgumentException configuration(){return new IllegalArgumentException("CUSTOMER_PROOF_CONFIGURATION_INVALID");}
    private static IllegalStateException rejected(){return new IllegalStateException("CUSTOMER_PROOF_PROTECTION_REJECTED");}
    private static void wipe(byte[] value){if(value!=null)Arrays.fill(value,(byte)0);}
    @Override public String activeVerifierKey(){return verifierId;}
    @Override public String activeEncryptionKey(){return encryptionId;}
    @Override public String hashPassword(String value){PasswordPolicy.validateNew(value);return passwords.encode(value);}
    @Override public Protected issue(CustomerProof proof,String destination) {
        if(proof==null || !proof.purpose().possessionRequired())throw rejected();proof.requireAvailable(proof.createdAt());new CustomerEmail(destination);
        byte[] entropy=new byte[32],token=null,nonce=new byte[12],hash=null,ciphertext=null,recipientNonce=new byte[12],recipientCiphertext=null,recipient=destination.getBytes(StandardCharsets.UTF_8);
        try {
            random.nextBytes(entropy);token=Base64.getUrlEncoder().withoutPadding().encode(entropy);
            if(proof.emailOtp()){
                byte[] padded=new byte[43];int code=random.nextInt(1_000_000);
                for(int index=5;index>=0;index--){padded[index]=(byte)('0'+code%10);code/=10;}
                System.arraycopy(token,0,padded,6,37);wipe(token);token=padded;
            }
            random.nextBytes(nonce);
            hash=verifier(proof,verifierId,token);ciphertext=crypt(Cipher.ENCRYPT_MODE,proof,destination,verifierId,encryptionId,nonce,token);
            random.nextBytes(recipientNonce);recipientCiphertext=recipientCrypt(Cipher.ENCRYPT_MODE,proof,verifierId,encryptionId,recipientNonce,recipient);
            return new Protected(verifierId,encryptionId,hash,nonce,ciphertext,recipientNonce,recipientCiphertext);
        }finally{wipe(entropy);wipe(token);wipe(nonce);wipe(hash);wipe(ciphertext);wipe(recipient);wipe(recipientNonce);wipe(recipientCiphertext);}
    }
    @Override public boolean matches(CustomerProof proof,String keyId,byte[] supplied,byte[] expected,Instant now) {
        try{proof.requireAvailable(now);}catch(RuntimeException invalid){return false;}
        if(proof.purpose()==CustomerProof.Purpose.CUSTOMER_RECOVERY && proof.emailOtp() && supplied!=null && supplied.length==43 && keyId!=null && verifiers.containsKey(keyId) && expected!=null && expected.length==32){
            byte[] grant=recoveryGrant(proof,keyId,expected,now);try{return MessageDigest.isEqual(grant,supplied);}finally{wipe(grant);}
        }
        if(!proof.purpose().possessionRequired() || keyId==null || !verifiers.containsKey(keyId) || !canonical(proof,supplied,false) || expected==null || expected.length!=32)return false;
        byte[] actual=verifier(proof,keyId,supplied);try{return MessageDigest.isEqual(actual,expected);}finally{wipe(actual);}
    }
    @Override public byte[] recoveryGrant(CustomerProof proof,String keyId,byte[] expected,Instant now) {
        proof.requireAvailable(now);
        if(proof.purpose()!=CustomerProof.Purpose.CUSTOMER_RECOVERY || !proof.emailOtp() || expected==null || expected.length!=32)throw rejected();
        byte[] raw=mac(keyId,fields("customer-recovery-reset-capability-v1",proof.id(),proof.subjectId(),proof.createdAt(),proof.expiresAt(),proof.authVersion(),keyId),expected);
        try{return Base64.getUrlEncoder().withoutPadding().encode(raw);}finally{wipe(raw);}
    }
    @Override public byte[] fingerprint(String retainedKeyId,String operation,String... exactFields) {
        if(retainedKeyId==null || operation==null || !operation.matches("[A-Za-z][A-Za-z0-9]{0,79}") || exactFields==null || exactFields.length>20)throw rejected();
        // Stream credential fields directly into the MAC; no aggregate plaintext password buffer.
        try {
            var key=verifiers.get(retainedKeyId);if(key==null)throw rejected();
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(key);
            mac.update(fields("customer-request-fingerprint-v1",operation,retainedKeyId));
            for(String field:exactFields) {
                byte[] encoded=field==null?null:field.getBytes(StandardCharsets.UTF_8);
                try {mac.update(ByteBuffer.allocate(4).putInt(encoded==null?-1:encoded.length).array());if(encoded!=null)mac.update(encoded);}
                finally {wipe(encoded);}
            }
            return mac.doFinal();
        }catch(GeneralSecurityException failure){throw rejected();}
    }
    @Override public Opened open(CustomerProof proof,String destination,String verifierKeyId,String encryptionKeyId,byte[] nonce,byte[] ciphertext,Instant now) {
        proof.requireAvailable(now);new CustomerEmail(destination);
        if(!proof.purpose().possessionRequired() || verifierKeyId==null || encryptionKeyId==null || !verifiers.containsKey(verifierKeyId)
            || nonce==null || nonce.length!=12 || ciphertext==null || ciphertext.length!=59)throw rejected();
        byte[] token=crypt(Cipher.DECRYPT_MODE,proof,destination,verifierKeyId,encryptionKeyId,nonce,ciphertext);
        try{if(!canonical(proof,token,true))throw rejected();return new Opened(token);}finally{wipe(token);}
    }
    private byte[] verifier(CustomerProof proof,String keyId,byte[] token) {
        byte[] input=proof.emailOtp()?Arrays.copyOf(token,6):token;
        try{return mac(keyId,fields("customer-possession-verifier-v1",proof.id(),proof.subjectId(),proof.purpose(),proof.createdAt(),proof.expiresAt(),
            proof.sessionId(),proof.authVersion(),proof.organizationId(),keyId),input);}
        finally{if(input!=token)wipe(input);}
    }
    @Override public String openDestination(CustomerProof proof,String verifierKeyId,String encryptionKeyId,byte[] nonce,byte[] ciphertext,Instant now) {
        proof.requireAvailable(now);
        if(!proof.purpose().possessionRequired() || verifierKeyId==null || !verifiers.containsKey(verifierKeyId) || encryptionKeyId==null
            || nonce==null || nonce.length!=12 || ciphertext==null || ciphertext.length<19 || ciphertext.length>270)throw rejected();
        byte[] bytes=recipientCrypt(Cipher.DECRYPT_MODE,proof,verifierKeyId,encryptionKeyId,nonce,ciphertext);
        try{return new CustomerEmail(new String(bytes,StandardCharsets.UTF_8)).value();}finally{wipe(bytes);}
    }
    private byte[] recipientCrypt(int mode,CustomerProof proof,String verifierKeyId,String encryptionKeyId,byte[] nonce,byte[] input) {
        try{
            var key=encryptors.get(encryptionKeyId);if(key==null)throw rejected();Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode,key,new GCMParameterSpec(128,nonce));cipher.updateAAD(fields("customer-recipient-v1",proof.id(),proof.subjectId(),proof.purpose(),
                proof.createdAt(),proof.expiresAt(),proof.sessionId(),proof.authVersion(),proof.organizationId(),verifierKeyId,encryptionKeyId));return cipher.doFinal(input);
        }catch(GeneralSecurityException failure){throw rejected();}
    }
    private byte[] mac(String keyId,byte[] context,byte[] input) {
        try{var key=verifiers.get(keyId);if(key==null)throw rejected();Mac mac=Mac.getInstance("HmacSHA256");mac.init(key);mac.update(context);return mac.doFinal(input);}
        catch(GeneralSecurityException failure){throw rejected();}
    }
    private byte[] crypt(int mode,CustomerProof proof,String destination,String verifierKeyId,String encryptionKeyId,byte[] nonce,byte[] input) {
        try {
            var key=encryptors.get(encryptionKeyId);if(key==null)throw rejected();Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode,key,new GCMParameterSpec(128,nonce));cipher.updateAAD(fields("customer-delivery-v1",proof.id(),proof.subjectId(),proof.purpose(),
                proof.createdAt(),proof.expiresAt(),proof.sessionId(),proof.authVersion(),proof.organizationId(),destination,verifierKeyId,encryptionKeyId));return cipher.doFinal(input);
        }catch(GeneralSecurityException failure){throw rejected();}
    }
    private static boolean canonical(CustomerProof proof,byte[] token,boolean deliveryEnvelope) {
        if(proof.emailOtp()){
            if(token==null || token.length!=(deliveryEnvelope?43:6))return false;
            for(int index=0;index<6;index++)if(token[index]<'0' || token[index]>'9')return false;
            if(!deliveryEnvelope)return true;
            for(int index=6;index<43;index++)if(!((token[index]>='A' && token[index]<='Z') || (token[index]>='a' && token[index]<='z')
                || (token[index]>='0' && token[index]<='9') || token[index]=='-' || token[index]=='_'))return false;
            return true;
        }
        if(token==null || token.length!=43)return false;byte[] raw=null,encoded=null;
        try{raw=Base64.getUrlDecoder().decode(token);encoded=Base64.getUrlEncoder().withoutPadding().encode(raw);return raw.length==32 && MessageDigest.isEqual(encoded,token);}
        catch(IllegalArgumentException invalid){return false;}finally{wipe(raw);wipe(encoded);}
    }
    private static byte[] fields(Object... values) {
        try{var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
            for(Object field:values){if(field==null){out.writeInt(-1);continue;}byte[] encoded=field.toString().getBytes(StandardCharsets.UTF_8);try{out.writeInt(encoded.length);out.write(encoded);}finally{wipe(encoded);}}
            return bytes.toByteArray();
        }catch(IOException impossible){throw rejected();}
    }
    @Override public String toString(){return "ProtectedCustomerProofs[keys redacted]";}
}
