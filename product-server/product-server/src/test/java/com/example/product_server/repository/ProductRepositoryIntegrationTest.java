package com.example.product_server.repository;

import com.example.product_server.models.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ProductRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void save_andFindById_roundTrip() {
        Product saved = productRepository.save(
                new Product(null, "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics"));

        var found = productRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Laptop", found.get().getName());
        assertEquals(0, new BigDecimal("999.99").compareTo(found.get().getPrice()));
    }

    @Test
    void findByPriceLessThan_returnsMatchingProducts() {
        productRepository.saveAll(List.of(
                new Product(null, "Mouse", "Wireless mouse", new BigDecimal("29.99"), "Accessories"),
                new Product(null, "Monitor", "4K monitor", new BigDecimal("399.99"), "Electronics")));

        List<Product> cheap = productRepository.findByPriceLessThan(new BigDecimal("50"));

        assertEquals(1, cheap.size());
        assertEquals("Mouse", cheap.get(0).getName());
    }
}
