package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.internal.application.ProvisionInput;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** V3/V4 runtime DML. Existing JdbcIdentityGrantStore owns admission, lock and transaction. */
public final class JdbcIdentityProvisionStore implements IdentityProvisionStore {
    private final JdbcIdentityGrantStore transactions;
    private final JdbcTemplate jdbc;
    public JdbcIdentityProvisionStore(DataSource dataSource) {
        transactions=new JdbcIdentityGrantStore(dataSource);jdbc=new JdbcTemplate(dataSource);
    }
    @Override public <T> T transaction(Function<Transaction,T> work) {
        return transactions.transaction(admission->work.apply(new Transaction() {
            public IdentityGrantStore.Transaction admission() {return admission;}
            public Optional<Receipt> receipt(UUID actor,UUID key) {
                return jdbc.query("SELECT target_id,result_version,encode(request_fingerprint,'hex') FROM identity.command_receipt WHERE binding_kind='ACTOR' AND actor_id=? AND operation='provisionIdentityAccount' AND operation_key=?",
                    (r,n)->new Receipt(r.getObject(1,UUID.class),r.getLong(2),r.getString(3)),actor,key).stream().findFirst();
            }
            public boolean usernameExists(Username username) {
                return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM identity.user_account WHERE username=?)",Boolean.class,username.value()));
            }
            public void insertPending(IdentityAccount account,ProvisionInput input,Instant now) {
                if(account.status()!=IdentityAccount.Status.PENDING_ACTIVATION || account.activation()!=IdentityAccount.Activation.UNVERIFIED
                        || account.version().version()!=1 || account.version().authVersion()!=1 || !account.username().equals(input.username())
                        || !account.grants().equals(input.grants())) throw new IllegalArgumentException("Pending account required");
                int inserted=jdbc.update("INSERT INTO identity.user_account(id,username,display_name,email,status,email_verified,created_at,updated_at) VALUES (?,?,?,?,'PENDING_ACTIVATION',FALSE,?,?) ON CONFLICT (username) DO NOTHING",
                    account.id(),input.username().value(),input.displayName(),input.email(),Timestamp.from(now),Timestamp.from(now));
                if(inserted!=1) throw new IdentityCommandFailure(IdentityCommandFailure.Code.USERNAME_CONFLICT);
                for(var grant:account.grants()) jdbc.update("INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at) VALUES (?,?,?,?,?)",
                    account.id(),grant.role().value(),grant.scope().type().name(),grant.scope().id(),Timestamp.from(now));
            }
            public void appendProof(ActionProof p,byte[] hash,String verifierKeyId) {
                jdbc.update("INSERT INTO identity.action_proof(id,account_id,purpose,token_hash,created_at,expires_at,verifier_key_id) VALUES (?,?,'INVITATION',?,?,?,?)",
                    p.id(),p.accountId(),hash,Timestamp.from(p.createdAt()),Timestamp.from(p.expiresAt()),verifierKeyId);
            }
            public void appendAudit(UUID actor,UUID target,UUID request,Instant now) {
                jdbc.update("INSERT INTO identity.audit_entry(id,actor_id,target_id,event_type,source,reason,request_id,occurred_at) VALUES (?,?,?,'IDENTITY_ACCOUNT_PROVISIONED','IDENTITY_APPLICATION','Controlled invitation provisioning',?,?)",
                    UUID.randomUUID(),actor,target,request.toString(),Timestamp.from(now));
            }
            public void appendReceipt(UUID actor,UUID key,Receipt r,Instant now,Instant expires) {
                jdbc.update("INSERT INTO identity.command_receipt(binding_kind,actor_id,operation,operation_key,request_fingerprint,target_id,result_version,result_status,created_at,expires_at) VALUES ('ACTOR',?,'provisionIdentityAccount',?,decode(?,'hex'),?,?,201,?,?)",
                    actor,key,r.fingerprint(),r.targetId(),r.resultVersion(),Timestamp.from(now),Timestamp.from(expires));
            }
        }));
    }
}
