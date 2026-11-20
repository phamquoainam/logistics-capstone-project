package com.elogistic.backend.orders.api;

import java.time.Instant;
import java.util.*;

/** Source/test-authorized document intake; deliberately no compensation/financial decision operation. */
public interface OrganizationClaimIntakes {
    record Create(UUID ticketId,String category,String narrative){@Override public String toString(){return "ClaimIntakeCreate[redacted]";}}
    record Summary(UUID id,UUID organizationId,UUID facilityId,UUID ticketId,UUID shipmentId,UUID creatorAccountId,String category,String state,long version,Instant createdAt,Instant updatedAt,String eligibility,String compensation){@Override public String toString(){return "ClaimIntake[redacted]";}}
    record Event(UUID id,String authorKind,String operation,String message,long version,Instant occurredAt){@Override public String toString(){return "ClaimIntakeEvent[redacted]";}}
    record Detail(Summary intake,List<Event> events,Set<String> allowedActions){public Detail{events=List.copyOf(events);allowedActions=Set.copyOf(allowedActions);}}
    record Page(List<Summary> items,int page,int size,boolean hasNext){public Page{items=List.copyOf(items);}}
    record Receipt(UUID operationId,UUID targetId,String outcome,long version){}
    record Report(String schemaVersion,UUID organizationId,Instant generatedAt,String total,Map<String,String> byState,Map<String,String> byCategory,String cleanEvidence,OrganizationSupportTickets.TimeSamples firstStaffResponse,OrganizationSupportTickets.TimeSamples intakeClosure,String slaState,String compensation){public Report{byState=Map.copyOf(byState);byCategory=Map.copyOf(byCategory);}}
    record Notice(UUID id,UUID intakeId,UUID ticketId,UUID eventId,String type,Instant createdAt){}
    record Notices(List<Notice> items,int page,boolean hasNext){public Notices{items=List.copyOf(items);}}
    final class Failure extends RuntimeException{public Failure(String code){super(code);}}
    Page list(OrganizationSupportTickets.Access access,int page,UUID ticketId,String state);
    Report report(OrganizationSupportTickets.Access access);
    Notices notices(OrganizationSupportTickets.Access access,int page);
    Detail get(OrganizationSupportTickets.Access access,UUID id);
    Set<UUID> references(OrganizationSupportTickets.Access access,UUID id);
    Detail create(OrganizationSupportTickets.Access access,Create input,UUID key);
    Detail supplement(OrganizationSupportTickets.Access access,UUID id,String narrative,long version,UUID key);
    Receipt receipt(OrganizationSupportTickets.Access access,String operation,UUID key);
    Detail assigned(OrganizationSupportTickets.StaffAccess access,UUID id);
    Page assignedIntakes(OrganizationSupportTickets.StaffAccess access,UUID ticketId,int page);
    Set<UUID> staffReferences(OrganizationSupportTickets.StaffAccess access,UUID id);
    Detail staffCommand(OrganizationSupportTickets.StaffAccess access,UUID id,String action,String message,long version,UUID key);
}
