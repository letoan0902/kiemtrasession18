package com.shopmart.order.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class InventoryFeignConfig {

    @Bean
    public ErrorDecoder inventoryErrorDecoder(ObjectMapper objectMapper) {
        return new InventoryErrorDecoder(objectMapper);
    }
}
