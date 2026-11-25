package com.elogistic.backend.identity.api.admin;

import java.util.Set;

/** Finite public problem, no raw SQL/provider exception or actor information. */
public final class AdminFailure extends RuntimeException {
    private final String code;
    public AdminFailure(String code) { super(Set.of("NOT_ACCEPTABLE","VALIDATION_FAILED","SESSION_INVALID","ACCESS_DENIED","SNAPSHOT_EXPIRED","SOURCE_UNAVAILABLE","SERVICE_UNAVAILABLE","IDEMPOTENCY_CONFLICT","RESOURCE_NOT_FOUND").contains(code)?code:"SERVICE_UNAVAILABLE"); this.code=getMessage(); }
    public String code(){return code;}
}
