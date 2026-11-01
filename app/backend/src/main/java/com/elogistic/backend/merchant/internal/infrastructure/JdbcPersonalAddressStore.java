package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.merchant.api.customer.PersonalAddresses.*;
import com.elogistic.backend.merchant.api.customer.PersonalAddressFailure;
import com.elogistic.backend.merchant.internal.application.port.PersonalAddressStore;
import com.elogistic.backend.merchant.internal.domain.CustomerAddress;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class JdbcPersonalAddressStore implements PersonalAddressStore {
    private final DataSource source;private final JdbcTemplate jdbc;
    private static final String COLUMNS="id,account_id,kind,label,contact_name,contact_phone,country,administrative_area,locality,address_line,is_default,archived_at,version,created_at,updated_at";
    public JdbcPersonalAddressStore(DataSource source){this.source=Objects.requireNonNull(source);jdbc=new JdbcTemplate(source);}
    public static void requireSchema(DataSource source){
        try(var c=source.getConnection();var s=c.createStatement();var r=s.executeQuery("SELECT current_setting('server_version_num')::int>=180000 AND to_regclass('merchant.customer_address_version') IS NOT NULL AND EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='merchant.customer_address'::regclass AND conname='fk_customer_address_current_version' AND condeferrable AND condeferred AND convalidated) AND EXISTS(SELECT 1 FROM pg_trigger WHERE tgrelid='merchant.customer_address'::regclass AND tgname='guard_customer_address_identity' AND tgenabled='O') AND EXISTS(SELECT 1 FROM pg_index WHERE indexrelid='merchant.uq_customer_address_active_default'::regclass AND indisunique AND indisvalid)")){
            if(!r.next() || !r.getBoolean(1))throw new IllegalStateException();
        }catch(Exception failure){throw new IllegalStateException("PERSONAL_ADDRESS_POSTGRES18_SCHEMA_V18_REQUIRED");}
    }
    private void joined(){if(!TransactionSynchronizationManager.isActualTransactionActive() || !TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("PERSONAL_ADDRESS_OWNED_ADMISSION_TRANSACTION_REQUIRED");}
    private static Timestamp stamp(Instant time){return time==null?null:Timestamp.from(time);}
    private static CustomerAddress row(ResultSet r,int ignored)throws SQLException{
        var location=new Location(r.getString("country"),r.getString("administrative_area"),r.getString("locality"),r.getString("address_line"));
        var input=new Input(r.getString("kind"),r.getString("label"),r.getString("contact_name"),r.getString("contact_phone"),location,r.getBoolean("is_default"));var archived=r.getTimestamp("archived_at");
        return new CustomerAddress(r.getObject("id",UUID.class),r.getObject("account_id",UUID.class),input,r.getLong("version"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant(),archived==null?null:archived.toInstant());
    }
    @Override public void lockOwner(UUID account){joined();jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS text),730018))",(r,n)->0,account.toString());}
    @Override public Optional<CustomerAddress> find(UUID account,UUID id,boolean lock){joined();return jdbc.query("SELECT "+COLUMNS+" FROM merchant.customer_address WHERE account_id=? AND id=?"+(lock?" FOR UPDATE":""),JdbcPersonalAddressStore::row,account,id).stream().findFirst();}
    @Override public Optional<CustomerAddress> activeDefault(UUID account,String kind,UUID excluding){joined();return jdbc.query("SELECT "+COLUMNS+" FROM merchant.customer_address WHERE account_id=? AND kind=? AND is_default AND archived_at IS NULL AND id<>? FOR UPDATE",JdbcPersonalAddressStore::row,account,kind,excluding).stream().findFirst();}
    @Override public List<CustomerAddress> list(UUID account,Query query){
        joined();String sql="SELECT "+COLUMNS+" FROM merchant.customer_address WHERE account_id=? AND archived_at IS NULL";List<Object> args=new ArrayList<>();args.add(account);
        if(query.kind()!=null){sql+=" AND kind=?";args.add(query.kind());}
        if(query.search()!=null){
            String phone=query.search().strip().replaceAll("[ ()-]","");if(phone.matches("0[35789][0-9]{8}"))phone="+84"+phone.substring(1);else if(phone.matches("84[35789][0-9]{8}"))phone="+"+phone;
            boolean exactPhone=phone.matches("\\+84[35789][0-9]{8}");sql+=" AND (lower(concat_ws(' ',label,contact_name,contact_phone,address_line,locality,administrative_area)) LIKE ? ESCAPE '!'";args.add("%"+query.search().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");
            if(exactPhone){sql+=" OR contact_phone=?";args.add(phone);}sql+=")";
        }
        sql+=" ORDER BY updated_at DESC,id ASC LIMIT 21 OFFSET ?";args.add(Math.multiplyExact((long)query.page(),20));return jdbc.query(sql,JdbcPersonalAddressStore::row,args.toArray());
    }
    @Override public CustomerAddress version(UUID account,UUID id,long version){joined();return jdbc.query("SELECT "+COLUMNS+" FROM merchant.customer_address_version WHERE account_id=? AND id=? AND version=?",JdbcPersonalAddressStore::row,account,id,version).stream().findFirst().orElseThrow(()->new IllegalStateException("ADDRESS_HISTORY_REQUIRED"));}
    private static Object[] values(CustomerAddress a){var i=a.input();var l=i.address();return new Object[]{a.id(),a.accountId(),i.kind(),i.label(),i.contactName(),i.contactPhone(),l.country(),l.administrativeArea(),l.locality(),l.addressLine(),i.isDefault(),stamp(a.archivedAt()),a.version(),stamp(a.createdAt()),stamp(a.updatedAt())};}
    @Override public void save(CustomerAddress before,CustomerAddress after){
        joined();int changed;
        if(before==null)changed=jdbc.update("INSERT INTO merchant.customer_address("+COLUMNS+") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",values(after));
        else {var i=after.input();var l=i.address();changed=jdbc.update("UPDATE merchant.customer_address SET kind=?,label=?,contact_name=?,contact_phone=?,country=?,administrative_area=?,locality=?,address_line=?,is_default=?,archived_at=?,version=?,updated_at=? WHERE account_id=? AND id=? AND version=? AND archived_at IS NULL",i.kind(),i.label(),i.contactName(),i.contactPhone(),l.country(),l.administrativeArea(),l.locality(),l.addressLine(),i.isDefault(),stamp(after.archivedAt()),after.version(),stamp(after.updatedAt()),before.accountId(),before.id(),before.version());}
        if(changed!=1)throw new PersonalAddressFailure(PersonalAddressFailure.Code.PRECONDITION_STALE);
        if(jdbc.update("INSERT INTO merchant.customer_address_version("+COLUMNS+") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",values(after))!=1)throw new IllegalStateException("ADDRESS_HISTORY_REQUIRED");
    }
    @Override public Optional<Receipt> receipt(UUID account,String operation,UUID key){joined();return jdbc.query("SELECT target_id,fingerprint_key_id,canonical_request_hash,original_version,result_version FROM merchant.command_receipt WHERE subject_account_id=? AND target_type='CUSTOMER_ADDRESS' AND operation=? AND operation_key=?",(r,n)->new Receipt(r.getObject(1,UUID.class),r.getString(2),r.getBytes(3),r.getLong(4),r.getLong(5)),account,operation,key).stream().findFirst();}
    @Override public void receipt(UUID account,String operation,UUID key,Receipt receipt,Instant now){
        joined();byte[] hash=receipt.fingerprint();try{int count=jdbc.update("INSERT INTO merchant.command_receipt(subject_account_id,target_type,target_id,operation,operation_key,fingerprint_key_id,canonical_request_hash,original_version,result_version,outcome,created_at,expires_at) VALUES (?,'CUSTOMER_ADDRESS',?,?,?,?,?,?,?,'COMMITTED',?,?)",account,receipt.target(),operation,key,receipt.keyId(),hash,receipt.originalVersion(),receipt.resultVersion(),stamp(now),stamp(now.plusSeconds(604800)));if(count!=1)throw new IllegalStateException("ADDRESS_RECEIPT_REQUIRED");}finally{Arrays.fill(hash,(byte)0);}
    }
    @Override public void audit(UUID account,String operation,UUID id,long before,long after,UUID request,Instant now){joined();int count=jdbc.update("INSERT INTO merchant.business_audit(id,actor_account_id,target_type,target_id,operation,prior_version,new_version,policy_version,request_id,occurred_at) VALUES (?,?,'CUSTOMER_ADDRESS',?,?,?,?,'PERSONAL-ADDRESS-2026-11-01',?,?)",UUID.randomUUID(),account,id,operation,before,after,request,stamp(now));if(count!=1)throw new IllegalStateException("ADDRESS_AUDIT_REQUIRED");}
}
