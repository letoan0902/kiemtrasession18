package com.shopmart.inventory.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WriteGuardedCacheManager implements CacheManager {

    private final CacheManager target;
    private final CacheErrorHandler errorHandler;
    private final ConcurrentMap<String, Cache> guardedCaches = new ConcurrentHashMap<>();

    public WriteGuardedCacheManager(CacheManager target, CacheErrorHandler errorHandler) {
        this.target = target;
        this.errorHandler = errorHandler;
    }

    public CacheManager getTarget() {
        return target;
    }

    @Override
    public Cache getCache(String name) {
        Cache existing = guardedCaches.get(name);
        if (existing != null) {
            return existing;
        }
        Cache original = target.getCache(name);
        if (original == null) {
            return null;
        }
        return guardedCaches.computeIfAbsent(name, n -> guard(original));
    }

    @Override
    public Collection<String> getCacheNames() {
        return target.getCacheNames();
    }

    private Cache guard(Cache original) {
        if (original instanceof TransactionAwareCacheDecorator decorator) {
            return new TransactionAwareCacheDecorator(
                    new WriteGuardedCache(decorator.getTargetCache(), errorHandler));
        }
        return new WriteGuardedCache(original, errorHandler);
    }
}
