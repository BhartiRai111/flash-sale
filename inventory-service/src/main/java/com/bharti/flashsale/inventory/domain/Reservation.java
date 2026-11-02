package com.bharti.flashsale.inventory.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One row per order. Keyed by orderId, so a duplicate OrderCreated event can never reserve stock twice
 * (this is what makes the Kafka consumer idempotent).
 */
@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public static Reservation reserved(UUID orderId, Long productId, int quantity) {
        return of(orderId, productId, quantity, ReservationStatus.RESERVED, null);
    }

    public static Reservation rejected(UUID orderId, Long productId, int quantity, String reason) {
        return of(orderId, productId, quantity, ReservationStatus.REJECTED, reason);
    }

    private static Reservation of(UUID orderId, Long productId, int quantity, ReservationStatus status, String reason) {
        Reservation r = new Reservation();
        r.orderId = orderId;
        r.productId = productId;
        r.quantity = quantity;
        r.status = status;
        r.reason = reason;
        return r;
    }

    public void release(String reason) {
        this.status = ReservationStatus.RELEASED;
        this.reason = reason;
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
