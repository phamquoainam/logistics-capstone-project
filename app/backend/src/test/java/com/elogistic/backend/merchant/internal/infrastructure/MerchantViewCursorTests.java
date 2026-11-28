package com.elogistic.backend.merchant.internal.infrastructure;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MerchantViewCursorTests {
    @Test void cursorBindsCurrentActorRealmAuthorityQuerySourceSizeAndDeadline(){
        byte[] key=new byte[32];Arrays.fill(key,(byte)19);
        var clock=Clock.fixed(Instant.parse("2026-11-26T00:00:00Z"),ZoneOffset.UTC);
        var cursor=new MerchantViewCursor(new ProtectedMerchantFingerprints(Map.of("t1",key),"t1"),clock);
        UUID actor=UUID.randomUUID(),realm=UUID.randomUUID();
        String value=cursor.next(actor,realm,"authority","query","source",20,20);
        assertEquals(20,cursor.offset(value,actor,realm,"authority","query","source",20));
        assertThrows(RuntimeException.class,()->cursor.offset(value,UUID.randomUUID(),realm,"authority","query","source",20));
        assertThrows(RuntimeException.class,()->cursor.offset(value,actor,realm,"revoked","query","source",20));
        assertThrows(RuntimeException.class,()->cursor.offset(value,actor,realm,"authority","foreign","source",20));
        assertThrows(RuntimeException.class,()->cursor.offset(value,actor,realm,"authority","query","changed",20));
        assertThrows(RuntimeException.class,()->cursor.offset(value,actor,realm,"authority","query","source",50));
        assertThrows(RuntimeException.class,()->cursor.offset(value+"x",actor,realm,"authority","query","source",20));
        var expired=new MerchantViewCursor(new ProtectedMerchantFingerprints(Map.of("t1",key),"t1"),Clock.offset(clock,Duration.ofMinutes(16)));
        assertThrows(RuntimeException.class,()->expired.offset(value,actor,realm,"authority","query","source",20));
        assertFalse(value.contains("query"));assertFalse(value.contains("authority"));
    }
}
