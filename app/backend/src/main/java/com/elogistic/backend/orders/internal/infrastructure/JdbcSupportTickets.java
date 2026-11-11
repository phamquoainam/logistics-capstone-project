package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.orders.api.SupportTickets.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Orders-owned commands join the existing locked Identity business transaction. */
public final class JdbcSupportTickets implements SupportTickets {
    private static final Set<String> STATES=Set.of("NEW","IN_PROGRESS","NEEDS_INFORMATION","RESOLVED","CLOSED"), CATEGORIES=Set.of("ACCOUNT_HELP","SHIPMENT_TRACE","FEE_REVIEW","OTHER"), CUSTOMER_OPS=Set.of("CREATE","REPLY","REQUEST_REVIEW","ACKNOWLEDGE");
    private final DataSource source; private final JdbcTemplate jdbc; private final PilotShipments shipments; private final Clock clock; private final boolean enabled;
    private final Map<String,SecretKeySpec> keys; private final String activeKey;
    private record Ticket(Summary summary,UUID owner,Long resolution,boolean review){}
    private record Stored(UUID target,long original,long result,String kid,byte[] fingerprint){}
    public JdbcSupportTickets(DataSource source,PilotShipments shipments,Clock clock,boolean enabled,Map<String,byte[]> supplied,String activeKey){
        this.source=Objects.requireNonNull(source);jdbc=new JdbcTemplate(source);this.shipments=Objects.requireNonNull(shipments);this.clock=Objects.requireNonNull(clock);this.enabled=enabled;
        if(supplied==null||supplied.isEmpty()||!supplied.containsKey(activeKey))throw new IllegalArgumentException("SUPPORT_RECEIPT_KEYS_REQUIRED");
        Map<String,SecretKeySpec> values=new HashMap<>();supplied.forEach((id,key)->{if(!id.matches("[A-Za-z0-9_-]{1,64}")||key.length!=32||Arrays.equals(key,new byte[32]))throw new IllegalArgumentException("SUPPORT_RECEIPT_KEYS_INVALID");values.put(id,new SecretKeySpec(key,"HmacSHA256"));});keys=Map.copyOf(values);this.activeKey=activeKey;
    }
    private static Failure fail(String code){return new Failure(code);}
    private void admit(Access access){access.requireCurrent();if(!enabled)throw fail("SUPPORT_DISABLED");if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("SUPPORT_ADMISSION_TRANSACTION_REQUIRED");}
    private void lock(){jdbc.queryForObject("SELECT 1 FROM pg_advisory_xact_lock(730022,1)",Integer.class);}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private static void one(int changed){if(changed!=1)throw new IllegalStateException("SUPPORT_MANDATORY_EVIDENCE_REQUIRED");}
    private static String text(String value,int max){if(value==null)throw fail("VALIDATION_FAILED");String v=value.replace("\r\n","\n").strip();if(v.isEmpty()||v.codePointCount(0,v.length())>max||v.codePoints().anyMatch(c->Character.isISOControl(c)&&!(max==4000&&(c==10||c==9))||c==0x7f))throw fail("VALIDATION_FAILED");return v;}
    private static void id(UUID value){if(value==null||value.equals(new UUID(0,0)))throw fail("VALIDATION_FAILED");}
    private static Ticket row(ResultSet r,int ignored)throws SQLException{return new Ticket(new Summary(r.getObject("id",UUID.class),r.getString("code"),r.getString("kind"),r.getObject("shipment_id",UUID.class),r.getString("category"),r.getString("subject"),r.getString("state"),r.getLong("version"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant(),"NORMAL",null,null),r.getObject("owner_id",UUID.class),(Long)r.getObject("resolution_version"),r.getBoolean("review_pending"));}
    private Ticket ticket(UUID target,UUID owner,boolean locked){id(target);var found=jdbc.query("SELECT * FROM orders.personal_support_ticket WHERE id=?"+(owner==null?"":" AND owner_id=?")+(locked?" FOR UPDATE":""),JdbcSupportTickets::row,owner==null?new Object[]{target}:new Object[]{target,owner});if(found.isEmpty())throw fail("RESOURCE_NOT_FOUND");return found.getFirst();}
    private void grant(UUID actor,String permission){if(jdbc.queryForObject("SELECT count(*) FROM orders.support_staff_grant WHERE account_id=? AND permission=? AND state='ACTIVE' AND effective_from<=?",Integer.class,actor,permission,Timestamp.from(now()))!=1)throw fail("SUPPORT_AUTHORITY_REQUIRED");}
    private void assignedAuthority(Access access,Ticket ticket){grant(access.accountId(),"SUPPORT_HANDLE");if(jdbc.queryForObject("SELECT count(*) FROM orders.personal_support_assignment WHERE ticket_id=? AND assignee_id=? AND state='ACTIVE'",Integer.class,ticket.summary.id(),access.accountId())!=1)throw fail("SUPPORT_ASSIGNMENT_REQUIRED");}
    private Detail detail(Ticket t,boolean staff){
        var messages=jdbc.query("SELECT id,author_kind,message,created_at,event_type FROM orders.personal_support_message WHERE ticket_id=? AND owner_id=? ORDER BY ticket_version",(r,n)->new Message(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getTimestamp(4).toInstant(),r.getString(5)),t.summary.id(),t.owner);
        Set<String> actions=new LinkedHashSet<>();String state=t.summary.state();
        if(staff){if(state.equals("NEW"))actions.add("START");if(state.equals("IN_PROGRESS"))actions.addAll(List.of("REQUEST_INFORMATION","RESOLVE"));if(state.equals("RESOLVED")&&t.review)actions.add("REOPEN");}
        else {if(Set.of("NEW","IN_PROGRESS","NEEDS_INFORMATION").contains(state))actions.add("REPLY");if(state.equals("RESOLVED")&&!t.review)actions.addAll(List.of("REQUEST_REVIEW","ACKNOWLEDGE"));}
        return new Detail(t.summary,messages,actions);
    }
    @Override public Detail get(Access a,UUID target){admit(a);return detail(ticket(target,a.accountId(),false),false);}
    @Override public Page list(Access a,int page,String state,String category,UUID shipment){admit(a);if(page<0||page>100000||state!=null&&!STATES.contains(state)||category!=null&&!CATEGORIES.contains(category))throw fail("VALIDATION_FAILED");List<Object> args=new ArrayList<>();args.add(a.accountId());String sql="SELECT * FROM orders.personal_support_ticket WHERE owner_id=?";if(state!=null){sql+=" AND state=?";args.add(state);}if(category!=null){sql+=" AND category=?";args.add(category);}if(shipment!=null){sql+=" AND shipment_id=?";args.add(shipment);}args.add(page*20);var all=jdbc.query(sql+" ORDER BY updated_at DESC,id LIMIT 21 OFFSET ?",JdbcSupportTickets::row,args.toArray());return new Page(all.stream().limit(20).map(Ticket::summary).toList(),page,20,all.size()>20);}
    @Override public Page queue(Access a,int page){admit(a);lock();grant(a.accountId(),"SUPPORT_TRIAGE");if(page<0||page>100000)throw fail("VALIDATION_FAILED");var all=jdbc.query("SELECT * FROM orders.personal_support_ticket WHERE state='NEW' ORDER BY updated_at DESC,id LIMIT 21 OFFSET ?",JdbcSupportTickets::row,page*20);return new Page(all.stream().limit(20).map(Ticket::summary).toList(),page,20,all.size()>20);}
    @Override public Detail assigned(Access a,UUID target){admit(a);lock();grant(a.accountId(),"SUPPORT_HANDLE");var t=ticket(target,null,false);assignedAuthority(a,t);return detail(t,true);}
    private Optional<Stored> stored(Access a,String op,UUID key){id(key);return jdbc.query("SELECT ticket_id,original_version,result_version,fingerprint_key_id,fingerprint FROM orders.support_command_receipt WHERE actor_id=? AND operation=? AND operation_key=?",(r,n)->new Stored(r.getObject(1,UUID.class),r.getLong(2),r.getLong(3),r.getString(4),r.getBytes(5)),a.accountId(),op,key).stream().findFirst();}
    private byte[] hash(String kid,Access a,String op,UUID target,long version,List<String> payload){
        if(!keys.containsKey(kid))throw fail("SUPPORT_UNAVAILABLE");try{var mac=Mac.getInstance("HmacSHA256");mac.init(keys.get(kid));var parts=new ArrayList<>(List.of("orders-personal-support-v1",kid,a.accountId().toString(),op,target.toString(),Long.toString(version)));parts.addAll(payload);for(String part:parts){byte[] bytes=part.getBytes(StandardCharsets.UTF_8);try{mac.update(ByteBuffer.allocate(4).putInt(bytes.length).array());mac.update(bytes);}finally{Arrays.fill(bytes,(byte)0);}}return mac.doFinal();}catch(java.security.GeneralSecurityException unavailable){throw fail("SUPPORT_UNAVAILABLE");}}
    private Optional<Detail> replay(Access a,String op,UUID key,UUID target,long version,List<String> payload,boolean staff){
        var previous=stored(a,op,key);if(previous.isEmpty())return Optional.empty();var r=previous.get();var t=ticket(r.target,staff?null:a.accountId(),true);if(staff){if(op.equals("ASSIGN"))grant(a.accountId(),"SUPPORT_TRIAGE");else assignedAuthority(a,t);}UUID bound=op.equals("CREATE")?key:target;
        byte[] expected=hash(r.kid,a,op,bound,version,payload);boolean equal;try{equal=MessageDigest.isEqual(r.fingerprint,expected);}finally{Arrays.fill(expected,(byte)0);Arrays.fill(r.fingerprint,(byte)0);}if(!equal||r.original!=version||!op.equals("CREATE")&&!r.target.equals(target))throw fail("IDEMPOTENCY_CONFLICT");return Optional.of(op.equals("ASSIGN")?new Detail(t.summary,List.of(),Set.of()):detail(t,staff));
    }
    private void quota(Access a,boolean create,Instant now){String kind=create?"CREATE":"CONVERSATION";Instant start=Instant.ofEpochSecond(Math.floorDiv(now.getEpochSecond(),900)*900);int limit=create?5:20;if(jdbc.update("INSERT INTO orders.support_request_window(owner_id,window_start,kind,count) VALUES (?,?,?,1) ON CONFLICT(owner_id,window_start,kind) DO UPDATE SET count=orders.support_request_window.count+1 WHERE orders.support_request_window.count<?",a.accountId(),Timestamp.from(start),kind,limit)!=1)throw fail("RATE_LIMITED");}
    private void evidence(Access a,Ticket after,String op,UUID key,long original,List<String> payload,String message,String event,boolean staff,Instant time){
        var t=after.summary;one(jdbc.update("INSERT INTO orders.personal_support_message VALUES (?,?,?,?,?,?,?,?)",UUID.randomUUID(),t.id(),after.owner,t.version(),staff?"SUPPORT":"CUSTOMER",message,event,Timestamp.from(time)));
        one(jdbc.update("INSERT INTO orders.support_business_audit(id,actor_id,ticket_id,owner_id,operation,prior_version,new_version,request_id,occurred_at) VALUES (?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),a.accountId(),t.id(),after.owner,op,original,t.version(),a.requestId(),Timestamp.from(time)));
        byte[] fingerprint=hash(activeKey,a,op,op.equals("CREATE")?key:t.id(),original,payload);try{one(jdbc.update("INSERT INTO orders.support_command_receipt VALUES (?,?,?,?,?,?,?,?,?,?)",a.accountId(),op,key,t.id(),after.owner,original,t.version(),activeKey,fingerprint,Timestamp.from(time)));}finally{Arrays.fill(fingerprint,(byte)0);}a.requireCurrent();a.meaningfulAction();
    }
    private Ticket update(Ticket before,String state,Long resolution,boolean review,Instant time){var t=before.summary;one(jdbc.update("UPDATE orders.personal_support_ticket SET state=?,version=version+1,updated_at=?,resolution_version=?,review_pending=? WHERE id=? AND owner_id=? AND version=?",state,Timestamp.from(time),resolution,review,t.id(),before.owner,t.version()));return ticket(t.id(),before.owner,false);}
    @Override public Detail create(Access a,Create input,UUID key){
        admit(a);id(key);if(input==null||input.kind()==null||!Set.of("GENERAL","SHIPMENT").contains(input.kind())||!CATEGORIES.contains(input.category())||input.kind().equals("GENERAL")&&input.shipmentId()!=null||input.kind().equals("SHIPMENT")&&input.shipmentId()==null||input.category().equals("SHIPMENT_TRACE")&&input.shipmentId()==null)throw fail("VALIDATION_FAILED");
        String subject=text(input.subject(),160),message=text(input.message(),4000);var payload=List.of(input.kind(),Objects.toString(input.shipmentId(),""),input.category(),subject,message);lock();var replay=replay(a,"CREATE",key,key,0,payload,false);if(replay.isPresent())return replay.get();if(input.shipmentId()!=null)shipments.get(a,input.shipmentId());Instant time=now();quota(a,true,time);UUID target=UUID.randomUUID();
        one(jdbc.update("INSERT INTO orders.personal_support_ticket(id,owner_id,code,kind,shipment_id,category,subject,state,version,created_at,updated_at) VALUES (?,?,?,?,?,?,?,'NEW',1,?,?)",target,a.accountId(),"AP-T-"+target.toString().replace("-",""),input.kind(),input.shipmentId(),input.category(),subject,Timestamp.from(time),Timestamp.from(time)));
        var after=ticket(target,a.accountId(),false);evidence(a,after,"CREATE",key,0,payload,message,"CREATED",false,time);return detail(after,false);
    }
    @Override public Detail customerCommand(Access a,UUID target,String op,String value,long version,UUID key){
        admit(a);if(!Set.of("REPLY","REQUEST_REVIEW","ACKNOWLEDGE").contains(op)||version<1)throw fail("VALIDATION_FAILED");String message=op.equals("ACKNOWLEDGE")?"Khách hàng xác nhận kết quả giải quyết.":text(value,4000);var payload=List.of(message);lock();var replay=replay(a,op,key,target,version,payload,false);if(replay.isPresent())return replay.get();var before=ticket(target,a.accountId(),true);if(before.summary.version()!=version)throw fail("PRECONDITION_STALE");String state=before.summary.state();
        if(op.equals("REPLY")&&!Set.of("NEW","IN_PROGRESS","NEEDS_INFORMATION").contains(state)||!op.equals("REPLY")&&(!state.equals("RESOLVED")||before.review))throw fail("SUPPORT_STATE_CONFLICT");Instant time=now();quota(a,false,time);
        var after=update(before,op.equals("ACKNOWLEDGE")?"CLOSED":state.equals("NEEDS_INFORMATION")?"IN_PROGRESS":state,before.resolution,op.equals("REQUEST_REVIEW"),time);evidence(a,after,op,key,version,payload,message,op,false,time);return detail(after,false);
    }
    @Override public Receipt receipt(Access a,String op,UUID key){admit(a);if(!CUSTOMER_OPS.contains(op))throw fail("RESOURCE_NOT_FOUND");var r=stored(a,op,key).orElseThrow(()->fail("RESOURCE_NOT_FOUND"));ticket(r.target,a.accountId(),false);return new Receipt(op,r.target,"COMMITTED",r.result);}
    @Override public Summary assign(Access a,UUID target,UUID assignee,long version,UUID key){
        admit(a);id(assignee);if(version<1||!a.eligibleAccount(assignee))throw fail("SUPPORT_AUTHORITY_REQUIRED");lock();grant(a.accountId(),"SUPPORT_TRIAGE");grant(assignee,"SUPPORT_HANDLE");var payload=List.of(assignee.toString());var replay=replay(a,"ASSIGN",key,target,version,payload,true);if(replay.isPresent())return replay.get().ticket();var before=ticket(target,null,true);if(before.summary.version()!=version)throw fail("PRECONDITION_STALE");if(!before.summary.state().equals("NEW"))throw fail("SUPPORT_STATE_CONFLICT");Instant time=now();jdbc.update("UPDATE orders.personal_support_assignment SET state='REVOKED',version=version+1 WHERE ticket_id=? AND state='ACTIVE'",target);one(jdbc.update("INSERT INTO orders.personal_support_assignment VALUES (?,?,?,?,'ACTIVE',1,?)",UUID.randomUUID(),target,before.owner,assignee,Timestamp.from(time)));var after=update(before,"NEW",null,false,time);evidence(a,after,"ASSIGN",key,version,payload,"Yêu cầu đã được phân công xử lý.","ASSIGN",true,time);return after.summary;
    }
    @Override public Detail transition(Access a,UUID target,String action,String value,long version,UUID key){
        admit(a);if(action==null||!Set.of("START","REQUEST_INFORMATION","RESOLVE","REOPEN").contains(action)||version<1)throw fail("VALIDATION_FAILED");String message=text(value,4000);var payload=List.of(action,message);lock();grant(a.accountId(),"SUPPORT_HANDLE");var before=ticket(target,null,true);assignedAuthority(a,before);var replay=replay(a,"TRANSITION",key,target,version,payload,true);if(replay.isPresent())return replay.get();if(before.summary.version()!=version)throw fail("PRECONDITION_STALE");String state=before.summary.state();boolean valid=switch(action){case "START"->state.equals("NEW");case "REQUEST_INFORMATION","RESOLVE"->state.equals("IN_PROGRESS");case "REOPEN"->state.equals("RESOLVED")&&before.review;default->false;};if(!valid)throw fail("SUPPORT_STATE_CONFLICT");String next=switch(action){case "START","REOPEN"->"IN_PROGRESS";case "REQUEST_INFORMATION"->"NEEDS_INFORMATION";default->"RESOLVED";};Instant time=now();var after=update(before,next,next.equals("RESOLVED")?version+1:null,false,time);evidence(a,after,"TRANSITION",key,version,payload,message,action,true,time);return detail(after,true);
    }
}
