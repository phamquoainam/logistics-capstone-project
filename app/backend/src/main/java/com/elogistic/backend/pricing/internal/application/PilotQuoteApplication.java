package com.elogistic.backend.pricing.internal.application;
import com.elogistic.backend.pricing.api.*;
import com.elogistic.backend.pricing.internal.application.port.PilotQuoteStore;
import com.elogistic.backend.pricing.internal.domain.RoadTariffCalculation;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.security.MessageDigest;
import java.util.*;
import static com.elogistic.backend.pricing.api.PilotQuoteFailure.Code.*;
public final class PilotQuoteApplication implements PilotQuotes {
    private final PilotQuoteStore store;private final Clock clock;private final boolean enabled;
    public PilotQuoteApplication(PilotQuoteStore store,Clock clock,boolean enabled){this.store=store;this.clock=clock;this.enabled=enabled;}
    private static PilotQuoteFailure fail(PilotQuoteFailure.Code code){return new PilotQuoteFailure(code);}
    private void current(Access a){a.requireCurrent();if(!enabled)throw fail(PILOT_DISABLED);}
    @Override public boolean enabled(){return enabled;}
    @Override public Rules rules(Access a){current(a);return store.rules(clock.instant());}
    @Override public Quote create(Access a,Request request,UUID key){
        current(a);if(key==null||request==null||request.draftId()==null||request.draftVersion()<1||request.route()==null)throw fail(VALIDATION_FAILED);if(!request.ordinaryGoodsConfirmed())throw fail(ORDINARY_GOODS_REQUIRED);
        var r=request.route();if(!"ARGO-SIM-COVERAGE-1".equals(r.datasetVersion())||!Set.of("INTRA","INTER").contains(r.classification())||r.originCode()==null||r.destinationCode()==null||r.dropOffCode()==null)throw fail(UNSUPPORTED_ROUTE);
        store.lockOwner(a.accountId());String input=store.canonical(request);var previous=store.byKey(a.accountId(),key);
        if(previous.isPresent()){var prior=previous.get();byte[] actual=store.fingerprint(prior.keyId(),a.accountId(),input),stored=prior.hash();try{if(!MessageDigest.isEqual(actual,stored))throw fail(IDEMPOTENCY_CONFLICT);}finally{Arrays.fill(actual,(byte)0);Arrays.fill(stored,(byte)0);}a.requireCurrent();return prior.quote();}
        Rules rules=store.rules(clock.instant());List<Line> lines;try{lines=RoadTariffCalculation.calculate(rules,r.classification(),request.parcels());}catch(IllegalArgumentException invalid){throw fail(PARCEL_NOT_ELIGIBLE);}long total=0;for(var line:lines)total=Math.addExact(total,line.feeVnd());Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);Quote quote=new Quote(UUID.randomUUID(),a.accountId(),request.draftId(),request.draftVersion(),rules.version(),r,request.parcels(),lines,total,rules.taxTreatment(),now,now.plusSeconds(rules.ttlSeconds()),null);String kid=store.activeKey();byte[] hash=store.fingerprint(kid,a.accountId(),input);try{store.insert(quote,key,kid,hash);}finally{Arrays.fill(hash,(byte)0);}a.requireCurrent();a.meaningfulAction();return quote;
    }
    @Override public Quote get(Access a,UUID id){current(a);if(id==null)throw fail(VALIDATION_FAILED);Quote q=store.find(a.accountId(),id,false).orElseThrow(()->fail(RESOURCE_NOT_FOUND));a.requireCurrent();return q;}
    @Override public UUID receipt(Access a,UUID key){current(a);if(key==null)throw fail(VALIDATION_FAILED);var q=store.byKey(a.accountId(),key).orElseThrow(()->fail(RESOURCE_NOT_FOUND)).quote();a.requireCurrent();return q.id();}
    @Override public Quote consume(Access a,UUID id,UUID draft,long version,UUID shipment){current(a);if(id==null||draft==null||version<1||shipment==null)throw fail(VALIDATION_FAILED);Quote q=store.find(a.accountId(),id,true).orElseThrow(()->fail(RESOURCE_NOT_FOUND));if(!q.draftId().equals(draft)||q.draftVersion()!=version)throw fail(PRECONDITION_STALE);if(q.consumedBy()!=null)throw fail(QUOTE_CONSUMED);Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);if(now.isBefore(q.createdAt())||!now.isBefore(q.expiresAt()))throw fail(QUOTE_EXPIRED);if(!store.rules(now).version().equals(q.tariffVersion()))throw fail(QUOTE_EXPIRED);store.consume(a.accountId(),id,shipment,now);a.requireCurrent();return new Quote(q.id(),q.accountId(),q.draftId(),q.draftVersion(),q.tariffVersion(),q.route(),q.parcels(),q.feeLines(),q.totalVnd(),q.taxTreatment(),q.createdAt(),q.expiresAt(),shipment);}
}
