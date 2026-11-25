package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.merchant.api.*;
import com.elogistic.backend.merchant.api.MerchantGovernance.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;

/** P0-GOV-13..16. No Carrier or Merchant self-service execution authority. */
@RestController @Profile("identity")
@ConditionalOnProperty(name="merchant.admin-governance.enabled",havingValue="true")
public final class MerchantOrganizationGovernanceHttp {
    private static final JsonMapper JSON=JsonMapper.builder().enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final AdminAdmission reads;private final AdminCommandAdmission commands;private final MerchantGovernance governance;
    public MerchantOrganizationGovernanceHttp(AdminAdmission reads,AdminCommandAdmission commands,MerchantGovernance governance){this.reads=Objects.requireNonNull(reads);this.commands=Objects.requireNonNull(commands);this.governance=Objects.requireNonNull(governance);}
    @ModelAttribute public void privacy(HttpServletResponse response){response.setHeader("Cache-Control","no-store");response.setHeader("Referrer-Policy","no-referrer");response.setHeader("X-Content-Type-Options","nosniff");}
    @GetMapping("/api/v1/admin/merchant-organizations") public OrganizationSummaryPage list(HttpServletRequest request){
        Map<String,String[]> p=request.getParameterMap();if(!Set.of("state","policyId","page","size").containsAll(p.keySet())||p.values().stream().anyMatch(v->v.length!=1))throw invalid();
        var query=new OrganizationQuery(request.getParameter("state"),p.containsKey("policyId")?id(request.getParameter("policyId")):null,number(request,"page",0,0,10000),number(request,"size",20,1,50));return reads.execute(c->governance.organizations(c,query));
    }
    @GetMapping("/api/v1/admin/merchant-organizations/{organizationId}") public ResponseEntity<OrganizationSummary> get(@PathVariable String organizationId,HttpServletRequest request){
        if(request.getQueryString()!=null)throw invalid();var value=reads.execute(c->governance.organization(c,id(organizationId)));return ResponseEntity.ok().eTag("\"v"+value.version()+"\"").body(value);
    }
    @PostMapping("/api/v1/admin/merchant-organizations/{organizationId}/suspension") public ResponseEntity<OrganizationSummary> suspend(@PathVariable String organizationId,HttpServletRequest request,HttpServletResponse response)throws IOException{return transition(organizationId,request,response,true);}
    @PostMapping("/api/v1/admin/merchant-organizations/{organizationId}/reinstatement") public ResponseEntity<OrganizationSummary> reinstate(@PathVariable String organizationId,HttpServletRequest request,HttpServletResponse response)throws IOException{return transition(organizationId,request,response,false);}
    private ResponseEntity<OrganizationSummary> transition(String raw,HttpServletRequest request,HttpServletResponse response,boolean suspend)throws IOException{
        UUID organization=id(raw),key=headerId(request,"Idempotency-Key"),requestId=id(response.getHeader("X-Request-Id"));long version=version(request);
        if(request.getQueryString()!=null||request.getContentType()==null||!request.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw invalid();byte[] bytes=request.getInputStream().readNBytes(32769);LifecycleInput input;
        try{var value=JSON.readTree(bytes);if(bytes.length>32768||value==null||!value.isObject()||!value.propertyNames().equals(Set.of("reason","reauthenticationProofId"))||!value.get("reason").isString()||!value.get("reauthenticationProofId").isString())throw invalid();input=new LifecycleInput(value.get("reason").asString(),id(value.get("reauthenticationProofId").asString()));}catch(AdminFailure failure){throw failure;}catch(RuntimeException invalid){throw invalid();}finally{Arrays.fill(bytes,(byte)0);}
        var references=reads.execute(c->governance.organizationSubjects(c,organization));var result=commands.execute(requestId,references,c->suspend?governance.suspend(c,organization,input,version,key):governance.reinstate(c,organization,input,version,key));return ResponseEntity.ok().eTag("\"v"+result.version()+"\"").body(result);
    }
    private static AdminFailure invalid(){return new AdminFailure("VALIDATION_FAILED");}
    private static UUID id(String raw){try{UUID value=UUID.fromString(raw);if(!value.toString().equals(raw)||value.equals(new UUID(0,0)))throw invalid();return value;}catch(IllegalArgumentException|NullPointerException failure){throw invalid();}}
    private static UUID headerId(HttpServletRequest r,String name){var values=Collections.list(r.getHeaders(name));if(values.size()!=1)throw invalid();return id(values.getFirst());}
    private static long version(HttpServletRequest r){var values=Collections.list(r.getHeaders("If-Match"));if(values.isEmpty())throw new MissingPrecondition();try{if(values.size()!=1||!values.getFirst().matches("\"v[1-9][0-9]*\""))throw invalid();return Long.parseLong(values.getFirst().substring(2,values.getFirst().length()-1));}catch(IllegalArgumentException invalid){throw invalid();}}
    private static int number(HttpServletRequest r,String name,int fallback,int min,int max){String raw=r.getParameter(name);if(raw==null)return fallback;try{if(!raw.matches("0|[1-9][0-9]*"))throw invalid();int value=Integer.parseInt(raw);if(value<min||value>max)throw invalid();return value;}catch(IllegalArgumentException failure){throw invalid();}}
    private static final class MissingPrecondition extends RuntimeException{}
    @ExceptionHandler(MissingPrecondition.class) public ResponseEntity<Map<String,String>> precondition(){return problem(428,"PRECONDITION_REQUIRED");}
    @ExceptionHandler(AdminFailure.class) public ResponseEntity<Map<String,String>> denied(AdminFailure failure){return problem(switch(failure.code()){case "VALIDATION_FAILED"->400;case "SESSION_INVALID"->401;case "ACCESS_DENIED"->403;case "RESOURCE_NOT_FOUND"->404;case "IDEMPOTENCY_CONFLICT"->409;default->503;},failure.code());}
    @ExceptionHandler(GovernanceFailure.class) public ResponseEntity<Map<String,String>> rejected(GovernanceFailure failure){return problem(switch(failure.code()){case VALIDATION_FAILED->400;case ACCESS_DENIED->403;case RESOURCE_NOT_FOUND->404;case PRECONDITION_STALE->412;case STATE_CONFLICT,IDEMPOTENCY_CONFLICT,BUSINESS_CONFLICT,SNAPSHOT_EXPIRED->409;case POLICY_UNAVAILABLE,LEGACY_KEY_CLEARANCE_REQUIRED->422;default->503;},failure.code().name());}
    private static ResponseEntity<Map<String,String>> problem(int status,String code){return ResponseEntity.status(status).body(Map.of("code",code,"message","Request rejected."));}
}
