package com.shopmart.order.dto;

import com.shopmart.order.domain.OrderEntity;
import com.shopmart.order.domain.OrderStatusHistory;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String orderId,
        String status,
        String customerId,
        Long productId,
        String productName,
        Integer quantity,
        Long unitPrice,
        Long totalAmount,
        String failureReason,
        String inventoryInstance,
        Instant createdAt,
        Instant updatedAt,
        List<HistoryEntry> history) {

    public record HistoryEntry(String fromStatus, String toStatus, String note, Instant at) {

        public static HistoryEntry from(OrderStatusHistory h) {
            return new HistoryEntry(
                    h.getFromStatus() != null ? h.getFromStatus().name() : null,
                    h.getToStatus().name(),
                    h.getNote(),
                    h.getChangedAt());
        }
    }

    public static OrderResponse from(OrderEntity o, List<OrderStatusHistory> history) {
        return new OrderResponse(
                o.getOrderId(),
                o.getStatus().name(),
                o.getCustomerId(),
                o.getProductId(),
                o.getProductName(),
                o.getQuantity(),
                o.getUnitPrice(),
                o.getTotalAmount(),
                o.getFailureReason(),
                o.getInventoryInstance(),
                o.getCreatedAt(),
                o.getUpdatedAt(),
                history.stream().map(HistoryEntry::from).toList());
    }
}
