package com.elogistic.backend.orders.internal.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClaimIntakePolicyTests {
    @Test void allegationsAndDocumentReviewNeverBecomeAnAwardOrCarrierState(){
        for(String category:new String[]{"LOSS","DAMAGE","DELAY","SERVICE_DISPUTE"})assertEquals(category,ClaimIntakePolicy.category(category));
        assertEquals("UNDER_REVIEW",ClaimIntakePolicy.staffTransition("RECEIVED","BEGIN_REVIEW"));
        assertEquals("NEEDS_INFORMATION",ClaimIntakePolicy.staffTransition("UNDER_REVIEW","REQUEST_INFORMATION"));
        assertEquals("UNDER_REVIEW",ClaimIntakePolicy.supplement("NEEDS_INFORMATION"));
        assertEquals("CLOSED_WITHOUT_DECISION",ClaimIntakePolicy.staffTransition("UNDER_REVIEW","CLOSE_WITHOUT_DECISION"));
        for(String action:new String[]{"APPROVE","PAY","AWARD","REJECT_ENTITLEMENT","DELIVERED","PAID","CARRIER_ACCEPTED","REFUND"})assertThrows(IllegalArgumentException.class,()->ClaimIntakePolicy.staffTransition("UNDER_REVIEW",action));
        assertThrows(IllegalArgumentException.class,()->ClaimIntakePolicy.supplement("CLOSED_WITHOUT_DECISION"));
        assertThrows(IllegalArgumentException.class,()->ClaimIntakePolicy.category("COD_PAYOUT"));
    }
    @Test void textIsMeasuredInUnicodeCodePointsAndDoesNotSilentlyTruncate(){
        assertEquals("Synthetic evidence narrative",ClaimIntakePolicy.narrative(" Synthetic evidence narrative "));
        assertEquals(4000,ClaimIntakePolicy.narrative("x".repeat(4000)).length());
        for(String bad:new String[]{""," ","x".repeat(4001),"control\u0000value"})assertThrows(IllegalArgumentException.class,()->ClaimIntakePolicy.narrative(bad));
    }
}
