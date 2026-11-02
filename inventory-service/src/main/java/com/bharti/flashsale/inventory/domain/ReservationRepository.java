package com.bharti.flashsale.inventory.domain;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    long countByProductIdAndStatus(Long productId, ReservationStatus status);
}
