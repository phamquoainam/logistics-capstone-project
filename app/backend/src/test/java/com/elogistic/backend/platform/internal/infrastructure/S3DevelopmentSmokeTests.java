package com.elogistic.backend.platform.internal.infrastructure;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3DevelopmentSmokeTests {
    @Test void syntheticProbeUsesExclusiveSmokePrefixVerifiesPrivateAccessAndDeletesOnlyItsOwnKey(){
        var authorized=mock(S3Client.class);var anonymous=mock(S3Client.class);var execution=UUID.randomUUID();
        byte[] bytes="E-Logistics development storage probe\n".getBytes(StandardCharsets.UTF_8);
        when(authorized.getObject(any(GetObjectRequest.class))).thenAnswer(call->new ResponseInputStream<>(
            GetObjectResponse.builder().contentLength((long)bytes.length).contentType("text/plain; charset=utf-8")
                .serverSideEncryption(ServerSideEncryption.AES256).build(),AbortableInputStream.create(new ByteArrayInputStream(bytes))));
        when(anonymous.getObject(any(GetObjectRequest.class))).thenThrow(S3Exception.builder().statusCode(403).build());
        S3DevelopmentSmoke.verify(authorized,anonymous,"synthetic-dev-bucket",execution);
        String key="dev-smoke/backend-"+execution+".txt";
        var put=org.mockito.ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(authorized).putObject(put.capture(),any(software.amazon.awssdk.core.sync.RequestBody.class));
        assertEquals(key,put.getValue().key());assertEquals("*",put.getValue().ifNoneMatch());
        assertEquals(ServerSideEncryption.AES256,put.getValue().serverSideEncryption());
        verify(authorized).deleteObject(argThat((DeleteObjectRequest request)->key.equals(request.key())));
        verify(anonymous).getObject(argThat((GetObjectRequest request)->key.equals(request.key())));
        assertThrows(IllegalArgumentException.class,()->S3DevelopmentSmoke.verify(authorized,anonymous,"../private",execution));
    }
}
