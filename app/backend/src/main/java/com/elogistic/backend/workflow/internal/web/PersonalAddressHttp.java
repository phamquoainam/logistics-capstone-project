package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.merchant.api.customer.PersonalAddresses;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

/** Uses the existing session-owned transaction; never accepts an owner or permission from the browser. */
@RestController @Profile("customer")
public final class PersonalAddressHttp {
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final CustomerAdmission admission;private final CustomerRequestContext requests;private final PersonalAddresses addresses;
    public PersonalAddressHttp(CustomerAdmission admission,CustomerRequestContext requests,PersonalAddresses addresses){this.admission=Objects.requireNonNull(admission);this.requests=Objects.requireNonNull(requests);this.addresses=Objects.requireNonNull(addresses);}
    private <T>T execute(Function<CustomerAdmission.Context,T> work){var request=requests.current();return admission.execute(request.encodedAccess(),request.requestId(),Set.of(),work);}
    @GetMapping("/api/v1/customer/addresses") public Map<String,Object> list(HttpServletRequest request){
        var parameters=request.getParameterMap();if(!Set.of("page","size","search","kind").containsAll(parameters.keySet()) || parameters.values().stream().anyMatch(values->values.length!=1))throw problem(400,"VALIDATION_FAILED");
        int page=number(request.getParameter("page"),0),size=number(request.getParameter("size"),20);var query=new PersonalAddresses.Query(page,size,request.getParameter("search"),request.getParameter("kind"));var result=execute(context->addresses.list(context,query));
        return Map.of("items",result.items().stream().map(PersonalAddressHttp::body).toList(),"page",result.page(),"size",result.size(),"hasNext",result.hasNext());
    }
    @GetMapping("/api/v1/customer/addresses/{id}") public ResponseEntity<Map<String,Object>> get(@PathVariable String id){return response(execute(context->addresses.get(context,uuid(id))));}
    @PostMapping("/api/v1/customer/addresses") public ResponseEntity<Map<String,Object>> create(HttpServletRequest request)throws IOException{
        UUID key=key(request);var input=input(request);var result=execute(context->addresses.create(context,input,key));return ResponseEntity.created(URI.create("/api/v1/customer/addresses/"+result.id())).eTag(etag(result)).body(body(result));
    }
    @PutMapping("/api/v1/customer/addresses/{id}") public ResponseEntity<Map<String,Object>> update(@PathVariable String id,HttpServletRequest request)throws IOException{
        UUID target=uuid(id),key=key(request);long version=version(request);var input=input(request);return response(execute(context->addresses.update(context,target,input,version,key)));
    }
    @PostMapping("/api/v1/customer/addresses/{id}/archive") public ResponseEntity<Void> archive(@PathVariable String id,HttpServletRequest request)throws IOException{
        UUID target=uuid(id),key=key(request);long version=version(request);if(request.getInputStream().read()!=-1)throw problem(400,"VALIDATION_FAILED");execute(context->{addresses.archive(context,target,version,key);return null;});return ResponseEntity.noContent().build();
    }
    @GetMapping("/api/v1/customer/address-receipts/{operationId}/{key}") public Map<String,Object> receipt(@PathVariable String operationId,@PathVariable String key){
        var result=execute(context->addresses.receipt(context,operationId,uuid(key)));return Map.of("operationId",result.operationId(),"targetId",result.targetId(),"outcome",result.outcome(),"version",result.version());
    }
    private static PersonalAddresses.Input input(HttpServletRequest request)throws IOException{
        if(request.getContentType()==null || !request.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");byte[] bytes=request.getInputStream().readNBytes(16385);
        try{
            if(bytes.length>16384)throw problem(400,"VALIDATION_FAILED");var value=JSON.readTree(bytes);Set<String> required=Set.of("kind","contactName","contactPhone","address","isDefault"),allowed=Set.of("kind","label","contactName","contactPhone","address","isDefault");
            if(value==null || !value.isObject() || !allowed.containsAll(value.propertyNames()) || !value.propertyNames().containsAll(required))throw problem(400,"VALIDATION_FAILED");
            for(String name:List.of("kind","contactName","contactPhone"))if(!value.get(name).isString())throw problem(400,"VALIDATION_FAILED");
            if(value.has("label")&&!value.get("label").isString() || !value.get("isDefault").isBoolean())throw problem(400,"VALIDATION_FAILED");var location=value.get("address");
            if(!location.isObject() || !location.propertyNames().equals(Set.of("country","administrativeArea","locality","addressLine")))throw problem(400,"VALIDATION_FAILED");for(var field:location.properties())if(!field.getValue().isString())throw problem(400,"VALIDATION_FAILED");
            return new PersonalAddresses.Input(value.get("kind").asString(),value.has("label")?value.get("label").asString():null,value.get("contactName").asString(),value.get("contactPhone").asString(),new PersonalAddresses.Location(location.get("country").asString(),location.get("administrativeArea").asString(),location.get("locality").asString(),location.get("addressLine").asString()),value.get("isDefault").asBoolean());
        }catch(CustomerProfileHttp.HttpProblem failure){throw failure;}catch(Exception invalid){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}
    }
    private static UUID uuid(String value){try{UUID id=UUID.fromString(value);if(!id.toString().equals(value))throw problem(400,"VALIDATION_FAILED");return id;}catch(IllegalArgumentException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static UUID key(HttpServletRequest request){var headers=Collections.list(request.getHeaders("Idempotency-Key"));if(headers.size()!=1)throw problem(400,"VALIDATION_FAILED");return uuid(headers.getFirst());}
    private static long version(HttpServletRequest request){var headers=Collections.list(request.getHeaders("If-Match"));if(headers.isEmpty())throw problem(428,"PRECONDITION_REQUIRED");try{if(headers.size()!=1 || !headers.getFirst().matches("\"v[1-9][0-9]*\""))throw problem(400,"VALIDATION_FAILED");return Long.parseLong(headers.getFirst().substring(2,headers.getFirst().length()-1));}catch(NumberFormatException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static int number(String value,int fallback){if(value==null)return fallback;try{if(!value.matches("0|[1-9][0-9]*"))throw problem(400,"VALIDATION_FAILED");return Integer.parseInt(value);}catch(NumberFormatException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static String etag(PersonalAddresses.Address a){return "\"v"+a.version()+"\"";}
    private static ResponseEntity<Map<String,Object>> response(PersonalAddresses.Address a){return ResponseEntity.ok().eTag(etag(a)).body(body(a));}
    private static Map<String,Object> body(PersonalAddresses.Address a){
        var i=a.input();Map<String,Object> result=new LinkedHashMap<>();result.put("id",a.id());result.put("accountId",a.accountId());result.put("kind",i.kind());if(i.label()!=null)result.put("label",i.label());result.put("contactName",i.contactName());result.put("contactPhone",i.contactPhone());result.put("address",i.address());result.put("isDefault",i.isDefault());result.put("verificationStatus","UNVERIFIED");result.put("version",a.version());result.put("createdAt",a.createdAt());result.put("updatedAt",a.updatedAt());return result;
    }
}
