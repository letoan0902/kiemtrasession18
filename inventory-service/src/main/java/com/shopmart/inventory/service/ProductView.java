package com.shopmart.inventory.service;

import com.shopmart.inventory.cache.ProductCacheData;

public record ProductView(ProductCacheData data, ProductSource source) {
}
