package com.shopmart.inventory.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class ProductLoadTracker {

    private final ThreadLocal<Boolean> loadedFromDatabase = new ThreadLocal<>();
    private final AtomicLong dbQueryCount = new AtomicLong();

    public void startTracking() {
        loadedFromDatabase.set(Boolean.FALSE);
    }

    public void markDatabaseLoad() {
        dbQueryCount.incrementAndGet();
        loadedFromDatabase.set(Boolean.TRUE);
    }

    public boolean wasLoadedFromDatabase() {
        return Boolean.TRUE.equals(loadedFromDatabase.get());
    }

    public void stopTracking() {
        loadedFromDatabase.remove();
    }

    public long getDbQueryCount() {
        return dbQueryCount.get();
    }

    public void reset() {
        dbQueryCount.set(0);
    }
}
