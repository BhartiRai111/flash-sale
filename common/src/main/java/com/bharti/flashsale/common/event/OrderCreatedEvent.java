package com.bharti.flashsale.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Published by order-service (via outbox) when a new order is placed. */
public record OrderCreatedEvent(
        UUID orderId,
        String userId,
        String customerEmail,
        Long productId,
        int quantity,
        BigDecimal amount,
        Instant occurredAt) {
}
