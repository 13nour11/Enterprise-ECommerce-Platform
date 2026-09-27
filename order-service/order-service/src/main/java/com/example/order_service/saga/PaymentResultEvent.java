package com.example.order_service.saga;

public record PaymentResultEvent(String orderId, boolean success) {
}
