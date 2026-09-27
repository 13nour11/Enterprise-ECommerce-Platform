package com.example.product_server.service;

import com.example.product_server.event.ProductChangedEvent;
import com.example.product_server.models.Product;
import com.example.product_server.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCommandServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductCommandService productCommandService;

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void create_rejectsNonPositivePrice(String price) {
        Product product = new Product(null, "Bad", "Invalid", new BigDecimal(price), "Electronics");

        assertThrows(InvalidProductException.class, () -> productCommandService.create(product));

        verifyNoInteractions(productRepository, eventPublisher);
    }

    @Test
    void create_savesProductAndPublishesCreatedEvent() {
        when(productRepository.save(any(Product.class)))
                .thenReturn(new Product(42L, "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics"));
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        ArgumentCaptor<ProductChangedEvent> eventCaptor = ArgumentCaptor.forClass(ProductChangedEvent.class);

        productCommandService.create(new Product(null, "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics"));

        verify(productRepository).save(productCaptor.capture());
        assertNull(productCaptor.getValue().getId());
        assertEquals("Laptop", productCaptor.getValue().getName());
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(new ProductChangedEvent(42L, "CREATED"), eventCaptor.getValue());
    }
}
