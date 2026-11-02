package com.bharti.flashsale.payment.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.bharti.flashsale.common.Topics;
import com.bharti.flashsale.common.event.InventoryReservedEvent;
import com.bharti.flashsale.payment.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.INVENTORY_RESERVED)
    public void onInventoryReserved(String payload) throws JsonProcessingException {
        paymentService.process(objectMapper.readValue(payload, InventoryReservedEvent.class));
    }
}
