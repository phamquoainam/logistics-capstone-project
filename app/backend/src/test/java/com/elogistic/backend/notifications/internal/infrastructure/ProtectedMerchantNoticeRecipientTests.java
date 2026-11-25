package com.elogistic.backend.notifications.internal.infrastructure;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtectedMerchantNoticeRecipientTests {
    private static byte[] key(int value){byte[] key=new byte[32];Arrays.fill(key,(byte)value);return key;}
    @Test void everyBindingAndRetainedKeyIsRequiredWithoutPlaintextFallback(){
        var old=new ProtectedMerchantNoticeRecipient(Map.of("old",key(31)),"old");UUID id=UUID.randomUUID(),event=UUID.randomUUID(),app=UUID.randomUUID(),applicant=UUID.randomUUID();
        byte[] envelope=old.seal(id,event,app,applicant,"APPROVED","a@b");assertTrue(envelope.length>=32);assertEquals("a@b",old.open("old",id,event,app,applicant,"APPROVED",envelope));
        var rotated=new ProtectedMerchantNoticeRecipient(Map.of("old",key(31),"new",key(32)),"new");assertEquals("a@b",rotated.open("old",id,event,app,applicant,"APPROVED",envelope));
        assertThrows(IllegalStateException.class,()->rotated.open("missing",id,event,app,applicant,"APPROVED",envelope));
        assertThrows(IllegalStateException.class,()->rotated.open("new",id,event,app,applicant,"APPROVED",envelope));
        for(int field=0;field<5;field++){int changed=field;assertThrows(IllegalStateException.class,()->rotated.open("old",changed==0?UUID.randomUUID():id,changed==1?UUID.randomUUID():event,changed==2?UUID.randomUUID():app,changed==3?UUID.randomUUID():applicant,changed==4?"REJECTED":"APPROVED",envelope));}
        byte[] tampered=envelope.clone();tampered[tampered.length-1]^=1;assertThrows(IllegalStateException.class,()->old.open("old",id,event,app,applicant,"APPROVED",tampered));
        assertThrows(IllegalStateException.class,()->old.open("old",id,event,app,applicant,"APPROVED",new byte[31]));
        assertThrows(IllegalStateException.class,()->new ProtectedMerchantNoticeRecipient(Map.of("old",key(31)),null));
        assertThrows(IllegalStateException.class,()->new ProtectedMerchantNoticeRecipient(Map.of("old",new byte[32]),"old"));
    }
}
