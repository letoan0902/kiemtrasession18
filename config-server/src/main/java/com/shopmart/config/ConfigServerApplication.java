package com.shopmart.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    private static final Logger log = LoggerFactory.getLogger(ConfigServerApplication.class);

    private final Environment environment;

    public ConfigServerApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void inThongTinKhoiDong() {
        log.info("Config Server đã sẵn sàng ở cổng {}, profile {}, nguồn native: {}, nguồn git: {}",
                environment.getProperty("local.server.port"),
                String.join(",", environment.getActiveProfiles()),
                environment.getProperty("spring.cloud.config.server.native.search-locations", "(không dùng)"),
                environment.getProperty("spring.cloud.config.server.git.uri", "(không dùng)"));
    }
}
