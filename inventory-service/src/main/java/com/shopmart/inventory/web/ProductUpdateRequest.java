package com.shopmart.inventory.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductUpdateRequest(
        @NotBlank(message = "name không được để trống") String name,
        @NotNull(message = "price không được để trống")
        @PositiveOrZero(message = "price không được âm") Long price,
        @NotNull(message = "stock không được để trống")
        @PositiveOrZero(message = "stock không được âm") Integer stock) {
}
