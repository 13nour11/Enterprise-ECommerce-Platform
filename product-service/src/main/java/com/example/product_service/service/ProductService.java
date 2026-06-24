package com.example.product_service.service;

import com.example.product_service.models.Product;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProductService {
    // TODO 1: Inject a Map<Long, Product> as an in-memory store (no DB yet)
    private final Map<Long, Product> productStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    // TODO 2: Implement findAll() returning List<Product>
    public List<Product> findAll() {
        return new ArrayList<>(productStore.values());
    }

    // TODO 3: Implement findById(Long id) returning Optional<Product>
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(productStore.get(id));
    }

    // TODO 4: Implement save(Product product) returning the saved Product
    public Product save(Product product) {
        // إذا كان ID null، نولده تلقائياً
        Long id = product.id();
        if (id == null) {
            id = idGenerator.getAndIncrement();
            product = new Product(
                    id,
                    product.name(),
                    product.description(),
                    product.price(),
                    product.category()
            );
        }
        productStore.put(id, product);
        return product;
    }

    // TODO 5: Implement deleteById(Long id)
    public boolean deleteById(Long id) {
        return productStore.remove(id) != null;
    }
}
