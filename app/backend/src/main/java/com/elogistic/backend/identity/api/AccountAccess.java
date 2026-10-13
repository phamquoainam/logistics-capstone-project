package com.elogistic.backend.identity.api;
import java.util.Optional;
import java.util.UUID;
public interface AccountAccess { Optional<AccountView> find(UUID id); }
