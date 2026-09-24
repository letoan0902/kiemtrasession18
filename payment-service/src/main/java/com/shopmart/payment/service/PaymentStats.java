package com.shopmart.payment.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PaymentStats {

    public record Snapshot(
            Map<String, Long> receivedByType,
            long chargeSucceeded,
            long chargeFailed,
            long refunded,
            long refundNothingToDo,
            long duplicatesIgnored,
            long skippedMessages,
            long publishedEvents) {
    }

    private final Map<String, AtomicLong> receivedByType = new ConcurrentHashMap<>();
    private final AtomicLong chargeSucceeded = new AtomicLong();
    private final AtomicLong chargeFailed = new AtomicLong();
    private final AtomicLong refunded = new AtomicLong();
    private final AtomicLong refundNothingToDo = new AtomicLong();
    private final AtomicLong duplicatesIgnored = new AtomicLong();
    private final AtomicLong skippedMessages = new AtomicLong();
    private final AtomicLong publishedEvents = new AtomicLong();

    public void received(String eventType) {
        receivedByType.computeIfAbsent(eventType, k -> new AtomicLong()).incrementAndGet();
    }

    public void outcome(SagaOutcome.Kind kind) {
        switch (kind) {
            case CHARGE_COMPLETED -> chargeSucceeded.incrementAndGet();
            case CHARGE_FAILED -> chargeFailed.incrementAndGet();
            case REFUND_COMPLETED -> refunded.incrementAndGet();
            case REFUND_NOTHING -> refundNothingToDo.incrementAndGet();
            case CHARGE_DUPLICATE, REFUND_DUPLICATE -> duplicatesIgnored.incrementAndGet();
        }
    }

    public void skipped() {
        skippedMessages.incrementAndGet();
    }

    public void published() {
        publishedEvents.incrementAndGet();
    }

    public Snapshot snapshot() {
        Map<String, Long> byType = new TreeMap<>();
        receivedByType.forEach((k, v) -> byType.put(k, v.get()));
        return new Snapshot(byType, chargeSucceeded.get(), chargeFailed.get(), refunded.get(),
                refundNothingToDo.get(), duplicatesIgnored.get(), skippedMessages.get(), publishedEvents.get());
    }
}
