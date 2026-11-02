package com.bharti.flashsale.inventory.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.bharti.flashsale.common.Topics;
import com.bharti.flashsale.common.event.OrderCreatedEvent;
import com.bharti.flashsale.common.event.PaymentFailedEvent;
import com.bharti.flashsale.inventory.service.StockReservationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final StockReservationService reservationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.ORDER_CREATED)
    public void onOrderCreated(String payload) throws JsonProcessingException {
        reservationService.reserve(objectMapper.readValue(payload, OrderCreatedEvent.class));
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED)
    public void onPaymentFailed(String payload) throws JsonProcessingException {
        reservationService.release(objectMapper.readValue(payload, PaymentFailedEvent.class));
    }
}
