package com.bharti.flashsale.inventory.messaging;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Sends an event and waits for the broker's ack. If the send fails, the exception makes the
 * Kafka listener retry the incoming message; the reservation table then lets us replay the
 * same outcome without touching stock again.
 */
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload).get(10, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize " + event, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing to " + topic, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Could not publish to " + topic, e);
        }
    }
}
