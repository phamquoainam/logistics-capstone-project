package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.merchant.api.customer.BusinessWorkspaces;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class JdbcBusinessWorkspaces implements BusinessWorkspaces {
    private final DataSource source; private final JdbcTemplate jdbc; private final boolean enabled; private final String environment;
    private static final Set<String> PERMISSIONS=Set.of("dashboard.read","dashboard.finance.read","dashboard.staff.read","shipment.draft.read","shipment.draft.write","shipment.import","shipment.template.manage","shipment.prepare","shipment.quote","shipment.book","shipment.label.read","facility.manage","defaults.manage");
    public JdbcBusinessWorkspaces(DataSource source,boolean enabled,String environment){this.source=source;this.jdbc=new JdbcTemplate(source);this.enabled=enabled;this.environment=environment;}
    public static void requireSchema(DataSource source){
        try(var c=source.getConnection();var s=c.createStatement();var r=s.executeQuery("""
            SELECT current_setting('server_version_num')::int>=180000
              AND to_regclass('merchant.workspace_audit') IS NOT NULL AND to_regclass('merchant.workspace_command_receipt') IS NOT NULL
              AND to_regclass('merchant.member_grant') IS NOT NULL
              AND EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='merchant.organization'::regclass AND conname='workspace_initial_owner_fk' AND condeferrable AND condeferred AND convalidated)
              AND EXISTS(SELECT 1 FROM pg_trigger WHERE tgrelid='merchant.organization'::regclass AND tgname='workspace_org_invariants' AND tgdeferrable AND tginitdeferred AND tgenabled='O')
              AND EXISTS(SELECT 1 FROM pg_trigger WHERE tgrelid='merchant.member_grant'::regclass AND tgname='workspace_grant_invariants' AND tgdeferrable AND tginitdeferred AND tgenabled='O')
            """)){if(!r.next()||!r.getBoolean(1))throw new IllegalStateException();}
        catch(Exception rejected){throw new IllegalStateException("BUSINESS_WORKSPACE_POSTGRES18_SCHEMA_V23_REQUIRED");}
    }
    private void lock(Access access){
        access.requireCurrent();
        if(!enabled)throw new Failure("BUSINESS_DISABLED");
        if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("BUSINESS_ADMISSION_REQUIRED");
        jdbc.query("SELECT pg_advisory_xact_lock(730023,1)",(r,n)->0);
    }
    private record Row(Context context,UUID owner){}
    private List<Row> rows(Access access,UUID org){
        return jdbc.query("""
            SELECT o.id,o.display_name,m.id membership_id,m.role,m.authority_version,owner.account_id owner_account,
                   array_agg(g.permission ORDER BY g.permission) permissions
            FROM merchant.organization o JOIN merchant.membership m ON m.organization_id=o.id
            JOIN merchant.membership owner ON owner.id=o.initial_owner_membership_id AND owner.organization_id=o.id
            JOIN merchant.member_grant g ON g.membership_id=m.id AND g.organization_id=o.id
            WHERE m.account_id=? AND m.state='ACTIVE' AND o.state='ACTIVE'
              AND owner.state='ACTIVE' AND owner.role='OWNER' AND o.environment=?
              AND o.admission_origin='LOCAL_OPERATOR_PROVISIONED' AND g.scope='ORGANIZATION'
              AND (?::uuid IS NULL OR o.id=?)
            GROUP BY o.id,m.id,owner.account_id
            HAVING bool_or(g.permission='dashboard.read')
            ORDER BY o.id LIMIT 101
            """,(r,n)->{
                var array=r.getArray("permissions"); List<String> permissions;
                try{permissions=Arrays.stream((Object[])array.getArray()).map(Object::toString).toList();}finally{array.free();}
                if(!PERMISSIONS.containsAll(permissions))throw new Failure("BUSINESS_CONTEXT_UNAVAILABLE");
                return new Row(new Context(r.getObject("id",UUID.class),r.getString("display_name"),r.getObject("membership_id",UUID.class),r.getString("role"),permissions,r.getLong("authority_version")),r.getObject("owner_account",UUID.class));
            },access.accountId(),environment,org,org);
    }
    private List<Row> bounded(Access a,UUID org){var found=rows(a,org);if(found.size()>100)throw new Failure("BUSINESS_CONTEXT_LIMIT");return found;}
    @Override public Set<UUID> references(Access a,UUID org){lock(a);var ids=new HashSet<UUID>();for(var r:bounded(a,org))ids.add(r.owner);a.requireCurrent();return Set.copyOf(ids);}
    @Override public Contexts list(Access a){
        lock(a);var visible=bounded(a,null).stream().filter(r->a.eligibleAccount(r.owner)).map(Row::context).toList();a.requireCurrent();return new Contexts("BUSINESS_CONTEXTS_V1",visible);
    }
    @Override public <T>T inOrganization(Access a,UUID id,Function<Scope,T> read){
        lock(a);if(id==null)throw new Failure("BUSINESS_CONTEXT_UNAVAILABLE");
        var found=bounded(a,id);if(found.size()!=1||!a.eligibleAccount(found.getFirst().owner))throw new Failure("BUSINESS_CONTEXT_UNAVAILABLE");
        var context=found.getFirst().context; boolean[] current={true};
        Scope scope=new Scope(){
            public Context context(){requireCurrent();return context;}
            public void requireCurrent(){a.requireCurrent();if(!current[0])throw new Failure("BUSINESS_CONTEXT_UNAVAILABLE");}
            public boolean permits(String permission,UUID facility){
                requireCurrent();if(!PERMISSIONS.contains(permission))return false;
                if(context.permissions().contains(permission))return true;
                return facility!=null&&facilities(permission).contains(facility);
            }
            public Set<UUID> facilities(String permission){
                requireCurrent();return Set.copyOf(jdbc.query("SELECT facility_id FROM merchant.member_grant WHERE membership_id=? AND organization_id=? AND permission=? AND scope='FACILITY'",(r,n)->r.getObject(1,UUID.class),context.membershipId(),id,permission));
            }
            public void requireActiveFacility(UUID facility){
                requireCurrent();if(facility!=null&&jdbc.queryForObject("SELECT count(*) FROM merchant.facility WHERE id=? AND organization_id=? AND state='ACTIVE'",Integer.class,facility,id)!=1)throw new Failure("BUSINESS_CONTEXT_UNAVAILABLE");
            }
        };
        try{T result=read.apply(scope);scope.requireCurrent();return result;}finally{current[0]=false;}
    }
}
