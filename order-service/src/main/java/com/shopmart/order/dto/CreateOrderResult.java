package com.shopmart.order.dto;

public record CreateOrderResult(Outcome outcome, OrderResponse order) {

    public enum Outcome {
        RESERVED,
        OUT_OF_STOCK,
        PRODUCT_NOT_FOUND,
        INVENTORY_UNAVAILABLE
    }
}
