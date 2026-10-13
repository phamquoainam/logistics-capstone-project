package com.elogistic.backend.identity.api;
import java.util.UUID;
public record AccountView(UUID id,String username,String displayName,String role,boolean enabled,UUID merchantId) implements AuthenticatedActor {}
