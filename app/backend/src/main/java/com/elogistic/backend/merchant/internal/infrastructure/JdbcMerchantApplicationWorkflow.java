package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.merchant.api.*;
import com.elogistic.backend.merchant.api.customer.MerchantApplications;
import com.elogistic.backend.merchant.internal.application.MerchantApplicationService;
import com.elogistic.backend.merchant.internal.application.port.MerchantFingerprints;
import com.elogistic.backend.merchant.internal.domain.MerchantApplicationDraft;
import com.elogistic.backend.platform.api.*;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.sql.*;
import java.text.Normalizer;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;
import static com.elogistic.backend.merchant.api.GovernanceFailure.Code.*;

/** V42 opt-in application workflow; the legacy default still has no submission capability. */
public final class JdbcMerchantApplicationWorkflow implements MerchantApplications {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private final CustomerAdmission admission;private final CustomerBusinessContacts contacts;private final MerchantFingerprints fingerprints;
    private final MerchantApplicationService drafts;private final JdbcTemplate jdbc;private final Clock clock;private final DataSource source;
    private final ProtectedGovernancePayloads payloads;private final PrivateEvidenceStore blobs;private final PrivateEvidenceScreening scanner;private final String environment;private final boolean enabled;
    public JdbcMerchantApplicationWorkflow(DataSource source,CustomerAdmission admission,CustomerBusinessContacts contacts,MerchantFingerprints fingerprints,
        PrivateEvidenceStore blobs,PrivateEvidenceScreening scanner,ProtectedGovernancePayloads payloads,Clock clock,boolean enabled,String environment){
        this.source=Objects.requireNonNull(source);this.admission=Objects.requireNonNull(admission);this.contacts=Objects.requireNonNull(contacts);this.fingerprints=Objects.requireNonNull(fingerprints);
        this.payloads=Objects.requireNonNull(payloads);this.blobs=Objects.requireNonNull(blobs);this.scanner=Objects.requireNonNull(scanner);this.clock=Objects.requireNonNull(clock);this.environment=environment;this.enabled=enabled;
        jdbc=new JdbcTemplate(source);drafts=new MerchantApplicationService(admission,new JdbcMerchantApplicationStore(source),fingerprints,clock);
        if(enabled&&!Set.of("LOCAL","TEST").contains(environment))throw new IllegalArgumentException("APPLICATION_WORKFLOW_LOCAL_TEST_REQUIRED");
    }
    @Override public Application create(CustomerAdmission.Context c,Input input,UUID key){return drafts.create(c,input,key);}
    @Override public Application update(CustomerAdmission.Context c,UUID id,Input input,long version,UUID key){return drafts.update(c,id,input,version,key);}
    @Override public Application get(CustomerAdmission.Context c,UUID id){return drafts.get(c,id);}
    @Override public Page list(CustomerAdmission.Context c,Query query){return drafts.list(c,query);}
    @Override public Receipt receipt(CustomerAdmission.Context c,String operation,UUID key){
        if(Set.of("createApplicationDraft","updateApplicationDraft").contains(operation))return drafts.receipt(c,operation,key);
        current(c);key(key);if(!Set.of("SUBMIT","RESUBMIT","WITHDRAW","ARCHIVE_EVIDENCE").contains(operation))throw fail(RESOURCE_NOT_FOUND);control();
        var r=saved(c,operation,null,key).orElseThrow(()->fail(RESOURCE_NOT_FOUND));var a=own(c,r.target());if(a.policy()!=null)policy(a);current(c);return new Receipt(operation,r.target(),"COMMITTED",r.result());
    }
    private static GovernanceFailure fail(GovernanceFailure.Code code){return new GovernanceFailure(code);}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private static Timestamp stamp(Instant time){return Timestamp.from(time);}
    private void current(CustomerAdmission.Context c){admission.requireCurrent(c);if(!enabled)throw fail(POLICY_UNAVAILABLE);if(!TransactionSynchronizationManager.hasResource(source))throw fail(ACCESS_DENIED);}
    private void control(){jdbc.query("SELECT pg_advisory_xact_lock(?)",(r,n)->0,JdbcCustomerProfileStore.CONTROL_LOCK);}
    private static void key(UUID id){if(id==null||id.equals(new UUID(0,0)))throw fail(VALIDATION_FAILED);}
    private record Row(Application value,UUID policy,Instant firstSubmitted,Instant created){}
    private Row own(CustomerAdmission.Context c,UUID id){
        key(id);return jdbc.query("SELECT * FROM merchant.application WHERE id=? AND applicant_account_id=? FOR UPDATE",(r,n)->new Row(new Application(id,c.actor().accountId(),r.getString("state"),r.getLong("current_revision"),r.getLong("version"),JSON.readValue(r.getString("draft"),Input.class)),r.getObject("policy_id",UUID.class),r.getTimestamp("first_submitted_at")==null?null:r.getTimestamp("first_submitted_at").toInstant(),r.getTimestamp("created_at").toInstant()),id,c.actor().accountId()).stream().findFirst().orElseThrow(()->fail(RESOURCE_NOT_FOUND));
    }
    private record Policy(UUID id,long version,String consent,String requiredEvidence,ConsentStatements statements){}
    private Policy policy(Row row){
        Input in=row.value().draft();if(in.entityType()==null||in.jurisdiction()==null)throw fail(POLICY_UNAVAILABLE);
        Instant at=now();var policies=jdbc.query("SELECT id,version,consent_version,array_to_string(required_evidence,','),consent_statements FROM merchant.application_policy WHERE environment=? AND state='PUBLISHED' AND entity_types @> ARRAY[?]::TEXT[] AND jurisdiction_codes @> ARRAY[?]::TEXT[] AND valid_from<=? AND (valid_to IS NULL OR valid_to>?) ORDER BY id",
            (r,n)->new Policy(r.getObject(1,UUID.class),r.getLong(2),r.getString(3),r.getString(4),JSON.readValue(r.getString(5),ConsentStatements.class)),environment,in.entityType(),MerchantApplicationDraft.jurisdictionKey(in.jurisdiction()),stamp(at),stamp(at));
        if(policies.size()!=1)throw fail(POLICY_UNAVAILABLE);if(row.policy()!=null&&!row.policy().equals(policies.getFirst().id()))throw fail(POLICY_UNAVAILABLE);return policies.getFirst();
    }
    @Override public ApplicationRequirements requirements(CustomerAdmission.Context actor,UUID application){
        current(actor);control();var row=own(actor,application);var policy=policy(row);current(actor);
        return new ApplicationRequirements(application,policy.id(),Long.toString(policy.version()),policy.consent(),policy.statements(),Arrays.asList(policy.requiredEvidence().split(",")));
    }
    private static void editable(Row row){if(!Set.of("DRAFT","NEEDS_INFORMATION").contains(row.value().state()))throw fail(STATE_CONFLICT);}
    private static String canonical(Map<String,Object> values){return JSON.writeValueAsString(new TreeMap<>(values));}
    private byte[] digest(String keyId,String operation,UUID target,long original,CustomerAdmission.Context c,UUID key,String body){return fingerprints.hash(keyId,operation,target,original,c.actor().accountId().toString(),"CUSTOMER",key.toString(),body);}
    private record Saved(UUID target,String keyId,byte[] digest,long original,long result,UUID event,long revision,Instant at){}
    private Optional<Saved> saved(CustomerAdmission.Context c,String operation,UUID target,UUID key){return jdbc.query("SELECT r.target_id,r.input_key_id,r.input_digest,r.original_version,r.result_version,r.event_id,e.revision,e.occurred_at FROM merchant.governance_receipt r JOIN merchant.governance_event e ON e.id=r.event_id WHERE r.actor_kind='CUSTOMER' AND r.actor_binding=? AND r.realm_id IS NULL AND r.operation=? AND r.operation_key=? AND r.target_type='APPLICATION'",(r,n)->new Saved(r.getObject(1,UUID.class),r.getString(2),r.getBytes(3),r.getLong(4),r.getLong(5),r.getObject(6,UUID.class),r.getLong(7),r.getTimestamp(8).toInstant()),c.actor().accountId().toString(),operation,key).stream().findFirst();}
    private void exact(Saved saved,CustomerAdmission.Context c,String hashOperation,UUID target,long expected,UUID key,String body){
        byte[] actual=digest(saved.keyId(),hashOperation,target,expected,c,key,body);
        try{if(!saved.target().equals(target)||saved.original()!=expected||!MessageDigest.isEqual(actual,saved.digest()))throw fail(IDEMPOTENCY_CONFLICT);}finally{Arrays.fill(actual,(byte)0);Arrays.fill(saved.digest(),(byte)0);}
    }
    private void eventReceipt(CustomerAdmission.Context c,UUID app,UUID policy,String operation,String hashOperation,UUID key,long prior,long result,long revision,String reason,String body,Instant at){
        eventReceipt(c,app,policy,operation,hashOperation,key,prior,result,revision,reason,body,body,at);
    }
    private void eventReceipt(CustomerAdmission.Context c,UUID app,UUID policy,String operation,String hashOperation,UUID key,long prior,long result,long revision,String reason,String body,String eventBody,Instant at){
        UUID event=UUID.randomUUID();byte[] protectedBody=payloads.seal(event,app,revision,operation,eventBody);
        try{jdbc.update("INSERT INTO merchant.governance_event(id,actor_kind,actor_id,target_type,target_id,application_id,policy_id,operation,revision,prior_version,new_version,reason_code,protected_payload,request_id,occurred_at) VALUES (?,'CUSTOMER',?,'APPLICATION',?,?,?,?,?,?,?,?,?,?,?)",event,c.actor().accountId(),app,app,policy,operation,revision==0?null:revision,prior,result,reason,protectedBody,c.requestId(),stamp(at));}finally{Arrays.fill(protectedBody,(byte)0);}
        byte[] hash=digest(fingerprints.activeKey(),hashOperation,app,prior,c,key,body);
        try{jdbc.update("INSERT INTO merchant.governance_receipt(actor_kind,actor_binding,operation,operation_key,target_type,target_id,original_version,result_version,input_key_id,input_digest,event_id,outcome,recorded_at,expires_at) VALUES ('CUSTOMER',?,?,?,'APPLICATION',?,?,?,?,?,?,'COMMITTED',?,?)",c.actor().accountId().toString(),operation,key,app,prior,result,fingerprints.activeKey(),hash,event,stamp(at),stamp(at.plus(Duration.ofDays(7))));}finally{Arrays.fill(hash,(byte)0);}
    }
    private static void complete(Input in){
        try{MerchantApplicationDraft.validate(in);}catch(RuntimeException invalid){throw fail(VALIDATION_FAILED);}
        if(in.entityType()==null||in.jurisdiction()==null||in.registeredName()==null||in.legalIdentifier()==null||in.registeredAddress()==null||in.operatingAddress()==null||in.representativeName()==null||in.representativeEmail()==null||in.representativeRelationship()==null||in.businessContactAttestationId()==null||in.declarations()==null)throw fail(VALIDATION_FAILED);
        if(in.evidenceIds()==null||in.evidenceIds().size()<2)throw fail(EVIDENCE_NOT_READY);
    }
    @Override public Application submit(CustomerAdmission.Context c,UUID id,SubmissionInput input,long expected,UUID key){return submit(c,id,input,expected,key,false);}
    @Override public Application resubmit(CustomerAdmission.Context c,UUID id,SubmissionInput input,long expected,UUID key){return submit(c,id,input,expected,key,true);}
    private Application submit(CustomerAdmission.Context c,UUID id,SubmissionInput input,long expected,UUID operationKey,boolean resubmit){
        current(c);key(operationKey);if(expected<1||input==null||input.organizationDisplayName()==null||input.organizationDisplayName().isBlank()||input.organizationDisplayName().codePointCount(0,input.organizationDisplayName().length())>160||!Normalizer.isNormalized(input.organizationDisplayName(),Normalizer.Form.NFC)||input.organizationDisplayName().codePoints().anyMatch(ch->Character.isISOControl(ch)||ch==0x2028||ch==0x2029||ch>=0xd800&&ch<=0xdfff))throw fail(VALIDATION_FAILED);
        control();Row row=own(c,id);Policy p=policy(row);String operation=resubmit?"RESUBMIT":"SUBMIT",hashOperation=resubmit?"resubmitMerchantApplication":"submitMerchantApplication",body=canonical(Map.of("organizationDisplayName",input.organizationDisplayName()));
        var previous=saved(c,operation,id,operationKey);
        if(previous.isPresent()){
            Saved receipt=previous.get();exact(receipt,c,hashOperation,id,expected,operationKey,body);
            Input original=jdbc.queryForObject("SELECT draft FROM merchant.application_draft_version WHERE application_id=? AND application_version=? AND applicant_account_id=?",(r,n)->JSON.readValue(r.getString(1),Input.class),id,receipt.result(),c.actor().accountId());current(c);return new Application(id,c.actor().accountId(),"SUBMITTED",receipt.revision(),receipt.result(),original);
        }
        if(row.value().version()!=expected)throw fail(PRECONDITION_STALE);if(!row.value().state().equals(resubmit?"NEEDS_INFORMATION":"DRAFT"))throw fail(STATE_CONFLICT);
        Input in=row.value().draft();complete(in);if(!p.consent().equals(in.declarations().consentVersion()))throw fail(POLICY_UNAVAILABLE);
        long revision=resubmit?row.value().revision()+1:1;Instant at=now();String draft=JSON.writeValueAsString(in);List<Evidence> evidence=new ArrayList<>();
        for(UUID evidenceId:in.evidenceIds()){var e=evidence(c,id,evidenceId);if(!e.state().equals("CLEAN"))throw fail(EVIDENCE_NOT_READY);evidence.add(e);}
        if(!evidence.stream().map(Evidence::purpose).collect(java.util.stream.Collectors.toSet()).containsAll(Arrays.asList(p.requiredEvidence().split(","))))throw fail(EVIDENCE_NOT_READY);
        CustomerBusinessContacts.BusinessContactAttestation contactFact;
        try{contacts.consumeAttestation(c,in.businessContactAttestationId(),id,revision,in.representativeEmail());contactFact=contacts.submittedAttestation(c,in.businessContactAttestationId(),id,revision,in.representativeEmail());}
        catch(com.elogistic.backend.identity.api.IdentityCommandFailure rejected){if(rejected.code()==com.elogistic.backend.identity.api.IdentityCommandFailure.Code.TOKEN_INVALID_OR_EXPIRED)throw fail(CONTACT_PROOF_REQUIRED);throw rejected;}
        var frozenFact=Map.of("id",contactFact.id().toString(),"applicationId",id.toString(),"revision",revision,"verifiedAt",contactFact.verifiedAt().toString());
        int changed=jdbc.update("UPDATE merchant.application SET state='SUBMITTED',current_revision=?,submitted_revision=?,policy_id=?,first_submitted_at=coalesce(first_submitted_at,?),version=version+1,updated_at=? WHERE id=? AND applicant_account_id=? AND version=?",revision,revision,p.id(),stamp(at),stamp(at),id,c.actor().accountId(),expected);if(changed!=1)throw fail(PRECONDITION_STALE);
        jdbc.update("INSERT INTO merchant.application_draft_version VALUES (?,?,?,?::jsonb,?,?)",id,expected+1,c.actor().accountId(),draft,stamp(row.created()),stamp(at));
        byte[] snapshot;
        try{snapshot=MessageDigest.getInstance("SHA-256").digest(canonical(Map.of("input",in,"displayName",input.organizationDisplayName(),"policy",p.id(),"policyVersion",p.version(),"contactFact",frozenFact,"evidence",evidence.stream().map(e->Map.of("id",e.id(),"hash",e.sha256())).toList())).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        catch(java.security.GeneralSecurityException unavailable){throw fail(SERVICE_UNAVAILABLE);}
        try{jdbc.update("INSERT INTO merchant.application_revision VALUES (?,?,?, ?,?::jsonb,?,?,?,?,?,?,?)",id,revision,c.actor().accountId(),expected+1,draft,input.organizationDisplayName(),snapshot,p.id(),p.version(),p.consent(),in.businessContactAttestationId(),stamp(at));}finally{Arrays.fill(snapshot,(byte)0);}
        for(Evidence e:evidence)jdbc.update("INSERT INTO merchant.application_revision_evidence VALUES (?,?,?,?)",id,revision,e.id(),e.sha256());
        eventReceipt(c,id,p.id(),operation,hashOperation,operationKey,expected,expected+1,revision,"APPLICANT_SUBMISSION",body,canonical(Map.of("command",JSON.readTree(body),"contactFact",frozenFact)),at);current(c);admission.meaningfulAction(c);
        return get(c,id);
    }
    @Override public Application withdraw(CustomerAdmission.Context c,UUID id,ReasonInput input,long expected,UUID operationKey){
        current(c);key(operationKey);if(expected<1||input==null||input.reason()==null||input.reason().isBlank()||input.reason().codePointCount(0,input.reason().length())>300||input.reason().codePoints().anyMatch(ch->Character.isISOControl(ch)||ch==0x2028||ch==0x2029))throw fail(VALIDATION_FAILED);
        control();var row=own(c,id);if(row.policy()!=null)policy(row);String body=canonical(Map.of("reason",input.reason()));
        var previous=saved(c,"WITHDRAW",id,operationKey);if(previous.isPresent()){var r=previous.get();exact(r,c,"withdrawMerchantApplication",id,expected,operationKey,body);Input original=jdbc.queryForObject("SELECT draft FROM merchant.application_draft_version WHERE application_id=? AND application_version=?",(rs,n)->JSON.readValue(rs.getString(1),Input.class),id,r.result());current(c);return new Application(id,c.actor().accountId(),"WITHDRAWN",r.revision(),r.result(),original);}
        if(row.value().version()!=expected)throw fail(PRECONDITION_STALE);if(!Set.of("DRAFT","SUBMITTED","NEEDS_INFORMATION").contains(row.value().state()))throw fail(STATE_CONFLICT);
        Instant at=now();jdbc.update("UPDATE merchant.application SET state='WITHDRAWN',version=version+1,terminal_at=?,updated_at=? WHERE id=? AND version=?",stamp(at),stamp(at),id,expected);
        jdbc.update("INSERT INTO merchant.application_draft_version VALUES (?,?,?,?::jsonb,?,?)",id,expected+1,c.actor().accountId(),JSON.writeValueAsString(row.value().draft()),stamp(row.created()),stamp(at));
        eventReceipt(c,id,row.policy(),"WITHDRAW","withdrawMerchantApplication",operationKey,expected,expected+1,row.value().revision(),"APPLICANT_WITHDRAWAL",body,at);current(c);admission.meaningfulAction(c);return get(c,id);
    }
    @Override public void archiveEvidence(CustomerAdmission.Context c,UUID app,UUID id,long expected,UUID operationKey){
        current(c);key(operationKey);if(expected<1)throw fail(VALIDATION_FAILED);control();Row row=own(c,app);Policy policy=policy(row);Evidence value=evidence(c,app,id);String body=canonical(Map.of("evidenceId",id));
        var previous=saved(c,"ARCHIVE_EVIDENCE",app,operationKey);if(previous.isPresent()){exact(previous.get(),c,"archiveApplicationEvidence",app,expected,operationKey,body);current(c);return;}
        editable(row);if(value.version()!=expected)throw fail(PRECONDITION_STALE);if("ARCHIVED".equals(value.state())||Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchant.application_revision_evidence WHERE evidence_id=?)",Boolean.class,id)))throw fail(STATE_CONFLICT);
        Instant at=now();if(jdbc.update("UPDATE merchant.application_evidence SET state='ARCHIVED',version=version+1,updated_at=? WHERE id=? AND application_id=? AND version=?",stamp(at),id,app,expected)!=1)throw fail(PRECONDITION_STALE);
        eventReceipt(c,app,policy.id(),"ARCHIVE_EVIDENCE","archiveApplicationEvidence",operationKey,expected,expected+1,0,"EVIDENCE_ARCHIVED",body,at);current(c);admission.meaningfulAction(c);
    }
    @Override public Evidence evidence(CustomerAdmission.Context c,UUID app,UUID id){
        current(c);own(c,app);key(id);return jdbc.query("SELECT * FROM merchant.application_evidence WHERE id=? AND application_id=?",(r,n)->new Evidence(id,app,r.getString("purpose"),r.getString("state"),r.getString("mime"),r.getLong("bytes"),r.getString("sha256"),r.getLong("version"),r.getTimestamp("screened_at")==null?null:r.getTimestamp("screened_at").toInstant()),id,app).stream().findFirst().orElseThrow(()->fail(RESOURCE_NOT_FOUND));
    }
    @Override public byte[] evidenceContent(String access,UUID request,UUID application,UUID id){
        if(TransactionSynchronizationManager.isActualTransactionActive())throw fail(ACCESS_DENIED);
        var authorized=run(access,request,c->{var value=evidence(c,application,id);if(!"CLEAN".equals(value.state()))throw fail(EVIDENCE_NOT_READY);return new PrivateEvidenceStore.Blob(jdbc.queryForObject("SELECT blob_key FROM merchant.application_evidence WHERE id=? AND application_id=?",UUID.class,id,application),value.bytes(),value.sha256());});
        byte[] result=null;
        try(var content=blobs.read(authorized)){
            result=content.bytes();run(access,request,c->{var value=evidence(c,application,id);if(!"CLEAN".equals(value.state())||value.bytes()!=authorized.bytes()||!value.sha256().equals(authorized.sha256()))throw fail(EVIDENCE_NOT_READY);return null;});
            byte[] returned=result;result=null;return returned;
        }finally{if(result!=null)Arrays.fill(result,(byte)0);}
    }
    private record Reservation(UUID id,UUID application,String state,Instant expires,byte[] digest){@Override public String toString(){return "EvidenceReservation[redacted]";}}
    private <T>T run(String access,UUID request,Function<CustomerAdmission.Context,T> action){return admission.execute(access,request,Set.of(),action);}
    @Override public Evidence uploadEvidence(String access,UUID request,UUID app,String purpose,String mime,byte[] supplied,UUID operationKey){
        key(operationKey);key(request);if(TransactionSynchronizationManager.isActualTransactionActive()||supplied==null||supplied.length<1||supplied.length>PrivateEvidenceStore.MAX_BYTES||purpose==null||!Set.of("REGISTRATION","REPRESENTATIVE_AUTHORIZATION").contains(purpose)||mime==null||!Set.of("image/png","image/jpeg","application/pdf").contains(mime))throw fail(VALIDATION_FAILED);
        byte[] bytes=supplied.clone();
        try{
            String sha;try{sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(java.security.GeneralSecurityException unavailable){throw fail(SERVICE_UNAVAILABLE);}
            String body=canonical(Map.of("purpose",purpose,"mime",mime,"bytes",bytes.length,"sha256",sha));
            Reservation reservation=run(access,request,c->{
                current(c);control();Row row=own(c,app);editable(row);Policy p=policy(row);
                var found=jdbc.query("SELECT id,application_id,state,expires_at,input_digest FROM merchant.application_evidence_reservation WHERE applicant_id=? AND operation_key=? FOR UPDATE",(r,n)->new Reservation(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getString(3),r.getTimestamp(4).toInstant(),r.getBytes(5)),c.actor().accountId(),operationKey);
                if(!found.isEmpty()){
                    var existing=found.getFirst();if(!app.equals(existing.application()))throw fail(IDEMPOTENCY_CONFLICT);boolean exact=false;for(String keyId:fingerprints.retainedKeys()){byte[] value=digest(keyId,"reserveApplicationEvidence",app,0,c,operationKey,body);try{exact|=MessageDigest.isEqual(value,existing.digest());}finally{Arrays.fill(value,(byte)0);}}
                    if(!exact)throw fail(IDEMPOTENCY_CONFLICT);if(!existing.state().equals("FINALIZED")&&(!existing.state().equals("RESERVED")||!now().isBefore(existing.expires())))throw fail(STATE_CONFLICT);return existing;
                }
                UUID id=UUID.randomUUID();Instant at=now();byte[] hash=digest(fingerprints.activeKey(),"reserveApplicationEvidence",app,0,c,operationKey,body);
                try{jdbc.update("INSERT INTO merchant.application_evidence_reservation VALUES (?,?,?,?,?,?,'RESERVED',?,?,NULL)",id,app,c.actor().accountId(),operationKey,hash,purpose,stamp(at),stamp(at.plus(Duration.ofMinutes(15))));}finally{Arrays.fill(hash,(byte)0);}
                eventReceipt(c,app,p.id(),"RESERVE_EVIDENCE","reserveApplicationEvidence",operationKey,0,1,0,"EVIDENCE_RESERVED",body,at);return new Reservation(id,app,"RESERVED",at.plus(Duration.ofMinutes(15)),new byte[0]);
            });
            if(reservation.state().equals("FINALIZED"))return run(access,request,c->{var id=jdbc.queryForObject("SELECT id FROM merchant.application_evidence WHERE reservation_id=?",UUID.class,reservation.id());return evidence(c,app,id);});
            // File/network/scanner I/O must never hold the Identity population/application transaction.
            if(TransactionSynchronizationManager.isActualTransactionActive())throw fail(ACCESS_DENIED);
            String inspected=scanner.inspect(bytes,mime);if(!mime.equals(inspected))throw fail(EVIDENCE_NOT_READY);
            var blob=blobs.put(reservation.id(),new ByteArrayInputStream(bytes));if(blob.bytes()!=bytes.length||!sha.equals(blob.sha256())||!reservation.id().equals(blob.key()))throw fail(EVIDENCE_NOT_READY);
            try(var content=blobs.read(blob)){byte[] actual=content.bytes();try{if(!MessageDigest.isEqual(bytes,actual))throw fail(EVIDENCE_NOT_READY);}finally{Arrays.fill(actual,(byte)0);}}
            return run(access,request,c->{
                current(c);control();Row row=own(c,app);editable(row);Policy p=policy(row);
                var existing=saved(c,"FINALIZE_EVIDENCE",app,operationKey);if(existing.isPresent()){exact(existing.get(),c,"finalizeApplicationEvidence",app,0,operationKey,body);return evidence(c,app,jdbc.queryForObject("SELECT id FROM merchant.application_evidence WHERE reservation_id=?",UUID.class,reservation.id()));}
                if(!now().isBefore(reservation.expires()))throw fail(STATE_CONFLICT);Instant at=now();UUID id=UUID.randomUUID();
                jdbc.update("INSERT INTO merchant.application_evidence VALUES (?,?,?,?,?,'CLEAN',?,?,?,'ARGO_PRIVATE_EVIDENCE_1',?,1,?,?)",id,app,reservation.id(),blob.key(),purpose,inspected,blob.bytes(),blob.sha256(),stamp(at),stamp(at),stamp(at));
                if(jdbc.update("UPDATE merchant.application_evidence_reservation SET state='FINALIZED',finalized_at=? WHERE id=? AND application_id=? AND applicant_id=? AND state='RESERVED'",stamp(at),reservation.id(),app,c.actor().accountId())!=1)throw fail(STATE_CONFLICT);
                eventReceipt(c,app,p.id(),"FINALIZE_EVIDENCE","finalizeApplicationEvidence",operationKey,0,1,0,"EVIDENCE_FINALIZED",body,at);current(c);admission.meaningfulAction(c);return evidence(c,app,id);
            });
        }catch(PrivateEvidenceStore.Rejected|PrivateEvidenceScreening.Rejected rejected){throw fail(EVIDENCE_NOT_READY);}
        catch(PrivateEvidenceStore.Unavailable|PrivateEvidenceScreening.Unavailable unavailable){throw fail(SERVICE_UNAVAILABLE);}
        finally{Arrays.fill(bytes,(byte)0);}
    }
}
