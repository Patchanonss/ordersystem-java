package com.ordersystem.inventoryservice.consumer;

import com.ordersystem.inventoryservice.dto.OrderEvent;
import com.ordersystem.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(
            topics = "order-events",
            groupId = "inventory-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderEvent(OrderEvent event) {
        log.info("Received OrderEvent: orderId={}, product={}, qty={}, idempotencyKey={}",
                event.getOrderId(), event.getProductName(), event.getQuantity(), event.getIdempotencyKey());

        inventoryService.processOrderEvent(event);

        log.info("Successfully processed OrderEvent for orderId={}", event.getOrderId());
    }
}
