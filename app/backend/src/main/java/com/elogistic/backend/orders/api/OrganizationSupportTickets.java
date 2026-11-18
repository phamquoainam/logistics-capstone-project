package com.elogistic.backend.orders.api;

import java.time.*;
import java.util.*;

/** OSC manifest 9484c415: Organization Support, Local/Test only. Identity owns admission. */
public interface OrganizationSupportTickets {
    interface Access extends BusinessPreparations.Access {
        boolean eligibleAccount(UUID account);
        Map<UUID,String> displayNames(Set<UUID> accounts);
        Set<UUID> eligibleAccounts(Set<UUID> accounts);
    }
    interface StaffAccess extends ShippingDrafts.Access { boolean eligibleAccount(UUID account); }
    record Filters(int page,int size,String state,String category,UUID shipmentId,UUID facilityId,UUID creatorId,
                   String assignee,String requestedPriority,LocalDate from,LocalDate until,String sort){}
    record Search(Filters filters,String search){@Override public String toString(){return "SupportSearch[redacted]";}}
    record Create(String kind,UUID shipmentId,UUID facilityId,String category,String subject,String message,String requestedPriority){@Override public String toString(){return "OrganizationSupportCreate[redacted]";}}
    record Summary(UUID id,String code,UUID organizationId,UUID facilityId,UUID creatorAccountId,String kind,UUID shipmentId,
                   String category,String subject,String state,long version,Instant createdAt,Instant updatedAt,String requestedPriority,
                   String priority,Instant responseDueAt,String teamLabel,UUID internalAssigneeAccountId){}
    record Page(List<Summary> items,int page,int size,boolean hasNext){public Page{items=List.copyOf(items);}}
    record Message(UUID id,String authorKind,String message,Instant createdAt,String eventType){@Override public String toString(){return "SupportMessage[redacted]";}}
    record Detail(UUID id,String code,UUID organizationId,UUID facilityId,UUID creatorAccountId,String kind,UUID shipmentId,
                  String category,String subject,String state,long version,Instant createdAt,Instant updatedAt,String requestedPriority,
                  String priority,Instant responseDueAt,String teamLabel,UUID internalAssigneeAccountId,List<Message> publicMessages,
                  Assignment internalAssignment,Set<String> allowedActions){
        public Detail{publicMessages=List.copyOf(publicMessages);allowedActions=Set.copyOf(allowedActions);}
        public Detail(Summary s,List<Message> messages,Assignment assignment,Set<String> actions){this(s.id(),s.code(),s.organizationId(),s.facilityId(),s.creatorAccountId(),s.kind(),s.shipmentId(),s.category(),s.subject(),s.state(),s.version(),s.createdAt(),s.updatedAt(),s.requestedPriority(),s.priority(),s.responseDueAt(),s.teamLabel(),s.internalAssigneeAccountId(),messages,assignment,actions);}
    }
    record Receipt(UUID operationId,UUID targetId,String outcome,long version){}
    record Assignment(UUID ticketId,UUID organizationId,UUID assigneeAccountId,long version,Instant updatedAt){}
    record Member(UUID accountId,UUID membershipId,String displayName){}
    record MemberPage(List<Member> items,int page,int size,boolean hasNext){public MemberPage{items=List.copyOf(items);}}
    record History(UUID id,String operation,UUID actorAccountId,UUID priorAssigneeAccountId,UUID assigneeAccountId,long version,Instant occurredAt){}
    record HistoryPage(List<History> items,int page,int size,boolean hasNext){public HistoryPage{items=List.copyOf(items);}}
    record TimeSamples(String sampleCount,String totalMs,String minMs,String maxMs){}
    record Period(LocalDate from,LocalDate until){}
    record Report(String schemaVersion,UUID organizationId,Instant generatedAt,String timeZone,Period period,String totalTickets,
                  Map<String,String> byState,Map<String,String> byCategory,Map<String,String> byRequestedPriority,TimeSamples firstResponse,TimeSamples firstResolution,String slaState){}
    record Preferences(boolean enabled,List<String> eventTypes,long version,Instant updatedAt){public Preferences{eventTypes=List.copyOf(eventTypes);}}
    record Notice(UUID id,UUID organizationId,UUID ticketId,UUID eventId,String type,Instant createdAt,Instant readAt,long version){}
    record NoticePage(List<Notice> items,int page,int size,boolean hasNext){public NoticePage{items=List.copyOf(items);}}
    Set<UUID> references(Access a,UUID ticket);
    Page list(Access a,Filters filters,String search);
    Detail get(Access a,UUID ticket);
    Detail create(Access a,Create input,UUID key);
    Detail command(Access a,UUID ticket,String operation,String message,long expected,UUID key);
    Receipt receipt(Access a,String operation,UUID key);
    Assignment assignment(Access a,UUID ticket);
    MemberPage candidates(Access a,UUID ticket,int page);
    Assignment assign(Access a,UUID ticket,UUID assignee,long expected,UUID key);
    HistoryPage history(Access a,UUID ticket,int page);
    Report report(Access a,Filters filters);
    Preferences preferences(Access a);
    Preferences preferences(Access a,boolean enabled,List<String> events,long expected,UUID key);
    NoticePage notices(Access a,int page);
    Notice read(Access a,UUID notice,long expected,UUID key);
    Page queue(StaffAccess a,UUID organization,int page);
    Set<UUID> staffReferences(StaffAccess a,UUID ticket,boolean triage);
    Detail assigned(StaffAccess a,UUID ticket);
    Summary carrierAssign(StaffAccess a,UUID ticket,UUID assignee,long expected,UUID key);
    Detail carrierCommand(StaffAccess a,UUID ticket,String operation,String action,String message,long expected,UUID key);
    Receipt carrierReceipt(StaffAccess a,String operation,UUID key);
    final class Failure extends RuntimeException{public Failure(String code){super(code);}}
}
