package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** V3-V5 runtime DML; reuses the existing Session-owned transaction guard unchanged. */
public final class JdbcIdentityAdministrationStore implements IdentityAdministrationStore {
    private final JdbcIdentitySessionStore owned;
    private final JdbcTemplate jdbc;
    public JdbcIdentityAdministrationStore(DataSource runtime){owned=new JdbcIdentitySessionStore(runtime);jdbc=new JdbcTemplate(runtime);}
    private static Timestamp stamp(Instant t){return Timestamp.from(t);}
    private static String operation(String value) {
        if(!Set.of("disableIdentityAccount","reactivateIdentityAccount","resendIdentityInvitation").contains(value))throw new IllegalArgumentException("ADMIN_OPERATION_INVALID");
        return value;
    }
    @Override public <T>T transaction(Function<Transaction,T> action) {
        return owned.transaction(session->action.apply(new Transaction(){
            public IdentityGrantStore.Transaction accounts(){return session.accounts();}
            public Optional<Realm> activeRealm(){
                return jdbc.query("SELECT id,code FROM identity.realm WHERE code='SYSTEM' AND status='ACTIVE' AND bootstrap_account_id IS NOT NULL AND activated_at IS NOT NULL",(r,n)->new Realm(r.getObject(1,UUID.class),r.getString(2))).stream().findFirst();
            }
            public List<UUID> visibleAccountIds(UUID actor,UUID realm,UUID after,int limit){
                // Full target predicate before cursor/limit; no global total or unowned contact projection.
                String sql="SELECT a.id FROM identity.user_account a WHERE a.id<>? AND EXISTS (SELECT 1 FROM identity.account_grant g WHERE g.account_id=a.id AND g.role_code='IDENTITY_MANAGEMENT' AND g.scope_type='IDENTITY' AND g.scope_id=?) AND NOT EXISTS (SELECT 1 FROM identity.account_grant g WHERE g.account_id=a.id AND (g.role_code<>'IDENTITY_MANAGEMENT' OR g.scope_type<>'IDENTITY' OR g.scope_id<>?))";
                if(after==null)return jdbc.query(sql+" ORDER BY a.id LIMIT ?",(r,n)->r.getObject(1,UUID.class),actor,realm,realm,limit);
                return jdbc.query(sql+" AND a.id>? ORDER BY a.id LIMIT ?",(r,n)->r.getObject(1,UUID.class),actor,realm,realm,after,limit);
            }
            public Optional<Receipt> receipt(UUID actor,String op,UUID key){
                return jdbc.query("SELECT target_id,original_version,result_version,encode(request_fingerprint,'hex'),expires_at FROM identity.command_receipt WHERE binding_kind='ACTOR' AND actor_id=? AND operation=? AND operation_key=?",(r,n)->new Receipt(r.getObject(1,UUID.class),r.getLong(2),r.getLong(3),r.getString(4),r.getTimestamp(5).toInstant()),actor,operation(op),key).stream().findFirst();
            }
            public void updateState(IdentityAccount before,IdentityAccount after,Instant now){
                int count=jdbc.update("UPDATE identity.user_account SET status=?,version=?,auth_version=?,disabled_at=?,updated_at=? WHERE id=? AND version=? AND auth_version=?",after.status().name(),after.version().version(),after.version().authVersion(),after.status()==IdentityAccount.Status.DISABLED?stamp(now):null,stamp(now),before.id(),before.version().version(),before.version().authVersion());
                if(count!=1)throw new IdentityCommandFailure(IdentityCommandFailure.Code.PRECONDITION_STALE);
            }
            public void invalidateProofs(UUID account,Instant now){
                jdbc.update("UPDATE identity.action_proof SET revoked_at=? WHERE account_id=? AND consumed_at IS NULL AND revoked_at IS NULL",stamp(now),account);
                jdbc.update("UPDATE identity.invitation_outbox SET status='EXPIRED',nonce=NULL,ciphertext=NULL,lease_id=NULL,lease_until=NULL,last_outcome='NOT_ELIGIBLE',finished_at=? WHERE account_id=? AND status IN ('PENDING','LEASED')",stamp(now),account);
            }
            public void appendProof(ActionProof proof,byte[] hash,String keyId){
                jdbc.update("INSERT INTO identity.action_proof(id,account_id,purpose,token_hash,created_at,expires_at,verifier_key_id) VALUES (?,?,'INVITATION',?,?,?,?)",proof.id(),proof.accountId(),hash,stamp(proof.createdAt()),stamp(proof.expiresAt()),keyId);
            }
            public void audit(UUID actor,UUID target,UUID request,String op,String reason,Instant now){
                String event=switch(operation(op)){case "disableIdentityAccount"->"IDENTITY_ACCOUNT_DISABLED";case "reactivateIdentityAccount"->"IDENTITY_ACCOUNT_REACTIVATED";default->"IDENTITY_INVITATION_RESENT";};
                jdbc.update("INSERT INTO identity.audit_entry(id,actor_id,target_id,event_type,source,reason,request_id,occurred_at) VALUES (?,?,?,?,'IDENTITY_APPLICATION',?,?,?)",UUID.randomUUID(),actor,target,event,reason,request.toString(),stamp(now));
            }
            public void receipt(UUID actor,String op,UUID key,Receipt receipt,int status,Instant now){
                jdbc.update("INSERT INTO identity.command_receipt(binding_kind,actor_id,operation,operation_key,request_fingerprint,target_id,original_version,result_version,result_status,created_at,expires_at) VALUES ('ACTOR',?,?,?,decode(?,'hex'),?,?,?,?,?,?)",actor,operation(op),key,receipt.fingerprint(),receipt.targetId(),receipt.originalVersion(),receipt.resultVersion(),status,stamp(now),stamp(receipt.expiresAt()));
            }
        }));
    }
}
