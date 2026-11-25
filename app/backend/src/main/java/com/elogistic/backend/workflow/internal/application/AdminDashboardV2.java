package com.elogistic.backend.workflow.internal.application;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.notifications.api.*;
import com.elogistic.backend.notifications.api.AdminDashboardReads.*;
import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.finance.api.AdminObligationFacts;
import com.elogistic.backend.merchant.api.*;
import com.elogistic.backend.dispatch.api.AdminNetworkFacts;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;

/** Public-owner composition in one genuine Identity RR callback. No source SQL or authority union. */
public final class AdminDashboardV2 {
    public static final String MEDIA="application/vnd.argopost.admin-dashboard.v2+json",SCHEMA="ADMIN_DASHBOARD_2";
    private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private final AdminDashboardReads reports;private final AdminShipmentFacts shipments;private final AdminObligationFacts finance;private final AdminApplicationFacts merchant;private final AdminNetworkFacts network;private final AdminOversightReads oversight;private final MerchantGovernance governance;
    public AdminDashboardV2(AdminDashboardReads reports,AdminShipmentFacts shipments,AdminObligationFacts finance,AdminApplicationFacts merchant,AdminNetworkFacts network,AdminOversightReads oversight,MerchantGovernance governance){this.reports=reports;this.shipments=shipments;this.finance=finance;this.merchant=merchant;this.network=network;this.oversight=oversight;this.governance=governance;}
    private record Access(AdminReadContext actor) implements AdminShipmentFacts.Access,AdminObligationFacts.Access,AdminApplicationFacts.Access,AdminOversightReads.Access,AdminNetworkFacts.Access{
        public void requireShipmentFacts(){actor.requireCurrent();if(!actor.permissions().contains(AdminReadContext.SHIPMENT)&&!actor.permissions().contains(AdminReadContext.FINANCE))throw new AdminFailure("ACCESS_DENIED");}
        public void requireFinanceFacts(){actor.require(AdminReadContext.FINANCE);}public void requireApplicationFacts(){actor.require(AdminReadContext.MERCHANT);}public AdminReadContext identityContext(){return actor;}public void requireOversightFacts(){actor.require(AdminReadContext.SHIPMENT);}public void requireNetworkFacts(){actor.require(AdminReadContext.NETWORK);}
    }
    private record Data(Dashboard legacy,AdminApplicationFacts.Capture applications,List<AdminShipmentFacts.Row> shipments,List<AdminObligationFacts.Row> obligations,String scopeVersion,String fingerprint,List<ResourceScopeV2> scopes,Map<UUID,Instant> acceptanceTimes){}
    private static String digest(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException unavailable){throw new AdminFailure("SERVICE_UNAVAILABLE");}}
    private static AdminApplicationFacts.ApplicationCohort applications(QueryV2 q){return new AdminApplicationFacts.ApplicationCohort(q.from().atStartOfDay(ZONE).toInstant(),q.to().plusDays(1).atStartOfDay(ZONE).toInstant(),q.applicationState(),q.policyId());}
    private Data capture(AdminReadContext actor,QueryV2 q,Instant cutoff,Instant checkedAt){
        actor.require(AdminReadContext.ENTRY);if(q.applicationState()!=null||q.policyId()!=null)actor.require(AdminReadContext.MERCHANT);
        var access=new Access(actor);var rows=actor.permissions().contains(AdminReadContext.SHIPMENT)||actor.permissions().contains(AdminReadContext.FINANCE)?shipments.capture(access,q.from().atStartOfDay(ZONE).toInstant(),q.to().plusDays(1).atStartOfDay(ZONE).toInstant(),checkedAt,q.shipmentStatus()).stream().filter(r->!r.bookedAt().isAfter(cutoff)).toList():List.<AdminShipmentFacts.Row>of();
        var ids=new HashSet<>(rows.stream().map(AdminShipmentFacts.Row::id).toList());var money=actor.permissions().contains(AdminReadContext.FINANCE)?finance.capture(access,ids):List.<AdminObligationFacts.Row>of();
        if(actor.permissions().contains(AdminReadContext.NETWORK)&&!"SOURCE_NOT_IMPLEMENTED".equals(network.availability(access)))throw new AdminFailure("SERVICE_UNAVAILABLE");
        var old=reports.dashboard(actor,q.legacy(),cutoff,rows,money,Set.of("MERCHANT","NETWORK"));
        var facts=actor.permissions().contains(AdminReadContext.MERCHANT)?merchant.capture(access,applications(q),cutoff):null;
        var context=governance==null?null:governance.context(actor);var scopes=new ArrayList<ResourceScopeV2>();
        var realmCapabilities=actor.permissions().stream().filter(p->p.equals(AdminReadContext.SHIPMENT)||p.equals(AdminReadContext.FINANCE)||p.equals(AdminReadContext.TECHNICAL)).sorted().toList();
        if(!realmCapabilities.isEmpty())scopes.add(new ResourceScopeV2("REALM_BOUND_LOCAL",actor.realmId(),realmCapabilities,actor.permissionVersion()));
        if(context!=null)for(var assignment:context.assignments()){
            var capabilities=assignment.permissions().stream().filter(p->p.endsWith(".read")).sorted().toList();if(!capabilities.isEmpty())scopes.add(new ResourceScopeV2(assignment.scopeType().equals("POLICY")?"MERCHANT_POLICY":"MERCHANT_ORGANIZATION",assignment.policyId()==null?assignment.organizationId():assignment.policyId(),capabilities,digest(context.authorityVersion()+"|"+assignment)));
        }
        if(scopes.size()>100)throw new AdminFailure("SERVICE_UNAVAILABLE");
        String scope=digest(AdminApplicationMetrics.VERSION+"|"+actor.accountId()+"|"+actor.realmId()+"|"+actor.environment()+"|"+actor.permissionVersion()+"|"+(context==null?"SOURCE_NOT_IMPLEMENTED":context.authorityVersion())+"|"+(facts==null?"NO_MERCHANT_READ":facts.resourceScopeVersion()));
        Map<UUID,Instant> accepts=new TreeMap<>();
        if(actor.permissions().contains(AdminReadContext.SHIPMENT))for(var row:rows)if(row.status().equals("AT_STATION")){
            if(oversight==null)throw new AdminFailure("SOURCE_UNAVAILABLE");var detail=oversight.get(access,row.id(),new AdminOversightReads.Selector(q.from().atStartOfDay(ZONE).toInstant(),q.to().plusDays(1).atStartOfDay(ZONE).toInstant(),checkedAt,q.shipmentStatus(),ids));
            if(detail.acceptance()==null)throw new AdminFailure("SERVICE_UNAVAILABLE");accepts.put(row.id(),detail.acceptance().occurredAt());
        }
        actor.requireCurrent();return new Data(old,facts,rows,money,scope,digest(old.snapshotId()+"|"+(facts==null?"NO_MERCHANT_READ":facts.fingerprint())+"|"+accepts),List.copyOf(scopes),Map.copyOf(accepts));
    }
    public ContextV2 context(AdminReadContext actor,Instant checkedAt){
        var today=checkedAt.atZone(ZONE).toLocalDate();var q=new QueryV2(today,today,"DAY",null,null,null,null);var data=capture(actor,q,checkedAt,checkedAt);
        return new ContextV2(SCHEMA,actor.accountId(),actor.displayName(),actor.permissions().stream().sorted().toList(),actor.environment(),ZONE.getId(),false,data.scopeVersion(),data.scopes());
    }
    private static Map<String,String> filters(QueryV2 q,boolean merchant){Map<String,String> result=new TreeMap<>();result.put("from",q.from().toString());result.put("to",q.to().toString());result.put("grain",q.grain());if(q.applicationState()!=null)result.put("applicationState",q.applicationState());if(q.policyId()!=null)result.put("policyId",q.policyId().toString());if(q.shipmentStatus()!=null)result.put("shipmentStatus",q.shipmentStatus());return result;}
    private List<MetricMetadataV2> metadata(AdminReadContext actor,Data data,Instant checkedAt){
        List<MetricMetadataV2> result=new ArrayList<>();
        for(var section:data.legacy().sections())if(!section.key().equals("MERCHANT"))for(var metric:section.metrics()){
            var contributing=AdminMetricCohorts.select(data.shipments(),metric.key());Instant last=contributing.stream().map(r->metric.key().equals("ACCEPTED")?data.acceptanceTimes().get(r.id()):r.bookedAt()).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
            result.add(new MetricMetadataV2(metric.key(),AdminApplicationMetrics.VERSION,"BOOKED_SHIPMENT",ZONE.getId(),metric.value(),null,metric.unit(),null,last,checkedAt,"CURRENT_TRANSACTION",null));
        }
        if(actor.permissions().contains(AdminReadContext.MERCHANT))for(var entry:AdminApplicationMetrics.measure(data.applications()).entrySet()){
            var value=entry.getValue();result.add(new MetricMetadataV2(entry.getKey(),AdminApplicationMetrics.VERSION,"FIRST_SUBMITTED_APPLICATION",ZONE.getId(),value.numerator(),value.denominator(),value.unit(),value.sampleCount(),value.lastSourceEventAt(),checkedAt,"CURRENT_TRANSACTION",null));
        }
        if(result.size()>32)throw new AdminFailure("SERVICE_UNAVAILABLE");return List.copyOf(result);
    }
    public DashboardV2 dashboard(AdminReadContext actor,QueryV2 q,Instant checkedAt){
        var data=capture(actor,q,checkedAt,checkedAt);var sections=new ArrayList<SectionV2>();
        for(var section:data.legacy().sections()){
            if(section.key().equals("MERCHANT")){
                var facts=data.applications();var metrics=new ArrayList<MetricV2>();for(var entry:AdminApplicationMetrics.measure(facts).entrySet()){var value=entry.getValue();metrics.add(new MetricV2(entry.getKey(),value.value(),value.unit(),value.value()==null?null:new Drilldown("/api/v1/admin/dashboard/merchant-applications",filters(q,true),entry.getKey())));}
                var series=AdminApplicationMetrics.series(facts,q.from(),q.to(),q.grain()).stream().map(p->new Point(p.from(),p.to(),p.metric(),p.value())).toList();sections.add(new SectionV2("MERCHANT",facts.availability(),facts.reasonCode(),metrics,series));
            }else sections.add(new SectionV2(section.key(),section.availability(),section.reasonCode(),section.metrics().stream().map(m->new MetricV2(m.key(),m.value(),m.unit(),m.drilldown()==null?null:new Drilldown(m.drilldown().path(),filters(q,false),m.key()))).toList(),section.series()));
        }
        String snapshot=reports.snapshotV2(actor,q,checkedAt,data.scopeVersion(),data.fingerprint());return new DashboardV2(SCHEMA,q,checkedAt,snapshot,sections,data.scopeVersion(),metadata(actor,data,checkedAt));
    }
    private Data verify(AdminReadContext actor,QueryV2 q,String token,Instant now){
        var signed=reports.selectorV2(actor,q,token,now);var data=capture(actor,q,signed.cutoff(),now);
        if(!signed.resourceScopeVersion().equals(data.scopeVersion())||!signed.fingerprint().equals(data.fingerprint()))throw new AdminFailure("SNAPSHOT_EXPIRED");return data;
    }
    public PageV2<AdminShipmentFacts.Row> shipments(AdminReadContext actor,QueryV2 q,String token,Instant now,String metric,int page,int size){
        actor.require(AdminReadContext.SHIPMENT);if(metric!=null&&metric.endsWith("FEES"))actor.require(AdminReadContext.FINANCE);var data=verify(actor,q,token,now);
        var selected=AdminMetricCohorts.select(data.shipments(),metric);checkPage(page,size);long start=(long)page*size;
        return new PageV2<>(SCHEMA,selected.stream().skip(start).limit(size).toList(),page,size,start+size<selected.size(),token,data.scopeVersion(),metadata(actor,data,now).stream().filter(m->m.metricKey().equals(metric)).toList());
    }
    public PageV2<ApplicationItem> applications(AdminReadContext actor,QueryV2 q,String token,Instant now,String metric,int page,int size){
        actor.require(AdminReadContext.MERCHANT);var data=verify(actor,q,token,now);if(!data.applications().availability().equals("AVAILABLE"))throw new AdminFailure("SOURCE_UNAVAILABLE");
        if(!AdminApplicationMetrics.measure(data.applications()).containsKey(Objects.toString(metric,"")))throw new AdminFailure("VALIDATION_FAILED");
        var signed=reports.selectorV2(actor,q,token,now);var selected=merchant.page(new Access(actor),applications(q),new AdminApplicationFacts.Selector(metric,signed.cutoff(),data.applications().resourceScopeVersion(),data.applications().fingerprint()),page,size);
        return new PageV2<>(SCHEMA,selected.items().stream().map(r->new ApplicationItem(r.id(),r.state(),r.submittedAt(),r.updatedAt())).toList(),page,size,selected.hasNext(),token,data.scopeVersion(),metadata(actor,data,now).stream().filter(m->m.metricKey().equals(metric)).toList());
    }
    private static void checkPage(int page,int size){if(page<0||page>10000||size<1||size>50)throw new AdminFailure("VALIDATION_FAILED");}
}
