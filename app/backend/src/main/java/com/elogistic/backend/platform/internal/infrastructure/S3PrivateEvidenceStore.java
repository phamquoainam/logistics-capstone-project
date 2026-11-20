package com.elogistic.backend.platform.internal.infrastructure;

import com.elogistic.backend.platform.api.PrivateEvidenceStore;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/** Quarantine bytes, never authority or public media. No delete, ACL, URL or caller-controlled path. */
public final class S3PrivateEvidenceStore implements PrivateEvidenceStore {
    private final S3Client s3;private final String bucket;
    public S3PrivateEvidenceStore(S3Client s3,String bucket){
        this.s3=Objects.requireNonNull(s3);this.bucket=Objects.requireNonNull(bucket);
        if(!bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")||bucket.contains(".."))throw new IllegalArgumentException("PRIVATE_EVIDENCE_CONFIGURATION_INVALID");
    }
    private static String key(UUID id){if(id==null||id.equals(new UUID(0,0)))throw new Rejected();return "private/claims/quarantine/"+id;}
    @Override public Blob put(InputStream source){return put(UUID.randomUUID(),source);}
    @Override public Blob put(UUID reservation,InputStream source){
        String object=key(reservation);if(source==null)throw new Rejected();byte[] bytes=null;
        try{
            bytes=PrivateEvidenceBytes.read(source);byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);
            var expected=new Blob(reservation,bytes.length,HexFormat.of().formatHex(digest));
            var request=PutObjectRequest.builder().bucket(bucket).key(object).ifNoneMatch("*").serverSideEncryption(ServerSideEncryption.AES256)
                .contentType("application/octet-stream").contentLength((long)bytes.length).checksumSHA256(Base64.getEncoder().encodeToString(digest)).metadata(Map.of("sha256",expected.sha256())).build();
            try{s3.putObject(request,RequestBody.fromBytes(bytes));}
            catch(S3Exception conflict){
                if(conflict.statusCode()!=412)throw new Unavailable();
                // An uncertain earlier write or exact concurrent retry may already exist. Never overwrite.
                try(var content=read(expected)){return expected;}
            }
            return expected;
        }catch(Rejected rejected){throw rejected;}catch(Exception unavailable){throw new Unavailable();}
        finally{if(bytes!=null)Arrays.fill(bytes,(byte)0);}
    }
    @Override public Content read(Blob expected){
        if(expected==null)throw new Unavailable();byte[] bytes=null;
        try(var object=s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key(expected.key())).checksumMode(ChecksumMode.ENABLED).build())){
            var response=object.response();
            if(response.contentLength()==null||response.contentLength()!=expected.bytes()||response.serverSideEncryption()!=ServerSideEncryption.AES256
                ||!expected.sha256().equals(response.metadata().get("sha256")))throw new Unavailable();
            bytes=object.readNBytes((int)expected.bytes()+1);
            if(bytes.length!=expected.bytes()||!MessageDigest.isEqual(HexFormat.of().parseHex(expected.sha256()),MessageDigest.getInstance("SHA-256").digest(bytes)))throw new Unavailable();
            return new Content(bytes);
        }catch(Exception unavailable){throw new Unavailable();}
        finally{if(bytes!=null)Arrays.fill(bytes,(byte)0);}
    }
    @Override public String toString(){return "S3PrivateEvidenceStore[redacted]";}
}
