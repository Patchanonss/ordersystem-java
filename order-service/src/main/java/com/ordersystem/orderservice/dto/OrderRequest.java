package com.ordersystem.orderservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {
    private String productName;
    private Integer quantity;
    private BigDecimal price;
}
