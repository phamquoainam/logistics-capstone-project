package com.elogistic.backend.workflow.internal.web;
import com.elogistic.backend.identity.api.staff.*;
import com.elogistic.backend.dispatch.api.OperationalStationIntake;
import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.merchant.api.customer.PilotCoverage;
import com.elogistic.backend.finance.api.PilotObligations;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

/** New OPS channel; the existing Dispatch/Orders/Finance transaction owns all effects. */
@RestController @Profile("customer")
public final class OperationalStationHttp {
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final OperationalStaffAdmission admission;private final StaffRequestContext requests;private final OperationalStationIntake intake;
    private static final String V2="application/vnd.argopost.station-cargo.v2+json";
    private final PilotHandling handling;private final PilotCoverage coverage;private final PilotObligations finance;private final com.elogistic.backend.finance.api.BusinessObligations businessFinance;
    public OperationalStationHttp(OperationalStaffAdmission admission,@Qualifier("operationsRequests")StaffRequestContext requests,OperationalStationIntake intake,PilotHandling handling,PilotCoverage coverage,PilotObligations finance,com.elogistic.backend.finance.api.BusinessObligations businessFinance){this.admission=admission;this.requests=requests;this.intake=intake;this.handling=handling;this.coverage=coverage;this.finance=finance;this.businessFinance=businessFinance;}
    private final class Scope implements PilotHandling.CarrierAccess,PilotObligations.HandoverAccess {
        final OperationalStaffAdmission.Context context;final UUID site;final String code;UUID org,facility;
        Scope(OperationalStaffAdmission.Context c,UUID site){admission.requireCurrent(c);this.context=c;this.site=site;intake.require(c,site);code=intake.sites(c).stream().filter(s->s.id().equals(site)).findFirst().orElseThrow(()->problem(403,"AUTHORITY_REQUIRED")).code();}
        public UUID actorId(){requireCurrent();return context.actor().accountId();}public UUID requestId(){requireCurrent();return context.requestId();}public String stationCode(){requireCurrent();return code;}
        public void requireCurrent(){admission.requireCurrent(context);if(org!=null)coverage.requireIntakeOrganization(context,org,facility);intake.require(context,site);}
    }
    private <T>T execute(String site,Function<Scope,T> work){UUID id=uuid(site);var r=requests.current();return admission.execute(r.encodedAccess(),r.requestId(),c->coverage.withIntakeOrganizationLock(c,()->work.apply(new Scope(c,id))));}
    private void policy(Scope s,PilotHandling.Cargo cargo){
        var q=cargo.shipment().quote().route();
        if(!coverage.catalog(s.context).routes().stream().anyMatch(r->r.datasetVersion().equals(q.datasetVersion())&&r.originCode().equals(q.originCode())&&r.destinationCode().equals(q.destinationCode())&&r.dropOffCode().equals(q.dropOffCode())&&r.classification().equals(q.classification())))throw new PilotHandlingFailure(PilotHandlingFailure.Code.INTAKE_POLICY_BLOCKED);
        var booked=cargo.shipment();finance.requireUnpaidHandover(s,booked.accountId(),booked.id(),booked.quote().id(),booked.totalVnd());
    }
    private void policy(Scope s,PilotHandling.CarrierCargo cargo){
        var q=cargo.route();
        if(!coverage.catalog(s.context).routes().stream().anyMatch(r->r.datasetVersion().equals(q.datasetVersion())&&r.originCode().equals(q.originCode())&&r.destinationCode().equals(q.destinationCode())&&r.dropOffCode().equals(q.dropOffCode())&&r.classification().equals(q.classification())))throw new PilotHandlingFailure(PilotHandlingFailure.Code.INTAKE_POLICY_BLOCKED);
        if("BUSINESS".equals(cargo.ownerType())){s.org=cargo.organizationId();s.facility=cargo.facilityId();s.requireCurrent();businessFinance.requireUnpaidHandover(s,cargo.organizationId(),cargo.facilityId(),cargo.shipmentId(),cargo.quoteId(),cargo.totalVnd());}
        else finance.requireUnpaidHandover(s,cargo.accountId(),cargo.shipmentId(),cargo.quoteId(),cargo.totalVnd());
    }
    private static boolean v2(HttpServletRequest r){return V2.equals(r.getHeader("Accept"));}
    private Map<String,Object> viewV2(Scope s,UUID shipment){var cargo=handling.carrierAtStation(s,shipment);policy(s,cargo);return Map.of("cargo",cargo,"scans",intake.scans(s.context,s.site,shipment),"paymentState","UNPAID","simulation",true,"policyVersion","BUSINESS".equals(cargo.ownerType())?"BR13-BUSINESS-INTAKE-01":"BR13-LOCAL-INTAKE-01");}
    private Map<String,Object> view(Scope s,UUID shipment){var cargo=handling.atStation(s,shipment);policy(s,cargo);return Map.of("cargo",cargo,"scans",intake.scans(s.context,s.site,shipment),"paymentState","UNPAID","simulation",true,"policyVersion","BR13-LOCAL-INTAKE-01");}
    @GetMapping("/api/v1/ops/sites") public Map<String,Object> sites(){var r=requests.current();return admission.execute(r.encodedAccess(),r.requestId(),c->Map.of("sites",intake.sites(c)));}
    @GetMapping("/api/v1/ops/sites/{siteId}/parcels/{barcode}") public Map<String,Object> parcel(@PathVariable("siteId") String site,@PathVariable String barcode,HttpServletRequest request){return execute(site,s->{if(v2(request)){var cargo=handling.carrierLookup(s,barcode);return viewV2(s,cargo.shipmentId());}var cargo=handling.lookup(s,barcode);return view(s,cargo.shipment().id());});}
    @GetMapping("/api/v1/ops/sites/{siteId}/shipments/{id}") public Map<String,Object> shipment(@PathVariable("siteId") String site,@PathVariable String id,HttpServletRequest request){return execute(site,s->v2(request)?viewV2(s,uuid(id)):view(s,uuid(id)));}
    @PostMapping("/api/v1/ops/sites/{siteId}/scans") public Object scan(@PathVariable("siteId") String site,HttpServletRequest request)throws IOException{var v=body(request,Set.of("barcode"));if(!v.get("barcode").isString())throw problem(400,"VALIDATION_FAILED");UUID key=key(request);return execute(site,s->{if(v2(request)){var cargo=handling.carrierLookup(s,v.get("barcode").asString());policy(s,cargo);return intake.scan(s.context,s.site,cargo,v.get("barcode").asString(),key);}var cargo=handling.lookup(s,v.get("barcode").asString());policy(s,cargo);return intake.scan(s.context,s.site,cargo,v.get("barcode").asString(),key);});}
    @PostMapping("/api/v1/ops/sites/{siteId}/shipments/{id}/acceptance") public ResponseEntity<Map<String,Object>> accept(@PathVariable("siteId") String site,@PathVariable String id,HttpServletRequest request)throws IOException{
        var v=body(request,Set.of("scanIds","inspection"));if(!v.get("scanIds").isArray()||v.get("scanIds").size()>5||!v.get("inspection").isString())throw problem(400,"VALIDATION_FAILED");List<UUID> scans=new ArrayList<>();for(var n:v.get("scanIds")){if(!n.isString())throw problem(400,"VALIDATION_FAILED");scans.add(uuid(n.asString()));}
        UUID key=key(request),shipment=uuid(id);long expected=version(request);
        try {return ResponseEntity.ok().eTag("\"i1\"").body(execute(site,s->{if(v2(request)){var cargo=handling.carrierAtStation(s,shipment);policy(s,cargo);var a=intake.accept(s.context,s.site,cargo,scans,expected,v.get("inspection").asString(),key);if(a.newlyCommitted())handling.carrierAccept(s,shipment,expected,a.id(),a.occurredAt());return viewV2(s,shipment);}var cargo=handling.atStation(s,shipment);policy(s,cargo);var a=intake.accept(s.context,s.site,cargo,scans,expected,v.get("inspection").asString(),key);if(a.newlyCommitted())handling.accept(s,shipment,expected,a.id(),a.occurredAt());return view(s,shipment);}));}
        catch(PilotHandlingFailure failure){if(failure.code()==PilotHandlingFailure.Code.PRECONDITION_STALE)throw problem(412,"PRECONDITION_STALE");throw failure;}
    }
    @GetMapping("/api/v1/ops/sites/{siteId}/receipts/{key}") public Object receipt(@PathVariable("siteId") String site,@PathVariable String key){return execute(site,s->{var receipt=intake.receipt(s.context,s.site,uuid(key));policy(s,handling.carrierAtStation(s,receipt.shipmentId()));return receipt;});}
    @GetMapping("/api/v1/ops/sites/{siteId}/shipments/{id}/labels.pdf") public ResponseEntity<byte[]> label(@PathVariable("siteId") String site,@PathVariable String id){return execute(site,s->{var cargo=handling.carrierAtStation(s,uuid(id));policy(s,cargo);return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header("Cache-Control","private, no-store").header("Content-Disposition","attachment; filename=\"argopost-simulation-labels.pdf\"").body(handling.carrierLabel(s,uuid(id)));});}
    private static UUID uuid(String s){try{var id=UUID.fromString(s);if(!id.toString().equals(s)||id.equals(new UUID(0,0)))throw new IllegalArgumentException();return id;}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static UUID key(HttpServletRequest r){var values=Collections.list(r.getHeaders("Idempotency-Key"));if(values.size()!=1)throw problem(400,"VALIDATION_FAILED");return uuid(values.getFirst());}
    private static long version(HttpServletRequest r){var values=Collections.list(r.getHeaders("If-Match"));if(values.isEmpty())throw problem(428,"PRECONDITION_REQUIRED");if(values.size()!=1||!values.getFirst().matches("\"i[01]\""))throw problem(400,"VALIDATION_FAILED");return values.getFirst().charAt(2)-'0';}
    private static JsonNode body(HttpServletRequest r,Set<String> fields)throws IOException{if(r.getContentType()==null||!r.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");byte[] bytes=r.getInputStream().readNBytes(4097);try{var v=JSON.readTree(bytes);if(bytes.length>4096||v==null||!v.isObject()||!v.propertyNames().equals(fields))throw problem(400,"VALIDATION_FAILED");return v;}catch(CustomerProfileHttp.HttpProblem p){throw p;}catch(Exception e){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}}
}
