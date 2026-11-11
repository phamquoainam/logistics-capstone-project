package com.elogistic.backend.orders.api;
import java.time.Instant;
import java.util.*;
/** Owner-approved PERSONAL Local/Test tickets. Not Claims or a Shipment command. */
public interface SupportTickets {
    interface Access extends ShippingDrafts.Access { boolean eligibleAccount(UUID accountId); }
    record Create(String kind,UUID shipmentId,String category,String subject,String message){@Override public String toString(){return "CreateSupportTicket[redacted]";}}
    record Summary(UUID id,String code,String kind,UUID shipmentId,String category,String subject,String state,long version,Instant createdAt,Instant updatedAt,String priority,Instant responseDueAt,String teamLabel){@Override public String toString(){return "SupportTicket[redacted]";}}
    record Message(UUID id,String authorKind,String message,Instant createdAt,String eventType){@Override public String toString(){return "SupportMessage[redacted]";}}
    record Detail(UUID id,String code,String kind,UUID shipmentId,String category,String subject,String state,long version,Instant createdAt,Instant updatedAt,String priority,Instant responseDueAt,String teamLabel,List<Message> publicMessages,Set<String> allowedActions){
        public Detail{publicMessages=List.copyOf(publicMessages);allowedActions=Set.copyOf(allowedActions);}
        public Detail(Summary s,List<Message> messages,Set<String> actions){this(s.id(),s.code(),s.kind(),s.shipmentId(),s.category(),s.subject(),s.state(),s.version(),s.createdAt(),s.updatedAt(),s.priority(),s.responseDueAt(),s.teamLabel(),messages,actions);}
        public Summary ticket(){return new Summary(id,code,kind,shipmentId,category,subject,state,version,createdAt,updatedAt,priority,responseDueAt,teamLabel);}
        @Override public String toString(){return "SupportDetail[redacted]";}
    }
    record Page(List<Summary> items,int page,int size,boolean hasNext){public Page{items=List.copyOf(items);}}
    record Receipt(String operationId,UUID targetId,String outcome,long version){}
    final class Failure extends RuntimeException{private final String code;public Failure(String code){super(code);this.code=code;}public String code(){return code;}}
    Page list(Access access,int page,String state,String category,UUID shipmentId);
    Detail get(Access access,UUID id);
    Detail create(Access access,Create input,UUID key);
    Detail customerCommand(Access access,UUID id,String operation,String message,long version,UUID key);
    Receipt receipt(Access access,String operation,UUID key);
    Page queue(Access access,int page);
    Detail assigned(Access access,UUID id);
    Summary assign(Access access,UUID id,UUID assignee,long version,UUID key);
    Detail transition(Access access,UUID id,String action,String message,long version,UUID key);
}
