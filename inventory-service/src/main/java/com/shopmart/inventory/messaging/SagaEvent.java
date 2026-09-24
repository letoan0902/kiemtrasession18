package com.shopmart.inventory.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SagaEvent(String eventId,
                        String eventType,
                        String orderId,
                        Long productId,
                        Integer quantity,
                        String reason) {
}
