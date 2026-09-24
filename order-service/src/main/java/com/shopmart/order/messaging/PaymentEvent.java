package com.shopmart.order.messaging;

import java.time.Instant;

public record PaymentEvent(
        String eventId,
        String eventType,
        String orderId,
        String customerId,
        Long productId,
        Integer quantity,
        Long amount,
        String reason,
        Instant occurredAt) {
}
