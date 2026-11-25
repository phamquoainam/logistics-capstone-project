package com.elogistic.backend.notifications.internal;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class MerchantApplicationNoticeConfigurationTests {
    private MockEnvironment allowed(){var e=new MockEnvironment();e.setActiveProfiles("customer","identity","postgres");return e.withProperty("customer.rollout","SANDBOX").withProperty("identity.admin-dashboard.environment","TEST").withProperty("identity.business-contact.enabled","true").withProperty("identity.admin-governance-step-up.enabled","true");}
    @Test void localTestOnlyAndIndependentIdentityGatesRemainMandatory(){
        assertDoesNotThrow(()->new MerchantApplicationNoticeConfiguration(allowed()));
        for(String profile:List.of("production","prod","demo","migration","PRODUCTION")){var e=allowed();e.addActiveProfile(profile);assertThrows(IllegalStateException.class,()->new MerchantApplicationNoticeConfiguration(e));}
        for(String flag:List.of("identity.business-contact.enabled","identity.admin-governance-step-up.enabled"))assertThrows(IllegalStateException.class,()->new MerchantApplicationNoticeConfiguration(allowed().withProperty(flag,"false")));
        assertThrows(IllegalStateException.class,()->new MerchantApplicationNoticeConfiguration(allowed().withProperty("identity.admin-dashboard.environment","PRODUCTION")));
        assertThrows(IllegalStateException.class,()->new MerchantApplicationNoticeConfiguration(new MockEnvironment()));
    }
}
