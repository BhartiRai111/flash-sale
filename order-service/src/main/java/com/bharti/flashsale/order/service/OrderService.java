package com.bharti.flashsale.order.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.bharti.flashsale.common.Topics;
import com.bharti.flashsale.common.event.OrderCreatedEvent;
import com.bharti.flashsale.common.event.OrderStatusChangedEvent;
import com.bharti.flashsale.order.client.ProductClient;
import com.bharti.flashsale.order.client.ProductDto;
import com.bharti.flashsale.order.domain.Order;
import com.bharti.flashsale.order.domain.OrderRepository;
import com.bharti.flashsale.order.exception.OrderExceptions.OrderNotFoundException;
import com.bharti.flashsale.order.exception.OrderExceptions.OutOfStockException;
import com.bharti.flashsale.order.outbox.OutboxWriter;
import com.bharti.flashsale.order.web.CreateOrderRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxWriter outboxWriter;
    private final ProductClient productClient;
    private final TransactionTemplate transactionTemplate;

    /** Result of a create call; created=false means the idempotency key was already used. */
    public record CreateResult(Order order, boolean created) {
    }

    /**
     * Idempotent order creation:
     * 1. Same Idempotency-Key again -> return the existing order (no duplicate).
     * 2. The remote price lookup runs OUTSIDE the DB transaction (never hold a connection while calling another service).
     * 3. Order row + outbox row commit in one transaction.
     */
    public CreateResult createOrder(String userId, String idempotencyKey, CreateOrderRequest request) {
        Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new CreateResult(existing.get(), false);
        }

        ProductDto product = productClient.getProduct(request.productId());
        if (product.availableStock() < request.quantity()) {
            throw new OutOfStockException(request.productId());
        }

        try {
            Order order = transactionTemplate.execute(status -> {
                Order saved = orderRepository.saveAndFlush(Order.create(userId, request.customerEmail(),
                        request.productId(), request.quantity(), product.price(), idempotencyKey));
                outboxWriter.write(saved.getId().toString(), Topics.ORDER_CREATED, new OrderCreatedEvent(
                        saved.getId(), userId, saved.getCustomerEmail(), saved.getProductId(),
                        saved.getQuantity(), saved.getAmount(), Instant.now()));
                return saved;
            });
            log.info("Order {} created for user {} (product {}, qty {})",
                    order.getId(), userId, request.productId(), request.quantity());
            return new CreateResult(order, true);
        } catch (DataIntegrityViolationException e) {
            // Two identical requests raced: the unique index on idempotency_key let only one through
            return orderRepository.findByIdempotencyKey(idempotencyKey)
                    .map(order -> new CreateResult(order, false))
                    .orElseThrow(() -> e);
        }
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID orderId, String userId) {
        return orderRepository.findById(orderId)
                .filter(order -> order.getUserId().equals(userId))
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders(String userId) {
        return orderRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    // ---------------------------------------------------------------- Saga steps

    @Transactional
    public void confirm(UUID orderId) {
        findPending(orderId).ifPresent(order -> {
            order.confirm();
            publishStatusChange(order, Topics.ORDER_CONFIRMED);
            log.info("Order {} CONFIRMED", orderId);
        });
    }

    @Transactional
    public void cancel(UUID orderId, String reason) {
        findPending(orderId).ifPresent(order -> {
            order.cancel(reason);
            publishStatusChange(order, Topics.ORDER_CANCELLED);
            log.info("Order {} CANCELLED: {}", orderId, reason);
        });
    }

    /** Events can arrive more than once; only a PENDING order may change state. */
    private Optional<Order> findPending(UUID orderId) {
        Optional<Order> order = orderRepository.findById(orderId);
        if (order.isEmpty()) {
            log.warn("Saga event for unknown order {}", orderId);
            return Optional.empty();
        }
        if (!order.get().isPending()) {
            log.info("Order {} already {}, ignoring duplicate event", orderId, order.get().getStatus());
            return Optional.empty();
        }
        return order;
    }

    private void publishStatusChange(Order order, String topic) {
        outboxWriter.write(order.getId().toString(), topic, new OrderStatusChangedEvent(
                order.getId(), order.getUserId(), order.getCustomerEmail(), order.getProductId(),
                order.getQuantity(), order.getAmount(), order.getStatus().name(), order.getFailureReason(),
                Instant.now()));
    }
}
