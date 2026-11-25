package com.elogistic.backend.merchant.api;

import com.elogistic.backend.identity.api.admin.AdminCommandContext;
import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import java.util.Optional;
import java.util.UUID;

/** P0-NOTICE-COMPOSE-01. Only authoritative fresh command metadata crosses this boundary. */
public interface MerchantApplicationNoticeEvents {
    default Optional<Event> commandEvent(AdminCommandContext context,UUID applicationId,String operation,UUID operationKey){throw new GovernanceFailure(GovernanceFailure.Code.ACCESS_DENIED);}
    default Optional<Event> commandEvent(CustomerAdmission.Context context,UUID applicationId,String operation,UUID operationKey){throw new GovernanceFailure(GovernanceFailure.Code.ACCESS_DENIED);}
    record Event(UUID eventId,UUID applicationId,UUID applicantId,String template) {}
}
