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
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Approved P0-ID-API-01 / P0-GOV-09. A credential proof never substitutes for domain authority. */
@RestController @Profile("identity")
@ConditionalOnProperty(name="merchant.admin-governance.enabled",havingValue="true")
public final class MerchantGovernanceDecisionHttp {
    private final com.elogistic.backend.workflow.internal.application.MerchantNoticeComposition notices;
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final AdminAdmission reads;private final AdminCommandAdmission commands;private final MerchantGovernance governance;private final AdminGovernanceReauthentication proofs;
    public MerchantGovernanceDecisionHttp(AdminAdmission reads,AdminCommandAdmission commands,MerchantGovernance governance,AdminGovernanceReauthentication proofs){this(reads,commands,governance,proofs,null);}
    @org.springframework.beans.factory.annotation.Autowired
    public MerchantGovernanceDecisionHttp(AdminAdmission reads,AdminCommandAdmission commands,MerchantGovernance governance,AdminGovernanceReauthentication proofs,com.elogistic.backend.workflow.internal.application.MerchantNoticeComposition notices){this.notices=notices;this.reads=Objects.requireNonNull(reads);this.commands=Objects.requireNonNull(commands);this.governance=Objects.requireNonNull(governance);this.proofs=Objects.requireNonNull(proofs);}
    @ModelAttribute public void headers(HttpServletResponse response){response.setHeader("Cache-Control","no-store");response.setHeader("Referrer-Policy","no-referrer");response.setHeader("X-Content-Type-Options","nosniff");}
    @PostMapping("/api/v1/admin/governance-reauthentications")
    public AdminGovernanceReauthentication.StepUpResult stepUp(HttpServletRequest request,HttpServletResponse response)throws IOException{
        var body=body(request,Set.of("operation","targetId","expectedVersion","commandDigest","password"));
        String operation=text(body,"operation",32);if(!Set.of("REJECT","APPROVE","SUSPEND","REINSTATE").contains(operation)||operation.equals("APPROVE")&&notices==null)throw invalid();
        var input=new AdminGovernanceReauthentication.AdminStepUpInput(operation,id(text(body,"targetId",36)),positive(body,"expectedVersion"),text(body,"commandDigest",64),text(body,"password",72));UUID key=key(request);
        boolean organization=Set.of("SUSPEND","REINSTATE").contains(operation);var references=reads.execute(c->organization?governance.organizationSubjects(c,input.targetId()):governance.subjects(c,input.targetId(),null));var target=new Target(organization?Kind.ORGANIZATION:Kind.APPLICATION,input.targetId());
        return commands.execute(requestId(response),references,c->governance.withScope(c,target,s->{s.require(organization?(operation.equals("SUSPEND")?"org.suspend":"org.reinstate"):"application.decide",target);return proofs.request(c,input,s.version(),key);}));
    }
    @PostMapping("/api/v1/admin/merchant-applications/{applicationId}/rejection")
    public ResponseEntity<ApplicationSummary> reject(@PathVariable String applicationId,HttpServletRequest request,HttpServletResponse response)throws IOException{
        UUID application=id(applicationId),key=key(request);long version=version(request);var body=body(request,Set.of("revision","policyVersion","reauthenticationProofId","reason"));
        var input=new DecisionInput(positive(body,"revision"),text(body,"policyVersion",64),id(text(body,"reauthenticationProofId",36)),text(body,"reason",300));
        var references=reads.execute(c->governance.subjects(c,application,null));var result=commands.execute(requestId(response),references,c->{var value=governance.reject(c,application,input,version,key);if(notices!=null)notices.after(c,application,"REJECT",key);return value;});
        return ResponseEntity.ok().eTag("\"v"+result.version()+"\"").body(result);
    }
    @PostMapping("/api/v1/admin/merchant-applications/{applicationId}/approval")
    public ResponseEntity<ApplicationSummary> approve(@PathVariable String applicationId,HttpServletRequest request,HttpServletResponse response)throws IOException{
        if(notices==null)throw new AdminFailure("SERVICE_UNAVAILABLE");
        UUID application=id(applicationId),key=key(request);long version=version(request);var body=body(request,Set.of("revision","policyVersion","reauthenticationProofId","reason"));
        var input=new DecisionInput(positive(body,"revision"),text(body,"policyVersion",64),id(text(body,"reauthenticationProofId",36)),text(body,"reason",300));
        var references=reads.execute(c->governance.subjects(c,application,null));var result=commands.execute(requestId(response),references,c->{var value=governance.approve(c,application,input,version,key);if(notices!=null)notices.after(c,application,"APPROVE",key);return value;});
        return ResponseEntity.ok().eTag("\"v"+result.version()+"\"").body(result);
    }
    private static AdminFailure invalid(){return new AdminFailure("VALIDATION_FAILED");}
    private static UUID id(String value){try{UUID id=UUID.fromString(value);if(!id.toString().equals(value)||id.equals(new UUID(0,0)))throw invalid();return id;}catch(IllegalArgumentException|NullPointerException failure){throw invalid();}}
    private static UUID requestId(HttpServletResponse response){String value=response.getHeader("X-Request-Id");if(value==null)throw new AdminFailure("SESSION_INVALID");return id(value);}
    private static UUID key(HttpServletRequest request){var values=Collections.list(request.getHeaders("Idempotency-Key"));if(values.size()!=1)throw invalid();return id(values.getFirst());}
    private static long version(HttpServletRequest request){var values=Collections.list(request.getHeaders("If-Match"));if(values.isEmpty())throw new Precondition();try{if(values.size()!=1||!values.getFirst().matches("\"v[1-9][0-9]*\""))throw invalid();return Long.parseLong(values.getFirst().substring(2,values.getFirst().length()-1));}catch(IllegalArgumentException failure){throw invalid();}}
    private static String text(JsonNode body,String field,int limit){var value=body.get(field);if(value==null||!value.isString()||value.asString().isEmpty()||value.asString().codePointCount(0,value.asString().length())>limit)throw invalid();return value.asString();}
    private static long positive(JsonNode body,String field){var value=body.get(field);if(value==null||!value.isIntegralNumber()||!value.canConvertToLong()||value.asLong()<1)throw invalid();return value.asLong();}
    private static JsonNode body(HttpServletRequest request,Set<String> fields)throws IOException{
        if(request.getQueryString()!=null||!request.getParameterMap().isEmpty()||request.getContentType()==null||!request.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw invalid();byte[] bytes=request.getInputStream().readNBytes(32769);
        try{if(bytes.length>32768)throw invalid();var value=JSON.readTree(bytes);if(value==null||!value.isObject()||!value.propertyNames().equals(fields))throw invalid();return value;}
        catch(AdminFailure known){throw known;}catch(RuntimeException failure){throw invalid();}finally{Arrays.fill(bytes,(byte)0);}
    }
    private static final class Precondition extends RuntimeException{}
    @ExceptionHandler(Precondition.class) public ResponseEntity<Map<String,String>> precondition(){return problem(428,"PRECONDITION_REQUIRED");}
    @ExceptionHandler(AdminFailure.class) public ResponseEntity<Map<String,String>> denied(AdminFailure failure){return problem(switch(failure.code()){case "VALIDATION_FAILED"->400;case "SESSION_INVALID"->401;case "ACCESS_DENIED"->403;case "RESOURCE_NOT_FOUND"->404;case "IDEMPOTENCY_CONFLICT"->409;default->503;},failure.code());}
    @ExceptionHandler(GovernanceFailure.class) public ResponseEntity<Map<String,String>> rejected(GovernanceFailure failure){return problem(switch(failure.code()){case VALIDATION_FAILED->400;case ACCESS_DENIED->403;case RESOURCE_NOT_FOUND->404;case PRECONDITION_STALE->412;case STATE_CONFLICT,IDEMPOTENCY_CONFLICT,BUSINESS_CONFLICT,SNAPSHOT_EXPIRED->409;case CONTACT_PROOF_REQUIRED,EVIDENCE_NOT_READY,POLICY_UNAVAILABLE,LEGACY_KEY_CLEARANCE_REQUIRED->422;default->503;},failure.code().name());}
    private static ResponseEntity<Map<String,String>> problem(int status,String code){return ResponseEntity.status(status).body(Map.of("code",code,"message","Request rejected."));}
}
