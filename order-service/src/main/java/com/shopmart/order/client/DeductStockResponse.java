package com.shopmart.order.client;

public record DeductStockResponse(
        String orderId,
        Long productId,
        Integer quantity,
        Integer remainingStock,
        String servedBy,
        Boolean alreadyProcessed) {
}
