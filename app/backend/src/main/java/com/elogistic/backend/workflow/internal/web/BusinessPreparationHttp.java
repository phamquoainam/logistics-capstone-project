package com.elogistic.backend.workflow.internal.web;
import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.merchant.api.customer.BusinessWorkspaces;
import com.elogistic.backend.orders.api.BusinessPreparations;
import com.elogistic.backend.orders.api.BusinessImports;
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
public final class BusinessPreparationHttp {
    private static final String BASE="/api/v1/merchant-organizations/{organizationId}", PATH=BASE+"/shipment-preparations";
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final CustomerAdmission admission; private final CustomerRequestContext requests; private final BusinessWorkspaces workspaces; private final BusinessPreparations drafts; private final BusinessImports imports;
    public BusinessPreparationHttp(CustomerAdmission admission,CustomerRequestContext requests,BusinessWorkspaces workspaces,BusinessPreparations drafts,BusinessImports imports){this.admission=admission;this.requests=requests;this.workspaces=workspaces;this.drafts=drafts;this.imports=imports;}
    private BusinessWorkspaces.Access merchant(CustomerAdmission.Context c){return new BusinessWorkspaces.Access(){
        public UUID accountId(){requireCurrent();return c.actor().accountId();}public void requireCurrent(){admission.requireCurrent(c);}
        public boolean eligibleAccount(UUID id){requireCurrent();return c.subjects().containsKey(id)&&c.subjects().get(id).eligible();}
    };}
    <T>T execute(String organizationId,Function<BusinessPreparations.Access,T> work){return executeWithContext(organizationId,(a,c)->work.apply(a));}
    <T>T executeWithContext(String organizationId,java.util.function.BiFunction<BusinessPreparations.Access,CustomerAdmission.Context,T> work){
        UUID org=uuid(organizationId);var request=requests.current();try{
            var subjects=admission.execute(request.encodedAccess(),request.requestId(),Set.of(),c->workspaces.references(merchant(c),org));
            return admission.execute(request.encodedAccess(),request.requestId(),subjects,c->workspaces.inOrganization(merchant(c),org,scope->work.apply(new BusinessPreparations.Access(){
                public UUID actorAccountId(){requireCurrent();return c.actor().accountId();}public UUID requestId(){requireCurrent();return c.requestId();}
                public BusinessWorkspaces.Scope scope(){requireCurrent();return scope;}public void requireCurrent(){admission.requireCurrent(c);scope.requireCurrent();}
                public void meaningfulAction(){admission.meaningfulAction(c);}
            },c)));
        }catch(BusinessPreparations.Failure|BusinessWorkspaces.Failure rejected){throw problem(switch(rejected.getMessage()){
            case "VALIDATION_FAILED"->400;case "RESOURCE_NOT_FOUND","BUSINESS_CONTEXT_UNAVAILABLE"->404;
            case "PRECONDITION_STALE","IDEMPOTENCY_CONFLICT","SOURCE_REFERENCE_CONFLICT","SOURCE_REFERENCE_IMMUTABLE","IMPORT_ROW_NOT_VALID"->409;default->503;
        },rejected.getMessage());}
    }
    private static void query(HttpServletRequest r,boolean list){if(!(list?Set.of("page","size"):Set.of()).containsAll(r.getParameterMap().keySet())||r.getParameterMap().values().stream().anyMatch(v->v.length!=1))throw problem(400,"VALIDATION_FAILED");}
    @GetMapping(PATH) public ResponseEntity<BusinessPreparations.Page> list(@PathVariable String organizationId,HttpServletRequest r){query(r,true);int page=number(r.getParameter("page"),0);if(number(r.getParameter("size"),20)!=20)throw problem(400,"VALIDATION_FAILED");return ResponseEntity.ok().header("Cache-Control","no-store").body(execute(organizationId,a->drafts.list(a,page)));}
    @GetMapping(PATH+"/{id}") public ResponseEntity<BusinessPreparations.Draft> get(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r){query(r,false);return response(execute(organizationId,a->drafts.get(a,uuid(id))));}
    @PostMapping(PATH) public ResponseEntity<BusinessPreparations.Draft> create(@PathVariable String organizationId,HttpServletRequest r)throws IOException{query(r,false);UUID key=key(r);var input=input(r);var result=execute(organizationId,a->drafts.create(a,input,key));return ResponseEntity.created(URI.create("/api/v1/merchant-organizations/"+result.organizationId()+"/shipment-preparations/"+result.id())).header("Cache-Control","no-store").eTag("\"v"+result.version()+"\"").body(result);}
    @PutMapping(PATH+"/{id}") public ResponseEntity<BusinessPreparations.Draft> update(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{query(r,false);UUID key=key(r),target=uuid(id);long expected=version(r);var input=input(r);return response(execute(organizationId,a->drafts.update(a,target,input,expected,key)));}
    @PostMapping(PATH+"/{id}/archive") public ResponseEntity<BusinessPreparations.Draft> archive(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{query(r,false);UUID key=key(r),target=uuid(id);long expected=version(r);if(r.getInputStream().read()!=-1)throw problem(400,"VALIDATION_FAILED");return response(execute(organizationId,a->drafts.archive(a,target,expected,key)));}
    @GetMapping(BASE+"/shipping-preparation-receipts/{operation}/{key}") public ResponseEntity<BusinessPreparations.Receipt> receipt(@PathVariable String organizationId,@PathVariable String operation,@PathVariable String key,HttpServletRequest r){query(r,false);return ResponseEntity.ok().header("Cache-Control","no-store").body(execute(organizationId,a->operation.startsWith("IMPORT_")?imports.receipt(a,operation,uuid(key)):drafts.receipt(a,operation,uuid(key))));}
    private static ResponseEntity<BusinessPreparations.Draft> response(BusinessPreparations.Draft result){return ResponseEntity.ok().header("Cache-Control","no-store").eTag("\"v"+result.version()+"\"").body(result);}
    private static BusinessPreparations.Input input(HttpServletRequest request)throws IOException{
        if(request.getContentType()==null||!request.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");byte[] bytes=request.getInputStream().readNBytes(65537);
        try{if(bytes.length>65536)throw problem(400,"VALIDATION_FAILED");var v=JSON.readTree(bytes);shape(v,Set.of("sender","recipient","parcels"),Set.of("facilityId","sourceChannel","sourceOrderReference","reference","instructions","sender","recipient","parcels"));for(String k:List.of("sourceChannel","sourceOrderReference","reference","instructions"))if(v.has(k)&&!v.get(k).isNull()&&!v.get(k).isString())throw problem(400,"VALIDATION_FAILED");
            for(String k:List.of("sender","recipient")){var c=v.get(k);shape(c,Set.of("name","phone","address"),Set.of("name","phone","address"));if(!c.get("name").isString()||!c.get("phone").isString())throw problem(400,"VALIDATION_FAILED");var a=c.get("address");shape(a,Set.of("country","administrativeArea","locality","addressLine"),Set.of("country","administrativeArea","locality","addressLine"));for(var field:a.properties())if(!field.getValue().isString())throw problem(400,"VALIDATION_FAILED");}
            var parcels=v.get("parcels");if(!parcels.isArray()||parcels.isEmpty()||parcels.size()>5)throw problem(400,"VALIDATION_FAILED");for(var p:parcels){var fields=Set.of("description","quantity","weightGrams","lengthMm","widthMm","heightMm","declaredValueVnd");shape(p,fields,fields);if(!p.get("description").isString())throw problem(400,"VALIDATION_FAILED");for(String k:fields)if(!k.equals("description")&&(!p.get(k).isIntegralNumber()||!p.get(k).canConvertToLong()))throw problem(400,"VALIDATION_FAILED");}
            if(v.has("facilityId")&&!v.get("facilityId").isNull())uuid(v.get("facilityId").asString());
            return JSON.treeToValue(v,BusinessPreparations.Input.class);
        }catch(CustomerProfileHttp.HttpProblem rejected){throw rejected;}catch(Exception invalid){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}
    }
    static void shape(JsonNode v,Set<String> required,Set<String> allowed){if(v==null||!v.isObject()||!v.propertyNames().containsAll(required)||!allowed.containsAll(v.propertyNames()))throw problem(400,"VALIDATION_FAILED");}
    static UUID uuid(String v){try{var id=UUID.fromString(v);if(!id.toString().equals(v)||id.equals(new UUID(0,0)))throw new IllegalArgumentException();return id;}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
    static UUID key(HttpServletRequest r){var h=Collections.list(r.getHeaders("Idempotency-Key"));if(h.size()!=1)throw problem(400,"VALIDATION_FAILED");return uuid(h.getFirst());}
    static long version(HttpServletRequest r){var h=Collections.list(r.getHeaders("If-Match"));if(h.isEmpty())throw problem(428,"PRECONDITION_REQUIRED");try{if(h.size()!=1||!h.getFirst().matches("\"v[1-9][0-9]*\""))throw new IllegalArgumentException();return Long.parseLong(h.getFirst().substring(2,h.getFirst().length()-1));}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
    static int number(String v,int fallback){if(v==null)return fallback;try{if(!v.matches("0|[1-9][0-9]*"))throw new IllegalArgumentException();return Integer.parseInt(v);}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
}
