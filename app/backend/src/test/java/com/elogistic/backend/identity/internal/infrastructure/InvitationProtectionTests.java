package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.domain.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Genuine JCE, synthetic in-memory keys only. Never print token/key arrays in assertions. */
class InvitationProtectionTests {
    private static final Instant NOW=Instant.parse("2026-10-22T00:00:00.123456Z");
    private static final UUID ACCOUNT=UUID.fromString("10000000-0000-4000-8000-000000000001"),REALM=UUID.fromString("10000000-0000-4000-8000-000000000002");
    private static byte[] key(){byte[] key=new byte[32];new SecureRandom().nextBytes(key);return key;}
    private static InvitationProtection protection(){return new InvitationProtection(Map.of("v1",key()),"v1",Map.of("e1",key()),"e1");}
    private static ActionProof proof(){return new ActionProof(UUID.randomUUID(),ACCOUNT,ActionProof.Purpose.INVITATION,NOW,NOW.plusSeconds(86400),ActionProof.State.AVAILABLE,null);}
    private static InvitationProtection.Context changed(InvitationProtection.Context c,UUID message,UUID account,String destination,String encryption) {
        return new InvitationProtection.Context(message,c.proofId(),account,c.realmId(),destination,c.createdAt(),c.expiresAt(),c.verifierKeyId(),encryption);
    }
    @Test void freshTokensHaveFullUrlSafeEncodingAndDistinctProtectedValues() {
        var p=protection();var proof=proof();var c=p.context(proof,REALM,"fixture@example.invalid");
        try(var a=p.issue(c);var b=p.issue(c)) {
            byte[] x=p.open(c,a.nonce(),a.ciphertext(),NOW),y=p.open(c,b.nonce(),b.ciphertext(),NOW);
            try {
                assertEquals(43,x.length);assertEquals(32,Base64.getUrlDecoder().decode(x).length);
                for(byte value:x)assertTrue(value>='A' && value<='Z' || value>='a' && value<='z' || value>='0' && value<='9' || value=='-' || value=='_');
                assertFalse(Arrays.equals(x,y));assertFalse(Arrays.equals(a.verifier(),b.verifier()));assertFalse(Arrays.equals(a.nonce(),b.nonce()));
            } finally {InvitationProtection.wipe(x);InvitationProtection.wipe(y);}
        }
    }
    @Test void verifierAcceptsOnlyExactTokenAndBoundProofAccountPurposeAndTimes() {
        var p=protection();var proof=proof();var c=p.context(proof,REALM,"fixture@example.invalid");
        try(var sealed=p.issue(c)) {
            byte[] token=p.open(c,sealed.nonce(),sealed.ciphertext(),NOW);
            try {
                assertTrue(p.matches(proof,"v1",token,sealed.verifier(),NOW));
                assertFalse(p.matches(new ActionProof(proof.id(),REALM,proof.purpose(),NOW,proof.expiresAt(),proof.state(),null),"v1",token,sealed.verifier(),NOW));
                assertFalse(p.matches(new ActionProof(UUID.randomUUID(),ACCOUNT,proof.purpose(),NOW,proof.expiresAt(),proof.state(),null),"v1",token,sealed.verifier(),NOW));
                assertFalse(p.matches(new ActionProof(proof.id(),ACCOUNT,ActionProof.Purpose.RECOVERY,NOW,NOW.plusSeconds(900),proof.state(),null),"v1",token,sealed.verifier(),NOW));
                assertFalse(p.matches(new ActionProof(proof.id(),ACCOUNT,proof.purpose(),NOW.plusSeconds(1),proof.expiresAt(),proof.state(),null),"v1",token,sealed.verifier(),NOW.plusSeconds(1)));
                token[0]=token[0]=='A'?(byte)'B':(byte)'A';assertFalse(p.matches(proof,"v1",token,sealed.verifier(),NOW));
            } finally {InvitationProtection.wipe(token);}
        }
    }
    @Test void exactExpiryAndConsumedRevokedOrFutureProofFailClosed() {
        var p=protection();var proof=proof();var c=p.context(proof,REALM,"fixture@example.invalid");
        try(var sealed=p.issue(c)) {
            byte[] token=p.open(c,sealed.nonce(),sealed.ciphertext(),NOW);
            try {
                assertTrue(p.matches(proof,"v1",token,sealed.verifier(),proof.expiresAt().minusNanos(1)));
                for(Instant time:List.of(NOW.minusNanos(1),proof.expiresAt(),proof.expiresAt().plusSeconds(1)))assertFalse(p.matches(proof,"v1",token,sealed.verifier(),time));
                assertFalse(p.matches(proof.consume(proof.purpose(),ACCOUNT,NOW),"v1",token,sealed.verifier(),NOW));
                assertFalse(p.matches(proof.revoke(NOW),"v1",token,sealed.verifier(),NOW));
                assertThrows(IllegalStateException.class,()->p.open(c,sealed.nonce(),sealed.ciphertext(),proof.expiresAt()));
            } finally {InvitationProtection.wipe(token);}
        }
    }
    @Test void missingMalformedAndSharedPurposeKeysAreRejectedWithoutValues() {
        byte[] verifier=key(),encryption=key();
        for(Map<String,byte[]> invalid:List.of(Map.<String,byte[]>of(),Map.of("invalid/key",verifier),Map.of("v1",new byte[31]),Map.of("v1",new byte[32]))) {
            var error=assertThrows(IllegalArgumentException.class,()->new InvitationProtection(invalid,"v1",Map.of("e1",encryption),"e1"));
            assertEquals("INVITATION_KEY_CONFIGURATION_INVALID",error.getMessage());assertNull(error.getCause());
        }
        assertThrows(IllegalArgumentException.class,()->new InvitationProtection(null,"v1",Map.of("e1",encryption),"e1"));
        assertThrows(IllegalArgumentException.class,()->new InvitationProtection(Map.of("v1",verifier),null,Map.of("e1",encryption),"e1"));
        assertThrows(IllegalArgumentException.class,()->new InvitationProtection(Map.of("v1",verifier),"v1",Map.of("e1",verifier),"e1"));
    }
    @Test void keysAreDefensivelyCopiedAndOldKeysSupportRotation() {
        byte[] v=key(),e=key();var old=new InvitationProtection(Map.of("v1",v),"v1",Map.of("e1",e),"e1");
        var rotated=new InvitationProtection(Map.of("v1",v,"v2",key()),"v2",Map.of("e1",e,"e2",key()),"e2");
        InvitationProtection.wipe(v);InvitationProtection.wipe(e);
        var proof=proof();var c=old.context(proof,REALM,"fixture@example.invalid");
        try(var sealed=old.issue(c)) {
            byte[] token=rotated.open(c,sealed.nonce(),sealed.ciphertext(),NOW);
            try {assertTrue(rotated.matches(proof,"v1",token,sealed.verifier(),NOW));assertFalse(rotated.matches(proof,"missing",token,sealed.verifier(),NOW));}
            finally {InvitationProtection.wipe(token);}
            assertThrows(IllegalStateException.class,()->protection().open(c,sealed.nonce(),sealed.ciphertext(),NOW));
            assertThrows(IllegalStateException.class,()->rotated.issue(c));
        }
    }
    @Test void aeadRejectsCiphertextNonceAndContextTampering() {
        var p=protection();var c=p.context(proof(),REALM,"fixture@example.invalid");
        try(var sealed=p.issue(c)) {
            byte[] cipher=sealed.ciphertext(),nonce=sealed.nonce();cipher[0]^=1;nonce[0]^=1;
            assertThrows(IllegalStateException.class,()->p.open(c,sealed.nonce(),cipher,NOW));
            assertThrows(IllegalStateException.class,()->p.open(c,nonce,sealed.ciphertext(),NOW));
            for(var changed:List.of(changed(c,UUID.randomUUID(),ACCOUNT,c.destination(),"e1"),changed(c,c.messageId(),REALM,c.destination(),"e1"),changed(c,c.messageId(),ACCOUNT,"other@example.invalid","e1"),changed(c,c.messageId(),ACCOUNT,c.destination(),"unknown")))
                assertThrows(IllegalStateException.class,()->p.open(changed,sealed.nonce(),sealed.ciphertext(),NOW));
        }
    }
    @Test void malformedNoncanonicalOrUnknownVerifierNeverAuthenticates() {
        var p=protection();var proof=proof();var c=p.context(proof,REALM,"fixture@example.invalid");
        try(var sealed=p.issue(c)) {
            for(byte[] token:List.of(new byte[0],new byte[43],new byte[44],new byte[32]))assertFalse(p.matches(proof,"v1",token,sealed.verifier(),NOW));
            assertFalse(p.matches(proof,null,new byte[43],sealed.verifier(),NOW));
            assertFalse(p.matches(proof,"v1",null,sealed.verifier(),NOW));
        }
    }
    @Test void protectedBuffersAreDefensiveRedactedAndDestroyedOnClose() {
        var p=protection();var c=p.context(proof(),REALM,"fixture@example.invalid");var sealed=p.issue(c);
        byte[] cipher=sealed.ciphertext();cipher[0]^=1;
        byte[] token=p.open(c,sealed.nonce(),sealed.ciphertext(),NOW);InvitationProtection.wipe(token);
        assertTrue(Arrays.equals(new byte[43],token));assertEquals("SealedInvitation[redacted]",sealed.toString());
        assertFalse(c.toString().contains(c.destination()));assertEquals("InvitationProtection[keys redacted]",p.toString());
        sealed.close();sealed.close();assertThrows(IllegalStateException.class,sealed::ciphertext);assertThrows(IllegalStateException.class,sealed::verifier);
    }
    @Test void persistedMicrosecondPrecisionIsMandatoryForAuthenticatedContext() {
        var p=protection();var proof=proof();
        assertThrows(IllegalStateException.class,()->new InvitationProtection.Context(UUID.randomUUID(),proof.id(),ACCOUNT,REALM,"fixture@example.invalid",NOW.plusNanos(1),NOW.plusNanos(1).plusSeconds(86400),"v1","e1"));
        assertThrows(IllegalStateException.class,()->p.context(proof.consume(proof.purpose(),ACCOUNT,NOW),REALM,"fixture@example.invalid"));
    }
    @Test void deliveryPolicyRequiresExplicitBoundedAttemptsAndBudgets() {
        assertDoesNotThrow(()->new JdbcInvitationOutbox.Policy(3,Duration.ofSeconds(30),Duration.ofSeconds(10)));
        for(int attempts:List.of(0,11))assertThrows(IllegalArgumentException.class,()->new JdbcInvitationOutbox.Policy(attempts,Duration.ofSeconds(30),Duration.ofSeconds(10)));
        assertThrows(IllegalArgumentException.class,()->new JdbcInvitationOutbox.Policy(3,Duration.ofSeconds(4),Duration.ofSeconds(10)));
        assertThrows(IllegalArgumentException.class,()->new JdbcInvitationOutbox.Policy(3,Duration.ofSeconds(30),Duration.ofMinutes(6)));
    }
}
