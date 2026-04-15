package com.ordersystem.inventoryservice.service;

import com.ordersystem.inventoryservice.model.ProcessedEvent;
import com.ordersystem.inventoryservice.model.Product;
import com.ordersystem.inventoryservice.dto.OrderEvent;
import com.ordersystem.inventoryservice.dto.InventoryResultEvent;
import com.ordersystem.inventoryservice.repository.ProcessedEventRepository;
import com.ordersystem.inventoryservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private KafkaTemplate<String, InventoryResultEvent> kafkaTemplate;

    @InjectMocks
    private InventoryService inventoryService;

    // ---- Helper ----
    private OrderEvent buildEvent(String key, String productName, int qty) {
        return OrderEvent.builder()
                .orderId(1L)
                .idempotencyKey(key)
                .productName(productName)
                .quantity(qty)
                .build();
    }

    private void mockKafkaSend() {
        CompletableFuture<SendResult<String, InventoryResultEvent>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
    }

    // ---- Tests ----

    @Test
    void shouldSkipAllProcessingWhenDuplicateEvent() {
        when(processedEventRepository.existsByIdempotencyKey("KEY-001")).thenReturn(true);

        inventoryService.processOrderEvent(buildEvent("KEY-001", "iPhone", 1));

        // ถ้า duplicate จริง ต้องไม่แตะ product หรือ kafka เลย
        verifyNoInteractions(productRepository);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldThrowWhenProductNotFound() {
        when(processedEventRepository.existsByIdempotencyKey("KEY-002")).thenReturn(false);
        when(productRepository.findByName("Ghost")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> inventoryService.processOrderEvent(buildEvent("KEY-002", "Ghost", 1)));
    }

    @Test
    void shouldRejectAndMarkProcessedWhenInsufficientStock() {
        Product product = Product.builder().name("iPhone").stock(0).build();

        when(processedEventRepository.existsByIdempotencyKey("KEY-003")).thenReturn(false);
        when(productRepository.findByName("iPhone")).thenReturn(Optional.of(product));
        mockKafkaSend();

        inventoryService.processOrderEvent(buildEvent("KEY-003", "iPhone", 1));

        // stock ต้องไม่เปลี่ยน
        assertEquals(0, product.getStock());
        // ต้อง save processed event เพื่อกัน reprocess
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        // ต้อง publish REJECTED
        verify(kafkaTemplate).send(
                eq("inventory-results"),
                eq("KEY-003"),
                argThat(e -> "REJECTED".equals(e.getStatus()))
        );
    }

    @Test
    void shouldDeductStockAndPublishApprovedOnHappyPath() {
        Product product = Product.builder().name("iPhone").stock(10).build();

        when(processedEventRepository.existsByIdempotencyKey("KEY-004")).thenReturn(false);
        when(productRepository.findByName("iPhone")).thenReturn(Optional.of(product));
        mockKafkaSend();

        inventoryService.processOrderEvent(buildEvent("KEY-004", "iPhone", 3));

        // stock ต้องหัก 3
        assertEquals(7, product.getStock());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        verify(kafkaTemplate).send(
                eq("inventory-results"),
                eq("KEY-004"),
                argThat(e -> "APPROVED".equals(e.getStatus()))
        );
    }
}