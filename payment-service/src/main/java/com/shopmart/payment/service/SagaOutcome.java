package com.shopmart.payment.service;

import com.shopmart.payment.event.PaymentEvent;

public record SagaOutcome(Kind kind, PaymentEvent event) {

    public enum Kind {
        CHARGE_COMPLETED,
        CHARGE_FAILED,
        CHARGE_DUPLICATE,
        REFUND_COMPLETED,
        REFUND_DUPLICATE,
        REFUND_NOTHING
    }
}
