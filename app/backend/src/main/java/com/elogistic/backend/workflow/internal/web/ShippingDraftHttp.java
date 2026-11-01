package com.elogistic.backend.workflow.internal.web;
import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.orders.api.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

@RestController @Profile("customer")
public final class ShippingDraftHttp {
    private static final String PATH="/api/v1/customer/shipment-drafts";
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final CustomerAdmission admission;private final CustomerRequestContext requests;private final ShippingDrafts drafts;
    public ShippingDraftHttp(CustomerAdmission admission,CustomerRequestContext requests,ShippingDrafts drafts){this.admission=admission;this.requests=requests;this.drafts=drafts;}
    private <T>T execute(Function<ShippingDrafts.Access,T> work){var request=requests.current();return admission.execute(request.encodedAccess(),request.requestId(),Set.of(),context->work.apply(new ShippingDrafts.Access(){public UUID accountId(){admission.requireCurrent(context);return context.actor().accountId();}public UUID requestId(){admission.requireCurrent(context);return context.requestId();}public void requireCurrent(){admission.requireCurrent(context);}public void meaningfulAction(){admission.meaningfulAction(context);}}));}
    @GetMapping(PATH) public Map<String,Object> list(HttpServletRequest request){var p=request.getParameterMap();if(!Set.of("page","size","search").containsAll(p.keySet())||p.values().stream().anyMatch(v->v.length!=1))throw problem(400,"VALIDATION_FAILED");int page=number(request.getParameter("page"),0);if(number(request.getParameter("size"),20)!=20)throw problem(400,"VALIDATION_FAILED");var result=execute(access->drafts.list(access,page,request.getParameter("search")));return Map.of("items",result.items().stream().map(ShippingDraftHttp::body).toList(),"page",result.page(),"size",20,"hasNext",result.hasNext());}
    @GetMapping(PATH+"/{id}") public ResponseEntity<Map<String,Object>> get(@PathVariable String id){return response(execute(a->drafts.get(a,uuid(id))),false);}
    @PostMapping(PATH) public ResponseEntity<Map<String,Object>> create(HttpServletRequest request)throws IOException{UUID key=key(request);var input=input(request);return response(execute(a->drafts.create(a,input,key)),true);}
    @PutMapping(PATH+"/{id}") public ResponseEntity<Map<String,Object>> update(@PathVariable String id,HttpServletRequest request)throws IOException{UUID target=uuid(id),key=key(request);long version=version(request);var input=input(request);return response(execute(a->drafts.update(a,target,input,version,key)),false);}
    @PostMapping(PATH+"/{id}/archive") public ResponseEntity<Void> archive(@PathVariable String id,HttpServletRequest request)throws IOException{UUID target=uuid(id),key=key(request);long version=version(request);if(request.getInputStream().read()!=-1)throw problem(400,"VALIDATION_FAILED");execute(a->{drafts.archive(a,target,version,key);return null;});return ResponseEntity.noContent().build();}
    @GetMapping("/api/v1/customer/shipment-draft-receipts/{operationId}/{key}") public ShippingDrafts.Receipt receipt(@PathVariable String operationId,@PathVariable String key){return execute(a->drafts.receipt(a,operationId,uuid(key)));}
    private static ShippingDrafts.Input input(HttpServletRequest request)throws IOException{
        if(request.getContentType()==null||!request.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");byte[] bytes=request.getInputStream().readNBytes(65537);
        try{if(bytes.length>65536)throw problem(400,"VALIDATION_FAILED");var v=JSON.readTree(bytes);shape(v,Set.of("sender","recipient","parcels"),Set.of("reference","instructions","sender","recipient","parcels"));for(String k:List.of("reference","instructions"))if(v.has(k)&&!v.get(k).isNull()&&!v.get(k).isString())throw problem(400,"VALIDATION_FAILED");
            for(String k:List.of("sender","recipient")){var c=v.get(k);shape(c,Set.of("name","phone","address"),Set.of("name","phone","address"));if(!c.get("name").isString()||!c.get("phone").isString())throw problem(400,"VALIDATION_FAILED");var a=c.get("address");shape(a,Set.of("country","administrativeArea","locality","addressLine"),Set.of("country","administrativeArea","locality","addressLine"));for(var field:a.properties())if(!field.getValue().isString())throw problem(400,"VALIDATION_FAILED");}
            var parcels=v.get("parcels");if(!parcels.isArray()||parcels.isEmpty()||parcels.size()>5)throw problem(400,"VALIDATION_FAILED");for(var p:parcels){var fields=Set.of("description","quantity","weightGrams","lengthMm","widthMm","heightMm","declaredValueVnd");shape(p,fields,fields);if(!p.get("description").isString())throw problem(400,"VALIDATION_FAILED");for(String k:fields)if(!k.equals("description")&&(!p.get(k).isIntegralNumber()||!p.get(k).canConvertToLong()))throw problem(400,"VALIDATION_FAILED");}
            return JSON.treeToValue(v,ShippingDrafts.Input.class);
        }catch(CustomerProfileHttp.HttpProblem rejected){throw rejected;}catch(Exception invalid){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}
    }
    private static void shape(JsonNode v,Set<String> required,Set<String> allowed){if(v==null||!v.isObject()||!v.propertyNames().containsAll(required)||!allowed.containsAll(v.propertyNames()))throw problem(400,"VALIDATION_FAILED");}
    private static UUID uuid(String v){try{var id=UUID.fromString(v);if(!id.toString().equals(v))throw new IllegalArgumentException();return id;}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static UUID key(HttpServletRequest r){var h=Collections.list(r.getHeaders("Idempotency-Key"));if(h.size()!=1)throw problem(400,"VALIDATION_FAILED");return uuid(h.getFirst());}
    private static long version(HttpServletRequest r){var h=Collections.list(r.getHeaders("If-Match"));if(h.isEmpty())throw problem(428,"PRECONDITION_REQUIRED");try{if(h.size()!=1||!h.getFirst().matches("\"v[1-9][0-9]*\""))throw new IllegalArgumentException();return Long.parseLong(h.getFirst().substring(2,h.getFirst().length()-1));}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static int number(String v,int fallback){if(v==null)return fallback;try{if(!v.matches("0|[1-9][0-9]*"))throw new IllegalArgumentException();return Integer.parseInt(v);}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static Map<String,Object> body(ShippingDrafts.Draft d){Map<String,Object> body=new LinkedHashMap<>();body.put("id",d.id());body.put("accountId",d.accountId());body.put("state",d.state());body.put("input",d.input());body.put("version",d.version());body.put("createdAt",d.createdAt());body.put("updatedAt",d.updatedAt());body.put("ownerType","PERSONAL");return body;}
    private static ResponseEntity<Map<String,Object>> response(ShippingDrafts.Draft d,boolean created){return (created?ResponseEntity.created(URI.create(PATH+"/"+d.id())):ResponseEntity.ok()).eTag("\"v"+d.version()+"\"").body(body(d));}
}
