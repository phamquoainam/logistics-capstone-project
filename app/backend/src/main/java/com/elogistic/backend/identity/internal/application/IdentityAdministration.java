package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.api.IdentityAdministrationCommands.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission.Context;
import com.elogistic.backend.identity.internal.domain.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import static com.elogistic.backend.identity.api.IdentityCommandFailure.Code.*;

/** Seven accepted Application operations; no HTTP, sender, Recovery or broader grant policy. */
public final class IdentityAdministration implements IdentityAdministrationCommands {
    private static final String DISABLE="disableIdentityAccount", REACTIVATE="reactivateIdentityAccount", RESEND="resendIdentityInvitation";
    private final IdentityAdministrationStore store;
    private final CurrentSessionAdmission admission;
    private final GrantAuthority authorities;
    private final InvitationIssuer issuer;
    private final Clock clock;
    private final Duration retention;
    public IdentityAdministration(IdentityAdministrationStore store, CurrentSessionAdmission admission,
            GrantAuthority authority, InvitationIssuer issuer, Clock clock, Duration retention) {
        this.store=Objects.requireNonNull(store);this.admission=Objects.requireNonNull(admission);
        this.authorities=new FailClosedGrantAuthority(authority);this.issuer=issuer;this.clock=Objects.requireNonNull(clock);
        if(retention==null || retention.compareTo(Duration.ofHours(24))<0)throw new IllegalArgumentException("ADMIN_RECEIPT_RETENTION_INVALID");
        this.retention=retention;
    }
    private record Admitted(IdentityAdministrationStore.Transaction tx, IdentityAccount actor,
                            IdentitySession session, Context context, IdentityAdministrationStore.Realm realm) {}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private static IdentityCommandFailure fail(IdentityCommandFailure.Code c){return new IdentityCommandFailure(c);}
    private void facts(Context c,Instant time) {
        if(c==null || c.sessionId()==null || c.accountId()==null || c.authVersion()<1 || c.issuedAt()==null || c.expiresAt()==null
                || !c.expiresAt().isAfter(c.issuedAt()) || Duration.between(c.issuedAt(),c.expiresAt()).compareTo(IdentitySession.ACCESS)>0
                || time.isBefore(c.issuedAt()) || !time.isBefore(c.expiresAt()))throw fail(SESSION_INVALID);
    }
    private void current(Admitted a,Instant time) {
        facts(a.context(),time);
        var c=a.context();var s=a.session();var actor=a.actor();
        if(!s.id().equals(c.sessionId()) || !s.accountId().equals(actor.id()) || c.authVersion()!=s.authVersion()
                || c.authVersion()!=actor.version().authVersion() || c.issuedAt().isBefore(s.createdAt())
                || c.expiresAt().isAfter(s.idleExpiresAt()) || c.expiresAt().isAfter(s.absoluteExpiresAt())
                || actor.activation()!=IdentityAccount.Activation.VERIFIED)throw fail(SESSION_INVALID);
        try{s.admit(actor,time);}catch(DomainViolation e){throw fail(SESSION_INVALID);}
    }
    private <T>T execute(UUID request,Function<Admitted,T> work) {
        if(request==null)throw fail(SESSION_INVALID);
        var context=admission.admit(CurrentSessionAdmission.Operation.CURRENT_SESSION,request);facts(context,now());
        return store.transaction(tx->{
            var accounts=tx.accounts();accounts.lockAdministration();
            UUID id=accounts.sessionAccount(context.sessionId()).orElseThrow(()->fail(SESSION_INVALID));
            if(!id.equals(context.accountId()))throw fail(SESSION_INVALID);
            var actor=accounts.lockAccount(id).orElseThrow(()->fail(SESSION_INVALID));
            var session=accounts.lockSession(context.sessionId()).orElseThrow(()->fail(SESSION_INVALID));
            var a=new Admitted(tx,actor,session,context,null);current(a,now());
            var realm=tx.activeRealm().orElseThrow(()->fail(AUTHORITY_REQUIRED));
            var pair=new Grant(RoleCode.IDENTITY_MANAGEMENT,new ResourceScope(ResourceScope.Type.IDENTITY,realm.id()));
            if(!actor.grants().equals(Set.of(pair)))throw fail(AUTHORITY_REQUIRED);
            var admitted=new Admitted(tx,actor,session,context,realm);
            T result=work.apply(admitted);current(admitted,now());return result;
        });
    }
    private <T>T object(UUID request,Function<Admitted,T> action) {
        try{return execute(request,action);}catch(IdentityCommandFailure e){if(e.code()==AUTHORITY_REQUIRED)throw fail(ACCOUNT_NOT_ACCESSIBLE);throw e;}
    }
    private IdentityAccount target(Admitted a,UUID id,boolean selfDisable) {
        if(id==null)throw fail(ACCOUNT_NOT_ACCESSIBLE);
        var target=a.tx().accounts().lockAccount(id).orElseThrow(()->fail(ACCOUNT_NOT_ACCESSIBLE));
        var authority=authorities.resolve(a.actor(),target,target.grants());
        if(authority==null)throw fail(ACCOUNT_NOT_ACCESSIBLE);
        try{authority.requireManagement(a.actor(),target,Set.of());}
        catch(DomainViolation e){throw fail(e.reason()==DomainViolation.Reason.SELF_MANAGEMENT && selfDisable?SELF_MANAGEMENT_FORBIDDEN:ACCOUNT_NOT_ACCESSIBLE);}
        current(a,now());return target;
    }
    @Override public IdentityGrantCommands.Account getIdentityAccount(UUID id,UUID request) {
        return object(request,a->{var target=target(a,id,false);return a.tx().accounts().accountView(target.id());});
    }
    @Override public RoleCatalog listIdentityRoles(UUID request) {
        return execute(request,a->new RoleCatalog(List.of(new Role("IDENTITY_MANAGEMENT","Registered Identity Realm account management"))));
    }
    @Override public ScopeCatalog listIdentityScopes(UUID request) {
        return execute(request,a->new ScopeCatalog(List.of(new Scope(a.realm().id(),"IDENTITY",a.realm().displayName()))));
    }
    @Override public AccountPage listIdentityAccounts(Query query) {
        if(query==null)throw fail(VALIDATION_FAILED);
        return execute(query.requestId(),a->{
            int limit=query.limit()==null?25:query.limit();if(limit<1 || limit>100)throw fail(VALIDATION_FAILED);
            UUID after=cursor(query.cursor(),a.actor().id(),a.realm().id());
            // SQL applies the complete exact-Realm target predicate BEFORE keyset LIMIT.
            var ids=a.tx().visibleAccountIds(a.actor().id(),a.realm().id(),after,limit+1);
            if(ids.size()>limit+1)throw fail(AUTHORITY_REQUIRED);
            var items=new ArrayList<IdentityGrantCommands.Account>();
            for(UUID id:ids.subList(0,Math.min(limit,ids.size()))) {
                try {var target=target(a,id,false);items.add(a.tx().accounts().accountView(target.id()));}
                catch(IdentityCommandFailure denied){if(denied.code()==ACCOUNT_NOT_ACCESSIBLE)throw fail(AUTHORITY_REQUIRED);throw denied;}
            }
            String next=ids.size()>limit?cursor(a.actor().id(),a.realm().id(),items.getLast().id()):null;
            return new AccountPage(items,next);
        });
    }
    private UUID cursor(String raw,UUID actor,UUID realm) {
        if(raw==null)return null;
        try {
            if(raw.isEmpty() || raw.length()>512)throw new IllegalArgumentException();
            byte[] bytes=Base64.getUrlDecoder().decode(raw);
            if(bytes.length!=49 || !Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(raw))throw new IllegalArgumentException();
            var b=ByteBuffer.wrap(bytes);if(b.get()!=1 || !new UUID(b.getLong(),b.getLong()).equals(actor)
                    || !new UUID(b.getLong(),b.getLong()).equals(realm))throw new IllegalArgumentException();
            return new UUID(b.getLong(),b.getLong());
        }catch(IllegalArgumentException invalid){throw fail(VALIDATION_FAILED);}
    }
    private String cursor(UUID actor,UUID realm,UUID last) {
        var b=ByteBuffer.allocate(49);b.put((byte)1);
        for(UUID id:List.of(actor,realm,last))b.putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b.array());
    }
    @Override public IdentityGrantCommands.Result disableIdentityAccount(Command c){return mutate(c,DISABLE);}
    @Override public IdentityGrantCommands.Result reactivateIdentityAccount(Command c){return mutate(c,REACTIVATE);}
    @Override public void resendIdentityInvitation(Command c){mutate(c,RESEND);}
    private IdentityGrantCommands.Result mutate(Command c,String operation) {
        if(c==null)throw fail(VALIDATION_FAILED);
        return object(c.requestId(),a->{
            var tx=a.tx();var accounts=tx.accounts();var target=target(a,c.targetId(),operation.equals(DISABLE));
            if(c.expectedVersion()<1 || c.operationKey()==null || c.reason()==null || c.reason().codePointCount(0,c.reason().length())<1
                    || c.reason().codePointCount(0,c.reason().length())>300 || !StandardCharsets.UTF_8.newEncoder().canEncode(c.reason()))throw fail(VALIDATION_FAILED);
            String fingerprint=fingerprint(c,operation);Instant time=now();current(a,time);
            var previous=tx.receipt(a.actor().id(),operation,c.operationKey());
            if(previous.isPresent()) {
                var receipt=previous.get();
                if(!receipt.targetId().equals(target.id()) || receipt.originalVersion()!=c.expectedVersion() || !receipt.fingerprint().equals(fingerprint))throw fail(IDEMPOTENCY_CONFLICT);
                if(!time.isBefore(receipt.expiresAt()))throw fail(IDEMPOTENCY_CONFLICT);
                return operation.equals(RESEND)?null:new IdentityGrantCommands.Result(accounts.accountView(target.id()),receipt.resultVersion(),true);
            }
            var authority=authorities.resolve(a.actor(),target,target.grants());
            if(authority==null)throw fail(ACCOUNT_NOT_ACCESSIBLE);
            try{authority.requireManagement(a.actor(),target,Set.of());}catch(DomainViolation e){throw fail(ACCOUNT_NOT_ACCESSIBLE);}
            IdentityAccount changed;
            try {
                if(operation.equals(DISABLE))changed=target.disable(a.actor(),authority,c.expectedVersion(),accounts.activeAdministrators());
                else if(operation.equals(REACTIVATE))changed=target.reactivate(a.actor(),authority,c.expectedVersion());
                else {
                    target.version().requireExpected(c.expectedVersion());
                    if(target.status()!=IdentityAccount.Status.PENDING_ACTIVATION || target.activation()!=IdentityAccount.Activation.UNVERIFIED)throw fail(INVALID_ACCOUNT_STATE);
                    changed=new IdentityAccount(target.id(),target.username(),target.status(),target.activation(),target.version().next(),target.grants());
                }
            }catch(DomainViolation e){throw fail(switch(e.reason()){
                case SELF_MANAGEMENT -> SELF_MANAGEMENT_FORBIDDEN;
                case LAST_ACTIVE_ADMIN -> LAST_ADMIN_PROTECTED;
                case VERSION_CONFLICT -> PRECONDITION_STALE;
                case AUTHORITY_REQUIRED -> ACCOUNT_NOT_ACCESSIBLE;
                default -> INVALID_ACCOUNT_STATE;
            });}
            if(operation.equals(RESEND)) {
                if(issuer==null)throw new IllegalStateException("INVITATION_ISSUER_REQUIRED");
                String destination=accounts.accountView(target.id()).email();
                try(var prepared=issuer.prepare(target,destination,time)) {
                    var proof=prepared.proof();byte[] hash=prepared.tokenHash();
                    try {
                        if(proof==null || !proof.accountId().equals(target.id()) || proof.purpose()!=ActionProof.Purpose.INVITATION
                                || proof.state()!=ActionProof.State.AVAILABLE || !proof.createdAt().equals(time) || !proof.expiresAt().equals(time.plus(Duration.ofHours(24)))
                                || hash==null || hash.length!=32 || prepared.verifierKeyId()==null)throw new IllegalStateException("INVITATION_PREPARATION_INVALID");
                        time=now();current(a,time);
                        tx.updateState(target,changed,time);tx.invalidateProofs(target.id(),time);accounts.revokeSessions(target.id(),time);
                        tx.appendProof(proof,hash,prepared.verifierKeyId());prepared.appendDeliveryIntent();
                    }finally{if(hash!=null)Arrays.fill(hash,(byte)0);}
                }
            } else {
                tx.updateState(target,changed,time);accounts.revokeSessions(target.id(),time);tx.invalidateProofs(target.id(),time);
            }
            tx.audit(a.actor().id(),target.id(),c.requestId(),operation,c.reason(),time);
            tx.receipt(a.actor().id(),operation,c.operationKey(),new IdentityAdministrationStore.Receipt(target.id(),c.expectedVersion(),changed.version().version(),fingerprint,time.plus(retention)),operation.equals(RESEND)?202:200,time);
            return operation.equals(RESEND)?null:new IdentityGrantCommands.Result(accounts.accountView(target.id()),changed.version().version(),false);
        });
    }
    private String fingerprint(Command c,String operation) {
        try {
            var buffer=new ByteArrayOutputStream();try(var d=new DataOutputStream(buffer)){
                for(String s:List.of(operation,c.targetId().toString(),Long.toString(c.expectedVersion()),c.reason())) {
                    byte[] b=s.getBytes(StandardCharsets.UTF_8);d.writeInt(b.length);d.write(b);
                }
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(buffer.toByteArray()));
        }catch(IOException|NoSuchAlgorithmException impossible){throw new IllegalStateException("ADMIN_FINGERPRINT_UNAVAILABLE");}
    }
}
