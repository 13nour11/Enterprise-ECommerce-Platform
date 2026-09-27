package com.example.product_server.service;

import com.example.product_server.models.ProductSummaryProjection;
import com.example.product_server.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductQueryService {

    private final ProductRepository productRepository;

    public ProductQueryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Optional<ProductSummaryProjection> findById(Long id) {
        return productRepository.findSummaryById(id);
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryProjection> findAll() {
        return productRepository.findAllSummaries();
    }
}
