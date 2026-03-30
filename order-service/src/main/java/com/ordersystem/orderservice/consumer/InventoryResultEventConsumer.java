package com.ordersystem.orderservice.consumer;

import com.ordersystem.orderservice.dto.InventoryResultEvent;
import com.ordersystem.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryResultEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "inventory-results",
            groupId = "order-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeInventoryResultEvent(InventoryResultEvent event) {
        log.info("Received InventoryResultEvent: orderId={}, status={}, idempotencyKey={}",
                event.getOrderId(), event.getStatus(), event.getIdempotencyKey());

        orderService.updateOrderStatus(event.getIdempotencyKey(), event.getStatus());

        log.info("Successfully processed InventoryResultEvent for orderId={}", event.getOrderId());
    }
}
