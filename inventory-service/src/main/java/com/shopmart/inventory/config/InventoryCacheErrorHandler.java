package com.shopmart.inventory.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

import java.util.concurrent.atomic.AtomicLong;

public class InventoryCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(InventoryCacheErrorHandler.class);

    private final AtomicLong errorCount = new AtomicLong();

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        errorCount.incrementAndGet();
        log.warn("Đọc cache '{}' khóa {} thất bại ({}). Rơi xuống CSDL, service vẫn phục vụ bình thường.",
                cacheName(cache), key, describe(exception));
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        errorCount.incrementAndGet();
        log.warn("Ghi cache '{}' khóa {} thất bại ({}). Bỏ qua, lần đọc sau sẽ lấy từ CSDL.",
                cacheName(cache), key, describe(exception));
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        errorCount.incrementAndGet();
        log.warn("Xóa cache '{}' khóa {} thất bại ({}). Mục cache cũ có thể còn tới hết TTL.",
                cacheName(cache), key, describe(exception));
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        errorCount.incrementAndGet();
        log.warn("Xóa toàn bộ cache '{}' thất bại ({}). Các mục cũ có thể còn tới hết TTL.",
                cacheName(cache), describe(exception));
    }

    public long getErrorCount() {
        return errorCount.get();
    }

    public void reset() {
        errorCount.set(0);
    }

    private static String cacheName(Cache cache) {
        return cache == null ? "?" : cache.getName();
    }

    private static String describe(RuntimeException exception) {
        return exception.getClass().getSimpleName() + ": " + exception.getMessage();
    }
}
