package com.elogistic.backend.orders.internal.application;
import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.orders.internal.application.port.*;
import com.elogistic.backend.orders.internal.domain.ShippingPreparation;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.security.MessageDigest;
import java.util.*;
import static com.elogistic.backend.orders.api.ShippingDraftFailure.Code.*;

public final class ShippingDraftApplication implements ShippingDrafts {
    private final ShippingDraftStore store;private final ShippingDraftFingerprints fingerprints;private final Clock clock;
    public ShippingDraftApplication(ShippingDraftStore store,ShippingDraftFingerprints fingerprints,Clock clock){this.store=Objects.requireNonNull(store);this.fingerprints=Objects.requireNonNull(fingerprints);this.clock=Objects.requireNonNull(clock);}
    private static ShippingDraftFailure fail(ShippingDraftFailure.Code code){return new ShippingDraftFailure(code);}
    private static Input normalize(Input input){try{return ShippingPreparation.normalize(input);}catch(RuntimeException invalid){throw fail(VALIDATION_FAILED);}}
    private Draft own(Access access,UUID id,boolean archived){if(id==null)throw fail(VALIDATION_FAILED);Draft d=store.find(access.accountId(),id).orElseThrow(()->fail(RESOURCE_NOT_FOUND));if(!archived&&!d.state().equals("DRAFT"))throw fail(RESOURCE_NOT_FOUND);return d;}
    private Draft replay(Access access,String operation,UUID id,long expected,Input input,ShippingDraftStore.StoredReceipt receipt,boolean archive){
        own(access,id,archive);byte[] actual=fingerprints.hash(receipt.keyId(),access.accountId(),operation,id,expected,input==null?null:store.canonical(input)),stored=receipt.hash();
        try{if(!id.equals(receipt.target())||expected!=receipt.originalVersion()||!MessageDigest.isEqual(actual,stored))throw fail(IDEMPOTENCY_CONFLICT);}finally{Arrays.fill(actual,(byte)0);Arrays.fill(stored,(byte)0);}Draft result=store.version(access.accountId(),id,receipt.resultVersion());access.requireCurrent();return result;
    }
    private Draft commit(Access access,String operation,UUID key,Draft before,Input input,boolean archive){
        Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);long expected=before==null?0:before.version();UUID id=before==null?UUID.randomUUID():before.id();var after=new Draft(id,access.accountId(),archive?"ARCHIVED":"DRAFT",archive?before.input():input,expected+1,before==null?now:before.createdAt(),now);
        store.save(before,after,operation,access.requestId());String kid=fingerprints.activeKey();byte[] hash=fingerprints.hash(kid,access.accountId(),operation,id,expected,archive?null:store.canonical(input));
        try{store.receipt(access.accountId(),operation,key,new ShippingDraftStore.StoredReceipt(id,expected,after.version(),kid,hash),now);}finally{Arrays.fill(hash,(byte)0);}access.requireCurrent();access.meaningfulAction();return after;
    }
    @Override public Draft create(Access access,Input input,UUID key){access.requireCurrent();if(key==null)throw fail(VALIDATION_FAILED);Input valid=normalize(input);store.lockOwner(access.accountId());var prior=store.receipt(access.accountId(),"createShippingDraft",key);if(prior.isPresent())return replay(access,"createShippingDraft",prior.get().target(),0,valid,prior.get(),false);return commit(access,"createShippingDraft",key,null,valid,false);}
    @Override public Draft update(Access access,UUID id,Input input,long expected,UUID key){access.requireCurrent();if(key==null||expected<1)throw fail(VALIDATION_FAILED);store.lockOwner(access.accountId());Draft before=own(access,id,false);Input valid=normalize(input);var prior=store.receipt(access.accountId(),"updateShippingDraft",key);if(prior.isPresent())return replay(access,"updateShippingDraft",id,expected,valid,prior.get(),false);if(before.version()!=expected)throw fail(PRECONDITION_STALE);return commit(access,"updateShippingDraft",key,before,valid,false);}
    @Override public void archive(Access access,UUID id,long expected,UUID key){access.requireCurrent();if(key==null||expected<1)throw fail(VALIDATION_FAILED);store.lockOwner(access.accountId());Draft before=own(access,id,true);var prior=store.receipt(access.accountId(),"archiveShippingDraft",key);if(prior.isPresent()){replay(access,"archiveShippingDraft",id,expected,null,prior.get(),true);return;}if(!before.state().equals("DRAFT"))throw fail(RESOURCE_NOT_FOUND);if(before.version()!=expected)throw fail(PRECONDITION_STALE);commit(access,"archiveShippingDraft",key,before,null,true);}
    @Override public Draft get(Access access,UUID id){access.requireCurrent();var result=own(access,id,false);access.requireCurrent();return result;}
    @Override public Page list(Access access,int page,String search){access.requireCurrent();if(page<0||search!=null&&(search.isBlank()||search.length()>160||search.codePoints().anyMatch(Character::isISOControl)))throw fail(VALIDATION_FAILED);var rows=store.list(access.accountId(),page,search);access.requireCurrent();return new Page(rows.stream().limit(20).toList(),page,20,rows.size()>20);}
    @Override public Receipt receipt(Access access,String operation,UUID key){access.requireCurrent();if(key==null||!Set.of("createShippingDraft","updateShippingDraft","archiveShippingDraft").contains(operation))throw fail(RESOURCE_NOT_FOUND);var r=store.receipt(access.accountId(),operation,key).orElseThrow(()->fail(RESOURCE_NOT_FOUND));own(access,r.target(),true);access.requireCurrent();return new Receipt(operation,r.target(),"COMMITTED",r.resultVersion());}
}
