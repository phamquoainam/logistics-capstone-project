package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.merchant.api.customer.MerchantApplications;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

/** Merchant verifies ownership before Identity proof issuance/confirmation in the same admission. */
@RestController @Profile("customer")
public final class CustomerBusinessContactHttp {
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final CustomerAdmission admission;private final CustomerRequestContext requests;
    private final MerchantApplications applications;private final CustomerBusinessContacts contacts;
    public CustomerBusinessContactHttp(CustomerAdmission admission,CustomerRequestContext requests,MerchantApplications applications,CustomerBusinessContacts contacts){
        this.admission=Objects.requireNonNull(admission);this.requests=Objects.requireNonNull(requests);this.applications=Objects.requireNonNull(applications);this.contacts=Objects.requireNonNull(contacts);
    }
    private void owned(CustomerAdmission.Context actor,UUID application){
        var current=applications.get(actor,application);
        if(!Set.of("DRAFT","NEEDS_INFORMATION").contains(current.state()))throw problem(409,"STATE_CONFLICT");
    }
    @PostMapping("/api/v1/customer/business-contact-proofs") public ResponseEntity<CustomerBusinessContacts.ProofAccepted> request(HttpServletRequest http)throws IOException{
        UUID key=key(http);var input=body(http,Set.of("applicationId","email"));UUID application=uuid(input.get("applicationId").asString());String email=text(input,"email",254);
        var request=requests.current();return ResponseEntity.accepted().header("Cache-Control","no-store").body(admission.execute(request.encodedAccess(),request.requestId(),Set.of(),actor->{owned(actor,application);return contacts.request(actor,application,email,key);}));
    }
    @PostMapping("/api/v1/customer/business-contact-confirmations") public ResponseEntity<CustomerBusinessContacts.BusinessContactAttestation> confirm(HttpServletRequest http)throws IOException{
        UUID key=key(http);var input=body(http,Set.of("applicationId","proofId","proof"));UUID application=uuid(input.get("applicationId").asString()),proofId=uuid(input.get("proofId").asString());String encoded=text(input,"proof",43);
        if(!encoded.matches("[A-Za-z0-9_-]{43}"))throw problem(400,"VALIDATION_FAILED");byte[] token=encoded.getBytes(StandardCharsets.US_ASCII);
        try{var request=requests.current();return ResponseEntity.ok().header("Cache-Control","no-store").body(admission.execute(request.encodedAccess(),request.requestId(),Set.of(),actor->{owned(actor,application);return contacts.confirm(actor,application,proofId,token,key);}));}
        finally{Arrays.fill(token,(byte)0);}
    }
    private static UUID uuid(String text){try{UUID id=UUID.fromString(text);if(!id.toString().equals(text)||id.equals(new UUID(0,0)))throw new IllegalArgumentException();return id;}catch(IllegalArgumentException|NullPointerException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static UUID key(HttpServletRequest http){var values=Collections.list(http.getHeaders("Idempotency-Key"));if(values.size()!=1)throw problem(400,"VALIDATION_FAILED");return uuid(values.getFirst());}
    private static String text(JsonNode input,String name,int max){var value=input.get(name);if(value==null||!value.isString()||value.asString().isEmpty()||value.asString().length()>max)throw problem(400,"VALIDATION_FAILED");return value.asString();}
    private static JsonNode body(HttpServletRequest http,Set<String> keys)throws IOException{
        if(!http.getParameterMap().isEmpty()||http.getContentType()==null||!http.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");
        byte[] bytes=http.getInputStream().readNBytes(4097);
        try{if(bytes.length>4096)throw problem(400,"VALIDATION_FAILED");var value=JSON.readTree(bytes);if(value==null||!value.isObject()||!value.propertyNames().equals(keys))throw problem(400,"VALIDATION_FAILED");for(String key:keys)text(value,key,254);return value;}
        catch(CustomerProfileHttp.HttpProblem failure){throw failure;}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}
    }
    @ExceptionHandler(IdentityCommandFailure.class) public ResponseEntity<Map<String,String>> identity(IdentityCommandFailure failure){
        int status=switch(failure.code()){case IDEMPOTENCY_CONFLICT->409;case VALIDATION_FAILED->400;case TOKEN_INVALID_OR_EXPIRED->422;default->401;};
        String code=failure.code()==IdentityCommandFailure.Code.TOKEN_INVALID_OR_EXPIRED?"CONTACT_PROOF_REQUIRED":status==401?"SESSION_INVALID":failure.code().name();
        return ResponseEntity.status(status).header("Cache-Control","no-store").body(Map.of("code",code,"message","Request rejected."));
    }
    @ExceptionHandler(CustomerBusinessContacts.Limited.class) public ResponseEntity<Map<String,String>> limited(CustomerBusinessContacts.Limited failure){return ResponseEntity.status(429).header("Cache-Control","no-store").header("Retry-After",Integer.toString(failure.retryAfter())).body(Map.of("code","RATE_LIMITED","message","Request rejected."));}
}
