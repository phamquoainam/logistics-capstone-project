package com.elogistic.backend.orders.api;
import com.elogistic.backend.merchant.api.customer.BusinessWorkspaces;
import java.time.Instant;
import java.util.*;

/** Merchant work completion never mutates a Shipment, Carrier event or financial fact. */
public interface OrganizationActions {
    interface Access { UUID actorAccountId(); UUID requestId(); BusinessWorkspaces.Scope scope(); boolean eligibleAccount(UUID account); void requireCurrent(); void meaningfulAction(); }
    record Input(String targetKind,UUID targetId,String type,String title,String details,String priority,Instant dueAt){@Override public String toString(){return "OrganizationActionInput[redacted]";}}
    record Task(UUID id,UUID organizationId,UUID facilityId,String targetKind,UUID targetId,String type,String title,String state,String priority,UUID assigneeAccountId,Instant dueAt,Instant initialDueAt,long version,Instant createdAt,Instant updatedAt,Instant completedAt,boolean overdue,List<String> allowedActions){public Task{allowedActions=List.copyOf(allowedActions);}@Override public String toString(){return "OrganizationAction[redacted]";}}
    record Event(UUID id,String operation,UUID actorAccountId,String message,long version,Instant occurredAt){@Override public String toString(){return "OrganizationActionEvent[redacted]";}}
    record Detail(Task task,String details,List<Event> events){public Detail{events=List.copyOf(events);}@Override public String toString(){return "OrganizationActionDetail[redacted]";}}
    record Page(List<Task> items,int page,int size,boolean hasNext,long total,Map<String,Long> counts,Instant generatedAt){public Page{items=List.copyOf(items);counts=Map.copyOf(counts);}}
    record Receipt(String operationId,UUID targetId,String outcome,long version){}
    record Change(Detail result,UUID eventId,UUID notifyAccountId,String noticeType,boolean replay){}
    Page list(Access access,Map<String,String> query);
    Detail get(Access access,UUID id);
    Change create(Access access,Input input,UUID key);
    Change assign(Access access,UUID id,UUID assignee,String reason,long version,UUID key);
    Change comment(Access access,UUID id,String message,long version,UUID key);
    Change transition(Access access,UUID id,String action,String message,long version,UUID key);
    Change policy(Access access,UUID id,String priority,Instant dueAt,String reason,long version,UUID key);
    Receipt receipt(Access access,String operation,UUID key);
}
