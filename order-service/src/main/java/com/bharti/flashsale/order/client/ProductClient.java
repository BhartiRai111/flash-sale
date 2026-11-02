package com.bharti.flashsale.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.bharti.flashsale.order.exception.OrderExceptions.InventoryUnavailableException;
import com.bharti.flashsale.order.exception.OrderExceptions.ProductNotFoundException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

/**
 * Synchronous call to inventory-service to read the current price.
 * Retry handles short glitches; the circuit breaker stops hammering a service that is down.
 */
@Slf4j
@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(RestClient.Builder builder,
                         @Value("${inventory-service.url}") String inventoryUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(2000);
        this.restClient = builder
                .baseUrl(inventoryUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Retry(name = "inventory", fallbackMethod = "fallback")
    @CircuitBreaker(name = "inventory")
    public ProductDto getProduct(Long productId) {
        return restClient.get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .onStatus(status -> status.value() == 404, (request, response) -> {
                    throw new ProductNotFoundException(productId);
                })
                .body(ProductDto.class);
    }

    /** Called by Resilience4j after all retries fail (or immediately when the circuit is open). */
    public ProductDto fallback(Long productId, Throwable error) {
        if (error instanceof ProductNotFoundException notFound) {
            throw notFound; // a real "not found" is not an outage
        }
        log.warn("Inventory call failed for product {}: {}", productId, error.toString());
        throw new InventoryUnavailableException(error);
    }
}
