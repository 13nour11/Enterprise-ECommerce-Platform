//package com.example.product_server.models;
//import java.math.BigDecimal;
//
//public record Product(
//        Long id,
//        String name,
//        String description,
//        BigDecimal price,
//        String category
//) {}

package com.example.product_server.models;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product  {


        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private String category;

        public Product() {}

        public Product(Long id, String name, String description, BigDecimal price, String category) {
                this.id = id;
                this.name = name;
                this.description = description;
                this.price = price;
                this.category = category;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
}