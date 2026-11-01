package com.elogistic.backend.orders.internal.application.port;
import java.util.UUID;
public interface ShippingDraftFingerprints {
    String activeKey();
    byte[] hash(String keyId,UUID owner,String operation,UUID target,long version,String payload);
}
