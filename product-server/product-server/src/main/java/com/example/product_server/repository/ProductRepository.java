package com.example.product_server.repository;

import com.example.product_server.models.Product;
import com.example.product_server.models.ProductSummaryProjection;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAll(Sort sort);

    List<Product> findByPriceLessThan(BigDecimal price);

    @Query("SELECT p.id AS id, p.name AS name, p.price AS price, p.category AS categoryName " +
            "FROM Product p WHERE p.id = :id")
    Optional<ProductSummaryProjection> findSummaryById(@Param("id") Long id);

    @Query("SELECT p.id AS id, p.name AS name, p.price AS price, p.category AS categoryName " +
            "FROM Product p ORDER BY p.id")
    List<ProductSummaryProjection> findAllSummaries();
}
