package com.elogistic.backend.workflow.internal.web;
import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.dispatch.api.CarrierNetworkReads;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import static com.elogistic.backend.workflow.internal.web.CustomerProfileHttp.problem;

/** Admin network READ does not confer Operational Staff or provisioning authority. */
@RestController @Profile("identity")
public final class CarrierNetworkHttp {
    private final AdminAdmission admission;private final CarrierNetworkReads sites;
    public CarrierNetworkHttp(AdminAdmission admission,CarrierNetworkReads sites){this.admission=admission;this.sites=sites;}
    private CarrierNetworkReads.Access scope(AdminReadContext c){return ()->{admission.requireCurrent(c);c.require(AdminReadContext.ENTRY);c.require(AdminReadContext.NETWORK);};}
    @GetMapping("/api/v1/admin/carrier-sites") public CarrierNetworkReads.SitePage sites(@RequestParam(required=false)String state,@RequestParam(required=false)String type,@RequestParam(required=false)String areaCode,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){try{return admission.execute(c->sites.sites(scope(c),new CarrierNetworkReads.SiteQuery(state,type,areaCode,page,size)));}catch(IllegalArgumentException invalid){throw problem(400,"VALIDATION_FAILED");}}
    @GetMapping("/api/v1/admin/carrier-sites/{id}") public CarrierNetworkReads.SiteDetail site(@PathVariable String id){try{UUID value=UUID.fromString(id);if(!value.toString().equals(id))throw new IllegalArgumentException();return admission.execute(c->sites.site(scope(c),value));}catch(IllegalArgumentException invalid){throw problem(400,"VALIDATION_FAILED");}catch(NoSuchElementException absent){throw problem(404,"RESOURCE_NOT_FOUND");}}
    @ExceptionHandler(CustomerProfileHttp.HttpProblem.class) public org.springframework.http.ResponseEntity<Map<String,String>> invalid(CustomerProfileHttp.HttpProblem failure){return org.springframework.http.ResponseEntity.status(failure.status).header("Cache-Control","no-store").body(Map.of("code",failure.code,"message","Request rejected."));}
    @ExceptionHandler(AdminFailure.class) public org.springframework.http.ResponseEntity<Map<String,String>> denied(AdminFailure failure){int status=switch(failure.code()){case "SESSION_INVALID"->401;case "ACCESS_DENIED"->403;case "VALIDATION_FAILED"->400;default->503;};return org.springframework.http.ResponseEntity.status(status).header("Cache-Control","no-store").body(Map.of("code",failure.code(),"message","Request rejected."));}
}
