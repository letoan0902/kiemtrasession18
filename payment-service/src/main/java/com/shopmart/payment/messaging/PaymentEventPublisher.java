package com.shopmart.payment.messaging;

import com.shopmart.payment.event.PaymentEvent;
import reactor.core.publisher.Mono;

public interface PaymentEventPublisher {

    Mono<Void> publish(PaymentEvent event);
}
