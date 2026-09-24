package com.shopmart.inventory.config;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class WriteGuardedCache implements Cache {

    private final Cache target;
    private final CacheErrorHandler errorHandler;

    public WriteGuardedCache(Cache target, CacheErrorHandler errorHandler) {
        this.target = target;
        this.errorHandler = errorHandler;
    }

    public Cache getTarget() {
        return target;
    }

    @Override
    public String getName() {
        return target.getName();
    }

    @Override
    public Object getNativeCache() {
        return target.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        return target.get(key);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return target.get(key, type);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return target.get(key, valueLoader);
    }

    @Override
    public CompletableFuture<?> retrieve(Object key) {
        return target.retrieve(key);
    }

    @Override
    public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
        return target.retrieve(key, valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
        try {
            target.put(key, value);
        } catch (RuntimeException ex) {
            errorHandler.handleCachePutError(ex, target, key, value);
        }
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        try {
            return target.putIfAbsent(key, value);
        } catch (RuntimeException ex) {
            errorHandler.handleCachePutError(ex, target, key, value);
            return null;
        }
    }

    @Override
    public void evict(Object key) {
        try {
            target.evict(key);
        } catch (RuntimeException ex) {
            errorHandler.handleCacheEvictError(ex, target, key);
        }
    }

    @Override
    public boolean evictIfPresent(Object key) {
        try {
            return target.evictIfPresent(key);
        } catch (RuntimeException ex) {
            errorHandler.handleCacheEvictError(ex, target, key);
            return false;
        }
    }

    @Override
    public void clear() {
        try {
            target.clear();
        } catch (RuntimeException ex) {
            errorHandler.handleCacheClearError(ex, target);
        }
    }

    @Override
    public boolean invalidate() {
        try {
            return target.invalidate();
        } catch (RuntimeException ex) {
            errorHandler.handleCacheClearError(ex, target);
            return false;
        }
    }
}
