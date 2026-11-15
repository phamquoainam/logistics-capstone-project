package com.elogistic.backend.merchant.internal.domain;

import com.elogistic.backend.merchant.api.customer.OrganizationContacts.*;
import com.elogistic.backend.merchant.api.customer.PersonalAddresses;
import java.text.Normalizer;

/** Reuses accepted Personal phone/location validation, without changing Personal behavior. */
public final class OrganizationContactInput {
    private OrganizationContactInput(){}
    public static String text(String value,int max){if(value==null||value.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException();String result=Normalizer.normalize(value.strip(),Normalizer.Form.NFC);if(result.isEmpty()||result.codePointCount(0,result.length())>max)throw new IllegalArgumentException();return result;}
    private static String optional(String value){return value==null?null:text(value,80);}
    public static Input normalize(Input value){
        if(value==null)throw new IllegalArgumentException();var a=value.address();if(a==null)throw new IllegalArgumentException();
        var normalized=CustomerAddress.normalize(new PersonalAddresses.Input(value.kind(),optional(value.label()),text(value.contactName(),160),value.contactPhone(),new PersonalAddresses.Location(a.country(),text(a.administrativeArea(),160),text(a.locality(),160),text(a.addressLine(),500)),false));
        return new Input(value.kind(),value.facilityId(),normalized.label(),optional(value.reference()),optional(value.group()),normalized.contactName(),normalized.contactPhone(),normalized.address());
    }
}
