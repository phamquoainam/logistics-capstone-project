package com.elogistic.backend.orders.internal.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SupportCasePolicyTests {
    @Test void normalizationPreservesVietnameseAndPersonalCompatibility(){
        assertEquals("\u1ebf",SupportCasePolicy.text(" e\u0302\u0301 ",160,true));
        assertEquals("e\u0302\u0301",SupportCasePolicy.text(" e\u0302\u0301 ",160,false));
        assertEquals("a\nb\tc",SupportCasePolicy.text("a\r\nb\tc",4000,true));
        assertThrows(IllegalArgumentException.class,()->SupportCasePolicy.text("a\nb",160,true));
        assertThrows(IllegalArgumentException.class,()->SupportCasePolicy.text("a\u0000b",4000,true));
        assertThrows(IllegalArgumentException.class,()->SupportCasePolicy.text("x".repeat(4001),4000,true));
    }
    @Test void merchantNeverAcquiresCarrierLifecycleAuthority(){
        assertTrue(SupportCasePolicy.customerReply("NEEDS_INFORMATION"));
        assertFalse(SupportCasePolicy.customerReply("RESOLVED"));
        assertNull(SupportCasePolicy.transition("RESOLVED",false,"REOPEN"));
        assertEquals("IN_PROGRESS",SupportCasePolicy.transition("RESOLVED",true,"REOPEN"));
        assertNull(SupportCasePolicy.transition("CLOSED",true,"REOPEN"));
        for(String forbidden:new String[]{"PAID","DELIVERED","COMPENSATE","CARRIER_ACCEPTED"})assertNull(SupportCasePolicy.transition("IN_PROGRESS",false,forbidden));
    }
    @Test void personalCategoriesDoNotExpandWithOrganizationContract(){
        assertFalse(SupportCasePolicy.PERSONAL_CATEGORIES.contains("DAMAGE_REPORT"));
        assertTrue(SupportCasePolicy.ORGANIZATION_CATEGORIES.contains("DAMAGE_REPORT"));
        assertEquals(4,SupportCasePolicy.PERSONAL_CATEGORIES.size());
    }
    @Test void organizationSupportCannotBeEnabledByDefaultOrInProduction(){
        var env=new org.springframework.mock.env.MockEnvironment();
        assertFalse(com.elogistic.backend.orders.internal.OrganizationSupportConfiguration.enabled(env));
        env.withProperty("support.tickets.organization-enabled","true").withProperty("support.tickets.enabled","true").withProperty("support.tickets.environment","TEST").withProperty("customer.rollout","SANDBOX");
        assertTrue(com.elogistic.backend.orders.internal.OrganizationSupportConfiguration.enabled(env));
        for(String profile:new String[]{"prod","production"}){env.setActiveProfiles(profile);assertFalse(com.elogistic.backend.orders.internal.OrganizationSupportConfiguration.enabled(env));}
        env.setActiveProfiles("customer");env.withProperty("support.tickets.environment","PRODUCTION");assertFalse(com.elogistic.backend.orders.internal.OrganizationSupportConfiguration.enabled(env));
        env.withProperty("support.tickets.environment","TEST").withProperty("customer.rollout","LIVE");assertFalse(com.elogistic.backend.orders.internal.OrganizationSupportConfiguration.enabled(env));
    }
}
