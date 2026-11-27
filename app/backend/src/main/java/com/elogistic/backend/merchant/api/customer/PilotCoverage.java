package com.elogistic.backend.merchant.api.customer;
import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import java.time.Instant;
import java.util.*;
public interface PilotCoverage {
    record Area(String code,String region,String label,String dropOffCode){}
    record Route(String datasetVersion,String originCode,String destinationCode,String classification,String dropOffCode){}
    record Catalog(String datasetVersion,String namespace,String provenance,Instant effectiveFrom,List<Area> areas,List<Route> routes){public Catalog{areas=List.copyOf(areas);routes=List.copyOf(routes);}}
    Catalog catalog(CustomerAdmission.Context context);
    default Catalog catalog(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context context){throw new IllegalStateException("OPS_CONTEXT_REQUIRED");}
    Route eligible(CustomerAdmission.Context context,String origin,String destination);
    default <T>T withIntakeOrganizationLock(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context actor,java.util.function.Supplier<T> work){throw new IllegalStateException("BUSINESS_INTAKE_UNAVAILABLE");}
    default void requireIntakeOrganization(com.elogistic.backend.identity.api.staff.OperationalStaffAdmission.Context actor,UUID organizationId,UUID facilityId){throw new IllegalStateException("BUSINESS_INTAKE_UNAVAILABLE");}
}
