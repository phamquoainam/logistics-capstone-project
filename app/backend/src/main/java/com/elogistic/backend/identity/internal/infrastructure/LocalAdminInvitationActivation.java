package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.application.AcceptIdentityInvitation;
import com.elogistic.backend.identity.internal.application.port.IdentityInvitationStore;
import com.elogistic.backend.identity.internal.application.port.IdentityGrantStore;
import com.elogistic.backend.identity.api.IdentityInvitationCommands;
import com.elogistic.backend.identity.internal.domain.IdentityAccount;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Reuses the existing application/proof/password/receipt engine; only persistence is specialized. */
public final class LocalAdminInvitationActivation implements com.elogistic.backend.identity.internal.application.port.LocalAdminInvitationActivation {
    private final AcceptIdentityInvitation commands;
    public LocalAdminInvitationActivation(DataSource runtime,InvitationProtection protection,Clock clock,String environment){
        this(runtime,protection,clock,environment,false);
    }
    public LocalAdminInvitationActivation(DataSource runtime,InvitationProtection protection,Clock clock,String environment,boolean bootstrapEnabled){
        if(!Set.of("LOCAL","TEST").contains(environment))throw new IllegalArgumentException("ADMIN_ENVIRONMENT_REQUIRED");
        commands=new AcceptIdentityInvitation(new Store(runtime,environment,bootstrapEnabled),new ProtectedInvitationCredentials(protection),clock,Duration.ofHours(24));
    }
    public void accept(UUID proofId,byte[] token,String password,UUID operationKey,UUID requestId){
        if(TransactionSynchronizationManager.isActualTransactionActive()||TransactionSynchronizationManager.isSynchronizationActive()||!TransactionSynchronizationManager.getResourceMap().isEmpty())throw new IllegalStateException("ADMIN_ACTIVATION_TRANSACTION_REQUIRED");
        try(var command=new IdentityInvitationCommands.Command(proofId,token,password,operationKey,requestId)){
            commands.acceptIdentityInvitation(command);
        }
    }
    private static final class Store implements IdentityInvitationStore {
        private final JdbcIdentityInvitationStore existing;private final JdbcTemplate jdbc;private final String environment;private final boolean bootstrapEnabled;
        Store(DataSource runtime,String environment,boolean bootstrapEnabled){existing=new JdbcIdentityInvitationStore(runtime);jdbc=new JdbcTemplate(runtime);this.environment=environment;this.bootstrapEnabled=bootstrapEnabled;}
        @Override public <T>T transaction(Function<Transaction,T> work){
            return existing.transaction(delegate->work.apply(new Transaction(){
                private String verifier;private UUID request;private boolean bootstrap;
                @Override public IdentityGrantStore.Transaction admission(){return delegate.admission();}
                @Override public Optional<StoredProof> lockProof(UUID proof){
                    // Resolve/lock account first under population lock, then the proof.
                    var accounts=jdbc.query("SELECT account_id FROM identity.action_proof WHERE id=?",(r,n)->r.getObject(1,UUID.class),proof);
                    if(accounts.size()==1){
                        delegate.admission().lockAccount(accounts.getFirst());
                        bootstrap=bootstrapEnabled&&!Boolean.TRUE.equals(jdbc.queryForObject("SELECT identity.local_admin_registered(?)",Boolean.class,accounts.getFirst()));
                    }
                    return delegate.lockProof(proof);
                }
                @Override public boolean eligibleRealm(UUID account,UUID proof){
                    if(bootstrap)return Boolean.TRUE.equals(jdbc.queryForObject("""
                        SELECT identity.local_admin_environment_matches(?) AND EXISTS(
                         SELECT 1 FROM identity.realm r JOIN identity.invitation_outbox o ON o.realm_id=r.id
                         JOIN identity.user_account a ON a.id=o.account_id WHERE r.code='SYSTEM' AND r.status='INACTIVE'
                         AND r.activated_at IS NULL AND r.bootstrap_account_id=a.id AND a.id=? AND o.proof_id=?
                         AND o.purpose='INVITATION' AND o.format_version=1 AND o.destination COLLATE "C"=a.email
                         AND EXISTS(SELECT 1 FROM identity.account_grant g WHERE g.account_id=a.id AND g.role_code='IDENTITY_MANAGEMENT' AND g.scope_type='IDENTITY' AND g.scope_id=r.id)
                         AND NOT EXISTS(SELECT 1 FROM identity.account_grant g WHERE g.account_id=a.id AND(g.role_code<>'IDENTITY_MANAGEMENT' OR g.scope_type<>'IDENTITY' OR g.scope_id<>r.id)))
                        """,Boolean.class,environment,account,proof));
                    return Boolean.TRUE.equals(jdbc.queryForObject("""
                        SELECT identity.local_admin_environment_matches(?) AND identity.local_admin_registered(?)
                         AND EXISTS(SELECT 1 FROM identity.invitation_outbox o JOIN identity.realm r ON r.id=o.realm_id
                          JOIN identity.user_account a ON a.id=o.account_id WHERE o.proof_id=? AND a.id=?
                          AND r.status='ACTIVE' AND r.code='SYSTEM' AND r.bootstrap_account_id<>a.id
                          AND o.purpose='INVITATION' AND o.format_version=1 AND o.destination COLLATE "C"=a.email
                          AND (a.status='PENDING_ACTIVATION' OR identity.local_admin_eligible(a.id,r.id))
                          AND EXISTS(SELECT 1 FROM identity.account_grant g WHERE g.account_id=a.id AND g.scope_id=r.id AND g.scope_type='IDENTITY' AND g.role_code='ADMIN_DASHBOARD_READ')
                          AND NOT EXISTS(SELECT 1 FROM identity.account_grant g WHERE g.account_id=a.id AND
                           (g.scope_type<>'IDENTITY' OR g.scope_id<>r.id OR g.role_code NOT IN
                            ('ADMIN_DASHBOARD_READ','ADMIN_MERCHANT_READ','ADMIN_NETWORK_READ','ADMIN_SHIPMENT_READ','ADMIN_FINANCE_READ','ADMIN_TECHNICAL_READ'))))
                        """,Boolean.class,environment,account,proof,account));
                }
                @Override public Optional<Receipt> receipt(UUID proof,UUID key){return delegate.receipt(proof,key);}
                @Override public void activate(IdentityAccount before,IdentityAccount.ProofChange change,String hash,Instant now){
                    if(verifier!=null||!change.account().id().equals(before.id())||!change.account().version().equals(before.version().next()))throw new IllegalStateException("ADMIN_ACTIVATION_CHANGE_INVALID");
                    verifier=hash;
                }
                @Override public void clearDelivery(UUID proof,Instant now){/* SQL09 owns this atomic write. */}
                @Override public void appendAudit(UUID target,UUID requestId,Instant now){request=requestId;}
                @Override public void appendReceipt(UUID proof,UUID key,Receipt receipt,long original,Instant now){
                    if(verifier==null||request==null)throw new IllegalStateException("ADMIN_ACTIVATION_CHANGE_INVALID");
                    byte[] digest=receipt.fingerprint();
                    try{jdbc.query(bootstrap?"SELECT identity.accept_local_realm_bootstrap_invitation(?,?,?,?,?)":"SELECT identity.accept_local_admin_invitation(?,?,?,?,?)",r->{},proof,key,digest,verifier,request);}
                    finally{Arrays.fill(digest,(byte)0);verifier=null;}
                }
            }));
        }
    }
}
