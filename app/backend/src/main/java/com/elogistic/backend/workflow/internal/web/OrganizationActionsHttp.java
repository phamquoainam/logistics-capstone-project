package com.elogistic.backend.workflow.internal.web;
import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.merchant.api.customer.BusinessWorkspaces;
import com.elogistic.backend.orders.api.BusinessPreparations;
import com.elogistic.backend.orders.api.OrganizationActions;
import com.elogistic.backend.orders.api.OrganizationActions.*;
import com.elogistic.backend.notifications.api.OrganizationActionNotices;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static com.elogistic.backend.workflow.internal.web.BusinessPreparationHttp.*;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

@RestController @Profile("customer")
public final class OrganizationActionsHttp {
    private static final String BASE="/api/v1/merchant-organizations/{organizationId}";
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final BusinessPreparationHttp admission;private final OrganizationActions actions;private final OrganizationActionNotices notices;
    public OrganizationActionsHttp(BusinessPreparationHttp admission,OrganizationActions actions,OrganizationActionNotices notices){this.admission=admission;this.actions=actions;this.notices=notices;}
    @ModelAttribute public void noStore(jakarta.servlet.http.HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    private record Scope(BusinessPreparations.Access preparation,CustomerAdmission.Context identity) implements OrganizationActions.Access,OrganizationActionNotices.Access {
        public UUID organizationId(){requireCurrent();return scope().context().organizationId();}
        public boolean canReadAllTasks(){requireCurrent();return scope().permits("shipment.draft.read",null);}
        public Set<UUID> readableFacilities(){requireCurrent();return scope().facilities("shipment.draft.read");}
        public UUID actorAccountId(){return preparation.actorAccountId();}public UUID requestId(){return preparation.requestId();}public BusinessWorkspaces.Scope scope(){return preparation.scope();}public void requireCurrent(){preparation.requireCurrent();}public void meaningfulAction(){preparation.meaningfulAction();}public boolean eligibleAccount(UUID account){requireCurrent();return identity.subjects().containsKey(account)&&identity.subjects().get(account).eligible();}
    }
    private <T>T execute(String org,Set<UUID> references,Function<Scope,T> work){return admission.executeWithContext(org,references,(a,c)->work.apply(new Scope(a,c)));}
    private <T>T execute(String org,Function<Scope,T> work){return execute(org,Set.of(),work);}
    private static Map<String,String> params(HttpServletRequest r,Set<String> allowed){var result=new HashMap<String,String>();r.getParameterMap().forEach((k,v)->{if(!allowed.contains(k)||v.length!=1)throw problem(400,"VALIDATION_FAILED");result.put(k,v[0]);});return result;}
    private static JsonNode body(HttpServletRequest r,Set<String> required,Set<String> allowed)throws IOException{if(r.getContentType()==null||!r.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");byte[] bytes=r.getInputStream().readNBytes(16385);try{if(bytes.length>16384)throw problem(400,"VALIDATION_FAILED");var v=JSON.readTree(bytes);shape(v,required,allowed);return v;}catch(CustomerProfileHttp.HttpProblem e){throw e;}catch(RuntimeException bad){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}}
    private static String string(JsonNode v,String key){if(!v.has(key)||!v.get(key).isString())throw problem(400,"VALIDATION_FAILED");return v.get(key).asString();}
    private static Instant due(JsonNode v){if(!v.has("dueAt")||v.get("dueAt").isNull())return null;try{return Instant.parse(string(v,"dueAt"));}catch(RuntimeException bad){throw problem(400,"VALIDATION_FAILED");}}
    private static ResponseEntity<Detail> response(Detail d){return ResponseEntity.ok().eTag("\"v"+d.task().version()+"\"").body(d);}
    private Detail notify(Scope a,Change change){if(!change.replay()&&change.notifyAccountId()!=null){var t=change.result().task();notices.record(a,t.id(),change.eventId(),change.notifyAccountId(),t.facilityId(),change.noticeType(),t.updatedAt());}return change.result();}
    @GetMapping(BASE+"/action-tasks") public Page list(@PathVariable String organizationId,HttpServletRequest r){var q=params(r,Set.of("page","size","targetKind","targetId","facilityId","assignee","state","priority","overdue"));return execute(organizationId,a->actions.list(a,q));}
    @PostMapping(BASE+"/action-tasks") public ResponseEntity<Detail> create(@PathVariable String organizationId,HttpServletRequest r)throws IOException{params(r,Set.of());UUID key=key(r);var v=body(r,Set.of("targetKind","targetId","type","title","details"),Set.of("targetKind","targetId","type","title","details","priority","dueAt"));Input input=new Input(string(v,"targetKind"),uuid(string(v,"targetId")),string(v,"type"),string(v,"title"),string(v,"details"),v.has("priority")?string(v,"priority"):"NORMAL",due(v));Detail d=execute(organizationId,a->notify(a,actions.create(a,input,key)));return ResponseEntity.created(URI.create("/api/v1/merchant-organizations/"+organizationId+"/action-tasks/"+d.task().id())).eTag("\"v"+d.task().version()+"\"").body(d);}
    @GetMapping(BASE+"/action-tasks/{id}") public ResponseEntity<Detail> get(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r){params(r,Set.of());return response(execute(organizationId,a->actions.get(a,uuid(id))));}
    @PutMapping(BASE+"/action-tasks/{id}/assignment") public ResponseEntity<Detail> assign(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{params(r,Set.of());UUID key=key(r),target=uuid(id);long version=version(r);var v=body(r,Set.of("assigneeAccountId","reason"),Set.of("assigneeAccountId","reason"));UUID assignee=v.get("assigneeAccountId").isNull()?null:uuid(string(v,"assigneeAccountId"));String reason=string(v,"reason");return response(execute(organizationId,assignee==null?Set.of():Set.of(assignee),a->notify(a,actions.assign(a,target,assignee,reason,version,key))));}
    @PostMapping(BASE+"/action-tasks/{id}/comments") public ResponseEntity<Detail> comment(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{params(r,Set.of());UUID key=key(r),target=uuid(id);long version=version(r);String message=string(body(r,Set.of("message"),Set.of("message")),"message");return response(execute(organizationId,a->notify(a,actions.comment(a,target,message,version,key))));}
    @PostMapping(BASE+"/action-tasks/{id}/transitions") public ResponseEntity<Detail> transition(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{params(r,Set.of());UUID key=key(r),target=uuid(id);long version=version(r);var v=body(r,Set.of("action","message"),Set.of("action","message"));String op=string(v,"action"),message=string(v,"message");return response(execute(organizationId,a->notify(a,actions.transition(a,target,op,message,version,key))));}
    @PutMapping(BASE+"/action-tasks/{id}/policy") public ResponseEntity<Detail> policy(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{params(r,Set.of());UUID key=key(r),target=uuid(id);long version=version(r);var v=body(r,Set.of("priority","dueAt","reason"),Set.of("priority","dueAt","reason"));String p=string(v,"priority"),reason=string(v,"reason");Instant d=due(v);return response(execute(organizationId,a->notify(a,actions.policy(a,target,p,d,reason,version,key))));}
    @GetMapping(BASE+"/action-task-receipts/{operation}/{key}") public Receipt receipt(@PathVariable String organizationId,@PathVariable String operation,@PathVariable String key,HttpServletRequest r){params(r,Set.of());return execute(organizationId,a->actions.receipt(a,operation,uuid(key)));}
    @GetMapping(BASE+"/action-tasks/{id}/assignment-candidates") public Map<String,Object> candidates(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r){var p=params(r,Set.of("page","size"));int page=number(p.get("page"),0);if(number(p.get("size"),20)!=20)throw problem(400,"VALIDATION_FAILED");UUID target=uuid(id);var found=execute(organizationId,a->{var t=actions.get(a,target).task();if(!t.allowedActions().contains("ASSIGNMENT"))throw new BusinessPreparations.Failure("RESOURCE_NOT_FOUND");return a.scope().actionAssignmentMembers(t.facilityId(),page);});Set<UUID> references=new HashSet<>();found.forEach(m->references.add(m.accountId()));return execute(organizationId,references,a->{var t=actions.get(a,target).task();if(!t.allowedActions().contains("ASSIGNMENT"))throw new BusinessPreparations.Failure("RESOURCE_NOT_FOUND");var rows=a.scope().actionAssignmentMembers(t.facilityId(),page);var items=rows.stream().limit(20).filter(m->a.eligibleAccount(m.accountId())).map(m->Map.of("accountId",m.accountId(),"membershipId",m.membershipId(),"role",m.role(),"displayName",a.identity().subjects().get(m.accountId()).displayName())).toList();return Map.of("items",items,"page",page,"size",20,"hasNext",rows.size()>20);});}
    private boolean readable(Scope a,UUID task){try{actions.get(a,task);return true;}catch(BusinessPreparations.Failure denied){if(!"RESOURCE_NOT_FOUND".equals(denied.getMessage()))throw denied;return false;}}
    @GetMapping(BASE+"/action-notices") public OrganizationActionNotices.Page notices(@PathVariable String organizationId,HttpServletRequest r){var p=params(r,Set.of("page","size"));if(number(p.get("size"),20)!=20)throw problem(400,"VALIDATION_FAILED");return execute(organizationId,a->notices.list(a,number(p.get("page"),0),id->readable(a,id)));}
    @PostMapping(BASE+"/action-notices/{id}/read") public OrganizationActionNotices.Notice read(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{params(r,Set.of());if(r.getInputStream().read()!=-1)throw problem(400,"VALIDATION_FAILED");return execute(organizationId,a->notices.read(a,uuid(id),task->readable(a,task)));}
}
