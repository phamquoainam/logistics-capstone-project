package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.merchant.api.OnboardingFailure;
import com.elogistic.backend.merchant.api.customer.MerchantApplications.*;
import com.elogistic.backend.merchant.internal.application.port.MerchantApplicationStore;
import com.elogistic.backend.merchant.internal.domain.MerchantApplicationDraft;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

/** Merchant-only SQL, joined to the exact Identity-owned physical transaction. */
public final class JdbcMerchantApplicationStore implements MerchantApplicationStore {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private final DataSource source;private final JdbcTemplate jdbc;
    public JdbcMerchantApplicationStore(DataSource source){this.source=Objects.requireNonNull(source);jdbc=new JdbcTemplate(source);}
    public static void requireSchema(DataSource source){
        try(Connection connection=source.getConnection();Statement q=connection.createStatement()){
            if(!connection.getMetaData().getDatabaseProductName().equals("PostgreSQL") || connection.getMetaData().getDatabaseMajorVersion()!=18)throw new IllegalStateException();q.setQueryTimeout(3);
            try(ResultSet r=q.executeQuery("SELECT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='merchant.application'::regclass AND conname='fk_application_current_draft' AND condeferrable AND condeferred) AND EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='merchant.command_receipt'::regclass AND conname='ck_merchant_receipt_operation_binding') AND EXISTS(SELECT 1 FROM pg_trigger WHERE tgrelid='merchant.application'::regclass AND tgname='guard_application_draft_identity' AND tgenabled='O')")){if(!r.next() || !r.getBoolean(1))throw new IllegalStateException();}
        }catch(Exception unavailable){throw new IllegalStateException("MERCHANT_APPLICATION_POSTGRES18_SCHEMA_V14_REQUIRED");}
    }
    private void joined(){if(!TransactionSynchronizationManager.isActualTransactionActive() || !TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("MERCHANT_APPLICATION_OWNED_ADMISSION_TRANSACTION_REQUIRED");}
    private static void one(int count){if(count!=1)throw new IllegalStateException("APPLICATION_WRITE_REJECTED");}
    private static Timestamp stamp(Instant time){return Timestamp.from(time);}
    @Override public String canonical(Input input){return JSON.writeValueAsString(input);}
    private static MerchantApplicationDraft row(ResultSet r)throws SQLException{return new MerchantApplicationDraft(r.getObject("id",UUID.class),r.getObject("applicant_account_id",UUID.class),JSON.readValue(r.getString("draft"),Input.class),r.getLong("version"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant());}
    @Override public void lockControl(){joined();jdbc.query("SELECT pg_advisory_xact_lock(?)",(r,n)->0,JdbcCustomerProfileStore.CONTROL_LOCK);}
    @Override public void lockKeys(Collection<String> keys){joined();new TreeSet<>(keys).forEach(key->jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,?))",(r,n)->0,key,JdbcCustomerProfileStore.CONTROL_LOCK));}
    @Override public Optional<MerchantApplicationDraft> find(UUID actor,UUID id,boolean lock){joined();return jdbc.query("SELECT * FROM merchant.application WHERE applicant_account_id=? AND id=?"+(lock?" FOR UPDATE":""),(r,n)->row(r),actor,id).stream().findFirst();}
    @Override public List<MerchantApplicationDraft> list(UUID actor,Query query){
        joined();StringBuilder sql=new StringBuilder("SELECT * FROM merchant.application WHERE applicant_account_id=?");List<Object> parameters=new ArrayList<>();parameters.add(actor);
        if(query.state()!=null){sql.append(" AND state=?");parameters.add(query.state());}
        if(query.search()!=null){sql.append(" AND (strpos(coalesce(draft->>'registeredName',''),?)>0 OR strpos(coalesce(draft->>'legalIdentifier',''),?)>0)");parameters.add(query.search());parameters.add(query.search());}
        sql.append(" ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?");parameters.add(query.size()+1);parameters.add(Math.multiplyExact((long)query.page(),query.size()));
        return jdbc.query(sql.toString(),(r,n)->row(r),parameters.toArray());
    }
    @Override public Optional<Receipt> receipt(UUID actor,String operation,UUID key){joined();return jdbc.query("SELECT target_id,fingerprint_key_id,canonical_request_hash,original_version,result_version FROM merchant.command_receipt WHERE subject_account_id=? AND target_type='APPLICATION' AND operation=? AND operation_key=?",(r,n)->new Receipt(r.getObject(1,UUID.class),r.getString(2),r.getBytes(3),r.getLong(4),r.getLong(5)),actor,operation,key).stream().findFirst();}
    @Override public MerchantApplicationDraft version(UUID actor,UUID target,long version){joined();return jdbc.query("SELECT application_id AS id,application_version AS version,applicant_account_id,draft,created_at,updated_at FROM merchant.application_draft_version WHERE applicant_account_id=? AND application_id=? AND application_version=?",(r,n)->row(r),actor,target,version).stream().findFirst().orElseThrow(()->new IllegalStateException("APPLICATION_HISTORY_REQUIRED"));}
    @Override public void save(MerchantApplicationDraft before,MerchantApplicationDraft after){
        joined();try{
            if(before==null)one(jdbc.update("INSERT INTO merchant.application(id,applicant_account_id,version,draft,created_at,updated_at) VALUES (?,?,?,?::jsonb,?,?)",after.id(),after.applicantAccountId(),after.version(),canonical(after.input()),stamp(after.createdAt()),stamp(after.updatedAt())));
            else one(jdbc.update("UPDATE merchant.application SET version=?,draft=?::jsonb,updated_at=? WHERE id=? AND applicant_account_id=? AND version=? AND state='DRAFT'",after.version(),canonical(after.input()),stamp(after.updatedAt()),before.id(),before.applicantAccountId(),before.version()));
            one(jdbc.update("INSERT INTO merchant.application_draft_version VALUES (?,?,?,?::jsonb,?,?)",after.id(),after.version(),after.applicantAccountId(),canonical(after.input()),stamp(after.createdAt()),stamp(after.updatedAt())));
        }catch(DuplicateKeyException duplicate){throw new OnboardingFailure(OnboardingFailure.Code.BUSINESS_CONFLICT);}
    }
    @Override public void audit(UUID actor,String operation,UUID target,long before,long after,UUID request,Instant now){joined();one(jdbc.update("INSERT INTO merchant.business_audit(id,actor_account_id,target_type,target_id,operation,prior_version,new_version,policy_version,request_id,occurred_at) VALUES (?,?,'APPLICATION',?,?,?,?,'S003-2026-10-27',?,?)",UUID.randomUUID(),actor,target,operation,before,after,request,stamp(now)));}
    @Override public void receipt(UUID actor,String operation,UUID key,Receipt receipt,Instant now){
        joined();byte[] hash=receipt.fingerprint();try{one(jdbc.update("INSERT INTO merchant.command_receipt(subject_account_id,target_type,target_id,operation,operation_key,fingerprint_key_id,canonical_request_hash,original_version,result_version,outcome,created_at,expires_at) VALUES (?,'APPLICATION',?,?,?,?,?,?,?,'COMMITTED',?,?)",actor,receipt.target(),operation,key,receipt.keyId(),hash,receipt.originalVersion(),receipt.resultVersion(),stamp(now),stamp(now.plusSeconds(604800))));}finally{Arrays.fill(hash,(byte)0);}
    }
}
