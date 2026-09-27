package com.example.order_service.saga;

import java.math.BigDecimal;

public record ProcessPaymentCommand(String orderId, BigDecimal amount) {
}
