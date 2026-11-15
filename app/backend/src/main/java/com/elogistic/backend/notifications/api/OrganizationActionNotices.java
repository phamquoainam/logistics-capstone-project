package com.elogistic.backend.notifications.api;
import java.time.Instant;
import java.util.*;
public interface OrganizationActionNotices {
    /** Workflow supplies current admitted scope; Notifications does not query Merchant. */
    interface Access { UUID actorAccountId(); UUID organizationId(); boolean canReadAllTasks(); Set<UUID> readableFacilities(); void requireCurrent(); }
    record Notice(UUID id,UUID organizationId,UUID taskId,UUID eventId,String type,Instant createdAt,Instant readAt){}
    record Page(List<Notice> items,int page,int size,boolean hasNext){public Page{items=List.copyOf(items);}}
    void record(Access access,UUID task,UUID event,UUID recipient,UUID facility,String type,Instant occurredAt);
    Page list(Access access,int page,java.util.function.Predicate<UUID> currentTaskAccess);
    Notice read(Access access,UUID id,java.util.function.Predicate<UUID> currentTaskAccess);
}
