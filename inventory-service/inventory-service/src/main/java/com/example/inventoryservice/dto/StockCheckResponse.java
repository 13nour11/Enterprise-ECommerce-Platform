package com.example.inventoryservice.dto;

public record StockCheckResponse(
        String productId, int requestedQuantity, boolean available, int remainingStock
) {}