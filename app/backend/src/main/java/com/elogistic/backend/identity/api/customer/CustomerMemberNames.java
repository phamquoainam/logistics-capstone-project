package com.elogistic.backend.identity.api.customer;
import java.util.*;
/** Names and eligibility only for Merchant-discovered IDs under current admission. No global search or credentials. */
public interface CustomerMemberNames {
    Map<UUID,String> read(CustomerAdmission.Context context,Set<UUID> memberAccountIds);
    /** Owner-approved Support refinement: IDs discovered under Merchant scope only, no global search. */
    default Set<UUID> eligibleAccounts(CustomerAdmission.Context context,Set<UUID> memberAccountIds){throw new IllegalStateException("CUSTOMER_ELIGIBILITY_PORT_REQUIRED");}
}
