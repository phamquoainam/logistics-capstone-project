package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.merchant.api.GovernanceFailure;
import com.elogistic.backend.merchant.internal.application.port.MerchantFingerprints;
import java.io.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

/** Retained-key, actor/query/source-bound cursor; its envelope contains no search input. */
final class MerchantViewCursor {
    private static final UUID NAMESPACE=UUID.fromString("507a48f6-0ddc-4a82-a1a5-5862d3387300");
    private final MerchantFingerprints keys;private final Clock clock;
    MerchantViewCursor(MerchantFingerprints keys,Clock clock){this.keys=keys;this.clock=clock;}
    private static GovernanceFailure bad(){return new GovernanceFailure(GovernanceFailure.Code.SNAPSHOT_EXPIRED);}
    String next(UUID actor,UUID realm,String authority,String query,String source,int offset,int size){return encode(actor,realm,authority,query,source,offset,size,clock.instant().plusSeconds(900).getEpochSecond());}
    String advance(String previous,UUID actor,UUID realm,String authority,String query,String source,int offset,int size){
        if(previous==null)return next(actor,realm,authority,query,source,offset,size);
        offset(previous,actor,realm,authority,query,source,size);return encode(actor,realm,authority,query,source,offset,size,decode(previous).expiry());
    }
    private record Payload(String key,int offset,long expiry,byte[] body,byte[] mac){}
    private Payload decode(String token){
        try{
            if(token==null||token.length()>512)throw bad();byte[] bytes=Base64.getUrlDecoder().decode(token);
            if(!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(token)||bytes.length<47)throw bad();
            byte[] body=Arrays.copyOf(bytes,bytes.length-32),mac=Arrays.copyOfRange(bytes,bytes.length-32,bytes.length);
            try(var in=new DataInputStream(new ByteArrayInputStream(body))){if(in.readInt()!=1)throw bad();String key=in.readUTF();int offset=in.readInt();long expiry=in.readLong();if(in.available()!=0||!keys.retainedKeys().contains(key)||offset<1||offset>1000000)throw bad();return new Payload(key,offset,expiry,body,mac);}
        }catch(IOException|IllegalArgumentException failure){throw bad();}
    }
    int offset(String token,UUID actor,UUID realm,String authority,String query,String source,int size){
        if(token==null)return 0;var p=decode(token);byte[] expected=mac(p.key(),p.body(),actor,realm,authority,query,source,size);
        try{long now=clock.instant().getEpochSecond();if(now>=p.expiry()||p.expiry()>now+901||!MessageDigest.isEqual(expected,p.mac()))throw bad();return p.offset();}finally{Arrays.fill(expected,(byte)0);}
    }
    private String encode(UUID actor,UUID realm,String authority,String query,String source,int offset,int size,long expiry){
        try(var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes)){
            out.writeInt(1);out.writeUTF(keys.activeKey());out.writeInt(offset);out.writeLong(expiry);out.flush();byte[] body=bytes.toByteArray(),mac=mac(keys.activeKey(),body,actor,realm,authority,query,source,size);out.write(mac);Arrays.fill(mac,(byte)0);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        }catch(IOException impossible){throw bad();}
    }
    private byte[] mac(String key,byte[] body,UUID actor,UUID realm,String authority,String query,String source,int size){return keys.hash(key,"merchantViewCursor",NAMESPACE,1,actor.toString(),realm.toString(),authority,query,source,Integer.toString(size),Base64.getUrlEncoder().withoutPadding().encodeToString(body));}
}
