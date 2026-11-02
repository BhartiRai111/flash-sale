package com.bharti.flashsale.web.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** JSON shapes returned by the API gateway (copies of the services' responses, owned by the UI). */
public final class ApiModels {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a").withZone(ZoneId.systemDefault());

    private ApiModels() {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }

    public record ProductView(Long id, String name, BigDecimal price, Integer availableStock) {
        public boolean soldOut() {
            return availableStock == null || availableStock <= 0;
        }

        public boolean lowStock() {
            return availableStock != null && availableStock > 0 && availableStock <= 10;
        }
    }

    public record OrderView(
            UUID id,
            String userId,
            Long productId,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            String status,
            String failureReason,
            Instant createdAt,
            Instant updatedAt) {

        public boolean pending() {
            return "PENDING".equals(status);
        }

        public boolean confirmed() {
            return "CONFIRMED".equals(status);
        }

        public boolean cancelled() {
            return "CANCELLED".equals(status);
        }

        public String createdLabel() {
            return createdAt == null ? "" : DATE_TIME.format(createdAt);
        }

        public String updatedLabel() {
            return updatedAt == null ? "" : DATE_TIME.format(updatedAt);
        }

        public String shortId() {
            return id == null ? "" : id.toString().substring(0, 8);
        }
    }
}
