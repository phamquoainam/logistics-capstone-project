package com.elogistic.backend.merchant.internal.application.port;

import java.util.UUID;

/** External retained-key HMAC; receipts never persist contact/profile plaintext. */
public interface MerchantFingerprints {
    String activeKey();
    byte[] hash(String keyId,String operation,UUID target,long originalVersion,String... values);
}
