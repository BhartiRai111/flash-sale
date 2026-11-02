package com.bharti.flashsale.common.event;

import java.time.Instant;
import java.util.UUID;

/** Published by inventory-service when stock could not be reserved (sold out, unknown product). */
public record InventoryFailedEvent(
        UUID orderId,
        Long productId,
        String reason,
        Instant occurredAt) {
}
