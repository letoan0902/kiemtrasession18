package com.shopmart.payment.config;

import com.shopmart.payment.event.EventJson;
import com.shopmart.payment.messaging.KafkaPaymentEventPublisher;
import com.shopmart.payment.messaging.PaymentEventProcessor;
import com.shopmart.payment.messaging.PaymentEventPublisher;
import com.shopmart.payment.messaging.ReactivePaymentPipeline;
import com.shopmart.payment.service.PaymentStats;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "shopmart.payment.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReactiveKafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(ReactiveKafkaConfig.class);

    @Bean
    public ReceiverOptions<String, String> orderReceiverOptions(PaymentKafkaProperties props) {
        Map<String, Object> consumer = new HashMap<>();
        consumer.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        consumer.put(ConsumerConfig.GROUP_ID_CONFIG, props.groupId());
        consumer.put(ConsumerConfig.CLIENT_ID_CONFIG, "payment-service-consumer");
        consumer.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumer.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumer.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, props.autoOffsetReset());
        consumer.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return ReceiverOptions.<String, String>create(consumer)
                .subscription(List.of(props.orderTopic()))
                .commitInterval(Duration.ofMillis(props.commitIntervalMs()))
                .commitBatchSize(props.commitBatchSize())
                .addAssignListener(partitions -> log.info("Consumer phản ứng được giao partition: {}", partitions))
                .addRevokeListener(partitions -> log.info("Consumer phản ứng bị thu hồi partition: {}", partitions));
    }

    @Bean(destroyMethod = "close")
    public KafkaSender<String, String> paymentKafkaSender(PaymentKafkaProperties props) {
        Map<String, Object> producer = new HashMap<>();
        producer.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        producer.put(ProducerConfig.CLIENT_ID_CONFIG, "payment-service-producer");
        producer.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producer.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producer.put(ProducerConfig.ACKS_CONFIG, "all");
        producer.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        producer.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        producer.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        producer.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 15000);
        return KafkaSender.create(SenderOptions.create(producer));
    }

    @Bean
    public PaymentEventPublisher kafkaPaymentEventPublisher(KafkaSender<String, String> paymentKafkaSender,
                                                            EventJson eventJson, PaymentKafkaProperties props) {
        return new KafkaPaymentEventPublisher(paymentKafkaSender, eventJson, props.paymentTopic());
    }

    @Bean
    public ReactivePaymentPipeline reactivePaymentPipeline(PaymentEventProcessor processor,
                                                           PaymentEventPublisher publisher,
                                                           PaymentStats stats, PaymentKafkaProperties props) {
        return new ReactivePaymentPipeline(processor, publisher, stats, props.recordRetryAttempts(),
                Duration.ofMillis(props.recordRetryBackoffMs()));
    }
}
