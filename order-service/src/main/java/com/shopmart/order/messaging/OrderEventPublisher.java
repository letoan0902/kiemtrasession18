package com.shopmart.order.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmart.order.domain.OrderEntity;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishOrderCreated(OrderEntity order) {
        publish(buildEvent(KafkaTopics.ORDER_CREATED, order, null));
    }

    public void publishOrderCancelled(OrderEntity order, String reason) {
        publish(buildEvent(KafkaTopics.ORDER_CANCELLED, order, reason));
    }

    private OrderEvent buildEvent(String eventType, OrderEntity order, String reason) {
        return new OrderEvent(
                UUID.randomUUID().toString(),
                eventType,
                order.getOrderId(),
                order.getCustomerId(),
                order.getProductId(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.isSimulatePaymentFailure(),
                reason,
                Instant.now());
    }

    private void publish(OrderEvent event) {
        final String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không tuần tự hóa được sự kiện " + event.eventType(), e);
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event, json);
                }
            });
            return;
        }
        send(event, json);
    }

    private void send(OrderEvent event, String json) {
        String orderId = event.orderId();
        try {
            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(KafkaTopics.ORDER, orderId, json);
            log.info("[SAGA][{}] Phát {} lên topic {}{}", orderId, event.eventType(), KafkaTopics.ORDER,
                    event.reason() != null ? ", lý do: " + event.reason() : "");
            if (future != null) {
                future.whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[SAGA][{}] Kafka không nhận {}: {}", orderId, event.eventType(), ex.toString());
                    } else {
                        log.debug("[SAGA][{}] Kafka đã ghi {} vào partition {} offset {}", orderId,
                                event.eventType(), result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
            }
        } catch (RuntimeException ex) {
            log.error("[SAGA][{}] Phát {} thất bại: {}", orderId, event.eventType(), ex.toString());
        }
    }
}
