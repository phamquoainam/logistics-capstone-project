package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.merchant.api.*;
import com.elogistic.backend.merchant.api.MerchantGovernanceViews.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** M2R-01/02/03/04/05/07/08/09 only. Privacy-safe POST searches, canonical admission. */
@RestController @Profile("identity")
@ConditionalOnProperty(name={"merchant.admin-governance.enabled","merchant.admin-governance.views-enabled"},havingValue="true")
public final class MerchantGovernanceViewsHttp {
    private static final String APP="/api/v1/admin/merchant-applications",ORG="/api/v1/admin/merchant-organizations";
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final AdminAdmission reads;private final AdminCommandAdmission commands;private final MerchantGovernance governance;private final MerchantGovernanceViews views;
    public MerchantGovernanceViewsHttp(AdminAdmission reads,AdminCommandAdmission commands,MerchantGovernance governance,MerchantGovernanceViews views){this.reads=reads;this.commands=commands;this.governance=governance;this.views=views;}
    @ModelAttribute public void privacy(HttpServletResponse response){response.setHeader("Cache-Control","no-store");response.setHeader("Referrer-Policy","no-referrer");response.setHeader("X-Content-Type-Options","nosniff");}
    private <T>T run(HttpServletResponse response,UUID application,Function<AdminCommandContext,T> action){var refs=application==null?new AdminCommandAdmission.ReferencedSubjects(Set.of(),Set.of()):reads.execute(c->governance.subjects(c,application,null));return commands.execute(id(response.getHeader("X-Request-Id")),refs,action);}
    @PostMapping(APP+"/search") public ApplicationSearchPage applications(HttpServletRequest request,HttpServletResponse response)throws IOException{
        var q=body(request,ApplicationSearch.class,Set.of("from","to","state","policyId","reviewerId","applicationId","applicantAccountId","registeredName","legalIdentifier","purpose","page","dueBefore","overdue","checkCode"));
        if(q.checkCode()==null)return run(response,null,c->views.searchApplications(c,q));
        // Existing Identity port locks at most 100 referenced accounts. Discover canonical
        // candidates using the approved read port; the command recomputes scope and cohort.
        var refs=reads.execute(c->{Set<UUID> customers=new HashSet<>(),admins=new HashSet<>();
            for(int page=0;page<6;page++){
                var found=governance.applications(c,new MerchantGovernance.ApplicationQuery(q.from(),q.to(),q.state(),q.reviewerId(),q.policyId(),page,20));
                for(var item:found.items()){var subjects=governance.subjects(c,item.id(),null);customers.addAll(subjects.customers());admins.addAll(subjects.admins());if(customers.size()+admins.size()>100)throw new AdminFailure("SERVICE_UNAVAILABLE");}
                if(!found.hasNext())return new AdminCommandAdmission.ReferencedSubjects(customers,admins);
            }throw new AdminFailure("SERVICE_UNAVAILABLE");
        });return commands.execute(id(response.getHeader("X-Request-Id")),refs,c->views.searchApplications(c,q));
    }
    @PostMapping(ORG+"/search") public OrganizationSearchPage organizations(HttpServletRequest request,HttpServletResponse response)throws IOException{var q=body(request,OrganizationSearch.class,Set.of("state","policyId","organizationId","registeredName","legalIdentifier","currentOwnerAccountId","activatedFrom","activatedTo","purpose","page","admissionOrigin"));return run(response,null,c->views.searchOrganizations(c,q));}
    @PostMapping(APP+"/{applicationId}/revision-comparisons") public RevisionComparison compare(@PathVariable String applicationId,HttpServletRequest request,HttpServletResponse response)throws IOException{UUID app=id(applicationId);var q=body(request,ComparisonRequest.class,Set.of("fromRevision","toRevision","purpose"));return run(response,app,c->views.compare(c,app,q));}
    @PostMapping(ORG+"/{organizationId}/profile-disclosures") public OrganizationProfile profile(@PathVariable String organizationId,HttpServletRequest request,HttpServletResponse response)throws IOException{UUID org=id(organizationId);var q=body(request,DisclosureRequest.class,Set.of("purpose","page"));return run(response,null,c->views.profile(c,org,q));}
    @PostMapping(ORG+"/{organizationId}/membership-disclosures") public MembershipPage members(@PathVariable String organizationId,HttpServletRequest request,HttpServletResponse response)throws IOException{UUID org=id(organizationId);var q=body(request,DisclosureRequest.class,Set.of("purpose","page"));return run(response,null,c->views.members(c,org,q));}
    @PostMapping(APP+"/{applicationId}/review-comments") public Comment comment(@PathVariable String applicationId,HttpServletRequest request,HttpServletResponse response)throws IOException{UUID app=id(applicationId);var q=body(request,CommentInput.class,Set.of("revision","text"));long version=version(request);UUID key=headerId(request,"Idempotency-Key");return run(response,app,c->views.comment(c,app,q,version,key));}
    @PostMapping(APP+"/{applicationId}/comment-disclosures") public CommentPage comments(@PathVariable String applicationId,HttpServletRequest request,HttpServletResponse response)throws IOException{UUID app=id(applicationId);var q=body(request,PageQuery.class,Set.of("size","cursor"));return run(response,app,c->views.comments(c,app,q));}
    @GetMapping(APP+"/{applicationId}/checks") public Checks checks(@PathVariable String applicationId,HttpServletRequest request,HttpServletResponse response){none(request);UUID app=id(applicationId);return run(response,app,c->views.checks(c,app));}
    private static AdminFailure invalid(){return new AdminFailure("VALIDATION_FAILED");}
    private static void none(HttpServletRequest request){if(request.getQueryString()!=null||!request.getParameterMap().isEmpty())throw invalid();}
    private static UUID id(String text){try{UUID id=UUID.fromString(text);if(!id.toString().equals(text)||id.equals(new UUID(0,0)))throw invalid();return id;}catch(IllegalArgumentException|NullPointerException failure){throw invalid();}}
    private static UUID headerId(HttpServletRequest request,String name){var values=Collections.list(request.getHeaders(name));if(values.size()!=1)throw invalid();return id(values.getFirst());}
    private static long version(HttpServletRequest request){var values=Collections.list(request.getHeaders("If-Match"));if(values.isEmpty())throw new MissingVersion();try{if(values.size()!=1||!values.getFirst().matches("\"v[1-9][0-9]*\""))throw invalid();return Long.parseLong(values.getFirst().substring(2,values.getFirst().length()-1));}catch(IllegalArgumentException failure){throw invalid();}}
    private static void types(JsonNode node){for(var entry:node.properties()){var value=entry.getValue();if(value.isNull())continue;switch(entry.getKey()){
        case "page"->{if(!value.isObject()||!Set.of("size","cursor").containsAll(value.propertyNames()))throw invalid();types(value);}
        case "size","revision","fromRevision","toRevision"->{if(!value.isIntegralNumber()||!value.canConvertToLong())throw invalid();}
        case "overdue"->{if(!value.isBoolean())throw invalid();}
        default->{if(!value.isString())throw invalid();if(entry.getKey().endsWith("Id"))id(value.asString());}
    }}}
    private static <T>T body(HttpServletRequest request,Class<T> type,Set<String> fields)throws IOException{
        none(request);if(request.getContentType()==null||!request.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw invalid();byte[] bytes=request.getInputStream().readNBytes(32769);
        try{if(bytes.length>32768)throw invalid();var value=JSON.readTree(bytes);if(value==null||!value.isObject()||!fields.containsAll(value.propertyNames()))throw invalid();types(value);return JSON.treeToValue(value,type);}catch(AdminFailure known){throw known;}catch(RuntimeException failure){throw invalid();}finally{Arrays.fill(bytes,(byte)0);}
    }
    private static final class MissingVersion extends RuntimeException{}
    @ExceptionHandler(MissingVersion.class) public ResponseEntity<Map<String,String>> versionMissing(){return ResponseEntity.status(428).body(Map.of("code","PRECONDITION_REQUIRED"));}
    @ExceptionHandler(AdminFailure.class) public ResponseEntity<Map<String,String>> denied(AdminFailure failure){int status=switch(failure.code()){case "VALIDATION_FAILED"->400;case "SESSION_INVALID"->401;case "ACCESS_DENIED"->403;case "RESOURCE_NOT_FOUND"->404;default->503;};return ResponseEntity.status(status).body(Map.of("code",failure.code(),"message","Request rejected."));}
    @ExceptionHandler(GovernanceFailure.class) public ResponseEntity<Map<String,String>> rejected(GovernanceFailure failure){int status=switch(failure.code()){case VALIDATION_FAILED->400;case ACCESS_DENIED->403;case RESOURCE_NOT_FOUND->404;case PRECONDITION_STALE->412;case IDEMPOTENCY_CONFLICT,STATE_CONFLICT,BUSINESS_CONFLICT,SNAPSHOT_EXPIRED->409;case POLICY_UNAVAILABLE,CONTACT_PROOF_REQUIRED,EVIDENCE_NOT_READY,LEGACY_KEY_CLEARANCE_REQUIRED->422;default->503;};return ResponseEntity.status(status).body(Map.of("code",failure.code().name(),"message","Request rejected."));}
}
