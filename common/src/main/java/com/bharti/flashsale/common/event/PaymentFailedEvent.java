package com.bharti.flashsale.common.event;

import java.time.Instant;
import java.util.UUID;

/** Published by payment-service when the charge failed. Triggers stock release (compensation). */
public record PaymentFailedEvent(
        UUID orderId,
        String reason,
        Instant occurredAt) {
}
