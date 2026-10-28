package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.merchant.internal.application.port.MerchantFingerprints;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Exact fields are length-delimited and streamed into retained-key HMAC, without plaintext receipt snapshots. */
public final class ProtectedMerchantFingerprints implements MerchantFingerprints {
    private final Map<String,SecretKeySpec> keys;private final String active;
    public ProtectedMerchantFingerprints(Map<String,byte[]> supplied,String active){
        if(supplied==null || supplied.isEmpty() || supplied.size()>16 || active==null || !supplied.containsKey(active))throw invalid();
        Map<String,SecretKeySpec> copy=new HashMap<>();supplied.forEach((id,key)->{if(id==null || !id.matches("[A-Za-z0-9_-]{1,64}") || key==null || key.length!=32 || Arrays.equals(key,new byte[32]))throw invalid();copy.put(id,new SecretKeySpec(key,"HmacSHA256"));});keys=Map.copyOf(copy);this.active=active;
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("MERCHANT_RECEIPT_KEY_CONFIGURATION_INVALID");}
    @Override public String activeKey(){return active;}
    @Override public byte[] hash(String keyId,String operation,UUID target,long version,String... values){
        if(keyId==null || operation==null || !operation.matches("[A-Za-z][A-Za-z0-9]{0,79}") || target==null || version<0 || version==0 && !operation.equals("createApplicationDraft") || values==null || values.length>30 || !keys.containsKey(keyId))throw invalid();
        try{
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(keys.get(keyId));
            update(mac,"merchant-command-fingerprint-v1");update(mac,keyId);update(mac,operation);update(mac,target.toString());mac.update(ByteBuffer.allocate(8).putLong(version).array());
            for(String value:values)update(mac,value);return mac.doFinal();
        }catch(java.security.GeneralSecurityException unavailable){throw new IllegalStateException("MERCHANT_RECEIPT_PROTECTION_UNAVAILABLE");}
    }
    private static void update(Mac mac,String value){byte[] bytes=value==null?null:value.getBytes(StandardCharsets.UTF_8);try{mac.update(ByteBuffer.allocate(4).putInt(bytes==null?-1:bytes.length).array());if(bytes!=null)mac.update(bytes);}finally{if(bytes!=null)Arrays.fill(bytes,(byte)0);}}
    @Override public String toString(){return "ProtectedMerchantFingerprints[redacted]";}
}
