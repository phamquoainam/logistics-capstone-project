package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.merchant.api.*;
import com.elogistic.backend.merchant.api.OrganizationGovernanceCases.*;
import com.elogistic.backend.merchant.api.OrganizationGovernanceCases.Case;
import com.elogistic.backend.merchant.api.MerchantGovernanceViews.PageQuery;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Exact M2L Step A routes. No decision, execution, Customer consent or state activation. */
@RestController @Profile("identity")
@ConditionalOnProperty(name={"merchant.admin-governance.enabled","merchant.admin-governance.cases-enabled"},havingValue="true")
public final class OrganizationGovernanceCasesHttp {
    private static final String BASE="/api/v1/admin/merchant-organizations/{organizationId}/governance-cases";
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final AdminAdmission reads;private final AdminCommandAdmission commands;private final OrganizationGovernanceCases cases;
    public OrganizationGovernanceCasesHttp(AdminAdmission reads,AdminCommandAdmission commands,OrganizationGovernanceCases cases){this.reads=reads;this.commands=commands;this.cases=cases;}
    @ModelAttribute public void privacy(HttpServletResponse r){r.setHeader("Cache-Control","no-store");r.setHeader("Referrer-Policy","no-referrer");r.setHeader("X-Content-Type-Options","nosniff");}
    private static AdminFailure invalid(){return new AdminFailure("VALIDATION_FAILED");}
    private static UUID id(String s){try{UUID value=UUID.fromString(s);if(!value.toString().equals(s)||value.equals(new UUID(0,0)))throw invalid();return value;}catch(IllegalArgumentException|NullPointerException e){throw invalid();}}
    private static String text(JsonNode n,String field){var v=n.get(field);if(v==null||!v.isString())throw invalid();return v.asString();}
    private static long number(JsonNode n,String field){var v=n.get(field);if(v==null||!v.isIntegralNumber()||!v.canConvertToLong()||v.asLong()<1)throw invalid();return v.asLong();}
    private static void fields(JsonNode n,Set<String> allowed){if(n==null||!n.isObject()||!allowed.containsAll(n.propertyNames()))throw invalid();}
    private static Instant instant(JsonNode n,String name,boolean optional){var v=n.get(name);if(v==null||v.isNull()){if(optional)return null;throw invalid();}try{return Instant.parse(text(n,name));}catch(RuntimeException e){throw invalid();}}
    private static List<String> strings(JsonNode n,String field){var v=n.get(field);if(v==null||!v.isArray()||v.size()>16)throw invalid();List<String> values=new ArrayList<>();for(var item:v){if(!item.isString())throw invalid();values.add(item.asString());}return values;}
    private static CasePayload payload(String type,JsonNode n){if(n==null||!n.isObject())throw invalid();return switch(type){
        case "CLOSE"->{fields(n,Set.of());yield new Closure();}
        case "RESTRICT"->{fields(n,Set.of("deniedCapabilities","effectiveAt","expiresAt"));yield new Restriction(strings(n,"deniedCapabilities"),instant(n,"effectiveAt",false),instant(n,"expiresAt",true));}
        case "RISK_HOLD"->{fields(n,Set.of("holdCode","expiresAt"));yield new RiskHold(text(n,"holdCode"),instant(n,"expiresAt",true));}
        default->throw new GovernanceFailure(GovernanceFailure.Code.POLICY_UNAVAILABLE);
    };}
    private static void query(HttpServletRequest r,Set<String> allowed){if(!allowed.containsAll(r.getParameterMap().keySet())||r.getParameterMap().values().stream().anyMatch(v->v.length!=1))throw invalid();}
    private static PageQuery page(HttpServletRequest r){String size=r.getParameter("size");try{int count=size==null?20:Integer.parseInt(size);if(count<1||count>50||size!=null&&!Integer.toString(count).equals(size))throw invalid();String cursor=r.getParameter("cursor");if(cursor!=null&&cursor.length()>8192)throw invalid();return new PageQuery(count,cursor);}catch(NumberFormatException e){throw invalid();}}
    private static JsonNode body(HttpServletRequest r,Set<String> allowed)throws IOException{
        query(r,Set.of());if(r.getContentType()==null||!r.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw invalid();byte[] bytes=r.getInputStream().readNBytes(32769);
        try{if(bytes.length>32768)throw invalid();var n=JSON.readTree(bytes);fields(n,allowed);return n;}catch(AdminFailure e){throw e;}catch(RuntimeException e){throw invalid();}finally{Arrays.fill(bytes,(byte)0);}
    }
    private static UUID header(HttpServletRequest r,String name){var values=Collections.list(r.getHeaders(name));if(values.size()!=1)throw invalid();return id(values.getFirst());}
    private static long version(HttpServletRequest r){var values=Collections.list(r.getHeaders("If-Match"));if(values.isEmpty())throw new MissingVersion();if(values.size()!=1||!values.getFirst().matches("\"v[1-9][0-9]*\""))throw invalid();try{return Long.parseLong(values.getFirst().substring(2,values.getFirst().length()-1));}catch(NumberFormatException e){throw invalid();}}
    private <T>T run(UUID org,UUID caseId,UUID candidate,HttpServletResponse response,Function<AdminCommandContext,T> action){
        var refs=reads.execute(c->cases.subjects(c,org,caseId));var admins=new HashSet<>(refs.admins());if(candidate!=null)admins.add(candidate);
        return commands.execute(id(response.getHeader("X-Request-Id")),new AdminCommandAdmission.ReferencedSubjects(refs.customers(),admins),action);
    }
    @GetMapping(BASE) public CasePage list(@PathVariable String organizationId,HttpServletRequest r){query(r,Set.of("type","state","assignedToMe","size","cursor"));String mine=r.getParameter("assignedToMe");if(mine!=null&&!Set.of("true","false").contains(mine))throw invalid();var q=new CaseQuery(r.getParameter("type"),r.getParameter("state"),"true".equals(mine),page(r));return reads.execute(c->cases.list(c,id(organizationId),q));}
    @GetMapping(BASE+"/{caseId}") public Case detail(@PathVariable String organizationId,@PathVariable String caseId,HttpServletRequest r){query(r,Set.of());return reads.execute(c->cases.get(c,id(organizationId),id(caseId)));}
    @GetMapping(BASE+"/{caseId}/events") public CaseEventPage events(@PathVariable String organizationId,@PathVariable String caseId,HttpServletRequest r){query(r,Set.of("size","cursor"));var p=page(r);return reads.execute(c->cases.events(c,id(organizationId),id(caseId),p));}
    // Registered only after e4de5f4b... PG18 positive/foreign/revoked/missing-policy gates PASS.
    @GetMapping("/api/v1/admin/merchant-organizations/{organizationId}/governance-policies") public CasePolicies policies(@PathVariable String organizationId,HttpServletRequest r){query(r,Set.of());return reads.execute(c->cases.policies(c,id(organizationId)));}
    // Registered after 191afe7f... actual PG18 disclosure authorization/redaction/audit gates PASS.
    @PostMapping(BASE+"/{caseId}/disclosures") public CaseDisclosure disclose(@PathVariable String organizationId,@PathVariable String caseId,HttpServletRequest r,HttpServletResponse response)throws IOException{var n=body(r,Set.of("expectedCaseVersion","purpose"));var input=new CaseDisclosureInput(number(n,"expectedCaseVersion"),text(n,"purpose"));UUID org=id(organizationId),key=id(caseId);return run(org,key,null,response,c->cases.disclose(c,org,key,input));}
    @PostMapping(BASE) public Case create(@PathVariable String organizationId,HttpServletRequest r,HttpServletResponse response)throws IOException{
        var n=body(r,Set.of("type","reason","policyId","policyVersion","expectedOrganizationVersion","payload"));String type=text(n,"type");var input=new CaseInput(type,text(n,"reason"),id(text(n,"policyId")),number(n,"policyVersion"),number(n,"expectedOrganizationVersion"),payload(type,n.get("payload")));UUID key=header(r,"Idempotency-Key"),org=id(organizationId);return run(org,null,null,response,c->cases.create(c,org,input,key));
    }
    @PostMapping({BASE+"/{caseId}/assignment",BASE+"/{caseId}/review",BASE+"/{caseId}/request-information",BASE+"/{caseId}/resubmission",BASE+"/{caseId}/submission",BASE+"/{caseId}/cancellation"}) public Case command(@PathVariable String organizationId,@PathVariable String caseId,HttpServletRequest r,HttpServletResponse response)throws IOException{
        String operation=r.getRequestURI().substring(r.getRequestURI().lastIndexOf('/')+1);
        if(!Set.of("assignment","review","request-information","resubmission","submission","cancellation").contains(operation))throw new AdminFailure("RESOURCE_NOT_FOUND");UUID org=id(organizationId),caseKey=id(caseId),key=header(r,"Idempotency-Key");long expected=version(r);CaseCommand input;
        switch(operation){
            case "assignment"->{var n=body(r,Set.of("reviewerAccountId","reason"));input=new AssignmentInput(id(text(n,"reviewerAccountId")),text(n,"reason"));}
            case "review"->{var n=body(r,Set.of("action","checklist","reason"));Checklist check=null;var item=n.get("checklist");if(item!=null&&!item.isNull()){fields(item,Set.of("authority","conflictClear","evidenceComplete","impactReviewed"));if(item.size()!=4||item.properties().stream().anyMatch(e->!e.getValue().isBoolean()))throw invalid();check=new Checklist(item.get("authority").asBoolean(),item.get("conflictClear").asBoolean(),item.get("evidenceComplete").asBoolean(),item.get("impactReviewed").asBoolean());}input=new CaseReviewInput(text(n,"action"),check,text(n,"reason"));}
            case "request-information"->{var n=body(r,Set.of("fieldCodes","reason"));input=new InformationInput(strings(n,"fieldCodes"),text(n,"reason"));}
            case "resubmission"->{var n=body(r,Set.of("casePayload","evidenceIds","reason","reauthenticationProofId"));var evidence=n.get("evidenceIds");if(evidence==null||!evidence.isArray()||!evidence.isEmpty()||n.hasNonNull("reauthenticationProofId"))throw new GovernanceFailure(GovernanceFailure.Code.POLICY_UNAVAILABLE);String type=reads.execute(c->cases.get(c,org,caseKey)).type();input=new ResponseInput(payload(type,n.get("casePayload")),List.of(),text(n,"reason"),null);}
            default->{var n=body(r,Set.of("reason"));input=new ReasonAction(operation.equals("submission")?ReasonTag.SUBMIT:ReasonTag.CANCEL,new ReasonInput(text(n,"reason")));}
        }
        return run(org,caseKey,input instanceof AssignmentInput a?a.reviewerAccountId():null,response,c->cases.command(c,org,caseKey,input,expected,key));
    }
    private static final class MissingVersion extends RuntimeException{}
    @ExceptionHandler(MissingVersion.class) public ResponseEntity<Map<String,String>> missing(){return ResponseEntity.status(428).body(Map.of("code","PRECONDITION_REQUIRED"));}
    @ExceptionHandler(AdminFailure.class) public ResponseEntity<Map<String,String>> denied(AdminFailure e){int status=switch(e.code()){case "VALIDATION_FAILED"->400;case "SESSION_INVALID"->401;case "ACCESS_DENIED"->403;case "RESOURCE_NOT_FOUND"->404;default->503;};return ResponseEntity.status(status).body(Map.of("code",e.code(),"message","Request rejected."));}
    @ExceptionHandler(GovernanceFailure.class) public ResponseEntity<Map<String,String>> denied(GovernanceFailure e){int status=switch(e.code()){case VALIDATION_FAILED->400;case ACCESS_DENIED->403;case RESOURCE_NOT_FOUND->404;case PRECONDITION_STALE->412;case STATE_CONFLICT,IDEMPOTENCY_CONFLICT,SNAPSHOT_EXPIRED->409;case POLICY_UNAVAILABLE->422;default->503;};return ResponseEntity.status(status).body(Map.of("code",e.code().name(),"message","Request rejected."));}
}
