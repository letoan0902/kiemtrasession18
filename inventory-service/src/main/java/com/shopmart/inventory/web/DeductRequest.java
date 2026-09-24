package com.shopmart.inventory.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DeductRequest(
        @NotBlank(message = "orderId không được để trống") String orderId,
        @NotNull(message = "quantity không được để trống")
        @Positive(message = "quantity phải lớn hơn 0") Integer quantity) {
}
