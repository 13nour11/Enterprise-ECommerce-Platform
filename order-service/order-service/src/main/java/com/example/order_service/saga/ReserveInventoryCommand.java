package com.example.order_service.saga;

public record ReserveInventoryCommand(String orderId, String productId, int quantity) {
}
