package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.orders.api.OrganizationClaimIntakes;
import com.elogistic.backend.orders.api.OrganizationClaimIntakes.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

@RestController @Profile("customer")
public final class OrganizationClaimIntakeHttp {
    private static final String BASE="/api/v1/merchant-organizations/{organizationId}",CLAIMS=BASE+"/claim-intakes",STAFF="/api/v1/support-operations/organization-claim-intakes/{id}";
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final OrganizationSupportHttp support;
    private final OrganizationClaimIntakes claims;
    public OrganizationClaimIntakeHttp(OrganizationSupportHttp support,OrganizationClaimIntakes claims){this.support=support;this.claims=claims;}
    @ModelAttribute public void privacy(HttpServletResponse r){r.setHeader("Cache-Control","no-store");r.setHeader("X-Content-Type-Options","nosniff");}
    private <T>T errors(java.util.function.Supplier<T> work){try{return work.get();}catch(Failure f){throw problem(switch(f.getMessage()){case "VALIDATION_FAILED","CLAIM_SHIPMENT_TICKET_REQUIRED"->400;case "RESOURCE_NOT_FOUND"->404;case "IDEMPOTENCY_CONFLICT","PRECONDITION_STALE","CLAIM_ALREADY_OPEN","CLAIM_STATE_CONFLICT"->409;default->503;},f.getMessage());}}
    private static UUID id(String raw){try{UUID id=UUID.fromString(raw);if(!id.toString().equals(raw)||id.equals(new UUID(0,0)))throw new IllegalArgumentException();return id;}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static void query(HttpServletRequest r,Set<String> allowed){r.getParameterMap().forEach((k,v)->{if(!allowed.contains(k)||v.length!=1)throw problem(400,"VALIDATION_FAILED");});}
    private static String one(HttpServletRequest r,String header){var values=Collections.list(r.getHeaders(header));if(values.size()!=1)throw problem(header.equals("If-Match")?428:400,"VALIDATION_FAILED");return values.getFirst();}
    private static UUID key(HttpServletRequest r){return id(one(r,"Idempotency-Key"));}
    private static long version(HttpServletRequest r){String v=one(r,"If-Match");try{if(!v.matches("\"v[1-9][0-9]*\""))throw new IllegalArgumentException();return Long.parseLong(v.substring(2,v.length()-1));}catch(RuntimeException bad){throw problem(400,"VALIDATION_FAILED");}}
    private static JsonNode body(HttpServletRequest r,Set<String> fields)throws IOException{
        query(r,Set.of());if(r.getContentType()==null||!r.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");
        byte[] bytes=r.getInputStream().readNBytes(32769);try{if(bytes.length>32768)throw new IllegalArgumentException();var v=JSON.readTree(bytes);if(v==null||!v.isObject()||!v.propertyNames().equals(fields))throw new IllegalArgumentException();for(String f:fields)if(!v.get(f).isString())throw new IllegalArgumentException();return v;}catch(Exception invalid){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}
    }
    private static ResponseEntity<Detail> detail(Detail d){return ResponseEntity.ok().eTag("\"v"+d.intake().version()+"\"").body(d);}
    @GetMapping(BASE+"/claim-intake-reports") public Report report(@PathVariable String organizationId,HttpServletRequest r){query(r,Set.of());return errors(()->support.execute(organizationId,claims::report));}
    @GetMapping(BASE+"/claim-intake-notices") public Notices notices(@PathVariable String organizationId,HttpServletRequest r){query(r,Set.of("page"));int page;try{page=r.getParameter("page")==null?0:Integer.parseInt(r.getParameter("page"));}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}return errors(()->support.execute(organizationId,a->claims.notices(a,page)));}
    @GetMapping(CLAIMS) public Page list(@PathVariable String organizationId,HttpServletRequest r){query(r,Set.of("page","ticketId","state"));int page;try{page=r.getParameter("page")==null?0:Integer.parseInt(r.getParameter("page"));}catch(RuntimeException bad){throw problem(400,"VALIDATION_FAILED");}UUID ticket=r.getParameter("ticketId")==null?null:id(r.getParameter("ticketId"));return errors(()->support.execute(organizationId,a->claims.list(a,page,ticket,r.getParameter("state"))));}
    @PostMapping(CLAIMS) public ResponseEntity<Detail> create(@PathVariable String organizationId,HttpServletRequest r)throws IOException{var v=body(r,Set.of("ticketId","category","narrative"));var input=new Create(id(v.get("ticketId").asString()),v.get("category").asString(),v.get("narrative").asString());UUID key=key(r);var d=errors(()->support.execute(organizationId,a->claims.create(a,input,key)));return ResponseEntity.created(URI.create("/api/v1/merchant-organizations/"+organizationId+"/claim-intakes/"+d.intake().id())).eTag("\"v"+d.intake().version()+"\"").body(d);}
    @GetMapping(CLAIMS+"/{id}") public ResponseEntity<Detail> get(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r){query(r,Set.of());return detail(errors(()->support.execute(organizationId,a->claims.get(a,id(id)))));}
    @PostMapping(CLAIMS+"/{id}/supplements") public ResponseEntity<Detail> supplement(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{var v=body(r,Set.of("narrative"));UUID target=id(id),key=key(r);long version=version(r);var refs=errors(()->support.execute(organizationId,a->claims.references(a,target)));return detail(errors(()->support.execute(organizationId,refs,a->claims.supplement(a,target,v.get("narrative").asString(),version,key))));}
    @GetMapping(BASE+"/claim-intake-receipts/{operation}/{key}") public Receipt receipt(@PathVariable String organizationId,@PathVariable String operation,@PathVariable String key,HttpServletRequest r){query(r,Set.of());return errors(()->support.execute(organizationId,a->claims.receipt(a,operation,id(key))));}
    @GetMapping(STAFF) public ResponseEntity<Detail> staffGet(@PathVariable String id,HttpServletRequest r){query(r,Set.of());return detail(errors(()->support.staff(Set.of(),a->claims.assigned(a,id(id)))));}
    @GetMapping("/api/v1/support-operations/organization-cases/{id}/claim-intakes") public Page staffList(@PathVariable String id,HttpServletRequest r){query(r,Set.of("page"));int page;try{page=r.getParameter("page")==null?0:Integer.parseInt(r.getParameter("page"));}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}return errors(()->support.staff(Set.of(),a->claims.assignedIntakes(a,id(id),page)));}
    @PostMapping(STAFF+"/commands") public ResponseEntity<Detail> staffCommand(@PathVariable String id,HttpServletRequest r)throws IOException{var v=body(r,Set.of("action","message"));UUID target=id(id),key=key(r);long version=version(r);var refs=errors(()->support.staff(Set.of(),a->claims.staffReferences(a,target)));return detail(errors(()->support.staff(refs,a->claims.staffCommand(a,target,v.get("action").asString(),v.get("message").asString(),version,key))));}
}
