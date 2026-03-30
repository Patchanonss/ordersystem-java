package com.ordersystem.inventoryservice.service;

import com.ordersystem.inventoryservice.dto.OrderEvent;
import com.ordersystem.inventoryservice.model.ProcessedEvent;
import com.ordersystem.inventoryservice.model.Product;
import com.ordersystem.inventoryservice.repository.ProcessedEventRepository;
import com.ordersystem.inventoryservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import com.ordersystem.inventoryservice.dto.InventoryResultEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, InventoryResultEvent> kafkaTemplate;

    private static final String TOPIC = "inventory-results";

    @Transactional
    public void processOrderEvent(OrderEvent event) {
        // Idempotency check — skip if already processed
        if (processedEventRepository.existsByIdempotencyKey(event.getIdempotencyKey())) {
            log.warn("Duplicate event detected. idempotencyKey={} already processed. Skipping.", event.getIdempotencyKey());
            return;
        }

        // Find product by name — throws RuntimeException → triggers DLQ after 3 retries
        Product product = productRepository.findByName(event.getProductName())
                .orElseThrow(() -> new RuntimeException(
                        "Product not found: " + event.getProductName()));

        // Validate stock availability
        // NOTE: Insufficient stock is NOT retryable (stock won't magically increase),
        // so we log and return instead of throwing (which would trigger useless DLQ retries).
        if (product.getStock() < event.getQuantity()) {
            log.error("Insufficient stock for '{}'. Available: {}, Requested: {}. orderId={}, idempotencyKey={}",
                    product.getName(), product.getStock(), event.getQuantity(),
                    event.getOrderId(), event.getIdempotencyKey());
            // Mark as processed to prevent re-processing on restart
            saveProcessedEvent(event.getIdempotencyKey());
            
            // Send rejected result
            publishResult(event.getOrderId(), event.getIdempotencyKey(), "REJECTED");
            return;
        }

        // Deduct stock
        int previousStock = product.getStock();
        product.setStock(previousStock - event.getQuantity());
        productRepository.save(product);
        log.info("Stock deducted for '{}': {} -> {} (qty: {})",
                product.getName(), previousStock, product.getStock(), event.getQuantity());

        // Mark event as processed (idempotency record)
        saveProcessedEvent(event.getIdempotencyKey());
        
        // Send approved result
        publishResult(event.getOrderId(), event.getIdempotencyKey(), "APPROVED");
    }

    private void saveProcessedEvent(String idempotencyKey) {
        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .idempotencyKey(idempotencyKey)
                .build();
        processedEventRepository.save(processedEvent);
        log.info("Event marked as processed. idempotencyKey={}", idempotencyKey);
    }

    private void publishResult(Long orderId, String idempotencyKey, String status) {
        InventoryResultEvent resultEvent = InventoryResultEvent.builder()
                .orderId(orderId)
                .idempotencyKey(idempotencyKey)
                .status(status)
                .build();
        
        try {
            kafkaTemplate.send(TOPIC, idempotencyKey, resultEvent).get();
            log.info("Published InventoryResultEvent: orderId={}, status={}", orderId, status);
        } catch (Exception e) {
            log.error("Failed to publish InventoryResultEvent for orderId={}: {}", orderId, e.getMessage());
            throw new RuntimeException("Failed to publish inventory result event", e);
        }
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}

