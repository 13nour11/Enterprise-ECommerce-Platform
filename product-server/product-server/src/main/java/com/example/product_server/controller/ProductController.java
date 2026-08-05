package com.example.product_server.controller;

import com.example.product_server.models.Product;
import com.example.product_server.service.ProductNotFoundException;
import com.example.product_server.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    // TODO: Inject ProductService
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // TODO: GET /api/v1/products         → return all products
    @GetMapping
    public List<Product> findAll(){
        return  productService.findAll();
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
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestBody Product product) {

        System.out.println("Received product: " + product.getName()); // للتأكد
        System.out.println("Price: " + product.getPrice());
        System.out.println("Category: " + product.getCategory());

        return productService.save(product);
    }

    // TODO: DELETE /api/v1/products/{id} → delete a product

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
//        boolean deleted = productService.deleteById(id);
//        return deleted
//                ? ResponseEntity.noContent().build()
//                : ResponseEntity.notFound().build();
        try {
            productService.findById(id);
            productService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (ProductNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product product){
        try {
            // First, check if the product exists
            Product existingProduct = productService.findById(id);

            // Update the fields
            existingProduct.setName(product.getName());
            existingProduct.setDescription(product.getDescription());
            existingProduct.setPrice(product.getPrice());
            existingProduct.setCategory(product.getCategory());

            // Save the updated product
            Product updatedProduct = productService.update(existingProduct);
            return ResponseEntity.ok(updatedProduct);

        } catch (ProductNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }



}
