package com.elogistic.backend.orders.internal.infrastructure;
import com.elogistic.backend.orders.internal.application.port.ShippingDraftFingerprints;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class ProtectedShippingDraftFingerprints implements ShippingDraftFingerprints {
    private final Map<String,SecretKeySpec> keys;private final String active;
    public ProtectedShippingDraftFingerprints(Map<String,byte[]> supplied,String active){if(supplied==null||supplied.isEmpty()||supplied.size()>16||!supplied.containsKey(active))throw new IllegalArgumentException("SHIPPING_DRAFT_KEYS_INVALID");Map<String,SecretKeySpec> values=new HashMap<>();supplied.forEach((id,key)->{if(id==null||!id.matches("[A-Za-z0-9_-]{1,64}")||key==null||key.length!=32||Arrays.equals(key,new byte[32]))throw new IllegalArgumentException("SHIPPING_DRAFT_KEYS_INVALID");values.put(id,new SecretKeySpec(key,"HmacSHA256"));});keys=Map.copyOf(values);this.active=active;}
    @Override public String activeKey(){return active;}
    @Override public byte[] hash(String kid,UUID owner,String op,UUID id,long version,String payload){if(!keys.containsKey(kid)||owner==null||id==null||!Set.of("createShippingDraft","updateShippingDraft","archiveShippingDraft").contains(op)||version<0||version==0&&!op.equals("createShippingDraft"))throw new IllegalArgumentException("SHIPPING_DRAFT_FINGERPRINT_INVALID");try{var mac=Mac.getInstance("HmacSHA256");mac.init(keys.get(kid));for(var value:Arrays.asList("orders-shipping-draft-v1",kid,owner.toString(),op,id.toString(),Long.toString(version),payload)){byte[] bytes=value==null?null:value.getBytes(StandardCharsets.UTF_8);try{mac.update(ByteBuffer.allocate(4).putInt(bytes==null?-1:bytes.length).array());if(bytes!=null)mac.update(bytes);}finally{if(bytes!=null)Arrays.fill(bytes,(byte)0);}}return mac.doFinal();}catch(java.security.GeneralSecurityException unavailable){throw new IllegalStateException("SHIPPING_DRAFT_PROTECTION_UNAVAILABLE");}}
    @Override public String toString(){return "ShippingDraftFingerprints[redacted]";}
}
