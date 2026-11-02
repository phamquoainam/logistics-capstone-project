package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static com.elogistic.backend.orders.api.PilotHandlingFailure.Code.*;

public final class JdbcPilotHandling implements PilotHandling {
    private final DataSource source;private final JdbcTemplate jdbc;private final JdbcPilotShipmentStore shipments;private final boolean enabled;private final String environment;
    private final PilotLabelRenderer renderer=new PilotLabelRenderer();
    public JdbcPilotHandling(DataSource source,boolean enabled,String environment){this.source=source;this.jdbc=new JdbcTemplate(source);shipments=new JdbcPilotShipmentStore(source);this.enabled=enabled;this.environment=environment;}
    private void joined(){if(!enabled)throw new PilotHandlingFailure(INTAKE_POLICY_BLOCKED);if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("PILOT_HANDLING_TRANSACTION_REQUIRED");}
    private PilotShipments.Shipment owned(ShippingDrafts.Access a,UUID id){a.requireCurrent();joined();if(id==null)throw new PilotHandlingFailure(VALIDATION_FAILED);var s=shipments.find(a.accountId(),id);a.requireCurrent();return s;}
    private Handling projection(PilotShipments.Shipment s){
        var events=jdbc.query("SELECT id,event_type,station_code,occurred_at,provenance FROM orders.pilot_tracking_event WHERE owner_id=? AND shipment_id=? ORDER BY occurred_at,id",(r,n)->new Event(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getTimestamp(4).toInstant(),r.getString(5)),s.accountId(),s.id());
        var accepted=new HashSet<>(jdbc.query("SELECT parcel_id FROM orders.pilot_parcel_event WHERE shipment_id=?",(r,n)->r.getObject(1,UUID.class),s.id()));
        if(events.size()>1||(!events.isEmpty()&&(!accepted.equals(new HashSet<>(s.parcels().stream().map(PilotShipments.Parcel::id).toList())))))throw new IllegalStateException("PILOT_TRACKING_INTEGRITY_REQUIRED");
        return new Handling(s.id(),events.isEmpty()?"WAITING_FOR_HANDOVER":"AT_STATION",events.isEmpty()?0:1,s.parcels().stream().map(p->new ParcelState(p.id(),p.parcelNumber(),PilotLabelRenderer.barcode(p.id()),accepted.contains(p.id())?"AT_STATION":"NOT_ACCEPTED")).toList(),events,true,true);
    }
    @Override public Handling self(ShippingDrafts.Access a,UUID shipment){return projection(owned(a,shipment));}
    @Override public Cargo lookup(CarrierAccess a,String barcode){a.requireCurrent();joined();UUID parcel=PilotLabelRenderer.parcelId(barcode);var ids=jdbc.query("SELECT s.id FROM orders.pilot_shipment s JOIN orders.pilot_parcel p ON p.shipment_id=s.id WHERE p.id=? AND s.quote_snapshot#>>'{route,dropOffCode}'=?",(r,n)->r.getObject(1,UUID.class),parcel,a.stationCode());if(ids.size()!=1)throw new PilotHandlingFailure(RESOURCE_NOT_FOUND);return atStation(a,ids.getFirst());}
    @Override public Cargo atStation(CarrierAccess a,UUID shipment){
        a.requireCurrent();joined();if(shipment==null)throw new PilotHandlingFailure(VALIDATION_FAILED);
        var owners=jdbc.query("SELECT owner_id FROM orders.pilot_shipment WHERE id=? AND quote_snapshot#>>'{route,dropOffCode}'=?",(r,n)->r.getObject(1,UUID.class),shipment,a.stationCode());if(owners.size()!=1)throw new PilotHandlingFailure(RESOURCE_NOT_FOUND);
        var s=shipments.find(owners.getFirst(),shipment);a.requireCurrent();return new Cargo(s,projection(s));
    }
    @Override public void accept(CarrierAccess a,UUID shipment,long expected,UUID acceptance,Instant occurredAt){
        var cargo=atStation(a,shipment);if(expected!=cargo.handling().version())throw new PilotHandlingFailure(PRECONDITION_STALE);if(expected!=0||!cargo.handling().events().isEmpty())throw new PilotHandlingFailure(ALREADY_ACCEPTED);if(acceptance==null||occurredAt==null)throw new PilotHandlingFailure(VALIDATION_FAILED);
        UUID event=UUID.randomUUID();one(jdbc.update("INSERT INTO orders.pilot_tracking_event(id,shipment_id,owner_id,source_acceptance_id,event_type,station_code,actor_id,occurred_at,version,environment,provenance) VALUES (?,?,?,?,'CARRIER_ACCEPTED_AT_STATION',?,?,?,1,?,'SIMULATION')",event,shipment,cargo.shipment().accountId(),acceptance,a.stationCode(),a.actorId(),Timestamp.from(occurredAt),environment));
        for(var p:cargo.shipment().parcels())one(jdbc.update("INSERT INTO orders.pilot_parcel_event(event_id,shipment_id,parcel_id,event_type,occurred_at) VALUES (?,?,?,'CARRIER_ACCEPTED_AT_STATION',?)",event,shipment,p.id(),Timestamp.from(occurredAt)));a.requireCurrent();
    }
    private static void one(int count){if(count!=1)throw new IllegalStateException("PILOT_TRACKING_WRITE_REQUIRED");}
    @Override public byte[] label(ShippingDrafts.Access a,UUID shipment){var s=owned(a,shipment);var bytes=renderer.render(s);a.requireCurrent();return bytes;}
    @Override public byte[] stationLabel(CarrierAccess a,UUID shipment){var s=atStation(a,shipment).shipment();var bytes=renderer.render(s);a.requireCurrent();return bytes;}
}
