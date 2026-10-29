package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** V8-only enrollment writes plus existing valid V3 account creation. No public grant writes. */
public final class JdbcCustomerEnrollmentStore implements CustomerEnrollmentStore {
    private final JdbcIdentityGrantStore accounts;
    private final JdbcTemplate jdbc;
    public JdbcCustomerEnrollmentStore(DataSource source){accounts=new JdbcIdentityGrantStore(Objects.requireNonNull(source));jdbc=new JdbcTemplate(source);}
    private static Timestamp stamp(Instant time){return Timestamp.from(time);}
    private static Instant instant(ResultSet r,String name)throws SQLException{var t=r.getTimestamp(name);return t==null?null:t.toInstant();}
    private static void one(int result){if(result!=1)throw new IllegalStateException("CUSTOMER_ENROLLMENT_WRITE_REJECTED");}
    private static CustomerEnrollment enrollment(ResultSet r)throws SQLException{
        byte[] material=r.getBytes("protected_password");
        try{return new CustomerEnrollment(r.getObject("id",UUID.class),r.getObject("intended_account_id",UUID.class),new CustomerEmail(r.getString("exact_email")),
            r.getString("display_name"),r.getString("unverified_phone"),material==null?null:new String(material,StandardCharsets.US_ASCII),r.getString("consent_version"),
            instant(r,"created_at"),instant(r,"expires_at"),CustomerEnrollment.Status.valueOf(r.getString("status")),r.getLong("version"));}
        finally{if(material!=null)Arrays.fill(material,(byte)0);}
    }
    @Override public <T>T transaction(Function<Transaction,T> work){
        if(TransactionSynchronizationManager.isActualTransactionActive() || TransactionSynchronizationManager.isSynchronizationActive()
            || !TransactionSynchronizationManager.getResourceMap().isEmpty())throw new IllegalStateException("CUSTOMER_ENROLLMENT_TRANSACTION_OWNERSHIP_REQUIRED");
        return accounts.transaction(a->work.apply(new Transaction(){
            public IdentityGrantStore.Transaction accounts(){return a;}
            public Optional<Receipt> receipt(UUID binding,UUID key){
                return jdbc.query("SELECT operation,fingerprint_key_id,request_fingerprint,request_id FROM identity.customer_command_receipt WHERE preauth_binding_id=? AND operation_key=?",
                    (r,n)->new Receipt(r.getString(1),r.getString(2),r.getBytes(3),r.getObject(4,UUID.class)),binding,key).stream().findFirst();
            }
            public void receipt(UUID binding,UUID key,Receipt receipt,Instant now){
                byte[] fingerprint=receipt.fingerprint();
                try{one(jdbc.update("INSERT INTO identity.customer_command_receipt VALUES (?,?,?,?,?,?,?,?)",binding,key,receipt.operation(),receipt.keyId(),fingerprint,receipt.requestId(),stamp(now),stamp(now.plusSeconds(604800))));}
                finally{Arrays.fill(fingerprint,(byte)0);}
            }
            public Optional<Receipt> proofReceipt(UUID proof,UUID key){
                return jdbc.query("SELECT operation,fingerprint_key_id,request_fingerprint,request_id FROM identity.customer_proof_receipt WHERE proof_id=? AND operation_key=?",
                    (r,n)->new Receipt(r.getString(1),r.getString(2),r.getBytes(3),r.getObject(4,UUID.class)),proof,key).stream().findFirst();
            }
            public void proofReceipt(UUID proof,UUID key,Receipt receipt,Instant now){
                byte[] fingerprint=receipt.fingerprint();
                String purpose=switch(receipt.operation()){case "verifyCustomerEmail"->"CUSTOMER_ENROLLMENT";case "confirmCustomerRecovery"->"CUSTOMER_RECOVERY";default->throw new IllegalArgumentException("CUSTOMER_PROOF_RECEIPT_OPERATION_INVALID");};
                try{one(jdbc.update("INSERT INTO identity.customer_proof_receipt VALUES (?,?,?,?,?,?,?,?,?)",proof,purpose,key,receipt.operation(),receipt.keyId(),fingerprint,receipt.requestId(),stamp(now),stamp(now.plusSeconds(604800))));}
                finally{Arrays.fill(fingerprint,(byte)0);}
            }
            public boolean emailOccupied(String exact){
                return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM identity.customer_identifier WHERE exact_email=? AND status='ACTIVE') OR EXISTS(SELECT 1 FROM identity.user_account WHERE email COLLATE \"C\"=?)",Boolean.class,exact,exact));
            }
            public Optional<CustomerEnrollment> pending(String exact){return jdbc.query("SELECT * FROM identity.customer_enrollment WHERE exact_email=? AND status='PENDING' FOR UPDATE",(r,n)->JdbcCustomerEnrollmentStore.enrollment(r),exact).stream().findFirst();}
            public Optional<CustomerEnrollment> enrollment(UUID id){return jdbc.query("SELECT * FROM identity.customer_enrollment WHERE id=? FOR UPDATE",(r,n)->JdbcCustomerEnrollmentStore.enrollment(r),id).stream().findFirst();}
            public Optional<StoredProof> proof(UUID id){
                return jdbc.query("SELECT * FROM identity.customer_proof WHERE id=? FOR UPDATE",(r,n)->{
                    Long version=r.getObject("bound_auth_version",Long.class);
                    var proof=new CustomerProof(r.getObject("id",UUID.class),r.getObject("subject_id",UUID.class),CustomerProof.Purpose.valueOf(r.getString("purpose")),
                        instant(r,"created_at"),instant(r,"expires_at"),r.getObject("bound_session_id",UUID.class),version,r.getObject("organization_id",UUID.class),instant(r,"consumed_at"),instant(r,"revoked_at"));
                    byte[] verifier=r.getBytes("verifier");
                    return new StoredProof(proof,r.getObject("enrollment_id",UUID.class),r.getString("verifier_key_id"),verifier==null?new byte[0]:verifier);
                },id).stream().findFirst();
            }
            public void insert(CustomerEnrollment e){
                byte[] material=e.protectedPassword().getBytes(StandardCharsets.US_ASCII);
                try{one(jdbc.update("INSERT INTO identity.customer_enrollment(id,intended_account_id,exact_email,display_name,protected_password,consent_version,created_at,expires_at,status,version,unverified_phone) VALUES (?,?,?,?,?,?,?,?,?,?,?)",e.id(),e.intendedAccountId(),e.email().value(),e.displayName(),material,e.consentVersion(),stamp(e.createdAt()),stamp(e.expiresAt()),e.status().name(),e.version(),e.unverifiedPhone()));}
                finally{Arrays.fill(material,(byte)0);}
            }
            public void update(CustomerEnrollment before,CustomerEnrollment after){
                byte[] material=after.protectedPassword()==null?null:after.protectedPassword().getBytes(StandardCharsets.US_ASCII);
                try{one(jdbc.update("UPDATE identity.customer_enrollment SET protected_password=?,expires_at=?,status=?,version=? WHERE id=? AND version=? AND status='PENDING'",material,stamp(after.expiresAt()),after.status().name(),after.version(),before.id(),before.version()));}
                finally{if(material!=null)Arrays.fill(material,(byte)0);}
            }
            public void revokePending(UUID enrollment,Instant now){
                jdbc.update("UPDATE identity.customer_proof SET revoked_at=? WHERE enrollment_id=? AND consumed_at IS NULL AND revoked_at IS NULL",stamp(now),enrollment);
                jdbc.update("UPDATE identity.customer_delivery SET status='EXPIRED',lease_id=NULL,lease_until=NULL,nonce=NULL,ciphertext=NULL,recipient_nonce=NULL,recipient_ciphertext=NULL,finished_at=?,version=version+1 WHERE proof_id IN (SELECT id FROM identity.customer_proof WHERE enrollment_id=?) AND status IN ('QUEUED','SENDING','RETRY_WAIT','BLOCKED')",stamp(now),enrollment);
            }
            public void issue(CustomerEnrollment e,CustomerProof p,CustomerProofProtection.Protected sealed,Instant now){
                byte[] verifier=sealed.verifier(),nonce=sealed.nonce(),ciphertext=sealed.ciphertext(),recipientNonce=sealed.recipientNonce(),recipientCiphertext=sealed.recipientCiphertext();
                try{
                    one(jdbc.update("INSERT INTO identity.customer_proof(id,subject_id,enrollment_id,purpose,verifier,verifier_key_id,created_at,expires_at,policy_version) VALUES (?,?,?,?,?,?,?,?, 'S003-2026-10-27')",p.id(),p.subjectId(),e.id(),p.purpose().name(),verifier,sealed.verifierKey(),stamp(p.createdAt()),stamp(p.expiresAt())));
                    one(jdbc.update("INSERT INTO identity.customer_delivery(id,proof_id,subject_id,purpose,verifier_key_id,encryption_key_id,template_version,configuration_version,created_at,expires_at,status,due_at,nonce,ciphertext,recipient_nonce,recipient_ciphertext,version) VALUES (?,?,?,?,?,?,'S003-EMAIL-1','S003-SMTP-1',?,?,'QUEUED',?,?,?,?,?,1)",UUID.randomUUID(),p.id(),p.subjectId(),p.purpose().name(),sealed.verifierKey(),sealed.encryptionKey(),stamp(p.createdAt()),stamp(p.expiresAt()),stamp(now),nonce,ciphertext,recipientNonce,recipientCiphertext));
                }finally{Arrays.fill(verifier,(byte)0);Arrays.fill(nonce,(byte)0);Arrays.fill(ciphertext,(byte)0);Arrays.fill(recipientNonce,(byte)0);Arrays.fill(recipientCiphertext,(byte)0);}
            }
            public void activate(CustomerEnrollment e,CustomerProof p,Instant now){
                one(jdbc.update("INSERT INTO identity.user_account(id,username,display_name,email,password_hash,status,email_verified,version,auth_version,created_at,updated_at,activated_at) VALUES (?,?,?,?,?,'ACTIVE',TRUE,1,1,?,?,?)",e.intendedAccountId(),"customer."+e.intendedAccountId(),e.displayName(),e.email().value(),e.protectedPassword(),stamp(now),stamp(now),stamp(now)));
                one(jdbc.update("INSERT INTO identity.customer_identifier VALUES (?,?,?,?,'ACTIVE',1)",UUID.randomUUID(),e.intendedAccountId(),e.email().value(),stamp(now)));
                if(e.unverifiedPhone()!=null)one(jdbc.update("INSERT INTO identity.customer_phone_contact(account_id,canonical_phone,verification_status,created_at,updated_at) VALUES (?,?,'UNVERIFIED',?,?)",e.intendedAccountId(),e.unverifiedPhone(),stamp(now),stamp(now)));
                one(jdbc.update("UPDATE identity.customer_proof SET consumed_at=? WHERE id=? AND consumed_at IS NULL AND revoked_at IS NULL AND expires_at>?",stamp(now),p.id(),stamp(now)));
                byte[] evidence;
                try{evidence=MessageDigest.getInstance("SHA-256").digest(("RECIPIENT_PROOF_CONSUMED:"+p.id()).getBytes(StandardCharsets.US_ASCII));}
                catch(NoSuchAlgorithmException impossible){throw new IllegalStateException("CUSTOMER_DELIVERY_EVIDENCE_UNAVAILABLE");}
                // Actual verified possession, not SMTP acceptance, is the recipient evidence.
                one(jdbc.update("UPDATE identity.customer_delivery SET status='DELIVERED',lease_id=NULL,lease_until=NULL,nonce=NULL,ciphertext=NULL,recipient_nonce=NULL,recipient_ciphertext=NULL,delivered_at=?,delivery_evidence=?,finished_at=?,version=version+1 WHERE proof_id=? AND status<>'DELIVERED'",stamp(now),evidence,stamp(now),p.id()));
            }
            public void audit(UUID subject,UUID request,String event,Instant now){
                if(!Set.of("CUSTOMER_ENROLLMENT_QUEUED","CUSTOMER_ENROLLMENT_EXPIRED","CUSTOMER_VERIFICATION_RESENT","CUSTOMER_EMAIL_VERIFIED").contains(event))throw new IllegalArgumentException("CUSTOMER_ENROLLMENT_EVENT_INVALID");
                UUID target=event.equals("CUSTOMER_EMAIL_VERIFIED")?subject:null;
                one(jdbc.update("INSERT INTO identity.audit_entry(id,actor_id,target_id,event_type,source,reason,request_id,occurred_at) VALUES (?,NULL,?,?,'IDENTITY_CUSTOMER',?,?,?)",UUID.randomUUID(),target,event,"Customer enrollment subject "+subject,request.toString(),stamp(now)));
            }
        }));
    }
}
