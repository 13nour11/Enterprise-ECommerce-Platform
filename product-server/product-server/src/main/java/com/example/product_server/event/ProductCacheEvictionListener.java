package com.example.product_server.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;

@Component
public class ProductCacheEvictionListener {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheEvictionListener.class);

    private final CacheManager cacheManager;

    public ProductCacheEvictionListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @TransactionalEventListener
    public void onProductChanged(ProductChangedEvent event) {
        Objects.requireNonNull(cacheManager.getCache("products")).evict(event.productId());
        Objects.requireNonNull(cacheManager.getCache("products")).evict("all");
        log.info("[CQRS] Cache evicted for product {} due to {}", event.productId(), event.changeType());
    }
}
