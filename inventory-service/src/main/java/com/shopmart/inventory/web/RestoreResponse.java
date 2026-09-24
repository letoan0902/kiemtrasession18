package com.shopmart.inventory.web;

public record RestoreResponse(String orderId, Long productId, Integer quantity, Integer remainingStock,
                              boolean restored, String reason, String servedBy) {
}
