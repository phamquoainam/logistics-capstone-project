package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.internal.application.port.AccessSessionTiming;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Read-only locator. Current-session Application still locks/rechecks ALL authoritative state. */
public final class JdbcAccessSessionTiming implements AccessSessionTiming {
    private final JdbcTemplate jdbc;
    public JdbcAccessSessionTiming(DataSource runtime) { jdbc=new JdbcTemplate(runtime); }
    @Override public Instant createdAt(UUID sessionId,UUID accountId,long authVersion) {
        try {
            return jdbc.query("SELECT created_at FROM identity.auth_session WHERE id=? AND account_id=? AND auth_version=?",
                (r,n)->r.getTimestamp(1).toInstant(),sessionId,accountId,authVersion).stream().findFirst()
                .orElseThrow(()->new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID));
        } catch (org.springframework.dao.DataAccessException rejected) {
            throw new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID);
        }
    }
}
