package com.elogistic.backend.finance.internal.infrastructure;
import com.elogistic.backend.finance.api.PilotObligations;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
public final class JdbcPilotObligations implements PilotObligations {
    private final DataSource source;private final JdbcTemplate jdbc;private final Clock clock;private final boolean enabled;
    public JdbcPilotObligations(DataSource source,Clock clock,boolean enabled){this.source=source;this.clock=clock;this.enabled=enabled;jdbc=new JdbcTemplate(source);}
    private void current(Access a){a.requireCurrent();if(!enabled||!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("PILOT_OBLIGATION_ADMISSION_REQUIRED");}
    @Override public Obligation recordUnpaid(Access a,UUID shipment,UUID quote,long amount){current(a);if(shipment==null||quote==null||amount<1)throw new IllegalArgumentException("PILOT_OBLIGATION_INVALID");UUID id=UUID.randomUUID();Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);if(jdbc.update("INSERT INTO finance.pilot_shipping_obligation(id,owner_id,shipment_id,quote_id,amount_vnd,currency,state,payer,method,policy_version,request_id,created_at) VALUES (?,?,?,?,?,'VND','UNPAID','SENDER','CASH_ON_HANDOVER','BR13-LOCAL-PILOT-01',?,?)",id,a.accountId(),shipment,quote,amount,a.requestId(),java.sql.Timestamp.from(now))!=1)throw new IllegalStateException("PILOT_OBLIGATION_WRITE_REQUIRED");return get(a,shipment);}
    @Override public Obligation get(Access a,UUID shipment){current(a);var rows=jdbc.query("SELECT id,owner_id,shipment_id,quote_id,amount_vnd,currency,state,payer,method,policy_version,created_at FROM finance.pilot_shipping_obligation WHERE owner_id=? AND shipment_id=?",(r,n)->new Obligation(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getObject(3,UUID.class),r.getObject(4,UUID.class),r.getLong(5),r.getString(6),r.getString(7),r.getString(8),r.getString(9),r.getString(10),r.getTimestamp(11).toInstant()),a.accountId(),shipment);if(rows.size()!=1)throw new IllegalStateException("PILOT_OBLIGATION_REQUIRED");a.requireCurrent();return rows.getFirst();}
}
