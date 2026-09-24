package com.shopmart.order.messaging;

public final class KafkaTopics {

    public static final String ORDER = "order";
    public static final String PAYMENT = "payment";

    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";

    public static final String PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String PAYMENT_REFUNDED = "PAYMENT_REFUNDED";

    private KafkaTopics() {
    }
}
