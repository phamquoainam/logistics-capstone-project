package com.elogistic.backend.workflow.internal.web;

import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.notifications.api.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

@RestController @Profile("customer")
public final class PersonalAnalyticsHttp {
    private final CustomerAdmission admission;private final CustomerRequestContext requests;private final PersonalAnalytics analytics;
    public PersonalAnalyticsHttp(CustomerAdmission admission,CustomerRequestContext requests,PersonalAnalytics analytics){this.admission=admission;this.requests=requests;this.analytics=analytics;}
    private final class Scope implements PersonalAnalytics.Access {
        private final CustomerAdmission.Context context;
        Scope(CustomerAdmission.Context context){this.context=context;}
        public UUID accountId(){requireCurrent();return context.actor().accountId();}
        public UUID requestId(){requireCurrent();return context.requestId();}
        public void requireCurrent(){admission.requireCurrent(context);}
        public void meaningfulAction(){throw new IllegalStateException("ANALYTICS_READ_CANNOT_EXTEND_ACTIVITY");}
    }
    @GetMapping("/api/v1/customer/personal-analytics") public PersonalAnalytics.Report read(HttpServletRequest request){
        Map<String,String> parameters=new HashMap<>();
        request.getParameterMap().forEach((key,values)->{if(values.length!=1)throw problem(400,"VALIDATION_FAILED");parameters.put(key,values[0]);});
        try{var current=requests.current();return admission.execute(current.encodedAccess(),current.requestId(),Set.of(),context->analytics.read(new Scope(context),Map.copyOf(parameters)));}
        catch(AnalyticsFailure failure){int status=switch(failure.code()){case VALIDATION_FAILED->400;case ANALYTICS_RANGE_TOO_LARGE->422;default->503;};throw problem(status,failure.code().name());}
    }
    @GetMapping("/api/v1/customer/personal-shipping-reports") public PersonalAnalytics.ShippingReport shippingReport(HttpServletRequest request){
        Map<String,String> parameters=new HashMap<>();
        request.getParameterMap().forEach((key,values)->{if(values.length!=1)throw problem(400,"VALIDATION_FAILED");parameters.put(key,values[0]);});
        try{var current=requests.current();return admission.execute(current.encodedAccess(),current.requestId(),Set.of(),context->analytics.readShippingReport(new Scope(context),Map.copyOf(parameters)));}
        catch(AnalyticsFailure failure){int status=switch(failure.code()){case VALIDATION_FAILED->400;case ANALYTICS_RANGE_TOO_LARGE->422;default->503;};throw problem(status,failure.code().name());}
    }

}
