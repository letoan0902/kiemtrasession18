package com.shopmart.payment.messaging;

import com.shopmart.payment.event.EventJson;
import com.shopmart.payment.event.OrderEvent;
import com.shopmart.payment.event.PaymentEvent;
import com.shopmart.payment.service.ChaosMode;
import com.shopmart.payment.service.ChaosService;
import com.shopmart.payment.service.PaymentService;
import com.shopmart.payment.service.PaymentStats;
import com.shopmart.payment.service.SagaOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.Callable;

@Component
public class PaymentEventProcessor implements OrderEventHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProcessor.class);

    private final EventJson eventJson;
    private final PaymentService paymentService;
    private final ChaosService chaosService;
    private final PaymentStats stats;

    public PaymentEventProcessor(EventJson eventJson, PaymentService paymentService,
                                 ChaosService chaosService, PaymentStats stats) {
        this.eventJson = eventJson;
        this.paymentService = paymentService;
        this.chaosService = chaosService;
        this.stats = stats;
    }

    @Override
    public Mono<PaymentEvent> process(String key, String json) {
        return Mono.fromCallable(() -> eventJson.readOrderEvent(json))
                .flatMap(event -> {
                    stats.received(event.eventType());
                    return route(event);
                });
    }

    private Mono<PaymentEvent> route(OrderEvent event) {
        return switch (event.eventType()) {
            case OrderEvent.ORDER_CREATED -> onOrderCreated(event);
            case OrderEvent.ORDER_CANCELLED -> onOrderCancelled(event);
            default -> {
                log.warn("[SAGA][{}] Bỏ qua loại sự kiện không thuộc phạm vi payment-service: {}",
                        event.orderId(), event.eventType());
                yield Mono.empty();
            }
        };
    }

    private Mono<PaymentEvent> onOrderCreated(OrderEvent event) {
        log.info("[SAGA][{}] Nhận ORDER_CREATED: khách {}, số tiền {} đồng (luồng {})",
                event.orderId(), event.customerId(), event.totalAmount(), Thread.currentThread().getName());
        return slowModeDelay(event)
                .then(blocking(() -> paymentService.charge(event, chaosService.current().mode() == ChaosMode.FAIL)))
                .flatMap(this::toEvent);
    }

    private Mono<PaymentEvent> onOrderCancelled(OrderEvent event) {
        log.info("[SAGA][{}] Nhận ORDER_CANCELLED (lý do: {}), kiểm tra có cần hoàn tiền không (luồng {})",
                event.orderId(), event.reason(), Thread.currentThread().getName());
        return blocking(() -> paymentService.refund(event)).flatMap(this::toEvent);
    }

    private Mono<Void> slowModeDelay(OrderEvent event) {
        return Mono.defer(() -> {
            ChaosService.ChaosState chaos = chaosService.current();
            if (chaos.mode() != ChaosMode.SLOW || chaos.delaySeconds() <= 0) {
                return Mono.<Void>empty();
            }
            log.warn("[SAGA][{}] Chế độ slow: trễ {} giây rồi mới trừ tiền (trễ không chặn, luồng {} được trả về ngay)",
                    event.orderId(), chaos.delaySeconds(), Thread.currentThread().getName());
            return Mono.delay(Duration.ofSeconds(chaos.delaySeconds()))
                    .doOnNext(tick -> log.info("[SAGA][{}] Hết {} giây trễ, bắt đầu trừ tiền (luồng {})",
                            event.orderId(), chaos.delaySeconds(), Thread.currentThread().getName()))
                    .then();
        });
    }

    private Mono<PaymentEvent> toEvent(SagaOutcome outcome) {
        stats.outcome(outcome.kind());
        return Mono.justOrEmpty(outcome.event());
    }

    private static <T> Mono<T> blocking(Callable<T> call) {
        return Mono.fromCallable(call).subscribeOn(Schedulers.boundedElastic());
    }
}
