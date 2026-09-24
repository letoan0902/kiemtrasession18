package com.shopmart.payment.service;

import java.util.Locale;

public enum ChaosMode {
    NORMAL,
    FAIL,
    SLOW;

    public static ChaosMode parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Thiếu tham số mode, giá trị hợp lệ: normal, fail, slow");
        }
        try {
            return ChaosMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Chế độ không hợp lệ: '" + value + "', giá trị hợp lệ: normal, fail, slow");
        }
    }

    public String apiName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
