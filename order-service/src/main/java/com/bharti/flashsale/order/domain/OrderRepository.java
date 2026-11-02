package com.bharti.flashsale.order.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    List<Order> findTop50ByUserIdOrderByCreatedAtDesc(String userId);
}
