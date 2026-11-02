package com.bharti.flashsale.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import com.bharti.flashsale.common.event.OrderCreatedEvent;
import com.bharti.flashsale.common.event.PaymentFailedEvent;
import com.bharti.flashsale.inventory.domain.Product;
import com.bharti.flashsale.inventory.domain.ProductRepository;
import com.bharti.flashsale.inventory.domain.ReservationStatus;
import com.bharti.flashsale.inventory.service.StockReservationService;

/**
 * The flash-sale test: 50 buyers race for 10 units. Exactly 10 must win and stock must end at 0,
 * never negative. Runs against real PostgreSQL, Redis and Kafka (Docker required).
 */
@SpringBootTest
@Testcontainers
class StockReservationConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0");

    @Autowired
    StockReservationService reservationService;

    @Autowired
    ProductRepository productRepository;

    @Test
    void fiftyConcurrentBuyersForTenUnitsNeverOversell() throws Exception {
        Product product = productRepository.save(Product.create("Limited Edition Item", new BigDecimal("999.00"), 10));
        int buyers = 50;

        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            results.add(pool.submit(() -> {
                start.await();
                reservationService.reserve(new OrderCreatedEvent(UUID.randomUUID(), "user", "user@example.com",
                        product.getId(), 1, new BigDecimal("999.00"), Instant.now()));
                return null;
            }));
        }
        start.countDown(); // release all buyers at the same moment
        for (Future<?> result : results) {
            result.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        Product after = productRepository.findById(product.getId()).orElseThrow();
        assertThat(after.getAvailableStock()).isZero();
        assertThat(reservationService.countReservations(product.getId(), ReservationStatus.RESERVED)).isEqualTo(10);
        assertThat(reservationService.countReservations(product.getId(), ReservationStatus.REJECTED)).isEqualTo(40);
    }

    @Test
    void duplicateEventReservesStockOnlyOnce() {
        Product product = productRepository.save(Product.create("Duplicate Test Item", new BigDecimal("10.00"), 5));
        OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID(), "user", "user@example.com",
                product.getId(), 2, new BigDecimal("20.00"), Instant.now());

        reservationService.reserve(event);
        reservationService.reserve(event); // Kafka redelivered the same message

        assertThat(productRepository.findById(product.getId()).orElseThrow().getAvailableStock()).isEqualTo(3);
    }

    @Test
    void paymentFailureReleasesStock() {
        Product product = productRepository.save(Product.create("Release Test Item", new BigDecimal("10.00"), 5));
        UUID orderId = UUID.randomUUID();
        reservationService.reserve(new OrderCreatedEvent(orderId, "user", "user@example.com",
                product.getId(), 2, new BigDecimal("20.00"), Instant.now()));

        reservationService.release(new PaymentFailedEvent(orderId, "Card declined", Instant.now()));
        reservationService.release(new PaymentFailedEvent(orderId, "Card declined", Instant.now())); // duplicate

        assertThat(productRepository.findById(product.getId()).orElseThrow().getAvailableStock()).isEqualTo(5);
        assertThat(reservationService.findReservation(orderId).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.RELEASED);
    }
}
