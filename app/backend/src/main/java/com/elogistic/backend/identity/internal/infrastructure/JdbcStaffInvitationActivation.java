package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.internal.domain.*;
import java.time.Clock;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static com.elogistic.backend.identity.api.IdentityCommandFailure.Code.*;

/** Existing Identity proof/credential engine; SQL-05 owns the atomic Staff state transition. */
public final class JdbcStaffInvitationActivation implements com.elogistic.backend.identity.internal.application.port.StaffInvitationActivation {
    private final JdbcIdentityGrantStore transactions;
    private final boolean operations;
    private final JdbcTemplate jdbc;
    private final InvitationProtection protection;
    private final ProtectedInvitationCredentials credentials;
    private final Clock clock;

    public JdbcStaffInvitationActivation(DataSource runtime,InvitationProtection protection,Clock clock) {
        this(runtime,protection,clock,false);
    }
    public JdbcStaffInvitationActivation(DataSource runtime,InvitationProtection protection,Clock clock,boolean operations) {
        this.operations=operations;transactions=new JdbcIdentityGrantStore(runtime,operations?IdentitySessionStorage.OPS:IdentitySessionStorage.STAFF);jdbc=new JdbcTemplate(runtime);
        this.protection=Objects.requireNonNull(protection);credentials=new ProtectedInvitationCredentials(protection);this.clock=Objects.requireNonNull(clock);
    }
    public void accept(UUID proofId,byte[] supplied,String password,UUID key,UUID request) {
        if(proofId==null || supplied==null || key==null || request==null)throw failure(VALIDATION_FAILED);
        try{PasswordPolicy.validateNew(password);}catch(DomainViolation invalid){throw failure(VALIDATION_FAILED);}
        if(TransactionSynchronizationManager.isActualTransactionActive() || TransactionSynchronizationManager.isSynchronizationActive() || !TransactionSynchronizationManager.getResourceMap().isEmpty())throw failure(SESSION_INVALID);
        byte[] token=supplied.clone();
        try {
            transactions.transaction(tx->{
                tx.lockAdministration();
                var rows=jdbc.query("SELECT p.*,s.state AS staff_state FROM identity.action_proof p JOIN identity.local_staff_principal s ON s.account_id=p.account_id WHERE p.id=? AND p.purpose='STAFF_INV' AND s.purpose='"+(operations?"OPERATIONS":"SUPPORT")+"' FOR UPDATE OF p",(r,n)->{
                    var revoked=r.getTimestamp("revoked_at");var consumed=r.getTimestamp("consumed_at");
                    var state=revoked!=null?ActionProof.State.REVOKED:consumed!=null?ActionProof.State.CONSUMED:ActionProof.State.AVAILABLE;
                    return new Stored(new ActionProof(proofId,r.getObject("account_id",UUID.class),ActionProof.Purpose.STAFF_INV,r.getTimestamp("created_at").toInstant(),r.getTimestamp("expires_at").toInstant(),state,revoked!=null?revoked.toInstant():consumed!=null?consumed.toInstant():null),r.getString("verifier_key_id"),r.getBytes("token_hash"));
                },proofId);
                if(rows.size()!=1)throw failure(TOKEN_INVALID_OR_EXPIRED);
                Stored stored=rows.getFirst();byte[] fingerprint=null;
                try {
                    var proof=stored.proof();
                    if(proof.state()==ActionProof.State.REVOKED)throw failure(TOKEN_INVALID_OR_EXPIRED);
                    // As in SYSTEM acceptance, possession may be authenticated on a consumed row
                    // only to locate the exact existing receipt. It cannot authorize another activation.
                    var cryptographicIdentity=new ActionProof(proof.id(),proof.accountId(),proof.purpose(),proof.createdAt(),proof.expiresAt(),ActionProof.State.AVAILABLE,null);
                    if(!protection.matches(cryptographicIdentity,stored.key(),token,stored.hash(),clock.instant()))throw failure(TOKEN_INVALID_OR_EXPIRED);
                    fingerprint=protection.acceptanceFingerprint(proof,stored.key(),token,password);
                    var prior=jdbc.query("SELECT request_fingerprint FROM identity.command_receipt WHERE binding_kind='PROOF' AND proof_id=? AND operation='acceptStaffInvitation' AND operation_key=?",(r,n)->r.getBytes(1),proofId,key);
                    if(!prior.isEmpty() && !java.security.MessageDigest.isEqual(prior.getFirst(),fingerprint))throw failure(IDEMPOTENCY_CONFLICT);
                    var account=tx.lockAccount(proof.accountId()).orElseThrow(()->failure(TOKEN_INVALID_OR_EXPIRED));
                    if(prior.isEmpty()) {
                        try{account.activateStaffInvitation(proof,password,account.version().version(),clock.instant());}
                        catch(DomainViolation invalid){throw failure(TOKEN_INVALID_OR_EXPIRED);}
                    } else if(proof.state()!=ActionProof.State.CONSUMED || !Boolean.TRUE.equals(jdbc.queryForObject("SELECT identity."+(operations?"operations":"support")+"_staff_eligible(?)",Boolean.class,account.id())))throw failure(TOKEN_INVALID_OR_EXPIRED);
                    String verifier=credentials.hashPassword(password);
                    jdbc.query("SELECT identity.accept_local_"+(operations?"operations":"support")+"_staff_invitation(?,?,?,?,?)",r->{},proofId,key,fingerprint,verifier,request);
                    return null;
                }finally{Arrays.fill(stored.hash(),(byte)0);if(fingerprint!=null)Arrays.fill(fingerprint,(byte)0);}
            });
        }finally{Arrays.fill(token,(byte)0);}
    }
    private record Stored(ActionProof proof,String key,byte[] hash){@Override public String toString(){return "StoredStaffProof[redacted]";}}
    private static IdentityCommandFailure failure(IdentityCommandFailure.Code code){return new IdentityCommandFailure(code);}
}
