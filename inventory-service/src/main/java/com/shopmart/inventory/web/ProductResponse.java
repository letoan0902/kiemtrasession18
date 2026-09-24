package com.shopmart.inventory.web;

import com.shopmart.inventory.cache.ProductCacheData;
import com.shopmart.inventory.service.ProductSource;

public record ProductResponse(Long id, String name, long price, int stock, String servedBy, String source) {

    public static ProductResponse of(ProductCacheData data, ProductSource source, String servedBy) {
        return new ProductResponse(data.getId(), data.getName(), data.getPrice(), data.getStock(),
                servedBy, source.name());
    }
}
