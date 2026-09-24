package com.shopmart.payment.messaging;

import com.shopmart.payment.event.EventJson;
import com.shopmart.payment.event.PaymentEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPaymentEventPublisher.class);

    private final KafkaSender<String, String> sender;
    private final EventJson eventJson;
    private final String topic;

    public KafkaPaymentEventPublisher(KafkaSender<String, String> sender, EventJson eventJson, String topic) {
        this.sender = sender;
        this.eventJson = eventJson;
        this.topic = topic;
    }

    @Override
    public Mono<Void> publish(PaymentEvent event) {
        return Mono.fromCallable(() -> eventJson.write(event))
                .flatMap(json -> sender
                        .send(Mono.just(SenderRecord.create(new ProducerRecord<>(topic, event.orderId(), json), event.orderId())))
                        .next())
                .flatMap(result -> result.exception() != null
                        ? Mono.error(result.exception())
                        : Mono.just(result))
                .doOnNext(result -> log.info("[SAGA][{}] Đã phát {} lên topic {} (partition {}, offset {}, luồng {})",
                        event.orderId(), event.eventType(), topic, result.recordMetadata().partition(),
                        result.recordMetadata().offset(), Thread.currentThread().getName()))
                .doOnError(e -> log.error("[SAGA][{}] Phát {} lên topic {} THẤT BẠI: {}",
                        event.orderId(), event.eventType(), topic, e.toString()))
                .then();
    }
}
