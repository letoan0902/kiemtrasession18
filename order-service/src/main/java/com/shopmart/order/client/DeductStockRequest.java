package com.shopmart.order.client;

public record DeductStockRequest(String orderId, Integer quantity) {
}
