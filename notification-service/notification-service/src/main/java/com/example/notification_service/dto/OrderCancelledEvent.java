package com.example.notification_service.dto;

public record OrderCancelledEvent(
        String orderId,
        String reason
) {
}