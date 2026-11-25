package com.elogistic.backend.identity.api.admin;

import java.util.function.Function;

/** Executes the callback in one current-authority, read-only repeatable-read transaction. */
public interface AdminAdmission {
    <T>T execute(Function<AdminReadContext,T> work);
    /** Only the issuing Identity owner may validate callback authority. */
    default void requireCurrent(AdminReadContext context){throw new AdminFailure("ACCESS_DENIED");}
}
