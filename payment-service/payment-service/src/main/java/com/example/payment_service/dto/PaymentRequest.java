package com.example.payment_service.dto;

import java.math.BigDecimal;

public record PaymentRequest(BigDecimal amount) {
}
