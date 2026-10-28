package com.elogistic.backend.merchant.internal.application;

import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.merchant.api.*;
import com.elogistic.backend.merchant.api.customer.MerchantApplications;
import com.elogistic.backend.merchant.internal.application.port.*;
import com.elogistic.backend.merchant.internal.domain.MerchantApplicationDraft;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.elogistic.backend.merchant.api.OnboardingFailure.Code.*;

public final class MerchantApplicationService implements MerchantApplications {
    private final CustomerAdmission admission;private final MerchantApplicationStore store;private final MerchantFingerprints fingerprints;private final Clock clock;
    public MerchantApplicationService(CustomerAdmission admission,MerchantApplicationStore store,MerchantFingerprints fingerprints,Clock clock){this.admission=Objects.requireNonNull(admission);this.store=Objects.requireNonNull(store);this.fingerprints=Objects.requireNonNull(fingerprints);this.clock=Objects.requireNonNull(clock);}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private static OnboardingFailure invalid(){return new OnboardingFailure(VALIDATION_FAILED);}
    private static void input(Input input){try{MerchantApplicationDraft.validate(input);}catch(Exception invalid){throw invalid();}}
    private MerchantApplicationDraft own(CustomerAdmission.Context context,UUID id,boolean lock){if(id==null)throw invalid();return store.find(context.actor().accountId(),id,lock).orElseThrow(()->new OnboardingFailure(RESOURCE_NOT_FOUND));}
    private Application replay(CustomerAdmission.Context context,String operation,UUID target,Input input,long expected,MerchantApplicationStore.Receipt receipt){
        byte[] hash=fingerprints.hash(receipt.keyId(),operation,target,expected,store.canonical(input)),stored=receipt.fingerprint();
        try{if(receipt.originalVersion()!=expected || !receipt.target().equals(target) || !MessageDigest.isEqual(hash,stored))throw new OnboardingFailure(IDEMPOTENCY_CONFLICT);}
        finally{Arrays.fill(hash,(byte)0);Arrays.fill(stored,(byte)0);}
        var result=store.version(context.actor().accountId(),target,receipt.resultVersion());admission.requireCurrent(context);return result.view();
    }
    private Application write(CustomerAdmission.Context context,String operation,UUID key,MerchantApplicationDraft before,MerchantApplicationDraft after){
        UUID actor=context.actor().accountId();long original=before==null?0:before.version();
        store.save(before,after);store.audit(actor,operation,after.id(),original,after.version(),context.requestId(),after.updatedAt());
        byte[] hash=fingerprints.hash(fingerprints.activeKey(),operation,after.id(),original,store.canonical(after.input()));
        try{store.receipt(actor,operation,key,new MerchantApplicationStore.Receipt(after.id(),fingerprints.activeKey(),hash,original,after.version()),after.updatedAt());}finally{Arrays.fill(hash,(byte)0);}
        admission.requireCurrent(context);admission.meaningfulAction(context);return after.view();
    }
    @Override public Application create(CustomerAdmission.Context context,Input input,UUID key){
        admission.requireCurrent(context);if(key==null)throw invalid();input(input);store.lockControl();
        var previous=store.receipt(context.actor().accountId(),"createApplicationDraft",key);
        if(previous.isPresent())return replay(context,"createApplicationDraft",previous.get().target(),input,0,previous.get());
        String businessKey=MerchantApplicationDraft.businessKey(input);store.lockKeys(businessKey==null?List.of():List.of(businessKey));
        Instant now=now();return write(context,"createApplicationDraft",key,null,new MerchantApplicationDraft(UUID.randomUUID(),context.actor().accountId(),input,1,now,now));
    }
    @Override public Application update(CustomerAdmission.Context context,UUID id,Input input,long expectedVersion,UUID key){
        admission.requireCurrent(context);if(key==null || expectedVersion<1)throw invalid();store.lockControl();var candidate=own(context,id,false);
        var previous=store.receipt(context.actor().accountId(),"updateApplicationDraft",key);input(input);
        if(previous.isPresent())return replay(context,"updateApplicationDraft",id,input,expectedVersion,previous.get());
        Set<String> keys=new TreeSet<>();for(var fields:List.of(candidate.input(),input)){String k=MerchantApplicationDraft.businessKey(fields);if(k!=null)keys.add(k);}store.lockKeys(keys);
        var before=own(context,id,true);if(before.version()!=expectedVersion)throw new OnboardingFailure(PRECONDITION_STALE);
        return write(context,"updateApplicationDraft",key,before,before.replace(input,now()));
    }
    @Override public Application get(CustomerAdmission.Context context,UUID id){admission.requireCurrent(context);var result=own(context,id,false).view();admission.requireCurrent(context);return result;}
    @Override public Page list(CustomerAdmission.Context context,Query query){
        admission.requireCurrent(context);if(query==null || query.page()<0 || query.size()<1 || query.size()>100 || query.search()!=null && (query.search().isBlank() || query.search().codePointCount(0,query.search().length())>160 || query.search().codePoints().anyMatch(Character::isISOControl)) || query.state()!=null && !Set.of("DRAFT","SUBMITTED","UNDER_REVIEW","NEEDS_INFORMATION","APPROVED","REJECTED","WITHDRAWN").contains(query.state()))throw invalid();
        var rows=store.list(context.actor().accountId(),query);boolean more=rows.size()>query.size();var result=new Page(rows.stream().limit(query.size()).map(MerchantApplicationDraft::view).toList(),query.page(),query.size(),more);admission.requireCurrent(context);return result;
    }
    @Override public Receipt receipt(CustomerAdmission.Context context,String operation,UUID key){
        admission.requireCurrent(context);if(key==null || !Set.of("createApplicationDraft","updateApplicationDraft").contains(operation))throw new OnboardingFailure(RESOURCE_NOT_FOUND);
        var result=store.receipt(context.actor().accountId(),operation,key).orElseThrow(()->new OnboardingFailure(RESOURCE_NOT_FOUND));own(context,result.target(),false);admission.requireCurrent(context);return new Receipt(operation,result.target(),"COMMITTED",result.resultVersion());
    }
}
