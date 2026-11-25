package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.*;
import java.sql.Timestamp;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** No Customer/label/POD endpoint reuse and no sender/recipient/contact/financial payload SQL. */
@Component @Profile("identity")
public final class JdbcAdminOversightReads implements AdminOversightReads {
    private final DataSource source;private final JdbcTemplate jdbc;private final AdminShipmentFacts facts;
    public JdbcAdminOversightReads(DataSource source){this.source=source;jdbc=new JdbcTemplate(source);facts=new JdbcAdminShipmentFacts(source);}
    @Override public Facts get(Access actor,UUID id,Selector selector){
        actor.requireOversightFacts();
        if(!TransactionSynchronizationManager.hasResource(source)||!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.isCurrentTransactionReadOnly())throw new AdminOversightReads.Failure("ACCESS_DENIED");
        if(id==null||selector==null||!selector.contributingIds().contains(id))throw new AdminOversightReads.Failure("RESOURCE_NOT_FOUND");
        var row=facts.capture(actor::requireOversightFacts,selector.from(),selector.until(),selector.cutoff(),selector.status()).stream().filter(r->r.id().equals(id)).findFirst().orElseThrow(()->new AdminOversightReads.Failure("RESOURCE_NOT_FOUND"));
        var parcels=jdbc.query("SELECT id,weight_grams,length_mm,width_mm,height_mm FROM orders.pilot_parcel WHERE shipment_id=? ORDER BY parcel_number LIMIT 6",
            (r,n)->new Parcel(r.getObject(1,UUID.class),PilotLabelRenderer.barcode(r.getObject(1,UUID.class)),r.getLong(2),r.getLong(3),r.getLong(4),r.getLong(5)),id);
        var events=jdbc.query("SELECT id,event_type,occurred_at,station_code,source_acceptance_id FROM orders.pilot_tracking_event WHERE shipment_id=? AND occurred_at<=? ORDER BY occurred_at,id LIMIT 2",
            (r,n)->new AbstractMap.SimpleImmutableEntry<>(new Event(r.getObject(1,UUID.class),r.getString(2),r.getTimestamp(3).toInstant()),new Acceptance(r.getObject(5,UUID.class),r.getTimestamp(3).toInstant(),r.getString(4))),id,Timestamp.from(selector.cutoff()));
        if(parcels.size()!=row.parcelCount()||parcels.isEmpty()||parcels.size()>5||events.size()>1)throw new AdminOversightReads.Failure("SERVICE_UNAVAILABLE");
        Acceptance acceptance=null;
        if(!events.isEmpty()){
            var event=events.getFirst().getKey();if(!event.kind().equals("CARRIER_ACCEPTED_AT_STATION")||!row.status().equals("AT_STATION"))throw new AdminOversightReads.Failure("SERVICE_UNAVAILABLE");
            var actual=new HashSet<>(jdbc.query("SELECT parcel_id FROM orders.pilot_parcel_event WHERE event_id=? AND shipment_id=?",(r,n)->r.getObject(1,UUID.class),event.id(),id));
            if(!actual.equals(new HashSet<>(parcels.stream().map(Parcel::id).toList())))throw new AdminOversightReads.Failure("SERVICE_UNAVAILABLE");
            acceptance=events.getFirst().getValue();
        }else if(!row.status().equals("BOOKED"))throw new AdminOversightReads.Failure("SERVICE_UNAVAILABLE");
        actor.requireOversightFacts();return new Facts(row,parcels,events.stream().map(Map.Entry::getKey).toList(),acceptance);
    }
}
