package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Joins genuine Customer admission. Contact possession never becomes an Identity credential. */
public final class JdbcCustomerBusinessContacts implements CustomerBusinessContacts {
    private final DataSource source;private final JdbcTemplate jdbc;private final CustomerAdmission admission;
    private final CustomerProofProtection protection;private final IdentityRateLimiter limiter;private final Supplier<String> peer;
    private final Clock clock;private final boolean enabled;private final String environment;
    public JdbcCustomerBusinessContacts(DataSource source,CustomerAdmission admission,CustomerProofProtection protection,IdentityRateLimiter limiter,Supplier<String> peer,Clock clock,boolean enabled,String environment){
        this.source=Objects.requireNonNull(source);jdbc=new JdbcTemplate(source);this.admission=Objects.requireNonNull(admission);this.protection=Objects.requireNonNull(protection);
        this.limiter=Objects.requireNonNull(limiter);this.peer=Objects.requireNonNull(peer);this.clock=Objects.requireNonNull(clock);this.enabled=enabled;this.environment=environment;
    }
    /** Opt-in startup check only; disabled legacy compositions need no V41 catalog. */
    public static void requireSchema(DataSource source){
        var jdbc=new JdbcTemplate(source);
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT identity.local_admin_environment_matches('LOCAL') OR identity.local_admin_environment_matches('TEST')",Boolean.class))
            ||!Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regclass('identity.business_contact_attestation') IS NOT NULL AND to_regclass('identity.governance_proof_receipt') IS NOT NULL AND to_regclass('identity.governance_contact_proof_request') IS NOT NULL AND EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='identity.governance_proof_receipt'::regclass AND conname='ck_governance_pending_address')",Boolean.class)))throw new IllegalStateException("BUSINESS_CONTACT_V41_SCHEMA_REQUIRED");
    }
    private static IdentityCommandFailure rejected(){return new IdentityCommandFailure(IdentityCommandFailure.Code.TOKEN_INVALID_OR_EXPIRED);}
    private static void id(UUID id){if(id==null||id.equals(new UUID(0,0)))throw new IdentityCommandFailure(IdentityCommandFailure.Code.VALIDATION_FAILED);}
    private static Timestamp stamp(Instant t){return Timestamp.from(t);}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private void current(CustomerAdmission.Context actor){
        if(!enabled||actor==null||!Set.of("LOCAL","TEST").contains(Objects.toString(environment,""))||!TransactionSynchronizationManager.hasResource(source))throw rejected();
        admission.requireCurrent(actor);
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT identity.local_admin_environment_matches(?)",Boolean.class,environment)))throw rejected();
    }
    private byte[] binding(String key,CustomerAdmission.Context c,String operation,UUID operationKey,UUID application,String email,UUID proof,byte[] token){
        return protection.fingerprint(key,operation,c.actor().accountId().toString(),c.sessionId().toString(),Long.toString(c.actor().securityVersion()),operationKey.toString(),application.toString(),email,proof==null?null:proof.toString(),token==null?null:new String(token,StandardCharsets.US_ASCII));
    }
    private Map<String,Object> prior(CustomerAdmission.Context actor,String operation,UUID key){
        return jdbc.queryForList("SELECT * FROM identity.governance_proof_receipt WHERE actor_id=? AND operation=? AND operation_key=?",actor.actor().accountId(),operation,key).stream().findFirst().orElse(null);
    }
    private void match(Map<String,Object> row,CustomerAdmission.Context actor,byte[] digest){
        if(!actor.sessionId().equals(row.get("customer_session_id"))||actor.actor().securityVersion()!=((Number)row.get("auth_version")).longValue()||!MessageDigest.isEqual(digest,(byte[])row.get("input_digest")))throw new IdentityCommandFailure(IdentityCommandFailure.Code.IDEMPOTENCY_CONFLICT);
    }
    @Override public ProofAccepted request(CustomerAdmission.Context actor,UUID application,String email,UUID key){
        current(actor);id(application);id(key);String exact;
        try{exact=new CustomerEmail(email).value();}catch(RuntimeException invalid){throw new IdentityCommandFailure(IdentityCommandFailure.Code.VALIDATION_FAILED);}
        var previous=prior(actor,"REQUEST_BUSINESS_CONTACT",key);
        String keyId=previous==null?protection.activeVerifierKey():(String)previous.get("input_key_id");
        byte[] digest=binding(keyId,actor,"requestBusinessContact",key,application,exact,null,null);
        try{
            if(previous!=null){match(previous,actor,digest);current(actor);return new ProofAccepted((UUID)previous.get("customer_proof_id"),true);}
            try{limiter.reserve(List.of(new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.RESEND_ACCOUNT,"BUSINESS_CONTACT:"+actor.actor().accountId()),new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.RESEND_IP,"BUSINESS_CONTACT:"+peer.get())));}
            catch(IdentityRateLimiter.Exhausted failure){throw new CustomerBusinessContacts.Limited(failure.retryAfter);}
            Instant time=now();UUID proofId=UUID.randomUUID();var proof=CustomerProof.issue(proofId,actor.actor().accountId(),CustomerProof.Purpose.BUSINESS_CONTACT,time,actor.sessionId(),actor.actor().securityVersion(),null);
            admission.requireDeadline(actor,proof.expiresAt());
            jdbc.update("UPDATE identity.customer_delivery SET status='EXPIRED',lease_id=NULL,lease_until=NULL,nonce=NULL,ciphertext=NULL,recipient_nonce=NULL,recipient_ciphertext=NULL,finished_at=?,version=version+1 WHERE proof_id IN (SELECT id FROM identity.customer_proof WHERE subject_id=? AND purpose='BUSINESS_CONTACT' AND consumed_at IS NULL AND revoked_at IS NULL) AND status NOT IN ('DELIVERED','EXPIRED')",stamp(time),actor.actor().accountId());
            jdbc.update("UPDATE identity.customer_proof SET revoked_at=? WHERE subject_id=? AND purpose='BUSINESS_CONTACT' AND consumed_at IS NULL AND revoked_at IS NULL",stamp(time),actor.actor().accountId());
            try(var sealed=protection.issue(proof,exact)){
                byte[] verifier=sealed.verifier(),nonce=sealed.nonce(),cipher=sealed.ciphertext(),recipientNonce=sealed.recipientNonce(),recipientCipher=sealed.recipientCiphertext(),envelope=GovernanceRecipientEnvelope.seal(sealed);
                try{
                    jdbc.update("INSERT INTO identity.customer_proof(id,subject_id,account_id,purpose,verifier,verifier_key_id,created_at,expires_at,bound_session_id,bound_auth_version,policy_version) VALUES (?,?,?,'BUSINESS_CONTACT',?,?,?,?,?,?,'S003-2026-10-27')",proofId,proof.subjectId(),proof.subjectId(),verifier,sealed.verifierKey(),stamp(time),stamp(proof.expiresAt()),actor.sessionId(),actor.actor().securityVersion());
                    jdbc.update("INSERT INTO identity.customer_delivery(id,proof_id,subject_id,purpose,verifier_key_id,encryption_key_id,template_version,configuration_version,created_at,expires_at,status,due_at,nonce,ciphertext,recipient_nonce,recipient_ciphertext,version) VALUES (?,?,?,'BUSINESS_CONTACT',?,?,'S003-EMAIL-1','S003-SMTP-1',?,?,'QUEUED',?,?,?,?,?,1)",UUID.randomUUID(),proofId,proof.subjectId(),sealed.verifierKey(),sealed.encryptionKey(),stamp(time),stamp(proof.expiresAt()),stamp(time),nonce,cipher,recipientNonce,recipientCipher);
                    receipt(actor,"REQUEST_BUSINESS_CONTACT",key,application,keyId,digest,proofId,null,envelope,time);
                    audit(proofId,actor,"BUSINESS_CONTACT_REQUESTED",time);
                }finally{for(byte[] bytes:List.of(verifier,nonce,cipher,recipientNonce,recipientCipher,envelope))Arrays.fill(bytes,(byte)0);}
            }
            current(actor);return new ProofAccepted(proofId,true);
        }finally{Arrays.fill(digest,(byte)0);}
    }
    private CustomerProof proof(Map<String,Object> p){
        return new CustomerProof((UUID)p.get("id"),(UUID)p.get("subject_id"),CustomerProof.Purpose.valueOf((String)p.get("purpose")),((Timestamp)p.get("created_at")).toInstant(),((Timestamp)p.get("expires_at")).toInstant(),(UUID)p.get("bound_session_id"),((Number)p.get("bound_auth_version")).longValue(),null,p.get("consumed_at")==null?null:((Timestamp)p.get("consumed_at")).toInstant(),p.get("revoked_at")==null?null:((Timestamp)p.get("revoked_at")).toInstant());
    }
    @Override public BusinessContactAttestation confirm(CustomerAdmission.Context actor,UUID application,UUID proofId,byte[] token,UUID key){
        current(actor);id(application);id(proofId);id(key);if(token==null||token.length!=43)throw rejected();
        var previous=prior(actor,"CONFIRM_BUSINESS_CONTACT",key);String keyId=previous==null?protection.activeVerifierKey():(String)previous.get("input_key_id");
        byte[] digest=binding(keyId,actor,"confirmBusinessContact",key,application,null,proofId,token);
        try{
            if(previous!=null){match(previous,actor,digest);return attestation((UUID)previous.get("attestation_id"));}
            var requests=jdbc.queryForList("SELECT protected_address FROM identity.governance_proof_receipt WHERE actor_id=? AND operation='REQUEST_BUSINESS_CONTACT' AND target_id=? AND customer_proof_id=? AND customer_session_id=? AND auth_version=?",actor.actor().accountId(),application,proofId,actor.sessionId(),actor.actor().securityVersion());
            var rows=jdbc.queryForList("SELECT * FROM identity.customer_proof WHERE id=? AND account_id=? AND purpose='BUSINESS_CONTACT' FOR UPDATE",proofId,actor.actor().accountId());
            if(requests.size()!=1||rows.size()!=1)throw rejected();var row=rows.getFirst();var proof=proof(row);Instant time=now();
            if(!protection.matches(proof,(String)row.get("verifier_key_id"),token,(byte[])row.get("verifier"),time))throw rejected();
            byte[] envelope=((byte[])requests.getFirst().get("protected_address")).clone(),addressDigest=null;
            try{
                String exact=GovernanceRecipientEnvelope.open(protection,proof,(String)row.get("verifier_key_id"),envelope,time);
                addressDigest=protection.fingerprint((String)row.get("verifier_key_id"),"businessContactAddress",actor.actor().accountId().toString(),application.toString(),proofId.toString(),exact);
                proof.consume(CustomerProof.Purpose.BUSINESS_CONTACT,actor.actor().accountId(),actor.sessionId(),actor.actor().securityVersion(),null,time);admission.requireDeadline(actor,proof.expiresAt());
                if(jdbc.update("UPDATE identity.customer_proof SET consumed_at=? WHERE id=? AND consumed_at IS NULL AND revoked_at IS NULL",stamp(time),proofId)!=1)throw rejected();
                UUID id=UUID.randomUUID();String encryptionKey=new String(Arrays.copyOfRange(envelope,2,2+Byte.toUnsignedInt(envelope[1])),StandardCharsets.US_ASCII);
                jdbc.update("INSERT INTO identity.business_contact_attestation(id,proof_id,account_id,application_binding,address_digest,protected_address,key_id,verified_at,expires_at) VALUES (?,?,?,?,?,?,?,?,?)",id,proofId,actor.actor().accountId(),application,addressDigest,envelope,encryptionKey,stamp(time),stamp(proof.expiresAt()));
                receipt(actor,"CONFIRM_BUSINESS_CONTACT",key,application,keyId,digest,proofId,id,null,time);audit(id,actor,"BUSINESS_CONTACT_VERIFIED",time);
                byte[] evidence=MessageDigest.getInstance("SHA-256").digest(("business-contact-possession:"+proofId).getBytes(StandardCharsets.US_ASCII));
                try{jdbc.update("UPDATE identity.customer_delivery SET status='DELIVERED',lease_id=NULL,lease_until=NULL,nonce=NULL,ciphertext=NULL,recipient_nonce=NULL,recipient_ciphertext=NULL,delivered_at=?,delivery_evidence=?,finished_at=?,version=version+1 WHERE proof_id=? AND status<>'DELIVERED'",stamp(time),evidence,stamp(time),proofId);}finally{Arrays.fill(evidence,(byte)0);}
                current(actor);return new BusinessContactAttestation(id,application,time,proof.expiresAt());
            }catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException("BUSINESS_CONTACT_EVIDENCE_UNAVAILABLE");}
            finally{Arrays.fill(envelope,(byte)0);if(addressDigest!=null)Arrays.fill(addressDigest,(byte)0);}
        }finally{Arrays.fill(digest,(byte)0);}
    }
    private BusinessContactAttestation attestation(UUID id){return jdbc.queryForObject("SELECT id,application_binding,verified_at,expires_at FROM identity.business_contact_attestation WHERE id=?",(r,n)->new BusinessContactAttestation(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getTimestamp(3).toInstant(),r.getTimestamp(4).toInstant()),id);}
    @Override public void consumeAttestation(CustomerAdmission.Context actor,UUID id,UUID application,long revision,String expectedEmail){
        current(actor);id(id);id(application);if(revision<1)throw rejected();String email=new CustomerEmail(expectedEmail).value();
        var rows=jdbc.queryForList("SELECT a.*,p.bound_session_id,p.bound_auth_version,p.verifier_key_id,p.revoked_at AS proof_revoked FROM identity.business_contact_attestation a JOIN identity.customer_proof p ON p.id=a.proof_id WHERE a.id=? FOR UPDATE OF a",id);
        if(rows.size()!=1)throw rejected();var row=rows.getFirst();Instant time=now();
        if(!actor.actor().accountId().equals(row.get("account_id"))||!application.equals(row.get("application_binding"))||!actor.sessionId().equals(row.get("bound_session_id"))||actor.actor().securityVersion()!=((Number)row.get("bound_auth_version")).longValue()||row.get("revoked_at")!=null||row.get("proof_revoked")!=null||row.get("used_at")!=null||time.isBefore(((Timestamp)row.get("verified_at")).toInstant())||!time.isBefore(((Timestamp)row.get("expires_at")).toInstant()))throw rejected();
        byte[] expected=protection.fingerprint((String)row.get("verifier_key_id"),"businessContactAddress",actor.actor().accountId().toString(),application.toString(),row.get("proof_id").toString(),email);
        try{if(!MessageDigest.isEqual(expected,(byte[])row.get("address_digest")))throw rejected();}finally{Arrays.fill(expected,(byte)0);}
        admission.requireDeadline(actor,((Timestamp)row.get("expires_at")).toInstant());if(jdbc.update("UPDATE identity.business_contact_attestation SET used_revision=?,used_at=? WHERE id=? AND used_at IS NULL AND revoked_at IS NULL",revision,stamp(time),id)!=1)throw rejected();current(actor);
    }
    @Override public BusinessContactAttestation submittedAttestation(CustomerAdmission.Context actor,UUID id,UUID application,long revision,String expectedEmail){
        current(actor);var subject=actor.actor();
        if(!subject.eligible())throw rejected();
        var result=submitted(id,application,subject.accountId(),subject.securityVersion(),revision,expectedEmail);
        current(actor);return result;
    }
    @Override public BusinessContactAttestation submittedAttestation(com.elogistic.backend.identity.api.admin.AdminCommandContext actor,UUID id,UUID application,UUID applicant,long revision,String expectedEmail){
        JdbcAdminCommandAdmission.requireOwned(source,actor);
        if(!enabled||!Set.of("LOCAL","TEST").contains(Objects.toString(environment,""))||!Boolean.TRUE.equals(jdbc.queryForObject("SELECT identity.local_admin_environment_matches(?)",Boolean.class,environment)))throw rejected();
        var subject=actor.customer(applicant);if(!subject.eligible())throw rejected();
        var result=submitted(id,application,subject.accountId(),subject.securityVersion(),revision,expectedEmail);
        JdbcAdminCommandAdmission.requireOwned(source,actor);return result;
    }
    private BusinessContactAttestation submitted(UUID id,UUID application,UUID account,long securityVersion,long revision,String expectedEmail){
        id(id);id(application);id(account);if(revision<1)throw rejected();String email;
        try{email=new CustomerEmail(expectedEmail).value();}catch(RuntimeException invalid){throw rejected();}
        var rows=jdbc.queryForList("""
            SELECT a.*,p.bound_auth_version,p.verifier_key_id,p.revoked_at AS proof_revoked,p.consumed_at AS proof_consumed,
              p.purpose,p.subject_id,p.account_id AS proof_account,p.expires_at AS proof_expiry
            FROM identity.business_contact_attestation a JOIN identity.customer_proof p ON p.id=a.proof_id
            JOIN identity.user_account u ON u.id=a.account_id AND u.status='ACTIVE' AND u.email_verified AND u.auth_version=?
            JOIN identity.customer_identifier i ON i.account_id=u.id AND i.status='ACTIVE' AND i.exact_email=u.email COLLATE "C"
            WHERE a.id=? AND a.account_id=? AND a.application_binding=? AND a.used_revision=? AND a.used_at IS NOT NULL
             AND EXISTS(SELECT 1 FROM identity.governance_proof_receipt r WHERE r.operation='CONFIRM_BUSINESS_CONTACT'
              AND r.actor_id=a.account_id AND r.attestation_id=a.id AND r.customer_proof_id=p.id AND r.target_id=a.application_binding
              AND r.auth_version=p.bound_auth_version AND r.customer_session_id=p.bound_session_id)
            """,securityVersion,id,account,application,revision);
        if(rows.size()!=1)throw rejected();var row=rows.getFirst();
        Instant verified=((Timestamp)row.get("verified_at")).toInstant();
        if(row.get("revoked_at")!=null||row.get("proof_revoked")!=null||row.get("proof_consumed")==null
            ||!row.get("proof_consumed").equals(row.get("verified_at"))||!row.get("expires_at").equals(row.get("proof_expiry"))
            ||!account.equals(row.get("subject_id"))||!account.equals(row.get("proof_account"))||!"BUSINESS_CONTACT".equals(row.get("purpose"))
            ||securityVersion!=((Number)row.get("bound_auth_version")).longValue()||now().isBefore(verified))throw rejected();
        byte[] expected=protection.fingerprint((String)row.get("verifier_key_id"),"businessContactAddress",account.toString(),application.toString(),row.get("proof_id").toString(),email);
        try{if(!MessageDigest.isEqual(expected,(byte[])row.get("address_digest")))throw rejected();}finally{Arrays.fill(expected,(byte)0);}
        return new BusinessContactAttestation(id,application,verified,((Timestamp)row.get("expires_at")).toInstant());
    }
    private void receipt(CustomerAdmission.Context actor,String operation,UUID key,UUID app,String keyId,byte[] digest,UUID proof,UUID attestation,byte[] envelope,Instant time){
        jdbc.update("INSERT INTO identity.governance_proof_receipt(actor_id,operation,operation_key,customer_session_id,auth_version,target_id,input_key_id,input_digest,customer_proof_id,attestation_id,protected_address,created_at,expires_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",actor.actor().accountId(),operation,key,actor.sessionId(),actor.actor().securityVersion(),app,keyId,digest,proof,attestation,envelope,stamp(time),stamp(time.plus(Duration.ofDays(7))));
    }
    private void audit(UUID id,CustomerAdmission.Context actor,String event,Instant time){jdbc.update("INSERT INTO identity.audit_entry(id,actor_id,target_id,event_type,source,reason,request_id,occurred_at) VALUES (?,?,?,?,'IDENTITY_CUSTOMER','Purpose-bound business contact possession',?,?)",id,actor.actor().accountId(),actor.actor().accountId(),event,actor.requestId().toString(),stamp(time));}
    @Override public String toString(){return "CustomerBusinessContacts[redacted]";}
}
