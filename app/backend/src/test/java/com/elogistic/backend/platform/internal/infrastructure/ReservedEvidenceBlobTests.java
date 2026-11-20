package com.elogistic.backend.platform.internal.infrastructure;

import com.elogistic.backend.platform.api.PrivateEvidenceStore;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ReservedEvidenceBlobTests {
    @TempDir Path directory;
    @Test void serverReservationCreatesOnceAndExactRetryDoesNotOverwrite(){
        var store=new FileSystemPrivateEvidenceStore(directory.resolve("private"),List.of(directory.resolve("public")));
        UUID key=UUID.randomUUID();byte[] data="Synthetic reserved bytes".getBytes(StandardCharsets.US_ASCII);
        var first=store.put(key,new ByteArrayInputStream(data));assertEquals(key,first.key());
        var retry=store.put(key,new ByteArrayInputStream(data));assertEquals(first,retry);
        assertThrows(PrivateEvidenceStore.Unavailable.class,()->store.put(key,new ByteArrayInputStream("Changed content".getBytes(StandardCharsets.US_ASCII))));
        try(var content=store.read(first)){assertArrayEquals(data,content.bytes());}
    }
    @Test void reservationRejectsInvalidSizeKeyAndNonProgressingInput(){
        var store=new FileSystemPrivateEvidenceStore(directory.resolve("private"),List.of(directory.resolve("public")));
        assertThrows(PrivateEvidenceStore.Rejected.class,()->store.put(new UUID(0,0),new ByteArrayInputStream(new byte[]{1})));
        assertThrows(PrivateEvidenceStore.Rejected.class,()->store.put(UUID.randomUUID(),new ByteArrayInputStream(new byte[0])));
        assertThrows(PrivateEvidenceStore.Rejected.class,()->store.put(UUID.randomUUID(),new ByteArrayInputStream(new byte[(int)PrivateEvidenceStore.MAX_BYTES+1])));
        assertThrows(PrivateEvidenceStore.Unavailable.class,()->store.put(UUID.randomUUID(),new InputStream(){public int read(){return 0;}public int read(byte[] bytes,int offset,int length){return 0;}}));
    }
}
