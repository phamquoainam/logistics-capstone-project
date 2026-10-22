package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.internal.domain.*;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pure policy inputs are synthetic; registry, session and transaction proofs belong to PostgreSQL tests. */
class IdentityRealmGrantPolicyTests {
    private static final UUID R = new UUID(0,1), OTHER = new UUID(0,2);
    private static final Grant PAIR = pair(R);
    private final IdentityRealmGrantPolicy policy = new IdentityRealmGrantPolicy();
    private static Grant pair(UUID realm) { return new Grant(RoleCode.IDENTITY_MANAGEMENT, new ResourceScope(ResourceScope.Type.IDENTITY,realm)); }
    private static IdentityAccount account(int id, Set<Grant> grants) {
        return new IdentityAccount(new UUID(0,id),new Username("fixture.realm"),IdentityAccount.Status.ACTIVE,
            IdentityAccount.Activation.VERIFIED,new AccountVersion(2,3),grants);
    }
    @Test void exactCurrentSameRealmPairProducesBoundedAuthority() {
        var actor=account(10,Set.of(PAIR));var target=account(11,Set.of(PAIR));
        var authority=policy.resolve(actor,target,Set.of(PAIR),R);
        assertNotNull(authority);authority.requireManagement(actor,target,Set.of(PAIR));
        assertEquals(Set.of(PAIR),authority.manageable());assertEquals(Set.of(PAIR),authority.assignable());
        assertEquals(3,authority.actorAuthVersion());assertEquals(2,authority.targetVersion());
    }
    @Test void missingRegisteredRealmNeverAuthorizes() {
        assertNull(policy.resolve(account(10,Set.of(PAIR)),account(11,Set.of(PAIR)),Set.of(PAIR),null));
    }
    @Test void ordinaryGrantPossessionNeverDelegatesManagement() {
        var ordinary=new Grant(new RoleCode("FINANCE"),PAIR.scope());
        assertNull(policy.resolve(account(10,Set.of(ordinary)),account(11,Set.of(PAIR)),Set.of(PAIR),R));
    }
    @Test void targetMustHaveActualSameRealmManagementContext() {
        var actor=account(10,Set.of(PAIR));
        for(var grants:java.util.List.of(Set.<Grant>of(),Set.of(pair(OTHER))))
            assertNull(policy.resolve(actor,account(11,grants),Set.of(PAIR),R));
    }
    @Test void crossRealmActorCannotManageMatchingTarget() {
        assertNull(policy.resolve(account(10,Set.of(pair(OTHER))),account(11,Set.of(PAIR)),Set.of(PAIR),R));
    }
    @Test void unrelatedRequestedAndHiddenCurrentGrantDenyEntireReplacement() {
        var external=new Grant(new RoleCode("OPERATOR"),new ResourceScope(ResourceScope.Type.WAREHOUSE,OTHER));
        var actor=account(10,Set.of(PAIR));
        assertNull(policy.resolve(actor,account(11,Set.of(PAIR,external)),Set.of(PAIR),R));
        assertNull(policy.resolve(actor,account(11,Set.of(PAIR)),Set.of(PAIR,external),R));
        assertNull(policy.resolve(actor,account(11,Set.of(PAIR)),Set.of(new Grant(new RoleCode("FINANCE"),PAIR.scope())),R));
    }
    @Test void disabledActorAndMissingInputsDeny() {
        var a=account(10,Set.of(PAIR));var target=account(11,Set.of(PAIR));
        var disabled=new IdentityAccount(a.id(),a.username(),IdentityAccount.Status.DISABLED,a.activation(),a.version(),a.grants());
        assertNull(policy.resolve(disabled,target,Set.of(PAIR),R));
        assertNull(policy.resolve(null,target,Set.of(PAIR),R));assertNull(policy.resolve(a,null,Set.of(PAIR),R));
        assertNull(policy.resolve(a,target,null,R));assertNull(policy.resolve(a,target,Set.of(),R));
    }
    @Test void existingDomainStillRejectsSelfManagement() {
        var a=account(10,Set.of(PAIR));var authority=policy.resolve(a,a,Set.of(PAIR),R);
        assertNotNull(authority);assertThrows(DomainViolation.class,()->authority.requireManagement(a,a,Set.of(PAIR)));
    }
}
