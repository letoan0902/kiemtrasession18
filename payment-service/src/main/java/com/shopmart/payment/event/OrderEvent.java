package com.shopmart.payment.event;

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
        String reason) {

    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";

    public boolean wantsSimulatedFailure() {
        return Boolean.TRUE.equals(simulatePaymentFailure);
    }
}
