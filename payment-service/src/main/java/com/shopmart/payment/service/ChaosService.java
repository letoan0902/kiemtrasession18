package com.shopmart.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class ChaosService {

    private static final Logger log = LoggerFactory.getLogger(ChaosService.class);
    private static final int MAX_DELAY_SECONDS = 600;

    public record ChaosState(ChaosMode mode, int delaySeconds) {
    }

    private final int defaultDelaySeconds;
    private final AtomicReference<ChaosState> state;

    public ChaosService(@Value("${shopmart.payment.slow-delay-seconds:25}") int defaultDelaySeconds) {
        this.defaultDelaySeconds = defaultDelaySeconds;
        this.state = new AtomicReference<>(new ChaosState(ChaosMode.NORMAL, defaultDelaySeconds));
    }

    public ChaosState current() {
        return state.get();
    }

    public ChaosState set(String mode, Integer delaySeconds) {
        ChaosMode parsed = ChaosMode.parse(mode);
        int delay = delaySeconds == null ? defaultDelaySeconds : delaySeconds;
        if (delay < 0 || delay > MAX_DELAY_SECONDS) {
            throw new IllegalArgumentException("delaySeconds phải nằm trong khoảng 0 đến " + MAX_DELAY_SECONDS);
        }
        ChaosState next = new ChaosState(parsed, delay);
        state.set(next);
        log.warn("Đã chuyển chế độ hỗn loạn của payment-service sang '{}' (trễ {} giây nếu là slow)",
                parsed.apiName(), delay);
        return next;
    }
}
