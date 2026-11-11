package com.elogistic.backend.orders.api;

import com.elogistic.backend.merchant.api.customer.BusinessWorkspaces;
import java.time.Instant;
import java.util.*;

/** Organization preparation is not a confirmed Shipment or Carrier instruction. */
public interface BusinessPreparations {
    interface Access {
        UUID actorAccountId(); UUID requestId(); BusinessWorkspaces.Scope scope();
        void requireCurrent(); void meaningfulAction();
    }
    record Input(UUID facilityId,String sourceChannel,String sourceOrderReference,String reference,String instructions,
                 ShippingDrafts.Contact sender,ShippingDrafts.Contact recipient,List<ShippingDrafts.Parcel> parcels) {
        public Input { if(parcels!=null)parcels=List.copyOf(parcels); }
        public ShippingDrafts.Input shipping(){return new ShippingDrafts.Input(reference,instructions,sender,recipient,parcels);}
    }
    record Draft(UUID id,UUID organizationId,UUID facilityId,UUID creatorAccountId,String state,String preparationState,
                 long version,ShippingDrafts.Input input,String sourceChannel,String sourceOrderReference,
                 Instant createdAt,Instant updatedAt,List<String> allowedActions) {
        public Draft { allowedActions=List.copyOf(allowedActions); }
    }
    record Page(List<Draft> items,int page,int size,boolean hasNext){public Page{items=List.copyOf(items);}}
    record Receipt(String operationId,UUID targetId,String outcome,long version){}
    Draft create(Access access,Input input,UUID key);
    Draft update(Access access,UUID id,Input input,long expected,UUID key);
    Draft archive(Access access,UUID id,long expected,UUID key);
    Draft get(Access access,UUID id);
    Page list(Access access,int page);
    Receipt receipt(Access access,String operation,UUID key);
    final class Failure extends RuntimeException { public Failure(String code){super(code);} }
}
