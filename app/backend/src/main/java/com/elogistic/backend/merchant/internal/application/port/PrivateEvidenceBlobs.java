package com.elogistic.backend.merchant.internal.application.port;

import java.io.InputStream;
import java.util.*;

/** Private quarantine bytes only. Caller must separately enforce current resource authority, quota and screening. */
public interface PrivateEvidenceBlobs {
    long MAX_BYTES=5L*1024*1024;
    record Blob(UUID key,long bytes,String sha256){
        public Blob{if(key==null || bytes<1 || bytes>MAX_BYTES || sha256==null || !sha256.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("EVIDENCE_METADATA_INVALID");}
        @Override public String toString(){return "PrivateEvidenceBlob[redacted]";}
    }
    Blob put(InputStream source);
    /** Expected metadata comes from the locked server record, never from request body. No public URL/path. */
    Content read(Blob expected);
    final class Content implements AutoCloseable {
        private final byte[] bytes;private boolean closed;
        public Content(byte[] bytes){this.bytes=bytes.clone();}
        public byte[] bytes(){if(closed)throw new IllegalStateException("EVIDENCE_CONTENT_CLOSED");return bytes.clone();}
        @Override public void close(){Arrays.fill(bytes,(byte)0);closed=true;}
        @Override public String toString(){return "PrivateEvidenceContent[redacted]";}
    }
    final class Rejected extends RuntimeException {public Rejected(){super("EVIDENCE_CONTENT_REJECTED");}}
    final class Unavailable extends RuntimeException {public Unavailable(){super("PRIVATE_EVIDENCE_UNAVAILABLE");}}
}
