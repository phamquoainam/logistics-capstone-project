package com.elogistic.backend.merchant.api;

import com.elogistic.backend.identity.api.admin.AdminCommandContext;
import com.elogistic.backend.identity.api.admin.AdminReadContext;
import com.elogistic.backend.identity.api.admin.AdminCommandAdmission;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** P0-PORT-04: domain scope is additional to, never a replacement for, Identity admission. */
public interface MerchantGovernance {
    <T>T withScope(AdminReadContext actor,Target target,Function<GovernanceScope,T> work);
    <T>T withScope(AdminCommandContext actor,Target target,Function<GovernanceScope,T> work);
    /** Server-only reference discovery before the Identity-owned command lock set; not an account directory. */
    default AdminCommandAdmission.ReferencedSubjects subjects(AdminReadContext actor,UUID application,UUID candidate){throw unavailable();}
    default ApplicationSummary application(AdminReadContext actor,UUID id){throw unavailable();}
    default ReviewData reviewData(AdminCommandContext actor,UUID id,String purpose){throw unavailable();}
    default GovernanceContext context(AdminReadContext actor){throw unavailable();}
    default ApplicationSummaryPage applications(AdminReadContext actor,ApplicationQuery query){throw unavailable();}
    default ApplicationRevisionPage revisions(AdminReadContext actor,UUID id,int page,int size){throw unavailable();}
    default List<UUID> reviewerCandidateIds(AdminReadContext actor,UUID policy,UUID after){throw unavailable();}
    default List<ReviewerCandidate> eligibleReviewers(AdminCommandContext actor,UUID policy,List<UUID> candidates){throw unavailable();}
    default ApplicationSummary assign(AdminCommandContext actor,UUID id,ReviewerAssignmentInput input,long expectedVersion,UUID key){throw unavailable();}
    default ApplicationSummary review(AdminCommandContext actor,UUID id,ReviewInput input,long expectedVersion,UUID key){throw unavailable();}
    default ApplicationSummary requestInformation(AdminCommandContext actor,UUID id,InformationRequest input,long expectedVersion,UUID key){throw unavailable();}
    default ApplicationSummary approve(AdminCommandContext actor,UUID id,DecisionInput input,long expectedVersion,UUID key){throw unavailable();}
    default ApplicationSummary reject(AdminCommandContext actor,UUID id,DecisionInput input,long expectedVersion,UUID key){throw unavailable();}
    default AuditPage events(AdminReadContext actor,UUID application,int page,int size){throw unavailable();}
    default AuditPage audit(AdminReadContext actor,AuditQuery query){throw unavailable();}
    default CommandReceipt receipt(AdminReadContext actor,String operation,UUID key){throw unavailable();}
    default EvidenceContent evidenceContent(UUID application,UUID evidence,String purpose,UUID request){throw unavailable();}
    default OrganizationSummaryPage organizations(AdminReadContext actor,OrganizationQuery query){throw unavailable();}
    default OrganizationSummary organization(AdminReadContext actor,UUID id){throw unavailable();}
    default AdminCommandAdmission.ReferencedSubjects organizationSubjects(AdminReadContext actor,UUID id){throw unavailable();}
    default OrganizationSummary suspend(AdminCommandContext actor,UUID id,LifecycleInput input,long expectedVersion,UUID key){throw unavailable();}
    default OrganizationSummary reinstate(AdminCommandContext actor,UUID id,LifecycleInput input,long expectedVersion,UUID key){throw unavailable();}
    /** P0-READ-03: reader owns genuine admissions and filters current eligibility before paging. */
    default WorkPage work(UUID requestId,WorkQuery query){throw unavailable();}
    record WorkQuery(String kind,UUID policyId,int page,int size){}
    record WorkItem(UUID id,String kind,UUID applicationId,long revision,long assignmentVersion,Instant createdAt,Instant dueAt,String targetPath,List<String> allowedActions){public WorkItem{allowedActions=List.copyOf(allowedActions);}}
    record WorkPage(List<WorkItem> items,int page,int size,boolean hasNext){public WorkPage{items=List.copyOf(items);}}
    record OrganizationQuery(String state,UUID policyId,int page,int size){}
    record OrganizationSummary(UUID id,String displayName,String state,String admissionOrigin,long version,UUID initialOwnerMembershipId,ActivationOutcome activation,java.util.Map<String,java.util.Map<String,Long>> membershipCounts,List<String> allowedActions){public OrganizationSummary{var copy=new java.util.TreeMap<String,java.util.Map<String,Long>>();membershipCounts.forEach((role,states)->copy.put(role,java.util.Map.copyOf(states)));membershipCounts=java.util.Map.copyOf(copy);allowedActions=List.copyOf(allowedActions);}}
    record OrganizationSummaryPage(List<OrganizationSummary> items,int page,int size,boolean hasNext){public OrganizationSummaryPage{items=List.copyOf(items);}}
    record LifecycleInput(String reason,UUID reauthenticationProofId){@Override public String toString(){return "OrganizationLifecycleInput[redacted]";}}
    private static GovernanceFailure unavailable(){return new GovernanceFailure(GovernanceFailure.Code.POLICY_UNAVAILABLE);}
    record ReviewerAssignmentInput(UUID assigneeAccountId,String reason){@Override public String toString(){return "ReviewerAssignmentInput[redacted]";}}
    record ReviewData(ApplicationSummary application,com.elogistic.backend.merchant.api.customer.MerchantApplications.Input submission,List<com.elogistic.backend.merchant.api.customer.MerchantApplications.Evidence> evidence,Instant contactVerifiedAt,String policyVersion,List<java.util.Map<String,Object>> reviewEvents,String organizationDisplayName){
        public ReviewData{evidence=List.copyOf(evidence);reviewEvents=List.copyOf(reviewEvents);}
        @Override public String toString(){return "ReviewData[redacted]";}
    }
    record Checklist(boolean registrationValid,boolean representationValid,boolean contactValid,boolean duplicatesClear,boolean conflictsClear){}
    record ReviewInput(String action,long revision,String policyVersion,Checklist checklist,String reason){@Override public String toString(){return "ReviewInput[redacted]";}}
    record DecisionInput(long revision,String policyVersion,UUID reauthenticationProofId,String reason){@Override public String toString(){return "DecisionInput[redacted]";}}
    record GovernanceAssignment(UUID id,String scopeType,UUID policyId,UUID organizationId,List<String> permissions,long version,Instant expiresAt){public GovernanceAssignment{permissions=List.copyOf(permissions);}}
    record GovernanceContext(UUID accountId,UUID realmId,String authorityVersion,List<GovernanceAssignment> assignments){public GovernanceContext{assignments=List.copyOf(assignments);}}
    record ApplicationQuery(java.time.LocalDate from,java.time.LocalDate to,String state,UUID reviewerId,UUID policyId,int page,int size){}
    record ApplicationSummaryPage(List<ApplicationSummary> items,int page,int size,boolean hasNext){public ApplicationSummaryPage{items=List.copyOf(items);}}
    record ApplicationRevision(UUID applicationId,long revision,Instant submittedAt,String policyVersion,String organizationDisplayName,String digest,List<UUID> evidenceIds,Instant contactVerifiedAt){public ApplicationRevision{evidenceIds=List.copyOf(evidenceIds);}}
    record ApplicationRevisionPage(List<ApplicationRevision> items,int page,int size,boolean hasNext){public ApplicationRevisionPage{items=List.copyOf(items);}}
    record ReviewerCandidate(UUID accountId,String displayName){}
    record AuditItem(UUID id,UUID actorId,String operation,String targetType,UUID targetId,long priorVersion,long newVersion,Long revision,String policyVersion,String reasonCode,Instant occurredAt,UUID requestId){}
    record AuditQuery(String domain,UUID targetId,Instant from,Instant to,int page,int size){}
    record AuditPage(List<AuditItem> items,int page,int size,boolean hasNext){public AuditPage{items=List.copyOf(items);}}
    record CommandReceipt(String operationId,UUID targetId,String outcome,long version,ActivationOutcome activation){}
    record EvidenceContent(byte[] bytes,String mime){public EvidenceContent{bytes=bytes.clone();} @Override public byte[] bytes(){return bytes.clone();}@Override public String toString(){return "GovernanceEvidenceContent[redacted]";}}
    record ReviewerCandidatePage(List<ReviewerCandidate> items,int page,int size,boolean hasNext){public ReviewerCandidatePage{items=List.copyOf(items);}}
    record InformationRequest(long revision,List<String> fieldCodes,String reason){public InformationRequest{fieldCodes=fieldCodes==null?null:List.copyOf(fieldCodes);}@Override public String toString(){return "InformationRequest[redacted]";}}
    record ActivationOutcome(UUID applicationId,long revision,UUID decisionEventId,UUID organizationId,UUID ownerMembershipId,String state,Instant activatedAt){}
    record ApplicationSummary(UUID id,String state,long revision,long version,String legalNameMasked,String legalIdentifierMasked,UUID policyId,UUID reviewerId,Instant firstSubmittedAt,Instant updatedAt,ActivationOutcome activation,List<String> allowedActions){public ApplicationSummary{allowedActions=List.copyOf(allowedActions);}}
    enum Kind {POLICY,APPLICATION,ORGANIZATION}
    record Target(Kind kind,UUID id) {
        public Target {if(kind==null||id==null)throw new IllegalArgumentException("GOVERNANCE_TARGET_REQUIRED");}
    }
    interface GovernanceScope {
        void require(String permission,Target target);
        String version();
    }
}
