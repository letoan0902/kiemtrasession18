package com.shopmart.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record RestoreRequest(@NotBlank(message = "orderId không được để trống") String orderId) {
}
