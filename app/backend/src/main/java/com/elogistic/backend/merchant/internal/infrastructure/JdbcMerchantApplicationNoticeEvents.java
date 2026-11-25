package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.merchant.api.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Reads Merchant-owned rows only, after exact Identity owner admission and current domain scope. */
public final class JdbcMerchantApplicationNoticeEvents implements MerchantApplicationNoticeEvents {
    private final DataSource source;private final JdbcTemplate jdbc;private final CustomerAdmission customers;
    private final AdminCommandAdmission admins;private final MerchantGovernance governance;
    public JdbcMerchantApplicationNoticeEvents(DataSource source,CustomerAdmission customers,AdminCommandAdmission admins,MerchantGovernance governance){this.source=Objects.requireNonNull(source);jdbc=new JdbcTemplate(source);this.customers=Objects.requireNonNull(customers);this.admins=Objects.requireNonNull(admins);this.governance=Objects.requireNonNull(governance);}
    private static GovernanceFailure denied(){return new GovernanceFailure(GovernanceFailure.Code.ACCESS_DENIED);}
    private void transaction(){if(!TransactionSynchronizationManager.isActualTransactionActive()||TransactionSynchronizationManager.isCurrentTransactionReadOnly()||!TransactionSynchronizationManager.hasResource(source))throw denied();}
    @Override public Optional<Event> commandEvent(AdminCommandContext context,UUID app,String operation,UUID key){
        admins.requireCurrent(context);transaction();String permission=switch(operation==null?"":operation){case "REQUEST_INFORMATION"->"application.review";case "REJECT","APPROVE"->"application.decide";default->throw denied();};
        return governance.withScope(context,new MerchantGovernance.Target(MerchantGovernance.Kind.APPLICATION,app),scope->{
            scope.require(permission,new MerchantGovernance.Target(MerchantGovernance.Kind.APPLICATION,app));
            UUID applicant=jdbc.query("SELECT applicant_account_id FROM merchant.application WHERE id=?",(r,n)->r.getObject(1,UUID.class),app).stream().findFirst().orElseThrow(JdbcMerchantApplicationNoticeEvents::denied);
            if(!context.customer(applicant).eligible())throw denied();
            var result=lookup("SYSTEM",context.accountId(),context.realmId(),app,operation,key,applicant);admins.requireCurrent(context);return result;
        });
    }
    @Override public Optional<Event> commandEvent(CustomerAdmission.Context context,UUID app,String operation,UUID key){
        customers.requireCurrent(context);transaction();if(!"WITHDRAW".equals(operation))throw denied();
        UUID applicant=jdbc.query("SELECT applicant_account_id FROM merchant.application WHERE id=? AND applicant_account_id=?",(r,n)->r.getObject(1,UUID.class),app,context.actor().accountId()).stream().findFirst().orElseThrow(JdbcMerchantApplicationNoticeEvents::denied);
        if(!context.actor().eligible())throw denied();var result=lookup("CUSTOMER",applicant,null,app,operation,key,applicant);customers.requireCurrent(context);return result;
    }
    private Optional<Event> lookup(String kind,UUID actor,UUID realm,UUID app,String operation,UUID key,UUID applicant){
        if(app==null||key==null||app.equals(new UUID(0,0))||key.equals(new UUID(0,0)))throw denied();
        var rows=jdbc.queryForList("SELECT e.id,e.new_version,e.operation,a.version,a.state,(e.xmin::text=pg_current_xact_id()::text AND r.xmin::text=pg_current_xact_id()::text) AS fresh FROM merchant.governance_receipt r JOIN merchant.governance_event e ON e.id=r.event_id JOIN merchant.application a ON a.id=e.application_id WHERE r.actor_kind=? AND r.actor_binding=? AND r.realm_id IS NOT DISTINCT FROM ?::uuid AND r.operation=? AND r.operation_key=? AND r.target_type='APPLICATION' AND r.target_id=? AND r.outcome='COMMITTED' AND r.input_key_id<>'SQL_SHA256_1' AND e.actor_kind=r.actor_kind AND e.actor_id::text=r.actor_binding AND e.realm_id IS NOT DISTINCT FROM r.realm_id AND e.operation=r.operation AND e.target_type=r.target_type AND e.target_id=r.target_id AND e.application_id=r.target_id AND e.prior_version=r.original_version AND e.new_version=r.result_version AND e.policy_id IS NOT DISTINCT FROM a.policy_id AND a.applicant_account_id=?",kind,actor.toString(),realm,operation,key,app,applicant);
        if(rows.size()!=1)throw denied();var row=rows.getFirst();if(!Boolean.TRUE.equals(row.get("fresh")))return Optional.empty();
        String template=switch(operation){case "REQUEST_INFORMATION"->"REQUEST_INFORMATION";case "REJECT"->"REJECTED";case "APPROVE"->"APPROVED";case "WITHDRAW"->"WITHDRAWN";default->throw denied();};
        String state=switch(operation){case "REQUEST_INFORMATION"->"NEEDS_INFORMATION";default->template;};
        if(!Objects.equals(row.get("version"),row.get("new_version"))||!state.equals(row.get("state")))throw denied();
        return Optional.of(new Event((UUID)row.get("id"),app,applicant,template));
    }
}
