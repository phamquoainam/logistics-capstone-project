package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class JdbcAnalyticsShipments implements AnalyticsShipments {
    private final DataSource source;
    private final JdbcTemplate jdbc;
    public JdbcAnalyticsShipments(DataSource source){this.source=source;this.jdbc=new JdbcTemplate(source);}
    @Override public List<Fact> read(ShippingDrafts.Access access,Instant from,Instant until){
        access.requireCurrent();
        if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("ANALYTICS_ADMISSION_REQUIRED");
        if(from==null||until==null||until.isBefore(from))throw new IllegalArgumentException("ANALYTICS_PERIOD_REQUIRED");
        var rows=jdbc.query("""
            SELECT s.id,s.code,s.quote_id,s.created_at,s.total_vnd,
                   s.quote_snapshot#>>'{route,originCode}' origin,
                   s.quote_snapshot#>>'{route,destinationCode}' destination,
                   s.quote_snapshot#>>'{route,datasetVersion}' dataset,
                   s.quote_snapshot->>'tariffVersion' tariff,p.weights,q.actual_weights,q.chargeable_weights,e.occurred_at
            FROM orders.pilot_shipment s
            JOIN LATERAL (SELECT array_agg(weight_grams ORDER BY parcel_number) weights
                          FROM orders.pilot_parcel WHERE shipment_id=s.id) p ON true
            JOIN LATERAL (SELECT array_agg((line->>'actualGrams')::bigint ORDER BY (line->>'parcelNumber')::int) actual_weights,
                                 array_agg((line->>'chargeableGrams')::bigint ORDER BY (line->>'parcelNumber')::int) chargeable_weights
                          FROM jsonb_array_elements(s.quote_snapshot->'feeLines') line) q ON true
            LEFT JOIN orders.pilot_tracking_event e ON e.shipment_id=s.id AND e.owner_id=s.owner_id
                  AND e.event_type='CARRIER_ACCEPTED_AT_STATION'
            WHERE s.owner_id=? AND s.owner_type='PERSONAL' AND s.booking_state='CONFIRMED'
                  AND s.created_at>=? AND s.created_at<?
            ORDER BY s.created_at,s.id LIMIT 10001
            """,(r,n)->{
                if(!"ARGO-ROAD-TARIFF-1".equals(r.getString("tariff")))throw new IllegalStateException("ANALYTICS_SERVICE_SOURCE_REQUIRED");
                var array=r.getArray("weights");
                if(array==null)throw new IllegalStateException("ANALYTICS_PARCELS_REQUIRED");
                List<Long> weights;
                try{weights=Arrays.stream((Object[])array.getArray()).map(v->((Number)v).longValue()).toList();}finally{array.free();}
                if(weights.isEmpty()||weights.size()>5)throw new IllegalStateException("ANALYTICS_PARCELS_REQUIRED");
                java.sql.Array actual=r.getArray("actual_weights"),chargeable=r.getArray("chargeable_weights");
                if(actual==null||chargeable==null)throw new IllegalStateException("ANALYTICS_QUOTE_WEIGHTS_REQUIRED");
                List<Long> quoteActual,billable;
                try{quoteActual=Arrays.stream((Object[])actual.getArray()).map(v->((Number)v).longValue()).toList();billable=Arrays.stream((Object[])chargeable.getArray()).map(v->((Number)v).longValue()).toList();}finally{actual.free();chargeable.free();}
                if(!quoteActual.equals(weights)||billable.size()!=weights.size())throw new IllegalStateException("ANALYTICS_QUOTE_PARCEL_BINDING_REQUIRED");
                var accepted=r.getTimestamp("occurred_at");
                return new Fact(r.getObject("id",UUID.class),r.getString("code"),r.getObject("quote_id",UUID.class),r.getTimestamp("created_at").toInstant(),r.getLong("total_vnd"),r.getString("origin"),r.getString("destination"),r.getString("dataset"),accepted==null?null:accepted.toInstant(),weights,billable);
            },access.accountId(),Timestamp.from(from),Timestamp.from(until));
        access.requireCurrent();return List.copyOf(rows);
    }
}
