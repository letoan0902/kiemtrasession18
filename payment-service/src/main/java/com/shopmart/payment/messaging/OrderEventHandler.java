package com.shopmart.payment.messaging;

import com.shopmart.payment.event.PaymentEvent;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface OrderEventHandler {

    Mono<PaymentEvent> process(String key, String json);
}
