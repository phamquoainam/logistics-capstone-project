package com.elogistic.backend.orders.internal.domain;
import java.time.Instant;
import java.util.UUID;
public record ShipmentEvent(UUID id,String eventType,UUID actorId,String reason,Instant occurredAt) {}
