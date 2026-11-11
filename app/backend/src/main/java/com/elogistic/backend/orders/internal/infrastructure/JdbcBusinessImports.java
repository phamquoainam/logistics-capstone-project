package com.elogistic.backend.orders.internal.infrastructure;
import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.orders.api.BusinessPreparations.Access;
import com.elogistic.backend.orders.internal.application.BusinessCsv;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

public final class JdbcBusinessImports implements BusinessImports {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private final DataSource source;private final JdbcTemplate jdbc;private final BusinessPreparations drafts;private final BusinessPreparationProtection protection;private final Clock clock;
    public JdbcBusinessImports(DataSource source,BusinessPreparations drafts,BusinessPreparationProtection protection,Clock clock){this.source=source;jdbc=new JdbcTemplate(source);this.drafts=drafts;this.protection=protection;this.clock=clock;}
    private static BusinessPreparations.Failure fail(String code){return new BusinessPreparations.Failure(code);}
    private UUID org(Access a){a.requireCurrent();a.scope().requireCurrent();if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("BUSINESS_IMPORT_ADMISSION_REQUIRED");return a.scope().context().organizationId();}
    private void permit(Access a,UUID facility,boolean write){org(a);if(!a.scope().permits("shipment.draft.read",facility)||!a.scope().permits("shipment.import",facility)||write&&!a.scope().permits("shipment.draft.write",facility))throw fail("RESOURCE_NOT_FOUND");}
    private record Stored(UUID actor,UUID facility,UUID target,long expected,long result,String keyId,byte[] hash,String snapshot){}
    private Optional<Stored> prior(Access a,String op,UUID key){return jdbc.query("SELECT actor_account_id,facility_id,target_id,original_version,result_version,fingerprint_key_id,fingerprint,result_snapshot::text FROM orders.organization_preparation_receipt WHERE organization_id=? AND operation=? AND operation_key=?",(r,n)->new Stored(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getObject(3,UUID.class),r.getLong(4),r.getLong(5),r.getString(6),r.getBytes(7),r.getString(8)),org(a),op,key).stream().findFirst();}
    private Job replay(Access a,Stored r,String op,String payload){permit(a,r.facility(),op.equals("IMPORT_COMMIT"));byte[] hash=protection.hash(r.keyId(),org(a),a.actorAccountId(),op,payload);
        try{if(!r.actor().equals(a.actorAccountId())||!MessageDigest.isEqual(hash,r.hash()))throw fail("IDEMPOTENCY_CONFLICT");}finally{Arrays.fill(hash,(byte)0);Arrays.fill(r.hash(),(byte)0);}return JSON.readValue(r.snapshot(),Job.class);}
    private void one(int changed){if(changed!=1)throw new IllegalStateException("BUSINESS_IMPORT_EVIDENCE_REQUIRED");}
    private void evidence(Access a,String operation,UUID key,long before,String priorState,Job after,String payload){Instant now=after.updatedAt();byte[] hash=protection.hash(protection.active(),org(a),a.actorAccountId(),operation,payload);
        try{one(jdbc.update("INSERT INTO orders.organization_preparation_receipt(organization_id,operation,operation_key,actor_account_id,facility_id,target_id,original_version,result_version,fingerprint_key_id,fingerprint,recorded_at,result_snapshot) VALUES(?,?,?,?,?,?,?,?,?,?,?,?::jsonb)",org(a),operation,key,a.actorAccountId(),after.facilityId(),after.id(),before,after.version(),protection.active(),hash,Timestamp.from(now),JSON.writeValueAsString(after)));}finally{Arrays.fill(hash,(byte)0);}
        one(jdbc.update("INSERT INTO orders.organization_preparation_audit(id,organization_id,actor_account_id,target_id,operation,prior_version,new_version,prior_state,new_state,request_id,occurred_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),org(a),a.actorAccountId(),after.id(),operation,before,after.version(),priorState,after.state(),a.requestId(),Timestamp.from(now)));a.requireCurrent();a.meaningfulAction();
    }
    private String previewPayload(String template,UUID facility,String csv){return JSON.writeValueAsString(Arrays.asList(template,facility,csv));}
    @Override public Job preview(Access a,String template,UUID facility,String csv,UUID key){permit(a,facility,false);a.scope().requireActiveFacility(facility);if(!"BUSINESS_CSV_V1".equals(template)||key==null)throw fail("VALIDATION_FAILED");
        List<Row> rows=BusinessCsv.parse(facility,csv);String payload=previewPayload(template,facility,csv);var old=prior(a,"IMPORT_PREVIEW",key);if(old.isPresent())return replay(a,old.get(),"IMPORT_PREVIEW",payload);
        UUID id=UUID.randomUUID();Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);int invalid=(int)rows.stream().filter(r->r.state().equals("INVALID")).count();
        Job result=new Job(id,org(a),facility,a.actorAccountId(),template,"PREVIEW",1,rows.size(),rows.size()-invalid,invalid,0,0,now,now,rows.stream().limit(20).toList(),0,20,rows.size()>20);
        byte[] hash=protection.hash(protection.active(),org(a),a.actorAccountId(),"IMPORT_PREVIEW",payload);
        try{one(jdbc.update("INSERT INTO orders.organization_import_job VALUES(?,?,?,?,?,?,?,?::jsonb,'PREVIEW',1,?,?,?,0,0,?,?)",id,org(a),facility,a.actorAccountId(),template,protection.active(),hash,JSON.writeValueAsString(result),rows.size(),rows.size()-invalid,invalid,Timestamp.from(now),Timestamp.from(now)));}finally{Arrays.fill(hash,(byte)0);}
        for(var row:rows){byte[] rowHash=protection.hash(protection.active(),org(a),id,row.rowNumber(),JSON.writeValueAsString(row));try{
            one(jdbc.update("INSERT INTO orders.organization_import_row VALUES(?,?,?,?::jsonb,?,?,?, ?,?::jsonb,NULL,1)",id,row.rowNumber(),org(a),row.input()==null?null:JSON.writeValueAsString(row.input()),rowHash,row.input()==null?null:row.input().sourceChannel(),row.input()==null?null:row.input().sourceOrderReference(),row.state(),JSON.writeValueAsString(row.errors())));
        }finally{Arrays.fill(rowHash,(byte)0);}}
        evidence(a,"IMPORT_PREVIEW",key,0,null,result,payload);return result;
    }
    private record Metadata(UUID id,UUID organizationId,UUID facility,UUID creator,String state,long version,int total,int valid,int invalid,int created,int conflict,Instant createdAt,Instant updatedAt){}
    private Metadata metadata(Access a,UUID id){var found=jdbc.query("SELECT * FROM orders.organization_import_job WHERE id=? AND organization_id=?",(r,n)->new Metadata(r.getObject("id",UUID.class),r.getObject("organization_id",UUID.class),r.getObject("facility_id",UUID.class),r.getObject("creator_account_id",UUID.class),r.getString("state"),r.getLong("version"),r.getInt("total_rows"),r.getInt("valid_rows"),r.getInt("invalid_rows"),r.getInt("created_rows"),r.getInt("conflict_rows"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant()),id,org(a));if(found.size()!=1)throw fail("RESOURCE_NOT_FOUND");Metadata m=found.getFirst();permit(a,m.facility(),false);return m;}
    private List<Row> rows(Access a,UUID id,int page){return jdbc.query("SELECT row_number,state,proposed_input::text,errors::text,preparation_id,version FROM orders.organization_import_row WHERE job_id=? AND organization_id=? ORDER BY row_number LIMIT 21 OFFSET ?",(r,n)->{
        BusinessPreparations.Input input=r.getString(3)==null?null:JSON.readValue(r.getString(3),BusinessPreparations.Input.class);var errors=Arrays.asList(JSON.readValue(r.getString(4),Error[].class));return new Row(r.getInt(1),r.getString(2),input,errors,r.getObject(5,UUID.class),r.getLong(6));
    },id,org(a),Math.multiplyExact((long)page,20));}
    @Override public Job get(Access a,UUID id,int page){if(page<0)throw fail("VALIDATION_FAILED");var m=metadata(a,id);var rows=rows(a,id,page);a.requireCurrent();return new Job(id,m.organizationId(),m.facility(),m.creator(),"BUSINESS_CSV_V1",m.state(),m.version(),m.total(),m.valid(),m.invalid(),m.created(),m.conflict(),m.createdAt(),m.updatedAt(),rows.stream().limit(20).toList(),page,20,rows.size()>20);}
    @Override public Page list(Access a,int page){if(page<0)throw fail("VALIDATION_FAILED");UUID org=org(a);boolean all=a.scope().permits("shipment.import",null)&&a.scope().permits("shipment.draft.read",null);var sites=a.scope().facilities("shipment.import");if(!all&&sites.isEmpty())throw fail("RESOURCE_NOT_FOUND");
        List<Object> args=new ArrayList<>();args.add(org);String sql="SELECT id FROM orders.organization_import_job WHERE organization_id=?";if(!all){sql+=" AND facility_id IN ("+String.join(",",Collections.nCopies(sites.size(),"?"))+")";args.addAll(sites);}sql+=" ORDER BY updated_at DESC,id LIMIT 21 OFFSET ?";args.add(Math.multiplyExact((long)page,20));var ids=jdbc.query(sql,(r,n)->r.getObject(1,UUID.class),args.toArray());return new Page(ids.stream().limit(20).map(id->get(a,id,0)).toList(),page,20,ids.size()>20);
    }
    @Override public Job commit(Access a,UUID id,List<Integer> selected,long expected,UUID key){
        var before=metadata(a,id);permit(a,before.facility(),true);a.scope().requireActiveFacility(before.facility());
        if(key==null||selected==null||selected.isEmpty()||selected.size()>20||selected.stream().anyMatch(n->n==null||n<1||n>100)||new HashSet<>(selected).size()!=selected.size()||expected<1)throw fail("VALIDATION_FAILED");
        var ordered=selected.stream().sorted().toList();String payload=JSON.writeValueAsString(Arrays.asList(id,expected,ordered));var old=prior(a,"IMPORT_COMMIT",key);if(old.isPresent())return replay(a,old.get(),"IMPORT_COMMIT",payload);
        if(before.version()!=expected)throw fail("PRECONDITION_STALE");List<Row> candidates=new ArrayList<>();for(int number:ordered){var page=rows(a,id,(number-1)/20);var found=page.stream().filter(r->r.rowNumber()==number).findFirst().orElseThrow(()->fail("VALIDATION_FAILED"));if(!found.state().equals("VALID"))throw fail("IMPORT_ROW_NOT_VALID");candidates.add(found);}
        for(Row row:candidates){UUID preparation=null;String state="CREATED";List<Error> errors=List.of();
            try{UUID rowKey=UUID.nameUUIDFromBytes(("organization-import-v1:"+org(a)+":"+id+":"+row.rowNumber()).getBytes(StandardCharsets.UTF_8));preparation=drafts.create(a,row.input(),rowKey).id();}
            catch(BusinessPreparations.Failure conflict){if(!conflict.getMessage().equals("SOURCE_REFERENCE_CONFLICT"))throw conflict;state="CONFLICT";errors=List.of(new Error(row.rowNumber(),"source_order_reference","SOURCE_REFERENCE_CONFLICT"));}
            one(jdbc.update("UPDATE orders.organization_import_row SET state=?,errors=?::jsonb,preparation_id=?,version=version+1 WHERE job_id=? AND organization_id=? AND row_number=? AND state='VALID'",state,JSON.writeValueAsString(errors),preparation,id,org(a),row.rowNumber()));
        }
        Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);
        one(jdbc.update("UPDATE orders.organization_import_job j SET valid_rows=c.valid,created_rows=c.created,conflict_rows=c.conflict,state=CASE WHEN c.valid=0 THEN 'COMPLETED' ELSE 'PARTIAL' END,version=j.version+1,updated_at=? FROM (SELECT count(*) FILTER(WHERE state='VALID') valid,count(*) FILTER(WHERE state='CREATED') created,count(*) FILTER(WHERE state='CONFLICT') conflict FROM orders.organization_import_row WHERE job_id=?) c WHERE j.id=? AND j.organization_id=? AND j.version=?",Timestamp.from(now),id,id,org(a),expected));
        Job result=get(a,id,0);evidence(a,"IMPORT_COMMIT",key,expected,before.state(),result,payload);return result;
    }
    @Override public String template(Access a){org(a);if(!a.scope().permits("shipment.import",null)&&a.scope().facilities("shipment.import").isEmpty())throw fail("RESOURCE_NOT_FOUND");return String.join(",",BusinessCsv.HEADERS)+"\r\n";}
    public BusinessPreparations.Receipt receipt(Access a,String operation,UUID key){if(!Set.of("IMPORT_PREVIEW","IMPORT_COMMIT").contains(operation))throw fail("RESOURCE_NOT_FOUND");var r=prior(a,operation,key).orElseThrow(()->fail("RESOURCE_NOT_FOUND"));if(!r.actor().equals(a.actorAccountId()))throw fail("RESOURCE_NOT_FOUND");permit(a,r.facility(),operation.equals("IMPORT_COMMIT"));return new BusinessPreparations.Receipt(operation,r.target(),"COMMITTED",r.result());}
}
