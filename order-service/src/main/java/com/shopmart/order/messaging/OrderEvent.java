package com.shopmart.order.messaging;

import java.time.Instant;

public record OrderEvent(
        String eventId,
        String eventType,
        String orderId,
        String customerId,
        Long productId,
        Integer quantity,
        Long unitPrice,
        Long totalAmount,
        Boolean simulatePaymentFailure,
        String reason,
        Instant occurredAt) {
}
