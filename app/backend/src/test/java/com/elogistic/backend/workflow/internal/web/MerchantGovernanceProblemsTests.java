package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.merchant.api.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MerchantGovernanceProblemsTests {
    @Test void approvedConflictAndPolicyProblemsHaveSafeFiniteHttpResponses(){
        var reads=mock(AdminAdmission.class);var commands=mock(AdminCommandAdmission.class);var governance=mock(MerchantGovernance.class);
        var decisions=new MerchantGovernanceDecisionHttp(reads,commands,governance,mock(AdminGovernanceReauthentication.class));
        var applications=new MerchantGovernanceHttp(reads,commands,governance);
        var organizations=new MerchantOrganizationGovernanceHttp(reads,commands,governance);
        for(var entry:Map.of(GovernanceFailure.Code.BUSINESS_CONFLICT,409,GovernanceFailure.Code.SNAPSHOT_EXPIRED,409,GovernanceFailure.Code.LEGACY_KEY_CLEARANCE_REQUIRED,422,GovernanceFailure.Code.POLICY_UNAVAILABLE,422,GovernanceFailure.Code.ACCESS_DENIED,403,GovernanceFailure.Code.SERVICE_UNAVAILABLE,503).entrySet()){
            var failure=new GovernanceFailure(entry.getKey());
            for(ResponseEntity<Map<String,String>> response:java.util.List.of(decisions.rejected(failure),applications.rejected(failure),organizations.rejected(failure))){
                assertEquals(entry.getValue(),response.getStatusCode().value());
                assertEquals(Map.of("code",entry.getKey().name(),"message","Request rejected."),response.getBody());
            }
        }
    }
}
