package com.elogistic.backend.identity.internal;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class ProductionStaffConfigurationTests {
    @Test void defaultsDoNotRequireOrGenerateAnySecret(){assertFalse(ProductionStaffConfiguration.enabled(new MockEnvironment()));}
    @Test void explicitEnablementRequiresAllIndependentIsolationGates(){
        for(String environment:new String[]{"DISABLED","PRODUCTION","","local"})assertThrows(IllegalStateException.class,()->ProductionStaffConfiguration.enabled(valid().withProperty("identity.staff.environment",environment)));
        for(String profile:new String[]{"production","prod","demo","migration","PRODUCTION"}){
            var e=valid();e.setActiveProfiles("customer","identity","postgres",profile);assertThrows(IllegalStateException.class,()->ProductionStaffConfiguration.enabled(e));
        }
        assertThrows(IllegalStateException.class,()->ProductionStaffConfiguration.enabled(valid().withProperty("customer.rollout","PUBLIC")));
        assertThrows(IllegalStateException.class,()->ProductionStaffConfiguration.enabled(valid().withProperty("server.forward-headers-strategy","NATIVE")));
        assertThrows(IllegalStateException.class,()->ProductionStaffConfiguration.enabled(valid().withProperty("identity.staff.enabled","TRUE")));
        assertTrue(ProductionStaffConfiguration.enabled(valid()));
    }
    private MockEnvironment valid(){var e=new MockEnvironment().withProperty("identity.staff.enabled","true").withProperty("identity.staff.environment","TEST").withProperty("customer.rollout","SANDBOX");e.setActiveProfiles("customer","identity","postgres");return e;}
}
