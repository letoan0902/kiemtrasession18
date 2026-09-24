package com.shopmart.inventory.service;

public record RestoreResult(String orderId, Long productId, Integer quantity, Integer remainingStock,
                            boolean restored, String reason) {
}
