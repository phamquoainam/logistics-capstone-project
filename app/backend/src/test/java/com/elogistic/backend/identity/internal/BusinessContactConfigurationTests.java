package com.elogistic.backend.identity.internal;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.*;
import static org.junit.jupiter.api.Assertions.*;

class BusinessContactConfigurationTests {
    private StandardEnvironment environment(boolean enabled,String purpose,String...profiles){
        var env=new StandardEnvironment();env.getPropertySources().remove("systemEnvironment");env.getPropertySources().remove("systemProperties");env.setActiveProfiles(profiles);
        env.getPropertySources().addFirst(new MapPropertySource("synthetic-contact-gate",Map.of("identity.business-contact.enabled",enabled,"customer.rollout","SANDBOX","identity.admin-dashboard.environment",purpose)));return env;
    }
    @Test void legacyCompositionStaysDisabledWithoutV41OptIn(){assertFalse(ProductionCustomerConfiguration.businessContactEnabled(environment(false,"DISABLED","customer","identity","postgres")));}
    @Test void productionAliasesAndWrongPhysicalPurposeCannotEnableContactWorkflow(){
        for(String profile:new String[]{"production","prod","demo","migration"})assertThrows(IllegalStateException.class,()->ProductionCustomerConfiguration.businessContactEnabled(environment(true,"TEST","customer","identity","postgres",profile)));
        assertThrows(IllegalStateException.class,()->ProductionCustomerConfiguration.businessContactEnabled(environment(true,"PRODUCTION","customer","identity","postgres")));
        assertThrows(IllegalStateException.class,()->ProductionCustomerConfiguration.businessContactEnabled(environment(true,"TEST","customer","identity")));
    }
    @Test void explicitLocalTestSandboxCompositionCanRequestThePhysicalSchemaGate(){
        for(String environment:new String[]{"LOCAL","TEST"})assertTrue(ProductionCustomerConfiguration.businessContactEnabled(environment(true,environment,"customer","identity","postgres")));
    }
}
