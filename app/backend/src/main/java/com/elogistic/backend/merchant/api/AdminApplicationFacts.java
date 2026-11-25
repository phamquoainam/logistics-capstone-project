package com.elogistic.backend.merchant.api;

import com.elogistic.backend.identity.api.admin.*;
import java.time.Instant;
import java.util.*;

/** P0-PORT-05; source facts are not a new wire DTO or an authority issuer. */
public interface AdminApplicationFacts {
    interface Access {
        void requireApplicationFacts();
        /** P0-FACTS-CTX-01: only the Identity owner verifies issuance. */
        default AdminReadContext identityContext(){throw new AdminFailure("ACCESS_DENIED");}
    }
    record ApplicationCohort(Instant from,Instant until,String state,UUID policyId){}
    record Row(UUID id,String state,Instant submittedAt,Instant updatedAt,Instant terminalAt,Instant lastSourceEventAt){}
    record Capture(String availability,String reasonCode,String resourceScopeVersion,String fingerprint,List<Row> rows){public Capture{rows=List.copyOf(rows);}}
    record Selector(String metric,Instant cutoff,String resourceScopeVersion,String fingerprint){}
    record Page(List<Row> items,int page,int size,boolean hasNext){public Page{items=List.copyOf(items);}}
    String availability(Access actor);
    default Capture capture(Access actor,ApplicationCohort query,Instant cutoff){throw new AdminFailure("ACCESS_DENIED");}
    default Page page(Access actor,ApplicationCohort query,Selector selector,int page,int size){throw new AdminFailure("ACCESS_DENIED");}
}
