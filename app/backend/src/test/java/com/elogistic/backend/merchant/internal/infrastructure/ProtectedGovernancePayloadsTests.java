package com.elogistic.backend.merchant.internal.infrastructure;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtectedGovernancePayloadsTests {
    private static byte[] key(int value){byte[] key=new byte[32];Arrays.fill(key,(byte)value);return key;}
    @Test void contextBindingTamperAndRetainedKeysAreFailClosed(){
        var old=new ProtectedGovernancePayloads(Map.of("old",key(11)),"old");UUID event=UUID.randomUUID(),app=UUID.randomUUID();String text="Synthetic private review reason";
        byte[] sealed=old.seal(event,app,1,"REQUEST_INFORMATION",text);assertFalse(new String(sealed,java.nio.charset.StandardCharsets.ISO_8859_1).contains(text));
        var current=new ProtectedGovernancePayloads(Map.of("old",key(11),"new",key(12)),"new");assertEquals(text,current.open(event,app,1,"REQUEST_INFORMATION",sealed));
        assertThrows(IllegalStateException.class,()->current.open(UUID.randomUUID(),app,1,"REQUEST_INFORMATION",sealed));
        assertThrows(IllegalStateException.class,()->current.open(event,UUID.randomUUID(),1,"REQUEST_INFORMATION",sealed));
        assertThrows(IllegalStateException.class,()->current.open(event,app,2,"REQUEST_INFORMATION",sealed));
        assertThrows(IllegalStateException.class,()->current.open(event,app,1,"ASSIGN_REVIEWER",sealed));
        byte[] bad=sealed.clone();bad[bad.length-1]^=1;assertThrows(IllegalStateException.class,()->current.open(event,app,1,"REQUEST_INFORMATION",bad));
        var noOld=new ProtectedGovernancePayloads(Map.of("new",key(12)),"new");assertThrows(IllegalStateException.class,()->noOld.open(event,app,1,"REQUEST_INFORMATION",sealed));
        assertThrows(IllegalStateException.class,()->new ProtectedGovernancePayloads(Map.of(),"missing"));
        assertThrows(IllegalStateException.class,()->new ProtectedGovernancePayloads(Map.of("e1",key(12)),null));
    }
}
