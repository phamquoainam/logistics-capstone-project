package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.merchant.api.customer.BusinessWorkspaces;
import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.finance.api.BusinessObligations;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;
import static com.elogistic.backend.workflow.internal.web.BusinessPreparationHttp.*;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

/** Read/export/internal responsibility; no Merchant action becomes a Carrier event. */
@RestController @Profile("customer")
public final class OrganizationShipmentManagementHttp {
    private static final String BASE="/api/v1/merchant-organizations/{organizationId}";
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final BusinessPreparationHttp admission;private final BusinessShipmentManagement management;private final BusinessShipments shipments;private final BusinessObligations finance;
    public OrganizationShipmentManagementHttp(BusinessPreparationHttp admission,BusinessShipmentManagement management,BusinessShipments shipments,BusinessObligations finance){this.admission=admission;this.management=management;this.shipments=shipments;this.finance=finance;}
    @ModelAttribute public void noStore(jakarta.servlet.http.HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    private record Scope(BusinessPreparations.Access preparation,CustomerAdmission.Context identity) implements BusinessShipmentManagement.Access,BusinessObligations.Access {
        public UUID actorAccountId(){return preparation.actorAccountId();}public UUID requestId(){return preparation.requestId();}public UUID organizationId(){return scope().context().organizationId();}
        public BusinessWorkspaces.Scope scope(){return preparation.scope();}public void requireCurrent(){preparation.requireCurrent();}public void meaningfulAction(){preparation.meaningfulAction();}
        public boolean eligibleAccount(UUID account){requireCurrent();return identity.subjects().containsKey(account)&&identity.subjects().get(account).eligible();}
        public void requirePermission(String permission,UUID facility){requireCurrent();if(!scope().permits(permission,facility))throw new BusinessPreparations.Failure("RESOURCE_NOT_FOUND");}
    }
    private <T>T execute(String org,Set<UUID> references,Function<Scope,T> work){return admission.executeWithContext(org,references,(a,c)->work.apply(new Scope(a,c)));}
    private static Map<String,String> parameters(HttpServletRequest request){Map<String,String> result=new HashMap<>();request.getParameterMap().forEach((key,values)->{if(values.length!=1)throw problem(400,"VALIDATION_FAILED");result.put(key,values[0]);});return Map.copyOf(result);}
    private static int page(HttpServletRequest request){var p=parameters(request);if(!Set.of("page","size").containsAll(p.keySet())||!"20".equals(p.getOrDefault("size","20")))throw problem(400,"VALIDATION_FAILED");try{String raw=p.getOrDefault("page","0");if(!raw.matches("0|[1-9][0-9]{0,9}"))throw problem(400,"VALIDATION_FAILED");return Integer.parseInt(raw);}catch(NumberFormatException bad){throw problem(400,"VALIDATION_FAILED");}}
    private static void emptyQuery(HttpServletRequest request){if(!parameters(request).isEmpty())throw problem(400,"VALIDATION_FAILED");}
    private void validate(Scope scope,List<BusinessShipmentManagement.FeeBinding> rows){finance.validateCaptured(scope,rows.stream().map(r->new BusinessObligations.Captured(r.shipmentId(),r.quoteId(),r.facilityId(),r.amountVnd())).toList());}
    private Map<String,Object> record(Scope scope,BusinessShipments.Shipment shipment){var f=finance.get(scope,shipment.facilityId(),shipment.id());if(!f.quoteId().equals(shipment.quote().id())||f.amountVnd()!=shipment.totalVnd()||!"UNPAID".equals(f.state()))throw new IllegalStateException("BUSINESS_OBLIGATION_BINDING_REQUIRED");return Map.of("simulation",true,"shipment",shipment,"obligation",f);}
    @GetMapping(value=BASE+"/shipments",params="view=management") public Map<String,Object> list(@PathVariable String organizationId,HttpServletRequest request){
        var p=parameters(request);boolean v2="application/vnd.argopost.business-management.v2+json".equals(request.getHeader("Accept"));
        return execute(organizationId,Set.of(),a->{
            Map<String,Object> response=new LinkedHashMap<>();response.put("timeZone","Asia/Ho_Chi_Minh");response.put("size",20);
            if(v2){
                var result=management.readV2(a,p,rows->validate(a,rows));response.put("schemaVersion","ORGANIZATION_MANAGEMENT_2");
                response.put("generatedAt",result.generatedAt());response.put("summary",result.summary());response.put("total",result.total());
                response.put("page",result.page());response.put("hasNext",result.hasNext());
                response.put("items",result.items().stream().map(item->Map.of("record",record(a,item.shipment()),"status",item.handling().state(),"source",item.source(),"assignment",item.assignment(),"allowedActions",item.allowedActions(),"handling",item.handling())).toList());
            }else{
                var result=management.read(a,p,rows->validate(a,rows));
                if(!result.summary().bookedShipments().equals(result.summary().waitingForHandover()))throw new IllegalStateException("BUSINESS_LIFECYCLE_SOURCE_UNAVAILABLE");
                response.put("schemaVersion","ORGANIZATION_MANAGEMENT_1");response.put("generatedAt",result.generatedAt());response.put("summary",result.summary());response.put("total",result.total());
                response.put("page",result.page());response.put("hasNext",result.hasNext());
                response.put("items",result.items().stream().map(item->Map.of("record",record(a,item.shipment()),"status","WAITING_FOR_HANDOVER","source",item.source(),"assignment",item.assignment(),"allowedActions",item.allowedActions())).toList());
            }
            return response;
        });
    }
    @GetMapping(BASE+"/shipment-exports") public Map<String,Object> export(@PathVariable String organizationId,HttpServletRequest request){var p=parameters(request);return execute(organizationId,Set.of(),a->{var result=management.export(a,p,rows->validate(a,rows));Map<String,Object> response=new LinkedHashMap<>();response.put("schemaVersion","ORGANIZATION_EXPORT_1");response.put("exportId",result.exportId());response.put("organizationId",result.organizationId());response.put("generatedAt",result.generatedAt());response.put("timeZone","Asia/Ho_Chi_Minh");response.put("piiIncluded",result.piiIncluded());response.put("rowCount",result.rowCount());response.put("columns",result.columns());response.put("rows",result.rows());return response;});}
    @GetMapping(BASE+"/shipments/{id}/assignment") public ResponseEntity<BusinessShipmentManagement.Assignment> assignment(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest request){emptyQuery(request);return assignmentResponse(execute(organizationId,Set.of(),a->management.assignment(a,uuid(id))));}
    private static ResponseEntity<BusinessShipmentManagement.Assignment> assignmentResponse(BusinessShipmentManagement.Assignment result){return ResponseEntity.ok().header("Cache-Control","no-store").eTag("\"a"+result.version()+"\"").body(result);}
    @PutMapping(BASE+"/shipments/{id}/assignment") public ResponseEntity<BusinessShipmentManagement.Assignment> assign(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest request)throws IOException{
        emptyQuery(request);UUID shipment=uuid(id),key=key(request);long expected;
        var matches=Collections.list(request.getHeaders("If-Match"));try{if(matches.size()!=1||!matches.getFirst().matches("\"a(?:0|[1-9][0-9]{0,18})\""))throw problem(428,"PRECONDITION_REQUIRED");expected=Long.parseLong(matches.getFirst().substring(2,matches.getFirst().length()-1));}catch(NumberFormatException bad){throw problem(400,"VALIDATION_FAILED");}
        if(request.getContentType()==null||!request.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");
        byte[] bytes=request.getInputStream().readNBytes(4097);UUID assignee;String reason;
        try{if(bytes.length>4096)throw problem(400,"VALIDATION_FAILED");var body=JSON.readTree(bytes);shape(body,Set.of("assigneeAccountId","reasonCode"),Set.of("assigneeAccountId","reasonCode"));if(!body.get("assigneeAccountId").isNull()&&!body.get("assigneeAccountId").isString()||!body.get("reasonCode").isString())throw problem(400,"VALIDATION_FAILED");assignee=body.get("assigneeAccountId").isNull()?null:uuid(body.get("assigneeAccountId").asString());reason=body.get("reasonCode").asString();}
        catch(CustomerProfileHttp.HttpProblem rejected){throw rejected;}catch(Exception invalid){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}
        return assignmentResponse(execute(organizationId,assignee==null?Set.of():Set.of(assignee),a->management.assign(a,shipment,assignee,reason,expected,key)));
    }
    @GetMapping(BASE+"/shipments/{id}/assignment-candidates") public Map<String,Object> candidates(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest request){
        int page=page(request);UUID shipment=uuid(id);var discovered=execute(organizationId,Set.of(),a->{var s=shipments.get(a,shipment);return a.scope().assignmentMembers(s.facilityId(),page);});
        Set<UUID> references=new HashSet<>();discovered.forEach(m->references.add(m.accountId()));
        return execute(organizationId,references,a->{var s=shipments.get(a,shipment);var rows=a.scope().assignmentMembers(s.facilityId(),page);var items=rows.stream().limit(20).filter(m->a.eligibleAccount(m.accountId())).map(m->Map.of("accountId",m.accountId(),"membershipId",m.membershipId(),"role",m.role(),"displayName",a.identity().subjects().get(m.accountId()).displayName())).toList();return Map.of("items",items,"page",page,"size",20,"hasNext",rows.size()>20);});
    }
    @GetMapping(BASE+"/shipments/{id}/internal-history") public Map<String,Object> history(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest request){int page=page(request);return execute(organizationId,Set.of(),a->{var rows=management.history(a,uuid(id),page);return Map.of("items",rows.items(),"page",page,"size",20,"hasNext",rows.hasNext());});}
    @GetMapping(BASE+"/shipment-assignment-receipts/{key}") public BusinessShipmentManagement.Receipt receipt(@PathVariable String organizationId,@PathVariable String key,HttpServletRequest request){emptyQuery(request);return execute(organizationId,Set.of(),a->management.receipt(a,uuid(key)));}
}
