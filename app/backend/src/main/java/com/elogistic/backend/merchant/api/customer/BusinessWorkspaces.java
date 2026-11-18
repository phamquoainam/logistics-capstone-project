package com.elogistic.backend.merchant.api.customer;

import java.util.*;
import java.util.function.Function;

/** Current Merchant authorization, inside the Identity-owned physical transaction. */
public interface BusinessWorkspaces {
    interface Access {
        UUID accountId(); void requireCurrent(); boolean eligibleAccount(UUID account);
    }
    record Context(UUID organizationId,String displayName,UUID membershipId,String role,List<String> permissions,long authorityVersion) {
        public Context { permissions=List.copyOf(permissions); }
    }
    record Contexts(String schemaVersion,List<Context> items) { public Contexts { items=List.copyOf(items); } }
    record AssignmentMember(UUID accountId,UUID membershipId,String role){}
    interface Scope {
        Context context(); void requireCurrent();
        /** Null facility requires organization-wide authority; never widens a facility grant. */
        boolean permits(String permission,UUID facilityId);
        void requireActiveFacility(UUID facilityId);
        Set<UUID> facilities(String permission);
        default boolean eligibleFacilityMember(UUID account,UUID facility){throw new Failure("BUSINESS_MEMBERSHIP_QUERY_UNAVAILABLE");}
        default List<AssignmentMember> facilityMembers(UUID facility,int page){throw new Failure("BUSINESS_MEMBERSHIP_QUERY_UNAVAILABLE");}
        default boolean memberExists(UUID account){throw new Failure("BUSINESS_MEMBERSHIP_QUERY_UNAVAILABLE");}
        default boolean facilityExists(UUID facility){throw new Failure("BUSINESS_FACILITY_QUERY_UNAVAILABLE");}
        default boolean eligibleSupportAssignee(UUID account,UUID facility){throw new Failure("BUSINESS_MEMBERSHIP_QUERY_UNAVAILABLE");}
        default List<AssignmentMember> supportAssignmentMembers(UUID facility,int page){throw new Failure("BUSINESS_MEMBERSHIP_QUERY_UNAVAILABLE");}
        default boolean eligibleAssignee(UUID account,UUID facility){throw new Failure("BUSINESS_MEMBERSHIP_QUERY_UNAVAILABLE");}
        default boolean eligibleActionAssignee(UUID account,UUID facility){throw new Failure("BUSINESS_MEMBERSHIP_QUERY_UNAVAILABLE");}
        default List<AssignmentMember> actionAssignmentMembers(UUID facility,int page){throw new Failure("BUSINESS_MEMBERSHIP_QUERY_UNAVAILABLE");}
        default List<AssignmentMember> assignmentMembers(UUID facility,int page){throw new Failure("BUSINESS_MEMBERSHIP_QUERY_UNAVAILABLE");}
    }
    /** Discovery only. Call again with Identity locking these referenced subjects. */
    Set<UUID> references(Access access,UUID organizationId);
    Contexts list(Access access);
    <T>T inOrganization(Access access,UUID organizationId,Function<Scope,T> read);
    final class Failure extends RuntimeException {
        public Failure(String code){super(code);}
    }
}
