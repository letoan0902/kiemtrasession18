package com.shopmart.payment.messaging;

public record RecordResult(String orderId, int partition, long offset, Status status, String publishedEventType) {

    public enum Status {
        PUBLISHED,
        NO_EVENT,
        SKIPPED
    }
}
