package com.shopmart.payment.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

@Component
public class EventJson {

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public OrderEvent readOrderEvent(String json) {
        if (json == null || json.isBlank()) {
            throw new MalformedEventException("Bản tin rỗng");
        }
        OrderEvent event;
        try {
            event = mapper.readValue(json, OrderEvent.class);
        } catch (JsonProcessingException e) {
            throw new MalformedEventException("Không phân giải được JSON: " + e.getOriginalMessage(), e);
        }
        if (event == null || isBlank(event.orderId()) || isBlank(event.eventType())) {
            throw new MalformedEventException("Bản tin thiếu trường bắt buộc orderId hoặc eventType");
        }
        return event;
    }

    public String write(PaymentEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không tuần tự hóa được sự kiện thanh toán", e);
        }
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
