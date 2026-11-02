package com.bharti.flashsale.order.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /** Must run inside the caller's transaction so the event commits together with the business change. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void write(String aggregateId, String topic, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(OutboxEvent.of(aggregateId, topic, event.getClass().getSimpleName(), payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize event " + event, e);
        }
    }
}
