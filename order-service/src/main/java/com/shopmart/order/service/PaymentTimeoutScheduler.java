package com.shopmart.order.service;

import com.shopmart.order.domain.OrderEntity;
import com.shopmart.order.domain.OrderStatus;
import com.shopmart.order.messaging.OrderEventPublisher;
import com.shopmart.order.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentTimeoutScheduler.class);

    private final OrderRepository orderRepository;
    private final OrderStateMachine stateMachine;
    private final OrderEventPublisher eventPublisher;
    private final long timeoutSeconds;

    public PaymentTimeoutScheduler(OrderRepository orderRepository, OrderStateMachine stateMachine,
                                   OrderEventPublisher eventPublisher,
                                   @Value("${shopmart.saga.payment-timeout-seconds:15}") long timeoutSeconds) {
        this.orderRepository = orderRepository;
        this.stateMachine = stateMachine;
        this.eventPublisher = eventPublisher;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Scheduled(fixedDelay = 2000)
    public void scheduledSweep() {
        try {
            cancelExpiredOrders(Instant.now());
        } catch (RuntimeException ex) {
            log.error("Bộ quét đơn quá hạn gặp lỗi, sẽ thử lại ở lượt sau: {}", ex.toString());
        }
    }

    public int cancelExpiredOrders(Instant now) {
        Instant deadline = now.minusSeconds(timeoutSeconds);
        List<OrderEntity> expired = orderRepository
                .findTop100ByStatusAndReservedAtBeforeOrderByReservedAtAsc(OrderStatus.INVENTORY_RESERVED, deadline);
        int cancelled = 0;
        for (OrderEntity order : expired) {
            String reason = "Quá thời hạn " + timeoutSeconds + " giây chờ kết quả thanh toán";
            boolean changed = stateMachine.transition(order.getOrderId(), OrderStatus.INVENTORY_RESERVED,
                    OrderStatus.CANCELLED, reason, "Bộ quét quá hạn. " + reason);
            if (!changed) {
                continue;
            }
            cancelled++;
            log.info("[SAGA][{}] Quá thời hạn {} giây chờ thanh toán, chuyển CANCELLED và phát ORDER_CANCELLED",
                    order.getOrderId(), timeoutSeconds);
            eventPublisher.publishOrderCancelled(order, reason);
        }
        return cancelled;
    }
}
