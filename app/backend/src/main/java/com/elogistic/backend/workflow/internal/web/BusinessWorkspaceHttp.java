package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.merchant.api.customer.BusinessWorkspaces;
import com.elogistic.backend.notifications.api.BusinessDashboard;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Function;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

@RestController @Profile("customer")
public final class BusinessWorkspaceHttp {
    private final CustomerAdmission admission;private final CustomerRequestContext requests;private final BusinessWorkspaces workspaces;private final BusinessDashboard dashboard;
    public BusinessWorkspaceHttp(CustomerAdmission admission,CustomerRequestContext requests,BusinessWorkspaces workspaces,BusinessDashboard dashboard){this.admission=admission;this.requests=requests;this.workspaces=workspaces;this.dashboard=dashboard;}
    private BusinessWorkspaces.Access access(CustomerAdmission.Context c){return new BusinessWorkspaces.Access(){
        public UUID accountId(){requireCurrent();return c.actor().accountId();} public void requireCurrent(){admission.requireCurrent(c);}
        public boolean eligibleAccount(UUID id){requireCurrent();return c.subjects().containsKey(id)&&c.subjects().get(id).eligible();}
    };}
    private <T>T read(UUID org,Function<BusinessWorkspaces.Access,T> read){
        var request=requests.current();
        try{
            // Read discovery carries no authority: actual current binding is checked again
            // after Identity has locked all referenced immutable initial-owner subjects.
            var references=admission.execute(request.encodedAccess(),request.requestId(),Set.of(),c->workspaces.references(access(c),org));
            return admission.execute(request.encodedAccess(),request.requestId(),references,c->read.apply(access(c)));
        }catch(BusinessWorkspaces.Failure | BusinessDashboard.Failure rejected){throw problem(switch(rejected.getMessage()){case "VALIDATION_FAILED"->400;case "BUSINESS_CONTEXT_UNAVAILABLE"->404;default->503;},rejected.getMessage());}
    }
    @GetMapping("/api/v1/customer/business-contexts") public ResponseEntity<BusinessWorkspaces.Contexts> contexts(HttpServletRequest r){
        if(!r.getParameterMap().isEmpty())throw problem(400,"VALIDATION_FAILED");return ResponseEntity.ok().header("Cache-Control","no-store").body(read(null,workspaces::list));
    }
    @GetMapping("/api/v1/merchant-organizations/{organizationId}/dashboard") public ResponseEntity<BusinessDashboard.Report> dashboard(@PathVariable String organizationId,HttpServletRequest r){
        UUID id;try{id=UUID.fromString(organizationId);if(!id.toString().equals(organizationId)||id.equals(new UUID(0,0)))throw new IllegalArgumentException();}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}
        Map<String,String> query=new HashMap<>();r.getParameterMap().forEach((k,v)->{if(v.length!=1)throw problem(400,"VALIDATION_FAILED");query.put(k,v[0]);});
        return ResponseEntity.ok().header("Cache-Control","no-store").body(read(id,a->workspaces.inOrganization(a,id,s->dashboard.read(new BusinessDashboard.Access(){
            public void requireCurrent(){s.requireCurrent();}
            public BusinessDashboard.Context context(){var c=s.context();return new BusinessDashboard.Context(c.organizationId(),c.displayName(),c.membershipId(),c.authorityVersion());}
            public Set<String> permissions(){return Set.copyOf(s.context().permissions());}
        },Map.copyOf(query)))));
    }
}
