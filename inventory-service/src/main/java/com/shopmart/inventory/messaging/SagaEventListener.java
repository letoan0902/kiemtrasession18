package com.shopmart.inventory.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmart.inventory.service.RestoreResult;
import com.shopmart.inventory.service.StockService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SagaEventListener {

    private static final Logger log = LoggerFactory.getLogger(SagaEventListener.class);

    public static final String TOPIC_ORDER = "order";
    public static final String TOPIC_PAYMENT = "payment";

    private final ObjectMapper objectMapper;
    private final StockService stockService;

    public SagaEventListener(ObjectMapper objectMapper, StockService stockService) {
        this.objectMapper = objectMapper;
        this.stockService = stockService;
    }

    @KafkaListener(topics = {TOPIC_ORDER, TOPIC_PAYMENT}, groupId = "inventory-service-group")
    public void onMessage(ConsumerRecord<String, String> record) {
        handle(record.topic(), record.value());
    }

    public void handle(String topic, String payload) {
        SagaEvent event;
        try {
            event = payload == null ? null : objectMapper.readValue(payload, SagaEvent.class);
        } catch (Exception ex) {
            log.error("Không phân giải được bản tin trên topic '{}' ({}). Bỏ qua để không kẹt partition. Nội dung: {}",
                    topic, ex.getMessage(), payload);
            return;
        }
        if (event == null || event.orderId() == null || event.orderId().isBlank()
                || event.eventType() == null) {
            log.error("Bản tin trên topic '{}' thiếu orderId hoặc eventType, bỏ qua. Nội dung: {}", topic, payload);
            return;
        }

        switch (event.eventType()) {
            case "PAYMENT_FAILED", "ORDER_CANCELLED" -> compensate(event);
            default -> log.debug("[SAGA][{}] inventory-service bỏ qua sự kiện {} trên topic '{}'",
                    event.orderId(), event.eventType(), topic);
        }
    }

    private void compensate(SagaEvent event) {
        log.info("[SAGA][{}] nhận {}, bắt đầu hoàn tồn kho (lý do: {})",
                event.orderId(), event.eventType(), event.reason());
        RestoreResult result = stockService.restore(event.orderId(), null, event.eventType());
        if (event.productId() != null && result.productId() != null
                && !event.productId().equals(result.productId())) {
            log.warn("[SAGA][{}] sự kiện ghi sản phẩm {} nhưng bản ghi trừ kho là sản phẩm {}, đã hoàn theo bản ghi trừ kho",
                    event.orderId(), event.productId(), result.productId());
        }
    }
}
