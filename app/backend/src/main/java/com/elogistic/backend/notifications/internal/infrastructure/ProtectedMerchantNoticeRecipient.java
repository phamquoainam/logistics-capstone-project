package com.elogistic.backend.notifications.internal.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;

/** Same configured retained key ring, separate AEAD purpose. No generated key or plaintext fallback. */
public final class ProtectedMerchantNoticeRecipient {
    private final Map<String,SecretKeySpec> keys;private final String active;private final SecureRandom random=new SecureRandom();
    public ProtectedMerchantNoticeRecipient(Map<String,byte[]> configured,String active){
        if(configured==null||configured.isEmpty()||configured.size()>16||active==null||!configured.containsKey(active))throw invalid();
        var copy=new HashMap<String,SecretKeySpec>();configured.forEach((id,key)->{if(id==null||!id.matches("[A-Za-z0-9_-]{1,64}")||key==null||key.length!=32||Arrays.equals(key,new byte[32]))throw invalid();copy.put(id,new SecretKeySpec(key,"AES"));});keys=Map.copyOf(copy);this.active=active;
    }
    private static IllegalStateException invalid(){return new IllegalStateException("MERCHANT_NOTICE_PROTECTION_REQUIRED");}
    public String activeKey(){return active;}
    private static byte[] aad(UUID id,UUID event,UUID app,UUID applicant,String template){return ("argopost/merchant-notice/v1|"+id+"|"+event+"|"+app+"|"+applicant+"|"+template).getBytes(StandardCharsets.US_ASCII);}
    public byte[] seal(UUID id,UUID event,UUID app,UUID applicant,String template,String recipient){
        byte[] plain=recipient.getBytes(StandardCharsets.UTF_8),nonce=new byte[12];random.nextBytes(nonce);
        try{var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,keys.get(active),new GCMParameterSpec(128,nonce));cipher.updateAAD(aad(id,event,app,applicant,template));byte[] encrypted=cipher.doFinal(plain);try{byte[] result=new byte[4+nonce.length+encrypted.length];java.nio.ByteBuffer.wrap(result).putInt(1).put(nonce).put(encrypted);return result;}finally{Arrays.fill(encrypted,(byte)0);}}
        catch(GeneralSecurityException failure){throw invalid();}finally{Arrays.fill(plain,(byte)0);Arrays.fill(nonce,(byte)0);}
    }
    public String open(String key,UUID id,UUID event,UUID app,UUID applicant,String template,byte[] envelope){
        if(envelope==null||envelope.length<35||envelope.length>2048||!keys.containsKey(key)||java.nio.ByteBuffer.wrap(envelope).getInt()!=1)throw invalid();byte[] plain=null;
        try{var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,keys.get(key),new GCMParameterSpec(128,envelope,4,12));cipher.updateAAD(aad(id,event,app,applicant,template));plain=cipher.doFinal(envelope,16,envelope.length-16);return new String(plain,StandardCharsets.UTF_8);}
        catch(GeneralSecurityException failure){throw invalid();}finally{if(plain!=null)Arrays.fill(plain,(byte)0);}
    }
    @Override public String toString(){return "MerchantNoticeProtection[redacted]";}
}
