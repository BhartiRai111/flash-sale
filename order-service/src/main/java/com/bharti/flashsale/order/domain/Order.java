package com.bharti.flashsale.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Entity name avoids clashing with the ORDER keyword in JPQL; the table is still "orders"
@Entity(name = "PurchaseOrder")
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    /** Unique per user request: retries with the same key return the same order. */
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic locking: protects against two saga events updating the same order at once. */
    @Version
    private Long version;

    public static Order create(String userId, String customerEmail, Long productId, int quantity,
                               BigDecimal unitPrice, String idempotencyKey) {
        Order order = new Order();
        order.userId = userId;
        order.customerEmail = customerEmail;
        order.productId = productId;
        order.quantity = quantity;
        order.unitPrice = unitPrice;
        order.amount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        order.status = OrderStatus.PENDING;
        order.idempotencyKey = idempotencyKey;
        return order;
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public void confirm() {
        this.status = OrderStatus.CONFIRMED;
        this.failureReason = null;
    }

    public void cancel(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.failureReason = reason;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
