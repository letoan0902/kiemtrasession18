package com.shopmart.inventory.service;

public record DeductResult(String orderId, Long productId, int quantity, int remainingStock,
                           boolean alreadyProcessed) {
}
