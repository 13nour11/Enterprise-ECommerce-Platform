package com.example.order_service.dto;

import java.math.BigDecimal;

public record PaymentRequest(BigDecimal amount) {
}
