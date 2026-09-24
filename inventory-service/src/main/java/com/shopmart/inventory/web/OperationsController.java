package com.shopmart.inventory.web;

import com.shopmart.inventory.config.CacheConfig;
import com.shopmart.inventory.config.InventoryCacheErrorHandler;
import com.shopmart.inventory.service.ChaosService;
import com.shopmart.inventory.service.ProductLoadTracker;
import com.shopmart.inventory.service.ProductService;
import com.shopmart.inventory.support.InstanceIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class OperationsController {

    private final ProductService productService;
    private final ProductLoadTracker tracker;
    private final InventoryCacheErrorHandler cacheErrorHandler;
    private final ChaosService chaosService;
    private final InstanceIdentity instance;
    private final String configSource;

    public OperationsController(ProductService productService,
                                ProductLoadTracker tracker,
                                InventoryCacheErrorHandler cacheErrorHandler,
                                ChaosService chaosService,
                                InstanceIdentity instance,
                                @Value("${shopmart.config.nguon:local}") String configSource) {
        this.productService = productService;
        this.tracker = tracker;
        this.cacheErrorHandler = cacheErrorHandler;
        this.chaosService = chaosService;
        this.instance = instance;
        this.configSource = configSource;
    }

    @DeleteMapping("/cache")
    public MessageResponse clearCache() {
        productService.evictAllProducts();
        return new MessageResponse("Đã xóa toàn bộ vùng cache '" + CacheConfig.PRODUCTS_CACHE + "'",
                instance.servedBy());
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return new StatsResponse(instance.servedBy(), tracker.getDbQueryCount(), cacheErrorHandler.getErrorCount());
    }

    @DeleteMapping("/stats")
    public StatsResponse resetStats() {
        tracker.reset();
        cacheErrorHandler.reset();
        return stats();
    }

    @PostMapping("/admin/chaos")
    public ChaosResponse setChaos(@RequestParam String mode) {
        ChaosService.Mode newMode = chaosService.setMode(mode);
        String message = newMode == ChaosService.Mode.DOWN
                ? "Đã bật giả lập sự cố: GET sản phẩm và trừ kho trả 503, hoàn kho vẫn chạy"
                : "Đã trở về chế độ bình thường";
        return new ChaosResponse(newMode.code(), instance.servedBy(), message);
    }

    @GetMapping("/admin/chaos")
    public ChaosResponse getChaos() {
        return new ChaosResponse(chaosService.getMode().code(), instance.servedBy(), "Chế độ hiện tại");
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", instance.servedBy(), configSource);
    }
}
