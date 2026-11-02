package com.bharti.flashsale.inventory.web;

import java.math.BigDecimal;

import com.bharti.flashsale.inventory.domain.Product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record ProductResponse(Long id, String name, BigDecimal price, Integer availableStock) {
        public static ProductResponse from(Product product) {
            return new ProductResponse(product.getId(), product.getName(), product.getPrice(),
                    product.getAvailableStock());
        }
    }

    public record CreateProductRequest(
            @NotBlank String name,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            @NotNull @Min(0) Integer stock) {
    }

    public record RestockRequest(@NotNull @Min(1) Integer quantity) {
    }
}
