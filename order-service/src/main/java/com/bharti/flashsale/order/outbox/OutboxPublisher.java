package com.bharti.flashsale.order.outbox;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Polls the outbox table and publishes pending events to Kafka.
 * Delivery is at-least-once, so every consumer in the system is idempotent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final int BATCH_SIZE = 100;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:500}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = outboxRepository.lockNextBatch(BATCH_SIZE);
        for (OutboxEvent event : batch) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                event.markSent();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                event.recordFailure("interrupted");
                return;
            } catch (Exception e) {
                // Stop here to keep events in order; the rest are retried on the next poll
                log.warn("Outbox publish failed for event {} ({}): {}", event.getId(), event.getTopic(), e.getMessage());
                event.recordFailure(e.getMessage());
                return;
            }
        }
        if (!batch.isEmpty()) {
            log.debug("Published {} outbox events", batch.size());
        }
    }
}
