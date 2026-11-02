package com.bharti.flashsale.payment.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bharti.flashsale.common.Topics;
import com.bharti.flashsale.common.event.InventoryReservedEvent;
import com.bharti.flashsale.common.event.PaymentCompletedEvent;
import com.bharti.flashsale.common.event.PaymentFailedEvent;
import com.bharti.flashsale.payment.domain.Payment;
import com.bharti.flashsale.payment.domain.PaymentRepository;
import com.bharti.flashsale.payment.domain.PaymentStatus;
import com.bharti.flashsale.payment.gateway.MockPaymentGateway;
import com.bharti.flashsale.payment.gateway.MockPaymentGateway.PaymentDeclinedException;
import com.bharti.flashsale.payment.messaging.EventPublisher;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MockPaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;

    public void process(InventoryReservedEvent event) {
        // Idempotency: never charge the same order twice; just re-send the stored outcome
        Optional<Payment> existing = paymentRepository.findByOrderId(event.orderId());
        if (existing.isPresent()) {
            log.info("Order {} already charged ({}), replaying outcome", event.orderId(), existing.get().getStatus());
            publishOutcome(existing.get());
            return;
        }

        Payment payment;
        try {
            paymentGateway.charge(event.orderId(), event.amount());
            payment = Payment.completed(event.orderId(), event.amount());
        } catch (PaymentDeclinedException e) {
            payment = Payment.failed(event.orderId(), event.amount(), e.getMessage());
        } catch (CallNotPermittedException e) {
            payment = Payment.failed(event.orderId(), event.amount(), "Payment provider unavailable (circuit open)");
        } catch (RuntimeException e) {
            // Still failing after all retries: fail the order so the stock is released for other buyers
            payment = Payment.failed(event.orderId(), event.amount(), "Payment provider error: " + e.getMessage());
        }

        Payment saved = paymentRepository.saveAndFlush(payment);
        log.info("Order {} payment {}", event.orderId(), saved.getStatus());
        publishOutcome(saved);
    }

    public Optional<Payment> findByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    private void publishOutcome(Payment payment) {
        String key = payment.getOrderId().toString();
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            eventPublisher.publish(Topics.PAYMENT_COMPLETED, key,
                    new PaymentCompletedEvent(payment.getOrderId(), payment.getId(), payment.getAmount(), Instant.now()));
        } else {
            eventPublisher.publish(Topics.PAYMENT_FAILED, key,
                    new PaymentFailedEvent(payment.getOrderId(), payment.getFailureReason(), Instant.now()));
        }
    }
}
