package com.example.product_server.service;

import com.example.product_server.models.Product;
import com.example.product_server.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    // Save product to database
    public Product save(Product product) {
        return productRepository.save(product);
    }

    // Cache individual product lookups
    @Cacheable(value = "products", key = "#id")
    public Product findById(Long id) {
        log.info("[CACHE MISS] Loading product {} from database", id);
        log.info("Searching for product with id: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
    }

    // Cache all products list
    @Cacheable(value = "products", key = "'all'")
    public List<Product> findAll() {
        log.info("[CACHE MISS] Loading all products from database");
        return productRepository.findAll(Sort.by(Sort.Direction.ASC,"id"));
    }

    // Evict specific product cache AND the all-products list cache
    @Caching(evict = {
            @CacheEvict(value = "products", key = "#product.id"),
            @CacheEvict(value = "products", key = "'all'")
    })
    public Product update(Product product) {
        log.info("[CACHE EVICT] Invalidating cache for product {}", product.getId());
//        evictAllProductsCache();
        return productRepository.save(product);
    }

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(value = "products", key = "'all'")
    })
    public void deleteById(Long id) {
        log.info("[CACHE EVICT] Invalidating cache for product {}", id);
        productRepository.deleteById(id);
    }

    @CacheEvict(value = "products", key = "'all'")
    public void evictAllProductsCache() {
        log.info("[CACHE EVICT] Invalidating all-products cache");
    }
}