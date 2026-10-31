package com.elogistic.backend.platform.internal.infrastructure;

import com.elogistic.backend.platform.api.PublicMediaStore;
import java.io.IOException;
import java.util.Objects;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/** Uses the SDK credential chain; no bucket ACL or anonymous S3 access is required. */
public final class S3PublicMediaStore implements PublicMediaStore {
    private final S3Client s3;private final String bucket;
    public S3PublicMediaStore(S3Client s3,String bucket){
        this.s3=Objects.requireNonNull(s3);this.bucket=Objects.requireNonNull(bucket);
        if(!bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]"))throw new IllegalArgumentException("PUBLIC_MEDIA_BUCKET_INVALID");
    }
    @Override public Media read(String key){
        if(!"hero-network.svg".equals(key))throw new IllegalArgumentException("PUBLIC_MEDIA_KEY_INVALID");
        try(var object=s3.getObject(GetObjectRequest.builder().bucket(bucket).key("public/website/"+key).build())){
            if(object.response().contentLength()==null || object.response().contentLength()<1 || object.response().contentLength()>262144
                || !"image/svg+xml".equals(object.response().contentType()))throw new IllegalStateException("PUBLIC_MEDIA_INVALID");
            byte[] bytes=object.readNBytes(262145);
            if(bytes.length!=object.response().contentLength() || bytes.length>262144)throw new IllegalStateException("PUBLIC_MEDIA_INVALID");
            return new Media(bytes,"image/svg+xml");
        }catch(IOException unavailable){throw new IllegalStateException("PUBLIC_MEDIA_UNAVAILABLE",unavailable);}
    }
}
