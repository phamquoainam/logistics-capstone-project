package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.domain.ActionProof;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Identity-INTERNAL worker boundary; no public module API/HTTP registration or network sender.
 * At-least-once transport: use stable messageId as provider dedupe key; a lease is not exactly-once mail.
 * Every operation shares the existing administration lock/timeout and runtime DataSource.
 * A future worker MUST invoke purge regularly even while delivery is disabled. */
public final class JdbcInvitationOutbox {
    public record Policy(int maxAttempts,Duration lease,Duration retryDelay) {
        public Policy {
            if(maxAttempts<1 || maxAttempts>10 || lease==null || retryDelay==null
                || lease.compareTo(Duration.ofSeconds(5))<0 || lease.compareTo(Duration.ofMinutes(5))>0
                || retryDelay.compareTo(Duration.ofSeconds(1))<0 || retryDelay.compareTo(Duration.ofMinutes(5))>0
                || lease.getNano()!=0 || retryDelay.getNano()!=0)throw new IllegalArgumentException("INVITATION_DELIVERY_POLICY_INVALID");
        }
    }
    public record Lease(UUID messageId,UUID leaseId,Instant until) {
        public Lease {Objects.requireNonNull(messageId);Objects.requireNonNull(leaseId);Objects.requireNonNull(until);}
        @Override public String toString(){return "InvitationLease[redacted]";}
    }
    public enum Outcome { SENT, TRANSIENT_FAILURE, PERMANENT_FAILURE }
    /** Authorized worker owns the returned transient buffer and must close it in all paths. */
    public static final class Delivery implements AutoCloseable {
        private final UUID messageId;
        private final String destination;
        private final byte[] token;
        private boolean closed;
        private Delivery(UUID id,String destination,byte[] token){this.messageId=id;this.destination=destination;this.token=token;}
        public UUID messageId(){return messageId;}
        public String destination(){if(closed)throw rejected();return destination;}
        /** Owned buffer, not a String. Do not log, put into diagnostics or retain after sending. */
        public byte[] token(){if(closed)throw rejected();return token;}
        @Override public void close(){InvitationProtection.wipe(token);closed=true;}
        @Override public String toString(){return "InvitationDelivery[redacted]";}
    }
    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final JdbcIdentityGrantStore transactions;
    private final InvitationProtection protection;
    private final Policy policy;
    private final Clock clock;
    public JdbcInvitationOutbox(DataSource runtime,InvitationProtection protection,Policy policy,Clock clock) {
        dataSource=Objects.requireNonNull(runtime);jdbc=new JdbcTemplate(runtime);transactions=new JdbcIdentityGrantStore(runtime);
        this.protection=Objects.requireNonNull(protection);this.policy=Objects.requireNonNull(policy);this.clock=Objects.requireNonNull(clock);
    }
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private static Timestamp stamp(Instant value){return Timestamp.from(value);}
    private static IllegalStateException rejected(){return new IllegalStateException("INVITATION_DELIVERY_REJECTED");}
    private <T>T transaction(Supplier<T> action){return transactions.transaction(tx->{tx.lockAdministration();return action.get();});}
    void enqueue(InvitationProtection.Context c,InvitationProtection.Sealed sealed) {
        if(!TransactionSynchronizationManager.isActualTransactionActive() || TransactionSynchronizationManager.isCurrentTransactionReadOnly()
            || !TransactionSynchronizationManager.hasResource(dataSource))throw new IllegalStateException("INVITATION_SAME_TRANSACTION_REQUIRED");
        byte[] nonce=sealed.nonce(),ciphertext=sealed.ciphertext();
        try {
            jdbc.update("INSERT INTO identity.invitation_outbox(id,proof_id,account_id,realm_id,purpose,destination,format_version,verifier_key_id,encryption_key_id,created_at,expires_at,status,next_attempt_at,nonce,ciphertext) VALUES (?,?,?,?,'INVITATION',?,1,?,?,?,?,'PENDING',?,?,?)",
                c.messageId(),c.proofId(),c.accountId(),c.realmId(),c.destination(),c.verifierKeyId(),c.encryptionKeyId(),stamp(c.createdAt()),stamp(c.expiresAt()),stamp(c.createdAt()),nonce,ciphertext);
        } finally {InvitationProtection.wipe(nonce);InvitationProtection.wipe(ciphertext);}
    }
    // Current state, not a stale queued snapshot, gates every open/claim. No external authority inference.
    private static final String ELIGIBLE="EXISTS (SELECT 1 FROM identity.action_proof p JOIN identity.user_account a ON a.id=p.account_id JOIN identity.realm r ON r.id=o.realm_id WHERE p.id=o.proof_id AND p.account_id=o.account_id AND p.purpose='INVITATION' AND p.consumed_at IS NULL AND p.revoked_at IS NULL AND a.status='PENDING_ACTIVATION' AND NOT a.email_verified AND a.password_hash IS NULL AND a.email=o.destination AND r.status='ACTIVE' AND EXISTS (SELECT 1 FROM identity.account_grant g WHERE g.account_id=a.id AND g.role_code='IDENTITY_MANAGEMENT' AND g.scope_type='IDENTITY' AND g.scope_id=r.id) AND NOT EXISTS (SELECT 1 FROM identity.account_grant g WHERE g.account_id=a.id AND (g.role_code<>'IDENTITY_MANAGEMENT' OR g.scope_type<>'IDENTITY' OR g.scope_id<>r.id)))";

    /** Bounded 100-row maintenance; removes only protected payloads, never accounts/proofs/audit. */
    public int purge(){return transaction(()->purge(now()));}
    private int purge(Instant now) {
        return jdbc.update("WITH obsolete AS (SELECT o.id FROM identity.invitation_outbox o WHERE o.status IN ('PENDING','LEASED') AND (o.expires_at<=? OR NOT ("+ELIGIBLE+") OR (o.attempts>=? AND (o.status='PENDING' OR o.lease_until<=?))) ORDER BY o.id LIMIT 100 FOR UPDATE OF o SKIP LOCKED) UPDATE identity.invitation_outbox o SET status=CASE WHEN o.expires_at<=? OR NOT ("+ELIGIBLE+") THEN 'EXPIRED' ELSE 'FAILED' END,last_outcome=CASE WHEN o.expires_at<=? OR NOT ("+ELIGIBLE+") THEN 'NOT_ELIGIBLE' ELSE 'ATTEMPTS_EXHAUSTED' END,nonce=NULL,ciphertext=NULL,lease_id=NULL,lease_until=NULL,finished_at=? FROM obsolete b WHERE o.id=b.id",
            stamp(now),policy.maxAttempts(),stamp(now),stamp(now),stamp(now),stamp(now));
    }
    public Optional<Lease> claim() {
        return transaction(()->{
            Instant now=now();purge(now);
            var candidates=jdbc.query("SELECT o.id,o.expires_at FROM identity.invitation_outbox o WHERE o.created_at<=? AND o.expires_at>? AND o.attempts<? AND o.next_attempt_at<=? AND (o.status='PENDING' OR (o.status='LEASED' AND o.lease_until<=?)) AND "+ELIGIBLE+" ORDER BY o.next_attempt_at,o.id LIMIT 1 FOR UPDATE OF o SKIP LOCKED",
                (r,n)->new Lease(r.getObject(1,UUID.class),UUID.randomUUID(),r.getTimestamp(2).toInstant()),stamp(now),stamp(now),policy.maxAttempts(),stamp(now),stamp(now));
            if(candidates.isEmpty())return Optional.empty();
            var c=candidates.getFirst();Instant until=now.plus(policy.lease());if(until.isAfter(c.until()))until=c.until();
            var lease=new Lease(c.messageId(),c.leaseId(),until);
            jdbc.update("UPDATE identity.invitation_outbox SET status='LEASED',attempts=attempts+1,lease_id=?,lease_until=? WHERE id=?",lease.leaseId(),stamp(until),lease.messageId());
            return Optional.of(lease);
        });
    }
    public Delivery open(Lease lease) {
        Objects.requireNonNull(lease);
        return transaction(()->{
            Instant now=now();
            var rows=jdbc.query("SELECT o.*,p.token_hash FROM identity.invitation_outbox o JOIN identity.action_proof p ON p.id=o.proof_id WHERE o.id=? AND o.lease_id=? AND o.status='LEASED' AND o.lease_until>? AND o.created_at<=? AND o.expires_at>? AND "+ELIGIBLE+" FOR UPDATE OF o",
                (r,n)->{
                    var c=new InvitationProtection.Context(r.getObject("id",UUID.class),r.getObject("proof_id",UUID.class),r.getObject("account_id",UUID.class),r.getObject("realm_id",UUID.class),r.getString("destination"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("expires_at").toInstant(),r.getString("verifier_key_id"),r.getString("encryption_key_id"));
                    byte[] token=protection.open(c,r.getBytes("nonce"),r.getBytes("ciphertext"),now);
                    try {
                        var proof=new ActionProof(c.proofId(),c.accountId(),ActionProof.Purpose.INVITATION,c.createdAt(),c.expiresAt(),ActionProof.State.AVAILABLE,null);
                        if(!protection.matches(proof,c.verifierKeyId(),token,r.getBytes("token_hash"),now))throw rejected();
                        return new Delivery(c.messageId(),c.destination(),token);
                    }catch(RuntimeException failure){InvitationProtection.wipe(token);throw failure;}
                },lease.messageId(),lease.leaseId(),stamp(now),stamp(now),stamp(now));
            if(rows.size()!=1)throw rejected();return rows.getFirst();
        });
    }
    /** Provider result only; never consumes an invitation or mutates account/security state. */
    public void complete(Lease lease,Outcome outcome) {
        Objects.requireNonNull(lease);Objects.requireNonNull(outcome);
        transaction(()->{
            Instant now=now();purge(now);
            var attempts=jdbc.query("SELECT attempts FROM identity.invitation_outbox WHERE id=? AND lease_id=? AND status='LEASED' AND lease_until>? AND expires_at>? FOR UPDATE",
                (r,n)->r.getInt(1),lease.messageId(),lease.leaseId(),stamp(now),stamp(now));
            if(attempts.size()!=1)throw rejected();
            boolean retry=outcome==Outcome.TRANSIENT_FAILURE && attempts.getFirst()<policy.maxAttempts();
            // The SQL clamps next_attempt_at to expires_at; claim/purge enforce the true deadline.
            if(retry)jdbc.update("UPDATE identity.invitation_outbox SET status='PENDING',lease_id=NULL,lease_until=NULL,last_outcome='TRANSIENT_FAILURE',next_attempt_at=LEAST(?,expires_at) WHERE id=?",
                stamp(now.plus(policy.retryDelay())),lease.messageId());
            else jdbc.update("UPDATE identity.invitation_outbox SET status=?,lease_id=NULL,lease_until=NULL,last_outcome=?,finished_at=?,nonce=NULL,ciphertext=NULL WHERE id=?",
                outcome==Outcome.SENT ? "SENT":"FAILED",outcome==Outcome.TRANSIENT_FAILURE ? "ATTEMPTS_EXHAUSTED":outcome.name(),stamp(now),lease.messageId());
            return null;
        });
    }
}
