package com.elogistic.backend.platform.internal.infrastructure;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PublicMediaStoreTests {
    @TempDir Path root;
    @Test void localStoreServesOnlyAllowlistedAssetAndRejectsSymlink()throws Exception{
        byte[] image="<svg xmlns='http://www.w3.org/2000/svg'/>".getBytes(StandardCharsets.UTF_8);
        Files.write(root.resolve("hero-network.svg"),image);
        var store=new LocalPublicMediaStore(root);
        assertArrayEquals(image,store.read("hero-network.svg").bytes());
        assertThrows(IllegalArgumentException.class,()->store.read("../private/evidence"));
        Files.delete(root.resolve("hero-network.svg"));
        try{Files.createSymbolicLink(root.resolve("hero-network.svg"),root.resolve("private-evidence"));}
        catch(UnsupportedOperationException | java.nio.file.FileSystemException unsupported){return;}
        assertThrows(IllegalStateException.class,()->store.read("hero-network.svg"));
    }
    @Test void s3StoreUsesOnlyPublicPrefixAndRejectsUnexpectedMetadata(){
        var s3=mock(S3Client.class);byte[] image="<svg/>".getBytes(StandardCharsets.UTF_8);
        var response=GetObjectResponse.builder().contentLength((long)image.length).contentType("image/svg+xml").build();
        when(s3.getObject(any(GetObjectRequest.class))).thenAnswer(call->{
            GetObjectRequest request=call.getArgument(0);
            assertEquals("public/website/hero-network.svg",request.key());assertEquals("synthetic-public-bucket",request.bucket());
            return new ResponseInputStream<>(response,AbortableInputStream.create(new ByteArrayInputStream(image)));
        });
        var store=new S3PublicMediaStore(s3,"synthetic-public-bucket");
        assertArrayEquals(image,store.read("hero-network.svg").bytes());
        assertThrows(IllegalArgumentException.class,()->store.read("private/merchant-evidence/file"));
        verify(s3,times(1)).getObject(any(GetObjectRequest.class));
    }
}
