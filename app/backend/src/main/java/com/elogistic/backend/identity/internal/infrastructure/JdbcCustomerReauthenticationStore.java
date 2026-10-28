package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.CustomerProof;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** No independent transaction and no cross-module SQL. */
public final class JdbcCustomerReauthenticationStore implements CustomerReauthenticationStore {
    private final DataSource source;private final JdbcTemplate jdbc;
    public JdbcCustomerReauthenticationStore(DataSource source){this.source=Objects.requireNonNull(source);jdbc=new JdbcTemplate(source);}
    private void joined(){if(!TransactionSynchronizationManager.isActualTransactionActive() || !TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("CUSTOMER_REAUTHENTICATION_ADMISSION_TRANSACTION_REQUIRED");}
    private static Timestamp stamp(Instant time){return Timestamp.from(time);}
    private static void one(int count){if(count!=1)throw new IllegalStateException("CUSTOMER_REAUTHENTICATION_WRITE_REJECTED");}
    private static Instant time(ResultSet row,String name)throws SQLException{var value=row.getTimestamp(name);return value==null?null:value.toInstant();}
    private static CustomerProof proof(ResultSet row)throws SQLException{return new CustomerProof(row.getObject("id",UUID.class),row.getObject("subject_id",UUID.class),CustomerProof.Purpose.valueOf(row.getString("purpose")),time(row,"created_at"),time(row,"expires_at"),row.getObject("bound_session_id",UUID.class),(Long)row.getObject("bound_auth_version"),row.getObject("organization_id",UUID.class),time(row,"consumed_at"),time(row,"revoked_at"));}
    @Override public String passwordVerifier(UUID account){joined();return jdbc.queryForObject("SELECT password_hash FROM identity.user_account WHERE id=?",String.class,account);}
    @Override public Optional<Receipt> receipt(UUID account,UUID key){
        joined();return jdbc.query("SELECT p.*,r.operation,r.fingerprint_key_id,r.request_fingerprint,r.request_id FROM identity.customer_security_receipt r JOIN identity.customer_proof p ON p.id=r.proof_id WHERE r.account_id=? AND r.operation_key=?",(row,n)->new Receipt(new CustomerEnrollmentStore.Receipt(row.getString("operation"),row.getString("fingerprint_key_id"),row.getBytes("request_fingerprint"),row.getObject("request_id",UUID.class)),row.getObject("bound_session_id",UUID.class),row.getLong("bound_auth_version"),proof(row)),account,key).stream().findFirst();
    }
    @Override public Optional<CustomerProof> proof(UUID id){joined();return jdbc.query("SELECT * FROM identity.customer_proof WHERE id=? FOR UPDATE",(row,n)->proof(row),id).stream().findFirst();}
    @Override public void replace(CustomerProof proof,Instant now){
        joined();if(proof.purpose()!=CustomerProof.Purpose.REAUTHENTICATION)throw CustomerProof.rejected();
        jdbc.update("UPDATE identity.customer_proof SET revoked_at=? WHERE subject_id=? AND purpose='REAUTHENTICATION' AND consumed_at IS NULL AND revoked_at IS NULL",stamp(now),proof.subjectId());
        one(jdbc.update("INSERT INTO identity.customer_proof(id,subject_id,account_id,purpose,created_at,expires_at,bound_session_id,bound_auth_version,policy_version) VALUES (?,?,?,'REAUTHENTICATION',?,?,?,?,'S003-2026-10-27')",proof.id(),proof.subjectId(),proof.subjectId(),stamp(proof.createdAt()),stamp(proof.expiresAt()),proof.sessionId(),proof.authVersion()));
    }
    @Override public void consume(CustomerProof before,CustomerProof after){
        joined();if(!after.equals(before.consume(CustomerProof.Purpose.REAUTHENTICATION,before.subjectId(),before.sessionId(),before.authVersion(),null,after.consumedAt())))throw CustomerProof.rejected();
        one(jdbc.update("UPDATE identity.customer_proof SET consumed_at=? WHERE id=? AND purpose='REAUTHENTICATION' AND consumed_at IS NULL AND revoked_at IS NULL AND created_at<=? AND expires_at>?",stamp(after.consumedAt()),before.id(),stamp(after.consumedAt()),stamp(after.consumedAt())));
    }
    @Override public void receipt(UUID account,UUID key,Receipt receipt,Instant now){
        joined();byte[] hash=receipt.binding().fingerprint();try{one(jdbc.update("INSERT INTO identity.customer_security_receipt(account_id,operation_key,operation,bound_session_id,bound_auth_version,proof_id,proof_purpose,fingerprint_key_id,request_fingerprint,request_id,created_at,expires_at) VALUES (?,?,?,?,?,?,'REAUTHENTICATION',?,?,?,?,?)",account,key,receipt.binding().operation(),receipt.sessionId(),receipt.authVersion(),receipt.proof().id(),receipt.binding().keyId(),hash,receipt.binding().requestId(),stamp(now),stamp(now.plusSeconds(604800))));}finally{Arrays.fill(hash,(byte)0);}
    }
    @Override public void audit(UUID account,UUID request,String event,Instant now){
        joined();if(!Set.of("CUSTOMER_REAUTHENTICATED","CUSTOMER_REAUTHENTICATION_CONSUMED").contains(event))throw new IllegalArgumentException();
        one(jdbc.update("INSERT INTO identity.audit_entry(id,actor_id,target_id,event_type,source,request_id,occurred_at) VALUES (?,?,?,?,'IDENTITY_CUSTOMER',?,?)",UUID.randomUUID(),account,account,event,request.toString(),stamp(now)));
    }
}
