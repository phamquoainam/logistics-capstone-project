package com.elogistic.backend.merchant.internal.infrastructure;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;

/** Retained configured encryption keys only; no generated key or plaintext audit fallback. */
public final class ProtectedGovernancePayloads {
    private final Map<String,SecretKeySpec> keys;private final String active;private final SecureRandom random=new SecureRandom();
    public ProtectedGovernancePayloads(Map<String,byte[]> configured,String active){
        if(configured==null||configured.isEmpty()||configured.size()>16||active==null||!configured.containsKey(active))throw invalid();
        var copy=new HashMap<String,SecretKeySpec>();configured.forEach((id,key)->{if(id==null||!id.matches("[A-Za-z0-9_-]{1,64}")||key==null||key.length!=32||Arrays.equals(key,new byte[32]))throw invalid();copy.put(id,new SecretKeySpec(key,"AES"));});this.keys=Map.copyOf(copy);this.active=active;
    }
    private static IllegalStateException invalid(){return new IllegalStateException("GOVERNANCE_PAYLOAD_PROTECTION_REQUIRED");}
    private static byte[] aad(UUID event,UUID target,long revision,String operation){if(event==null||target==null||revision<0||operation==null||!operation.matches("[A-Z_]{1,40}"))throw invalid();return ("argopost/merchant-governance/v1|"+event+"|"+target+"|"+revision+"|"+operation).getBytes(StandardCharsets.UTF_8);}
    public byte[] seal(UUID event,UUID target,long revision,String operation,String text){
        if(text==null)throw invalid();byte[] plain=text.getBytes(StandardCharsets.UTF_8),nonce=new byte[12];if(plain.length>32768){Arrays.fill(plain,(byte)0);throw invalid();}random.nextBytes(nonce);
        try{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,keys.get(active),new GCMParameterSpec(128,nonce));cipher.updateAAD(aad(event,target,revision,operation));byte[] encrypted=cipher.doFinal(plain);
            try(var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes)){out.writeInt(1);out.writeUTF(active);out.write(nonce);out.writeInt(encrypted.length);out.write(encrypted);return bytes.toByteArray();}
            finally{Arrays.fill(encrypted,(byte)0);}
        }catch(IOException|GeneralSecurityException failure){throw invalid();}finally{Arrays.fill(plain,(byte)0);Arrays.fill(nonce,(byte)0);}
    }
    public String open(UUID event,UUID target,long revision,String operation,byte[] protectedValue){
        if(protectedValue==null||protectedValue.length>33000)throw invalid();
        byte[] nonce=null,encrypted=null,plain=null;
        try(var input=new DataInputStream(new ByteArrayInputStream(protectedValue))){if(input.readInt()!=1)throw invalid();String id=input.readUTF();if(!keys.containsKey(id))throw invalid();nonce=input.readNBytes(12);int length=input.readInt();if(nonce.length!=12||length<16||length>32784||length!=input.available())throw invalid();encrypted=input.readNBytes(length);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,keys.get(id),new GCMParameterSpec(128,nonce));cipher.updateAAD(aad(event,target,revision,operation));plain=cipher.doFinal(encrypted);return new String(plain,StandardCharsets.UTF_8);
        }catch(IOException|GeneralSecurityException failure){throw invalid();}finally{if(nonce!=null)Arrays.fill(nonce,(byte)0);if(encrypted!=null)Arrays.fill(encrypted,(byte)0);if(plain!=null)Arrays.fill(plain,(byte)0);}
    }
    @Override public String toString(){return "ProtectedGovernancePayloads[redacted]";}
}
