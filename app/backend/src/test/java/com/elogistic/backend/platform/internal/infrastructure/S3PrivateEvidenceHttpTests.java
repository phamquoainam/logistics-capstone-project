package com.elogistic.backend.platform.internal.infrastructure;

import com.elogistic.backend.platform.api.PrivateEvidenceStore;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import static org.junit.jupiter.api.Assertions.*;

/** Actual SDK over controlled loopback HTTP. This is not live AWS verification. */
class S3PrivateEvidenceHttpTests {
    @Test void sdkPerformsPrivateConditionalWriteAndIntegrityReadOverHttp()throws Exception{
        var objects=new ConcurrentHashMap<String,byte[]>();var deletes=new AtomicInteger();var puts=new AtomicInteger();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{try{
            if(exchange.getRequestHeaders().getFirst("Authorization")==null){exchange.sendResponseHeaders(403,-1);return;}
            String path=exchange.getRequestURI().getPath();if(!path.matches("/synthetic-private-bucket/private/claims/quarantine/[0-9a-f-]{36}")){exchange.sendResponseHeaders(400,-1);return;}
            if(exchange.getRequestMethod().equals("PUT")){
                assertEquals("*",exchange.getRequestHeaders().getFirst("If-None-Match"));assertEquals("AES256",exchange.getRequestHeaders().getFirst("x-amz-server-side-encryption"));assertNull(exchange.getRequestHeaders().getFirst("x-amz-acl"));
                byte[] bytes=exchange.getRequestBody().readAllBytes();puts.incrementAndGet();if(objects.putIfAbsent(path,bytes)!=null){byte[] error="<Error><Code>PreconditionFailed</Code><Message>Synthetic conditional conflict</Message></Error>".getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/xml");exchange.sendResponseHeaders(412,error.length);exchange.getResponseBody().write(error);return;}
                exchange.getResponseHeaders().set("ETag","\"synthetic\"");exchange.sendResponseHeaders(200,-1);
            }else if(exchange.getRequestMethod().equals("GET")){
                byte[] bytes=objects.get(path);if(bytes==null){exchange.sendResponseHeaders(404,-1);return;}exchange.getResponseHeaders().set("x-amz-server-side-encryption","AES256");exchange.getResponseHeaders().set("x-amz-meta-sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);
            }else{deletes.incrementAndGet();exchange.sendResponseHeaders(405,-1);}
        }catch(java.security.GeneralSecurityException impossible){throw new IOException(impossible);}finally{exchange.close();}});
        server.start();try(var client=S3Client.builder().region(Region.AP_SOUTHEAST_2).endpointOverride(URI.create("http://127.0.0.1:"+server.getAddress().getPort())).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("synthetic-test-key","synthetic-test-secret"))).serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).chunkedEncodingEnabled(false).build()).requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED).overrideConfiguration(c->c.apiCallTimeout(Duration.ofSeconds(5))).build()){
            var store=new S3PrivateEvidenceStore(client,"synthetic-private-bucket");byte[] data="Synthetic evidence transport fixture".getBytes(StandardCharsets.US_ASCII);UUID key=UUID.randomUUID();
            var blob=store.put(key,new ByteArrayInputStream(data));try(var content=store.read(blob)){assertArrayEquals(data,content.bytes());}
            assertEquals(blob,store.put(key,new ByteArrayInputStream(data)));assertEquals(1,objects.size());assertEquals(2,puts.get());assertEquals(0,deletes.get());
            assertThrows(PrivateEvidenceStore.Unavailable.class,()->store.put(key,new ByteArrayInputStream(new byte[]{7,8,9})));assertEquals(1,objects.size());
            var anonymous=(HttpURLConnection)URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/synthetic-private-bucket/private/claims/quarantine/"+key).toURL().openConnection();anonymous.setConnectTimeout(1000);anonymous.setReadTimeout(1000);try{assertEquals(403,anonymous.getResponseCode());}finally{anonymous.disconnect();}
        }finally{server.stop(0);}
    }
}
