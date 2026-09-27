package com.example.product_server.event;

public record ProductChangedEvent(Long productId, String changeType) {
}
