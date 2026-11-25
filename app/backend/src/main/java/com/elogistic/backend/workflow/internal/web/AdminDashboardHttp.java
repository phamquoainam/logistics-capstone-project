package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.notifications.api.AdminDashboardReads;
import com.elogistic.backend.orders.api.AdminShipmentFacts;
import com.elogistic.backend.orders.api.AdminOversightReads;
import com.elogistic.backend.notifications.api.AdminMetricCohorts;
import com.elogistic.backend.finance.api.AdminObligationFacts;
import com.elogistic.backend.merchant.api.AdminApplicationFacts;
import com.elogistic.backend.dispatch.api.AdminNetworkFacts;
import jakarta.servlet.http.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Public-owner composition only. Admission owns one read-only RR callback, no independent read transactions. */
@RestController @Profile("identity")
public final class AdminDashboardHttp {
    private static final String BASE="/api/v1/admin/dashboard";
    private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private final AdminAdmission admission;private final AdminDashboardReads reports;private final AdminShipmentFacts shipments;
    private final AdminObligationFacts finance;private final AdminApplicationFacts merchant;private final AdminNetworkFacts network;private final Clock clock;private final AdminOversightReads oversight;private final java.util.function.Supplier<com.elogistic.backend.merchant.api.MerchantGovernance> governance;
    public AdminDashboardHttp(AdminAdmission admission,AdminDashboardReads reports,AdminShipmentFacts shipments,AdminObligationFacts finance,AdminApplicationFacts merchant,AdminNetworkFacts network,Clock clock){this(admission,reports,shipments,finance,merchant,network,null,clock);}
    public AdminDashboardHttp(AdminAdmission admission,AdminDashboardReads reports,AdminShipmentFacts shipments,AdminObligationFacts finance,AdminApplicationFacts merchant,AdminNetworkFacts network,AdminOversightReads oversight,Clock clock){this(admission,reports,shipments,finance,merchant,network,oversight,clock,()->null);}
    @org.springframework.beans.factory.annotation.Autowired
    public AdminDashboardHttp(AdminAdmission admission,AdminDashboardReads reports,AdminShipmentFacts shipments,AdminObligationFacts finance,AdminApplicationFacts merchant,AdminNetworkFacts network,AdminOversightReads oversight,@Qualifier("identityHttpClock")Clock clock,org.springframework.beans.factory.ObjectProvider<com.elogistic.backend.merchant.api.MerchantGovernance> governance){this(admission,reports,shipments,finance,merchant,network,oversight,clock,governance::getIfAvailable);}
    public AdminDashboardHttp(AdminAdmission admission,AdminDashboardReads reports,AdminShipmentFacts shipments,AdminObligationFacts finance,AdminApplicationFacts merchant,AdminNetworkFacts network,AdminOversightReads oversight,Clock clock,java.util.function.Supplier<com.elogistic.backend.merchant.api.MerchantGovernance> governance){this.admission=admission;this.reports=reports;this.shipments=shipments;this.finance=finance;this.merchant=merchant;this.network=network;this.clock=clock;this.oversight=oversight;this.governance=governance;}
    private com.elogistic.backend.workflow.internal.application.AdminDashboardV2 v2(){return new com.elogistic.backend.workflow.internal.application.AdminDashboardV2(reports,shipments,finance,merchant,network,oversight,governance.get());}
    private static boolean v2(HttpServletRequest request){return Boolean.TRUE.equals(request.getAttribute("argopost.dashboard.v2"));}
    public record Context(UUID accountId,String displayName,List<String> permissions,String environment,String timeZone,boolean regionFilterAvailable){}
    public record SourceStatus(String availability,List<AdminDashboardReads.Metric> metrics){}
    public record PlatformStatus(Instant checkedAt,String apiAvailability,SourceStatus queues,SourceStatus scheduledJobs,SourceStatus integrationFailures){}
    private record Cohort(List<AdminShipmentFacts.Row> rows,List<AdminObligationFacts.Row> obligations){}
    public record ShipmentOversight(AdminShipmentFacts.Row shipment,List<AdminOversightReads.Parcel> parcels,List<AdminOversightReads.Event> tracking,AdminOversightReads.Acceptance acceptance,String snapshotId){}
    /** Owner-local port views preserve the architecture collaborator graph; no new shared authority engine. */
    private record Scope(AdminReadContext actor) implements AdminShipmentFacts.Access,AdminObligationFacts.Access,AdminApplicationFacts.Access,AdminNetworkFacts.Access,AdminOversightReads.Access {
        public void requireOversightFacts(){actor.require(AdminReadContext.SHIPMENT);}
        public void requireShipmentFacts(){actor.requireCurrent();if(!actor.permissions().contains(AdminReadContext.SHIPMENT)&&!actor.permissions().contains(AdminReadContext.FINANCE))throw new AdminFailure("ACCESS_DENIED");}
        public void requireFinanceFacts(){actor.require(AdminReadContext.FINANCE);}
        public void requireApplicationFacts(){actor.require(AdminReadContext.MERCHANT);}
        public AdminReadContext identityContext(){return actor;}
        public void requireNetworkFacts(){actor.require(AdminReadContext.NETWORK);}
    }
    @ModelAttribute public void noStore(HttpServletRequest request,HttpServletResponse r){
        r.setHeader("Cache-Control","no-store");r.setHeader("Referrer-Policy","no-referrer");r.setHeader("X-Content-Type-Options","nosniff");
        if(!request.getRequestURI().startsWith(BASE))return;r.setHeader("Vary","Accept");
        var headers=Collections.list(request.getHeaders("Accept"));if(headers.size()>1)throw new AdminFailure("NOT_ACCEPTABLE");
        boolean version2=false;for(String entry:headers.isEmpty()?List.<String>of():Arrays.asList(headers.getFirst().split(","))){String type=entry.trim().split(";",2)[0].trim().toLowerCase(Locale.ROOT);if(type.startsWith("application/vnd.argopost.admin-dashboard.")){if(!type.equals(com.elogistic.backend.workflow.internal.application.AdminDashboardV2.MEDIA)||entry.matches("(?i).*;\\s*q=0(?:\\.0*)?\\s*(?:;.*)?$"))throw new AdminFailure("NOT_ACCEPTABLE");version2=true;}}
        if(version2){request.setAttribute("argopost.dashboard.v2",true);r.setContentType(com.elogistic.backend.workflow.internal.application.AdminDashboardV2.MEDIA);}
    }
    private Map<String,String> parameters(HttpServletRequest request){var result=new HashMap<String,String>();request.getParameterMap().forEach((key,value)->{if(value.length!=1||value[0]==null||key.length()>64||value[0].length()>4096)throw new AdminFailure("VALIDATION_FAILED");result.put(key,value[0]);});if(result.size()>12)throw new AdminFailure("VALIDATION_FAILED");return Map.copyOf(result);}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private void none(Map<String,String> p){if(!p.isEmpty())throw new AdminFailure("VALIDATION_FAILED");}
    private Cohort capture(AdminReadContext a,AdminDashboardReads.Query q,Instant cutoff){
        var scope=new Scope(a);var rows=a.permissions().contains(AdminReadContext.SHIPMENT)||a.permissions().contains(AdminReadContext.FINANCE)?shipments.capture(scope,q.from().atStartOfDay(ZONE).toInstant(),q.to().plusDays(1).atStartOfDay(ZONE).toInstant(),cutoff,q.shipmentStatus()):List.<AdminShipmentFacts.Row>of();
        var money=a.permissions().contains(AdminReadContext.FINANCE)?finance.capture(scope,new HashSet<>(rows.stream().map(AdminShipmentFacts.Row::id).toList())):List.<AdminObligationFacts.Row>of();return new Cohort(rows,money);
    }
    @GetMapping(BASE+"/context") public Object context(HttpServletRequest request){var p=parameters(request);return admission.execute(a->{none(p);if(v2(request))return v2().context(a,now());return new Context(a.accountId(),a.displayName(),a.permissions().stream().sorted().toList(),a.environment(),ZONE.getId(),false);});}
    @GetMapping(BASE) public Object dashboard(HttpServletRequest request){
        var p=parameters(request);return admission.execute(a->{Instant cutoff=now();if(v2(request))return v2().dashboard(a,reports.queryV2(p,cutoff,false),cutoff);var q=reports.query(p,cutoff,false);var missing=new HashSet<String>();
            if(a.permissions().contains(AdminReadContext.MERCHANT)){if(!merchant.availability(new Scope(a)).equals("SOURCE_NOT_IMPLEMENTED"))throw new AdminFailure("SERVICE_UNAVAILABLE");missing.add("MERCHANT");}
            if(a.permissions().contains(AdminReadContext.NETWORK)){if(!network.availability(new Scope(a)).equals("SOURCE_NOT_IMPLEMENTED"))throw new AdminFailure("SERVICE_UNAVAILABLE");missing.add("NETWORK");}
            var c=capture(a,q,cutoff);return reports.dashboard(a,q,cutoff,c.rows(),c.obligations(),missing);
        });
    }
    private int bounded(String text,int min,int max){try{if(!text.matches("0|[1-9][0-9]{0,5}"))throw new IllegalArgumentException();int n=Integer.parseInt(text);if(n<min||n>max)throw new IllegalArgumentException();return n;}catch(IllegalArgumentException bad){throw new AdminFailure("VALIDATION_FAILED");}}
    @GetMapping("/api/v1/admin/shipments/{shipmentId}") public ShipmentOversight oversight(@PathVariable String shipmentId,HttpServletRequest request){
        UUID id;try{id=UUID.fromString(shipmentId);if(!id.toString().equals(shipmentId))throw new IllegalArgumentException();}catch(IllegalArgumentException bad){throw new AdminFailure("VALIDATION_FAILED");}
        var p=parameters(request);if(!Set.of("from","to","grain","shipmentStatus","metric","snapshotId").containsAll(p.keySet()))throw new AdminFailure("VALIDATION_FAILED");
        return admission.execute(a->{
            a.require(AdminReadContext.SHIPMENT);if(oversight==null)throw new AdminFailure("SOURCE_UNAVAILABLE");
            var q=reports.query(p,now(),true);var selector=reports.selector(a,q,p.get("snapshotId"),now());var c=capture(a,q,selector.cutoff());
            reports.shipments(a,q,p.get("snapshotId"),now(),c.rows(),c.obligations(),p.get("metric"),0,1);
            var ids=new HashSet<>(AdminMetricCohorts.select(c.rows(),p.get("metric")).stream().map(AdminShipmentFacts.Row::id).toList());
            AdminOversightReads.Facts details;try{details=oversight.get(new Scope(a),id,new AdminOversightReads.Selector(q.from().atStartOfDay(ZONE).toInstant(),q.to().plusDays(1).atStartOfDay(ZONE).toInstant(),selector.cutoff(),q.shipmentStatus(),ids));}catch(AdminOversightReads.Failure failure){throw new AdminFailure(failure.code());}
            return new ShipmentOversight(details.shipment(),details.parcels(),details.tracking(),details.acceptance(),p.get("snapshotId"));
        });
    }
    @GetMapping(BASE+"/shipments") public Object shipments(HttpServletRequest request){
        var p=parameters(request);return admission.execute(a->{a.require(AdminReadContext.SHIPMENT);String metric=p.get("metric");
            if(v2(request))return v2().shipments(a,reports.queryV2(p,now(),true),p.get("snapshotId"),now(),metric,bounded(p.getOrDefault("page","0"),0,10000),bounded(p.getOrDefault("size","20"),1,50));
            if(!Set.of("BOOKINGS","WAITING_HANDOVER","ACCEPTED","PARCELS","CONFIRMED_FEES","UNPAID_FEES").contains(Objects.toString(metric,"")))throw new AdminFailure("VALIDATION_FAILED");
            if(metric.endsWith("FEES"))a.require(AdminReadContext.FINANCE);
            var q=reports.query(p,now(),true);var selector=reports.selector(a,q,p.get("snapshotId"),now());var c=capture(a,q,selector.cutoff());
            return reports.shipments(a,q,p.get("snapshotId"),now(),c.rows(),c.obligations(),metric,bounded(p.getOrDefault("page","0"),0,10000),bounded(p.getOrDefault("size","20"),1,50));
        });
    }
    @GetMapping(BASE+"/merchant-applications") public Object applications(HttpServletRequest request){var p=parameters(request);return admission.execute(a->{a.require(AdminReadContext.MERCHANT);if(v2(request))return v2().applications(a,reports.queryV2(p,now(),true),p.get("snapshotId"),now(),p.get("metric"),bounded(p.getOrDefault("page","0"),0,10000),bounded(p.getOrDefault("size","20"),1,50));var q=reports.query(p,now(),true);if(!"MERCHANT_APPLICATIONS".equals(p.get("metric")))throw new AdminFailure("VALIDATION_FAILED");reports.selector(a,q,p.get("snapshotId"),now());merchant.availability(new Scope(a));throw new AdminFailure("SOURCE_UNAVAILABLE");});}
    @GetMapping(BASE+"/network") public AdminDashboardReads.NetworkPage network(HttpServletRequest request){var p=parameters(request);return admission.execute(a->{a.require(AdminReadContext.NETWORK);if(v2(request)){var q2=reports.queryV2(p,now(),true);reports.selectorV2(a,q2,p.get("snapshotId"),now());network.availability(new Scope(a));throw new AdminFailure("SOURCE_UNAVAILABLE");}var q=reports.query(p,now(),true);if(!Set.of("STATIONS","HUBS","PARTNERS").contains(Objects.toString(p.get("metric"),"")))throw new AdminFailure("VALIDATION_FAILED");reports.selector(a,q,p.get("snapshotId"),now());network.availability(new Scope(a));throw new AdminFailure("SOURCE_UNAVAILABLE");});}
    @GetMapping(BASE+"/platform") public Object platform(HttpServletRequest request){var p=parameters(request);return admission.execute(a->{a.require(AdminReadContext.TECHNICAL);none(p);var missing=new SourceStatus("SOURCE_NOT_IMPLEMENTED",List.of());if(v2(request)){var v2missing=new AdminDashboardReads.SourceStatusV2("SOURCE_NOT_IMPLEMENTED",List.of());return new AdminDashboardReads.PlatformV2("ADMIN_DASHBOARD_2",now(),"UP",v2missing,v2missing,v2missing);}return new PlatformStatus(now(),"UP",missing,missing,missing);});}
    @ExceptionHandler(AdminFailure.class) public ResponseEntity<Map<String,String>> error(AdminFailure failure){int status=switch(failure.code()){case "NOT_ACCEPTABLE"->406;case "VALIDATION_FAILED"->400;case "SESSION_INVALID"->401;case "ACCESS_DENIED"->403;case "RESOURCE_NOT_FOUND"->404;case "SNAPSHOT_EXPIRED"->409;case "SOURCE_UNAVAILABLE"->422;default->503;};return ResponseEntity.status(status).header("Cache-Control","no-store").header("Content-Type","application/json").body(Map.of("code",failure.code(),"message","Request rejected."));}
    @ExceptionHandler(com.elogistic.backend.merchant.api.GovernanceFailure.class) public ResponseEntity<Map<String,String>> merchantError(com.elogistic.backend.merchant.api.GovernanceFailure failure){int status=switch(failure.code()){case VALIDATION_FAILED->400;case ACCESS_DENIED->403;case RESOURCE_NOT_FOUND->404;case SNAPSHOT_EXPIRED->409;case POLICY_UNAVAILABLE->422;default->503;};return ResponseEntity.status(status).header("Cache-Control","no-store").header("Content-Type","application/json").body(Map.of("code",failure.code().name(),"message","Request rejected."));}
    @ExceptionHandler(AdminOversightReads.Failure.class) public ResponseEntity<Map<String,String>> oversightError(AdminOversightReads.Failure failure){return error(new AdminFailure(failure.code()));}
}
