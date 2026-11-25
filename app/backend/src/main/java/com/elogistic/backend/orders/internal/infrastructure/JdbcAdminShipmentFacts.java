package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.AdminShipmentFacts;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component @Profile("identity")
public final class JdbcAdminShipmentFacts implements AdminShipmentFacts {
    private final JdbcTemplate jdbc;
    public JdbcAdminShipmentFacts(DataSource source){jdbc=new JdbcTemplate(source);}
    @Override public List<Row> capture(Access actor,Instant from,Instant until,Instant cutoff,String status){
        actor.requireShipmentFacts();
        if(from==null||until==null||cutoff==null||!until.isAfter(from)||status!=null&&!Set.of("BOOKED","AT_STATION").contains(status))throw new IllegalArgumentException("ADMIN_QUERY_INVALID");
        var rows=jdbc.query("""
            SELECT s.id,s.code,s.owner_type,s.created_at,
             CASE WHEN EXISTS(SELECT 1 FROM orders.pilot_tracking_event e WHERE e.shipment_id=s.id
                AND e.event_type='CARRIER_ACCEPTED_AT_STATION' AND e.occurred_at<=?) THEN 'AT_STATION' ELSE 'BOOKED' END AS state,
             (SELECT count(*) FROM orders.pilot_parcel p WHERE p.shipment_id=s.id) AS parcels
            FROM orders.pilot_shipment s WHERE s.booking_state='CONFIRMED'
             AND s.created_at>=? AND s.created_at<? AND s.created_at<=?
            ORDER BY s.created_at DESC,s.id DESC LIMIT 100001
            """,(r,n)->new Row(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getTimestamp(4).toInstant(),r.getString(5),r.getInt(6)),Timestamp.from(cutoff),Timestamp.from(from),Timestamp.from(until),Timestamp.from(cutoff));
        if(rows.size()>100000||rows.stream().anyMatch(r->r.parcelCount()<1||r.parcelCount()>5||!Set.of("PERSONAL","BUSINESS").contains(r.ownerKind())))throw new IllegalStateException("ADMIN_COHORT_UNAVAILABLE");
        return rows.stream().filter(r->status==null||r.status().equals(status)).toList();
    }
}
