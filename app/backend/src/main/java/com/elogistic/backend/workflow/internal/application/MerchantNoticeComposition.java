package com.elogistic.backend.workflow.internal.application;

import com.elogistic.backend.identity.api.admin.AdminCommandContext;
import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.merchant.api.MerchantApplicationNoticeEvents;
import com.elogistic.backend.notifications.api.MerchantApplicationNotices;
import java.util.*;

/** Workflow owns cross-domain orchestration within the existing Identity callback transaction. */
public final class MerchantNoticeComposition {
    private final MerchantApplicationNoticeEvents events;private final MerchantApplicationNotices notices;
    public MerchantNoticeComposition(MerchantApplicationNoticeEvents events,MerchantApplicationNotices notices){this.events=Objects.requireNonNull(events);this.notices=Objects.requireNonNull(notices);}
    public void after(AdminCommandContext context,UUID app,String operation,UUID key){events.commandEvent(context,app,operation,key).ifPresent(event->notices.enqueue(new MerchantApplicationNotices.Access(null,context),wire(event),context.customer(event.applicantId())));}
    public void after(CustomerAdmission.Context context,UUID app,String operation,UUID key){events.commandEvent(context,app,operation,key).ifPresent(event->notices.enqueue(new MerchantApplicationNotices.Access(context,null),wire(event),context.actor()));}
    private static MerchantApplicationNotices.ApplicationEvent wire(MerchantApplicationNoticeEvents.Event event){return new MerchantApplicationNotices.ApplicationEvent(event.eventId(),event.applicationId(),event.applicantId(),event.template());}
}
