package com.elogistic.backend.merchant.internal.domain;

import com.elogistic.backend.merchant.api.customer.PersonalAddresses.*;
import java.time.Instant;
import java.util.*;

public record CustomerAddress(UUID id,UUID accountId,Input input,long version,Instant createdAt,Instant updatedAt,Instant archivedAt) {
    public CustomerAddress {Objects.requireNonNull(id);Objects.requireNonNull(accountId);input=normalize(input);Objects.requireNonNull(createdAt);Objects.requireNonNull(updatedAt);if(version<1 || updatedAt.isBefore(createdAt) || archivedAt!=null && (input.isDefault() || !archivedAt.equals(updatedAt)))throw new IllegalArgumentException("INVALID_ADDRESS_STATE");}
    public static Input normalize(Input input){
        Objects.requireNonNull(input);if(!Set.of("SENDER","RECIPIENT").contains(input.kind()))throw new IllegalArgumentException();
        String label=input.label()==null?null:text(input.label(),80),name=text(input.contactName(),160),phone=text(input.contactPhone(),32).replaceAll("[ ()-]","");
        if(phone.matches("0[35789][0-9]{8}"))phone="+84"+phone.substring(1);
        else if(phone.matches("84[35789][0-9]{8}"))phone="+"+phone;
        if(!phone.matches("\\+84[35789][0-9]{8}"))throw new IllegalArgumentException();
        Location a=Objects.requireNonNull(input.address());if(!"VN".equals(a.country()))throw new IllegalArgumentException();
        return new Input(input.kind(),label,name,phone,new Location("VN",text(a.administrativeArea(),160),text(a.locality(),160),text(a.addressLine(),500)),input.isDefault());
    }
    private static String text(String value,int max){Objects.requireNonNull(value);if(value.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException();String result=value.strip();if(result.isEmpty() || result.codePointCount(0,result.length())>max)throw new IllegalArgumentException();return result;}
    public CustomerAddress replace(Input value,Instant now){if(archivedAt!=null)throw new IllegalArgumentException();return new CustomerAddress(id,accountId,value,Math.addExact(version,1),createdAt,now,null);}
    public CustomerAddress withoutDefault(Instant now){return replace(new Input(input.kind(),input.label(),input.contactName(),input.contactPhone(),input.address(),false),now);}
    public CustomerAddress archive(Instant now){return new CustomerAddress(id,accountId,withoutDefault(now).input(),Math.addExact(version,1),createdAt,now,now);}
    public Address view(){return new Address(id,accountId,input,version,createdAt,updatedAt);}
    @Override public String toString(){return "CustomerAddress[redacted]";}
}
