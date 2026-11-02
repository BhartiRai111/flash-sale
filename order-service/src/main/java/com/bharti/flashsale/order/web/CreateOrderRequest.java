package com.bharti.flashsale.order.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull Long productId,
        // Flash-sale rule: max 5 units per order
        @NotNull @Min(1) @Max(5) Integer quantity,
        @NotBlank @Email String customerEmail) {
}
