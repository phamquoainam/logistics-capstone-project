package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Reviewed recovery only: contracts/README.md:17,27,67 authorize both-family reset and recipient proof evidence.
 * No invitation, grant, business membership, username or security-contact mutation. */
public final class JdbcCustomerRecoveryStore implements CustomerRecoveryStore {
    private final JdbcCustomerEnrollmentStore store;private final JdbcTemplate jdbc;private final CustomerProofProtection protection;
    public JdbcCustomerRecoveryStore(DataSource source,CustomerProofProtection protection){store=new JdbcCustomerEnrollmentStore(Objects.requireNonNull(source));jdbc=new JdbcTemplate(source);this.protection=Objects.requireNonNull(protection);}
    private static Timestamp stamp(Instant time){return Timestamp.from(time);}
    private static void one(int count){if(count!=1)throw new IllegalStateException("CUSTOMER_RECOVERY_WRITE_REJECTED");}
    @Override public <T>T transaction(Function<Transaction,T> work){return store.transaction(tx->work.apply(new Transaction(){
        public IdentityGrantStore.Transaction accounts(){return tx.accounts();}
        public Optional<UUID> accountForEmail(String exact){return jdbc.query("SELECT c.account_id FROM identity.customer_identifier c JOIN identity.user_account a ON a.id=c.account_id WHERE c.exact_email=? AND c.status='ACTIVE' AND c.exact_email=a.email COLLATE \"C\" AND a.status='ACTIVE' AND a.email_verified",(r,n)->r.getObject(1,UUID.class),exact).stream().findFirst();}
        public Optional<String> currentEmail(UUID account){return jdbc.query("SELECT a.email FROM identity.user_account a JOIN identity.customer_identifier c ON c.account_id=a.id AND c.status='ACTIVE' AND c.exact_email=a.email COLLATE \"C\" WHERE a.id=? AND a.status='ACTIVE' AND a.email_verified",(r,n)->r.getString(1),account).stream().findFirst();}
        public void lockSessions(UUID account){
            var sessions=jdbc.query("SELECT id,FALSE AS customer FROM identity.auth_session WHERE account_id=? UNION ALL SELECT id,TRUE FROM identity.customer_auth_session WHERE account_id=? ORDER BY id,customer",(r,n)->Map.entry(r.getObject(1,UUID.class),r.getBoolean(2)),account,account);
            for(var session:sessions)jdbc.query("SELECT id FROM "+(session.getValue()?"identity.customer_auth_session":"identity.auth_session")+" WHERE id=? AND account_id=? FOR UPDATE",(r,n)->r.getObject(1,UUID.class),session.getKey(),account);
        }
        public Optional<UUID> proofSubject(UUID proof){return jdbc.query("SELECT subject_id FROM identity.customer_proof WHERE id=? AND purpose='CUSTOMER_RECOVERY'",(r,n)->r.getObject(1,UUID.class),proof).stream().findFirst();}
        public Optional<CustomerEnrollmentStore.StoredProof> proof(UUID proof){return tx.proof(proof);}
        public Optional<CustomerEnrollmentStore.Receipt> receipt(UUID binding,UUID key){return tx.receipt(binding,key);}
        public void receipt(UUID binding,UUID key,CustomerEnrollmentStore.Receipt receipt,Instant now){tx.receipt(binding,key,receipt,now);}
        public Optional<CustomerEnrollmentStore.Receipt> proofReceipt(UUID proof,UUID key){return tx.proofReceipt(proof,key);}
        public void proofReceipt(UUID proof,UUID key,CustomerEnrollmentStore.Receipt receipt,Instant now){tx.proofReceipt(proof,key,receipt,now);}
        public void revokePending(UUID account,Instant now){
            jdbc.update("UPDATE identity.customer_proof SET revoked_at=? WHERE account_id=? AND purpose='CUSTOMER_RECOVERY' AND consumed_at IS NULL AND revoked_at IS NULL",stamp(now),account);
            jdbc.update("UPDATE identity.customer_delivery d SET status='EXPIRED',lease_id=NULL,lease_until=NULL,nonce=NULL,ciphertext=NULL,recipient_nonce=NULL,recipient_ciphertext=NULL,finished_at=?,version=d.version+1 FROM identity.customer_proof p WHERE d.proof_id=p.id AND p.account_id=? AND p.purpose='CUSTOMER_RECOVERY' AND p.revoked_at IS NOT NULL AND d.status IN ('QUEUED','SENDING','RETRY_WAIT','BLOCKED','ACCEPTED_BY_PROVIDER')",stamp(now),account);
        }
        public void issue(CustomerProof p,String destination,CustomerProofProtection.Protected sealed,Instant now){
            if(p.purpose()!=CustomerProof.Purpose.CUSTOMER_RECOVERY || currentEmail(p.subjectId()).filter(destination::equals).isEmpty())throw new IllegalStateException("CUSTOMER_RECOVERY_DESTINATION_REJECTED");
            byte[] verifier=sealed.verifier(),nonce=sealed.nonce(),ciphertext=sealed.ciphertext(),recipientNonce=sealed.recipientNonce(),recipientCiphertext=sealed.recipientCiphertext();
            try{
                one(jdbc.update("INSERT INTO identity.customer_proof(id,subject_id,account_id,purpose,verifier,verifier_key_id,created_at,expires_at,bound_auth_version,policy_version) VALUES (?,?,?,?,?,?,?,?,?,?)",p.id(),p.subjectId(),p.subjectId(),p.purpose().name(),verifier,sealed.verifierKey(),stamp(p.createdAt()),stamp(p.expiresAt()),p.authVersion(),p.emailOtp()?"S003-OTP-2026-10-30":"S003-2026-10-27"));
                one(jdbc.update("INSERT INTO identity.customer_delivery(id,proof_id,subject_id,purpose,verifier_key_id,encryption_key_id,template_version,configuration_version,created_at,expires_at,status,due_at,nonce,ciphertext,recipient_nonce,recipient_ciphertext,version) VALUES (?,?,?,?,?,?,?,'S003-SMTP-1',?,?,'QUEUED',?,?,?,?,?,1)",UUID.randomUUID(),p.id(),p.subjectId(),p.purpose().name(),sealed.verifierKey(),sealed.encryptionKey(),p.emailOtp()?"S003-EMAIL-OTP-1":"S003-EMAIL-1",stamp(p.createdAt()),stamp(p.expiresAt()),stamp(now),nonce,ciphertext,recipientNonce,recipientCiphertext));
            }finally{Arrays.fill(verifier,(byte)0);Arrays.fill(nonce,(byte)0);Arrays.fill(ciphertext,(byte)0);Arrays.fill(recipientNonce,(byte)0);Arrays.fill(recipientCiphertext,(byte)0);}
        }
        public void reset(IdentityAccount before,IdentityAccount.CustomerProofChange change,String hash,byte[] supplied,Instant now){
            var after=change.account();var consumed=change.consumedProof();var stored=tx.proof(consumed.id()).orElseThrow(()->new IllegalStateException("CUSTOMER_RECOVERY_PROOF_REQUIRED"));var available=stored.proof();
            var expected=new IdentityAccount(before.id(),before.username(),before.status(),before.activation(),before.version().next(),before.grants());
            if(!after.equals(expected) || !available.subjectId().equals(before.id()) || available.purpose()!=CustomerProof.Purpose.CUSTOMER_RECOVERY
                || available.emailOtp() && (supplied==null || supplied.length!=43)
                || !Objects.equals(available.authVersion(),before.version().authVersion()) || !now.equals(consumed.consumedAt())
                || hash==null || !hash.matches("\\$2[aby]\\$12\\$[./A-Za-z0-9]{53}"))throw new IllegalStateException("CUSTOMER_RECOVERY_BINDING_REQUIRED");
            // Recheck actual token possession inside the locked persistence boundary, not a claimed delivery flag.
            byte[] verifier=stored.verifier();try{if(!protection.matches(available,stored.verifierKey(),supplied,verifier,now))throw new IllegalStateException("CUSTOMER_RECOVERY_POSSESSION_REQUIRED");}finally{Arrays.fill(verifier,(byte)0);}
            one(jdbc.update("UPDATE identity.user_account SET password_hash=?,version=?,auth_version=?,updated_at=? WHERE id=? AND version=? AND auth_version=? AND status='ACTIVE' AND email_verified",hash,after.version().version(),after.version().authVersion(),stamp(now),before.id(),before.version().version(),before.version().authVersion()));
            one(jdbc.update("UPDATE identity.customer_proof SET consumed_at=? WHERE id=? AND account_id=? AND purpose='CUSTOMER_RECOVERY' AND bound_auth_version=? AND consumed_at IS NULL AND revoked_at IS NULL AND expires_at>?",stamp(now),available.id(),before.id(),before.version().authVersion(),stamp(now)));
            // README:27 and ADR0009 require credential reset to invalidate both families of this same proven account.
            tx.accounts().revokeSessions(before.id(),now);
            jdbc.update("UPDATE identity.customer_auth_session SET revoked_at=? WHERE account_id=? AND revoked_at IS NULL",stamp(now),before.id());
            jdbc.update("UPDATE identity.customer_refresh_token SET revoked_at=? WHERE session_id IN (SELECT id FROM identity.customer_auth_session WHERE account_id=?) AND revoked_at IS NULL",stamp(now),before.id());
            // Only password-recovery/session-version proofs are affected; privileged invitations remain intact.
            jdbc.update("UPDATE identity.action_proof SET revoked_at=? WHERE account_id=? AND purpose='RECOVERY' AND consumed_at IS NULL AND revoked_at IS NULL",stamp(now),before.id());
            jdbc.update("UPDATE identity.customer_proof SET revoked_at=? WHERE account_id=? AND (purpose='CUSTOMER_RECOVERY' OR bound_auth_version IS NOT NULL) AND consumed_at IS NULL AND revoked_at IS NULL",stamp(now),before.id());
            jdbc.update("UPDATE identity.customer_delivery d SET status='EXPIRED',lease_id=NULL,lease_until=NULL,nonce=NULL,ciphertext=NULL,recipient_nonce=NULL,recipient_ciphertext=NULL,finished_at=?,version=d.version+1 FROM identity.customer_proof p WHERE d.proof_id=p.id AND p.account_id=? AND p.revoked_at IS NOT NULL AND d.status IN ('QUEUED','SENDING','RETRY_WAIT','BLOCKED','ACCEPTED_BY_PROVIDER')",stamp(now),before.id());
            byte[] evidence;try{evidence=MessageDigest.getInstance("SHA-256").digest(("RECIPIENT_PROOF_CONSUMED:"+available.id()).getBytes(StandardCharsets.US_ASCII));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException("CUSTOMER_DELIVERY_EVIDENCE_UNAVAILABLE");}
            // README:67 explicitly permits verified recipient proof consumption as delivery evidence, not SMTP250.
            one(jdbc.update("UPDATE identity.customer_delivery d SET status='DELIVERED',lease_id=NULL,lease_until=NULL,nonce=NULL,ciphertext=NULL,recipient_nonce=NULL,recipient_ciphertext=NULL,delivered_at=?,delivery_evidence=?,finished_at=?,version=d.version+1 FROM identity.customer_proof p WHERE d.proof_id=p.id AND p.id=? AND p.account_id=? AND p.purpose='CUSTOMER_RECOVERY' AND p.consumed_at=? AND d.status<>'DELIVERED'",stamp(now),evidence,stamp(now),available.id(),before.id(),stamp(now)));
        }
        public void audit(UUID account,UUID request,String event,Instant now){
            if(!Set.of("CUSTOMER_RECOVERY_QUEUED","CUSTOMER_RECOVERY_CONFIRMED").contains(event))throw new IllegalArgumentException("CUSTOMER_RECOVERY_EVENT_INVALID");
            one(jdbc.update("INSERT INTO identity.audit_entry(id,actor_id,target_id,event_type,source,reason,request_id,occurred_at) VALUES (?,NULL,?,?,'IDENTITY_CUSTOMER','Customer recovery transition',?,?)",UUID.randomUUID(),account,event,request.toString(),stamp(now)));
        }
    }));}
}
