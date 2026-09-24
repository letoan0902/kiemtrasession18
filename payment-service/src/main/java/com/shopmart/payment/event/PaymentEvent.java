package com.shopmart.payment.event;

import java.time.Instant;
import java.util.UUID;

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

    public static final String PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String PAYMENT_REFUNDED = "PAYMENT_REFUNDED";

    public static PaymentEvent of(String eventType, String orderId, String customerId, Long productId,
                                  Integer quantity, Long amount, String reason) {
        return new PaymentEvent(UUID.randomUUID().toString(), eventType, orderId, customerId, productId,
                quantity, amount, reason, Instant.now());
    }
}
