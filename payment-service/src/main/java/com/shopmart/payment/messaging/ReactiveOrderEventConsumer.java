package com.shopmart.payment.messaging;

import com.shopmart.payment.config.PaymentKafkaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
@ConditionalOnProperty(prefix = "shopmart.payment.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReactiveOrderEventConsumer implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ReactiveOrderEventConsumer.class);

    private final ReceiverOptions<String, String> receiverOptions;
    private final ReactivePaymentPipeline pipeline;
    private final PaymentTopicInitializer topicInitializer;
    private final PaymentKafkaProperties props;

    private volatile Disposable subscription;

    public ReactiveOrderEventConsumer(ReceiverOptions<String, String> receiverOptions,
                                      ReactivePaymentPipeline pipeline,
                                      PaymentTopicInitializer topicInitializer,
                                      PaymentKafkaProperties props) {
        this.receiverOptions = receiverOptions;
        this.pipeline = pipeline;
        this.topicInitializer = topicInitializer;
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Flux<ReceiverRecord<String, String>> source =
                Flux.defer(() -> KafkaReceiver.create(receiverOptions).receive());

        subscription = Mono.fromRunnable(topicInitializer::ensurePaymentTopic)
                .subscribeOn(Schedulers.boundedElastic())
                .thenMany(pipeline.run(source))
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(signal -> log.warn(
                                "Luồng consumer phản ứng gặp lỗi, tự nối lại lần {}: {}",
                                signal.totalRetries() + 1, signal.failure().toString())))
                .subscribe(
                        result -> log.debug("[SAGA][{}] Đã acknowledge partition {} offset {} ({})",
                                result.orderId(), result.partition(), result.offset(), result.status()),
                        error -> log.error("Luồng consumer phản ứng đã DỪNG HẲN, cần khởi động lại service", error));

        log.info("Đã khởi động consumer Reactor Kafka: topic '{}', group '{}', phát kết quả lên topic '{}'",
                props.orderTopic(), props.groupId(), props.paymentTopic());
    }

    @Override
    public void destroy() {
        Disposable current = subscription;
        if (current != null && !current.isDisposed()) {
            current.dispose();
            log.info("Đã dừng consumer Reactor Kafka của topic '{}'", props.orderTopic());
        }
    }
}
