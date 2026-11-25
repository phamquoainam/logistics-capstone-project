package com.elogistic.backend.workflow.internal;

import com.elogistic.backend.merchant.api.MerchantApplicationNoticeEvents;
import com.elogistic.backend.notifications.api.MerchantApplicationNotices;
import com.elogistic.backend.workflow.internal.application.MerchantNoticeComposition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration @Profile("customer")
@ConditionalOnProperty(name={"merchant.application-workflow.enabled","merchant.admin-governance.enabled"},havingValue="true")
public class MerchantNoticeConfiguration {
    @Bean MerchantNoticeComposition merchantNoticeComposition(MerchantApplicationNoticeEvents events,MerchantApplicationNotices notices){return new MerchantNoticeComposition(events,notices);}
}
