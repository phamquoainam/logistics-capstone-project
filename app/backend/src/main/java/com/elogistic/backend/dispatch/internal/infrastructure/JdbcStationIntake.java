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
public final class JdbcStationIntake implements StationIntake {
    private final DataSource source;private final JdbcTemplate jdbc;private final Clock clock;private final boolean enabled;private final String environment;
    private record Authority(UUID id,long version) {}
    private record Saved(String operation,byte[] hash,UUID target,UUID shipment) {}
    public JdbcStationIntake(DataSource source,Clock clock,boolean enabled,String environment){this.source=source;jdbc=new JdbcTemplate(source);this.clock=clock;this.enabled=enabled;this.environment=environment;}
    private static PilotHandlingFailure fail(PilotHandlingFailure.Code code){return new PilotHandlingFailure(code);}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private void admitted(Access a){a.requireCurrent();if(!enabled)throw fail(INTAKE_POLICY_BLOCKED);if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("STATION_ADMISSION_TRANSACTION_REQUIRED");jdbc.execute("SELECT pg_advisory_xact_lock(730021,1)");}
    private Authority authorize(Access a,String station){
        admitted(a);if(station==null||!Set.of("SIM-DROP-A1","SIM-DROP-A2","SIM-DROP-B1").contains(station))throw fail(AUTHORITY_REQUIRED);
        var rows=jdbc.query("SELECT a.id,a.version FROM dispatch.pilot_station_assignment a JOIN dispatch.pilot_station s ON s.code=a.station_code WHERE a.account_id=? AND a.station_code=? AND a.permission='STATION_INTAKE' AND a.state='ACTIVE' AND a.effective_from<=? AND (a.effective_to IS NULL OR a.effective_to>?) AND s.active AND s.drop_off_enabled AND NOT s.pickup_enabled AND s.dataset_version='ARGO-SIM-COVERAGE-1' AND s.provenance='SIMULATION'",(r,n)->new Authority(r.getObject(1,UUID.class),r.getLong(2)),a.actorId(),station,Timestamp.from(now()),Timestamp.from(now()));
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
    @Override public Scan scan(Access a,String station,PilotHandling.Cargo cargo,String barcode,UUID key){
        var permission=authorize(a,station);if(cargo==null||!station.equals(cargo.dropOffCode()))throw fail(RESOURCE_NOT_FOUND);
        var parcel=cargo.handling().parcels().stream().filter(p->p.barcode().equals(barcode)).findFirst().orElseThrow(()->fail(RESOURCE_NOT_FOUND));String canonical="SCAN|"+cargo.shipment().id()+"|"+parcel.parcelId();
        var replay=saved(a,station,key,"SCAN",canonical);if(replay.isPresent())return readScan(a,station,replay.get().target());
        if(cargo.handling().version()!=0)throw fail(ALREADY_ACCEPTED);
        var existing=jdbc.query("SELECT * FROM dispatch.pilot_intake_scan WHERE actor_id=? AND station_code=? AND parcel_id=?",this::scanRow,a.actorId(),station,parcel.parcelId());Instant time=now();Scan result;
        if(existing.isEmpty()){
            result=new Scan(UUID.randomUUID(),cargo.shipment().id(),parcel.parcelId(),barcode,station,time);
            one(jdbc.update("INSERT INTO dispatch.pilot_intake_scan(id,actor_id,assignment_id,station_code,shipment_id,parcel_id,scanned_at,request_id,environment,provenance) VALUES (?,?,?,?,?,?,?,?,?,'SIMULATION')",result.id(),a.actorId(),permission.id(),station,result.shipmentId(),result.parcelId(),Timestamp.from(time),a.requestId(),environment));audit(a,station,"SCAN",result.id(),result.shipmentId(),time);a.meaningfulAction();
        }else result=existing.getFirst();
        receipt(a,station,key,"SCAN",canonical,result.id(),result.shipmentId(),time);authorize(a,station);return result;
    }
    @Override public List<Scan> scans(Access a,String station,UUID shipment){authorize(a,station);return jdbc.query("SELECT * FROM dispatch.pilot_intake_scan WHERE actor_id=? AND station_code=? AND shipment_id=? ORDER BY scanned_at,id",this::scanRow,a.actorId(),station,shipment);}
    @Override public Acceptance accept(Access a,String station,PilotHandling.Cargo cargo,List<UUID> scanIds,long expected,String inspection,UUID key){
        var permission=authorize(a,station);if(cargo==null||!station.equals(cargo.dropOffCode()))throw fail(RESOURCE_NOT_FOUND);
        if(scanIds==null||scanIds.isEmpty()||scanIds.size()>5||scanIds.stream().anyMatch(Objects::isNull)||new HashSet<>(scanIds).size()!=scanIds.size()||expected<0)throw fail(VALIDATION_FAILED);
        if(!"MATCHED".equals(inspection))throw fail(INSPECTION_MISMATCH);
        String canonical="ACCEPT|"+cargo.shipment().id()+"|"+expected+"|MATCHED|"+scanIds.stream().sorted().map(UUID::toString).reduce((x,y)->x+","+y).orElseThrow();
        var replay=saved(a,station,key,"ACCEPT",canonical);if(replay.isPresent()){
            var rows=jdbc.query("SELECT id,shipment_id,station_code,occurred_at FROM dispatch.pilot_intake_acceptance WHERE id=? AND actor_id=? AND station_code=?",(r,n)->new Acceptance(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getString(3),r.getTimestamp(4).toInstant(),false),replay.get().target(),a.actorId(),station);if(rows.size()!=1)throw new IllegalStateException("STATION_RECEIPT_BINDING_REQUIRED");return rows.getFirst();
        }
        if(cargo.handling().version()!=expected)throw fail(PRECONDITION_STALE);if(expected!=0||!cargo.handling().state().equals("WAITING_FOR_HANDOVER"))throw fail(ALREADY_ACCEPTED);
        var required=new HashSet<>(cargo.shipment().parcels().stream().map(PilotShipments.Parcel::id).toList());var observed=new ArrayList<Scan>();
        for(UUID id:scanIds){var scan=readScan(a,station,id);if(!scan.shipmentId().equals(cargo.shipment().id())||scan.scannedAt().isBefore(cargo.shipment().createdAt())||scan.scannedAt().isAfter(now()))throw fail(PARCEL_SET_INCOMPLETE);observed.add(scan);}
        if(observed.size()!=required.size()||!new HashSet<>(observed.stream().map(Scan::parcelId).toList()).equals(required))throw fail(PARCEL_SET_INCOMPLETE);
        Instant time=now();var result=new Acceptance(UUID.randomUUID(),cargo.shipment().id(),station,time,true);
        one(jdbc.update("INSERT INTO dispatch.pilot_intake_acceptance(id,shipment_id,actor_id,assignment_id,station_code,parcel_count,occurred_at,request_id,expected_version,inspection,policy_version,environment,provenance) VALUES (?,?,?,?,?,?,?,?,0,'MATCHED','BR13-LOCAL-INTAKE-01',?,'SIMULATION')",result.id(),result.shipmentId(),a.actorId(),permission.id(),station,required.size(),Timestamp.from(time),a.requestId(),environment));
        for(var scan:observed)one(jdbc.update("INSERT INTO dispatch.pilot_custody(acceptance_id,parcel_id,scan_id,shipment_id,station_code,actor_id,occurred_at) VALUES (?,?,?,?,?,?,?)",result.id(),scan.parcelId(),scan.id(),result.shipmentId(),station,a.actorId(),Timestamp.from(time)));
        audit(a,station,"ACCEPT",result.id(),result.shipmentId(),time);receipt(a,station,key,"ACCEPT",canonical,result.id(),result.shipmentId(),time);authorize(a,station);a.meaningfulAction();return result;
    }
    @Override public Receipt receipt(Access a,String station,UUID key){authorize(a,station);if(key==null)throw fail(VALIDATION_FAILED);var rows=jdbc.query("SELECT operation,target_id,shipment_id FROM dispatch.pilot_intake_receipt WHERE actor_id=? AND station_code=? AND operation_key=?",(r,n)->new Receipt(r.getString(1),r.getObject(2,UUID.class),r.getObject(3,UUID.class),"COMMITTED"),a.actorId(),station,key);if(rows.size()!=1)throw fail(RESOURCE_NOT_FOUND);return rows.getFirst();}
}
