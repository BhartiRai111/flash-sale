package com.bharti.flashsale.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

/**
 * Stand-in for a real payment provider (Razorpay, Stripe...). It can:
 *  - decline: amount above the configured limit -> permanent failure, never retried
 *  - time out: random transient error (failure-rate) -> retried by Resilience4j
 */
@Slf4j
@Component
public class MockPaymentGateway {

    private final BigDecimal maxAmount;
    private final double failureRate;

    public MockPaymentGateway(@Value("${payment.mock.max-amount}") BigDecimal maxAmount,
                              @Value("${payment.mock.failure-rate}") double failureRate) {
        this.maxAmount = maxAmount;
        this.failureRate = failureRate;
    }

    @Retry(name = "paymentGateway")
    @CircuitBreaker(name = "paymentGateway")
    public String charge(UUID orderId, BigDecimal amount) {
        if (amount.compareTo(maxAmount) > 0) {
            throw new PaymentDeclinedException("Amount " + amount + " exceeds card limit " + maxAmount);
        }
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            log.warn("Simulated gateway timeout for order {}", orderId);
            throw new PaymentGatewayException("Payment provider timed out");
        }
        return "txn_" + UUID.randomUUID();
    }

    /** Permanent failure: retrying will not help. */
    public static class PaymentDeclinedException extends RuntimeException {
        public PaymentDeclinedException(String message) {
            super(message);
        }
    }

    /** Transient failure: worth retrying. */
    public static class PaymentGatewayException extends RuntimeException {
        public PaymentGatewayException(String message) {
            super(message);
        }
    }
}
