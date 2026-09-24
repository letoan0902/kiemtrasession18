package com.shopmart.order.client;

public record ProductResponse(
        Long id,
        String name,
        Long price,
        Integer stock,
        String servedBy,
        String source) {
}
