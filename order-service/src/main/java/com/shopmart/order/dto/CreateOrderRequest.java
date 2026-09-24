package com.shopmart.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotBlank(message = "Mã khách hàng không được để trống")
        @Size(max = 32, message = "Mã khách hàng tối đa 32 ký tự")
        String customerId,

        @NotNull(message = "Mã sản phẩm là bắt buộc")
        @Positive(message = "Mã sản phẩm phải là số dương")
        Long productId,

        @NotNull(message = "Số lượng là bắt buộc")
        @Min(value = 1, message = "Số lượng tối thiểu là 1")
        @Max(value = 1000, message = "Số lượng tối đa là 1000")
        Integer quantity,

        Boolean simulatePaymentFailure) {
}
