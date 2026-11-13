package com.elogistic.backend.finance.internal.infrastructure;

import com.elogistic.backend.finance.api.BusinessObligations;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class JdbcBusinessObligations implements BusinessObligations {
    private final DataSource source;private final JdbcTemplate jdbc;private final Clock clock;private final boolean enabled;
    public JdbcBusinessObligations(DataSource source,Clock clock,boolean enabled){this.source=source;this.jdbc=new JdbcTemplate(source);this.clock=clock;this.enabled=enabled;}
    private void current(Access a,UUID site){a.requireCurrent();a.requirePermission("shipment.draft.read",site);if(!enabled||!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("BUSINESS_OBLIGATION_ADMISSION_REQUIRED");}
    @Override public Obligation recordUnpaid(Access a,UUID site,UUID shipment,UUID quote,long amount){current(a,site);a.requirePermission("shipment.book",site);if(amount<1)throw new IllegalArgumentException("BUSINESS_OBLIGATION_AMOUNT_INVALID");if(jdbc.update("INSERT INTO finance.pilot_shipping_obligation(id,owner_type,organization_id,facility_id,creator_account_id,shipment_id,quote_id,amount_vnd,currency,state,payer,method,policy_version,request_id,created_at) VALUES (?,'BUSINESS',?,?,?,?,?,?,'VND','UNPAID','SENDER','CASH_ON_HANDOVER','BR13-BUSINESS-LOCAL-01',?,?)",UUID.randomUUID(),a.organizationId(),site,a.actorAccountId(),shipment,quote,amount,a.requestId(),Timestamp.from(clock.instant().truncatedTo(ChronoUnit.MICROS)))!=1)throw new IllegalStateException("BUSINESS_OBLIGATION_WRITE_REQUIRED");return get(a,site,shipment);}
    @Override public Obligation get(Access a,UUID site,UUID shipment){current(a,site);var rows=jdbc.query("SELECT id,organization_id,facility_id,shipment_id,quote_id,amount_vnd,currency,state,payer,method,policy_version,created_at FROM finance.pilot_shipping_obligation WHERE owner_type='BUSINESS' AND organization_id=? AND shipment_id=? AND facility_id IS NOT DISTINCT FROM ?::uuid",(r,n)->new Obligation(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getObject(3,UUID.class),r.getObject(4,UUID.class),r.getObject(5,UUID.class),r.getLong(6),r.getString(7),r.getString(8),r.getString(9),r.getString(10),r.getString(11),r.getTimestamp(12).toInstant()),a.organizationId(),shipment,site);if(rows.size()!=1)throw new IllegalStateException("BUSINESS_OBLIGATION_BINDING_REQUIRED");a.requireCurrent();return rows.getFirst();}
}
