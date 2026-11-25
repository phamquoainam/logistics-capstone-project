package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.application.port.CustomerProofProtection;
import com.elogistic.backend.identity.internal.domain.CustomerProof;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/** Versioned packaging of existing purpose-bound recipient ciphertext; no new encryption engine. */
final class GovernanceRecipientEnvelope {
    private GovernanceRecipientEnvelope(){}
    static byte[] seal(CustomerProofProtection.Protected sealed){
        byte[] key=sealed.encryptionKey().getBytes(StandardCharsets.US_ASCII),nonce=sealed.recipientNonce(),cipher=sealed.recipientCiphertext();
        try{return ByteBuffer.allocate(2+key.length+nonce.length+cipher.length).put((byte)1).put((byte)key.length).put(key).put(nonce).put(cipher).array();}
        finally{Arrays.fill(key,(byte)0);Arrays.fill(nonce,(byte)0);Arrays.fill(cipher,(byte)0);}
    }
    static String open(CustomerProofProtection protection,CustomerProof proof,String verifierKey,byte[] envelope,Instant now){
        if(envelope==null||envelope.length<34||envelope.length>512)throw new IllegalStateException("BUSINESS_CONTACT_ENVELOPE_INVALID");
        var data=ByteBuffer.wrap(envelope);if(data.get()!=1)throw new IllegalStateException("BUSINESS_CONTACT_ENVELOPE_INVALID");int length=Byte.toUnsignedInt(data.get());
        if(length<1||length>64||data.remaining()<length+31)throw new IllegalStateException("BUSINESS_CONTACT_ENVELOPE_INVALID");
        byte[] key=new byte[length],nonce=new byte[12],cipher=null;
        try{data.get(key);data.get(nonce);cipher=new byte[data.remaining()];data.get(cipher);String keyId=new String(key,StandardCharsets.US_ASCII);if(!keyId.matches("[A-Za-z0-9_-]{1,64}"))throw new IllegalStateException("BUSINESS_CONTACT_ENVELOPE_INVALID");return protection.openDestination(proof,verifierKey,keyId,nonce,cipher,now);}
        finally{Arrays.fill(key,(byte)0);Arrays.fill(nonce,(byte)0);if(cipher!=null)Arrays.fill(cipher,(byte)0);}
    }
}
