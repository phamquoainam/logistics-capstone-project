package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.identity.internal.application.port.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Same password verifier/independent durable limiter/key ring; no parallel authentication engine. */
public final class JdbcAdminGovernanceReauthentication implements AdminGovernanceReauthentication {
    private static final Set<String> OPERATIONS=Set.of("APPROVE","REJECT","SUSPEND","REINSTATE");
    private final DataSource source;private final JdbcTemplate jdbc;private final CustomerProofProtection protection;
    private final SessionCredentials credentials;private final IdentityRateLimiter limiter;private final Supplier<String> peer;
    private final Clock clock;private final boolean enabled;
    public JdbcAdminGovernanceReauthentication(DataSource source,CustomerProofProtection protection,SessionCredentials credentials,IdentityRateLimiter limiter,Supplier<String> peer,Clock clock,boolean enabled){
        this.source=Objects.requireNonNull(source);jdbc=new JdbcTemplate(source);this.protection=Objects.requireNonNull(protection);
        this.credentials=Objects.requireNonNull(credentials);this.limiter=Objects.requireNonNull(limiter);this.peer=Objects.requireNonNull(peer);this.clock=Objects.requireNonNull(clock);this.enabled=enabled;
    }
    private static boolean hex(String s){return s!=null&&s.matches("[0-9a-f]{64}");}
    private static boolean id(UUID id){return id!=null&&!id.equals(new UUID(0,0));}
    private static AdminFailure denied(){return new AdminFailure("ACCESS_DENIED");}
    private static Timestamp stamp(Instant time){return Timestamp.from(time);}
    private void current(AdminCommandContext actor){
        if(!enabled||actor==null||!TransactionSynchronizationManager.hasResource(source))throw denied();JdbcAdminCommandAdmission.requireOwned(source,actor);
        if(!Set.of("LOCAL","TEST").contains(actor.environment()))throw denied();
    }
    private byte[] fingerprint(String key,AdminCommandContext actor,AdminStepUpInput input,String authority,UUID operationKey){
        return protection.fingerprint(key,"adminGovernanceReauthentication",operationKey.toString(),actor.accountId().toString(),actor.realmId().toString(),actor.sessionId().toString(),Long.toString(actor.authVersion()),authority,input.operation(),input.targetId().toString(),Long.toString(input.expectedVersion()),input.commandDigest(),input.password());
    }
    @Override public StepUpResult request(AdminCommandContext actor,AdminStepUpInput input,String authority,UUID key){
        current(actor);
        if(input==null||!id(key)||!id(input.targetId())||!OPERATIONS.contains(input.operation())||input.expectedVersion()<1||!hex(input.commandDigest())||!hex(authority)||input.password()==null||input.password().isEmpty())throw new AdminFailure("VALIDATION_FAILED");
        byte[] passwordBytes=input.password().getBytes(StandardCharsets.UTF_8);try{if(passwordBytes.length>72)throw new AdminFailure("VALIDATION_FAILED");}finally{Arrays.fill(passwordBytes,(byte)0);}
        var prior=jdbc.queryForList("""
            SELECT r.input_key_id,r.input_digest,r.system_session_id,r.auth_version,p.id,p.expires_at
            FROM identity.governance_proof_receipt r JOIN identity.admin_governance_reauthentication p ON p.id=r.system_proof_id
            WHERE r.actor_id=? AND r.operation='REQUEST_ADMIN_STEPUP' AND r.operation_key=?
            """,actor.accountId(),key);
        if(!prior.isEmpty()){
            var row=prior.getFirst();byte[] expected=fingerprint((String)row.get("input_key_id"),actor,input,authority,key);
            try{if(!MessageDigest.isEqual(expected,(byte[])row.get("input_digest"))||!actor.sessionId().equals(row.get("system_session_id"))||actor.authVersion()!=((Number)row.get("auth_version")).longValue())throw new AdminFailure("IDEMPOTENCY_CONFLICT");}
            finally{Arrays.fill(expected,(byte)0);}
            current(actor);return new StepUpResult((UUID)row.get("id"),((Timestamp)row.get("expires_at")).toInstant());
        }
        String username=jdbc.queryForObject("SELECT username FROM identity.user_account WHERE id=?",String.class,actor.accountId());
        var reservation=limiter.reserve(List.of(new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.LOGIN_ACCOUNT,username),new IdentityRateLimiter.Subject(IdentityRateLimiter.Policy.LOGIN_IP,peer.get())));
        boolean matches=credentials.verify(input.password(),jdbc.queryForObject("SELECT password_hash FROM identity.user_account WHERE id=?",String.class,actor.accountId()));
        reservation.finishLogin(matches);if(!matches)throw new AdminFailure("SESSION_INVALID");
        Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS),expires=now.plus(Duration.ofMinutes(15));UUID proof=UUID.randomUUID();
        String keyId=protection.activeVerifierKey();byte[] digest=HexFormat.of().parseHex(input.commandDigest()),binding=fingerprint(keyId,actor,input,authority,key);
        try{
            jdbc.update("""
                INSERT INTO identity.admin_governance_reauthentication(id,account_id,realm_id,session_id,auth_version,authority_version,operation,target_id,expected_version,command_digest,created_at,expires_at)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
                """,proof,actor.accountId(),actor.realmId(),actor.sessionId(),actor.authVersion(),authority,input.operation(),input.targetId(),input.expectedVersion(),digest,stamp(now),stamp(expires));
            jdbc.update("""
                INSERT INTO identity.governance_proof_receipt(actor_id,operation,operation_key,system_session_id,auth_version,target_id,input_key_id,input_digest,system_proof_id,created_at,expires_at)
                VALUES(?,'REQUEST_ADMIN_STEPUP',?,?,?,?,?,?,?,?,?)
                """,actor.accountId(),key,actor.sessionId(),actor.authVersion(),input.targetId(),keyId,binding,proof,stamp(now),stamp(now.plus(Duration.ofDays(7))));
            audit(proof,actor,"ADMIN_GOVERNANCE_REAUTHENTICATED",now);current(actor);return new StepUpResult(proof,expires);
        }finally{Arrays.fill(digest,(byte)0);Arrays.fill(binding,(byte)0);}
    }
    @Override public void consume(AdminCommandContext actor,UUID proof,String authority,String operation,UUID target,long version,byte[] digest){
        current(actor);if(!id(proof)||!id(target)||!hex(authority)||!OPERATIONS.contains(operation)||version<1||digest==null||digest.length!=32)throw denied();
        var rows=jdbc.queryForList("SELECT * FROM identity.admin_governance_reauthentication WHERE id=? FOR UPDATE",proof);
        if(rows.size()!=1)throw denied();var p=rows.getFirst();Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);
        if(!actor.accountId().equals(p.get("account_id"))||!actor.realmId().equals(p.get("realm_id"))||!actor.sessionId().equals(p.get("session_id"))
            ||actor.authVersion()!=((Number)p.get("auth_version")).longValue()||!authority.equals(p.get("authority_version"))||!operation.equals(p.get("operation"))
            ||!target.equals(p.get("target_id"))||version!=((Number)p.get("expected_version")).longValue()||!MessageDigest.isEqual(digest,(byte[])p.get("command_digest"))
            ||p.get("consumed_at")!=null||now.isBefore(((Timestamp)p.get("created_at")).toInstant())||!now.isBefore(((Timestamp)p.get("expires_at")).toInstant()))throw denied();
        if(jdbc.update("UPDATE identity.admin_governance_reauthentication SET consumed_at=?,consumed_request_id=? WHERE id=? AND consumed_at IS NULL",stamp(now),actor.requestId(),proof)!=1)throw denied();
        audit(actor.requestId(),actor,"ADMIN_GOVERNANCE_PROOF_CONSUMED",now);current(actor);
    }
    private void audit(UUID id,AdminCommandContext actor,String event,Instant now){
        jdbc.update("INSERT INTO identity.audit_entry(id,actor_id,target_id,event_type,source,reason,request_id,occurred_at) VALUES(?,?,?,?,'IDENTITY_ADMIN','Purpose-bound governance credential verification',?,?)",id,actor.accountId(),actor.accountId(),event,actor.requestId().toString(),stamp(now));
    }
    @Override public String toString(){return "AdminGovernanceReauthentication[redacted]";}
}
