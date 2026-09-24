package com.shopmart.order.web;

import com.shopmart.order.client.ProductResponse;
import com.shopmart.order.dto.CreateOrderRequest;
import com.shopmart.order.dto.CreateOrderResult;
import com.shopmart.order.dto.OrderResponse;
import com.shopmart.order.exception.InventoryUnavailableException;
import com.shopmart.order.gateway.InventoryGateway;
import com.shopmart.order.service.OrderService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;
    private final InventoryGateway inventoryGateway;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final String configSource;

    public OrderController(OrderService orderService, InventoryGateway inventoryGateway,
                           CircuitBreakerRegistry circuitBreakerRegistry,
                           @Value("${shopmart.config.nguon:local}") String configSource) {
        this.orderService = orderService;
        this.inventoryGateway = inventoryGateway;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.configSource = configSource;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderResult result = orderService.createOrder(request);
        HttpStatus status = switch (result.outcome()) {
            case RESERVED -> HttpStatus.CREATED;
            case OUT_OF_STOCK -> HttpStatus.CONFLICT;
            case PRODUCT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVENTORY_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return ResponseEntity.status(status).body(result.order());
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId);
    }

    @GetMapping("/orders")
    public List<OrderResponse> listOrders() {
        return orderService.listOrders();
    }

    @GetMapping("/products/{id}")
    public Map<String, Object> getProduct(@PathVariable Long id) {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            ProductResponse product = inventoryGateway.getProduct(id);
            body.put("id", product.id());
            body.put("name", product.name());
            body.put("price", product.price());
            body.put("stock", product.stock());
            body.put("servedBy", product.servedBy());
            body.put("source", product.source());
            body.put("fallback", false);
        } catch (InventoryUnavailableException ex) {
            body.put("id", id);
            body.put("fallback", true);
            body.put("message", InventoryGateway.UNAVAILABLE_MESSAGE);
            body.put("fallbackReason", ex.isCircuitOpen() ? "CIRCUIT_OPEN" : "INVENTORY_ERROR");
            body.put("detail", ex.getMessage());
        }
        body.put("circuitBreakerState", inventoryCircuitBreaker().getState().name());
        return body;
    }

    @GetMapping("/circuit-breaker")
    public Map<String, Object> circuitBreaker() {
        CircuitBreaker cb = inventoryCircuitBreaker();
        CircuitBreaker.Metrics metrics = cb.getMetrics();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", cb.getName());
        body.put("state", cb.getState().name());
        body.put("failureRate", metrics.getFailureRate());
        body.put("bufferedCalls", metrics.getNumberOfBufferedCalls());
        body.put("failedCalls", metrics.getNumberOfFailedCalls());
        body.put("notPermittedCalls", metrics.getNumberOfNotPermittedCalls());
        return body;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("configSource", configSource);
        return body;
    }

    private CircuitBreaker inventoryCircuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker(InventoryGateway.CIRCUIT_BREAKER_NAME);
    }
}
