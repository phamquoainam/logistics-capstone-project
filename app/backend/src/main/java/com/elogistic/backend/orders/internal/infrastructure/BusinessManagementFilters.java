package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.BusinessPreparations;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Exact accepted query grammar; values never become SQL identifiers or expressions. */
public record BusinessManagementFilters(Map<String,String> parameters,int page,String search,Instant from,Instant until,
        String status,String sort,String origin,String destination,UUID facility,UUID creator,String assignee,
        String sourceChannel,UUID batch,List<UUID> selected,boolean includePii) {
    private static final Set<String> COMMON=Set.of("page","size","search","from","until","status","service","origin","destination","obligation","handover","facilityId","creatorId","assignee","sourceChannel","batchId","sort");
    private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    public BusinessManagementFilters{parameters=Map.copyOf(parameters);selected=List.copyOf(selected);}
    private static BusinessPreparations.Failure invalid(){return new BusinessPreparations.Failure("VALIDATION_FAILED");}
    public static UUID uuid(String value){try{UUID id=UUID.fromString(value);if(!id.toString().equals(value)||id.equals(new UUID(0,0)))throw invalid();return id;}catch(IllegalArgumentException|NullPointerException bad){throw invalid();}}
    private static UUID optional(Map<String,String> p,String field){return p.containsKey(field)?uuid(p.get(field)):null;}
    private static String text(Map<String,String> p,String field,int max){String value=p.get(field);if(value!=null&&(value.isBlank()||value.length()>max||value.codePoints().anyMatch(Character::isISOControl)))throw invalid();return value;}
    private static String option(Map<String,String> p,String field,String fallback,Set<String> choices){String value=p.getOrDefault(field,fallback);if(!choices.contains(value))throw invalid();return value;}
    public static BusinessManagementFilters parse(Map<String,String> p,Instant now,boolean export){
        if(p==null||now==null||p.values().stream().anyMatch(Objects::isNull))throw invalid();
        Set<String> allowed=new HashSet<>(COMMON);allowed.add(export?"ids":"view");if(export)allowed.add("includePii");
        if(!allowed.containsAll(p.keySet())||(!export&&!"management".equals(p.get("view"))))throw invalid();
        int page;try{String raw=p.getOrDefault("page","0");if(!raw.matches("0|[1-9][0-9]{0,9}")||!"20".equals(p.getOrDefault("size","20")))throw invalid();page=Integer.parseInt(raw);}catch(NumberFormatException bad){throw invalid();}
        if(export&&(p.containsKey("page")||p.containsKey("size")))throw invalid();
        String status=option(p,"status","",Set.of("","WAITING_FOR_HANDOVER")),sort=option(p,"sort","NEWEST",Set.of("NEWEST","OLDEST","FEE_ASC","FEE_DESC"));
        option(p,"service","",Set.of("","ROAD_STANDARD"));option(p,"obligation","",Set.of("","UNPAID"));option(p,"handover","",Set.of("","DROP_OFF"));
        String origin=p.get("origin"),destination=p.get("destination");if((origin==null)!=(destination==null)||origin!=null&&!Set.of("SIM-A1/SIM-A2","SIM-A2/SIM-A1","SIM-A1/SIM-B1","SIM-B1/SIM-A1").contains(origin+"/"+destination))throw invalid();
        Instant from=null,until=now.plusNanos(1000);if(p.containsKey("from")!=p.containsKey("until"))throw invalid();
        if(p.containsKey("from"))try{String first=p.get("from"),last=p.get("until");if(!first.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")||!last.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw invalid();LocalDate a=LocalDate.parse(first),b=LocalDate.parse(last);long days=ChronoUnit.DAYS.between(a,b);if(days<1||days>366||b.isAfter(now.atZone(ZONE).toLocalDate().plusDays(1)))throw invalid();from=a.atStartOfDay(ZONE).toInstant();Instant end=b.atStartOfDay(ZONE).toInstant();if(end.isBefore(until))until=end;}catch(DateTimeException bad){throw invalid();}
        String assignee=p.get("assignee");if(assignee!=null&&!assignee.equals("NONE"))uuid(assignee);
        List<UUID> ids=new ArrayList<>();if(p.containsKey("ids")){String[] values=p.get("ids").split(",",-1);if(values.length<1||values.length>20)throw invalid();for(String value:values)ids.add(uuid(value));if(new HashSet<>(ids).size()!=ids.size())throw invalid();}
        boolean pii="true".equals(option(p,"includePii","false",Set.of("false","true")));
        return new BusinessManagementFilters(new TreeMap<>(p),page,text(p,"search",160),from,until,status,sort,origin,destination,optional(p,"facilityId"),optional(p,"creatorId"),assignee,text(p,"sourceChannel",40),optional(p,"batchId"),ids,pii);
    }
    public static String like(String value){return "%"+value.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";}
    public String order(){return switch(sort){case "OLDEST"->"s.created_at ASC,s.id ASC";case "FEE_ASC"->"s.total_vnd ASC,s.id ASC";case "FEE_DESC"->"s.total_vnd DESC,s.id ASC";default->"s.created_at DESC,s.id ASC";};}
}
