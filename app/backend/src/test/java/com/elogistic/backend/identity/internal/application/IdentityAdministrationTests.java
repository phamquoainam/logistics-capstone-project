package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.api.IdentityAdministrationCommands.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.elogistic.backend.identity.api.IdentityCommandFailure.Code.*;

class IdentityAdministrationTests {
    private static final Instant NOW=Instant.parse("2026-10-24T00:00:00Z");
    private static final UUID A=UUID.randomUUID(),B=UUID.randomUUID(),R=UUID.randomUUID(),SID=UUID.randomUUID(),REQ=UUID.randomUUID(),KEY=UUID.randomUUID();
    private final Grant pair=new Grant(RoleCode.IDENTITY_MANAGEMENT,new ResourceScope(ResourceScope.Type.IDENTITY,R));
    private IdentityAdministrationStore store;private IdentityAdministrationStore.Transaction tx;
    private IdentityGrantStore.Transaction accounts;private CurrentSessionAdmission admission;
    private GrantAuthority authority;private InvitationIssuer issuer;private Clock clock;private IdentityAdministration app;
    private IdentityAccount actor,target;private IdentitySession session;
    private final Map<String,IdentityAdministrationStore.Receipt> receipts=new HashMap<>();
    @BeforeEach void fixture(){
        receipts.clear();actor=account(A,IdentityAccount.Status.ACTIVE,IdentityAccount.Activation.VERIFIED);target=account(B,IdentityAccount.Status.ACTIVE,IdentityAccount.Activation.VERIFIED);session=IdentitySession.open(SID,actor,NOW);
        store=mock(IdentityAdministrationStore.class);tx=mock(IdentityAdministrationStore.Transaction.class);accounts=mock(IdentityGrantStore.Transaction.class);admission=mock(CurrentSessionAdmission.class);issuer=mock(InvitationIssuer.class);clock=mock(Clock.class);when(clock.instant()).thenReturn(NOW);
        when(admission.admit(eq(CurrentSessionAdmission.Operation.CURRENT_SESSION),eq(REQ))).thenReturn(new CurrentSessionAdmission.Context(SID,A,1,NOW,NOW.plusSeconds(900)));
        when(store.transaction(any())).thenAnswer(i->{Function<IdentityAdministrationStore.Transaction,Object> f=i.getArgument(0);return f.apply(tx);});when(tx.accounts()).thenReturn(accounts);
        when(accounts.sessionAccount(SID)).thenReturn(Optional.of(A));when(accounts.lockAccount(A)).thenAnswer(i->Optional.of(actor));when(accounts.lockAccount(B)).thenAnswer(i->Optional.of(target));when(accounts.lockSession(SID)).thenReturn(Optional.of(session));when(accounts.activeAdministrators()).thenReturn(2L);
        when(tx.activeRealm()).thenReturn(Optional.of(new IdentityAdministrationStore.Realm(R,"SYSTEM")));
        authority=mock(GrantAuthority.class);when(authority.resolve(any(),any(),any())).thenAnswer(i->{IdentityAccount a=i.getArgument(0),t=i.getArgument(1);return new ManagementAuthority(a.id(),a.version().authVersion(),t.id(),t.version().version(),Set.of(pair),Set.of(pair));});
        when(accounts.accountView(B)).thenAnswer(i->view(target));doAnswer(i->{target=i.getArgument(1);return null;}).when(tx).updateState(any(),any(),any());
        when(tx.receipt(eq(A),anyString(),eq(KEY))).thenAnswer(i->Optional.ofNullable(receipts.get(i.getArgument(1))));doAnswer(i->{receipts.put(i.getArgument(1),i.getArgument(3));return null;}).when(tx).receipt(eq(A),anyString(),eq(KEY),any(),anyInt(),any());
        app=new IdentityAdministration(store,admission,authority,issuer,clock,Duration.ofHours(24));
    }
    private IdentityAccount account(UUID id,IdentityAccount.Status s,IdentityAccount.Activation a){return new IdentityAccount(id,new Username("synthetic."+id),s,a,new AccountVersion(1,1),Set.of(pair));}
    private IdentityGrantCommands.Account view(IdentityAccount a){return new IdentityGrantCommands.Account(a.id(),a.username().value(),"Synthetic","synthetic@example.invalid",a.status().name(),List.of(new IdentityGrantCommands.GrantData("IDENTITY_MANAGEMENT","IDENTITY",R)),a.version().version(),a.activation()==IdentityAccount.Activation.VERIFIED);}
    private Command command(){return new Command(REQ,B,1,KEY,"Synthetic lifecycle change");}
    private void denied(IdentityCommandFailure.Code code,Runnable f){assertEquals(code,assertThrows(IdentityCommandFailure.class,f::run).code());}
    private void noWrites(){verify(tx,never()).updateState(any(),any(),any());verify(tx,never()).audit(any(),any(),any(),any(),any(),any());verify(tx,never()).appendProof(any(),any(),any());}
    private void pending(){target=account(B,IdentityAccount.Status.PENDING_ACTIVATION,IdentityAccount.Activation.UNVERIFIED);}
    private InvitationIssuer.Prepared prepared(){var p=mock(InvitationIssuer.Prepared.class);when(p.proof()).thenReturn(ActionProof.invitation(UUID.randomUUID(),target,NOW));when(p.tokenHash()).thenReturn(new byte[32]);when(p.verifierKeyId()).thenReturn("v1");when(issuer.prepare(any(),any(),any())).thenReturn(p);return p;}
    @Test void accountReadReturnsOnlyAcceptedViewWithoutActivityWrites(){assertEquals(view(target),app.getIdentityAccount(B,REQ));noWrites();verify(accounts,never()).revokeSessions(any(),any());}
    @Test void unknownAndUnmanagedTargetHaveSameSafeDenial(){when(accounts.lockAccount(B)).thenReturn(Optional.empty());denied(ACCOUNT_NOT_ACCESSIBLE,()->app.getIdentityAccount(B,REQ));when(accounts.lockAccount(B)).thenReturn(Optional.of(target));doReturn(null).when(authority).resolve(any(),any(),any());denied(ACCOUNT_NOT_ACCESSIBLE,()->app.getIdentityAccount(B,REQ));noWrites();}
    @Test void trustedSubjectMustMatchAuthoritativeSessionOwner(){when(accounts.sessionAccount(SID)).thenReturn(Optional.of(B));denied(SESSION_INVALID,()->app.listIdentityRoles(REQ));verify(accounts,never()).lockAccount(any());noWrites();}
    @Test void expiredAccessFactsCannotReachPersistence(){when(clock.instant()).thenReturn(NOW.plusSeconds(900));denied(SESSION_INVALID,()->app.listIdentityRoles(REQ));verifyNoInteractions(store);}
    @Test void waitingForLocksRechecksAccessExpiry(){when(clock.instant()).thenReturn(NOW,NOW.plusSeconds(900));denied(SESSION_INVALID,()->app.listIdentityRoles(REQ));verify(accounts).lockSession(SID);noWrites();}
    @Test void disabledActorCannotUseAdministrativeOperations(){actor=account(A,IdentityAccount.Status.DISABLED,IdentityAccount.Activation.VERIFIED);denied(SESSION_INVALID,()->app.disableIdentityAccount(command()));noWrites();}
    @Test void staleAuthorizationVersionAndRevocationDeny(){when(accounts.lockSession(SID)).thenReturn(Optional.of(session.revoke(NOW)));denied(SESSION_INVALID,()->app.listIdentityRoles(REQ));when(accounts.lockSession(SID)).thenReturn(Optional.of(session));actor=new IdentityAccount(A,actor.username(),actor.status(),actor.activation(),new AccountVersion(2,2),actor.grants());denied(SESSION_INVALID,()->app.listIdentityScopes(REQ));noWrites();}
    @Test void inactiveRealmHasOperationAppropriateSafeDenial(){when(tx.activeRealm()).thenReturn(Optional.empty());denied(AUTHORITY_REQUIRED,()->app.listIdentityRoles(REQ));denied(ACCOUNT_NOT_ACCESSIBLE,()->app.getIdentityAccount(B,REQ));noWrites();}
    @Test void mixedActorScopesNeverAcquireNewAuthority(){actor=new IdentityAccount(A,actor.username(),actor.status(),actor.activation(),actor.version(),Set.of(pair,new Grant(new RoleCode("OPERATOR"),new ResourceScope(ResourceScope.Type.WAREHOUSE,UUID.randomUUID()))));denied(AUTHORITY_REQUIRED,()->app.listIdentityScopes(REQ));noWrites();}
    @Test void missingAdmissionAndShortRetentionCannotCompose(){assertThrows(NullPointerException.class,()->new IdentityAdministration(store,null,authority,issuer,clock,Duration.ofHours(24)));assertThrows(IllegalArgumentException.class,()->new IdentityAdministration(store,admission,authority,issuer,clock,Duration.ofHours(1)));}
    @Test void authorizationPrecedesMalformedMutationMetadata(){doReturn(null).when(authority).resolve(any(),any(),any());denied(ACCOUNT_NOT_ACCESSIBLE,()->app.disableIdentityAccount(new Command(REQ,B,0,null,null)));noWrites();}
    @Test void roleAndScopeCatalogExposeOnlyApprovedPair(){assertEquals(List.of(new Role("IDENTITY_MANAGEMENT","Registered Identity Realm account management")),app.listIdentityRoles(REQ).items());assertEquals(List.of(new Scope(R,"IDENTITY","SYSTEM")),app.listIdentityScopes(REQ).items());noWrites();}
    @Test void paginationRequestsFilteredLimitPlusOneAndNoTotal(){when(tx.visibleAccountIds(A,R,null,2)).thenReturn(List.of(B,UUID.randomUUID()));var page=app.listIdentityAccounts(new Query(REQ,null,1));assertEquals(List.of(view(target)),page.items());assertNotNull(page.nextCursor());assertEquals(Set.of("items","nextCursor"),Arrays.stream(AccountPage.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).collect(java.util.stream.Collectors.toSet()));noWrites();}
    @Test void pageDefaultsAreBoundedAndEmptyHasNoCursor(){when(tx.visibleAccountIds(A,R,null,26)).thenReturn(List.of());var page=app.listIdentityAccounts(new Query(REQ,null,null));assertTrue(page.items().isEmpty());assertNull(page.nextCursor());verify(tx).visibleAccountIds(A,R,null,26);}
    @Test void invalidPageLimitsFailWithoutListing(){for(int n:new int[]{0,-1,101})denied(VALIDATION_FAILED,()->app.listIdentityAccounts(new Query(REQ,null,n)));verify(tx,never()).visibleAccountIds(any(),any(),any(),anyInt());}
    @Test void invalidCursorCannotBypassAuthorization(){for(String c:List.of("","not-a-cursor","x".repeat(513)))denied(VALIDATION_FAILED,()->app.listIdentityAccounts(new Query(REQ,c,1)));noWrites();}
    @Test void cursorBindsActorRealmButNeverGrantsAuthority(){var b=java.nio.ByteBuffer.allocate(49).put((byte)1);for(UUID id:List.of(B,R,B))b.putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits());String c=Base64.getUrlEncoder().withoutPadding().encodeToString(b.array());denied(VALIDATION_FAILED,()->app.listIdentityAccounts(new Query(REQ,c,1)));noWrites();}
    @Test void disableUsesDomainTransitionAndAtomicRevocationPorts(){var result=app.disableIdentityAccount(command());assertEquals("DISABLED",result.account().status());assertEquals(2,result.appliedVersion());assertEquals(2,target.version().authVersion());verify(accounts).revokeSessions(B,NOW);verify(tx).invalidateProofs(B,NOW);verify(tx).audit(A,B,REQ,"disableIdentityAccount",command().reason(),NOW);}
    @Test void pendingDisableDoesNotActivateAccount(){pending();app.disableIdentityAccount(command());assertEquals(IdentityAccount.Status.DISABLED,target.status());assertEquals(IdentityAccount.Activation.UNVERIFIED,target.activation());}
    @Test void reactivateVerifiedDisabledAccountWithoutRestoringSessions(){target=account(B,IdentityAccount.Status.DISABLED,IdentityAccount.Activation.VERIFIED);assertEquals("ACTIVE",app.reactivateIdentityAccount(command()).account().status());verify(accounts).revokeSessions(B,NOW);verify(tx).invalidateProofs(B,NOW);}
    @Test void disabledPendingCannotReactivate(){target=account(B,IdentityAccount.Status.DISABLED,IdentityAccount.Activation.UNVERIFIED);denied(INVALID_ACCOUNT_STATE,()->app.reactivateIdentityAccount(command()));noWrites();}
    @Test void selfDisableRemainsForbidden(){denied(SELF_MANAGEMENT_FORBIDDEN,()->app.disableIdentityAccount(new Command(REQ,A,1,KEY,"Synthetic")));noWrites();}
    @Test void lastAdministratorDomainGuardIsRetained(){when(accounts.activeAdministrators()).thenReturn(1L);denied(LAST_ADMIN_PROTECTED,()->app.disableIdentityAccount(command()));noWrites();}
    @Test void staleVersionFailsBeforeMutation(){denied(PRECONDITION_STALE,()->app.disableIdentityAccount(new Command(REQ,B,2,KEY,"Synthetic")));noWrites();}
    @Test void exactReceiptReplayPrecedesStaleVersionWithoutRepeatWrites(){app.disableIdentityAccount(command());assertTrue(app.disableIdentityAccount(command()).replayed());verify(tx,times(1)).updateState(any(),any(),any());verify(tx,times(1)).audit(any(),any(),any(),any(),any(),any());}
    @Test void changedPayloadConflictsOnSameOperationKey(){app.disableIdentityAccount(command());denied(IDEMPOTENCY_CONFLICT,()->app.disableIdentityAccount(new Command(REQ,B,1,KEY,"Different reason")));verify(tx,times(1)).updateState(any(),any(),any());}
    @Test void auditFailurePropagatesForOwnedTransactionRollback(){doThrow(new IllegalStateException("SYNTHETIC_AUDIT_FAILURE")).when(tx).audit(any(),any(),any(),any(),any(),any());assertThrows(IllegalStateException.class,()->app.disableIdentityAccount(command()));verify(tx,never()).receipt(any(),any(),any(),any(),anyInt(),any());}
    @Test void missingIssuerStopsResendBeforeAllWrites(){pending();app=new IdentityAdministration(store,admission,authority,null,clock,Duration.ofHours(24));assertThrows(IllegalStateException.class,()->app.resendIdentityInvitation(command()));noWrites();}
    @Test void activeOrDisabledPendingCannotReceiveResend(){denied(INVALID_ACCOUNT_STATE,()->app.resendIdentityInvitation(command()));target=account(B,IdentityAccount.Status.DISABLED,IdentityAccount.Activation.UNVERIFIED);denied(INVALID_ACCOUNT_STATE,()->app.resendIdentityInvitation(command()));verifyNoInteractions(issuer);noWrites();}
    @Test void resendInvalidatesOldProofBeforeProtectedReplacement(){pending();var p=prepared();app.resendIdentityInvitation(command());var order=inOrder(tx,p);order.verify(tx).invalidateProofs(B,NOW);order.verify(tx).appendProof(any(),any(),eq("v1"));order.verify(p).appendDeliveryIntent();verify(p).close();assertEquals(2,target.version().version());assertEquals(IdentityAccount.Status.PENDING_ACTIVATION,target.status());}
    @Test void resendReplayDoesNotGenerateAnotherProof(){pending();prepared();app.resendIdentityInvitation(command());app.resendIdentityInvitation(command());verify(issuer,times(1)).prepare(any(),any(),any());verify(tx,times(1)).appendProof(any(),any(),any());}
    @Test void accessExpiryDuringCryptoPreparationAbortsBeforeWrites(){pending();prepared();when(issuer.prepare(any(),any(),any())).thenAnswer(i->{when(clock.instant()).thenReturn(NOW.plusSeconds(900));var p=mock(InvitationIssuer.Prepared.class);when(p.proof()).thenReturn(ActionProof.invitation(UUID.randomUUID(),target,NOW));when(p.tokenHash()).thenReturn(new byte[32]);when(p.verifierKeyId()).thenReturn("v1");return p;});denied(SESSION_INVALID,()->app.resendIdentityInvitation(command()));noWrites();}
    @Test void rejectedTrustedAdmissionCannotListOrMutate(){when(admission.admit(any(),any())).thenThrow(new IdentityCommandFailure(SESSION_INVALID));denied(SESSION_INVALID,()->app.listIdentityAccounts(new Query(REQ,null,1)));denied(SESSION_INVALID,()->app.disableIdentityAccount(command()));verifyNoInteractions(store);}
    @Test void publicBoundaryHasNoCredentialOrClientPrincipalFields(){assertEquals(Set.of("requestId","targetId","expectedVersion","operationKey","reason"),Arrays.stream(Command.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).collect(java.util.stream.Collectors.toSet()));assertEquals("IdentityAdministrationCommand[redacted]",command().toString());assertEquals("IdentityAccountPage[redacted]",new AccountPage(List.of(view(target)),null).toString());}
    @Test void expiryDuringEmptyQueryCannotReturnAnAuthorizedPage(){when(tx.visibleAccountIds(A,R,null,26)).thenAnswer(i->{when(clock.instant()).thenReturn(NOW.plusSeconds(900));return List.of();});denied(SESSION_INVALID,()->app.listIdentityAccounts(new Query(REQ,null,null)));noWrites();}
    @Test void listingAuthorityLookupFailureHasAcceptedSafeCode(){when(tx.visibleAccountIds(A,R,null,2)).thenReturn(List.of(B));doReturn(null).when(authority).resolve(any(),any(),any());denied(AUTHORITY_REQUIRED,()->app.listIdentityAccounts(new Query(REQ,null,1)));noWrites();}
}
