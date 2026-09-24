package com.shopmart.payment.messaging;

import com.shopmart.payment.event.MalformedEventException;
import com.shopmart.payment.service.PaymentStats;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

import java.time.Duration;

public class ReactivePaymentPipeline {

    private static final Logger log = LoggerFactory.getLogger(ReactivePaymentPipeline.class);

    static final int MAX_CONCURRENT_PARTITIONS = 64;

    private final OrderEventHandler handler;
    private final PaymentEventPublisher publisher;
    private final PaymentStats stats;
    private final int retryAttempts;
    private final Duration retryBackoff;

    public ReactivePaymentPipeline(OrderEventHandler handler, PaymentEventPublisher publisher, PaymentStats stats,
                                   int retryAttempts, Duration retryBackoff) {
        this.handler = handler;
        this.publisher = publisher;
        this.stats = stats;
        this.retryAttempts = retryAttempts;
        this.retryBackoff = retryBackoff;
    }

    public Flux<RecordResult> run(Flux<ReceiverRecord<String, String>> records) {
        return records
                .groupBy(record -> new TopicPartition(record.topic(), record.partition()))
                .flatMap(partition -> partition.concatMap(this::handleRecord), MAX_CONCURRENT_PARTITIONS);
    }

    Mono<RecordResult> handleRecord(ReceiverRecord<String, String> record) {
        String orderId = record.key() == null ? "khong-ro-orderId" : record.key();
        ReceiverOffset offset = record.receiverOffset();
        int partition = record.partition();
        long position = record.offset();

        return Mono.defer(() -> handler.process(record.key(), record.value()))
                .flatMap(event -> publisher.publish(event)
                        .doOnSuccess(done -> stats.published())
                        .thenReturn(new RecordResult(orderId, partition, position,
                                RecordResult.Status.PUBLISHED, event.eventType())))
                .defaultIfEmpty(new RecordResult(orderId, partition, position, RecordResult.Status.NO_EVENT, null))
                .retryWhen(Retry.backoff(retryAttempts, retryBackoff)
                        .filter(e -> !(e instanceof MalformedEventException))
                        .doBeforeRetry(signal -> log.warn("[SAGA][{}] Lỗi tạm thời, thử lại lần {}: {}",
                                orderId, signal.totalRetries() + 1, signal.failure().toString())))
                .onErrorResume(error -> {
                    Throwable cause = Exceptions.isRetryExhausted(error) && error.getCause() != null
                            ? error.getCause() : error;
                    log.error("[SAGA][{}] Bỏ qua bản tin lỗi ở partition {} offset {}: {} (luồng {})",
                            orderId, partition, position, cause.toString(), Thread.currentThread().getName());
                    stats.skipped();
                    return Mono.just(new RecordResult(orderId, partition, position, RecordResult.Status.SKIPPED, null));
                })
                .doOnNext(result -> offset.acknowledge());
    }
}
