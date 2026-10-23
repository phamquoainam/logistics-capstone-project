package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** V3/V4 runtime DML, same DataSource/administration-lock protocol as Grant/Provision/Activation. */
public final class JdbcIdentitySessionStore implements IdentitySessionStore {
    private final JdbcIdentityGrantStore store;
    private final JdbcTemplate jdbc;
    public JdbcIdentitySessionStore(DataSource runtime) {store=new JdbcIdentityGrantStore(runtime);jdbc=new JdbcTemplate(runtime);}
    private static Timestamp stamp(Instant t) {return Timestamp.from(t);}
    private static void one(int count) {if(count!=1)throw new IllegalStateException("SESSION_WRITE_REJECTED");}
    @Override public <T>T transaction(Function<Transaction,T> work) {
        // Session must own the physical commit before returning a result or replay denial.
        // Synchronization or pre-bound resources also make ownership ambiguous: fail closed.
        // Do not suspend, commit or roll back any caller-owned transaction/resource.
        if (TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.getResourceMap().isEmpty()) {
            throw new IllegalStateException("SESSION_TRANSACTION_OWNERSHIP_REQUIRED");
        }
        return store.transaction(a->work.apply(new Transaction() {
            public IdentityGrantStore.Transaction accounts() {return a;}
            public Optional<UUID> findUsername(String username) {return jdbc.query("SELECT id FROM identity.user_account WHERE username=?",(r,n)->r.getObject(1,UUID.class),username).stream().findFirst();}
            public String passwordVerifier(UUID account) {return jdbc.queryForObject("SELECT password_hash FROM identity.user_account WHERE id=?",String.class,account);}
            public boolean eligibleRealm(UUID account) {
                // Bounded approved Identity Management pair only; external owner authority is not invented.
                return Boolean.TRUE.equals(jdbc.queryForObject("SELECT count(*)=1 AND COALESCE(bool_and(g.role_code='IDENTITY_MANAGEMENT' AND g.scope_type='IDENTITY' AND r.status='ACTIVE'),FALSE) FROM identity.account_grant g LEFT JOIN identity.realm r ON r.id=g.identity_realm_id WHERE g.account_id=?",Boolean.class,account));
            }
            public Optional<Refresh> findRefresh(byte[] digest) {
                return jdbc.query("SELECT id,session_id,expires_at,consumed_at,revoked_at FROM identity.refresh_token WHERE token_hash=? FOR UPDATE",(r,n)->{
                    var consumed=r.getTimestamp(4);var revoked=r.getTimestamp(5);
                    return new Refresh(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getTimestamp(3).toInstant(),consumed==null?null:consumed.toInstant(),revoked==null?null:revoked.toInstant());
                },digest).stream().findFirst();
            }
            public Optional<RefreshFamily> family(IdentitySession session) {
                var rows=jdbc.query("SELECT id,consumed_at,revoked_at,expires_at FROM identity.refresh_token WHERE session_id=?",(r,n)->new Refresh(r.getObject(1,UUID.class),session.id(),r.getTimestamp(4).toInstant(),r.getTimestamp(2)==null?null:r.getTimestamp(2).toInstant(),r.getTimestamp(3)==null?null:r.getTimestamp(3).toInstant()),session.id());
                var live=rows.stream().filter(r->r.consumedAt()==null && r.revokedAt()==null).toList();
                if(live.size()!=1 || live.get(0).expiresAt().isAfter(session.absoluteExpiresAt()))return Optional.empty();
                Set<UUID> consumed=new HashSet<>();rows.stream().filter(r->r.consumedAt()!=null).forEach(r->consumed.add(r.id()));
                return Optional.of(new RefreshFamily(session.id(),live.get(0).id(),live.get(0).expiresAt(),consumed,RefreshFamily.State.ACTIVE));
            }
            public void insertSession(IdentitySession s) {
                one(jdbc.update("INSERT INTO identity.auth_session(id,account_id,auth_version,created_at,absolute_expires_at,last_activity_at,idle_expires_at) VALUES (?,?,?,?,?,?,?)",s.id(),s.accountId(),s.authVersion(),stamp(s.createdAt()),stamp(s.absoluteExpiresAt()),stamp(s.lastActivityAt()),stamp(s.idleExpiresAt())));
            }
            public void insertRefresh(UUID id,UUID predecessor,IdentitySession s,byte[] hash,Instant now) {
                one(jdbc.update("INSERT INTO identity.refresh_token(id,session_id,token_hash,predecessor_id,issued_at,expires_at) VALUES (?,?,?,?,?,?)",id,s.id(),hash,predecessor,stamp(now),stamp(s.absoluteExpiresAt())));
            }
            public void consumeRefresh(UUID id,Instant now) {one(jdbc.update("UPDATE identity.refresh_token SET consumed_at=? WHERE id=? AND consumed_at IS NULL AND revoked_at IS NULL AND expires_at>?",stamp(now),id,stamp(now)));}
            public boolean revokeFamily(UUID id,Instant now) {
                int session=jdbc.update("UPDATE identity.auth_session SET revoked_at=? WHERE id=? AND revoked_at IS NULL",stamp(now),id);
                int tokens=jdbc.update("UPDATE identity.refresh_token SET revoked_at=? WHERE session_id=? AND revoked_at IS NULL",stamp(now),id);
                return session>0 || tokens>0;
            }
            public void audit(UUID account,UUID request,String event,Instant now) {
                if(!Set.of("IDENTITY_LOGIN","IDENTITY_REFRESH","IDENTITY_REFRESH_REPLAY","IDENTITY_LOGOUT").contains(event))throw new IllegalArgumentException("SESSION_EVENT_INVALID");
                one(jdbc.update("INSERT INTO identity.audit_entry(id,actor_id,target_id,event_type,source,reason,request_id,occurred_at) VALUES (?,?,?,?,'IDENTITY_APPLICATION','Identity session transition',?,?)",UUID.randomUUID(),account,account,event,request.toString(),stamp(now)));
            }
        }));
    }
}
