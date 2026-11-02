package com.bharti.flashsale.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    MockPaymentGateway paymentGateway;

    @Mock
    EventPublisher eventPublisher;

    @InjectMocks
    PaymentService paymentService;

    private final UUID orderId = UUID.randomUUID();
    private final InventoryReservedEvent event = new InventoryReservedEvent(
            orderId, "alice", 1L, 1, new BigDecimal("14999.00"), Instant.now());

    @Test
    void successfulChargePublishesPaymentCompleted() {
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentGateway.charge(orderId, event.amount())).thenReturn("txn_1");
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.process(event);

        verify(eventPublisher).publish(eq(Topics.PAYMENT_COMPLETED), eq(orderId.toString()),
                any(PaymentCompletedEvent.class));
    }

    @Test
    void declinedChargeIsStoredAsFailedAndPublishesPaymentFailed() {
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentGateway.charge(orderId, event.amount())).thenThrow(new PaymentDeclinedException("limit"));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.process(event);

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(eventPublisher).publish(eq(Topics.PAYMENT_FAILED), eq(orderId.toString()),
                any(PaymentFailedEvent.class));
    }

    @Test
    void duplicateEventDoesNotChargeAgain() {
        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(Payment.completed(orderId, event.amount())));

        paymentService.process(event);

        verify(paymentGateway, never()).charge(any(), any());
        verify(eventPublisher).publish(eq(Topics.PAYMENT_COMPLETED), eq(orderId.toString()),
                any(PaymentCompletedEvent.class));
    }
}
