package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.orders.api.OrganizationClaimEvidence.*;
import com.elogistic.backend.orders.api.OrganizationSupportTickets.Access;
import com.elogistic.backend.orders.api.OrganizationSupportTickets.StaffAccess;
import com.elogistic.backend.platform.api.PrivateEvidenceStore;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Durable metadata only. External upload/scanning never runs in this transaction. */
public final class JdbcOrganizationClaimEvidence implements OrganizationClaimEvidence {
    private final JdbcTemplate jdbc;private final OrganizationClaimIntakes claims;private final BusinessPreparationProtection protection;private final Clock clock;private final boolean enabled;
    private record File(Evidence value,UUID org,UUID actor,UUID key,long original,UUID object,Instant expires,String kid,byte[] digest){}
    public JdbcOrganizationClaimEvidence(DataSource source,OrganizationClaimIntakes claims,BusinessPreparationProtection protection,Clock clock,boolean enabled){jdbc=new JdbcTemplate(source);this.claims=claims;this.protection=protection;this.clock=clock;this.enabled=enabled;}
    private static OrganizationClaimIntakes.Failure fail(String code){return new OrganizationClaimIntakes.Failure(code);}
    private void available(){if(!enabled)throw fail("CLAIM_EVIDENCE_DISABLED");}
    private File row(ResultSet r,int n)throws SQLException{var attached=r.getTimestamp("attached_at");Long version=(Long)r.getObject("attached_version");return new File(new Evidence(r.getObject("id",UUID.class),r.getObject("intake_id",UUID.class),r.getString("mime"),r.getLong("bytes"),r.getString("sha256"),r.getString("state"),r.getTimestamp("created_at").toInstant(),attached==null?null:attached.toInstant(),version==null?r.getLong("reserved_version"):version),r.getObject("organization_id",UUID.class),r.getObject("actor_account_id",UUID.class),r.getObject("operation_key",UUID.class),r.getLong("original_version"),r.getObject("object_key",UUID.class),r.getTimestamp("expires_at").toInstant(),r.getString("fingerprint_key_id"),r.getBytes("fingerprint"));}
    private File file(UUID intake,UUID id,UUID org){return jdbc.query("SELECT * FROM orders.organization_claim_evidence WHERE intake_id=? AND id=? AND organization_id=?",this::row,intake,id,org).stream().findFirst().orElseThrow(()->fail("RESOURCE_NOT_FOUND"));}
    private byte[] digest(String kid,UUID org,UUID actor,UUID intake,long version,Input in){return protection.hash(kid,"claim-evidence-reservation-v1",org,actor,intake,version,in.mime(),in.bytes(),in.sha256());}
    private static boolean valid(Input in){return in!=null&&in.mime()!=null&&Set.of("image/png","image/jpeg","application/pdf").contains(in.mime())&&in.bytes()>0&&in.bytes()<=PrivateEvidenceStore.MAX_BYTES&&in.sha256()!=null&&in.sha256().matches("[0-9a-f]{64}");}
    private OrganizationClaimIntakes.Detail access(Access a,UUID id,boolean write){var detail=claims.get(a,id);available();if(write&&!a.scope().permits("support.write",detail.intake().facilityId()))throw fail("RESOURCE_NOT_FOUND");return detail;}
    private static void open(OrganizationClaimIntakes.Detail d){if(d.intake().state().equals("CLOSED_WITHOUT_DECISION"))throw fail("CLAIM_STATE_CONFLICT");}
    @Override public List<Evidence> list(Access a,UUID intake){var d=access(a,intake,false);return jdbc.query("SELECT * FROM orders.organization_claim_evidence WHERE intake_id=? AND organization_id=? ORDER BY reserved_version",this::row,intake,d.intake().organizationId()).stream().map(File::value).toList();}
    @Override public Evidence reserve(Access a,UUID intake,Input input,long expected,UUID key){
        var detail=access(a,intake,true);if(!valid(input)||key==null||key.equals(new UUID(0,0)))throw fail("VALIDATION_FAILED");UUID org=detail.intake().organizationId();
        var previous=jdbc.query("SELECT * FROM orders.organization_claim_evidence WHERE organization_id=? AND actor_account_id=? AND operation_key=?",this::row,org,a.actorAccountId(),key);
        if(!previous.isEmpty()){var f=previous.getFirst();byte[] calculated=digest(f.kid,org,a.actorAccountId(),intake,expected,input);try{if(!f.value.intakeId().equals(intake)||f.original!=expected||!MessageDigest.isEqual(f.digest,calculated))throw fail("IDEMPOTENCY_CONFLICT");}finally{Arrays.fill(calculated,(byte)0);Arrays.fill(f.digest,(byte)0);}return f.value;}
        open(detail);if(detail.intake().version()!=expected)throw fail("PRECONDITION_STALE");
        if(jdbc.queryForObject("SELECT count(*)>=10 OR coalesce(sum(bytes),0)+?>26214400 FROM orders.organization_claim_evidence WHERE intake_id=?",Boolean.class,input.bytes(),intake))throw fail("CLAIM_EVIDENCE_LIMIT");
        var changed=claims.supplement(a,intake,"Đã đăng ký tệp bằng chứng",expected,key);Instant created=changed.intake().updatedAt();UUID id=UUID.randomUUID();String kid=protection.active();byte[] hash=digest(kid,org,a.actorAccountId(),intake,expected,input);
        try{jdbc.update("INSERT INTO orders.organization_claim_evidence VALUES(?,?,?,?,?,?,?,?,?,'RESERVED',?,?,NULL,?,?,?,?,NULL)",id,intake,org,a.actorAccountId(),key,UUID.randomUUID(),input.mime(),input.bytes(),input.sha256(),expected,changed.intake().version(),kid,hash,Timestamp.from(created),Timestamp.from(created.plusSeconds(900)));}finally{Arrays.fill(hash,(byte)0);}
        return file(intake,id,org).value;
    }
    @Override public Permit uploadPermit(Access a,UUID intake,UUID id){var detail=access(a,intake,true);var f=file(intake,id,detail.intake().organizationId());if(!f.actor.equals(a.actorAccountId()))throw fail("RESOURCE_NOT_FOUND");if(!f.value.state().equals("CLEAN")){open(detail);if(!clock.instant().isBefore(f.expires))throw fail("CLAIM_EVIDENCE_EXPIRED");}return new Permit(f.org,a.actorAccountId(),f.value,f.object);}
    @Override public Evidence complete(Access a,Permit permit,PrivateEvidenceStore.Blob verified){
        if(permit==null||verified==null)throw fail("VALIDATION_FAILED");var current=uploadPermit(a,permit.evidence().intakeId(),permit.evidence().id());
        if(!a.actorAccountId().equals(permit.actorId())||!current.organizationId().equals(permit.organizationId())||!current.objectKey().equals(permit.objectKey())||!current.evidence().id().equals(permit.evidence().id())||!current.evidence().sha256().equals(permit.evidence().sha256())||current.evidence().bytes()!=permit.evidence().bytes()||!current.evidence().mime().equals(permit.evidence().mime())||!verified.key().equals(current.objectKey())||verified.bytes()!=current.evidence().bytes()||!verified.sha256().equals(current.evidence().sha256()))throw fail("IDEMPOTENCY_CONFLICT");
        if(current.evidence().state().equals("CLEAN"))return current.evidence();
        var detail=claims.get(a,current.evidence().intakeId());var updated=claims.supplement(a,detail.intake().id(),"Đã đính kèm tệp bằng chứng",detail.intake().version(),current.evidence().id());
        jdbc.update("UPDATE orders.organization_claim_evidence SET state='CLEAN',attached_version=?,attached_at=? WHERE id=? AND state='RESERVED'",updated.intake().version(),Timestamp.from(updated.intake().updatedAt()),current.evidence().id());a.requireCurrent();return file(detail.intake().id(),current.evidence().id(),current.organizationId()).value;
    }
    @Override public Permit readPermit(Access a,UUID intake,UUID id){var d=access(a,intake,false);var f=file(intake,id,d.intake().organizationId());if(!f.value.state().equals("CLEAN"))throw fail("RESOURCE_NOT_FOUND");return new Permit(f.org,a.actorAccountId(),f.value,f.object);}
    @Override public Permit staffReadPermit(StaffAccess a,UUID intake,UUID id){var d=claims.assigned(a,intake);available();var f=file(intake,id,d.intake().organizationId());if(!f.value.state().equals("CLEAN"))throw fail("RESOURCE_NOT_FOUND");return new Permit(f.org,a.accountId(),f.value,f.object);}
    @Override public List<Evidence> staffList(StaffAccess a,UUID intake){var d=claims.assigned(a,intake);available();return jdbc.query("SELECT * FROM orders.organization_claim_evidence WHERE intake_id=? AND organization_id=? ORDER BY reserved_version",this::row,intake,d.intake().organizationId()).stream().map(File::value).toList();}
}
