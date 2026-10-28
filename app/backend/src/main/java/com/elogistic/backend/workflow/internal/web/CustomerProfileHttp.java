package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.merchant.api.customer.CustomerProfiles;
import com.elogistic.backend.merchant.api.customer.MerchantApplications;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;

/** HTTP orchestrates only public owner APIs; Identity owns the local commit and final authority check. */
@RestController @Profile("customer")
public final class CustomerProfileHttp {
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final CustomerAdmission admission;private final CustomerRequestContext requests;private final CustomerProfiles profiles;private final MerchantApplications applications;
    public CustomerProfileHttp(CustomerAdmission admission,CustomerRequestContext requests,CustomerProfiles profiles,MerchantApplications applications){this.admission=Objects.requireNonNull(admission);this.requests=Objects.requireNonNull(requests);this.profiles=Objects.requireNonNull(profiles);this.applications=Objects.requireNonNull(applications);}
    @GetMapping("/api/v1/customer/profile") public ResponseEntity<Map<String,Object>> get(){
        var request=requests.current();return response(admission.execute(request.encodedAccess(),request.requestId(),Set.of(),profiles::get));
    }
    @GetMapping("/api/v1/onboarding-receipts/{operationId}/{idempotencyKey}") public ResponseEntity<Map<String,Object>> receipt(@PathVariable String operationId,@PathVariable String idempotencyKey){
        if(!Set.of("updateCustomerProfile","createApplicationDraft","updateApplicationDraft").contains(operationId))throw problem(404,"RESOURCE_NOT_FOUND");
        UUID key;try{key=UUID.fromString(idempotencyKey);if(!key.toString().equals(idempotencyKey))throw problem(400,"VALIDATION_FAILED");}catch(IllegalArgumentException invalid){throw problem(400,"VALIDATION_FAILED");}
        var request=requests.current();
        if(!operationId.equals("updateCustomerProfile")){
            var receipt=admission.execute(request.encodedAccess(),request.requestId(),Set.of(),context->applications.receipt(context,operationId,key));
            return ResponseEntity.ok(Map.of("operationId",receipt.operationId(),"targetId",receipt.targetId(),"outcome",receipt.outcome(),"version",receipt.version()));
        }
        var receipt=admission.execute(request.encodedAccess(),request.requestId(),Set.of(),context->profiles.receipt(context,key));
        return ResponseEntity.ok(Map.of("operationId",receipt.operationId(),"targetId",receipt.targetId(),"outcome",receipt.outcome(),"version",receipt.version()));
    }
    @PutMapping("/api/v1/customer/profile") public ResponseEntity<Map<String,Object>> update(HttpServletRequest http)throws IOException{
        UUID key=key(http);long version=version(http);var input=body(http);var request=requests.current();
        return response(admission.execute(request.encodedAccess(),request.requestId(),Set.of(),context->profiles.update(context,new CustomerProfiles.Update(input,version,key))));
    }
    private static CustomerProfiles.Input body(HttpServletRequest request)throws IOException{
        if(request.getContentType()==null || !request.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");
        byte[] bytes=request.getInputStream().readNBytes(16385);
        try{
            if(bytes.length>16384)throw problem(400,"VALIDATION_FAILED");var value=JSON.readTree(bytes);var allowed=Set.of("displayName","preferredLanguage","businessContactEmail","businessContactPhone");
            if(value==null || !value.isObject() || !value.has("displayName") || !value.has("preferredLanguage") || !allowed.containsAll(value.propertyNames()))throw problem(400,"VALIDATION_FAILED");
            for(var field:value.properties())if(!field.getValue().isString())throw problem(400,"VALIDATION_FAILED");
            return new CustomerProfiles.Input(value.get("displayName").asString(),value.get("preferredLanguage").asString(),value.has("businessContactEmail")?value.get("businessContactEmail").asString():null,value.has("businessContactPhone")?value.get("businessContactPhone").asString():null);
        }catch(HttpProblem rejected){throw rejected;}catch(Exception invalid){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}
    }
    private static UUID key(HttpServletRequest request){
        var values=Collections.list(request.getHeaders("Idempotency-Key"));try{if(values.size()!=1)throw problem(400,"VALIDATION_FAILED");UUID key=UUID.fromString(values.getFirst());if(!key.toString().equals(values.getFirst()))throw problem(400,"VALIDATION_FAILED");return key;}
        catch(IllegalArgumentException invalid){throw problem(400,"VALIDATION_FAILED");}
    }
    private static long version(HttpServletRequest request){
        var values=Collections.list(request.getHeaders("If-Match"));if(values.isEmpty())throw problem(428,"PRECONDITION_REQUIRED");
        try{if(values.size()!=1 || !values.getFirst().matches("\"v[1-9][0-9]*\""))throw problem(400,"VALIDATION_FAILED");long version=Long.parseLong(values.getFirst().substring(2,values.getFirst().length()-1));if(version<1)throw problem(400,"VALIDATION_FAILED");return version;}
        catch(NumberFormatException invalid){throw problem(400,"VALIDATION_FAILED");}
    }
    private static ResponseEntity<Map<String,Object>> response(CustomerProfiles.Profile profile){
        Map<String,Object> body=new LinkedHashMap<>();body.put("id",profile.id());body.put("accountId",profile.accountId());body.put("displayName",profile.displayName());body.put("preferredLanguage",profile.preferredLanguage());body.put("version",profile.version());
        if(profile.businessContactEmail()!=null)body.put("businessContactEmail",profile.businessContactEmail());if(profile.businessContactPhone()!=null)body.put("businessContactPhone",profile.businessContactPhone());
        return ResponseEntity.ok().eTag("\"v"+profile.version()+"\"").body(body);
    }
    static HttpProblem problem(int status,String code){return new HttpProblem(status,code);}
    static final class HttpProblem extends RuntimeException {final int status;final String code;HttpProblem(int status,String code){super(code);this.status=status;this.code=code;}}
}
