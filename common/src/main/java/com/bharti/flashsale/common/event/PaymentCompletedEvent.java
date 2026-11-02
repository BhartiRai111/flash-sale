package com.bharti.flashsale.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Published by payment-service when the charge succeeded. */
public record PaymentCompletedEvent(
        UUID orderId,
        UUID paymentId,
        BigDecimal amount,
        Instant occurredAt) {
}
