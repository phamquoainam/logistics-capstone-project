package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.orders.api.PersonalShipmentManagement.*;
import com.elogistic.backend.orders.internal.application.port.PilotShipmentStore;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;
import static com.elogistic.backend.orders.api.PilotBookingFailure.Code.*;

/** One materialized cohort captures counts and page status before immutable hydration. */
public final class JdbcPersonalShipmentManagement implements PersonalShipmentManagement {
    private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final Set<String> KEYS=Set.of("view","page","size","search","recipient","from","until","status","service","origin","destination","obligation","sort");
    private final DataSource source;
    private final JdbcTemplate jdbc;
    private final PilotShipmentStore shipments;
    private final Clock clock;
    private final boolean enabled;
    public JdbcPersonalShipmentManagement(DataSource source,PilotShipmentStore shipments,Clock clock,boolean enabled){this.source=source;this.jdbc=new JdbcTemplate(source);this.shipments=shipments;this.clock=clock;this.enabled=enabled;}
    private static PilotBookingFailure invalid(){return new PilotBookingFailure(VALIDATION_FAILED);}
    private static String option(Map<String,String> p,String key,String fallback,Set<String> allowed){String value=p.getOrDefault(key,fallback);if(!allowed.contains(value))throw invalid();return value;}
    private static String search(Map<String,String> p,String key){String value=p.get(key);if(value==null)return null;if(value.isBlank()||value.length()>160||value.codePoints().anyMatch(Character::isISOControl))throw invalid();return "%"+value.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";}
    private static LocalDate date(String value){if(!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw invalid();return LocalDate.parse(value);}
    private record Captured(Summary summary,String total,List<Identity> rows){}
    private record Identity(UUID id,String status){}
    @Override public Page read(ShippingDrafts.Access access,Map<String,String> p){
        access.requireCurrent();if(!enabled)throw new PilotBookingFailure(PILOT_DISABLED);
        if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("MANAGEMENT_ADMISSION_TRANSACTION_REQUIRED");
        if(!KEYS.containsAll(p.keySet())||p.values().stream().anyMatch(Objects::isNull)||!"management".equals(p.get("view")))throw invalid();
        String rawPage=p.getOrDefault("page","0");if(!rawPage.matches("0|[1-9][0-9]{0,2}"))throw invalid();
        int page=Integer.parseInt(rawPage);if(page>500||!"20".equals(p.getOrDefault("size","20")))throw invalid();
        String status=option(p,"status","",Set.of("","WAITING_FOR_HANDOVER","AT_STATION"));
        String sort=option(p,"sort","NEWEST",Set.of("NEWEST","OLDEST","FEE_DESC","FEE_ASC"));
        String service=option(p,"service","",Set.of("","ROAD_STANDARD"));
        option(p,"obligation","",Set.of("","UNPAID"));
        String origin=p.getOrDefault("origin",""),destination=p.getOrDefault("destination","");
        if(p.containsKey("origin")!=p.containsKey("destination")||p.containsKey("origin")&&!Set.of("SIM-A1/SIM-A2","SIM-A2/SIM-A1","SIM-A1/SIM-B1","SIM-B1/SIM-A1").contains(origin+"/"+destination))throw invalid();
        Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS),end=now.plusNanos(1000),from=null;
        if(p.containsKey("from")!=p.containsKey("until"))throw invalid();
        if(p.containsKey("from"))try{
            var first=date(p.get("from"));var last=date(p.get("until"));long days=ChronoUnit.DAYS.between(first,last);
            if(days<1||days>366||last.isAfter(now.atZone(ZONE).toLocalDate().plusDays(1)))throw invalid();
            from=first.atStartOfDay(ZONE).toInstant();Instant limit=last.atStartOfDay(ZONE).toInstant();if(limit.isBefore(end))end=limit;
        }catch(DateTimeException malformed){throw invalid();}
        String find=search(p,"search"),recipient=search(p,"recipient");
        List<Object> args=new ArrayList<>();args.add(Timestamp.from(now));args.add(access.accountId());args.add(Timestamp.from(end));
        StringBuilder where=new StringBuilder("s.owner_id=? AND s.owner_type='PERSONAL' AND s.booking_state='CONFIRMED' AND s.created_at<?");
        if(from!=null){where.append(" AND s.created_at>=?");args.add(Timestamp.from(from));}
        if(find!=null){where.append(" AND lower(concat_ws(' ',s.code,s.draft_snapshot->>'reference',s.draft_snapshot#>>'{sender,name}',s.draft_snapshot#>>'{sender,phone}',s.draft_snapshot#>>'{recipient,name}',s.draft_snapshot#>>'{recipient,phone}')) LIKE ? ESCAPE '!'");args.add(find);}
        if(recipient!=null){where.append(" AND lower(concat_ws(' ',s.draft_snapshot#>>'{recipient,name}',s.draft_snapshot#>>'{recipient,phone}')) LIKE ? ESCAPE '!'");args.add(recipient);}
        if(!service.isEmpty())where.append(" AND s.quote_snapshot->>'tariffVersion'='ARGO-ROAD-TARIFF-1'");
        if(!origin.isEmpty()){where.append(" AND s.quote_snapshot#>>'{route,originCode}'=? AND s.quote_snapshot#>>'{route,destinationCode}'=?");args.add(origin);args.add(destination);}
        String selected=status.isEmpty()?"true":status.equals("AT_STATION")?"accepted":"NOT accepted";
        String ordering=switch(sort){case "OLDEST"->"created_at ASC,id ASC";case "FEE_DESC"->"total_vnd DESC,id ASC";case "FEE_ASC"->"total_vnd ASC,id ASC";default->"created_at DESC,id ASC";};
        String sql="""
            WITH base AS MATERIALIZED (
              SELECT s.id,s.created_at,s.total_vnd,
                EXISTS(SELECT 1 FROM orders.pilot_tracking_event e WHERE e.shipment_id=s.id
                  AND e.owner_id=s.owner_id AND e.event_type='CARRIER_ACCEPTED_AT_STATION'
                  AND e.occurred_at<=?) accepted
              FROM orders.pilot_shipment s WHERE %s
            ), selected AS (SELECT * FROM base WHERE %s), page AS (
              SELECT id,accepted,row_number() OVER (ORDER BY %s) ordinal FROM selected
              ORDER BY %s LIMIT 21 OFFSET ?
            )
            SELECT (SELECT count(*)::text FROM base) total_shipments,
                   (SELECT count(*)::text FROM base WHERE NOT accepted) waiting,
                   (SELECT count(*)::text FROM base WHERE accepted) at_station,
                   (SELECT count(*)::text FROM selected) filtered_total,
                   coalesce((SELECT jsonb_agg(jsonb_build_object('id',id,'status',
                     CASE WHEN accepted THEN 'AT_STATION' ELSE 'WAITING_FOR_HANDOVER' END)
                     ORDER BY ordinal) FROM page),'[]'::jsonb)::text page_rows
            """.formatted(where,selected,ordering,ordering);
        args.add(page*20);
        var captured=jdbc.queryForObject(sql,(r,n)->{
            List<Identity> rows=new ArrayList<>();
            for(var item:JSON.readTree(r.getString("page_rows")))rows.add(new Identity(UUID.fromString(item.get("id").asString()),item.get("status").asString()));
            return new Captured(new Summary(r.getString("total_shipments"),r.getString("waiting"),r.getString("at_station")),r.getString("filtered_total"),List.copyOf(rows));
        },args.toArray());
        if(captured==null)throw new IllegalStateException("MANAGEMENT_COHORT_REQUIRED");
        var items=captured.rows().stream().limit(20).map(row->new Item(shipments.find(access.accountId(),row.id()),row.status())).toList();
        access.requireCurrent();return new Page(now,captured.summary(),captured.total(),page,captured.rows().size()>20,items);
    }
}
