package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.admin.AdminReadContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Explicit Owner ADM-02 amendment. Existing Identity-management assignability is unchanged. */
public final class AdminSessionPolicy {
    public record Authority(UUID accountId,UUID realmId,String displayName,Set<String> permissions,String version) { public Authority{permissions=Set.copyOf(permissions);} }
    private final JdbcTemplate jdbc;private final boolean enabled;private final String environment;
    public AdminSessionPolicy(DataSource source,boolean enabled,String environment){this.jdbc=new JdbcTemplate(source);this.enabled=enabled;this.environment=environment;}
    public static AdminSessionPolicy disabled(DataSource source){return new AdminSessionPolicy(source,false,"DISABLED");}
    public String environment(){return environment;}
    public Optional<Authority> current(UUID account){
        if(!enabled||!Set.of("LOCAL","TEST").contains(environment))return Optional.empty();
        if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("ADMIN_TRANSACTION_REQUIRED");
        // AON hardening: current dedicated physical binding and purpose marker, never Staff authority.
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT identity.local_admin_environment_matches(?)",Boolean.class,environment)))return Optional.empty();
        var rows=jdbc.queryForList("""
            SELECT a.display_name,a.auth_version,g.role_code,g.scope_type,g.scope_id,g.granted_at,r.code,r.status,
             r.bootstrap_account_id,EXISTS(SELECT 1 FROM identity.customer_identifier c WHERE c.account_id=a.id) AS customer,
             identity.support_staff_registered(a.id) AS staff,identity.local_admin_eligible(a.id,g.scope_id) AS dedicated
            FROM identity.user_account a JOIN identity.account_grant g ON g.account_id=a.id
            LEFT JOIN identity.realm r ON r.id=g.identity_realm_id
            WHERE a.id=? AND a.status='ACTIVE' AND a.email_verified AND a.password_hash IS NOT NULL
            ORDER BY g.role_code,g.scope_id
            """,account);
        if(rows.isEmpty()||rows.size()>6)return Optional.empty();
        UUID realm=null;Set<String> permissions=new TreeSet<>();StringBuilder version=new StringBuilder(account.toString());
        for(var row:rows){
            String permission=Objects.toString(row.get("role_code"),"");
            if(!AdminReadContext.CODES.contains(permission)||!"IDENTITY".equals(row.get("scope_type"))||!"SYSTEM".equals(row.get("code"))||!"ACTIVE".equals(row.get("status"))||!Boolean.TRUE.equals(row.get("dedicated"))||Boolean.TRUE.equals(row.get("customer"))||Boolean.TRUE.equals(row.get("staff"))||account.equals(row.get("bootstrap_account_id")))return Optional.empty();
            UUID scope=(UUID)row.get("scope_id");if(realm!=null&&!realm.equals(scope))return Optional.empty();realm=scope;
            if(!permissions.add(permission))return Optional.empty();
            version.append('|').append(row.get("auth_version")).append('|').append(scope).append('|').append(permission).append('|').append(row.get("granted_at"));
        }
        if(!permissions.contains(AdminReadContext.ENTRY))return Optional.empty();
        try{return Optional.of(new Authority(account,realm,(String)rows.getFirst().get("display_name"),permissions,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(version.toString().getBytes(StandardCharsets.UTF_8)))));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException("ADMIN_DIGEST_UNAVAILABLE");}
    }
}
