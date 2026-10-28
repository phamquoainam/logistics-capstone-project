package com.elogistic.backend.merchant.internal.domain;

import com.elogistic.backend.merchant.api.customer.MerchantApplications.*;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;

/** Partial policy-shaped draft, never a submitted revision or a verified business representation. */
public record MerchantApplicationDraft(UUID id,UUID applicantAccountId,Input input,long version,Instant createdAt,Instant updatedAt) {
    public MerchantApplicationDraft {
        Objects.requireNonNull(id);Objects.requireNonNull(applicantAccountId);Objects.requireNonNull(input);
        Objects.requireNonNull(createdAt);Objects.requireNonNull(updatedAt);
        if(version<1 || updatedAt.isBefore(createdAt))throw new IllegalArgumentException("APPLICATION_VERSION_INVALID");
        validate(input);
    }
    public static void validate(Input input){
        Objects.requireNonNull(input);
        if(input.entityType()!=null && !Set.of("COMPANY","HOUSEHOLD_BUSINESS").contains(input.entityType()))invalid();
        text(input.jurisdiction(),64);text(input.registeredName(),240);text(input.legalIdentifier(),80);
        text(input.representativeName(),160);text(input.representativeRelationship(),160);
        if(input.representativeEmail()!=null){text(input.representativeEmail(),254);if(!input.representativeEmail().matches("[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+@[A-Za-z0-9.-]+") || input.representativeEmail().contains("..") || input.representativeEmail().startsWith(".") || input.representativeEmail().contains(".@"))invalid();}
        address(input.registeredAddress());address(input.operatingAddress());
        if(input.evidenceIds()!=null && (input.evidenceIds().size()>5 || new HashSet<>(input.evidenceIds()).size()!=input.evidenceIds().size()))invalid();
        if(input.declarations()!=null){if(!input.declarations().accuracy() || !input.declarations().representation() || input.declarations().consentVersion()==null)invalid();text(input.declarations().consentVersion(),64);}
        if(input.legalIdentifier()!=null && legalKey(input.legalIdentifier()).isEmpty())invalid();
    }
    private static void address(Address a){if(a==null)return;if(a.country()==null || !a.country().matches("[A-Z]{2}") || a.administrativeArea()==null || a.locality()==null || a.addressLine()==null)invalid();text(a.administrativeArea(),160);text(a.locality(),160);text(a.addressLine(),500);}
    private static void text(String value,int max){if(value!=null && (value.isBlank() || value.codePointCount(0,value.length())>max || value.codePoints().anyMatch(c->Character.isISOControl(c) || c==0x2028 || c==0x2029 || c>=0xd800 && c<=0xdfff)))invalid();}
    private static void invalid(){throw new IllegalArgumentException("APPLICATION_INPUT_INVALID");}
    // Sandbox S003-2026-10-27: NFC, ASCII outer spaces; legal ID removes ASCII space,-,.,/,_ only.
    public static String jurisdictionKey(String value){return Normalizer.normalize(value.replaceAll("^ +| +$",""),Normalizer.Form.NFC);}
    public static String legalKey(String value){return Normalizer.normalize(value.replace(" ","").replace("-","").replace(".","").replace("/","").replace("_",""),Normalizer.Form.NFC);}
    public static String businessKey(Input input){
        if(input.entityType()==null || input.jurisdiction()==null || input.legalIdentifier()==null)return null;
        String region=jurisdictionKey(input.jurisdiction()),legal=legalKey(input.legalIdentifier());
        return region.length()+":"+region+input.entityType().length()+":"+input.entityType()+legal.length()+":"+legal;
    }
    public MerchantApplicationDraft replace(Input next,Instant now){if(version==Long.MAX_VALUE)invalid();return new MerchantApplicationDraft(id,applicantAccountId,next,version+1,createdAt,now);}
    public Application view(){return new Application(id,applicantAccountId,"DRAFT",1,version,input);}
    @Override public String toString(){return "MerchantApplicationDraft[redacted]";}
}
