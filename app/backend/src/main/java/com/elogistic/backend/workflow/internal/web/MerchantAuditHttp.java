package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.merchant.api.GovernanceFailure;
import com.elogistic.backend.merchant.api.MerchantGovernance;
import com.elogistic.backend.merchant.api.MerchantGovernance.*;
import jakarta.servlet.http.*;
import java.time.Instant;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** P0-READ-02: no IAM, cross-domain audit or evidence-disclosure authority. */
@RestController @Profile("identity")
@ConditionalOnProperty(name="merchant.admin-governance.enabled",havingValue="true")
public final class MerchantAuditHttp {
    private final AdminAdmission reads;private final MerchantGovernance governance;
    public MerchantAuditHttp(AdminAdmission reads,MerchantGovernance governance){this.reads=Objects.requireNonNull(reads);this.governance=Objects.requireNonNull(governance);}
    @GetMapping("/api/v1/admin/audit") public AuditPage audit(HttpServletRequest request,HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");response.setHeader("Referrer-Policy","no-referrer");response.setHeader("X-Content-Type-Options","nosniff");
        var parameters=request.getParameterMap();if(!Set.of("domain","targetId","from","to","page","size").containsAll(parameters.keySet())||parameters.values().stream().anyMatch(v->v.length!=1||v[0]==null||v[0].length()>80))throw invalid();
        AuditQuery query;try{query=new AuditQuery(request.getParameter("domain"),parameters.containsKey("targetId")?id(request.getParameter("targetId")):null,Instant.parse(request.getParameter("from")),Instant.parse(request.getParameter("to")),number(request,"page",0,0,10000),number(request,"size",20,1,50));}catch(RuntimeException failure){throw invalid();}
        return reads.execute(actor->governance.audit(actor,query));
    }
    private static GovernanceFailure invalid(){return new GovernanceFailure(GovernanceFailure.Code.VALIDATION_FAILED);}
    private static UUID id(String value){UUID id=UUID.fromString(value);if(!id.toString().equals(value)||id.equals(new UUID(0,0)))throw invalid();return id;}
    private static int number(HttpServletRequest request,String key,int fallback,int min,int max){String value=request.getParameter(key);if(value==null)return fallback;if(!value.matches("0|[1-9][0-9]{0,5}"))throw invalid();int number=Integer.parseInt(value);if(number<min||number>max)throw invalid();return number;}
    @ExceptionHandler(GovernanceFailure.class) public ResponseEntity<Map<String,String>> rejected(GovernanceFailure failure){return problem(switch(failure.code()){case VALIDATION_FAILED->400;case ACCESS_DENIED->403;case RESOURCE_NOT_FOUND->404;case SNAPSHOT_EXPIRED,STATE_CONFLICT,IDEMPOTENCY_CONFLICT->409;case POLICY_UNAVAILABLE->422;default->503;},failure.code().name());}
    @ExceptionHandler(AdminFailure.class) public ResponseEntity<Map<String,String>> denied(AdminFailure failure){return problem(switch(failure.code()){case "SESSION_INVALID"->401;case "ACCESS_DENIED"->403;default->503;},failure.code());}
    private static ResponseEntity<Map<String,String>> problem(int status,String code){return ResponseEntity.status(status).header("Cache-Control","no-store").body(Map.of("code",code,"message","Request rejected."));}
}
