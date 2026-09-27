package com.example.product_server.service;

import com.example.product_server.models.Product;
import com.example.product_server.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

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

    public int calcDiscount(String tier) {
        return switch (tier) {
            case "SILVER" -> 5;
            case "GOLD" -> 10;
            case "PLATINUM" -> 15;
            default -> 0;
        };
    }
}