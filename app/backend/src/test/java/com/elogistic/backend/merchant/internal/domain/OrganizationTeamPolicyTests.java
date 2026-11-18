package com.elogistic.backend.merchant.internal.domain;

import com.elogistic.backend.merchant.api.customer.OrganizationTeam.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrganizationTeamPolicyTests {
    private final Grant base=new Grant("dashboard.read","ORGANIZATION",null);
    @Test void templatesNeverGrantOrDelegateTeamFinanceOrPlatformByRole(){
        assertEquals(27,OrganizationTeamPolicy.delegable("OWNER").size());
        for(String role:List.of("MANAGER","OPERATOR"))for(String permission:List.of("team.read","team.invite","team.access.manage","shipment.finance.read","dashboard.finance.read","shipment.export.pii","STATION_INTAKE")){
            var ex=assertThrows(Failure.class,()->OrganizationTeamPolicy.delegation("OWNER",role,List.of(base,new Grant(permission,"ORGANIZATION",null)),(p,f)->true));
            assertTrue(Set.of("TEAM_DELEGATION_DENIED","VALIDATION_FAILED").contains(ex.getMessage()));
        }
        assertThrows(Failure.class,()->OrganizationTeamPolicy.delegation("MANAGER","MANAGER",List.of(base),(p,f)->true));
        assertThrows(Failure.class,()->OrganizationTeamPolicy.delegation("OPERATOR","OPERATOR",List.of(base),(p,f)->true));
    }
    @Test void grantsAreExplicitAndEveryFacilityMustBeCovered(){
        UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        var requested=List.of(base,new Grant("shipment.draft.read","FACILITY",a),new Grant("shipment.draft.write","FACILITY",a));
        assertDoesNotThrow(()->OrganizationTeamPolicy.delegation("OWNER","OPERATOR",requested,(p,f)->f==null||f.equals(a)));
        assertThrows(Failure.class,()->OrganizationTeamPolicy.delegation("OWNER","OPERATOR",requested,(p,f)->f==null||f.equals(b)));
        assertThrows(Failure.class,()->OrganizationTeamPolicy.grants(List.of(base,base)));
        assertThrows(Failure.class,()->OrganizationTeamPolicy.grants(List.of(new Grant("shipment.draft.read","ORGANIZATION",null))));
        assertThrows(Failure.class,()->OrganizationTeamPolicy.grants(List.of(base,new Grant("team.read","FACILITY",a))));
        assertThrows(Failure.class,()->OrganizationTeamPolicy.grants(List.of(base,new Grant("defaults.manage","FACILITY",a))));
        assertThrows(Failure.class,()->OrganizationTeamPolicy.grants(List.of(base,new Grant("shipment.draft.write","FACILITY",a))));
        assertThrows(Failure.class,()->OrganizationTeamPolicy.grants(List.of(base,new Grant("shipment.draft.read","FACILITY",a),new Grant("shipment.draft.write","FACILITY",b))));
        assertDoesNotThrow(()->OrganizationTeamPolicy.grants(List.of(base,new Grant("shipment.draft.read","ORGANIZATION",null),new Grant("shipment.draft.write","FACILITY",a))));
        assertThrows(Failure.class,()->OrganizationTeamPolicy.grants(List.of(base,new Grant("contacts.import","FACILITY",a),new Grant("contacts.read","FACILITY",a))));
    }
    @Test void ownerAndTerminalRevokeAreProtectedAcrossAllTransitions(){
        for(String op:List.of("ACCESS","SUSPEND","RESUME","REVOKE_MEMBER")){
            assertEquals("LAST_OWNER_PROTECTED",assertThrows(Failure.class,()->OrganizationTeamPolicy.transition("OWNER","ACTIVE",op)).getMessage());
            assertThrows(Failure.class,()->OrganizationTeamPolicy.transition("OPERATOR","REVOKED",op));
        }
        assertDoesNotThrow(()->OrganizationTeamPolicy.transition("OPERATOR","SUSPENDED","RESUME"));
        assertDoesNotThrow(()->OrganizationTeamPolicy.transition("MANAGER","ACTIVE","SUSPEND"));
        assertThrows(Failure.class,()->OrganizationTeamPolicy.transition("MANAGER","ACTIVE","RESUME"));
    }
    @Test void sensitiveInputDoesNotAppearInGeneratedToString(){
        assertEquals("TeamInvitationInput[redacted]",new InvitationInput("synthetic@example.invalid","OPERATOR",List.of(base),"Synthetic").toString());
        assertEquals("TeamInvitationDecision[redacted]",new InvitationDecision("synthetic-proof").toString());
        assertThrows(Failure.class,()->OrganizationTeamPolicy.reason("a\nb"));
        assertThrows(Failure.class,()->OrganizationTeamPolicy.reason(" "));
    }
}
