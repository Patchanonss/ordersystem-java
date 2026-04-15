package com.ordersystem.orderservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status, createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Source entity type, e.g. "Order" */
    @Column(nullable = false)
    private String aggregateType;

    /** Source entity ID, e.g. the orderId */
    @Column(nullable = false)
    private String aggregateId;

    /** Descriptive event name, e.g. "ORDER_CREATED" */
    @Column(nullable = false)
    private String eventType;

    /** Kafka topic to publish to */
    @Column(nullable = false)
    private String topic;

    /** Kafka message key (idempotencyKey) — ensures partition affinity */
    @Column(nullable = false)
    private String partitionKey;

    /** JSON-serialized event payload */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    /** Number of failed publish attempts */
    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp of successful Kafka publish */
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = OutboxStatus.PENDING;
        }
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
    }
}
