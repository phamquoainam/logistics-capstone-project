package com.elogistic.backend.merchant.api;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.merchant.api.MerchantGovernanceViews.PageQuery;
import java.time.Instant;
import java.util.*;

/** M2-PORT-03 Step A only. No Customer transfer, decision or execution authority. */
public interface OrganizationGovernanceCases {
    sealed interface CasePayload permits Restriction,ReleaseRestriction,Closure,RiskHold,ReleaseHold,Dispute,Reverification {}
    record Restriction(List<String> deniedCapabilities,Instant effectiveAt,Instant expiresAt) implements CasePayload {}
    record ReleaseRestriction(UUID restrictionCaseId) implements CasePayload {}
    record Closure() implements CasePayload {}
    record RiskHold(String holdCode,Instant expiresAt) implements CasePayload {}
    record ReleaseHold(UUID holdId) implements CasePayload {}
    record Dispute(String category,String statement) implements CasePayload {@Override public String toString(){return "Dispute[redacted]";}}
    record Reverification(List<String> fieldCodes,Instant requestedBy) implements CasePayload {}
    record CaseInput(String type,String reason,UUID policyId,long policyVersion,long expectedOrganizationVersion,CasePayload payload){@Override public String toString(){return "CaseInput[redacted]";}}
    record Case(UUID id,UUID organizationId,String type,String state,long version,UUID policyId,long policyVersion,long organizationVersion,String makerActorKind,UUID makerAccountId,UUID reviewerAccountId,UUID deciderAccountId,UUID executorAccountId,Instant createdAt,Instant expiresAt,Instant effectiveAt,List<String> allowedActions,UUID lastEventId,boolean isExpired,Instant asOf){public Case{allowedActions=List.copyOf(allowedActions);}}
    record CasePage(List<Case> items,String nextCursor){public CasePage{items=List.copyOf(items);}}
    record CaseQuery(String type,String state,boolean assignedToMe,PageQuery page){}
    sealed interface CaseCommand permits AssignmentInput,CaseReviewInput,InformationInput,ResponseInput,ReasonAction {}
    record AssignmentInput(UUID reviewerAccountId,String reason) implements CaseCommand {}
    record Checklist(boolean authority,boolean conflictClear,boolean evidenceComplete,boolean impactReviewed){}
    record CaseReviewInput(String action,Checklist checklist,String reason) implements CaseCommand {}
    record InformationInput(List<String> fieldCodes,String reason) implements CaseCommand {}
    record ResponseInput(CasePayload casePayload,List<UUID> evidenceIds,String reason,UUID reauthenticationProofId) implements CaseCommand {@Override public String toString(){return "CaseResponse[redacted]";}}
    record ReasonInput(String reason){}
    enum ReasonTag { SUBMIT,CANCEL }
    record ReasonAction(ReasonTag action,ReasonInput input) implements CaseCommand {}
    record CaseEvent(UUID id,UUID caseId,UUID orgId,long revision,String actorKind,UUID actorId,String operation,String fromState,String toState,long priorVersion,long newVersion,long policyVersion,String reasonCode,Instant occurredAt,UUID requestId){}
    record CaseEventPage(List<CaseEvent> items,String nextCursor){public CaseEventPage{items=List.copyOf(items);}}
    record Policy(UUID policyId,String code,long version,List<String> allowedCaseTypes,Instant validFrom,Instant validTo,Map<String,Object> limits,List<String> requiredChecklists,Long proofAge,long partyConsentTTL,Map<String,Object> readPreservation){}
    record CasePolicies(List<Policy> items){public CasePolicies{items=List.copyOf(items);}}
    record CaseDisclosureInput(long expectedCaseVersion,String purpose){}
    record CaseDisclosure(UUID caseId,UUID organizationId,long version,long revision,CasePayload casePayload,String reason,List<String> requestedFieldCodes,UUID sourceEventId,Instant disclosedAt){
        public CaseDisclosure{requestedFieldCodes=List.copyOf(requestedFieldCodes);}
        @Override public String toString(){return "CaseDisclosure[redacted]";}
    }
    CasePage list(AdminReadContext actor,UUID organizationId,CaseQuery query);
    Case get(AdminReadContext actor,UUID organizationId,UUID caseId);
    AdminCommandAdmission.ReferencedSubjects subjects(AdminReadContext actor,UUID organizationId,UUID caseId);
    Case create(AdminCommandContext actor,UUID organizationId,CaseInput input,UUID key);
    Case command(AdminCommandContext actor,UUID organizationId,UUID caseId,CaseCommand input,long expectedVersion,UUID key);
    CaseEventPage events(AdminReadContext actor,UUID organizationId,UUID caseId,PageQuery query);
    CasePolicies policies(AdminReadContext actor,UUID organizationId);
    CaseDisclosure disclose(AdminCommandContext actor,UUID organizationId,UUID caseId,CaseDisclosureInput input);
}
