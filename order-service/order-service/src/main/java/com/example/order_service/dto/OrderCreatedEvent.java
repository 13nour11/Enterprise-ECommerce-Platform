package com.example.order_service.dto;

import java.math.BigDecimal;

public record OrderCreatedEvent(
        Long orderId,
        Long customerId,
        BigDecimal totalAmount
) {
}
