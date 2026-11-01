package com.elogistic.backend.pricing.internal.infrastructure;
import com.elogistic.backend.pricing.api.*;
import com.elogistic.backend.pricing.api.PilotQuotes.*;
import com.elogistic.backend.pricing.internal.application.port.PilotQuoteStore;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;
public final class JdbcPilotQuoteStore implements PilotQuoteStore {
    private static final JsonMapper JSON=JsonMapper.builder().build();private final DataSource source;private final JdbcTemplate jdbc;private final Map<String,SecretKeySpec> keys;private final String active;
    public JdbcPilotQuoteStore(DataSource source,Map<String,byte[]> supplied,String active){this.source=source;jdbc=new JdbcTemplate(source);if(supplied==null||supplied.isEmpty()||supplied.size()>16||!supplied.containsKey(active))throw new IllegalArgumentException("PILOT_QUOTE_KEYS_INVALID");Map<String,SecretKeySpec> copy=new HashMap<>();supplied.forEach((id,key)->{if(id==null||!id.matches("[A-Za-z0-9_-]{1,64}")||key==null||key.length!=32||Arrays.equals(key,new byte[32]))throw new IllegalArgumentException("PILOT_QUOTE_KEYS_INVALID");copy.put(id,new SecretKeySpec(key,"HmacSHA256"));});keys=Map.copyOf(copy);this.active=active;}
    private void joined(){if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("PILOT_QUOTE_ADMISSION_TRANSACTION_REQUIRED");}
    @Override public String canonical(Request input){return JSON.writeValueAsString(input);}
    @Override public String activeKey(){return active;}
    @Override public byte[] fingerprint(String kid,UUID owner,String input){if(owner==null||input==null||!keys.containsKey(kid))throw new IllegalArgumentException("PILOT_QUOTE_FINGERPRINT_INVALID");try{var mac=Mac.getInstance("HmacSHA256");mac.init(keys.get(kid));for(String v:List.of("pricing-pilot-quote-v1",kid,owner.toString(),input)){byte[] bytes=v.getBytes(StandardCharsets.UTF_8);try{mac.update(ByteBuffer.allocate(4).putInt(bytes.length).array());mac.update(bytes);}finally{Arrays.fill(bytes,(byte)0);}}return mac.doFinal();}catch(java.security.GeneralSecurityException failure){throw new IllegalStateException("PILOT_QUOTE_PROTECTION_UNAVAILABLE");}}
    @Override public void lockOwner(UUID owner){joined();jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS text),730020))",(r,n)->0,owner.toString());}
    @Override public Rules rules(Instant now){joined();var values=jdbc.query("SELECT version,intra_first_vnd,intra_step_vnd,inter_first_vnd,inter_step_vnd,step_grams,volumetric_divisor,max_parcels,max_actual_grams,max_dimension_mm,max_girth_mm,quote_ttl_seconds,tax_treatment FROM pricing.pilot_tariff_version WHERE version='ARGO-ROAD-TARIFF-1' AND effective_from<=? AND (effective_to IS NULL OR effective_to>?)",(r,n)->new Rules(r.getString(1),r.getLong(2),r.getLong(3),r.getLong(4),r.getLong(5),r.getInt(6),r.getInt(7),r.getInt(8),r.getInt(9),r.getInt(10),r.getInt(11),r.getInt(12),r.getString(13)),Timestamp.from(now),Timestamp.from(now));if(values.size()!=1)throw new PilotQuoteFailure(PilotQuoteFailure.Code.PILOT_DISABLED);return values.getFirst();}
    private static Quote row(ResultSet r,int ignored)throws SQLException{try{return new Quote(r.getObject("id",UUID.class),r.getObject("owner_id",UUID.class),r.getObject("draft_id",UUID.class),r.getLong("draft_version"),r.getString("tariff_version"),new Route(r.getString("coverage_version"),r.getString("origin_code"),r.getString("destination_code"),r.getString("classification"),r.getString("drop_off_code")),Arrays.asList(JSON.readValue(r.getString("parcel_facts"),Parcel[].class)),Arrays.asList(JSON.readValue(r.getString("fee_lines"),Line[].class)),r.getLong("total_vnd"),"SIMULATION_NOT_TAX_INVOICE",r.getTimestamp("created_at").toInstant(),r.getTimestamp("expires_at").toInstant(),r.getObject("consumed_by",UUID.class));}catch(RuntimeException invalid){throw new IllegalStateException("PILOT_QUOTE_SNAPSHOT_INVALID");}}
    @Override public Optional<Stored> byKey(UUID owner,UUID key){joined();return jdbc.query("SELECT * FROM pricing.pilot_quote WHERE owner_id=? AND operation_key=?",(r,n)->new Stored(row(r,n),r.getString("fingerprint_key_id"),r.getBytes("fingerprint")),owner,key).stream().findFirst();}
    @Override public Optional<Quote> find(UUID owner,UUID id,boolean lock){joined();return jdbc.query("SELECT * FROM pricing.pilot_quote WHERE owner_id=? AND id=?"+(lock?" FOR UPDATE":""),JdbcPilotQuoteStore::row,owner,id).stream().findFirst();}
    @Override public void insert(Quote q,UUID key,String kid,byte[] hash){joined();var r=q.route();if(jdbc.update("INSERT INTO pricing.pilot_quote(id,owner_id,draft_id,draft_version,tariff_version,coverage_version,origin_code,destination_code,classification,drop_off_code,parcel_facts,fee_lines,total_vnd,created_at,expires_at,operation_key,fingerprint_key_id,fingerprint,ordinary_goods_confirmed) VALUES (?,?,?,?,?,?,?,?,?,?,CAST(? AS jsonb),CAST(? AS jsonb),?,?,?,?,?,?,TRUE)",q.id(),q.accountId(),q.draftId(),q.draftVersion(),q.tariffVersion(),r.datasetVersion(),r.originCode(),r.destinationCode(),r.classification(),r.dropOffCode(),JSON.writeValueAsString(q.parcels()),JSON.writeValueAsString(q.feeLines()),q.totalVnd(),Timestamp.from(q.createdAt()),Timestamp.from(q.expiresAt()),key,kid,hash)!=1)throw new IllegalStateException("PILOT_QUOTE_WRITE_REQUIRED");}
    @Override public void consume(UUID owner,UUID id,UUID shipment,Instant now){joined();if(jdbc.update("UPDATE pricing.pilot_quote SET consumed_by=?,consumed_at=? WHERE owner_id=? AND id=? AND consumed_by IS NULL AND expires_at>?",shipment,Timestamp.from(now),owner,id,Timestamp.from(now))!=1)throw new PilotQuoteFailure(PilotQuoteFailure.Code.QUOTE_CONSUMED);}
    @Override public String toString(){return "JdbcPilotQuoteStore[redacted]";}
}
