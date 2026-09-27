package com.example.order_service.saga;

public record InventoryResultEvent(String orderId, boolean success) {
}
