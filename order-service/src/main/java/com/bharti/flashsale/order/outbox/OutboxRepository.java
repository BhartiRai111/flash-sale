package com.bharti.flashsale.order.outbox;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * FOR UPDATE SKIP LOCKED lets several order-service instances publish in parallel
     * without picking up the same rows twice.
     */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status = 'NEW'
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextBatch(@Param("limit") int limit);

    long countByStatus(OutboxStatus status);
}
