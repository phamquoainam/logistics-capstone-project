package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.dispatch.api.StationIntake;
import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.merchant.api.customer.PilotCoverage;
import com.elogistic.backend.finance.api.PilotObligations;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

/** Trusted current Identity and Dispatch context; one transaction for all scan/intake/source effects. */
@RestController @Profile("customer")
public final class StationIntakeHttp {
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final CustomerAdmission admission;private final CustomerRequestContext requests;private final StationIntake intake;private final PilotHandling handling;private final PilotCoverage coverage;private final PilotObligations finance;
    public StationIntakeHttp(CustomerAdmission admission,CustomerRequestContext requests,StationIntake intake,PilotHandling handling,PilotCoverage coverage,PilotObligations finance){this.admission=admission;this.requests=requests;this.intake=intake;this.handling=handling;this.coverage=coverage;this.finance=finance;}
    private final class Scope implements StationIntake.Access,ShippingDrafts.Access {
        final CustomerAdmission.Context context;Scope(CustomerAdmission.Context c){context=c;}
        public UUID actorId(){requireCurrent();return context.actor().accountId();}public UUID accountId(){return actorId();}public UUID requestId(){requireCurrent();return context.requestId();}public void requireCurrent(){admission.requireCurrent(context);}public void meaningfulAction(){admission.meaningfulAction(context);}
    }
    private final class Carrier implements PilotHandling.CarrierAccess,PilotObligations.HandoverAccess {
        final Scope scope;final String station;Carrier(Scope scope,String station){this.scope=scope;this.station=station;requireCurrent();}
        public UUID actorId(){scope.requireCurrent();return scope.actorId();}public UUID requestId(){return scope.requestId();}public String stationCode(){return station;}public void requireCurrent(){scope.requireCurrent();intake.require(scope,station);}
    }
    private <T>T execute(Function<Scope,T> work){var r=requests.current();return admission.execute(r.encodedAccess(),r.requestId(),Set.of(),c->work.apply(new Scope(c)));}
    private void route(Scope s,PilotHandling.Cargo cargo){var booked=cargo.shipment().quote().route();boolean eligible=coverage.catalog(s.context).routes().stream().anyMatch(r->r.datasetVersion().equals(booked.datasetVersion())&&r.originCode().equals(booked.originCode())&&r.destinationCode().equals(booked.destinationCode())&&r.dropOffCode().equals(booked.dropOffCode())&&r.classification().equals(booked.classification()));if(!eligible)throw new PilotHandlingFailure(PilotHandlingFailure.Code.INTAKE_POLICY_BLOCKED);}
    private void payment(Carrier c,PilotHandling.Cargo cargo){var s=cargo.shipment();finance.requireUnpaidHandover(c,s.accountId(),s.id(),s.quote().id(),s.totalVnd());}
    private Map<String,Object> view(Scope s,Carrier c,UUID shipment){var cargo=handling.atStation(c,shipment);route(s,cargo);payment(c,cargo);return Map.of("cargo",cargo,"scans",intake.scans(s,c.station,shipment),"paymentState","UNPAID","simulation",true,"policyVersion","BR13-LOCAL-INTAKE-01");}
    @GetMapping("/api/v1/customer/shipments/{id}/handling") public PilotHandling.Handling personal(@PathVariable String id){return execute(s->handling.self(s,uuid(id)));}
    @GetMapping("/api/v1/customer/shipments/{id}/labels.pdf") public ResponseEntity<byte[]> label(@PathVariable String id){UUID shipment=uuid(id);return pdf(execute(s->handling.label(s,shipment)),shipment);}
    @GetMapping("/api/v1/station-operations/stations") public Map<String,Object> stations(){return execute(s->Map.of("stations",intake.stations(s),"simulation",true));}
    @GetMapping("/api/v1/station-operations/stations/{station}/parcels/{barcode}") public Map<String,Object> lookup(@PathVariable String station,@PathVariable String barcode){return execute(s->{var c=new Carrier(s,station);var cargo=handling.lookup(c,barcode);route(s,cargo);return view(s,c,cargo.shipment().id());});}
    @GetMapping("/api/v1/station-operations/stations/{station}/shipments/{id}") public Map<String,Object> shipment(@PathVariable String station,@PathVariable String id){return execute(s->view(s,new Carrier(s,station),uuid(id)));}
    @PostMapping("/api/v1/station-operations/stations/{station}/scans") public StationIntake.Scan scan(@PathVariable String station,HttpServletRequest request)throws IOException{
        var v=body(request,Set.of("barcode"));if(!v.get("barcode").isString())throw problem(400,"VALIDATION_FAILED");UUID key=key(request);return execute(s->{var c=new Carrier(s,station);var cargo=handling.lookup(c,v.get("barcode").asString());route(s,cargo);payment(c,cargo);return intake.scan(s,station,cargo,v.get("barcode").asString(),key);});
    }
    @PostMapping("/api/v1/station-operations/stations/{station}/shipments/{id}/acceptance") public ResponseEntity<Map<String,Object>> accept(@PathVariable String station,@PathVariable String id,HttpServletRequest request)throws IOException{
        UUID shipment=uuid(id),key=key(request);long expected=version(request);var v=body(request,Set.of("scanIds","inspection"));if(!v.get("scanIds").isArray()||v.get("scanIds").size()>5||!v.get("inspection").isString())throw problem(400,"VALIDATION_FAILED");List<UUID> scans=new ArrayList<>();for(var node:v.get("scanIds")){if(!node.isString())throw problem(400,"VALIDATION_FAILED");scans.add(uuid(node.asString()));}
        var result=execute(s->{var c=new Carrier(s,station);var cargo=handling.atStation(c,shipment);route(s,cargo);payment(c,cargo);var accepted=intake.accept(s,station,cargo,scans,expected,v.get("inspection").asString(),key);if(accepted.newlyCommitted())handling.accept(c,shipment,expected,accepted.id(),accepted.occurredAt());return view(s,c,shipment);});return ResponseEntity.ok().eTag("\"i1\"").body(result);
    }
    @GetMapping("/api/v1/station-operations/stations/{station}/receipts/{key}") public StationIntake.Receipt receipt(@PathVariable String station,@PathVariable String key){return execute(s->intake.receipt(s,station,uuid(key)));}
    @GetMapping("/api/v1/station-operations/stations/{station}/shipments/{id}/labels.pdf") public ResponseEntity<byte[]> stationLabel(@PathVariable String station,@PathVariable String id){UUID shipment=uuid(id);return pdf(execute(s->{var c=new Carrier(s,station);route(s,handling.atStation(c,shipment));return handling.stationLabel(c,shipment);}),shipment);}
    private static ResponseEntity<byte[]> pdf(byte[] bytes,UUID shipment){return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header("Cache-Control","private, no-store").header("Content-Disposition","attachment; filename=\"argopost-simulation-"+shipment+".pdf\"").header("X-Content-Type-Options","nosniff").body(bytes);}
    private static UUID uuid(String v){try{UUID id=UUID.fromString(v);if(!id.toString().equals(v))throw new IllegalArgumentException();return id;}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static UUID key(HttpServletRequest r){var values=Collections.list(r.getHeaders("Idempotency-Key"));if(values.size()!=1)throw problem(400,"VALIDATION_FAILED");return uuid(values.getFirst());}
    private static long version(HttpServletRequest r){var values=Collections.list(r.getHeaders("If-Match"));if(values.isEmpty())throw problem(428,"PRECONDITION_REQUIRED");if(values.size()!=1||!values.getFirst().matches("\"i[01]\""))throw problem(400,"VALIDATION_FAILED");return values.getFirst().charAt(2)-'0';}
    private static JsonNode body(HttpServletRequest r,Set<String> fields)throws IOException{if(r.getContentType()==null||!r.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");byte[] bytes=r.getInputStream().readNBytes(4097);try{if(bytes.length>4096)throw problem(400,"VALIDATION_FAILED");var v=JSON.readTree(bytes);if(v==null||!v.isObject()||!v.propertyNames().equals(fields))throw problem(400,"VALIDATION_FAILED");return v;}catch(CustomerProfileHttp.HttpProblem invalid){throw invalid;}catch(Exception invalid){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}}
}
