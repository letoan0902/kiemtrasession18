package com.shopmart.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class ApiGatewayApplication {

    private static final Logger log = LoggerFactory.getLogger(ApiGatewayApplication.class);

    private final RouteLocator routeLocator;

    public ApiGatewayApplication(RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
    }

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void inDanhSachRoute() {
        routeLocator.getRoutes()
                .doOnNext(route -> log.info("Route {} -> {} (predicate: {})",
                        route.getId(), route.getUri(), route.getPredicate()))
                .count()
                .subscribe(soRoute -> {
                    if (soRoute == 0) {
                        log.warn("Gateway chưa có route nào. Kiểm tra Config Server và config-repo/api-gateway.yml");
                    } else {
                        log.info("Gateway đã nạp {} route", soRoute);
                    }
                });
    }
}
