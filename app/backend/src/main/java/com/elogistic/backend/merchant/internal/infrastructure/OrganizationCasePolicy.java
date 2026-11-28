package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.merchant.api.GovernanceFailure;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Exact Owner Policy V1 validation. No publication, grants or retention defaults. */
final class OrganizationCasePolicy {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final String V1="{\"schema_version\":\"ORG_GOVERNANCE_1\",\"environment\":\"TEST\",\"allowed_case_types\":[\"RESTRICT\",\"RELEASE_RESTRICTION\",\"CLOSE\",\"RISK_HOLD\",\"RELEASE_HOLD\",\"DISPUTE\",\"REVERIFY\"],\"capability_policy\":{\"RESTRICTED\":{\"readSupportAllowlist\":[\"HISTORY\",\"TRACKING\",\"SUPPORT\",\"EXISTING_SHIPMENT_HANDLING\",\"EXISTING_FINANCIAL_OBLIGATIONS\"],\"deniedBusinessOperations\":[\"QUOTE_NEW\",\"BOOKING_CONFIRM\",\"TEAM_DELEGATION\",\"CONTRACT_CHANGE\"]},\"CLOSED\":{\"retainedReadSupportAllowlist\":[\"HISTORY\",\"SUPPORT\",\"EXISTING_FINANCIAL_OBLIGATIONS\"],\"terminal\":true},\"hold_effects\":{\"BOOKING_HOLD\":[\"QUOTE_NEW\",\"BOOKING_CONFIRM\"],\"CONTRACT_CHANGE_HOLD\":[\"CONTRACT_CHANGE\"]}},\"separation\":{\"maker_reviewer_distinct\":true,\"reviewer_decider_distinct\":true,\"maker_decider_distinct\":true,\"executor_may_equal_decider\":true},\"case_ttl_seconds\":604800,\"consent_ttl_seconds\":86400,\"max_future_effect_seconds\":0,\"required_checklist_codes\":[\"authority\",\"conflictClear\",\"evidenceComplete\",\"impactReviewed\"],\"close_requires\":[\"ORDERS_CLEAR\",\"FINANCE_CLEAR\"],\"transfer_eligibility\":{\"organizationState\":\"ACTIVE\",\"admissionOrigin\":\"APPLICATION_APPROVED\",\"sameOrganization\":true,\"verifiedParties\":true,\"activeNonOwnerRecipient\":true,\"openOwnershipDispute\":false,\"blockingMaterialReverification\":false,\"ordinaryUnpaidBlocks\":false},\"post_transfer_grants\":{\"outgoingRole\":\"OPERATOR\",\"outgoingState\":\"ACTIVE\",\"outgoingPermissions\":[\"dashboard.read\"],\"incomingPermissions\":[\"dashboard.read\",\"ownership.transfer.request\"]},\"retention_policy_reference\":null}";
    private final JsonNode body;
    private OrganizationCasePolicy(JsonNode body){this.body=body;}
    static String ownerV1(String environment){if(!Set.of("LOCAL","TEST").contains(environment))throw invalid();return V1.replace("\"environment\":\"TEST\"","\"environment\":\""+environment+"\"");}
    static OrganizationCasePolicy parse(String text){
        try{JsonNode value=JSON.readTree(text);if(value==null||!value.isObject()||!Set.of("LOCAL","TEST").contains(value.path("environment").asString())||!value.equals(JSON.readTree(ownerV1(value.path("environment").asString()))))throw invalid();return new OrganizationCasePolicy(value);}
        catch(GovernanceFailure known){throw known;}catch(RuntimeException malformed){throw invalid();}
    }
    private static GovernanceFailure invalid(){return new GovernanceFailure(GovernanceFailure.Code.POLICY_UNAVAILABLE);}
    long caseTtl(){return body.get("case_ttl_seconds").asLong();}
    long consentTtl(){return body.get("consent_ttl_seconds").asLong();}
    long maxFuture(){return body.get("max_future_effect_seconds").asLong();}
    Set<String> types(){Set<String> result=new TreeSet<>();for(var v:body.get("allowed_case_types"))result.add(v.asString());return Set.copyOf(result);}
    boolean evidenceReady(){return false;} // No Organization-case retention reference has been approved.
}
