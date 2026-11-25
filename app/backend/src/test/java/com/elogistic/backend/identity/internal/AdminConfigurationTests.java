package com.elogistic.backend.identity.internal;

import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminConfigurationTests {
    @Test void onboardingDefaultsOffAndRejectsImplicitOrProductionActivation(){
        var defaults=new MockEnvironment();defaults.setActiveProfiles("identity","postgres");assertFalse(ProductionAdminOnboardingConfiguration.enabled(defaults));
        for(String flag:new String[]{"identity.admin-invitations.delivery-enabled","identity.local-realm-bootstrap.enabled"})assertThrows(IllegalStateException.class,()->ProductionAdminOnboardingConfiguration.enabled(new MockEnvironment().withProperty(flag,"true")));
        for(String profile:new String[]{"prod","production","demo","migration"}){var e=new MockEnvironment().withProperty("identity.admin-onboarding.enabled","true").withProperty("identity.admin-dashboard.environment","TEST").withProperty("customer.rollout","SANDBOX");e.setActiveProfiles("identity","postgres",profile);assertThrows(IllegalStateException.class,()->ProductionAdminOnboardingConfiguration.enabled(e));}
        var valid=new MockEnvironment().withProperty("identity.admin-onboarding.enabled","true").withProperty("identity.admin-dashboard.environment","TEST").withProperty("customer.rollout","SANDBOX");valid.setActiveProfiles("identity","postgres");assertTrue(ProductionAdminOnboardingConfiguration.enabled(valid));
        assertThrows(IllegalStateException.class,()->ProductionAdminOnboardingConfiguration.enabled(new MockEnvironment().withProperty("identity.admin-onboarding.enabled","TRUE")));
    }
    @Test void adminReadGateIsOffByDefaultWithoutDatabaseAccess(){var env=new MockEnvironment();env.setActiveProfiles("identity","postgres");var source=mock(DataSource.class);assertTrue(new ProductionIdentityHttpConfiguration(env).adminSessionPolicy(source,env).current(UUID.randomUUID()).isEmpty());verifyNoInteractions(source);}
    @Test void productionProfilesDenyEvenWhenLocalFlagsAreRequested(){for(String profile:new String[]{"prod","production","PRODUCTION","Prod"}){var env=new MockEnvironment().withProperty("identity.admin-dashboard.enabled","true").withProperty("identity.admin-dashboard.environment","TEST");env.setActiveProfiles("identity","postgres",profile);var source=mock(DataSource.class);assertTrue(new ProductionIdentityHttpConfiguration(env).adminSessionPolicy(source,env).current(UUID.randomUUID()).isEmpty());verifyNoInteractions(source);}}
    @Test void unsupportedEnvironmentNeverAdmitsFromAFlag(){var env=new MockEnvironment().withProperty("identity.admin-dashboard.enabled","true").withProperty("identity.admin-dashboard.environment","PRODUCTION");env.setActiveProfiles("identity","postgres");var source=mock(DataSource.class);assertTrue(new ProductionIdentityHttpConfiguration(env).adminSessionPolicy(source,env).current(UUID.randomUUID()).isEmpty());verifyNoInteractions(source);}
}
