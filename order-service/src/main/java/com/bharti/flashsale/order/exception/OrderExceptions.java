package com.bharti.flashsale.order.exception;

import java.util.UUID;

/** Business exceptions for order-service, mapped to HTTP status codes in GlobalExceptionHandler. */
public final class OrderExceptions {

    private OrderExceptions() {
    }

    /** 404 - product id does not exist in inventory-service. */
    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(Long productId) {
            super("Product " + productId + " not found");
        }
    }

    /** 404 - order does not exist or belongs to another user. */
    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(UUID orderId) {
            super("Order " + orderId + " not found");
        }
    }

    /** 409 - fast-fail check before creating the order (final check happens in inventory-service). */
    public static class OutOfStockException extends RuntimeException {
        public OutOfStockException(Long productId) {
            super("Product " + productId + " is sold out");
        }
    }

    /** 503 - inventory-service down or its circuit breaker is open. */
    public static class InventoryUnavailableException extends RuntimeException {
        public InventoryUnavailableException(Throwable cause) {
            super("Inventory service is unavailable, please retry", cause);
        }
    }
}
