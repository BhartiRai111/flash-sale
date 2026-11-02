package com.bharti.flashsale.inventory.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.bharti.flashsale.common.Topics;
import com.bharti.flashsale.common.event.InventoryFailedEvent;
import com.bharti.flashsale.common.event.InventoryReservedEvent;
import com.bharti.flashsale.common.event.OrderCreatedEvent;
import com.bharti.flashsale.common.event.PaymentFailedEvent;
import com.bharti.flashsale.inventory.domain.Product;
import com.bharti.flashsale.inventory.domain.ProductRepository;
import com.bharti.flashsale.inventory.domain.Reservation;
import com.bharti.flashsale.inventory.domain.ReservationRepository;
import com.bharti.flashsale.inventory.domain.ReservationStatus;
import com.bharti.flashsale.inventory.messaging.EventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Core of the flash sale: reserving stock without overselling.
 *
 * Several consumer threads (and several service instances) process orders for the same product
 * at the same time. A Redis distributed lock per product makes the read-check-write sequence
 * atomic across ALL instances. The lock is released only AFTER the DB transaction commits,
 * so the next thread always reads the committed stock.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockReservationService {

    private static final long LOCK_WAIT_SECONDS = 5;

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;
    private final LockRegistry lockRegistry;
    private final TransactionTemplate transactionTemplate;
    private final CacheManager cacheManager;
    private final EventPublisher eventPublisher;

    public void reserve(OrderCreatedEvent event) {
        // Idempotency: this order was already handled -> just re-send the same outcome
        Optional<Reservation> existing = reservationRepository.findById(event.orderId());
        if (existing.isPresent()) {
            log.info("Order {} already processed ({}), replaying outcome", event.orderId(), existing.get().getStatus());
            publishOutcome(existing.get(), event);
            return;
        }

        Reservation reservation = withProductLock(event.productId(), () -> transactionTemplate.execute(status -> {
            // Double-check inside the lock: another thread may have handled the same order meanwhile
            Optional<Reservation> again = reservationRepository.findById(event.orderId());
            if (again.isPresent()) {
                return again.get();
            }
            Optional<Product> product = productRepository.findById(event.productId());
            if (product.isEmpty()) {
                return reservationRepository.save(Reservation.rejected(event.orderId(), event.productId(),
                        event.quantity(), "Product not found"));
            }
            if (!product.get().hasStock(event.quantity())) {
                return reservationRepository.save(Reservation.rejected(event.orderId(), event.productId(),
                        event.quantity(), "Sold out"));
            }
            product.get().decreaseStock(event.quantity());
            return reservationRepository.save(Reservation.reserved(event.orderId(), event.productId(),
                    event.quantity()));
        }));

        evictProductCache(event.productId());
        log.info("Order {} -> {} (product {}, qty {})", event.orderId(), reservation.getStatus(),
                event.productId(), event.quantity());
        publishOutcome(reservation, event);
    }

    /** Saga compensation: payment failed, so give the stock back. */
    public void release(PaymentFailedEvent event) {
        Optional<Reservation> existing = reservationRepository.findById(event.orderId());
        if (existing.isEmpty() || existing.get().getStatus() != ReservationStatus.RESERVED) {
            log.info("Nothing to release for order {}", event.orderId());
            return;
        }
        Long productId = existing.get().getProductId();

        withProductLock(productId, () -> transactionTemplate.execute(status -> {
            Reservation reservation = reservationRepository.findById(event.orderId()).orElseThrow();
            if (reservation.getStatus() != ReservationStatus.RESERVED) {
                return null; // released by a duplicate event in the meantime
            }
            productRepository.findById(productId).ifPresent(p -> p.increaseStock(reservation.getQuantity()));
            reservation.release("Payment failed: " + event.reason());
            return null;
        }));

        evictProductCache(productId);
        log.info("Released stock for order {} (product {})", event.orderId(), productId);
    }

    private void publishOutcome(Reservation reservation, OrderCreatedEvent event) {
        String key = event.orderId().toString();
        switch (reservation.getStatus()) {
            case RESERVED -> eventPublisher.publish(Topics.INVENTORY_RESERVED, key, new InventoryReservedEvent(
                    event.orderId(), event.userId(), event.productId(), event.quantity(), event.amount(), Instant.now()));
            case REJECTED -> eventPublisher.publish(Topics.INVENTORY_FAILED, key, new InventoryFailedEvent(
                    event.orderId(), event.productId(), reservation.getReason(), Instant.now()));
            case RELEASED -> {
                // Saga already finished for this order; nothing to send
            }
        }
    }

    private <T> T withProductLock(Long productId, Supplier<T> action) {
        Lock lock = lockRegistry.obtain("product:" + productId);
        boolean acquired;
        try {
            acquired = lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for lock on product " + productId, e);
        }
        if (!acquired) {
            // Kafka error handler will retry this message
            throw new IllegalStateException("Could not lock product " + productId + " within " + LOCK_WAIT_SECONDS + "s");
        }
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    private void evictProductCache(Long productId) {
        Cache cache = cacheManager.getCache(ProductService.PRODUCT_CACHE);
        if (cache != null) {
            cache.evict(productId);
        }
    }

    /** Used by tests and the admin endpoint. */
    public long countReservations(Long productId, ReservationStatus status) {
        return reservationRepository.countByProductIdAndStatus(productId, status);
    }

    public Optional<Reservation> findReservation(UUID orderId) {
        return reservationRepository.findById(orderId);
    }
}
