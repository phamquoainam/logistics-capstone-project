package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.merchant.api.customer.MerchantApplications;
import com.elogistic.backend.merchant.api.customer.MerchantApplications.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

/** Only approved applicant endpoints; existing Customer filter owns HTTPS/Origin/CSRF/session checks. */
@RestController @Profile("customer")
public final class MerchantApplicationWorkflowHttp {
    private final com.elogistic.backend.workflow.internal.application.MerchantNoticeComposition notices;private final boolean noticeRequired;
    private static final String BASE="/api/v1/merchant-applications/{applicationId}";
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final CustomerAdmission admission;private final CustomerRequestContext requests;private final MerchantApplications applications;
    public MerchantApplicationWorkflowHttp(CustomerAdmission admission,CustomerRequestContext requests,MerchantApplications applications){this.notices=null;this.noticeRequired=false;this.admission=Objects.requireNonNull(admission);this.requests=Objects.requireNonNull(requests);this.applications=Objects.requireNonNull(applications);}
    @org.springframework.beans.factory.annotation.Autowired
    public MerchantApplicationWorkflowHttp(CustomerAdmission admission,CustomerRequestContext requests,MerchantApplications applications,org.springframework.beans.factory.ObjectProvider<com.elogistic.backend.workflow.internal.application.MerchantNoticeComposition> notices){this.notices=notices.getIfAvailable();this.noticeRequired=true;this.admission=Objects.requireNonNull(admission);this.requests=Objects.requireNonNull(requests);this.applications=Objects.requireNonNull(applications);}
    @GetMapping(BASE+"/requirements") public ResponseEntity<ApplicationRequirements> requirements(@PathVariable String applicationId,HttpServletRequest http){
        query(http);UUID application=uuid(applicationId);var request=requests.current();var result=admission.execute(request.encodedAccess(),request.requestId(),Set.of(),actor->applications.requirements(actor,application));
        return ResponseEntity.ok().header("Cache-Control","no-store").body(result);
    }
    @PostMapping(BASE+"/submit") public ResponseEntity<Map<String,Object>> submit(@PathVariable String applicationId,HttpServletRequest http)throws IOException{return submission(applicationId,http,false);}
    @PostMapping(BASE+"/resubmit") public ResponseEntity<Map<String,Object>> resubmit(@PathVariable String applicationId,HttpServletRequest http)throws IOException{return submission(applicationId,http,true);}
    private ResponseEntity<Map<String,Object>> submission(String applicationId,HttpServletRequest http,boolean resubmit)throws IOException{
        UUID app=uuid(applicationId),key=key(http);long version=version(http);var input=new SubmissionInput(body(http,"organizationDisplayName",160));var request=requests.current();
        Application result=admission.execute(request.encodedAccess(),request.requestId(),Set.of(),c->resubmit?applications.resubmit(c,app,input,version,key):applications.submit(c,app,input,version,key));
        return ResponseEntity.ok().header("Cache-Control","no-store").eTag("\"v"+result.version()+"\"").body(MerchantApplicationHttp.wire(result));
    }
    @PostMapping(BASE+"/withdrawal") public ResponseEntity<Map<String,Object>> withdraw(@PathVariable String applicationId,HttpServletRequest http)throws IOException{
        UUID app=uuid(applicationId),key=key(http);long version=version(http);var input=new ReasonInput(body(http,"reason",300));var request=requests.current();
        var result=admission.execute(request.encodedAccess(),request.requestId(),Set.of(),c->{if(noticeRequired&&notices==null)throw new com.elogistic.backend.merchant.api.GovernanceFailure(com.elogistic.backend.merchant.api.GovernanceFailure.Code.SERVICE_UNAVAILABLE);var value=applications.withdraw(c,app,input,version,key);if(notices!=null)notices.after(c,app,"WITHDRAW",key);return value;});
        return ResponseEntity.ok().header("Cache-Control","no-store").eTag("\"v"+result.version()+"\"").body(MerchantApplicationHttp.wire(result));
    }
    @PostMapping(BASE+"/evidence") public ResponseEntity<Evidence> upload(@PathVariable String applicationId,HttpServletRequest http)throws IOException{
        UUID app=uuid(applicationId),key=key(http);if(http.getQueryString()!=null||!(http instanceof MultipartHttpServletRequest multipart)||!multipart.getMultiFileMap().keySet().equals(Set.of("file"))||multipart.getMultiFileMap().get("file").size()!=1||!http.getParameterMap().keySet().equals(Set.of("purpose"))||http.getParameterValues("purpose").length!=1)throw problem(400,"VALIDATION_FAILED");
        String purpose=http.getParameter("purpose");var file=multipart.getFile("file");if(file==null||file.getSize()<1||file.getSize()>5*1024*1024)throw problem(400,"VALIDATION_FAILED");
        byte[] bytes;try(var stream=file.getInputStream()){bytes=stream.readNBytes(5*1024*1024+1);}
        try{
            if(bytes.length!=file.getSize())throw problem(400,"VALIDATION_FAILED");var request=requests.current();
            var result=applications.uploadEvidence(request.encodedAccess(),request.requestId(),app,purpose,file.getContentType(),bytes,key);
            return ResponseEntity.status(201).header("Cache-Control","no-store").eTag("\"v"+result.version()+"\"").body(result);
        }finally{Arrays.fill(bytes,(byte)0);}
    }
    @GetMapping(BASE+"/evidence/{evidenceId}") public ResponseEntity<Evidence> evidence(@PathVariable String applicationId,@PathVariable String evidenceId,HttpServletRequest http){
        query(http);UUID app=uuid(applicationId),id=uuid(evidenceId);var request=requests.current();var result=admission.execute(request.encodedAccess(),request.requestId(),Set.of(),c->applications.evidence(c,app,id));
        return ResponseEntity.ok().header("Cache-Control","no-store").eTag("\"v"+result.version()+"\"").body(result);
    }
    @GetMapping(BASE+"/evidence/{evidenceId}/content") public ResponseEntity<byte[]> content(@PathVariable String applicationId,@PathVariable String evidenceId,HttpServletRequest http){
        query(http);UUID app=uuid(applicationId),id=uuid(evidenceId);var request=requests.current();var metadata=admission.execute(request.encodedAccess(),request.requestId(),Set.of(),c->applications.evidence(c,app,id));
        byte[] result=applications.evidenceContent(request.encodedAccess(),request.requestId(),app,id);
        return ResponseEntity.ok().header("Cache-Control","no-store").header("X-Content-Type-Options","nosniff").header("Content-Disposition","attachment; filename=\"merchant-evidence\"").contentType(MediaType.parseMediaType(metadata.mime())).body(result);
    }
    @DeleteMapping(BASE+"/evidence/{evidenceId}") public ResponseEntity<Void> archive(@PathVariable String applicationId,@PathVariable String evidenceId,HttpServletRequest http){
        query(http);if(http.getContentLengthLong()>0)throw problem(400,"VALIDATION_FAILED");UUID app=uuid(applicationId),id=uuid(evidenceId),key=key(http);long version=version(http);var request=requests.current();
        admission.execute(request.encodedAccess(),request.requestId(),Set.of(),c->{applications.archiveEvidence(c,app,id,version,key);return null;});return ResponseEntity.noContent().header("Cache-Control","no-store").build();
    }
    private static void query(HttpServletRequest http){if(http.getQueryString()!=null||!http.getParameterMap().isEmpty())throw problem(400,"VALIDATION_FAILED");}
    private static UUID uuid(String value){try{UUID id=UUID.fromString(value);if(!id.toString().equals(value)||id.equals(new UUID(0,0)))throw new IllegalArgumentException();return id;}catch(IllegalArgumentException|NullPointerException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static UUID key(HttpServletRequest http){var headers=Collections.list(http.getHeaders("Idempotency-Key"));if(headers.size()!=1)throw problem(400,"VALIDATION_FAILED");return uuid(headers.getFirst());}
    private static long version(HttpServletRequest http){var headers=Collections.list(http.getHeaders("If-Match"));if(headers.isEmpty())throw problem(428,"PRECONDITION_REQUIRED");try{if(headers.size()!=1||!headers.getFirst().matches("\"v[1-9][0-9]*\""))throw new IllegalArgumentException();return Long.parseLong(headers.getFirst().substring(2,headers.getFirst().length()-1));}catch(IllegalArgumentException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static String body(HttpServletRequest http,String field,int limit)throws IOException{
        query(http);if(http.getContentType()==null||!http.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");byte[] bytes=http.getInputStream().readNBytes(2049);
        try{if(bytes.length>2048)throw problem(400,"VALIDATION_FAILED");var value=JSON.readTree(bytes);if(value==null||!value.isObject()||!value.propertyNames().equals(Set.of(field))||!value.get(field).isString()||value.get(field).asString().codePointCount(0,value.get(field).asString().length())>limit)throw problem(400,"VALIDATION_FAILED");return value.get(field).asString();}
        catch(CustomerProfileHttp.HttpProblem known){throw known;}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}
    }
}
