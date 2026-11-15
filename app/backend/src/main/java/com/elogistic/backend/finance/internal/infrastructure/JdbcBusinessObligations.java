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
    @Override public void validateCaptured(Access a,List<Captured> rows){
        a.requireCurrent();if(rows==null||rows.isEmpty()||rows.size()>1000||rows.stream().map(Captured::shipmentId).distinct().count()!=rows.size())throw new IllegalArgumentException("BUSINESS_FINANCE_CAPTURE_INVALID");
        rows.stream().map(Captured::facilityId).distinct().forEach(site->current(a,site));
        List<Object> args=new ArrayList<>();args.add(a.organizationId());rows.forEach(r->args.add(r.shipmentId()));
        var found=jdbc.query("SELECT shipment_id,quote_id,facility_id,amount_vnd,currency,state,payer,method,policy_version FROM finance.pilot_shipping_obligation WHERE owner_type='BUSINESS' AND organization_id=? AND shipment_id IN ("+String.join(",",Collections.nCopies(rows.size(),"?"))+")",(r,n)->{
            if(!"VND".equals(r.getString(5))||!"UNPAID".equals(r.getString(6))||!"SENDER".equals(r.getString(7))||!"CASH_ON_HANDOVER".equals(r.getString(8))||!"BR13-BUSINESS-LOCAL-01".equals(r.getString(9)))throw new IllegalStateException("BUSINESS_OBLIGATION_BINDING_REQUIRED");
            return new Captured(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getObject(3,UUID.class),r.getLong(4));
        },args.toArray());
        if(found.size()!=rows.size()||!new HashSet<>(found).equals(new HashSet<>(rows)))throw new IllegalStateException("BUSINESS_OBLIGATION_BINDING_REQUIRED");a.requireCurrent();
    }
}
