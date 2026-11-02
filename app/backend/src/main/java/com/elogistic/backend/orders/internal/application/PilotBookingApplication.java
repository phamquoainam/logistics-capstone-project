package com.elogistic.backend.orders.internal.application;
import com.elogistic.backend.orders.api.*;
import com.elogistic.backend.orders.internal.application.port.*;
import com.elogistic.backend.pricing.api.PilotQuotes;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.elogistic.backend.orders.api.PilotBookingFailure.Code.*;
public final class PilotBookingApplication implements PilotShipments {
    private final ShippingDraftStore drafts;private final PilotShipmentStore store;private final Clock clock;private final boolean enabled;
    public PilotBookingApplication(ShippingDraftStore drafts,PilotShipmentStore store,Clock clock,boolean enabled){this.drafts=drafts;this.store=store;this.clock=clock;this.enabled=enabled;}
    private static PilotBookingFailure fail(PilotBookingFailure.Code code){return new PilotBookingFailure(code);}
    private void current(ShippingDrafts.Access a){a.requireCurrent();if(!enabled)throw fail(PILOT_DISABLED);}
    private ShippingDrafts.Draft own(ShippingDrafts.Access a,UUID id){if(id==null)throw fail(VALIDATION_FAILED);return drafts.find(a.accountId(),id).orElseThrow(()->fail(RESOURCE_NOT_FOUND));}
    @Override public Optional<Shipment> replay(ShippingDrafts.Access a,UUID draft,long version,UUID quote,UUID key){current(a);if(key==null||quote==null||version<1)throw fail(VALIDATION_FAILED);drafts.lockOwner(a.accountId());own(a,draft);var previous=store.receipt(a.accountId(),key);if(previous.isEmpty())return Optional.empty();var r=previous.get();if(!r.draft().equals(draft)||r.version()!=version||!r.quote().equals(quote))throw fail(IDEMPOTENCY_CONFLICT);Shipment result=store.find(a.accountId(),r.shipment());a.requireCurrent();return Optional.of(result);}
    @Override public ShippingDrafts.Draft requireBookable(ShippingDrafts.Access a,UUID id,long version){current(a);drafts.lockOwner(a.accountId());var d=own(a,id);if(!d.state().equals("DRAFT"))throw fail(DRAFT_ALREADY_BOOKED);if(d.version()!=version)throw fail(PRECONDITION_STALE);return d;}
    @Override public Shipment confirm(ShippingDrafts.Access a,UUID id,long version,UUID quoteId,UUID key,UUID shipmentId,PilotQuotes.Quote q){
        current(a);var d=requireBookable(a,id,version);if(q==null||shipmentId==null||key==null||!q.id().equals(quoteId)||!q.accountId().equals(a.accountId())||!q.draftId().equals(id)||q.draftVersion()!=version||!shipmentId.equals(q.consumedBy())||q.totalVnd()<1||d.input().parcels().size()!=q.parcels().size()||!clock.instant().isBefore(q.expiresAt()))throw fail(QUOTE_INVALID);
        for(int i=0;i<q.parcels().size();i++){var p=d.input().parcels().get(i);var quoted=q.parcels().get(i);if(p.weightGrams()!=quoted.weightGrams()||p.lengthMm()!=quoted.lengthMm()||p.widthMm()!=quoted.widthMm()||p.heightMm()!=quoted.heightMm())throw fail(QUOTE_INVALID);}
        Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);List<Parcel> parcels=new ArrayList<>();int number=0;for(var p:d.input().parcels())parcels.add(new Parcel(UUID.randomUUID(),++number,p));var result=new Shipment(shipmentId,a.accountId(),"AP-SIM-"+shipmentId.toString().replace("-",""),id,version,"CONFIRMED","NOT_ACCEPTED",d.input(),q,q.totalVnd(),now,parcels);store.insert(result);drafts.save(d,new ShippingDrafts.Draft(id,a.accountId(),"BOOKED",d.input(),version+1,d.createdAt(),now),"confirmPilotBooking",a.requestId());store.receipt(a.accountId(),key,new PilotShipmentStore.BookingReceipt(id,version,quoteId,shipmentId),now);a.requireCurrent();a.meaningfulAction();return result;
    }
    @Override public Shipment get(ShippingDrafts.Access a,UUID id){current(a);if(id==null)throw fail(VALIDATION_FAILED);var result=store.find(a.accountId(),id);a.requireCurrent();return result;}
    @Override public Page list(ShippingDrafts.Access a,int page,String search,String status){current(a);if(page<0||search!=null&&(search.isBlank()||search.length()>160||search.codePoints().anyMatch(Character::isISOControl))||status!=null&&!Set.of("WAITING_FOR_HANDOVER","AT_STATION").contains(status))throw fail(VALIDATION_FAILED);var rows=store.list(a.accountId(),page,search,status);a.requireCurrent();return new Page(rows.stream().limit(20).toList(),page,20,rows.size()>20);}
    @Override public Receipt receipt(ShippingDrafts.Access a,UUID key){current(a);if(key==null)throw fail(VALIDATION_FAILED);var r=store.receipt(a.accountId(),key).orElseThrow(()->fail(RESOURCE_NOT_FOUND));store.find(a.accountId(),r.shipment());a.requireCurrent();return new Receipt(r.shipment(),"COMMITTED");}
}
