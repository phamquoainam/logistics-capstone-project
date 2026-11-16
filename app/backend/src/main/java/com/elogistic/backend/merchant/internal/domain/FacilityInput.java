package com.elogistic.backend.merchant.internal.domain;

import com.elogistic.backend.merchant.api.customer.MerchantFacilities.*;
import com.elogistic.backend.merchant.api.customer.PersonalAddresses;
import java.util.*;
import static com.elogistic.backend.merchant.internal.domain.OrganizationContactInput.text;

public final class FacilityInput {
    private FacilityInput(){}
    public static final List<String> DAYS=List.of("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY");
    public static final Set<String> KINDS=Set.of("WAREHOUSE","STORE","PICKUP_ADDRESS","SENDER_POINT");
    public static Contact contact(Contact c){
        if(c==null)throw new IllegalArgumentException();
        var a=Objects.requireNonNull(c.address());
        var v=CustomerAddress.normalize(new PersonalAddresses.Input("SENDER",null,text(c.name(),160),c.phone(),new PersonalAddresses.Location(a.country(),text(a.administrativeArea(),160),text(a.locality(),160),text(a.addressLine(),500)),false));
        return new Contact(v.contactName(),v.contactPhone(),v.address());
    }
    private static int minutes(String s,boolean closing){
        if(s==null||!s.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]")&&!(closing&&s.equals("24:00")))throw new IllegalArgumentException();
        return Integer.parseInt(s.substring(0,2))*60+Integer.parseInt(s.substring(3));
    }
    public static Hours hours(Hours h){
        if(h==null)return null;
        if(!"Asia/Ho_Chi_Minh".equals(h.timeZone())||h.days()==null||h.days().size()!=7)throw new IllegalArgumentException();
        Map<String,Day> days=new HashMap<>();
        for(var d:h.days()){
            if(d==null||!DAYS.contains(d.day())||days.put(d.day(),d)!=null||d.intervals()==null||d.intervals().size()>2)throw new IllegalArgumentException();
            int previous=0;
            for(var i:d.intervals()){
                if(i==null)throw new IllegalArgumentException();int start=minutes(i.opensAt(),false),end=minutes(i.closesAt(),true);
                if(start>=end||start<previous)throw new IllegalArgumentException();previous=end;
            }
        }
        return new Hours(h.timeZone(),DAYS.stream().map(days::get).toList());
    }
    public static Input normalize(Input v){
        if(v==null||!KINDS.contains(v.kind()))throw new IllegalArgumentException();
        String code=text(v.code(),40).toUpperCase(Locale.ROOT);if(!code.matches("[A-Z0-9_-]{1,40}"))throw new IllegalArgumentException();
        return new Input(text(v.name(),160),code,v.kind(),contact(v.contact()),v.senderContact()==null?null:contact(v.senderContact()),v.returnContact()==null?null:contact(v.returnContact()),hours(v.operatingHours()));
    }
}
