package com.bharti.flashsale.web.controller;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Form-backing beans for the HTML forms (Thymeleaf binds fields by getters/setters). */
public final class Forms {

    private Forms() {
    }

    @Data
    public static class LoginForm {
        @NotBlank(message = "Enter your username")
        private String username;
        @NotBlank(message = "Enter your password")
        private String password;
    }

    @Data
    public static class BuyForm {
        /** Generated once when the page renders; the same key is re-sent if the user double-clicks. */
        @NotBlank
        private String idempotencyKey;
        @NotNull(message = "Choose a quantity")
        @Min(value = 1, message = "At least 1")
        @Max(value = 5, message = "Max 5 per order")
        private Integer quantity = 1;
        @NotBlank(message = "Enter your email")
        @Email(message = "Enter a valid email")
        private String customerEmail;
    }

    @Data
    public static class ProductForm {
        @NotBlank(message = "Enter a name")
        private String name;
        @NotNull(message = "Enter a price")
        @DecimalMin(value = "0.01", message = "Price must be more than 0")
        private BigDecimal price;
        @NotNull(message = "Enter the stock")
        @Min(value = 0, message = "Stock cannot be negative")
        private Integer stock;
    }

    @Data
    public static class RestockForm {
        @NotNull(message = "Enter a quantity")
        @Min(value = 1, message = "At least 1")
        private Integer quantity;
    }
}
