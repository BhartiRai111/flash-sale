package com.bharti.flashsale.payment.domain;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Unique: an order can be charged at most once, even if the event is delivered twice. */
    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static Payment completed(UUID orderId, BigDecimal amount) {
        return of(orderId, amount, PaymentStatus.COMPLETED, null);
    }

    public static Payment failed(UUID orderId, BigDecimal amount, String reason) {
        return of(orderId, amount, PaymentStatus.FAILED, reason);
    }

    private static Payment of(UUID orderId, BigDecimal amount, PaymentStatus status, String reason) {
        Payment payment = new Payment();
        payment.orderId = orderId;
        payment.amount = amount;
        payment.status = status;
        payment.failureReason = reason;
        return payment;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
