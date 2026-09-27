package com.example.product_server.controller;

import com.example.product_server.models.Product;
import com.example.product_server.models.ProductSummaryProjection;
import com.example.product_server.service.ProductCommandService;
import com.example.product_server.service.ProductNotFoundException;
import com.example.product_server.service.ProductQueryService;
import com.example.product_server.service.ProductService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    // TODO: Inject ProductService
    private final ProductService productService;
    private final ProductCommandService productCommandService;
    private final ProductQueryService productQueryService;

    public ProductController(ProductService productService,
                             ProductCommandService productCommandService,
                             ProductQueryService productQueryService) {
        this.productService = productService;
        this.productCommandService = productCommandService;
        this.productQueryService = productQueryService;
    }

    // TODO: GET /api/v1/products         → return all products
    @GetMapping
    public List<Product> findAll(){
        return  productService.findAll();
    }

    @GetMapping("/summary")
    public List<ProductSummaryProjection> findAllSummaries() {
        return productQueryService.findAll();
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<ProductSummaryProjection> findSummaryById(@PathVariable Long id) {
        return productQueryService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // TODO: GET /api/v1/products/{id}    → return product by id (404 if not found)

    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(@PathVariable Long id) {
//        return productService.findById(id)
//                .map(ResponseEntity::ok)
//                .orElseGet(() -> ResponseEntity.notFound().build());
        try {
            Product product = productService.findById(id);  // ✅ بترجع Product مش Optional
            return ResponseEntity.ok(product);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }


    // TODO: POST /api/v1/products        → create a new product
    @Timed(value = "product.create.duration", description = "Time to create a product")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody Product product) {

        System.out.println("Received product: " + product.getName()); // للتأكد
        System.out.println("Price: " + product.getPrice());
        System.out.println("Category: " + product.getCategory());

        return productCommandService.create(product);
    }

    // TODO: DELETE /api/v1/products/{id} → delete a product

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
//        boolean deleted = productService.deleteById(id);
//        return deleted
//                ? ResponseEntity.noContent().build()
//                : ResponseEntity.notFound().build();
        try {
            productCommandService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (ProductNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product product){
        try {
            return ResponseEntity.ok(productCommandService.update(id, product));

        } catch (ProductNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }



}
