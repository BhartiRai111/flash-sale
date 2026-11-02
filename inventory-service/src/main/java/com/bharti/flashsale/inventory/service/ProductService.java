package com.bharti.flashsale.inventory.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.bharti.flashsale.inventory.domain.Product;
import com.bharti.flashsale.inventory.domain.ProductRepository;
import com.bharti.flashsale.inventory.web.ProductDtos.CreateProductRequest;
import com.bharti.flashsale.inventory.web.ProductDtos.ProductResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    public static final String PRODUCT_CACHE = "products";

    private final ProductRepository productRepository;

    /**
     * Cached in Redis: during a flash sale thousands of users refresh the product page,
     * so most reads never reach PostgreSQL. The entry is evicted whenever stock changes.
     */
    @Cacheable(cacheNames = PRODUCT_CACHE, key = "#productId")
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        return productRepository.findById(productId)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product " + productId + " not found"));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts() {
        return productRepository.findAll().stream().map(ProductResponse::from).toList();
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Product product = productRepository.save(Product.create(request.name(), request.price(), request.stock()));
        return ProductResponse.from(product);
    }

    @CacheEvict(cacheNames = PRODUCT_CACHE, key = "#productId")
    @Transactional
    public ProductResponse restock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product " + productId + " not found"));
        product.increaseStock(quantity);
        return ProductResponse.from(product);
    }
}
