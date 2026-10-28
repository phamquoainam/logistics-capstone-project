package com.elogistic.backend.merchant.internal.application;

import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.merchant.api.OnboardingFailure;
import com.elogistic.backend.merchant.api.customer.CustomerProfiles;
import com.elogistic.backend.merchant.internal.application.port.CustomerProfileStore;
import com.elogistic.backend.merchant.internal.domain.CustomerBusinessProfile;
import com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Isolated business tests only; physical admission/rollback/races are verified separately on PG18. */
class CustomerProfileTests {
    private static final UUID ACCOUNT=UUID.randomUUID(),REQUEST=UUID.randomUUID(),SESSION=UUID.randomUUID();
    private static final Instant NOW=Instant.parse("2026-10-27T00:00:00Z");
    private final CustomerAdmission.Context context=new CustomerAdmission.Context(REQUEST,SESSION,new CustomerAdmission.Subject(ACCOUNT,"Identity display","security@example.invalid",1,true),Map.of());
    private final Authority admission=new Authority();private final Store store=new Store();
    private final CustomerProfileApplication app=new CustomerProfileApplication(admission,store,fingerprints(),Clock.fixed(NOW,ZoneOffset.UTC));
    private static ProtectedMerchantFingerprints fingerprints(){byte[] k=new byte[32];Arrays.fill(k,(byte)41);return new ProtectedMerchantFingerprints(Map.of("k1",k),"k1");}
    private static CustomerProfiles.Input input(String display){return new CustomerProfiles.Input(display,"vi","business@example.invalid","synthetic contact");}
    private CustomerProfiles.Update update(String display,long version,UUID key){return new CustomerProfiles.Update(input(display),version,key);}
    private final class Authority implements CustomerAdmission {
        boolean enabled=true;int activity;
        public <T>T execute(String token,UUID request,Set<UUID> references,Function<Context,T> work){throw new UnsupportedOperationException("UNIT_BOUNDARY_ONLY");}
        public void requireCurrent(Context supplied){if(!enabled || supplied!=context)throw new IllegalStateException("CURRENT_AUTHORITY_REQUIRED");}
        public void meaningfulAction(Context supplied){requireCurrent(supplied);activity++;}
    }
    private static final class Store implements CustomerProfileStore {
        CustomerBusinessProfile current;int writes,audits,locks;Map<UUID,Receipt> receipts=new HashMap<>();Map<Long,CustomerBusinessProfile> history=new HashMap<>();
        public void lockControl(){locks++;}
        public Optional<CustomerBusinessProfile> find(UUID account,boolean lock){assertEquals(ACCOUNT,account);return Optional.ofNullable(current);}
        public Optional<Receipt> receipt(UUID account,UUID target,UUID key){assertEquals(ACCOUNT,account);return Optional.ofNullable(receipts.get(key));}
        public CustomerBusinessProfile version(UUID account,UUID profile,long version){assertEquals(ACCOUNT,account);return Objects.requireNonNull(history.get(version));}
        public void save(CustomerBusinessProfile before,CustomerBusinessProfile after,boolean insert){assertEquals(current==null,insert);current=after;history.put(after.version(),after);writes++;}
        public void audit(UUID account,UUID target,long before,long after,UUID request,Instant time){assertEquals(ACCOUNT,account);assertEquals(REQUEST,request);assertEquals(before+1,after);audits++;}
        public void receipt(UUID account,UUID target,UUID key,Receipt receipt,Instant time){receipts.put(key,receipt);}
    }
    @Test void uninitializedSelfProfileIsReadOnlyAndDoesNotCreateTenantOrActivity(){
        var profile=app.get(context);assertEquals(ACCOUNT,profile.accountId());assertEquals("Identity display",profile.displayName());assertEquals(1,profile.version());assertNull(profile.businessContactEmail());assertNull(store.current);assertEquals(0,store.writes+store.audits+store.locks+admission.activity);
    }
    @Test void businessEditDoesNotMutateSecuritySubjectAndUsesVersionedReceipt(){
        var result=app.update(context,update("Business display",1,UUID.randomUUID()));assertEquals(2,result.version());assertEquals("business@example.invalid",result.businessContactEmail());assertEquals("security@example.invalid",context.actor().verifiedEmail());assertEquals("Identity display",context.actor().displayName());assertEquals(1,store.writes);assertEquals(1,store.audits);assertEquals(1,store.receipts.size());assertEquals(1,admission.activity);
    }
    @Test void replayReturnsOriginalSnapshotAfterLaterEditWithoutAuditOrIdleExtension(){
        UUID key=UUID.randomUUID();var original=app.update(context,update("First",1,key));app.update(context,update("Second",2,UUID.randomUUID()));var replay=app.update(context,update("First",1,key));assertEquals(original,replay);assertEquals("Second",app.get(context).displayName());assertEquals(2,store.audits);assertEquals(2,admission.activity);
    }
    @Test void changedBodyOrOriginalVersionCannotReuseCommittedKey(){
        UUID key=UUID.randomUUID();app.update(context,update("First",1,key));
        for(var command:List.of(update("Different",1,key),update("First",2,key)))assertEquals(OnboardingFailure.Code.IDEMPOTENCY_CONFLICT,assertThrows(OnboardingFailure.class,()->app.update(context,command)).code());assertEquals(1,store.writes);assertEquals(1,admission.activity);
    }
    @Test void staleVersionCannotWriteHistoryAuditOrReceipt(){
        app.update(context,update("First",1,UUID.randomUUID()));assertEquals(OnboardingFailure.Code.PRECONDITION_STALE,assertThrows(OnboardingFailure.class,()->app.update(context,update("Stale",1,UUID.randomUUID()))).code());assertEquals(1,store.writes);assertEquals(1,store.receipts.size());
    }
    @Test void invalidContactLanguageAndDisplayCannotPersistOrBecomeAuthority(){
        for(var input:List.of(new CustomerProfiles.Input(" ","vi",null,null),new CustomerProfiles.Input("Name","fr",null,null),new CustomerProfiles.Input("Name","vi","bad..local@example.invalid",null),new CustomerProfiles.Input("Name","vi",null,"x\ny"),new CustomerProfiles.Input("Name","vi",null,"x".repeat(33))))assertEquals(OnboardingFailure.Code.VALIDATION_FAILED,assertThrows(OnboardingFailure.class,()->app.update(context,new CustomerProfiles.Update(input,1,UUID.randomUUID()))).code());assertEquals(0,store.writes+store.audits+admission.activity);
    }
    @Test void revokedOrConstructedContextCannotReadOrMutateBusinessProfile(){
        admission.enabled=false;assertThrows(IllegalStateException.class,()->app.get(context));assertThrows(IllegalStateException.class,()->app.update(context,update("Name",1,UUID.randomUUID())));assertEquals(0,store.writes+store.locks);
        admission.enabled=true;var constructed=new CustomerAdmission.Context(REQUEST,SESSION,context.actor(),Map.of());assertThrows(IllegalStateException.class,()->app.get(constructed));
    }
    @Test void profileAndCommandDiagnosticsDoNotExposePersonalContact(){
        var command=update("Private display",1,UUID.randomUUID());var result=app.update(context,command);assertFalse(command.toString().contains("Private"));assertFalse(result.toString().contains("business@"));assertFalse(store.current.toString().contains("Private"));assertFalse(store.receipts.values().toString().contains("business@"));
    }
    @Test void optionalContactsCanBeRemovedWithoutChangingImmutableAccountBinding(){
        app.update(context,update("Name",1,UUID.randomUUID()));var result=app.update(context,new CustomerProfiles.Update(new CustomerProfiles.Input("New","en",null,null),2,UUID.randomUUID()));assertNull(result.businessContactEmail());assertNull(result.businessContactPhone());assertEquals(ACCOUNT,result.accountId());assertEquals(3,result.version());
    }
    @Test void scopedReceiptReadReportsOriginalCommittedVersionWithoutContactOrActivity(){
        UUID key=UUID.randomUUID();assertEquals(OnboardingFailure.Code.RESOURCE_NOT_FOUND,assertThrows(OnboardingFailure.class,()->app.receipt(context,key)).code());
        app.update(context,update("First",1,key));app.update(context,update("Later",2,UUID.randomUUID()));
        var receipt=app.receipt(context,key);assertEquals("updateCustomerProfile",receipt.operationId());assertEquals(ACCOUNT,receipt.targetId());assertEquals("COMMITTED",receipt.outcome());assertEquals(2,receipt.version());assertFalse(receipt.toString().contains("business@"));assertEquals(2,store.audits);assertEquals(2,admission.activity);
        admission.enabled=false;assertThrows(IllegalStateException.class,()->app.receipt(context,key));
    }
}
