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
import static com.elogistic.backend.identity.api.IdentityCommandFailure.Code.*;

/** V3-V5 runtime privileges only. No Realm UPDATE/bootstrap, DDL, deletes or network I/O. */
public final class JdbcIdentityInvitationStore implements IdentityInvitationStore {
    private final JdbcIdentityGrantStore transactions;
    private final JdbcTemplate jdbc;
    public JdbcIdentityInvitationStore(DataSource runtime){transactions=new JdbcIdentityGrantStore(runtime);jdbc=new JdbcTemplate(runtime);}
    private static Timestamp stamp(Instant value){return Timestamp.from(value);}
    @Override public <T>T transaction(Function<Transaction,T> work) {
        return transactions.transaction(admission->work.apply(new Transaction() {
            public IdentityGrantStore.Transaction admission(){return admission;}
            public Optional<StoredProof> lockProof(UUID id) {
                return jdbc.query("SELECT * FROM identity.action_proof WHERE id=? FOR UPDATE",(r,n)->{
                    var revoked=r.getTimestamp("revoked_at");var consumed=r.getTimestamp("consumed_at");
                    var state=revoked!=null ? ActionProof.State.REVOKED : consumed!=null ? ActionProof.State.CONSUMED : ActionProof.State.AVAILABLE;
                    var proof=new ActionProof(r.getObject("id",UUID.class),r.getObject("account_id",UUID.class),ActionProof.Purpose.valueOf(r.getString("purpose")),r.getTimestamp("created_at").toInstant(),r.getTimestamp("expires_at").toInstant(),state,revoked!=null ? revoked.toInstant() : consumed!=null ? consumed.toInstant() : null);
                    return new StoredProof(proof,r.getString("verifier_key_id"),r.getBytes("token_hash"));
                },id).stream().findFirst();
            }
            public boolean eligibleRealm(UUID id,UUID proofId) {
                // Only the currently approved exact same-Realm management pair. An INACTIVE
                // bootstrap Realm is deliberately NOT activated/bypassed by this application slice.
                return Boolean.TRUE.equals(jdbc.queryForObject("SELECT count(*)=1 AND COALESCE(bool_and(g.role_code='IDENTITY_MANAGEMENT' AND g.scope_type='IDENTITY' AND r.status='ACTIVE' AND o.realm_id=r.id AND o.destination=a.email),FALSE) FROM identity.account_grant g JOIN identity.user_account a ON a.id=g.account_id LEFT JOIN identity.realm r ON r.id=g.identity_realm_id JOIN identity.invitation_outbox o ON o.account_id=a.id AND o.proof_id=? WHERE g.account_id=?",Boolean.class,proofId,id));
            }
            public Optional<Receipt> receipt(UUID proof,UUID key) {
                return jdbc.query("SELECT target_id,result_version,request_fingerprint,expires_at FROM identity.command_receipt WHERE binding_kind='PROOF' AND proof_id=? AND operation='acceptIdentityInvitation' AND operation_key=?",
                    (r,n)->new Receipt(r.getObject(1,UUID.class),r.getLong(2),r.getBytes(3),r.getTimestamp(4).toInstant()),proof,key).stream().findFirst();
            }
            public void activate(IdentityAccount before,IdentityAccount.ProofChange change,String verifier,Instant now) {
                var after=change.account();var proof=change.consumedProof();
                if(!after.id().equals(before.id()) || !proof.accountId().equals(before.id()) || !after.grants().equals(before.grants())
                    || before.status()!=IdentityAccount.Status.PENDING_ACTIVATION || after.status()!=IdentityAccount.Status.ACTIVE
                    || after.activation()!=IdentityAccount.Activation.VERIFIED || proof.state()!=ActionProof.State.CONSUMED
                    || !after.version().equals(before.version().next()) || !now.equals(proof.endedAt()))throw new IllegalStateException("INVITATION_CHANGE_INVALID");
                int count=jdbc.update("UPDATE identity.user_account SET password_hash=?,status='ACTIVE',email_verified=TRUE,activated_at=?,updated_at=?,version=?,auth_version=? WHERE id=? AND version=? AND auth_version=? AND status='PENDING_ACTIVATION' AND NOT email_verified AND password_hash IS NULL",
                    verifier,stamp(now),stamp(now),after.version().version(),after.version().authVersion(),before.id(),before.version().version(),before.version().authVersion());
                if(count!=1)throw new IdentityCommandFailure(INVALID_ACCOUNT_STATE);
                count=jdbc.update("UPDATE identity.action_proof SET consumed_at=? WHERE id=? AND account_id=? AND purpose='INVITATION' AND consumed_at IS NULL AND revoked_at IS NULL AND created_at<=? AND expires_at>?",
                    stamp(now),proof.id(),before.id(),stamp(now),stamp(now));
                if(count!=1)throw new IdentityCommandFailure(TOKEN_INVALID_OR_EXPIRED);
            }
            public void clearDelivery(UUID proof,Instant now) {
                jdbc.update("UPDATE identity.invitation_outbox SET status='EXPIRED',last_outcome='NOT_ELIGIBLE',nonce=NULL,ciphertext=NULL,lease_id=NULL,lease_until=NULL,finished_at=? WHERE proof_id=? AND status IN ('PENDING','LEASED')",stamp(now),proof);
            }
            public void appendAudit(UUID target,UUID request,Instant now) {
                jdbc.update("INSERT INTO identity.audit_entry(id,actor_id,target_id,event_type,source,reason,request_id,occurred_at) VALUES (?,NULL,?,'IDENTITY_INVITATION_ACCEPTED','IDENTITY_APPLICATION','Verified invitation activation',?,?)",UUID.randomUUID(),target,request.toString(),stamp(now));
            }
            public void appendReceipt(UUID proof,UUID key,Receipt r,long original,Instant now) {
                byte[] fingerprint=r.fingerprint();
                try {jdbc.update("INSERT INTO identity.command_receipt(binding_kind,proof_id,operation,operation_key,request_fingerprint,target_id,original_version,result_version,result_status,created_at,expires_at) VALUES ('PROOF',?,'acceptIdentityInvitation',?,?,?,?,?,204,?,?)",
                    proof,key,fingerprint,r.targetId(),original,r.version(),stamp(now),stamp(r.expiresAt()));}
                finally {Arrays.fill(fingerprint,(byte)0);}
            }
        }));
    }
}
