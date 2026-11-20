package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.domain.ActionProof;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real cryptography, only synthetic in-memory keys; assertions never print secrets. */
class StaffInvitationProtectionTests {
    private static final Instant NOW=Instant.parse("2026-11-19T00:00:00.123456Z");
    private static byte[] key(){var bytes=new byte[32];new SecureRandom().nextBytes(bytes);return bytes;}

    @Test void staffFormatTwoIsPurposeSeparatedFromLegacySystemContextAndFingerprint() {
        var protection=new InvitationProtection(Map.of("v1",key()),"v1",Map.of("e1",key()),"e1");
        var account=UUID.randomUUID();var proofId=UUID.randomUUID();var message=UUID.randomUUID();
        var staff=new ActionProof(proofId,account,ActionProof.Purpose.STAFF_INV,NOW,NOW.plusSeconds(86400),ActionProof.State.AVAILABLE,null);
        var context=protection.staffContext(message,staff,"staff@example.invalid");
        assertNull(context.realmId());assertEquals(ActionProof.Purpose.STAFF_INV,context.purpose());
        try(var sealed=protection.issue(context)){
            byte[] token=protection.open(context,sealed.nonce(),sealed.ciphertext(),NOW);
            byte[] staffFingerprint=null,systemFingerprint=null;
            try{
                assertEquals(43,token.length);assertTrue(protection.matches(staff,"v1",token,sealed.verifier(),NOW));
                var system=new ActionProof(proofId,account,ActionProof.Purpose.INVITATION,NOW,staff.expiresAt(),ActionProof.State.AVAILABLE,null);
                assertFalse(protection.matches(system,"v1",token,sealed.verifier(),NOW));
                var wrongContext=new InvitationProtection.Context(message,proofId,account,UUID.randomUUID(),context.destination(),NOW,staff.expiresAt(),"v1","e1");
                assertThrows(IllegalStateException.class,()->protection.open(wrongContext,sealed.nonce(),sealed.ciphertext(),NOW));
                staffFingerprint=protection.acceptanceFingerprint(staff,"v1",token,"Synthetic password 7!");
                systemFingerprint=protection.acceptanceFingerprint(system,"v1",token,"Synthetic password 7!");
                assertFalse(java.security.MessageDigest.isEqual(staffFingerprint,systemFingerprint));
                assertFalse(protection.matches(staff,"v1",token,sealed.verifier(),staff.expiresAt()));
                assertFalse(protection.matches(staff.revoke(NOW),"v1",token,sealed.verifier(),NOW));
            }finally{InvitationProtection.wipe(token);InvitationProtection.wipe(staffFingerprint);InvitationProtection.wipe(systemFingerprint);}
        }
    }

    @Test void staffContextRequiresExplicitPurposeAndNoSystemRealm() {
        var protection=new InvitationProtection(Map.of("v1",key()),"v1",Map.of("e1",key()),"e1");
        var system=new ActionProof(UUID.randomUUID(),UUID.randomUUID(),ActionProof.Purpose.INVITATION,NOW,NOW.plusSeconds(86400),ActionProof.State.AVAILABLE,null);
        assertThrows(IllegalStateException.class,()->protection.staffContext(UUID.randomUUID(),system,"staff@example.invalid"));
        assertThrows(IllegalStateException.class,()->new InvitationProtection.Context(UUID.randomUUID(),system.id(),system.accountId(),UUID.randomUUID(),"staff@example.invalid",NOW,system.expiresAt(),"v1","e1",ActionProof.Purpose.STAFF_INV));
    }
}
