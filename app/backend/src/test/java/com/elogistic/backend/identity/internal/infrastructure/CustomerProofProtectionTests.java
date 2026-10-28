package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.domain.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

/** Real JCE/BCrypt with synthetic ephemeral keys; never print secret arrays in failures. */
class CustomerProofProtectionTests {
    private static final Instant NOW=Instant.parse("2026-10-27T00:00:00.123456Z");
    private static final UUID ACCOUNT=UUID.randomUUID(),SESSION=UUID.randomUUID(),ORG=UUID.randomUUID();
    private static final String DESTINATION="fixture@example.invalid";
    private static byte[] key(){byte[] key=new byte[32];new SecureRandom().nextBytes(key);return key;}
    private static ProtectedCustomerProofs protection(){return new ProtectedCustomerProofs(Map.of("v1",key()),"v1",Map.of("e1",key()),"e1");}
    private static CustomerProof proof(CustomerProof.Purpose purpose){
        boolean session=purpose.customerSessionRequired() || purpose==CustomerProof.Purpose.LEGACY_CUSTOMER_LINK;
        return CustomerProof.issue(UUID.randomUUID(),ACCOUNT,purpose,NOW,session?SESSION:null,session || purpose==CustomerProof.Purpose.CUSTOMER_RECOVERY?3L:null,purpose==CustomerProof.Purpose.OWNER_RECOVERY_CONSENT?ORG:null);
    }
    private static CustomerProof copy(CustomerProof p,UUID id,UUID account,Instant time,UUID session,Long version){
        return CustomerProof.issue(id,account,p.purpose(),time,session,version,p.organizationId());
    }
    @Test void purposeDeadlinesAndRequiredBindingsAreExact(){
        for(var purpose:CustomerProof.Purpose.values()){
            var p=proof(purpose);assertEquals(NOW.plusSeconds(purpose==CustomerProof.Purpose.CUSTOMER_ENROLLMENT?86400:900),p.expiresAt());
            if(purpose.customerSessionRequired() || purpose==CustomerProof.Purpose.LEGACY_CUSTOMER_LINK)
                assertThrows(DomainViolation.class,()->CustomerProof.issue(p.id(),ACCOUNT,purpose,NOW,null,null,p.organizationId()));
        }
    }
    @Test void invalidPartialSessionAndUnscopedOrganizationAreRejected(){
        assertThrows(DomainViolation.class,()->CustomerProof.issue(UUID.randomUUID(),ACCOUNT,CustomerProof.Purpose.SECURITY_EMAIL_CHANGE,NOW,SESSION,null,null));
        assertThrows(DomainViolation.class,()->CustomerProof.issue(UUID.randomUUID(),ACCOUNT,CustomerProof.Purpose.CUSTOMER_ENROLLMENT,NOW,SESSION,3L,null));
        assertThrows(DomainViolation.class,()->CustomerProof.issue(UUID.randomUUID(),ACCOUNT,CustomerProof.Purpose.CUSTOMER_RECOVERY,NOW,null,null,ORG));
        assertThrows(DomainViolation.class,()->CustomerProof.issue(UUID.randomUUID(),ACCOUNT,CustomerProof.Purpose.OWNER_RECOVERY_CONSENT,NOW,SESSION,3L,null));
        assertThrows(DomainViolation.class,()->CustomerProof.issue(UUID.randomUUID(),ACCOUNT,CustomerProof.Purpose.CUSTOMER_RECOVERY,NOW.plusNanos(1),null,null,null));
    }
    @Test void consumptionRequiresEveryCurrentBindingAndCannotReplay(){
        var p=proof(CustomerProof.Purpose.OWNER_RECOVERY_CONSENT);
        for(Runnable invalid:List.<Runnable>of(
            ()->p.consume(CustomerProof.Purpose.REAUTHENTICATION,ACCOUNT,SESSION,3L,ORG,NOW),
            ()->p.consume(p.purpose(),UUID.randomUUID(),SESSION,3L,ORG,NOW),
            ()->p.consume(p.purpose(),ACCOUNT,UUID.randomUUID(),3L,ORG,NOW),
            ()->p.consume(p.purpose(),ACCOUNT,SESSION,4L,ORG,NOW),
            ()->p.consume(p.purpose(),ACCOUNT,SESSION,3L,UUID.randomUUID(),NOW)))assertThrows(DomainViolation.class,invalid::run);
        var consumed=p.consume(p.purpose(),ACCOUNT,SESSION,3L,ORG,NOW);
        assertThrows(DomainViolation.class,()->consumed.consume(p.purpose(),ACCOUNT,SESSION,3L,ORG,NOW));
    }
    @Test void futureExpiryConsumedAndRevokedProofsAreUnavailable(){
        var p=proof(CustomerProof.Purpose.CUSTOMER_RECOVERY);
        p.requireAvailable(p.expiresAt().minusNanos(1));
        for(Instant time:List.of(NOW.minusNanos(1),p.expiresAt(),p.expiresAt().plusSeconds(1)))assertThrows(DomainViolation.class,()->p.requireAvailable(time));
        var revoked=new CustomerProof(p.id(),ACCOUNT,p.purpose(),NOW,p.expiresAt(),null,null,null,null,NOW);
        assertThrows(DomainViolation.class,()->revoked.requireAvailable(NOW));
    }
    @Test void issuanceEncryptsDistinctCanonicalHighEntropyPossessionTokens(){
        var crypto=protection();var p=proof(CustomerProof.Purpose.CUSTOMER_ENROLLMENT);
        try(var a=crypto.issue(p,DESTINATION);var b=crypto.issue(p,DESTINATION);
            var x=crypto.open(p,DESTINATION,a.verifierKey(),a.encryptionKey(),a.nonce(),a.ciphertext(),NOW);
            var y=crypto.open(p,DESTINATION,b.verifierKey(),b.encryptionKey(),b.nonce(),b.ciphertext(),NOW)){
            byte[] first=x.token(),second=y.token();try{
                assertEquals(43,first.length);assertEquals(32,Base64.getUrlDecoder().decode(first).length);
                assertFalse(Arrays.equals(first,second));assertFalse(Arrays.equals(a.nonce(),b.nonce()));assertFalse(Arrays.equals(a.verifier(),b.verifier()));
                assertEquals(59,a.ciphertext().length);assertEquals(12,a.nonce().length);
                assertTrue(crypto.matches(p,"v1",first,a.verifier(),NOW));
            }finally{Arrays.fill(first,(byte)0);Arrays.fill(second,(byte)0);}
        }
    }
    @Test void verifierBindsLocatorSubjectPurposeTimeSessionAndVersion(){
        var crypto=protection();var p=proof(CustomerProof.Purpose.SECURITY_EMAIL_CHANGE);
        try(var a=crypto.issue(p,DESTINATION);var opened=crypto.open(p,DESTINATION,"v1","e1",a.nonce(),a.ciphertext(),NOW)){
            byte[] token=opened.token();try{
                for(var changed:List.of(copy(p,UUID.randomUUID(),ACCOUNT,NOW,SESSION,3L),copy(p,p.id(),UUID.randomUUID(),NOW,SESSION,3L),
                    copy(p,p.id(),ACCOUNT,NOW.plusSeconds(1),SESSION,3L),copy(p,p.id(),ACCOUNT,NOW,UUID.randomUUID(),3L),copy(p,p.id(),ACCOUNT,NOW,SESSION,4L),
                    CustomerProof.issue(p.id(),ACCOUNT,CustomerProof.Purpose.BUSINESS_CONTACT,NOW,SESSION,3L,null)))
                    assertFalse(crypto.matches(changed,"v1",token,a.verifier(),NOW.plusSeconds(1)));
            }finally{Arrays.fill(token,(byte)0);}
        }
    }
    @Test void tamperingDestinationAndEitherKeyIdCannotDecrypt(){
        var crypto=protection();var p=proof(CustomerProof.Purpose.CUSTOMER_ENROLLMENT);
        try(var a=crypto.issue(p,DESTINATION)){
            byte[] changed=a.ciphertext();changed[0]^=1;
            assertThrows(IllegalStateException.class,()->crypto.open(p,DESTINATION,"v1","e1",a.nonce(),changed,NOW));
            assertThrows(IllegalStateException.class,()->crypto.open(p,"other@example.invalid","v1","e1",a.nonce(),a.ciphertext(),NOW));
            assertThrows(IllegalStateException.class,()->crypto.open(p,DESTINATION,"other","e1",a.nonce(),a.ciphertext(),NOW));
            assertThrows(IllegalStateException.class,()->crypto.open(p,DESTINATION,"v1","other",a.nonce(),a.ciphertext(),NOW));
            assertThrows(IllegalStateException.class,()->crypto.open(copy(p,UUID.randomUUID(),ACCOUNT,NOW,null,null),DESTINATION,"v1","e1",a.nonce(),a.ciphertext(),NOW));
        }
    }
    @Test void exactExpiryAndConsumedProofDenyDecryptionAndMatching(){
        var crypto=protection();var p=proof(CustomerProof.Purpose.CUSTOMER_RECOVERY);
        try(var a=crypto.issue(p,DESTINATION);var opened=crypto.open(p,DESTINATION,"v1","e1",a.nonce(),a.ciphertext(),NOW)){
            byte[] token=opened.token();try{
                var consumed=p.consume(p.purpose(),ACCOUNT,null,p.authVersion(),null,NOW);
                assertFalse(crypto.matches(p,"v1",token,a.verifier(),p.expiresAt()));
                assertFalse(crypto.matches(consumed,"v1",token,a.verifier(),NOW));
                assertThrows(DomainViolation.class,()->crypto.open(p,DESTINATION,"v1","e1",a.nonce(),a.ciphertext(),p.expiresAt()));
                assertThrows(DomainViolation.class,()->crypto.open(consumed,DESTINATION,"v1","e1",a.nonce(),a.ciphertext(),NOW));
            }finally{Arrays.fill(token,(byte)0);}
        }
    }
    @Test void retainedRotationKeysWorkWithoutReinterpretingTheirIdentifiers(){
        byte[] v1=key(),e1=key(),v2=key(),e2=key();
        var old=new ProtectedCustomerProofs(Map.of("v1",v1),"v1",Map.of("e1",e1),"e1");
        var rotated=new ProtectedCustomerProofs(Map.of("v1",v1,"v2",v2),"v2",Map.of("e1",e1,"e2",e2),"e2");
        var removed=new ProtectedCustomerProofs(Map.of("v2",v2),"v2",Map.of("e2",e2),"e2");
        var p=proof(CustomerProof.Purpose.CUSTOMER_ENROLLMENT);
        try(var a=old.issue(p,DESTINATION);var opened=rotated.open(p,DESTINATION,"v1","e1",a.nonce(),a.ciphertext(),NOW)){
            byte[] token=opened.token();try{
                assertTrue(rotated.matches(p,"v1",token,a.verifier(),NOW));assertFalse(rotated.matches(p,"v2",token,a.verifier(),NOW));
                assertFalse(removed.matches(p,"v1",token,a.verifier(),NOW));
                assertThrows(IllegalStateException.class,()->removed.open(p,DESTINATION,"v1","e1",a.nonce(),a.ciphertext(),NOW));
            }finally{Arrays.fill(token,(byte)0);}
        }
    }
    @Test void malformedMissingAndNonCanonicalPossessionInputsFailClosed(){
        var crypto=protection();var p=proof(CustomerProof.Purpose.CUSTOMER_RECOVERY);
        try(var a=crypto.issue(p,DESTINATION)){
            assertFalse(crypto.matches(p,null,null,a.verifier(),NOW));
            for(byte[] token:List.of(new byte[0],new byte[32],new byte[43],"a".repeat(43).getBytes(StandardCharsets.US_ASCII)))
                assertFalse(crypto.matches(p,"v1",token,a.verifier(),NOW));
            assertThrows(IllegalStateException.class,()->crypto.open(p,DESTINATION,"v1","e1",new byte[11],a.ciphertext(),NOW));
        }
    }
    @Test void consentAndReauthenticationCannotBecomeEmailPossessionProofs(){
        var crypto=protection();
        for(var purpose:List.of(CustomerProof.Purpose.REAUTHENTICATION,CustomerProof.Purpose.OWNER_RECOVERY_CONSENT))
            assertThrows(IllegalStateException.class,()->crypto.issue(proof(purpose),DESTINATION));
    }
    @Test void invalidWeakMissingAndCrossPurposeSharedKeysDenyStartup(){
        byte[] strong=key(),encryption=key();
        for(var invalid:List.of(Map.<String,byte[]>of(),Map.of("bad/key",strong),Map.of("v1",new byte[31]),Map.of("v1",new byte[32])))
            assertThrows(IllegalArgumentException.class,()->new ProtectedCustomerProofs(invalid,"v1",Map.of("e1",encryption),"e1"));
        assertThrows(IllegalArgumentException.class,()->new ProtectedCustomerProofs(Map.of("v1",strong),"missing",Map.of("e1",encryption),"e1"));
        assertThrows(IllegalArgumentException.class,()->new ProtectedCustomerProofs(Map.of("v1",strong),"v1",Map.of("e1",strong),"e1"));
    }
    @Test void requestFingerprintsAreKeyedOperationBoundAndLengthDelimited(){
        var crypto=protection();byte[] original=crypto.fingerprint("v1","enrollCustomer","ab","c",null);
        assertEquals(32,original.length);assertTrue(Arrays.equals(original,crypto.fingerprint("v1","enrollCustomer","ab","c",null)));
        assertFalse(Arrays.equals(original,crypto.fingerprint("v1","enrollCustomer","a","bc",null)));
        assertFalse(Arrays.equals(original,crypto.fingerprint("v1","enrollCustomer","ab","c","")));
        assertFalse(Arrays.equals(original,crypto.fingerprint("v1","requestCustomerRecovery","ab","c",null)));
        assertFalse(Arrays.equals(original,protection().fingerprint("v1","enrollCustomer","ab","c",null)));
        assertThrows(IllegalStateException.class,()->crypto.fingerprint("missing","enrollCustomer","input"));
    }
    @Test void protectedBuffersAreDefensiveClosedAndDiagnosticsRedacted(){
        var crypto=protection();var p=proof(CustomerProof.Purpose.CUSTOMER_ENROLLMENT);var a=crypto.issue(p,DESTINATION);
        byte[] changed=a.ciphertext();changed[0]^=1;
        var opened=crypto.open(p,DESTINATION,"v1","e1",a.nonce(),a.ciphertext(),NOW);
        byte[] token=opened.token();Arrays.fill(token,(byte)0);assertTrue(crypto.matches(p,"v1",opened.token(),a.verifier(),NOW));
        assertEquals("CustomerProof[redacted]",p.toString());assertEquals("ProtectedCustomerProof[redacted]",a.toString());
        assertEquals("OpenedCustomerProof[redacted]",opened.toString());assertEquals("ProtectedCustomerProofs[keys redacted]",crypto.toString());
        opened.close();a.close();assertThrows(IllegalStateException.class,opened::token);assertThrows(IllegalStateException.class,a::ciphertext);
    }
    @Test void passwordMaterialUsesAcceptedPolicyAndBcryptWithoutRawPersistence(){
        var crypto=protection();String password="synthetic-customer-password";String encoded=crypto.hashPassword(password);
        assertNotEquals(password,encoded);assertTrue(encoded.startsWith("$2a$12$"));assertTrue(new BCryptPasswordEncoder().matches(password,encoded));
        for(String invalid:List.of("short","a".repeat(73),"\u00e9".repeat(37)))assertThrows(DomainViolation.class,()->crypto.hashPassword(invalid));
    }
    @Test void deliveryRecipientIsSeparatelyEncryptedAndBoundToProofAndKeys(){
        var crypto=protection();var p=proof(CustomerProof.Purpose.CUSTOMER_ENROLLMENT);
        try(var sealed=crypto.issue(p,DESTINATION)){
            assertEquals(DESTINATION,crypto.openDestination(p,"v1","e1",sealed.recipientNonce(),sealed.recipientCiphertext(),NOW));
            byte[] changed=sealed.recipientCiphertext();changed[0]^=1;
            assertThrows(IllegalStateException.class,()->crypto.openDestination(p,"v1","e1",sealed.recipientNonce(),changed,NOW));
            assertThrows(IllegalStateException.class,()->crypto.openDestination(copy(p,UUID.randomUUID(),ACCOUNT,NOW,null,null),"v1","e1",sealed.recipientNonce(),sealed.recipientCiphertext(),NOW));
            assertThrows(IllegalStateException.class,()->crypto.openDestination(p,"other","e1",sealed.recipientNonce(),sealed.recipientCiphertext(),NOW));
            assertThrows(DomainViolation.class,()->crypto.openDestination(p,"v1","e1",sealed.recipientNonce(),sealed.recipientCiphertext(),p.expiresAt()));
            sealed.close();assertThrows(IllegalStateException.class,sealed::recipientCiphertext);
        }
    }
}
