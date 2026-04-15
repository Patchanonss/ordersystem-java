package com.ordersystem.orderservice.outbox;

import com.ordersystem.orderservice.model.OutboxEvent;
import com.ordersystem.orderservice.model.OutboxStatus;
import com.ordersystem.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Polls the outbox_events table for PENDING events and publishes them to Kafka.
 *
 * This is the heart of the Outbox Pattern — it decouples the "write to DB"
 * step from the "publish to Kafka" step, making them independently reliable.
 *
 * Safety with multiple instances:
 * - FOR UPDATE SKIP LOCKED ensures each row is processed by exactly one instance.
 * - Even if a duplicate somehow gets through, the downstream Inventory Service
 *   has an idempotency check (processed_events table) that will catch it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> outboxKafkaTemplate;
    private final TransactionTemplate transactionTemplate; // เพิ่มตรงนี้

    @Value("${outbox.polling.batch-size:20}")
    private int batchSize;

    @Value("${outbox.max-retry-count:5}")
    private int maxRetryCount;

    @Scheduled(fixedDelayString = "${outbox.polling.interval-ms:5000}")
    public void publishPendingEvents() {
        // Step 1 — fetch events (transaction สั้นๆ แค่ read)
        List<OutboxEvent> pendingEvents = transactionTemplate.execute(status ->
                outboxEventRepository.findPendingEventsWithLock(batchSize));

        if (pendingEvents == null || pendingEvents.isEmpty()) return;

        log.info("Outbox publisher found {} pending event(s)", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // Step 2 — send Kafka (ไม่มี DB connection ตรงนี้)
                outboxKafkaTemplate
                        .send(event.getTopic(), event.getPartitionKey(), event.getPayload())
                        .get(10, TimeUnit.SECONDS);

                // Step 3 — update status (transaction สั้นๆ แค่ write)
                transactionTemplate.execute(status -> {
                    event.setStatus(OutboxStatus.SENT);
                    event.setSentAt(LocalDateTime.now());
                    return outboxEventRepository.save(event);
                });

                log.info("Outbox event published: id={}, type={}", event.getId(), event.getEventType());

            } catch (Exception e) {
                transactionTemplate.execute(status -> {
                    event.setRetryCount(event.getRetryCount() + 1);
                    if (event.getRetryCount() >= maxRetryCount) {
                        event.setStatus(OutboxStatus.FAILED);
                        log.error("Outbox event FAILED after {} retries: id={}", maxRetryCount, event.getId());
                    } else {
                        log.warn("Outbox retry {}/{}: id={}", event.getRetryCount(), maxRetryCount, event.getId());
                    }
                    return outboxEventRepository.save(event);
                });
            }
        }
    }
}