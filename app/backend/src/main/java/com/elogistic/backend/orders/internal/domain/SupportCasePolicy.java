package com.elogistic.backend.orders.internal.domain;

import java.text.Normalizer;
import java.util.Set;

/** Pure policy shared by the Personal and Organization cases; no persistence or authority. */
public final class SupportCasePolicy {
    private SupportCasePolicy() {}
    public static final Set<String> STATES=Set.of("NEW","IN_PROGRESS","NEEDS_INFORMATION","RESOLVED","CLOSED");
    public static final Set<String> PERSONAL_CATEGORIES=Set.of("ACCOUNT_HELP","SHIPMENT_TRACE","FEE_REVIEW","OTHER");
    public static final Set<String> ORGANIZATION_CATEGORIES=Set.of("ACCOUNT_HELP","SHIPMENT_TRACE","DAMAGE_REPORT","LOSS_REPORT","FEE_REVIEW","OTHER");
    public static final Set<String> PRIORITIES=Set.of("LOW","NORMAL","HIGH");
    public static String text(String value,int max,boolean nfc){
        if(value==null)throw new IllegalArgumentException("VALIDATION_FAILED");
        String result=value.replace("\r\n","\n").strip();
        if(nfc)result=Normalizer.normalize(result,Normalizer.Form.NFC);
        if(result.isEmpty()||result.codePointCount(0,result.length())>max||result.codePoints().anyMatch(c->Character.isISOControl(c)&&!(max==4000&&(c==10||c==9))||c==127))throw new IllegalArgumentException("VALIDATION_FAILED");
        return result;
    }
    public static boolean customerReply(String state){return Set.of("NEW","IN_PROGRESS","NEEDS_INFORMATION").contains(state);}
    public static String transition(String state,boolean review,String action){
        return switch(action){
            case "START" -> state.equals("NEW")?"IN_PROGRESS":null;
            case "REQUEST_INFORMATION" -> state.equals("IN_PROGRESS")?"NEEDS_INFORMATION":null;
            case "RESOLVE" -> state.equals("IN_PROGRESS")?"RESOLVED":null;
            case "REOPEN" -> state.equals("RESOLVED")&&review?"IN_PROGRESS":null;
            default -> null;
        };
    }
}
