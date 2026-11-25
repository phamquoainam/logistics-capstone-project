package com.elogistic.backend.merchant.api.customer;

import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import java.util.*;

/** Applicant self scope only. A saved draft and its references confer no Merchant authority. */
public interface MerchantApplications {
    Application create(CustomerAdmission.Context context,Input input,UUID key);
    Application update(CustomerAdmission.Context context,UUID id,Input input,long expectedVersion,UUID key);
    Application get(CustomerAdmission.Context context,UUID id);
    Page list(CustomerAdmission.Context context,Query query);
    Receipt receipt(CustomerAdmission.Context context,String operation,UUID key);
    default ApplicationRequirements requirements(CustomerAdmission.Context context,UUID application){throw new com.elogistic.backend.merchant.api.GovernanceFailure(com.elogistic.backend.merchant.api.GovernanceFailure.Code.POLICY_UNAVAILABLE);}
    // Approved P0 commands remain fail-closed for the legacy/default composition.
    default Application submit(CustomerAdmission.Context context,UUID id,SubmissionInput input,long expectedVersion,UUID key){throw new com.elogistic.backend.merchant.api.GovernanceFailure(com.elogistic.backend.merchant.api.GovernanceFailure.Code.POLICY_UNAVAILABLE);}
    default Application resubmit(CustomerAdmission.Context context,UUID id,SubmissionInput input,long expectedVersion,UUID key){throw new com.elogistic.backend.merchant.api.GovernanceFailure(com.elogistic.backend.merchant.api.GovernanceFailure.Code.POLICY_UNAVAILABLE);}
    default Application withdraw(CustomerAdmission.Context context,UUID id,ReasonInput input,long expectedVersion,UUID key){throw new com.elogistic.backend.merchant.api.GovernanceFailure(com.elogistic.backend.merchant.api.GovernanceFailure.Code.POLICY_UNAVAILABLE);}
    /** Owns two bounded Identity admissions around external byte I/O; never accepts a caller actor ID. */
    default Evidence uploadEvidence(String access,UUID request,UUID application,String purpose,String mime,byte[] file,UUID key){throw new com.elogistic.backend.merchant.api.GovernanceFailure(com.elogistic.backend.merchant.api.GovernanceFailure.Code.POLICY_UNAVAILABLE);}
    default Evidence evidence(CustomerAdmission.Context context,UUID application,UUID id){throw new com.elogistic.backend.merchant.api.GovernanceFailure(com.elogistic.backend.merchant.api.GovernanceFailure.Code.POLICY_UNAVAILABLE);}
    default void archiveEvidence(CustomerAdmission.Context context,UUID application,UUID evidence,long expectedVersion,UUID key){throw new com.elogistic.backend.merchant.api.GovernanceFailure(com.elogistic.backend.merchant.api.GovernanceFailure.Code.POLICY_UNAVAILABLE);}
    default byte[] evidenceContent(String access,UUID request,UUID application,UUID evidenceId){throw new com.elogistic.backend.merchant.api.GovernanceFailure(com.elogistic.backend.merchant.api.GovernanceFailure.Code.POLICY_UNAVAILABLE);}
    record SubmissionInput(String organizationDisplayName){}
    record ConsentStatements(String accuracy,String representation){}
    record ApplicationRequirements(UUID applicationId,UUID policyId,String policyVersion,String consentVersion,ConsentStatements consentStatements,List<String> requiredEvidence){public ApplicationRequirements{requiredEvidence=List.copyOf(requiredEvidence);}}
    record ReasonInput(String reason){@Override public String toString(){return "ReasonInput[redacted]";}}
    record Evidence(UUID id,UUID applicationId,String purpose,String state,String mime,long bytes,String sha256,long version,java.time.Instant screenedAt){@Override public String toString(){return "ApplicationEvidence[redacted]";}}
    record Address(String country,String administrativeArea,String locality,String addressLine) {
        @Override public String toString(){return "ApplicationAddress[redacted]";}
    }
    record Declaration(boolean accuracy,boolean representation,String consentVersion) {}
    record Input(String entityType,String jurisdiction,String registeredName,String legalIdentifier,
                 Address registeredAddress,Address operatingAddress,String representativeName,String representativeEmail,
                 String representativeRelationship,UUID businessContactAttestationId,List<UUID> evidenceIds,Declaration declarations) {
        public Input {if(evidenceIds!=null)evidenceIds=List.copyOf(evidenceIds);}
        @Override public String toString(){return "ApplicationInput[redacted]";}
    }
    record Application(UUID id,UUID applicantAccountId,String state,long revision,long version,Input draft) {
        @Override public String toString(){return "MerchantApplication[redacted]";}
    }
    record Query(int page,int size,String search,String state) {
        @Override public String toString(){return "ApplicationQuery[redacted]";}
    }
    record Page(List<Application> items,int page,int size,boolean hasNext) {public Page{items=List.copyOf(items);}}
    record Receipt(String operationId,UUID targetId,String outcome,long version) {}
}
