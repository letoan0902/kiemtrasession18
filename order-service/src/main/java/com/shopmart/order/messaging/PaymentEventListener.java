package com.shopmart.order.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmart.order.service.PaymentResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final ObjectMapper objectMapper;
    private final PaymentResultHandler handler;

    public PaymentEventListener(ObjectMapper objectMapper, PaymentResultHandler handler) {
        this.objectMapper = objectMapper;
        this.handler = handler;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT, groupId = "order-service-group",
            autoStartup = "${shopmart.kafka.listener-auto-startup:true}")
    public void onPaymentEvent(String payload) {
        PaymentEvent event;
        try {
            event = objectMapper.readValue(payload, PaymentEvent.class);
        } catch (JsonProcessingException ex) {
            log.error("Bỏ qua bản tin topic payment không phân giải được: {}. Nội dung: {}",
                    ex.getOriginalMessage(), abbreviate(payload));
            return;
        }
        if (event.orderId() == null || event.eventType() == null) {
            log.error("Bỏ qua bản tin topic payment thiếu orderId hoặc eventType: {}", abbreviate(payload));
            return;
        }
        log.debug("[SAGA][{}] Nhận {} từ topic payment", event.orderId(), event.eventType());
        handler.handle(event);
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "null";
        }
        return text.length() <= 300 ? text : text.substring(0, 300) + "...";
    }
}
