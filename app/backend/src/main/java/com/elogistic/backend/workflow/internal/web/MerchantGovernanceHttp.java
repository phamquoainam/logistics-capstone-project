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

/** Bounded opt-in governance routes. Identity filter and domain owners remain the authority. */
@RestController @Profile("identity")
@ConditionalOnProperty(name="merchant.admin-governance.enabled",havingValue="true")
public final class MerchantGovernanceHttp {
    private static final String BASE="/api/v1/admin/merchant-applications/{applicationId}";
    private final com.elogistic.backend.workflow.internal.application.MerchantNoticeComposition notices;
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final AdminAdmission reads;private final AdminCommandAdmission commands;private final MerchantGovernance governance;
    public MerchantGovernanceHttp(AdminAdmission reads,AdminCommandAdmission commands,MerchantGovernance governance){this(reads,commands,governance,null);}
    @org.springframework.beans.factory.annotation.Autowired
    public MerchantGovernanceHttp(AdminAdmission reads,AdminCommandAdmission commands,MerchantGovernance governance,com.elogistic.backend.workflow.internal.application.MerchantNoticeComposition notices){this.notices=notices;this.reads=Objects.requireNonNull(reads);this.commands=Objects.requireNonNull(commands);this.governance=Objects.requireNonNull(governance);}
    @ModelAttribute public void headers(HttpServletResponse response){response.setHeader("Cache-Control","no-store");response.setHeader("Referrer-Policy","no-referrer");response.setHeader("X-Content-Type-Options","nosniff");}
    @GetMapping("/api/v1/admin/merchant-governance/context") public GovernanceContext context(HttpServletRequest request){none(request);return reads.execute(governance::context);}
    @GetMapping("/api/v1/admin/merchant-applications") public ApplicationSummaryPage applications(HttpServletRequest request){
        var p=parameters(request,Set.of("from","to","state","reviewerId","policyId","page","size"));
        ApplicationQuery query;try{query=new ApplicationQuery(java.time.LocalDate.parse(p.get("from")),java.time.LocalDate.parse(p.get("to")),p.get("state"),p.containsKey("reviewerId")?id(p.get("reviewerId")):null,p.containsKey("policyId")?id(p.get("policyId")):null,number(p,"page",0),number(p,"size",20));}catch(RuntimeException invalid){throw invalid();}
        return reads.execute(c->governance.applications(c,query));
    }
    @GetMapping(BASE+"/revisions") public ApplicationRevisionPage revisions(@PathVariable String applicationId,HttpServletRequest request){var p=parameters(request,Set.of("page","size"));UUID app=id(applicationId);return reads.execute(c->governance.revisions(c,app,number(p,"page",0),number(p,"size",20)));}
    @GetMapping(BASE+"/events") public AuditPage events(@PathVariable String applicationId,HttpServletRequest request){var p=parameters(request,Set.of("page","size"));UUID app=id(applicationId);return reads.execute(c->governance.events(c,app,number(p,"page",0),number(p,"size",20)));}
    @GetMapping(BASE+"/evidence/{evidenceId}/content") public ResponseEntity<byte[]> evidence(@PathVariable String applicationId,@PathVariable String evidenceId,HttpServletRequest request,HttpServletResponse response){
        var p=parameters(request,Set.of("purpose"));if(!p.keySet().equals(Set.of("purpose"))||!"MERCHANT_REVIEW".equals(p.get("purpose")))throw invalid();
        var content=governance.evidenceContent(id(applicationId),id(evidenceId),p.get("purpose"),requestId(response));
        return ResponseEntity.ok().header("Content-Disposition","attachment; filename=\"merchant-evidence\"").contentType(org.springframework.http.MediaType.parseMediaType(content.mime())).body(content.bytes());
    }
    @GetMapping("/api/v1/admin/merchant-governance/receipts/{operationId}/{idempotencyKey}") public CommandReceipt receipt(@PathVariable String operationId,@PathVariable String idempotencyKey,HttpServletRequest request){none(request);UUID key=id(idempotencyKey);return reads.execute(c->governance.receipt(c,operationId,key));}
    private static Map<String,String> parameters(HttpServletRequest request,Set<String> allowed){Map<String,String> result=new HashMap<>();request.getParameterMap().forEach((k,v)->{if(!allowed.contains(k)||v.length!=1||v[0]==null||v[0].isBlank()||v[0].length()>64)throw invalid();result.put(k,v[0]);});return result;}
    private static int number(Map<String,String> values,String key,int fallback){String value=values.get(key);if(value==null)return fallback;try{if(!value.matches("0|[1-9][0-9]{0,5}"))throw invalid();return Integer.parseInt(value);}catch(IllegalArgumentException invalid){throw invalid();}}
    @GetMapping("/api/v1/admin/merchant-governance/reviewer-candidates") public ReviewerCandidatePage candidates(HttpServletRequest request,HttpServletResponse response){
        var p=parameters(request,Set.of("policyId","page","size"));UUID policy=id(p.get("policyId"));int page=number(p,"page",0),size=number(p,"size",20);if(page>10000||size<1||size>50)throw invalid();
        List<ReviewerCandidate> eligible=new ArrayList<>();UUID cursor=null;long deadline=System.nanoTime()+java.time.Duration.ofSeconds(10).toNanos();int needed=(page+1)*size+1;
        while(eligible.size()<needed){
            if(System.nanoTime()>=deadline)throw new AdminFailure("SERVICE_UNAVAILABLE");UUID after=cursor;
            var ids=reads.execute(c->governance.reviewerCandidateIds(c,policy,after));if(ids.isEmpty())break;
            var refs=new AdminCommandAdmission.ReferencedSubjects(Set.of(),new HashSet<>(ids));eligible.addAll(commands.execute(requestId(response),refs,c->governance.eligibleReviewers(c,policy,ids)));cursor=ids.getLast();
            if(ids.size()<100)break;
        }
        int start=Math.min(page*size,eligible.size()),end=Math.min(start+size,eligible.size());return new ReviewerCandidatePage(eligible.subList(start,end),page,size,eligible.size()>end);
    }
    @GetMapping(BASE) public ResponseEntity<ApplicationSummary> application(@PathVariable String applicationId,HttpServletRequest request){none(request);UUID app=id(applicationId);return result(reads.execute(c->governance.application(c,app)));}
    @GetMapping(BASE+"/review-data") public ReviewData reviewData(@PathVariable String applicationId,HttpServletRequest request,HttpServletResponse response){
        UUID app=id(applicationId);if(!request.getParameterMap().keySet().equals(Set.of("purpose"))||request.getParameterValues("purpose").length!=1||!"MERCHANT_REVIEW".equals(request.getParameter("purpose")))throw invalid();
        var refs=reads.execute(c->governance.subjects(c,app,null));return commands.execute(requestId(response),refs,c->governance.reviewData(c,app,"MERCHANT_REVIEW"));
    }
    @PostMapping(BASE+"/review-assignment") public ResponseEntity<ApplicationSummary> assign(@PathVariable String applicationId,HttpServletRequest request,HttpServletResponse response)throws IOException{
        var value=body(request,Set.of("assigneeAccountId","reason"));var input=new ReviewerAssignmentInput(id(text(value,"assigneeAccountId",36)),text(value,"reason",300));UUID app=id(applicationId),key=key(request);long version=version(request);
        var refs=reads.execute(c->governance.subjects(c,app,input.assigneeAccountId()));return result(commands.execute(requestId(response),refs,c->governance.assign(c,app,input,version,key)));
    }
    @PostMapping(BASE+"/review") public ResponseEntity<ApplicationSummary> review(@PathVariable String applicationId,HttpServletRequest request,HttpServletResponse response)throws IOException{
        var value=body(request,Set.of("action","revision","policyVersion","checklist","reason"));var check=value.get("checklist");Checklist checklist=null;
        if(check!=null&&!check.isNull()){
            if(!check.isObject()||!check.propertyNames().equals(Set.of("registrationValid","representationValid","contactValid","duplicatesClear","conflictsClear"))||check.properties().stream().anyMatch(e->!e.getValue().isBoolean()))throw invalid();
            checklist=JSON.treeToValue(check,Checklist.class);
        }
        var input=new ReviewInput(text(value,"action",32),positive(value,"revision"),text(value,"policyVersion",64),checklist,text(value,"reason",300));UUID app=id(applicationId),key=key(request);long version=version(request);
        var refs=reads.execute(c->governance.subjects(c,app,null));return result(commands.execute(requestId(response),refs,c->governance.review(c,app,input,version,key)));
    }
    @PostMapping(BASE+"/request-information") public ResponseEntity<ApplicationSummary> information(@PathVariable String applicationId,HttpServletRequest request,HttpServletResponse response)throws IOException{
        var value=body(request,Set.of("revision","fieldCodes","reason"));var fields=value.get("fieldCodes");if(fields==null||!fields.isArray()||fields.size()<1||fields.size()>16)throw invalid();List<String> codes=new ArrayList<>();for(var field:fields){if(!field.isString())throw invalid();codes.add(field.asString());}
        var input=new InformationRequest(positive(value,"revision"),codes,text(value,"reason",300));UUID app=id(applicationId),key=key(request);long version=version(request);
        var refs=reads.execute(c->governance.subjects(c,app,null));return result(commands.execute(requestId(response),refs,c->{var committed=governance.requestInformation(c,app,input,version,key);if(notices!=null)notices.after(c,app,"REQUEST_INFORMATION",key);return committed;}));
    }
    private static ResponseEntity<ApplicationSummary> result(ApplicationSummary value){return ResponseEntity.ok().eTag("\"v"+value.version()+"\"").body(value);}
    private static AdminFailure invalid(){return new AdminFailure("VALIDATION_FAILED");}
    private static void none(HttpServletRequest request){if(request.getQueryString()!=null||!request.getParameterMap().isEmpty())throw invalid();}
    private static UUID id(String value){try{var id=UUID.fromString(value);if(!id.toString().equals(value)||id.equals(new UUID(0,0)))throw invalid();return id;}catch(IllegalArgumentException|NullPointerException failure){throw invalid();}}
    /** This response header was set by the verified Identity filter, never copied from client input. */
    private static UUID requestId(HttpServletResponse response){String value=response.getHeader("X-Request-Id");if(value==null)throw new AdminFailure("SESSION_INVALID");return id(value);}
    private static UUID key(HttpServletRequest request){var values=Collections.list(request.getHeaders("Idempotency-Key"));if(values.size()!=1)throw invalid();return id(values.getFirst());}
    private static long version(HttpServletRequest request){var values=Collections.list(request.getHeaders("If-Match"));if(values.isEmpty())throw new Problem(428,"PRECONDITION_REQUIRED");try{if(values.size()!=1||!values.getFirst().matches("\"v[1-9][0-9]*\""))throw invalid();return Long.parseLong(values.getFirst().substring(2,values.getFirst().length()-1));}catch(IllegalArgumentException failure){throw invalid();}}
    private static String text(JsonNode value,String field,int maximum){var node=value.get(field);if(node==null||!node.isString()||node.asString().isBlank()||node.asString().codePointCount(0,node.asString().length())>maximum)throw invalid();return node.asString();}
    private static long positive(JsonNode value,String field){var node=value.get(field);if(node==null||!node.isIntegralNumber()||!node.canConvertToLong()||node.asLong()<1)throw invalid();return node.asLong();}
    private static JsonNode body(HttpServletRequest request,Set<String> fields)throws IOException{
        none(request);if(request.getContentType()==null||!request.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw invalid();byte[] bytes=request.getInputStream().readNBytes(32769);
        try{if(bytes.length>32768)throw invalid();var value=JSON.readTree(bytes);if(value==null||!value.isObject()||!value.propertyNames().equals(fields))throw invalid();return value;}
        catch(AdminFailure known){throw known;}catch(RuntimeException failure){throw invalid();}finally{Arrays.fill(bytes,(byte)0);}
    }
    private static final class Problem extends RuntimeException{private final int status;Problem(int status,String code){super(code);this.status=status;}}
    @ExceptionHandler(Problem.class) public ResponseEntity<Map<String,String>> problem(Problem failure){return ResponseEntity.status(failure.status).body(Map.of("code",failure.getMessage(),"message","Request rejected."));}
    @ExceptionHandler(AdminFailure.class) public ResponseEntity<Map<String,String>> denied(AdminFailure failure){int status=switch(failure.code()){case "VALIDATION_FAILED"->400;case "SESSION_INVALID"->401;case "ACCESS_DENIED"->403;case "RESOURCE_NOT_FOUND"->404;case "IDEMPOTENCY_CONFLICT"->409;default->503;};return ResponseEntity.status(status).body(Map.of("code",failure.code(),"message","Request rejected."));}
    @ExceptionHandler(GovernanceFailure.class) public ResponseEntity<Map<String,String>> rejected(GovernanceFailure failure){int status=switch(failure.code()){case VALIDATION_FAILED->400;case ACCESS_DENIED->403;case RESOURCE_NOT_FOUND->404;case STATE_CONFLICT,IDEMPOTENCY_CONFLICT,BUSINESS_CONFLICT,SNAPSHOT_EXPIRED->409;case PRECONDITION_STALE->412;case CONTACT_PROOF_REQUIRED,EVIDENCE_NOT_READY,POLICY_UNAVAILABLE,LEGACY_KEY_CLEARANCE_REQUIRED->422;default->503;};return ResponseEntity.status(status).body(Map.of("code",failure.code().name(),"message","Request rejected."));}
}
