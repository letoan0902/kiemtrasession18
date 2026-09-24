package com.shopmart.inventory.web;

import java.time.Instant;

public record ErrorResponse(Instant timestamp, int status, String error, String message, String path,
                            String servedBy) {
}
