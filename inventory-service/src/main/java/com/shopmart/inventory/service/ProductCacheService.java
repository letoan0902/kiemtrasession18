package com.shopmart.inventory.service;

import com.shopmart.inventory.cache.ProductCacheData;
import com.shopmart.inventory.config.CacheConfig;
import com.shopmart.inventory.domain.Product;
import com.shopmart.inventory.exception.ProductNotFoundException;
import com.shopmart.inventory.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ProductCacheService {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheService.class);

    private final ProductRepository productRepository;
    private final ProductLoadTracker tracker;

    public ProductCacheService(ProductRepository productRepository, ProductLoadTracker tracker) {
        this.productRepository = productRepository;
        this.tracker = tracker;
    }

    @Cacheable(cacheNames = CacheConfig.PRODUCTS_CACHE, key = "#id")
    public ProductCacheData findById(Long id) {
        tracker.markDatabaseLoad();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        log.info("Trượt cache sản phẩm {}, đã đọc từ CSDL và nạp vào cache", id);
        return ProductCacheData.from(product);
    }

    @CachePut(cacheNames = CacheConfig.PRODUCTS_CACHE, key = "#id")
    @Transactional
    public ProductCacheData update(Long id, String name, long price, int stock) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.saveAndFlush(product);
        log.info("Đã cập nhật sản phẩm {}: giá {}, tồn kho {}. Cache sẽ được ghi đè sau commit", id, price, stock);
        return ProductCacheData.from(product);
    }

    @CacheEvict(cacheNames = CacheConfig.PRODUCTS_CACHE, allEntries = true)
    public void evictAll() {
        log.info("Đã yêu cầu xóa toàn bộ vùng cache '{}'", CacheConfig.PRODUCTS_CACHE);
    }
}
