package com.bharti.flashsale.payment.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.bharti.flashsale.payment.domain.Payment;
import com.bharti.flashsale.payment.domain.PaymentStatus;
import com.bharti.flashsale.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    public record PaymentResponse(UUID id, UUID orderId, BigDecimal amount, PaymentStatus status,
                                  String failureReason, Instant createdAt) {
        static PaymentResponse from(Payment p) {
            return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmount(), p.getStatus(),
                    p.getFailureReason(), p.getCreatedAt());
        }
    }

    @GetMapping("/order/{orderId}")
    public PaymentResponse byOrder(@PathVariable UUID orderId) {
        return paymentService.findByOrderId(orderId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No payment for order " + orderId));
    }
}
