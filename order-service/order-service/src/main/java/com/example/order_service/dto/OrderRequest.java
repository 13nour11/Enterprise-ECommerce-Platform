package com.example.order_service.dto;

import java.math.BigDecimal;

public record OrderRequest(BigDecimal amount) {
}
