package com.elogistic.backend.orders.internal.domain;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record ShipmentView(UUID id,UUID merchantId,String reference,String pickupAddress,String recipientName,
 String recipientPhone,String deliveryStreet,String deliveryWard,String deliveryProvince,String parcelDescription,
 BigDecimal weightKg,String status,UUID createdBy,Instant createdAt,Instant updatedAt) {}
