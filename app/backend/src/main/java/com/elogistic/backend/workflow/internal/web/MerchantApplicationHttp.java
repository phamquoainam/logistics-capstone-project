package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.merchant.api.customer.MerchantApplications;
import com.elogistic.backend.merchant.api.customer.MerchantApplications.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

/** Exact reviewed applicant operations. No client actor, role, organization or status authority. */
@RestController @Profile("customer")
public final class MerchantApplicationHttp {
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final CustomerAdmission admission;private final CustomerRequestContext requests;private final MerchantApplications applications;
    public MerchantApplicationHttp(CustomerAdmission admission,CustomerRequestContext requests,MerchantApplications applications){this.admission=Objects.requireNonNull(admission);this.requests=Objects.requireNonNull(requests);this.applications=Objects.requireNonNull(applications);}
    @GetMapping("/api/v1/merchant-applications") public ResponseEntity<Map<String,Object>> list(HttpServletRequest http){
        for(String name:http.getParameterMap().keySet())if(!Set.of("page","size","search","state").contains(name) || http.getParameterValues(name).length!=1)throw problem(400,"VALIDATION_FAILED");
        var query=new Query(integer(http,"page",0),integer(http,"size",20),http.getParameter("search"),http.getParameter("state"));var request=requests.current();
        var page=admission.execute(request.encodedAccess(),request.requestId(),Set.of(),context->applications.list(context,query));
        return ResponseEntity.ok(Map.of("items",page.items().stream().map(MerchantApplicationHttp::wire).toList(),"page",page.page(),"size",page.size(),"hasNext",page.hasNext()));
    }
    @GetMapping("/api/v1/merchant-applications/{applicationId}") public ResponseEntity<Map<String,Object>> get(@PathVariable String applicationId){UUID id=uuid(applicationId);var request=requests.current();return response(admission.execute(request.encodedAccess(),request.requestId(),Set.of(),context->applications.get(context,id)),false);}
    @PostMapping("/api/v1/merchant-applications") public ResponseEntity<Map<String,Object>> create(HttpServletRequest http)throws IOException{
        UUID key=key(http);Input input=body(http);var request=requests.current();return response(admission.execute(request.encodedAccess(),request.requestId(),Set.of(),context->applications.create(context,input,key)),true);
    }
    @PutMapping("/api/v1/merchant-applications/{applicationId}") public ResponseEntity<Map<String,Object>> update(@PathVariable String applicationId,HttpServletRequest http)throws IOException{
        UUID id=uuid(applicationId),key=key(http);long version=version(http);Input input=body(http);var request=requests.current();
        return response(admission.execute(request.encodedAccess(),request.requestId(),Set.of(),context->applications.update(context,id,input,version,key)),false);
    }
    private static int integer(HttpServletRequest http,String name,int fallback){String value=http.getParameter(name);if(value==null)return fallback;try{if(!value.matches("0|[1-9][0-9]*"))throw new IllegalArgumentException();return Integer.parseInt(value);}catch(IllegalArgumentException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static UUID uuid(String value){try{UUID id=UUID.fromString(value);if(!id.toString().equals(value))throw new IllegalArgumentException();return id;}catch(IllegalArgumentException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static UUID key(HttpServletRequest http){var values=Collections.list(http.getHeaders("Idempotency-Key"));if(values.size()!=1)throw problem(400,"VALIDATION_FAILED");return uuid(values.getFirst());}
    private static long version(HttpServletRequest http){var values=Collections.list(http.getHeaders("If-Match"));if(values.isEmpty())throw problem(428,"PRECONDITION_REQUIRED");try{if(values.size()!=1 || !values.getFirst().matches("\"v[1-9][0-9]*\""))throw new IllegalArgumentException();return Long.parseLong(values.getFirst().substring(2,values.getFirst().length()-1));}catch(IllegalArgumentException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static void object(JsonNode node,Set<String> allowed,boolean complete){if(node==null || !node.isObject() || !allowed.containsAll(node.propertyNames()) || complete && !node.propertyNames().containsAll(allowed))throw problem(400,"VALIDATION_FAILED");}
    private static String text(JsonNode node,String key){var value=node.get(key);if(value==null)return null;if(!value.isString())throw problem(400,"VALIDATION_FAILED");return value.asString();}
    private static Address address(JsonNode root,String name){var a=root.get(name);if(a==null)return null;object(a,Set.of("country","administrativeArea","locality","addressLine"),true);return new Address(text(a,"country"),text(a,"administrativeArea"),text(a,"locality"),text(a,"addressLine"));}
    private static Input body(HttpServletRequest request)throws IOException{
        if(request.getContentType()==null || !request.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");byte[] bytes=request.getInputStream().readNBytes(32769);
        try{
            if(bytes.length>32768)throw problem(400,"VALIDATION_FAILED");var root=JSON.readTree(bytes);
            object(root,Set.of("entityType","jurisdiction","registeredName","legalIdentifier","registeredAddress","operatingAddress","representativeName","representativeEmail","representativeRelationship","businessContactAttestationId","evidenceIds","declarations"),false);
            var attestation=text(root,"businessContactAttestationId");List<UUID> evidence=null;var list=root.get("evidenceIds");
            if(list!=null){if(!list.isArray() || list.size()>5)throw problem(400,"VALIDATION_FAILED");evidence=new ArrayList<>();for(var id:list){if(!id.isString())throw problem(400,"VALIDATION_FAILED");evidence.add(uuid(id.asString()));}}
            Declaration declarations=null;var d=root.get("declarations");if(d!=null){object(d,Set.of("accuracy","representation","consentVersion"),true);if(!d.get("accuracy").isBoolean() || !d.get("representation").isBoolean())throw problem(400,"VALIDATION_FAILED");declarations=new Declaration(d.get("accuracy").asBoolean(),d.get("representation").asBoolean(),text(d,"consentVersion"));}
            return new Input(text(root,"entityType"),text(root,"jurisdiction"),text(root,"registeredName"),text(root,"legalIdentifier"),address(root,"registeredAddress"),address(root,"operatingAddress"),text(root,"representativeName"),text(root,"representativeEmail"),text(root,"representativeRelationship"),attestation==null?null:uuid(attestation),evidence,declarations);
        }catch(CustomerProfileHttp.HttpProblem rejected){throw rejected;}catch(Exception invalid){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}
    }
    static Map<String,Object> wire(Application application){Map<String,Object> body=new LinkedHashMap<>();body.put("id",application.id());body.put("applicantAccountId",application.applicantAccountId());body.put("state",application.state());body.put("revision",application.revision());body.put("version",application.version());body.put("draft",fields(application.draft()));return body;}
    private static Map<String,Object> fields(Input input){
        Map<String,Object> fields=new LinkedHashMap<>();var node=JSON.valueToTree(input);for(var field:node.properties())if(!field.getValue().isNull())fields.put(field.getKey(),JSON.convertValue(field.getValue(),Object.class));return fields;
    }
    private static ResponseEntity<Map<String,Object>> response(Application application,boolean created){var response=created?ResponseEntity.created(URI.create("/api/v1/merchant-applications/"+application.id())):ResponseEntity.ok();return response.eTag("\"v"+application.version()+"\"").body(wire(application));}
}
