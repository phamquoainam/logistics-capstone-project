package com.elogistic.backend.workflow.internal.web;
import com.elogistic.backend.merchant.api.customer.OrganizationTeam;
import com.elogistic.backend.merchant.api.customer.OrganizationTeam.*;
import com.elogistic.backend.identity.api.customer.*;
import jakarta.servlet.http.*;
import java.io.IOException;
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

/** OTA read slice; no anonymous directory and no role-label authority. Commands are added only with DB/Identity evidence. */
@RestController @Profile("customer")
public final class OrganizationTeamHttp {
    private static final String BASE="/api/v1/merchant-organizations/{organizationId}/team";
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final BusinessPreparationHttp admission;private final OrganizationTeam team;private final CustomerMemberNames names;private final CustomerReauthentication reauthentication;private final CustomerMembershipInvitations memberships;private final CustomerAdmission customers;private final CustomerRequestContext requests;
    public OrganizationTeamHttp(BusinessPreparationHttp admission,OrganizationTeam team,CustomerMemberNames names,CustomerReauthentication reauthentication,CustomerMembershipInvitations memberships,CustomerAdmission customers,CustomerRequestContext requests){this.admission=admission;this.team=team;this.names=names;this.reauthentication=reauthentication;this.memberships=memberships;this.customers=customers;this.requests=requests;}
    @ModelAttribute public void noStore(HttpServletResponse r){r.setHeader("Cache-Control","no-store");}
    private <T>T execute(String org,Function<Access,T> work){return execute(org,Set.of(),work);}
    private <T>T target(String org,UUID member,Function<Access,T> work){var references=execute(org,a->team.references(a,member));return execute(org,references,work);}
    private <T>T execute(String org,Set<UUID> references,Function<Access,T> work){
        try{return admission.executeWithContext(org,references,(a,c)->work.apply(new Access(){
            public UUID actorAccountId(){return a.actorAccountId();}public UUID requestId(){return a.requestId();}
            public com.elogistic.backend.merchant.api.customer.BusinessWorkspaces.Scope scope(){return a.scope();}
            public void requireCurrent(){a.requireCurrent();}public void meaningfulAction(){a.meaningfulAction();}
            public Map<UUID,String> displayNames(Set<UUID> ids){requireCurrent();return names.read(c,ids);}
            public boolean eligibleAccount(UUID id){return c.subjects().containsKey(id)&&c.subjects().get(id).eligible();}
            public void reauthenticate(UUID proof){reauthentication.consume(c,proof);}
            private CustomerMembershipInvitations.Bound bound(){requireCurrent();return new CustomerMembershipInvitations.Bound(c,a.scope().context().organizationId());}
            private ProtectedDelivery protectedDelivery(CustomerMembershipInvitations.ProtectedInvitation p){return new ProtectedDelivery(p.recipientDigest(),p.protectedRecipient(),p.proofDigest());}
            public byte[] recipientDigest(String recipient){return memberships.recipientDigest(bound(),recipient);}
            public ProtectedDelivery enqueue(UUID id,long version,String recipient,byte[] proof,java.time.Instant expires){return protectedDelivery(memberships.enqueueMembershipInvitation(bound(),id,version,recipient,proof,expires));}
            public ProtectedDelivery reenqueue(UUID issuer,UUID id,long version,byte[] contact,byte[] proof,java.time.Instant expires){requireCurrent();return protectedDelivery(memberships.resendMembershipInvitation(new CustomerMembershipInvitations.Bound(c,a.scope().context().organizationId(),issuer),id,version,contact,proof,expires));}
            public String maskedRecipient(UUID id,byte[] contact){return memberships.maskedRecipient(bound(),id,contact);}
            public void revokeInvitationProof(UUID id){memberships.revoke(bound(),id);}
            public String deliveryState(UUID id,long version){return memberships.deliveryState(bound(),id,version);}
        }));}catch(com.elogistic.backend.identity.api.IdentityCommandFailure rejected){if(rejected.code()==com.elogistic.backend.identity.api.IdentityCommandFailure.Code.TOKEN_INVALID_OR_EXPIRED)throw problem(422,"TOKEN_INVALID_OR_EXPIRED");throw rejected;}
        catch(CustomerMembershipInvitations.Failure rejected){throw problem(switch(rejected.getMessage()){case "VALIDATION_FAILED"->400;case "TEAM_INVITATION_INVALID_OR_EXPIRED"->422;default->503;},rejected.getMessage());}
        catch(OrganizationTeam.Failure rejected){throw problem(switch(rejected.getMessage()){
            case "VALIDATION_FAILED"->400;case "AUTHORITY_REQUIRED","TEAM_DELEGATION_DENIED"->403;case "RESOURCE_NOT_FOUND"->404;
            case "IDEMPOTENCY_CONFLICT","TEAM_STATE_CONFLICT","LAST_OWNER_PROTECTED","MEMBERSHIP_ALREADY_EXISTS"->409;
            case "VERSION_MISMATCH"->412;case "TEAM_INVITATION_INVALID_OR_EXPIRED"->422;default->503;
        },rejected.getMessage());}
    }
    private static void noQuery(HttpServletRequest r){if(!r.getParameterMap().isEmpty())throw problem(400,"VALIDATION_FAILED");}
    private static JsonNode body(HttpServletRequest r,Set<String> fields)throws IOException{
        noQuery(r);if(r.getContentType()==null||!r.getContentType().split(";",2)[0].equalsIgnoreCase("application/json"))throw problem(400,"VALIDATION_FAILED");
        byte[] bytes=r.getInputStream().readNBytes(65537);try{if(bytes.length>65536)throw problem(400,"VALIDATION_FAILED");var v=JSON.readTree(bytes);shape(v,fields,fields);return v;}
        catch(CustomerProfileHttp.HttpProblem bad){throw bad;}catch(Exception bad){throw problem(400,"VALIDATION_FAILED");}finally{Arrays.fill(bytes,(byte)0);}
    }
    private static String text(JsonNode v,String field,boolean nullable){var value=v.get(field);if(nullable&&value.isNull())return null;if(!value.isString())throw problem(400,"VALIDATION_FAILED");return value.asString();}
    private static int integer(JsonNode v,String field){var value=v.get(field);if(!value.isIntegralNumber()||!value.canConvertToInt())throw problem(400,"VALIDATION_FAILED");return value.asInt();}
    @PostMapping(BASE+"/members/search") public MemberPage search(@PathVariable String organizationId,HttpServletRequest r)throws IOException{
        var v=body(r,Set.of("search","role","state","facilityId","page","size"));String facility=text(v,"facilityId",true);
        var q=new MemberSearch(text(v,"search",false),text(v,"role",true),text(v,"state",true),facility==null?null:uuid(facility),integer(v,"page"),integer(v,"size"));return execute(organizationId,a->team.members(a,q));
    }
    @GetMapping(BASE+"/members/{membershipId}") public ResponseEntity<MemberDetail> member(@PathVariable String organizationId,@PathVariable String membershipId,HttpServletRequest r){noQuery(r);UUID id=uuid(membershipId);var result=execute(organizationId,a->team.member(a,id));return ResponseEntity.ok().eTag("\"v"+result.version()+"\"").body(result);}
    @GetMapping(BASE+"/role-templates") public RoleTemplates templates(@PathVariable String organizationId,HttpServletRequest r){noQuery(r);return execute(organizationId,team::roleTemplates);}
    private static ResponseEntity<MemberDetail> memberResponse(MemberDetail m){return ResponseEntity.ok().eTag("\"v"+m.version()+"\"").body(m);}
    private static List<Grant> grants(JsonNode node){
        if(!node.isArray()||node.isEmpty()||node.size()>200)throw problem(400,"VALIDATION_FAILED");var values=new ArrayList<Grant>();
        for(var v:node){var fields=Set.of("permission","scope","facilityId");shape(v,fields,fields);String site=text(v,"facilityId",true);values.add(new Grant(text(v,"permission",false),text(v,"scope",false),site==null?null:uuid(site)));}
        return List.copyOf(values);
    }
    @PutMapping(BASE+"/members/{id}/access") public ResponseEntity<MemberDetail> access(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{
        UUID target=uuid(id),key=key(r);long version=version(r);var v=body(r,Set.of("role","grants","reason","reauthenticationProofId"));var in=new AccessInput(text(v,"role",false),grants(v.get("grants")),text(v,"reason",false),uuid(text(v,"reauthenticationProofId",false)));
        return memberResponse(target(organizationId,target,a->team.updateAccess(a,target,in,version,key)));
    }
    @PostMapping({BASE+"/members/{id}/suspend",BASE+"/members/{id}/resume",BASE+"/members/{id}/revoke"}) public ResponseEntity<MemberDetail> lifecycle(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{
        UUID target=uuid(id),key=key(r);long version=version(r);var v=body(r,Set.of("reason","reauthenticationProofId"));var in=new SensitiveReason(text(v,"reason",false),uuid(text(v,"reauthenticationProofId",false)));String op=r.getRequestURI().endsWith("/suspend")?"SUSPEND":r.getRequestURI().endsWith("/resume")?"RESUME":"REVOKE_MEMBER";
        return memberResponse(target(organizationId,target,a->team.lifecycle(a,target,op,in,version,key)));
    }
    @GetMapping(BASE+"/receipts/{operation}/{key}") public Receipt receipt(@PathVariable String organizationId,@PathVariable String operation,@PathVariable String key,HttpServletRequest r){noQuery(r);UUID id=uuid(key);return execute(organizationId,a->team.receipt(a,operation,id));}
    @PostMapping(BASE+"/audit/search") public AuditPage audit(@PathVariable String organizationId,HttpServletRequest r)throws IOException{
        var v=body(r,Set.of("membershipId","operation","from","to","page","size"));String member=text(v,"membershipId",true);java.time.Instant from,to;
        try{String f=text(v,"from",false),t=text(v,"to",false);if(!f.endsWith("Z")||!t.endsWith("Z"))throw new IllegalArgumentException();from=java.time.Instant.parse(f);to=java.time.Instant.parse(t);}catch(RuntimeException invalid){throw problem(400,"VALIDATION_FAILED");}
        var q=new AuditSearch(member==null?null:uuid(member),text(v,"operation",true),from,to,integer(v,"page"),integer(v,"size"));return execute(organizationId,a->team.audit(a,q));
    }
    private static ResponseEntity<Invitation> invitationResponse(Invitation i){return ResponseEntity.ok().eTag("\"v"+i.version()+"\"").body(i);}
    @PostMapping(BASE+"/invitations") public ResponseEntity<Invitation> invite(@PathVariable String organizationId,HttpServletRequest r)throws IOException{
        UUID key=key(r);var v=body(r,Set.of("email","role","grants","reason"));var in=new InvitationInput(text(v,"email",false),text(v,"role",false),grants(v.get("grants")),text(v,"reason",false));
        var result=execute(organizationId,a->team.invite(a,in,key));return ResponseEntity.status(201).eTag("\"v"+result.version()+"\"").body(result);
    }
    @PostMapping(BASE+"/invitations/search") public InvitationPage invitations(@PathVariable String organizationId,HttpServletRequest r)throws IOException{
        var v=body(r,Set.of("state","page","size"));var q=new InvitationSearch(text(v,"state",true),integer(v,"page"),integer(v,"size"));return execute(organizationId,a->team.invitations(a,q));
    }
    @PostMapping({BASE+"/invitations/{id}/resend",BASE+"/invitations/{id}/revoke"}) public ResponseEntity<Invitation> invitationCommand(@PathVariable String organizationId,@PathVariable String id,HttpServletRequest r)throws IOException{
        UUID target=uuid(id),key=key(r);long version=version(r);var v=body(r,Set.of("reason"));var in=new Reason(text(v,"reason",false));boolean resend=r.getRequestURI().endsWith("/resend");
        var references=execute(organizationId,a->team.invitationReferences(a,target));return invitationResponse(execute(organizationId,references,a->resend?team.resend(a,target,in,version,key):team.revoke(a,target,in,version,key)));
    }
    private RecipientAccess recipient(CustomerAdmission.Context context){return new RecipientAccess(){
        public UUID actorAccountId(){requireCurrent();return context.actor().accountId();}
        public void requireCurrent(){customers.requireCurrent(context);}
        public void meaningfulAction(){customers.meaningfulAction(context);}
        public boolean eligibleAccount(UUID id){requireCurrent();return context.subjects().containsKey(id)&&context.subjects().get(id).eligible();}
        public boolean matchesRecipient(UUID org,UUID invitation,byte[] contact){requireCurrent();return memberships.isRecipient(new CustomerMembershipInvitations.Bound(context,org),invitation,contact);}
        public void consume(UUID org,UUID invitation,long version,byte[] proof){requireCurrent();memberships.consume(new CustomerMembershipInvitations.Bound(context,org),invitation,version,proof);}
    };}
    @PostMapping({"/api/v1/customer/organization-invitations/{id}/accept","/api/v1/customer/organization-invitations/{id}/decline"})
    public DecisionReceipt decide(@PathVariable String id,HttpServletRequest r)throws IOException{
        UUID target=uuid(id),key=key(r);var value=body(r,Set.of("proof"));var input=new InvitationDecision(text(value,"proof",false));boolean accept=r.getRequestURI().endsWith("/accept");var request=requests.current();
        try{var references=customers.execute(request.encodedAccess(),request.requestId(),Set.of(),c->team.recipientReferences(recipient(c),target));
            return customers.execute(request.encodedAccess(),request.requestId(),references.accounts(),c->team.decide(recipient(c),target,accept,input,key));
        }catch(CustomerMembershipInvitations.Failure rejected){throw problem(switch(rejected.getMessage()){case "VALIDATION_FAILED"->400;case "TEAM_INVITATION_INVALID_OR_EXPIRED"->422;default->503;},rejected.getMessage());}
        catch(OrganizationTeam.Failure rejected){throw problem(switch(rejected.getMessage()){case "IDEMPOTENCY_CONFLICT","MEMBERSHIP_ALREADY_EXISTS"->409;case "VALIDATION_FAILED"->400;case "TEAM_INVITATION_INVALID_OR_EXPIRED"->422;default->503;},rejected.getMessage());}
    }
}
