package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.application.IdentityRealmGrantPolicy;
import com.elogistic.backend.identity.internal.application.port.GrantAuthority;
import com.elogistic.backend.identity.internal.domain.*;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Current, in-process, same-transaction registry lookup. No cache, owner call or bootstrap. */
public final class JdbcIdentityRealmGrantAuthority implements GrantAuthority {
    private static final long ADMIN_LOCK = 0x4944454e54495459L;
    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final IdentityRealmGrantPolicy policy = new IdentityRealmGrantPolicy();

    public JdbcIdentityRealmGrantAuthority(DataSource dataSource) {
        this.dataSource = java.util.Objects.requireNonNull(dataSource);
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override public ManagementAuthority resolve(IdentityAccount actor, IdentityAccount target, Set<Grant> requested) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                || !TransactionSynchronizationManager.hasResource(dataSource)) return null;
        Boolean locked = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM pg_locks WHERE locktype='advisory' "
            + "AND pid=pg_backend_pid() AND granted AND mode='ExclusiveLock' AND objsubid=1 "
            + "AND classid::bigint=? AND objid::bigint=?)", Boolean.class,
            ADMIN_LOCK >>> 32, ADMIN_LOCK & 0xffffffffL);
        if (!Boolean.TRUE.equals(locked)) return null;
        var realms = jdbc.query("SELECT id FROM identity.realm WHERE code='SYSTEM' AND status='ACTIVE' "
            + "AND bootstrap_account_id IS NOT NULL AND activated_at IS NOT NULL",
            (r, n) -> r.getObject(1, UUID.class));
        return realms.size() == 1 ? policy.resolve(actor, target, requested, realms.getFirst()) : null;
    }
}
