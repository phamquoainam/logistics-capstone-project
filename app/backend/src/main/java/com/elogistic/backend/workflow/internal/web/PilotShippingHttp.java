package com.elogistic.backend.workflow.internal.web;
import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.merchant.api.customer.PilotCoverage;
import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.pricing.api.*;
import com.elogistic.backend.finance.api.PilotObligations;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;
import static com.elogistic.backend.pricing.api.PilotQuoteFailure.Code.*;

/** Same physical admission transaction for quote consumption, Orders snapshots and Finance UNPAID facts. */
@RestController @Profile("customer")
public final class PilotShippingHttp {
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final CustomerAdmission admission;private final CustomerRequestContext requests;private final ShippingDrafts drafts;private final PilotCoverage coverage;private final PilotQuotes quotes;private final PilotShipments shipments;private final PersonalShipmentManagement management;private final PilotObligations obligations;private final Clock clock;
    public PilotShippingHttp(CustomerAdmission admission,CustomerRequestContext requests,ShippingDrafts drafts,PilotCoverage coverage,PilotQuotes quotes,PilotShipments shipments,PersonalShipmentManagement management,PilotObligations obligations,@Qualifier("identityHttpClock")Clock clock){this.admission=admission;this.requests=requests;this.drafts=drafts;this.coverage=coverage;this.quotes=quotes;this.shipments=shipments;this.management=management;this.obligations=obligations;this.clock=clock;}
    private final class Scope implements ShippingDrafts.Access,PilotQuotes.Access,PilotObligations.Access {
        private final CustomerAdmission.Context context;Scope(CustomerAdmission.Context context){this.context=context;}
        public UUID accountId(){requireCurrent();return context.actor().accountId();}public UUID requestId(){requireCurrent();return context.requestId();}public void requireCurrent(){admission.requireCurrent(context);}public void meaningfulAction(){admission.meaningfulAction(context);}
    }
    private <T>T execute(Function<Scope,T> work){var r=requests.current();return admission.execute(r.encodedAccess(),r.requestId(),Set.of(),c->work.apply(new Scope(c)));}
    private PilotQuotes.Route route(Scope s,String origin,String destination){var r=coverage.catalog(s.context).routes().stream().filter(v->v.originCode().equals(origin)&&v.destinationCode().equals(destination)).findFirst().orElseThrow(()->new PilotQuoteFailure(UNSUPPORTED_ROUTE));return new PilotQuotes.Route(r.datasetVersion(),r.originCode(),r.destinationCode(),r.classification(),r.dropOffCode());}
    @GetMapping("/api/v1/customer/shipping-catalog") public Map<String,Object> catalog(){return execute(s->Map.of("simulation",true,"serviceCode","ROAD_STANDARD","quoteEnabled",quotes.enabled(),"bookingEnabled",quotes.enabled(),"coverage",coverage.catalog(s.context),"tariff",quotes.rules(s),"paymentMethod","CASH_ON_HANDOVER","payer","SENDER","paymentCollectionEnabled",false));}
    @GetMapping("/api/v1/customer/shipping-availability") public Map<String,Object> availability(){return execute(s->{boolean ready=false;if(quotes.enabled()){try{quotes.rules(s);coverage.catalog(s.context);ready=true;}catch(PilotQuoteFailure|IllegalStateException unavailable){s.requireCurrent();}}return Map.of("quoteEnabled",ready,"bookingEnabled",ready,"codEnabled",false,"paymentCollectionEnabled",false,"blockers",ready?List.of("PAYMENT_COLLECTION_AUTHORITY_UNAVAILABLE","PICKUP_NOT_SUPPORTED","CARRIER_OPERATIONS_UNAVAILABLE"):List.of("PILOT_DISABLED_OR_NOT_EFFECTIVE"));});}
    @PostMapping("/api/v1/customer/shipment-drafts/{id}/quotes") public ResponseEntity<Map<String,Object>> quote(@PathVariable String id,HttpServletRequest request)throws IOException{
        UUID draft=uuid(id),key=key(request);long version=version(request);JsonNode v=body(request,Set.of("originCode","destinationCode","ordinaryGoodsConfirmed"));if(!v.get("originCode").isString()||!v.get("destinationCode").isString()||!v.get("ordinaryGoodsConfirmed").isBoolean())throw problem(400,"VALIDATION_FAILED");
        var result=execute(s->{quotes.rules(s);var d=drafts.get(s,draft);if(d.version()!=version)throw new PilotQuoteFailure(PRECONDITION_STALE);var r=route(s,v.get("originCode").asString(),v.get("destinationCode").asString());var facts=d.input().parcels().stream().map(p->new PilotQuotes.Parcel(p.weightGrams(),p.lengthMm(),p.widthMm(),p.heightMm())).toList();return quoteView(s,quotes.create(s,new PilotQuotes.Request(draft,version,r,facts,v.get("ordinaryGoodsConfirmed").asBoolean()),key));});var priced=(PilotQuotes.Quote)result.get("quote");return ResponseEntity.created(URI.create("/api/v1/customer/shipping-quotes/"+priced.id())).body(result);
    }
    @GetMapping("/api/v1/customer/shipping-quotes/{id}") public Map<String,Object> getQuote(@PathVariable String id){return execute(s->quoteView(s,quotes.get(s,uuid(id))));}
    @GetMapping("/api/v1/customer/quote-receipts/{key}") public Map<String,Object> quoteReceipt(@PathVariable String key){return execute(s->Map.of("targetId",quotes.receipt(s,uuid(key)),"outcome","COMMITTED"));}
    private Map<String,Object> quoteView(Scope s,PilotQuotes.Quote q){boolean usable=q.consumedBy()==null&&!clock.instant().isBefore(q.createdAt())&&clock.instant().isBefore(q.expiresAt());if(usable){try{usable=drafts.get(s,q.draftId()).version()==q.draftVersion()&&route(s,q.route().originCode(),q.route().destinationCode()).equals(q.route())&&quotes.rules(s).version().equals(q.tariffVersion());}catch(ShippingDraftFailure missing){if(missing.code()!=ShippingDraftFailure.Code.RESOURCE_NOT_FOUND)throw missing;usable=false;}catch(PilotQuoteFailure invalid){if(invalid.code()!=UNSUPPORTED_ROUTE)throw invalid;usable=false;}}return Map.of("quote",q,"usable",usable,"serverTime",clock.instant());}
    @PostMapping("/api/v1/customer/shipment-drafts/{id}/bookings") public ResponseEntity<Map<String,Object>> book(@PathVariable String id,HttpServletRequest request)throws IOException{
        UUID draft=uuid(id),key=key(request);long version=version(request);var v=body(request,Set.of("quoteId"));if(!v.get("quoteId").isString())throw problem(400,"VALIDATION_FAILED");UUID quote=uuid(v.get("quoteId").asString());
        var result=execute(s->{var previous=shipments.replay(s,draft,version,quote,key);if(previous.isPresent())return shipmentView(s,previous.get());shipments.requireBookable(s,draft,version);var priced=quotes.get(s,quote);if(!route(s,priced.route().originCode(),priced.route().destinationCode()).equals(priced.route()))throw new PilotQuoteFailure(UNSUPPORTED_ROUTE);UUID target=UUID.randomUUID();var consumed=quotes.consume(s,quote,draft,version,target);var shipment=shipments.confirm(s,draft,version,quote,key,target,consumed);obligations.recordUnpaid(s,shipment.id(),quote,shipment.totalVnd());return shipmentView(s,shipment);});var shipment=(PilotShipments.Shipment)result.get("shipment");return ResponseEntity.created(URI.create("/api/v1/customer/shipments/"+shipment.id())).eTag("\"v1\"").body(result);
    }
    @GetMapping("/api/v1/customer/shipments/{id}") public ResponseEntity<Map<String,Object>> shipment(@PathVariable String id){return ResponseEntity.ok().eTag("\"v1\"").body(execute(s->shipmentView(s,shipments.get(s,uuid(id)))));}
    @GetMapping("/api/v1/customer/shipments") public Map<String,Object> list(HttpServletRequest r){
        var p=r.getParameterMap();if(p.values().stream().anyMatch(v->v.length!=1))throw problem(400,"VALIDATION_FAILED");
        if(p.containsKey("view")){
            Map<String,String> parameters=new HashMap<>();p.forEach((k,v)->parameters.put(k,v[0]));
            return execute(s->{var result=management.read(s,Map.copyOf(parameters));return Map.of("schemaVersion","PERSONAL_MANAGEMENT_1","generatedAt",result.generatedAt(),"timeZone","Asia/Ho_Chi_Minh","summary",result.summary(),"total",result.total(),"page",result.page(),"size",20,"hasNext",result.hasNext(),"items",result.items().stream().map(v->Map.of("record",shipmentView(s,v.shipment()),"status",v.status())).toList());});
        }
        if(!Set.of("page","size","search","status").containsAll(p.keySet()))throw problem(400,"VALIDATION_FAILED");int page=number(r.getParameter("page"),0);if(number(r.getParameter("size"),20)!=20)throw problem(400,"VALIDATION_FAILED");return execute(s->{var result=shipments.list(s,page,r.getParameter("search"),r.getParameter("status"));return Map.of("items",result.items().stream().map(v->shipmentView(s,v)).toList(),"page",page,"size",20,"hasNext",result.hasNext());});
    }
    @GetMapping("/api/v1/customer/booking-receipts/{key}") public PilotShipments.Receipt receipt(@PathVariable String key){return execute(s->shipments.receipt(s,uuid(key)));}
    private Map<String,Object> shipmentView(Scope s,PilotShipments.Shipment shipment){var obligation=obligations.get(s,shipment.id());if(!"UNPAID".equals(obligation.state())||!"VND".equals(obligation.currency())||obligation.amountVnd()!=shipment.totalVnd()||!obligation.quoteId().equals(shipment.quote().id()))throw new IllegalStateException("PILOT_OBLIGATION_BINDING_REQUIRED");return Map.of("shipment",shipment,"obligation",obligation,"simulation",true);}
    private static JsonNode body(HttpServletRequest r,Set<String> fields)throws IOException{if(r.getContentType()==null||!r.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");byte[] bytes=r.getInputStream().readNBytes(4097);try{if(bytes.length>4096)throw problem(400,"VALIDATION_FAILED");var v=JSON.readTree(bytes);if(v==null||!v.isObject()||!v.propertyNames().equals(fields))throw problem(400,"VALIDATION_FAILED");return v;}catch(CustomerProfileHttp.HttpProblem invalid){throw invalid;}catch(Exception invalid){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}}
    private static UUID uuid(String v){try{UUID id=UUID.fromString(v);if(!id.toString().equals(v))throw new IllegalArgumentException();return id;}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static UUID key(HttpServletRequest r){var values=Collections.list(r.getHeaders("Idempotency-Key"));if(values.size()!=1)throw problem(400,"VALIDATION_FAILED");return uuid(values.getFirst());}
    private static long version(HttpServletRequest r){var values=Collections.list(r.getHeaders("If-Match"));if(values.isEmpty())throw problem(428,"PRECONDITION_REQUIRED");try{if(values.size()!=1||!values.getFirst().matches("\"v[1-9][0-9]*\""))throw new IllegalArgumentException();return Long.parseLong(values.getFirst().substring(2,values.getFirst().length()-1));}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static int number(String v,int fallback){if(v==null)return fallback;try{if(!v.matches("0|[1-9][0-9]*"))throw new IllegalArgumentException();return Integer.parseInt(v);}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
}
