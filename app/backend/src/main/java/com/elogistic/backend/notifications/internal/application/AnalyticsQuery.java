package com.elogistic.backend.notifications.internal.application;

import com.elogistic.backend.notifications.api.AnalyticsFailure;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import static com.elogistic.backend.notifications.api.AnalyticsFailure.Code.VALIDATION_FAILED;

record AnalyticsQuery(LocalDate from,LocalDate until,boolean compare,String bucket,String status,String service,
                      String origin,String destination,LocalDate day,String obligation,String weight,boolean multi,String search,String sort,int page) {
    static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    static final Set<String> KEYS=Set.of("from","until","compare","bucket","status","service","origin","destination","day","obligation","weight","multi","search","sort","page");
    static AnalyticsQuery parse(Map<String,String> p,Instant now){
        try{
            if(!KEYS.containsAll(p.keySet())||p.values().stream().anyMatch(Objects::isNull)||p.containsKey("from")!=p.containsKey("until"))throw invalid();
            var today=now.atZone(ZONE).toLocalDate();
            LocalDate until=p.containsKey("until")?date(p.get("until")):today.plusDays(1),from=p.containsKey("from")?date(p.get("from")):until.minusDays(30);
            long days=ChronoUnit.DAYS.between(from,until);
            if(days<1||days>366||until.isAfter(today.plusDays(1))||from.isAfter(today))throw invalid();
            String compare=option(p,"compare","previous",Set.of("previous","none")),bucket=option(p,"bucket","day",Set.of("day","week","month"));
            String status=option(p,"status","",Set.of("","WAITING_FOR_HANDOVER","AT_STATION")),service=option(p,"service","",Set.of("","ROAD_STANDARD"));
            String origin=p.getOrDefault("origin",""),destination=p.getOrDefault("destination","");
            if(!origin.isEmpty()||!destination.isEmpty()){
                if(!Set.of("SIM-A1/SIM-A2","SIM-A2/SIM-A1","SIM-A1/SIM-B1","SIM-B1/SIM-A1").contains(origin+"/"+destination))throw invalid();
            }
            LocalDate day=p.containsKey("day")?date(p.get("day")):null;
            if(day!=null&&(day.isBefore(from)||!day.isBefore(until)))throw invalid();
            String obligation=option(p,"obligation","",Set.of("","UNPAID")),weight=option(p,"weight","",Set.of("","UP_TO_500G","UP_TO_5KG","OVER_5KG"));
            String multi=option(p,"multi","",Set.of("","true")),search=p.getOrDefault("search","");
            if(search.length()>64||search.codePoints().anyMatch(Character::isISOControl))throw invalid();
            String sort=option(p,"sort","NEWEST",Set.of("NEWEST","OLDEST","FEE_DESC","FEE_ASC")),page=p.getOrDefault("page","0");
            if(!page.matches("0|[1-9][0-9]{0,2}")||Integer.parseInt(page)>500)throw invalid();
            return new AnalyticsQuery(from,until,compare.equals("previous"),bucket,status,service,origin,destination,day,obligation,weight,multi.equals("true"),search.strip().toLowerCase(Locale.ROOT),sort,Integer.parseInt(page));
        }catch(DateTimeException|NumberFormatException malformed){throw invalid();}
    }
    private static LocalDate date(String v){if(!v.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw invalid();return LocalDate.parse(v);}
    private static String option(Map<String,String> p,String key,String fallback,Set<String> allowed){String v=p.getOrDefault(key,fallback);if(!allowed.contains(v))throw invalid();return v;}
    static AnalyticsFailure invalid(){return new AnalyticsFailure(VALIDATION_FAILED);}
    long days(){return ChronoUnit.DAYS.between(from,until);}
    Instant start(){return from.atStartOfDay(ZONE).toInstant();}
    Instant end(Instant now){Instant limit=until.atStartOfDay(ZONE).toInstant();Instant throughNow=now.plusNanos(1000);return throughNow.isBefore(limit)?throughNow:limit;}
    LocalDate group(LocalDate date){return switch(bucket){case "week"->date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));case "month"->date.withDayOfMonth(1);default->date;};}
    boolean selectedDay(LocalDate date,boolean previous){
        if(day==null)return true;
        LocalDate selected=previous?day.minusDays(days()):day;
        // The point represents a displayed time bucket, clipped by the global cohort interval.
        if(previous){LocalDate shifted=date.plusDays(days());return group(shifted).equals(group(day));}
        return group(date).equals(group(selected));
    }
    boolean weightMatches(long grams){return weight.isEmpty()||weightKey(grams).equals(weight);}
    static String weightKey(long grams){return grams<=500?"UP_TO_500G":grams<=5000?"UP_TO_5KG":"OVER_5KG";}
}
