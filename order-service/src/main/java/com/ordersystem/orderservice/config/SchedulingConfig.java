package com.ordersystem.orderservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's @Scheduled support, required for the
 * OutboxEventPublisher and OutboxCleanupScheduler to run.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
