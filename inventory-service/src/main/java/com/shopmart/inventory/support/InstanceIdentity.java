package com.shopmart.inventory.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InstanceIdentity {

    private final String servedBy;

    public InstanceIdentity(@Value("${spring.application.name}") String applicationName,
                            @Value("${server.port:8082}") String port) {
        this.servedBy = applicationName + ":" + port;
    }

    public String servedBy() {
        return servedBy;
    }
}
