package com.bharti.flashsale.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Published by inventory-service when stock was reserved for an order. */
public record InventoryReservedEvent(
        UUID orderId,
        String userId,
        Long productId,
        int quantity,
        BigDecimal amount,
        Instant occurredAt) {
}
