package com.bharti.flashsale.order.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.bharti.flashsale.common.Topics;
import com.bharti.flashsale.common.event.InventoryFailedEvent;
import com.bharti.flashsale.common.event.PaymentCompletedEvent;
import com.bharti.flashsale.common.event.PaymentFailedEvent;
import com.bharti.flashsale.order.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/** Moves the order to its final state based on events from inventory and payment. */
@Component
@RequiredArgsConstructor
public class SagaEventListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.INVENTORY_FAILED)
    public void onInventoryFailed(String payload) throws JsonProcessingException {
        InventoryFailedEvent event = objectMapper.readValue(payload, InventoryFailedEvent.class);
        orderService.cancel(event.orderId(), event.reason());
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED)
    public void onPaymentCompleted(String payload) throws JsonProcessingException {
        PaymentCompletedEvent event = objectMapper.readValue(payload, PaymentCompletedEvent.class);
        orderService.confirm(event.orderId());
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED)
    public void onPaymentFailed(String payload) throws JsonProcessingException {
        PaymentFailedEvent event = objectMapper.readValue(payload, PaymentFailedEvent.class);
        orderService.cancel(event.orderId(), event.reason());
    }
}
