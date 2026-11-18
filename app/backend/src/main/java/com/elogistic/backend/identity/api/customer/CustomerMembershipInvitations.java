package com.elogistic.backend.identity.api.customer;
import java.time.Instant;
import java.util.*;
/** Purpose-separated protected delivery. This never activates an account or grants Membership. */
public interface CustomerMembershipInvitations {
    record Bound(CustomerAdmission.Context customer,UUID organizationId,UUID issuerAccountId){
        public Bound(CustomerAdmission.Context customer,UUID organizationId){this(customer,organizationId,customer.actor().accountId());}
    }
    record ProtectedInvitation(UUID deliveryId,byte[] recipientDigest,byte[] protectedRecipient,byte[] proofDigest){
        public ProtectedInvitation{recipientDigest=recipientDigest.clone();protectedRecipient=protectedRecipient.clone();proofDigest=proofDigest.clone();}
        @Override public byte[] recipientDigest(){return recipientDigest.clone();}
        @Override public byte[] protectedRecipient(){return protectedRecipient.clone();}
        @Override public byte[] proofDigest(){return proofDigest.clone();}
        @Override public String toString(){return "ProtectedMembershipInvitation[redacted]";}
    }
    ProtectedInvitation enqueueMembershipInvitation(Bound context,UUID invitationId,long proofVersion,String recipient,byte[] proof,Instant expiresAt);
    ProtectedInvitation resendMembershipInvitation(Bound context,UUID invitationId,long proofVersion,byte[] protectedRecipient,byte[] proof,Instant expiresAt);
    byte[] recipientDigest(Bound context,String recipient);
    String maskedRecipient(Bound context,UUID invitationId,byte[] protectedRecipient);
    boolean isRecipient(Bound context,UUID invitationId,byte[] protectedRecipient);
    void consume(Bound context,UUID invitationId,long proofVersion,byte[] proof);
    void revoke(Bound context,UUID invitationId);
    String deliveryState(Bound context,UUID invitationId,long proofVersion);
    final class Failure extends RuntimeException {public Failure(String code){super(code);}}
}
