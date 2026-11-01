package com.elogistic.backend.orders.internal.domain;
import com.elogistic.backend.orders.api.ShippingDrafts.*;
import java.util.*;

/** Preparation syntax only; no tariff, service eligibility or operational transition. */
public final class ShippingPreparation {
    private ShippingPreparation(){}
    private static String text(String value,int max,boolean optional){if(value==null){if(optional)return null;throw new IllegalArgumentException();}String s=value.strip();if(s.isEmpty()||s.codePointCount(0,s.length())>max||s.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException();return s;}
    private static long range(long value,long min,long max){if(value<min||value>max)throw new IllegalArgumentException();return value;}
    private static Contact contact(Contact contact){
        if(contact==null||contact.address()==null)throw new IllegalArgumentException();var a=contact.address();String phone=text(contact.phone(),32,false).replaceAll("[ ()-]","");if(phone.matches("0[35789][0-9]{8}"))phone="+84"+phone.substring(1);else if(phone.matches("84[35789][0-9]{8}"))phone="+"+phone;
        if(!phone.matches("\\+84[35789][0-9]{8}")||!"VN".equals(a.country()))throw new IllegalArgumentException();return new Contact(text(contact.name(),160,false),phone,new Address("VN",text(a.administrativeArea(),160,false),text(a.locality(),160,false),text(a.addressLine(),500,false)));
    }
    public static Input normalize(Input value){
        if(value==null||value.parcels()==null||value.parcels().isEmpty()||value.parcels().size()>5)throw new IllegalArgumentException();List<Parcel> parcels=new ArrayList<>();
        for(var p:value.parcels()){if(p==null)throw new IllegalArgumentException();parcels.add(new Parcel(text(p.description(),160,false),range(p.quantity(),1,1000000),range(p.weightGrams(),1,1000000000),range(p.lengthMm(),1,1000000),range(p.widthMm(),1,1000000),range(p.heightMm(),1,1000000),range(p.declaredValueVnd(),0,1000000000000L)));}
        return new Input(text(value.reference(),80,true),text(value.instructions(),1000,true),contact(value.sender()),contact(value.recipient()),parcels);
    }
}
