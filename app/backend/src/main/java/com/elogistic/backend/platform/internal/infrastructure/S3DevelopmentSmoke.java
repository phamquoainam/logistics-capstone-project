package com.elogistic.backend.platform.internal.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/** A bounded, synthetic SDK probe. It may only create and remove its own dev-smoke object. */
public final class S3DevelopmentSmoke {
    private static final byte[] CONTENT="E-Logistics development storage probe\n".getBytes(StandardCharsets.UTF_8);
    private S3DevelopmentSmoke(){}
    public static void verify(S3Client authorized,S3Client anonymous,String bucket,UUID execution){
        Objects.requireNonNull(authorized);Objects.requireNonNull(anonymous);Objects.requireNonNull(execution);
        if(bucket==null || !bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]"))throw new IllegalArgumentException("S3_SMOKE_BUCKET_INVALID");
        String key="dev-smoke/backend-"+execution+".txt";
        boolean uploaded=false;
        try{
            authorized.putObject(PutObjectRequest.builder().bucket(bucket).key(key).ifNoneMatch("*")
                .contentType("text/plain; charset=utf-8").serverSideEncryption(ServerSideEncryption.AES256).build(),RequestBody.fromBytes(CONTENT));
            uploaded=true;
            try(var object=authorized.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())){
                if(object.response().contentLength()==null || object.response().contentLength()!=CONTENT.length
                    || !"text/plain; charset=utf-8".equals(object.response().contentType())
                    || object.response().serverSideEncryption()!=ServerSideEncryption.AES256)throw new IllegalStateException("S3_SMOKE_METADATA_INVALID");
                byte[] received=object.readNBytes(CONTENT.length+1);
                try{if(!MessageDigest.isEqual(CONTENT,received))throw new IllegalStateException("S3_SMOKE_INTEGRITY_INVALID");}
                finally{Arrays.fill(received,(byte)0);}
            }
            try(var ignored=anonymous.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())){
                throw new IllegalStateException("S3_SMOKE_PUBLIC_ACCESS_INVALID");
            }catch(S3Exception denied){if(denied.statusCode()!=403)throw new IllegalStateException("S3_SMOKE_PUBLIC_ACCESS_UNVERIFIED");}
        }catch(IOException unavailable){throw new IllegalStateException("S3_SMOKE_READ_UNAVAILABLE",unavailable);}
        finally{if(uploaded)authorized.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());}
    }
}
