package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.finance.api.BusinessObligations;
import com.elogistic.backend.notifications.api.BusinessReporting;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

/** Admitted public sources, independent Finance verification and access audit in one transaction. */
@RestController @Profile("customer")
public final class BusinessReportingHttp {
    private static final String BASE="/api/v1/merchant-organizations/{organizationId}";
    private final BusinessPreparationHttp admission;private final BusinessReportSources sources;private final BusinessObligations finance;private final BusinessReporting reporting;private final Clock clock;
    public BusinessReportingHttp(BusinessPreparationHttp admission,BusinessReportSources sources,BusinessObligations finance,BusinessReporting reporting,@Qualifier("identityHttpClock")Clock clock){this.admission=admission;this.sources=sources;this.finance=finance;this.reporting=reporting;this.clock=clock;}
    @ModelAttribute public void noStore(jakarta.servlet.http.HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    private record Scope(BusinessPreparations.Access delegate) implements BusinessShipmentManagement.Access,BusinessObligations.Access {
        public UUID actorAccountId(){return delegate.actorAccountId();}public UUID requestId(){return delegate.requestId();}public UUID organizationId(){return scope().context().organizationId();}
        public com.elogistic.backend.merchant.api.customer.BusinessWorkspaces.Scope scope(){return delegate.scope();}public void requireCurrent(){delegate.requireCurrent();}public void meaningfulAction(){throw new IllegalStateException("REPORT_READ_CANNOT_EXTEND_ACTIVITY");}public boolean eligibleAccount(UUID id){return false;}
        public void requirePermission(String permission,UUID site){requireCurrent();if(!scope().permits(permission,site))throw new BusinessPreparations.Failure("RESOURCE_NOT_FOUND");}
    }
    private <T>T execute(String org,Function<Scope,T> work){return admission.execute(org,a->work.apply(new Scope(a)));}
    private Map<String,String> parameters(HttpServletRequest request){var result=new HashMap<String,String>();request.getParameterMap().forEach((k,v)->{if(v.length!=1)throw problem(400,"VALIDATION_FAILED");result.put(k,v[0]);});return Map.copyOf(result);}
    private BusinessReportSources.Cohort capture(Scope a,BusinessReporting.Query q,String kind,boolean exporting){return sources.capture(a,q.source(),kind,exporting,rows->finance.validateCaptured(a,rows.stream().map(r->new BusinessObligations.Captured(r.shipmentId(),r.quoteId(),r.facilityId(),r.amountVnd())).toList()));}
    @GetMapping(BASE+"/operations-reports") public BusinessReporting.Operations operations(@PathVariable String organizationId,HttpServletRequest request){var params=parameters(request);return execute(organizationId,a->{var q=reporting.query(params,clock.instant(),"OPERATIONS",false);return reporting.operations(a.organizationId(),q,capture(a,q,"OPERATIONS",false));});}
    @GetMapping(BASE+"/shipping-finance") public BusinessReporting.Finance finance(@PathVariable String organizationId,HttpServletRequest request){var params=parameters(request);return execute(organizationId,a->{var q=reporting.query(params,clock.instant(),"FINANCE",false);return reporting.finance(a.organizationId(),q,capture(a,q,"FINANCE",false));});}
    @GetMapping(BASE+"/shipping-report-exports") public BusinessReporting.Export export(@PathVariable String organizationId,HttpServletRequest request){
        var p=new HashMap<>(parameters(request));String kind=p.remove("kind"),section=p.remove("section");if(kind==null||section==null||!Set.of("trend","facility","creator","channel","service","route").contains(section))throw problem(400,"VALIDATION_FAILED");
        return execute(organizationId,a->{var q=reporting.query(p,clock.instant(),kind,true);var cohort=capture(a,q,kind,true);var result=reporting.export(a.organizationId(),q,cohort,kind,section);var canonical=new HashMap<>(q.source());canonical.put("interval",q.interval());UUID id=sources.auditExport(a,kind,section,canonical,cohort.rows().stream().map(BusinessReportSources.Row::shipmentId).toList(),result.rows().size(),cohort.generatedAt());return new BusinessReporting.Export(id,a.organizationId().toString(),cohort.generatedAt(),"Asia/Ho_Chi_Minh",kind,section,result.rows().size(),result.columns(),result.rows(),"ORGANIZATION_REPORT_EXPORT_1");});
    }
    @GetMapping(BASE+"/shipping-report-history") public Map<String,Object> history(@PathVariable String organizationId,HttpServletRequest request){
        var p=parameters(request);int page;try{if(!Set.of("page","size").containsAll(p.keySet())||!"20".equals(p.getOrDefault("size","20"))||!p.getOrDefault("page","0").matches("0|[1-9][0-9]{0,9}"))throw problem(400,"VALIDATION_FAILED");page=Integer.parseInt(p.getOrDefault("page","0"));}catch(NumberFormatException invalid){throw problem(400,"VALIDATION_FAILED");}
        return execute(organizationId,a->{var result=sources.history(a,page);return Map.of("items",result.items(),"page",page,"size",20,"hasNext",result.hasNext());});
    }
}
