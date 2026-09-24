package com.shopmart.order.service;

import com.shopmart.order.domain.OrderEntity;
import com.shopmart.order.domain.OrderStatus;
import com.shopmart.order.domain.OrderStatusHistory;
import com.shopmart.order.repository.OrderRepository;
import com.shopmart.order.repository.OrderStatusHistoryRepository;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.INVENTORY_RESERVED, OrderStatus.REJECTED));
        ALLOWED.put(OrderStatus.INVENTORY_RESERVED, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
    }

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;

    public OrderStateMachine(OrderRepository orderRepository, OrderStatusHistoryRepository historyRepository) {
        this.orderRepository = orderRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public OrderEntity createPending(OrderEntity order, String note) {
        Instant now = Instant.now();
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        OrderEntity saved = orderRepository.save(order);
        historyRepository.save(new OrderStatusHistory(saved.getOrderId(), null, OrderStatus.PENDING, note, now));
        return saved;
    }

    @Transactional
    public boolean markReserved(String orderId, String inventoryInstance, String note) {
        Instant now = Instant.now();
        int updated = orderRepository.markReserved(orderId, inventoryInstance, now);
        if (updated != 1) {
            return false;
        }
        historyRepository.save(new OrderStatusHistory(orderId, OrderStatus.PENDING,
                OrderStatus.INVENTORY_RESERVED, note, now));
        return true;
    }

    @Transactional
    public boolean transition(String orderId, OrderStatus from, OrderStatus to, String failureReason, String note) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalArgumentException("Không cho phép chuyển trạng thái " + from + " sang " + to);
        }
        Instant now = Instant.now();
        int updated = failureReason == null
                ? orderRepository.transition(orderId, from, to, now)
                : orderRepository.transitionWithReason(orderId, from, to, failureReason, now);
        if (updated != 1) {
            return false;
        }
        historyRepository.save(new OrderStatusHistory(orderId, from, to, note, now));
        return true;
    }

    @Transactional
    public boolean recordNoteOnce(String orderId, String note) {
        Optional<OrderEntity> order = orderRepository.findByOrderId(orderId);
        if (order.isEmpty() || historyRepository.existsByOrderIdAndNote(orderId, note)) {
            return false;
        }
        OrderStatus current = order.get().getStatus();
        historyRepository.save(new OrderStatusHistory(orderId, current, current, note, Instant.now()));
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<OrderStatus> currentStatus(String orderId) {
        return orderRepository.findByOrderId(orderId).map(OrderEntity::getStatus);
    }
}
