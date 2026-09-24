package com.shopmart.inventory.web;

import com.shopmart.inventory.domain.StockReservation;

import java.time.LocalDateTime;

public record ReservationResponse(Long id, String orderId, Long productId, int quantity, String status,
                                  LocalDateTime createdAt, LocalDateTime updatedAt, String servedBy) {

    public static ReservationResponse of(StockReservation r, String servedBy) {
        return new ReservationResponse(r.getId(), r.getOrderId(), r.getProductId(), r.getQuantity(),
                r.getStatus().name(), r.getCreatedAt(), r.getUpdatedAt(), servedBy);
    }
}
