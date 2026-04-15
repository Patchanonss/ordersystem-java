package com.ordersystem.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.orderservice.dto.OrderRequest;
import com.ordersystem.orderservice.model.Order;
import com.ordersystem.orderservice.model.OutboxEvent;
import com.ordersystem.orderservice.model.OutboxStatus;
import com.ordersystem.orderservice.repository.OrderRepository;
import com.ordersystem.orderservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    private OrderRequest buildRequest() {
        return OrderRequest.builder()
                .productName("iPhone")
                .quantity(2)
                .price(new BigDecimal("29000.0"))
                .build();
    }

    @Test
    void shouldSaveBothOrderAndOutboxEventOnHappyPath() throws Exception {
        Order savedOrder = Order.builder()
                .id(1L)
                .productName("iPhone")
                .quantity(2)
                .price(new BigDecimal("29000.0"))
                .status("PENDING")
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":1}");

        Order result = orderService.createOrder(buildRequest());

        assertEquals(1L, result.getId());
        verify(orderRepository).save(any(Order.class));
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void shouldThrowAndNeverSaveOutboxWhenSerializationFails() throws Exception {
        Order savedOrder = Order.builder()
                .id(1L)
                .productName("iPhone")
                .quantity(2)
                .price(new BigDecimal("29000.0"))
                .status("PENDING")
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(JsonProcessingException.class);

        assertThrows(RuntimeException.class,
                () -> orderService.createOrder(buildRequest()));

        verify(outboxEventRepository, never()).save(any());
    }
    @Test
void shouldThrowWhenOrderRepositoryFails() {
    // Arrange
    when(orderRepository.save(any(Order.class)))
            .thenThrow(new RuntimeException("DB connection lost"));

    // Act & Assert
    assertThrows(RuntimeException.class,
            () -> orderService.createOrder(buildRequest()));

    // OutboxEvent ต้องไม่ถูกแตะเลย เพราะ order save ยังไม่สำเร็จ
    verifyNoInteractions(outboxEventRepository);
}

@Test
void shouldThrowWhenOutboxRepositoryFails() throws Exception {
    // Arrange
    Order savedOrder = Order.builder()
            .id(1L)
            .productName("iPhone")
            .quantity(2)
            .price(new BigDecimal("29000.0"))
            .status("PENDING")
            .build();

    when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":1}");
    when(outboxEventRepository.save(any(OutboxEvent.class)))
            .thenThrow(new RuntimeException("DB connection lost"));

    // Act & Assert
    // ถ้า outbox save fail ต้อง throw ออกไป
    // @Transactional จะ rollback order ด้วยในระบบจริง
    assertThrows(RuntimeException.class,
            () -> orderService.createOrder(buildRequest()));
}

@Test
void shouldSaveOutboxEventWithCorrectFields() throws Exception {
    // Arrange
    Order savedOrder = Order.builder()
            .id(1L)
            .productName("iPhone")
            .quantity(2)
            .price(new BigDecimal("29000.0"))
            .status("PENDING")
            .build();

    when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":1}");

    // Act
    orderService.createOrder(buildRequest());

    // Assert — ดักจับ OutboxEvent ที่ถูก save แล้วเช็ค field
    verify(outboxEventRepository).save(argThat(outbox ->
            "Order".equals(outbox.getAggregateType()) &&
            "1".equals(outbox.getAggregateId()) &&
            "ORDER_CREATED".equals(outbox.getEventType()) &&
            "order-events".equals(outbox.getTopic()) &&
            OutboxStatus.PENDING.equals(outbox.getStatus())
    ));
}
}