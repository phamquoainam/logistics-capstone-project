package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.CustomerProof;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** No Merchant SQL, remote I/O, grant write or independent transaction. */
public final class JdbcCustomerOwnerConsentStore implements CustomerOwnerConsentStore {
    private final DataSource source;private final JdbcTemplate jdbc;
    public JdbcCustomerOwnerConsentStore(DataSource source){this.source=Objects.requireNonNull(source);jdbc=new JdbcTemplate(source);}
    private void joined(){if(!TransactionSynchronizationManager.isActualTransactionActive() || !TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("CUSTOMER_OWNER_CONSENT_ADMISSION_TRANSACTION_REQUIRED");}
    private static Timestamp stamp(Instant value){return Timestamp.from(value);}
    private static Instant time(ResultSet row,String column)throws SQLException{var value=row.getTimestamp(column);return value==null?null:value.toInstant();}
    private static void one(int count){if(count!=1)throw new IllegalStateException("CUSTOMER_OWNER_CONSENT_WRITE_REJECTED");}
    @Override public Optional<Receipt> receipt(UUID account,UUID key){
        joined();return jdbc.query("SELECT p.*,r.operation,r.fingerprint_key_id,r.request_fingerprint,r.request_id,r.source_reauthentication_proof_id FROM identity.customer_owner_consent_receipt r JOIN identity.customer_proof p ON p.id=r.proof_id WHERE r.account_id=? AND r.operation_key=?",(row,n)->new Receipt(new CustomerEnrollmentStore.Receipt(row.getString("operation"),row.getString("fingerprint_key_id"),row.getBytes("request_fingerprint"),row.getObject("request_id",UUID.class)),new CustomerProof(row.getObject("id",UUID.class),row.getObject("subject_id",UUID.class),CustomerProof.Purpose.valueOf(row.getString("purpose")),time(row,"created_at"),time(row,"expires_at"),row.getObject("bound_session_id",UUID.class),(Long)row.getObject("bound_auth_version"),row.getObject("organization_id",UUID.class),time(row,"consumed_at"),time(row,"revoked_at")),row.getObject("source_reauthentication_proof_id",UUID.class)),account,key).stream().findFirst();
    }
    @Override public void replace(CustomerProof proof,Instant now){
        joined();if(proof.purpose()!=CustomerProof.Purpose.OWNER_RECOVERY_CONSENT)throw CustomerProof.rejected();
        jdbc.update("UPDATE identity.customer_proof SET revoked_at=? WHERE subject_id=? AND purpose='OWNER_RECOVERY_CONSENT' AND organization_id=? AND consumed_at IS NULL AND revoked_at IS NULL",stamp(now),proof.subjectId(),proof.organizationId());
        one(jdbc.update("INSERT INTO identity.customer_proof(id,subject_id,account_id,purpose,created_at,expires_at,bound_session_id,bound_auth_version,organization_id,policy_version) VALUES (?,?,?,'OWNER_RECOVERY_CONSENT',?,?,?,?,?,'S003-2026-10-27')",proof.id(),proof.subjectId(),proof.subjectId(),stamp(proof.createdAt()),stamp(proof.expiresAt()),proof.sessionId(),proof.authVersion(),proof.organizationId()));
    }
    @Override public void receipt(UUID account,UUID key,Receipt receipt,Instant now){
        joined();var proof=receipt.proof();byte[] hash=receipt.binding().fingerprint();
        try{one(jdbc.update("INSERT INTO identity.customer_owner_consent_receipt(account_id,operation_key,operation,bound_session_id,bound_auth_version,proof_id,fingerprint_key_id,request_fingerprint,request_id,created_at,expires_at,bound_organization_id,source_reauthentication_proof_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",account,key,receipt.binding().operation(),proof.sessionId(),proof.authVersion(),proof.id(),receipt.binding().keyId(),hash,receipt.binding().requestId(),stamp(now),stamp(now.plusSeconds(604800)),proof.organizationId(),receipt.sourceReauthentication()));}
        finally{Arrays.fill(hash,(byte)0);}
    }
    @Override public void audit(UUID account,UUID request,Instant now){
        joined();one(jdbc.update("INSERT INTO identity.audit_entry(id,actor_id,target_id,event_type,source,request_id,occurred_at) VALUES (?,?,?,'CUSTOMER_OWNER_RECOVERY_CONSENT_CREATED','IDENTITY_CUSTOMER',?,?)",UUID.randomUUID(),account,account,request.toString(),stamp(now)));
    }
}
