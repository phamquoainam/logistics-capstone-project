package com.elogistic.backend.identity.internal.web;

import com.elogistic.backend.identity.api.*;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Existing five reads and three Bearer-only administration mutations. Browser-auth remains isolated. */
@RestController @Profile("identity")
public final class ProductionIdentityReads {
    private final IdentityCurrentSessionCommands sessions;
    private final IdentityAdministrationCommands administration;
    private final ProductionAccessAdmission admission;
    private final IdentityGrantCommands grants;
    public ProductionIdentityReads(IdentityCurrentSessionCommands sessions,IdentityAdministrationCommands administration,ProductionAccessAdmission admission,IdentityGrantCommands grants) {
        this.sessions=sessions;this.administration=administration;this.admission=admission;this.grants=grants;
    }
    @GetMapping("/api/v1/auth/me") public ResponseEntity<IdentityGrantCommands.Account> me(){return account(sessions.getIdentityMe(admission.requestId()));}
    @GetMapping("/api/v1/admin/users") public IdentityAdministrationCommands.AccountPage users(@RequestParam(required=false)String cursor,@RequestParam(required=false)Integer limit) {
        return administration.listIdentityAccounts(new IdentityAdministrationCommands.Query(admission.requestId(),cursor,limit));
    }
    @GetMapping("/api/v1/admin/users/{accountId}") public ResponseEntity<IdentityGrantCommands.Account> user(@PathVariable UUID accountId) {
        return account(administration.getIdentityAccount(accountId,admission.requestId()));
    }
    @GetMapping("/api/v1/admin/roles") public IdentityAdministrationCommands.RoleCatalog roles(){return administration.listIdentityRoles(admission.requestId());}
    @GetMapping("/api/v1/admin/scopes") public IdentityAdministrationCommands.ScopeCatalog scopes(){return administration.listIdentityScopes(admission.requestId());}
    private static final tools.jackson.databind.json.JsonMapper JSON=tools.jackson.databind.json.JsonMapper.builder()
        .enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    public static final class PreconditionFailure extends RuntimeException {
        final int status;final String code;
        PreconditionFailure(int status,String code){super(code);this.status=status;this.code=code;}
    }
    @PutMapping(value="/api/v1/admin/users/{accountId}/grants",consumes="application/json")
    public ResponseEntity<IdentityGrantCommands.Account> replace(@PathVariable UUID accountId,@RequestBody String body,
            @RequestHeader(value="If-Match",required=false)String match,@RequestHeader(value="Idempotency-Key",required=false)String key) {
        authorizeTarget(accountId,true);long version=version(match);UUID operation=uuid(key);var input=input(body,Set.of("grants","reason"));
        var list=input.get("grants");if(!list.isArray() || list.isEmpty())throw invalid();List<IdentityGrantCommands.GrantData> requested=new ArrayList<>();
        for(var row:list){if(!row.isObject() || row.size()!=3 || row.get("role")==null || row.get("scopeType")==null || row.get("scopeId")==null)throw invalid();
            String role=string(row.get("role"),80),type=string(row.get("scopeType"),32),scope=string(row.get("scopeId"),36);
            if(!Set.of("IDENTITY","MERCHANT","WAREHOUSE","HUB","ASSIGNMENT").contains(type))throw invalid();
            requested.add(new IdentityGrantCommands.GrantData(role,type,uuid(scope)));}
        if(new HashSet<>(requested).size()!=requested.size())throw invalid();
        var actor=admission.admit(com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission.Operation.CURRENT_SESSION,admission.requestId());
        var result=grants.replaceIdentityGrants(new IdentityGrantCommands.Command(actor.sessionId(),accountId,requested,version,operation,string(input.get("reason"),300),admission.requestId()));
        return completed(result);
    }
    @PostMapping(value="/api/v1/admin/users/{accountId}/disable",consumes="application/json")
    public ResponseEntity<IdentityGrantCommands.Account> disable(@PathVariable UUID accountId,@RequestBody String body,
            @RequestHeader(value="If-Match",required=false)String match,@RequestHeader(value="Idempotency-Key",required=false)String key) {
        authorizeTarget(accountId,true);var command=lifecycle(accountId,match,key,body);return completed(administration.disableIdentityAccount(command));
    }
    @PostMapping(value="/api/v1/admin/users/{accountId}/reactivate",consumes="application/json")
    public ResponseEntity<IdentityGrantCommands.Account> reactivate(@PathVariable UUID accountId,@RequestBody String body,
            @RequestHeader(value="If-Match",required=false)String match,@RequestHeader(value="Idempotency-Key",required=false)String key) {
        authorizeTarget(accountId,false);var command=lifecycle(accountId,match,key,body);return completed(administration.reactivateIdentityAccount(command));
    }
    private ResponseEntity<IdentityGrantCommands.Account> completed(IdentityGrantCommands.Result result){
        if(!result.replayed())admission.recordCompletedAction(sessions);return account(result.account());
    }
    private void authorizeTarget(UUID target,boolean selfForbidden){
        var actor=admission.admit(com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission.Operation.CURRENT_SESSION,admission.requestId());
        if(selfForbidden && target.equals(actor.accountId()))throw new IdentityCommandFailure(IdentityCommandFailure.Code.SELF_MANAGEMENT_FORBIDDEN);
        administration.getIdentityAccount(target,admission.requestId());
    }
    private IdentityAdministrationCommands.Command lifecycle(UUID target,String match,String key,String body){
        long version=version(match);UUID operation=uuid(key);var input=input(body,Set.of("reason"));
        return new IdentityAdministrationCommands.Command(admission.requestId(),target,version,operation,string(input.get("reason"),300));
    }
    private static long version(String value){
        if(value==null)throw new PreconditionFailure(428,"PRECONDITION_REQUIRED");
        try{if(!value.matches("\\\"v[1-9][0-9]{0,18}\\\""))throw new IllegalArgumentException();return Long.parseLong(value.substring(2,value.length()-1));}
        catch(Exception invalid){throw new PreconditionFailure(400,"PRECONDITION_MALFORMED");}
    }
    private static UUID uuid(String text){try{var value=UUID.fromString(text);if(!value.toString().equalsIgnoreCase(text))throw new IllegalArgumentException();return value;}catch(Exception invalid){throw invalid();}}
    private static tools.jackson.databind.JsonNode input(String body,Set<String> names){
        try{if(body==null || body.length()>1048576)throw invalid();var value=JSON.readTree(body);if(!value.isObject() || value.size()!=names.size())throw invalid();
            for(String name:names)if(value.get(name)==null)throw invalid();return value;
        }catch(Exception rejected){throw invalid();}
    }
    private static String string(tools.jackson.databind.JsonNode value,int max){
        if(value==null || !value.isString())throw invalid();String text=value.asString();if(text.isEmpty() || text.codePointCount(0,text.length())>max)throw invalid();return text;
    }
    private static IdentityCommandFailure invalid(){return new IdentityCommandFailure(IdentityCommandFailure.Code.VALIDATION_FAILED);}
    private ResponseEntity<IdentityGrantCommands.Account> account(IdentityGrantCommands.Account account){return ResponseEntity.ok().eTag("\"v"+account.version()+"\"").body(account);}
}
