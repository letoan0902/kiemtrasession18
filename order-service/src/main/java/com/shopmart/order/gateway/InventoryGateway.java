package com.shopmart.order.gateway;

import com.shopmart.order.client.DeductStockRequest;
import com.shopmart.order.client.DeductStockResponse;
import com.shopmart.order.client.InventoryClient;
import com.shopmart.order.client.ProductResponse;
import com.shopmart.order.exception.InventoryBusinessException;
import com.shopmart.order.exception.InventoryUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InventoryGateway {

    public static final String CIRCUIT_BREAKER_NAME = "inventoryService";

    public static final String UNAVAILABLE_MESSAGE = "Kho hàng tạm thời không khả dụng, vui lòng thử lại sau";
    public static final String CIRCUIT_OPEN_MESSAGE =
            "Cầu dao inventoryService đang mở, tạm ngừng gọi kho hàng, vui lòng thử lại sau";

    private static final Logger log = LoggerFactory.getLogger(InventoryGateway.class);

    private final InventoryClient inventoryClient;

    public InventoryGateway(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getProductFallback")
    public ProductResponse getProduct(Long productId) {
        return inventoryClient.getProduct(productId);
    }

    private ProductResponse getProductFallback(Long productId, InventoryBusinessException ex) {
        throw ex;
    }

    private ProductResponse getProductFallback(Long productId, CallNotPermittedException ex) {
        log.warn("Cầu dao {} đang mở, không gọi kho để lấy sản phẩm {}", CIRCUIT_BREAKER_NAME, productId);
        throw new InventoryUnavailableException(CIRCUIT_OPEN_MESSAGE, true, ex);
    }

    private ProductResponse getProductFallback(Long productId, Throwable ex) {
        log.warn("Gọi kho lấy sản phẩm {} thất bại: {}", productId, ex.toString());
        throw new InventoryUnavailableException(UNAVAILABLE_MESSAGE + " (" + ex.getMessage() + ")", false, ex);
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "deductStockFallback")
    public DeductStockResponse deductStock(Long productId, String orderId, int quantity) {
        return inventoryClient.deductStock(productId, new DeductStockRequest(orderId, quantity));
    }

    private DeductStockResponse deductStockFallback(Long productId, String orderId, int quantity,
                                                    InventoryBusinessException ex) {
        throw ex;
    }

    private DeductStockResponse deductStockFallback(Long productId, String orderId, int quantity,
                                                    CallNotPermittedException ex) {
        log.warn("[SAGA][{}] Cầu dao {} đang mở, không gửi yêu cầu trừ kho", orderId, CIRCUIT_BREAKER_NAME);
        throw new InventoryUnavailableException(CIRCUIT_OPEN_MESSAGE, true, ex);
    }

    private DeductStockResponse deductStockFallback(Long productId, String orderId, int quantity, Throwable ex) {
        log.error("[SAGA][{}] Trừ kho thất bại, không chắc kho đã trừ hay chưa: {}", orderId, ex.toString());
        throw new InventoryUnavailableException(UNAVAILABLE_MESSAGE + " (" + ex.getMessage() + ")", false, ex);
    }
}
