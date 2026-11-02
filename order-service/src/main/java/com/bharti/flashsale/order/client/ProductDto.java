package com.bharti.flashsale.order.client;

import java.math.BigDecimal;

public record ProductDto(Long id, String name, BigDecimal price, Integer availableStock) {
}
