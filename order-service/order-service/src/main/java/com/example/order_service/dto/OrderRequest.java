package com.example.order_service.dto;

import java.math.BigDecimal;

public record OrderRequest(String productId, int quantity, BigDecimal amount) {
    public OrderRequest(BigDecimal amount){
        this(null,0,null);
    }
}
