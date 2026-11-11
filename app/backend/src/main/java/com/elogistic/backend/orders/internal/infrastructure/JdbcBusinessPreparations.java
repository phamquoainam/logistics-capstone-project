package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.BusinessPreparations;
import com.elogistic.backend.orders.internal.domain.ShippingPreparation;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

/** All calls join the existing Identity -> Merchant locked physical transaction. */
public final class JdbcBusinessPreparations implements BusinessPreparations {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private final DataSource source; private final JdbcTemplate jdbc; private final Clock clock;
    private final BusinessPreparationProtection protection;
    public JdbcBusinessPreparations(DataSource source,Clock clock,BusinessPreparationProtection protection){this.source=source;jdbc=new JdbcTemplate(source);this.clock=clock;this.protection=protection;}
    private static Failure fail(String code){return new Failure(code);}
    private UUID org(Access a){a.requireCurrent();a.scope().requireCurrent();
        if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("BUSINESS_PREPARATION_ADMISSION_REQUIRED");
        return a.scope().context().organizationId();
    }
    private void permit(Access a,String permission,UUID facility){org(a);if(!a.scope().permits("shipment.draft.read",facility)||!a.scope().permits(permission,facility))throw fail("RESOURCE_NOT_FOUND");}
    private Draft actions(Access a,Draft d){
        List<String> allowed=new ArrayList<>();if(d.state().equals("DRAFT")&&a.scope().permits("shipment.draft.write",d.facilityId()))allowed.addAll(List.of("UPDATE","ARCHIVE"));
        return new Draft(d.id(),d.organizationId(),d.facilityId(),d.creatorAccountId(),d.state(),d.preparationState(),d.version(),d.input(),d.sourceChannel(),d.sourceOrderReference(),d.createdAt(),d.updatedAt(),allowed);
    }
    private static String text(String value,int max){if(value==null)return null;String v=value.strip();if(v.isEmpty()||v.codePointCount(0,v.length())>max||v.codePoints().anyMatch(Character::isISOControl))throw fail("VALIDATION_FAILED");return v;}
    private Input valid(Input in){try{
        if(in==null)throw new IllegalArgumentException();var v=ShippingPreparation.normalize(in.shipping());String channel=text(in.sourceChannel(),40),ref=text(in.sourceOrderReference(),80);
        if((channel==null)!=(ref==null))throw new IllegalArgumentException();return new Input(in.facilityId(),channel,ref,v.reference(),v.instructions(),v.sender(),v.recipient(),v.parcels());
    }catch(RuntimeException invalid){throw fail("VALIDATION_FAILED");}}
    private Draft decode(String value){try{return JSON.readValue(value,Draft.class);}catch(RuntimeException invalid){throw new IllegalStateException("BUSINESS_PREPARATION_SNAPSHOT_INVALID");}}
    private Draft find(Access a,UUID id){UUID org=org(a);var rows=jdbc.query("SELECT v.snapshot::text FROM orders.organization_preparation d JOIN orders.organization_preparation_version v ON (v.id,v.organization_id,v.version)=(d.id,d.organization_id,d.version) WHERE d.id=? AND d.organization_id=?",(r,n)->decode(r.getString(1)),id,org);
        if(rows.size()!=1)throw fail("RESOURCE_NOT_FOUND");Draft result=rows.getFirst();permit(a,"shipment.draft.read",result.facilityId());return result;
    }
    private record Stored(UUID actor,UUID facility,UUID target,long expected,long result,String keyId,byte[] fingerprint){}
    private Optional<Stored> prior(Access a,String op,UUID key){return jdbc.query("SELECT actor_account_id,facility_id,target_id,original_version,result_version,fingerprint_key_id,fingerprint FROM orders.organization_preparation_receipt WHERE organization_id=? AND operation=? AND operation_key=?",(r,n)->new Stored(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getObject(3,UUID.class),r.getLong(4),r.getLong(5),r.getString(6),r.getBytes(7)),org(a),op,key).stream().findFirst();}
    private byte[] fingerprint(Access a,String keyId,String op,UUID id,long expected,Input in){return protection.hash(keyId,org(a),a.actorAccountId(),op,id,expected,in==null?null:JSON.writeValueAsString(in));}
    private Draft replay(Access a,String op,UUID id,long expected,Input in,Stored r){
        permit(a,"shipment.draft.write",r.facility());find(a,r.target());byte[] hash=fingerprint(a,r.keyId(),op,id,expected,in);
        try{if(!a.actorAccountId().equals(r.actor())||!id.equals(r.target())||expected!=r.expected()||!MessageDigest.isEqual(hash,r.fingerprint()))throw fail("IDEMPOTENCY_CONFLICT");}
        finally{Arrays.fill(hash,(byte)0);Arrays.fill(r.fingerprint(),(byte)0);}
        Draft original=jdbc.query("SELECT snapshot::text FROM orders.organization_preparation_version WHERE id=? AND organization_id=? AND version=?",(row,n)->decode(row.getString(1)),id,org(a),r.result()).getFirst();
        a.requireCurrent();return actions(a,original);
    }
    private void one(int changed){if(changed!=1)throw new IllegalStateException("BUSINESS_PREPARATION_EVIDENCE_REQUIRED");}
    private Draft commit(Access a,String op,UUID key,Draft before,Input input,boolean archive){
        UUID org=org(a),id=before==null?UUID.randomUUID():before.id();long expected=before==null?0:before.version();Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);
        Draft after=new Draft(id,org,archive?before.facilityId():input.facilityId(),before==null?a.actorAccountId():before.creatorAccountId(),archive?"ARCHIVED":"DRAFT","DRAFT",expected+1,
            archive?before.input():input.shipping(),before==null?input.sourceChannel():before.sourceChannel(),before==null?input.sourceOrderReference():before.sourceOrderReference(),before==null?now:before.createdAt(),now,List.of());
        permit(a,"shipment.draft.write",after.facilityId());if(!archive)a.scope().requireActiveFacility(after.facilityId());
        if(before==null){
            if(input.sourceChannel()!=null&&jdbc.queryForObject("SELECT count(*) FROM orders.organization_preparation WHERE organization_id=? AND source_channel=? AND source_order_reference=?",Integer.class,org,input.sourceChannel(),input.sourceOrderReference())!=0)throw fail("SOURCE_REFERENCE_CONFLICT");
            one(jdbc.update("INSERT INTO orders.organization_preparation(id,organization_id,facility_id,creator_account_id,state,preparation_state,version,source_channel,source_order_reference,payload,created_at,updated_at) VALUES (?,?,?,?,'DRAFT','DRAFT',1,?,?,?::jsonb,?,?)",id,org,after.facilityId(),a.actorAccountId(),input.sourceChannel(),input.sourceOrderReference(),JSON.writeValueAsString(after.input()),Timestamp.from(now),Timestamp.from(now)));
        }else one(jdbc.update("UPDATE orders.organization_preparation SET facility_id=?,state=?,preparation_state='DRAFT',version=version+1,payload=?::jsonb,updated_at=? WHERE id=? AND organization_id=? AND state='DRAFT' AND version=?",after.facilityId(),after.state(),JSON.writeValueAsString(after.input()),Timestamp.from(now),id,org,expected));
        one(jdbc.update("INSERT INTO orders.organization_preparation_version VALUES(?,?,?,?::jsonb,?)",id,org,after.version(),JSON.writeValueAsString(after),Timestamp.from(now)));
        one(jdbc.update("INSERT INTO orders.organization_preparation_audit(id,organization_id,actor_account_id,target_id,operation,prior_version,new_version,prior_state,new_state,request_id,occurred_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),org,a.actorAccountId(),id,op,expected,after.version(),before==null?null:before.state(),after.state(),a.requestId(),Timestamp.from(now)));
        byte[] hash=fingerprint(a,protection.active(),op,id,expected,input);
        try{one(jdbc.update("INSERT INTO orders.organization_preparation_receipt(organization_id,operation,operation_key,actor_account_id,facility_id,target_id,original_version,result_version,fingerprint_key_id,fingerprint,recorded_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",org,op,key,a.actorAccountId(),after.facilityId(),id,expected,after.version(),protection.active(),hash,Timestamp.from(now)));}finally{Arrays.fill(hash,(byte)0);}
        a.requireCurrent();a.meaningfulAction();return actions(a,after);
    }
    @Override public Draft create(Access a,Input input,UUID key){Input v=valid(input);permit(a,"shipment.draft.write",v.facilityId());if(key==null)throw fail("VALIDATION_FAILED");var r=prior(a,"CREATE",key);if(r.isPresent())return replay(a,"CREATE",r.get().target(),0,v,r.get());return commit(a,"CREATE",key,null,v,false);}
    @Override public Draft update(Access a,UUID id,Input input,long expected,UUID key){Input v=valid(input);Draft before=find(a,id);permit(a,"shipment.draft.write",before.facilityId());permit(a,"shipment.draft.write",v.facilityId());var r=prior(a,"UPDATE",key);if(r.isPresent())return replay(a,"UPDATE",id,expected,v,r.get());if(!before.state().equals("DRAFT")||before.version()!=expected)throw fail("PRECONDITION_STALE");if(!Objects.equals(v.sourceChannel(),before.sourceChannel())||!Objects.equals(v.sourceOrderReference(),before.sourceOrderReference()))throw fail("SOURCE_REFERENCE_IMMUTABLE");return commit(a,"UPDATE",key,before,v,false);}
    @Override public Draft archive(Access a,UUID id,long expected,UUID key){Draft before=find(a,id);permit(a,"shipment.draft.write",before.facilityId());var r=prior(a,"ARCHIVE",key);if(r.isPresent())return replay(a,"ARCHIVE",id,expected,null,r.get());if(!before.state().equals("DRAFT")||before.version()!=expected)throw fail("PRECONDITION_STALE");return commit(a,"ARCHIVE",key,before,null,true);}
    @Override public Draft get(Access a,UUID id){Draft d=find(a,id);a.requireCurrent();return actions(a,d);}
    @Override public Page list(Access a,int page){UUID org=org(a);if(page<0)throw fail("VALIDATION_FAILED");boolean all=a.scope().permits("shipment.draft.read",null);Set<UUID> sites=a.scope().facilities("shipment.draft.read");if(!all&&sites.isEmpty())throw fail("RESOURCE_NOT_FOUND");
        List<Object> args=new ArrayList<>();args.add(org);String sql="SELECT v.snapshot::text FROM orders.organization_preparation d JOIN orders.organization_preparation_version v ON (v.id,v.organization_id,v.version)=(d.id,d.organization_id,d.version) WHERE d.organization_id=? AND d.state='DRAFT'";
        if(!all){sql+=" AND d.facility_id IN ("+String.join(",",Collections.nCopies(sites.size(),"?"))+")";args.addAll(sites);}
        sql+=" ORDER BY d.updated_at DESC,d.id LIMIT 21 OFFSET ?";args.add(Math.multiplyExact((long)page,20));var rows=jdbc.query(sql,(r,n)->actions(a,decode(r.getString(1))),args.toArray());a.requireCurrent();return new Page(rows.stream().limit(20).toList(),page,20,rows.size()>20);
    }
    @Override public Receipt receipt(Access a,String op,UUID key){if(!Set.of("CREATE","UPDATE","ARCHIVE").contains(op))throw fail("RESOURCE_NOT_FOUND");var r=prior(a,op,key).orElseThrow(()->fail("RESOURCE_NOT_FOUND"));if(!r.actor().equals(a.actorAccountId()))throw fail("RESOURCE_NOT_FOUND");find(a,r.target());permit(a,"shipment.draft.write",r.facility());return new Receipt(op,r.target(),"COMMITTED",r.result());}
}
