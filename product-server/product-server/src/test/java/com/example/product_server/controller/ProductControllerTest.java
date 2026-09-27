package com.example.product_server.controller;

import com.example.product_server.models.Product;
import com.example.product_server.service.ProductNotFoundException;
import com.example.product_server.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    @DisplayName("GET /api/v1/products/{id} returns 200 with product JSON")
    void getProduct_found_returns200() throws Exception {
        when(productService.findById(1L))
                .thenReturn(new Product(1L, "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics"));

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(999.99));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} returns 404 when not found")
    void getProduct_notFound_returns404() throws Exception {
        when(productService.findById(99L)).thenThrow(new ProductNotFoundException("Product not found: 99"));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/products returns 201 with created product")
    void createProduct_valid_returns201() throws Exception {
        when(productService.save(any(Product.class)))
                .thenReturn(new Product(1L, "Mouse", "Wireless mouse", new BigDecimal("29.99"), "Accessories"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mouse","description":"Wireless mouse","price":29.99,"category":"Accessories"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mouse"))
                .andExpect(jsonPath("$.price").value(29.99));
    }

    @Test
    @DisplayName("POST /api/v1/products with missing name returns 400")
    void createProduct_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":50}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }
}
