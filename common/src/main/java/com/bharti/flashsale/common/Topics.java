package com.bharti.flashsale.common;

/**
 * Kafka topic names shared by all services.
 *
 * Saga flow (choreography):
 *   order.created      -> inventory reserves stock -> inventory.reserved | inventory.failed
 *   inventory.reserved -> payment charges          -> payment.completed  | payment.failed
 *   payment.completed  -> order CONFIRMED          -> order.confirmed
 *   payment.failed     -> order CANCELLED + inventory releases stock (compensation)
 *   inventory.failed   -> order CANCELLED          -> order.cancelled
 */
public final class Topics {

    public static final String ORDER_CREATED = "order.created";
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_FAILED = "inventory.failed";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String ORDER_CONFIRMED = "order.confirmed";
    public static final String ORDER_CANCELLED = "order.cancelled";

    private Topics() {
    }
}
