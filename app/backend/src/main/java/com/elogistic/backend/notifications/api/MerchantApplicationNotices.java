package com.elogistic.backend.notifications.api;

import com.elogistic.backend.identity.api.admin.AdminCommandContext;
import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import java.util.UUID;

/** P0-PORT-07. Values carry references only; implementation must verify Identity ownership. */
public interface MerchantApplicationNotices {
    void enqueue(Access access,ApplicationEvent event,CustomerAdmission.Subject recipient);
    record Access(CustomerAdmission.Context customer,AdminCommandContext admin) {
        public Access {if((customer==null)==(admin==null))throw new IllegalArgumentException("NOTICE_ADMISSION_REQUIRED");}
        @Override public String toString(){return "MerchantNoticeAccess[redacted]";}
    }
    record ApplicationEvent(UUID eventId,UUID applicationId,UUID applicantId,String template) {
        @Override public String toString(){return "MerchantNoticeEvent[redacted]";}
    }
}
