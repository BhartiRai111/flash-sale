package com.bharti.flashsale.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Published by order-service when an order reaches a final state (CONFIRMED or CANCELLED). */
public record OrderStatusChangedEvent(
        UUID orderId,
        String userId,
        String customerEmail,
        Long productId,
        int quantity,
        BigDecimal amount,
        String status,
        String reason,
        Instant occurredAt) {
}
