package com.elogistic.backend.pricing.internal.infrastructure;

import com.elogistic.backend.pricing.api.*;
import com.elogistic.backend.pricing.api.BusinessQuotes.*;
import com.elogistic.backend.pricing.internal.domain.RoadTariffCalculation;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;
import static com.elogistic.backend.pricing.api.PilotQuoteFailure.Code.*;

/** Joins the workflow's Identity/Merchant transaction and existing tariff arithmetic. */
public final class JdbcBusinessQuotes implements BusinessQuotes {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private final DataSource source;private final JdbcTemplate jdbc;private final JdbcPilotQuoteStore protection;private final Clock clock;private final boolean enabled;
    public JdbcBusinessQuotes(DataSource source,JdbcPilotQuoteStore protection,Clock clock,boolean enabled){this.source=source;this.jdbc=new JdbcTemplate(source);this.protection=protection;this.clock=clock;this.enabled=enabled;}
    private static PilotQuoteFailure fail(PilotQuoteFailure.Code c){return new PilotQuoteFailure(c);}
    private UUID org(Access a){a.requireCurrent();if(!enabled)throw fail(PILOT_DISABLED);if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("BUSINESS_QUOTE_ADMISSION_REQUIRED");return a.organizationId();}
    private void permit(Access a,UUID facility){org(a);a.requirePermission("shipment.draft.read",facility);a.requirePermission("shipment.quote",facility);}
    @Override public Optional<Contract> contract(Access a){return jdbc.query("SELECT id,organization_id,version,tariff_version,state,effective_from,effective_to FROM pricing.organization_pilot_contract WHERE organization_id=? ORDER BY version DESC LIMIT 1",(r,n)->new Contract(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getLong(3),r.getString(4),r.getString(5),r.getTimestamp(6).toInstant(),r.getTimestamp(7)==null?null:r.getTimestamp(7).toInstant()),org(a)).stream().findFirst();}
    private Contract active(Access a){var c=contract(a).orElseThrow(()->fail(PILOT_DISABLED));Instant now=clock.instant();if(!c.state().equals("ACTIVE")||now.isBefore(c.effectiveFrom())||(c.effectiveTo()!=null&&!now.isBefore(c.effectiveTo())))throw fail(PILOT_DISABLED);return c;}
    @Override public PilotQuotes.Rules rules(Access a){var c=active(a);var r=protection.rules(clock.instant());if(!c.tariffVersion().equals(r.version()))throw fail(PILOT_DISABLED);return r;}
    private Quote row(ResultSet r,int ignored)throws SQLException{return new Quote(r.getObject("id",UUID.class),r.getObject("organization_id",UUID.class),r.getObject("facility_id",UUID.class),r.getObject("creator_account_id",UUID.class),r.getObject("organization_preparation_id",UUID.class),r.getLong("organization_preparation_version"),r.getObject("contract_id",UUID.class),r.getLong("contract_version"),r.getString("tariff_version"),new PilotQuotes.Route(r.getString("coverage_version"),r.getString("origin_code"),r.getString("destination_code"),r.getString("classification"),r.getString("drop_off_code")),Arrays.asList(JSON.readValue(r.getString("parcel_facts"),PilotQuotes.Parcel[].class)),Arrays.asList(JSON.readValue(r.getString("fee_lines"),PilotQuotes.Line[].class)),r.getLong("total_vnd"),"SIMULATION_NOT_TAX_INVOICE",r.getTimestamp("created_at").toInstant(),r.getTimestamp("expires_at").toInstant(),r.getObject("consumed_by",UUID.class));}
    @Override public Quote get(Access a,UUID id){var rows=jdbc.query("SELECT * FROM pricing.pilot_quote WHERE owner_type='BUSINESS' AND organization_id=? AND id=?",this::row,org(a),id);if(rows.size()!=1)throw fail(RESOURCE_NOT_FOUND);Quote q=rows.getFirst();permit(a,q.facilityId());a.requireCurrent();return q;}
    private record Receipt(UUID target,UUID facility,long expected,String kid,byte[] hash){}
    private Optional<Receipt> prior(Access a,UUID key){return jdbc.query("SELECT target_id,facility_id,expected_version,fingerprint_key_id,fingerprint FROM pricing.organization_quote_receipt WHERE organization_id=? AND actor_account_id=? AND operation_key=?",(r,n)->new Receipt(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getLong(3),r.getString(4),r.getBytes(5)),org(a),a.actorAccountId(),key).stream().findFirst();}
    private byte[] hash(Access a,String kid,Request request){return protection.fingerprint(kid,a.actorAccountId(),JSON.writeValueAsString(List.of("BUSINESS_QUOTE_V1",org(a),request)));}
    private void one(int n){if(n!=1)throw new IllegalStateException("BUSINESS_QUOTE_ATOMIC_EVIDENCE_REQUIRED");}
    @Override public Quote create(Access a,Request in,UUID key){
        if(in==null||in.preparationId()==null||in.preparationVersion()<1||key==null||in.route()==null)throw fail(VALIDATION_FAILED);permit(a,in.facilityId());
        if(!in.ordinaryGoodsConfirmed())throw fail(ORDINARY_GOODS_REQUIRED);
        var old=prior(a,key);if(old.isPresent()){var r=old.get();permit(a,r.facility());byte[] actual=hash(a,r.kid(),in);try{if(!MessageDigest.isEqual(actual,r.hash()))throw fail(IDEMPOTENCY_CONFLICT);}finally{Arrays.fill(actual,(byte)0);Arrays.fill(r.hash(),(byte)0);}return get(a,r.target());}
        var c=active(a);var rules=rules(a);List<PilotQuotes.Line> lines;
        try{lines=RoadTariffCalculation.calculate(rules,in.route().classification(),in.parcels());}catch(IllegalArgumentException invalid){throw fail(PARCEL_NOT_ELIGIBLE);}
        long total=0;for(var l:lines)total=Math.addExact(total,l.feeVnd());Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);UUID target=UUID.randomUUID();var route=in.route();String kid=protection.activeKey();byte[] hash=hash(a,kid,in);
        try{
            one(jdbc.update("INSERT INTO pricing.pilot_quote(id,owner_type,organization_id,facility_id,creator_account_id,organization_preparation_id,organization_preparation_version,draft_version,contract_id,contract_version,tariff_version,coverage_version,origin_code,destination_code,classification,drop_off_code,parcel_facts,fee_lines,total_vnd,created_at,expires_at,operation_key,fingerprint_key_id,fingerprint,ordinary_goods_confirmed) VALUES (?,'BUSINESS',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?::jsonb,?,?,?,?,?,?,TRUE)",target,org(a),in.facilityId(),a.actorAccountId(),in.preparationId(),in.preparationVersion(),in.preparationVersion(),c.id(),c.version(),rules.version(),route.datasetVersion(),route.originCode(),route.destinationCode(),route.classification(),route.dropOffCode(),JSON.writeValueAsString(in.parcels()),JSON.writeValueAsString(lines),total,Timestamp.from(now),Timestamp.from(now.plusSeconds(rules.ttlSeconds())),key,kid,hash));
            one(jdbc.update("INSERT INTO pricing.organization_quote_receipt VALUES(?,?,?,?,?,?,?,?,?)",org(a),a.actorAccountId(),key,in.facilityId(),target,in.preparationVersion(),kid,hash,Timestamp.from(now)));
        }finally{Arrays.fill(hash,(byte)0);}a.meaningfulAction();return get(a,target);
    }
    @Override public UUID receipt(Access a,UUID key){var r=prior(a,key).orElseThrow(()->fail(RESOURCE_NOT_FOUND));permit(a,r.facility());return get(a,r.target()).id();}
    @Override public boolean usable(Access a,Quote q){permit(a,q.facilityId());var c=contract(a);Instant now=clock.instant();return q.consumedBy()==null&&!now.isBefore(q.createdAt())&&now.isBefore(q.expiresAt())&&c.isPresent()&&c.get().state().equals("ACTIVE")&&c.get().id().equals(q.contractId())&&c.get().version()==q.contractVersion()&&!now.isBefore(c.get().effectiveFrom())&&(c.get().effectiveTo()==null||now.isBefore(c.get().effectiveTo()));}
    @Override public Quote consume(Access a,UUID id,UUID preparation,long version,UUID shipment){
        Quote q=get(a,id);a.requirePermission("shipment.book",q.facilityId());if(!q.creatorAccountId().equals(a.actorAccountId()))throw fail(RESOURCE_NOT_FOUND);
        if(!q.preparationId().equals(preparation)||q.preparationVersion()!=version)throw fail(PRECONDITION_STALE);
        if(q.consumedBy()!=null)throw fail(QUOTE_CONSUMED);if(!usable(a,q)||!rules(a).version().equals(q.tariffVersion()))throw fail(QUOTE_EXPIRED);
        Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);if(jdbc.update("UPDATE pricing.pilot_quote SET consumed_by=?,consumed_at=? WHERE owner_type='BUSINESS' AND organization_id=? AND id=? AND consumed_by IS NULL AND expires_at>?",shipment,Timestamp.from(now),org(a),id,Timestamp.from(now))!=1)throw fail(QUOTE_CONSUMED);
        return get(a,id);
    }
}
