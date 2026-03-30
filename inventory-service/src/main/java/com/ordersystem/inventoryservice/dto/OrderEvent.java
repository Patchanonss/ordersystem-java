package com.ordersystem.inventoryservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent {
    private Long orderId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private String idempotencyKey;
    private String status;
}
