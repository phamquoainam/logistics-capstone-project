package com.elogistic.backend.merchant.api.customer;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/** OTA V1: one canonical Organization Membership authority, never platform administration. */
public interface OrganizationTeam {
    interface Access {
        UUID actorAccountId(); UUID requestId(); BusinessWorkspaces.Scope scope();
        void requireCurrent(); void meaningfulAction();
        Map<UUID,String> displayNames(Set<UUID> accounts);
        boolean eligibleAccount(UUID account);
        void reauthenticate(UUID proof);
        byte[] recipientDigest(String recipient);
        ProtectedDelivery enqueue(UUID invitation,long proofVersion,String recipient,byte[] proof,Instant expiresAt);
        ProtectedDelivery reenqueue(UUID issuer,UUID invitation,long proofVersion,byte[] protectedRecipient,byte[] proof,Instant expiresAt);
        String maskedRecipient(UUID invitation,byte[] protectedRecipient);
        void revokeInvitationProof(UUID invitation);
        String deliveryState(UUID invitation,long proofVersion);
    }
    MemberPage members(Access access,MemberSearch search);
    MemberDetail member(Access access,UUID id);
    RoleTemplates roleTemplates(Access access);
    Set<UUID> references(Access access,UUID memberId);
    MemberDetail updateAccess(Access access,UUID memberId,AccessInput input,long version,UUID key);
    MemberDetail lifecycle(Access access,UUID memberId,String operation,SensitiveReason input,long version,UUID key);
    Receipt receipt(Access access,String operation,UUID key);
    AuditPage audit(Access access,AuditSearch query);
    boolean deliveryCurrent(UUID invitation,UUID proof,UUID organization,UUID issuer,Instant now,java.util.function.Predicate<UUID> eligibleAccount);
    Invitation invite(Access access,InvitationInput input,UUID key);
    InvitationPage invitations(Access access,InvitationSearch query);
    Invitation resend(Access access,UUID id,Reason input,long version,UUID key);
    Invitation revoke(Access access,UUID id,Reason input,long version,UUID key);
    Set<UUID> invitationReferences(Access access,UUID id);
    interface RecipientAccess {
        UUID actorAccountId(); void requireCurrent(); void meaningfulAction(); boolean eligibleAccount(UUID id);
        boolean matchesRecipient(UUID organization,UUID invitation,byte[] contact);
        void consume(UUID organization,UUID invitation,long proofVersion,byte[] proof);
    }
    record RecipientReferences(UUID organizationId,Set<UUID> accounts){public RecipientReferences{accounts=Set.copyOf(accounts);}}
    RecipientReferences recipientReferences(RecipientAccess access,UUID invitation);
    DecisionReceipt decide(RecipientAccess access,UUID invitation,boolean accept,InvitationDecision input,UUID key);
    record ProtectedDelivery(byte[] recipientDigest,byte[] protectedRecipient,byte[] proofDigest){
        @Override public String toString(){return "TeamProtectedDelivery[redacted]";}
    }
    record Grant(String permission,String scope,UUID facilityId) {}
    record MemberSearch(String search,String role,String state,UUID facilityId,int page,int size) {
        @Override public String toString(){return "TeamMemberSearch[redacted]";}
    }
    record Member(UUID membershipId,String displayName,String role,String state,long authorityVersion,long version,Instant createdAt,Instant updatedAt) {
        @Override public String toString(){return "TeamMember[redacted]";}
    }
    record MemberPage(List<Member> items,int page,int size,boolean hasNext){public MemberPage{items=List.copyOf(items);}}
    record MemberDetail(UUID membershipId,String displayName,String role,String state,long authorityVersion,long version,Instant createdAt,Instant updatedAt,List<Grant> grants){
        public MemberDetail{grants=List.copyOf(grants);}
        @Override public String toString(){return "TeamMemberDetail[redacted]";}
    }
    record RoleTemplate(String role,long version,List<String> allowedPermissions){public RoleTemplate{allowedPermissions=List.copyOf(allowedPermissions);}}
    record RoleTemplates(List<RoleTemplate> items){public RoleTemplates{items=List.copyOf(items);}}
    record InvitationInput(String email,String role,List<Grant> grants,String reason){
        public InvitationInput{if(grants!=null)grants=List.copyOf(grants);}
        @Override public String toString(){return "TeamInvitationInput[redacted]";}
    }
    record Invitation(UUID id,String maskedEmail,String role,List<Grant> grants,String state,long version,Instant expiresAt,String deliveryState,Instant createdAt){
        public Invitation{grants=List.copyOf(grants);}
        @Override public String toString(){return "TeamInvitation[redacted]";}
    }
    record InvitationSearch(String state,int page,int size){}
    record InvitationPage(List<Invitation> items,int page,int size,boolean hasNext){public InvitationPage{items=List.copyOf(items);}}
    record Reason(String reason){@Override public String toString(){return "TeamReason[redacted]";}}
    record SensitiveReason(String reason,UUID reauthenticationProofId){@Override public String toString(){return "TeamSensitiveReason[redacted]";}}
    record AccessInput(String role,List<Grant> grants,String reason,UUID reauthenticationProofId){
        public AccessInput{if(grants!=null)grants=List.copyOf(grants);}
        @Override public String toString(){return "TeamAccessInput[redacted]";}
    }
    record InvitationDecision(String proof){@Override public String toString(){return "TeamInvitationDecision[redacted]";}}
    record AuditSearch(UUID membershipId,String operation,Instant from,Instant to,int page,int size){}
    record Audit(UUID id,UUID actorMembershipId,UUID targetMembershipId,String operation,String reason,Instant occurredAt){@Override public String toString(){return "TeamAudit[redacted]";}}
    record AuditPage(List<Audit> items,int page,int size,boolean hasNext){public AuditPage{items=List.copyOf(items);}}
    record Receipt(String operation,UUID key,UUID targetId,String outcome,long version){}
    record DecisionReceipt(UUID invitationId,String outcome,UUID membershipId){}
    final class Failure extends RuntimeException{public Failure(String code){super(code);}}
}
