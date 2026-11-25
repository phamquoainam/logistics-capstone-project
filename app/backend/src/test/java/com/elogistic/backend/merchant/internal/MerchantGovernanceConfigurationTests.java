package com.elogistic.backend.merchant.internal;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class MerchantGovernanceConfigurationTests {
    private MockEnvironment environment(){var e=new MockEnvironment();e.setActiveProfiles("identity","customer","postgres");return e.withProperty("customer.rollout","SANDBOX").withProperty("identity.admin-dashboard.environment","TEST").withProperty("identity.business-contact.enabled","true").withProperty("identity.admin-dashboard.enabled","true").withProperty("identity.admin-governance-step-up.enabled","true");}
    @Test void localGovernanceRequiresEveryIndependentGate(){
        assertDoesNotThrow(()->new MerchantGovernanceConfiguration(environment()));
        for(String profile:List.of("prod","production","demo","migration")){var e=environment();e.addActiveProfile(profile);assertThrows(IllegalStateException.class,()->new MerchantGovernanceConfiguration(e));}
        for(String flag:List.of("identity.business-contact.enabled","identity.admin-dashboard.enabled","identity.admin-governance-step-up.enabled")){var e=environment().withProperty(flag,"false");assertThrows(IllegalStateException.class,()->new MerchantGovernanceConfiguration(e));}
        assertThrows(IllegalStateException.class,()->new MerchantGovernanceConfiguration(environment().withProperty("identity.admin-dashboard.environment","PRODUCTION")));
        var incomplete=environment();incomplete.setActiveProfiles("identity","postgres");assertThrows(IllegalStateException.class,()->new MerchantGovernanceConfiguration(incomplete));
    }
    @Test void applicantGateDoesNotActivateOrRequireAdminAuthentication(){
        var e=environment().withProperty("identity.admin-dashboard.enabled","false").withProperty("identity.admin-governance-step-up.enabled","false");assertDoesNotThrow(()->MerchantGovernanceConfiguration.requireApplicantLocal(e));assertThrows(IllegalStateException.class,()->new MerchantGovernanceConfiguration(e));
    }
    @Test void protectionUsesRetainedConfiguredKeysWithoutDefaultOrGeneration(){
        assertThrows(IllegalStateException.class,()->MerchantGovernanceConfiguration.payloads(environment()));
        byte[] key=new byte[32];Arrays.fill(key,(byte)77);String encoded=Base64.getEncoder().encodeToString(key);
        var e=environment().withProperty("customer.security.delivery-encryption-keys","{\"e1\":\""+encoded+"\"}").withProperty("customer.security.current-encryption-key","e1");
        var payloads=MerchantGovernanceConfiguration.payloads(e);UUID event=UUID.randomUUID(),application=UUID.randomUUID();byte[] protectedValue=payloads.seal(event,application,1,"SUBMIT","Synthetic protected value");assertEquals("Synthetic protected value",payloads.open(event,application,1,"SUBMIT",protectedValue));
        assertThrows(IllegalStateException.class,()->MerchantGovernanceConfiguration.payloads(e.withProperty("customer.security.current-encryption-key","missing")));
    }
}
