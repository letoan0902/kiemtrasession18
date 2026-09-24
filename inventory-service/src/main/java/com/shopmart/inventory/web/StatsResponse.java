package com.shopmart.inventory.web;

public record StatsResponse(String servedBy, long dbQueryCount, long cacheErrors) {
}
