package com.shopmart.order.service;

import com.shopmart.order.client.DeductStockResponse;
import com.shopmart.order.client.ProductResponse;
import com.shopmart.order.domain.OrderEntity;
import com.shopmart.order.domain.OrderStatus;
import com.shopmart.order.dto.CreateOrderRequest;
import com.shopmart.order.dto.CreateOrderResult;
import com.shopmart.order.dto.CreateOrderResult.Outcome;
import com.shopmart.order.dto.OrderResponse;
import com.shopmart.order.exception.InsufficientStockException;
import com.shopmart.order.exception.InventoryUnavailableException;
import com.shopmart.order.exception.OrderNotFoundException;
import com.shopmart.order.exception.ProductNotFoundException;
import com.shopmart.order.gateway.InventoryGateway;
import com.shopmart.order.messaging.OrderEventPublisher;
import com.shopmart.order.repository.OrderRepository;
import com.shopmart.order.repository.OrderStatusHistoryRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final InventoryGateway inventoryGateway;
    private final OrderStateMachine stateMachine;
    private final OrderEventPublisher eventPublisher;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;

    public OrderService(InventoryGateway inventoryGateway, OrderStateMachine stateMachine,
                        OrderEventPublisher eventPublisher, OrderRepository orderRepository,
                        OrderStatusHistoryRepository historyRepository) {
        this.inventoryGateway = inventoryGateway;
        this.stateMachine = stateMachine;
        this.eventPublisher = eventPublisher;
        this.orderRepository = orderRepository;
        this.historyRepository = historyRepository;
    }

    public CreateOrderResult createOrder(CreateOrderRequest request) {
        String orderId = newOrderId();
        boolean simulateFailure = Boolean.TRUE.equals(request.simulatePaymentFailure());

        ProductResponse product;
        try {
            product = inventoryGateway.getProduct(request.productId());
        } catch (InventoryUnavailableException ex) {
            OrderEntity order = stateMachine.createPending(newOrder(orderId, request, null, 0L, simulateFailure),
                    "Tạo đơn, chưa lấy được thông tin sản phẩm");
            log.info("[SAGA][{}] Tạo đơn PENDING cho khách {}, sản phẩm {}, số lượng {}", orderId,
                    request.customerId(), request.productId(), request.quantity());
            reject(orderId, "Không lấy được thông tin sản phẩm: " + ex.getMessage());
            return new CreateOrderResult(Outcome.INVENTORY_UNAVAILABLE, getOrder(order.getOrderId()));
        }

        long unitPrice = product.price() != null ? product.price() : 0L;
        OrderEntity order = stateMachine.createPending(
                newOrder(orderId, request, product.name(), unitPrice, simulateFailure),
                "Tạo đơn, chờ giữ kho");
        log.info("[SAGA][{}] Tạo đơn PENDING cho khách {}, sản phẩm {}, số lượng {}, tổng tiền {}", orderId,
                request.customerId(), request.productId(), request.quantity(), order.getTotalAmount());

        DeductStockResponse deducted;
        try {
            deducted = inventoryGateway.deductStock(request.productId(), orderId, request.quantity());
        } catch (InsufficientStockException ex) {
            reject(orderId, "Hết hàng: " + ex.getMessage());
            return new CreateOrderResult(Outcome.OUT_OF_STOCK, getOrder(orderId));
        } catch (ProductNotFoundException ex) {
            reject(orderId, "Sản phẩm không tồn tại: " + ex.getMessage());
            return new CreateOrderResult(Outcome.PRODUCT_NOT_FOUND, getOrder(orderId));
        } catch (InventoryUnavailableException ex) {
            String reason = "Kho không khả dụng khi trừ kho: " + ex.getMessage();
            reject(orderId, reason);
            if (ex.isCircuitOpen()) {
                log.info("[SAGA][{}] Cầu dao đang mở, yêu cầu trừ kho chưa được gửi, không cần bù trừ", orderId);
            } else {
                eventPublisher.publishOrderCancelled(reloadEntity(orderId), reason);
            }
            return new CreateOrderResult(Outcome.INVENTORY_UNAVAILABLE, getOrder(orderId));
        }

        String instance = deducted != null ? deducted.servedBy() : null;
        boolean reserved = stateMachine.markReserved(orderId, instance,
                "Giữ kho thành công tại " + instance);
        if (!reserved) {
            log.error("[SAGA][{}] Không chuyển được sang INVENTORY_RESERVED, phát ORDER_CANCELLED để hoàn kho",
                    orderId);
            eventPublisher.publishOrderCancelled(reloadEntity(orderId), "Lỗi nội bộ khi giữ kho");
            return new CreateOrderResult(Outcome.INVENTORY_UNAVAILABLE, getOrder(orderId));
        }
        log.info("[SAGA][{}] Giữ kho thành công tại {}, còn lại {}, chuyển INVENTORY_RESERVED", orderId,
                instance, deducted != null ? deducted.remainingStock() : null);

        eventPublisher.publishOrderCreated(reloadEntity(orderId));
        return new CreateOrderResult(Outcome.RESERVED, getOrder(orderId));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        OrderEntity order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return OrderResponse.from(order, historyRepository.findByOrderIdOrderByIdAsc(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(o -> OrderResponse.from(o, historyRepository.findByOrderIdOrderByIdAsc(o.getOrderId())))
                .toList();
    }

    private void reject(String orderId, String reason) {
        boolean changed = stateMachine.transition(orderId, OrderStatus.PENDING, OrderStatus.REJECTED, reason, reason);
        if (changed) {
            log.info("[SAGA][{}] Đơn bị từ chối, chuyển REJECTED: {}", orderId, reason);
        } else {
            log.warn("[SAGA][{}] Không chuyển được sang REJECTED vì đơn không còn ở PENDING", orderId);
        }
    }

    private OrderEntity reloadEntity(String orderId) {
        return orderRepository.findByOrderId(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private static OrderEntity newOrder(String orderId, CreateOrderRequest request, String productName,
                                        long unitPrice, boolean simulateFailure) {
        OrderEntity order = new OrderEntity();
        order.setOrderId(orderId);
        order.setCustomerId(request.customerId());
        order.setProductId(request.productId());
        order.setProductName(productName);
        order.setQuantity(request.quantity());
        order.setUnitPrice(unitPrice);
        order.setTotalAmount(Math.multiplyExact(unitPrice, request.quantity().longValue()));
        order.setSimulatePaymentFailure(simulateFailure);
        return order;
    }

    private static String newOrderId() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
