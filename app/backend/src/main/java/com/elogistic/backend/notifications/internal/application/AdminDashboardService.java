package com.elogistic.backend.notifications.internal.application;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.notifications.api.AdminDashboardReads;
import com.elogistic.backend.orders.api.AdminShipmentFacts;
import com.elogistic.backend.finance.api.AdminObligationFacts;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** One cohort, exact arithmetic, real calendar buckets, authenticated stateless drill-through. */
public final class AdminDashboardService implements AdminDashboardReads {
    private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> PARAMETERS=Set.of("from","to","grain","shipmentStatus","regionId");
    private final byte[] key;
    public AdminDashboardService(byte[] existingLimiterSecret){
        if(existingLimiterSecret==null||existingLimiterSecret.length<32)throw new IllegalArgumentException("ADMIN_SNAPSHOT_KEY_REQUIRED");
        key=hmac(existingLimiterSecret,"ARGOPOST_ADMIN_SNAPSHOT_KEY_V1".getBytes(StandardCharsets.US_ASCII));
    }
    private static AdminFailure invalid(){return new AdminFailure("VALIDATION_FAILED");}
    @Override public Query query(Map<String,String> p,Instant now,boolean page){
        var allowed=new HashSet<>(PARAMETERS);if(page)allowed.addAll(Set.of("snapshotId","metric","page","size"));
        if(p==null||!allowed.containsAll(p.keySet())||!p.containsKey("from")||!p.containsKey("to"))throw invalid();
        try{
            LocalDate from=LocalDate.parse(p.get("from")),to=LocalDate.parse(p.get("to"));String grain=p.getOrDefault("grain","DAY"),status=p.get("shipmentStatus"),region=p.get("regionId");
            if(!from.toString().equals(p.get("from"))||!to.toString().equals(p.get("to"))||from.getYear()<2000||to.isBefore(from)||to.isAfter(now.atZone(ZONE).toLocalDate())||ChronoUnit.DAYS.between(from,to)>=366||!Set.of("DAY","WEEK","MONTH").contains(grain)||status!=null&&!Set.of("BOOKED","AT_STATION").contains(status)||region!=null&&!region.isEmpty())throw invalid();
            if(page){bounded(p.getOrDefault("page","0"),0,10000);bounded(p.getOrDefault("size","20"),1,50);if(!p.containsKey("snapshotId")||!p.containsKey("metric"))throw invalid();}
            return new Query(from,to,grain,status,null);
        }catch(DateTimeException|NullPointerException bad){throw invalid();}
    }
    public static int bounded(String text,int min,int max){try{if(text==null||!text.matches("0|[1-9][0-9]{0,5}"))throw invalid();int number=Integer.parseInt(text);if(number<min||number>max)throw invalid();return number;}catch(NumberFormatException bad){throw invalid();}}
    private static byte[] hmac(byte[] secret,byte[] value){try{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));return mac.doFinal(value);}catch(GeneralSecurityException bad){throw new IllegalStateException("ADMIN_SNAPSHOT_UNAVAILABLE");}}
    private static String digest(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(GeneralSecurityException bad){throw new IllegalStateException("ADMIN_SNAPSHOT_UNAVAILABLE");}}
    private static String canonical(Query q){return q.from()+"|"+q.to()+"|"+q.grain()+"|"+Objects.toString(q.shipmentStatus(),"");}
    private String fingerprint(List<AdminShipmentFacts.Row> rows,List<AdminObligationFacts.Row> money){
        var value=new StringBuilder();rows.stream().sorted(Comparator.comparing(AdminShipmentFacts.Row::id)).forEach(r->value.append(r.id()).append('|').append(r.code()).append('|').append(r.ownerKind()).append('|').append(r.bookedAt()).append('|').append(r.status()).append('|').append(r.parcelCount()).append(';'));
        money.stream().sorted(Comparator.comparing(AdminObligationFacts.Row::shipmentId)).forEach(r->value.append(r.shipmentId()).append('|').append(r.amountVnd()).append('|').append(r.state()).append(';'));return digest(value.toString());
    }
    private String snapshot(AdminReadContext a,Query q,Instant cutoff,String fingerprint){
        String payload=String.join("\n","ARGOPOST_ADMIN_SNAPSHOT_1",a.accountId().toString(),a.realmId().toString(),a.permissionVersion(),canonical(q),cutoff.toString(),fingerprint);
        byte[] bytes=payload.getBytes(StandardCharsets.UTF_8);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(key,bytes));
    }
    private record Verified(Instant cutoff,String fingerprint){}
    private Verified verify(AdminReadContext a,Query q,String selector,Instant now){
        a.require(AdminReadContext.ENTRY);
        try{
            if(selector==null||selector.length()>4096||!selector.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]{43}"))throw new IllegalArgumentException();
            var parts=selector.split("\\.");byte[] bytes=Base64.getUrlDecoder().decode(parts[0]),signature=Base64.getUrlDecoder().decode(parts[1]);
            if(!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(parts[0])||!Base64.getUrlEncoder().withoutPadding().encodeToString(signature).equals(parts[1])||!MessageDigest.isEqual(signature,hmac(key,bytes)))throw new IllegalArgumentException();
            String[] fields=new String(bytes,StandardCharsets.UTF_8).split("\n",-1);
            if(fields.length!=7||!fields[0].equals("ARGOPOST_ADMIN_SNAPSHOT_1")||!fields[1].equals(a.accountId().toString())||!fields[2].equals(a.realmId().toString())||!fields[3].equals(a.permissionVersion())||!fields[4].equals(canonical(q)))throw new IllegalArgumentException();
            Instant cutoff=Instant.parse(fields[5]);if(now.isBefore(cutoff)||!now.isBefore(cutoff.plusSeconds(900))||!fields[6].matches("[a-f0-9]{64}"))throw new IllegalArgumentException();return new Verified(cutoff,fields[6]);
        }catch(IllegalArgumentException|DateTimeException malformed){throw new AdminFailure("SNAPSHOT_EXPIRED");}
    }
    @Override public Selector selector(AdminReadContext a,Query q,String snapshot,Instant now){return new Selector(q,verify(a,q,snapshot,now).cutoff());}
    private Map<String,String> filters(Query q){var p=new TreeMap<String,String>();p.put("from",q.from().toString());p.put("to",q.to().toString());p.put("grain",q.grain());if(q.shipmentStatus()!=null)p.put("shipmentStatus",q.shipmentStatus());return p;}
    private Metric metric(AdminReadContext a,Query q,String key,BigInteger number,String unit){return new Metric(key,number.toString(),unit,a.permissions().contains(AdminReadContext.SHIPMENT)?new Drilldown("/api/v1/admin/dashboard/shipments",filters(q),key):null);}
    private BigInteger count(List<AdminShipmentFacts.Row> rows,String metric){return metric.equals("PARCELS")?rows.stream().map(r->BigInteger.valueOf(r.parcelCount())).reduce(BigInteger.ZERO,BigInteger::add):BigInteger.valueOf(rows.stream().filter(r->!metric.equals("WAITING_HANDOVER")&&!metric.equals("ACCEPTED")||r.status().equals(metric.equals("ACCEPTED")?"AT_STATION":"BOOKED")).count());}
    private BigInteger sum(List<AdminObligationFacts.Row> money){return money.stream().map(AdminObligationFacts.Row::amountVnd).reduce(BigInteger.ZERO,BigInteger::add);}
    private List<Point> trend(Query q,List<AdminShipmentFacts.Row> rows,List<AdminObligationFacts.Row> money,String metric){
        List<Point> points=new ArrayList<>();LocalDate end=q.to().plusDays(1),cursor=q.from();
        while(cursor.isBefore(end)){
            LocalDate next=switch(q.grain()){case "WEEK"->cursor.with(TemporalAdjusters.next(DayOfWeek.MONDAY));case "MONTH"->cursor.withDayOfMonth(1).plusMonths(1);default->cursor.plusDays(1);};if(next.isAfter(end))next=end;
            Instant from=cursor.atStartOfDay(ZONE).toInstant(),until=next.atStartOfDay(ZONE).toInstant();var bucket=rows.stream().filter(r->!r.bookedAt().isBefore(from)&&r.bookedAt().isBefore(until)).toList();
            var ids=new HashSet<UUID>();bucket.forEach(r->ids.add(r.id()));BigInteger value=metric.endsWith("FEES")?sum(money.stream().filter(r->ids.contains(r.shipmentId())).toList()):count(bucket,metric);
            points.add(new Point(cursor,next,metric,value.toString()));cursor=next;
        }return List.copyOf(points);
    }
    private void validateCohort(AdminReadContext a,List<AdminShipmentFacts.Row> rows,List<AdminObligationFacts.Row> money){
        if(rows.size()>100000||rows.stream().map(AdminShipmentFacts.Row::id).distinct().count()!=rows.size())throw new AdminFailure("SERVICE_UNAVAILABLE");
        if(a.permissions().contains(AdminReadContext.FINANCE)){
            if(money.size()!=rows.size()||money.stream().map(AdminObligationFacts.Row::shipmentId).distinct().count()!=money.size()||!new HashSet<>(rows.stream().map(AdminShipmentFacts.Row::id).toList()).equals(new HashSet<>(money.stream().map(AdminObligationFacts.Row::shipmentId).toList()))||money.stream().anyMatch(r->r.amountVnd().signum()<=0||!r.state().equals("UNPAID")))throw new AdminFailure("SERVICE_UNAVAILABLE");
        }else if(!money.isEmpty())throw new AdminFailure("ACCESS_DENIED");
    }
    @Override public Dashboard dashboard(AdminReadContext a,Query q,Instant cutoff,List<AdminShipmentFacts.Row> rows,List<AdminObligationFacts.Row> money,Set<String> unavailable){
        a.require(AdminReadContext.ENTRY);validateCohort(a,rows,money);List<Section> sections=new ArrayList<>();
        if(a.permissions().contains(AdminReadContext.MERCHANT)&&!unavailable.contains("MERCHANT")||a.permissions().contains(AdminReadContext.NETWORK)&&!unavailable.contains("NETWORK"))throw new AdminFailure("SERVICE_UNAVAILABLE");
        if(a.permissions().contains(AdminReadContext.MERCHANT))sections.add(new Section("MERCHANT","SOURCE_NOT_IMPLEMENTED","SUBMITTED_APPLICATION_SOURCE_REQUIRED",List.of(),List.of()));
        if(a.permissions().contains(AdminReadContext.NETWORK))sections.add(new Section("NETWORK","SOURCE_NOT_IMPLEMENTED","OFFICIAL_NETWORK_SOURCE_REQUIRED",List.of(),List.of()));
        if(a.permissions().contains(AdminReadContext.SHIPMENT)){
            var metrics=List.of("BOOKINGS","WAITING_HANDOVER","ACCEPTED","PARCELS").stream().map(k->metric(a,q,k,count(rows,k),k.equals("PARCELS")?"PARCEL":"SHIPMENT")).toList();var series=new ArrayList<Point>();for(var m:metrics)series.addAll(trend(q,rows,List.of(),m.key()));
            sections.add(new Section("SHIPMENTS","AVAILABLE","BOOKING_COHORT_CURRENT_SUPPORTED_STATES",metrics,series));
            sections.add(new Section("SLA","POLICY_NOT_CONFIGURED","SLA_AND_ADVANCED_LIFECYCLE_REQUIRED",List.of(),List.of()));
            sections.add(new Section("PRIORITIES","SOURCE_NOT_IMPLEMENTED","APPROVAL_QUEUE_REQUIRED",List.of(),List.of()));
        }
        if(a.permissions().contains(AdminReadContext.FINANCE)){var metrics=List.of(metric(a,q,"CONFIRMED_FEES",sum(money),"VND"),metric(a,q,"UNPAID_FEES",sum(money),"VND"));var series=new ArrayList<Point>();for(var m:metrics)series.addAll(trend(q,rows,money,m.key()));sections.add(new Section("FINANCE","AVAILABLE","CONFIRMED_UNPAID_OBLIGATIONS_NOT_REVENUE",metrics,series));}
        return new Dashboard(q,cutoff,snapshot(a,q,cutoff,fingerprint(rows,money)),sections);
    }
    @Override public Page shipments(AdminReadContext a,Query q,String snapshot,Instant now,List<AdminShipmentFacts.Row> rows,List<AdminObligationFacts.Row> money,String metric,int page,int size){
        a.require(AdminReadContext.SHIPMENT);if(Set.of("CONFIRMED_FEES","UNPAID_FEES").contains(Objects.toString(metric,"")))a.require(AdminReadContext.FINANCE);
        if(!Set.of("BOOKINGS","WAITING_HANDOVER","ACCEPTED","PARCELS","CONFIRMED_FEES","UNPAID_FEES").contains(Objects.toString(metric,""))||page<0||page>10000||size<1||size>50)throw invalid();
        var verified=verify(a,q,snapshot,now);validateCohort(a,rows,money);if(!MessageDigest.isEqual(verified.fingerprint().getBytes(StandardCharsets.US_ASCII),fingerprint(rows,money).getBytes(StandardCharsets.US_ASCII)))throw new AdminFailure("SNAPSHOT_EXPIRED");
        var selected=com.elogistic.backend.notifications.api.AdminMetricCohorts.select(rows,metric);
        long start=(long)page*size;return new Page(selected.stream().skip(start).limit(size).toList(),page,size,start+size<selected.size(),snapshot);
    }
    @Override public QueryV2 queryV2(Map<String,String> parameters,Instant now,boolean page){
        if(parameters==null)throw invalid();var p=new HashMap<>(parameters);String state=p.remove("applicationState"),policy=p.remove("policyId");
        var old=query(p,now,page);UUID id=null;
        try{if(state!=null&&!Set.of("SUBMITTED","UNDER_REVIEW","NEEDS_INFORMATION","APPROVED","REJECTED","WITHDRAWN").contains(state))throw invalid();if(policy!=null){id=UUID.fromString(policy);if(!id.toString().equals(policy)||id.equals(new UUID(0,0)))throw invalid();}}catch(IllegalArgumentException bad){throw invalid();}
        if(old.shipmentStatus()!=null&&(state!=null||id!=null))throw invalid();
        return new QueryV2(old.from(),old.to(),old.grain(),old.shipmentStatus(),old.regionId(),state,id);
    }
    private static String canonicalV2(QueryV2 q){return canonical(q.legacy())+"|"+Objects.toString(q.applicationState(),"")+"|"+Objects.toString(q.policyId(),"");}
    @Override public String snapshotV2(AdminReadContext actor,QueryV2 q,Instant cutoff,String scope,String fingerprint){
        actor.require(AdminReadContext.ENTRY);if(scope==null||!scope.matches("[a-f0-9]{64}")||fingerprint==null||!fingerprint.matches("[a-f0-9]{64}"))throw invalid();
        String payload=String.join("\n","ARGOPOST_ADMIN_SNAPSHOT_2",actor.accountId().toString(),actor.realmId().toString(),actor.permissionVersion(),canonicalV2(q),cutoff.toString(),scope,fingerprint,"ARGO_ADMIN_METRICS_2");
        byte[] bytes=payload.getBytes(StandardCharsets.UTF_8);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(key,bytes));
    }
    @Override public SelectorV2 selectorV2(AdminReadContext actor,QueryV2 q,String token,Instant now){
        actor.require(AdminReadContext.ENTRY);
        try{
            if(token==null||token.length()>4096||!token.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]{43}"))throw new IllegalArgumentException();
            var parts=token.split("\\.");byte[] bytes=Base64.getUrlDecoder().decode(parts[0]),signature=Base64.getUrlDecoder().decode(parts[1]);
            if(!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(parts[0])||!Base64.getUrlEncoder().withoutPadding().encodeToString(signature).equals(parts[1])||!MessageDigest.isEqual(signature,hmac(key,bytes)))throw new IllegalArgumentException();
            String[] fields=new String(bytes,StandardCharsets.UTF_8).split("\n",-1);
            if(fields.length!=9||!fields[0].equals("ARGOPOST_ADMIN_SNAPSHOT_2")||!fields[1].equals(actor.accountId().toString())||!fields[2].equals(actor.realmId().toString())||!fields[3].equals(actor.permissionVersion())||!fields[4].equals(canonicalV2(q))||!fields[8].equals("ARGO_ADMIN_METRICS_2")||!fields[6].matches("[a-f0-9]{64}")||!fields[7].matches("[a-f0-9]{64}"))throw new IllegalArgumentException();
            Instant cutoff=Instant.parse(fields[5]);if(now.isBefore(cutoff)||!now.isBefore(cutoff.plusSeconds(900)))throw new IllegalArgumentException();return new SelectorV2(q,cutoff,fields[6],fields[7]);
        }catch(IllegalArgumentException|DateTimeException malformed){throw new AdminFailure("SNAPSHOT_EXPIRED");}
    }

}
