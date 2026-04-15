package com.ordersystem.orderservice.outbox;

import com.ordersystem.orderservice.model.OutboxStatus;
import com.ordersystem.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Periodically cleans up old SENT outbox events to prevent the table from growing forever.
 *
 * Only deletes events with status = SENT.
 * PENDING and FAILED events are intentionally kept for investigation and retry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxCleanupScheduler {

    private final OutboxEventRepository outboxEventRepository;

    @Value("${outbox.cleanup.retention-days:7}")
    private int retentionDays;

    @Scheduled(fixedDelayString = "${outbox.cleanup.interval-ms:3600000}")
    @Transactional
    public void cleanupOldSentEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int deletedCount = outboxEventRepository.deleteByStatusAndSentAtBefore(
                OutboxStatus.SENT, cutoff);

        if (deletedCount > 0) {
            log.info("Outbox cleanup: deleted {} SENT event(s) older than {} days",
                    deletedCount, retentionDays);
        }
    }
}
