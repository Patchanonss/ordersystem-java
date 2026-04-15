package com.ordersystem.orderservice.repository;

import com.ordersystem.orderservice.model.OutboxEvent;
import com.ordersystem.orderservice.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Fetches PENDING outbox events with a row-level lock.
     *
     * FOR UPDATE   — locks the selected rows so no other transaction can grab them.
     * SKIP LOCKED  — if another instance already locked a row, skip it instead of waiting.
     *
     * This makes it safe to run multiple Order Service instances (K8s replicas)
     * without duplicate Kafka publishes. Even if a duplicate slips through,
     * Inventory Service's idempotency check catches it.
     */
    @Query(value = "SELECT * FROM outbox_events " +
            "WHERE status = 'PENDING' " +
            "ORDER BY created_at ASC " +
            "LIMIT :batchSize " +
            "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> findPendingEventsWithLock(@Param("batchSize") int batchSize);

    /**
     * Deletes old SENT events to prevent the outbox table from growing forever.
     * FAILED and PENDING events are intentionally kept for investigation.
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = :status AND e.sentAt < :cutoff")
    int deleteByStatusAndSentAtBefore(
            @Param("status") OutboxStatus status,
            @Param("cutoff") LocalDateTime cutoff);
}
