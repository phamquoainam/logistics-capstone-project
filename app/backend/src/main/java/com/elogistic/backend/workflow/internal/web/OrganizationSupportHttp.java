package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.orders.api.OrganizationSupportTickets;
import com.elogistic.backend.orders.api.OrganizationSupportTickets.*;
import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.identity.api.staff.*;
import com.elogistic.backend.merchant.api.customer.BusinessWorkspaces;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

@RestController @Profile("customer")
public final class OrganizationSupportHttp {
    private static final String BASE="/api/v1/merchant-organizations/{organizationId}",TICKETS=BASE+"/support-tickets",STAFF="/api/v1/support-operations";
    private static final Set<String> FILTERS=Set.of("page","size","state","category","shipmentId","facilityId","creatorId","assignee","requestedPriority","from","until","sort");
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final BusinessPreparationHttp business;private final OrganizationSupportTickets tickets;private final CustomerMemberNames names;
    private final StaffAdmission staffAdmission;private final StaffRequestContext staffRequests;
    /** Existing Merchant-only callers retain their API; absent Staff assembly is fail-closed. */
    public OrganizationSupportHttp(BusinessPreparationHttp business,OrganizationSupportTickets tickets,CustomerAdmission admission,CustomerRequestContext requests,CustomerMemberNames names){this(business,tickets,names,null,null);}
    @org.springframework.beans.factory.annotation.Autowired
    public OrganizationSupportHttp(BusinessPreparationHttp business,OrganizationSupportTickets tickets,CustomerMemberNames names,StaffAdmission staffAdmission,StaffRequestContext staffRequests){this.business=business;this.tickets=tickets;this.names=names;this.staffAdmission=staffAdmission;this.staffRequests=staffRequests;}
    @ModelAttribute public void noStore(HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    private <T>T errors(java.util.function.Supplier<T> action){try{return action.get();}catch(Failure rejected){throw problem(switch(rejected.getMessage()){case "VALIDATION_FAILED"->400;case "RESOURCE_NOT_FOUND"->404;case "SUPPORT_AUTHORITY_REQUIRED","SUPPORT_ASSIGNMENT_REQUIRED"->403;case "PRECONDITION_STALE","IDEMPOTENCY_CONFLICT","SUPPORT_STATE_CONFLICT","ASSIGNMENT_NO_CHANGE"->409;case "RATE_LIMITED"->429;default->503;},rejected.getMessage());}}
    <T>T execute(String org,Set<UUID> references,Function<Access,T> work){return errors(()->business.executeWithContext(org,references,(a,c)->work.apply(new Access(){
        public UUID actorAccountId(){return a.actorAccountId();}public UUID requestId(){return a.requestId();}public BusinessWorkspaces.Scope scope(){return a.scope();}
        public void requireCurrent(){a.requireCurrent();}public void meaningfulAction(){a.meaningfulAction();}
        public boolean eligibleAccount(UUID id){requireCurrent();return c.subjects().containsKey(id)&&c.subjects().get(id).eligible();}
        public Map<UUID,String> displayNames(Set<UUID> ids){return names.read(c,ids);}
        public Set<UUID> eligibleAccounts(Set<UUID> ids){return names.eligibleAccounts(c,ids);}
    })));}
    <T>T execute(String org,Function<Access,T> work){return execute(org,Set.of(),work);}
    private <T>T target(String org,UUID target,Set<UUID> additional,Function<Access,T> work){var refs=new HashSet<>(execute(org,a->tickets.references(a,target)));refs.addAll(additional);return execute(org,refs,work);}
    <T>T staff(Set<UUID> refs,Function<StaffAccess,T> work){return errors(()->{if(staffAdmission==null||staffRequests==null)throw problem(503,"STAFF_UNAVAILABLE");var r=staffRequests.current();return staffAdmission.execute(r.encodedAccess(),r.requestId(),refs,c->work.apply(new StaffAccess(){
        public UUID accountId(){requireCurrent();return c.actor().accountId();}public UUID requestId(){requireCurrent();return c.requestId();}public void requireCurrent(){staffAdmission.requireCurrent(c);}public void meaningfulAction(){staffAdmission.meaningfulAction(c);}
        public boolean eligibleAccount(UUID id){requireCurrent();return c.subjects().containsKey(id)&&c.subjects().get(id).eligible();}
    }));});}
    private <T>T staffTarget(UUID id,boolean triage,Set<UUID> additional,Function<StaffAccess,T> work){var refs=new HashSet<>(staff(Set.of(),a->tickets.staffReferences(a,id,triage)));refs.addAll(additional);return staff(refs,work);}
    private static UUID uuid(String value){try{UUID id=UUID.fromString(value);if(!id.toString().equals(value)||id.equals(new UUID(0,0)))throw new IllegalArgumentException();return id;}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static void query(HttpServletRequest r,Set<String> allowed){if(!allowed.containsAll(r.getParameterMap().keySet())||r.getParameterMap().values().stream().anyMatch(v->v.length!=1))throw problem(400,"VALIDATION_FAILED");}
    private static int number(String raw,int fallback){if(raw==null)return fallback;try{if(!raw.matches("0|[1-9][0-9]{0,5}"))throw new IllegalArgumentException();int n=Integer.parseInt(raw);if(n>100000)throw new IllegalArgumentException();return n;}catch(RuntimeException bad){throw problem(400,"VALIDATION_FAILED");}}
    private static int page(HttpServletRequest r){query(r,Set.of("page","size"));if(number(r.getParameter("size"),20)!=20)throw problem(400,"VALIDATION_FAILED");return number(r.getParameter("page"),0);}
    private static LocalDate date(String raw){if(raw==null)return null;try{var d=LocalDate.parse(raw);if(!d.toString().equals(raw))throw new IllegalArgumentException();return d;}catch(RuntimeException bad){throw problem(400,"VALIDATION_FAILED");}}
    private static Filters filters(Map<String,String> m){int size=number(m.get("size"),20);if(size!=20)throw problem(400,"VALIDATION_FAILED");return new Filters(number(m.get("page"),0),size,m.get("state"),m.get("category"),optionalId(m.get("shipmentId")),optionalId(m.get("facilityId")),optionalId(m.get("creatorId")),m.get("assignee"),m.get("requestedPriority"),date(m.get("from")),date(m.get("until")),m.getOrDefault("sort","UPDATED"));}
    private static UUID optionalId(String raw){return raw==null?null:uuid(raw);}
    private static Map<String,String> params(HttpServletRequest r){var result=new HashMap<String,String>();r.getParameterMap().forEach((k,v)->result.put(k,v[0]));return result;}
    private static JsonNode body(HttpServletRequest r,Set<String> required,Set<String> allowed)throws IOException{
        query(r,Set.of());if(r.getContentType()==null||!r.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");byte[] bytes=r.getInputStream().readNBytes(32769);
        try{if(bytes.length>32768)throw problem(400,"VALIDATION_FAILED");var v=JSON.readTree(bytes);if(v==null||!v.isObject()||!v.propertyNames().containsAll(required)||!allowed.containsAll(v.propertyNames()))throw problem(400,"VALIDATION_FAILED");return v;}catch(CustomerProfileHttp.HttpProblem bad){throw bad;}catch(Exception bad){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}
    }
    private static String string(JsonNode v,String field,boolean nullable){var n=v.get(field);if(nullable&&(n==null||n.isNull()))return null;if(n==null||!n.isString())throw problem(400,"VALIDATION_FAILED");return n.asString();}
    private static UUID key(HttpServletRequest r){var values=Collections.list(r.getHeaders("Idempotency-Key"));if(values.size()!=1)throw problem(400,"VALIDATION_FAILED");return uuid(values.getFirst());}
    private static long version(HttpServletRequest r,String prefix,boolean zero){var values=Collections.list(r.getHeaders("If-Match"));if(values.isEmpty())throw problem(428,"PRECONDITION_REQUIRED");try{if(values.size()!=1||!values.getFirst().matches("\""+prefix+(zero?"(0|[1-9][0-9]*)":"[1-9][0-9]*")+"\""))throw new IllegalArgumentException();return Long.parseLong(values.getFirst().substring(2,values.getFirst().length()-1));}catch(RuntimeException bad){throw problem(400,"VALIDATION_FAILED");}}
    private static ResponseEntity<Detail> detail(Detail d){return ResponseEntity.ok().eTag("\"v"+d.version()+"\"").body(d);}
    @GetMapping(TICKETS) public Page list(@PathVariable String organizationId,HttpServletRequest r){query(r,FILTERS);return execute(organizationId,a->tickets.list(a,filters(params(r)),null));}
    @PostMapping(TICKETS+"/search") public Page search(@PathVariable String organizationId,HttpServletRequest r)throws IOException{var allowed=new HashSet<>(FILTERS);allowed.add("search");var v=body(r,Set.of("search"),allowed);var m=new HashMap<String,String>();for(var e:v.properties()){if(e.getKey().equals("search"))continue;if(e.getValue().isNull())continue;if(Set.of("page","size").contains(e.getKey())){if(!e.getValue().isIntegralNumber()||!e.getValue().canConvertToInt())throw problem(400,"VALIDATION_FAILED");m.put(e.getKey(),Integer.toString(e.getValue().asInt()));}else m.put(e.getKey(),string(v,e.getKey(),false));}var q=filters(m);String needle=string(v,"search",false);return execute(organizationId,a->tickets.list(a,q,needle));}
    @PostMapping(TICKETS) public ResponseEntity<Detail> create(@PathVariable String organizationId,HttpServletRequest r)throws IOException{var v=body(r,Set.of("kind","category","subject","message","requestedPriority"),Set.of("kind","shipmentId","facilityId","category","subject","message","requestedPriority"));var input=new Create(string(v,"kind",false),optionalId(string(v,"shipmentId",true)),optionalId(string(v,"facilityId",true)),string(v,"category",false),string(v,"subject",false),string(v,"message",false),string(v,"requestedPriority",false));UUID key=key(r);var d=execute(organizationId,a->tickets.create(a,input,key));return ResponseEntity.created(URI.create("/api/v1/merchant-organizations/"+organizationId+"/support-tickets/"+d.id())).eTag("\"v"+d.version()+"\"").body(d);}
    @GetMapping(TICKETS+"/{id}") public ResponseEntity<Detail> get(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r){query(r,Set.of());return detail(execute(organizationId,a->tickets.get(a,uuid(id))));}
    private ResponseEntity<Detail> command(String org,String raw,HttpServletRequest r,String op)throws IOException{var v=body(r,op.equals("ACKNOWLEDGE")?Set.of():Set.of("message"),op.equals("ACKNOWLEDGE")?Set.of():Set.of("message"));String msg=op.equals("ACKNOWLEDGE")?null:string(v,"message",false);UUID target=uuid(raw),key=key(r);long expected=version(r,"v",false);return detail(target(org,target,Set.of(),a->tickets.command(a,target,op,msg,expected,key)));}
    @PostMapping(TICKETS+"/{id}/replies") public ResponseEntity<Detail> reply(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{return command(organizationId,id,r,"REPLY");}
    @PostMapping(TICKETS+"/{id}/review-requests") public ResponseEntity<Detail> review(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{return command(organizationId,id,r,"REQUEST_REVIEW");}
    @PostMapping(TICKETS+"/{id}/acknowledgements") public ResponseEntity<Detail> acknowledge(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{return command(organizationId,id,r,"ACKNOWLEDGE");}
    @GetMapping(BASE+"/support-receipts/{operation}/{key}") public Receipt receipt(@PathVariable String organizationId,@PathVariable String operation,@PathVariable String key,HttpServletRequest r){query(r,Set.of());return execute(organizationId,a->tickets.receipt(a,operation,uuid(key)));}
    @GetMapping(TICKETS+"/{id}/assignment") public ResponseEntity<Assignment> assignment(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r){query(r,Set.of());var d=execute(organizationId,a->tickets.assignment(a,uuid(id)));return ResponseEntity.ok().eTag("\"a"+d.version()+"\"").body(d);}
    @GetMapping(TICKETS+"/{id}/assignment-candidates") public MemberPage candidates(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r){int page=page(r);UUID target=uuid(id);return target(organizationId,target,Set.of(),a->tickets.candidates(a,target,page));}
    @PutMapping(TICKETS+"/{id}/assignment") public ResponseEntity<Assignment> assign(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{var v=body(r,Set.of("assigneeAccountId"),Set.of("assigneeAccountId"));UUID assignee=optionalId(string(v,"assigneeAccountId",true)),target=uuid(id),key=key(r);long expected=version(r,"a",true);var d=target(organizationId,target,assignee==null?Set.of():Set.of(assignee),a->tickets.assign(a,target,assignee,expected,key));return ResponseEntity.ok().eTag("\"a"+d.version()+"\"").body(d);}
    @GetMapping(TICKETS+"/{id}/internal-history") public HistoryPage history(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r){int page=page(r);return execute(organizationId,a->tickets.history(a,uuid(id),page));}
    @GetMapping(BASE+"/support-reports") public Report report(@PathVariable String organizationId,HttpServletRequest r){var allowed=new HashSet<>(FILTERS);allowed.removeAll(Set.of("page","size","sort"));query(r,allowed);var f=filters(params(r));return execute(organizationId,a->tickets.report(a,f));}
    @GetMapping(BASE+"/support-notification-preferences") public ResponseEntity<Preferences> preferences(@PathVariable String organizationId,HttpServletRequest r){query(r,Set.of());var d=execute(organizationId,tickets::preferences);return ResponseEntity.ok().eTag("\"n"+d.version()+"\"").body(d);}
    @PutMapping(BASE+"/support-notification-preferences") public ResponseEntity<Preferences> preferencesWrite(@PathVariable String organizationId,HttpServletRequest r)throws IOException{var v=body(r,Set.of("enabled","eventTypes"),Set.of("enabled","eventTypes"));if(!v.get("enabled").isBoolean()||!v.get("eventTypes").isArray())throw problem(400,"VALIDATION_FAILED");var events=new ArrayList<String>();for(var e:v.get("eventTypes")){if(!e.isString())throw problem(400,"VALIDATION_FAILED");events.add(e.asString());}UUID key=key(r);long expected=version(r,"n",true);var d=execute(organizationId,a->tickets.preferences(a,v.get("enabled").asBoolean(),events,expected,key));return ResponseEntity.ok().eTag("\"n"+d.version()+"\"").body(d);}
    @GetMapping(BASE+"/support-notices") public NoticePage notices(@PathVariable String organizationId,HttpServletRequest r){int page=page(r);return execute(organizationId,a->tickets.notices(a,page));}
    @PostMapping(BASE+"/support-notices/{id}/read") public ResponseEntity<Notice> read(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{body(r,Set.of(),Set.of());UUID target=uuid(id),key=key(r);long expected=version(r,"v",false);var n=execute(organizationId,a->tickets.read(a,target,expected,key));return ResponseEntity.ok().eTag("\"v"+n.version()+"\"").body(n);}
    @GetMapping(STAFF+"/organization-queue") public Page queue(HttpServletRequest r){query(r,Set.of("organizationId","page","size"));if(number(r.getParameter("size"),20)!=20)throw problem(400,"VALIDATION_FAILED");UUID org=uuid(r.getParameter("organizationId"));int page=number(r.getParameter("page"),0);return staff(Set.of(),a->tickets.queue(a,org,page));}
    @GetMapping(STAFF+"/organization-cases/{id}") public ResponseEntity<Detail> caseDetail(@PathVariable String id,HttpServletRequest r){query(r,Set.of());return detail(staff(Set.of(),a->tickets.assigned(a,uuid(id))));}
    @PostMapping(STAFF+"/organization-cases/{id}/assignments") public ResponseEntity<Summary> carrierAssign(@PathVariable String id,HttpServletRequest r)throws IOException{var v=body(r,Set.of("assigneeAccountId"),Set.of("assigneeAccountId"));UUID assignee=uuid(string(v,"assigneeAccountId",false)),target=uuid(id),key=key(r);long expected=version(r,"v",false);var d=staffTarget(target,true,Set.of(assignee),a->tickets.carrierAssign(a,target,assignee,expected,key));return ResponseEntity.ok().eTag("\"v"+d.version()+"\"").body(d);}
    private ResponseEntity<Detail> carrier(String raw,HttpServletRequest r,boolean reply)throws IOException{var fields=reply?Set.of("message"):Set.of("action","message");var v=body(r,fields,fields);String msg=string(v,"message",false),action=reply?null:string(v,"action",false);UUID target=uuid(raw),key=key(r);long expected=version(r,"v",false);return detail(staffTarget(target,false,Set.of(),a->tickets.carrierCommand(a,target,reply?"CARRIER_REPLY":"CARRIER_TRANSITION",action,msg,expected,key)));}
    @PostMapping(STAFF+"/organization-cases/{id}/replies") public ResponseEntity<Detail> carrierReply(@PathVariable String id,HttpServletRequest r)throws IOException{return carrier(id,r,true);}
    @PostMapping(STAFF+"/organization-cases/{id}/transitions") public ResponseEntity<Detail> carrierTransition(@PathVariable String id,HttpServletRequest r)throws IOException{return carrier(id,r,false);}
    @GetMapping(STAFF+"/organization-receipts/{operation}/{key}") public Receipt carrierReceipt(@PathVariable String operation,@PathVariable String key,HttpServletRequest r){query(r,Set.of());return staff(Set.of(),a->tickets.carrierReceipt(a,operation,uuid(key)));}
}
