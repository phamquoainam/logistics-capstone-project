package com.elogistic.backend.identity.internal.infrastructure;
import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.identity.api.customer.CustomerMembershipInvitations.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Existing proof/outbox/crypto + purpose-separated recipient binding; no SMTP in this transaction. */
public final class JdbcCustomerMembershipInvitations implements CustomerMembershipInvitations {
    private final CustomerAdmission admission;private final DataSource source;private final JdbcTemplate jdbc;
    private final CustomerProofProtection protection;private final IdentityRateLimiter limiter;private final Function<String,byte[]> identifier;private final Clock clock;
    public JdbcCustomerMembershipInvitations(DataSource source,CustomerAdmission admission,CustomerProofProtection protection,IdentityRateLimiter limiter,Function<String,byte[]> identifier,Clock clock){this.source=source;this.admission=admission;this.jdbc=new JdbcTemplate(source);this.protection=protection;this.limiter=limiter;this.identifier=identifier;this.clock=clock;}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private void current(Bound context){if(context==null||context.organizationId()==null)throw failure("VALIDATION_FAILED");admission.requireCurrent(context.customer());if(!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("MEMBERSHIP_IDENTITY_ADMISSION_REQUIRED");}
    private static Failure failure(String code){return new Failure(code);}
    public static UUID proofId(UUID id,long version){return UUID.nameUUIDFromBytes(("merchant-membership-proof-v1:"+id+":"+version).getBytes(StandardCharsets.US_ASCII));}
    private static UUID deliveryId(UUID id,long version){return UUID.nameUUIDFromBytes(("merchant-membership-delivery-v1:"+id+":"+version).getBytes(StandardCharsets.US_ASCII));}
    @Override public byte[] recipientDigest(Bound c,String recipient){current(c);try{new CustomerEmail(recipient);return identifier.apply("membership-recipient|"+c.organizationId()+"|"+recipient);}catch(DomainViolation invalid){throw failure("VALIDATION_FAILED");}}
    @Override public ProtectedInvitation enqueueMembershipInvitation(Bound c,UUID invitation,long version,String recipient,byte[] token,Instant expires){
        current(c);Instant now=now();if(invitation==null||version<1||expires==null)throw failure("VALIDATION_FAILED");
        var issuer=c.customer().subjects().get(c.issuerAccountId());if(issuer==null||!issuer.eligible())throw failure("TEAM_INVITATION_INVALID_OR_EXPIRED");
        Instant created=expires.minusSeconds(259200);if(created.isAfter(now)||created.isBefore(now.minusSeconds(30)))throw failure("VALIDATION_FAILED");
        byte[] recipientHash=recipientDigest(c,recipient);UUID proofId=proofId(invitation,version),deliveryId=deliveryId(invitation,version);
        var proof=CustomerProof.issue(proofId,invitation,CustomerProof.Purpose.MERCHANT_MEMBERSHIP,created,null,null,c.organizationId());
        limiter.reserve(List.of(new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.CUSTOMER_ENROLL_EMAIL,"membership-command|"+c.customer().actor().accountId()+"|"+c.organizationId()+"|"+recipient)));
        revoke(c,invitation);
        try(var sealed=protection.protectMembership(proof,recipient,token,now)){
            byte[] verifier=sealed.verifier(),nonce=sealed.nonce(),ciphertext=sealed.ciphertext(),rn=sealed.recipientNonce(),rc=sealed.recipientCiphertext(),contact=protection.sealMembershipRecipient(invitation,c.organizationId(),recipient);
            try{
                jdbc.update("INSERT INTO identity.customer_proof(id,subject_id,account_id,purpose,verifier,verifier_key_id,created_at,expires_at,organization_id,policy_version) VALUES(?,?,?,'MERCHANT_MEMBERSHIP',?,?,?,?,?,'OTA-TEAM-1')",proofId,invitation,c.issuerAccountId(),verifier,sealed.verifierKey(),Timestamp.from(created),Timestamp.from(expires),c.organizationId());
                jdbc.update("INSERT INTO identity.customer_delivery(id,proof_id,subject_id,purpose,verifier_key_id,encryption_key_id,template_version,configuration_version,created_at,expires_at,status,due_at,nonce,ciphertext,recipient_nonce,recipient_ciphertext,version) VALUES(?,?,?,'MERCHANT_MEMBERSHIP',?,?,'OTA-TEAM-EMAIL-1','S003-SMTP-1',?,?,'QUEUED',?,?,?,?,?,1)",deliveryId,proofId,invitation,sealed.verifierKey(),sealed.encryptionKey(),Timestamp.from(created),Timestamp.from(expires),Timestamp.from(created),nonce,ciphertext,rn,rc);
                return new ProtectedInvitation(deliveryId,recipientHash,contact,verifier);
            }finally{for(byte[] b:List.of(verifier,nonce,ciphertext,rn,rc,contact))Arrays.fill(b,(byte)0);}
        }finally{Arrays.fill(recipientHash,(byte)0);}
    }
    @Override public ProtectedInvitation resendMembershipInvitation(Bound c,UUID invitation,long version,byte[] contact,byte[] token,Instant expires){current(c);String recipient=protection.openMembershipRecipient(invitation,c.organizationId(),contact);return enqueueMembershipInvitation(c,invitation,version,recipient,token,expires);}
    @Override public String maskedRecipient(Bound c,UUID invitation,byte[] recipient){current(c);String email=protection.openMembershipRecipient(invitation,c.organizationId(),recipient);int at=email.indexOf('@');return email.substring(0,1)+"***@"+email.substring(at+1);}
    @Override public boolean isRecipient(Bound c,UUID invitation,byte[] recipient){current(c);String email=protection.openMembershipRecipient(invitation,c.organizationId(),recipient);return java.security.MessageDigest.isEqual(email.getBytes(StandardCharsets.UTF_8),c.customer().actor().verifiedEmail().getBytes(StandardCharsets.UTF_8));}
    @Override public void consume(Bound c,UUID invitation,long version,byte[] token){
        current(c);Instant now=now();UUID id=proofId(invitation,version);
        var rows=jdbc.query("SELECT * FROM identity.customer_proof WHERE id=? AND subject_id=? AND organization_id=? AND purpose='MERCHANT_MEMBERSHIP' FOR UPDATE",(r,n)->new Object[]{new CustomerProof(id,invitation,CustomerProof.Purpose.MERCHANT_MEMBERSHIP,r.getTimestamp("created_at").toInstant(),r.getTimestamp("expires_at").toInstant(),r.getObject("bound_session_id",UUID.class),(Long)r.getObject("bound_auth_version"),c.organizationId(),r.getTimestamp("consumed_at")==null?null:r.getTimestamp("consumed_at").toInstant(),r.getTimestamp("revoked_at")==null?null:r.getTimestamp("revoked_at").toInstant()),r.getString("verifier_key_id"),r.getBytes("verifier")},id,invitation,c.organizationId());
        if(rows.size()!=1)throw failure("TEAM_INVITATION_INVALID_OR_EXPIRED");var row=rows.getFirst();var proof=(CustomerProof)row[0];byte[] expected=(byte[])row[2];
        try{if(!protection.matches(proof,(String)row[1],token,expected,now))throw failure("TEAM_INVITATION_INVALID_OR_EXPIRED");}finally{Arrays.fill(expected,(byte)0);}
        admission.requireDeadline(c.customer(),proof.expiresAt());
        if(jdbc.update("UPDATE identity.customer_proof SET consumed_at=?,account_id=?,bound_session_id=?,bound_auth_version=? WHERE id=? AND consumed_at IS NULL AND revoked_at IS NULL",Timestamp.from(now),c.customer().actor().accountId(),c.customer().sessionId(),c.customer().actor().securityVersion(),id)!=1)throw failure("TEAM_INVITATION_INVALID_OR_EXPIRED");
        byte[] evidence=protection.fingerprint(protection.activeVerifierKey(),"membershipDeliveryConsumption",id.toString(),c.customer().actor().accountId().toString());
        try{jdbc.update("UPDATE identity.customer_delivery SET status='DELIVERED',delivered_at=?,delivery_evidence=?,finished_at=COALESCE(finished_at,?),lease_id=NULL,lease_until=NULL,nonce=NULL,ciphertext=NULL,recipient_nonce=NULL,recipient_ciphertext=NULL,version=version+1 WHERE proof_id=? AND status<>'DELIVERED'",Timestamp.from(now),evidence,Timestamp.from(now),id);}finally{Arrays.fill(evidence,(byte)0);}
    }
    @Override public void revoke(Bound c,UUID invitation){current(c);Instant now=now();jdbc.update("UPDATE identity.customer_proof SET revoked_at=? WHERE subject_id=? AND organization_id=? AND purpose='MERCHANT_MEMBERSHIP' AND consumed_at IS NULL AND revoked_at IS NULL",Timestamp.from(now),invitation,c.organizationId());
        jdbc.update("UPDATE identity.customer_delivery SET status='EXPIRED',finished_at=?,lease_id=NULL,lease_until=NULL,nonce=NULL,ciphertext=NULL,recipient_nonce=NULL,recipient_ciphertext=NULL,version=version+1 WHERE subject_id=? AND purpose='MERCHANT_MEMBERSHIP' AND status NOT IN ('EXPIRED','DELIVERED')",Timestamp.from(now),invitation);}
    @Override public String deliveryState(Bound c,UUID invitation,long version){current(c);return jdbc.query("SELECT status FROM identity.customer_delivery WHERE id=? AND subject_id=? AND purpose='MERCHANT_MEMBERSHIP'",(r,n)->r.getString(1),deliveryId(invitation,version),invitation).stream().findFirst().orElseThrow(()->failure("SERVICE_UNAVAILABLE"));}
}
