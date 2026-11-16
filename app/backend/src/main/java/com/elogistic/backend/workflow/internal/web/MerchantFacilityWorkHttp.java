package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.orders.api.MerchantFacilityWork;
import com.elogistic.backend.orders.api.MerchantFacilityWork.*;
import com.elogistic.backend.orders.api.BusinessPreparations;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static com.elogistic.backend.workflow.internal.web.BusinessPreparationHttp.*;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

@RestController @Profile("customer")
public final class MerchantFacilityWorkHttp {
    private static final String BASE="/api/v1/merchant-organizations/{organizationId}";
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final BusinessPreparationHttp admission;private final MerchantFacilityWork work;
    public MerchantFacilityWorkHttp(BusinessPreparationHttp admission,MerchantFacilityWork work){this.admission=admission;this.work=work;}
    @ModelAttribute public void noStore(jakarta.servlet.http.HttpServletResponse r){r.setHeader("Cache-Control","no-store");}
    private static Access access(BusinessPreparations.Access a){return new Access(){public UUID actorAccountId(){return a.actorAccountId();}public UUID requestId(){return a.requestId();}public com.elogistic.backend.merchant.api.customer.BusinessWorkspaces.Scope scope(){return a.scope();}public void requireCurrent(){a.requireCurrent();}public void meaningfulAction(){a.meaningfulAction();}};}
    private <T>T execute(String org,Function<Access,T> f){try{return admission.execute(org,a->f.apply(access(a)));}catch(BusinessPreparations.Failure e){throw problem(switch(e.getMessage()){case "VALIDATION_FAILED"->400;case "RESOURCE_NOT_FOUND"->404;case "WORK_RESPONSIBILITY_REQUIRED"->403;case "PRECONDITION_STALE","IDEMPOTENCY_CONFLICT","WORK_ALREADY_STARTED","WORK_SOURCE_NOT_ELIGIBLE","WORK_PARCEL_NOT_ELIGIBLE","WORK_PARCELS_INCOMPLETE","WORK_STATE_CONFLICT","HANDOVER_REOPEN_REQUIRED","WORK_ALREADY_IN_HANDOVER"->409;case "WORK_BATCH_LIMIT_EXCEEDED","WORK_HISTORY_LIMIT_EXCEEDED"->422;default->503;},e.getMessage());}}
    private static Map<String,String> params(HttpServletRequest r,Set<String> allowed){var p=new HashMap<String,String>();r.getParameterMap().forEach((k,v)->{if(!allowed.contains(k)||v.length!=1)throw problem(400,"VALIDATION_FAILED");p.put(k,v[0]);});return p;}
    private static int page(Map<String,String> p){if(number(p.get("size"),20)!=20)throw problem(400,"VALIDATION_FAILED");return number(p.get("page"),0);}
    private static JsonNode body(HttpServletRequest r)throws IOException{if(r.getContentType()==null||!r.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");byte[] b=r.getInputStream().readNBytes(16385);try{if(b.length>16384)throw problem(400,"VALIDATION_FAILED");return JSON.readTree(b);}catch(RuntimeException bad){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(b,(byte)0);}}
    private static String text(JsonNode v,String k){var n=v.get(k);if(n==null||!n.isString())throw problem(400,"VALIDATION_FAILED");return n.asString();}
    private static String optional(JsonNode v,String k){return !v.has(k)||v.get(k).isNull()?null:text(v,k);}
    private static Check check(JsonNode v,boolean batch){String field=batch?"result":"condition";shape(v,Set.of("parcelId",field),Set.of("parcelId",field,"note"));return new Check(uuid(text(v,"parcelId")),text(v,field),optional(v,"note"));}
    private static List<UUID> ids(JsonNode v){var values=v.get("shipmentIds");if(values==null||!values.isArray()||values.isEmpty()||values.size()>20)throw problem(400,"VALIDATION_FAILED");var list=new ArrayList<UUID>();for(var n:values){if(!n.isString())throw problem(400,"VALIDATION_FAILED");list.add(uuid(n.asString()));}return list;}
    private static ResponseEntity<Preparation> response(Preparation v){return ResponseEntity.ok().eTag("\"v"+v.version()+"\"").body(v);}
    private static ResponseEntity<Batch> batchResponse(Batch v){return ResponseEntity.ok().eTag("\"v"+v.version()+"\"").body(v);}
    @GetMapping(BASE+"/facilities/{id}/preparation-work") public WorkPage list(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r){var p=params(r,Set.of("page","size","preparationState"));int page=page(p);return execute(organizationId,a->work.list(a,uuid(id),p.get("preparationState"),page));}
    @PostMapping(BASE+"/shipment-preparation-work") public ResponseEntity<Preparation> create(@PathVariable String organizationId,HttpServletRequest r)throws IOException{params(r,Set.of());UUID key=key(r);var v=body(r);shape(v,Set.of("shipmentId"),Set.of("shipmentId"));UUID source=uuid(text(v,"shipmentId"));return response(execute(organizationId,a->work.create(a,source,key)));}
    @GetMapping(BASE+"/shipment-preparation-work/{id}") public ResponseEntity<Preparation> get(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r){params(r,Set.of());return response(execute(organizationId,a->work.get(a,uuid(id))));}
    @PostMapping(BASE+"/shipment-preparation-work/{id}/checks") public ResponseEntity<Preparation> check(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{params(r,Set.of());UUID target=uuid(id),key=key(r);long version=version(r);var c=check(body(r),false);return response(execute(organizationId,a->work.check(a,target,c,version,key)));}
    @PostMapping(BASE+"/shipment-preparation-work/{id}/transitions") public ResponseEntity<Preparation> transition(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{params(r,Set.of());UUID target=uuid(id),key=key(r);long version=version(r);var v=body(r);shape(v,Set.of("action"),Set.of("action","reason"));String action=text(v,"action"),reason=optional(v,"reason");return response(execute(organizationId,a->work.transition(a,target,action,reason,version,key)));}
    @GetMapping(BASE+"/handover-preparations") public BatchPage batches(@PathVariable String organizationId,HttpServletRequest r){var p=params(r,Set.of("page","size","facilityId","state"));int page=page(p);UUID site=p.containsKey("facilityId")?uuid(p.get("facilityId")):null;return execute(organizationId,a->work.batches(a,site,p.get("state"),page));}
    @PostMapping(BASE+"/handover-preparations") public ResponseEntity<Batch> createBatch(@PathVariable String organizationId,HttpServletRequest r)throws IOException{params(r,Set.of());UUID key=key(r);var v=body(r);shape(v,Set.of("facilityId","shipmentIds"),Set.of("facilityId","shipmentIds"));UUID site=uuid(text(v,"facilityId"));var ids=ids(v);return batchResponse(execute(organizationId,a->work.createBatch(a,site,ids,key)));}
    @GetMapping(BASE+"/handover-preparations/{id}") public ResponseEntity<Batch> batch(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r){params(r,Set.of());return batchResponse(execute(organizationId,a->work.batch(a,uuid(id))));}
    @PutMapping(BASE+"/handover-preparations/{id}/items") public ResponseEntity<Batch> items(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{params(r,Set.of());UUID target=uuid(id),key=key(r);long version=version(r);var v=body(r);shape(v,Set.of("shipmentIds"),Set.of("shipmentIds"));var ids=ids(v);return batchResponse(execute(organizationId,a->work.items(a,target,ids,version,key)));}
    @PostMapping(BASE+"/handover-preparations/{id}/checks") public ResponseEntity<Batch> checkBatch(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{params(r,Set.of());UUID target=uuid(id),key=key(r);long version=version(r);var c=check(body(r),true);return batchResponse(execute(organizationId,a->work.checkBatch(a,target,c,version,key)));}
    @PostMapping(BASE+"/handover-preparations/{id}/transitions") public ResponseEntity<Batch> transitionBatch(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{params(r,Set.of());UUID target=uuid(id),key=key(r);long version=version(r);var v=body(r);shape(v,Set.of("action"),Set.of("action","reason"));String action=text(v,"action"),reason=optional(v,"reason");return batchResponse(execute(organizationId,a->work.transitionBatch(a,target,action,reason,version,key)));}
    @GetMapping(BASE+"/handover-preparations/{id}/document") public ResponseEntity<byte[]> document(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r){params(r,Set.of());UUID target=uuid(id);return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header("X-Content-Type-Options","nosniff").header("Content-Disposition","attachment; filename=argopost-handover-"+target+".pdf").body(execute(organizationId,a->work.document(a,target)));}
    @GetMapping(BASE+"/facility-work-receipts/{operation}/{key}") public Receipt receipt(@PathVariable String organizationId,@PathVariable String operation,@PathVariable String key,HttpServletRequest r){params(r,Set.of());return execute(organizationId,a->work.receipt(a,operation,uuid(key)));}
}
