package com.example.product_server.service;

import com.example.product_server.models.Product;
import com.example.product_server.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @ParameterizedTest
    @CsvSource({
            "SILVER, 5",
            "GOLD, 10",
            "PLATINUM, 15",
            "BRONZE, 0"
    })
    void calcDiscount(String tier, int expected) {
        assertEquals(expected, productService.calcDiscount(tier));
    }

    @Test
    void save_passesProductToRepository() {
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        productService.save(new Product(null, "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics"));

        verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();
        assertNull(saved.getId());
        assertEquals("Laptop", saved.getName());
        assertEquals(new BigDecimal("999.99"), saved.getPrice());
        assertEquals("Electronics", saved.getCategory());
    }
}
