package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.api.IdentityGrantCommands.GrantData;
import com.elogistic.backend.identity.internal.domain.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdentityProvisionInputTests {
    private static final UUID R=new UUID(0,1), OTHER=new UUID(0,2);
    private static final GrantData PAIR=new GrantData("IDENTITY_MANAGEMENT","IDENTITY",R);
    private static IdentityProvisionCommands.Command input(String username,String display,String email,List<GrantData> grants) {
        return new IdentityProvisionCommands.Command(new UUID(0,3),username,display,email,grants,new UUID(0,4),new UUID(0,5));
    }
    private static IdentityProvisionCommands.Command valid() {return input("fixture.identity","Synthetic Identity","identity@example.invalid",List.of(PAIR));}
    private static void invalid(IdentityProvisionCommands.Command input) {
        var error=assertThrows(IdentityCommandFailure.class,()->ProvisionInput.from(input));
        assertEquals(IdentityCommandFailure.Code.VALIDATION_FAILED,error.code());assertEquals("VALIDATION_FAILED",error.getMessage());assertNull(error.getCause());
    }
    private static Grant pair(UUID realm) {return new Grant(RoleCode.IDENTITY_MANAGEMENT,new ResourceScope(ResourceScope.Type.IDENTITY,realm));}
    private static IdentityAccount actor() {
        return new IdentityAccount(new UUID(0,10),new Username("fixture.actor"),IdentityAccount.Status.ACTIVE,
            IdentityAccount.Activation.VERIFIED,new AccountVersion(2,3),Set.of(pair(R)));
    }
    private static IdentityAccount pending() {
        return new IdentityAccount(new UUID(0,11),new Username("fixture.pending"),IdentityAccount.Status.PENDING_ACTIVATION,
            IdentityAccount.Activation.UNVERIFIED,new AccountVersion(1,1),Set.of());
    }
    @Test void exactUsernameAndContactAreNotNormalized() {
        var p=ProvisionInput.from(input(" Fixture.User "," Synthetic ","Identity@Example.invalid",List.of(PAIR)));
        assertEquals(" Fixture.User ",p.username().value());assertEquals(" Synthetic ",p.displayName());assertEquals("Identity@Example.invalid",p.email());
    }
    @Test void displayNameCountsUnicodeCodePointsAndRejectsMalformedText() {
        assertEquals(160,ProvisionInput.from(input("valid","\uD83D\uDE80".repeat(160),"i@example.invalid",List.of(PAIR))).displayName().codePointCount(0,320));
        for(String s:List.of("","x".repeat(161),"\uD800","\uDC00","bad\u0000value")) invalid(input("valid",s,"i@example.invalid",List.of(PAIR)));
    }
    @Test void missingKeysOrRequiredFieldsAreRejectedWithoutEcho() {
        invalid(null);invalid(input(null,"Synthetic","i@example.invalid",List.of(PAIR)));
        invalid(input("\u00a0\uFEFF","Synthetic","i@example.invalid",List.of(PAIR)));
        invalid(input("x".repeat(81),"Synthetic","i@example.invalid",List.of(PAIR)));
        invalid(input("bad"+(char)0,"Synthetic","i@example.invalid",List.of(PAIR)));
        invalid(new IdentityProvisionCommands.Command(new UUID(0,3),"valid","Synthetic","i@example.invalid",List.of(PAIR),null,new UUID(0,5)));
        invalid(new IdentityProvisionCommands.Command(new UUID(0,3),"valid","Synthetic","i@example.invalid",List.of(PAIR),new UUID(0,4),null));
    }
    @Test void grantSetRejectsEmptyDuplicateAndMalformedMembers() {
        invalid(input("valid","Synthetic","i@example.invalid",null));invalid(input("valid","Synthetic","i@example.invalid",List.of()));
        invalid(input("valid","Synthetic","i@example.invalid",List.of(PAIR,PAIR)));
        invalid(input("valid","Synthetic","i@example.invalid",Arrays.asList((GrantData)null)));
        invalid(input("valid","Synthetic","i@example.invalid",List.of(new GrantData("ROLE","UNKNOWN",R))));
    }
    @Test void emailMatchesAcceptedFullFormatRatherThanTrimming() {
        for(String email:List.of("i@example.invalid","A.B+tag@sub.example.invalid")) assertEquals(email,ProvisionInput.from(input("valid","Synthetic",email,List.of(PAIR))).email());
        for(String email:List.of("missing-at","x@localhost","a..b@example.invalid"," x@example.invalid","x@example.invalid ","x@-example.invalid")) invalid(input("valid","Synthetic",email,List.of(PAIR)));
        String longEmail="a.".repeat(10000)+"a@example.invalid";
        assertEquals(longEmail,ProvisionInput.from(input("valid","Synthetic",longEmail,List.of(PAIR))).email());
    }
    @Test void fingerprintBindsEveryPayloadFieldAndIgnoresGrantOrder() {
        GrantData external=new GrantData("OPERATOR","WAREHOUSE",OTHER);
        var a=ProvisionInput.from(input("valid","Synthetic","i@example.invalid",List.of(PAIR,external)));
        assertEquals(a.fingerprint(),ProvisionInput.from(input("valid","Synthetic","i@example.invalid",List.of(external,PAIR))).fingerprint());
        for(var c:List.of(input("Valid","Synthetic","i@example.invalid",List.of(PAIR,external)),input("valid","Changed","i@example.invalid",List.of(PAIR,external)),
                input("valid","Synthetic","other@example.invalid",List.of(PAIR,external)),input("valid","Synthetic","i@example.invalid",List.of(PAIR))))
            assertNotEquals(a.fingerprint(),ProvisionInput.from(c).fingerprint());
    }
    @Test void commandAndValidatedInputAreImmutableAndRedacted() {
        var grants=new ArrayList<>(List.of(PAIR));var command=input("private.fixture","Private Contact","private@example.invalid",grants);grants.clear();
        assertEquals(1,command.grants().size());assertThrows(UnsupportedOperationException.class,()->command.grants().clear());
        assertFalse(command.toString().contains("private"));assertFalse(ProvisionInput.from(command).toString().contains("private"));
        assertEquals(Set.of("sessionId","username","displayName","email","grants","operationKey","requestId"),
            Arrays.stream(command.getClass().getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).collect(java.util.stream.Collectors.toSet()));
    }
    @Test void explicitInitialPairBindsActorAndNewPendingTarget() {
        var a=actor();var p=pending();var authority=new IdentityRealmGrantPolicy().resolveProvisioning(a,p,Set.of(pair(R)),R);
        assertNotNull(authority);authority.requireManagement(a,p,Set.of(pair(R)));assertEquals(a.id(),authority.actorId());assertEquals(p.id(),authority.targetId());
    }
    @Test void unsupportedOrCrossRealmInitialGrantsCannotDelegate() {
        var policy=new IdentityRealmGrantPolicy();var a=actor();var p=pending();
        for(var grants:List.of(Set.<Grant>of(),Set.of(pair(OTHER)),Set.of(pair(R),pair(OTHER)),Set.of(new Grant(new RoleCode("FINANCE"),pair(R).scope()))))
            assertNull(policy.resolveProvisioning(a,p,grants,R));
    }
    @Test void initialAuthorityRequiresActiveActorRealmAndFreshUnverifiedTarget() {
        var policy=new IdentityRealmGrantPolicy();var a=actor();var p=pending();
        assertNull(policy.resolveProvisioning(a,p,Set.of(pair(R)),null));assertNull(policy.resolveProvisioning(null,p,Set.of(pair(R)),R));
        assertNull(policy.resolveProvisioning(a,a,Set.of(pair(R)),R));
        var bound=new IdentityAccount(p.id(),p.username(),p.status(),p.activation(),p.version(),Set.of(pair(R)));
        assertNull(policy.resolveProvisioning(a,bound,Set.of(pair(R)),R));
        var changed=new IdentityAccount(p.id(),p.username(),p.status(),p.activation(),new AccountVersion(2,1),Set.of());
        assertNull(policy.resolveProvisioning(a,changed,Set.of(pair(R)),R));
    }
    @Test void provisioningDoesNotRelaxReplacementEmptyTargetGuard() {
        var policy=new IdentityRealmGrantPolicy();assertNotNull(policy.resolveProvisioning(actor(),pending(),Set.of(pair(R)),R));
        assertNull(policy.resolve(actor(),pending(),Set.of(pair(R)),R));
        assertNull(new FailClosedGrantAuthority((a,t,g)->{throw new AssertionError("Empty target must not reach replacement delegate");}).resolve(actor(),pending(),Set.of(pair(R))));
    }
    @Test void domainProvisioningRemainsPendingUnverifiedAndVersionOne() {
        var a=actor();var p=pending();var scope=new IdentityRealmGrantPolicy().resolveProvisioning(a,p,Set.of(pair(R)),R);
        var result=IdentityAccount.provision(p.id(),p.username(),Set.of(pair(R)),a,scope);
        assertEquals(IdentityAccount.Status.PENDING_ACTIVATION,result.status());assertEquals(IdentityAccount.Activation.UNVERIFIED,result.activation());
        assertEquals(new AccountVersion(1,1),result.version());assertFalse(result.active());assertFalse(result.managementAdmin());
    }
}
