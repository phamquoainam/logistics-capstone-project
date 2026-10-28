package com.elogistic.backend.merchant.internal.application;

import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.merchant.api.OnboardingFailure;
import com.elogistic.backend.merchant.api.customer.MerchantApplications.*;
import com.elogistic.backend.merchant.internal.application.port.MerchantApplicationStore;
import com.elogistic.backend.merchant.internal.domain.MerchantApplicationDraft;
import com.elogistic.backend.merchant.internal.infrastructure.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static com.elogistic.backend.merchant.api.OnboardingFailure.Code.*;

/** Isolated application rules; actual transactional/HTTP/tenant/race behavior belongs to PG18. */
class MerchantApplicationTests {
    private static final Instant NOW=Instant.parse("2026-10-28T00:00:00Z");
    private final UUID actor=UUID.randomUUID();
    private final CustomerAdmission.Context context=new CustomerAdmission.Context(UUID.randomUUID(),UUID.randomUUID(),new CustomerAdmission.Subject(actor,"Customer","customer@example.invalid",1,true),Map.of());
    private final Authority admission=new Authority();private final Store store=new Store();
    private final MerchantApplicationService service=new MerchantApplicationService(admission,store,protection(),Clock.fixed(NOW,ZoneOffset.UTC));
    private static ProtectedMerchantFingerprints protection(){byte[] key=new byte[32];Arrays.fill(key,(byte)73);return new ProtectedMerchantFingerprints(Map.of("k1",key),"k1");}
    private static Input input(String name){return new Input(null,null,name,null,null,null,null,null,null,null,null,null);}
    private static Input keyInput(String jurisdiction,String identifier){return new Input("COMPANY",jurisdiction,"Synthetic",identifier,null,null,null,null,null,null,null,null);}
    private final class Authority implements CustomerAdmission {
        boolean active=true;int activity;
        public <T>T execute(String token,UUID request,Set<UUID> refs,Function<Context,T> work){throw new UnsupportedOperationException();}
        public void requireCurrent(Context c){if(!active || c!=context)throw new IllegalStateException("CURRENT_REQUIRED");}
        public void meaningfulAction(Context c){requireCurrent(c);activity++;}
    }
    private final class Store implements MerchantApplicationStore {
        Map<UUID,MerchantApplicationDraft> rows=new LinkedHashMap<>();Map<String,Receipt> receipts=new HashMap<>();Map<String,MerchantApplicationDraft> versions=new HashMap<>();int writes,audits,locks;boolean failAudit;List<String> keys=new ArrayList<>();
        public void lockControl(){locks++;}
        public void lockKeys(Collection<String> values){keys.addAll(values);assertEquals(new ArrayList<>(new TreeSet<>(values)),new ArrayList<>(values));}
        public Optional<MerchantApplicationDraft> find(UUID account,UUID id,boolean lock){return Optional.ofNullable(rows.get(id)).filter(r->r.applicantAccountId().equals(account));}
        public List<MerchantApplicationDraft> list(UUID account,Query query){return rows.values().stream().filter(r->r.applicantAccountId().equals(account)).filter(r->query.state()==null || query.state().equals("DRAFT")).skip((long)query.page()*query.size()).limit(query.size()+1).toList();}
        public Optional<Receipt> receipt(UUID account,String op,UUID key){assertEquals(actor,account);return Optional.ofNullable(receipts.get(op+key));}
        public MerchantApplicationDraft version(UUID account,UUID id,long v){assertEquals(actor,account);return Objects.requireNonNull(versions.get(id+":"+v));}
        public void save(MerchantApplicationDraft before,MerchantApplicationDraft after){assertEquals(before,rows.get(after.id()));rows.put(after.id(),after);versions.put(after.id()+":"+after.version(),after);writes++;}
        public void audit(UUID account,String op,UUID id,long before,long after,UUID request,Instant now){assertEquals(actor,account);assertEquals(before+1,after);if(failAudit)throw new IllegalStateException("AUDIT_UNAVAILABLE");audits++;}
        public void receipt(UUID account,String op,UUID key,Receipt receipt,Instant now){receipts.put(op+key,receipt);}
        public String canonical(Input input){return JsonMapper.builder().build().writeValueAsString(input);}
    }
    @Test void emptyAndPartialDraftsBindOnlyTheCurrentApplicantWithoutMerchantAuthority(){
        var created=service.create(context,input(null),UUID.randomUUID());assertEquals(actor,created.applicantAccountId());assertEquals("DRAFT",created.state());assertEquals(1,created.version());assertEquals(1,created.revision());assertEquals(1,store.audits);assertEquals(1,admission.activity);assertEquals(1,service.list(context,new Query(0,20,null,null)).items().size());
        var changed=service.update(context,created.id(),input("Synthetic business"),1,UUID.randomUUID());assertEquals(2,changed.version());assertEquals("Synthetic business",service.get(context,created.id()).draft().registeredName());assertEquals(2,admission.activity);
    }
    @Test void retriesReturnOriginalPrivateSnapshotsAfterLaterEditsWithoutRepeatedActivity(){
        UUID create=UUID.randomUUID(),update=UUID.randomUUID();var first=service.create(context,input("First"),create);var second=service.update(context,first.id(),input("Second"),1,update);service.update(context,first.id(),input("Third"),2,UUID.randomUUID());
        assertEquals(first,service.create(context,input("First"),create));assertEquals(second,service.update(context,first.id(),input("Second"),1,update));assertEquals(3,service.get(context,first.id()).version());assertEquals(3,store.writes);assertEquals(3,store.audits);assertEquals(3,admission.activity);
    }
    @Test void changedPayloadTargetOrOriginalVersionCannotReuseACommittedKey(){
        UUID create=UUID.randomUUID(),update=UUID.randomUUID();var first=service.create(context,input("First"),create);service.update(context,first.id(),input("Next"),1,update);var other=service.create(context,input("Other"),UUID.randomUUID());
        assertEquals(IDEMPOTENCY_CONFLICT,assertThrows(OnboardingFailure.class,()->service.create(context,input("Changed"),create)).code());
        assertEquals(IDEMPOTENCY_CONFLICT,assertThrows(OnboardingFailure.class,()->service.update(context,first.id(),input("Next"),2,update)).code());
        assertEquals(IDEMPOTENCY_CONFLICT,assertThrows(OnboardingFailure.class,()->service.update(context,other.id(),input("Next"),1,update)).code());assertEquals(3,store.writes);
    }
    @Test void foreignAndUnknownTargetsDenyBeforeVersionDisclosureAndCannotReadReceipts(){
        UUID id=UUID.randomUUID();store.rows.put(id,new MerchantApplicationDraft(id,UUID.randomUUID(),input("Private"),1,NOW,NOW));
        for(UUID target:List.of(id,UUID.randomUUID())){assertEquals(RESOURCE_NOT_FOUND,assertThrows(OnboardingFailure.class,()->service.get(context,target)).code());assertEquals(RESOURCE_NOT_FOUND,assertThrows(OnboardingFailure.class,()->service.update(context,target,input("Change"),999,UUID.randomUUID())).code());}
        assertTrue(service.list(context,new Query(0,20,null,null)).items().isEmpty());assertEquals(RESOURCE_NOT_FOUND,assertThrows(OnboardingFailure.class,()->service.receipt(context,"createApplicationDraft",UUID.randomUUID())).code());assertEquals(0,store.writes);
    }
    @Test void staleWritesAndMandatoryAuditFailureCannotPublishSuccessOrActivity(){
        var first=service.create(context,input("First"),UUID.randomUUID());assertEquals(PRECONDITION_STALE,assertThrows(OnboardingFailure.class,()->service.update(context,first.id(),input("Changed"),2,UUID.randomUUID())).code());assertEquals(1,store.writes);
        store.failAudit=true;assertThrows(IllegalStateException.class,()->service.update(context,first.id(),input("Audit failure"),1,UUID.randomUUID()));assertEquals(1,store.receipts.size());assertEquals(1,admission.activity); // Physical rollback is a separate real PG gate.
    }
    @Test void currentAdmissionIsRequiredForReadsMutationReplayAndReceipt(){
        UUID key=UUID.randomUUID();var result=service.create(context,input("Private"),key);admission.active=false;
        assertThrows(IllegalStateException.class,()->service.create(context,input("Private"),key));assertThrows(IllegalStateException.class,()->service.get(context,result.id()));assertThrows(IllegalStateException.class,()->service.list(context,new Query(0,20,null,null)));assertThrows(IllegalStateException.class,()->service.receipt(context,"createApplicationDraft",key));assertEquals(1,store.writes);
    }
    @Test void exactPartialSchemaRejectsInvalidFieldsAndNeverTreatsReferencesAsProof(){
        for(Input bad:List.of(input(" "),input("x".repeat(241)),input("line\nprivate"),keyInput("VN"," -./_ "),new Input("OWNER",null,null,null,null,null,null,null,null,null,null,null),new Input(null,null,null,null,new Address("vn","Area","City","Street"),null,null,null,null,null,null,null),new Input(null,null,null,null,null,null,null,null,null,null,null,new Declaration(false,true,"v1"))))assertEquals(VALIDATION_FAILED,assertThrows(OnboardingFailure.class,()->service.create(context,bad,UUID.randomUUID())).code());assertEquals(0,store.writes);
        UUID opaque=UUID.randomUUID();var draft=service.create(context,new Input(null,null,null,null,null,null,null,null,null,opaque,List.of(opaque),null),UUID.randomUUID());assertEquals("DRAFT",draft.state());assertEquals(1,draft.revision());
    }
    @Test void sandboxBusinessKeysNormalizeOnlyReviewedBusinessFieldsAndLockBeforeWrite(){
        var a=keyInput(" VN ","12-34.5/6_7");var b=keyInput("VN","1234567");assertEquals(MerchantApplicationDraft.businessKey(a),MerchantApplicationDraft.businessKey(b));assertNotEquals(MerchantApplicationDraft.businessKey(a),MerchantApplicationDraft.businessKey(keyInput("vn","1234567")));
        assertEquals(MerchantApplicationDraft.businessKey(keyInput("VN","A-\u0301")),MerchantApplicationDraft.businessKey(keyInput("VN","\u00c1")));
        service.create(context,a,UUID.randomUUID());assertEquals(List.of(MerchantApplicationDraft.businessKey(a)),store.keys);assertEquals(1,store.locks);
    }
    @Test void scopedPagingAndReceiptReadsHaveNoGlobalTotalsOrIdleWrites(){
        UUID key=UUID.randomUUID();var first=service.create(context,input("First"),key);service.create(context,input("Second"),UUID.randomUUID());var page=service.list(context,new Query(0,1,null,null));assertTrue(page.hasNext());assertEquals(1,page.items().size());assertTrue(service.list(context,new Query(0,20,null,"APPROVED")).items().isEmpty());
        var receipt=service.receipt(context,"createApplicationDraft",key);assertEquals(first.id(),receipt.targetId());assertEquals(1,receipt.version());assertEquals("COMMITTED",receipt.outcome());assertEquals(2,admission.activity);
        for(Query bad:List.of(new Query(-1,20,null,null),new Query(0,101,null,null),new Query(0,1," ",null),new Query(0,1,null,"OWNER")))assertEquals(VALIDATION_FAILED,assertThrows(OnboardingFailure.class,()->service.list(context,bad)).code());
    }
    @Test void sensitiveDraftDiagnosticsAreRedactedAndPersistenceRequiresExactOwnedTransaction(){
        var input=input("Private legal name");var result=service.create(context,input,UUID.randomUUID());assertFalse(input.toString().contains("Private"));assertFalse(result.toString().contains("Private"));assertFalse(store.rows.values().toString().contains("Private"));assertFalse(store.receipts.values().toString().contains("Private"));
        var source=new DriverManagerDataSource();var sql=new JdbcMerchantApplicationStore(source);assertThrows(IllegalStateException.class,sql::lockControl);assertThrows(IllegalStateException.class,()->sql.find(actor,result.id(),false));assertThrows(IllegalStateException.class,()->sql.list(actor,new Query(0,20,null,null)));
        assertThrows(IllegalArgumentException.class,()->protection().hash("k1","updateCustomerProfile",actor,0,"body"));assertEquals(32,protection().hash("k1","createApplicationDraft",actor,0,"{}").length);
    }
}
