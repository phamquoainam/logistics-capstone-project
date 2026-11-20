package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.OrganizationClaimIntakes;
import com.elogistic.backend.orders.api.OrganizationSupportTickets;
import com.elogistic.backend.orders.api.OrganizationSupportTickets.Access;
import com.elogistic.backend.orders.api.OrganizationSupportTickets.StaffAccess;
import com.elogistic.backend.orders.internal.domain.ClaimIntakePolicy;
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

/** Document intake only. All work joins current Identity admission and Orders locks. */
public final class JdbcOrganizationClaimIntakes implements OrganizationClaimIntakes {
    private final DataSource source;
    private final JdbcTemplate jdbc;
    private final OrganizationSupportTickets tickets;
    private final Clock clock;
    private final boolean enabled;
    private final boolean evidenceEnabled;
    private final Map<String,SecretKeySpec> keys;
    private final String activeKey;
    private record Stored(UUID id,UUID org,UUID actor,String operation,UUID target,long original,long result,String kid,byte[] digest){}

    public JdbcOrganizationClaimIntakes(DataSource source,OrganizationSupportTickets tickets,Clock clock,boolean enabled,boolean evidenceEnabled,Map<String,byte[]> supplied,String activeKey){
        this.source=Objects.requireNonNull(source);this.jdbc=new JdbcTemplate(source);this.tickets=Objects.requireNonNull(tickets);this.clock=Objects.requireNonNull(clock);this.enabled=enabled;this.evidenceEnabled=evidenceEnabled;
        var copy=new HashMap<String,SecretKeySpec>();
        supplied.forEach((kid,key)->{if(!kid.matches("[A-Za-z0-9_-]{1,64}")||key.length!=32||Arrays.equals(key,new byte[32]))throw new IllegalArgumentException("CLAIM_RECEIPT_KEYS_INVALID");copy.put(kid,new SecretKeySpec(key,"HmacSHA256"));});
        if(!copy.containsKey(activeKey))throw new IllegalArgumentException("CLAIM_RECEIPT_KEYS_REQUIRED");keys=Map.copyOf(copy);this.activeKey=activeKey;
    }
    private static Failure fail(String code){return new Failure(code);}
    private static UUID id(UUID id){if(id==null||id.equals(new UUID(0,0)))throw fail("VALIDATION_FAILED");return id;}
    private static Timestamp ts(Instant time){return Timestamp.from(time);}
    private void admit(Runnable current){
        current.run();if(!enabled)throw fail("CLAIM_INTAKE_DISABLED");
        if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("CLAIM_ADMISSION_TRANSACTION_REQUIRED");
        // Same order as Support/Shipping; serialize revocation, assignment and intake transitions.
        jdbc.query("SELECT pg_advisory_xact_lock(730023,1),pg_advisory_xact_lock(730022,1),pg_advisory_xact_lock(730035,1)",(r,n)->0);
    }
    private static UUID org(Access a){return a.scope().context().organizationId();}
    private Summary row(ResultSet r,int n)throws SQLException{return new Summary(r.getObject("id",UUID.class),r.getObject("organization_id",UUID.class),r.getObject("facility_id",UUID.class),r.getObject("ticket_id",UUID.class),r.getObject("shipment_id",UUID.class),r.getObject("creator_account_id",UUID.class),r.getString("category"),r.getString("state"),r.getLong("version"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant(),"UNASSESSED","BLOCKED");}
    private Summary target(UUID target,UUID org){return jdbc.query("SELECT * FROM orders.organization_claim_intake WHERE id=?"+(org==null?"":" AND organization_id=?"),this::row,org==null?new Object[]{id(target)}:new Object[]{id(target),org}).stream().findFirst().orElseThrow(()->fail("RESOURCE_NOT_FOUND"));}
    private void authority(Access a,Summary s,boolean write){
        a.requireCurrent();if(!a.scope().permits("support.read",s.facilityId())||write&&!a.scope().permits("support.write",s.facilityId()))throw fail("RESOURCE_NOT_FOUND");
        tickets.get(a,s.ticketId());
    }
    private void staff(StaffAccess a,Summary s){a.requireCurrent();tickets.assigned(a,s.ticketId());}
    private Detail detail(Summary s,boolean write,boolean staff){
        var events=jdbc.query("SELECT id,author_kind,operation,message,new_version,occurred_at FROM orders.organization_claim_intake_event WHERE intake_id=? AND organization_id=? ORDER BY new_version",(r,n)->new Event(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4),r.getLong(5),r.getTimestamp(6).toInstant()),s.id(),s.organizationId());
        var actions=new HashSet<String>();if(evidenceEnabled)actions.add("VIEW_EVIDENCE");if(!s.state().equals("CLOSED_WITHOUT_DECISION")){
            if(write){actions.add("SUPPLEMENT");if(evidenceEnabled)actions.add("ATTACH_EVIDENCE");}if(staff){actions.add("CLOSE_WITHOUT_DECISION");if(s.state().equals("RECEIVED"))actions.add("BEGIN_REVIEW");if(Set.of("RECEIVED","UNDER_REVIEW").contains(s.state()))actions.add("REQUEST_INFORMATION");}
        }return new Detail(s,events,actions);
    }
    @Override public Page list(Access a,int page,UUID ticketId,String state){
        admit(a::requireCurrent);if(page<0||page>100000||state!=null&&!ClaimIntakePolicy.STATES.contains(state))throw fail("VALIDATION_FAILED");
        if(ticketId!=null)tickets.get(a,id(ticketId));
        if(!a.scope().permits("support.read",null)&&a.scope().facilities("support.read").isEmpty())throw fail("RESOURCE_NOT_FOUND");
        var args=new ArrayList<Object>(List.of(org(a),a.actorAccountId()));String sql="SELECT c.* FROM orders.organization_claim_intake c WHERE organization_id=? AND orders.organization_support_permission(organization_id,?,facility_id,'support.read')";
        if(ticketId!=null){sql+=" AND ticket_id=?";args.add(ticketId);}if(state!=null){sql+=" AND state=?";args.add(state);}sql+=" ORDER BY updated_at DESC,id LIMIT 21 OFFSET ?";args.add((long)page*20);
        var rows=jdbc.query(sql,this::row,args.toArray());return new Page(rows.stream().limit(20).toList(),page,20,rows.size()>20);
    }
    private void any(Access a,String permission){a.requireCurrent();if(!a.scope().permits(permission,null)&&a.scope().facilities(permission).isEmpty())throw fail("RESOURCE_NOT_FOUND");}
    private String reportScope(){return " FROM orders.organization_claim_intake c WHERE c.organization_id=? AND orders.organization_support_permission(c.organization_id,?,c.facility_id,'support.reports.read')";}
    private Map<String,String> groups(Access a,String column,Set<String> values){var result=new TreeMap<String,String>();values.forEach(v->result.put(v,"0"));jdbc.query("SELECT "+column+",count(*)::TEXT"+reportScope()+" GROUP BY "+column,r->{result.put(r.getString(1),r.getString(2));},org(a),a.actorAccountId());return result;}
    private OrganizationSupportTickets.TimeSamples samples(Access a,boolean closed){String ms=closed?"floor(extract(epoch FROM(c.updated_at-c.created_at))*1000)::BIGINT":"(SELECT floor(extract(epoch FROM(min(e.occurred_at)-c.created_at))*1000)::BIGINT FROM orders.organization_claim_intake_event e WHERE e.intake_id=c.id AND e.author_kind='SUPPORT')";return jdbc.queryForObject("SELECT count(ms)::TEXT,coalesce(sum(ms),0)::TEXT,min(ms)::TEXT,max(ms)::TEXT FROM (SELECT "+ms+" AS ms"+reportScope()+(closed?" AND c.state='CLOSED_WITHOUT_DECISION'":"")+") measured",(r,n)->new OrganizationSupportTickets.TimeSamples(r.getString(1),r.getString(2),r.getString(3),r.getString(4)),org(a),a.actorAccountId());}
    @Override public Report report(Access a){admit(a::requireCurrent);any(a,"support.reports.read");String total=jdbc.queryForObject("SELECT count(*)::TEXT"+reportScope(),String.class,org(a),a.actorAccountId());String clean=jdbc.queryForObject("SELECT count(*)::TEXT FROM orders.organization_claim_evidence e JOIN orders.organization_claim_intake c ON c.id=e.intake_id AND c.organization_id=e.organization_id WHERE e.state='CLEAN' AND c.organization_id=? AND orders.organization_support_permission(c.organization_id,?,c.facility_id,'support.reports.read')",String.class,org(a),a.actorAccountId());return new Report("CLAIM_INTAKE_REPORT_1",org(a),clock.instant(),total,groups(a,"state",ClaimIntakePolicy.STATES),groups(a,"category",ClaimIntakePolicy.CATEGORIES),clean,samples(a,false),samples(a,true),"UNPUBLISHED","BLOCKED");}
    @Override public Notices notices(Access a,int page){admit(a::requireCurrent);any(a,"support.read");if(page<0||page>100000)throw fail("VALIDATION_FAILED");var rows=jdbc.query("SELECT n.*,c.ticket_id FROM orders.organization_claim_notice n JOIN orders.organization_claim_intake c ON c.id=n.intake_id AND c.organization_id=n.organization_id WHERE n.organization_id=? AND n.recipient_account_id=? AND orders.organization_support_permission(c.organization_id,?,c.facility_id,'support.read') ORDER BY n.created_at DESC,n.id LIMIT 21 OFFSET ?",(r,n)->new Notice(r.getObject("id",UUID.class),r.getObject("intake_id",UUID.class),r.getObject("ticket_id",UUID.class),r.getObject("event_id",UUID.class),r.getString("type"),r.getTimestamp("created_at").toInstant()),org(a),a.actorAccountId(),a.actorAccountId(),(long)page*20);return new Notices(rows.stream().limit(20).toList(),page,rows.size()>20);}
    @Override public Detail get(Access a,UUID id){admit(a::requireCurrent);var s=target(id,org(a));authority(a,s,false);return detail(s,a.scope().permits("support.write",s.facilityId()),false);}
    @Override public Set<UUID> references(Access a,UUID id){admit(a::requireCurrent);var s=target(id,org(a));authority(a,s,false);var ids=new HashSet<>(tickets.references(a,s.ticketId()));ids.add(s.creatorAccountId());return Set.copyOf(ids);}
    private Stored stored(ResultSet r,int n)throws SQLException{return new Stored(r.getObject("id",UUID.class),r.getObject("organization_id",UUID.class),r.getObject("actor_account_id",UUID.class),r.getString("operation"),r.getObject("intake_id",UUID.class),r.getLong("original_version"),r.getLong("result_version"),r.getString("fingerprint_key_id"),r.getBytes("fingerprint"));}
    private Optional<Stored> stored(UUID org,UUID actor,String op,UUID key){return jdbc.query("SELECT * FROM orders.organization_claim_intake_receipt WHERE organization_id=? AND actor_account_id=? AND operation=? AND operation_key=?",this::stored,org,actor,op,id(key)).stream().findFirst();}
    private byte[] hash(String kid,UUID org,UUID actor,String op,UUID target,long version,List<String> payload){
        if(!keys.containsKey(kid))throw fail("CLAIM_INTAKE_UNAVAILABLE");try{
            var mac=Mac.getInstance("HmacSHA256");mac.init(keys.get(kid));var parts=new ArrayList<>(List.of("orders-claim-intake-v1",kid,org.toString(),actor.toString(),op,target.toString(),Long.toString(version)));parts.addAll(payload);
            for(String part:parts){byte[] b=part.getBytes(StandardCharsets.UTF_8);try{mac.update(ByteBuffer.allocate(4).putInt(b.length).array());mac.update(b);}finally{Arrays.fill(b,(byte)0);}}return mac.doFinal();
        }catch(java.security.GeneralSecurityException ex){throw fail("CLAIM_INTAKE_UNAVAILABLE");}
    }
    private void matches(Stored r,UUID target,long version,List<String> payload){byte[] digest=hash(r.kid,r.org,r.actor,r.operation,target,version,payload);try{if(r.original!=version||!MessageDigest.isEqual(r.digest,digest))throw fail("IDEMPOTENCY_CONFLICT");}finally{Arrays.fill(digest,(byte)0);Arrays.fill(r.digest,(byte)0);}}
    private void record(Summary s,UUID actor,UUID request,String op,UUID key,long prior,String priorState,String message,List<String> payload,boolean staff,java.util.function.Predicate<UUID> eligible){
        UUID event=UUID.randomUUID();jdbc.update("INSERT INTO orders.organization_claim_intake_event VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",event,s.id(),s.organizationId(),actor,staff?"SUPPORT":"MERCHANT",op,message,prior,s.version(),priorState,s.state(),ts(s.updatedAt()));
        byte[] digest=hash(activeKey,s.organizationId(),actor,op,op.equals("CREATE")?key:s.id(),prior,payload);
        try{jdbc.update("INSERT INTO orders.organization_claim_intake_receipt VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),s.organizationId(),actor,op,key,s.id(),prior,s.version(),activeKey,digest,request,ts(s.updatedAt()));}
        finally{Arrays.fill(digest,(byte)0);}
        if(!op.equals("CREATE")){
            String type=op.equals("REQUEST_INFORMATION")?"NEEDS_INFORMATION":staff?"REVIEW":"REPLY";var recipients=new HashSet<UUID>();recipients.add(s.creatorAccountId());
            jdbc.query("SELECT assignee_account_id FROM orders.organization_support_assignment WHERE ticket_id=? AND organization_id=? AND assignee_account_id IS NOT NULL",(r,n)->r.getObject(1,UUID.class),s.ticketId(),s.organizationId()).forEach(recipients::add);
            for(UUID recipient:recipients)if(!recipient.equals(actor)&&eligible.test(recipient))jdbc.update("INSERT INTO orders.organization_claim_notice SELECT ?,?,?,?,?,?,? WHERE orders.organization_support_permission(?,?,?,'support.read') AND EXISTS(SELECT 1 FROM orders.organization_support_notification_preference p WHERE p.organization_id=? AND p.account_id=? AND p.enabled AND ?=ANY(p.event_types)) ON CONFLICT(event_id,recipient_account_id) DO NOTHING",UUID.randomUUID(),s.id(),s.organizationId(),event,recipient,type,ts(s.updatedAt()),s.organizationId(),recipient,s.facilityId(),s.organizationId(),recipient,type);
        }
    }
    private static String text(String raw){try{return ClaimIntakePolicy.narrative(raw);}catch(IllegalArgumentException ex){throw fail("VALIDATION_FAILED");}}
    @Override public Detail create(Access a,Create input,UUID key){
        admit(a::requireCurrent);id(key);if(input==null)throw fail("VALIDATION_FAILED");String category;try{category=ClaimIntakePolicy.category(input.category());}catch(IllegalArgumentException|NullPointerException ex){throw fail("VALIDATION_FAILED");}String message=text(input.narrative());
        var ticket=tickets.get(a,id(input.ticketId()));if(!ticket.kind().equals("SHIPMENT")||ticket.shipmentId()==null)throw fail("CLAIM_SHIPMENT_TICKET_REQUIRED");if(!a.scope().permits("support.write",ticket.facilityId()))throw fail("RESOURCE_NOT_FOUND");
        var payload=List.of(ticket.id().toString(),category,message);var previous=stored(org(a),a.actorAccountId(),"CREATE",key);
        if(previous.isPresent()){var r=previous.get();authority(a,target(r.target,org(a)),true);matches(r,key,0,payload);return get(a,r.target);}
        if(jdbc.queryForObject("SELECT count(*) FROM orders.organization_claim_intake WHERE organization_id=? AND shipment_id=? AND category=? AND state<>'CLOSED_WITHOUT_DECISION'",Long.class,org(a),ticket.shipmentId(),category)>0)throw fail("CLAIM_ALREADY_OPEN");
        Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);UUID target=UUID.randomUUID();jdbc.update("INSERT INTO orders.organization_claim_intake VALUES(?,?,?,?,?,?,?,'RECEIVED',1,?,?)",target,org(a),ticket.facilityId(),ticket.id(),ticket.shipmentId(),a.actorAccountId(),category,ts(now),ts(now));
        var s=target(target,org(a));record(s,a.actorAccountId(),a.requestId(),"CREATE",key,0,null,message,payload,false,a::eligibleAccount);a.requireCurrent();a.meaningfulAction();return get(a,target);
    }
    private Summary update(Summary s,String state,long expected){if(s.version()!=expected)throw fail("PRECONDITION_STALE");Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);if(now.isBefore(s.updatedAt()))now=s.updatedAt();if(jdbc.update("UPDATE orders.organization_claim_intake SET state=?,version=version+1,updated_at=? WHERE id=? AND version=?",state,ts(now),s.id(),expected)!=1)throw fail("PRECONDITION_STALE");return target(s.id(),s.organizationId());}
    @Override public Detail supplement(Access a,UUID id,String narrative,long version,UUID key){
        admit(a::requireCurrent);var s=target(id,org(a));authority(a,s,true);String message=text(narrative);var payload=List.of(message);var prior=stored(org(a),a.actorAccountId(),"SUPPLEMENT",key);
        if(prior.isPresent()){if(!prior.get().target.equals(id))throw fail("IDEMPOTENCY_CONFLICT");matches(prior.get(),id,version,payload);return get(a,id);}
        if(s.version()!=version)throw fail("PRECONDITION_STALE");String state;try{state=ClaimIntakePolicy.supplement(s.state());}catch(IllegalArgumentException ex){throw fail("CLAIM_STATE_CONFLICT");}
        var result=update(s,state,version);record(result,a.actorAccountId(),a.requestId(),"SUPPLEMENT",key,version,s.state(),message,payload,false,a::eligibleAccount);a.requireCurrent();a.meaningfulAction();return get(a,id);
    }
    @Override public Receipt receipt(Access a,String operation,UUID key){admit(a::requireCurrent);if(!Set.of("CREATE","SUPPLEMENT").contains(operation))throw fail("VALIDATION_FAILED");var r=stored(org(a),a.actorAccountId(),operation,key).orElseThrow(()->fail("RESOURCE_NOT_FOUND"));authority(a,target(r.target,org(a)),true);return new Receipt(r.id,r.target,"COMMITTED",r.result);}
    @Override public Detail assigned(StaffAccess a,UUID id){admit(a::requireCurrent);var s=target(id,null);staff(a,s);return detail(s,false,true);}
    @Override public Page assignedIntakes(StaffAccess a,UUID ticketId,int page){admit(a::requireCurrent);if(page<0||page>100000)throw fail("VALIDATION_FAILED");var ticket=tickets.assigned(a,id(ticketId));var rows=jdbc.query("SELECT * FROM orders.organization_claim_intake WHERE ticket_id=? AND organization_id=? ORDER BY updated_at DESC,id LIMIT 21 OFFSET ?",this::row,ticketId,ticket.organizationId(),(long)page*20);return new Page(rows.stream().limit(20).toList(),page,20,rows.size()>20);}
    @Override public Set<UUID> staffReferences(StaffAccess a,UUID id){admit(a::requireCurrent);var s=target(id,null);staff(a,s);var ids=new HashSet<>(tickets.staffReferences(a,s.ticketId(),false));ids.add(s.creatorAccountId());return Set.copyOf(ids);}
    @Override public Detail staffCommand(StaffAccess a,UUID id,String action,String input,long version,UUID key){
        admit(a::requireCurrent);if(action==null||!Set.of("BEGIN_REVIEW","REQUEST_INFORMATION","CLOSE_WITHOUT_DECISION").contains(action))throw fail("VALIDATION_FAILED");var s=target(id,null);staff(a,s);String message=text(input);var payload=List.of(message);var prior=stored(s.organizationId(),a.accountId(),action,key);
        if(prior.isPresent()){if(!prior.get().target.equals(id))throw fail("IDEMPOTENCY_CONFLICT");matches(prior.get(),id,version,payload);return assigned(a,id);}
        if(s.version()!=version)throw fail("PRECONDITION_STALE");String state;try{state=ClaimIntakePolicy.staffTransition(s.state(),action);}catch(IllegalArgumentException ex){throw fail("CLAIM_STATE_CONFLICT");}
        var result=update(s,state,version);record(result,a.accountId(),a.requestId(),action,key,version,s.state(),message,payload,true,a::eligibleAccount);a.requireCurrent();a.meaningfulAction();return assigned(a,id);
    }
}
