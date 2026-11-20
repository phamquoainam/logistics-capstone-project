package com.elogistic.backend.platform.internal.infrastructure;

import com.elogistic.backend.platform.api.PrivateEvidenceStore;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3PrivateEvidenceStoreTests {
    private static final byte[] DATA="Synthetic private Claim evidence".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private String hash()throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(DATA));}
    private ResponseInputStream<GetObjectResponse> read(byte[] bytes,String hash){return new ResponseInputStream<>(GetObjectResponse.builder().contentLength((long)bytes.length).contentType("application/octet-stream").serverSideEncryption(ServerSideEncryption.AES256).metadata(Map.of("sha256",hash)).build(),AbortableInputStream.create(new ByteArrayInputStream(bytes)));}
    @Test void privateReservedUploadUsesConditionalSseWriteAndIntegrityCheckedRead()throws Exception{
        var s3=mock(S3Client.class);UUID key=UUID.randomUUID();
        when(s3.putObject(any(PutObjectRequest.class),any(RequestBody.class))).thenAnswer(call->{var request=call.getArgument(0,PutObjectRequest.class);assertEquals("private/claims/quarantine/"+key,request.key());assertNull(request.acl());assertEquals("*",request.ifNoneMatch());assertEquals(ServerSideEncryption.AES256,request.serverSideEncryption());try(var in=call.getArgument(1,RequestBody.class).contentStreamProvider().newStream()){assertArrayEquals(DATA,in.readAllBytes());}return PutObjectResponse.builder().build();});
        when(s3.getObject(any(GetObjectRequest.class))).thenAnswer(call->read(DATA,hash()));
        var store=new S3PrivateEvidenceStore(s3,"synthetic-private-bucket");var metadata=store.put(key,new ByteArrayInputStream(DATA));assertEquals(key,metadata.key());assertEquals(hash(),metadata.sha256());
        try(var content=store.read(metadata)){assertArrayEquals(DATA,content.bytes());}
        verify(s3,never()).deleteObject(any(DeleteObjectRequest.class));assertFalse(store.toString().contains("bucket"));
    }
    @Test void exactConditionalRetryReadsExistingContentButNeverOverwritesDifferentContent()throws Exception{
        var s3=mock(S3Client.class);when(s3.putObject(any(PutObjectRequest.class),any(RequestBody.class))).thenThrow(S3Exception.builder().statusCode(412).build());
        when(s3.getObject(any(GetObjectRequest.class))).thenAnswer(call->read(DATA,hash()));var store=new S3PrivateEvidenceStore(s3,"synthetic-private-bucket");UUID key=UUID.randomUUID();
        assertEquals(hash(),store.put(key,new ByteArrayInputStream(DATA)).sha256());
        assertThrows(PrivateEvidenceStore.Unavailable.class,()->store.put(key,new ByteArrayInputStream(new byte[]{1,2,3})));
        verify(s3,never()).deleteObject(any(DeleteObjectRequest.class));
    }
    @Test void wrongContentEncryptionMetadataOrLengthFailsClosed()throws Exception{
        var s3=mock(S3Client.class);var store=new S3PrivateEvidenceStore(s3,"synthetic-private-bucket");var expected=new PrivateEvidenceStore.Blob(UUID.randomUUID(),DATA.length,hash());
        for(var response:List.of(GetObjectResponse.builder().contentLength((long)DATA.length).metadata(Map.of("sha256",hash())).build(),GetObjectResponse.builder().contentLength((long)DATA.length).serverSideEncryption(ServerSideEncryption.AES256).metadata(Map.of("sha256","0".repeat(64))).build(),GetObjectResponse.builder().contentLength((long)DATA.length+1).serverSideEncryption(ServerSideEncryption.AES256).metadata(Map.of("sha256",hash())).build())){
            when(s3.getObject(any(GetObjectRequest.class))).thenAnswer(call->new ResponseInputStream<>(response,AbortableInputStream.create(new ByteArrayInputStream(DATA))));assertThrows(PrivateEvidenceStore.Unavailable.class,()->store.read(expected));
        }
        assertThrows(IllegalArgumentException.class,()->new S3PrivateEvidenceStore(s3,"../public"));
    }
}
