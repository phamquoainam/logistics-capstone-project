package com.elogistic.backend.dispatch.internal.infrastructure;

import com.elogistic.backend.dispatch.api.AdminNetworkFacts;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** SIM-DROP points cannot be counted as an official Carrier network. */
@Component @Profile("identity")
public final class AdminNetworkSource implements AdminNetworkFacts {
    public String availability(Access actor){actor.requireNetworkFacts();return "SOURCE_NOT_IMPLEMENTED";}
}
