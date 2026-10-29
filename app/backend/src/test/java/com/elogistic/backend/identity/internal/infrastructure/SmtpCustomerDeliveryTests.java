package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.application.port.CustomerDeliveryTransport;
import com.elogistic.backend.identity.internal.domain.CustomerProof;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;
import javax.net.ssl.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real loopback SMTP inbox and TLS handshakes. Synthetic recipients/certificates, TEST environment only. */
@Timeout(20)
class SmtpCustomerDeliveryTests {
    private static final Instant NOW=Instant.parse("2026-10-27T00:00:00Z");
    private static final String ORIGIN="https://portal.example.invalid",DESTINATION="fixture@example.invalid";
    private static SSLContext serverTls;private static KeyStore trustRoots;
    @BeforeAll static void ephemeralCertificate()throws Exception{
        Path directory=Files.createTempDirectory("spec003-smtp-tls-");Path keystore=directory.resolve("test.p12");char[] password=UUID.randomUUID().toString().toCharArray();Process process=null;
        try{
            String executable=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"keytool.exe":"keytool").toString();
            process=new ProcessBuilder(executable,"-genkeypair","-alias","smtp-test","-keyalg","RSA","-keysize","3072","-validity","1","-dname","CN=localhost","-ext","SAN=dns:localhost,ip:127.0.0.1","-storetype","PKCS12","-keystore",keystore.toString(),"-storepass",new String(password),"-noprompt")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            assertTrue(process.waitFor(15,TimeUnit.SECONDS));assertEquals(0,process.exitValue());
            var keys=KeyStore.getInstance("PKCS12");try(InputStream input=Files.newInputStream(keystore)){keys.load(input,password);}
            var managers=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());managers.init(keys,password);
            serverTls=SSLContext.getInstance("TLS");serverTls.init(managers.getKeyManagers(),null,null);
            trustRoots=KeyStore.getInstance(KeyStore.getDefaultType());trustRoots.load(null,null);trustRoots.setCertificateEntry("smtp-test",keys.getCertificate("smtp-test"));
        }finally{if(process!=null && process.isAlive()){process.destroyForcibly();assertTrue(process.waitFor(3,TimeUnit.SECONDS));}Arrays.fill(password,'\0');Files.deleteIfExists(keystore);Files.deleteIfExists(directory);}
    }
    private static CustomerDeliveryTransport.Message message(UUID id){return message(id,CustomerProof.Purpose.CUSTOMER_ENROLLMENT);}
    private static CustomerDeliveryTransport.Message message(UUID id,CustomerProof.Purpose purpose){
        var proof=CustomerProof.issue(UUID.randomUUID(),UUID.randomUUID(),purpose,NOW,null,purpose==CustomerProof.Purpose.CUSTOMER_RECOVERY?1L:null,null);byte[] raw=new byte[32];new SecureRandom().nextBytes(raw);byte[] token=Base64.getUrlEncoder().withoutPadding().encode(raw);
        try{return new CustomerDeliveryTransport.Message(id,proof,DESTINATION,token);}finally{Arrays.fill(raw,(byte)0);Arrays.fill(token,(byte)0);}
    }
    private SmtpCustomerDelivery adapter(CustomerSmtpTestInbox inbox,SmtpCustomerDelivery.Mode mode,KeyStore roots){
        boolean plain=mode==SmtpCustomerDelivery.Mode.TEST_LOOPBACK;
        return new SmtpCustomerDelivery(new SmtpCustomerDelivery.Settings("127.0.0.1",inbox.port(),mode,plain?null:"synthetic-smtp-user",plain?null:"synthetic-smtp-password","no-reply@example.invalid",Set.of("example.invalid"),ORIGIN,true),Clock.fixed(NOW,ZoneOffset.UTC),roots);
    }
    @Test void loopbackSmtpActuallyReceivesPurposeBoundFragmentWithoutClaimingDelivery()throws Exception{
        try(var inbox=new CustomerSmtpTestInbox(1,false,false,250,serverTls);var intent=message(UUID.randomUUID())){
            var outcome=adapter(inbox,SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null).send(intent);assertEquals(CustomerDeliveryTransport.Kind.ACCEPTED_BY_PROVIDER,outcome.kind());assertTrue(outcome.sandbox());
            var received=inbox.messages();assertEquals(1,received.size());var mail=new MimeMessage(Session.getInstance(new Properties()),new ByteArrayInputStream(received.getFirst()));
            assertEquals("<"+intent.id()+"@example.invalid>",mail.getMessageID());assertEquals(DESTINATION,mail.getAllRecipients()[0].toString());
            String content=(String)mail.getContent();assertTrue(content.contains(ORIGIN+"/verify-email#proofId="+intent.proof().id()+"&proof="));assertFalse(content.contains("/verify-email?"));
            byte[] token=intent.token();try{assertTrue(content.contains(new String(token,StandardCharsets.US_ASCII)));}finally{Arrays.fill(token,(byte)0);}
            assertEquals(0,inbox.authentications.get());assertEquals("CustomerDeliveryOutcome[redacted]",outcome.toString());assertEquals("CustomerDeliveryMessage[redacted]",intent.toString());
        }
    }
    @Test void realStarttlsChecksApprovedCertificateAndAuthenticatesBeforeSending()throws Exception{
        try(var inbox=new CustomerSmtpTestInbox(1,true,false,250,serverTls);var intent=message(UUID.randomUUID())){
            var outcome=adapter(inbox,SmtpCustomerDelivery.Mode.STARTTLS,trustRoots).send(intent);assertEquals(CustomerDeliveryTransport.Kind.ACCEPTED_BY_PROVIDER,outcome.kind());assertEquals(1,inbox.messages().size());assertEquals(1,inbox.authentications.get());assertEquals(1,inbox.handshakes.get());
        }
    }
    @Test void verificationAndRecoveryUseDistinctVietnameseMessagesAndExactFrontendRoutes()throws Exception{
        try(var inbox=new CustomerSmtpTestInbox(2,false,false,250,serverTls);
            var enrollment=message(UUID.randomUUID());var recovery=message(UUID.randomUUID(),CustomerProof.Purpose.CUSTOMER_RECOVERY)){
            var sender=adapter(inbox,SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null);
            assertEquals(CustomerDeliveryTransport.Kind.ACCEPTED_BY_PROVIDER,sender.send(enrollment).kind());
            assertEquals(CustomerDeliveryTransport.Kind.ACCEPTED_BY_PROVIDER,sender.send(recovery).kind());
            var first=new MimeMessage(Session.getInstance(new Properties()),new ByteArrayInputStream(inbox.messages().get(0)));
            var second=new MimeMessage(Session.getInstance(new Properties()),new ByteArrayInputStream(inbox.messages().get(1)));
            assertEquals("E-Logistics - Xác minh tài khoản",first.getSubject());
            assertEquals("E-Logistics - Đặt lại mật khẩu",second.getSubject());
            assertTrue(((String)first.getContent()).contains(ORIGIN+"/verify-email#proofId="));
            assertTrue(((String)second.getContent()).contains(ORIGIN+"/recover/confirm#proofId="));
        }
    }
    @Test void realImplicitTlsUsesApprovedCertificateAndHasNoPlaintextFallback()throws Exception{
        try(var inbox=new CustomerSmtpTestInbox(1,false,true,250,serverTls);var intent=message(UUID.randomUUID())){
            var outcome=adapter(inbox,SmtpCustomerDelivery.Mode.IMPLICIT_TLS,trustRoots).send(intent);assertEquals(CustomerDeliveryTransport.Kind.ACCEPTED_BY_PROVIDER,outcome.kind());assertEquals(1,inbox.messages().size());assertEquals(1,inbox.authentications.get());
        }
    }
    @Test void missingStarttlsIsBlockedBeforeAuthEnvelopeOrProofBody()throws Exception{
        try(var inbox=new CustomerSmtpTestInbox(1,false,false,250,serverTls);var intent=message(UUID.randomUUID())){
            var outcome=adapter(inbox,SmtpCustomerDelivery.Mode.STARTTLS,trustRoots).send(intent);assertEquals(CustomerDeliveryTransport.Kind.BLOCKED,outcome.kind());assertTrue(inbox.messages().isEmpty());assertEquals(0,inbox.authentications.get());assertEquals(0,inbox.envelopes.get());
        }
    }
    @Test void untrustedCertificateFailsBeforeAuthAndRecipientDisclosure()throws Exception{
        try(var inbox=new CustomerSmtpTestInbox(1,true,false,250,serverTls);var intent=message(UUID.randomUUID())){
            var outcome=adapter(inbox,SmtpCustomerDelivery.Mode.STARTTLS,null).send(intent);assertEquals(CustomerDeliveryTransport.Kind.BLOCKED,outcome.kind());assertTrue(inbox.messages().isEmpty());assertEquals(0,inbox.authentications.get());assertEquals(0,inbox.envelopes.get());
        }
    }
    @Test void permanentAndTransientSmtpRejectionsAreDistinctAndDoNotClaimAcceptance()throws Exception{
        for(int rejection:List.of(450,550))try(var inbox=new CustomerSmtpTestInbox(1,false,false,rejection,serverTls);var intent=message(UUID.randomUUID())){
            var outcome=adapter(inbox,SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null).send(intent);assertEquals(rejection==450?CustomerDeliveryTransport.Kind.TRANSIENT_FAILURE:CustomerDeliveryTransport.Kind.PERMANENT_FAILURE,outcome.kind());assertNull(outcome.reference());assertTrue(inbox.messages().isEmpty());
        }
    }
    @Test void retryRetainsStableMessageIdRatherThanIssuingAnotherProof()throws Exception{
        try(var inbox=new CustomerSmtpTestInbox(2,false,false,250,serverTls);var intent=message(UUID.randomUUID())){
            var smtp=adapter(inbox,SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null);assertEquals(smtp.send(intent),smtp.send(intent));
            var received=inbox.messages();assertEquals(2,received.size());
            var first=new MimeMessage(Session.getInstance(new Properties()),new ByteArrayInputStream(received.get(0)));var second=new MimeMessage(Session.getInstance(new Properties()),new ByteArrayInputStream(received.get(1)));
            assertEquals(first.getMessageID(),second.getMessageID());assertEquals(first.getContent(),second.getContent());
        }
    }
    @Test void unsafeWireLoggingBlocksBeforeConnectingOrDecryptedBodyEmission()throws Exception{
        var logger=Logger.getLogger("org.eclipse.angus.mail.smtp.protocol");Level old=logger.getLevel();
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,serverTls);var intent=message(UUID.randomUUID())){
            logger.setLevel(Level.FINEST);assertEquals(CustomerDeliveryTransport.Kind.BLOCKED,adapter(inbox,SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null).send(intent).kind());assertTrue(inbox.messages().isEmpty());
        }finally{logger.setLevel(old);}
    }
    @Test void insecureOrUnapprovedSmtpConfigurationCannotEnableProductionFallback(){
        assertThrows(IllegalArgumentException.class,()->new SmtpCustomerDelivery.Settings("smtp.example.invalid",25,SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null,null,"no-reply@example.invalid",Set.of("example.invalid"),ORIGIN,true));
        assertThrows(IllegalArgumentException.class,()->new SmtpCustomerDelivery.Settings("127.0.0.1",25,SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null,null,"no-reply@example.invalid",Set.of("example.invalid"),ORIGIN,false));
        assertThrows(IllegalArgumentException.class,()->new SmtpCustomerDelivery.Settings("smtp.example.invalid",587,SmtpCustomerDelivery.Mode.STARTTLS,null,null,"no-reply@example.invalid",Set.of("example.invalid"),ORIGIN,false));
        assertThrows(IllegalArgumentException.class,()->new SmtpCustomerDelivery.Settings("127.0.0.1",25,SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null,null,"no-reply@example.invalid",Set.of("other.invalid"),ORIGIN,true));
        assertThrows(IllegalArgumentException.class,()->new SmtpCustomerDelivery.Settings("smtp.gmail.com",465,SmtpCustomerDelivery.Mode.LOCAL_REAL_SMTP,"sender@gmail.com","synthetic","sender@gmail.com",Set.of("gmail.com"),ORIGIN,true));
        assertThrows(IllegalArgumentException.class,()->new SmtpCustomerDelivery.Settings("smtp.gmail.com",587,SmtpCustomerDelivery.Mode.LOCAL_REAL_SMTP,"other@gmail.com","synthetic","sender@gmail.com",Set.of("gmail.com"),ORIGIN,true));
        assertDoesNotThrow(()->new SmtpCustomerDelivery.Settings("smtp.gmail.com",587,SmtpCustomerDelivery.Mode.LOCAL_REAL_SMTP,"sender@gmail.com","synthetic","sender@gmail.com",Set.of("gmail.com"),ORIGIN,true));
    }
}
