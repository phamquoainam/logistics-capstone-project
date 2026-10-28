package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Enrollment and version-bound recovery delivery; other purposes require their own current-authority checks. */
public final class JdbcCustomerDeliveryStore implements CustomerDeliveryStore {
    private final JdbcIdentityGrantStore accounts;private final JdbcTemplate jdbc;
    public JdbcCustomerDeliveryStore(DataSource source){accounts=new JdbcIdentityGrantStore(Objects.requireNonNull(source));jdbc=new JdbcTemplate(source);}
    private static Timestamp stamp(Instant time){return time==null?null:Timestamp.from(time);}
    private static Instant instant(ResultSet r,String column)throws SQLException{var value=r.getTimestamp(column);return value==null?null:value.toInstant();}
    private <T>T owned(Function<IdentityGrantStore.Transaction,T> work){
        if(TransactionSynchronizationManager.isActualTransactionActive() || TransactionSynchronizationManager.isSynchronizationActive()
            || !TransactionSynchronizationManager.getResourceMap().isEmpty())throw new IllegalStateException("CUSTOMER_DELIVERY_TRANSACTION_OWNERSHIP_REQUIRED");
        return accounts.transaction(tx->{tx.lockAdministration();return work.apply(tx);});
    }
    private static void one(int count){if(count!=1)throw new IllegalStateException("CUSTOMER_DELIVERY_WRITE_REJECTED");}
    private CustomerDelivery delivery(UUID id){return jdbc.queryForObject("SELECT * FROM identity.customer_delivery WHERE id=? FOR UPDATE",(r,n)->new CustomerDelivery(
        r.getObject("id",UUID.class),r.getObject("proof_id",UUID.class),CustomerDelivery.Status.valueOf(r.getString("status")),r.getInt("attempts"),instant(r,"created_at"),instant(r,"expires_at"),instant(r,"due_at"),r.getObject("lease_id",UUID.class),instant(r,"lease_until"),instant(r,"finished_at"),r.getLong("version")),id);}
    private CustomerProof proof(UUID id){return jdbc.queryForObject("SELECT * FROM identity.customer_proof WHERE id=? FOR UPDATE",(r,n)->new CustomerProof(
        r.getObject("id",UUID.class),r.getObject("subject_id",UUID.class),CustomerProof.Purpose.valueOf(r.getString("purpose")),instant(r,"created_at"),instant(r,"expires_at"),
        r.getObject("bound_session_id",UUID.class),r.getObject("bound_auth_version",Long.class),r.getObject("organization_id",UUID.class),instant(r,"consumed_at"),instant(r,"revoked_at")),id);}
    private void audit(UUID subject,UUID request,String event,Instant now){
        one(jdbc.update("INSERT INTO identity.audit_entry(id,actor_id,target_id,event_type,source,reason,request_id,occurred_at) VALUES (?,NULL,NULL,?,'IDENTITY_CUSTOMER',?,?,?)",
            UUID.randomUUID(),event,"Customer delivery subject "+subject,request.toString(),stamp(now)));
    }
    private void update(CustomerDelivery before,CustomerDelivery after,CustomerDeliveryTransport.Outcome outcome,Instant now){
        boolean clear=Set.of(CustomerDelivery.Status.ACCEPTED_BY_PROVIDER,CustomerDelivery.Status.FAILED,CustomerDelivery.Status.EXPIRED).contains(after.status());
        boolean accepted=after.status()==CustomerDelivery.Status.ACCEPTED_BY_PROVIDER;
        one(jdbc.update("UPDATE identity.customer_delivery SET status=?,attempts=?,due_at=?,lease_id=?,lease_until=?,finished_at=?,version=?,"
            +"nonce=CASE WHEN ? THEN NULL ELSE nonce END,ciphertext=CASE WHEN ? THEN NULL ELSE ciphertext END,recipient_nonce=CASE WHEN ? THEN NULL ELSE recipient_nonce END,recipient_ciphertext=CASE WHEN ? THEN NULL ELSE recipient_ciphertext END,"
            +"provider_reference=COALESCE(?,provider_reference),accepted_at=CASE WHEN ? THEN ? ELSE accepted_at END WHERE id=? AND version=?",
            after.status().name(),after.attempts(),stamp(after.dueAt()),after.leaseId(),stamp(after.leaseUntil()),stamp(after.finishedAt()),after.version(),clear,clear,clear,clear,
            accepted?outcome.reference():null,accepted,stamp(now),before.id(),before.version()));
    }
    private int expireEnrollments(Instant now){return owned(tx->{
        var expired=jdbc.query("SELECT id,intended_account_id FROM identity.customer_enrollment WHERE status='PENDING' AND expires_at<=? ORDER BY id LIMIT 25 FOR UPDATE",
            (r,n)->Map.entry(r.getObject(1,UUID.class),r.getObject(2,UUID.class)),stamp(now));
        for(var e:expired){
            one(jdbc.update("UPDATE identity.customer_enrollment SET status='EXPIRED',protected_password=NULL,version=version+1 WHERE id=? AND status='PENDING'",e.getKey()));
            jdbc.update("UPDATE identity.customer_proof SET revoked_at=? WHERE enrollment_id=? AND consumed_at IS NULL AND revoked_at IS NULL",stamp(now),e.getKey());
            jdbc.update("UPDATE identity.customer_delivery SET status='EXPIRED',lease_id=NULL,lease_until=NULL,nonce=NULL,ciphertext=NULL,recipient_nonce=NULL,recipient_ciphertext=NULL,finished_at=?,version=version+1 WHERE proof_id IN (SELECT id FROM identity.customer_proof WHERE enrollment_id=?) AND status<>'DELIVERED' AND status<>'EXPIRED'",stamp(now),e.getKey());
            audit(e.getValue(),UUID.randomUUID(),"CUSTOMER_ENROLLMENT_EXPIRED",now);
        }return expired.size();
    });}
    @Override public int expire(Instant now){
        int enrollments=expireEnrollments(now);
        return enrollments+owned(tx->{
            var candidates=jdbc.query("SELECT id,account_id FROM identity.customer_proof WHERE purpose='CUSTOMER_RECOVERY' AND consumed_at IS NULL AND revoked_at IS NULL AND expires_at<=? ORDER BY id LIMIT 25",(r,n)->Map.entry(r.getObject(1,UUID.class),r.getObject(2,UUID.class)),stamp(now));
            var subjects=new TreeSet<UUID>();candidates.forEach(p->subjects.add(p.getValue()));subjects.forEach(tx::lockAccount);
            for(var candidate:candidates){
                one(jdbc.update("UPDATE identity.customer_proof SET revoked_at=? WHERE id=? AND purpose='CUSTOMER_RECOVERY' AND consumed_at IS NULL AND revoked_at IS NULL AND expires_at<=?",stamp(now),candidate.getKey(),stamp(now)));
                jdbc.update("UPDATE identity.customer_delivery SET status='EXPIRED',lease_id=NULL,lease_until=NULL,nonce=NULL,ciphertext=NULL,recipient_nonce=NULL,recipient_ciphertext=NULL,finished_at=?,version=version+1 WHERE proof_id=? AND status NOT IN ('DELIVERED','EXPIRED')",stamp(now),candidate.getKey());
                audit(candidate.getValue(),UUID.randomUUID(),"CUSTOMER_RECOVERY_EXPIRED",now);
            }return candidates.size();
        });
    }
    private Optional<String> eligible(CustomerProof proof,Instant now){
        try{proof.requireAvailable(now);}catch(DomainViolation invalid){return Optional.empty();}
        if(proof.purpose()==CustomerProof.Purpose.CUSTOMER_RECOVERY){
            return jdbc.query("SELECT a.email FROM identity.user_account a JOIN identity.customer_identifier c ON c.account_id=a.id AND c.status='ACTIVE' AND c.exact_email=a.email COLLATE \"C\" WHERE a.id=? AND a.status='ACTIVE' AND a.email_verified AND a.auth_version=?",(r,n)->r.getString(1),proof.subjectId(),proof.authVersion()).stream().findFirst();
        }
        if(proof.purpose()!=CustomerProof.Purpose.CUSTOMER_ENROLLMENT)return Optional.empty();
        return jdbc.query("SELECT e.exact_email FROM identity.customer_enrollment e JOIN identity.customer_proof p ON p.enrollment_id=e.id AND p.subject_id=e.intended_account_id WHERE p.id=? AND e.status='PENDING' AND e.expires_at>?"
            +" AND NOT EXISTS(SELECT 1 FROM identity.user_account a WHERE a.email COLLATE \"C\"=e.exact_email) AND NOT EXISTS(SELECT 1 FROM identity.customer_identifier c WHERE c.exact_email=e.exact_email AND c.status='ACTIVE')",
            (r,n)->r.getString(1),proof.id(),stamp(now)).stream().findFirst();
    }
    @Override public Optional<Lease> claim(Instant now){return owned(tx->{
        var candidate=jdbc.query("SELECT d.id,d.proof_id,p.subject_id FROM identity.customer_delivery d JOIN identity.customer_proof p ON p.id=d.proof_id WHERE p.purpose IN ('CUSTOMER_ENROLLMENT','CUSTOMER_RECOVERY') AND d.status IN ('QUEUED','SENDING','RETRY_WAIT','BLOCKED') AND d.due_at<=? AND (d.status<>'SENDING' OR d.lease_until<=?) ORDER BY d.due_at,d.id LIMIT 1",
            (r,n)->List.of(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getObject(3,UUID.class)),stamp(now),stamp(now)).stream().findFirst();
        if(candidate.isEmpty())return Optional.empty();var ids=candidate.get();tx.lockAccount(ids.get(2));var proof=proof(ids.get(1));var before=delivery(ids.getFirst());
        var destination=eligible(proof,now);
        if(destination.isEmpty() || !now.isBefore(before.expiresAt())){
            update(before,before.invalidate(now),null,now);audit(proof.subjectId(),UUID.randomUUID(),"CUSTOMER_DELIVERY_EXPIRED",now);return Optional.empty();
        }
        if(before.attempts()==5){update(before,before.exhaust(now),null,now);audit(proof.subjectId(),UUID.randomUUID(),"CUSTOMER_DELIVERY_FAILED",now);return Optional.empty();}
        var after=before.claim(UUID.randomUUID(),now);update(before,after,null,now);audit(proof.subjectId(),after.leaseId(),"CUSTOMER_DELIVERY_LEASED",now);
        return Optional.of(jdbc.queryForObject("SELECT verifier_key_id,encryption_key_id,nonce,ciphertext,recipient_nonce,recipient_ciphertext FROM identity.customer_delivery WHERE id=?",(r,n)->{
            byte[] nonce=r.getBytes(3),ciphertext=r.getBytes(4),recipientNonce=r.getBytes(5),recipientCiphertext=r.getBytes(6);
            try{return new Lease(after,proof,r.getString(1),r.getString(2),destination.get(),nonce,ciphertext,recipientNonce,recipientCiphertext);}
            finally{Arrays.fill(nonce,(byte)0);Arrays.fill(ciphertext,(byte)0);Arrays.fill(recipientNonce,(byte)0);Arrays.fill(recipientCiphertext,(byte)0);}
        },after.id()));
    });}
    @Override public boolean finish(Lease lease,CustomerDeliveryTransport.Outcome outcome,Instant now){return owned(tx->{
        tx.lockAccount(lease.proof().subjectId());var currentProof=proof(lease.proof().id());var before=delivery(lease.delivery().id());
        if(Set.of(CustomerDelivery.Status.DELIVERED,CustomerDelivery.Status.ACCEPTED_BY_PROVIDER,CustomerDelivery.Status.FAILED,CustomerDelivery.Status.EXPIRED).contains(before.status()))return false;
        if(!Objects.equals(before.leaseId(),lease.delivery().leaseId()) || before.version()!=lease.delivery().version())return false;
        if(eligible(currentProof,now).isEmpty() || !now.isBefore(before.expiresAt())){
            update(before,before.invalidate(now),null,now);audit(currentProof.subjectId(),before.leaseId(),"CUSTOMER_DELIVERY_EXPIRED",now);return false;
        }
        if(!before.owns(lease.delivery().leaseId(),now))return false;
        var after=before.finish(lease.delivery().leaseId(),CustomerDelivery.Outcome.valueOf(outcome.kind().name()),now);update(before,after,outcome,now);
        String event=after.status()==CustomerDelivery.Status.ACCEPTED_BY_PROVIDER?(outcome.sandbox()?"CUSTOMER_DELIVERY_ACCEPTED_TEST":"CUSTOMER_DELIVERY_ACCEPTED_SMTP"):"CUSTOMER_DELIVERY_"+after.status().name();
        audit(currentProof.subjectId(),before.leaseId(),event,now);return true;
    });}
}
