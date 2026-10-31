package com.elogistic.backend.platform.internal.infrastructure;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** Opt-in one-shot smoke; never part of the customer HTTP runtime or CI. */
@Configuration @Profile("s3-smoke")
public class S3SmokeConfiguration {
    @Bean ApplicationRunner s3Smoke(Environment environment){
        String region=environment.getRequiredProperty("public.media.s3-region");
        String bucket=environment.getRequiredProperty("public.media.s3-bucket");
        if(!"ap-southeast-2".equals(region) || !"elogistics-dev-media-160905".equals(bucket))
            throw new IllegalArgumentException("S3_SMOKE_TARGET_INVALID");
        String localRoot=environment.getProperty("public.media.local-root","");
        return arguments->{
            try(var authorized=S3Client.builder().region(Region.of(region)).build();
                var anonymous=S3Client.builder().region(Region.of(region)).credentialsProvider(AnonymousCredentialsProvider.create()).build()){
                S3DevelopmentSmoke.verify(authorized,anonymous,bucket,UUID.randomUUID());
                System.out.println("S3_SMOKE_PASS");
                if(!localRoot.isBlank()){
                    byte[] expected=Files.readAllBytes(Path.of(localRoot,"hero-network.svg"));
                    byte[] actual=new S3PublicMediaStore(authorized,bucket).read("hero-network.svg").bytes();
                    if(!MessageDigest.isEqual(expected,actual))throw new IllegalStateException("S3_PUBLIC_MEDIA_INTEGRITY_INVALID");
                    System.out.println("S3_PUBLIC_MEDIA_READ_PASS");
                }
            }
        };
    }
}
