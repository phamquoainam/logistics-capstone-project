package com.elogistic.backend.merchant.internal.application;

import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.merchant.api.customer.*;
import com.elogistic.backend.merchant.internal.application.port.*;
import com.elogistic.backend.merchant.internal.domain.CustomerAddress;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.elogistic.backend.merchant.api.customer.PersonalAddressFailure.Code.*;

public final class PersonalAddressApplication implements PersonalAddresses {
    private final CustomerAdmission admission;private final PersonalAddressStore store;private final MerchantFingerprints fingerprints;private final Clock clock;
    public PersonalAddressApplication(CustomerAdmission admission,PersonalAddressStore store,MerchantFingerprints fingerprints,Clock clock){this.admission=Objects.requireNonNull(admission);this.store=Objects.requireNonNull(store);this.fingerprints=Objects.requireNonNull(fingerprints);this.clock=Objects.requireNonNull(clock);}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private static PersonalAddressFailure fail(PersonalAddressFailure.Code code){return new PersonalAddressFailure(code);}
    private static Input normalized(Input input){try{return CustomerAddress.normalize(input);}catch(RuntimeException invalid){throw fail(VALIDATION_FAILED);}}
    private CustomerAddress own(CustomerAdmission.Context context,UUID id,boolean lock,boolean archived){if(id==null)throw fail(VALIDATION_FAILED);var result=store.find(context.actor().accountId(),id,lock).orElseThrow(()->fail(RESOURCE_NOT_FOUND));if(!archived && result.archivedAt()!=null)throw fail(RESOURCE_NOT_FOUND);return result;}
    private byte[] hash(String key,String operation,UUID target,long expected,Input input){
        return input==null?fingerprints.hash(key,operation,target,expected):fingerprints.hash(key,operation,target,expected,input.kind(),input.label(),input.contactName(),input.contactPhone(),input.address().country(),input.address().administrativeArea(),input.address().locality(),input.address().addressLine(),Boolean.toString(input.isDefault()));
    }
    private CustomerAddress replay(CustomerAdmission.Context context,String operation,UUID target,long expected,Input input,PersonalAddressStore.Receipt receipt,boolean archive){
        own(context,target,false,archive);byte[] actual=hash(receipt.keyId(),operation,target,expected,input),stored=receipt.fingerprint();
        try{if(!target.equals(receipt.target()) || expected!=receipt.originalVersion() || !MessageDigest.isEqual(actual,stored))throw fail(IDEMPOTENCY_CONFLICT);}finally{Arrays.fill(actual,(byte)0);Arrays.fill(stored,(byte)0);}
        var original=store.version(context.actor().accountId(),target,receipt.resultVersion());admission.requireCurrent(context);return original;
    }
    private void save(CustomerAdmission.Context context,String operation,CustomerAddress before,CustomerAddress after){
        store.save(before,after);store.audit(context.actor().accountId(),operation,after.id(),before==null?0:before.version(),after.version(),context.requestId(),after.updatedAt());
    }
    private void displaceDefault(CustomerAdmission.Context context,CustomerAddress after){
        if(after.input().isDefault())store.activeDefault(context.actor().accountId(),after.input().kind(),after.id()).ifPresent(previous->save(context,"updatePersonalAddress",previous,previous.withoutDefault(after.updatedAt())));
    }
    private Address commit(CustomerAdmission.Context context,String operation,UUID key,CustomerAddress before,CustomerAddress after,Input fingerprintInput){
        displaceDefault(context,after);save(context,operation,before,after);long expected=before==null?0:before.version();String keyId=fingerprints.activeKey();byte[] hash=hash(keyId,operation,after.id(),expected,fingerprintInput);
        try{store.receipt(context.actor().accountId(),operation,key,new PersonalAddressStore.Receipt(after.id(),keyId,hash,expected,after.version()),after.updatedAt());}finally{Arrays.fill(hash,(byte)0);}
        admission.requireCurrent(context);admission.meaningfulAction(context);return after.view();
    }
    @Override public Address create(CustomerAdmission.Context context,Input input,UUID key){
        admission.requireCurrent(context);if(key==null)throw fail(VALIDATION_FAILED);Input value=normalized(input);UUID account=context.actor().accountId();store.lockOwner(account);var previous=store.receipt(account,"createPersonalAddress",key);
        if(previous.isPresent())return replay(context,"createPersonalAddress",previous.get().target(),0,value,previous.get(),false).view();
        Instant now=now();return commit(context,"createPersonalAddress",key,null,new CustomerAddress(UUID.randomUUID(),account,value,1,now,now,null),value);
    }
    @Override public Address update(CustomerAdmission.Context context,UUID id,Input input,long expected,UUID key){
        admission.requireCurrent(context);if(key==null || expected<1)throw fail(VALIDATION_FAILED);store.lockOwner(context.actor().accountId());var before=own(context,id,true,false);Input value=normalized(input);var previous=store.receipt(context.actor().accountId(),"updatePersonalAddress",key);
        if(previous.isPresent())return replay(context,"updatePersonalAddress",id,expected,value,previous.get(),false).view();
        if(before.version()!=expected)throw fail(PRECONDITION_STALE);return commit(context,"updatePersonalAddress",key,before,before.replace(value,now()),value);
    }
    @Override public void archive(CustomerAdmission.Context context,UUID id,long expected,UUID key){
        admission.requireCurrent(context);if(key==null || expected<1)throw fail(VALIDATION_FAILED);store.lockOwner(context.actor().accountId());var before=own(context,id,true,true);var previous=store.receipt(context.actor().accountId(),"archivePersonalAddress",key);
        if(previous.isPresent()){replay(context,"archivePersonalAddress",id,expected,null,previous.get(),true);return;}
        if(before.archivedAt()!=null)throw fail(RESOURCE_NOT_FOUND);if(before.version()!=expected)throw fail(PRECONDITION_STALE);commit(context,"archivePersonalAddress",key,before,before.archive(now()),null);
    }
    @Override public Address get(CustomerAdmission.Context context,UUID id){admission.requireCurrent(context);var result=own(context,id,false,false).view();admission.requireCurrent(context);return result;}
    @Override public Page list(CustomerAdmission.Context context,Query query){
        admission.requireCurrent(context);if(query==null || query.page()<0 || query.size()!=20 || query.kind()!=null && !Set.of("SENDER","RECIPIENT").contains(query.kind()) || query.search()!=null && (query.search().isBlank() || query.search().codePointCount(0,query.search().length())>160 || query.search().codePoints().anyMatch(Character::isISOControl)))throw fail(VALIDATION_FAILED);
        var rows=store.list(context.actor().accountId(),query);var result=new Page(rows.stream().limit(20).map(CustomerAddress::view).toList(),query.page(),20,rows.size()>20);admission.requireCurrent(context);return result;
    }
    @Override public Receipt receipt(CustomerAdmission.Context context,String operation,UUID key){
        admission.requireCurrent(context);if(key==null || !Set.of("createPersonalAddress","updatePersonalAddress","archivePersonalAddress").contains(operation))throw fail(RESOURCE_NOT_FOUND);
        var receipt=store.receipt(context.actor().accountId(),operation,key).orElseThrow(()->fail(RESOURCE_NOT_FOUND));own(context,receipt.target(),false,true);admission.requireCurrent(context);return new Receipt(operation,receipt.target(),"COMMITTED",receipt.resultVersion());
    }
}
