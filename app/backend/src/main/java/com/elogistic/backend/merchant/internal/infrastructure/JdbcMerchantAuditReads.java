package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.identity.api.admin.AdminReadContext;
import com.elogistic.backend.merchant.api.GovernanceFailure;
import com.elogistic.backend.merchant.api.MerchantGovernance.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static com.elogistic.backend.merchant.api.GovernanceFailure.Code.*;

/** P0-READ-02: owning-domain metadata only; no other-domain audit SQL or private payload. */
final class JdbcMerchantAuditReads {
    private final JdbcTemplate jdbc;private final JdbcMerchantGovernance scopes;private final Clock clock;
    JdbcMerchantAuditReads(JdbcTemplate jdbc,JdbcMerchantGovernance scopes,Clock clock){this.jdbc=jdbc;this.scopes=scopes;this.clock=clock;}
    private static GovernanceFailure fail(GovernanceFailure.Code code){return new GovernanceFailure(code);}
    AuditPage read(AdminReadContext actor,AuditQuery query){
        // Verifies the exact Identity-owned callback before inspecting caller fields.
        var context=scopes.context(actor);
        if(query==null||!Set.of("MERCHANT_APPLICATION","MERCHANT_ORGANIZATION").contains(Objects.toString(query.domain(),""))||query.from()==null||query.to()==null||!query.from().isBefore(query.to())||Duration.between(query.from(),query.to()).compareTo(Duration.ofDays(366))>0||query.page()<0||query.page()>10000||query.size()<1||query.size()>50||query.targetId()!=null&&query.targetId().equals(new UUID(0,0)))throw fail(VALIDATION_FAILED);
        Kind kind=query.domain().equals("MERCHANT_APPLICATION")?Kind.APPLICATION:Kind.ORGANIZATION;
        if(query.targetId()!=null)scopes.withProjectionScope(actor,new Target(kind,query.targetId()),scope->{scope.require("audit.read",new Target(kind,query.targetId()));return null;});
        return page(actor,query,context,kind);
    }
    private AuditPage page(AdminReadContext actor,AuditQuery query,GovernanceContext context,Kind kind){
        var policies=context.assignments().stream().filter(s->s.scopeType().equals("POLICY")&&s.permissions().contains("audit.read")).map(GovernanceAssignment::policyId).distinct().toList();
        var organizations=context.assignments().stream().filter(s->s.scopeType().equals("ORGANIZATION")&&s.permissions().contains("audit.read")).map(GovernanceAssignment::organizationId).distinct().toList();
        if(policies.isEmpty()&&(kind==Kind.APPLICATION||organizations.isEmpty()))throw fail(ACCESS_DENIED);
        var args=new ArrayList<Object>();args.add(kind.name());args.add(Timestamp.from(query.from()));args.add(Timestamp.from(query.to()));
        String where="e.target_type=? AND e.occurred_at>=? AND e.occurred_at<?";
        var scopesSql=new ArrayList<String>();
        if(!policies.isEmpty()){
            String list=String.join(",",Collections.nCopies(policies.size(),"?"));
            scopesSql.add(kind==Kind.APPLICATION?"e.policy_id IN ("+list+")":"EXISTS(SELECT 1 FROM merchant.application_activation x WHERE x.organization_id=e.organization_id AND x.policy_id IN ("+list+") AND merchant.application_origin_admitted(x.organization_id))");args.addAll(policies);
        }
        if(kind==Kind.ORGANIZATION&&!organizations.isEmpty()){scopesSql.add("e.organization_id IN ("+String.join(",",Collections.nCopies(organizations.size(),"?"))+")");args.addAll(organizations);}
        where+=" AND ("+String.join(" OR ",scopesSql)+")";
        if(query.targetId()!=null){where+=" AND e.target_id=?";args.add(query.targetId());}
        args.add(query.size()+1);args.add((long)query.page()*query.size());
        var rows=jdbc.query("SELECT e.id,e.actor_id,e.operation,e.target_type,e.target_id,e.prior_version,e.new_version,e.revision,p.version AS policy_version,e.reason_code,e.occurred_at,e.request_id FROM merchant.governance_event e JOIN merchant.application_policy p ON p.id=e.policy_id WHERE "+where+" ORDER BY e.occurred_at DESC,e.id DESC LIMIT ? OFFSET ?",(r,n)->new AuditItem(r.getObject("id",UUID.class),r.getObject("actor_id",UUID.class),r.getString("operation"),r.getString("target_type"),r.getObject("target_id",UUID.class),r.getLong("prior_version"),r.getLong("new_version"),r.getObject("revision",Long.class),Long.toString(r.getLong("policy_version")),r.getString("reason_code"),r.getTimestamp("occurred_at").toInstant(),r.getObject("request_id",UUID.class)),args.toArray());
        for(var row:rows){var target=new Target(kind,row.targetId());scopes.withProjectionScope(actor,target,scope->{scope.require("audit.read",target);return null;});}
        if(!context.authorityVersion().equals(scopes.context(actor).authorityVersion()))throw fail(SNAPSHOT_EXPIRED);
        return new AuditPage(rows.stream().limit(query.size()).toList(),query.page(),query.size(),rows.size()>query.size());
    }
}
