package com.shopmart.inventory.service;

import com.shopmart.inventory.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ChaosService {

    private static final Logger log = LoggerFactory.getLogger(ChaosService.class);

    public enum Mode {
        NORMAL, DOWN;

        public String code() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final AtomicReference<Mode> mode = new AtomicReference<>(Mode.NORMAL);

    public Mode getMode() {
        return mode.get();
    }

    public Mode setMode(String value) {
        Mode newMode;
        try {
            newMode = Mode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Chế độ chaos không hợp lệ: '" + value
                    + "'. Chỉ chấp nhận normal hoặc down");
        }
        Mode old = mode.getAndSet(newMode);
        if (old != newMode) {
            log.warn("Đổi chế độ chaos từ {} sang {}", old.code(), newMode.code());
        }
        return newMode;
    }

    public void ensureAvailable(String operation) {
        if (mode.get() == Mode.DOWN) {
            throw new ServiceUnavailableException("Kho hàng đang giả lập sự cố (chaos=down), không phục vụ "
                    + operation);
        }
    }
}
