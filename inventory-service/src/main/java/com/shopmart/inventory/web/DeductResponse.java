package com.shopmart.inventory.web;

public record DeductResponse(String orderId, Long productId, int quantity, int remainingStock,
                             String servedBy, boolean alreadyProcessed) {
}
