package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.merchant.api.AdminApplicationFacts;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** V14 Draft is not a submitted application or an approval decision. */
@Component @Profile("identity")
public final class AdminApplicationSource implements AdminApplicationFacts {
    public String availability(Access actor){actor.requireApplicationFacts();return "SOURCE_NOT_IMPLEMENTED";}
}
