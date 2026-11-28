package com.elogistic.backend.dispatch.internal.infrastructure;

import com.elogistic.backend.dispatch.api.StationIntake;
import com.elogistic.backend.orders.api.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static com.elogistic.backend.orders.api.PilotHandlingFailure.Code.*;

/** Same-transaction Dispatch authority/evidence. Runtime cannot create or change staff assignments. */
public final class JdbcStationIntake implements StationIntake,com.elogistic.backend.dispatch.api.OperationalStationIntake {
    private final DataSource source;private final JdbcTemplate jdbc;private final Clock clock;private final boolean enabled;private final String environment;
    private record Authority(UUID id,long version,UUID siteId,UUID siteVersionId) {}
    private final com.elogistic.backend.identity.api.staff.OperationalStaffAdmission operations;
    private final PilotHandling handling;private final boolean businessEnabled;
    private record Facts(UUID id,Instant createdAt,List<UUID> parcels,PilotHandling.Handling state,String ownerType,UUID org,String station,String domain){}
    private static Facts facts(PilotHandling.Cargo c){return new Facts(c.shipment().id(),c.shipment().createdAt(),c.shipment().parcels().stream().map(PilotShipments.Parcel::id).toList(),c.handling(),"PERSONAL",null,c.dropOffCode(),"");}
    private record Saved(String operation,byte[] hash,UUID target,UUID shipment) {}
    public JdbcStationIntake(DataSource source,Clock clock,boolean enabled,String environment){this(source,clock,enabled,environment,null);}
    public JdbcStationIntake(DataSource source,Clock clock,boolean enabled,String environment,com.elogistic.backend.identity.api.staff.OperationalStaffAdmission operations){this(source,clock,enabled,environment,operations,null,false);}
    public JdbcStationIntake(DataSource source,Clock clock,boolean enabled,String environment,com.elogistic.backend.identity.api.staff.OperationalStaffAdmission operations,PilotHandling handling,boolean businessEnabled){this.operations=operations;this.handling=handling;this.source=source;jdbc=new JdbcTemplate(source);this.clock=clock;this.enabled=enabled&&Set.of("LOCAL","TEST").contains(environment);this.environment=environment;this.businessEnabled=this.enabled&&businessEnabled;}
    private static PilotHandlingFailure fail(PilotHandlingFailure.Code code){return new PilotHandlingFailure(code);}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private void admitted(Access a){a.requireCurrent();if(!enabled)throw fail(INTAKE_POLICY_BLOCKED);if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("STATION_ADMISSION_TRANSACTION_REQUIRED");jdbc.execute("SELECT pg_advisory_xact_lock(730021,1)");}
    private Authority authorize(Access a,String station){
        admitted(a);if(a instanceof OpsAccess op)return operationalAuthority(op,station);if(station==null||!Set.of("SIM-DROP-A1","SIM-DROP-A2","SIM-DROP-B1").contains(station))throw fail(AUTHORITY_REQUIRED);
        var rows=jdbc.query("SELECT a.id,a.version FROM dispatch.pilot_station_assignment a JOIN dispatch.pilot_station s ON s.code=a.station_code WHERE a.account_id=? AND a.station_code=? AND a.permission='STATION_INTAKE' AND a.state='ACTIVE' AND a.effective_from<=? AND (a.effective_to IS NULL OR a.effective_to>?) AND s.active AND s.drop_off_enabled AND NOT s.pickup_enabled AND s.dataset_version='ARGO-SIM-COVERAGE-1' AND s.provenance='SIMULATION'",(r,n)->new Authority(r.getObject(1,UUID.class),r.getLong(2),null,null),a.actorId(),station,Timestamp.from(now()),Timestamp.from(now()));
        if(rows.size()!=1)throw fail(AUTHORITY_REQUIRED);a.requireCurrent();return rows.getFirst();
    }
    @Override public void require(Access a,String station){authorize(a,station);}
    @Override public List<Station> stations(Access a){
        admitted(a);var rows=jdbc.query("SELECT a.station_code,s.dataset_version,a.version,a.permission FROM dispatch.pilot_station_assignment a JOIN dispatch.pilot_station s ON s.code=a.station_code WHERE a.account_id=? AND a.permission='STATION_INTAKE' AND a.state='ACTIVE' AND a.effective_from<=? AND (a.effective_to IS NULL OR a.effective_to>?) AND s.active AND s.drop_off_enabled AND NOT s.pickup_enabled ORDER BY a.station_code",(r,n)->new Station(r.getString(1),r.getString(2),r.getLong(3),r.getString(4),true),a.actorId(),Timestamp.from(now()),Timestamp.from(now()));if(rows.isEmpty())throw fail(AUTHORITY_REQUIRED);a.requireCurrent();return rows;
    }
    private Optional<Saved> saved(Access a,String station,UUID key,String operation,String canonical){
        if(key==null)throw fail(VALIDATION_FAILED);
        var rows=jdbc.query("SELECT operation,fingerprint,target_id,shipment_id FROM dispatch.pilot_intake_receipt WHERE actor_id=? AND station_code=? AND operation_key=?",(r,n)->new Saved(r.getString(1),r.getBytes(2),r.getObject(3,UUID.class),r.getObject(4,UUID.class)),a.actorId(),station,key);
        if(rows.isEmpty())return Optional.empty();var old=rows.getFirst();if(!old.operation().equals(operation)||!MessageDigest.isEqual(old.hash(),hash(canonical)))throw fail(IDEMPOTENCY_CONFLICT);return Optional.of(old);
    }
    private static byte[] hash(String value){try{return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
    private void receipt(Access a,String station,UUID key,String operation,String canonical,UUID target,UUID shipment,Instant time){one(jdbc.update("INSERT INTO dispatch.pilot_intake_receipt(actor_id,station_code,operation_key,operation,fingerprint,target_id,shipment_id,created_at) VALUES (?,?,?,?,?,?,?,?)",a.actorId(),station,key,operation,hash(canonical),target,shipment,Timestamp.from(time)));}
    private void audit(Access a,String station,String operation,UUID target,UUID shipment,Instant time){one(jdbc.update("INSERT INTO dispatch.pilot_intake_audit(id,actor_id,station_code,operation,target_id,shipment_id,request_id,occurred_at) VALUES (?,?,?,?,?,?,?,?)",UUID.randomUUID(),a.actorId(),station,operation,target,shipment,a.requestId(),Timestamp.from(time)));}
    private static void one(int changed){if(changed!=1)throw new IllegalStateException("STATION_ATOMIC_WRITE_REQUIRED");}
    private Scan scanRow(ResultSet r,int number)throws SQLException{return new Scan(r.getObject("id",UUID.class),r.getObject("shipment_id",UUID.class),r.getObject("parcel_id",UUID.class),"AP-P-"+r.getObject("parcel_id",UUID.class).toString().replace("-",""),r.getString("station_code"),r.getTimestamp("scanned_at").toInstant());}
    private Scan readScan(Access a,String station,UUID id){return jdbc.query("SELECT * FROM dispatch.pilot_intake_scan WHERE actor_id=? AND station_code=? AND id=?",this::scanRow,a.actorId(),station,id).stream().findFirst().orElseThrow(()->fail(RESOURCE_NOT_FOUND));}
    @Override public Scan scan(Access a,String station,PilotHandling.Cargo cargo,String barcode,UUID key){if(cargo==null)throw fail(RESOURCE_NOT_FOUND);return scanFacts(a,station,facts(cargo),barcode,key);}
    private Scan scanFacts(Access a,String station,Facts cargo,String barcode,UUID key){
        var permission=authorize(a,station);if(cargo==null||!station.equals(cargo.station()))throw fail(RESOURCE_NOT_FOUND);
        var parcel=cargo.state().parcels().stream().filter(p->p.barcode().equals(barcode)).findFirst().orElseThrow(()->fail(RESOURCE_NOT_FOUND));String canonical=cargo.domain()+"SCAN|"+cargo.id()+"|"+parcel.parcelId();
        var replay=saved(a,station,key,"SCAN",canonical);if(replay.isPresent())return readScan(a,station,replay.get().target());
        if(cargo.state().version()!=0)throw fail(ALREADY_ACCEPTED);
        var existing=jdbc.query("SELECT * FROM dispatch.pilot_intake_scan WHERE actor_id=? AND station_code=? AND parcel_id=?",this::scanRow,a.actorId(),station,parcel.parcelId());Instant time=now();if(time.isBefore(cargo.createdAt()))throw fail(PARCEL_SET_INCOMPLETE);Scan result;
        if(existing.isEmpty()){
            result=new Scan(UUID.randomUUID(),cargo.id(),parcel.parcelId(),barcode,station,time);
            one(jdbc.update("INSERT INTO dispatch.pilot_intake_scan(id,actor_id,assignment_id,station_code,shipment_id,parcel_id,scanned_at,request_id,environment,provenance,operational_assignment_id,site_version_id,site_id) VALUES (?,?,?,?,?,?,?,?,?,'SIMULATION',?,?,?)",result.id(),a.actorId(),permission.siteId()==null?permission.id():null,station,result.shipmentId(),result.parcelId(),Timestamp.from(time),a.requestId(),environment,permission.siteId()==null?null:permission.id(),permission.siteVersionId(),permission.siteId()));audit(a,station,"SCAN",result.id(),result.shipmentId(),time);a.meaningfulAction();
        }else result=existing.getFirst();
        receipt(a,station,key,"SCAN",canonical,result.id(),result.shipmentId(),time);authorize(a,station);return result;
    }
    @Override public List<Scan> scans(Access a,String station,UUID shipment){authorize(a,station);return jdbc.query("SELECT * FROM dispatch.pilot_intake_scan WHERE actor_id=? AND station_code=? AND shipment_id=? ORDER BY scanned_at,id",this::scanRow,a.actorId(),station,shipment);}
    @Override public Acceptance accept(Access a,String station,PilotHandling.Cargo cargo,List<UUID> scanIds,long expected,String inspection,UUID key){if(cargo==null)throw fail(RESOURCE_NOT_FOUND);return acceptFacts(a,station,facts(cargo),scanIds,expected,inspection,key);}
    private Acceptance acceptFacts(Access a,String station,Facts cargo,List<UUID> scanIds,long expected,String inspection,UUID key){
        var permission=authorize(a,station);if(cargo==null||!station.equals(cargo.station()))throw fail(RESOURCE_NOT_FOUND);
        if(scanIds==null||scanIds.isEmpty()||scanIds.size()>5||scanIds.stream().anyMatch(Objects::isNull)||new HashSet<>(scanIds).size()!=scanIds.size()||expected<0)throw fail(VALIDATION_FAILED);
        if(!"MATCHED".equals(inspection))throw fail(INSPECTION_MISMATCH);
        String canonical=cargo.domain()+"ACCEPT|"+cargo.id()+"|"+expected+"|MATCHED|"+scanIds.stream().sorted().map(UUID::toString).reduce((x,y)->x+","+y).orElseThrow();
        var replay=saved(a,station,key,"ACCEPT",canonical);if(replay.isPresent()){
            var rows=jdbc.query("SELECT id,shipment_id,station_code,occurred_at FROM dispatch.pilot_intake_acceptance WHERE id=? AND actor_id=? AND station_code=?",(r,n)->new Acceptance(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getString(3),r.getTimestamp(4).toInstant(),false),replay.get().target(),a.actorId(),station);if(rows.size()!=1)throw new IllegalStateException("STATION_RECEIPT_BINDING_REQUIRED");return rows.getFirst();
        }
        if(cargo.state().version()!=expected)throw fail(PRECONDITION_STALE);if(expected!=0||!cargo.state().state().equals("WAITING_FOR_HANDOVER"))throw fail(ALREADY_ACCEPTED);
        var required=new HashSet<>(cargo.parcels());var observed=new ArrayList<Scan>();
        for(UUID id:scanIds){var scan=readScan(a,station,id);if(!scan.shipmentId().equals(cargo.id())||scan.scannedAt().isBefore(cargo.createdAt())||scan.scannedAt().isAfter(now()))throw fail(PARCEL_SET_INCOMPLETE);observed.add(scan);}
        if(observed.size()!=required.size()||!new HashSet<>(observed.stream().map(Scan::parcelId).toList()).equals(required))throw fail(PARCEL_SET_INCOMPLETE);
        Instant time=now();var result=new Acceptance(UUID.randomUUID(),cargo.id(),station,time,true);
        one(jdbc.update("INSERT INTO dispatch.pilot_intake_acceptance(id,shipment_id,actor_id,assignment_id,station_code,parcel_count,occurred_at,request_id,expected_version,inspection,policy_version,environment,provenance,operational_assignment_id,site_version_id,site_id,owner_type) VALUES (?,?,?,?,?,?,?,?,0,'MATCHED',?,?,'SIMULATION',?,?,?,?)",result.id(),result.shipmentId(),a.actorId(),permission.siteId()==null?permission.id():null,station,required.size(),Timestamp.from(time),a.requestId(),"BUSINESS".equals(cargo.ownerType())?"BR13-BUSINESS-INTAKE-01":"BR13-LOCAL-INTAKE-01",environment,permission.siteId()==null?null:permission.id(),permission.siteVersionId(),permission.siteId(),cargo.ownerType()));
        for(var scan:observed)one(jdbc.update("INSERT INTO dispatch.pilot_custody(acceptance_id,parcel_id,scan_id,shipment_id,station_code,actor_id,occurred_at) VALUES (?,?,?,?,?,?,?)",result.id(),scan.parcelId(),scan.id(),result.shipmentId(),station,a.actorId(),Timestamp.from(time)));
        audit(a,station,"ACCEPT",result.id(),result.shipmentId(),time);receipt(a,station,key,"ACCEPT",canonical,result.id(),result.shipmentId(),time);authorize(a,station);a.meaningfulAction();return result;
    }
    @Override public Receipt receipt(Access a,String station,UUID key){authorize(a,station);if(key==null)throw fail(VALIDATION_FAILED);var rows=jdbc.query("SELECT operation,target_id,shipment_id FROM dispatch.pilot_intake_receipt WHERE actor_id=? AND station_code=? AND operation_key=?",(r,n)->new Receipt(r.getString(1),r.getObject(2,UUID.class),r.getObject(3,UUID.class),"COMMITTED"),a.actorId(),station,key);if(rows.size()!=1)throw fail(RESOURCE_NOT_FOUND);return rows.getFirst();}

    private final class OpsAccess implements Access,PilotHandling.CarrierAccess {
        final com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context context;final UUID site;final String code;
        OpsAccess(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context context,UUID site,String code){this.context=context;this.site=site;this.code=code;}
        public UUID actorId(){requireCurrent();return context.actor().accountId();}public UUID requestId(){requireCurrent();return context.requestId();}
        public String stationCode(){requireCurrent();return code;}
        public void requireCurrent(){operations.requireCurrent(context);}public void meaningfulAction(){operations.meaningfulAction(context);}
    }
    private OpsAccess operational(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context context,UUID site){
        if(operations==null)throw fail(AUTHORITY_REQUIRED);operations.requireCurrent(context);if(site==null)throw fail(VALIDATION_FAILED);
        if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw fail(INTAKE_POLICY_BLOCKED);
        String code=jdbc.query("SELECT code FROM dispatch.carrier_site WHERE id=?",(r,n)->r.getString(1),site).stream().findFirst().orElseThrow(()->fail(AUTHORITY_REQUIRED));
        var a=new OpsAccess(context,site,code);authorize(a,code);return a;
    }
    private Authority operationalAuthority(OpsAccess a,String station){
        a.requireCurrent();if(!a.code.equals(station))throw fail(AUTHORITY_REQUIRED);
        var rows=jdbc.query("SELECT a.id,a.version,v.id AS site_version FROM dispatch.operational_site_assignment a JOIN dispatch.carrier_site s ON s.id=a.site_id JOIN dispatch.carrier_site_version v ON v.site_id=s.id WHERE a.account_id=? AND a.site_id=? AND a.permission='STATION_INTAKE' AND a.state='ACTIVE' AND a.effective_from<=? AND a.effective_to>? AND s.type='STATION' AND s.code=? AND v.environment=? AND v.state='ACTIVE' AND 'STATION_INTAKE'=ANY(v.capabilities) AND v.effective_from<=? AND (v.effective_to IS NULL OR v.effective_to>?) AND v.version=(SELECT max(x.version) FROM dispatch.carrier_site_version x WHERE x.site_id=s.id AND x.effective_from<=?) AND EXISTS(SELECT 1 FROM dispatch.operational_site_assignment ar WHERE ar.account_id=a.account_id AND ar.site_id=a.site_id AND ar.permission='SITE_READ' AND ar.state='ACTIVE' AND ar.effective_from<=? AND ar.effective_to>?) AND EXISTS(SELECT 1 FROM dispatch.carrier_site_link l WHERE l.from_site_id=s.id AND l.drop_off_code=s.code AND l.dataset_version='ARGO-SIM-COVERAGE-1' AND l.service_code='ROAD_STANDARD' AND l.area_code=v.area_code AND l.effective_from<=? AND (l.effective_to IS NULL OR l.effective_to>?) AND l.version=(SELECT max(x.version) FROM dispatch.carrier_site_link x WHERE x.link_id=l.link_id AND x.effective_from<=?))",(r,n)->new Authority(r.getObject(1,UUID.class),r.getLong(2),a.site,r.getObject(3,UUID.class)),a.actorId(),a.site,Timestamp.from(now()),Timestamp.from(now()),station,environment,Timestamp.from(now()),Timestamp.from(now()),Timestamp.from(now()),Timestamp.from(now()),Timestamp.from(now()),Timestamp.from(now()),Timestamp.from(now()),Timestamp.from(now()));
        if(rows.size()!=1)throw fail(AUTHORITY_REQUIRED);a.requireCurrent();return rows.getFirst();
    }
    @Override public List<OperationalSite> sites(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context c){
        if(operations==null)throw fail(AUTHORITY_REQUIRED);operations.requireCurrent(c);
        if(!enabled||!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw fail(INTAKE_POLICY_BLOCKED);
        jdbc.execute("SELECT pg_advisory_xact_lock(730021,1)");
        var result=jdbc.query("SELECT s.id,s.code,s.type,v.* FROM dispatch.carrier_site s JOIN dispatch.carrier_site_version v ON v.site_id=s.id WHERE v.environment=? AND v.effective_from<=? AND (v.effective_to IS NULL OR v.effective_to>?) AND v.version=(SELECT max(x.version) FROM dispatch.carrier_site_version x WHERE x.site_id=s.id AND x.effective_from<=?) AND v.state='ACTIVE' AND EXISTS(SELECT 1 FROM dispatch.operational_site_assignment a WHERE a.site_id=s.id AND a.account_id=? AND a.permission='SITE_READ' AND a.state='ACTIVE' AND a.effective_from<=? AND a.effective_to>?) ORDER BY s.code",(r,n)->new OperationalSite(r.getObject(1,UUID.class),r.getString("code"),r.getString("name"),r.getString("type"),r.getString("state"),r.getLong("version"),List.of((String[])r.getArray("capabilities").getArray()),r.getTimestamp("effective_from").toInstant(),r.getTimestamp("effective_to")==null?null:r.getTimestamp("effective_to").toInstant()),environment,Timestamp.from(now()),Timestamp.from(now()),Timestamp.from(now()),c.actor().accountId(),Timestamp.from(now()),Timestamp.from(now()));
        operations.requireCurrent(c);return result;
    }
    @Override public void require(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context c,UUID site){operational(c,site);}
    @Override public Scan scan(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context c,UUID site,PilotHandling.Cargo cargo,String barcode,UUID key){var a=operational(c,site);return scan(a,a.code,cargo,barcode,key);}
    @Override public List<Scan> scans(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context c,UUID site,UUID shipment){var a=operational(c,site);return scans(a,a.code,shipment);}
    @Override public Acceptance accept(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context c,UUID site,PilotHandling.Cargo cargo,List<UUID> ids,long expected,String inspection,UUID key){var a=operational(c,site);return accept(a,a.code,cargo,ids,expected,inspection,key);}
    @Override public Receipt receipt(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context c,UUID site,UUID key){var a=operational(c,site);return receipt(a,a.code,key);}

    private Facts carrierFacts(OpsAccess a,PilotHandling.CarrierCargo supplied){
        if(handling==null||supplied==null)throw fail(INTAKE_POLICY_BLOCKED);
        var c=handling.carrierAtStation(a,supplied.shipmentId());
        if(!c.equals(supplied))throw fail(VALIDATION_FAILED);
        if("BUSINESS".equals(c.ownerType())&&!businessEnabled)throw fail(INTAKE_POLICY_BLOCKED);
        String domain="BUSINESS".equals(c.ownerType())?"BUSINESS|"+c.organizationId()+"|"+c.facilityId()+"|"+c.quoteId()+"|"+c.parcels().stream().map(p->p.id().toString()).sorted().collect(java.util.stream.Collectors.joining(","))+"|":"";
        return new Facts(c.shipmentId(),c.createdAt(),c.parcels().stream().map(PilotShipments.Parcel::id).toList(),c.handling(),c.ownerType(),c.organizationId(),a.code,domain);
    }
    @Override public Scan scan(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context c,UUID site,PilotHandling.CarrierCargo cargo,String barcode,UUID key){var a=operational(c,site);return scanFacts(a,a.code,carrierFacts(a,cargo),barcode,key);}
    @Override public Acceptance accept(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context c,UUID site,PilotHandling.CarrierCargo cargo,List<UUID> ids,long expected,String inspection,UUID key){var a=operational(c,site);return acceptFacts(a,a.code,carrierFacts(a,cargo),ids,expected,inspection,key);}
}
