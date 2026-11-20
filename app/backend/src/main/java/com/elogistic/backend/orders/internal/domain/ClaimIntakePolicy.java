package com.elogistic.backend.orders.internal.domain;

import java.util.Set;

/** Document intake only. Allegation/category is never a Carrier fact or compensation decision. */
public final class ClaimIntakePolicy {
    public static final Set<String> CATEGORIES=Set.of("LOSS","DAMAGE","DELAY","SERVICE_DISPUTE");
    public static final Set<String> STATES=Set.of("RECEIVED","UNDER_REVIEW","NEEDS_INFORMATION","CLOSED_WITHOUT_DECISION");
    private ClaimIntakePolicy(){}
    public static String category(String value){if(!CATEGORIES.contains(value))throw invalid();return value;}
    public static String narrative(String value){if(value==null)throw invalid();return SupportCasePolicy.text(value,4000,false);}
    public static String supplement(String state){
        if(!STATES.contains(state)||state.equals("CLOSED_WITHOUT_DECISION"))throw invalid();
        return state.equals("NEEDS_INFORMATION")?"UNDER_REVIEW":state;
    }
    public static String staffTransition(String state,String action){
        if(!STATES.contains(state)||state.equals("CLOSED_WITHOUT_DECISION")||action==null)throw invalid();
        return switch(action){
            case "BEGIN_REVIEW"->{if(!state.equals("RECEIVED"))throw invalid();yield "UNDER_REVIEW";}
            case "REQUEST_INFORMATION"->{if(!Set.of("RECEIVED","UNDER_REVIEW").contains(state))throw invalid();yield "NEEDS_INFORMATION";}
            case "CLOSE_WITHOUT_DECISION"->"CLOSED_WITHOUT_DECISION";
            default->throw invalid();
        };
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("CLAIM_INTAKE_STATE_OR_INPUT_INVALID");}
}
