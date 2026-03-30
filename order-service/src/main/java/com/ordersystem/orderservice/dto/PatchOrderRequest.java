package com.ordersystem.orderservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatchOrderRequest {
    private String productName;   // null = "don't change"
    private Integer quantity;      // null = "don't change"
    private BigDecimal price;      // null = "don't change"
    private String status;         // null = "don't change"
    // No @NotNull annotations — everything is optional
}
