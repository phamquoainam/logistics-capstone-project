package com.elogistic.backend.orders.internal.application.port;

import com.elogistic.backend.orders.internal.domain.DraftInput;
import com.elogistic.backend.orders.internal.domain.ShipmentEvent;
import com.elogistic.backend.orders.internal.domain.ShipmentView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Module-local port preserving the existing persistence operations and transaction caller. */
public interface ShipmentStore {
    record Receipt(String hash, UUID shipmentId) {}
    record Summary(UUID id, UUID merchantId, String reference, String status, Instant createdAt) {}
    record Overview(long total, long draft, long cancelled) {}
    List<ShipmentView> list(UUID merchant);
    Optional<ShipmentView> find(UUID merchant, UUID id, boolean lock);
    List<ShipmentEvent> events(UUID shipment);
    Optional<Receipt> receipt(UUID merchant, String operation, String key);
    void receipt(UUID merchant, String operation, String key, String hash, UUID shipment);
    void create(UUID id, UUID merchant, UUID actor, DraftInput input);
    void cancel(UUID merchant, UUID shipment, UUID actor, String reason);
    List<Summary> summaries();
    Overview overview();
}
