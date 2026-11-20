package com.elogistic.backend.orders.api;

import com.elogistic.backend.platform.api.PrivateEvidenceStore;
import java.time.Instant;
import java.util.*;

/** Evidence metadata and domain authority; never exposes private object keys to HTTP. */
public interface OrganizationClaimEvidence {
    record Input(String mime,long bytes,String sha256){}
    record Evidence(UUID id,UUID intakeId,String mime,long bytes,String sha256,String state,Instant createdAt,Instant attachedAt,long version){@Override public String toString(){return "ClaimEvidence[redacted]";}}
    record Permit(UUID organizationId,UUID actorId,Evidence evidence,UUID objectKey){@Override public String toString(){return "ClaimEvidencePermit[redacted]";}}
    List<Evidence> list(OrganizationSupportTickets.Access access,UUID intakeId);
    Evidence reserve(OrganizationSupportTickets.Access access,UUID intakeId,Input input,long version,UUID operationKey);
    Permit uploadPermit(OrganizationSupportTickets.Access access,UUID intakeId,UUID evidenceId);
    Evidence complete(OrganizationSupportTickets.Access access,Permit permit,PrivateEvidenceStore.Blob verified);
    Permit readPermit(OrganizationSupportTickets.Access access,UUID intakeId,UUID evidenceId);
    Permit staffReadPermit(OrganizationSupportTickets.StaffAccess access,UUID intakeId,UUID evidenceId);
    List<Evidence> staffList(OrganizationSupportTickets.StaffAccess access,UUID intakeId);
}
