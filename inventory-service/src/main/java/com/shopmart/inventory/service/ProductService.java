package com.shopmart.inventory.service;

import com.shopmart.inventory.cache.ProductCacheData;
import com.shopmart.inventory.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductCacheService productCacheService;
    private final ProductRepository productRepository;
    private final ProductLoadTracker tracker;

    public ProductService(ProductCacheService productCacheService,
                          ProductRepository productRepository,
                          ProductLoadTracker tracker) {
        this.productCacheService = productCacheService;
        this.productRepository = productRepository;
        this.tracker = tracker;
    }

    public ProductView getProduct(Long id) {
        tracker.startTracking();
        try {
            ProductCacheData data = productCacheService.findById(id);
            ProductSource source = tracker.wasLoadedFromDatabase() ? ProductSource.DATABASE : ProductSource.CACHE;
            return new ProductView(data, source);
        } finally {
            tracker.stopTracking();
        }
    }

    public List<ProductCacheData> listProducts() {
        return productRepository.findAll(Sort.by("id")).stream()
                .map(ProductCacheData::from)
                .toList();
    }

    public ProductCacheData updateProduct(Long id, String name, long price, int stock) {
        return productCacheService.update(id, name, price, stock);
    }

    public void evictAllProducts() {
        productCacheService.evictAll();
    }
}
