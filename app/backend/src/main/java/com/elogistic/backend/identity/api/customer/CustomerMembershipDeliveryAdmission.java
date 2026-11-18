package com.elogistic.backend.identity.api.customer;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Predicate;
/** Worker-only current Merchant authority; caller owns Identity admission/locks and eligibility. */
public interface CustomerMembershipDeliveryAdmission {
    boolean current(UUID invitation,UUID proof,UUID organization,UUID issuer,Instant now,Predicate<UUID> eligibleAccount);
}
