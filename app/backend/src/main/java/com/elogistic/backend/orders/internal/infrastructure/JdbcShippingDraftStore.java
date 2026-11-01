package com.elogistic.backend.orders.internal.infrastructure;
import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.orders.api.ShippingDrafts.*;
import com.elogistic.backend.orders.internal.application.port.ShippingDraftStore;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

public final class JdbcShippingDraftStore implements ShippingDraftStore {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final String SELECT="SELECT d.id,d.owner_id,d.state,d.version,d.created_at,d.updated_at,v.payload::text FROM orders.shipping_draft d JOIN orders.shipping_draft_version v ON (v.id,v.version,v.owner_id)=(d.id,d.version,d.owner_id)";
    private final DataSource source;private final JdbcTemplate jdbc;
    public JdbcShippingDraftStore(DataSource source){this.source=Objects.requireNonNull(source);jdbc=new JdbcTemplate(source);}
    public static void requireSchema(DataSource source){try(var c=source.getConnection();var s=c.createStatement();var r=s.executeQuery("SELECT current_setting('server_version_num')::int>=180000 AND EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='orders.shipping_draft'::regclass AND conname='fk_shipping_draft_current' AND condeferrable AND condeferred AND convalidated) AND EXISTS(SELECT 1 FROM pg_trigger WHERE tgrelid='orders.shipping_draft'::regclass AND tgname='guard_shipping_draft' AND tgenabled='O')")){if(!r.next()||!r.getBoolean(1))throw new IllegalStateException();}catch(Exception unavailable){throw new IllegalStateException("SHIPPING_DRAFT_POSTGRES18_V19_REQUIRED");}}
    private void joined(){if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("SHIPPING_DRAFT_ADMISSION_TRANSACTION_REQUIRED");}
    private static Draft row(ResultSet r,int ignored)throws SQLException{try{return new Draft(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getString(3),JSON.readValue(r.getString(7),Input.class),r.getLong(4),r.getTimestamp(5).toInstant(),r.getTimestamp(6).toInstant());}catch(RuntimeException invalid){throw new IllegalStateException("SHIPPING_DRAFT_SNAPSHOT_INVALID");}}
    @Override public String canonical(Input input){return JSON.writeValueAsString(input);}
    @Override public void lockOwner(UUID owner){joined();jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS text),730019))",(r,n)->0,owner.toString());}
    @Override public Optional<Draft> find(UUID owner,UUID id){joined();return jdbc.query(SELECT+" WHERE d.owner_id=? AND d.id=?",JdbcShippingDraftStore::row,owner,id).stream().findFirst();}
    @Override public Draft version(UUID owner,UUID id,long version){joined();return jdbc.query("SELECT id,owner_id,state,version,created_at,updated_at,payload::text FROM orders.shipping_draft_version WHERE owner_id=? AND id=? AND version=?",JdbcShippingDraftStore::row,owner,id,version).stream().findFirst().orElseThrow(()->new IllegalStateException("SHIPPING_DRAFT_HISTORY_REQUIRED"));}
    @Override public List<Draft> list(UUID owner,int page,String search){joined();String sql=SELECT+" WHERE d.owner_id=? AND d.state='DRAFT'";List<Object> args=new ArrayList<>();args.add(owner);if(search!=null){sql+=" AND lower(concat_ws(' ',v.payload->>'reference',v.payload#>>'{sender,name}',v.payload#>>'{recipient,name}')) LIKE ? ESCAPE '!'";args.add("%"+search.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");}sql+=" ORDER BY d.updated_at DESC,d.id ASC LIMIT 21 OFFSET ?";args.add(Math.multiplyExact((long)page,20));return jdbc.query(sql,JdbcShippingDraftStore::row,args.toArray());}
    @Override public Optional<StoredReceipt> receipt(UUID owner,String operation,UUID key){joined();return jdbc.query("SELECT target_id,original_version,result_version,fingerprint_key_id,fingerprint FROM orders.shipping_draft_receipt WHERE owner_id=? AND operation=? AND operation_key=?",(r,n)->new StoredReceipt(r.getObject(1,UUID.class),r.getLong(2),r.getLong(3),r.getString(4),r.getBytes(5)),owner,operation,key).stream().findFirst();}
    private static void one(int changed){if(changed!=1)throw new IllegalStateException("SHIPPING_DRAFT_MANDATORY_EVIDENCE_REQUIRED");}
    @Override public void save(Draft before,Draft after,String operation,UUID request){
        joined();Timestamp created=Timestamp.from(after.createdAt()),updated=Timestamp.from(after.updatedAt());
        if(before==null)one(jdbc.update("INSERT INTO orders.shipping_draft(id,owner_type,owner_id,state,version,created_at,updated_at) VALUES (?,'PERSONAL',?,?,?,?,?)",after.id(),after.accountId(),after.state(),after.version(),created,updated));
        else if(jdbc.update("UPDATE orders.shipping_draft SET state=?,version=?,updated_at=? WHERE id=? AND owner_id=? AND version=? AND state='DRAFT'",after.state(),after.version(),updated,after.id(),after.accountId(),before.version())!=1)throw new ShippingDraftFailure(ShippingDraftFailure.Code.PRECONDITION_STALE);
        one(jdbc.update("INSERT INTO orders.shipping_draft_version(id,owner_id,version,state,payload,created_at,updated_at) VALUES (?,?,?,?,CAST(? AS jsonb),?,?)",after.id(),after.accountId(),after.version(),after.state(),canonical(after.input()),created,updated));
        one(jdbc.update("INSERT INTO orders.shipping_draft_audit(id,owner_id,target_id,operation,prior_version,new_version,request_id,occurred_at) VALUES (?,?,?,?,?,?,?,?)",UUID.randomUUID(),after.accountId(),after.id(),operation,before==null?0:before.version(),after.version(),request,updated));
    }
    @Override public void receipt(UUID owner,String operation,UUID key,StoredReceipt receipt,Instant now){joined();byte[] bytes=receipt.hash();try{one(jdbc.update("INSERT INTO orders.shipping_draft_receipt(owner_id,operation,operation_key,target_id,original_version,result_version,fingerprint_key_id,fingerprint,created_at) VALUES (?,?,?,?,?,?,?,?,?)",owner,operation,key,receipt.target(),receipt.originalVersion(),receipt.resultVersion(),receipt.keyId(),bytes,Timestamp.from(now)));}finally{Arrays.fill(bytes,(byte)0);}}
}
