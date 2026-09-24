package com.shopmart.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "shopmart.payment.kafka")
public record PaymentKafkaProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("localhost:9092") String bootstrapServers,
        @DefaultValue("order") String orderTopic,
        @DefaultValue("payment") String paymentTopic,
        @DefaultValue("3") int paymentTopicPartitions,
        @DefaultValue("payment-service-group") String groupId,
        @DefaultValue("earliest") String autoOffsetReset,
        @DefaultValue("1000") long commitIntervalMs,
        @DefaultValue("10") int commitBatchSize,
        @DefaultValue("3") int recordRetryAttempts,
        @DefaultValue("1000") long recordRetryBackoffMs) {
}
