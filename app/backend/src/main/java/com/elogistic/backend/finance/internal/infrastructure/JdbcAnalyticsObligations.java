package com.elogistic.backend.finance.internal.infrastructure;

import com.elogistic.backend.finance.api.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class JdbcAnalyticsObligations implements AnalyticsObligations {
    private final DataSource source;
    private final JdbcTemplate jdbc;
    public JdbcAnalyticsObligations(DataSource source){this.source=source;this.jdbc=new JdbcTemplate(source);}
    @Override public List<Fact> read(PilotObligations.Access access,List<UUID> ids){
        access.requireCurrent();
        if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("ANALYTICS_ADMISSION_REQUIRED");
        if(ids==null||ids.size()>10000||ids.stream().anyMatch(Objects::isNull)||new HashSet<>(ids).size()!=ids.size())throw new IllegalArgumentException("ANALYTICS_BOUND_REQUIRED");
        if(ids.isEmpty())return List.of();
        var result=jdbc.query(connection->{
            var statement=connection.prepareStatement("SELECT shipment_id,quote_id,amount_vnd,currency,state FROM finance.pilot_shipping_obligation WHERE owner_id=? AND shipment_id=ANY(?)");
            statement.setObject(1,access.accountId());statement.setArray(2,connection.createArrayOf("uuid",ids.toArray()));return statement;
        },(r,n)->new Fact(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getLong(3),r.getString(4),r.getString(5)));
        access.requireCurrent();return List.copyOf(result);
    }
}
