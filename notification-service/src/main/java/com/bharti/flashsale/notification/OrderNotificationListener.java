package com.bharti.flashsale.notification;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.bharti.flashsale.common.Topics;
import com.bharti.flashsale.common.event.OrderStatusChangedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Emails are at-least-once: in a rare redelivery the customer may get the same email twice,
 * which is an acceptable trade-off for a notification (unlike a payment).
 */
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {Topics.ORDER_CONFIRMED, Topics.ORDER_CANCELLED})
    public void onOrderStatusChanged(String payload) throws JsonProcessingException {
        emailService.sendOrderUpdate(objectMapper.readValue(payload, OrderStatusChangedEvent.class));
    }
}
