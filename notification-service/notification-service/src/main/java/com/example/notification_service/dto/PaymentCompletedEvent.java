package com.example.notification_service.dto;

public record PaymentCompletedEvent(
        String orderId,
        String transactionId
) {
}