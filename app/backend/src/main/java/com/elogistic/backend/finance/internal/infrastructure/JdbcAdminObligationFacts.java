package com.elogistic.backend.finance.internal.infrastructure;

import com.elogistic.backend.finance.api.AdminObligationFacts;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component @Profile("identity")
public final class JdbcAdminObligationFacts implements AdminObligationFacts {
    private final JdbcTemplate jdbc;
    public JdbcAdminObligationFacts(DataSource source){jdbc=new JdbcTemplate(source);}
    @Override public List<Row> capture(Access actor,Set<UUID> shipments){
        actor.requireFinanceFacts();if(shipments==null||shipments.size()>100000||shipments.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("ADMIN_QUERY_INVALID");
        if(shipments.isEmpty())return List.of();var all=new ArrayList<Row>();var ids=new ArrayList<>(shipments);
        for(int first=0;first<ids.size();first+=1000){
            var chunk=ids.subList(first,Math.min(first+1000,ids.size()));
            all.addAll(jdbc.query(connection->{var q=connection.prepareStatement("SELECT shipment_id,amount_vnd,state FROM finance.pilot_shipping_obligation WHERE shipment_id=ANY(?) AND currency='VND'");q.setArray(1,connection.createArrayOf("uuid",chunk.toArray()));return q;},(r,n)->new Row(r.getObject(1,UUID.class),r.getBigDecimal(2).toBigIntegerExact(),r.getString(3))));
        }
        if(all.size()!=shipments.size()||all.stream().anyMatch(r->r.amountVnd().signum()<=0||!"UNPAID".equals(r.state()))||all.stream().map(Row::shipmentId).distinct().count()!=all.size())throw new IllegalStateException("ADMIN_OBLIGATION_UNAVAILABLE");
        return List.copyOf(all);
    }
}
