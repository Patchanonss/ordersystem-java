package com.ordersystem.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.orderservice.dto.OrderEvent;
import com.ordersystem.orderservice.dto.OrderRequest;
import com.ordersystem.orderservice.dto.PatchOrderRequest;
import com.ordersystem.orderservice.model.Order;
import com.ordersystem.orderservice.model.OutboxEvent;
import com.ordersystem.orderservice.model.OutboxStatus;
import com.ordersystem.orderservice.repository.OrderRepository;
import com.ordersystem.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "order-events";

    @Transactional
    public Order createOrder(OrderRequest request) {
        // Generate a unique idempotency key
        String idempotencyKey = UUID.randomUUID().toString();

        Order order = Order.builder()
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .status("PENDING")
                .idempotencyKey(idempotencyKey)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order saved with ID: {} and idempotencyKey: {}", savedOrder.getId(), idempotencyKey);

        // Build Kafka event
        OrderEvent event = OrderEvent.builder()
                .orderId(savedOrder.getId())
                .productName(savedOrder.getProductName())
                .quantity(savedOrder.getQuantity())
                .price(savedOrder.getPrice())
                .idempotencyKey(idempotencyKey)
                .status(savedOrder.getStatus())
                .build();

        // Outbox Pattern: instead of sending to Kafka directly, we serialize the event
        // and save it to the outbox_events table in the SAME transaction as the order.
        // This guarantees atomicity — either both order + outbox event are committed, or neither.
        // The OutboxEventPublisher (scheduler) will pick up PENDING events and publish to Kafka.
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(String.valueOf(savedOrder.getId()))
                    .eventType("ORDER_CREATED")
                    .topic(TOPIC)
                    .partitionKey(idempotencyKey)
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Outbox event saved for orderId={}, idempotencyKey={}", savedOrder.getId(), idempotencyKey);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderEvent for orderId={}: {}", savedOrder.getId(), e.getMessage());
            throw new RuntimeException("Failed to serialize order event for outbox", e);
        }

        return savedOrder;
    }
    
    @Transactional
    public Order patchOrder(Long id, PatchOrderRequest patch){
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    if (patch.getProductName() != null) order.setProductName(patch.getProductName());
    if (patch.getQuantity()    != null) order.setQuantity(patch.getQuantity());
    if (patch.getPrice()       != null) order.setPrice(patch.getPrice());
    if (patch.getStatus()      != null) order.setStatus(patch.getStatus());
    return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getTemp1Orders() {
        System.out.println("123123123123123");
        return orderRepository.findByQuantityGreaterThan(50);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }
    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
            orderRepository.delete(order);
        log.info("Order deleted with id: {}", id);
    }
    @Transactional
    public void updateOrderStatus(String idempotencyKey, String status) {
        orderRepository.findByIdempotencyKey(idempotencyKey).ifPresentOrElse(order -> {
            order.setStatus(status);
            orderRepository.save(order);
            log.info("Order status updated for idempotencyKey {}: set to {}", idempotencyKey, status);
        }, () -> {
            log.warn("Order not found for idempotencyKey: {}", idempotencyKey);
        });
    }
}
