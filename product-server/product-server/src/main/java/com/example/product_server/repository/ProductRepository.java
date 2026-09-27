package com.example.product_server.repository;

import com.example.product_server.models.Product;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAll(Sort sort);

    List<Product> findByPriceLessThan(BigDecimal price);
}
