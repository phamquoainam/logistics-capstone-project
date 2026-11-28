package com.elogistic.backend.merchant.api;

import com.elogistic.backend.identity.api.admin.AdminCommandContext;
import java.time.*;
import java.util.*;

/** Owner-approved M2-R01 Merchant projections. No lifecycle, timing or notice authority. */
public interface MerchantGovernanceViews {
    record PageQuery(Integer size,String cursor) {}
    record ApplicationSearch(LocalDate from,LocalDate to,String state,UUID policyId,UUID reviewerId,UUID applicationId,UUID applicantAccountId,String registeredName,String legalIdentifier,String purpose,PageQuery page,Instant dueBefore,Boolean overdue,String checkCode) {
        @Override public String toString(){return "ApplicationSearch[redacted]";}
    }
    record OrganizationSearch(String state,UUID policyId,UUID organizationId,String registeredName,String legalIdentifier,UUID currentOwnerAccountId,LocalDate activatedFrom,LocalDate activatedTo,String purpose,PageQuery page,String admissionOrigin) {
        @Override public String toString(){return "OrganizationSearch[redacted]";}
    }
    record ApplicationSearchPage(List<MerchantGovernance.ApplicationSummary> items,String nextCursor,String snapshotVersion,long matchedCount){public ApplicationSearchPage{items=List.copyOf(items);}}
    record OrganizationRow(UUID id,String displayName,String state,String admissionOrigin,long version,UUID initialOwnerMembershipId,UUID currentOwnerMembershipId,Long ownershipVersion,MerchantGovernance.ActivationOutcome activation,Map<String,Map<String,Long>> membershipCounts) {}
    record OrganizationSearchPage(List<OrganizationRow> items,String nextCursor,String snapshotVersion,long matchedCount){public OrganizationSearchPage{items=List.copyOf(items);}}
    record ComparisonRequest(long fromRevision,long toRevision,String purpose) {}
    record FieldChange(String field,Object before,Object after){@Override public String toString(){return "FieldChange[redacted]";}}
    record ContactTimes(Instant from,Instant to) {}
    record RevisionComparison(UUID applicationId,long fromRevision,long toRevision,String fromDigest,String toDigest,List<FieldChange> changes,List<UUID> evidenceAdded,List<UUID> evidenceRemoved,ContactTimes contactVerifiedAt,UUID disclosureEventId){public RevisionComparison{changes=List.copyOf(changes);evidenceAdded=List.copyOf(evidenceAdded);evidenceRemoved=List.copyOf(evidenceRemoved);}@Override public String toString(){return "RevisionComparison[redacted]";}}
    record DisclosureRequest(String purpose,PageQuery page) {}
    record OrganizationProfile(UUID organizationId,long organizationVersion,UUID initialOwnerMembershipId,UUID currentOwnerMembershipId,UUID activationApplicationId,Map<String,Object> legal,Map<String,Object> contact,Map<String,Object> facilities,Map<String,Object> pricing,Map<String,Object> holds,UUID disclosureEventId){@Override public String toString(){return "OrganizationProfile[redacted]";}}
    record Member(UUID membershipId,UUID accountId,String role,String state,long version,long authorityVersion,boolean isInitialOwner,boolean isCurrentOwner) {}
    record MembershipPage(UUID organizationId,long organizationVersion,List<Member> items,long pendingInvitationCount,String nextCursor,UUID disclosureEventId){public MembershipPage{items=List.copyOf(items);}}
    record CommentInput(long revision,String text){@Override public String toString(){return "CommentInput[redacted]";}}
    record Comment(UUID id,UUID applicationId,long revision,UUID actorAccountId,Instant createdAt,String text){@Override public String toString(){return "ReviewComment[redacted]";}}
    record CommentPage(List<Comment> items,String nextCursor,UUID disclosureEventId){public CommentPage{items=List.copyOf(items);}@Override public String toString(){return "ReviewComments[redacted]";}}
    record Check(String code,String result,String sourceVersion,UUID sourceEventId) {}
    record Checks(UUID applicationId,long revision,List<Check> checks,String foreignMatches,String automaticVerification){public Checks{checks=List.copyOf(checks);}}
    ApplicationSearchPage searchApplications(AdminCommandContext actor,ApplicationSearch input);
    OrganizationSearchPage searchOrganizations(AdminCommandContext actor,OrganizationSearch input);
    RevisionComparison compare(AdminCommandContext actor,UUID applicationId,ComparisonRequest input);
    OrganizationProfile profile(AdminCommandContext actor,UUID organizationId,DisclosureRequest input);
    MembershipPage members(AdminCommandContext actor,UUID organizationId,DisclosureRequest input);
    Comment comment(AdminCommandContext actor,UUID applicationId,CommentInput input,long expectedVersion,UUID key);
    CommentPage comments(AdminCommandContext actor,UUID applicationId,PageQuery input);
    Checks checks(AdminCommandContext actor,UUID applicationId);
}
