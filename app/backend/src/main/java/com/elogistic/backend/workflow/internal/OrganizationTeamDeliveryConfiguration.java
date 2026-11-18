package com.elogistic.backend.workflow.internal;
import com.elogistic.backend.identity.api.customer.CustomerMembershipDeliveryAdmission;
import com.elogistic.backend.merchant.api.customer.OrganizationTeam;
import org.springframework.context.annotation.*;
@Configuration @Profile("customer")
public class OrganizationTeamDeliveryConfiguration {
    @Bean CustomerMembershipDeliveryAdmission membershipDeliveryAdmission(OrganizationTeam team){return team::deliveryCurrent;}
}
