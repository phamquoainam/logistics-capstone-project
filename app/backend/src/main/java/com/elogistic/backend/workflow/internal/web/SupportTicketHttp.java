package com.elogistic.backend.workflow.internal.web;
import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.orders.api.SupportTickets;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

/** Both route groups are protected by the existing Customer guard; Orders checks case authority again. */
@RestController @Profile("customer")
public final class SupportTicketHttp {
    private static final String PATH="/api/v1/customer/support-tickets",STAFF="/api/v1/support-operations";
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final CustomerAdmission admission;private final CustomerRequestContext requests;private final SupportTickets tickets;
    public SupportTicketHttp(CustomerAdmission admission,CustomerRequestContext requests,SupportTickets tickets){this.admission=admission;this.requests=requests;this.tickets=tickets;}
    private <T>T execute(Set<UUID> referenced,Function<SupportTickets.Access,T> work){
        var request=requests.current();try{return admission.execute(request.encodedAccess(),request.requestId(),referenced,c->work.apply(new SupportTickets.Access(){public UUID accountId(){requireCurrent();return c.actor().accountId();}public UUID requestId(){requireCurrent();return c.requestId();}public void requireCurrent(){admission.requireCurrent(c);}public void meaningfulAction(){admission.meaningfulAction(c);}public boolean eligibleAccount(UUID id){requireCurrent();return c.subjects().containsKey(id)&&c.subjects().get(id).eligible();}}));}
        catch(SupportTickets.Failure rejected){int status=switch(rejected.code()){case "VALIDATION_FAILED"->400;case "RESOURCE_NOT_FOUND"->404;case "SUPPORT_AUTHORITY_REQUIRED","SUPPORT_ASSIGNMENT_REQUIRED"->403;case "PRECONDITION_STALE","IDEMPOTENCY_CONFLICT","SUPPORT_STATE_CONFLICT"->409;case "RATE_LIMITED"->429;default->503;};throw problem(status,rejected.code());}
    }
    private <T>T execute(Function<SupportTickets.Access,T> work){return execute(Set.of(),work);}
    @GetMapping(PATH) public SupportTickets.Page list(HttpServletRequest r){query(r,Set.of("page","size","state","category","shipmentId"));return execute(a->tickets.list(a,page(r),r.getParameter("state"),r.getParameter("category"),r.getParameter("shipmentId")==null?null:uuid(r.getParameter("shipmentId"))));}
    @GetMapping(PATH+"/{id}") public ResponseEntity<SupportTickets.Detail> get(@PathVariable String id){return response(execute(a->tickets.get(a,uuid(id))));}
    @PostMapping(PATH) public ResponseEntity<SupportTickets.Detail> create(HttpServletRequest r)throws IOException{UUID key=key(r);var v=body(r,Set.of("kind","category","subject","message"),Set.of("kind","shipmentId","category","subject","message"));var input=new SupportTickets.Create(string(v,"kind"),v.has("shipmentId")?uuid(string(v,"shipmentId")):null,string(v,"category"),string(v,"subject"),string(v,"message"));var result=execute(a->tickets.create(a,input,key));return ResponseEntity.created(URI.create(PATH+"/"+result.ticket().id())).eTag(etag(result.ticket().version())).body(result);}
    @PostMapping(PATH+"/{id}/replies") public ResponseEntity<SupportTickets.Detail> reply(@PathVariable String id,HttpServletRequest r)throws IOException{return customerCommand(id,r,"REPLY");}
    @PostMapping(PATH+"/{id}/review-requests") public ResponseEntity<SupportTickets.Detail> review(@PathVariable String id,HttpServletRequest r)throws IOException{return customerCommand(id,r,"REQUEST_REVIEW");}
    @PostMapping(PATH+"/{id}/acknowledgements") public ResponseEntity<SupportTickets.Detail> acknowledge(@PathVariable String id,HttpServletRequest r)throws IOException{return customerCommand(id,r,"ACKNOWLEDGE");}
    private ResponseEntity<SupportTickets.Detail> customerCommand(String id,HttpServletRequest r,String operation)throws IOException{UUID key=key(r),target=uuid(id);long version=version(r);String message;if(operation.equals("ACKNOWLEDGE")){if(r.getInputStream().read()!=-1)throw problem(400,"VALIDATION_FAILED");message=null;}else message=string(body(r,Set.of("message"),Set.of("message")),"message");String value=message;return response(execute(a->tickets.customerCommand(a,target,operation,value,version,key)));}
    @GetMapping("/api/v1/customer/support-receipts/{operation}/{key}") public SupportTickets.Receipt receipt(@PathVariable String operation,@PathVariable String key){return execute(a->tickets.receipt(a,operation,uuid(key)));}
    @GetMapping(STAFF+"/queue") public SupportTickets.Page queue(HttpServletRequest r){query(r,Set.of("page","size"));return execute(a->tickets.queue(a,page(r)));}
    @GetMapping(STAFF+"/cases/{id}") public ResponseEntity<SupportTickets.Detail> assigned(@PathVariable String id){return response(execute(a->tickets.assigned(a,uuid(id))));}
    @PostMapping(STAFF+"/cases/{id}/assignments") public ResponseEntity<SupportTickets.Summary> assign(@PathVariable String id,HttpServletRequest r)throws IOException{UUID key=key(r),target=uuid(id),assignee=uuid(string(body(r,Set.of("assigneeAccountId"),Set.of("assigneeAccountId")),"assigneeAccountId"));long version=version(r);var result=execute(Set.of(assignee),a->tickets.assign(a,target,assignee,version,key));return ResponseEntity.ok().eTag(etag(result.version())).body(result);}
    @PostMapping(STAFF+"/cases/{id}/transitions") public ResponseEntity<SupportTickets.Detail> transition(@PathVariable String id,HttpServletRequest r)throws IOException{UUID key=key(r),target=uuid(id);long version=version(r);var v=body(r,Set.of("action","message"),Set.of("action","message"));return response(execute(a->tickets.transition(a,target,string(v,"action"),string(v,"message"),version,key)));}
    private static ResponseEntity<SupportTickets.Detail> response(SupportTickets.Detail d){return ResponseEntity.ok().eTag(etag(d.ticket().version())).body(d);}
    private static String etag(long version){return "\"v"+version+"\"";}
    private static String string(JsonNode v,String field){if(!v.get(field).isString())throw problem(400,"VALIDATION_FAILED");return v.get(field).asString();}
    private static JsonNode body(HttpServletRequest r,Set<String> required,Set<String> allowed)throws IOException{if(r.getContentType()==null||!r.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");byte[] bytes=r.getInputStream().readNBytes(32769);try{if(bytes.length>32768)throw problem(400,"VALIDATION_FAILED");var v=JSON.readTree(bytes);if(v==null||!v.isObject()||!v.propertyNames().containsAll(required)||!allowed.containsAll(v.propertyNames()))throw problem(400,"VALIDATION_FAILED");return v;}catch(CustomerProfileHttp.HttpProblem rejected){throw rejected;}catch(Exception invalid){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}}
    private static void query(HttpServletRequest r,Set<String> fields){if(!fields.containsAll(r.getParameterMap().keySet())||r.getParameterMap().values().stream().anyMatch(v->v.length!=1)||r.getParameter("size")!=null&&!r.getParameter("size").equals("20"))throw problem(400,"VALIDATION_FAILED");}
    private static int page(HttpServletRequest r){String p=r.getParameter("page");if(p==null)return 0;try{if(!p.matches("0|[1-9][0-9]{0,5}"))throw new IllegalArgumentException();return Integer.parseInt(p);}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static UUID uuid(String v){try{UUID id=UUID.fromString(v);if(!id.toString().equals(v)||id.equals(new UUID(0,0)))throw new IllegalArgumentException();return id;}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
    private static UUID key(HttpServletRequest r){var v=Collections.list(r.getHeaders("Idempotency-Key"));if(v.size()!=1)throw problem(400,"VALIDATION_FAILED");return uuid(v.getFirst());}
    private static long version(HttpServletRequest r){var v=Collections.list(r.getHeaders("If-Match"));if(v.isEmpty())throw problem(428,"PRECONDITION_REQUIRED");try{if(v.size()!=1||!v.getFirst().matches("\"v[1-9][0-9]*\""))throw new IllegalArgumentException();return Long.parseLong(v.getFirst().substring(2,v.getFirst().length()-1));}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}}
}
