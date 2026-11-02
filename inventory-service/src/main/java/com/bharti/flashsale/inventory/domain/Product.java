package com.bharti.flashsale.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "available_stock", nullable = false)
    private Integer availableStock;

    /** Second line of defence after the Redis lock: a stale write fails instead of overselling. */
    @Version
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Product create(String name, BigDecimal price, int stock) {
        Product product = new Product();
        product.name = name;
        product.price = price;
        product.availableStock = stock;
        return product;
    }

    public boolean hasStock(int quantity) {
        return availableStock >= quantity;
    }

    public void decreaseStock(int quantity) {
        if (!hasStock(quantity)) {
            throw new IllegalStateException("Not enough stock for product " + id);
        }
        availableStock -= quantity;
    }

    public void increaseStock(int quantity) {
        availableStock += quantity;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
