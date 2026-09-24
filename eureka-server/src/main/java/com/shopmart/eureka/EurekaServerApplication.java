package com.shopmart.eureka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    private static final Logger log = LoggerFactory.getLogger(EurekaServerApplication.class);

    private final Environment environment;

    public EurekaServerApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void inThongTinKhoiDong() {
        log.info("Eureka Server đã sẵn sàng ở cổng {}, bảng điều khiển: http://localhost:{}/",
                environment.getProperty("local.server.port"),
                environment.getProperty("local.server.port"));
    }
}
