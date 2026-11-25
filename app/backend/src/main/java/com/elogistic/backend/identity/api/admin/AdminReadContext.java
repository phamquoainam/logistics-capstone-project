package com.elogistic.backend.identity.api.admin;

import java.util.Set;
import java.util.UUID;

/** Callback-scoped SYSTEM authority. Never an HTTP input or a persisted grant issuer. */
public interface AdminReadContext {
    UUID accountId(); UUID realmId(); String displayName(); String environment();
    String permissionVersion(); Set<String> permissions();
    void requireCurrent();
    default void require(String permission) { requireCurrent(); if(!permissions().contains(permission))throw new AdminFailure("ACCESS_DENIED"); }
    String ENTRY="ADMIN_DASHBOARD_READ",MERCHANT="ADMIN_MERCHANT_READ",NETWORK="ADMIN_NETWORK_READ",
        SHIPMENT="ADMIN_SHIPMENT_READ",FINANCE="ADMIN_FINANCE_READ",TECHNICAL="ADMIN_TECHNICAL_READ";
    Set<String> CODES=Set.of(ENTRY,MERCHANT,NETWORK,SHIPMENT,FINANCE,TECHNICAL);
}
