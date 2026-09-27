package com.example.product_server.models;

import java.math.BigDecimal;

public interface ProductSummaryProjection {

    Long getId();

    String getName();

    BigDecimal getPrice();

    String getCategoryName();

    default String getDisplayLabel() {
        return getName() + " (" + getCategoryName() + ")";
    }
}
