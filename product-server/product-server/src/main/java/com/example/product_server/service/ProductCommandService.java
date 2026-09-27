package com.example.product_server.service;

import com.example.product_server.event.ProductChangedEvent;
import com.example.product_server.models.Product;
import com.example.product_server.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProductCommandService {

    private static final Logger log = LoggerFactory.getLogger(ProductCommandService.class);

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProductCommandService(ProductRepository productRepository, ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Product create(Product product) {
        validatePrice(product.getPrice());
        Product saved = productRepository.save(product);
        publish(saved.getId(), "CREATED");
        return saved;
    }

    @Transactional
    public Product update(Long id, Product changes) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
        validatePrice(changes.getPrice());

        existing.setName(changes.getName());
        existing.setDescription(changes.getDescription());
        existing.setPrice(changes.getPrice());
        existing.setCategory(changes.getCategory());

        Product saved = productRepository.save(existing);
        publish(saved.getId(), "UPDATED");
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
        publish(id, "DELETED");
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidProductException("Price must be positive");
        }
    }

    private void publish(Long productId, String changeType) {
        eventPublisher.publishEvent(new ProductChangedEvent(productId, changeType));
        log.info("[CQRS] ProductChangedEvent published: productId={}, changeType={}", productId, changeType);
    }
}
