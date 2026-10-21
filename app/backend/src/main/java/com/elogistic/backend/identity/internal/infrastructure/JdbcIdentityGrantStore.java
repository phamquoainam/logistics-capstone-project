package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.IdentityGrantCommands.Account;
import com.elogistic.backend.identity.api.IdentityGrantCommands.GrantData;
import com.elogistic.backend.identity.internal.application.port.IdentityGrantStore;
import com.elogistic.backend.identity.internal.domain.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** PostgreSQL V3 runtime DML only. Not auto-registered in Demo or H2 profiles. */
public final class JdbcIdentityGrantStore implements IdentityGrantStore {
    // Fixed Identity-local population lock, not a row/tenant supplied by a caller.
    // All future population-changing Identity commands must use this same protocol.
    private static final long ADMIN_LOCK = 0x4944454e54495459L;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public JdbcIdentityGrantStore(DataSource dataSource) {
        jdbc = new JdbcTemplate(Objects.requireNonNull(dataSource));
        var manager = new JdbcTransactionManager(dataSource);
        manager.setValidateExistingTransaction(true);
        transaction = new TransactionTemplate(manager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transaction.setTimeout(15);
    }

    @Override public <T> T transaction(Function<Transaction, T> work) {
        return transaction.execute(status -> {
            jdbc.execute("SET LOCAL lock_timeout = '5s'");
            jdbc.execute("SET LOCAL statement_timeout = '10s'");
            return work.apply(new JdbcTransaction());
        });
    }

    private final class JdbcTransaction implements Transaction {
        public void lockAdministration() {
            jdbc.query("SELECT pg_advisory_xact_lock(?)", (rs, row) -> 0, ADMIN_LOCK);
        }
        public Optional<UUID> sessionAccount(UUID id) {
            return jdbc.query("SELECT account_id FROM identity.auth_session WHERE id=?",
                (r, n) -> r.getObject(1, UUID.class), id).stream().findFirst();
        }
        public Optional<IdentityAccount> lockAccount(UUID id) {
            return jdbc.query("SELECT id,username,status,email_verified,version,auth_version FROM identity.user_account WHERE id=? FOR UPDATE",
                (r, n) -> new IdentityAccount(r.getObject("id", UUID.class), new Username(r.getString("username")),
                    IdentityAccount.Status.valueOf(r.getString("status")), r.getBoolean("email_verified")
                        ? IdentityAccount.Activation.VERIFIED : IdentityAccount.Activation.UNVERIFIED,
                    new AccountVersion(r.getLong("version"), r.getLong("auth_version")), grants(id)), id)
                .stream().findFirst();
        }
        private Set<Grant> grants(UUID id) {
            return Set.copyOf(jdbc.query("SELECT role_code,scope_type,scope_id FROM identity.account_grant WHERE account_id=?",
                (r, n) -> new Grant(new RoleCode(r.getString(1)),
                    new ResourceScope(ResourceScope.Type.valueOf(r.getString(2)), r.getObject(3, UUID.class))), id));
        }
        public Optional<IdentitySession> lockSession(UUID id) {
            return jdbc.query("SELECT * FROM identity.auth_session WHERE id=? FOR UPDATE", (r, n) ->
                new IdentitySession(r.getObject("id", UUID.class), r.getObject("account_id", UUID.class),
                    r.getLong("auth_version"), instant(r, "created_at"), instant(r, "absolute_expires_at"),
                    instant(r, "last_activity_at"), instant(r, "idle_expires_at"), instant(r, "revoked_at")), id)
                .stream().findFirst();
        }
        public long activeAdministrators() {
            return jdbc.queryForObject("SELECT count(*) FROM identity.user_account a WHERE a.status='ACTIVE' AND EXISTS " +
                "(SELECT 1 FROM identity.account_grant g WHERE g.account_id=a.id AND g.role_code='IDENTITY_MANAGEMENT' AND g.scope_type='IDENTITY')", Long.class);
        }
        public Optional<Receipt> receipt(UUID actor, UUID key) {
            return jdbc.query("SELECT target_id,original_version,result_version,encode(request_fingerprint,'hex') FROM identity.command_receipt " +
                "WHERE binding_kind='ACTOR' AND actor_id=? AND operation='replaceIdentityGrants' AND operation_key=?",
                (r, n) -> new Receipt(r.getObject(1, UUID.class), r.getLong(2), r.getLong(3), r.getString(4)), actor, key)
                .stream().findFirst();
        }
        public void replace(IdentityAccount before, IdentityAccount after, Instant now) {
            int count = jdbc.update("UPDATE identity.user_account SET version=?,auth_version=?,updated_at=? WHERE id=? AND version=? AND auth_version=?",
                after.version().version(), after.version().authVersion(), Timestamp.from(now), before.id(),
                before.version().version(), before.version().authVersion());
            if (count != 1) throw new IdentityCommandFailure(IdentityCommandFailure.Code.PRECONDITION_STALE);
            jdbc.update("DELETE FROM identity.account_grant WHERE account_id=?", before.id());
            for (Grant g : after.grants()) jdbc.update("INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at) VALUES (?,?,?,?,?)",
                after.id(), g.role().value(), g.scope().type().name(), g.scope().id(), Timestamp.from(now));
        }
        public void revokeSessions(UUID id, Instant now) {
            jdbc.update("UPDATE identity.auth_session SET revoked_at=? WHERE account_id=? AND revoked_at IS NULL", Timestamp.from(now), id);
            jdbc.update("UPDATE identity.refresh_token SET revoked_at=? WHERE session_id IN " +
                "(SELECT id FROM identity.auth_session WHERE account_id=?) AND revoked_at IS NULL", Timestamp.from(now), id);
        }
        public void appendAudit(UUID actor, UUID target, UUID request, String reason, Instant now) {
            jdbc.update("INSERT INTO identity.audit_entry(id,actor_id,target_id,event_type,source,reason,request_id,occurred_at) VALUES (?,?,?,'IDENTITY_GRANTS_REPLACED','IDENTITY_APPLICATION',?,?,?)",
                UUID.randomUUID(), actor, target, reason, request.toString(), Timestamp.from(now));
        }
        public void appendReceipt(UUID actor, UUID key, Receipt r, Instant now, Instant expires) {
            jdbc.update("INSERT INTO identity.command_receipt(binding_kind,actor_id,operation,operation_key,request_fingerprint,target_id,original_version,result_version,result_status,created_at,expires_at) " +
                "VALUES ('ACTOR',?,'replaceIdentityGrants',?,decode(?,'hex'),?,?,?,200,?,?)",
                actor, key, r.fingerprint(), r.targetId(), r.originalVersion(), r.resultVersion(), Timestamp.from(now), Timestamp.from(expires));
        }
        public Account accountView(UUID id) {
            List<GrantData> values = grants(id).stream().map(g -> new GrantData(g.role().value(), g.scope().type().name(), g.scope().id()))
                .sorted(Comparator.comparing(GrantData::role).thenComparing(GrantData::scopeType).thenComparing(GrantData::scopeId)).toList();
            return jdbc.queryForObject("SELECT id,username,display_name,email,status,version,email_verified FROM identity.user_account WHERE id=?",
                (r, n) -> new Account(r.getObject(1, UUID.class), r.getString(2), r.getString(3), r.getString(4),
                    r.getString(5), values, r.getLong(6), r.getBoolean(7)), id);
        }
    }
    private static Instant instant(ResultSet r, String column) throws SQLException {
        Timestamp value = r.getTimestamp(column); return value == null ? null : value.toInstant();
    }
}
