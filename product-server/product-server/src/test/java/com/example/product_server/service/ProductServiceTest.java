package com.example.product_server.service;

import com.example.product_server.repository.ProductRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
