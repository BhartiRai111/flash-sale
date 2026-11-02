package com.bharti.flashsale.order.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.bharti.flashsale.order.domain.Order;
import com.bharti.flashsale.order.domain.OrderStatus;

public record OrderResponse(
        UUID id,
        String userId,
        Long productId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        OrderStatus status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getUserId(), order.getProductId(), order.getQuantity(),
                order.getUnitPrice(), order.getAmount(), order.getStatus(), order.getFailureReason(),
                order.getCreatedAt(), order.getUpdatedAt());
    }
}
