package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.merchant.api.GovernanceFailure;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrganizationCasePolicyTests {
    @Test void ownerV1HasExactLimitsAndNoInventedRetentionOrTransferAuthority(){
        var policy=OrganizationCasePolicy.parse(OrganizationCasePolicy.ownerV1("TEST"));
        assertEquals(604800,policy.caseTtl());assertEquals(86400,policy.consentTtl());assertEquals(0,policy.maxFuture());
        assertFalse(policy.evidenceReady());assertTrue(policy.types().contains("RESTRICT"));assertFalse(policy.types().contains("OWNER_TRANSFER"));
        assertThrows(GovernanceFailure.class,()->OrganizationCasePolicy.parse(OrganizationCasePolicy.ownerV1("TEST").replace("604800","1")));
        assertThrows(GovernanceFailure.class,()->OrganizationCasePolicy.parse(OrganizationCasePolicy.ownerV1("TEST").replace("\"TEST\"","\"PRODUCTION\"")));
        assertThrows(GovernanceFailure.class,()->OrganizationCasePolicy.parse(OrganizationCasePolicy.ownerV1("TEST").replace("\"retention_policy_reference\":null","\"retention_policy_reference\":\"invented\"")));
    }
}
