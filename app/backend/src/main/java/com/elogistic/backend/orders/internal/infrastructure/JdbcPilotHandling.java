package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.pricing.api.BusinessQuotes;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;
import static com.elogistic.backend.orders.api.PilotHandlingFailure.Code.*;

/** One Orders event stream for both immutable ownership variants. */
public final class JdbcPilotHandling implements PilotHandling {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private final DataSource source;
    private final JdbcTemplate jdbc;
    private final JdbcPilotShipmentStore shipments;
    private final boolean enabled,businessEnabled;
    private final String environment;
    private final PilotLabelRenderer renderer=new PilotLabelRenderer();

    public JdbcPilotHandling(DataSource source,boolean enabled,String environment){this(source,enabled,environment,false);}
    public JdbcPilotHandling(DataSource source,boolean enabled,String environment,boolean businessEnabled){
        this.source=source;this.jdbc=new JdbcTemplate(source);shipments=new JdbcPilotShipmentStore(source);
        this.enabled=enabled&&Set.of("LOCAL","TEST").contains(environment);this.environment=environment;
        this.businessEnabled=this.enabled&&businessEnabled;
    }
    private void joined(){
        if(!enabled)throw new PilotHandlingFailure(INTAKE_POLICY_BLOCKED);
        if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))
            throw new IllegalStateException("PILOT_HANDLING_TRANSACTION_REQUIRED");
    }
    private PilotShipments.Shipment owned(ShippingDrafts.Access a,UUID id){
        a.requireCurrent();joined();if(id==null)throw new PilotHandlingFailure(VALIDATION_FAILED);
        var s=shipments.find(a.accountId(),id);a.requireCurrent();return s;
    }
    private Handling projection(UUID id,String kind,UUID account,UUID org,List<PilotShipments.Parcel> parcels){
        var events=jdbc.query("SELECT id,event_type,station_code,occurred_at,provenance FROM orders.pilot_tracking_event WHERE owner_type=? AND shipment_id=? AND owner_id IS NOT DISTINCT FROM ?::uuid AND organization_id IS NOT DISTINCT FROM ?::uuid ORDER BY occurred_at,id",
            (r,n)->new Event(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getTimestamp(4).toInstant(),r.getString(5)),kind,id,account,org);
        var accepted=new HashSet<>(jdbc.query("SELECT parcel_id FROM orders.pilot_parcel_event WHERE shipment_id=?",(r,n)->r.getObject(1,UUID.class),id));
        return handling(id,parcels,events,accepted);
    }
    private Handling handling(UUID id,List<PilotShipments.Parcel> parcels,List<Event> events,Set<UUID> accepted){
        var required=new HashSet<>(parcels.stream().map(PilotShipments.Parcel::id).toList());
        if(events.size()>1||(!events.isEmpty()&&!accepted.equals(required))||(events.isEmpty()&&!accepted.isEmpty()))
            throw new IllegalStateException("PILOT_TRACKING_INTEGRITY_REQUIRED");
        return new Handling(id,events.isEmpty()?"WAITING_FOR_HANDOVER":"AT_STATION",events.isEmpty()?0:1,
            parcels.stream().map(p->new ParcelState(p.id(),p.parcelNumber(),PilotLabelRenderer.barcode(p.id()),accepted.contains(p.id())?"AT_STATION":"NOT_ACCEPTED")).toList(),events,true,true);
    }
    private Handling projection(PilotShipments.Shipment s){return projection(s.id(),"PERSONAL",s.accountId(),null,s.parcels());}
    @Override public Handling self(ShippingDrafts.Access a,UUID shipment){return projection(owned(a,shipment));}
    @Override public Cargo lookup(CarrierAccess a,String barcode){
        a.requireCurrent();joined();UUID parcel=PilotLabelRenderer.parcelId(barcode);
        var ids=jdbc.query("SELECT s.id FROM orders.pilot_shipment s JOIN orders.pilot_parcel p ON p.shipment_id=s.id WHERE s.owner_type='PERSONAL' AND p.id=? AND s.quote_snapshot#>>'{route,dropOffCode}'=?",(r,n)->r.getObject(1,UUID.class),parcel,a.stationCode());
        if(ids.size()!=1)throw new PilotHandlingFailure(RESOURCE_NOT_FOUND);return atStation(a,ids.getFirst());
    }
    @Override public Cargo atStation(CarrierAccess a,UUID shipment){
        a.requireCurrent();joined();if(shipment==null)throw new PilotHandlingFailure(VALIDATION_FAILED);
        var owners=jdbc.query("SELECT owner_id FROM orders.pilot_shipment WHERE owner_type='PERSONAL' AND id=? AND quote_snapshot#>>'{route,dropOffCode}'=?",(r,n)->r.getObject(1,UUID.class),shipment,a.stationCode());
        if(owners.size()!=1)throw new PilotHandlingFailure(RESOURCE_NOT_FOUND);
        var s=shipments.find(owners.getFirst(),shipment);a.requireCurrent();return new Cargo(s,projection(s));
    }
    private void append(CarrierAccess a,CarrierCargo cargo,UUID acceptance,Instant occurredAt){
        UUID event=UUID.randomUUID();
        one(jdbc.update("INSERT INTO orders.pilot_tracking_event(id,shipment_id,owner_type,owner_id,organization_id,facility_id,source_acceptance_id,event_type,station_code,actor_id,occurred_at,version,environment,provenance) VALUES (?,?,?,?,?,?,?,'CARRIER_ACCEPTED_AT_STATION',?,?,?,1,?,'SIMULATION')",
            event,cargo.shipmentId(),cargo.ownerType(),cargo.accountId(),cargo.organizationId(),cargo.facilityId(),acceptance,a.stationCode(),a.actorId(),Timestamp.from(occurredAt),environment));
        for(var p:cargo.parcels())one(jdbc.update("INSERT INTO orders.pilot_parcel_event(event_id,shipment_id,parcel_id,event_type,occurred_at) VALUES (?,?,?,'CARRIER_ACCEPTED_AT_STATION',?)",event,cargo.shipmentId(),p.id(),Timestamp.from(occurredAt)));
        a.requireCurrent();
    }
    private static void acceptable(Handling handling,long expected,UUID acceptance,Instant occurredAt){
        if(expected!=handling.version())throw new PilotHandlingFailure(PRECONDITION_STALE);
        if(expected!=0||!handling.events().isEmpty())throw new PilotHandlingFailure(ALREADY_ACCEPTED);
        if(acceptance==null||occurredAt==null)throw new PilotHandlingFailure(VALIDATION_FAILED);
    }
    private CarrierCargo personal(Cargo c){
        var s=c.shipment();return new CarrierCargo(s.id(),s.code(),"PERSONAL",s.accountId(),null,null,s.quote().id(),s.totalVnd(),s.createdAt(),s.input(),s.quote().route(),s.parcels(),c.handling());
    }
    @Override public void accept(CarrierAccess a,UUID shipment,long expected,UUID acceptance,Instant occurredAt){
        var cargo=atStation(a,shipment);acceptable(cargo.handling(),expected,acceptance,occurredAt);
        if(occurredAt.isBefore(cargo.shipment().createdAt()))throw new PilotHandlingFailure(VALIDATION_FAILED);
        append(a,personal(cargo),acceptance,occurredAt);
    }
    private List<PilotShipments.Parcel> parcels(UUID shipment){
        return jdbc.query("SELECT id,parcel_number,description,quantity,weight_grams,length_mm,width_mm,height_mm,declared_value_vnd FROM orders.pilot_parcel WHERE shipment_id=? ORDER BY parcel_number",
            (r,n)->new PilotShipments.Parcel(r.getObject(1,UUID.class),r.getInt(2),new ShippingDrafts.Parcel(r.getString(3),r.getLong(4),r.getLong(5),r.getLong(6),r.getLong(7),r.getLong(8),r.getLong(9))),shipment);
    }
    private record BusinessSnapshot(UUID org,UUID facility,String code,UUID quote,long total,ShippingDrafts.Input input,
                                    com.elogistic.backend.pricing.api.PilotQuotes.Route route,Instant createdAt,List<PilotShipments.Parcel> parcels){}
    private BusinessSnapshot business(UUID id){
        var rows=jdbc.query("SELECT organization_id,facility_id,code,quote_id,total_vnd,draft_snapshot,quote_snapshot,created_at FROM orders.pilot_shipment WHERE owner_type='BUSINESS' AND id=?",
            (r,n)->{var q=JSON.readValue(r.getString("quote_snapshot"),BusinessQuotes.Quote.class);
                return new BusinessSnapshot(r.getObject("organization_id",UUID.class),r.getObject("facility_id",UUID.class),r.getString("code"),r.getObject("quote_id",UUID.class),r.getLong("total_vnd"),
                    JSON.readValue(r.getString("draft_snapshot"),ShippingDrafts.Input.class),q.route(),r.getTimestamp("created_at").toInstant(),parcels(id));},id);
        if(rows.size()!=1)throw new PilotHandlingFailure(RESOURCE_NOT_FOUND);return rows.getFirst();
    }
    @Override public CarrierCargo carrierLookup(CarrierAccess a,String barcode){
        a.requireCurrent();joined();UUID parcel=PilotLabelRenderer.parcelId(barcode);
        var ids=jdbc.query("SELECT s.id FROM orders.pilot_shipment s JOIN orders.pilot_parcel p ON p.shipment_id=s.id WHERE (s.owner_type='PERSONAL' OR (? AND s.owner_type='BUSINESS')) AND p.id=? AND s.quote_snapshot#>>'{route,dropOffCode}'=?",
            (r,n)->r.getObject(1,UUID.class),businessEnabled,parcel,a.stationCode());
        if(ids.size()!=1)throw new PilotHandlingFailure(RESOURCE_NOT_FOUND);return carrierAtStation(a,ids.getFirst());
    }
    @Override public CarrierCargo carrierAtStation(CarrierAccess a,UUID id){
        a.requireCurrent();joined();
        var kinds=jdbc.query("SELECT owner_type FROM orders.pilot_shipment WHERE id=? AND quote_snapshot#>>'{route,dropOffCode}'=?",(r,n)->r.getString(1),id,a.stationCode());
        if(kinds.size()!=1)throw new PilotHandlingFailure(RESOURCE_NOT_FOUND);
        if("PERSONAL".equals(kinds.getFirst()))return personal(atStation(a,id));
        if(!businessEnabled)throw new PilotHandlingFailure(INTAKE_POLICY_BLOCKED);
        var s=business(id);a.requireCurrent();
        return new CarrierCargo(id,s.code(),"BUSINESS",null,s.org(),s.facility(),s.quote(),s.total(),s.createdAt(),s.input(),s.route(),s.parcels(),projection(id,"BUSINESS",null,s.org(),s.parcels()));
    }
    @Override public void carrierAccept(CarrierAccess a,UUID id,long expected,UUID acceptance,Instant occurredAt){
        var cargo=carrierAtStation(a,id);acceptable(cargo.handling(),expected,acceptance,occurredAt);
        Instant created="PERSONAL".equals(cargo.ownerType())?atStation(a,id).shipment().createdAt():business(id).createdAt();
        if(occurredAt.isBefore(created))throw new PilotHandlingFailure(VALIDATION_FAILED);
        append(a,cargo,acceptance,occurredAt);
    }
    @Override public Handling businessSelf(BusinessPreparations.Access a,UUID id){
        a.requireCurrent();joined();var s=business(id);
        if(!a.scope().context().organizationId().equals(s.org())||!a.scope().permits("shipment.draft.read",s.facility()))throw new PilotHandlingFailure(RESOURCE_NOT_FOUND);
        var result=projection(id,"BUSINESS",null,s.org(),s.parcels());a.requireCurrent();return result;
    }
    /** Package-owned batch projection after each Shipment passed current Business access. */
    Map<UUID,Handling> businessProjections(List<BusinessShipments.Shipment> rows){
        joined();if(rows.isEmpty())return Map.of();
        UUID org=rows.getFirst().organizationId();var ids=rows.stream().map(BusinessShipments.Shipment::id).toList();
        if(rows.size()>20||new HashSet<>(ids).size()!=rows.size()||rows.stream().anyMatch(s->!org.equals(s.organizationId())))throw new IllegalArgumentException("BUSINESS_HANDLING_COHORT_INVALID");
        List<Object> args=new ArrayList<>();args.add(org);args.addAll(ids);String placeholders=String.join(",",Collections.nCopies(ids.size(),"?"));
        Map<UUID,List<Event>> events=new HashMap<>();Map<UUID,Set<UUID>> accepted=new HashMap<>();
        jdbc.query("SELECT shipment_id,id,event_type,station_code,occurred_at,provenance FROM orders.pilot_tracking_event WHERE owner_type='BUSINESS' AND organization_id=? AND shipment_id IN ("+placeholders+") ORDER BY occurred_at,id",r->{events.computeIfAbsent(r.getObject(1,UUID.class),x->new ArrayList<>()).add(new Event(r.getObject(2,UUID.class),r.getString(3),r.getString(4),r.getTimestamp(5).toInstant(),r.getString(6)));},args.toArray());
        jdbc.query("SELECT p.shipment_id,p.parcel_id FROM orders.pilot_parcel_event p JOIN orders.pilot_shipment s ON s.id=p.shipment_id WHERE s.owner_type='BUSINESS' AND s.organization_id=? AND p.shipment_id IN ("+placeholders+")",r->{accepted.computeIfAbsent(r.getObject(1,UUID.class),x->new HashSet<>()).add(r.getObject(2,UUID.class));},args.toArray());
        Map<UUID,Handling> result=new HashMap<>();for(var s:rows)result.put(s.id(),handling(s.id(),s.parcels(),events.getOrDefault(s.id(),List.of()),accepted.getOrDefault(s.id(),Set.of())));return Map.copyOf(result);
    }
    private static void one(int count){if(count!=1)throw new IllegalStateException("PILOT_TRACKING_WRITE_REQUIRED");}
    @Override public byte[] label(ShippingDrafts.Access a,UUID shipment){var bytes=renderer.render(owned(a,shipment));a.requireCurrent();return bytes;}
    @Override public byte[] stationLabel(CarrierAccess a,UUID shipment){var bytes=renderer.render(atStation(a,shipment).shipment());a.requireCurrent();return bytes;}
    @Override public byte[] carrierLabel(CarrierAccess a,UUID shipment){var s=carrierAtStation(a,shipment);var bytes=renderer.render(s.shipmentCode(),s.input(),s.route(),s.parcels());a.requireCurrent();return bytes;}
}
