package com.shopmart.order.service;

import com.shopmart.order.domain.OrderStatus;
import com.shopmart.order.messaging.KafkaTopics;
import com.shopmart.order.messaging.PaymentEvent;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentResultHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultHandler.class);

    private final OrderStateMachine stateMachine;

    public PaymentResultHandler(OrderStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    public void handle(PaymentEvent event) {
        switch (event.eventType()) {
            case KafkaTopics.PAYMENT_COMPLETED -> onPaymentCompleted(event);
            case KafkaTopics.PAYMENT_FAILED -> onPaymentFailed(event);
            case KafkaTopics.PAYMENT_REFUNDED -> onPaymentRefunded(event);
            default -> log.warn("[SAGA][{}] Bỏ qua sự kiện thanh toán không rõ loại {}", event.orderId(),
                    event.eventType());
        }
    }

    private void onPaymentCompleted(PaymentEvent event) {
        String orderId = event.orderId();
        boolean changed = stateMachine.transition(orderId, OrderStatus.INVENTORY_RESERVED, OrderStatus.CONFIRMED,
                null, "Nhận PAYMENT_COMPLETED, đã thanh toán " + event.amount() + " đồng");
        if (changed) {
            log.info("[SAGA][{}] Nhận PAYMENT_COMPLETED, chuyển CONFIRMED", orderId);
            return;
        }
        Optional<OrderStatus> current = stateMachine.currentStatus(orderId);
        if (current.isEmpty()) {
            log.warn("[SAGA][{}] Nhận PAYMENT_COMPLETED cho đơn không tồn tại, bỏ qua", orderId);
        } else if (current.get() == OrderStatus.CONFIRMED) {
            log.info("[SAGA][{}] Nhận lại PAYMENT_COMPLETED, đơn đã CONFIRMED từ trước, bỏ qua bản tin trùng",
                    orderId);
        } else if (current.get() == OrderStatus.CANCELLED) {
            log.warn("[SAGA][{}] Đơn đã CANCELLED, thanh toán đến muộn, payment-service sẽ hoàn tiền khi nhận "
                    + "ORDER_CANCELLED", orderId);
        } else {
            log.warn("[SAGA][{}] Nhận PAYMENT_COMPLETED khi đơn đang {}, không chuyển trạng thái", orderId,
                    current.get());
        }
    }

    private void onPaymentFailed(PaymentEvent event) {
        String orderId = event.orderId();
        String reason = "Thanh toán thất bại" + (event.reason() != null ? ": " + event.reason() : "");
        boolean changed = stateMachine.transition(orderId, OrderStatus.INVENTORY_RESERVED, OrderStatus.CANCELLED,
                reason, "Nhận PAYMENT_FAILED. " + reason);
        if (changed) {
            log.info("[SAGA][{}] Nhận PAYMENT_FAILED, chuyển CANCELLED: {}", orderId, reason);
            return;
        }
        Optional<OrderStatus> current = stateMachine.currentStatus(orderId);
        if (current.isPresent() && current.get() == OrderStatus.CANCELLED) {
            log.info("[SAGA][{}] Nhận PAYMENT_FAILED, đơn đã CANCELLED từ trước, bỏ qua", orderId);
        } else {
            log.warn("[SAGA][{}] Nhận PAYMENT_FAILED khi đơn đang {}, không chuyển trạng thái", orderId,
                    current.map(Enum::name).orElse("không tồn tại"));
        }
    }

    private void onPaymentRefunded(PaymentEvent event) {
        String orderId = event.orderId();
        String note = "Nhận PAYMENT_REFUNDED, đã hoàn " + event.amount() + " đồng cho khách " + event.customerId();
        if (stateMachine.recordNoteOnce(orderId, note)) {
            log.info("[SAGA][{}] Nhận PAYMENT_REFUNDED, ghi lịch sử hoàn tiền {} đồng", orderId, event.amount());
        } else {
            log.info("[SAGA][{}] Nhận PAYMENT_REFUNDED trùng hoặc đơn không tồn tại, bỏ qua", orderId);
        }
    }
}
