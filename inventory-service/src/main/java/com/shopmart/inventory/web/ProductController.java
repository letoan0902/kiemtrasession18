package com.shopmart.inventory.web;

import com.shopmart.inventory.cache.ProductCacheData;
import com.shopmart.inventory.service.ChaosService;
import com.shopmart.inventory.service.DeductResult;
import com.shopmart.inventory.service.ProductService;
import com.shopmart.inventory.service.ProductSource;
import com.shopmart.inventory.service.ProductView;
import com.shopmart.inventory.service.RestoreResult;
import com.shopmart.inventory.service.StockService;
import com.shopmart.inventory.support.InstanceIdentity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class ProductController {

    private final ProductService productService;
    private final StockService stockService;
    private final ChaosService chaosService;
    private final InstanceIdentity instance;

    public ProductController(ProductService productService, StockService stockService,
                             ChaosService chaosService, InstanceIdentity instance) {
        this.productService = productService;
        this.stockService = stockService;
        this.chaosService = chaosService;
        this.instance = instance;
    }

    @GetMapping("/products")
    public List<ProductResponse> listProducts() {
        chaosService.ensureAvailable("danh sách sản phẩm");
        return productService.listProducts().stream()
                .map(p -> ProductResponse.of(p, ProductSource.DATABASE, instance.servedBy()))
                .toList();
    }

    @GetMapping("/products/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        chaosService.ensureAvailable("đọc sản phẩm");
        ProductView view = productService.getProduct(id);
        return ProductResponse.of(view.data(), view.source(), instance.servedBy());
    }

    @PostMapping("/products/{id}/deduct")
    public DeductResponse deduct(@PathVariable Long id, @Valid @RequestBody DeductRequest request) {
        chaosService.ensureAvailable("trừ kho");
        DeductResult r = stockService.deduct(id, request.orderId(), request.quantity());
        return new DeductResponse(r.orderId(), r.productId(), r.quantity(), r.remainingStock(),
                instance.servedBy(), r.alreadyProcessed());
    }

    @PostMapping("/products/{id}/restore")
    public RestoreResponse restore(@PathVariable Long id, @Valid @RequestBody RestoreRequest request) {
        RestoreResult r = stockService.restore(request.orderId(), id, "REST");
        return new RestoreResponse(r.orderId(), r.productId(), r.quantity(), r.remainingStock(),
                r.restored(), r.reason(), instance.servedBy());
    }

    @PutMapping("/products/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        ProductCacheData data = productService.updateProduct(id, request.name(), request.price(), request.stock());
        return ProductResponse.of(data, ProductSource.DATABASE, instance.servedBy());
    }

    @GetMapping("/reservations/{orderId}")
    public ReservationResponse getReservation(@PathVariable String orderId) {
        return ReservationResponse.of(stockService.getReservation(orderId), instance.servedBy());
    }
}
