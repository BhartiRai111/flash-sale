package com.bharti.flashsale.order.outbox;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Transactional outbox row. Written in the SAME database transaction as the order change,
 * then published to Kafka by {@link OutboxPublisher}. This guarantees an event is never lost
 * even if Kafka is down when the order is saved.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Used as the Kafka message key, so all events of one order go to the same partition (ordering). */
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String topic;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    public static OutboxEvent of(String aggregateId, String topic, String eventType, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.aggregateId = aggregateId;
        event.topic = topic;
        event.eventType = eventType;
        event.payload = payload;
        event.status = OutboxStatus.NEW;
        event.attempts = 0;
        event.createdAt = Instant.now();
        return event;
    }

    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.sentAt = Instant.now();
        this.attempts++;
        this.lastError = null;
    }

    public void recordFailure(String error) {
        this.attempts++;
        this.lastError = error == null ? "unknown" : error.substring(0, Math.min(error.length(), 500));
    }
}
