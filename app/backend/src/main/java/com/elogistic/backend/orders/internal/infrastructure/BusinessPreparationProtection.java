package com.elogistic.backend.orders.internal.infrastructure;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Reuses the configured receipt key ring with a distinct, length-framed purpose. */
public final class BusinessPreparationProtection {
    private final Map<String,SecretKeySpec> keys; private final String active;
    public BusinessPreparationProtection(Map<String,byte[]> supplied,String active){
        if(supplied==null||supplied.isEmpty()||supplied.size()>16||!supplied.containsKey(active))throw new IllegalArgumentException();
        Map<String,SecretKeySpec> copy=new HashMap<>();supplied.forEach((id,key)->{
            if(id==null||!id.matches("[A-Za-z0-9_-]{1,64}")||key==null||key.length!=32||Arrays.equals(key,new byte[32]))throw new IllegalArgumentException();
            copy.put(id,new SecretKeySpec(key,"HmacSHA256"));
        });keys=Map.copyOf(copy);this.active=active;
    }
    public String active(){return active;}
    public byte[] hash(String keyId,Object... values){
        if(!keys.containsKey(keyId))throw new IllegalStateException("BUSINESS_RECEIPT_KEY_UNAVAILABLE");
        try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(keys.get(keyId));
            var framed=new ArrayList<Object>();framed.add("orders-organization-preparation-v1");framed.add(keyId);framed.addAll(Arrays.asList(values));
            for(Object value:framed){byte[] bytes=value==null?null:value.toString().getBytes(StandardCharsets.UTF_8);
                try{mac.update(ByteBuffer.allocate(4).putInt(bytes==null?-1:bytes.length).array());if(bytes!=null)mac.update(bytes);}
                finally{if(bytes!=null)Arrays.fill(bytes,(byte)0);}}
            return mac.doFinal();
        }catch(java.security.GeneralSecurityException unavailable){throw new IllegalStateException("BUSINESS_RECEIPT_PROTECTION_UNAVAILABLE");}
    }
    @Override public String toString(){return "BusinessPreparationProtection[redacted]";}
}
