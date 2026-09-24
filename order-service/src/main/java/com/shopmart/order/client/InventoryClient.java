package com.shopmart.order.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", configuration = InventoryFeignConfig.class)
public interface InventoryClient {

    @GetMapping("/api/inventory/products/{id}")
    ProductResponse getProduct(@PathVariable("id") Long id);

    @PostMapping("/api/inventory/products/{id}/deduct")
    DeductStockResponse deductStock(@PathVariable("id") Long id, @RequestBody DeductStockRequest body);

    @PostMapping("/api/inventory/products/{id}/restore")
    Map<String, Object> restoreStock(@PathVariable("id") Long id, @RequestBody RestoreStockRequest body);
}
