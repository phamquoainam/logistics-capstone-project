package com.elogistic.backend.identity.internal.infrastructure;
import com.elogistic.backend.identity.api.customer.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
/** Workflow calls after Merchant directory admission; Identity administration lock is already held. */
public final class JdbcCustomerMemberNames implements CustomerMemberNames {
    private final CustomerAdmission admission;private final JdbcTemplate jdbc;
    public JdbcCustomerMemberNames(DataSource source,CustomerAdmission admission){this.jdbc=new JdbcTemplate(source);this.admission=admission;}
    @Override public Set<UUID> eligibleAccounts(CustomerAdmission.Context context,Set<UUID> ids){
        admission.requireCurrent(context);if(ids==null||ids.size()>50050||ids.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("CUSTOMER_ELIGIBILITY_QUERY_INVALID");
        // The canonical Identity administration lock is held by CustomerAdmission through commit.
        // Match CustomerAdmissions' ACTIVE + VERIFIED credential + CUSTOMER realm eligibility.
        var result=new HashSet<UUID>();var ordered=ids.stream().sorted().toList();
        for(int offset=0;offset<ordered.size();offset+=100){var batch=ordered.subList(offset,Math.min(offset+100,ordered.size()));
            result.addAll(jdbc.query("SELECT a.id FROM identity.user_account a WHERE a.id IN ("+String.join(",",Collections.nCopies(batch.size(),"?"))+") AND a.status='ACTIVE' AND a.email_verified AND a.password_hash IS NOT NULL AND EXISTS(SELECT 1 FROM identity.customer_identifier c WHERE c.account_id=a.id AND c.status='ACTIVE' AND c.exact_email=a.email COLLATE \"C\")",(r,n)->r.getObject(1,UUID.class),batch.toArray()));
        }admission.requireCurrent(context);return Set.copyOf(result);
    }
    @Override public Map<UUID,String> read(CustomerAdmission.Context context,Set<UUID> ids){
        admission.requireCurrent(context);if(ids==null||ids.size()>50050||ids.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("TEAM_NAME_QUERY_INVALID");
        var result=new HashMap<UUID,String>();var ordered=ids.stream().sorted().toList();
        for(int offset=0;offset<ordered.size();offset+=100){var batch=ordered.subList(offset,Math.min(offset+100,ordered.size()));
            jdbc.query("SELECT id,display_name FROM identity.user_account WHERE id IN ("+String.join(",",Collections.nCopies(batch.size(),"?"))+")",r->{result.put(r.getObject(1,UUID.class),r.getString(2));},batch.toArray());
        }admission.requireCurrent(context);return Map.copyOf(result);
    }
}
