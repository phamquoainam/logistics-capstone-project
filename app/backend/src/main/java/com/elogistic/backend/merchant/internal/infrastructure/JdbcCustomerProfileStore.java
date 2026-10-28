package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.merchant.internal.application.port.CustomerProfileStore;
import com.elogistic.backend.merchant.internal.domain.CustomerBusinessProfile;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Merchant SQL only. The exact DataSource must already be bound by the Identity admission transaction. */
public final class JdbcCustomerProfileStore implements CustomerProfileStore {
    static final long CONTROL_LOCK=0x4d45524348414e54L;
    private final DataSource source;private final JdbcTemplate jdbc;
    public JdbcCustomerProfileStore(DataSource source){this.source=Objects.requireNonNull(source);jdbc=new JdbcTemplate(source);}
    /** Read-only runtime check; no privileged migration-history access or automatic schema mutation. */
    public static void requireSchema(DataSource source){
        try(Connection connection=source.getConnection();Statement query=connection.createStatement()){
            if(!connection.getMetaData().getDatabaseProductName().equals("PostgreSQL") || connection.getMetaData().getDatabaseMajorVersion()!=18)throw new IllegalStateException();
            query.setQueryTimeout(3);
            try(ResultSet row=query.executeQuery("SELECT to_regclass('merchant.customer_profile') IS NOT NULL AND to_regclass('merchant.customer_profile_version') IS NOT NULL AND to_regclass('merchant.command_receipt') IS NOT NULL AND to_regclass('merchant.business_audit') IS NOT NULL AND EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='merchant.customer_profile'::regclass AND conname='fk_customer_profile_current_version' AND condeferrable AND condeferred) AND EXISTS(SELECT 1 FROM pg_trigger WHERE tgrelid='merchant.customer_profile'::regclass AND tgname='guard_customer_profile_identity' AND tgenabled='O')")){
                if(!row.next() || !row.getBoolean(1))throw new IllegalStateException();
            }
        }catch(Exception unavailable){throw new IllegalStateException("CUSTOMER_PROFILE_POSTGRES18_SCHEMA_V11_REQUIRED");}
    }
    private void joined(){if(!TransactionSynchronizationManager.isActualTransactionActive() || !TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("CUSTOMER_PROFILE_OWNED_ADMISSION_TRANSACTION_REQUIRED");}
    private static Timestamp stamp(Instant time){return Timestamp.from(time);}
    private static void one(int count){if(count!=1)throw new IllegalStateException("CUSTOMER_PROFILE_WRITE_REJECTED");}
    private static CustomerBusinessProfile profile(ResultSet r)throws SQLException{return new CustomerBusinessProfile(r.getObject("id",UUID.class),r.getObject("account_id",UUID.class),r.getString("display_name"),r.getString("preferred_language"),r.getString("business_contact_email"),r.getString("business_contact_phone"),r.getLong("version"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant());}
    @Override public void lockControl(){joined();jdbc.query("SELECT pg_advisory_xact_lock(?)",(r,n)->0,CONTROL_LOCK);}
    @Override public Optional<CustomerBusinessProfile> find(UUID account,boolean lock){joined();return jdbc.query("SELECT * FROM merchant.customer_profile WHERE account_id=?"+(lock?" FOR UPDATE":""),(r,n)->profile(r),account).stream().findFirst();}
    @Override public Optional<Receipt> receipt(UUID account,UUID target,UUID key){
        joined();return jdbc.query("SELECT fingerprint_key_id,canonical_request_hash,original_version,result_version FROM merchant.command_receipt WHERE subject_account_id=? AND target_type='CUSTOMER_PROFILE' AND operation='updateCustomerProfile' AND target_id=? AND operation_key=?",(r,n)->new Receipt(r.getString(1),r.getBytes(2),r.getLong(3),r.getLong(4)),account,target,key).stream().findFirst();
    }
    @Override public CustomerBusinessProfile version(UUID account,UUID target,long version){
        joined();return jdbc.query("SELECT profile_id AS id,account_id,display_name,preferred_language,business_contact_email,business_contact_phone,profile_version AS version,created_at,updated_at FROM merchant.customer_profile_version WHERE account_id=? AND profile_id=? AND profile_version=?",(r,n)->profile(r),account,target,version).stream().findFirst().orElseThrow(()->new IllegalStateException("CUSTOMER_PROFILE_HISTORY_REQUIRED"));
    }
    @Override public void save(CustomerBusinessProfile before,CustomerBusinessProfile after,boolean insert){
        joined();
        if(insert)one(jdbc.update("INSERT INTO merchant.customer_profile VALUES (?,?,?,?,?,?,?,?,?)",after.id(),after.accountId(),after.displayName(),after.preferredLanguage(),after.businessContactEmail(),after.businessContactPhone(),after.version(),stamp(after.createdAt()),stamp(after.updatedAt())));
        else one(jdbc.update("UPDATE merchant.customer_profile SET display_name=?,preferred_language=?,business_contact_email=?,business_contact_phone=?,version=?,updated_at=? WHERE id=? AND account_id=? AND version=?",after.displayName(),after.preferredLanguage(),after.businessContactEmail(),after.businessContactPhone(),after.version(),stamp(after.updatedAt()),before.id(),before.accountId(),before.version()));
        one(jdbc.update("INSERT INTO merchant.customer_profile_version VALUES (?,?,?,?,?,?,?,?,?)",after.id(),after.version(),after.accountId(),after.displayName(),after.preferredLanguage(),after.businessContactEmail(),after.businessContactPhone(),stamp(after.createdAt()),stamp(after.updatedAt())));
    }
    @Override public void audit(UUID account,UUID target,long before,long after,UUID request,Instant now){
        joined();one(jdbc.update("INSERT INTO merchant.business_audit(id,actor_account_id,target_type,target_id,operation,prior_version,new_version,policy_version,request_id,occurred_at) VALUES (?,?,'CUSTOMER_PROFILE',?,'updateCustomerProfile',?,?,'S003-2026-10-27',?,?)",UUID.randomUUID(),account,target,before,after,request,stamp(now)));
    }
    @Override public void receipt(UUID account,UUID target,UUID key,Receipt receipt,Instant now){
        joined();byte[] hash=receipt.fingerprint();
        try{one(jdbc.update("INSERT INTO merchant.command_receipt(subject_account_id,target_type,target_id,operation,operation_key,fingerprint_key_id,canonical_request_hash,original_version,result_version,outcome,created_at,expires_at) VALUES (?,'CUSTOMER_PROFILE',?,'updateCustomerProfile',?,?,?,?,?,'COMMITTED',?,?)",account,target,key,receipt.keyId(),hash,receipt.originalVersion(),receipt.resultVersion(),stamp(now),stamp(now.plusSeconds(604800))));}
        finally{Arrays.fill(hash,(byte)0);}
    }
}
