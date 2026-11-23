package com.elogistic.backend.identity.api.admin;

import java.util.function.Function;

/** Executes the callback in one current-authority, read-only repeatable-read transaction. */
public interface AdminAdmission { <T>T execute(Function<AdminReadContext,T> work); }
