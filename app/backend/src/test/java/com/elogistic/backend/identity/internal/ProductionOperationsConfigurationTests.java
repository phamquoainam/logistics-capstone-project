package com.elogistic.backend.identity.internal;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class ProductionOperationsConfigurationTests {
    @Test void dedicatedOpsDefaultsOffAndCannotEnableInProductionOrCustomerRollout(){
        assertFalse(ProductionOperationsConfiguration.enabled(new MockEnvironment()));
        for(String profile:new String[]{"production","prod","demo","migration","PRODUCTION"}){
            var e=valid();e.setActiveProfiles("customer","identity","postgres",profile);
            assertThrows(IllegalStateException.class,()->ProductionOperationsConfiguration.enabled(e));
        }
        for(String environment:new String[]{"PRODUCTION","DISABLED","","local"})assertThrows(IllegalStateException.class,()->ProductionOperationsConfiguration.enabled(valid().withProperty("identity.operations.environment",environment)));
        assertThrows(IllegalStateException.class,()->ProductionOperationsConfiguration.enabled(valid().withProperty("customer.rollout","PUBLIC")));
        assertThrows(IllegalStateException.class,()->ProductionOperationsConfiguration.enabled(valid().withProperty("server.forward-headers-strategy","NATIVE")));
        assertTrue(ProductionOperationsConfiguration.enabled(valid()));
    }
    private MockEnvironment valid(){var e=new MockEnvironment().withProperty("identity.operations.enabled","true").withProperty("identity.operations.environment","TEST").withProperty("customer.rollout","SANDBOX");e.setActiveProfiles("customer","identity","postgres");return e;}
}
